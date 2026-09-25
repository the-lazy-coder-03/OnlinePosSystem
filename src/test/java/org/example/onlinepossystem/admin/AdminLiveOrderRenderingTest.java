package org.example.onlinepossystem.admin;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class AdminLiveOrderRenderingTest {

    @Test
    void adminOrdersRenderItemSpecificDetails() throws IOException {
        String adminJs = resourceText("static/js/admin.js");
        String orderRendererJs = resourceText("static/js/pos-order-renderer.js");
        String adminCss = resourceText("static/css/admin.css");

        assertThat(adminJs)
                .contains("appendOrderItems(body, order)")
                .contains("function renderMenuOrderItem(item)")
                .contains("function renderPizzaOrderItem(item)")
                .contains("item.extras || []")
                .contains("item.pizzaSizeCm")
                .contains("item.pizzaBaseOptionName")
                .contains("Extra topping:")
                .contains("Notes:");

        assertThat(adminJs)
                .contains("appendLine(body, \"Gate access\", accessCode)")
                .contains("appendLine(content, \"Gate access\", accessCode)")
                .contains("function gateAccessSummary(order)");
        assertThat(orderRendererJs)
                .contains("`Gate access: ${accessCode}`")
                .contains("['Gate access:', accessCode]")
                .contains("function gateAccess(order)");

        assertThat(adminJs)
                .doesNotContain("appendLine(body, \"Items\", itemSummary(order))")
                .doesNotContain("function itemSummary(order)");

        assertThat(adminCss)
                .contains(".order-item-list")
                .contains(".order-item-details")
                .contains("overflow-wrap: anywhere");
    }

    private String resourceText(String path) throws IOException {
        return new String(new ClassPathResource(path).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
