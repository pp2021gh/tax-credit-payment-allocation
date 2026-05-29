
***

 

## ✅ GENERIC TEMPLATE 

 

# FUNCTIONAL & TECHNICAL ANALYSIS TEMPLATE (GENERIC)

 

## Document metadata

 

*   **Product / System:** `<ProductName>`
*   **Program / Release:** `<Program>` — `<Release>` (e.g., MVP0 / Sprint X / Version Y)
*   **Author:** `<Name>`
*   **Contributors:** `<Names>`
*   **Status:** `Draft | In review | Approved | Deprecated`
*   **Version:** `<x.y>`
*   **Date:** `<YYYY-MM-DD>`
*   **Related references:**
    *   Backlog / Epic: `<link>`
    *   Mockups: `<link>`
    *   OpenAPI / Postman: `<link>`
    *   ADR (decisions): `<link>`
    *   Runbook / Ops: `<link>`

 

***

 

## Document structure

 

This document is organized according to the hierarchy:

 

*   **EPIC** → **FEATURE** → **USER STORY** → **TECHNICAL ANALYSIS**

 

**Recommended numbering rules:**

 

*   EPIC: `E-<n>` (e.g., `E-0`)
*   FEATURE: `F-<epic>.<n>` (e.g., `F-0.1`)
*   USER STORY: `US-<epic>.<feature>.<n>` (e.g., `US-0.1.1`)
*   Business rules: `RG-<NNN>`
*   Edge cases: `EC-<NNN>`
*   Patterns/regex: `RGX-<NNN>`
*   UI errors: `ERR_<SCOPE>_<NAME>`
*   API errors: `API_<DOMAIN>_<NAME>`
*   Tests: `UT_<...>`, `IT_<...>`, `PERF_<...>`, `SEC_<...>`, `A11Y_<...>`

 

***

 

# EPIC `<E-ID>`: `<Epic Title>`

 

## Epic objective

 

*   **Business problem:** `<What problem are we solving?>`
*   **Value:** `<Expected value, metrics>`
*   **Out of scope:** `<what is NOT handled>`
*   **Major constraints:** `<tech, legal, timeline>`

 

***

 

# FEATURE `<F-ID>`: `<Feature Title>`

 

## Feature objective

 

*   **Goal:** `<user or business outcome>`
*   **Inputs / Triggers:** `<events, actions>`
*   **Outputs:** `<screens, notifications, data>`
*   **Dependencies:** `<other features / systems>`

 

***

 

## USER STORY `<US-ID>`: `<User Story Title>`

 

### 1. Context and objective

 

**User Story reference**

 

*   **ID:** `<US-ID>`
*   **Title:** `<Title>`
*   **Priority:** `Critical | High | Medium | Low`
*   **As a:** `<Persona / Role>`
*   **I want:** `<capability>`
*   **So that:** `<benefit / value>`

 

**Technical objective**

 

*   `<describe the expected technical outcome (without forcing an unnecessary implementation)>`

 

**Technical prerequisites**

 

*   `<databases, schemas, external services, secrets, feature flags, analytics, etc.>`

 

**Assumptions & out of scope**

 

*   Assumptions: `<...>`
*   Out of scope: `<...>`

 

***

 

### 2. Detailed functional specifications

 

#### 2.1 User flow

 

Describe the journey sequentially.

 

Example (structure):

 

```
Screen/Step A: <Name>
    ↓
Screen/Step B: <Name>
    ↓
Decision: <Condition>
    ├─ Yes → <Step C>
    └─ No  → <Step D>
    ↓
Success: <Result>
```

 

#### 2.2 Screen / step specification

 

> Repeat this block for each screen/step.

 

##### Screen/Step `<N>`: `<Name>`

 

**UI components**

 

*   `<title>`
*   `<description / help>`
*   `<components (buttons, fields, cards, etc.)>`
*   `<states (loading, empty, error)>`

 

**Rules**

 

*   Navigation:
    *   `<action> → <destination>`
*   Enable/Disable:
    *   `<button> enabled if <conditions>`
*   Behavior:
    *   `<timers, confirmations, modals, etc.>`
*   Accessibility (if applicable):
    *   `<focus, screen reader, contrast, sizes>`

 

***

 

#### 2.3 Validations (client-side) & normalizations

 

**Validation rules per field**

 

*   Field: `<fieldName>`
    *   Required: `Yes|No`
    *   Length: `<min/max>`
    *   Allowed characters: `<description>`
    *   Pattern: `RGX-<NNN>` (defined below)
    *   Normalization: `<trim/lowercase/formatting>`
    *   UI error messages: `<ERR_...>`

 

**Patterns (referenced by ID)**

 

*   `RGX-001`: `<pattern as text/code>`
*   `RGX-002`: `<pattern as text/code>`

 

**Real-time validation**

 

*   Indicators: `<✓ / ✗>`
*   Triggers: `<onChange / onBlur>`
*   Debounce: `<duration>`
*   Async checks: `<uniqueness, availability, etc.>`

 

***

 

#### 2.4 Draft / resume (if applicable)

 

*   Trigger: `<events>` + `<debounce>`
*   Storage: `<local storage / cache / db>`
*   Key: `<key>`
*   Expiration: `<duration>`
*   Restore: `<rule>`
*   Deletion: `<when>`

 

***

 

### 3. API contract

 

> Document all endpoints used by this User Story.

 

#### 3.1 Endpoint: `<Endpoint Name>`

 

*   **Method & URL:**

 

```
<METHOD> <PATH>
```

 

*   **Required headers**
    *   `<Header-1>`: `<type>` — `<description>`
    *   `<Header-2>`: `<type>` — `<description>`

 

