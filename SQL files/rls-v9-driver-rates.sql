-- Rate changes apply to unsettled shifts; completed payroll remains immutable.
ALTER TABLE public.driver_dispatch_event ADD COLUMN details jsonb;

CREATE OR REPLACE FUNCTION app_security.guard_driver_shift() RETURNS trigger
LANGUAGE plpgsql SET search_path = pg_catalog, pg_temp AS $$
DECLARE profile_branch integer; profile_hourly numeric(10,2); profile_delivery numeric(10,2); profile_active boolean;
BEGIN
    IF TG_OP = 'INSERT' THEN
        SELECT branch_id, hourly_rate, per_delivery_rate, active
        INTO profile_branch, profile_hourly, profile_delivery, profile_active
        FROM public.driver_profile WHERE driver_id = NEW.driver_id;
        IF NOT profile_active OR NEW.branch_id IS DISTINCT FROM profile_branch
           OR NEW.hourly_rate IS DISTINCT FROM profile_hourly
           OR NEW.per_delivery_rate IS DISTINCT FROM profile_delivery
           OR NEW.business_date IS DISTINCT FROM (now() AT TIME ZONE 'Africa/Johannesburg')::date
           OR NEW.state <> 'ACTIVE' OR NEW.clocked_out_at IS NOT NULL
           OR NEW.approved_minutes IS NOT NULL OR NEW.returning_at IS NOT NULL
           OR NEW.clocked_in_at > now() OR NEW.clocked_in_at < now() - interval '1 minute' THEN
            RAISE EXCEPTION 'Invalid driver shift clock-in' USING ERRCODE = '23514';
        END IF;
        RETURN NEW;
    END IF;
    IF NEW.shift_id IS DISTINCT FROM OLD.shift_id OR NEW.driver_id IS DISTINCT FROM OLD.driver_id
       OR NEW.branch_id IS DISTINCT FROM OLD.branch_id OR NEW.business_date IS DISTINCT FROM OLD.business_date
       OR NEW.clocked_in_at IS DISTINCT FROM OLD.clocked_in_at OR OLD.state = 'SETTLED' THEN
        RAISE EXCEPTION 'Shift history cannot be changed' USING ERRCODE = '42501';
    END IF;
    IF NEW.hourly_rate IS DISTINCT FROM OLD.hourly_rate
       OR NEW.per_delivery_rate IS DISTINCT FROM OLD.per_delivery_rate THEN
        SELECT branch_id, hourly_rate, per_delivery_rate
        INTO profile_branch, profile_hourly, profile_delivery
        FROM public.driver_profile WHERE driver_id = NEW.driver_id;
        IF NOT (app_security.actor_role() = 'SUPER_ADMIN'
                OR (app_security.actor_role() = 'ADMIN' AND OLD.branch_id = app_security.actor_branch_id()))
           OR OLD.state NOT IN ('ACTIVE', 'CLOCKED_OUT') OR NEW.state IS DISTINCT FROM OLD.state
           OR NEW.clocked_out_at IS DISTINCT FROM OLD.clocked_out_at
           OR NEW.approved_minutes IS DISTINCT FROM OLD.approved_minutes
           OR NEW.returning_at IS DISTINCT FROM OLD.returning_at
           OR NEW.branch_id IS DISTINCT FROM profile_branch
           OR NEW.hourly_rate IS DISTINCT FROM profile_hourly
           OR NEW.per_delivery_rate IS DISTINCT FROM profile_delivery THEN
            RAISE EXCEPTION 'Only a branch manager can apply saved rates to an unsettled shift' USING ERRCODE = '42501';
        END IF;
    END IF;
    IF app_security.actor_role() = 'DRIVER' THEN
        IF NEW.approved_minutes IS DISTINCT FROM OLD.approved_minutes OR NEW.state = 'SETTLED'
           OR (OLD.state = 'CLOCKED_OUT' AND NEW.state <> OLD.state)
           OR (OLD.state = 'ACTIVE' AND NEW.state NOT IN ('ACTIVE', 'CLOCKED_OUT'))
           OR (NEW.state = 'ACTIVE' AND NEW.clocked_out_at IS NOT NULL)
           OR (NEW.state = 'CLOCKED_OUT' AND (OLD.state <> 'ACTIVE' OR NEW.clocked_out_at IS NULL))
           OR (NEW.clocked_out_at IS DISTINCT FROM OLD.clocked_out_at
               AND (OLD.clocked_out_at IS NOT NULL OR NEW.clocked_out_at > now()
                    OR NEW.clocked_out_at < now() - interval '1 minute'))
           OR (NEW.returning_at IS DISTINCT FROM OLD.returning_at AND NEW.returning_at IS NOT NULL
               AND (NEW.returning_at > now() OR NEW.returning_at < now() - interval '1 minute')) THEN
            RAISE EXCEPTION 'Invalid driver shift transition' USING ERRCODE = '42501';
        END IF;
    END IF;
    IF NEW.state = 'SETTLED' AND (OLD.state <> 'CLOCKED_OUT'
       OR NEW.approved_minutes IS NULL OR NOT EXISTS (
           SELECT 1 FROM public.driver_settlement WHERE shift_id = NEW.shift_id)) THEN
        RAISE EXCEPTION 'Shift requires a completed settlement' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END $$;
