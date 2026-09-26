package org.example.onlinepossystem.special;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class SpecialUiContractTest {

    @Test
    void browserUsesTheQuoteResponseLineTotal() throws IOException {
        try (var stream = getClass().getClassLoader().getResourceAsStream("templates/PlaceOrder.html")) {
            assertThat(stream).isNotNull();
            String template = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(template)
                    .contains("money(quote.lineTotal)")
                    .contains("Number(quote.lineTotal)")
                    .contains("if (select.value) {")
                    .contains("`${state.apiBase}/${branchId}/specials/week`")
                    .contains("api.getSpecials(branchId)")
                    .contains("specialScheduleLabel(special.days)")
                    .contains("special.availableToday")
                    .contains("const lockedSizeCm = Number(state.special.pending?.sizeCm)")
                    .contains("sizes.find(size => Number(size.sizeCm) === 30)")
                    .contains("|| sizes[0]")
                    .contains("state.customize.sizeCm = defaultSize ? Number(defaultSize.sizeCm) : null")
                    .doesNotContain("select.value && !component.allowCustomization")
                    .doesNotContain("quote.finalTotal");
        }
    }
}
