# Phase 10D — Separate Properties and Work Orders Screen State

Date: 2026-09-17

Status: **LEVEL 2 — IN PROGRESS**

Branch: `feat/phase-10d-separate-folder-screen-state-20260917`

## Problem

`MainActivity` currently reuses one mutable `visibleFolders` list for both the Properties screen and the Work Orders screen.

That coupling previously contributed to a real defect where a property/root folder could appear as a work order. Runtime guards now stop the known bad path, but one list still represents two different screen domains.

## Goal

Give Properties and Work Orders separate in-memory collections/adapters while preserving the existing Drive queries, provider identities, selection, create/reuse, and destructive safety behavior.

## Scope

Runtime:
- replace shared `visibleFolders` with separate `propertyFolders` and `workOrderFolders`;
- bind `PropertyListAdapter` only to property state;
- bind `WorkOrderListAdapter` only to work-order state;
- update screen-specific refresh/create/reuse callbacks to mutate only their owning list;
- keep existing provider-ID guards and current-work-order reconciliation.

Tests:
- update reflection-based UI tests that currently inject the old shared list;
- add focused regression coverage proving property state does not become work-order state and work-order state does not become property state.

## Read/write surfaces

Reads:
- existing Drive folder queries;
- existing selected property/work-order preferences;
- in-memory screen-specific folder collections.

Writes:
- in-memory screen list state only;
- existing Drive and persisted preference writes remain owned by existing methods.

No new persistence, schema, provider permission, Drive write, upload, retry, reconciliation, or deletion behavior is introduced.

## Protected behavior

Unchanged:
- stable provider document ID remains authoritative;
- property selection/opening behavior;
- work-order discovery and selection;
- work-order create/reuse/Clear & Reuse safety;
- current work-order persistence;
- camera/photo workflow;
- queue/upload/retry/UNCERTAIN/reconciliation behavior.

## Failure prevention

After this change:
- refreshing/opening Work Orders cannot clear or populate the Properties adapter's backing list;
- refreshing Properties cannot populate the Work Orders adapter's backing list;
- tapping a property always resolves against `propertyFolders`;
- tapping a work order always resolves against `workOrderFolders`;
- the existing self-child guard remains as defense in depth even though screen state is no longer shared.

## Verification

Required:
- focused unit/instrumentation coverage for state separation;
- existing Home/Work Orders navigation tests updated;
- full Android CI on the exact final runtime head;
- focused Samsung smoke only if the runtime integration changes visible behavior beyond state isolation.

No Drive destructive gate is required solely for this Level 2 state-ownership repair.
