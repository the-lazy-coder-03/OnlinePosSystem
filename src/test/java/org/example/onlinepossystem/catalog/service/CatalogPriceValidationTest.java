package org.example.onlinepossystem.catalog.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class CatalogPriceValidationTest {
    @ParameterizedTest
    @ValueSource(strings = {"NaN", "Infinity", "-Infinity", "-0.01", "100000000", "1.001", "not-a-price"})
    void optionalPriceFieldsRejectValuesThatCannotBeCharged(String price) {
        assertThatThrownBy(() -> CatalogAdminSupport.parsePrice(price)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -1, 100000000, 1.001})
    void singlePriceUpdatesRejectInvalidAmounts(double price) {
        assertThatThrownBy(() -> CatalogAdminSupport.validatedPrice(price)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void validPricesAndEmptyOptionalFieldsRemainSupported() {
        assertThat(CatalogAdminSupport.parsePrice(" ")).isEmpty();
        assertThat(CatalogAdminSupport.parsePrice(null)).isEmpty();
        assertThat(CatalogAdminSupport.parsePrice("0.00")).contains(0.0);
        assertThat(CatalogAdminSupport.parsePrice("99999999.99")).contains(99999999.99);
        assertThat(CatalogAdminSupport.validatedPrice(12.50)).isEqualTo(12.50);
        assertThatThrownBy(() -> CatalogAdminSupport.validatedPrice(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
