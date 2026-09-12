# Paradox — Native Android Product Requirements Document (PRD)

**Document Version:** 2.1  
**Status:** Consolidated Product Definition (Updated with Debts & Udhaar Khata, Dynamic Month Selector, Wallets Net Balance Hero, & Multilingual Localization)  
**Product:** Paradox Native Android App  
**Platform:** Android  
**Document Type:** Master Product Requirements Document

> This document consolidates the strongest product requirements from the reviewed Paradox, FinTrack, HisabKitab, BudgetBrain, and Spendora specifications. Conflicting implementation choices are resolved in favor of a simple, native, privacy-first, local-first product.

---

## 1. Product Overview

Paradox is a native Android personal finance and financial-intelligence application designed to make recording, organizing, understanding, and improving personal finances simple, fast, private, accurate, and intelligent.

Paradox is a **new standalone Android project**. It must not depend on, reuse, or assume the existing Paradox web application's backend, API, database, authentication implementation, or technical architecture.

The product combines:
- Paradox's simplicity, privacy, AI-assistant, native Android, progressive-disclosure, offline-resilience, and user-control principles.
- FinTrack's disciplined MVP, local profiles, explicit scope boundaries, real-data requirement, and Run → Test → Deploy discipline.
- HisabKitab's fast logging target, budget guardrails, multilingual direction, and assisted-entry pipeline.
- BudgetBrain's local-first security, encrypted storage, hybrid AI/BYOK direction, performance expectations, and native Android stack.
- Spendora's income/cash-flow, Safe-to-Spend, savings goals, Financial Health Score, Leak Hunter, transaction review, and proactive intelligence.

These ideas are consolidated rather than copied feature-for-feature. Advanced features are phased so the product remains simple.

---

## 2. Product Vision & Principles

### Vision
**Paradox is an AI-first native Android financial companion that helps users record money activity quickly, understand where their money goes, stay within limits, and make better spending decisions.**

### Principles
1. Simplicity over feature count.
2. Privacy first.
3. Financial accuracy.
4. Real user data only.
5. Native Android first.
6. AI as an assistant, not an authority.
7. User control and confirmation.
8. Offline resilience.
9. Graceful degradation.
10. Progressive disclosure.
11. No unnecessary complexity.
12. Free/local-first core functionality where practical.

---

## 3. Problem Statement & Goals

People often stop tracking expenses because manual entry is tedious. Spending happens through cash, UPI, cards, bank transfers, wallets, subscriptions, and online purchases. Even when users record transactions, they still have to interpret the data themselves.

Paradox should help answer:
- How much did I spend this month?
- Where did my money go?
- Am I within my budget?
- How much can I safely spend today?
- Why was this month more expensive?
- Which recurring expenses are costing me money?
- Can I afford a planned purchase?
- How much am I saving?
- Am I progressing toward my savings goal?
- What spending habits should I change?

### Goals
- Make expense recording fast.
- Make financial information understandable.
- Provide accurate budgeting and calculations.
- Protect sensitive financial data.
- Support reliable offline core usage.
- Reduce manual entry through assisted capture.
- Provide useful intelligence without overwhelming users.
- Make the app feel genuinely native to Android.
- Help users act before overspending becomes a problem.

---

## 4. Users & Product Boundaries

### Primary users
- Individuals tracking personal expenses.
- Budget-conscious users.
- Users who dislike repetitive entry.
- UPI/card/wallet/cash users.
- Users seeking visual financial insight.
- Users wanting AI-assisted understanding.
- Users with savings goals.
- Users with variable income.

### Not the target
- Corporate accounting.
- Payroll.
- Enterprise finance/RBAC.
- Banking services.
- Investment brokerage or stock trading.
- Lending.
- Tax filing.
- Public/social finance.

Shared/family financial spaces may be considered later; private financial data remains the default.

---

## 5. Core Experience

### Core loop
**Create/Select Profile → Unlock → Record Activity → See Immediate Financial Impact → Review Budget/Safe-to-Spend → Understand → Adjust**

