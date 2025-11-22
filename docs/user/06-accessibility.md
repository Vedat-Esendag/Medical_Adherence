# Accessibility Features

## TL;DR
The app is designed with elderly users in mind: large touch targets, adjustable fonts, calm colors, and friendly language throughout.

## Large Touch Targets

### Minimum Size
All interactive elements are **≥48dp** (density-independent pixels):
- Buttons (Taken, Missed, Snooze)
- Bottom navigation icons
- FAB (floating action button)
- Menu icons (⋮)
- Radio buttons and checkboxes

### Why This Matters
- Easier to tap accurately
- Reduces frustration from mis-taps
- Accommodates reduced dexterity
- Works well with tremors or arthritis

### What's Affected
- All primary action buttons: 48dp minimum height
- Navigation bar items: Large tap areas
- Icon buttons: Generous padding around icons
- Form fields: Tall input boxes

## Font Scaling

### Adjustable Text Size
Two options in Settings:
- **Normal** (1.0x): Standard readable size
- **Large** (1.15x): 15% bigger for low vision

### Dynamic Scaling
Changes apply to:
- Medication names and dosages
- Button labels
- Headers and titles
- Body text and descriptions
- Statistics and percentages
- Navigation labels

### System Font Settings
App also respects Android's system font size:
- Settings > Display > Font size
- Scales proportionally with device setting
- Combined with app's Large setting if needed

## Color & Contrast

### Calm Color Palette
- **Primary**: Soothing blue (#0D47A1 family)
- **Backgrounds**: Soft whites/grays in light mode
- **Text**: High contrast black on light backgrounds
- **Not alarming**: No harsh reds for "Missed" - uses neutral gray

### Material 3 Compliance
- WCAG AA contrast ratios
- Readable text on all backgrounds
- Works in both light and dark modes
- Dynamic color support on Android 12+

### Color Coding
Used sparingly and always with text labels:
- **Green progress bar**: ≥80% adherence (also shows percentage)
- **Orange progress bar**: 60-79% (also shows percentage)
- **Blue bars**: Good daily adherence (also shows percentage)
- Never rely on color alone - always paired with text/numbers

### High Contrast Mode
Available in Settings for users who need maximum contrast:
- Pure black background (#000000)
- Pure white text for all content
- Eliminates subtle colors and grays
- Easier to read in bright sunlight or for low vision users
- Toggle on/off anytime in Settings without app restart
- Applies consistently across all screens

**How to Enable**:
1. Go to Settings screen
2. Find "High Contrast Mode" toggle
3. Switch to ON
4. All screens immediately update to high contrast

## Visual Design

### Card-Based Layout
- Clear separation between medications
- Generous padding and spacing
- One action per screen area
- Minimal clutter

### Typography
- **Clear hierarchy**: Large headers, medium body text
- **Sans-serif fonts**: Roboto for readability
- **Bold for emphasis**: Medication names, important numbers
- **Line height**: Comfortable spacing between lines

### Iconography
- Simple, recognizable icons (Home, Settings)
- Emoji for familiarity (💊 for medications)
- Text labels always accompany icons
- Material Icons standard set

## Friendly Language

### Non-Judgmental
Instead of harsh terms:
- ✅ "Missed" (neutral)
- ❌ NOT "Failed" (judgmental)
- ✅ "Let's work together to improve"
- ❌ NOT "You're doing poorly"

### Encouraging Feedback
- "Excellent work! Keep it up!" (90%+)
- "Great job!" (80%+)
- "Every dose you take helps your health!" (<60%)
- Focus on progress, not perfection

### Clear Instructions
- "Tap the + button to add your first medication"
- "This will permanently remove this medication"
- Simple, direct language
- No technical jargon

## Navigation

### Bottom Navigation Bar
- Always visible on main screens
- Large tap targets
- Clear icons with text labels
- Current screen highlighted
- Only 4 items (not overwhelming)

### Back Navigation
- Consistent back arrow in top left
- Cancel buttons on forms
- Clear exit points

### No Hidden Features
- All actions visible
- No hidden gestures required
- No complicated menus
- Everything accessible via taps

## Screen Reader Support

### Semantic Structure
- Proper heading hierarchy
- Content grouping in cards
- Labeled buttons and inputs
- Descriptive icons

### Content Descriptions
- All icons have text descriptions
- Button purposes clearly stated
- Images have alt text
- Forms have clear labels

### TalkBack Compatible
Works with Android's TalkBack screen reader:
- Reads button labels
- Announces card content
- Describes icons and actions
- Navigates logically through content

## Motor Accessibility

### No Precise Gestures
- No swiping required
- No long-press needed
- Simple taps only
- No drag-and-drop

### Button Spacing
- 8dp minimum between buttons
- Prevents accidental adjacent taps
- Easy to target single button

### No Time Pressure
- No timed interactions
- Take as long as needed to read
- Snooze available if need more time
- Undo available if mistake made

## Cognitive Accessibility

### Consistent Patterns
- Same layout on every medication card
- Bottom nav always in same position
- Save/Cancel buttons always at bottom
- Predictable behavior

### Clear Feedback
- Visual confirmation of actions (✓ Taken chip)
- Snackbar messages for important actions
- Progress indicators (percentages)
- Simple status displays

### Minimal Cognitive Load
- One task per screen
- Clear call-to-action buttons
- Limited choices (3 buttons max)
- No overwhelming information

### Forgiveness
- Undo button always available
- Confirmation dialogs for destructive actions
- Can edit medications anytime
- No penalty for mistakes

## Future Accessibility Improvements

### Planned Features
- Voice input for adding medications
- Enhanced medication reminder notifications
- Additional font scale options (1.3x, 1.5x beyond current 1.15x)
- Text-to-speech for dose reminders

### Under Consideration
- Text-to-speech for dose reminders
- Vibration patterns for notifications
- Photo upload for medication bottles
- Integration with phone's accessibility services

## Best Practices for Users

### For Low Vision
- Enable Large font in Settings
- Use device's font scaling (Settings > Display)
- Turn on dark mode in dim lighting
- Use TalkBack if needed

### For Motor Impairments
- Take time to carefully tap buttons
- Use Undo if you tap wrong button
- Rest device on stable surface when using
- Consider using stylus for precision

### For Cognitive Support
- Review Tips & Best Practices doc
- Ask family member to help set up medications initially
- Check Home screen regularly (build routine)
- Use Notes field for memory aids

## Testing & Feedback

The app was designed with elderly users in mind based on:
- Large touch target guidelines (Material Design)
- Senior-friendly UX patterns
- Plain language principles
- Accessibility best practices

Feedback welcome on further improvements to make the app more accessible for all users.
