# UI Components & Compose Patterns

## TL;DR
Material 3 Compose components with custom `DoseCard` for medication tracking. Reusable composables, modifiers for accessibility, and theme system.

## Custom Components

### DoseCard
Reusable card component for displaying medication doses.

**Location**: `app/src/main/java/com/example/medicaladherence/ui/components/DoseCard.kt`

```kotlin
@Composable
fun DoseCard(
    dose: DoseItem,
    onTaken: () -> Unit,
    onMissed: () -> Unit,
    onSnooze: () -> Unit,
    onUndo: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
)
```

**Features**:
- Two states: expanded (action buttons) or collapsed (status chip + undo)
- Three action buttons: Taken, Missed, Snooze 15m
- Overflow menu: Edit, Delete
- Delete confirmation dialog
- Large touch targets (≥48dp)

**Implementation**:
```kotlin
ElevatedCard {
    Row {
        Text("💊")  // Pill icon
        Column {
            Text(dose.medication.name)
            Text("${dose.medication.dosage} at ${dose.time}")
        }
        IconButton { /* Overflow menu */ }
    }

    // Conditional rendering
    if (dose.taken != null) {
        // Collapsed: chip + undo button
        AssistChip(label = "✓ Taken")
        TextButton(onClick = onUndo) { Text("Undo") }
    } else {
        // Expanded: action buttons
        Button(onClick = onTaken) { Text("Taken") }
        OutlinedButton(onClick = onMissed) { Text("Missed") }
        FilledTonalButton(onClick = onSnooze) { Text("Snooze 15m") }
    }
}
```

### MedicationLibraryCard
Display card for medications library.

**Location**: `MedicationsLibraryScreen.kt:130-237`

```kotlin
@Composable
fun MedicationLibraryCard(
    medication: Medication,
    onEdit: () -> Unit,
    onDelete: () -> Unit
)
```

**Features**:
- Pill icon
- Name and dosage
- Full schedule display (all times)
- Notes (if present)
- Edit/Delete menu

## Material 3 Components Used

### Cards
- **ElevatedCard**: Main content cards with elevation
- **Card**: Secondary cards (stats)

```kotlin
ElevatedCard(
    modifier = Modifier.fillMaxWidth(),
    elevation = CardDefaults.elevatedCardElevation(
        defaultElevation = 2.dp,
        pressedElevation = 4.dp
    )
)
```

### Buttons
- **Button**: Primary actions (Taken, Save)
- **OutlinedButton**: Secondary actions (Missed, Cancel)
- **FilledTonalButton**: Tertiary actions (Snooze)
- **TextButton**: Low-priority actions (Undo, Cancel)
- **IconButton**: Menu triggers

```kotlin
Button(
    onClick = onTaken,
    modifier = Modifier
        .weight(1f)
        .heightIn(min = 48.dp)  // Accessibility
) {
    Text("Taken")
}
```

### Text Fields
- **OutlinedTextField**: All form inputs

```kotlin
OutlinedTextField(
    value = uiState.name,
    onValueChange = { viewModel.updateName(it) },
    label = { Text("Medication Name") },
    isError = uiState.nameError != null,
    supportingText = uiState.nameError?.let { { Text(it) } },
    modifier = Modifier.fillMaxWidth()
)
```

### Navigation
- **NavigationBar**: Bottom navigation
- **NavigationBarItem**: Individual tabs

```kotlin
NavigationBar {
    NavigationBarItem(
        icon = { Icon(Icons.Filled.Home, "Home") },
        label = { Text("Home") },
        selected = currentRoute == Routes.HOME,
        onClick = { navController.navigate(Routes.HOME) }
    )
}
```

### Dialogs
- **AlertDialog**: Confirmations (delete medication)

```kotlin
AlertDialog(
    onDismissRequest = { showDialog = false },
    title = { Text("Delete Medication?") },
    text = { Text("This action cannot be undone.") },
    confirmButton = {
        Button(onClick = onConfirm) { Text("Delete") }
    },
    dismissButton = {
        TextButton(onClick = { showDialog = false }) {
            Text("Cancel")
        }
    }
)
```

### Other Components
- **FloatingActionButton**: Add medication
- **Scaffold**: Screen structure
- **TopAppBar**: Screen titles
- **Snackbar**: Feedback messages
- **LinearProgressIndicator**: Stats progress bar
- **RadioButton**: Settings options
- **AssistChip**: Status display

## Layout Composables

### LazyColumn
For scrollable lists.

```kotlin
LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
) {
    items(doses) { dose →
        DoseCard(dose = dose, ...)
    }
}
```

### Spacer
For consistent spacing.

