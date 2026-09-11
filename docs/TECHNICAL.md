# Paradox Native Android App — Technical Architecture & Implementation Log

## Architecture Summary
Paradox Android is a native, local-first, privacy-first personal finance application built with Clean Architecture:
- **Presentation**: Jetpack Compose + Material 3, Navigation Compose, ViewModels (`StateFlow`), Unidirectional Data Flow.
- **Domain**: Pure Kotlin use cases, value classes (`Money` with `BigDecimal`), repository contracts. Zero Android or Room framework dependencies.
- **Data**: Repository implementations, Room Entities & DAOs with SQL-level profile isolation (`WHERE profileId = :profileId`), bidirectional entity/domain mappers.
- **Local Storage**: SQLCipher 256-bit AES-GCM encrypted Room SQLite database (`paradox_vault.db`).
- **Security & Keystore**: Hardware-backed master key via `AndroidKeyStore`, wrapping the SQLCipher database passphrase; salted PBKDF2-HMAC-SHA256 (12,000 iterations) credential hashing; AndroidX `BiometricPrompt`.
- **Dependency Injection**: Google Hilt.

## Database Entities (Phase 1 & Phase 2)
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

## Phase 3 Assisted Capture Pipeline Components
- **Natural Language Quick Add Engine (`NaturalLanguageParser.kt`)**: Tokenizes freeform sentences (e.g. *"Uber 250 cash yesterday"*, *"Coffee 4.50 card"*) into structured transactions with amount, date, payment method, and category heuristics.
- **Voice Recognition Manager (`VoiceRecognitionManager.kt`)**: Reactive SpeechRecognizer wrapper providing stateful streaming transcripts into the NL parsing pipeline.
- **CameraX + ML Kit OCR Receipt Scanner (`CameraScannerView.kt`, `ReceiptImageOcrHelper.kt`, `ReceiptOcrParser.kt`)**: On-device optical character recognition detecting merchant headers, date formats, total amounts (with Grand Total precedence), and payment methods.
- **Duplicate Guard (`DuplicateGuardUseCase.kt`)**: Multi-signal transaction evaluation (amount match, fuzzy title similarity, +/- 3-day window) generating non-blocking warning banners.
- **CSV Statement Import (`CsvImportUseCase.kt`)**: RFC-4180 compliant CSV parser with auto column matching, validation, previewing, and batch commit to active profile vault.
- **Share Sheet Receiver**: Registered `ACTION_SEND` intent filter in `AndroidManifest.xml` for shared text and receipts/screenshots.

## Test Coverage (30 Passing Unit Tests)
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