The first four steps must remain extremely simple.

### Assisted-entry rule
All assisted capture follows:

**Capture → Extract → Validate → Preview → User Confirmation → Save**

AI or OCR must never silently create uncertain financial records.

---

## 6. Profiles, Authentication & Privacy

Paradox uses a **local-first private profile model** for the core experience.

Each profile owns its:
- Expenses
- Income
- Categories
- Payment methods/accounts
- Budgets
- Savings goals
- Financial preferences
- AI preferences
- Local settings

Every user-owned record must be scoped to the active profile.

### Profile isolation
The data layer must enforce profile ownership. UI filtering alone is insufficient. A profile must never read another profile's financial data or AI context.

### Unlock
- PIN.
- Android biometric authentication.
- Device credential fallback where appropriate.

Biometric matching must use Android's official APIs; raw biometric information must never be accessed by the app.

### Future account authentication
When cross-device sync is justified, the product may add:
- Email/password.
- Google Sign-In.
- Password recovery.
- Passkeys.
- Session management.
- Remote logout.

This keeps cloud identity from unnecessarily complicating the local MVP.

---

## 7. Expense Management

Users must be able to:
- Create, view, edit, and delete expenses.
- View details and history.
- Search, filter, and sort.

### Expense fields
- Expense ID
- Profile ID
- Title
- Amount
- Currency
- Category
- Payment method/account
- Expense date
- Notes/description
- Recurring status
- Source
- Created timestamp
- Updated timestamp
- Optional attachment/reference

### Sources
Manual, voice, receipt scan, screenshot, share sheet, import, SMS candidate, or AI-assisted entry.

### Validation
- Amount must be positive.
- Invalid/future dates are rejected unless a future-transaction mode is later introduced.
- Required fields are validated.
- Financial values use exact/appropriate decimal representation.
- User-entered values are never silently changed.

---

## 8. Fast Entry & Capture

### Manual
Reliable baseline. Target under 30 seconds initially, with common entry progressively optimized toward 15 seconds.

### Natural-language Quick Add
Example: “Uber 240 cash yesterday”.

Extract title/merchant, amount, date, payment method, category, and notes where available. Show an editable preview before save.

### Voice
Speech-to-text converts spoken expenses into a candidate transaction and then uses the same validation/confirmation flow.

### Receipt/Bill Scanner
Camera or selected image → OCR/extraction → preview → edit → confirm → save.

Possible extraction:
- Merchant
- Amount
- Date
- Currency
- Payment method
- Category
- Relevant text

### Screenshot / Share to Paradox
Payment confirmations, screenshots, receipts, invoices, and supported text can create a candidate transaction. User confirmation is required.

---

## 9. Categories, Payment Methods & Ledger

### Categories
- Starter categories.
- User-created categories.
- Rename.
- Safe deletion/reassignment.
- AI suggestions.
- User override.
- Optional AI-assisted category creation.

Example starter categories: Food, Transport, Housing, Bills, Shopping, Entertainment, Healthcare, Education, Other.

### Payment methods & Wallets / Accounts Model
Paradox provides a comprehensive multi-account wallet architecture:
- **Account Types Supported**: Cash, Bank Account, Debit/Credit Card, Digital Wallet (UPI/Paytm/GPay), Savings Vault, and Custom Account types.
- **Net Balance Hero Card**: A prominent overview card summarizing total combined financial net worth across all active accounts with dynamic liquidity metrics.
- **Account Customization**: Customizable color palettes (HEX/Material themes), custom icons, and primary/default account designations.
- **Account Ledger & Transfers**: Real-time balance calculations derived strictly from reconciled income, expenses, debt repayments, and internal account-to-account transfers. The app must never claim an unverified account balance.
- **UI & Ergonomics**: Spacious, readable 20dp padding cards, elevated balance counters, and quick add/edit bottom sheets.

