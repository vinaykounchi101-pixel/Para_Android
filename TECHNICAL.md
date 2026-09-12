# Paradox Native Android App — Technical Architecture & Implementation Log

## Architecture Summary
Paradox Android is a native, local-first, privacy-first personal finance application built with Clean Architecture:
- **Presentation**: Jetpack Compose + Material 3, Navigation Compose, ViewModels (`StateFlow`), Unidirectional Data Flow.
- **Domain**: Pure Kotlin use cases, value classes (`Money` with `BigDecimal`), repository contracts. Zero Android or Room framework dependencies.
- **Data**: Repository implementations, Room Entities & DAOs with SQL-level profile isolation (`WHERE profileId = :profileId`), bidirectional entity/domain mappers.
- **Local Storage**: SQLCipher 256-bit AES-GCM encrypted Room SQLite database (`paradox_vault.db`).
- **Security & Keystore**: Hardware-backed master key via `AndroidKeyStore`, wrapping the SQLCipher database passphrase; salted PBKDF2-HMAC-SHA256 (12,000 iterations) credential hashing; AndroidX `BiometricPrompt`.
- **Dependency Injection**: Google Hilt.

## Database Entities (Phases 1 - 8)
1. `profiles`: `id`, `name`, `primaryAuthType`, `credentialHash`, `biometricEnabled`, `createdAt`
2. `categories`: `id`, `profileId` (FK CASCADE), `name`, `iconName`, `colorHex`, `isCustom`, `isDefault`
3. `payment_methods`: `id`, `profileId` (FK CASCADE), `type`, `label`, `isCustom`
4. `expenses`: `id`, `profileId` (FK CASCADE), `title`, `amount`, `currency`, `categoryId` (FK RESTRICT), `paymentMethodId` (FK RESTRICT), `date`, `notes`, `recurringFlag`, `source`, `attachmentRef`, `createdAt`, `updatedAt`
5. `budgets`: `id`, `profileId` (FK CASCADE), `type`, `amount`, `categoryId` (FK CASCADE), `thresholdPct`
6. `incomes`: `id`, `profileId` (FK CASCADE), `source`, `amount`, `currency`, `date`, `notes`, `createdAt`, `updatedAt`
7. `accounts`: `id`, `profileId` (FK CASCADE), `name`, `type`, `currency`, `initialBalance`, `colorHex`, `iconName`, `isDefault`, `createdAt`, `updatedAt`
8. `recurring_expenses`: `id`, `profileId` (FK CASCADE), `title`, `amount`, `currency`, `categoryId` (FK RESTRICT), `paymentMethodId` (FK RESTRICT), `frequency`, `startDate`, `nextDueDate`, `isActive`, `notes`, `createdAt`, `updatedAt`
9. `savings_goals`: `id`, `profileId` (FK CASCADE), `name`, `targetAmount`, `currentAmount`, `currency`, `targetDate`, `colorHex`, `iconName`, `createdAt`, `updatedAt`
10. `savings_contributions`: `id`, `goalId` (FK CASCADE), `profileId` (FK CASCADE), `amount`, `currency`, `date`, `notes`, `createdAt`
11. `ai_insight_logs`: `id`, `profileId` (FK CASCADE), `type`, `payload`, `isEstimate`, `generatedAt`
12. `sync_queue`: `id`, `profileId` (FK CASCADE), `entityRef`, `operation`, `status`, `createdAt`
13. `debts`: `id`, `profileId` (FK CASCADE), `type` (LENT/BORROWED), `contactName`, `contactPhone`, `initialAmount`, `remainingBalance`, `currency`, `dueDate`, `notes`, `categoryId` (FK SET NULL), `isSettled`, `createdAt`, `updatedAt`
14. `debt_repayments`: `id`, `debtId` (FK CASCADE), `profileId` (FK CASCADE), `amount`, `currency`, `date`, `notes`, `createdAt`

## Phase 4: Financial Intelligence Architecture
- **Ask Paradox (`AskParadoxUseCase.kt`, `AskParadoxViewModel.kt`, `AskParadoxScreen.kt`)**: Context-grounded deterministic AI engine parsing natural language finance questions (*"How much did I spend on food this month?"*, *"Can I afford dinner?"*, *"What are my biggest expenses?"*) with 0% hallucination risk, computing answers directly from verified SQLCipher profile records.
- **Safe-to-Spend Engine (`CalculateSafeToSpendUseCase.kt`)**: Multi-horizon (`Today`, `This Week`, `Rest of Month`) deterministic buffer calculation subtracting remaining committed budget allocations and upcoming bills from liquid account balances.
- **5-Pillar Health Scoring (`CalculateFinancialHealthScoreUseCase.kt`)**: Holistic 100-point score evaluating Savings Discipline (20), Budget Adherence (20), Bill Regularity (20), Net Cash Flow (20), and Emergency Buffer (20).
- **Leak Hunter (`DetectSpendingLeaksUseCase.kt`)**: Algorithmic detector flagging idle subscriptions, sneaky small recurring micro-leaks (<₹100 multiple times weekly), and rapid category inflation.
- **Purchase Simulator (`SimulatePurchaseUseCase.kt`)**: Simulates prospective purchases and evaluates budget threshold overflows, category impacts, and days of safe spend remaining.

