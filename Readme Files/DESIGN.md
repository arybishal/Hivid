# DESIGN.md: HDR Studio (working name) Android App

Dark, high-contrast utility UI with one yellow accent. The reference screenshots use a near-black background, dark rounded panels and a single yellow for every action. This file reproduces that *look and logic* with your own original name, logo and wording.

> Do not copy the reference app's name, logo, icon or text. Use your own brand mark. Rename "Dicky" and "Golden" everywhere.

## 1. Design principles (from the screenshots)

1. **Yellow = action.** Buttons, selected chips, slider thumb, progress fill, active tab.
2. **Dark panel = information.** Cards that explain or report.
3. **Slider = adjust.** Only the HDR expansion slider.
4. One legend line under the header tells the user this (keep it, it is good UX):
   `YELLOW = ACTION · DARK PANEL = INFORMATION · SLIDER = ADJUST`
5. Technical detail is shown, but secondary (small, muted, monospace for values).

## 2. Color tokens

| Token | Hex | Use |
|---|---|---|
| `bg` | `#0B0B0D` | App background |
| `surface` | `#17171A` | Cards / panels |
| `surfaceHigh` | `#202024` | Raised cards, dialogs |
| `border` | `#2C2C32` | 1dp card outline |
| `accent` | `#F2DC3C` | Primary actions, active states |
| `accentPressed` | `#D9C42E` | Pressed state |
| `accentDim` | `#8A7F22` | Disabled/progress track fill (dim yellow) |
| `onAccent` | `#1A1A1A` | Text on yellow |
| `text` | `#F2F2F2` | Primary text |
| `textMuted` | `#A3A3AB` | Descriptions |
| `textFaint` | `#6E6E76` | Hints, track |
| `success` | `#7BD88F` | READY, verified |
| `warning` | `#FFB74D` | Limited support |
| `error` | `#FF6B6B` | Failures |
| `scrim` | `#000000` at 60% | Dialog backdrop |

Subtle background texture in screenshot 1 (faint star/line particles): optional, 4% opacity max. Skip in v1.

## 3. Typography

| Role | Font | Size / weight |
|---|---|---|
| App title | Inter (or Roboto Flex) | 22sp / 700 |
| Section title (card) | Inter | 18sp / 600 |
| Section label (above card, caps) | Inter | 12sp / 600, letter-spacing 1.2sp, accent color |
| Body / description | Inter | 13sp / 400, `textMuted` |
| Button label | Inter | 15sp / 600, uppercase optional |
| Technical values | JetBrains Mono | 12sp / 400 |
| Big heading on marketing art | Inter Black | 28sp / 900 |

Fallback chain: Inter, Roboto, system sans-serif. Bundle Inter and JetBrains Mono as variable fonts in `res/font`.

## 4. Shape, spacing, elevation

- Screen padding: 16dp. Card gap: 12dp. Card inner padding: 16dp.
- Card: radius 20dp, fill `surface`, border 1dp `border`, no shadow.
- Primary button: height 52dp, radius 26dp (pill), fill `accent`, text `onAccent`.
- Secondary button: height 52dp, radius 26dp, transparent, 1dp `border`, text `text`.
- Toggle chip: height 48dp, radius 24dp. ON = fill `accent` + text `onAccent` with the word "ON". OFF = outline only.
- Segment chips (HDR10 / HDR10+ MAX): radio dot + "H" glyph + label, radius 12dp.
- Progress bar: height 4dp, track `textFaint`, fill `accent`.
- Slider: track 3dp, thumb 18dp circle `accent`, active track `accent`.
- Dialog: radius 16dp, `surfaceHigh`, 24dp padding, "OK" text button in `accent`.
- Bottom nav: floating pill container, radius 32dp, 4 items, active item = filled yellow pill.
- Spacing scale: 4, 8, 12, 16, 24, 32.

## 5. Screens

### 5.1 Header (all tabs)
- Left: 48dp rounded-square app icon. Right: small badge chip (e.g. "MAX").
- Title (22sp) + subtitle line (`STUDIO MAX · SDR → HDR`, 12sp muted).
- Legend line (11sp, `textFaint`).

