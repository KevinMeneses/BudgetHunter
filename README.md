# BudgetHunter 📱💰

A modern **cross-platform** budget tracking application built with Kotlin Multiplatform that helps you manage your finances on both Android and iOS with intelligent features including AI-powered receipt scanning and automatic SMS transaction detection.

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![iOS](https://img.shields.io/badge/Platform-iOS-lightgrey.svg)](https://developer.apple.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![KMP](https://img.shields.io/badge/Kotlin-Multiplatform-purple.svg)](https://kotlinlang.org/docs/multiplatform.html)
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
- **Cross-Platform**: Shared codebase for Android and iOS with Kotlin Multiplatform
- **Dark/Light Theme**: Adaptive UI that follows system preferences
- **Offline-First**: Local SQLite database with SqlDelight for reliable data storage
- **Modern UI**: Built with Compose Multiplatform for a smooth, native experience on both platforms
- **Real-time Updates**: Reactive UI with StateFlow and Compose integration
- **Configurable AI Processing**: Toggle AI features on/off in settings

## 🛠️ Tech Stack

### Architecture & Patterns
- **Kotlin Multiplatform (KMP)** - Shared business logic across Android and iOS
- **MVI (Model-View-Intent)** - Unidirectional data flow architecture
- **Clean Architecture** - Separation of concerns with domain, data, and presentation layers
- **Repository Pattern** - Data abstraction layer
- **Dependency Injection** - Koin with KSP annotation processing

### Core Technologies
- **Kotlin 2.1.0** - Primary programming language
- **Compose Multiplatform** - Modern declarative UI toolkit for Android and iOS
- **SqlDelight 2.0.2** - Type-safe SQL database library with multiplatform drivers
- **Navigation Compose** - Type-safe navigation (both AndroidX and JetBrains multiplatform)
- **DataStore** - Multiplatform preferences storage

### Key Libraries
- **Google Generative AI (Gemini 2.5 Flash)** - AI-powered receipt processing
- **Ktor 2.3.12** - HTTP client and WebSocket communication
- **Kotlinx Serialization 1.7.3** - JSON serialization
- **Compottie 2.0.0** - Lottie animations for Compose Multiplatform
- **Material 3** - Modern Material Design components
- **Koin 4.0.0** - Dependency injection framework

### Development & Quality Tools
- **Ktlint** - Kotlin code style checking and formatting
- **JUnit & MockK** - Unit testing framework
- **Jacoco** - Code coverage reporting
- **Trivy** - Security vulnerability scanning
- **GitHub Actions** - Comprehensive CI/CD pipeline with automated Play Store deployment

## 🚀 Getting Started

### Prerequisites

#### For Android Development
- Android Studio Koala or later with Kotlin Multiplatform plugin
- JDK 17 (required)
- Android SDK with:
  - minSdk: 26
  - targetSdk: 36
  - compileSdk: 36

#### For iOS Development
- macOS with Xcode 14.0 or later
- CocoaPods (for iOS dependencies)
- iOS 14.0+ for deployment target

#### Required API Keys
- Google Gemini API key (for AI-powered receipt scanning)

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

   **For Android:**
   ```bash
   ./gradlew build
   ./gradlew installDebug
   ```

   **For iOS:**
   ```bash
   # Open the iOS project in Xcode
   open iosApp/iosApp.xcodeproj
   # Then build and run from Xcode
   ```

## 🏗️ Project Structure

```
BudgetHunter/
├── composeApp/                          # Multiplatform shared code
│   └── src/
│       ├── commonMain/                  # Shared code for all platforms
│       │   └── kotlin/com/meneses/budgethunter/
│       │       ├── budgetList/          # Budget listing and management
│       │       ├── budgetDetail/        # Individual budget view and editing
│       │       ├── budgetEntry/         # Transaction entry creation/editing
│       │       ├── budgetMetrics/       # Analytics and spending insights
│       │       ├── settings/            # App configuration and preferences
│       │       ├── sms/                 # SMS transaction parsing (Android)
│       │       ├── navigation/          # Navigation handling
│       │       ├── commons/             # Shared utilities and UI components
│       │       ├── splash/              # App initialization
│       │       ├── theme/               # Design system and theming
│       │       └── di/                  # Dependency injection (Koin)
│       ├── androidMain/                 # Android-specific implementations
│       └── iosMain/                     # iOS-specific implementations
├── iosApp/                              # iOS native app wrapper
│   └── iosApp.xcodeproj/                # Xcode project
└── gradle/
    └── libs.versions.toml               # Centralized dependency management
```

Each feature module follows **Clean Architecture** with:
- `application/` - ViewModels and presentation logic
- `data/` - Repository implementations and data sources
- `domain/` - Business logic and use cases
- `ui/` - Compose UI components

## 🔧 Development Commands

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test
./gradlew connectedAndroidTest

# Code coverage
./gradlew testDebugUnitTestCoverage  # Generate Jacoco coverage report

# Code style
./gradlew ktlint          # Check code style
./gradlew ktlintFormat    # Auto-fix style issues

# Clean build
./gradlew clean

# Android-specific
./gradlew installDebug    # Install debug APK
./gradlew bundleRelease   # Build release AAB for Play Store
```

## 🚀 CI/CD & Deployment

The project includes comprehensive GitHub Actions workflows:

- **PR Checks** - Automated linting, building, and testing on pull requests
- **Development** - Continuous integration on the master branch
- **Release** - Automated Play Store deployment to Beta track with security scanning
- **Security** - Trivy vulnerability scanning with SARIF reports

### Play Store Deployment
Release builds are automatically deployed to the **Beta track** on Google Play Store when tagged releases are created.

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
