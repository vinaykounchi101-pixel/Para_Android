# Paradox — Software Requirements Specification (SRS)

**Document Version:** 1.1
**Status:** Draft
**Based on:** PARADOX_MASTER_PRD.md (v2.0)
**Platform:** Native Android
**Document Type:** Software Requirements Specification

---

## 1. Introduction

### 1.1 Purpose
This SRS translates the Paradox Master PRD (v2.0) into concrete, buildable software requirements: functional requirements organized by delivery phase, non-functional requirements, the technical architecture and stack, the data model, and the project folder structure. It is the source of truth for *what to build and how it's structured* — the PRD remains the source of truth for *why*.

### 1.2 Scope
Paradox is a native Android personal finance app built around **local-first private profiles** (no cloud account required for core use), fast expense capture, budgeting, and progressively-introduced AI intelligence (Ask Paradox, Safe-to-Spend, Financial Health Score, Leak Hunter, etc.). This SRS covers Phases 1–6 as defined in the Master PRD, with Phase 1 specified to implementation-ready detail and later phases specified to a level sufficient for planning and architecture decisions today (module boundaries, data model, extension points).

### 1.3 Definitions
- **Profile** — a local, isolated financial identity on-device. Not a cloud account.
- **Assisted capture** — any non-manual entry path (voice, OCR, quick add, screenshot, share, SMS candidate) that always ends in a user-confirmed preview.
- **Safe-to-Spend** — deterministic daily spending-capacity estimate.
- **Ask Paradox** — the app's grounded conversational AI surface.

### 1.4 References
- PARADOX_MASTER_PRD.md (v2.0) — product requirements source.

---

## 2. Overall Description

### 2.1 Product Perspective
Paradox Android is a **new, standalone project**. It does not share backend, database, authentication, or architecture with any existing Paradox web application. All data for Phase 1–4 is local to the device; cloud sync is an optional Phase 5 capability, added only if justified.

### 2.2 Product Functions (Summary)
Local private profiles and unlock → expense/income/budget/savings management → fast and assisted capture (voice, OCR, NL quick add, share, screenshot, SMS) → dashboards and analytics → financial intelligence (Ask Paradox, Safe-to-Spend, Financial Health Score, Leak Hunter, Purchase Simulator) → native Android integrations (widget, shortcuts, notifications, share sheet) → optional sync/backup in later phases.

### 2.3 User Classes
Single-user, single-device-per-profile individuals tracking personal expenses; no multi-role or admin user classes in this scope.

### 2.4 Operating Environment
Native Android application. Minimum and target SDK levels, and tablet/foldable layout commitments, are **explicitly deferred** per product decision and will be appended to §6.1 when decided — do not assume a specific API level when implementing.

### 2.5 Design & Implementation Constraints
- Must not depend on any existing web app backend/auth/DB.
- All financial arithmetic must use exact/fixed-precision decimal types — never floating point (`Float`/`Double`) for money.
- AI/OCR/voice must never silently write a financial record; every assisted-capture path ends in an editable, user-confirmed preview.
- Profile data isolation must be enforced at the data layer, not just the UI layer.

### 2.6 Assumptions & Dependencies
- On-device ML (ML Kit) is assumed sufficient for OCR/receipt scanning in Phase 3; a cloud OCR/LLM fallback is optional, not required, per PRD's "prefer local/on-device where practical."
- SMS-based transaction candidates depend on Android platform permission policy at build time and may need redesign if platform restrictions apply — flagged as an open risk, not a blocker for other phases.

---

## 3. System Architecture

### 3.1 Architectural Style
**Layered / MVVM**, organized by **feature module** within a single Gradle module (multi-module split is an optional future refactor, not required for MVP):

```
Presentation (Compose UI + ViewModel)
        ↓ calls
Domain (Use Cases + domain models, no Android dependencies)
        ↓ calls
Data (Repository implementations, Room DAOs, DataStore, mappers)
        ↓ reads/writes
Local Storage (Room + SQLCipher-encrypted SQLite)
```

- **Presentation layer**: Jetpack Compose screens, one `ViewModel` per screen/feature, unidirectional data flow (UI State exposed via `StateFlow`, one-shot events via `Channel`/`SharedFlow`).
- **Domain layer**: pure Kotlin use cases (e.g., `AddExpenseUseCase`, `CalculateSafeToSpendUseCase`), no Android/Room/Compose imports — keeps business/financial logic unit-testable and framework-independent.
- **Data layer**: Repository pattern; Room entities and DAOs live here, never leak into Domain or Presentation. Mappers convert Room entities ↔ domain models.
- **Dependency direction**: Presentation → Domain → Data. Data never depends on Domain or Presentation.

### 3.2 Profile Isolation (Architectural Rule)
Every table that stores user-owned data carries a `profileId` foreign key. All DAO queries are scoped by `profileId` at the query level (not filtered post-query in memory). The active profile's ID is held in a single source of truth (session/current-profile holder) injected into repositories — never assumed from UI state alone.

### 3.3 Concurrency & Reactivity
- Kotlin Coroutines + Flow throughout; Room DAOs expose `Flow<T>` for observable queries (e.g., dashboard totals update reactively as expenses change).
- Long-running work (OCR processing, scheduled recurring-expense generation, future sync) runs via **WorkManager**, not raw coroutines tied to UI lifecycle.

### 3.4 Error Handling Strategy
A shared `Result<T>` / sealed-class wrapper (e.g., `Success`, `Error`, `Loading`) is used across Domain→Presentation boundaries so every screen can render the required Loading/Empty/Error/Offline/Partial-data states consistently (per PRD §28).

---

## 4. Technology Stack

This stack follows the Master PRD's Recommended Technical Direction (§26) directly; items marked *(SRS addition)* are technical decisions this SRS introduces to make the architecture concrete, where the PRD intentionally left implementation detail open.

| Concern | Choice | Notes |
|---|---|---|
| Language | **Kotlin** | Sole app language. |
| UI Toolkit | **Jetpack Compose** + Material 3 | Declarative UI, no XML layouts. |
| Architecture | **MVVM**, layered (Presentation/Domain/Data) | See §3. |
| Local Database | **Room** | Over SQLCipher-encrypted SQLite. |
| Encryption | **SQLCipher** (or equivalent approved encrypted-storage library) | Full local DB encryption. |
| Key Management | **Android Keystore** / StrongBox where available | Encryption keys never stored in plaintext or app code. |
| Biometrics | **BiometricPrompt** (AndroidX Biometric) | Official API only; app never accesses raw biometric data. |
| Camera | **CameraX** | Receipt/bill capture. |
| OCR | **ML Kit Text Recognition** (on-device) | Preferred on-device; cloud OCR optional/future. |
| Voice | **SpeechRecognizer** / on-device speech API | Voice expense entry, voice Ask Paradox input. |
| Background Work | **WorkManager** | Recurring-expense generation, scheduled notifications, future sync. |
| Widgets | **Jetpack Glance** | Home-screen widget (Phase 2). |
| Local Preferences | **Jetpack DataStore** *(SRS addition)* | App/profile-level settings, non-financial preferences. |
| Dependency Injection | **Hilt** *(SRS addition)* | Standard DI for a layered Android/Compose app; not specified in PRD but required for a maintainable MVVM structure at this scope. |
| Navigation | **Navigation Compose** *(SRS addition)* | Screen-to-screen navigation graph. |
| Async/Reactive | **Kotlin Coroutines + Flow** *(SRS addition)* | Underpins Room observability and use-case execution. |
| Money/Decimal Handling | **`BigDecimal`** (or equivalent fixed-precision type) *(SRS addition)* | Never `Float`/`Double` for currency values. |
| Networking (Phase 5+ only) | **Retrofit + OkHttp** *(SRS addition, deferred)* | Only introduced when sync/cloud identity is built; no networking dependency in Phases 1–4. |
| AI/LLM integration (Phase 4+) | **On-device preferred; external API optional** | Provider selection intentionally deferred — see PRD's "prefer local/on-device where practical; external AI optional." Not decided in this SRS. |
| Testing | **JUnit5, Turbine (Flow testing), Espresso/Compose UI Test** *(SRS addition)* | See §9. |
| Logging | **Timber** *(SRS addition)* | Structured local logging; no financial data in logs. |

---

## 5. Folder Structure

Single-module, feature-first, layered internally. This structure applies from Phase 1 onward — later-phase features add new `feature/` packages without restructuring existing ones.

