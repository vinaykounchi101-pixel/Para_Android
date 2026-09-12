# Features in CashFlow Docs Not Implemented / Not Explicitly Covered in Paradox

## Comparison Basis

This comparison uses:

- **Paradox:** `PARADOX_ANDROID_SRS.md`
- **CashFlow:** `CashFlow_PRD_EN.md` and `CashFlow_SRS_Phase1.md`

The comparison is feature-focused. A CashFlow feature is listed below when Paradox does **not** currently contain an equivalent feature, or when CashFlow specifies a materially different capability that is not explicitly present in the Paradox SRS.

> **Important:** “Not implemented” here means **not present in the current Paradox documentation**, not that the feature can never be added. Some Paradox features are intentionally deferred to later phases.

---

## 1. Features Missing from Paradox

### 1.1 Debt / Udhaar Management

CashFlow has a complete debt-management module that is not present in the current Paradox SRS.

**Missing features:**
- Record money **lent to** another person.
- Record money **borrowed from** another person.
- Debt type: `LENT` / `BORROWED`.
- Person/contact information associated with the debt.
- Contact picker integration.
- Direct call action from a debt entry.
- Partial repayment tracking.
- Automatic `SETTLED` status when remaining amount reaches zero.
- Total receivable vs. total payable summary.
- Due-date reminders.
- WhatsApp/SMS payment reminders.

**CashFlow references:** Phase 1 Debt/Udhaar Management.

---

### 1.2 Split Expenses

CashFlow supports splitting a group expense among multiple people.

**Missing from Paradox:**
- Add multiple participants to an expense.
- Store each participant's name/contact.
- Store each participant's share amount.
- Track each participant as settled/unsettled.
- Track who owes how much for a group expense.

Paradox currently has a different concept in its future Phase 6: shared budgets/split expenses/family spaces, but this is optional future expansion rather than the concrete Phase 1–4 split-expense capability defined by CashFlow.

---

### 1.3 Multi-Currency Expense Recording

CashFlow explicitly allows an expense to be recorded in a foreign currency.

**Missing from Paradox:**
- A user-facing currency selection specifically for individual expenses.
- Recording expenses in currencies other than the profile/default currency.

Paradox's SRS does contain a `currency` field on expenses and fixed-precision money handling, so the data model is partially prepared. However, the current functional requirements do not explicitly define CashFlow-style foreign-currency expense entry.

---

### 1.4 Wallet-Level Money Operations

CashFlow has more explicit wallet/account transaction functionality.

**Missing or not explicitly defined in Paradox:**
- Add/top-up money into an account.
- Transfer money from one account/wallet to another.
- Explicit wallet transaction history.
- Explicit total net-worth calculation from wallet balances.
- Wallet/account balance management as a concrete Phase 1 feature.

Paradox's Phase 2 account model is more cautious: it supports accounts/wallets without claiming a balance unless sufficient recorded data or an approved integration exists.

---

### 1.5 Wallet Customization

CashFlow allows each wallet/account to have:

- Custom name.
- Account type.
- Initial balance.
- Currency.
- Color.
- Icon.
- Default-wallet designation.

These wallet-specific customization fields are not explicitly defined as Paradox functionality.

---

### 1.6 Recurring Expense Generation

CashFlow specifies automatic recurring-expense generation.

**CashFlow supports:**
- Daily recurring expenses.
- Weekly recurring expenses.
- Monthly recurring expenses.
- Background generation of the next expense.
- Recurrence parent/instance relationship.

Paradox supports recurring expenses, but its SRS places recurring expenses in Phase 2 and additionally focuses on upcoming-payment visibility and monthly-normalized commitments.

So the key CashFlow capability that should be explicitly added if desired is **automatic creation of recurring expense instances**.

---

### 1.7 UPI Payment Notification → Quick Add

CashFlow has a native Android convenience feature:

1. Detect a UPI payment notification.
2. Show a Paradox/CashFlow notification.
3. Offer the user a one-tap option to log the payment as an expense.

This is **not explicitly present in the current Paradox SRS**.

Paradox instead has a more general Phase 3 SMS transaction-candidate flow and Phase 2 configurable notifications.

---

### 1.8 Excel (`.xlsx`) Export

CashFlow explicitly supports:

- PDF export.
- Excel `.xlsx` export.

Paradox currently specifies:

- CSV.
- PDF.
- Spreadsheet-compatible export.

Therefore, if actual Excel `.xlsx` generation is required, this is not explicitly guaranteed by the Paradox SRS.

---

### 1.9 Week / Month / Year Report Tabs

CashFlow explicitly defines report periods:

- Week.
- Month.
- Year.

Paradox has dashboard spending trends and analytics, but the current SRS does not explicitly define the CashFlow-style **Week / Month / Year report tabs**.

---

### 1.10 Current Month vs Previous Month Spending Comparison

CashFlow explicitly provides a comparison between the current month's spending and the previous month's spending.

Paradox has spending trends and broader financial intelligence/forecasting, but this exact report comparison is not explicitly specified.

---

## 2. Authentication Features Missing from Paradox

CashFlow's original PRD defines account-based authentication in Phase 2.

However, Paradox intentionally uses a different architecture: local private profiles are not cloud accounts.

Therefore these CashFlow features are **not currently implemented in Paradox**:

### 2.1 Email/Password Account

- Create account using name, email, password.
- Login.
- Logout.
- Email verification.

### 2.2 Google OAuth Login

- Sign up/login with Google.
- OAuth-based authentication.

