# Contributing to Zdon

Thank you for your interest in contributing to Zdon! This document provides guidelines and instructions for contributing.

## Code of Conduct

This project adheres to a Code of Conduct. By participating, you are expected to uphold this code. Please read [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).

## How Can I Contribute?

### Reporting Bugs

Before creating bug reports, please check existing issues to avoid duplicates.

When creating a bug report, include:
- **Clear title and description**
- **Steps to reproduce**
- **Expected vs actual behavior**
- **Android version and device model**
- **App version**
- **Logs if available**

Use the bug report template when creating an issue.

### Suggesting Features

Feature suggestions are welcome! Please:
- Check if the feature has already been requested
- Provide a clear use case
- Explain why this feature would be useful
- Consider implementation complexity

Use the feature request template when creating an issue.

### Pull Requests

1. **Fork the repository**
2. **Create a feature branch** from `main`
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. **Make your changes**
4. **Follow code style** (see below)
5. **Write/update tests** if applicable
6. **Run lint and tests**
   ```bash
   ./gradlew lint
   ./gradlew test
   ```
7. **Commit with clear messages**
8. **Push to your fork**
9. **Open a Pull Request**

## Development Setup

### Prerequisites
- Android Studio Ladybug or newer
- JDK 17 or higher
- Android SDK with API 26+

### Building
```bash
git clone https://github.com/zuher5/Zdon.git
cd Zdon
./gradlew assembleDebug
```

### Running Tests
```bash
./gradlew test
./gradlew connectedAndroidTest
```

## Code Style

### Kotlin
- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Prefer immutability (val over var)
- Use extension functions where appropriate
- Document public APIs

### Compose
- Keep composables small and focused
- Extract reusable components
- Use remember for state
- Prefer stateless composables
- Follow Material 3 guidelines

### Architecture
- Follow Clean Architecture principles
- Keep modules decoupled
- Use dependency injection (Hilt)
- Repository pattern for data access
- ViewModels for UI state

## Project Structure

```
Zdon/
├── app/                    # Application module
├── core/
│   ├── common/            # Shared utilities
│   ├── data/              # Repositories
│   ├── database/          # Room DB
│   ├── datastore/         # Preferences
│   ├── designsystem/      # UI components
│   ├── downloader/        # Download logic
│   ├── engine/            # yt-dlp wrapper
│   └── model/             # Domain models
└── feature/
    ├── downloads/         # Downloads screen
    ├── history/           # History screen
    ├── home/              # Home screen
    └── settings/          # Settings screen
```

## Commit Messages

Use clear, descriptive commit messages:

```
feat: Add download speed limiter
fix: Resolve crash on playlist download
docs: Update installation instructions
refactor: Extract download state logic
test: Add unit tests for DownloadRepository
```

Prefixes:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation
- `style`: Code style changes
- `refactor`: Code refactoring
- `test`: Tests
- `chore`: Build/tooling

## Testing

- Write unit tests for business logic
- Write UI tests for critical flows
- Test on different Android versions
- Test different screen sizes
- Test ABI-specific builds

## Documentation

- Update README.md for user-facing changes
- Update docs/ for technical changes
- Add KDoc for public APIs
- Update CHANGELOG.md

## Review Process

Pull requests will be reviewed for:
- Code quality and style
- Test coverage
- Documentation
- Performance impact
- Security considerations
- Backward compatibility

## Getting Help

- Check [FAQ](docs/faq.md)
- Search existing issues
- Ask in discussions
- Review documentation

## License

By contributing, you agree that your contributions will be licensed under the MIT License.

Thank you for contributing! 🎉
