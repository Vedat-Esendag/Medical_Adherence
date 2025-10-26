# Medical Adherence App - Documentation

Complete documentation for understanding and working with the Medical Adherence Android app.

## 📄 Quick Start

**New to the project?** Start here:
- [**PROJECT_OVERVIEW.md**](PROJECT_OVERVIEW.md) - **2-page printable high SNR overview** covering essentials of both user and technical aspects

## 👥 User Documentation

For users, caregivers, and anyone learning to use the app:

1. [**Overview**](user/00-overview.md) - What the app does and quick start guide
2. [**Home Screen**](user/01-home-screen.md) - Daily dose tracking and quick actions
3. [**Medications**](user/02-medications.md) - Managing your medication library
4. [**Statistics**](user/03-statistics.md) - Understanding adherence metrics
5. [**Settings**](user/04-settings.md) - Customizing font size and theme
6. [**Tips & Best Practices**](user/05-tips-best-practices.md) - Maximizing medication adherence
7. [**Accessibility**](user/06-accessibility.md) - Elderly-friendly design features

## 🔧 Technical Documentation

For developers, architects, and contributors:

### Architecture & Design
1. [**Architecture**](technical/00-architecture.md) - MVVM pattern, project structure, data flow
2. [**Tech Stack**](technical/01-tech-stack.md) - Dependencies, versions, build tools
3. [**Data Models**](technical/02-data-models.md) - Medication, DoseEvent, Room entities
4. [**Navigation**](technical/03-navigation.md) - Compose Navigation, routes, flows
5. [**State Management**](technical/04-state-management.md) - StateFlow, ViewModels, reactive UI

### Implementation Details
6. [**Database**](technical/05-database.md) - Room setup, DAOs, queries, Flow
7. [**UI Components**](technical/06-ui-components.md) - Composables, Material 3, theming
8. [**Build & Deploy**](technical/07-build-deploy.md) - Gradle, APK builds, signing

### Testing & Maintenance
9. [**Testing**](technical/08-testing.md) - Unit tests, instrumented tests, strategies
10. [**Migrations**](technical/09-migrations.md) - Database schema evolution

## 📂 Documentation Structure

```
docs/
├── README.md                     # This file
├── PROJECT_OVERVIEW.md           # ⭐ 2-page printable overview
│
├── user/                         # User guides (7 files)
│   ├── 00-overview.md
│   ├── 01-home-screen.md
│   ├── 02-medications.md
│   ├── 03-statistics.md
│   ├── 04-settings.md
│   ├── 05-tips-best-practices.md
│   └── 06-accessibility.md
│
└── technical/                    # Technical docs (9 files)
    ├── 00-architecture.md
    ├── 01-tech-stack.md
    ├── 02-data-models.md
    ├── 03-navigation.md
    ├── 04-state-management.md
    ├── 05-database.md
    ├── 06-ui-components.md
    ├── 07-build-deploy.md
    ├── 08-testing.md
    └── 09-migrations.md
```

## 🎯 Quick References

### For Users
- **First time setup**: [user/00-overview.md](user/00-overview.md#getting-started)
- **How to add medication**: [user/02-medications.md](user/02-medications.md#adding-a-medication)
- **Understanding stats**: [user/03-statistics.md](user/03-statistics.md#main-percentage-card)
- **Accessibility features**: [user/06-accessibility.md](user/06-accessibility.md)

### For Developers
- **Building the app**: [technical/07-build-deploy.md](technical/07-build-deploy.md#building-from-command-line)
- **Database queries**: [technical/05-database.md](technical/05-database.md#daos-data-access-objects)
- **Adding a screen**: [technical/03-navigation.md](technical/03-navigation.md#navigation-patterns)
- **Writing tests**: [technical/08-testing.md](technical/08-testing.md#unit-testing)
- **Schema changes**: [technical/09-migrations.md](technical/09-migrations.md#production-migration-strategy)

## 🔍 Finding Specific Topics

### User Topics
- **Daily tracking workflow** → user/01-home-screen.md
- **Snooze feature** → user/01-home-screen.md, user/05-tips-best-practices.md
- **Streak counter explained** → user/03-statistics.md
- **Large font setting** → user/04-settings.md, user/06-accessibility.md
- **Missed doses** → user/01-home-screen.md, user/03-statistics.md

### Technical Topics
- **StateFlow pattern** → technical/04-state-management.md
- **Room type converters** → technical/05-database.md, technical/02-data-models.md
- **Compose components** → technical/06-ui-components.md
- **ViewModel lifecycle** → technical/04-state-management.md
- **Navigation arguments** → technical/03-navigation.md
- **Migration testing** → technical/09-migrations.md

## 📊 Documentation Stats

- **Total files**: 17 (1 overview + 7 user + 9 technical)
- **Format**: Markdown with code examples
- **Style**: TL;DR summaries with detailed explanations
- **Code references**: File locations and line numbers included
- **Average length**: ~300-500 words per file (moderate detail)

## 🤝 Contributing to Docs

When updating documentation:
1. Maintain TL;DR section at top of each file
2. Include file locations for code references
3. Add code examples for complex concepts
4. Update this README if adding new sections
5. Keep PROJECT_OVERVIEW.md in sync with major changes

## 📋 License

Documentation follows the same license as the project (prototype/demonstration).

---

**Last Updated**: October 2025
**Version**: 1.0
**App Version**: 1.0
