# SDR to HDR Android App: Market Research and PRD

Prepared for Bishal · 3 November 2026 · Version 0.1 (draft)

## 1. Summary

The goal is an Android app that converts ordinary SDR video into an HDR10 file (optionally HDR10+ flagged) entirely on the phone, with no upload and no server. It is modelled on the "Dicky HDR10+ Studio Max" tool shown in your three screenshots.

Three findings shape everything below:

- **Demand exists, but it is creator-driven and hype-driven.** The reference app is spread through TikTok tutorials ("4K 60FPS HDR Engine"), and the screenshot shows it getting roughly 1.3K likes and 900+ saves. People want videos that look brighter and more vivid on HDR phone screens.
- **Android is under-served.** The offline SDR to HDR tools I found are mostly iOS or Mac (HDR Easy, HDR Converter, WowHDR). On Android, the visible converter is Glow HDR, which handles photos (Ultra HDR), not video. The reference app itself appears to be shared as a tutorial plus APK, not as a polished Play Store product.
- **The technical truth matters for your product and your reputation.** SDR to HDR is *inverse tone mapping*. It expands highlights and re-encodes into an HDR container. It cannot recover detail that was never captured. A trustworthy app should say this clearly and focus on a good-looking result, not on a false "real HDR" claim.

## 2. What the reference app does (from your screenshots)

From the three screenshots, the reference app is an on-device pipeline with these parts:

| Area | Observed behaviour |
| --- | --- |
| Input | "Pilih video" picker; source example 2160x3840, 60 fps, rotation 0° |
| Output profiles | HDR10 (static metadata) and "HDR10+ MAX" (dynamic metadata, only if the device encoder supports it) |
| Codec and color | HEVC Main10, BT.2020 colour, PQ / ST2084 transfer, 10-bit |
| Encoder | Hardware encoder (c2.qti.hevc.encoder on a Qualcomm phone); max request 160 Mbps shown |
| Frame rate | "Stable" mode keeps source cadence, up to 60 fps; a force-120 path is disabled in that build |
| HDR expansion | Slider, 0 to 100%, with labels from Natural to Aggressive (example: 70%) |
| Bitrate | Auto. Example: source 4 Mbps, requested 8 Mbps, actual encoded about 6.5 Mbps |
| Audio | AAC passthrough (copied, not re-encoded) |
| Progress | Percentage progress bar, then a completion report dialog |
| Report | Output name, profile, colour, encoder, resolution, FPS, bitrate, sample count, file size, compatibility note |
| Output | New timestamped file in Movies/\<AppName> folder; the original is never overwritten |
| Other screens | Tabs: Engine, Guide, History, Settings; a "technical access" panel that hides parser details |

Note the honest caveat inside the reference app itself: HDR10+ dynamic metadata depends on native encoder support on the device. Your app should be at least this honest.

## 3. Market research

### 3.1 Why people want this

- Phones with bright HDR (OLED) screens make HDR clips look noticeably punchier in the gallery, and creators want that "glow" on their own old or SDR footage.
- Instagram has supported HDR video since 2022 but its pipeline stripped the metadata. In late 2025 Meta announced fuller HDR support on iOS (Dolby Vision and ambient viewing metadata). That is a signal that platforms are investing in HDR, which raises creator interest.
- Reality check: guides aimed at creators still say SDR Rec.709 is the safest baseline for social platforms, and HDR is a separate path with more ways to fail (washed-out or too-dark results when something is mismatched). So the use case is real but narrow, and results vary by platform and viewing device.

### 3.2 Competitor landscape

