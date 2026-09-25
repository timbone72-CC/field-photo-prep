# Governance Rule-Routing Refactor — 2026-09-25

## Change class

Level 1 documentation/governance restructuring.

No Android runtime, Supabase state, Google Drive data, CI behavior, signing, deployment, or application behavior is changed by this branch.

## Problem

The repository has accumulated strong safety rules across several long documents. Many rules are repeated across product, testing, integration, staging, and agent instructions. Important control rules can become hard to locate, and phase-specific historical text can appear alongside permanent operating rules.

The Phase 12F split demonstrated a specific gap: the contracts controlled implementation safety well but did not make takeover/continuation ownership strong enough to prevent a second overlapping implementation line.

## Approved direction

Preserve detailed rules while introducing a routing/control layer:

1. a short universal governance file;
2. an FPP-specific project profile;
3. a rule index that maps work surfaces to detailed rule packs;
4. a short `AGENTS.md` entry point that requires classification/routing;
5. scope headers on the existing detailed contracts so they explain when they apply.

## New governance rules

The refactor adds these permanent controls:
- one authoritative implementation line per same/overlapping scope;
- mandatory takeover preflight before creating a branch;
- continue rather than restart active work unless explicitly superseding it;
- live external state must remain represented/reconciled with the authoritative branch;
- phase/scope closeout includes roadmap/status/handoff reconciliation;
- work should be done in the largest coherent batch that can be safely understood, rolled back, and verified;
- app/workflow consistency is protected behavior;
- scope expansion requires rule-pack/risk reclassification.

## Protected behavior

This refactor must not:
- weaken any existing product safety rule;
- change photo, Drive, queue, camera, identity, authorization, or deletion behavior;
- change any current Phase 12 implementation decision;
- alter current open Phase 12F runtime/backend work;
- delete detailed edge-case rules merely to shorten documents.

## Rollback

Rollback to `main` at:
`31bfaaffa012cadf2da0c9c31c6da64967d9d24f`

Branch:
`docs/governance-rule-routing`

## Verification

Required:
- inspect every documentation diff;
- confirm new files route to existing authoritative contracts rather than contradict them;
- confirm no runtime/source/configuration file changes;
- confirm no detailed rules were removed in this first pass;
- confirm the new takeover/external-state/closeout rules are explicit.

No runtime CI or device gate is required for this documentation-only change.

## Follow-up, deliberately separate

After this routing layer is proven and merged, a second documentation-only cleanup may deduplicate detailed contracts by moving feature-specific regression material into smaller rule packs.

That later cleanup must preserve meaning and will not be combined with this first routing change.
