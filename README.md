# 💰 MoneyTracker Mobile App

A modern, high-performance personal finance Android application built using **Jetpack Compose**, **Kotlin Coroutines & Flow**, **Room Persistence Library (SQLite)**, and **Material 3 Design**.

---

## ✨ Key Features

### 1. 📊 Expense & Cashflow Tracking
- Add, categorize, and audit daily transactions with currency formatting.
- Instant category tagging (Food & Dining, Groceries, Shopping, Transport, Bills, Entertainment, Health, Investments, etc.).
- Filter and search transactions by date, merchant, note, and category.

### 2. 💳 Check Payment Methods & Rail Breakdown
- Native tracking across all primary payment rails:
  - 💳 **Credit Cards** (Tracked by individual card, billing cycle, credit limit)
  - 📱 **UPI / Instant Bank Transfer** (GPay, PhonePe, Paytm)
  - 🏧 **Debit Cards**
  - 💵 **Cash Wallet**
  - 🌐 **Net Banking**
- Visual breakdown of spending percentage across payment rails in the Analytics view.

### 3. 📈 Beautiful Visual Analytics & Deep Insights
- **Interactive Donut Chart**: Touch-responsive category breakdown with percentage badges.
- **6-Month Spending Trend Bar Chart**: Side-by-side comparison of income vs expenses trajectory.
- **Savings Ratio Meter**: Calculates real-time savings rate and net disposable surplus.
- **Key Metrics**: Average daily burn rate, top spending categories, and projected monthly expense.

### 4. 💳 Dedicated Credit Card Command Center
*(Requirement: "how much paid using credit card")*
- **Total Credit Card Spend**: Real-time aggregation of all credit card charges for the current billing cycle.
- **Realistic Credit Card Carousel**: Visual card graphics with bank branding, masked numbers (`•••• •••• •••• 4892`), and metallic EMV chip.
- **Credit Limit & Utilization Meter**: Color-coded progress bar (🟢 Green <30% Safe, 🟡 Amber 30-60%, 🔴 Red >60% Alert) to protect credit scores.
- **Billing Cycle & Due Date Countdown**: Statement generation day and payment due countdown.
- **1-Tap Settle / Pay Bill**: Dialog to record bill settlement transfers directly from bank account.

### 5. 🔄 Recurring Credits & Debits Engine
*(Requirement: "include recurring credits and debits like salary, loan and all")*
- **Recurring Credits**: Monthly salary credits, rental income, and freelance retainers.
- **Loan EMI Tracker**:
  - Home Loan EMI, Car Loan EMI, Personal Loan EMI.
  - Visual tenure progress bars (e.g., *Paid 34 of 240 months*).
  - Total principal paid so far vs remaining balance.
  - 1-tap "Record EMI Paid" action.
- **Fixed Outflows & Subscriptions**:
  - Netflix, Spotify, Prime Video, High-speed fiber internet, Mutual Fund SIPs.
- **Committed Cashflow Banner**: Calculates Net Monthly Fixed Surplus (Fixed Inflow minus Fixed Outflows).

---

## 🛠️ Architecture & Tech Stack

- **UI**: 100% Declarative UI with Jetpack Compose & Material 3.
- **Architecture**: MVVM + Clean Architecture with Repository Pattern.
- **Reactive Streams**: Kotlin Coroutines & `StateFlow`.
- **Database**: Room Database with TypeConverters, Foreign Keys, and reactive queries.
- **Data Privacy**: 100% Offline-First. No financial data leaves the user's device.

---

## 🚀 How to Run in Android Studio

1. Open **Android Studio**.
2. Click **Open** and select this directory: `c:\Users\deeks\Repos\Money-Tracker`.
3. Wait for Android Studio to sync Gradle dependencies (it will automatically download Gradle 8.10.2).
4. Select an Android Emulator (API 34 or 35) or connect a physical Android device with USB Debugging enabled.
5. Click the green **Run (▶)** button!
