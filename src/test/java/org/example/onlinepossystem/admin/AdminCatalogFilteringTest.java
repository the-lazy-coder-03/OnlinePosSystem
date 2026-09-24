package org.example.onlinepossystem.admin;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class AdminCatalogFilteringTest {

    @Test
    void itemTablesExposeSearchableFilterDataAndAccessibleControls() throws IOException {
        String template = resourceText("templates/admin.html");

        assertThat(template)
                .contains("data-catalog-filter=\"pizza\"")
                .contains("data-catalog-filter=\"menu\"")
                .contains("data-filter-table=\"pizza\"")
                .contains("data-filter-table=\"menu\"")
                .contains("data-filter-search")
                .contains("data-filter-category")
                .contains("data-filter-status")
                .contains("data-filter-format")
                .contains("value=\"unassigned\"")
                .contains("data-filter-formats")
                .contains("data-filter-count aria-live=\"polite\"")
                .contains("data-filter-empty");
    }

    @Test
    void catalogFiltersCombineCriteriaAndCanBeCleared() throws IOException {
        String adminJs = resourceText("static/js/admin.js");
        String adminCss = resourceText("static/css/admin.css");

        assertThat(adminJs)
                .contains("function setupCatalogFilters()")
                .contains("searchableText.includes(query)")
                .contains("row.dataset.filterCategory === selectedCategory")
                .contains("row.dataset.filterStatus === selectedStatus")
                .contains("formats.has(selectedFormat)")
                .contains("row.classList.toggle(\"d-none\", !matches)")
                .contains("empty?.classList.toggle(\"d-none\", visible !== 0)")
                .contains("setupCatalogFilters();");

        assertThat(adminCss)
                .contains(".catalog-filter-toolbar")
                .contains(".catalog-filter-search")
                .contains(".catalog-filter-select")
                .contains(".catalog-filter-summary");
    }

    private String resourceText(String path) throws IOException {
        return new String(new ClassPathResource(path).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
