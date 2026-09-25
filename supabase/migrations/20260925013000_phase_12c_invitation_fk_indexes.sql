create index fpp_invitations_auth_user_idx
    on public.fpp_invitations (auth_user_id)
    where auth_user_id is not null;

create index fpp_invitations_invited_by_user_idx
    on public.fpp_invitations (invited_by_user_id);
