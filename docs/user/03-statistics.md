# Statistics - Understanding Your Adherence

## TL;DR
Track your medication adherence with weekly percentage, streak counter, daily breakdown, and encouraging feedback.

## Main Percentage Card

### This Week
- **Large percentage display**: 0-100% (e.g., "83%")
- **Progress bar**: Visual representation with color coding
  - Green (≥80%): Great adherence
  - Orange (60-79%): Okay adherence
  - Red (<60%): Needs improvement
- **Dose counter**: "X out of Y doses taken"
  - Example: "29 out of 35 doses taken"

### How It's Calculated
- Counts all scheduled doses for the past 7 days
- Divides taken doses by total doses
- Rounds to nearest whole percentage

## Quick Stats Cards

### Streak Counter
- **Flame icon** (🔥) for motivation
- Days in a row of 100% adherence
- Example: "5 days in a row"
- Resets when you miss any dose
- Great motivator for consistency

### Missed Doses
- Total missed doses this week
- Simple count (e.g., "6 missed")
- Helps you see patterns
- Non-judgmental tracking

## Daily Adherence Chart

### Weekly Bar Chart
- **7 bars**: One for each day (Mon-Sun)
- **Height**: Represents adherence percentage (0-100%)
- **Color coding**:
  - Primary blue: ≥90% (excellent)
  - Tertiary purple: 70-89% (good)
  - Gray: <70% (room for improvement)

### Reading the Chart
- Taller bars = better adherence that day
- See patterns (e.g., weekends vs weekdays)
- Identify days when you tend to miss doses
- Each bar shows percentage label on top

### Example
```
Mon: 100% (blue, tall)
Tue: 80% (purple, medium)
Wed: 100% (blue, tall)
Thu: 60% (gray, short)
Fri: 100% (blue, tall)
Sat: 100% (blue, tall)
Sun: 80% (purple, medium)
```

## Encouraging Feedback

### Personalized Messages
Based on your weekly percentage:

- **90%+**: "Excellent work! You're taking great care of your health. Keep it up!"
- **80-89%**: "Great job! You're doing really well with your medications."
- **70-79%**: "Good work! You're staying on track most of the time."
- **60-69%**: "You're doing okay. Try to take your medications every day this week."
- **<60%**: "Let's work together to improve. Every dose you take helps your health!"

### Emoji Indicators
- 🎉 (90%+): Celebration
- 😊 (80-89%): Happy
- 👍 (60-79%): Thumbs up
- 💪 (<60%): Strength/encouragement

## Tips for Improvement

### Building Streaks
- Set phone alarms for dose times
- Link doses to daily routines (breakfast, dinner)
- Use the Snooze button when busy
- Check Home screen in the morning to see the day's schedule

### Understanding Patterns
- Notice which days you miss most
- Identify problematic times (morning vs evening)
- Adjust schedules if certain times don't work
- Talk to your doctor about difficult schedules

### Weekly Goals
- Aim for 80%+ adherence
- Focus on improving by 5-10% each week
- Don't beat yourself up over missed doses
- Celebrate streaks, no matter how small

## What Counts

### Taken Doses
- Marked with "Taken" button
- Counts toward adherence percentage
- Builds your streak

### Missed Doses
- Marked with "Missed" button
- Does NOT count toward adherence
- Breaks your streak

### Snoozed Doses
- Treated like unmarked doses initially
- Must eventually mark as Taken or Missed
- Doesn't affect stats until marked

## Privacy Note
Statistics are calculated from your medication data stored securely in Firebase Firestore. If you've paired with a caregiver via PIN/QR code, they can view your statistics to help monitor your adherence. Otherwise, your data remains private and is not shared with anyone.
