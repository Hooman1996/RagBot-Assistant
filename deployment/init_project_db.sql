-- Creates the project's own database, schemas and roles on the shared dev PostgreSQL (plan §12).
-- Run once, as a database admin, only after the owner approves it. See deployment/README.md.
-- It names no RagBot object and changes nothing outside ragbot_assistant_dev.
\set ON_ERROR_STOP on

CREATE ROLE assistant_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
CREATE ROLE mock_bank_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;

CREATE DATABASE ragbot_assistant_dev;
REVOKE ALL ON DATABASE ragbot_assistant_dev FROM PUBLIC;
GRANT CONNECT ON DATABASE ragbot_assistant_dev TO assistant_app, mock_bank_app;

\connect ragbot_assistant_dev

-- Stop before any change if the connection landed anywhere else.
DO $$
BEGIN
    IF current_database() <> 'ragbot_assistant_dev' THEN
        RAISE EXCEPTION 'expected ragbot_assistant_dev, connected to %', current_database();
    END IF;
END
$$;

-- Each role owns only its own schema and has no access to the other one.
REVOKE ALL ON SCHEMA public FROM PUBLIC;
CREATE SCHEMA assistant AUTHORIZATION assistant_app;
CREATE SCHEMA mock_bank AUTHORIZATION mock_bank_app;
