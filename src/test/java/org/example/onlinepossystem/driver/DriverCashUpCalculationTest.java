package org.example.onlinepossystem.driver;

import org.example.onlinepossystem.customer.api.AccountAccess;
import org.example.onlinepossystem.customer.api.AccountAccessReader;
import org.example.onlinepossystem.ordering.api.PersistedOrderSnapshotReader;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DriverCashUpCalculationTest {
    @ParameterizedTest
    @CsvSource({"15,25,13:00,60.00,75.00,865.00", "15,25,09:01,0.25,75.00,924.75",
            "400,25,13:00,1600.00,75.00,-675.00", "0,0,13:00,0.00,0.00,1000.00"})
    void deductsSavedHourlyAndDeliveryRatesFromPersistedOrderSales(
            BigDecimal hourlyRate, BigDecimal perDeliveryRate, LocalTime clockOut,
            BigDecimal hourlyPay, BigDecimal deliveryPay, BigDecimal expectedCash) {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        AccountAccessReader accounts = mock(AccountAccessReader.class);
        PersistedOrderSnapshotReader orders = mock(PersistedOrderSnapshotReader.class);
        var service = new DriverDispatchService(jdbc, accounts, orders, mock(ApplicationEventPublisher.class));
        var recordedIn = OffsetDateTime.parse("2025-10-18T09:00:00+02:00");
        var shift = new DriverDispatchService.Shift(7, 42, 1, recordedIn.toLocalDate(), recordedIn,
                recordedIn.plusHours(4), null, hourlyRate, perDeliveryRate, null, "CLOCKED_OUT");
        when(accounts.findByUsername("manager")).thenReturn(new AccountAccess(1));
        when(jdbc.query(contains("FROM driver_shift WHERE shift_id"),
                org.mockito.ArgumentMatchers.<RowMapper<DriverDispatchService.Shift>>any(), eq(7L)))
                .thenReturn(List.of(shift));
        when(jdbc.query(contains("SELECT order_id FROM delivery_assignment"),
                org.mockito.ArgumentMatchers.<RowMapper<Long>>any(), eq(7L)))
                .thenReturn(List.of(1L, 2L, 3L));
        when(jdbc.query(contains("FROM delivery_payment"),
                org.mockito.ArgumentMatchers.<RowMapper<BigDecimal[]>>any(), anyLong())).thenReturn(List.of());
        when(orders.findById(1L)).thenReturn(Optional.of(
                new PersistedOrderSnapshotReader.Snapshot(null, new BigDecimal("400.00"))));
        for (long id : List.of(2L, 3L)) {
            when(orders.findById(id)).thenReturn(Optional.of(
                    new PersistedOrderSnapshotReader.Snapshot(null, new BigDecimal("300.00"))));
        }

        var preview = service.preview(new UsernamePasswordAuthenticationToken("manager", "unused"),
                7, 1, LocalTime.of(9, 0), clockOut);

        assertThat(preview.orderSales()).isEqualByComparingTo("1000.00");
        assertThat(preview.deliveredCount()).isEqualTo(3);
        assertThat(preview.hourlyRate()).isEqualByComparingTo(hourlyRate);
        assertThat(preview.perDeliveryRate()).isEqualByComparingTo(perDeliveryRate);
        assertThat(preview.hourlyPay()).isEqualByComparingTo(hourlyPay);
        assertThat(preview.deliveryPay()).isEqualByComparingTo(deliveryPay);
        assertThat(preview.expectedCashDue()).isEqualByComparingTo(expectedCash);
    }
}
