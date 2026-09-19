# Upstream contribution plan

Last updated: 2026-09-19 05:08 AM CDT

How to get work from this fork into
[jcarolus/android-chess](https://github.com/jcarolus/android-chess). Written while the
context is fresh; everything below is verifiable from the branch.

The `eink` branch is **not** a PR as it stands — its history mixes the feature with
generic fixes and diagnostic add/remove pairs. PRs are built on fresh branches off
`upstream/master`. The feature itself is **agreed with the maintainer** (#241,
2026-09-18), in the settings-plus-preset form `eink` now implements.

**Order agreed 2026-09-17:** first open a feature request asking whether an e-ink mode
would be welcome at all, then device-test the branch rebased onto upstream 10.4.0, and
only then prepare a PR. Tier 3 below is fork-only *unless* the maintainer says yes.

**Commit hashes** below are those of the branch after it was rebased onto upstream
`4be3f52` (10.4.0) on 2026-09-17. The pre-rebase history is kept at tag
`eink-pre-sync-2026-09-17`.

---

## Contents

- [Where this stands](#where-this-stands)
- [Before you start](#before-you-start)
- [Tier 1 — send these first](#tier-1--send-these-first)
- [Tier 2 — plausible, needs discussion](#tier-2--plausible-needs-discussion)
- [Tier 3 — fork-only](#tier-3--fork-only)
- [Mechanics](#mechanics)
- [Known frictions](#known-frictions)

---

## Where this stands

**Three PRs are open (2026-09-19), waiting for the maintainer to merge or comment:**

| PR | Branch (on the fork) | What | Notes |
|---|---|---|---|
| [#242](https://github.com/jcarolus/android-chess/pull/242) | `fix/gradle-wrapper` | Gradle 1.6 snapshot wrapper → official 9.1.0 | 3 files; merges cleanly with the others |
| [#243](https://github.com/jcarolus/android-chess/pull/243) | `perf/clock-settext-guard` | Clock and engine score only redraw on change | 1 file; **conflicts with #244** in `PlayActivity.OnEngineInfo()` |
| [#244](https://github.com/jcarolus/android-chess/pull/244) | `feat/eink-settings` | The e-ink feature: three settings, greyscale scheme, preset, auto-enable, `?attr/` button styles, minimal-controls fix | 56 files, one commit; device-tested on the Poke3 |

See *What to do when the maintainer responds* at the end of this section. The feature
itself was agreed in [issue #241](https://github.com/jcarolus/android-chess/issues/241)
(2026-09-18); the exchanges are below, in order. The issue asked three
questions: is an e-ink mode wanted, should it default on via vendor detection, and is
converting layouts from `style="@style/ChessButton"` to theme attributes acceptable.

**Maintainer's answer (jcarolus, 2026-09-18):**

1. **Wanted:** PRs are judged on benefit, and this one "looks like it will be" beneficial.
2. **Separate settings, not one mode.** Their own e-reader refreshes fast; they would keep
   dragging and maybe another piece set. They suggested a **master "e-ink preset" switch
   that sets the existing switches** (e.g. `pref_use_piece_animation`, `pieceset`) to
   e-ink-friendly values. So the forcing `EinkMode` design does not go upstream as is, and
   **vendor auto-detect is out** of the upstream version.
3. **Unclear to them** — "once point 3 is a bit more clear lets talk PR".

Also explicitly welcomed: the clock-tick guard, the Gradle wrapper and the GitHub Actions
workflow.

**Our reply to point 3 (2026-09-18):** explained that an explicit `style=` bypasses the
theme (91 buttons in 22 layouts: 65 `ChessButton`, 26 `ChessImageButton`), and proposed
the narrow fix: `style="?attr/chessButtonStyle"` / `?attr/chessImageButtonStyle`, mapped
to the existing styles in every current theme. One line per button, plus `attrs.xml` and
`themes.xml`, no Java. Also asked which device they use. *Not yet built in the all-`?attr`
form:* the `eink` branch uses `?attr/` only for image and toggle buttons, and strips
`style=` from text buttons (see `attrs.xml` and `EINK-NOTES.md` → *The unsolved button
defect*, where `?attr/` resolution was later ruled out as the cause).

**Second exchange (2026-09-18).** jcarolus: device is a **BOOX Go 6 (Gen II)** (Onyx, so on
our vendor list); the `?attr/` style change is accepted ("that cleans up the `style` in
the templates"); asked whether anything beyond "animate pieces", piece set and a new
disable-drag setting is needed.

Our reply (16:15 UTC):

- **Proposes keeping auto-enable** after all: on recognised e-ink devices the mode (the
  preset) switches on automatically, and the user can turn it off entirely or turn
  individual features back on. A fully manual preset is offered as the alternative, and we
  asked whether auto-enable is OK. Note the maintainer's own device would be caught, so
  expect a real opinion on this.
- **Settings proposed:** three new ones — *disable drag*, *e-ink theme* (black-and-white
  screens and dialogs, outlined buttons and panes, no ripples; next to night mode) and
  *reduce animations* (engine progress bar, button pulse, list item animations). Plus a
  new greyscale entry in the colour-scheme list. Everything else reuses existing
  settings: `pref_use_piece_animation` off, `pieceset` = Alpha, `fullScreen`, `minimal`.
- **Preset:** switches all of these on in one go; each stays individually changeable.
- **PR order proposed:** small PRs (Gradle wrapper, CI, clock) → `?attr/` style change →
  settings + preset.

The tap-to-reselect behaviour belongs to *disable drag*; the info-balloon suppression and
hidden empty captured-piece slots belong to *reduce animations* and the *e-ink theme*
respectively (mapping from the `EinkMode.isEnabled()` call sites on `eink`).

**Third exchange (2026-09-18, 18:44 UTC):** jcarolus: *"Ok, agreed on the
auto-detection. I'll keep an eye out for the PR's"*. Everything proposed is agreed.

**Decided by the fork owner (2026-09-19):**

- `eink` itself is reworked onto the settings-plus-preset model; no separate upstream
  branch. Done 2026-09-19 (see `EINK-NOTES.md` → *What e-ink mode changes*).
- Switching the preset off restores the previous values, except settings the user has
  changed since.
- Text buttons move to `style="?attr/chessButtonStyle"`, as proposed in #241.
- PRs: Gradle wrapper and clock guard as their own small PRs; the `?attr/` style change
  and the settings plus preset together as **one feature PR**.

**How the PRs were built (2026-09-19):**

- Each is one commit on a fresh branch off `upstream/master`, pushed to the fork with
  tracking set (`origin/<branch>`).
- #244 is `eink`'s tree minus everything that isn't the feature: `.github/`, `docs/`, the
  wrapper (#242), the clock guard (#243), the `showMoves` default and the `onDraw` changes
  (both later PRs). `git diff eink feat/eink-settings` (ignoring those paths) shows only
  those four files.
- The highlight-colour table (Tier 1 item 2) went into #244 rather than its own PR: the
  greyscale scheme needs per-scheme highlight and coordinate colours.
- The minimal-controls fix (last-move text `INVISIBLE`, not `GONE`) is in #244 and named
  in its description.
- The black text buttons are raised in #244 as a known quirk, asking whether the
  maintainer knows the cause.
- Every PR text was approved by Nils before opening. #242 and #243 end with the default
  "🤖 Generated with Claude Code" line; from #244 on, PRs end with exactly "Developed with
  the assistance of Claude Code." (no link) — see `~/.claude/CLAUDE.md`.
- CI: upstream has no workflow, so each branch was built on the fork via a throwaway
  `ci/<name>` branch (see *Mechanics*). Runs:
  [#242](https://github.com/CR0CKER/android-chess/actions/runs/35431189386),
  [#243](https://github.com/CR0CKER/android-chess/actions/runs/35431220654),
  [#244](https://github.com/CR0CKER/android-chess/actions/runs/35436183977). Upstream's
  own 2013 wrapper jar fails `setup-gradle`'s wrapper validation, so #243 and #244 only
  build on CI together with the #242 fix.

Branch state: `eink` is rebased onto upstream `4be3f52` (10.4.0), CI green, and tested on
the Poke3. Pre-rebase history is at tag `eink-pre-sync-2026-09-17`, on the fork as well as
locally.

**Positions taken in #241, for consistency if it turns into a discussion:**

- The solid-black text buttons were not raised in #241, but are raised in #244 as a
  known quirk (a workaround for an unexplained rendering defect, see `EINK-NOTES.md`).
- Every claim in the issue is verifiable: the Gradle wrapper snapshot, the `onDraw`
  allocations, the `showMoves` mismatch and the clock ticks were all re-checked against
  `upstream/master` on 2026-09-17.

**What to do when the maintainer responds:**

| Event | Next step |
|---|---|
| #243 or #244 merged | Rebase the other onto the new `upstream/master`, resolve `OnEngineInfo()` (keep both the `setText` guard and the `isReduceAnimations()` balloon condition), build via a `ci/` branch, force-push its branch. Its description already announces this. |
| Any PR merged | Delete its branch on the fork (`git push origin --delete <branch>`) and locally. Then sync `eink` (below). |
| Review comments | Answer or fix each one. Draft replies for Nils's approval before posting; if a fix adds a commit, update the PR description in the same pass. |
| All three merged | Send the remaining Tier 1 items as small PRs: `onDraw` allocations (item 3), `showMoves` default (item 4), CI workflow (item 6). |

**Syncing `eink` after a merge:** `git fetch upstream && git rebase upstream/master` on
`eink`. The merged changes already exist on `eink` as different commits (and a squash
merge changes the SHAs), so expect conflicts or empty commits. Resolve them in upstream's
favour, then re-run the after-sync checks in `CLAUDE.md`. `eink`'s own `OnClockTime()`
comment differs slightly from #243's; take #243's.

<sub>[↑ Back to contents](#contents)</sub>

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

*Welcomed in #241. **Opened as [#242](https://github.com/jcarolus/android-chess/pull/242) (2026-09-19).***

### 2. `getHightlightColor()` ignores its own table

`constants/ColorSchemes.java` returns a hardcoded `0x66ffff00` with the real lookup
commented out, so slot `[3]` of every scheme is dead. The branch restores the table read
but then fills slot `[3]` of every colour scheme with that same `0x66ffff00`, so the
per-scheme values stay unused. Upstream it is a tidy-up with no visible effect, not a
bug fix — the weakest item here; only worth sending if the maintainer wants
per-scheme highlights.

*No separate PR: folded into [#244](https://github.com/jcarolus/android-chess/pull/244), which needs per-scheme highlight and coordinate colours for the greyscale scheme.*

### 3. `onDraw` allocations — `56fc5fa`

`ChessSquareView.onDraw` inflated a `Drawable` via `getResources().getDrawable()` on every
repaint (twice: tile pattern and D-pad focus ring) and allocated two `Rect`s.
`ChessPieceLabelView.onDraw` called `setTextSize` unconditionally, which requests a layout
and invalidates — a draw scheduling another draw.

*Pure performance, no visual change, benefits every device. **Next small PR**, once the open three are through.*

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

*Welcomed in #241. **Opened as [#243](https://github.com/jcarolus/android-chess/pull/243) (2026-09-19).***

### 6. CI workflow — `19d77f3` + `834e620`

`.github/workflows/build.yml` builds `assembleFossDebug` and uploads the APK.
Welcomed by the maintainer in #241 (2026-09-18); upstream issue **#198** had asked about
CI/CD adoption too. Reference both in the PR. Not opened yet; send after #242, since the
workflow cannot pass with the old wrapper jar.

Note it must accept SDK licences before installing the NDK, or `sdkmanager` stalls on an
interactive prompt and the build later fails with `LicenceNotAcceptedException`.

<sub>[↑ Back to contents](#contents)</sub>

## Tier 2 — plausible, needs discussion

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
issues **#207** ("Point and move") and **#211** (accessibility). **Now a general setting
("Tap to move") in #244**, so no separate PR is needed.

<sub>[↑ Back to contents](#contents)</sub>

## Tier 3 — fork-only

Mostly superseded: the maintainer agreed to the feature in #241, so `EinkMode`, the e-ink
theme/style/drawable set, vendor detection, the greyscale colour scheme and the `?attr/`
button styles all go into the **feature PR**. Forced minimal, forced fullscreen and the
forcing of board appearance no longer exist — the preset writes ordinary settings.

Still fork-only:

- `docs/EINK-NOTES.md` and this file.
- The solid-black text buttons (`ChessButtonEink`) are a workaround for an unexplained
  rendering defect, not a fix. Raised in #244 as a known quirk.

<sub>[↑ Back to contents](#contents)</sub>

## Mechanics

The procedure used for #242–#244 (2026-09-19):

```bash
git fetch upstream
git switch -c fix/thing upstream/master     # fix/, perf/, feat/ prefixes
# bring the change over: cherry-pick, or `git checkout eink -- <files>` and trim by hand
git commit                                  # Conventional Commit, Co-Authored-By trailer

# CI check: upstream has no workflow and the PR branch must not gain one, so build a
# throwaway branch = PR commit + the #242 wrapper fix + the workflow with "ci/**" added
# to its push trigger (a push event reads the workflow from the pushed commit).
git switch -c ci/thing fix/thing
git cherry-pick -x 823ba7c                  # wrapper fix, until #242 is merged
git checkout eink -- .github/workflows/build.yml
sed -i 's/branches: \[eink, master\]/branches: [eink, master, "ci\/**"]/' .github/workflows/build.yml
git add .github/workflows/build.yml         # the sed edit must be staged, or the push builds nothing
git commit -m "ci: throwaway build of fix/thing"
git push -f origin ci/thing                 # wait for green, then delete the branch

git push -u origin fix/thing
# draft title + body, Nils approves the exact text, then:
gh pr create -R jcarolus/android-chess --base master --head CR0CKER:fix/thing \
  --title "<approved title>" --body-file <approved body>
git push origin --delete ci/thing && git branch -D ci/thing
```

Before committing on a PR branch, check line endings against upstream
(`grep -c $'\r$'`). Several files are CRLF, and `GamesListActivity.java` is mixed.

PR body structure upstream will find easiest to review:

- **What changed** — one paragraph
- **Why** — the concrete defect, with the evidence (checksum, file:line, measured numbers)
- **Testing** — say plainly that it was built via GitHub Actions and, where relevant, that
  there is no local toolchain; never imply tests were run that were not

Every PR must state how it was verified: a CI build (there is no test suite to point at),
plus any device testing, naming what was *not* tested. End with exactly "Developed with
the assistance of Claude Code." (no link).

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
