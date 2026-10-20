# Driver dispatch and cash-up

## Access and workflow

- A customer registers normally. A manager opens **Admin > Dispatch**, selects their branch if they are a super admin, and promotes the customer's registered email. Branch managers can only promote to `DRIVER` for their own branch. They set hourly and per-delivery rates there.
- Drivers sign in at `/login` and are sent to `/driver`. Clock-in creates one active shift for the Africa/Johannesburg business date and freezes the current rates on that shift. A new shift cannot start while the previous shift is awaiting cash-up.
- Managers can assign individual uncollected deliveries or select up to 100 queue orders for an atomic batch assignment to an active driver. Drivers may self-claim one unassigned order at a time from their branch's FIFO queue. The assignment is separate from kitchen status; pickup is available only after the kitchen marks the order `Completed`.
- After marking all assigned deliveries delivered, the driver can signal **Returning**. This state is saved and broadcast to the branch dashboard; reconnecting reloads the saved state. A rejected order assigned before pickup can be cancelled by the manager, preserving a dispatch event.
- The driver clocks out after all assignments are delivered or cancelled. At cash-up the manager selects clock-in and clock-out times in SAST; the server calculates payable minutes and pay automatically. The original clock events remain unchanged, and the selected times plus an optional adjustment reason are retained in settlement history. A nonzero cash variance requires a reason. Closed settlements remain read-only history.

## Accounting

Cash-up includes every delivered order assigned to the shift. Order sales are calculated from persisted order-line prices using the ordering module's existing total calculator, not browser totals. Hourly pay is `server-calculated payable minutes / 60 * shift hourly rate`, rounded to cents; delivery pay is `delivered count * shift per-delivery rate`. Time-only selections use the recorded clock-in date; a clock-out time earlier than clock-in means the next day, while equal times mean zero minutes. Future clock-out and shifts over 24 hours are rejected.

`expected cash due to store = order sales - verified online order payments - hourly pay - delivery pay - online tips`. A negative result is cash payable by the store to the driver. Actual net cash is handed-in cash minus paid-out cash; variance is actual net cash minus expected cash due. Cash tips belong to the driver and are reported separately from order sales.

The current application has no payment provider or customer tipping flow. Every order is treated as cash, and online payment and tip values remain zero. The `delivery_payment` table is reserved for future provider-verified entries and has no runtime write grant. When payment integration is added, each order must have one payment method, verified online order payments must be capped at that order's saved total, and any excess must be recorded as a separate verified online tip. Never use browser-submitted payment or coordinate values for settlement.

## Security and deployment

`rls-v6-drivers.sql` and `rls-v7-driver-times.sql` are immutable migrations. Run them through the existing owner migration process before starting the runtime app. The runtime role must not own protected tables. Forced RLS limits drivers to their own shifts and assigned delivery orders, while manager reads/writes are branch-scoped. The reviewed policy/function contract in `SupportConfigFiles/rls-contract.json` is checked at application startup.

Actions use authenticated REST with CSRF protection. WebSocket messages are server-published notifications only; clients cannot send state changes over STOMP. No payment provider, production deployment, or credential changes are part of this feature.
