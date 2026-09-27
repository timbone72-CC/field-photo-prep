# Phase 12K Build State

Date: 2026-09-26

Status: **PRE-IMPLEMENTATION — LEVEL 3 AUTHORITATIVE LINE ESTABLISHED**

## Governed base

- main/rollback: `c502260b7207d75a1349581a2f72b397e7fa5e70`;
- Phase 12J merged through PR #83.

## Authoritative line

- branch: `phase-12k/production-identity-release-path`;
- PR: pending creation immediately after this documentation checkpoint;
- competing 12K branch/PR at takeover: none;
- unrelated Draft PR #39 remains outside 12K.

## Classification

**Level 3 — deployment/signing/runtime trust**

Explicit operator approval is required before merge.

## Verified preflight facts

- production application ID: `com.inandout.fieldphotoprep`;
- internal application ID: `com.inandout.fieldphotoprep.internal`;
- production callback: `com.inandout.fieldphotoprep://auth-callback`;
- internal callback: `com.inandout.fieldphotoprep.internal://auth-callback`;
- governed-base versionCode: `36`;
- internal stable test signer is intentionally non-production and remains unchanged;
- git ignores ordinary `*.jks` / `*.keystore`;
- no production signer/workflow currently exists;
- live dedicated FPP Supabase project URL and active modern publishable key match Android source;
- Supabase Auth redirect allowlist is not readable through the connected management API and remains a reality gate.

## Planned candidate identity

- production package: `com.inandout.fieldphotoprep`;
- versionCode: `37`;
- production callback: `com.inandout.fieldphotoprep://auth-callback`;
- dedicated Supabase project ref: `vtyiktvqhbgabawotkrj`;
- public release decision: **not part of 12K**.

## Exact next checkpoint

Implement the source-controlled release tooling only:
1. external-secret-backed release signing config that fails closed;
2. production identity/config verification task/script;
3. manual-only signed release-candidate workflow;
4. release artifact verification/provenance script;
5. controlled release runbook;
6. focused tests/static checks.

Do not generate, commit, or invent a production private key.

After source tooling is green, stop at the real-signer structural gate if no secured production signer is yet available.

## External-state status

- Supabase project URL/key: **VERIFIED**;
- production redirect allowlist: **UNVERIFIED — reality gate**;
- production signing key: **NOT PRESENT IN REPO BY DESIGN**;
- GitHub production signing secrets: **NOT ASSUMED/NOT VERIFIED**;
- production-suitable external SMTP: **DEFERRED until outside-user/public distribution**.

## Merge approval

**PENDING — DO NOT MERGE**