```
paradox-android/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/paradox/app/
│       │   │   ├── ParadoxApplication.kt
│       │   │   ├── MainActivity.kt
│       │   │
│       │   │   ├── di/                         # Hilt modules
│       │   │   │   ├── DatabaseModule.kt
│       │   │   │   ├── RepositoryModule.kt
│       │   │   │   ├── SecurityModule.kt
│       │   │   │   └── UseCaseModule.kt
│       │   │
│       │   │   ├── navigation/
│       │   │   │   ├── ParadoxNavGraph.kt
│       │   │   │   └── Screen.kt
│       │   │
│       │   │   ├── core/
│       │   │   │   ├── common/                 # Result wrapper, Constants, extension fns
│       │   │   │   ├── database/               # Room DB class, migrations, TypeConverters
│       │   │   │   ├── datastore/               # DataStore preferences (settings, AI prefs)
│       │   │   │   ├── security/                # Keystore, SQLCipher passphrase mgmt, BiometricPrompt wrapper
│       │   │   │   ├── money/                   # BigDecimal helpers, currency formatting
│       │   │   │   ├── ui/
│       │   │   │   │   ├── theme/               # Color, Typography, Shape, Theme.kt
│       │   │   │   │   └── components/          # Shared composables (buttons, states, charts)
│       │   │   │   └── util/
│       │   │
│       │   │   ├── data/
│       │   │   │   ├── local/
│       │   │   │   │   ├── entity/              # Room @Entity classes (per feature)
│       │   │   │   │   └── dao/                 # Room @Dao interfaces (per feature)
│       │   │   │   ├── remote/                  # (empty until Phase 5 — sync API clients)
│       │   │   │   ├── repository/               # Repository implementations
│       │   │   │   └── mapper/                   # Entity <-> Domain model mappers
│       │   │
│       │   │   ├── domain/
│       │   │   │   ├── model/                    # Pure Kotlin domain models
│       │   │   │   ├── repository/                # Repository interfaces
│       │   │   │   └── usecase/                   # Use cases, grouped by feature subfolder
│       │   │
│       │   │   ├── feature/
│       │   │   │   ├── onboarding/                # Phase 1
│       │   │   │   ├── profile/                   # Phase 1 — create/unlock/switch profile
│       │   │   │   ├── expense/                   # Phase 1 — CRUD, ledger, search/filter/sort
│       │   │   │   ├── category/                  # Phase 1
│       │   │   │   ├── paymentmethod/             # Phase 1
│       │   │   │   ├── budget/                    # Phase 1 — budgets & guardrails
│       │   │   │   ├── dashboard/                 # Phase 1 — core dashboard + charts
│       │   │   │   │
│       │   │   │   ├── income/                    # Phase 2
│       │   │   │   ├── account/                   # Phase 2 — wallets/accounts model
│       │   │   │   ├── recurring/                 # Phase 2 — recurring expenses & subscriptions
│       │   │   │   ├── savingsgoal/               # Phase 2
│       │   │   │   ├── export/                    # Phase 2 — CSV/PDF export
│       │   │   │   │
│       │   │   │   ├── capture/                   # Phase 3
│       │   │   │   │   ├── voice/
│       │   │   │   │   ├── ocr/
│       │   │   │   │   ├── quickadd/              # NL quick add
│       │   │   │   │   ├── screenshot/
│       │   │   │   │   ├── sharesheet/
│       │   │   │   │   ├── smscandidate/
│       │   │   │   │   └── duplicateguard/
│       │   │   │   │
│       │   │   │   ├── askparadox/                # Phase 4 — conversational assistant
│       │   │   │   ├── insights/                  # Phase 4
│       │   │   │   │   ├── safetospend/
│       │   │   │   │   ├── financialhealth/
│       │   │   │   │   ├── leakhunter/
│       │   │   │   │   ├── forecast/
│       │   │   │   │   └── purchasesimulator/
│       │   │   │   │
│       │   │   │   ├── sync/                      # Phase 5 — cloud sync, conflict resolution
│       │   │   │   ├── auth/                      # Phase 5 — email/Google/passkey (only if sync introduced)
│       │   │   │   ├── backup/                    # Phase 5 — encrypted backup/restore
│       │   │   │   │
│       │   │   │   ├── engagement/                # Phase 6 (optional) — digest, streaks, vibe indicator
│       │   │   │   └── settings/                  # All phases — grows incrementally
│       │   │   │
│       │   │   └── widget/                        # Phase 2 — Glance widget
│       │   │
│       │   └── res/
│       │       ├── drawable/  ├── mipmap/  ├── values/  └── values-*/   # localization
│       │
│       ├── test/                                  # Unit tests — mirrors domain/ and data/ package structure
│       │   └── java/com/paradox/app/
│       │       ├── domain/usecase/...
│       │       └── data/repository/...
│       │
│       └── androidTest/                           # Instrumented + Compose UI tests
│           └── java/com/paradox/app/
│               ├── data/local/                    # Room DAO tests, migration tests
│               └── feature/.../                   # Compose UI tests per feature
│
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

**Convention:** each `feature/<name>/` package internally follows `data/ | domain/ | presentation/` only if that feature has capture-specific logic beyond the shared core layers (e.g., `capture/ocr/` has its own ML Kit integration code); otherwise a feature package holds just `presentation/` (screens + ViewModel) and reuses the shared `domain/usecase/<feature>/` and `data/repository/` already defined centrally. This avoids duplicating Room/DAO code per feature while keeping screens organized by feature.

---

## 6. Functional Requirements by Phase

Requirement IDs: `FR-P<phase>-<number>`.

### 6.1 Phase 1 — Core Foundation / MVP

| ID | Requirement |
|---|---|
| FR-P1-001 | System shall allow creation of one or more local private profiles, each isolated at the data layer. |
| FR-P1-002 | System shall support profile unlock via at least one primary credential method (password, PIN, or pattern — see §12) plus optional Android biometric authentication (fingerprint and/or face, where device-supported) layered on top of the configured primary credential. |
| FR-P1-003 | System shall never allow a profile to read another profile's financial data or AI context, enforced via `profileId`-scoped queries. |
| FR-P1-004 | System shall support full CRUD on expenses: title, amount, currency, category, payment method, date, notes, recurring flag, source, timestamps, optional attachment reference. |
| FR-P1-005 | System shall validate: amount > 0, required fields present, no future dates (unless a future-transaction mode is later enabled), and never silently alter a user-entered value. |
| FR-P1-006 | System shall support starter categories and user-created categories with rename and safe delete/reassignment (no orphaned historical expenses). |
| FR-P1-007 | System shall support payment methods/accounts: Cash, UPI, debit card, credit card, bank account, digital wallet, custom. |
| FR-P1-008 | System shall support combined ledger search/filter/sort: text, date range, category, amount range, payment method; sort by date/amount/category. |
| FR-P1-009 | System shall support daily/weekly/monthly/yearly and category-specific budgets with status: On Track / Near Limit (default ~80% threshold) / Over Budget. |
| FR-P1-010 | System shall support a "soft guardrail" warning when spending approaches or is projected to exceed a budget limit. |
| FR-P1-011 | System shall present a dashboard showing total/current-period spending, remaining budget, budget status, recent expenses, top categories, category breakdown, and spending trend — using only real stored data. |
| FR-P1-012 | System shall perform all financial calculations using exact/fixed-precision decimal arithmetic. |
| FR-P1-013 | Core expense recording, viewing, and management shall function fully offline. |
| FR-P1-014 | Every core screen shall implement Loading, Empty, Success, Error, and Offline states. |
| FR-P1-015 | Destructive actions (delete expense, delete category with dependents, delete profile) shall require explicit confirmation. |
| FR-P1-016 | System shall contain no hardcoded or fake financial data in production builds. |
| FR-P1-017 | Local financial data shall be encrypted at rest; encryption keys shall be managed via Android Keystore, never stored in plaintext. |

### 6.2 Phase 2 — Native Convenience & Financial Foundation

| ID | Requirement |
|---|---|
| FR-P2-001 | System shall provide a home-screen widget (Jetpack Glance) showing Safe-to-Spend, budget progress, current spending, and a Quick Add action, using live data. |
| FR-P2-002 | System shall provide Android app shortcuts for Add Expense, Scan Receipt, and Ask Paradox. |
| FR-P2-003 | System shall register a Share Sheet target that routes supported shared text/images into the assisted-entry pipeline. |
| FR-P2-004 | System shall support configurable notifications: budget warnings, recurring-payment reminders, Safe-to-Spend warnings, savings milestones, transaction review prompts. |
| FR-P2-005 | System shall support income records (salary, freelance, business/personal, other) and calculate total income, total expenses, net cash flow, savings amount, and savings rate from real recorded data. |
| FR-P2-006 | System shall support an accounts/wallets model (cash, bank, card, wallet) without claiming a balance unless sufficient recorded data or an approved integration exists. |
| FR-P2-007 | System shall support recurring expenses (weekly/monthly/yearly) with upcoming-payment visibility and monthly-normalized commitment calculation. |
| FR-P2-008 | System shall support savings goals: name, target amount, current amount, currency, target date, contributions/withdrawals, progress, and estimated completion date. |
| FR-P2-009 | System shall support CSV/PDF/spreadsheet-compatible export of transaction and report data. |
| FR-P2-010 | System shall support light/dark theme and improved responsive layouts for a practical range of phone sizes. |

### 6.3 Phase 3 — Assisted Capture

| ID | Requirement |
|---|---|
| FR-P3-001 | System shall support natural-language Quick Add (e.g., "Uber 240 cash yesterday"), extracting title/merchant, amount, date, payment method, category, and notes into an editable preview before save. |
| FR-P3-002 | System shall support voice-to-expense entry via speech-to-text feeding the same extraction/preview/confirm pipeline as Quick Add. |
| FR-P3-003 | System shall support receipt/bill scanning via CameraX + ML Kit OCR, extracting merchant, amount, date, currency, payment method, category, and relevant text into an editable preview. |
| FR-P3-004 | System shall support screenshot-to-expense: a shared image processed via OCR into a candidate transaction requiring confirmation. |
| FR-P3-005 | System shall implement a Duplicate Guard using amount, merchant/title, date proximity, payment method, and source signals, surfacing a warning (never an automatic reject) before save. |
| FR-P3-006 | System shall support CSV import: column detection → parse → validate → duplicate detection → category suggestion → preview → confirm before commit. |
| FR-P3-007 | Where platform-permitted, system shall support SMS transaction-candidate detection: extract → mask sensitive fields → review → confirm/edit/ignore → save. Full card/account numbers and unnecessary balance data shall never be retained. |
| FR-P3-008 | System shall support AI-suggested categorization during entry, always overridable by the user, and optional AI-assisted new-category suggestions requiring explicit user approval. |

### 6.4 Phase 4 — Financial Intelligence

| ID | Requirement |
|---|---|
| FR-P4-001 | System shall provide **Ask Paradox**, a conversational AI surface, read-only by default, grounded exclusively in the user's actual stored records and deterministic calculations. It shall never fabricate transactions, balances, income, budgets, savings, or history, and shall require explicit confirmation before any consequential action. |
| FR-P4-002 | System shall calculate **Safe-to-Spend** deterministically from remaining budget, current spending, remaining period days, known upcoming expenses, buffer, and savings commitments, presented as Healthy/Moderate/Caution/Danger. |
| FR-P4-003 | System shall calculate a **Financial Health Score** (0–100) from five explainable pillars: Savings Discipline, Budget Adherence, Spending Stability, Cash Cushion, Leak Control — showing contributing factors and up to three suggested actions. |
| FR-P4-004 | System shall implement **Leak Hunter**, identifying recurring subscriptions, repeated small purchases, micro-spending, and high-frequency merchants, clearly distinguishing observed fact from AI inference/recommendation. |
| FR-P4-005 | System shall implement a **Purchase Simulator**: given a hypothetical amount, evaluate against current spending, remaining budget, Safe-to-Spend, upcoming expenses, and savings goals, returning Safe to Buy / Proceed with Caution / Delay Purchase — never auto-creating the expense. |
| FR-P4-006 | System shall calculate burn-rate, budget forecasts, and predictive spending from historical records, clearly labeled as estimates. |
| FR-P4-007 | System shall support adaptive budget recommendations and a 50/30/20 (Needs/Wants/Savings) comparison, presented as optional, explainable suggestions. |
| FR-P4-008 | All AI-derived outputs shall be visually/textually distinguished from actual recorded data. |

### 6.5 Phase 5 — Sync, Backup & Advanced Personalization

| ID | Requirement |
|---|---|
| FR-P5-001 | System shall support optional cross-device sync, introduced only when justified; local-only operation shall remain fully functional without it. |
| FR-P5-002 | When sync is enabled, system shall support account authentication: email/password, Google Sign-In, password recovery, passkeys, session management, and remote logout. |
| FR-P5-003 | System shall queue local changes safely offline, resume sync after connectivity returns, prevent duplicate records, resolve conflicts deterministically, and display clear sync status without silent data loss. |
| FR-P5-004 | System shall support encrypted backup and restore, distinct from sync, with backup behavior clearly communicated to the user. |
| FR-P5-005 | System shall support additional localization languages beyond the Phase 1 baseline (Marathi, Hindi planned; further languages demand-driven). |

### 6.6 Phase 6 — Future Expansion (Optional)

| ID | Requirement |
|---|---|
| FR-P6-001 | System may support shared budgets, split expenses, and family spaces, with private data remaining the default. |
| FR-P6-002 | System may support bank/account aggregation, advanced forecasting, Wear OS, Android Auto, and deeper assistant integration. |
| FR-P6-003 | System may support optional engagement features (monthly digest, logging streaks, achievements, financial vibe indicator, playful commentary) — must never shame users or encourage unhealthy spending, and must remain fully optional. |

---

## 7. Non-Functional Requirements

### 7.1 Security
- No plaintext storage of PIN, password, or credentials.
- Full local database encryption (SQLCipher or equivalent); keys managed via Android Keystore/StrongBox.
- Biometric authentication only via official `BiometricPrompt` API; raw biometric data never accessed by the app.
- Strict profile-level data isolation enforced at the data (query) layer.
- Minimize financial data transmission; no networking dependency until Phase 5.
- Sensitive SMS data (account/card numbers) masked and discarded once no longer required.
- Sensitive screens protected against screenshots/recents-preview where appropriate (`FLAG_SECURE`).
- Permissions requested only when the triggering feature is used; explain-then-request flow; denial must not break unrelated features.

### 7.2 Performance
- Core local operations (add/edit/view expense, dashboard load) should feel immediate (target: no perceptible delay on representative mid-range devices — exact ms targets to be validated during implementation, not fixed prematurely in this SRS).
- Dashboard calculations must not require network access.
- Assisted-capture flows (OCR, voice, quick add) must provide timely feedback and a visible processing state.

### 7.3 Reliability
- All local writes atomic; no partial/corrupt records on interruption.
- No silent data loss under any failure mode.
- Deterministic financial calculations — identical inputs always produce identical outputs.
- Graceful feature-level failure (e.g., AI unavailable does not block manual expense entry).

### 7.4 Usability & Accessibility
- TalkBack/screen-reader support with meaningful content descriptions throughout, not only in a dedicated accessibility screen.
- Adequate touch target sizes, dynamic text scaling, sufficient contrast, reduced-motion consideration.
- Accessible charts and financial summaries (not color-only encoding).

### 7.5 Responsiveness
- Support a practical range of Android phone sizes at minimum; foldable/tablet layout support is a stretch goal, not a Phase 1 commitment.

### 7.6 Localization
- English at launch; architecture must support adding Marathi/Hindi (and further languages) without structural rework — i.e., no hardcoded user-facing strings.

---

## 8. Data Requirements (Entity Overview)

Full schema (column types, indices, migrations) belongs in a separate technical data-modeling document; this section defines entity scope and key relationships for architecture purposes.

| Entity | Key Fields | Scoped By | Phase |
|---|---|---|---|
| Profile | id, name, createdAt, unlockMethod | — (root entity) | 1 |
| Category | id, profileId, name, isCustom, parentDefaultId? | profileId | 1 |
| PaymentMethod | id, profileId, type, label, isCustom | profileId | 1 |
| Expense | id, profileId, title, amount, currency, categoryId, paymentMethodId, date, notes, recurringFlag, source, attachmentRef, createdAt, updatedAt | profileId | 1 |
| Budget | id, profileId, type(daily/weekly/monthly/yearly/category), amount, categoryId?, thresholdPct | profileId | 1 |
| Income | id, profileId, source, amount, currency, date, notes | profileId | 2 |
| Account | id, profileId, type, label, trackedBalance? | profileId | 2 |
| RecurringExpense | id, profileId, expenseTemplate, frequency, nextDueDate | profileId | 2 |
| SavingsGoal | id, profileId, name, targetAmount, currentAmount, currency, targetDate, contributions[], withdrawals[] | profileId | 2 |
| CaptureCandidate | id, profileId, sourceType, rawPayload, extractedFields, status(pending/confirmed/ignored) | profileId | 3 |
| AiInsightLog | id, profileId, type(healthScore/leak/forecast/etc.), payload, generatedAt, isEstimate | profileId | 4 |
| SyncQueueItem | id, profileId, entityRef, operation, status | profileId | 5 |

All monetary fields use fixed-precision decimal storage (not floating point). All tables carry `profileId` except `Profile` itself.

---

## 9. External Interface Requirements

### 9.1 User Interface
Jetpack Compose + Material 3; single Activity, Compose Navigation-driven screen graph.

### 9.2 Hardware Interfaces
- **Camera** (CameraX) — receipt/bill capture, Phase 3.
- **Microphone** — voice entry, Phase 3.
- **Biometric sensor** — via BiometricPrompt, Phase 1.

### 9.3 Software Interfaces
- **ML Kit** (on-device OCR) — Phase 3.
- **Android SpeechRecognizer** (or equivalent on-device speech API) — Phase 3.
- **WorkManager** — scheduled/background jobs, from Phase 2 (recurring generation) onward.

### 9.4 Communication Interfaces
None until Phase 5. When introduced: Retrofit/OkHttp over TLS for sync API calls only; no networking dependency in Phases 1–4 beyond optional external-AI calls if a cloud AI provider is later chosen for Phase 4 (provider undecided — see §4).

---

## 10. Testing Strategy (Summary)

- **Unit tests** (JUnit5): domain use cases (especially financial calculations — budget math, Safe-to-Spend, Financial Health Score), validation logic, authentication logic, profile-isolation logic, and repository logic, with Turbine for Flow-based tests.
- **Database tests** (Room testing library): DAO queries, foreign-key/constraint behavior, migrations, delete behavior, and — critically — **profile-isolation tests** verifying no cross-profile data leak (directly maps to PRD's critical security metric).
- **Compose UI tests**: onboarding, authentication/unlock, expense CRUD, search/filter/sort, budget setup, dashboard rendering, and required error/offline states.
- **Security tests**: unauthorized cross-profile access attempts, authentication-bypass attempts (navigation/deep-link), locked-state navigation behavior, sensitive-data exposure checks, and screenshot/`FLAG_SECURE` behavior on protected screens.
- **Edge-case tests**: covering the scenarios enumerated in §18 (Edge Cases) — malformed input, offline/storage-exhaustion conditions, assisted-capture failure modes, and AI-unavailable/timeout paths.
- Detailed coverage targets and CI pipeline configuration belong in a separate technical/QA document, per product decision to defer launch-readiness detail; this section defines *what* must be tested, not *how much* or *on what schedule*.

---

## 11. Open Items (Deferred by Product Decision)

The following are intentionally **not specified** in this SRS, per explicit product decision to defer until closer to build/launch relevance:

- Minimum/target Android SDK version and tablet/foldable support commitment.
- SMS-parsing platform-permission feasibility confirmation.
- External AI/LLM provider selection for Ask Paradox and cloud-assisted features (Phase 4+).
- Analytics/instrumentation plan, crash reporting tooling, monetization model, versioning/migration strategy, and legal/regulatory compliance detail (Play Store policy, DPDP Act, etc.).
- Exact authentication lockout policy (failed-attempt thresholds, cooldown duration) — §12.4 intentionally does not invent numbers.
- Detailed Phase 5 backup strategy (storage location, format, retention).
- Detailed Phase 5 sync conflict-resolution algorithm (beyond the "deterministic" requirement already stated).
- Multi-currency live conversion approach — Phase 1 stores original transaction currency only; conversion/rates handling is not yet decided (see PRD §10 and §21 of this SRS).
- Maximum receipt/attachment image size and retention policy specifics (§23 defines the *rules*, not the exact limits).

These should be resolved and appended to this SRS before their corresponding phase begins implementation.

---

## 12. Authentication & Application-Lock Specification

This section expands FR-P1-002 and the security principles in §7.1 into a full authentication model. It supersedes any implication elsewhere in this document that PIN + biometric are the only supported methods.

### 12.1 Supported Methods

**Primary app authentication** (credential-based; always available regardless of device biometric hardware):
1. Password
2. PIN
3. Pattern

At least one primary method must be configured before a profile can be used; a profile can never rely on biometric authentication alone.

**Biometric authentication** (optional convenience layer on top of a configured primary method, only where the device supports it):
- Fingerprint
- Face authentication, where supported by the device/platform as a system-recognized strong biometric mechanism
- Other Android-supported strong biometric mechanisms, where appropriate

The system shall never present a biometric option the current device does not actually support (e.g., face authentication must not be offered on a device without a supporting sensor/API).

### 12.2 Security Rules
- Passwords, PINs, and patterns shall never be stored in plaintext; verification shall use a secure one-way hashing approach appropriate to each credential type.
- Biometric data (fingerprint/face templates) shall never be stored, transmitted, or otherwise accessed by Paradox — the app only ever receives a success/failure result from Android's system biometric API.
- Biometric prompts shall use `BiometricPrompt` (or the current equivalent AndroidX Biometric API) exclusively; no custom biometric-recognition implementation.
- Cryptographic keys protecting stored credentials/data shall be managed via Android Keystore, using StrongBox where available.
- Authentication state shall be protected across process death (see §19) — the app must not silently resume in an unlocked state after the process is recreated.
- Authentication shall not be bypassable via back navigation, deep links, or any alternate entry path into a protected screen.
- A backgrounded-then-foregrounded app must not expose sensitive screens/data merely because the process remained alive; locking is enforced independently of process lifetime (see §12.3).
- Authentication failure and cancellation shall be handled gracefully, without exposing which part of a credential was incorrect.
- Passwords, PINs, patterns, biometric results, and any authentication tokens shall never appear in application logs (see §24).

### 12.3 Authentication Configuration Behavior
The system shall define behavior (not specific UI) for:
- Selecting an initial primary authentication method during profile setup.
- Changing the primary authentication method, requiring confirmation of the *existing* credential first.
- Enabling/disabling biometric unlock as a layer on top of the primary method.
- Setting or changing the password/PIN/pattern, always gated by confirming the current credential.
- Re-authentication before sensitive operations (see §7.1 sensitive-action list and PRD §7.2/§15.2 equivalents).
- Manual locking (explicit user action) and automatic locking (see triggers below).
- Locking on app backgrounding, and — where the product later defines a specific timeout — locking after configurable inactivity.
- Re-establishing authentication after process recreation, per §19.

### 12.4 Authentication Fallback Rules
- If biometric authentication fails or is cancelled, the user falls back to their configured primary credential (password/PIN/pattern); the app remains in the `LOCKED` state until either succeeds.
- If biometric hardware/enrollment is unavailable on the device, biometric unlock is not offered, and the primary credential remains the sole unlock path.
- If the user removes or changes device-level biometric enrollment (e.g., adds a new fingerprint) such that any biometric-bound cryptographic key becomes invalidated, the app shall detect this and safely fall back to the primary credential rather than fail silently or crash.
- Repeated failed authentication attempts shall not reveal which character/step was incorrect, nor otherwise leak information about the stored credential.
- Device/OS-level lockout behavior (e.g., Android's own biometric attempt limits) shall be respected and surfaced to the user via a generic "try again later" style message.
- Exact lockout thresholds (number of attempts, cooldown duration) are **not defined here** — see §11 Open Items; no arbitrary numeric policy is invented by this SRS.

### 12.5 Authentication State Machine

```
LOCKED
  │  (unlock requested)
  ▼
