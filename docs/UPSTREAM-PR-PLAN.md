# Upstream contribution plan

Last updated: 2026-09-17 07:04 AM CDT

How to get work from this fork into
[jcarolus/android-chess](https://github.com/jcarolus/android-chess). Written while the
context is fresh; everything below is verifiable from the branch.

The `eink` branch is **not** an upstream candidate as it stands — it is 41 commits mixing
a device-specific feature with generic fixes, and it defaults behaviour based on hardware
detection. Split it.

**Order agreed 2026-09-17:** first open a feature request asking whether an e-ink mode
would be welcome at all (draft kept outside the repo), then device-test the branch
rebased onto upstream 10.4.0, and only then prepare a PR. Tier 3 below is fork-only
*unless* the maintainer says yes to that request.

**Commit hashes** below are those of the branch after it was rebased onto upstream
`4be3f52` (10.4.0) on 2026-09-17. The pre-rebase history is kept at tag
`eink-pre-sync-2026-09-17`.

---

## Contents

- [Before you start](#before-you-start)
- [Tier 1 — send these first](#tier-1--send-these-first)
- [Tier 2 — plausible, needs discussion](#tier-2--plausible-needs-discussion)
- [Tier 3 — fork-only](#tier-3--fork-only)
- [Mechanics](#mechanics)
- [Known frictions](#known-frictions)

---

## Before you start

- Upstream has **no CI, no JVM tests, no instrumentation tests** and no `.github/`
  directory (re-checked 2026-09-17). A maintainer cannot verify a PR automatically; keep each one small and
  independently reviewable.
- Upstream is a single-maintainer project. Batch-and-dump will not get reviewed. Of the 7
  outside PRs merged since 2019, 4 were translations and the other 3 were small; the README does say
  "Contributions welcome — feel free to open a PR or issue". One
  focused PR at a time, smallest and most obviously-correct first, is the way in.
- `git config user.email` must print `6056387+CR0CKER@users.noreply.github.com` before
  any commit. Verify per clone.
- Rebase on `upstream/master` before opening anything:
  ```bash
  git remote add upstream https://github.com/jcarolus/android-chess.git   # once
  git fetch upstream && git rebase upstream/master
  ```

<sub>[↑ Back to contents](#contents)</sub>

## Tier 1 — send these first

Self-contained, no behaviour change for existing users, each defensible in a paragraph.
Cherry-pick each onto its own branch off `upstream/master`.

### 1. Gradle wrapper replacement — `2c27d51`

The committed `gradle-wrapper.jar` is a **Gradle 1.6 snapshot built 2013-04-04**
(`isSnapshot=true` in its own `build-receipt.properties`), added in a 2015 commit, while
`gradle-wrapper.properties` requests 9.1.0. Being an unreleased snapshot it matches no
published checksum and fails Gradle's wrapper validation.

Replaced with the official 9.1.0 jar, sha256
`76805e32c009c0cf0dd5d206bddc9fb22ea42e84db904b764f3047de095493f3`, matching
`services.gradle.org/distributions/gradle-9.1.0-wrapper.jar.sha256`. `gradlew` and
`gradlew.bat` come from the same `v9.1.0` tag; `gradlew.bat` must keep upstream's CRLF line
endings (the content is otherwise identical to Gradle's).

*Strongest candidate — a pure supply-chain fix with an externally verifiable checksum.*

### 2. `getHightlightColor()` ignores its own table

`constants/ColorSchemes.java` returns a hardcoded `0x66ffff00` with the real lookup
commented out, so slot `[3]` of every scheme is dead. The branch restores the table read
but then fills slot `[3]` of every colour scheme with that same `0x66ffff00`, so the
per-scheme values stay unused. Upstream it is a tidy-up with no visible effect, not a
bug fix — the weakest item here; only worth sending if the maintainer wants
per-scheme highlights.

*Extract from `1c6ec43`; do not bring the e-ink scheme row with it.*

### 3. `onDraw` allocations — `56fc5fa`

`ChessSquareView.onDraw` inflated a `Drawable` via `getResources().getDrawable()` on every
repaint (twice: tile pattern and D-pad focus ring) and allocated two `Rect`s.
`ChessPieceLabelView.onDraw` called `setTextSize` unconditionally, which requests a layout
and invalidates — a draw scheduling another draw.

*Pure performance, no visual change, benefits every device.*

### 4. `showMoves` default mismatch — `22abcea`

`BoardPreferencesActivity` reads `showMoves` with default `true` while `ChessBoardActivity`
uses `false`. A fresh install therefore has no destination dots until board settings has
been opened once; that screen then shows the box ticked and writes `showMoves=true` back
in `onPause`. `ChessBoardActivity` now defaults to `true` too — the maintainer describes
"Show moves" as on by default in #207. (The branch first fixed this the other way, making
both `false`; that contradicted upstream's intent and was reversed.)

### 5. Clock and engine `setText` guards

`LocalClockApi` ticks twice a second for a display that changes once a second, so half the
clock redraws were redundant. Same guard for the engine evaluation. Both now only call
`setText` when the string actually changes.

*Extract from `e25bd60` — take the guards, not the e-ink balloon suppression.*

<sub>[↑ Back to contents](#contents)</sub>

## Tier 2 — plausible, needs discussion

### CI workflow — `19d77f3` + `834e620`

`.github/workflows/build.yml` builds `assembleFossDebug` and uploads the APK.
Upstream issue **#198** asked about CI/CD adoption, so there is prior interest. Open as a
question referencing that issue rather than an unsolicited PR — a maintainer may have
opinions about Actions minutes and secrets.

Note it must accept SDK licences before installing the NDK, or `sdkmanager` stalls on an
interactive prompt and the build later fails with `LicenceNotAcceptedException`.

### Diff-based `rebuildBoard` — branch `perf/board-diff`, commit `728601c`

*Written against the pre-10.4.0 `ChessBoardActivity`; upstream has since added ~460
lines there (move animation, pre-moves), so expect to redo it rather than cherry-pick.*

`rebuildBoard()` removes every `ChessPieceView` and constructs ~32 replacements on each
call, from seven call sites covering every move, undo, redo, flip, setup and promotion.
The rewrite keeps views that already match, pools the rest, and prefers a spare already
depicting the same piece — a normal move reuses one view and allocates nothing.

**Reverted from `eink` deliberately:** no flicker is observable on the Poke3, so the
visual justification did not hold, and it is high-risk with no local test capability.
Offer it upstream only with that caveat stated, since a maintainer with a test setup can
validate what we could not. Needs verification of undo/redo, flip, setup, promotion,
castling, Chess960 and duck chess.

### Tap-to-move as an accessibility option

Independent of e-ink, this helps anyone who finds dragging hard, and relates to closed
issues **#207** ("Point and move") and **#211** (accessibility). Would need reframing as a
general preference rather than an e-ink side effect.

<sub>[↑ Back to contents](#contents)</sub>

## Tier 3 — fork-only

Do not send these. They are device-specific or behaviour-changing:

- `EinkMode` and the entire e-ink theme/style/drawable set
- Vendor-based hardware detection and the resulting default
- Forced minimal controls and forced fullscreen
- The `ColorSchemes.EINK` row (index 10, after upstream's `CUSTOM_COLOR_SCHEME` 9) and
  Alpha piece-set forcing
- Solid-black text buttons (a workaround for an unexplained rendering defect, not a fix)
- Conversion of `ChessButton`/`ChessImageButton` to theme attributes, and the removal of
  `style=` from 48 layout buttons — churn that only exists to support e-ink theming

<sub>[↑ Back to contents](#contents)</sub>

## Mechanics

```bash
git fetch upstream
git checkout -b upstream/gradle-wrapper upstream/master
git cherry-pick 2c27d51
# build check: push and let this fork's CI run it, since there is no local toolchain
git push -u origin upstream/gradle-wrapper
gh pr create -R jcarolus/android-chess --base master \
  --title "build: replace 2013 Gradle 1.6 snapshot wrapper with official 9.1.0" \
  --body-file /tmp/pr-body.md
```

PR body structure upstream will find easiest to review:

- **What changed** — one paragraph
- **Why** — the concrete defect, with the evidence (checksum, file:line, measured numbers)
- **Testing** — say plainly that it was built via GitHub Actions and, where relevant, that
  there is no local toolchain; never imply tests were run that were not

Every PR must state that it was verified by a CI build only, since there is no test suite
to point at.

<sub>[↑ Back to contents](#contents)</sub>

## Known frictions

- **No upstream CI**, so a PR arrives unverified from the maintainer's point of view.
  Mentioning that this fork's Actions build is green helps.
- **The fork's history is not linear against upstream** — 41 commits including several
  diagnostic add/remove pairs. Always cherry-pick onto a fresh branch; never
  propose `eink` directly.
- **Commit `728601c` now lives only on `perf/board-diff`** (and the pre-rebase tag). The
  add/revert pair was dropped from `eink` when rebasing, as it changed nothing there.
- Several Tier-1 items are *embedded in larger e-ink commits* rather than isolated, so
  they need extracting by hand rather than a clean cherry-pick. The commit hashes above
  name where to find them.

<sub>[↑ Back to contents](#contents)</sub>