| Product | Platform | What it does | Gap for you |
| --- | --- | --- | --- |
| HDR Easy | iOS (free, in-app purchases) | One-tap SDR to 10-bit HDR for photos and video, peak brightness control, Shortcuts, batch in Pro | Not on Android |
| HDR Converter (Prismatik derivative) | iOS | Offline SDR to HDR and conversion between HDR formats, 100% on-device privacy | Not on Android |
| WowHDR | Mac ($14.99) | SDR to HLG HDR, keeps audio and subtitles | Desktop only |
| Glow HDR: Ultra HDR converter | Android | SDR photos to Ultra HDR with gain map (about 3.6K installs, few ratings) | Photos only, small base |
| Dicky HDR10+ Studio Max | Android (APK, tutorial-distributed) | SDR video to HDR10 / HDR10+ with 60 fps and AAC passthrough | Not a mainstream store product; Indonesian-language UI; possible stability and trust issues |
| General editors (CapCut, InShot, VN) | Android and iOS | Editing, mostly HDR to SDR handling | Not focused on SDR to HDR |
| Desktop and web converters (UniConverter, UniFab, Convertio) | PC and web | HDR to SDR and some SDR to HDR | Upload or desktop, not mobile-first |

**Takeaway:** the whitespace is a clean, trustworthy, English-first (plus other languages) Android app that does SDR to HDR **video** offline, with clear device-support checks and a built-in before/after preview.

### 3.3 Target users

1. **Short-form creators** (TikTok, Reels, Shorts) who want brighter, richer clips.
2. **Phone photographers and videographers** with SDR footage from older phones or edited exports.
3. **Social media managers and small brands** (your own world: fashion and e-commerce content) who want more eye-catching product videos.
4. **Enthusiasts** who like to test HDR on their OLED phones.

### 3.4 Market size

I did not find reliable public numbers for "SDR to HDR converter" users, and I will not invent any. Treat the market as a niche utility within the video-editing category. Use these as validation steps before heavy investment:

- Check Play Store search results and install counts for "HDR converter", "SDR to HDR", "HDR video".
- Check TikTok and YouTube view counts for "SDR to HDR Android" tutorials.
- Run a landing page or a small closed beta and measure sign-ups.

### 3.5 Risks in the market

- **"Fake HDR" criticism.** Expanded SDR is not true HDR. Overclaiming invites bad reviews.
- **Platform behaviour.** TikTok, Instagram and WhatsApp may re-encode or tone-map the file, so the result can differ from the gallery preview.
- **Device fragmentation.** HDR10 encode support, 10-bit input and HDR10+ metadata vary a lot between chipsets.
- **Clone risk.** Copying the reference app's name, logo, layout or text would create trademark and copyright problems. Build original branding and design.
- **Play Store policy.** Claims must match what the app actually does (for example, do not advertise HDR10+ if you only flag HDR10).

## 4. Product vision

> Make any phone video look brighter and richer on HDR screens, privately, in one tap, without uploading anything.

### Principles

- **100% on-device.** No network permission is needed for processing. The app can ship with no INTERNET permission at all, which is a strong privacy message.
- **Honest.** Label output exactly as it is (HDR10, or HDR10+ only when truly written), show a device capability check, and explain that HDR is simulated from SDR.
- **Safe by default.** Never overwrite the source file. Always write a new file.
- **Simple first, pro later.** One-tap result for beginners, advanced controls for creators.

## 5. Goals and non-goals

**Goals (v1)**

- Convert an SDR video to HEVC Main10, BT.2020, PQ (HDR10) on-device.
- Keep source resolution, frame rate and audio.
- Give a quality slider for how strong the highlight expansion is.
- Show a clear completion report and save to the gallery.

**Non-goals (v1)**

- No cloud processing or accounts.
- No full video editing (trim, filters, text).
- No HDR to SDR conversion (maybe later).
- No promise of real HDR10+ dynamic metadata on all devices.
- No AI upscaling.

## 6. Functional requirements

### 6.1 Must have (P0)

