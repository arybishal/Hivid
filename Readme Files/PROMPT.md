# PROMPT.md: Step-by-step build prompts (Android, 100% on-device SDR to HDR)

How to use: open a new project in Claude Code, Cursor or Android Studio with an AI assistant. Attach `DESIGN.md` and the 3 screenshots. Paste the **Master context** once, then run the phases **in order**. Do not move on until the current phase builds and passes its checks. Test on a real phone (HDR conversion does not work on most emulators).

---

## Master context (paste first)

```
You are a senior Android engineer. Build "HDR Studio", an Android app that converts SDR video to HDR10 (optionally flagged HDR10+) fully on-device. No server, no upload, no INTERNET permission.

Stack: Kotlin, Jetpack Compose, Material 3, MediaExtractor/MediaCodec/MediaMuxer, OpenGL ES 3.x with EGL, Room, DataStore, Foreground Service. minSdk 29, targetSdk latest, full features on API 33+.
Architecture: single module, MVVM, packages: ui, domain, media, data, util.
Follow DESIGN.md exactly for colors, fonts, shapes, spacing and screen layout.
Original branding only. Never write the words "Dicky" or "Golden".
Rules: never overwrite the source file; always write a new timestamped file via MediaStore into Movies/HDR Studio; label output honestly (HDR10, and HDR10+ only if truly supported and written).
After each phase, list files changed and how to test it.
```

---

## Phase 0: Project setup

```
Create the Android project (Kotlin, Compose, Material 3, minSdk 29). Add dependencies: Compose BOM, navigation-compose, lifecycle-viewmodel-compose, Room, DataStore, WorkManager (optional), coil (thumbnails), and bundle Inter and JetBrains Mono fonts in res/font. Implement the theme from DESIGN.md (colors, typography, shapes) in ui/theme. Force dark theme. Add a bottom navigation pill with 4 tabs: Convert, Remux, Guide, History, plus Settings reachable from the header. Make each tab a placeholder screen. Do not add the INTERNET permission.
```
Check: app launches, dark theme matches DESIGN.md, tabs switch.

## Phase 1: Reusable UI components

```
Build reusable Compose components exactly per DESIGN.md: AppHeader (icon, title, subtitle, badge, legend line), InfoCard (title, description, content slot), SectionLabel, PrimaryButton (yellow pill), OutlineButton, ToggleChip (ON/OFF with text), RadioChip (with "H" glyph), SpecPill, MonoLine (key/value mono text), ProgressBar, ExpansionSlider (shows "70% · Aggressive"), ResultDialog, GlossaryRow. Add a preview screen showing every component.
```
Check: previews look like the screenshots in style.

## Phase 2: Video picker and source analysis (domain + media)

```
Implement source selection with ActivityResultContracts.PickVisualMedia (video only, no storage permission). Create SourceInfo analyzer using MediaExtractor + MediaMetadataRetriever to read: file name, duration, size, video codec/mime, coded width/height, visible width/height, rotation, frame rate (from KEY_FRAME_RATE and by counting sample timestamps), video bitrate, audio mime/channels/sample rate, color standard/transfer/range (KEY_COLOR_STANDARD, KEY_COLOR_TRANSFER, KEY_COLOR_RANGE), and whether the source is already HDR (transfer ST2084 or HLG). Show the result in the SOURCE VIDEO card ("VIDEO · name.mp4") and feed the OUTPUT PROFILE mono lines. If the source is already HDR, show a warning card.
```
Check: pick several videos (portrait 4K60, landscape 1080p30, rotated) and confirm values are correct.

## Phase 3: Device capability probe

```
Create CapabilityProbe using MediaCodecList(ALL_CODECS). Find hardware HEVC encoders and report: encoder name, whether HEVCProfileMain10 is supported (profileLevels), whether COLOR_FormatSurface input works, max width/height/frame rate from VideoCapabilities, max bitrate (BitrateRange upper), and whether HDR10+ is supported (CodecCapabilities.profileLevels contains HEVCProfileMain10HDR10Plus). Expose HDR10: READY/NOT SUPPORTED and HDR10+: READY/NOT SUPPORTED, encoder name, "Max request: N Mbps". Show these in the OUTPUT PROFILE card. Disable the HDR10+ MAX chip when not supported and explain why in a short muted line.
```
Check: values match what the phone really supports; test on 2+ devices.

## Phase 4: Settings model and Convert screen UI

```
Build the Convert tab exactly per DESIGN.md section 5.2 with ViewModel state: source, profile (HDR10 / HDR10_PLUS), fps mode (source cadence, cap 60), expansion 0..100 with label (0-30 Natural, 31-65 Balanced, 66-100 Aggressive), status (Idle, Preparing, Processing(percent), Done, Error, Cancelled). Include the PIPELINE DEFINITIONS glossary card. The CONVERT TO HDR button is disabled until a source is chosen and the device is READY. Persist last-used profile and expansion in DataStore.
```
Check: UI matches screenshot 2; state survives rotation.

## Phase 5: GPU pipeline spike (most important step)

