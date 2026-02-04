# Crypto Wallet Android App

A simple Android application demonstrating Web3 authentication and wallet functionality using the [Dynamic SDK](https://dynamic.xyz/).

## Features
- **Email OTP Login**: Secure passwordless authentication.
- **Wallet Details**: View EVM wallet address and Sepolia ETH balance.
- **Send Transaction**: Send ETH on the Sepolia testnet with transaction tracking.
- **Copy Address**: Easily copy wallet address to clipboard.

## Architecture
The app follows modern Android development best practices:
- **MVVM (Model-View-ViewModel)**: Separates UI from business logic.
- **Jetpack Compose**: Declarative UI toolkit for building native interfaces.
- **Hilt**: Dependency Injection for managing components.
- **Kotlin Coroutines & Flow**: For asynchronous operations and reactive state management.
- **Repository Pattern**: Abstraction layer for data sources (Dynamic SDK).

## Tech Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose, Material 3
- **DI**: Hilt
- **Async**: Coroutines, Flow
- **SDK**: Dynamic SDK for Android
- **Blockchain**: Ethereum Sepolia Testnet

## How to Run
1. Open the project in **Android Studio**.
2. Sync the project with Gradle files.
3. Select an emulator or physical device.
4. Click the **Run** button (green arrow).

## Setup
The project comes pre-configured with a Dynamic SDK Environment ID. 
If you wish to use your own environment:
1. Go to `MainActivity.kt`.
2. Update the `environmentId` in `ClientProps`.

## Assumptions
- The app targets the **Sepolia** testnet (Chain ID: 11155111).
- The user has a stable internet connection for SDK operations.
- Gas fees are estimated with hardcoded fallbacks (3 Gwei) if simpler estimation fails, but standard transfers usually work.

## Screens
### Login Screen
- Email input
- OTP Verification Dialog

### Wallet Details
- Shows Balance and Address
- Navigation to Send Transaction

### Send Transaction
- Recipient Address Input
- Amount Input (ETH)
- Success/Error Feedback
