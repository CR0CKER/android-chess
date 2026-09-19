# E-ink fork — engineering notes

Last updated: 2026-09-19 03:13 AM CDT

Working notes for the `eink` branch of `CR0CKER/android-chess`, a fork of
[jcarolus/android-chess](https://github.com/jcarolus/android-chess) (MIT) adapted for
electrophoretic displays, developed against an **Onyx Boox Poke3** (6", greyscale,
Android 10 / API 29, 1072×1448).

---

## Contents

- [Build and deploy](#build-and-deploy)
- [Design decisions](#design-decisions)
- [What e-ink mode changes](#what-e-ink-mode-changes)
- [Hard-won gotchas](#hard-won-gotchas)
- [The unsolved button defect](#the-unsolved-button-defect)
- [Open items](#open-items)

---

## Build and deploy

**You cannot build this locally on the dev machine.** It is aarch64 Fedora Asahi, and
Google ships Android SDK build-tools and the NDK for `linux-x86_64` only — the SDK
repository advertises host-os `linux`/`macosx`/`windows` and nothing else, so `aapt2`,
`d8` and the NDK clang have no aarch64 host build.

Builds therefore run on GitHub Actions (`.github/workflows/build.yml`) on every push to
`eink`, producing the `chess-eink-debug-apk` artifact.

```bash
# newest run id
gh run list -R CR0CKER/android-chess --branch eink --limit 1

cd ~/Downloads && rm -f app-foss-debug.apk
gh run download -R CR0CKER/android-chess <run-id> -n chess-eink-debug-apk
```

Deploy over MTP (no adb on the dev machine):

```bash
M="/run/user/1000/gvfs/mtp:host=ONYX_SDM636-MTP__SN%3AAC5AFEE3_1B9BD45F/Internal shared storage"
gio copy ~/Downloads/app-foss-debug.apk "$M/Download/chess-eink-N.apk"
```

MTP quirks that cost time:

- **`cp` silently produces 0-byte files**; `gio copy` works. `ls -l` over MTP also reports
  bogus sizes. Always use `gio copy`.
- **MTP will not overwrite an existing file** — `libmtp error: could not delete object`.
  Copy under a new name each time.
- The mount goes stale when the device sleeps. Recover without replugging:
  ```bash
  gio mount -u "mtp://ONYX_SDM636-MTP__SN%3AAC5AFEE3_1B9BD45F/"
  gio mount    "mtp://ONYX_SDM636-MTP__SN%3AAC5AFEE3_1B9BD45F/"
  ```
- The device's own screenshots live in `Pictures/Screenshots/`; an app's external files
  dir (`Android/data/<pkg>/files/`) is readable over MTP on Android 10, which is how
  runtime diagnostics were retrieved without adb.

Build facts: Gradle 9.1.0, AGP 9.0.1, JDK 21, compileSdk 36, minSdk 24, NDK
29.0.14033849 (required — the engine is native C++ under `native/`). Release signing
wants a keystore at `../../android-keystore`; debug builds do not. There are **no JVM or
instrumentation tests**; the only tests are native C++ (`make test` in
`native/project/jni`).

<sub>[↑ Back to contents](#contents)</sub>

## Design decisions

**Dragging cannot be made smooth on e-ink, hence "Tap to move".**
`View.DragShadowBuilder.onDrawShadow()` is called *once*; the bitmap goes into a surface
owned by the system compositor, which then translates it with the finger. The app gets no
hook to throttle that, so the panel receives a fresh dirty region at touch-sampling rate
and smears. Tap-to-move costs two panel updates per move instead of hundreds. The
select/move machinery (`selectPosition`/`handleMove`) already existed for the square-click
path; pieces route into it too.

**Board squares are mid-grey and white, not black and white.** Alpha black pieces are
solid `#101010` with no light outline, so they vanish against a near-black square. The
dark square is `#9e9e9e` (`ColorSchemes.EINK[0]`, kept in step with `@color/einkBoardDark`).
`EINK` is index **10**: upstream 10.4.0 took 9 for its Custom scheme, and the saved
`colorscheme` preference is a positional index into the dropdown.

**Alpha is the only usable piece set** — the only flat, gradient-free one. Merida uses
`<aapt:attr>` gradients that `setTint` cannot flatten.

**The e-ink palette is its own resource set, not `values-night`.** Night mode keeps
saturated blue and green accents, which collapse into near-identical greys on a
reflective panel.

**No Onyx SDK.** Deliberate: keeps the FOSS build clean and supports non-Boox devices.
The cost is no full-refresh control — there is no AOSP way to request a full panel
refresh to clear accumulated ghosting. Boox's own system-wide refresh interval covers it.

**Detection is a vendor allowlist, not a display query.** Android exposes no
panel-technology API. `EinkMode.isEinkHardware()` matches `Build.MANUFACTURER`/`BRAND`/
`MODEL` against vendors whose entire line is e-ink (Onyx, Bigme, Dasung, Supernote,
Meebook, Boyue, Moaan, PocketBook, reMarkable, InkBook). Mixed-line vendors (Hisense,
TCL) are excluded so an LCD user is never given a greyscale board. Detection runs once,
on the first launch (`EinkMode.ensureInitialised`, when `einkMode` has never been
stored); after that the user's settings always win. Agreed with the maintainer in #241
(2026-09-18) — their own device, a BOOX Go 6, is caught by it.

<sub>[↑ Back to contents](#contents)</sub>

## What e-ink mode changes

**Settings, plus a preset that sets them** — the model agreed with the maintainer in
#241 (2026-09-18), replacing the earlier single mode that overrode everything. All
preferences live in `SharedPreferences("ChessPlayer")`; the logic is in
`helpers/EinkMode.java`.

The preset (`einkMode`; Board settings and Game settings) writes these values into the
ordinary settings, each of which stays changeable on its own:

| Key | New? | E-ink value | Default |
|---|---|---|---|
| `disableDrag` — Tap to move | new | on | off |
| `einkTheme` — E-ink theme | new | on | off |
| `reduceAnimations` — Reduce animations | new | on | off |
| `pref_use_piece_animation` | existing | off | on |
| `pieceset` | existing | Alpha (`"0"`) | `"0"` |
| `colorscheme` | existing | Greyscale (`"10"`, `ColorSchemes.EINK`) | `"0"` |
| `squarePattern` | existing | none (`"0"`) | `"0"` |
| `fullScreen` | existing | on | off |
| `minimal` | existing | on | off |

**Switching the preset off** restores each setting's value from before
(`einkPrevious.<key>`), **unless the setting no longer has its e-ink value** — then the
user changed it since, and it stays. The table is `EinkMode.PRESET`; apply, revert and
the defaults all come from it.

What each new setting controls:

| Setting | Effect |
|---|---|
| Tap to move | No drag shadow; tapping another own piece re-selects, except a king tap along its rank (castling) |
| E-ink theme | `ChessThemeEink`/`ChessStartEink`/`ChessDialogThemeEink`, black and white only; icon buttons white + black outline, text buttons black + white label, toggles invert; outlined switches and panes (`?attr/paneBackground`); no ripples; engine score unboxed; empty captured-piece slots hidden |
| Reduce animations | No indeterminate engine progress bar, no pulse, no RecyclerView item animators, no drag-over square highlight, engine PV balloon not updated |

Always on, regardless of settings: the clock and engine score only `setText` when the
text changes.

The three flags are cached statically by `EinkMode.load`, called in
`BaseActivity`/`StartBaseActivity` `onCreate` (**before** `super.onCreate`, see gotchas)
and `onResume`, and in `ChessBoardActivity.onResume`. Both base activities remember the
theme they were created with and `recreate()` in `onResume` if it has changed.

<sub>[↑ Back to contents](#contents)</sub>

## Hard-won gotchas

Each of these cost at least one build cycle.

- **`android:switchPadding` is silently ignored.** `SwitchMaterial` extends `SwitchCompat`,
  which reads `switchPadding` from its *own* styleable. Use the unprefixed attribute.
- **`android:layout_*` in a style only applies through an explicit `style=`**, never
  through `defStyleAttr` (`materialButtonStyle`). This cuts both ways and caught us in
  each direction:
  - *Removing* a `style=` drops the height and margin the style used to supply, so they
    have to be inlined on the tag.
  - *Adding* a `style=` to a widget that previously used `defStyleAttr` **activates**
    layout items that were until then inert. Giving `MaterialButtonToggleGroup` children
    an explicit style pointed them at `ChessButton`, whose `layout_margin="4dip"` then
    applied for the first time: the children gained margins, separated, and the group
    stopped merging their corners because they were no longer flush.

  A style intended for use via `style=` should therefore declare no `layout_*` items at
  all — which is why `ChessToggleButton` exists as a copy of `ChessButton` without them.
- **`--` is illegal inside an XML comment** and fails the resource merger.
- **`moveToPositions` is only filled when "Show moves" is on**, and only for the side to
  move. Never use it to decide whether a tap is legal: the re-select shortcut once did,
  which made castling onto one's own rook impossible with dots off. Use
  `getSelectableColor()` rather than `jni.getTurn()` for "whose piece is this" — Lichess
  pre-moves select pieces while it is the opponent's turn.
- **Upstream keeps some files CRLF** (`GamesListActivity.java`, `ICSChatDlg.java`,
  `savegame.xml`, `styles.xml`, `gradlew.bat`). An editor that normalises to LF turns a
  one-line change into a whole-file diff. Check with `grep -c $'\r$'` before committing.
- **New upstream layouts bring back `style="@style/ChessButton"`**, which bypasses the
  e-ink theme — use `?attr/chessButtonStyle` / `?attr/chessImageButtonStyle` instead. After every upstream sync: `git grep 'style="@style/Chess' -- 'app/src/main/res/layout*'`
  must print nothing.
- **R classes are non-transitive** (AGP default), so library attributes such as
  `colorPrimary` are absent from the app's compile-time `R` even though they exist in the
  merged resource table. Resolve them at runtime with `getIdentifier`.
- **`setTheme()` must precede `super.onCreate()`**, not merely `setContentView()`.
  AppCompat resolves theme attributes while building its delegate, so a later `setTheme`
  leaves widgets styled from the previous theme even across `recreate()`.
- **Dialog themes were hardcoded.** `ResultDialog` and six other dialogs passed
  `R.style.ChessDialogTheme` directly, so no dialog ever saw the e-ink palette.
- **`materialAlertDialogTheme` is inherited**, so `MaterialAlertDialog` kept colour
  buttons until `ChessAlertDialogThemeEink` was added.
- **Minimal mode moved the menu button.** `TextViewLastMove` is the stretched column
  (`stretchColumns="2"`); setting it `GONE` collapses the row. Use `INVISIBLE`.
- **A screen's `onPause` saves its controls — including during `recreate()`.** Applying
  the preset and then recreating let `onPause` write the *old* control values back over
  it. `BoardPreferencesActivity` saves first, applies, reloads its controls, then
  recreates; `PlayActivity` sets `switchMinimal` from prefs before recreating.
- **Python's `read_text()`/`write_text()` normalise CRLF to LF** and so rewrite whole
  CRLF files (`GamesListActivity.java` is *mixed*: 656 of 660 lines CRLF). Edit those
  with `sed`, or read and write bytes.
- **Preferences reset on install on this device**, so a fresh install came up in the
  colour theme. Two "the buttons are still black" reports turned out to be the *stock*
  blue theme on greyscale, not a defect — always confirm `einkTheme` before trusting a
  visual bug report.

<sub>[↑ Back to contents](#contents)</sub>

## The unsolved button defect

**Symptom.** Standalone text `MaterialButton`s paint solid black in e-ink mode. Pressing
one briefly reveals white label text.

**Measured, not inferred.** A temporary diagnostic rendered the buttons offscreen and
sampled the pixels:

```
BROKEN(text):  bgTint #FFFFFFFF  text #FF000000  stroke 2  insets 0/0  state=enabled
               size 193x88   pixels: centre/inset4/quarter = #FF000000
WORKING(icon): bgTint #FFFFFFFF  text #FF000000  stroke 2  insets 0/0  state=enabled
               size  88x88   pixels: centre/inset4/quarter = #FFFFFFFF, edge0 = #FF000000
```

Identical background tint, drawable state, stroke width, insets and drawable tree
(`RippleDrawable[InsetDrawable[LayerDrawable[MaterialShapeDrawable ×2]], MaterialShapeDrawable]`).
The only difference is **width**, which should not determine colour. The text
`ColorStateList` is honoured throughout — hence white text on press — while the white
background tint reaches the drawable and is never painted.

**Falsified on-device, in order:** the dialog-vs-activity context; `?attr/` style
resolution; resource-qualifier shadowing; the parent style; style duplication;
`colorPrimary`; and zeroed insets. None was the cause.

**Text buttons back on `?attr/` (2026-09-19).** As proposed to the maintainer in #241,
all 91 buttons in the 22 upstream layouts now use `style="?attr/chessButtonStyle"` or
`?attr/chessImageButtonStyle` — a one-line swap of upstream's `@style/ChessButton` /
`@style/ChessImageButton`, with the inlined height/margin stopgaps removed (the style
supplies them again). Not yet device-tested: if text buttons go black-on-black in a way
that differs from the known defect below, revert that commit on its own.

**Current resolution.** The black is made *intentional* — `ChessButtonEink` is solid
black with a white label — so it is readable regardless. Toggle-group children render
correctly and keep inverting via `chessToggleButtonStyle`.

**If revisiting:** use `adb logcat` and the layout inspector rather than more builds.
This was debugged entirely through screenshots and a text file pulled over MTP, which is
why it cost so many rounds. Set up `sudo dnf install android-tools` plus USB debugging
first.

<sub>[↑ Back to contents](#contents)</sub>

## Open items

- The button defect above.
- **`perf/board-diff`** — `rebuildBoard()` destroys and recreates every piece view on
  every move (still true in 10.4.0). A diffing rewrite exists at commit `728601c` on that branch, reverted from
  `eink` because no flicker is observable on the Poke3 and the change is high-risk across
  seven call sites with no local test capability.
- Screens never exercised in e-ink mode: Lichess (including the Swiss/Teams screens added
  in 10.4.0, whose loading spinners are still indeterminate), ICS, hotspot board, PGN tools.
- **Settings-plus-preset rework (2026-09-19) is CI-built only**, not yet device-tested.
  Checklist: fresh install (uninstall first — an old install has `einkMode=true` but none
  of the new keys, so it comes up in colour; switching e-ink off and on once fixes it)
  comes up in e-ink; change the piece set, switch e-ink off → piece set stays, the rest
  reverts; switch on again; re-enable dragging with e-ink on; toggle from Game settings →
  play screen restyles and minimal follows; normal play, undo, flip, castling with dots off.
- **Rebased onto upstream 10.4.0 and device-tested on the Poke3, 2026-09-17** — normal play
  is good. Not individually confirmed yet: Chess960 castling with "Show moves" off, and the
  Lichess Swiss/Teams screens added in 10.4.0.
- `res/anim/` is dead (nothing references `R.anim.*`), left in place to keep the upstream
  diff small.
- `PlayActivity.onResume` ends with `postDelayed(this::updateGUI, 1000)`. This is the
  *only* GUI refresh on resume, not a redundant one — do not delete it.

<sub>[↑ Back to contents](#contents)</sub>