### Ledger
Support combined:
- Text search.
- Date-range filter.
- Category filter.
- Amount-range filter.
- Payment-method / Account filter.
- Date/amount/category sorting.

Example: Search Uber + Transport + This Month + Highest Amount.

---

## 10. Income & Cash Flow

Income is part of the consolidated long-term product model and should be introduced after the core expense loop is stable.

Income types:
- Salary.
- Freelance.
- Business/personal earnings.
- Other income.

Paradox may calculate:
- Total income.
- Total expenses.
- Net cash flow.
- Savings amount.
- Savings rate.

All values must be based on actual recorded data.

---

## 11. Budgeting & Financial Guardrails

### Budget types
- Daily.
- Weekly.
- Monthly.
- Yearly.
- Category-specific.

### Status
- **On Track**
- **Near Limit**
- **Over Budget**

A threshold around 80% may be used as the default beginning of Near Limit, subject to final implementation.

### Guardrails
**Soft:** warn when approaching a limit or when the spending pace predicts an overrun.

**Strict:** when explicitly enabled, prevent a transaction that would exceed a defined limit. Explain the reason and let the user change the relevant setting.

Budget calculations must be deterministic and financially precise.

---

## 12. Dashboard, Dynamic Month Selector, Analytics & Reports

### Dashboard
Prioritize clarity over information density.

**Dynamic Month & Period Selector**:
- Interactive dropdown menu in the dashboard header supporting navigation across the past 12+ months (e.g., current month, previous months, specific custom year-months).
- Instant, reactive recalculation of all dashboard metrics upon selecting a different month without requiring network calls or full app reloads.

**Core Metrics Scoped to Selected Month**:
- Total spending in the selected period and comparison against the prior period.
- Active budget progress, remaining allowance, and budget health indicator.
- Spending velocity chart (daily spend bars with dynamic average pace lines).
- Top category breakdown with progress indicators and percentage allocations.
- Recent transaction ledger preview for the selected timeframe.
- Safe-to-Spend daily allowance when available.

**Advanced Metrics**:
- Income vs. Expense net cash flow.
- Savings rate and goal contribution pace.
- Financial Health Score (0–100).
- Recurring commitments & subscription alerts.
- AI observations and spending trend anomaly detection.

### Analytics
- Daily/weekly/monthly summaries.
- Category distribution and drill-down.
- Spending trends and velocity.
- Historical comparisons across selected months.
- Month-over-month percentage changes.
- Average daily spending and burn rate.
- Top categories and budget adherence.

### Monthly report
May include income, expenses, net savings, savings rate, budget adherence, categories, recurring commitments, major changes, and AI observations. Export may support CSV, PDF, and spreadsheet-compatible formats.

---

## 13. Safe-to-Spend

Safe-to-Spend answers:

> **How much can I safely spend today?**

It may consider:
- Remaining budget.
- Current spending.
- Remaining days.
- Upcoming known expenses.
- Financial buffer.
- Savings commitments.
- User-defined constraints.

Possible states:
- Healthy
- Moderate
- Caution
- Danger

The result is an estimate based on recorded information, not a guarantee. The calculation should be deterministic and documented in technical specifications.

---

## 14. Savings Goals

Users can create:
- Emergency fund.
- Vacation.
- Gadget.
- Vehicle.
- Education.
- Custom goal.

Goal data:
- Goal ID.
- Name.
- Target amount.
- Current saved amount.
- Currency.
- Target date.
- Contributions.
- Withdrawals.
- Progress.
- Expected completion date.

Paradox may calculate required saving pace, current pace, estimated completion, gap to target, and possible acceleration opportunities. Recommended changes remain optional.

---

## 15. Recurring Expenses & Leak Hunter

Recurring expenses may support weekly, monthly, and yearly frequencies.

Provide:
- Recurring visibility.
- Upcoming payment awareness.
- Subscription visibility.
- Monthly normalized commitment.
- Annualized recurring cost.

