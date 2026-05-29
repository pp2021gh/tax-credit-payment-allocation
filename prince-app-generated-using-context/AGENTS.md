# AGENTS.md

This file provides guidance to agents when working with code in this repository.

## Project Overview
Single-file JSON-LD ontology for software requirements management. No build system, tests, or linters - just `software-requirements-ontology.jsonld` and a documentation template.

## Non-Obvious Ontology Rules

### Identity Pattern (Counterintuitive)
- Entities use UUID for `identityKey` (e.g., `requirementId: UUID`) but humans reference via code fields (e.g., `requirementCode`)
- This dual-identity pattern is intentional: UUIDs for system integrity, codes for human communication

### State Machine Constraints
- Requirements follow strict state flow: `RequirementDraft` (initial) → `RequirementVerified` or `RequirementRejected` (terminal)
- Operations have state preconditions: `RealizedBy` requires APPROVED/IMPLEMENTED, `VerifiedBy` requires IMPLEMENTED
- No backward transitions allowed once in terminal state

### Mandatory Relationships (Hidden Requirements)
- Requirements MUST have ≥1 stakeholder source (invariant enforced)
- Goals MUST be refined by ≥1 requirement (invariant enforced)
- TestCases MUST verify ≥1 requirement (invariant enforced)
- `DependsOn` operation explicitly forbids circular dependencies

### Strict Enumerations
- Priority: MUST|SHOULD|COULD|WONT (MoSCoW only)
- NonFunctionalRequirement.qualityAttribute: PERFORMANCE|SECURITY|USABILITY|RELIABILITY|MAINTAINABILITY
- TestCase.testType: UNIT|INTEGRATION|SYSTEM|ACCEPTANCE

### JSON-LD Structure
- Custom namespace `sro:` (http://example.org/sro#) for all domain terms
- Relationship properties (`hasState`, `relatesTo`, `emitsEvent`, `from`, `to`) are `@id` typed (not strings)
- All definitions live in `@graph` array (not at root level)

## Editing Rules
- Maintain invariant consistency when adding/modifying entities
- Respect state machine flow (no invalid transitions)
- Keep operation preconditions/postconditions logically sound
- All `identityKey` fields must be UUID type
- All `humanRef` fields must reference user-friendly code/name fields