```
Implement the core conversion engine in media/ as a class HdrConverter, in this order:
1. Decoder: MediaExtractor selects the video track; MediaCodec decoder outputs to a SurfaceTexture (OES texture) created on a GL thread.
2. EGL: create an EGL14 context (GLES 3.0) and an encoder window surface using a 10-bit config (EGL_RGBA 1010102) with EGL_EXT_gl_colorspace_bt2020_pq when available; otherwise fall back to writing PQ-encoded values into a 10-bit surface and tag colors in MediaFormat.
3. Fragment shader (GLSL ES 3.0): sample the OES texture, convert sRGB/Rec.709 transfer to linear, apply an inverse tone mapping curve with a parameter "expansion" (0..1) that raises highlights above ~0.6 luminance toward a peak of ~1000 nits (soft shoulder, no clipping of shadows), convert Rec.709 primaries to BT.2020 with the standard 3x3 matrix, then apply the PQ (ST 2084) OETF, output 10-bit.
4. Encoder: MediaCodec HEVC, profile HEVCProfileMain10 (or Main10HDR10Plus when chosen and supported), input from createInputSurface(), MediaFormat keys: KEY_COLOR_STANDARD = COLOR_STANDARD_BT2020, KEY_COLOR_TRANSFER = COLOR_TRANSFER_ST2084, KEY_COLOR_RANGE = LIMITED, bitrate from AutoBitrate (see below), frame rate from source, I-frame interval 1s.
5. Frame timing: set presentation time with EGLExt.eglPresentationTimeANDROID from the source PTS so cadence is preserved.
6. Mux: MediaMuxer (MP4), add the encoded video track; copy the source AAC audio track sample-by-sample unchanged (passthrough). Preserve rotation by applying it in the shader or via setOrientationHint; test both.
Write it as a suspend function with progress callbacks (percent from PTS/duration), cancellation support, and cleanup of every codec and EGL resource in finally blocks.
```
Check (critical): convert a 10-second 1080p clip; open the output in the Gallery. The HDR badge/brightness boost should appear. Confirm with `ffprobe` or MediaInfo: HEVC Main 10, BT.2020, ST2084. Do not continue until this works.

## Phase 6: Auto bitrate and resolution safety

```
Implement AutoBitrate: base = max(source video bitrate * 1.6, resolution/fps lookup table) clamped to the encoder's max bitrate and to a safe ceiling (e.g. 80 Mbps for 4K60). Table (Mbps, HEVC 10-bit): 1080p30=12, 1080p60=18, 1440p60=30, 2160p30=35, 2160p60=50. Add a manual bitrate override in Settings. Add a fallback: if the encoder fails to configure at the source resolution/fps, retry at 60fps cap, then at 1080p, and tell the user what was changed in the completion report.
```
Check: force a failure (huge resolution) and see the fallback and message.

## Phase 7: Foreground service, progress, cancel

```
Run conversion inside a foreground service (type mediaProcessing / dataSync as allowed for target SDK) with an ongoing notification showing percent and a Cancel action. Request POST_NOTIFICATIONS on Android 13+. Add a wake lock while converting. Expose progress to the ViewModel through a StateFlow. Show "PROCESSING · 33%" and the progress bar in the PROCESSING STATUS card. Handle: app backgrounded, screen off, low storage (check free space >= 1.5x estimated output before starting), thermal status (PowerManager thermal listener; warn on SEVERE).
```
Check: convert a 2-minute video, switch apps, return; cancel mid-way; no leftover temp files.

## Phase 8: Save to gallery and completion report

```
Write output to MediaStore.Video (RELATIVE_PATH = Movies/HDR Studio, IS_PENDING during write, DISPLAY_NAME = "HDRStudio_HDR10_yyyyMMdd_HHmmss_SSS.mp4"). After success, build a ConversionReport data class with: output name, profile, color (BT.2020 / PQ ST2084), encoder, source coded/visible size, rotation metadata, output pixels, source fps, output fps target, source bitrate, decoder ceiling, requested bitrate, actual encoded bitrate (file size / duration), encoded sample count, measured cadence, size in bytes, audio policy, compatibility note ("HDR10+ dynamic metadata depends on device encoder support"), saved name and location. Show it in the ResultDialog (mono list, OK button) and store it in Room for History.
```
Check: dialog content matches screenshot 3 in structure; file appears in the Gallery.

## Phase 9: HDR10+ option (honest mode)

```
Add the HDR10+ MAX path. If the encoder supports HEVCProfileMain10HDR10Plus, configure the encoder with that profile and, where the device exposes it, supply dynamic metadata via MediaCodec.PARAMETER_KEY_HDR10_PLUS_INFO per frame (generate simple per-frame metadata from the shader's measured max luminance if feasible; otherwise leave metadata to the encoder). After encoding, inspect the output (MediaExtractor format KEY_PROFILE) and only label the file "HDR10+" in the report if the profile is really Main10HDR10Plus; otherwise label it "HDR10" and explain. Never claim HDR10+ without verification.
```
Check: on a device without support, the chip is disabled; on a supporting device, report says HDR10+ only when verified.

## Phase 10: Remux / Metadata tab (screenshot 1 features)