### Leak Hunter
Identify:
- Recurring subscriptions.
- Repeated small purchases.
- Micro-spending.
- High-frequency merchants.
- Potential unnecessary spending where evidence supports it.

Clearly distinguish observed facts, patterns, AI inference, and recommendations. Never claim a subscription is unused without evidence.

---

## 16. Financial Health Score

A 0–100 Financial Health Score may use five pillars:
1. Savings Discipline
2. Budget Adherence
3. Spending Stability
4. Cash Cushion
5. Leak Control

The score must be explainable. Show what affected it, healthy areas, attention areas, and up to three practical actions.

It is an informational product metric, not professional financial advice.

---

## 17. Purchase Simulator

Users can ask:

> “Can I afford a ₹20,000 phone this month?”

Evaluate the hypothetical purchase against:
- Current spending.
- Remaining budget.
- Safe-to-Spend.
- Upcoming known expenses.
- Savings goals.
- Current recorded financial position.

Results:
- **Safe to Buy**
- **Proceed with Caution**
- **Delay Purchase**

The simulation must never automatically create the expense.

---

## 18. Debts & Udhaar Management (Khata System)

Paradox provides a complete peer-to-peer debt and credit ledger (Udhaar / Khata):
- **Lent vs. Borrowed Tracking**:
  - **Lent (You gave / Receivables)**: Money given to contacts, friends, family, or vendors.
  - **Borrowed (You took / Payables)**: Money borrowed from peers, lenders, or institutions.
- **Native Contact Picker Integration**:
  - Direct integration with Android contact picker (`PickContact` / `ContactsContract`) to seamlessly select contact names and phone numbers without manual typing.
  - Safe permission handling and graceful fallback allowing immediate manual name/phone entry if contacts permission is denied or unavailable.
- **Repayment Tracking (`RepaymentEntity`)**:
  - Support for partial repayments, milestone installments, and full lump-sum settlements.
  - Dynamic recalculation of remaining balance (`initialAmount - sum(repayments)`).
  - Explicit status: **Active** (open balance) vs. **Settled** (fully repaid).
- **Due Dates, Notes & Categories**:
  - Optional settlement due dates with automated reminder triggers.
  - Contextual notes and categorization for auditability.
- **Ergonomics & Modal Design System**:
  - Spacious Material 3 bottom sheets for recording debts and repayments with generous touch targets (20dp padding).
  - Quick amount selector chips (`+₹100`, `+₹500`, `+₹1000`, `+₹2000`).
  - Native Material 3 Date Picker integration.

---

## 19. Ask Paradox — AI Assistant

Ask Paradox is the central conversational AI surface.

Examples:
- “How much did I spend on food this month?”
- “Why was this month more expensive?”
- “How much can I safely spend today?”
- “What was my biggest expense?”
- “Show my subscriptions.”
- “Am I on track for my emergency fund?”
- “Can I afford ₹3,000 for dinner tonight?”
- “Summarize my finances this month.”

### Grounding
AI answers must use actual stored records, deterministic calculations, known settings, and clearly stated assumptions.

AI must never fabricate transactions, balances, income, budgets, savings, categories, or history.

### Action safety
Ask Paradox is read-only by default. Consequential actions require explicit confirmation. AI must state uncertainty and provide a manual fallback.

---

## 20. AI Intelligence Suite

### Faster logging
- Quick Add.
- Voice parsing.
- Receipt OCR.
- Smart categorization.
- Duplicate warnings.

### Understanding
- Financial Health Score.
- Burn-rate analysis.
- Budget forecasting.
- Spending trends.
- Anomaly detection.
- Predictive spending.

### Planning
- Safe-to-Spend.
- Purchase Simulator.
- Adaptive budgets.
- Savings planner.
- Goal runway.
- 50/30/20 comparison.

### Financial cleanup
- Subscription detection.
- Leak Hunter.
- Recurring detection.
- CSV/import assistance.
- SMS transaction candidates where technically and policy permitted.

AI features must not block normal expense tracking.

---

## 21. Duplicate Guard & Import