### 2.3 Password Recovery

- Forgot-password email.
- Password reset.
- Change password while logged in.

### 2.4 Email Notifications

- Account verification email.
- Password reset email.
- Monthly spending summary email.
- Budget-exceeded email.

**Important:** These are not necessarily gaps that should be added. Paradox deliberately defers cloud identity/authentication to Phase 5, where sync is introduced.

---

## 3. AI Features in CashFlow That Are Missing from Paradox

Paradox actually has a broader AI roadmap than CashFlow, but several CashFlow-specific AI behaviors are not explicitly represented.

### 3.1 AI Spending Roast

CashFlow specifies a fun/witty spending commentary feature.

Example concept:

> “Ordered from Zomato 14 times this week!”

It also specifies an explanation option showing the reasoning.

Paradox's optional Phase 6 engagement features include playful commentary, but the specific **AI Spending Roast + explanation** behavior is not explicitly defined as a Phase 4 feature.

---

### 3.2 Hinglish Financial Assistant

CashFlow explicitly says the conversational assistant should understand Hinglish.

Paradox has Ask Paradox, voice input, and localization planning, but **Hinglish understanding is not explicitly specified**.

---

### 3.3 AI-Driven Recurring Subscription Price Increase Detection

CashFlow explicitly requires:

- Detect recurring subscriptions.
- Calculate their monthly burden.
- Detect when a recurring payment increases compared with previous months.
- Alert the user about the increase.

Paradox has:

- Recurring expenses/subscriptions.
- Monthly-normalized commitment calculation.
- Leak Hunter.
- Forecasting.

But **explicit subscription price-increase detection and alerting is not separately specified**.

---

### 3.4 Goal Planner That Tells the User Where to Cut Back

Paradox has Savings Goals and AI recommendations, but CashFlow specifically requires the savings planner to:

- Calculate required monthly savings.
- Identify areas where the user can cut spending to reach the goal.

The first part is compatible with Paradox's savings-goal direction; the explicit **“where can I cut back?” goal-planning behavior** is not separately defined.

---

### 3.5 Unusual Large Expense Alerts

CashFlow explicitly alerts the user when:

- An unusually large expense occurs.
- A budget is crossed.

Paradox has budget warnings and Leak Hunter/financial intelligence, but the exact **real-time unusual-large-expense alert** is not explicitly defined.

---

## 4. Features CashFlow Has That Paradox Has Replaced With a Different Design

These should not automatically be treated as things Paradox must add.

### 4.1 Single User vs Local Profiles

CashFlow:

- Single user.
- No-login Phase 1.

Paradox:

- Local private profiles.
- Multiple local profiles can exist.
- Data is isolated at the data layer.

This is a deliberate Paradox enhancement rather than a missing feature.

---

### 4.2 CashFlow's Account Authentication vs Paradox's Local Unlock

CashFlow uses:

- Email/password.
- Google OAuth.
- Account verification.

Paradox uses:

- Local profile.
- PIN unlock.
- Biometric authentication.
- Device-credential fallback.

Therefore authentication is intentionally architected differently.

---

### 4.3 CashFlow's Phase 1 Wallet Balance Model vs Paradox's Phase 2 Account Model

CashFlow treats wallets/accounts as a concrete Phase 1 financial foundation.

Paradox deliberately moves the account/wallet model to Phase 2 and focuses Phase 1 on expenses, categories, budgets, and local profiles.

This is a **phase difference**, not necessarily a feature omission.

---

## 5. Summary — Recommended Additions

If the goal is to make Paradox cover **everything useful from CashFlow**, the main feature additions are:

### High-value additions

1. **Debt/Udhaar Management**
   - Lent/Borrowed
   - Contact picker
   - Call
   - Partial repayments
   - Settlement
   - Due reminders
   - WhatsApp/SMS reminders
   - Receivable/Payable summary

2. **Split Expenses**
   - Participants
   - Per-person shares
   - Settlement tracking

3. **Explicit Multi-Currency Expense UI**
   - Currency selector
   - Foreign-currency recording

4. **Wallet/Account Operations**
   - Add money
   - Transfer
   - Transaction history
   - Balance/net-worth handling

5. **UPI Notification Quick Add**

6. **Explicit Week/Month/Year Reports**

7. **Current-vs-previous-month comparison**

8. **Explicit `.xlsx` Excel export**

### AI additions worth considering

9. **Spending Roast + explanation**

10. **Hinglish support for Ask Paradox**

11. **Subscription price-increase detection**

12. **Goal planner with explicit cut-back recommendations**

13. **Unusual large-expense alerts**

---

## 6. Important Conclusion

The CashFlow documents are **not simply a superset of Paradox**.

Paradox already contains several capabilities that CashFlow does not define, including:

- Local multi-profile architecture.
- Profile-level data isolation.
- Encrypted local database.
- PIN + biometric unlock.
- Natural-language Quick Add.
- Screenshot-to-expense.
- Share Sheet capture.
- Duplicate Guard.
- CSV import.
- SMS transaction candidates.
- Ask Paradox.
- Deterministic Safe-to-Spend.
- Financial Health Score.
- Leak Hunter.
- Purchase Simulator.
- Burn-rate and forecasting.
- Adaptive budget recommendations.
- 50/30/20 comparison.
- Optional cross-device sync.
- Encrypted backup/restore.
- Passkey support planning.
- Additional localization planning.

Therefore, this document should be treated as a **gap analysis**, not as a request to replace the Paradox architecture or phase structure.