### 5.2 Convert tab (HDR engine, screenshot 2)
Cards in order:
1. **HDR MASTERING ENGINE** (info): description + 4 spec pills in a row: `10-BIT`, `BT.2020`, `PQ`, `GPU` (small yellow pills, 10sp).
2. **SOURCE VIDEO**: description, full-width yellow button "PICK VIDEO" with camera icon, line below: `VIDEO · filename.mp4`.
3. **OUTPUT PROFILE**: two radio chips `HDR10` and `HDR10+ MAX`, then mono spec lines: resolution/fps/rotation, coded vs visible size, source bitrate, `HDR10: READY · HDR10+: READY`, encoder name, max request Mbps.
4. **FRAME RATE ENGINE**: description + radio chip `60 FPS`.
5. **HDR EXPANSION**: description + slider + label `70% · Aggressive`.
6. **PROCESSING STATUS**: description, `PROCESSING · 33%`, progress bar, output hint line.
7. Full-width button **CONVERT TO HDR** (play icon). While running: label becomes "CANCEL".
8. **PIPELINE DEFINITIONS** (info): glossary rows (term in 15sp/600, explanation 12sp muted): HEVC Main10, BT.2020, PQ/ST2084, HDR10, HDR10+, Gallery.

### 5.3 Completion dialog (screenshot 3)
Title "HDR COMPLETE". Mono list: Output, Profile, Color, Encoder, Source coded/visible, Rotation, Output pixels, Source FPS, Output FPS target, Source bitrate, Decoder ceiling, Requested bitrate, Actual bitrate, Encoded samples, Measured cadence, Size, Audio, Compatibility note, Saved name, Location. Button OK.

### 5.4 Remux / Metadata tab (screenshot 1)
Section label `MAIN ENGINE`. Card "Container / metadata processing": 4 toggle chips (`SAFE H.264`, `ORIGINAL BITRATE`, `SAFE HDR/SDR`, `STREAMING`), outline button "PICK MP4 VIDEO", filename line. Section label `PROCESS`: card with status text ("Video ready to process", "Stage: waiting for source") and button "PROCESS". Section label `VERIFICATION`: card with check icon, "Verification system ready", button "OPEN TECHNICAL ACCESS".

### 5.5 Guide / History / Settings
- Guide: glossary cards plus FAQ in the same card style.
- History: list of cards (thumbnail, name, profile, size, date) with Open, Share, Delete.
- Settings: toggles and pickers (language, default profile, default expansion, output folder name, keep screen on, notifications).

## 6. Motion and feedback
- Press: scale 0.98, color to `accentPressed`, 100ms.
- Progress: linear, no bounce. Percent updates at most 4 times per second.
- Dialog: fade + 8dp rise, 180ms.
- Haptic tick on convert start and finish.

## 7. Accessibility
- All text/background pairs above meet WCAG AA (yellow on near-black is about 14:1; muted text on surface about 6:1).
- Minimum touch target 48dp. Never rely on color alone: ON/OFF is also written.
- Support font scale up to 130% without clipping.

## 8. Compose theme starter

```kotlin
object HdrColors {
    val Bg = Color(0xFF0B0B0D); val Surface = Color(0xFF17171A)
    val SurfaceHigh = Color(0xFF202024); val Border = Color(0xFF2C2C32)
    val Accent = Color(0xFFF2DC3C); val OnAccent = Color(0xFF1A1A1A)
    val Text = Color(0xFFF2F2F2); val Muted = Color(0xFFA3A3AB); val Faint = Color(0xFF6E6E76)
    val Success = Color(0xFF7BD88F); val Warning = Color(0xFFFFB74D); val Error = Color(0xFFFF6B6B)
}
```

## 9. Brand (original, not the reference)
- Name ideas: HDR Studio, LumaBoost, NitGlow, PeakFrame. Check store availability first.
- Icon: rounded square, `bg` fill, a yellow sun/arc or "HDR" monogram. Make it yourself.