AUTHENTICATING
  │                       │
  │ success                │ failure / cancel
  ▼                       ▼
UNLOCKED                LOCKED
```

Additional recognized states:
- **Biometric Unavailable** — device has no usable biometric enrollment; system routes directly to primary-credential entry within `AUTHENTICATING`.
- **Authentication Failed** — a terminal sub-state of a single attempt within `AUTHENTICATING`, distinct from returning to `LOCKED` (used for attempt-count tracking without exposing counts to the user beyond a generic message).
- **Authentication Cancelled** — user-initiated exit from `AUTHENTICATING` back to `LOCKED` (e.g., dismissing the biometric prompt).
- **Credential Recovery/Reset** — reachable only via an explicit, deliberately-friction-added flow (exact recovery mechanism is a product decision not yet made; see §11).

The app transitions to `LOCKED` on: explicit manual lock, app backgrounding (subject to product-defined grace behavior), and process recreation without a valid retained authentication session (§19).

---

## 13. Navigation & Screen Requirements

This section defines navigation *behavior*; visual/UI specification remains in `DESIGN_SYSTEM.md` (see §26).

- **Start destination**: on cold launch, the system routes to onboarding (first launch), profile selection (multiple existing profiles), or directly to the lock screen (single existing profile) — never directly into a protected screen without passing through the current `LOCKED`/`UNLOCKED` state from §12.5.
- **First-launch/onboarding flow**: profile creation → primary credential setup → optional biometric enablement → starter category/currency confirmation → main dashboard.
- **Profile selection/switching**: switching profiles always re-enters the `LOCKED` state for the newly selected profile; the previous profile's data must not remain visible or cached in a readable UI state during/after the switch.
- **Authentication flow**: any navigation into a protected screen while `LOCKED` routes through `AUTHENTICATING` first; a successful unlock returns the user to their originally intended destination.
- **Main navigation**: dashboard, ledger/expense list, budgets, insights (Phase 4+), and settings as top-level destinations; expense creation/edit and expense detail are pushed/modal destinations reachable from multiple entry points (dashboard quick add, ledger, widget, shortcuts).
- **Back navigation**: back from any top-level destination exits to the previous top-level destination or backgrounds the app, never bypassing the lock state.
- **Modal vs. full-screen**: destructive-action confirmations and quick-capture previews (assisted entry) are modal; primary CRUD screens are full-screen destinations.
- **Navigation after destructive actions**: deleting the currently-viewed entity (expense, category, profile) navigates back to the owning list/dashboard, never leaves the user on a now-invalid detail screen.
- **Deep-link security**: no deep links are introduced before Phase 5; if added later, any deep link into a protected screen must pass through the authentication flow exactly as in-app navigation does — a deep link must never be a bypass path (reinforces §12.2).

---

## 14. Android Permission Matrix

| Permission / Capability | Feature | Phase | Required/Optional | Denial Behavior |
|---|---|---|---|---|
| Camera | Receipt/bill scanning | 3 | Optional | Manual entry and all other features remain fully usable; capture screen shows a clear explanation and a path to manual entry. |
| Microphone | Voice expense entry, voice Ask Paradox input | 3 / 4 | Optional | Voice entry point is hidden/disabled with explanation; manual and text-based entry remain available. |
| Notifications (`POST_NOTIFICATIONS` on Android 13+) | Budget warnings, reminders, Safe-to-Spend alerts | 2 | Optional | In-app equivalents (dashboard indicators) remain visible; no functional loss beyond the notification itself. |
| Biometric hardware access | Biometric unlock | 1 | Optional | Primary credential (password/PIN/pattern) remains the unlock method; biometric option simply is not offered. |
| Storage/file access (scoped, for export/import/attachments) | CSV import/export, receipt attachments | 2 / 3 | Required *only when that specific feature is invoked* | Feature-specific: export/import/attachment action is blocked with explanation; unrelated features unaffected. |
| SMS read access | SMS transaction candidates | 3 | Optional, and **conditional on platform/policy feasibility** | Feature is disabled entirely if infeasible or denied; all other capture methods remain available. This permission is never requested during onboarding. |
| Location | Any future location-aware feature | 6 (not committed) | Optional, and only requested if such a feature is explicitly enabled by the user | No impact on core app — location is never required for ordinary expense tracking. |

Rules:
- Permissions are requested contextually, at the moment a feature needs them — never during onboarding.
- Each request is preceded by an in-app explanation of why it's needed.
- Denial of any permission above must never break unrelated features.
- SMS remains explicitly conditional pending the open feasibility item in §11.

---

## 15. Data Model — Implementation-Level Constraints

This section adds implementation constraints on top of the entity overview in §8. It does not restate every field already listed there.

### 15.1 Profile
- Primary key: `id`. No foreign keys (root entity).
- `name`: required, non-empty, reasonable max length.
- Authentication fields (added by §12): `primaryAuthType` (enum: password/pin/pattern), `primaryCredentialHash`, `biometricEnabled` (bool), `createdAt`.
- Delete behavior: deleting a profile cascades to all `profileId`-scoped rows across every entity below (hard delete by default — see §22).

### 15.2 Expense
- Primary key: `id`. Foreign keys: `profileId` → Profile (required, indexed), `categoryId` → Category (required), `paymentMethodId` → PaymentMethod (required).
- `amount`: required, fixed-precision decimal, must be > 0 (see §16.1).
- `currency`: required, ISO 4217-compatible code.
- `date`: required, not in the future (see §16.1).
- Update behavior: edits update `updatedAt`; `createdAt` is immutable.
- Delete behavior: hard delete by default (see §22); referential integrity handled per §15.3/§15.4 below when a referenced Category/PaymentMethod is deleted.
- Indexes: `profileId`, `categoryId`, `date` (supports ledger filter/sort performance).

### 15.3 Category
- Primary key: `id`. Foreign key: `profileId` (required, indexed).
- `name`: required, unique per `profileId` (case-insensitive).
- Delete behavior: a Category with existing dependent Expenses cannot be hard-deleted directly — the system must require reassignment of those expenses to another category (or a designated "Uncategorized" starter category) before deletion completes (reinforces FR-P1-006).

### 15.4 PaymentMethod
- Primary key: `id`. Foreign key: `profileId` (required, indexed).
- `type`: constrained enum (Cash, UPI, Debit Card, Credit Card, Bank Account, Digital Wallet, Custom).
- Delete behavior: same reassignment-before-delete rule as Category.

### 15.5 Budget
- Primary key: `id`. Foreign keys: `profileId` (required), `categoryId` (nullable — null means whole-profile budget).
- `type`: constrained enum (daily/weekly/monthly/yearly/category).
- `amount`: required, fixed-precision decimal, > 0.
- `thresholdPct`: default ~80% per FR-P1-009, overridable.

### 15.6 Income (Phase 2)
- Primary key: `id`. Foreign key: `profileId` (required, indexed).
- `amount`: required, fixed-precision decimal, > 0. `currency`: required. `date`: required.

### 15.7 Account (Phase 2)
- Primary key: `id`. Foreign key: `profileId` (required, indexed).
- `trackedBalance`: nullable — the system must never display a balance unless it is derived from sufficient recorded data or an approved integration (reinforces FR-P2-006).

### 15.8 RecurringExpense (Phase 2)
- Primary key: `id`. Foreign keys: `profileId` (required), references an Expense template.
- `nextDueDate`: required, drives WorkManager-scheduled generation (see §20).

### 15.9 SavingsGoal (Phase 2)
- Primary key: `id`. Foreign key: `profileId` (required).
- `targetAmount`, `currentAmount`: required, fixed-precision decimal, ≥ 0. `targetDate`: required, must be a valid future-relative date at creation time.

### 15.10 CaptureCandidate (Phase 3)
- Primary key: `id`. Foreign key: `profileId` (required).
- `status`: constrained enum (pending/confirmed/ignored). Never transitions to a saved Expense without passing through `confirmed`, which itself requires explicit user action (reinforces the capture pipeline rule in §1.3/§8).
- `rawPayload`: subject to the masking/retention rules in §23 when it originates from SMS or an image.

### 15.11 AI-Related Records — AiInsightLog (Phase 4)
- Primary key: `id`. Foreign key: `profileId` (required).
- `isEstimate`: required boolean — every AI-derived record must be explicitly flagged as such (reinforces FR-P4-008).
- Not a source of truth for financial totals; always derived from, never a substitute for, the underlying Expense/Income/Budget records.

All entities above inherit the global rules already stated in §8: fixed-precision decimal for money, `profileId` isolation on every table except Profile itself, and `createdAt`/`updatedAt` timestamps where the entity is mutable.

---

## 16. Validation Rules

### 16.1 Expense
- Amount: required, fixed-precision decimal, strictly > 0; no upper bound imposed by this SRS beyond what the decimal type supports (no arbitrary cap invented here).
- Title: required, non-empty, reasonable max length (exact limit left to implementation; must not silently truncate without informing the user).
- Category, Payment Method: required, must reference an existing, non-deleted record scoped to the active profile.
- Date: required; future dates rejected unless/until a future-transaction mode is introduced (not currently in scope).
- Description/notes: optional, reasonable max length.
- Currency: required, must be a supported/valid code.

### 16.2 Income
- Amount: required, fixed-precision decimal, strictly > 0.
- Date: required, not unreasonably far in the future.
- Source/category: required.
- Currency: required, valid code.

### 16.3 Budget
- Amount: required, strictly > 0.
- Period/type: required, one of the defined enum values.
- Category/scope: required when `type = category`; must be null/absent for whole-profile budgets.
- Date/period relationships: a category budget's period must not conflict with (e.g., silently override) an existing overlapping budget of the same scope — exact conflict-resolution UX is left to product design, but the system must not silently allow two contradictory active budgets for the same category+period without surfacing the conflict.

### 16.4 Savings Goal
- Target amount: required, strictly > 0.
- Current amount: required, ≥ 0, never permitted to exceed target amount through direct edit (only via recorded contributions, per §8's `contributions[]`/`withdrawals[]`).
- Target date: required, must be a valid date; the system does not impose an arbitrary minimum/maximum timeframe.
- Goal name: required, non-empty.

No field above receives an arbitrary length/value limit beyond what is stated; where a concrete limit is genuinely needed, it is an implementation detail to be documented in code, not invented here.

---

## 17. Error Taxonomy

| Error Category | User-Facing Behavior | Logging | Recovery |
|---|---|---|---|
| Validation error | Inline, field-specific message | Not logged as an error (expected user input state) | User corrects input |
| Authentication error | Generic "incorrect credential" message (no detail on which part failed) | Failure event logged without the credential itself | Retry, subject to §12.4 lockout behavior |
| Authorization/access error | Generic "not available" — never reveals existence of another profile's data | Logged with profile-scoped context only | N/A — should not be user-reachable if §3.2 isolation holds |
| Not found | "Item no longer available" + return to list | Logged at debug level | Return to previous screen |
| Database error | Generic "couldn't save/load" message | Logged with stack trace (no financial values) | Retry; data remains queued/unsaved rather than partially written |
| Encryption/security error | Generic security-error message; does not expose cryptographic detail | Logged without key material | May require re-authentication |
| Permission denied | Feature-specific explanation + link to manual alternative where one exists | Logged as a denial event, not an error | User can retry after granting, or continue without the feature |
| Camera failure | "Camera unavailable" + fallback to manual entry | Logged at debug level | Manual entry |
| OCR failure | "Couldn't read this receipt" + editable blank/partial preview | Logged at debug level (no image content in logs) | User completes fields manually |
| Speech recognition failure | "Didn't catch that" + retry or switch to manual | Logged at debug level | Retry or manual entry |
| Import failure | Row-level error summary, valid rows still previewed | Logged with row count, not raw financial content | User corrects file or excludes bad rows |
| Export failure | "Export failed" + retry option | Logged with stack trace | Retry |
| Backup/restore failure | Clear failure state, no partial/corrupt restore applied | Logged with stack trace | Retry; original data untouched until restore fully succeeds |
| Network failure (Phase 5+) | "You're offline" indicator, queued for retry | Logged at debug level | Automatic retry on connectivity return |
| AI provider failure | "AI unavailable right now" + manual fallback (reinforces FR-P4 grounding rules) | Logged without request/response content | Manual path remains fully functional |
| Sync failure (Phase 5+) | Visible sync-status indicator, never silent | Logged with operation type, not financial values | Automatic resume when connectivity/consistency allows |
| Storage full | "Not enough space" before attempting a write that would fail | Logged at debug level | User frees space; no partial write occurs |
| Unknown/unexpected error | Generic apology message, no stack trace shown to user | Full detail logged locally (no financial data) | Return to last known-good screen |

General rule: no error path exposes sensitive technical detail, financial values, or credentials to the user or to logs.

---

## 18. Edge Cases

### Expenses
- Zero or negative amount entry attempt (rejected per §16.1).
- Unrealistically large amount (accepted if the decimal type supports it; not arbitrarily capped).
- Future-dated entry attempt (rejected, see §16.1).
- Missing category at save time (blocked — category is required).
- A Category referenced by existing Expenses is deleted (must be prevented/require reassignment first, per §15.3).
- A PaymentMethod referenced by existing Expenses is deleted (same rule, §15.4).
- Duplicate submission from a rapid repeated tap (must be debounced/idempotent at the UI or use-case layer).
- Database write failure mid-save (no partial record persisted; user is informed and retains their entered data to retry).

### Profiles
- Switching profiles mid-session (must fully clear the previous profile's in-memory/UI state before the new profile's data loads — reinforces §3.2 and §13).
- Deleting a profile (requires confirmation per FR-P1-015; cascades per §15.1).
- An empty profile with no data yet (dashboard/ledger must render a defined Empty state, not an error).
- Interacting with a profile that is locked (all protected screens route through §12.5's `AUTHENTICATING`).
- Repeated wrong-credential entry (handled per §12.4, no information leakage).
- Process death occurring mid-profile-switch (system must not leave the app in an ambiguous state — on relaunch, the user re-enters via the lock screen for whichever profile was last fully active).

### Offline
- App launched with no connectivity at all (Phase 1–4 core functions are unaffected; only Phase 5+ sync-dependent UI shows an offline indicator).
- Local database temporarily unavailable/locked (rare; system surfaces a database error per §17 rather than crashing).
- Device storage nearly full (system should ideally warn before the threshold blocks writes).
- Device storage completely full (write attempts fail per §17's Storage Full behavior — no silent data loss, no partial write).
- Background work (WorkManager) executing while offline (jobs requiring connectivity defer/retry per §20; jobs that don't require it proceed normally).
- Recovery after a temporary failure (queued/pending operations resume automatically once the blocking condition clears).

### Assisted Capture
- OCR cannot detect an amount on the receipt (preview shows the field blank/editable, not a guessed value).
- OCR produces an incorrect value (user can freely edit before confirming — this is why every capture path ends in a preview).
- Multiple candidate amounts detected on one receipt (system must surface the ambiguity rather than silently picking one, e.g., highlight the most likely candidate as pre-filled but editable).
- Missing merchant/title (left blank for user completion, not fabricated).
- Poor image quality (system may prompt for a retake but must not block manual completion of an already-attempted preview).
- Camera permission denied mid-flow (graceful fallback to manual entry, per §14).
- Speech recognition service unavailable on-device (fallback to manual/text entry).
- Speech misrecognition (user edits the resulting preview before confirming, same as OCR).
- User cancels a capture mid-flow (no partial candidate is silently saved).

### AI (Phase 4+)
- AI service unavailable (manual fallback per §17's AI provider failure row; core app unaffected).
- AI request timeout (same fallback; user is not left in an indefinite loading state).
- Invalid/malformed AI response (treated as a failure, not partially trusted).
- AI produces an incorrect or internally conflicting result (user can reject it; AI output is never silently trusted over user correction).
- No AI provider configured/available (feature is hidden or clearly marked unavailable, not presented as broken).
- User rejects an AI suggestion (system must not re-apply it automatically; rejection is respected).

AI shall never silently modify a financial record without the explicit confirmation already mandated throughout §6.3–§6.4 and reinforced here.

---

## 19. Android Lifecycle & Process Death Requirements

- **Activity recreation / configuration changes** (rotation, theme change, locale change): in-progress screen state (e.g., a partially-filled expense form) must survive via standard state-saving mechanisms; already-committed financial data is never affected since it lives in Room, not in-memory state.
- **App backgrounding**: triggers the locking behavior defined in §12.3; in-progress but *unsaved* entry state may be preserved for a short return, but any sensitive display must not remain visible in the recents/app-switcher preview if `FLAG_SECURE`-equivalent protection is configured for that screen.
- **Process death** (system-initiated, e.g., low memory): on relaunch, the app returns to the `LOCKED` state (§12.5) regardless of what screen the user was on before death — an unlocked session must never silently resume after process death.
- **Interrupted expense creation**: if the process dies before the user confirms/saves, the in-progress entry is lost (acceptable, since it was never committed) — but any *already-saved* expense from earlier in the session must remain intact and correct.
- **Interrupted assisted capture**: same rule — an unconfirmed capture candidate does not survive process death; a confirmed/saved one does.
- **Interrupted authentication**: if process death occurs mid-`AUTHENTICATING`, the app returns to `LOCKED` on relaunch, not to a partially-authenticated state.
- **WorkManager execution after process death**: scheduled/background work (recurring-expense generation, future notifications/sync) must be resilient to process death — WorkManager's own persistence handles this, but use cases invoked from a Worker must remain idempotent (see §20).

No already-committed financial record may be lost due to UI-process death under any scenario above.

---

## 20. WorkManager Requirements

Applies to all current and future background work: recurring-expense generation (Phase 2), scheduled notifications (Phase 2), import processing (Phase 3), AI-related background processing (Phase 4, if any), and sync (Phase 5).

- **When used**: any work that should survive process death, doesn't need to block the UI, or runs on a schedule (recurring generation, reminders) or in response to connectivity (sync retry).
- **Constraints**: network-requiring work (Phase 5 sync) is constrained to require connectivity; non-network work (recurring-expense generation) has no such constraint.
- **Retry policy**: exponential backoff for transient failures (e.g., sync attempt failing due to connectivity).
- **Idempotency**: every Worker's use case must be safe to re-run without creating duplicate records — e.g., recurring-expense generation must check whether the due-date instance already exists before creating it.
- **Cancellation**: user-facing actions that make a queued job obsolete (e.g., deleting a RecurringExpense whose generation job is pending) must cancel the corresponding work.
- **Duplicate execution protection**: unique work names/constraints used where a job must not run twice concurrently (e.g., only one recurring-generation pass per profile per due date).
- **Battery considerations**: non-urgent work should not request aggressive scheduling; respect Android's standard deferred-execution behavior rather than forcing immediate execution where not needed.
- **Failure recovery**: a permanently-failing job (exhausted retries) must surface its failure state to the user where relevant (e.g., a stuck sync) rather than failing silently forever.

---

## 21. Money & Currency Specification (Expanded)

This expands the brief mentions in §2.5, §4, and §8 into a complete money-handling specification.

- Financial calculations shall **never** use `Float` or `Double`; all monetary values are stored and computed using a fixed-precision decimal representation (e.g., `BigDecimal` in Kotlin, per §4).
- Currency is represented using an ISO 4217-compatible code (e.g., `INR`, `USD`, `EUR`, `GBP`) stored alongside every monetary value, per §8's Expense/Income/Budget/SavingsGoal fields.
- **Rounding behavior**: rounding, where required (e.g., for display), must use a consistent, explicitly-defined rounding mode (e.g., half-even/banker's rounding or standard half-up — the specific mode is an implementation decision to document in code, but it must be applied consistently everywhere money is displayed or aggregated, never ad hoc per screen).
- **Display formatting is separate from stored values**: the stored decimal value is never mutated for display purposes; formatting (currency symbol, grouping separators, decimal places) is a presentation-layer concern applied at render time only.
- **No precision loss during calculation**: intermediate calculations (budget totals, Safe-to-Spend, Financial Health Score inputs) must preserve full decimal precision until a final, explicitly-rounded display value is produced.
- **Currency association**: every monetary record is associated with a currency at the transaction/record level (not silently inherited or assumed) — this is already reflected in the §8 entity table.
- **Multi-currency scope**: per the Master PRD, only the *original transaction currency* is stored and a *preferred display currency* may differ; this SRS does **not** define automatic exchange-rate conversion behavior — that remains an Open Item (§11) and must not be silently implemented without an approved specification. If/when conversion is introduced, cached rates must be clearly dated/sourced, and any mixed-currency total involving unavailable rates must not be presented as exact (per PRD §10).

---

## 22. Data Lifecycle & Deletion Rules

- **Delete Expense**: hard delete by default — no product/technical requirement for soft-delete has been identified. If a future requirement (e.g., an undo window) justifies soft-delete, it must be added as an explicit new requirement, not assumed here.
- **Delete Category / Payment Method**: cannot be hard-deleted while referenced by existing Expenses; requires reassignment or explicit cascading choice presented to the user first (§15.3/§15.4).
- **Delete Profile**: hard delete, cascading to all `profileId`-scoped data across every entity in §15, following the confirmation requirement in FR-P1-015.
- **Backup implications**: a hard delete is not automatically reversible from within the app; if Phase 5 backup exists, a prior backup could theoretically contain the deleted data, but the app itself provides no in-app "undo delete" — this is a data-recovery limitation to be clearly communicated to the user before a destructive action, not silently implied.
- **Export implications**: exported data (CSV/PDF) is a point-in-time snapshot; deleting records afterward does not retroactively affect already-exported files, and the app has no mechanism to recall or invalidate an export.
- **Restore implications** (Phase 5): a restore operation must not silently merge with existing data in a way that could duplicate or corrupt records — restore behavior (replace vs. merge) must be explicit and confirmed by the user.

---

## 23. Receipt / Attachment Storage Rules

Applies to receipt/bill images captured or selected for OCR (Phase 3) and any resulting stored attachment reference (§8's `attachmentRef` field).

- Images are stored in the app's private, non-public storage scope — never in publicly accessible shared storage.
- Images are stored outside the Room database itself (as files, referenced by path/URI in the `attachmentRef` field), not as BLOBs inside Room rows.
- File naming must not leak sensitive information (e.g., no merchant name or amount embedded in the filename).
- Every stored attachment is scoped to the owning `profileId`, consistent with the isolation rule in §3.2 — one profile must never be able to resolve another profile's attachment path.
- Attachments fall under the same local encryption-at-rest expectations as other sensitive local data (§7.1, §12.2's Keystore-backed protection).
- **Lifecycle**: an attachment persists as long as its owning Expense record exists.
- **Deletion**: deleting the owning Expense deletes its attachment file; an *abandoned* capture (user captured an image but never confirmed/saved the resulting expense, per §15.10's `ignored` status) must have its temporary file cleaned up rather than left orphaned on disk.
- **Backup/export implications**: whether attachments are included in a future backup/export is an Open Item (§11) — not decided here; export of raw images must not happen implicitly as a side effect of exporting transaction data (CSV/PDF) unless explicitly specified.
- **Maximum reasonable image size**: this SRS does not fix an exact byte/pixel limit; a sensible implementation-level cap should exist to prevent unbounded storage growth, but the exact number is an implementation detail, not a hard SRS requirement.

---

## 24. Security & Privacy Model (Expanded)

Consolidates and extends §7.1 into a fuller model, without contradicting anything already stated there.

- **Threat model (summary)**: the primary threats addressed are (a) another app or user on the same device reading Paradox's data, (b) one profile reading another profile's data within the app, and (c) sensitive data leaking via logs, screenshots, backups, or third-party (AI) transmission. Threats explicitly out of scope for this SRS: server-side breaches (no server exists in Phases 1–4) and sophisticated device-rooting/forensic attacks beyond what Android Keystore/StrongBox reasonably defends against.
- **Local database protection**: full encryption via SQLCipher (or equivalent), keys in Android Keystore/StrongBox — reinforces §7.1/§12.2, not a new requirement.
- **Screen protection**: sensitive screens (unlock, financial detail, export/backup) apply `FLAG_SECURE` or equivalent to prevent screenshots and recents-preview exposure.
- **Clipboard protection**: sensitive fields (e.g., a copied amount or account identifier) should not linger indefinitely on the system clipboard where Android's clipboard-history features could expose them longer than necessary — exact clipboard-expiry handling is an implementation detail, not a fixed number here.
- **Logs**: financial data, credentials, biometric results, and AI request/response content shall never be written to application logs, in either debug or release builds. Debug builds may log non-sensitive diagnostic information; release builds should minimize logging further.
- **Debug vs. release builds**: no test/mock financial data, hardcoded secrets, or verbose sensitive logging may ship in a release build (reinforces FR-P1-016 and §2.5).
- **Secrets management**: no API keys, credentials, or environment-specific values are hardcoded in source; they are sourced via secure build configuration (exact mechanism — e.g., Gradle properties not committed to version control — is a build-tooling detail, not fixed here).
- **Network security (Phase 5+)**: TLS-only communication for sync; no networking dependency exists before Phase 5 besides an optional external AI call if a cloud provider is later chosen for Phase 4 (§4/§9.4).
- **AI data boundaries**: any data sent to an external AI provider (if one is ever chosen) must be the minimum necessary for the specific request — no bulk transaction history sent speculatively, no credentials or device identifiers included. Exact data-minimization implementation is deferred alongside the AI-provider decision itself (§11).
- **Backup security** (Phase 5): backups, where introduced, must be encrypted, consistent with local-storage encryption expectations.

---

## 25. Accessibility — Measurable Acceptance Criteria

Expands §7.4 with testable criteria. Visual/styling specification remains in `DESIGN_SYSTEM.md`.

- **TalkBack/screen reader**: every interactive element (buttons, list items, form fields) has a non-empty, meaningful content description; decorative-only elements are marked so screen readers skip them.
- **Touch targets**: interactive elements meet or exceed Android's recommended minimum touch-target size (48dp) — exact value to be enforced at the design-system level, referenced here as a requirement, not redefined.
- **Dynamic font scaling**: text respects the system font-scale setting up to at least the platform's standard maximum scale without clipping or overlapping content.
- **Contrast**: text and meaningful UI elements meet WCAG AA contrast ratios at minimum.
- **Focus order**: keyboard/switch-access focus order follows a logical reading order matching the visual layout.
- **Error announcements**: validation and error states (§17) are announced to screen readers when they appear, not only shown visually.
- **Charts/graphs**: every chart/graph (dashboard, analytics) has a non-visual equivalent (e.g., an accessible summary or data table) — no financial insight is conveyed by color alone.
- **Reduced motion**: users with reduced-motion system settings enabled see non-essential animations reduced or removed; no functionality depends on an animation completing.

---

## 26. Relationship Between Project Documents

- **`PARADOX_MASTER_PRD.md`** — product goals, scope, and user-facing product requirements ("why" and "what the product does").
- **This document (`PARADOX_ANDROID_SRS.md`)** — technical/software requirements and implementation specification ("how it's built"); this remains the **master technical specification** for the project.
- **`DESIGN_SYSTEM.md`** — visual/UI/UX design rules (colors, typography, spacing, component styling). This SRS references design-dependent requirements (accessibility contrast/touch targets, navigation modal styling) without duplicating the design system's content.
- **`AGENTS.md`** — coding-agent/project execution rules for how an AI coding agent (e.g., Google Antigravity) should operate within this codebase.

Where this SRS and another document appear to overlap, this SRS's *technical/behavioral* requirements take precedence; *visual* specification always defers to `DESIGN_SYSTEM.md`.

---

## 27. Definition of Done

A requirement or feature is considered done when:
- The functional requirement(s) it addresses are implemented and traceable (§31).
- Applicable validation rules (§16) are implemented and enforced.
- Required Loading/Empty/Success/Error/Offline/Permission-Denied/Partial-Data states (§7, §17) are handled, not just the happy path.
- Accessibility criteria (§25) relevant to the screen are met.
- Security requirements relevant to the feature (§7.1, §12, §24) are satisfied.
- Unit, database, and (where applicable) UI/security tests are written and passing (§10, §31).
- Any database migration introduced is tested (§15).
- Profile isolation is verified for any new `profileId`-scoped data (§3.2).
- No fake, seeded, or hardcoded financial data is present (FR-P1-016).
- No hardcoded secrets, credentials, or environment-specific configuration values are introduced (§24).
- No financial data, credentials, or AI request/response content appears in logs (§24).
- UI matches `DESIGN_SYSTEM.md` for the relevant screens (§26).
- The build succeeds and relevant automated tests pass with no known critical regression.

---

## 28. Requirement Priority Classification

Priority is used only to aid implementation sequencing within a phase; it does not alter the phase roadmap defined in §6.

- **P0 — Critical**: profile creation/unlock and isolation (FR-P1-001–003), expense CRUD and validation (FR-P1-004–005, §16.1), financial-accuracy rules (FR-P1-012, §21), local encryption (FR-P1-017, §12.2).
- **P1 — Important**: categories/payment methods (FR-P1-006–007), ledger search/filter/sort (FR-P1-008), budgets and guardrails (FR-P1-009–010), dashboard (FR-P1-011), required UX states (FR-P1-014), destructive-action confirmation (FR-P1-015).
- **P2 — Later (Phase 2–3 core)**: native convenience integrations (§6.2), assisted capture (§6.3), duplicate guard.
- **P3 — Future/optional**: Phase 4 intelligence features beyond the core Ask Paradox grounding rules, all of Phase 5 (sync/backup/auth), and all of Phase 6.

---

## 29. Build, CI & Release Requirements

- **Debug vs. release builds**: debug builds may include additional diagnostic logging (still subject to the no-financial-data-in-logs rule, §24); release builds are the only builds considered for any real user data.
- **Build reproducibility**: dependency versions are pinned (not floating ranges) so a given commit produces a consistent build.
- **Versioning**: app version code/name incremented per release; internal database schema version tracked separately and tied to Room migrations (§15).
- **Environment configuration**: any environment-specific value (future API base URL, feature flags) is externalized from source, never hardcoded (reinforces §24).
- **Secret management**: no secrets committed to version control; sourced via secure build configuration.
- **Automated tests before release**: the test categories defined in §10 must pass before a build is considered release-ready.
- **Static analysis/linting**: standard Kotlin/Android lint and static-analysis checks run as part of the build; findings addressed or explicitly suppressed with justification, not silently ignored.
- **Database migration verification**: every Room migration is tested against a representative prior-version database before release (§15).
- **Release build validation**: a manual or automated smoke check of core flows (profile unlock, add/edit/delete expense, dashboard) before any release build is distributed.

Detailed CI pipeline implementation (specific tooling, triggers, environments) remains deferred, consistent with the existing product decision to defer launch-readiness detail (§11).

---

## 30. Use Cases

Concise format: Actor, Preconditions, Main Flow, Alternative/Failure Flow, Postconditions — included only where they add clarity beyond the functional requirement table.

**Create Profile**
- Actor: New user.
- Preconditions: App installed, no profile selected.
- Main Flow: User initiates profile creation → sets a name → configures a primary credential (§12.1) → optionally enables biometric → profile is created and set active.
- Failure Flow: Credential setup fails validation → user is prompted to correct it; no profile is created until a valid credential exists.
- Postconditions: A new, isolated profile exists; user is `UNLOCKED` in that profile.

**Unlock Profile**
- Actor: Returning user.
- Preconditions: Profile exists, state is `LOCKED`.
- Main Flow: User provides primary credential or biometric → `AUTHENTICATING` → success → `UNLOCKED`.
- Alternative Flow: Biometric fails/unavailable → fallback to primary credential (§12.4).
- Failure Flow: Credential incorrect → generic error, remains `LOCKED`, subject to lockout policy (§12.4).
- Postconditions: User has access to that profile's data only.

**Lock Application**
- Actor: Active user, or the system (background trigger).
- Main Flow: User manually locks, or app is backgrounded/times out → state transitions to `LOCKED` (§12.5).
- Postconditions: No protected screen is accessible without re-authentication.

**Switch Profile**
- Actor: User with multiple profiles.
- Main Flow: User selects a different profile from profile selection → previous profile's in-memory state is cleared → new profile enters `LOCKED` → unlock flow proceeds as above.
- Postconditions: No data from the previous profile remains visible or accessible.

**Add Expense**
- Actor: Unlocked user.
- Main Flow: User opens Add Expense (manual or via assisted capture) → enters/confirms fields → validation (§16.1) passes → expense is saved.
- Failure Flow: Validation fails → inline errors shown, expense not saved; or save fails at the database layer → error shown per §17, entered data retained for retry.
- Postconditions: Expense exists, scoped to the active profile; dashboard/ledger reflect it immediately (reactive Flow update, §3.3).

**View / Edit / Delete Expense**
- Standard CRUD flows on an existing, profile-scoped Expense; Edit re-runs the same validation as Add; Delete requires confirmation (FR-P1-015) and is a hard delete (§22).

**Search / Filter / Sort Expenses**
- Actor: Unlocked user, on the ledger screen.
- Main Flow: User applies any combination of text/date-range/category/amount-range/payment-method filters and a sort order → ledger updates reactively.
- Postconditions: Displayed list reflects only expenses matching the active filters, scoped to the active profile.

**Create Budget / View Budget Status**
- Main Flow: User defines a budget (type, amount, optional category) → system tracks spend against it and surfaces On Track/Near Limit/Over Budget status (FR-P1-009) on the dashboard/budget screen.

**View Dashboard**
- Main Flow: User navigates to the dashboard → system loads real stored data (never fabricated, FR-P1-016) and renders totals, budget status, recent expenses, and trend.
- Failure Flow: No data yet → Empty state shown, not an error.

**Handle Offline Mode**
- Main Flow: User uses core features with no connectivity → all Phase 1–4 core functions behave identically to online use; any sync-dependent (Phase 5+) UI shows an offline indicator rather than failing silently.

**Export Data**
- Main Flow: User requests export → system generates CSV/PDF from real stored data → file is made available via Android's standard sharing/storage mechanisms.
- Failure Flow: Export fails → error per §17, no partial/corrupt file left in a user-visible location.

**Backup / Restore** (Phase 5, when applicable)
- Main Flow: User initiates backup → encrypted backup created; restore requires explicit confirmation of replace-vs-merge behavior (§22) before proceeding.

**Scan Receipt** (Phase 3)
- Main Flow: User captures/selects an image → OCR extraction → editable preview shown → user confirms or edits → expense saved.
- Failure Flow: OCR fails/partial → preview shown with blank/uncertain fields for manual completion (§18).

**Voice Expense Entry** (Phase 3)
- Main Flow: User speaks an expense → speech-to-text → same extraction/preview/confirm pipeline as Quick Add.
- Failure Flow: Recognition fails/unavailable → fallback to manual entry.

**Ask Paradox** (Phase 4)
- Main Flow: User asks a financial question → system answers using only real stored data and deterministic calculations, clearly labeling any estimate (FR-P4-001, FR-P4-008).
- Failure Flow: AI unavailable/timeout → generic unavailability message, manual path (browsing the ledger/dashboard directly) remains fully available.

---

## 31. Requirements Traceability Matrix

Covers the Phase 1 capabilities explicitly required for traceability; later-phase features are traceable via their FR-IDs (§6.2–§6.6) and the corresponding use cases in §30 once they reach implementation.

| User Story / Use Case | Functional Requirement | Acceptance Criteria (Given/When/Then) | Test Coverage |
|---|---|---|---|
| Create Profile | FR-P1-001, FR-P1-002 | Given no active profile, when the user completes primary-credential setup, then a new isolated profile exists and the app enters `UNLOCKED` for it. | Unit (credential validation), DB (profile row created), UI (onboarding flow) |
| Switch Profile | FR-P1-001, FR-P1-003 | Given multiple profiles exist, when the user selects a different profile, then the app enters `LOCKED` for that profile and no data from the previous profile is visible. | DB (isolation test), UI (switch flow), Security (cross-profile leak test) |
| App Lock | FR-P1-002, §12 | Given the app is `UNLOCKED`, when it is backgrounded past the defined trigger, then the app returns to `LOCKED` and no protected screen is reachable without re-authentication. | Unit (state machine), Security (bypass attempt test) |
| Authentication | FR-P1-002, §12 | Given a `LOCKED` profile, when the user enters a correct primary credential (or successful biometric), then the app transitions to `UNLOCKED`; given an incorrect credential, then a generic error is shown and the app remains `LOCKED`. | Unit (auth logic), Security (bypass/lockout test) |
| Add Expense | FR-P1-004, FR-P1-005 | Given valid required fields, when the user saves, then the expense is persisted, scoped to the active profile, and reflected immediately on the dashboard/ledger. | Unit (use case), DB (DAO), UI (add-expense flow) |
| Edit Expense | FR-P1-004 | Given an existing expense, when the user edits and saves valid changes, then the record is updated (`updatedAt` changes) without altering `createdAt` or unrelated fields. | Unit, DB |
| Delete Expense | FR-P1-004, FR-P1-015 | Given an existing expense, when the user confirms deletion, then the record is hard-deleted and no longer appears in any query for that profile. | DB, UI (confirmation flow) |
| Expense Validation | FR-P1-005, §16.1 | Given an amount ≤ 0 or a future date, when the user attempts to save, then the save is blocked with an inline, field-specific error. | Unit (validation rules) |
| Categories | FR-P1-006, §15.3 | Given a category with existing expenses, when the user attempts to delete it, then deletion is blocked until expenses are reassigned. | Unit, DB (constraint test) |
| Payment Methods | FR-P1-007, §15.4 | Given a payment method with existing expenses, when the user attempts to delete it, then deletion is blocked until expenses are reassigned. | Unit, DB |
| Search/Filter/Sort | FR-P1-008 | Given a set of expenses, when the user applies a combination of filters and a sort order, then only matching expenses appear, correctly ordered, scoped to the active profile. | UI, Unit (query logic) |
| Budgets | FR-P1-009, FR-P1-010, §16.3 | Given a defined budget, when spend crosses the configured threshold, then the budget status updates to Near Limit/Over Budget accordingly, using exact decimal arithmetic. | Unit (budget calculation), UI |
| Dashboard | FR-P1-011 | Given real stored expense/budget data, when the user opens the dashboard, then all figures shown are derived from that real data — never hardcoded or fabricated. | UI, Unit (no-fake-data check) |
| Offline Operation | FR-P1-013 | Given no network connectivity, when the user performs any core expense/budget action, then the action completes exactly as it would online. | UI (offline-mode test) |
| Database Encryption | FR-P1-017, §12.2, §24 | Given the local database file, when inspected outside the app without the correct key, then its contents are not readable in plaintext. | Security test |
| Profile Data Isolation | FR-P1-003, §3.2 | Given two profiles with data, when queries are executed for Profile A, then no data belonging to Profile B is ever returned, at the data-layer query level (not just UI filtering). | DB (isolation test) — maps directly to the PRD's critical zero-leakage metric |
| Destructive Actions | FR-P1-015 | Given a destructive action (delete expense/category/profile) is initiated, when the user has not yet confirmed, then the action has not been performed; confirmation is required to proceed. | UI (confirmation flow) |
| Error Handling | §17 | Given any error category in §17, when it occurs, then the user sees the defined generic/appropriate message and no sensitive detail is exposed, while the error is logged without sensitive content. | Unit, UI (error-state rendering), Security (log-content check) |

---


**Document Status:** Revision 1.1 — expanded with authentication/app-lock specification, navigation and permission requirements, implementation-level data constraints, validation rules, error taxonomy, edge cases, Android lifecycle and WorkManager requirements, an expanded money/currency specification, data-lifecycle and attachment-storage rules, an expanded security/privacy model, measurable accessibility criteria, use cases, a requirements traceability matrix, a Definition of Done, requirement priority classification, and build/release requirements. All Phase 1–6 content, technology stack, and folder structure from Revision 1.0 are preserved unchanged except FR-P1-002, which was extended (not contradicted) to reflect the broader authentication model in §12. This remains the master technical specification for the project; `DESIGN_SYSTEM.md` and `AGENTS.md` remain separate per §26.