```
Build the Remux tab per DESIGN.md 5.4 as a LOSSLESS container tool. Toggles and their real behaviour:
- SAFE H.264: if the input is H.264, copy the stream without re-encoding (never transcode); if not H.264, show that it will be copied as-is.
- ORIGINAL BITRATE: copy samples unchanged so bitrate stays identical.
- SAFE HDR/SDR: preserve the colour tags (standard, transfer, range) and any HDR metadata exactly; never change them.
- STREAMING: write the MP4 with the moov atom at the start (fast start) so it plays while loading. Implement by remuxing with MediaMuxer then moving moov to the front (use a small pure-Kotlin MP4 box reader/writer, or run a two-pass: write with MediaMuxer, then rewrite boxes).
Flow: PICK MP4 VIDEO -> PROCESS (copies video and audio samples with original timestamps into a new file in Movies/HDR Studio) -> VERIFICATION.
Verification: compute SHA-256 over the concatenated sample payload of input and output (video and audio separately) and compare; also compare sample count, duration, colour tags. Show a green "Verification passed" card; the OPEN TECHNICAL ACCESS button opens a screen listing parser details (MP4 box structure, offsets, hashes). Be truthful in copy: this tool repackages the file losslessly; it does not guarantee what any social platform does after upload.
```
Check: output video and audio hashes equal the input's; file plays; moov is at the front (verify with a box dump).

## Phase 11: History, Guide, Settings

```
History: Room table of conversions (id, type, input name, output uri, report JSON, createdAt). List cards with thumbnail (Coil video frame), name, profile, size, date; actions Open (ACTION_VIEW), Share (ACTION_SEND with FileProvider/MediaStore uri), Delete (remove file and row, with confirm).
Guide: static glossary and FAQ cards (what is HDR10, PQ, BT.2020, why result differs per platform, battery/heat tips, SDR-expanded is not true HDR).
Settings: language, default profile, default expansion, fps cap (30/60), manual bitrate override, output folder name, keep screen on while converting, notifications. Persist in DataStore.
```

## Phase 12: Localization

```
Move all strings to strings.xml and add translations: English (default), Nepali, Hindi, Arabic (RTL support), Bengali, Indonesian. Make layouts RTL-safe. Add an in-app language picker using AppCompatDelegate.setApplicationLocales.
```

## Phase 13: Hardening and QA

```
Add: crash-safe temp files in cacheDir deleted on start; try/catch around every codec step with human-readable errors mapped to a sealed class (UnsupportedDevice, LowStorage, DecoderFailed, EncoderFailed, MuxerFailed, Cancelled); logging screen (copy report); unit tests for AutoBitrate, expansion labels, filename generator; instrumented tests that convert a small bundled sample clip and assert Main10/BT.2020/PQ in the output. Run Lint, enable R8 shrinking, check APK size.
```
Test matrix (minimum): Qualcomm, MediaTek, Exynos/Tensor devices; Android 13, 14, 15; clips: 1080p30, 1080p60, 4K30, 4K60, portrait rotated, VFR, no-audio, 10-minute.
Playback checks: system Gallery, YouTube upload, Instagram, TikTok, VLC, an HDR TV.

## Phase 14: Release

```
Prepare release: app icon (original), adaptive icon, splash, privacy policy page stating "all processing happens on your device; no files are uploaded", Play Store listing text that says "simulated HDR from SDR" and lists device requirements, screenshots, signed AAB, versioning. Data safety form: no data collected.
```

---

## Troubleshooting prompts

**Washed out or too dark in some players**
```
The output looks washed out in <player>. Review colour tagging: verify MediaFormat KEY_COLOR_STANDARD/TRANSFER/RANGE are set on the encoder and that the shader output really is PQ-encoded BT.2020. Add a debug screen that reads the output's colour keys and prints them. Suggest shader fixes (limited vs full range, peak nits constant).
```
**Green or pink frames / wrong colours**
```
Frames show wrong colours on <chipset>. Check the EGL surface format (1010102), YUV to RGB conversion of the OES texture, and range handling. Propose a fallback path that renders to an FBO (RGBA16F) and then to the encoder surface.
```
**Rotated or stretched video**
```
Output is rotated/stretched for portrait sources. Read rotation from MediaFormat KEY_ROTATION and the video track metadata; apply it in the vertex matrix and set muxer orientation correctly without double-rotating. Add tests for 0/90/180/270.
```
**Overheating or crash on 4K60**
```
Conversion crashes or throttles on 4K60. Add thermal monitoring, lower the encoder complexity/bitrate, offer an automatic 1080p fallback, process with a smaller GL texture, and release buffers promptly.
```

---

## Reality checks to keep in mind

- SDR to HDR is *simulated* (inverse tone mapping). It makes highlights brighter; it cannot add real captured detail.
- HDR10+ dynamic metadata depends on the phone's encoder. Many devices will only produce HDR10.
- Instagram, TikTok and WhatsApp may re-encode or tone-map uploads, so results can differ from your gallery preview.
- The Phase 5 spike decides whether the whole project is viable on your target phones. Finish and test it before polishing UI.
