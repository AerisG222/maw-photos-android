# AGENTS.md

## Project Overview
MawPhotos is an Android application that allows users to view photos and videos from a variety of categories.

## Technology Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM / Clean Architecture guidelines (implied by `core`, `feature`, `data` structure)
- **Dependency Injection**: Hilt
- **Navigation**: Type-Safe Navigation (Jetpack Navigation).
- **Image Loading**: Coil
- **Network**: Retrofit
- **Http**: OKHttp
- **Build System**: Gradle (Kotlin DSL)

## Development Workflow

### Prerequisites
- Android Studio (Latest Stable or Preview as needed for bleeding edge features)
- JDK 17+ (Standard for modern Android dev)

## Key Guidelines for Agents
1. **Compose First**: adhere to modern Jetpack Compose best practices.
2. **Material 3**: The app uses Material 3. Look for ways to use Material 3 Expressive components where appropriate.
3. **Preview**: Create @Preview functions for your Composables to ensure they render correctly.
4. Place all business logic in ViewModels.
5. Always follow official architecture recommendations, including use of a layered architecture. Use a unidirectional data flow (UDF), ViewModels, lifecycle-aware UI state collection, and other recommendations.
