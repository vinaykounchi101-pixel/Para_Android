# Paradox Native Android App — Project Progress

## Current Status: Phase 1 through Phase 9 Fully Completed & Verified

### Phase-by-Phase Roadmap Progress

- [x] **Phase 1: Core Foundation & MVP**
  - [x] Full native Android project structure implemented strictly per SRS §5
  - [x] Gradle Version Catalog (`libs.versions.toml`), root & app `build.gradle.kts`
  - [x] Room Database with SQLCipher 256-bit AES-GCM encryption & Keystore integration
  - [x] Local Private Profiles with PIN/Password PBKDF2 hashing & BiometricPrompt
  - [x] Strict SQL-level profile isolation (`WHERE profileId = :profileId`)
  - [x] Expense CRUD with multi-criteria filtering (search, category, date, amount) & sorting
  - [x] Category & Payment Method management with deletion safety & reassignment
  - [x] Deterministic Budgets & Guardrails (`On Track`, `Near Limit >80%`, `Over Budget >100%`)
  - [x] Reactive Dashboard (Month Spend gauge, Velocity, Top Categories, Recent Activity)
  - [x] Dual Pastel M3 Light & Mineral Obsidian Dark Design Systems
  - [x] Exact decimal `Money` arithmetic (`BigDecimal` with banker's rounding)
  - [x] Automated Unit Tests (`MoneyTest`, `BudgetStatusTest`, `AddExpenseUseCaseTest`, `DeleteCategoryUseCaseTest`)
  - [x] Automated Instrumentation Profile Isolation Test (`ProfileIsolationTest`)

- [x] **Phase 2: Native Convenience & Financial Foundation**
  - [x] Income & Net Cash Flow tracking (`FR-P2-005`)
  - [x] Wallets / Accounts model (`FR-P2-006`)
  - [x] Recurring Expenses & Subscriptions engine (`FR-P2-007`)
  - [x] Savings Goals tracker (`FR-P2-008`)
  - [x] CSV / Vector PDF export (`FR-P2-009`)
  - [x] Jetpack Glance Home Screen Widget & Shortcuts (`FR-P2-001`, `FR-P2-002`)

- [x] **Phase 3: Assisted Capture Pipeline**
  - [x] Natural-language Quick Add parser (`FR-P3-001`)
  - [x] Android SpeechRecognizer Voice-to-Expense entry (`FR-P3-002`)
  - [x] CameraX + Google ML Kit OCR live receipt/bill scanner (`FR-P3-003`)
  - [x] Screenshot-to-Expense & Share Sheet receiver (`FR-P2-003`, `FR-P3-004`)
  - [x] Proactive Duplicate Guard warning engine (`FR-P3-005`)
  - [x] CSV Statement Import with auto column detection & batch preview (`FR-P3-006`)

- [x] **Phase 4: Financial Intelligence**
  - [x] Ask Paradox Grounded Conversational AI Engine (`AskParadoxUseCase`, `AskParadoxViewModel`, `AskParadoxScreen`)
  - [x] Safe-to-Spend Multi-Horizon Deterministic Computation (`CalculateSafeToSpendUseCase`)
  - [x] 5-Pillar Holistic Financial Health Scoring Engine (`CalculateFinancialHealthScoreUseCase`)
  - [x] Recurring Leak Hunter (`DetectSpendingLeaksUseCase`)
  - [x] Purchase Simulator with Impact Modeling (`SimulatePurchaseUseCase`)
  - [x] Trend-Aware Spending Forecaster (`ForecastSpendingUseCase`)
  - [x] Insights Hub Screen & ViewModel (`InsightsHubScreen`, `InsightsViewModel`)
  - [x] AI Insight Audit Logging (`AiInsightLogEntity`, `AiInsightLogDao`)

- [x] **Phase 5: Sync, Backup & Advanced Personalization**
  - [x] Offline-First Sync Engine & Outbox Queue (`SyncEngine`, `SyncQueueItemEntity`, `SyncQueueDao`)
  - [x] AES-256-GCM Password-Protected Encrypted Vault Backup with full 8-entity serialization (`EncryptedBackupUseCase`)
  - [x] Safe Vault Restore with Topological Dependency Rebuilding & Passphrase Verification (`RestoreBackupUseCase`)
  - [x] Standalone File-Based Backup Export (`.paradoxvault` / `.json`) via Android SAF (`CreateDocument`) & Share Sheet (`FileProvider`)
  - [x] Standalone File-Based Backup Import via Android System File Picker (`OpenDocument`) with live badge details & decryption
  - [x] Dynamic Active Profile Resolution & Automatic Foreign-Key Fallback Generation (resolving error 1811)
  - [x] Clear Decryption Error Diagnostics (`AEADBadTagException` / `BadPaddingException` mapped to helpful user messages)
  - [x] Backup & Restore UI & ViewModel (`BackupScreen`, `BackupViewModel`)
  - [x] Sync Settings UI & Management (`SyncSettingsScreen`, `SyncViewModel`)
  - [x] Hindi (`values-hi/strings.xml`) and Marathi (`values-mr/strings.xml`) Multi-Language Localization

- [x] **Phase 6: Future Expansion & Ecosystem**
  - [x] Monthly Digest & Year-in-Review Narrative Generator (`GenerateMonthlyDigestUseCase`)
  - [x] Expense Splitting Engine with Unequal & Equal Shares (`SplitExpenseUseCase`, `SplitExpenseScreen`)
  - [x] Positive Reinforcement Streak & Discipline Counter (`CalculateStreakUseCase`)
  - [x] Financial Persona / Spending Vibe Analyzer (`DetermineFinancialVibeUseCase`)
  - [x] Engagement & Community Hub UI (`EngagementScreen`, `EngagementViewModel`)

- [x] **Phase 7: SmartSpend UI Alignment, Custom Authentication & Visual Polish**
  - [x] SmartSpend design system alignment for Onboarding, Unlock, and Settings screens
  - [x] 3-way primary lock method selector: `[ PIN ]`, `[ PASSWORD ]`, `[ PATTERN ]`
  - [x] Custom interactive 3x3 Pattern Lock canvas component (`PatternLockView`) with real-time gesture tracking, glowing node rings, connection lines, and validation (min 4 dots)
  - [x] Tactile circular Fingerprint biometric scan button with native Android `BiometricPrompt`
  - [x] 6 Curated Pastel Theme Palettes (`Ocean Slate`, `Forest Sage`, `Twilight Lilac`, `Warm Clay`, `Matcha Tea`, `Vintage Rose`) with dynamic System / Light / Dark modes
  - [x] Spending Velocity card redesign with custom styled bar charts and 7-day breakdown
  - [x] Dedicated **Sign Out** option in Settings header & Security group with root backstack cleanup (`popUpTo(0)`)
  - [x] On-Device Instrumented Test Data Seeder (`SeedSampleDataTest.kt`)

- [x] **Phase 8: Debts & Udhaar (Khata), Month Selector, Wallets Net Balance Hero & In-App Localization**
  - [x] Debts & Udhaar (Khata) Peer-to-Peer Ledger (`DebtEntity`, `DebtRepaymentEntity`, `DebtDao`, `DebtRepaymentDao`, `DebtRepositoryImpl`)
  - [x] Android Contact Picker Integration (`ActivityResultContracts.PickContact` / `ContactsContract`) with safe permission handling and zero-crash manual fallback
  - [x] Partial & full debt repayment tracking with live balance deductions and auto-settlement (`isSettled = true`)
  - [x] Material 3 Modal Bottom Sheets with 20dp padding and quick amount selector chips (`+₹100`, `+₹500`, `+₹1000`, `+₹2000`)
  - [x] Top App Bar Dynamic Month Selector Dropdown with reactive multi-month recalculation for dashboard totals, velocity bars, and top categories
  - [x] Wallets & Accounts Net Balance Hero card with liquid vs credit breakdown and 20dp card styling
  - [x] In-App Language Selector Bottom Sheet with runtime locale switching (EN, HI, MR, Hinglish) and DataStore persistence

- [x] **Phase 9: Paradox Quick Ball & Floating Assistive Capture Menu**
  - [x] Quick Ball 3-Mode Configuration (`OFF`, `IN_APP`, `SYSTEM_WIDE`) with `SessionDataStore` persistence (`FR-P2-016`)
  - [x] Foreground Overlay Service (`QuickBallOverlayService`) with edge-snap physics and `SYSTEM_ALERT_WINDOW` permission checking (`FR-P2-017`)
  - [x] Android 14/15 Background Activity Launch compliance via `PendingIntent.getActivity` with `ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED` (`FR-P2-017`)
  - [x] Radial Crescent Arc Menu (`QuickBallView`) popping out 4 shortcuts (Quick Add, Voice, Scan Receipt, Ask Paradox) with spring physics (`FR-P2-018`)
  - [x] Minimal Light Pastel sRGB UI Theme with high-contrast slate typography and 95% transparent dismiss scrim (`FR-P2-019`)
  - [x] 3-Second Inactivity Auto-Tuck: 50% edge hide and 38% opacity dimming with instant touch wake-up (`FR-P2-020`)
  - [x] Master PRD (`PARADOX_MASTER_PRD.md` v2.2) and Android SRS (`PARADOX_ANDROID_SRS.md` v1.3) fully synchronized with Quick Ball specifications
  - [x] Verified build and deployment on physical device (Motorola Edge 60 Fusion)