*   **Query parameters**
    *   `<param>`: `<type>` — `<required?>` — `<description>`

 

*   **Request body (if applicable)**

 

```json
{
  "<field>": "<typeOrExample>",
  "<nested>": {
    "<field>": "<typeOrExample>"
  }
}
```

 

*   **Constraints (summary)**
    *   `<field>`: `<rule>`
    *   `<field>`: `<rule>`

 

*   **Response codes**
    *   `200`: `<description>`
    *   `201`: `<description>`
    *   `400`: `<validation error>`
    *   `401`: `<auth error>`
    *   `403`: `<forbidden>`
    *   `409`: `<conflict>`
    *   `422`: `<semantic/consistency>`
    *   `500`: `<server error>`

 

*   **Response examples**

 

Success response:

 

```json
{
  "status": "success",
  "data": {
    "<id>": "<value>"
  }
}
```

 

Error response (recommended format):

 

```json
{
  "error": "<ERROR_CODE>",
  "message": "<Human readable message>",
  "details": [
    {
      "field": "<fieldName>",
      "message": "<what is wrong>",
      "rejectedValue": "<value>"
    }
  ],
  "correlationId": "<trace-id>"
}
```

 

***

 

### 4. Data model

 

> Describe only the tables/collections impacted by this User Story.

 

#### 4.1 Table/Collection: `<name>`

 

*   **Objective:** `<role of the table>`
*   **Columns / Attributes:**
    *   `<col>`: `<type>` — `<constraints>` — `<description>`
*   **Indexes**
    *   `<index>`: `<reason>`
*   **Constraints / DB rules**
    *   `<rule>`
*   **Identifier strategy**
    *   `<prefix + UUID / sequence / other>`
*   **Audit strategy**
    *   `<created_at, updated_at, append-only, soft delete, etc.>`

 

#### 4.2 Migrations

 

*   Tool: `<Flyway/Liquibase/...>`
*   Scripts: `<names>`
*   Rollback: `<strategy>`

 

***

 

### 5. Business rules and validations (server-side)

 

> Each rule must be testable.

 

*   **RG-001:** `<title>`
    *   Description: `<normative text>`
    *   Valid example: `<...>`
    *   Invalid example: `<...>`
    *   Verification: `<where and how (service, DB constraint, etc.)>`

 

*   **RG-002:** `<title>`
    *   ...

 

***

 

### 6. Error handling

 

#### 6.1 UI errors (client-side)

 

*   `ERR_<...>`: `<user message>`
    *   Trigger: `<condition>`
    *   Expected action: `<what the user must do>`
    *   Severity: `Info | Warning | Blocking`

 

#### 6.2 API errors (server-side)

 

*   `API_<...>` associated with HTTP `<code>`
    *   Cause: `<...>`
    *   Remediation: `<...>`
    *   Observability: `<log level, metric, alert>`

 

#### 6.3 Retry / timeouts (if applicable)

 

*   Timeout: `<duration>`
*   Auto-retry: `Yes|No`
*   Policy:
    *   5xx: `<n> attempts, backoff>`
    *   429: `<Retry-After, backoff>`
    *   4xx: `<no retry>`

 

***

 

### 7. Limit cases and edge cases

 

> List the scenarios that break the “happy path.”

 

*   **EC-001:** `<title>`
    *   Scenario: `<...>`
    *   Expected behavior: `<...>`
    *   Mechanism: `<transaction, lock, DB constraint, etc.>`

 

*   **EC-002:** `<title>`
    *   ...

 

***

 

### 8. Dependencies

 

#### 8.1 Technical dependencies

 

*   Frameworks: `<...>`
*   DB: `<...>`
*   Auth: `<...>`
*   Tools: `<...>`

 

#### 8.2 Functional dependencies

 

*   Internal services: `<...>`
*   External services: `<...>`

 

#### 8.3 Roadmap/business dependencies

 

*   Prerequisites: `<US/Feature>`
*   Next step: `<US/Feature>`

 

***

 

### 9. Technical acceptance criteria

 

#### 9.1 Unit tests

 

*   Minimum coverage: `<%>`
*   Classes/services to cover: `<list>`
*   Mandatory tests:
    *   `UT_<...>`: `<objective>` → `<assertion>`

 

#### 9.2 Integration tests (API)

 

*   `IT_<...>`: `<endpoint>` → `<expected code>` + `<assertions>`

 

#### 9.3 Performance

 

*   Objectives:
    *   `<metric>`: `<value>` (e.g., p95, throughput, error rate)
*   Method:
    *   `<tools>`

 

#### 9.4 Security

 

*   Controls:
    *   `<authz, validation, rate limit, anti-XSS, anti-SQLi>`
*   Tests:
    *   `SEC_<...>`: `<...>`

 

#### 9.5 Accessibility (if UI)

 

*   Standard: `<WCAG/equivalent>`
*   Rules:
    *   `<keyboard, contrast, screen reader, sizes>`

 

#### 9.6 UAT / product metrics (if applicable)

 

*   KPI:
    *   `<kpi>`: `<target>` — `<measurement>`

 

***

 

### 10. Appendices

 

#### 10.1 Diagrams (optional)

 

*   Sequence: `<ASCII/UML>`
*   States: `<ASCII/UML>`
*   Architecture: `<C4 / diagram>`

 

#### 10.2 DTO ↔ Entity mapping (optional)

 

*   DTO: `<...>` → Entity: `<...>`
*   Transformations: `<trim, normalize, defaults>`

 

#### 10.3 Environment variables / configuration (optional)

 

```properties
# Example structure (fictional values)
<KEY>=<VALUE>
```

 

***