```kotlin
Spacer(modifier = Modifier.height(16.dp))
Spacer(modifier = Modifier.width(8.dp))
```

### Row & Column
For layouts.

```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    Text("Label")
    Text("Value")
}
```

## Modifiers

### Accessibility Modifiers
```kotlin
Button(
    modifier = Modifier
        .fillMaxWidth()
        .height(56.dp)  // Large touch target
)
```

### Common Patterns
```kotlin
// Full width with padding
modifier = Modifier
    .fillMaxWidth()
    .padding(16.dp)

// Weight for flex layout
modifier = Modifier.weight(1f)

// Minimum height for touch targets
modifier = Modifier.heightIn(min = 48.dp)

// Spacing between items
verticalArrangement = Arrangement.spacedBy(16.dp)
```

## Theme System

### Material 3 Theme
**Location**: `app/src/main/java/com/example/medicaladherence/ui/theme/Theme.kt`

```kotlin
@Composable
fun MedicalAdherenceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    fontScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = scaledTypography(fontScale),
        content = content
    )
}
```

### Color Scheme
**Location**: `app/src/main/java/com/example/medicaladherence/ui/theme/Color.kt`

```kotlin
val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0D47A1),       // Soothing blue
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBBDEFB),
    // ... other colors
)
```

### Typography
**Location**: `app/src/main/java/com/example/medicaladherence/ui/theme/Type.kt`

```kotlin
val Typography = Typography(
    displayLarge = TextStyle(
        fontSize = 57.sp,
        lineHeight = 64.sp,
        fontWeight = FontWeight.Bold
    ),
    headlineMedium = TextStyle(
        fontSize = 28.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Bold
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    // ... other text styles
)
```

### Font Scaling
Custom typography scaling for accessibility:

```kotlin
fun scaledTypography(scale: Float): Typography {
    return Typography(
        displayLarge = Typography.displayLarge.copy(
            fontSize = Typography.displayLarge.fontSize * scale
        ),
        // ... scale all text styles
    )
}
```

## State Hoisting

### Pattern
State hoisted to ViewModel, passed down to components:

```kotlin
// ViewModel
val uiState by viewModel.uiState.collectAsState()

// Screen passes state + callbacks to component
DoseCard(
    dose = dose,
    onTaken = { viewModel.markTaken(...) }
)

// Component receives state, calls callbacks
@Composable
fun DoseCard(
    dose: DoseItem,
    onTaken: () -> Unit
) {
    Button(onClick = onTaken)
}
```

## Previews

### Compose Previews
Used for component development:

```kotlin
@Preview(showBackground = true)
@Composable
fun DoseCardPreview() {
    MedicalAdherenceTheme {
        DoseCard(
            dose = DoseItem(
                medication = Medication(...),
                time = "07:00",
                taken = null
            ),
            onTaken = {},
            onMissed = {},
            onSnooze = {},
            onUndo = {},
            onEdit = {},
            onDelete = {}
        )
    }
}
```

## Accessibility Features

### Large Touch Targets
All interactive elements ≥48dp:

```kotlin
Button(
    modifier = Modifier.heightIn(min = 48.dp)
)

IconButton(
    modifier = Modifier.size(48.dp)
)
```

### Content Descriptions
For screen readers:

```kotlin
Icon(
    Icons.Default.Home,
    contentDescription = "Home"
)
```

### Semantic Structure
Proper heading hierarchy:

```kotlin
Text(
    text = "Today's Doses",
    style = MaterialTheme.typography.titleLarge  // H2
)

Text(
    text = "Amlodipine",
    style = MaterialTheme.typography.titleMedium  // H3
)
```

## Custom Composables

### WeeklyBarChart
Custom chart for statistics.

**Location**: `StatsScreen.kt:257-302`

```kotlin
@Composable
fun WeeklyBarChart(data: List<DayBar>) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { dayBar →
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${dayBar.percentage}%")
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height((dayBar.percentage * 1.5f).dp)
                        .background(color)
                )
                Text(dayBar.dayLabel)
            }
        }
    }
}
```

## Best Practices

1. **Component reusability**: Extract reusable UI into composables
2. **State hoisting**: State in ViewModel, not in composables
3. **Accessibility**: Large touch targets, content descriptions
4. **Material 3**: Use standard components when possible
5. **Typography**: Use theme typography, not hardcoded sizes
6. **Colors**: Use theme colors, not hardcoded colors
7. **Modifiers**: Extract common modifiers to constants
8. **Previews**: Add previews for all custom components
9. **Immutable params**: Pass immutable data to composables
10. **Remember**: Use `remember` for expensive calculations
