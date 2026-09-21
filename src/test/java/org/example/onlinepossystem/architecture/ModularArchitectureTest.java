package org.example.onlinepossystem.architecture;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static org.assertj.core.api.Assertions.assertThat;

class ModularArchitectureTest {
    private static final String ROOT_PACKAGE = "org.example.onlinepossystem";
    private static final String ROOT_PREFIX = ROOT_PACKAGE + ".";
    private static final Set<String> MODULES = Set.of(
            "admin",
            "bootstrap",
            "branch",
            "catalog",
            "customer",
            "location",
            "ordering",
            "profile",
            "security",
            "shared",
            "staff"
    );
    private static final Map<String, Set<String>> ALLOWED_DEPENDENCIES = Map.ofEntries(
            Map.entry("admin", Set.of("catalog", "customer")),
            Map.entry("bootstrap", Set.of("catalog", "staff")),
            Map.entry("branch", Set.of()),
            Map.entry("catalog", Set.of("branch")),
            Map.entry("customer", Set.of("security")),
            Map.entry("location", Set.of()),
            Map.entry("ordering", Set.of("branch", "catalog", "customer")),
            Map.entry("profile", Set.of("customer", "ordering")),
            Map.entry("security", Set.of()),
            Map.entry("shared", Set.of("customer")),
            Map.entry("staff", Set.of())
    );

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages(ROOT_PACKAGE);

    @Test
    void fieldInjectionIsNotUsedInProductionCode() {
        noFields()
                .should()
                .beAnnotatedWith(Autowired.class)
                .orShould()
                .beAnnotatedWith(Value.class)
                .check(classes);
    }

    @Test
    void webPackagesDoNotDependOnRepositories() {
        noClasses()
                .that()
                .resideInAPackage("..web..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..repository..")
                .check(classes);
    }

    @Test
    void servicesDoNotDependOnWebRequestOrViewTypes() {
        noClasses()
                .that()
                .resideInAPackage("..service..")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName("org.springframework.ui.Model")
                .orShould()
                .dependOnClassesThat()
                .haveFullyQualifiedName("org.springframework.security.core.Authentication")
                .orShould()
                .dependOnClassesThat()
                .resideInAPackage("org.springframework.web..")
                .check(classes);
    }

    @Test
    void externalSdkTypesStayInsideAdaptersAndConfiguration() {
        noClasses()
                .that()
                .resideOutsideOfPackage("..notification.email.resend..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("com.resend..")
                .check(classes);

        noClasses()
                .that()
                .resideOutsideOfPackages("..location.integration..", "..shared.config..")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName("org.springframework.web.client.RestTemplate")
                .check(classes);

        noClasses()
                .that()
                .resideOutsideOfPackage("..customer.notification..")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName("org.springframework.mail.javamail.JavaMailSender")
                .check(classes);
    }

    @Test
    void repositoriesStayInsideTheirOwningModules() {
        noClasses()
                .that()
                .resideOutsideOfPackage("..branch..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..branch.repository..")
                .check(classes);

        noClasses()
                .that()
                .resideOutsideOfPackage("..catalog..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..catalog.repository..", "..catalog.menu.repository..", "..catalog.pizza.repository..")
                .check(classes);

        noClasses()
                .that()
                .resideOutsideOfPackage("..customer..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..customer.repository..")
                .check(classes);

        noClasses()
                .that()
                .resideOutsideOfPackage("..ordering..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..ordering.repository..")
                .check(classes);

        noClasses()
                .that()
                .resideOutsideOfPackage("..staff..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..staff.repository..")
                .check(classes);
    }

    @Test
    void modulesOnlyUseAllowedModuleDependencies() {
        List<String> violations = new ArrayList<>();

        for (JavaClass sourceClass : classes) {
            Optional<String> sourceModule = moduleName(sourceClass.getPackageName());
            if (sourceModule.isEmpty()) {
                continue;
            }

            for (Dependency dependency : sourceClass.getDirectDependenciesFromSelf()) {
                Optional<String> targetModule = moduleName(dependency.getTargetClass().getPackageName());
                if (targetModule.isEmpty() || targetModule.equals(sourceModule)) {
                    continue;
                }
                if (!ALLOWED_DEPENDENCIES.getOrDefault(sourceModule.get(), Set.of()).contains(targetModule.get())) {
                    violations.add(sourceClass.getName() + " -> " + dependency.getTargetClass().getName()
                            + " (" + sourceModule.get() + " -> " + targetModule.get() + ")");
                }
            }
        }

        violations.sort(Comparator.naturalOrder());
        assertThat(violations).isEmpty();
    }

    @Test
    void crossModuleDependenciesUsePublicContractsOnly() {
        List<String> violations = new ArrayList<>();

        for (JavaClass sourceClass : classes) {
            Optional<String> sourceModule = moduleName(sourceClass.getPackageName());
            if (sourceModule.isEmpty()) {
                continue;
            }

            for (Dependency dependency : sourceClass.getDirectDependenciesFromSelf()) {
                JavaClass targetClass = dependency.getTargetClass();
                Optional<String> targetModule = moduleName(targetClass.getPackageName());
                if (targetModule.isEmpty() || targetModule.equals(sourceModule)) {
                    continue;
                }

                String targetPackage = targetClass.getPackageName();
                if (!isPublicContractPackage(targetPackage)) {
                    violations.add(sourceClass.getName() + " -> " + targetClass.getName());
                }
            }
        }

        violations.sort(Comparator.naturalOrder());
        assertThat(violations).isEmpty();
    }

    private Optional<String> moduleName(String packageName) {
        if (!packageName.startsWith(ROOT_PREFIX)) {
            return Optional.empty();
        }

        String remainder = packageName.substring(ROOT_PREFIX.length());
        int separator = remainder.indexOf('.');
        String module = separator == -1 ? remainder : remainder.substring(0, separator);
        return MODULES.contains(module) ? Optional.of(module) : Optional.empty();
    }

    private boolean isPublicContractPackage(String packageName) {
        return packageName.endsWith(".api")
                || packageName.contains(".api.")
                || packageName.endsWith(".dto")
                || packageName.contains(".dto.");
    }
}
