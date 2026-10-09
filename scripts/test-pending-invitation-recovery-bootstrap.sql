-- Disposable PostgreSQL test harness, not a production migration.
-- Simulates only the Supabase Auth schema/roles/JWT claim lookup required by
-- the exact checked-in original Field Photo Prep migrations.
-- No production credentials, email addresses, or external connections.
create role anon nologin;
create role authenticated nologin;
create role service_role nologin bypassrls;
create schema auth;
create table auth.users (
    id uuid primary key,
    email text not null unique,
    email_confirmed_at timestamptz
);
create function auth.uid()
returns uuid
language sql stable
as $$
    select nullif(current_setting('request.jwt.claim.sub', true), '')::uuid;
$$;
grant usage on schema auth to anon, authenticated;
grant execute on function auth.uid() to anon, authenticated;
grant usage on schema public to anon, authenticated, service_role;
