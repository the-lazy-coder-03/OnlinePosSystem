-- Drivers confirm physical collection without waiting for a kitchen status update.
-- Retain assignment ownership, shift, timestamp and transition protections.
CREATE OR REPLACE FUNCTION app_security.guard_delivery_assignment() RETURNS trigger
LANGUAGE plpgsql SET search_path = pg_catalog, pg_temp AS $$
DECLARE order_branch integer; order_type_value text; kitchen_status text;
    shift_driver bigint; shift_branch integer; shift_state text;
BEGIN
    SELECT branch_id, order_type, status INTO order_branch, order_type_value, kitchen_status
    FROM public.customer_order WHERE order_id = NEW.order_id;
    SELECT driver_id, branch_id, state INTO shift_driver, shift_branch, shift_state
    FROM public.driver_shift WHERE shift_id = NEW.shift_id;
    IF order_type_value IS DISTINCT FROM 'delivery' OR NEW.branch_id IS DISTINCT FROM order_branch
       OR NEW.driver_id IS DISTINCT FROM shift_driver OR NEW.branch_id IS DISTINCT FROM shift_branch
       OR shift_state <> 'ACTIVE' THEN
        RAISE EXCEPTION 'Assignment must match a delivery order and driver shift' USING ERRCODE = '23514';
    END IF;
    IF TG_OP = 'INSERT' AND (NEW.state <> 'ASSIGNED' OR NEW.picked_up_at IS NOT NULL
       OR NEW.delivered_at IS NOT NULL OR kitchen_status NOT IN ('Pending', 'Preparing', 'Completed')) THEN
        RAISE EXCEPTION 'Invalid new assignment' USING ERRCODE = '23514';
    END IF;
    IF TG_OP = 'UPDATE' THEN
        IF NEW.order_id IS DISTINCT FROM OLD.order_id OR NEW.branch_id IS DISTINCT FROM OLD.branch_id
           OR OLD.state IN ('DELIVERED', 'CANCELLED') OR NEW.assigned_at IS DISTINCT FROM OLD.assigned_at THEN
            RAISE EXCEPTION 'Assignment history cannot be changed' USING ERRCODE = '42501';
        END IF;
        IF (NEW.driver_id IS DISTINCT FROM OLD.driver_id OR NEW.shift_id IS DISTINCT FROM OLD.shift_id)
           AND (OLD.state <> 'ASSIGNED' OR app_security.actor_role() NOT IN ('ADMIN', 'SUPER_ADMIN')) THEN
            RAISE EXCEPTION 'Only a manager can reassign before pickup' USING ERRCODE = '42501';
        END IF;
        IF app_security.actor_role() = 'DRIVER' AND
           (NEW.driver_id IS DISTINCT FROM OLD.driver_id OR NEW.shift_id IS DISTINCT FROM OLD.shift_id
           OR NEW.self_claimed IS DISTINCT FROM OLD.self_claimed OR NEW.assigned_by IS DISTINCT FROM OLD.assigned_by
           OR (OLD.state = 'ASSIGNED' AND NEW.state NOT IN ('ASSIGNED', 'PICKED_UP'))
           OR (OLD.state = 'PICKED_UP' AND NEW.state NOT IN ('PICKED_UP', 'DELIVERED'))) THEN
            RAISE EXCEPTION 'Invalid driver assignment transition' USING ERRCODE = '42501';
        END IF;
        IF (OLD.state = 'ASSIGNED' AND NEW.state = 'PICKED_UP'
                AND (kitchen_status IS NULL OR kitchen_status NOT IN ('Pending', 'Preparing', 'Completed')
                    OR NEW.picked_up_at IS NULL
                    OR NEW.picked_up_at > now() OR NEW.picked_up_at < now() - interval '1 minute'))
           OR (OLD.state = 'PICKED_UP' AND NEW.state = 'DELIVERED'
                AND (NEW.delivered_at IS NULL OR NEW.delivered_at > now()
                    OR NEW.delivered_at < now() - interval '1 minute'))
           OR (NEW.state <> OLD.state AND NOT (
                OLD.state = 'ASSIGNED' AND NEW.state IN ('PICKED_UP', 'CANCELLED')
                OR OLD.state = 'PICKED_UP' AND NEW.state = 'DELIVERED'))
           OR (NEW.picked_up_at IS DISTINCT FROM OLD.picked_up_at AND NEW.state <> 'PICKED_UP')
           OR (NEW.delivered_at IS DISTINCT FROM OLD.delivered_at AND NEW.state <> 'DELIVERED') THEN
            RAISE EXCEPTION 'Invalid delivery transition' USING ERRCODE = '23514';
        END IF;
    END IF;
    RETURN NEW;
END $$;
