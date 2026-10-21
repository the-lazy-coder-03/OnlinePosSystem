# Driver dispatch and cash-up

## Access and workflow

- A customer registers normally. A manager opens **Admin > Dispatch**, selects their branch if they are a super admin, and promotes the customer's registered email. Branch managers can only promote to `DRIVER` for their own branch. They set hourly and per-delivery rates there.
- Drivers sign in at `/login` and are sent to `/driver`. Clock-in creates one active shift for the Africa/Johannesburg business date and copies the current rates onto that shift. Saving rates in Dispatch also updates the driver's active or clocked-out, uncashed shift. A new shift cannot start while the previous shift is awaiting cash-up.
- Managers can assign individual uncollected deliveries or select up to 100 queue orders for an atomic batch assignment to an active driver. Drivers may self-claim one unassigned order at a time from their branch's FIFO queue. The assignment is separate from kitchen status; pickup is available only after the kitchen marks the order `Completed`.
- After marking all assigned deliveries delivered, the driver can signal **Returning**. This state is saved and broadcast to the branch dashboard; reconnecting reloads the saved state. A rejected order assigned before pickup can be cancelled by the manager, preserving a dispatch event.
- The driver clocks out after all assignments are delivered or cancelled. At cash-up the manager selects clock-in and clock-out times in SAST; the server calculates payable minutes and pay automatically. The original clock events remain unchanged, and the selected times plus an optional adjustment reason are retained in settlement history. A nonzero cash variance requires a reason. Closed settlements remain read-only history.

## Accounting

Cash-up includes every delivered order assigned to the shift. Order sales are calculated from persisted order-line prices using the ordering module's existing total calculator, not browser totals. Hourly pay is `server-calculated payable minutes / 60 * shift hourly rate`, rounded to cents; delivery pay is `delivered count * shift per-delivery rate`. Time-only selections use the recorded clock-in date; a clock-out time earlier than clock-in means the next day, while equal times mean zero minutes. Future clock-out and shifts over 24 hours are rejected.

`expected cash due to store = order sales - verified online order payments - hourly pay - delivery pay - online tips`. A negative result is cash payable by the store to the driver. Actual net cash is handed-in cash minus paid-out cash; variance is actual net cash minus expected cash due. Cash tips belong to the driver and are reported separately from order sales.

For example, R1,000 cash sales, four approved hours at R15/hour, and three deliveries at R25/delivery produce R60 hourly pay plus R75 delivery pay, leaving R865 due to the store. Cash-up shows the saved rates and each calculation. Saving rates or receiving a live dispatch update refreshes an open preview without resetting approved times, cash entries, or adjustment reasons.

Rate saving, clock-in, and settlement share a per-driver transaction lock. Rate changes to an unsettled shift record `SHIFT_RATES_CHANGED` in `driver_dispatch_event`, including the manager, timestamp, and old/new rates in nullable `details` JSON. Saving unchanged rates does not create a duplicate rate-change event. Settled shifts and their cash-ups keep their original rates and amounts.

The current application has no payment provider or customer tipping flow. Every order is treated as cash, and online payment and tip values remain zero. The `delivery_payment` table is reserved for future provider-verified entries and has no runtime write grant. When payment integration is added, each order must have one payment method, verified online order payments must be capped at that order's saved total, and any excess must be recorded as a separate verified online tip. Never use browser-submitted payment or coordinate values for settlement.

## Security and deployment

Driver migrations through `rls-v9-driver-rates.sql` are immutable and run through the existing owner migration process before runtime startup. Version 8 is the separate pickup change; version 9 adds rate-change event details and permits branch managers to apply saved profile rates to unsettled shifts. Drivers cannot change rates, and settled shifts remain immutable. The runtime role must not own protected tables. Forced RLS limits drivers to their own shifts and assigned delivery orders, while manager reads/writes are branch-scoped. The reviewed policy/function contract in `SupportConfigFiles/rls-contract.json` is checked at application startup.

The cash-up preview API adds `hourlyRate` and `perDeliveryRate`; existing request shapes are unchanged. Previewing a settled shift is rejected; read historical amounts through settlement history.

Deployment does not automatically reprice existing shifts. After an authorized deployment, re-save the affected driver's current rates in Dispatch to synchronize an old unsettled shift, including one whose profile already contains the correct rates. On the reviewed live shift #1, re-save R15/hour and R25/delivery, then verify R75 delivery pay and the pay for approved hours before settling. No production data repair or deployment was performed as part of local implementation.

Actions use authenticated REST with CSRF protection. WebSocket messages are server-published notifications only; clients cannot send state changes over STOMP. No payment provider, production deployment, or credential changes are part of this feature.

## Rate correction verification (2026-10-21)

- `DriverCashUpCalculationTest` covers the R865 example, minute rounding, zero rates, and a store payout.
- `RlsPostgresIT.savingRatesRepairsUncashedShiftsAndPreservesSettledPayroll` reproduces a zero-rate shift with three deliveries, repairs it by saving the already-correct profile rates, verifies audit details and authorization, preserves settled history and clock events, and races a rate save against settlement.
- Browser coverage in `driver-cash-up-rates.spec.cjs` checks both deductions, live refresh, stale responses, preview failures, and preservation of manager-entered cash/time/reasons. The full driver workflow saves rates after clock-in to exercise the real API path.
- Focused PostgreSQL, shift-time, calculation, and migration checks passed: 37 tests, no failures or skips. JavaScript/shell syntax and template script checks passed.
