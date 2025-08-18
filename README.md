# BudgetHunter 📱💰

A modern Android budget tracking application that helps you manage your finances with intelligent features including AI-powered receipt scanning and automatic SMS transaction detection.

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg)](https://android-arsenal.com/api?level=26)

## ✨ Features

### Core Functionality
- **Budget Management**: Create, edit, and delete budgets with customizable names and amounts
- **Expense Tracking**: Add income and expense entries with detailed categorization
- **Visual Analytics**: View spending patterns with interactive pie charts and metrics
- **Multi-language Support**: Available in English and Spanish

### Smart Features
- **🤖 AI Receipt Scanning**: Use Google Gemini AI to extract transaction details from receipt images and PDFs
- **📱 SMS Transaction Detection**: Automatically detect and parse bank transaction SMS messages
- **🏦 Multi-Bank Support**: Configurable SMS parsing for different bank formats
- **📊 Real-time Metrics**: Track spending by category with visual breakdowns

### Technical Features
- **Dark/Light Theme**: Adaptive UI that follows system preferences
- **Offline-First**: Local SQLite database with SqlDelight for reliable data storage
- **Modern UI**: Built with Jetpack Compose for a smooth, native Android experience
- **Real-time Updates**: Reactive UI with StateFlow and Compose integration

## 🛠️ Tech Stack

### Architecture & Patterns
- **MVI (Model-View-Intent)** - Unidirectional data flow architecture
- **Clean Architecture** - Separation of concerns with domain, data, and presentation layers
- **Repository Pattern** - Data abstraction layer

### Core Technologies
- **Kotlin** - Primary programming language
- **Jetpack Compose** - Modern declarative UI toolkit
- **SqlDelight** - Type-safe SQL database library
- **Navigation Compose** - Type-safe navigation

### Key Libraries
- **Google Generative AI (Gemini)** - AI-powered receipt processing
- **Ktor** - WebSocket communication
- **Kotlinx Serialization** - JSON serialization
- **Lottie** - Animations
- **Material 3** - Modern Material Design components

### Development Tools
- **Ktlint** - Kotlin code style checking
- **JUnit & MockK** - Unit testing
- **GitHub Actions** - CI/CD pipeline

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- JDK 17 or higher
- Android SDK with API level 26+
- Google Gemini API key (for AI features)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/kevinmeneses/BudgetHunter.git
   cd BudgetHunter
   ```

2. **Set up API Keys**
   Create a `local.properties` file in the root directory:
   ```properties
   GEMINI_API_KEY=your_gemini_api_key_here
   ```

3. **Build and run**
   ```bash
   ./gradlew build
   ./gradlew installDebug
   ```

## 🏗️ Project Structure

```
app/src/main/java/com/meneses/budgethunter/
├── budgetList/          # Main budget listing and management
├── budgetDetail/        # Individual budget view and editing
├── budgetEntry/         # Transaction entry creation/editing
├── budgetMetrics/       # Analytics and spending insights
├── settings/            # App configuration and preferences
├── sms/                 # SMS transaction parsing
├── commons/             # Shared utilities and UI components
├── splash/              # App initialization
└── theme/               # Design system and theming
```

## 🔧 Development Commands

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test
./gradlew connectedAndroidTest

# Code style
./gradlew ktlint          # Check code style
./gradlew ktlintFormat    # Auto-fix style issues

# Clean build
./gradlew clean
```

## 📱 Screenshots

*Screenshots coming soon*

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📧 Contact

Kevin Meneses - kevmeneses@gmail.com

Project Link: [https://github.com/kevinmeneses/BudgetHunter](https://github.com/kevinmeneses/BudgetHunter)
