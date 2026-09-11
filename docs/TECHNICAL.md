# Paradox Native Android App — Technical Architecture & Implementation Log

## Architecture Summary
Paradox Android is a native, local-first, privacy-first personal finance application built with Clean Architecture:
- **Presentation**: Jetpack Compose + Material 3, Navigation Compose, ViewModels (`StateFlow`), Unidirectional Data Flow.
- **Domain**: Pure Kotlin use cases, value classes (`Money` with `BigDecimal`), repository contracts. Zero Android or Room framework dependencies.
- **Data**: Repository implementations, Room Entities & DAOs with SQL-level profile isolation (`WHERE profileId = :profileId`), bidirectional entity/domain mappers.
- **Local Storage**: SQLCipher 256-bit AES-GCM encrypted Room SQLite database (`paradox_vault.db`).
- **Security & Keystore**: Hardware-backed master key via `AndroidKeyStore`, wrapping the SQLCipher database passphrase; salted PBKDF2-HMAC-SHA256 (12,000 iterations) credential hashing; AndroidX `BiometricPrompt`.
- **Dependency Injection**: Google Hilt.

## Database Entities (Phase 1)
1. `profiles`: `id`, `name`, `primaryAuthType`, `credentialHash`, `biometricEnabled`, `createdAt`
2. `categories`: `id`, `profileId` (FK CASCADE), `name`, `iconName`, `colorHex`, `isCustom`, `isDefault`
3. `payment_methods`: `id`, `profileId` (FK CASCADE), `type`, `label`, `isCustom`
4. `expenses`: `id`, `profileId` (FK CASCADE), `title`, `amount`, `currency`, `categoryId` (FK RESTRICT), `paymentMethodId` (FK RESTRICT), `date`, `notes`, `recurringFlag`, `source`, `createdAt`, `updatedAt`
5. `budgets`: `id`, `profileId` (FK CASCADE), `type`, `amount`, `categoryId` (FK CASCADE), `thresholdPct`

## Test Coverage
- `MoneyTest`: Validates exact decimal arithmetic, addition, subtraction, banker's rounding, multi-currency safety.
- `BudgetStatusTest`: Validates threshold transitions (`ON_TRACK`, `NEAR_LIMIT >=80%`, `OVER_BUDGET >=100%`).
- `AddExpenseUseCaseTest`: Validates amount > 0, non-future dates, and title validation.
- `DeleteCategoryUseCaseTest`: Validates referential integrity and reassignment requirement.
- `ProfileIsolationTest`: Verifies zero cross-profile leakage at the SQL database layer.