| ID | Requirement |
| --- | --- |
| F1 | Pick a video with the system photo/video picker (no broad storage permission) |
| F2 | Read and display source info: resolution, fps, rotation, codec, bitrate, duration, size, colour info |
| F3 | Detect if the source is already HDR and warn, or offer to skip |
| F4 | Device capability check: HEVC Main10 hardware encoder present, 10-bit support, maximum resolution, maximum fps, shown as READY or NOT SUPPORTED |
| F5 | Output profile choice: HDR10 (default). HDR10+ option shown only if the encoder reports support |
| F6 | HDR expansion slider, 0 to 100%, with Natural, Balanced and Aggressive presets |
| F7 | Frame rate: keep source cadence by default; cap at 60 fps |
| F8 | Resolution: keep source by default; option to downscale if the encoder cannot handle it |
| F9 | Bitrate: Auto (based on source and resolution) plus manual override |
| F10 | Audio passthrough (copy the AAC track unchanged); keep all audio tracks where possible |
| F11 | Convert in a foreground service with a notification, progress percentage, and a cancel button |
| F12 | Save the result as a new file in Movies/\<AppName> with a timestamp name via MediaStore |
| F13 | Completion report: output file, profile, colour, encoder, resolution, fps, bitrate, size, time taken |
| F14 | Preserve rotation and orientation correctly (portrait and landscape) |
| F15 | Graceful errors: clear messages for unsupported device, low storage, codec failure, cancelled job |

### 6.2 Should have (P1)

| ID | Requirement |
| --- | --- |
| F16 | Before/after preview with a split slider (note: only meaningful on an HDR-capable screen) |
| F17 | Short preview render (first 5 seconds) to test settings quickly |
| F18 | History tab with past conversions, share, open, delete |
| F19 | Batch queue for multiple videos |
| F20 | Presets (for example Social, Cinematic, Product) |
| F21 | Share directly to TikTok, Instagram, WhatsApp from the result screen |
| F22 | Guide tab explaining HDR10, PQ, BT.2020 and limits in plain language |
| F23 | Languages: English first, then Nepali, Hindi, Arabic, Bengali, Indonesian |

### 6.3 Could have (P2)

- HLG output option (better compatibility on some devices).
- Ultra HDR photo conversion (gain map) as a second tool.
- HDR10+ dynamic metadata generation where the encoder allows it.
- Auto-scene analysis to adapt expansion per scene.
- Tablet and foldable layouts.

## 7. Technical approach

### 7.1 Pipeline

1. **Decode** the SDR source with MediaCodec (hardware).
2. **Process** each frame on the GPU with an OpenGL ES shader: convert to linear light, apply the highlight-expansion curve (your slider), convert Rec.709 primaries to BT.2020, apply the PQ (ST 2084) transfer function, output in 10-bit.
3. **Encode** with a MediaCodec HEVC Main10 hardware encoder fed through an input Surface, with colour standard BT.2020, transfer ST2084 and range set in the output MediaFormat.
4. **Mux** video and the copied audio track into MP4 with MediaMuxer, preserving presentation timestamps so frame cadence is kept.
5. **Save** through MediaStore into Movies.

### 7.2 Key platform facts to design around

- Android describes HDR formats by 10-bit buffers, transfer function and metadata type: HDR10 uses PQ with static metadata on HEVC 10-bit. OEMs decide which formats they enable, so support is device-specific.
- Media3 Transformer is the official high-level editing library. It can tone-map HDR to SDR (OpenGL from API 29, MediaCodec on API 31+ on some devices) and edit HDR from Android 13. However, its documentation lists **SDR to HDR tone mapping as unsupported for multi-asset compositions**. So do not depend on Transformer for the core SDR to HDR step; plan a custom GL pipeline (you can still reuse Transformer pieces for muxing and demuxing if useful).
- HDR10+ dynamic metadata needs encoder support. Treat it as an optional feature gated by a runtime check, as the reference app does.
- Android 13 (API 33) is a sensible minimum for HDR editing support; consider API 29 as a hard floor with limited features.

### 7.3 Suggested stack

| Layer | Choice |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose, Material 3, dark theme |
| Media | MediaExtractor, MediaCodec, MediaMuxer, OpenGL ES 3.x (EGL with a 10-bit surface) |
| Background work | Foreground service plus WorkManager |
| Storage | MediaStore, Room for history |
| Min SDK | 29 (full features on 33+) |
| Network | None for conversion (no INTERNET permission) |

### 7.4 Major technical risks