### Duplicate Guard
Use signals such as amount, merchant/title, date proximity, payment method, and source/reference. Warn rather than automatically delete/reject.

### CSV import
1. Detect columns.
2. Parse records.
3. Validate.
4. Detect likely duplicates.
5. Suggest categories.
6. Show preview.
7. Confirm before commit.

### SMS transaction detection
Where supported and policy-compliant:
SMS → candidate extraction → mask sensitive fields → review → Confirm/Edit/Ignore → Save.

Full card/account numbers and unnecessary balance information must not be retained.

---

## 22. Native Android Experience

Use Android capabilities only when they genuinely improve the product.

### Camera
Receipt/bill capture and OCR.

### Microphone
Voice expense entry and voice Ask Paradox.

### Biometrics
Unlock, re-authentication, sensitive actions.

### Notifications
Budget warnings, recurring reminders, Safe-to-Spend warnings, savings milestones, transaction review prompts, and important insights. User-configurable.

### Widget
Safe-to-Spend, budget progress, current spending, and Quick Add. Must use live data.

### Shortcuts
Add Expense, Scan Receipt, Ask Paradox.

### Share Sheet
Send supported text/images directly into the assisted-entry pipeline.

### Quick Settings
Future one-tap access to Add Expense.

### Haptics
Optional feedback for successful scans, confirmations, and milestones.

### Wear OS / Android Auto
Future consideration only.

---

## 23. Offline-First & Synchronization

Core financial operations should not require continuous internet access.

Offline:
- Add/edit/delete expenses.
- View local data.
- Manage categories.
- Manage budgets.
- Record income once available.
- Manage savings data once available.
- Manage debts, repayments, and wallets.

If AI requires connectivity:
**AI unavailable → Explain → Manual fallback**

### Sync
Cloud synchronization is optional and should be introduced when cross-device use or recovery is a validated product need.

When sync exists:
- Queue local changes safely.
- Resume after connectivity returns.
- Prevent duplicates.
- Handle conflicts deterministically.
- Show sync status.
- Never silently lose data.

---

## 24. Backup, Export & Permissions

Later versions may provide encrypted backup/restore and CSV/PDF/spreadsheet exports. Backup is distinct from sync and must be clearly communicated.

Potential permissions:
- Contacts (for Debt contact selection; optional with manual fallback).
- Camera (for receipt scanning).
- Microphone (for voice entry).
- Notifications.
- SMS only where supported and policy-compliant.
- Location only for an explicitly enabled location-aware feature.

Flow:
**Explain why → Request only when needed → Handle denial gracefully**

Location must never be required for ordinary expense tracking.

---

## 25. Privacy & Security Requirements

Mandatory principles:
1. No plaintext PIN/password storage.
2. Secure credential handling.
3. Encrypted local financial data (SQLCipher).
4. Strict profile/data isolation.
5. Minimize financial data transmission.
6. Privacy-conscious AI access.
7. Discard sensitive SMS information when no longer required.
8. Confirm destructive actions.
9. Protect sensitive screens where appropriate.
10. Request permissions only when required.
11. Permission denial must not break unrelated features.
12. No fake financial data in production.
13. No hidden background financial collection.
14. User controls optional integrations.

---

## 26. Localization, Currency & Accessibility

### Localization & In-App Language System
Paradox features an in-app language switcher supporting regional and conversational formats:
- **Supported Languages**:
  - **English (`en`)**: Clean standard financial terminology.
  - **Hindi (`hi`)**: Native Devanagari script for broader reach.
  - **Marathi (`mr`)**: Native Marathi script with localized idioms.
  - **Hinglish / Minglish (`hi-Latn` / conversational)**: Casual Latin-script Hindi/Marathi mix for natural AI conversation and financial tracking.
- **Persistence**: Selected language preference is persisted locally via `SessionDataStore` and dynamically applied across Compose UI lifecycles.
- **Future Expansion**: Gujarati, Marwadi, German, Spanish, French, and additional demand-driven languages.

