# Navigation Architecture

## TL;DR
Single Activity with Jetpack Compose Navigation. Bottom nav for main screens, standard back navigation for forms. Type-safe route constants.

## Navigation Setup

### NavHost Configuration
Located in `MainActivity.kt:132-184`.

```kotlin
NavHost(
    navController = navController,
    startDestination = Routes.HOME,
    modifier = Modifier.padding(innerPadding)
) {
    composable(Routes.HOME) { HomeScreen(...) }
    composable(Routes.MEDICATIONS) { MedicationsLibraryScreen(...) }
    composable(Routes.STATS) { StatsScreen(...) }
    composable(Routes.SETTINGS) { SettingsScreen(...) }
    composable(
        route = "add_medication?id={medId}",
        arguments = listOf(navArgument("medId") { ... })
    ) { AddEditMedicationScreen(...) }
}
```

## Routes

### Route Constants
**Location**: `app/src/main/java/com/example/medicaladherence/ui/nav/NavGraph.kt:6-13`

```kotlin
object Routes {
    const val HOME = "home"
    const val MEDICATIONS = "medications"
    const val ADD_MEDICATION = "add_medication"
    const val EDIT_MEDICATION = "add_medication?id={medId}"
    const val STATS = "stats"
    const val SETTINGS = "settings"
}
```

### Route Types

**Simple Routes** (no arguments):
- `home` - Home screen
- `medications` - Medications library
- `stats` - Statistics screen
- `settings` - Settings screen

**Parameterized Routes**:
- `add_medication?id={medId}` - Add (id=null) or Edit (id=medicationId)

## Navigation Patterns

### Bottom Navigation
**Location**: `MainActivity.kt:52-128`

Shows on main screens:
```kotlin
val routesWithBottomBar = listOf(
    Routes.HOME,
    Routes.MEDICATIONS,
    Routes.STATS,
    Routes.SETTINGS
)
```

Four tabs:
1. **Home** (house icon)
2. **Medications** (💊 emoji)
3. **Stats** (📊 emoji)
4. **Settings** (gear icon)

### Back Navigation
Used for:
- Add/Edit medication screen
- Returning from Stats (though also in bottom nav)

**Implementation**: `TopAppBar` with back icon
```kotlin
navigationIcon = {
    IconButton(onClick = onNavigateBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
    }
}
```

## Navigation Flow

### User Journeys

**Add Medication**:
```
Home → FAB (+) → AddEditScreen → Save → Home
Medications → FAB (+) → AddEditScreen → Save → Medications
```

**Edit Medication**:
```
Home → DoseCard → ⋮ Menu → Edit → AddEditScreen → Save → Home
Medications → Card → ⋮ Menu → Edit → AddEditScreen → Save → Medications
```

**View Stats**:
```
Any Screen → Bottom Nav (Stats) → StatsScreen
```

## Navigation Arguments

### Optional medicationId
Add/Edit screen accepts optional ID parameter.

**Configuration**: `MainActivity.kt:155-170`
```kotlin
composable(
    route = "add_medication?id={medId}",
    arguments = listOf(
        navArgument("medId") {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        }
    )
) { backStackEntry →
    val medId = backStackEntry.arguments?.getString("medId")
    AddEditMedicationScreen(
        medicationId = medId,
        onNavigateBack = { navController.popBackStack() }
    )
}
```

**Usage**:
- Navigate to `add_medication` → Add mode (medId = null)
- Navigate to `add_medication?id=abc-123` → Edit mode

## NavController Usage

### Passing to Screens
Screens receive navigation callbacks, not NavController directly:

```kotlin
HomeScreen(
    onNavigateToAdd = { navController.navigate(Routes.ADD_MEDICATION) },
    onNavigateToEdit = { medId →
        navController.navigate("add_medication?id=$medId")
    }
)
```

**Benefits**:
- Testable screens (mock callbacks)
- Single responsibility (screens don't know about nav)
- Easier to refactor navigation

### Navigation Options

**Single Top Launch**:
```kotlin
onClick = {
    navController.navigate(Routes.HOME) {
        launchSingleTop = true
    }
}
```
Prevents duplicate instances.

**Pop Up To**:
```kotlin
navController.navigate(Routes.HOME) {
    popUpTo(Routes.HOME) { inclusive = true }
    launchSingleTop = true
}
```
Clears back stack up to destination.

**Simple Pop Back**:
```kotlin
onNavigateBack = { navController.popBackStack() }
```
Returns to previous screen.

## Bottom Bar Visibility

### Conditional Display
Bottom bar hidden on Add/Edit screen.

**Logic**: `MainActivity.kt:44-50`
```kotlin
val currentRoute = navBackStackEntry?.destination?.route
val showBottomBar = currentRoute in routesWithBottomBar
```

**Effect**: Full-screen form when adding/editing medications.

## State Management

### NavController State
```kotlin
val navController = rememberNavController()
val navBackStackEntry by navController.currentBackStackEntryAsState()
val currentRoute = navBackStackEntry?.destination?.route
```

**Use Cases**:
- Highlight current tab in bottom nav
- Show/hide bottom bar
- Conditional UI based on current screen

## Deep Linking

### Not Currently Implemented
Future support could add:
```kotlin
composable(
    route = "home",
    deepLinks = listOf(
        navDeepLink { uriPattern = "medadherence://home" }
    )
) { HomeScreen(...) }
```

## Navigation Testing

### Test Navigation
```kotlin
@Test
fun testNavigationToAddScreen() {
    composeTestRule.setContent {
        val navController = rememberNavController()
        MedicalAdherenceApp(navController)
    }

    composeTestRule.onNodeWithContentDescription("Add Medication").performClick()
    // Assert on AddEditScreen elements
}
```

## Back Stack Management

### Current Behavior
- Bottom nav: Replaces current screen (no back stack build-up)
- FAB → Add screen: Adds to back stack
- Edit → Back: Pops to previous screen

### Preventing Back Stack Issues
Single top launch prevents duplicates:
```kotlin
navController.navigate(Routes.MEDICATIONS) {
    launchSingleTop = true  // Don't duplicate if already on Medications
}
```

## Screen Transitions

### Default Animations
Compose Navigation provides default animations:
- **Enter**: Slide from right, fade in
- **Exit**: Slide to left, fade out
- **Pop Enter**: Slide from left
- **Pop Exit**: Slide to right

### Custom Animations
Not currently implemented, but possible:
```kotlin
composable(
    route = Routes.HOME,
    enterTransition = { slideInHorizontally() },
    exitTransition = { slideOutHorizontally() }
) { HomeScreen(...) }
```

## Route Extensions

### Helper Functions (Not Implemented)
Could add for cleaner code:
```kotlin
object Routes {
    fun editMedication(id: String) = "add_medication?id=$id"
}

// Usage:
navController.navigate(Routes.editMedication(medId))
```

## Navigation Error Handling

### Invalid Routes
Navigation library handles invalid routes gracefully:
- Logs error
- Doesn't crash
- Stays on current screen

### Missing Arguments
Nullable with default:
```kotlin
navArgument("medId") {
    nullable = true
    defaultValue = null
}
```

## Best Practices Used

1. **Type-safe routes**: Constants in `Routes` object
2. **Callback pattern**: Screens receive callbacks, not NavController
3. **Single source**: Navigation graph in MainActivity
4. **Conditional UI**: Bottom bar visibility based on route
5. **Back stack management**: Single top launch where appropriate
6. **Argument validation**: Nullable types with defaults
