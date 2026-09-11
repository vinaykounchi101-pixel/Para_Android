# Paradox Native Android App — Technical Architecture & Implementation Log

## Architecture Summary
Paradox Android is a native, local-first, privacy-first personal finance application built with Clean Architecture:
- **Presentation**: Jetpack Compose + Material 3, Navigation Compose, ViewModels (`StateFlow`), Unidirectional Data Flow.
- **Domain**: Pure Kotlin use cases, value classes (`Money` with `BigDecimal`), repository contracts. Zero Android or Room framework dependencies.
- **Data**: Repository implementations, Room Entities & DAOs with SQL-level profile isolation (`WHERE profileId = :profileId`), bidirectional entity/domain mappers.
- **Local Storage**: SQLCipher 256-bit AES-GCM encrypted Room SQLite database (`paradox_vault.db`).
- **Security & Keystore**: Hardware-backed master key via `AndroidKeyStore`, wrapping the SQLCipher database passphrase; salted PBKDF2-HMAC-SHA256 (12,000 iterations) credential hashing; AndroidX `BiometricPrompt`.
- **Dependency Injection**: Google Hilt.

## Database Entities (Phases 1 - 6)
1. `profiles`: `id`, `name`, `primaryAuthType`, `credentialHash`, `biometricEnabled`, `createdAt`
2. `categories`: `id`, `profileId` (FK CASCADE), `name`, `iconName`, `colorHex`, `isCustom`, `isDefault`
3. `payment_methods`: `id`, `profileId` (FK CASCADE), `type`, `label`, `isCustom`
4. `expenses`: `id`, `profileId` (FK CASCADE), `title`, `money`, `categoryId` (FK RESTRICT), `paymentMethodId` (FK RESTRICT), `date`, `notes`, `recurringFlag`, `source`, `createdAt`, `updatedAt`
5. `budgets`: `id`, `profileId` (FK CASCADE), `type`, `amount`, `categoryId` (FK CASCADE), `thresholdPct`
6. `incomes`: `id`, `profileId` (FK CASCADE), `source`, `title`, `amount`, `currency`, `date`, `notes`, `createdAt`, `updatedAt`
7. `accounts`: `id`, `profileId` (FK CASCADE), `name`, `type`, `currency`, `initialBalance`, `currentBalance`, `colorHex`, `iconName`, `isDefault`, `createdAt`, `updatedAt`
8. `recurring_expenses`: `id`, `profileId` (FK CASCADE), `name`, `amount`, `currency`, `frequency`, `categoryId` (FK RESTRICT), `paymentMethodId` (FK RESTRICT), `startDate`, `nextDueDate`, `notes`, `isActive`, `createdAt`, `updatedAt`
9. `savings_goals`: `id`, `profileId` (FK CASCADE), `name`, `targetAmount`, `currentAmount`, `currency`, `targetDate`, `colorHex`, `iconName`, `createdAt`, `updatedAt`
10. `savings_contributions`: `id`, `goalId` (FK CASCADE), `profileId` (FK CASCADE), `amount`, `currency`, `date`, `notes`, `createdAt`
11. `ai_insight_logs`: `id`, `profileId` (FK CASCADE), `type`, `summary`, `contentJson`, `createdAt`
12. `sync_queue`: `id`, `profileId` (FK CASCADE), `entityType`, `entityId`, `operation`, `payloadJson`, `retryCount`, `createdAt`

## Phase 4: Financial Intelligence Architecture
- **Ask Paradox (`AskParadoxUseCase.kt`, `AskParadoxViewModel.kt`, `AskParadoxScreen.kt`)**: Context-grounded deterministic AI engine parsing natural language finance questions (*"How much did I spend on food this month?"*, *"Can I afford dinner?"*, *"What are my biggest expenses?"*) with 0% hallucination risk, computing answers directly from verified SQLCipher profile records.
- **Safe-to-Spend Engine (`CalculateSafeToSpendUseCase.kt`)**: Multi-horizon (`Today`, `This Week`, `Rest of Month`) deterministic buffer calculation subtracting remaining committed budget allocations and upcoming bills from liquid account balances.
- **5-Pillar Health Scoring (`CalculateFinancialHealthScoreUseCase.kt`)**: Holistic 100-point score evaluating Savings Discipline (20), Budget Adherence (20), Bill Regularity (20), Net Cash Flow (20), and Emergency Buffer (20).
- **Leak Hunter (`DetectSpendingLeaksUseCase.kt`)**: Algorithmic detector flagging idle subscriptions, sneaky small recurring micro-leaks (<₹100 multiple times weekly), and rapid category inflation.
- **Purchase Simulator (`SimulatePurchaseUseCase.kt`)**: Simulates prospective purchases and evaluates budget threshold overflows, category impacts, and days of safe spend remaining.

