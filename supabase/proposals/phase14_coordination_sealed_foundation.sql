-- Phase 14 original FPP two-phone coordination schema — STAGED, NOT DEPLOYED.
-- Never apply this file to Field Work Hub or deploy before the real Android SAF
-- same-folder binding and delayed-delete fencing gates pass.
-- This migration is deliberately SEALED: no public/authenticated RPC grants or
-- app-accessible schema/table grants. Enabling client RPCs is a later governed step.
-- Target original FPP project vtyiktvqhbgabawotkrj only.

create table private.fpp_coord_jobs (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null
        references public.fpp_organizations(id) on delete restrict,
    cycle_id uuid not null default gen_random_uuid(),
    generation bigint not null default 1 check (generation > 0),
    mode text not null default 'ONE' check (mode in ('ONE', 'TWO')),
    phase text not null default 'OPEN'
        check (phase in ('OPEN', 'QUIESCING', 'CLEARING', 'FINISHED', 'RECOVERY_BLOCKED')),
    next_sequence bigint not null default 1 check (next_sequence > 0),
    lead_device_id uuid null,
    preferred_first_device_id uuid null,
    clear_operation_id uuid null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint fpp_coord_clear_phase_operation_consistency
        check ((phase = 'CLEARING' and clear_operation_id is not null)
            or (phase <> 'CLEARING'))
);

create index fpp_coord_jobs_organization_idx
    on private.fpp_coord_jobs(organization_id);

create table private.fpp_coord_participants (
    job_id uuid not null
        references private.fpp_coord_jobs(id) on delete restrict,
    device_id uuid not null,
    user_id uuid not null references auth.users(id) on delete restrict,
    acknowledged_generation bigint null check (acknowledged_generation > 0),
    registered_at timestamptz not null default now(),
    primary key(job_id, device_id)
);