| Risk | Mitigation |
| --- | --- |
| Encoders behave differently per chipset (Qualcomm, MediaTek, Exynos, Tensor) | Capability probe before conversion; test matrix on at least 8 to 10 devices |
| 4K 60 fps overheating or running out of memory | Thermal checks, lower-resolution fallback, chunked processing, clear warnings |
| Washed-out or too-dark result in some players | Correct colour tags, test in Gallery, YouTube, Instagram, TikTok, VLC |
| Wrong rotation or aspect ratio | Read rotation metadata, apply it explicitly, test portrait, landscape and odd sizes |
| Audio and video drift | Preserve source PTS, validate sample counts |
| HDR10+ claimed but not actually written | Only show the label when verified; otherwise label as HDR10 |

## 8. UX outline

Bottom navigation: **Convert · Guide · History · Settings** (similar structure to the reference, with your own design).

**Convert screen (top to bottom)**

1. Header with app name and a device status chip (HDR READY or LIMITED).
2. Source card: Choose video button, thumbnail, filename, key specs.
3. Output profile: HDR10 and optional HDR10+ with plain-language one-liners.
4. Frame rate and resolution (defaults auto).
5. HDR expansion slider (Natural to Aggressive) with live value.
6. Convert button, then progress card with percent, time left, cancel.
7. Result dialog: summary, Open, Share, Done.

**Design direction:** original dark UI with one accent colour. Do not reuse the reference app's yellow panels, logo or wording.

## 9. Monetization options

You have tended to prefer free, ad-free tools, so these are listed as options, not a decision.

| Model | Pros | Cons |
| --- | --- | --- |
| Free, no ads, donation link | Strong trust, easy launch | No revenue |
| Freemium (free up to 1080p, one-time unlock for 4K, batch, presets) | Simple, fits utility apps | Needs store billing |
| Ads | Quick revenue | Conflicts with privacy message and a no-internet design |
| Watermark on free exports | Marketing loop | Annoys creators |

Recommended for v1: free with a one-time Pro unlock later, once quality is proven.

## 10. Success metrics

- Conversion success rate above 95% on supported devices.
- Crash-free sessions above 99%.
- Median 1-minute 1080p30 clip converts in under about 2 minutes on a mid-range phone (to be benchmarked).
- Rating of 4.3 or higher after the first 500 reviews.
- Share of users who convert a second video in the first week.

## 11. Roadmap

| Phase | Scope | Notes |
| --- | --- | --- |
| 0. Spike (1 to 2 weeks) | Prove the pipeline: decode, GL shader, HEVC Main10 encode, play back as HDR on your own phone | Biggest technical risk, do this first |
| 1. MVP (4 to 6 weeks) | F1 to F15 on a handful of devices | Closed beta |
| 2. Beta (3 to 4 weeks) | Preview, history, presets, error handling, device matrix | Open testing on Play |
| 3. Launch | Store listing, guide, languages | Honest HDR messaging |
| 4. Growth | Batch, HLG, HDR10+ where possible, Ultra HDR photos | Based on feedback |

Timelines are rough estimates and depend heavily on how much GL and MediaCodec experience you bring or hire.

## 12. Legal and store checklist

- Original name, icon, UI and copy; do not imitate "Dicky" branding.
- Privacy policy stating that processing is local and files are not uploaded.
- Accurate store listing: say "simulated HDR from SDR" and list device requirements.
- Request no broad storage permission; use the system picker and MediaStore.
- Include open-source licences if you use libraries (for example Media3, Apache 2.0).

## 13. Open questions

1. Which minimum Android version and which chipsets do you want to support?
2. Do you want HDR10+ at launch, or only HDR10 with HDR10+ as a later add-on?
3. Should the app support photos (Ultra HDR) in v1 or video only?
4. Free, freemium or paid?
5. Which languages first?

## 14. Sources consulted

- Apple App Store listings: HDR Easy, HDR Converter, WowHDR; AppGoblin listing for Glow HDR.
- Android Developers: supported media formats (HDR formats table), Media3 Transformer supported formats and tone-mapping pages, media transcoding docs.
- Meta and trade coverage on Instagram HDR and Dolby Vision support (2025).
- Creator guides on colour space for YouTube, Instagram and TikTok exports (2026).
- Your three screenshots of the reference app.

Figures about installs, likes and ratings come from the pages and screenshots above at the time of research and may change.
