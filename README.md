# Gwani Keyboard — Phase 1

A gesture-based keyboard for Hausa language input.
Phase 1: Basic keyboard that types characters.

---

## Files In This Project

```
gwani-keyboard/
├── app/src/main/
│   ├── AndroidManifest.xml     ← Registers keyboard with Android
│   ├── kotlin/com/gwani/keyboard/
│   │   ├── GwaniIME.kt         ← Entry point, Android calls this
│   │   ├── KeyData.kt          ← All key definitions and layout
│   │   └── KeyboardView.kt     ← Draws keys, handles touches
│   └── res/
│       ├── xml/method.xml      ← IME metadata Android requires
│       └── values/
│           ├── strings.xml     ← App name text
│           └── colors.xml      ← All color values
```

---

## How To Build (On Your PC)

### Step 1 — Install JDK 17
Download from: https://adoptium.net/
Choose: Windows x64 installer

### Step 2 — Install Android Studio
Download from: https://developer.android.com/studio
During install, let it install the Android SDK automatically.

### Step 3 — Open Project
- Open Android Studio
- Click "Open"
- Select the gwani-keyboard folder

### Step 4 — Connect Phone
- Enable Developer Options on your phone:
  Settings → About Phone → tap "Build Number" 7 times
- Enable USB Debugging:
  Settings → Developer Options → USB Debugging → ON
- Connect phone to PC with USB cable
- Your phone should appear in Android Studio top bar

### Step 5 — Install On Phone
- Click the green Play button in Android Studio
- The APK builds and installs on your phone automatically

### Step 6 — Enable The Keyboard
- Settings → General Management → Keyboard → Manage Keyboards
- Turn ON "Gwani"
- Set as Default if you want

---

## What Phase 1 Does
- Shows a full QWERTY keyboard
- Types all letters
- Backspace works
- Enter works
- Space works
- Symbol row visible at all times

## What Is Not In Phase 1 (Coming Next)
- Shift / caps lock
- Layer switching
- Gesture flicks
- Modified characters (ƙ, ɗ, ɓ)
- Animations
- Haptics
- Sounds
- Themes