create table private.fpp_coord_photos (
    job_id uuid not null
        references private.fpp_coord_jobs(id) on delete restrict,
    generation bigint not null check (generation > 0),
    cycle_id uuid not null,
    photo_id uuid not null,
    device_id uuid not null,
    sequence bigint not null check (sequence > 0),
    state text not null check (state in ('IN_FLIGHT', 'CONFIRMED', 'UNCERTAIN')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    primary key (job_id, photo_id),
    unique (job_id, generation, sequence),
    foreign key (job_id, device_id)
        references private.fpp_coord_participants(job_id, device_id) on delete restrict
);

create index fpp_coord_photos_cycle_idx
    on private.fpp_coord_photos(job_id, generation, state);

alter table private.fpp_coord_jobs enable row level security;
alter table private.fpp_coord_participants enable row level security;
alter table private.fpp_coord_photos enable row level security;

-- Keep all rows inaccessible to Android JWT roles; future security-definer
-- RPCs must check (auth.uid(), current Organization active Membership, enrolled
-- device) before every effect. No RPC exposure is approved in this migration.
revoke all on table private.fpp_coord_jobs from public, anon, authenticated;
revoke all on table private.fpp_coord_participants from public, anon, authenticated;
revoke all on table private.fpp_coord_photos from public, anon, authenticated;

-- This helper is internal-only. It serializes one job and checks the ORIGINAL
-- FPP membership. The device ID is still a claimed identity here; the future
-- pairing protocol must prove installation possession before exposure.
create function private.fpp_coord_lock_member_job(
    p_job_id uuid,
    p_device_id uuid
)
returns private.fpp_coord_jobs
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_job private.fpp_coord_jobs;
    v_user uuid := (select auth.uid());
begin
    if v_user is null then
        raise exception 'AUTH_REQUIRED' using errcode='42501';
    end if;

    select *
      into v_job
    from private.fpp_coord_jobs
    where id = p_job_id
    for update;

    if not found or not exists (
        select 1 from public.fpp_organizations o
        where o.id = v_job.organization_id and o.status = 'ACTIVE'
    ) then
        raise exception 'JOB_UNAVAILABLE' using errcode='42501';
    end if;

    if not exists (
        select 1 from public.fpp_memberships m
        where m.organization_id = v_job.organization_id
          and m.user_id = v_user and m.status = 'ACTIVE'
    ) then
        raise exception 'MEMBERSHIP_REQUIRED' using errcode='42501';
    end if;

    if not exists (
        select 1 from private.fpp_coord_participants p
        where p.job_id = p_job_id
          and p.device_id = p_device_id and p.user_id = v_user
    ) then
        raise exception 'ENROLLED_DEVICE_REQUIRED' using errcode='42501';
    end if;

    return v_job;
end;
$$;

revoke all on function private.fpp_coord_lock_member_job(uuid,uuid)
    from public, anon, authenticated;

-- Transactional reference RPC. Not GRANTED / not available to Android until
-- device proof and fenced-drive-operation recovery gates pass.
create function public.fpp_coord_reserve_two_phone_upload(
    p_job_id uuid, p_device_id uuid, p_generation bigint, p_photo_id uuid
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_job private.fpp_coord_jobs;
    v_existing private.fpp_coord_photos;
    v_number bigint;
begin
    v_job := private.fpp_coord_lock_member_job(p_job_id, p_device_id);

    if p_generation <> v_job.generation or v_job.phase <> 'OPEN'
       or v_job.mode <> 'TWO' then
        raise exception 'STALE_OR_BLOCKED_CYCLE' using errcode='P0001';
    end if;

    select * into v_existing
    from private.fpp_coord_photos
    where job_id = p_job_id and photo_id = p_photo_id;

    if found then
        -- Stable photo UUID never authorizes duplicate remote create,
        -- including after crash, old generation, confirmed or uncertainty.
        return pg_catalog.jsonb_build_object(
            'outcome', 'RECONCILE_EXISTING', 'generation', v_existing.generation,
            'sequence', v_existing.sequence, 'state', v_existing.state);
    end if;

    if v_job.preferred_first_device_id is not null
       and v_job.lead_device_id is null
       and v_job.preferred_first_device_id <> p_device_id then
        raise exception 'PREFERRED_FIRST_UPLOADER_WAITING' using errcode='P0001';
    end if;

    if v_job.next_sequence > 2147483647 then
        raise exception 'SEQUENCE_EXHAUSTED' using errcode='P0001';
    end if;

    v_number := v_job.next_sequence;
    update private.fpp_coord_jobs
    set next_sequence = v_number + 1, updated_at = now()
    where id = p_job_id;

    insert into private.fpp_coord_photos (
        job_id, generation, cycle_id, photo_id, device_id, sequence, state
    ) values (
        p_job_id, v_job.generation, v_job.cycle_id, p_photo_id,
        p_device_id, v_number, 'IN_FLIGHT'
    );

    return pg_catalog.jsonb_build_object(
        'outcome', 'RESERVED', 'generation', v_job.generation,
        'cycle_id', v_job.cycle_id, 'sequence', v_number);
end;
$$;

create function public.fpp_coord_confirm_two_phone_upload(
    p_job_id uuid, p_device_id uuid, p_generation bigint, p_photo_id uuid
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_job private.fpp_coord_jobs;
    v_photo private.fpp_coord_photos;
begin
    v_job := private.fpp_coord_lock_member_job(p_job_id, p_device_id);
    if v_job.generation <> p_generation
       or v_job.phase not in ('OPEN','QUIESCING') then
        raise exception 'STALE_OR_BLOCKED_CYCLE' using errcode='P0001';
    end if;

    select * into v_photo
    from private.fpp_coord_photos
    where job_id = p_job_id and photo_id = p_photo_id
      and generation = p_generation and device_id = p_device_id
    for update;

    if not found or v_photo.state not in ('IN_FLIGHT','CONFIRMED') then
        raise exception 'UPLOAD_NOT_CONFIRMABLE' using errcode='P0001';
    end if;

    update private.fpp_coord_photos
    set state = 'CONFIRMED', updated_at = now()
    where job_id = p_job_id and photo_id = p_photo_id;

    update private.fpp_coord_jobs
    set lead_device_id = coalesce(lead_device_id, p_device_id),
        updated_at = now()
    where id = p_job_id;

    return pg_catalog.jsonb_build_object(
        'outcome', 'CONFIRMED', 'sequence', v_photo.sequence,
        'lead_device_id', coalesce(v_job.lead_device_id,p_device_id));
end;
$$;

create function public.fpp_coord_mark_upload_uncertain(
    p_job_id uuid, p_device_id uuid, p_generation bigint, p_photo_id uuid
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_job private.fpp_coord_jobs;
begin
    v_job := private.fpp_coord_lock_member_job(p_job_id, p_device_id);
    if v_job.generation <> p_generation then
        raise exception 'STALE_CYCLE' using errcode='P0001';
    end if;

    update private.fpp_coord_photos
    set state = 'UNCERTAIN', updated_at = now()
    where job_id = p_job_id and photo_id = p_photo_id
      and device_id = p_device_id and generation = p_generation
      and state = 'IN_FLIGHT';

    if not found then
        raise exception 'UPLOAD_NOT_IN_FLIGHT' using errcode='P0001';
    end if;

    update private.fpp_coord_jobs
    set phase = 'RECOVERY_BLOCKED', updated_at = now()
    where id = p_job_id;

    return pg_catalog.jsonb_build_object('outcome','RECOVERY_BLOCKED');
end;
$$;

-- INTENTIONALLY OMITTED: enrollment, mode changes, quiesce, destructive
-- lease and completion RPCs, and recovery. Their contract cannot safely
-- be exposed before the two-device SAF link proof and late-write fencing.
-- Client RPC grants also intentionally OMITTED for all functions above.
revoke all on function public.fpp_coord_reserve_two_phone_upload(uuid,uuid,bigint,uuid)
    from public, anon, authenticated;
revoke all on function public.fpp_coord_confirm_two_phone_upload(uuid,uuid,bigint,uuid)
    from public, anon, authenticated;
revoke all on function public.fpp_coord_mark_upload_uncertain(uuid,uuid,bigint,uuid)
    from public, anon, authenticated;