## Phase 5: Sync, Backup & Localization Architecture
- **Offline-First Sync Engine (`SyncEngine.kt`)**: Transactional outbox pattern recording insert/update/delete mutations in `sync_queue` table with retry policies and deterministic conflict resolution.
- **AES-256-GCM Encrypted Vault Backup (`EncryptedBackupUseCase.kt`, `RestoreBackupUseCase.kt`, `BackupViewModel.kt`, `BackupScreen.kt`)**: 
  - Complete 8-entity serialization (Categories, Payment Methods, Accounts, Budgets, Recurring Expenses, Savings Goals, Expenses, Incomes) encrypted using PBKDF2-HMAC-SHA256 (12,000 iterations) + AES-256-GCM (128-bit auth tag, 16-byte salt, 12-byte IV).
  - Standalone file export (`.paradoxvault` / `.json`) using Android Storage Access Framework (`ActivityResultContracts.CreateDocument`) and Android Share Sheet (`FileProvider` configured for `cache-path` and `files-path` under `backups/`).
  - Standalone file import via system file picker (`ActivityResultContracts.OpenDocument`), extracting file metadata (display name, size), validating the encrypted envelope, and restoring records in topological order with passphrase authentication.
  - **Dynamic Active Profile Resolution**: Profile ownership dynamically resolved through `SessionDataStore` and `ProfileRepository`, re-scoping restored entities to the active session.
  - **Foreign-Key Safe Restoration**: Topological entity insertion ordering (Categories & Payment Methods first, followed by Accounts, Budgets, Recurring, Goals, Expenses, Incomes) with automatic fallback entity generation to prevent SQLite foreign key constraint failures (code 1811).
  - **Clear Decryption Diagnostics**: Explicit error mapping for `AEADBadTagException`, `BadPaddingException`, and OpenSSL `BAD_DECRYPT` errors to user-friendly messages.
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
- **On-Device Data Seeder (`SeedSampleDataTest.kt`)**: Native instrumented test runner capable of injecting complete test financial datasets directly into the SQLCipher on-device database.

## Phase 8: Debts & Udhaar Ledger, Month Selector, Wallets Hero & Localization Architecture
- **Debts & Udhaar System (`DebtEntity.kt`, `DebtRepaymentEntity.kt`, `DebtDao.kt`, `DebtRepositoryImpl.kt`, `DebtViewModel.kt`, `DebtScreen.kt`)**:
  - Peer-to-peer debt ledger supporting Lent (receivables) and Borrowed (payables) money records.
  - Native contact picker integration via `ActivityResultContracts.PickContact` with safe permission handling and zero-crash manual entry fallback.
  - Live partial & full repayment tracking (`AddRepaymentUseCase`) with automated remaining balance deduction and auto-settlement.
  - Material 3 modal bottom sheets with 20dp padding, quick amount chips (`+100`, `+500`, `+1000`, `+2000`), and date pickers.
- **Dynamic Month Selector (`DashboardViewModel.kt`, `DashboardScreen.kt`, `GetDashboardSummaryUseCase.kt`)**:
  - Top app bar dropdown menu supporting navigation across the past 12+ months.
  - Instant reactive recalculation of monthly spend, spending velocity bar charts, and top categories.
- **Wallets & Accounts Hero (`AccountsScreen.kt`, `AddEditAccountScreen.kt`)**:
  - Real-time Net Balance Hero card with liquid vs credit breakdown and 20dp card layout standards.
- **In-App Localization (`SettingsScreen.kt`, `SettingsViewModel.kt`, `SessionDataStore.kt`)**:
  - Interactive bottom sheet for switching between English (`en`), Hindi (`hi`), Marathi (`mr`), and Hinglish/Minglish (`hi-Latn`) with persistent DataStore storage.

## Phase 9: Paradox Quick Ball & Floating Assistive Capture Architecture
- **Quick Ball Modes & Settings (`SessionDataStore.kt`, `SettingsViewModel.kt`, `SettingsScreen.kt`)**:
  - `QuickBallMode`: `OFF`, `IN_APP` (zero permissions), and `SYSTEM_WIDE` (`SYSTEM_ALERT_WINDOW`).
  - Stored in DataStore with reactive Flow observation triggering overlay service startup/shutdown.
- **System-Wide Overlay Service (`QuickBallOverlayService.kt`)**:
  - `ForegroundService` with notification channel and `TYPE_APPLICATION_OVERLAY` WindowManager layout.
  - Custom `ComposeView` with `ViewTreeLifecycleOwner`, `ViewTreeSavedStateRegistryOwner`, and `ViewTreeViewModelStoreOwner` lifecycle attachments.
  - Floating orb with edge-snap physics (snapping to nearest left/right screen edge on drag release).
- **Radial Crescent Arc Layout (`QuickBallView.kt`)**:
  - Ergonomic C-curve crescent radial arc popping out 4 shortcuts (Quick Add, Voice, Scan Receipt, Ask Paradox) based on screen side (left/right).
  - Physics-based spring animations (`stiffness = 380f`, `dampingRatio = 0.65f`) and radial distance calculation (`radius = 135dp`).
- **Minimal Light Pastel sRGB UI Theme**:
  - Pastel rounded capsules (Mint Cream `#F0FDF4`, Sky Cyan Cream `#F0F9FF`, Soft Periwinkle `#EEF2FF`, Lavender Lilac `#FAF5FF`) with Dark Slate `#1E293B` typography.
  - 95% transparent dismiss scrim (`alpha = 0.05f`) preventing background/wallpaper darkening.
- **3-Second Inactivity Auto-Tuck & Dimming**:
  - Coroutine timer resetting on user interaction; after 3s idle, tucks 50% into screen edge and dims to 38% opacity.
  - Instant wake-up to 95% opacity on touch.
- **Android 14/15 Background Activity Launch Compliance**:
  - Employs `PendingIntent.getActivity` configured with `ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED` (`FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP`) and `Intent.FLAG_ACTIVITY_NEW_TASK` to guarantee flawless deep-link activity launching over third-party applications on Android 14 and Android 15.

## Test Coverage (45 Passing Unit Tests)
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
- `DebtUseCasesTest`: Debt creation (Lent/Borrowed), repayment recording, balance decrement, and settlement validation.
