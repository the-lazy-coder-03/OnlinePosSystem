-- Preserve manager-approved times without changing the driver's recorded clock events.
ALTER TABLE public.driver_settlement
    ADD COLUMN approved_clock_in_at timestamptz,
    ADD COLUMN approved_clock_out_at timestamptz,
    ADD COLUMN time_adjustment_reason text,
    ADD CONSTRAINT driver_settlement_approved_time_check CHECK (
        (approved_clock_in_at IS NULL AND approved_clock_out_at IS NULL)
        OR (approved_clock_in_at IS NOT NULL AND approved_clock_out_at IS NOT NULL
            AND approved_clock_out_at >= approved_clock_in_at
            AND approved_clock_out_at <= approved_clock_in_at + interval '24 hours')
    );
