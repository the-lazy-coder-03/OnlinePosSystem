package org.example.onlinepossystem.driver;

import org.example.onlinepossystem.customer.api.AccountAccess;
import org.example.onlinepossystem.customer.api.AccountAccessChanged;
import org.example.onlinepossystem.customer.api.AccountAccessReader;
import org.example.onlinepossystem.ordering.api.PersistedOrderSnapshotReader;
import org.example.onlinepossystem.ordering.dto.OrderResponseDTO;
import org.example.onlinepossystem.security.api.AccountPrincipal;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class DriverDispatchService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Africa/Johannesburg");
    private static final BigDecimal SIXTY = BigDecimal.valueOf(60);

    private final JdbcTemplate jdbc;
    private final AccountAccessReader accountAccessReader;
    private final PersistedOrderSnapshotReader orders;
    private final ApplicationEventPublisher events;

    public DriverDispatchService(JdbcTemplate jdbc, AccountAccessReader accountAccessReader,
                                 PersistedOrderSnapshotReader orders, ApplicationEventPublisher events) {
        this.jdbc = jdbc;
        this.accountAccessReader = accountAccessReader;
        this.orders = orders;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public DriverDashboard driverDashboard(Authentication authentication) {
        long driverId = driverId(authentication);
        Profile profile = profile(driverId);
        Shift shift = openShift(driverId);
        List<Delivery> deliveries = shift == null ? List.of() : deliveries(shift.id());
        List<Settlement> settlements = jdbc.query("""
                SELECT shift_id, manager_username, order_sales, online_order_paid, online_tips, cash_tips,
                    hourly_pay, delivery_pay, expected_cash_due, cash_handed_in, cash_paid_out,
                    variance, variance_reason, settled_at, approved_clock_in_at, approved_clock_out_at,
                    time_adjustment_reason
                FROM driver_settlement WHERE driver_id = ? ORDER BY settled_at DESC LIMIT 10
                """, this::settlementRow, driverId);
        return new DriverDashboard(profile, shift, deliveries, settlements);
    }

    @Transactional(readOnly = true)
    public ManagerDashboard managerDashboard(Authentication authentication, Integer requestedBranch) {
        int branch = managerBranch(authentication, requestedBranch);
        List<Profile> roster = jdbc.query("""
                SELECT p.driver_id, p.branch_id, p.hourly_rate, p.per_delivery_rate, p.active,
                    c.first_name, c.last_name, c.email
                FROM driver_profile p JOIN customers c ON c.id = p.driver_id
                WHERE p.branch_id = ? ORDER BY c.first_name, c.last_name, p.driver_id
                """, this::profileRow, branch);
        List<OrderBrief> queue = jdbc.query("""
                SELECT o.order_id, o.customer_name, o.status, o.created_at
                FROM customer_order o WHERE o.branch_id = ? AND o.order_type = 'delivery'
                  AND o.status IN ('Pending', 'Preparing', 'Completed')
                  AND NOT EXISTS (SELECT 1 FROM delivery_assignment a WHERE a.order_id = o.order_id)
                ORDER BY o.created_at, o.order_id LIMIT 100
                """, (rs, row) -> new OrderBrief(rs.getLong(1), rs.getString(2), rs.getString(3),
                        rs.getTimestamp(4).toLocalDateTime().toString()), branch);
        List<Shift> shifts = jdbc.query("""
                SELECT shift_id, driver_id, branch_id, business_date, clocked_in_at, clocked_out_at,
                    approved_minutes, hourly_rate, per_delivery_rate, returning_at, state
                FROM driver_shift WHERE branch_id = ? AND state <> 'SETTLED'
                ORDER BY clocked_in_at DESC
                """, this::shiftRow, branch);
        List<Delivery> assigned = jdbc.query("""
                SELECT assignment_id, order_id, driver_id, shift_id, branch_id, state, self_claimed,
                    assigned_at, picked_up_at, delivered_at
                FROM delivery_assignment WHERE branch_id = ? AND state <> 'CANCELLED'
                  AND shift_id IN (SELECT shift_id FROM driver_shift WHERE state <> 'SETTLED')
                ORDER BY assigned_at DESC
                """, (rs, row) -> deliveryRow(rs), branch);
        return new ManagerDashboard(branch, roster, queue, shifts, assigned);
    }

    @Transactional
    public Profile promote(Authentication authentication, String email, Integer requestedBranch) {
        int branch = managerBranch(authentication, requestedBranch);
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Registered email is required.");
        Long id;
        try {
            id = jdbc.queryForObject("SELECT app_security.promote_branch_driver(?, ?)",
                    Long.class, email.trim(), branch);
        } catch (DataAccessException exception) {
            if (hasSqlState(exception, "22023")) throw new IllegalArgumentException("A registered customer account is required.");
            if (hasSqlState(exception, "42501")) throw new AccessDeniedException("Driver promotion is not permitted.");
            throw exception;
        }
        Profile profile = profile(id);
        events.publishEvent(new AccountAccessChanged(id, profile.email()));
        changed(branch, profile.email(), "DRIVER_PROMOTED");
        return profile;
    }

    @Transactional
    public Profile updateRates(Authentication authentication, long driverId, Integer requestedBranch,
                               BigDecimal hourlyRate, BigDecimal perDeliveryRate) {
        int branch = managerBranch(authentication, requestedBranch);
        requireRate(hourlyRate);
        requireRate(perDeliveryRate);
        lockDriver(driverId);
        int count = jdbc.update("""
                UPDATE driver_profile SET hourly_rate = ?, per_delivery_rate = ?
                WHERE driver_id = ? AND branch_id = ?
                """, money(hourlyRate), money(perDeliveryRate), driverId, branch);
        if (count != 1) throw new NoSuchElementException("Driver not found in this branch.");
        List<Shift> shifts = jdbc.query("""
                SELECT shift_id, driver_id, branch_id, business_date, clocked_in_at, clocked_out_at,
                    approved_minutes, hourly_rate, per_delivery_rate, returning_at, state
                FROM driver_shift WHERE driver_id = ? AND branch_id = ?
                    AND state IN ('ACTIVE', 'CLOCKED_OUT') FOR UPDATE
                """, this::shiftRow, driverId, branch);
        for (Shift shift : shifts) {
            if (shift.hourlyRate().compareTo(hourlyRate) == 0
                    && shift.perDeliveryRate().compareTo(perDeliveryRate) == 0) continue;
            jdbc.update("UPDATE driver_shift SET hourly_rate = ?, per_delivery_rate = ? WHERE shift_id = ?",
                    money(hourlyRate), money(perDeliveryRate), shift.id());
            jdbc.update("""
                    INSERT INTO driver_dispatch_event(shift_id, actor_id, actor_username, branch_id,
                        event_type, details)
                    VALUES (?, ?, ?, ?, 'SHIFT_RATES_CHANGED', jsonb_build_object(
                        'oldHourlyRate', CAST(? AS numeric), 'newHourlyRate', CAST(? AS numeric),
                        'oldPerDeliveryRate', CAST(? AS numeric), 'newPerDeliveryRate', CAST(? AS numeric)))
                    """, shift.id(), actorId(authentication), authentication.getName(), branch,
                    shift.hourlyRate(), money(hourlyRate), shift.perDeliveryRate(), money(perDeliveryRate));
        }
        changed(branch, profile(driverId).email(), "RATES_CHANGED");
        return profile(driverId);
    }

    @Transactional
    public Shift clockIn(Authentication authentication) {
        long driverId = driverId(authentication);
        lockDriver(driverId);
        Profile profile = profile(driverId);
        if (!profile.active()) throw new AccessDeniedException("Driver account is inactive.");
        Shift existing = openShift(driverId);
        if (existing != null) {
            if ("ACTIVE".equals(existing.state())) return existing;
            throw new IllegalStateException("The previous shift must be cashed up first.");
        }
        jdbc.update("""
                INSERT INTO driver_shift(driver_id, branch_id, business_date, hourly_rate, per_delivery_rate)
                VALUES (?, ?, ?, ?, ?)
                """, driverId, profile.branchId(), LocalDate.now(BUSINESS_ZONE),
                profile.hourlyRate(), profile.perDeliveryRate());
        changed(profile.branchId(), profile.email(), "CLOCKED_IN");
        return openShift(driverId);
    }

    @Transactional
    public Shift clockOut(Authentication authentication) {
        long driverId = driverId(authentication);
        lockDriver(driverId);
        Shift existing = openShift(driverId);
        if (existing != null && "CLOCKED_OUT".equals(existing.state())) return existing;
        Shift shift = requiredActiveShift(driverId);
        Integer unfinished = jdbc.queryForObject("""
                SELECT count(*) FROM delivery_assignment WHERE shift_id = ? AND state IN ('ASSIGNED', 'PICKED_UP')
                """, Integer.class, shift.id());
        if (unfinished != null && unfinished > 0) throw new IllegalStateException("Finish assigned deliveries first.");
        jdbc.update("UPDATE driver_shift SET clocked_out_at = now(), state = 'CLOCKED_OUT' WHERE shift_id = ?",
                shift.id());
        changed(shift.branchId(), authentication.getName(), "CLOCKED_OUT");
        return openShift(driverId);
    }

    @Transactional
    public DriverDashboard claimNext(Authentication authentication) {
        long driverId = driverId(authentication);
        Shift shift = requiredActiveShift(driverId);
        Integer unfinished = jdbc.queryForObject("""
                SELECT count(*) FROM delivery_assignment
                WHERE driver_id = ? AND state IN ('ASSIGNED', 'PICKED_UP')
                """, Integer.class, driverId);
        if (unfinished != null && unfinished > 0)
            throw new IllegalStateException("Finish assigned deliveries before claiming another.");
        Long orderId;
        try {
            orderId = jdbc.queryForObject("SELECT app_security.claim_next_delivery(?)", Long.class, shift.id());
        } catch (DataAccessException exception) {
            if (hasSqlState(exception, "22023")) throw new IllegalStateException("Finish your claimed delivery first.");
            if (hasSqlState(exception, "42501")) throw new AccessDeniedException("Driver claim is not permitted.");
            throw exception;
        }
        if (orderId == null) throw new NoSuchElementException("No unassigned delivery is available.");
        Long assignmentId = jdbc.queryForObject("SELECT assignment_id FROM delivery_assignment WHERE order_id = ?",
                Long.class, orderId);
        recordEvent(shift.id(), assignmentId, driverId, authentication.getName(), shift.branchId(), "CLAIMED");
        jdbc.update("UPDATE driver_shift SET returning_at = NULL WHERE shift_id = ?", shift.id());
        changed(shift.branchId(), authentication.getName(), "CLAIMED");
        return driverDashboard(authentication);
    }

    @Transactional
    public DriverDashboard pickup(Authentication authentication, long orderId) {
        long driverId = driverId(authentication);
        Shift shift = requiredActiveShift(driverId);
        String kitchenStatus = jdbc.queryForObject("""
                SELECT o.status FROM customer_order o JOIN delivery_assignment a ON a.order_id = o.order_id
                WHERE o.order_id = ? AND a.driver_id = ? AND a.shift_id = ? AND a.state = 'ASSIGNED'
                """, String.class, orderId, driverId, shift.id());
        if (!List.of("Pending", "Preparing", "Completed").contains(kitchenStatus)) {
            throw new IllegalStateException("This order is no longer available for pickup.");
        }
        int updated = jdbc.update("""
                UPDATE delivery_assignment SET state = 'PICKED_UP', picked_up_at = now()
                WHERE order_id = ? AND driver_id = ? AND shift_id = ? AND state = 'ASSIGNED'
                """, orderId, driverId, shift.id());
        if (updated != 1) throw new IllegalStateException("Delivery was already updated.");
        Long assignmentId = assignmentId(orderId);
        recordEvent(shift.id(), assignmentId, driverId, authentication.getName(), shift.branchId(), "PICKED_UP");
        jdbc.update("UPDATE driver_shift SET returning_at = NULL WHERE shift_id = ?", shift.id());
        changed(shift.branchId(), authentication.getName(), "PICKED_UP");
        return driverDashboard(authentication);
    }

    @Transactional
    public DriverDashboard delivered(Authentication authentication, long orderId) {
        long driverId = driverId(authentication);
        Shift shift = requiredActiveShift(driverId);
        int updated = jdbc.update("""
                UPDATE delivery_assignment SET state = 'DELIVERED', delivered_at = now()
                WHERE order_id = ? AND driver_id = ? AND shift_id = ? AND state = 'PICKED_UP'
                """, orderId, driverId, shift.id());
        if (updated != 1) throw new IllegalStateException("Pick up this assigned order first.");
        recordEvent(shift.id(), assignmentId(orderId), driverId, authentication.getName(), shift.branchId(), "DELIVERED");
        changed(shift.branchId(), authentication.getName(), "DELIVERED");
        return driverDashboard(authentication);
    }

    @Transactional
    public Shift returning(Authentication authentication) {
        long driverId = driverId(authentication);
        Shift shift = requiredActiveShift(driverId);
        Integer inTransit = jdbc.queryForObject("""
                SELECT count(*) FROM delivery_assignment WHERE shift_id = ? AND state IN ('ASSIGNED', 'PICKED_UP')
                """, Integer.class, shift.id());
        if (inTransit != null && inTransit > 0) throw new IllegalStateException("Complete assigned deliveries before returning.");
        int updated = jdbc.update("UPDATE driver_shift SET returning_at = now() WHERE shift_id = ? AND returning_at IS NULL",
                shift.id());
        if (updated == 1) {
            recordEvent(shift.id(), null, driverId, authentication.getName(), shift.branchId(), "RETURNING");
            changed(shift.branchId(), authentication.getName(), "RETURNING");
        }
        return openShift(driverId);
    }

    @Transactional
    public ManagerDashboard assign(Authentication authentication, long orderId, long driverId,
                                   Integer requestedBranch) {
        int branch = managerBranch(authentication, requestedBranch);
        assignOne(authentication, orderId, driverId, branch);
        return managerDashboard(authentication, branch);
    }

    @Transactional
    public ManagerDashboard assignBatch(Authentication authentication, List<Long> orderIds, long driverId,
                                        Integer requestedBranch) {
        int branch = managerBranch(authentication, requestedBranch);
        if (orderIds == null || orderIds.isEmpty() || orderIds.size() > 100
                || orderIds.stream().anyMatch(id -> id == null || id <= 0)
                || orderIds.stream().distinct().count() != orderIds.size()) {
            throw new IllegalArgumentException("Select up to 100 distinct delivery orders.");
        }
        orderIds.stream().sorted().forEach(orderId -> assignOne(authentication, orderId, driverId, branch));
        return managerDashboard(authentication, branch);
    }

    private void assignOne(Authentication authentication, long orderId, long driverId, int branch) {
        Profile profile = profile(driverId);
        if (profile.branchId() != branch || !profile.active()) throw new AccessDeniedException("Driver is not active in this branch.");
        Shift shift = requiredActiveShift(driverId);
        String orderTypeAndStatus = jdbc.queryForObject("""
                SELECT order_type || ':' || status FROM customer_order WHERE order_id = ? AND branch_id = ? FOR UPDATE
                """, String.class, orderId, branch);
        if (orderTypeAndStatus == null || !List.of("delivery:Pending", "delivery:Preparing", "delivery:Completed")
                .contains(orderTypeAndStatus)) throw new IllegalStateException("Order is not eligible for dispatch.");
        List<String> existing = jdbc.query("SELECT state FROM delivery_assignment WHERE order_id = ? FOR UPDATE",
                (rs, row) -> rs.getString(1), orderId);
        Long actorId = actorId(authentication);
        if (existing.isEmpty()) {
            jdbc.update("""
                    INSERT INTO delivery_assignment(order_id, driver_id, shift_id, branch_id, assigned_by)
                    VALUES (?, ?, ?, ?, ?)
                    """, orderId, driverId, shift.id(), branch, actorId);
        } else if ("ASSIGNED".equals(existing.get(0))) {
            jdbc.update("""
                    UPDATE delivery_assignment SET driver_id = ?, shift_id = ?, assigned_by = ?, self_claimed = false
                    WHERE order_id = ? AND branch_id = ? AND state = 'ASSIGNED'
                    """, driverId, shift.id(), actorId, orderId, branch);
        } else {
            throw new IllegalStateException("Picked up or completed deliveries cannot be reassigned.");
        }
        recordEvent(shift.id(), assignmentId(orderId), actorId, authentication.getName(), branch, "ASSIGNED");
        jdbc.update("UPDATE driver_shift SET returning_at = NULL WHERE shift_id = ?", shift.id());
        changed(branch, profile.email(), "ASSIGNED");
    }

    @Transactional
    public ManagerDashboard cancelRejected(Authentication authentication, long orderId, Integer requestedBranch) {
        int branch = managerBranch(authentication, requestedBranch);
        List<Long[]> rows = jdbc.query("""
                SELECT a.assignment_id, a.shift_id, a.driver_id FROM delivery_assignment a
                JOIN customer_order o ON o.order_id = a.order_id
                WHERE a.order_id = ? AND a.branch_id = ? AND a.state = 'ASSIGNED' AND o.status = 'Rejected'
                FOR UPDATE OF a
                """, (rs, row) -> new Long[]{rs.getLong(1), rs.getLong(2), rs.getLong(3)}, orderId, branch);
        if (rows.isEmpty()) throw new IllegalStateException("Only an uncollected rejected order can be cancelled.");
        Long[] assignment = rows.get(0);
        jdbc.update("UPDATE delivery_assignment SET state = 'CANCELLED' WHERE assignment_id = ?", assignment[0]);
        recordEvent(assignment[1], assignment[0], actorId(authentication), authentication.getName(), branch, "CANCELLED");
        changed(branch, profile(assignment[2]).email(), "CANCELLED");
        return managerDashboard(authentication, branch);
    }

    @Transactional(readOnly = true)
    public CashUpPreview preview(Authentication authentication, long shiftId, Integer branchId,
                                 LocalTime clockInTime, LocalTime clockOutTime) {
        int branch = managerBranch(authentication, branchId);
        Shift shift = shift(shiftId);
        if (shift.branchId() != branch) throw new AccessDeniedException("Shift belongs to another branch.");
        if (!"CLOCKED_OUT".equals(shift.state())) throw new IllegalStateException("Clock out before cash-up.");
        return calculate(shift, clockInTime, clockOutTime);
    }

    @Transactional
    public CashUpResult cashUp(Authentication authentication, long shiftId, Integer branchId,
                               LocalTime clockInTime, LocalTime clockOutTime,
                               BigDecimal cashHandedIn, BigDecimal cashPaidOut,
                               String varianceReason, String timeAdjustmentReason) {
        int branch = managerBranch(authentication, branchId);
        Shift initial = shift(shiftId);
        if (initial.branchId() != branch) throw new AccessDeniedException("Shift belongs to another branch.");
        lockDriver(initial.driverId());
        jdbc.queryForObject("SELECT shift_id FROM driver_shift WHERE shift_id = ? FOR UPDATE", Long.class, shiftId);
        Shift shift = shift(shiftId);
        if (shift.branchId() != branch) throw new AccessDeniedException("Shift belongs to another branch.");
        if (!"CLOCKED_OUT".equals(shift.state())) throw new IllegalStateException("Clock out before cash-up.");
        Integer unfinished = jdbc.queryForObject("""
                SELECT count(*) FROM delivery_assignment WHERE shift_id = ? AND state IN ('ASSIGNED', 'PICKED_UP')
                """, Integer.class, shiftId);
        if (unfinished != null && unfinished > 0) throw new IllegalStateException("Unfinished deliveries cannot be settled.");
        if (clockInTime == null || clockOutTime == null)
            throw new IllegalArgumentException("Select both clock-in and clock-out times.");
        CashUpPreview preview = calculate(shift, clockInTime, clockOutTime);
        requireNonnegative(cashHandedIn);
        requireNonnegative(cashPaidOut);
        if (cashHandedIn.signum() > 0 && cashPaidOut.signum() > 0)
            throw new IllegalArgumentException("Cash cannot move in both directions.");
        BigDecimal variance = money(cashHandedIn).subtract(money(cashPaidOut))
                .subtract(preview.expectedCashDue());
        String reason = varianceReason == null ? "" : varianceReason.trim();
        if (variance.signum() != 0 && reason.isEmpty()) throw new IllegalArgumentException("Explain the cash variance.");
        if (reason.length() > 1000) throw new IllegalArgumentException("Variance reason is too long.");
        String timeReason = timeAdjustmentReason == null ? "" : timeAdjustmentReason.trim();
        if (timeReason.length() > 1000) throw new IllegalArgumentException("Time adjustment reason is too long.");
        jdbc.update("""
                INSERT INTO driver_settlement(shift_id, driver_id, branch_id, manager_id, manager_username,
                    order_sales, online_order_paid, online_tips, cash_tips, hourly_pay, delivery_pay,
                    expected_cash_due, cash_handed_in, cash_paid_out, variance, variance_reason,
                    approved_clock_in_at, approved_clock_out_at, time_adjustment_reason)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, shiftId, shift.driverId(), branch, actorId(authentication), authentication.getName(),
                preview.orderSales(), preview.onlineOrderPaid(), preview.onlineTips(), preview.cashTips(),
                preview.hourlyPay(), preview.deliveryPay(), preview.expectedCashDue(), money(cashHandedIn),
                money(cashPaidOut), variance, reason.isEmpty() ? null : reason,
                preview.approvedClockInAt(), preview.approvedClockOutAt(), timeReason.isEmpty() ? null : timeReason);
        jdbc.update("UPDATE driver_shift SET approved_minutes = ?, state = 'SETTLED' WHERE shift_id = ?",
                preview.approvedMinutes(), shiftId);
        changed(branch, profile(shift.driverId()).email(), "CASHED_UP");
        return new CashUpResult(preview, money(cashHandedIn), money(cashPaidOut), variance, reason);
    }

    @Transactional(readOnly = true)
    public List<Settlement> settlements(Authentication authentication, Integer branchId) {
        int branch = managerBranch(authentication, branchId);
        return jdbc.query("""
                SELECT shift_id, manager_username, order_sales, online_order_paid, online_tips, cash_tips,
                    hourly_pay, delivery_pay, expected_cash_due, cash_handed_in, cash_paid_out,
                    variance, variance_reason, settled_at, approved_clock_in_at, approved_clock_out_at,
                    time_adjustment_reason
                FROM driver_settlement WHERE branch_id = ? ORDER BY settled_at DESC LIMIT 100
                """, this::settlementRow, branch);
    }

    private Settlement settlementRow(ResultSet rs, int row) throws SQLException {
        return new Settlement(rs.getLong("shift_id"), rs.getString("manager_username"),
                rs.getBigDecimal("order_sales"), rs.getBigDecimal("online_order_paid"),
                rs.getBigDecimal("online_tips"), rs.getBigDecimal("cash_tips"),
                rs.getBigDecimal("hourly_pay"), rs.getBigDecimal("delivery_pay"),
                rs.getBigDecimal("expected_cash_due"), rs.getBigDecimal("cash_handed_in"),
                rs.getBigDecimal("cash_paid_out"), rs.getBigDecimal("variance"),
                rs.getString("variance_reason"), rs.getObject("settled_at", OffsetDateTime.class),
                rs.getObject("approved_clock_in_at", OffsetDateTime.class),
                rs.getObject("approved_clock_out_at", OffsetDateTime.class),
                rs.getString("time_adjustment_reason"));
    }

    private CashUpPreview calculate(Shift shift, LocalTime clockInTime, LocalTime clockOutTime) {
        ApprovedShiftTime.Selection selected = ApprovedShiftTime.select(
                shift.clockedInAt(), shift.clockedOutAt(), clockInTime, clockOutTime);
        int minutes = selected.minutes();
        List<Long> deliveredOrderIds = jdbc.query("""
                SELECT order_id FROM delivery_assignment WHERE shift_id = ? AND state = 'DELIVERED'
                ORDER BY delivered_at, order_id
                """, (rs, row) -> rs.getLong(1), shift.id());
        BigDecimal sales = BigDecimal.ZERO;
        BigDecimal online = BigDecimal.ZERO;
        BigDecimal onlineTips = BigDecimal.ZERO;
        BigDecimal cashTips = BigDecimal.ZERO;
        for (Long orderId : deliveredOrderIds) {
            PersistedOrderSnapshotReader.Snapshot order = orders.findById(orderId)
                    .orElseThrow(() -> new IllegalStateException("Assigned order is not visible."));
            sales = sales.add(order.total());
            List<BigDecimal[]> payment = jdbc.query("""
                    SELECT online_order_paid, online_tip, cash_tip FROM delivery_payment WHERE order_id = ?
                    """, (rs, row) -> new BigDecimal[]{rs.getBigDecimal(1), rs.getBigDecimal(2), rs.getBigDecimal(3)}, orderId);
            if (!payment.isEmpty()) {
                online = online.add(payment.get(0)[0]);
                onlineTips = onlineTips.add(payment.get(0)[1]);
                cashTips = cashTips.add(payment.get(0)[2]);
            }
        }
        BigDecimal hourly = shift.hourlyRate().multiply(BigDecimal.valueOf(minutes))
                .divide(SIXTY, 2, RoundingMode.HALF_UP);
        BigDecimal deliveryPay = shift.perDeliveryRate().multiply(BigDecimal.valueOf(deliveredOrderIds.size()));
        BigDecimal expected = sales.subtract(online).subtract(hourly).subtract(deliveryPay).subtract(onlineTips);
        return new CashUpPreview(shift.id(), deliveredOrderIds.size(), minutes, selected.clockInTime(),
                selected.clockOutTime(), selected.approvedClockInAt(), selected.approvedClockOutAt(), money(sales), money(online),
                money(onlineTips), money(cashTips), shift.hourlyRate(), shift.perDeliveryRate(),
                money(hourly), money(deliveryPay), money(expected));
    }

    private List<Delivery> deliveries(long shiftId) {
        return jdbc.query("""
                SELECT assignment_id, order_id, driver_id, shift_id, branch_id, state, self_claimed,
                    assigned_at, picked_up_at, delivered_at
                FROM delivery_assignment WHERE shift_id = ? AND state <> 'CANCELLED' ORDER BY assigned_at, assignment_id
                """, (rs, row) -> deliveryRow(rs), shiftId);
    }

    private Delivery deliveryRow(ResultSet rs) throws SQLException {
        long orderId = rs.getLong("order_id");
        PersistedOrderSnapshotReader.Snapshot snapshot = orders.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Assigned order is not visible."));
        return new Delivery(rs.getLong("assignment_id"), snapshot.order(), snapshot.total(), rs.getLong("driver_id"),
                rs.getLong("shift_id"), rs.getInt("branch_id"), rs.getString("state"),
                rs.getBoolean("self_claimed"), rs.getObject("assigned_at", OffsetDateTime.class),
                rs.getObject("picked_up_at", OffsetDateTime.class), rs.getObject("delivered_at", OffsetDateTime.class));
    }

    private Profile profile(long driverId) {
        List<Profile> rows = jdbc.query("""
                SELECT p.driver_id, p.branch_id, p.hourly_rate, p.per_delivery_rate, p.active,
                    c.first_name, c.last_name, c.email
                FROM driver_profile p JOIN customers c ON c.id = p.driver_id WHERE p.driver_id = ?
                """, this::profileRow, driverId);
        if (rows.isEmpty()) throw new NoSuchElementException("Driver profile not found.");
        return rows.get(0);
    }

    private Profile profileRow(ResultSet rs, int row) throws SQLException {
        return new Profile(rs.getLong("driver_id"), rs.getInt("branch_id"),
                rs.getString("first_name"), rs.getString("last_name"), rs.getString("email"),
                rs.getBigDecimal("hourly_rate"), rs.getBigDecimal("per_delivery_rate"), rs.getBoolean("active"));
    }

    private Shift openShift(long driverId) {
        List<Shift> rows = jdbc.query("""
                SELECT shift_id, driver_id, branch_id, business_date, clocked_in_at, clocked_out_at,
                    approved_minutes, hourly_rate, per_delivery_rate, returning_at, state
                FROM driver_shift WHERE driver_id = ? AND state <> 'SETTLED' ORDER BY shift_id DESC LIMIT 1
                """, this::shiftRow, driverId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Shift requiredActiveShift(long driverId) {
        lockDriver(driverId);
        Shift shift = openShift(driverId);
        if (shift == null || !"ACTIVE".equals(shift.state())) throw new IllegalStateException("Clock in first.");
        return shift;
    }

    /** Serializes shift and dispatch mutations even before the driver's first shift exists. */
    private void lockDriver(long driverId) {
        // Drivers cannot UPDATE their profile under RLS, so FOR UPDATE would hide that row.
        // A transaction advisory lock covers the first clock-in without widening any RLS policy.
        jdbc.queryForList("SELECT pg_advisory_xact_lock(hashtextextended('driver-dispatch:' || CAST(? AS text), 0))",
                driverId);
    }

    private Shift shift(long shiftId) {
        List<Shift> rows = jdbc.query("""
                SELECT shift_id, driver_id, branch_id, business_date, clocked_in_at, clocked_out_at,
                    approved_minutes, hourly_rate, per_delivery_rate, returning_at, state
                FROM driver_shift WHERE shift_id = ?
                """, this::shiftRow, shiftId);
        if (rows.isEmpty()) throw new NoSuchElementException("Shift not found.");
        return rows.get(0);
    }

    private Shift shiftRow(ResultSet rs, int row) throws SQLException {
        return new Shift(rs.getLong("shift_id"), rs.getLong("driver_id"), rs.getInt("branch_id"),
                rs.getDate("business_date").toLocalDate(), rs.getObject("clocked_in_at", OffsetDateTime.class),
                rs.getObject("clocked_out_at", OffsetDateTime.class), (Integer) rs.getObject("approved_minutes"),
                rs.getBigDecimal("hourly_rate"), rs.getBigDecimal("per_delivery_rate"),
                rs.getObject("returning_at", OffsetDateTime.class), rs.getString("state"));
    }

    private Long assignmentId(long orderId) {
        return jdbc.queryForObject("SELECT assignment_id FROM delivery_assignment WHERE order_id = ?", Long.class, orderId);
    }

    private void recordEvent(long shiftId, Long assignmentId, Long actorId, String username,
                             int branch, String eventType) {
        jdbc.update("""
                INSERT INTO driver_dispatch_event(shift_id, assignment_id, actor_id, actor_username, branch_id, event_type)
                VALUES (?, ?, ?, ?, ?, ?)
                """, shiftId, assignmentId, actorId, username, branch, eventType);
    }

    private void changed(int branch, String driverUsername, String eventType) {
        events.publishEvent(new DriverDispatchChanged(branch, driverUsername, eventType));
    }

    private long driverId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)
                || principal.accessLevel() != 4 || principal.customerId() == null)
            throw new AccessDeniedException("Driver account is required.");
        return principal.customerId();
    }

    private Long actorId(Authentication authentication) {
        return authentication.getPrincipal() instanceof AccountPrincipal principal ? principal.customerId() : null;
    }

    private int managerBranch(Authentication authentication, Integer requestedBranch) {
        if (authentication == null) throw new AccessDeniedException("Manager account is required.");
        AccountAccess access = accountAccessReader.findByUsername(authentication.getName());
        if (!access.isAdmin()) throw new AccessDeniedException("Manager account is required.");
        Integer branch = access.branchId() == null ? requestedBranch : access.branchId();
        if (branch == null || !access.canAccessBranch(branch)
                || (requestedBranch != null && !requestedBranch.equals(branch)))
            throw new AccessDeniedException("This manager cannot access the requested branch.");
        return branch;
    }

    private void requireRate(BigDecimal rate) {
        if (rate == null || rate.signum() < 0 || rate.compareTo(new BigDecimal("99999999.99")) > 0
                || rate.scale() > 2) throw new IllegalArgumentException("Use a nonnegative rate with two decimal places.");
    }

    private void requireNonnegative(BigDecimal amount) {
        if (amount == null || amount.signum() < 0 || amount.scale() > 2)
            throw new IllegalArgumentException("Cash amount must be nonnegative with two decimal places.");
    }

    private BigDecimal money(BigDecimal amount) { return amount.setScale(2, RoundingMode.HALF_UP); }

    private boolean hasSqlState(Throwable failure, String state) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && state.equals(sql.getSQLState())) return true;
        }
        return false;
    }

    public record Profile(long id, int branchId, String firstName, String lastName, String email,
                          BigDecimal hourlyRate, BigDecimal perDeliveryRate, boolean active) {}
    public record Shift(long id, long driverId, int branchId, LocalDate businessDate, OffsetDateTime clockedInAt,
                        OffsetDateTime clockedOutAt, Integer approvedMinutes, BigDecimal hourlyRate,
                        BigDecimal perDeliveryRate, OffsetDateTime returningAt, String state) {}
    public record Delivery(long assignmentId, OrderResponseDTO order, BigDecimal total, long driverId, long shiftId,
                           int branchId, String state, boolean selfClaimed, OffsetDateTime assignedAt,
                           OffsetDateTime pickedUpAt, OffsetDateTime deliveredAt) {}
    public record OrderBrief(long id, String customerName, String status, String createdAt) {}
    public record DriverDashboard(Profile profile, Shift shift, List<Delivery> deliveries,
                                  List<Settlement> settlements) {}
    public record ManagerDashboard(int branchId, List<Profile> drivers, List<OrderBrief> queue,
                                   List<Shift> shifts, List<Delivery> assignments) {}
    public record CashUpPreview(long shiftId, int deliveredCount, int approvedMinutes,
                                LocalTime clockInTime, LocalTime clockOutTime,
                                OffsetDateTime approvedClockInAt, OffsetDateTime approvedClockOutAt,
                                BigDecimal orderSales,
                                BigDecimal onlineOrderPaid, BigDecimal onlineTips, BigDecimal cashTips,
                                BigDecimal hourlyRate, BigDecimal perDeliveryRate,
                                BigDecimal hourlyPay, BigDecimal deliveryPay, BigDecimal expectedCashDue) {}
    public record CashUpResult(CashUpPreview preview, BigDecimal cashHandedIn, BigDecimal cashPaidOut,
                               BigDecimal variance, String varianceReason) {}
    public record Settlement(long shiftId, String managerUsername, BigDecimal orderSales,
                             BigDecimal onlineOrderPaid, BigDecimal onlineTips, BigDecimal cashTips,
                             BigDecimal hourlyPay, BigDecimal deliveryPay, BigDecimal expectedCashDue,
                             BigDecimal cashHandedIn, BigDecimal cashPaidOut, BigDecimal variance,
                             String varianceReason, OffsetDateTime settledAt,
                             OffsetDateTime approvedClockInAt, OffsetDateTime approvedClockOutAt,
                             String timeAdjustmentReason) {}
}