## Phase 5: Sync, Backup & Localization Architecture
- **Offline-First Sync Engine (`SyncEngine.kt`)**: Transactional outbox pattern recording insert/update/delete mutations in `sync_queue` table with retry policies and deterministic conflict resolution (server timestamp & local state merger).
- **AES-256-GCM Encrypted Vault Backup (`EncryptedBackupUseCase.kt`, `RestoreBackupUseCase.kt`)**: Password-authenticated PBKDF2 (12,000 iterations) + AES-256-GCM encrypted payload export/import protecting entire profile history.
- **Multi-Language Localization**: Full string coverage for English, Hindi (`values-hi/strings.xml`), and Marathi (`values-mr/strings.xml`).

## Phase 6: Future Expansion & Engagement Architecture
- **Monthly Digest (`GenerateMonthlyDigestUseCase.kt`)**: Automated month-end report generating top spending categories, highest day spend, savings rate, and financial vibe persona (*"Disciplined Saver"*, *"Balanced Spender"*, *"Impulsive Explorer"*).
- **Expense Splitter (`SplitExpenseUseCase.kt`, `SplitExpenseScreen.kt`)**: Accurate arithmetic for splitting group bills with equal/unequal share validation and rounded balance distribution.
- **Discipline Streaks (`CalculateStreakUseCase.kt`)**: Positive reinforcement tracking consecutive logging days without gamification clutter.

## Phase 7: SmartSpend UI & Custom Authentication Architecture
- **Pattern Lock Component (`PatternLockView.kt`)**: Custom Jetpack Compose Canvas implementation tracking drag gestures across a 3x3 node grid, computing dynamic connecting lines, outer glow rings on active nodes, haptic touch feedback, and dot sequence serialization (`"0-1-2-4"`).
- **Authentication Strategy**: Multi-modal lock system supporting `PIN`, `PASSWORD`, `PATTERN`, and hardware-backed `BiometricPrompt` with fallback handling and secure profile hash validation.
- **Pastel Color Architecture (`ThemePalette.kt`, `Color.kt`, `Theme.kt`)**: 6 non-neon curated pastel palettes (`Ocean Slate`, `Forest Sage`, `Twilight Lilac`, `Warm Clay`, `Matcha Tea`, `Vintage Rose`) integrated into MaterialTheme M3 Dynamic Color Schemes with System/Light/Dark mode runtime switching persisted in `SessionDataStore`.
- **Session Lifecycle & Sign Out (`SettingsViewModel.kt`, `ParadoxNavGraph.kt`)**: Implemented `signOut()` resetting session lock status (`sessionDataStore.setAppLocked(true)`) and popping the Compose navigation stack to root (`popUpTo(0)`), guaranteeing zero unauthorized back-navigation.

## Test Coverage (41 Passing Unit Tests)
- `MoneyTest`: Exact decimal arithmetic, banker's rounding, currency safety.
- `BudgetStatusTest`: Threshold transitions (`ON_TRACK`, `NEAR_LIMIT >=80%`, `OVER_BUDGET >=100%`).
- `AddExpenseUseCaseTest`: Amount validation, title validation, date safety.
- `DeleteCategoryUseCaseTest`: Referential integrity protection.
- `ProfileIsolationTest`: SQL-level isolation verifying zero cross-profile leakage.
- `IncomeUseCasesTest`: Income addition, retrieval, and net cash flow computation.
- `RecurringTest`: Frequency normalization for subscriptions.
- `SavingsGoalTest`: Progress percentage and goal remaining calculations.
- `NaturalLanguageParserTest`: Parsing accuracy across relative dates, payment types, and currency symbols.
- `ReceiptOcrParserTest`: Merchant extraction, date parsing, and Grand Total extraction.
- `DuplicateGuardTest`: Exact/fuzzy duplicate detection.
- `CsvImportUseCaseTest`: Header detection, row validation, and category mapping.
- `SafeToSpendUseCaseTest`: Daily/weekly/monthly safe-to-spend computation.
- `FinancialHealthScoreTest`: 5-pillar scoring and grade tier assignment.
- `LeakHunterAndSimulatorTest`: Subscription/micro-leak detection and purchase simulation.
- `AskParadoxTest`: Grounded natural language query resolution against database records.
- `EncryptedBackupTest`: AES-256-GCM encryption & decryption roundtrip validation.
- `EngagementUseCasesTest`: Streak calculations, monthly digest generation, and expense splitting arithmetic.