### Currency
Initial priorities: INR, USD, EUR, GBP.

Requirements:
- Preferred display currency.
- Original transaction currency retained.
- Display and transaction currencies separated.
- Exact/appropriate financial arithmetic.
- Live conversion optional.
- Cached exchange rates clearly dated/sourced.

Mixed-currency totals must not be presented as exact when required rates are unavailable.

### Accessibility
- TalkBack/screen readers.
- Meaningful semantics.
- Adequate touch targets.
- Dynamic text.
- Contrast.
- Accessible charts/summaries.
- Accessible errors.
- Reduced-motion consideration.

---

## 27. Recommended Technical Direction

The product must not inherit the existing web application's implementation.

Recommended native direction:
- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Architecture:** Layered / MVVM-style
- **Local database:** Room
- **Encryption:** SQLCipher or another approved encrypted-storage approach
- **Key management:** Android Keystore / StrongBox where available
- **Biometrics:** BiometricPrompt
- **Google identity where required:** Credential Manager
- **Camera:** CameraX
- **OCR:** ML Kit or equivalent on-device solution
- **Voice:** SpeechRecognizer / appropriate on-device speech capability
- **Background work:** WorkManager
- **Widgets:** Jetpack Glance
- **AI:** Prefer local/on-device where practical; external AI optional
- **Optional backup:** User-controlled cloud storage
- **Networking:** Only for features that require it

Final architecture, API contracts, database schema, testing strategy, project structure, and deployment details belong in the technical specification.

---

## 28. Non-Functional Requirements

### Security
Secure credentials, encrypted local data, Keystore-backed keys, official biometric APIs, appropriate screen protection, secure network configuration when networking exists, and permission minimization.

### Performance
Core local operations should feel immediate. Dashboard calculations should not require network access. Assisted capture should provide timely feedback. Validate targets on representative mid-range Android devices.

### Reliability
- Atomic local operations.
- No silent data loss.
- Safe recovery after interruption.
- Clear offline state.
- Deterministic calculations.
- Graceful feature failure.

### Responsiveness
Support a practical range of Android phones, with foldable/tablet layouts where useful.

### Accessibility
Accessibility requirements apply throughout the product, not only to a dedicated settings screen.

---

## 29. Data Integrity & UX Rules

### No hardcoded/dummy financial data
Production financial information must come from:
- User input.
- Real local records.
- Approved imported data.
- Approved external data.
- Deterministic calculations.
- Clearly labeled AI observations.

Prohibited:
- Hardcoded dashboard totals.
- Fake expenses.
- Fake chart values.
- Fake budget balances.
- Fake AI history.
- Static widget numbers.

Starter categories are allowed as configuration; fake user activity is not.

### Required UX states
Important screens must support:
- Loading.
- Empty.
- Success.
- Error.
- Offline.
- Permission denied.
- Partial data.
- AI unavailable.
- Low-confidence AI.
- Sync pending/failure.
- Destructive confirmation.

---

## 30. Roadmap

### Phase 1 — Core Foundation / MVP
- Native Android app.
- Local private profiles.
- PIN and biometric unlock.
- Strict data isolation.
- Expense CRUD.
- Dynamic categories.
- Payment methods & initial accounts.
- Search/filter/sort.
- Dashboard and core charts.
- **Dynamic Month Selector (Past 12+ months dropdown with reactive recalculations)**.
- Monthly/category budgets.
- Exact calculations (`BigDecimal`).
- Offline core usage.
- Empty/loading/error states.
- Basic security hardening (Keystore + SQLCipher).
- **In-App Multilingual Localization (English, Hindi, Marathi, Hinglish/Minglish)**.
- No hardcoded financial data.

