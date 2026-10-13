-- Cluster-wide role, created once; safe to re-run across tenant schemas.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'member_app_role') THEN
        CREATE ROLE member_app_role NOLOGIN NOSUPERUSER NOBYPASSRLS;
    END IF;
EXCEPTION WHEN duplicate_object THEN
    NULL;  -- lost a race with a concurrent tenant provisioning
END
$$;

GRANT member_app_role TO CURRENT_USER;
GRANT USAGE ON SCHEMA "${flyway:defaultSchema}" TO member_app_role;

-- Parses a DE-Role like DE_LAKER_MBR / DE_LAKER_ALL. The tenant segment is
-- NOT validated - X-Tenant-Id/schema routing already isolates by tenant;
-- this role only ever controls the carrier_hq_code prefix filter within
-- whichever schema the caller is already connected to.
CREATE OR REPLACE PROCEDURE set_user_context(p_role TEXT)
LANGUAGE plpgsql
AS $$
DECLARE
    v_role   TEXT := upper(btrim(p_role));
    v_suffix TEXT;
BEGIN
    IF v_role IS NULL OR v_role = '' THEN
        RAISE EXCEPTION 'DE role must not be blank' USING ERRCODE = '22023';
    END IF;

    v_suffix := substring(v_role from '_([^_]+)$');
    IF v_suffix IS NULL THEN
        RAISE EXCEPTION 'DE role % is not in the expected ..._<PREFIX> or ..._ALL format', v_role
            USING ERRCODE = '22023';
    END IF;

    PERFORM set_config('app.de_role', v_role, true);
    PERFORM set_config('app.de_role_prefix',
                        CASE WHEN v_suffix = 'ALL' THEN '' ELSE v_suffix END,
                        true);

    SET LOCAL ROLE member_app_role;
END;
$$;

ALTER TABLE member ENABLE ROW LEVEL SECURITY;

CREATE POLICY member_carrier_hq_isolation ON member
    FOR SELECT TO member_app_role
    USING (
        current_setting('app.de_role_prefix', true) = ''
        OR carrier_hq_code ILIKE current_setting('app.de_role_prefix', true) || '%'
    );

GRANT SELECT ON member TO member_app_role;

CREATE VIEW member_view WITH (security_invoker = true) AS
SELECT member_id, member_number, first_name, last_name, date_of_birth,
       email, carrier_hq_code, insurance_number, status
FROM member;

GRANT SELECT ON member_view TO member_app_role;