### Phase 2 — Native Convenience & Financial Foundation
- **Debts & Udhaar Management System (Lent/Borrowed, Contact Picker, Repayments, Settlements)**.
- **Wallets & Accounts Model (Net Balance Hero Card, multi-account types, custom colors)**.
- Income and cash flow.
- Recurring expenses & Subscription tracking.
- Savings goals.
- Widget (Jetpack Glance).
- Shortcuts.
- Share Sheet.
- Notifications.
- Quick Settings where appropriate.
- Export (CSV/PDF).
- Theme support (Material 3 Dark/Light).
- Improved responsive layouts.

### Phase 3 — Assisted Capture
- Receipt OCR.
- Voice entry.
- Natural-language Quick Add.
- Smart categories.
- Duplicate guard.
- Screenshot-to-expense.
- Import preview.
- SMS candidates where permitted.

### Phase 4 — Financial Intelligence
- Ask Paradox.
- Safe-to-Spend.
- Burn-rate and forecasts.
- Financial Health Score.
- Anomaly detection.
- Predictive spending.
- Adaptive budgets.
- Leak Hunter.
- Purchase Simulator.
- Savings planner.
- Goal runway.
- 50/30/20 comparison.
- AI summaries.

### Phase 5 — Sync, Backup & Advanced Personalization
- Cross-device sync when justified.
- Account authentication.
- Encrypted backup/restore.
- Passkeys.
- Advanced reports.
- More localization.
- Advanced notifications/themes.

### Phase 6 — Future Expansion
- Shared budgets.
- Split expenses.
- Family spaces.
- Bank/account aggregation.
- Advanced forecasting.
- Wear OS.
- Android Auto.
- Deeper assistant integration.
- Advanced savings automation.

---

## 31. Optional Engagement Features

Lower-priority possibilities:
- Monthly financial digest / Wrapped.
- Logging streaks.
- Achievements and milestones.
- Financial vibe indicator.
- Optional playful commentary.
- Emotion/spending analysis.

These must never shame users, encourage unhealthy spending, or compromise clarity. They must be optional.

---

## 32. Success Metrics

### Core
- Onboarding completion.
- Weekly expense logging.
- Expense-entry time.
- 30-day retention.
- Search/filter usage.
- Budget setup rate.
- Budget adherence.

### Assisted capture
- Percentage of expenses logged through assisted methods.
- OCR success.
- Voice parsing success.
- Category suggestion acceptance.
- Duplicate detection quality.

### Intelligence
- Ask Paradox usefulness.
- Safe-to-Spend usage.
- Purchase Simulator usage.
- Savings goal progress.
- Forecast usefulness.

### Reliability
- Crash rate.
- Data-loss incidents.
- Sync failures when sync exists.
- Cross-profile data leakage incidents.

### Critical security metric
**Zero confirmed cross-profile/cross-account financial data leakage incidents.**

---

## 33. Definition of Done

### MVP
- Private local profile creation/unlock works.
- PIN and supported biometric unlock work.
- Profile isolation is verified by tests.
- Expense CRUD works.
- Categories can be created/renamed/safely deleted.
- Payment methods can be recorded.
- Search/filter/sort work together.
- Dashboard uses real stored data.
- At least two useful visualizations work.
- Budget and remaining balance are accurate.
- Core expense recording works offline.
- Financial calculations are deterministic.
- Required loading/empty/error/offline states exist.
- Destructive actions require confirmation.
- No production hardcoded financial data.
- Sensitive local data is appropriately protected.
- End-to-end tests pass.
- Run → Test → Deploy is completed.

### Advanced features
A feature is complete only when it uses real data, handles failure states, has appropriate tests, preserves financial correctness and data isolation, handles uncertainty where AI is involved, has a fallback where needed, and passes the phase's Run → Test → Deploy cycle.

---

## 34. Final Product Direction

Paradox should not try to become the biggest finance application.

It should become the **most useful financial companion for everyday decisions**.

### Priority hierarchy
1. **Record accurately.**
2. **Understand clearly.**
3. **Stay within limits.**
4. **Plan intelligently.**
5. **Improve financial behavior.**

Everything else is secondary.

> **Paradox should feel simple on the surface and intelligent underneath.**