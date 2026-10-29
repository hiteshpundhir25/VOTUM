# VOTUM — MASTER BLUEPRINT (Empty Master Template)

> **Purpose.** This file is the *empty master* of the VOTUM project: it states the complete
> structure, every class contract, every data format and every business rule of the system,
> **without containing any implementation**. Any implementation that satisfies the contracts
> below reproduces the shipped application exactly.
>
> **Verified state (2026-10-29, JDK 26):** `javac` compile clean · `javac -Xlint:all` → 0 warnings, 0 errors · automated domain suite **24 / 24 PASS** · UI & academic contract smoke harness **27 / 27 PASS** (51 total checks PASS) · no direct storage access from `ui/`.
>
> **Part I (§1–§13)** is the empty master blueprint: structure, contracts, rules, data formats.
> **Part II** is the requested Design & Analysis Report (≤ 2 pages, bullet points, diagrams).

---

## 1. System Identity & Scope

| Item | Value |
| :--- | :--- |
| Product | **VOTUM** — Digital Campus Election Management System |
| Kind | Single-user Java desktop application (Swing), zero external dependencies |
| Runtime | JDK 17+ (verified on JDK 26), plain `javac` / `java` |
| Persistence | Delimiter-separated plain-text files in `data/` + append-only `audit.log` |
| Actors | `Admin` (Election Officer) and `Voter` (student) |
| Entry point | `com.votum.Main` |

**In scope:** admin authentication, **election creation from the admin UI**, election configuration
& status control, election deletion with cascade clean-up, candidate registration, voter-roll
registration, ballot casting with single-vote enforcement, live tally/turnout/tie detection,
official result dialog, audit trail viewer.

**Out of scope (must not be added):** databases/JDBC, network or web access, SSO/OTP, password
hashing frameworks, cryptographic ballot sealing, department-restricted elections, third-party
libraries of any kind. See §12.

---

## 2. Architecture Map

```text
src/com/votum/
├── Main.java                     # composition root (wires storage → services → UI)
├── model/                        # pure data + domain invariants (no I/O, no Swing)
│   ├── Identifiable.java  AuthenticatableUser.java
│   ├── User.java (abstract)  Admin.java  Voter.java  Candidate.java
│   ├── Election.java  ElectionStatus.java  Vote.java  ElectionResult.java
├── exception/                    # checked domain exception hierarchy (root: VotumException)
│   ├── VotumException.java  InvalidLoginException.java  DuplicateVoteException.java
│   └── ElectionClosedException.java  InvalidCandidateException.java  InvalidElectionException.java
├── storage/                      # text persistence only (no business rules)
│   ├── FileManager.java  AuditLogger.java
├── service/                      # all business rules (no Swing, no direct file APIs)
│   ├── AuthenticationService.java  ElectionService.java
│   └── VotingService.java  ResultService.java
├── ui/                           # Swing presentation only (talks to services, never to files)
│   ├── UITheme.java  LoginFrame.java  AdminDashboard.java
│   ├── VoterDashboard.java  ResultDialog.java
│   └── DateTimeInput.java        # validated JDK date-time field (create-election form)
└── test/
    ├── VotumTestSuite.java       # 24-point automated domain verification harness
    └── UISmokeTestSuite.java     # 27-point automated UI & academic contract smoke harness
```

**Dependency direction (enforced invariant):**

```text
ui  ──▶  service  ──▶  storage  ──▶  model
          └──────────────┴─────────────▶ exception
```

1. `model` depends on nothing but the JDK (`java.time`, collections).
2. `storage` may throw/consume `model` only; it never applies a business rule.
3. `service` owns every business rule and never imports `javax.swing` or `java.io` file APIs.
4. `ui` calls `service` methods only — **verified: `grep -rn "fileManager.load" src/com/votum/ui/` returns nothing.**
   Frames still *carry* `FileManager`/`AuditLogger` references solely to hand them to the next frame on logout.
5. `test` may touch every layer (isolation harness for storage).

---

## 3. Layer Contracts (Template Skeleton)

### 3.1 `model`

| Type | Fields (invariants) | Constructor | Contract |
| :--- | :--- | :--- | :--- |
| `Identifiable` (interface) | — | — | `String getId()` contract implemented by User, Admin, Voter, Candidate, Election, Vote |
| `AuthenticatableUser` (interface) | — | — | `getUsername()`, `verifyPassword(plain)`, `getRole()`, `getDisplayName()` contract |
| `User` (abstract class) | `username`, `password`, `role` (all non-null; role `ADMIN`/`VOTER`) | `User(String,String,String)` | implements `AuthenticatableUser, Identifiable`; accessors; `isAdmin()`, `isVoter()`; abstract `getDisplayName()`, abstract `getId()`; `toString()` never prints the password |
| `Admin extends User` | `adminId`, `fullName`, `email` | `Admin(username,password,adminId,fullName,email)` → `super(...,"ADMIN")` | implements `getId()` returning `adminId`, `getDisplayName()` returning `fullName (Officer)`; accessors; `toString()` |
| `Voter extends User` | `voterId`, `name`, `department`, `eligible`, `hasVoted`, `votedElectionId` (never null, `""` when unknown) | `Voter(voterId,name,department,username,password,eligible,hasVoted)` and `…,votedElectionId)` → `super(...,"VOTER")` | implements `getId()` returning `voterId`, `getDisplayName()` returning `name (department)`; accessors; `hasVoted()` is a *cache*; the authoritative per-election truth is the ballot record |
| `Candidate` | `candidateId`, `name`, `department`, `electionId`, `slogan` (never null) | `Candidate(id,name,dept,electionId)` → slogan `""`; `Candidate(…,slogan)` | implements `Identifiable` (`getId()` = `candidateId`); accessors; `toString()` = `name (id - dept)` |
| `Election` | `electionId`, `name`, `description` (non-null), `startTime`, `endTime`, `status` (never null; defaults `DRAFT`) | `Election(id,name,description,startTime,endTime,status)` | implements `Identifiable` (`getId()` = `electionId`); `isCurrentlyActive(now)` = status OPEN **and** `!(now < start)` **and** `!(now > end)`; `isBeforeStart(now)`; `isAfterEnd(now)`; window bounds are **inclusive** when non-null |
| `ElectionStatus` | enum | — | `DRAFT` → `OPEN` → `CLOSED` (no other values may be persisted) |
| `Vote` | `voteId`, `electionId`, `candidateId`, `voterId`, `timestamp` (never null; defaults to `now` if unset) | `Vote(voteId,electionId,candidateId,voterId,timestamp)` | implements `Identifiable` (`getId()` = `voteId`); immutable-by-convention ballot record |
| `ElectionResult` | `electionId`, `electionName`, `candidateVoteCounts` (insertion-ordered), `candidateDetails`, `totalVotes`, `eligibleVotersCount`, `turnoutPercentage`, `winningCandidates`, `isTie` | `ElectionResult(electionId, electionName)` → empty maps, 0 metrics | `addCandidateTally(Candidate,int)` (ignores nulls, clamps negatives), `getVotesFor(String)` (null-safe → 0), `getCandidate(String)` (null-safe), `getCandidateVoteCounts()` **unmodifiable**, `getWinningCandidates()` **unmodifiable copy**, `getWinnerSummary()` states below |

`getWinnerSummary()` contract:

1. `totalVotes == 0` → `"No votes recorded yet."`
2. no winner recorded → `"No winner determined."`
3. `isTie` → `"TIE between: <name>, <name>, …"`
4. otherwise → `"Winner: <name> (<n> votes)"`

### 3.2 `exception`

`VotumException(String message)` / `VotumException(String message, Throwable cause)` is the root
(checked, extends `Exception`). All five subclasses are single-argument, message-only and carry
`serialVersionUID = 1L`.

| Exception | Thrown when |
| :--- | :--- |
| `InvalidLoginException` | blank credentials, unknown user, or wrong password |
| `DuplicateVoteException` | the same voter already has a ballot for the target election |
| `ElectionClosedException` | election missing, `DRAFT`, `CLOSED`, before start, or after end |
| `InvalidCandidateException` | candidate blank / not registered for the target election |
| `InvalidElectionException` | election, candidate or voter validation or persistence failure |

### 3.3 `storage`

| Type | Contract |
| :--- | :--- |
| `FileManager` | `FileManager()` → `"data"`; `FileManager(String dataDir)` creates the directory and seeds demo data when a file is missing or empty. All public methods `synchronized` and re-entrant. Constants: delimiter `";"`, date format `DateTimeFormatter.ISO_LOCAL_DATE_TIME`. `resetToDemoData()` restores default admin, active election E001, 3 candidates, 5 voters, and empty ballots. |
| `loadAdmins/loadVoters/loadCandidates/loadElections/loadVotes()` | return a fresh `List`; skip blank lines and `#` comments; **skip malformed records individually** (warn on `stderr`, never abort the file); `loadElections` additionally rejects unparseable date or status; `loadVotes` rejects unparseable timestamps |
| `saveAdmins/saveVoters/saveCandidates/saveElections/saveVotes(List)` | overwrite the file; **sanitize every free-text value** (delimiter/CR/LF); return `true` on success, `false` on `IOException` |
| `updateVoter(Voter)` / `addCandidate(Candidate)` / `saveOrUpdateElection(Election)` / `addVote(Vote)` | read-modify-write with case-insensitive key matching; return the write result |
| `deleteElection(String)` | remove the election, then its candidates, then its ballots, then reset `hasVoted`/`votedElectionId` of every voter pointing at it; `true` only if the election existed |
| `AuditLogger` | `AuditLogger()` → `"data/audit.log"`; `AuditLogger(String path)` creates the file/parent directory. `log(event, details)` appends `yyyy-MM-dd HH:mm:ss \| EVENT \| DETAILS`. `readAllLogs()` returns non-blank lines in chronological order. `removeLogsForElection(id)` rewrites the file keeping only entries that do **not** contain the ID as a *whole token* (`[A-Za-z0-9_-]+`, case-insensitive, so `E001` never matches `E0010` or `VOTE-E001…`). `clearAllLogs()` truncates the file. I/O failures are reported on `stderr` and never propagate. |

### 3.4 `service`

| Service | Constructor | Contract |
| :--- | :--- | :--- |
| `AuthenticationService` | `(FileManager, AuditLogger)` | `login(username,password) throws InvalidLoginException → User`: blank → reject; administrators are matched before voters; username comparison is case-insensitive on a trimmed value, password comparison is exact; every attempt is audited (`Admin logged in`, `Voter logged in`, `Login failed`). Never returns `null`. |
| `ElectionService` | `(FileManager, AuditLogger)` | election lifecycle, participant registration, cascade delete — see table below |
| `VotingService` | `(FileManager, AuditLogger, ElectionService)` | `castVote(voterId,electionId,candidateId) throws VotumException → Vote` (uses the system clock); `castVoteAt(…, LocalDateTime) throws VotumException → Vote` (**synchronized**, injectable clock, used for boundary verification); `hasVoterCastVote(voterId, electionId) → boolean` (ballot records first, then the cached flag **for that election only**) |
| `ResultService` | `(FileManager, AuditLogger, ElectionService)` | `calculateResult(electionId) → ElectionResult`: tallies ballots of that election against its registered candidates, counts **eligible** voters, turnout = `round(totalVotes / eligibleVoters × 100, 2)` (0 when no eligible voters), determines the max-vote candidate(s) — multiple maxima ⇒ tie, `totalVotes == 0` or max `0` ⇒ no winner; audits `Results generated`. `previewResult(electionId) → ElectionResult` returns **identical numbers without an audit entry**, for live counters. Both share one private engine; neither ever returns `null`. |

`ElectionService` method contracts (all validated inputs are trimmed; every mutation is audited):

| Method | Validation / behaviour |
| :--- | :--- |
| `createElection(id,name,description,start,end)` | blank ID/name, null dates or `end <= start` → `InvalidElectionException`; duplicate ID (case-insensitive) rejected; created as `DRAFT`; persistence failure → exception |
| `openElection(id)` / `closeElection(id)` | missing election → exception; sets `OPEN` / `CLOSED`; persistence failure → exception |
| `deleteElection(id)` | blank or missing → exception; on success purges the election's candidates + ballots, resets affected voters, purges its audit history, then writes the `Election deleted & data purged` entry (so the deletion is still recorded); returns `boolean` |
| `resetDemoEnvironment()` | restores default admin, active E001 election, candidates, voters, clears ballots, and records audit event; returns `boolean` |
| `getElection(id)` | case-insensitive lookup on a trimmed ID, `null` when absent; `null` input → `null` |
| `getAllElections()` | all elections in file order |
| `getActiveOrFirstElection()` | first `OPEN` election, else the first election, else `null` (voter portal target) |
| `addCandidate(id,name,dept,electionId,slogan)` | all fields except slogan required; target election must exist; duplicate candidate ID rejected; persistence failure → exception |
| `getAllCandidates()` / `getCandidatesForElection(id)` | roster / election-scoped roster (empty list for null ID) |
| `addVoter(id,name,dept,username,password,eligible)` | all fields required; duplicate voter ID **or** username rejected; username must not collide with an **admin** username; registered with `hasVoted=false`; persistence failure → exception |
| `getAllVoters()` | full voter roll in file order |

### 3.5 `ui`

| Type | Constructor | Contract |
| :--- | :--- | :--- |
| `UITheme` | not instantiable in practice (static design system) | **Editorial Grid** design system: paper/ink/red tokens (`PAPER`, `PAPER_ALT`, `INK`, `ACCENT`, `MUTED`, `RULE`, `SUCCESS*`, `WARNING*`, `DANGER*`), serif display type (`DISPLAY_FONT_FAMILY`) plus sans UI type resolved from installed families, button factories (`createPrimaryButton`, `createDangerButton`, `createSuccessButton`, `createSecondaryButton`, `createStyledButton`, `createSidebarButton`, `createLinkButton`), custom controls (`EditorialRadioButton`, `EditorialCheckBox`, `EditorialComboBoxUI`, `EditorialScrollBarUI`), layout helpers (`createPage`, `addPageRow`, `createEditorialColumns`, `createSectionHeader`, `createPullQuote`, `createRule`), cards (`createCardPanel`, `StatCard` with live counting), badges, inputs, editorial tables, and motion (`RevealPanel`, animated `HorizontalVoteBar`) disabled by `votum.motion` / `votum.reduceMotion` |
| `DateTimeInput` | `DateTimeInput(String initialText)` | Validating entry field for `yyyy-MM-dd HH:mm` that paints its own valid/invalid border while typing. Display-free statics shared with the suite: `parse(String)`, `validateWindow(startText, endText)` (required, parseable, **End strictly after Start** — mirrors the service rule), `isEntirelyInThePast(start, end, now)`, `isEntirelyInTheFuture(start, end, now)`. Instance API: `parseDateTime()`, `setDateTime(LocalDateTime)`, `hasText()`, `isValidDateTime()`, `isValidState()`. |
| `LoginFrame` | `(FileManager, AuditLogger, ElectionService, VotingService, ResultService, AuthenticationService)` | brand header + credential card + quick-fill demo card, centred `GridBagLayout`, 16 ms entrance animation; `handleLogin()` routes `Admin → AdminDashboard`, `Voter → VoterDashboard`, shows `InvalidLoginException` messages inline and clears the password field. Keyboard-first: initial focus in Username, `Enter` submits from any field (root-pane default button), `Alt+L` login mnemonic, and labels are bound to their fields via `setLabelFor`. **First-run contract:** the credential card is sized to its contents (`syncLoginCardHeight()` caps `maximumSize` at the current preferred height) so the wrapped error banner never clips; the banner is dismissed by typing again (`DocumentListener`); a `Show`/`Hide` toggle switches the password's echo char; Caps Lock is named while the field has focus; `handleLogin` sets a busy state (`SIGNING IN…`, fields locked, wait cursor, double-submit refused) before the credential check runs; a static hint states `Press Enter to sign in` |
| `AdminDashboard` | `(Admin, FileManager, AuditLogger, ElectionService, VotingService, ResultService, AuthenticationService)` | sidebar `CardLayout` with 6 panels: `DASHBOARD`, `ELECTION`, `CANDIDATES`, `VOTERS`, `RESULTS`, `AUDIT`, every panel scroll-wrapped, plus a bottom status line that names the last action and its time. Managed background daemon worker `LiveRefreshWorker` (implements `Runnable`) polls metrics safely off the EDT every 15s. Top bar features deliberate confirmed `↺ Reset Demo Data` and `↻ Refresh` buttons. Holds three election selectors whose items are backed by parallel ID lists (selection resolved **by index** and preserved across refresh; a freshly created election wins the selection). Must expose: **create-election form** (ID auto-suggested, title, description, start/end date-time with quick-window presets, inline validation, DRAFT result, optional immediate open), open/close (both confirmed) / delete election, an election inventory line (`n configured · n open · n draft · n closed`), four **live** metric cards, candidate + voter registration forms with next-free-ID suggestions and filter boxes with visible row counts and empty states, live tally bars, official result dialog launcher, audit viewer (latest first) with refresh/clear, logout. **Guidance contract:** `refreshAttentionPanel()` rebuilds a *Needs Your Attention* card from live data on every refresh — an election with no candidates, or one still flagged `OPEN` whose window has elapsed — each row naming the consequence and carrying the button (`Add candidates` / `Close election`) that selects that election and navigates to the owning panel; when nothing is wrong it collapses to a single reassurance line. The overview status badge must agree with the clock: `OPEN` renders `● OPEN FOR VOTING` only while the window contains *now*, otherwise `▲ OPEN · WINDOW ENDED <elapsed>` or `▲ OPEN · STARTS <yyyy-MM-dd HH:mm>` |
| `VoterDashboard` | `(Voter, FileManager, AuditLogger, ElectionService, VotingService, ResultService, AuthenticationService)` | election info card (title, description, window, plus a human-readable **countdown**: "Voting opens in …", "Voting closes in …", "The voting window ended … ago") + status badge + candidate ballot cards (radio per candidate, whole-card click selects, red highlight). Ballot enablement uses a **single gate**: `eligible && !hasAlreadyVoted() && election.isCurrentlyActive(now)`, and `Submit` stays disabled until a candidate is actually selected. `refreshBallotState()` is the one place that decides what the ballot communicates: it re-derives the gate, sets the button's enabled state, and writes the inline hint (`Select a candidate card above to continue` → `Ready to submit: <name>. This is final and cannot be changed.` → `Your ballot for this election has already been recorded.`). Submission shows a confirmation dialog, then reports the vote ID + timestamp **and** renders `showBallotReceipt(...)`, a persistent success panel that survives scrolling; every radio is then disabled and the hint closes the loop. Failures say what to do next (a storage failure is reported as safely retryable). `Enter` submits via the root-pane default button; a null election, an election with no candidates, and an ineligible voter each get their own empty state |
| `ResultDialog` | `(Frame parent, ElectionResult)` | modal official result view: winner/tie banner, 3 stat cards (total votes, eligible voters, turnout), animated vote-distribution bars, close button (also the default button, with `Esc` bound to close) |

**Voter status badge state machine (priority order):**

| # | Condition | Text | Palette |
| :- | :--- | :--- | :--- |
| 1 | `!eligible` | `STATUS: INELIGIBLE` | danger |
| 2 | already voted in this election | `✓ VOTE SUBMITTED` | success |
| 3 | no election available | `■ NO ACTIVE ELECTION` | neutral |
| 4 | status ≠ `OPEN` | `■ ELECTION CLOSED` | neutral |
| 5 | `now < startTime` | `▲ VOTING OPENS <yyyy-MM-dd HH:mm>` | warning |
| 6 | `now > endTime` | `■ VOTING WINDOW ENDED` | danger |
| 7 | otherwise | `● READY TO VOTE` | crimson |

### 3.6 `Main` (composition root)

```text
UIManager.setLookAndFeel(system)                    # failure ignored → default Swing L&F
FileManager("data")  ──▶  AuditLogger("data/audit.log")
        │                        │
        ├── ElectionService(fileManager, auditLogger)
        ├── VotingService(fileManager, auditLogger, electionService)
        ├── ResultService(fileManager, auditLogger, electionService)
        └── AuthenticationService(fileManager, auditLogger)
auditLogger.log("Application started", "VOTUM Desktop GUI launched successfully")
SwingUtilities.invokeLater(LoginFrame(...).setVisible(true))
```

### 3.7 `test.VotumTestSuite`

`main` runs 24 independent scenarios against `data_test/` (wiped before **and** after the run),
counts passes/failures, prints a summary and exits with code `1` on any failure. Each test builds
a fresh `TestContext` (file manager, audit logger, four services) via `createIsolatedContext()`.

| # | Test | Asserts |
| :- | :--- | :--- |
| 1 | `test1_AdminLogin` | admin credentials return an `Admin` with `isAdmin()` |
| 2 | `test2_InvalidLogin` | wrong password → `InvalidLoginException` |
| 3 | `test3_VoterLogin` | voter credentials return a `Voter` with `isVoter()` |
| 4 | `test4_ElectionCreation` | valid creation is `DRAFT`; `end <= start` rejected |
| 5 | `test5_CandidateCreation` | candidate appears in its election roster |
| 6 | `test6_VoterCreation` | voter appears in the roll |
| 7 | `test7_ValidVote` | ballot stored with correct IDs, voter cache updated |
| 8 | `test8_DuplicateVoteRejection` | second ballot in the same election → `DuplicateVoteException` |
| 9 | `test9_InvalidCandidateRejection` | unknown candidate → `InvalidCandidateException` |
| 10 | `test10_VotingBeforeStartTime` | future window → `ElectionClosedException` |
| 11 | `test11_VotingAfterEndTime` | expired window → `ElectionClosedException` |
| 12 | `test12_VoteCounting` | per-candidate tallies match the ballots cast |
| 13 | `test13_WinnerCalculation` | single winner + turnout > 0 |
| 14 | `test14_TieCalculation` | equal maxima ⇒ `isTie()` with 2 winners |
| 15 | `test15_DataPersistenceAndReload` | a second `FileManager` sees the ballot and the voter cache |
| 16 | `test16_DeleteElection` | cascade: election, candidates, ballots removed, voter reset |
| 17 | `test17_PerElectionVoting` | one ballot per election **per voter**, duplicates still blocked per election |
| 18 | `test18_AdminUsernameCollision` | voter username equal to an admin username is rejected |
| 19 | `test19_CorruptRecordResilience` | malformed lines skipped, healthy records survive, `;` in free text cannot shift columns |
| 20 | `test20_AuditLogPurgePrecision` | purging `E001` keeps `E0010` history and records the deletion |
| 21 | `test21_VotingWindowBoundaries` | accepted exactly at start and end, rejected ±1 s outside, 2 ballots recorded |
| 22 | `test22_ElectionWindowValidation` | the create-election window rules (required, parseable, End > Start) plus past/future window classification — the same statics the admin form calls |
| 23 | `test23_CreateElectionLifecycle` | the exact call the form makes creates a `DRAFT`; duplicate ID rejected; then open → ballot → close → further ballots refused; tally and roster scoped |
| 24 | `test24_ResultPreviewDoesNotAudit` | `previewResult` returns identical numbers to `calculateResult` while leaving the audit trail untouched, and the official call does audit |

Helpers (private): `pass`, `fail`, `cleanTestDirectory`, `createIsolatedContext`, `TestContext`,
`appendLine(file,line)`, `containsToken(text,token)`.

---

## 4. Data Store Specification

| File | Columns (in order) | Notes |
| :--- | :--- | :--- |
| `data/users.txt` | `username;password;adminId;fullName;email` | ≤ 5 columns required |
| `data/voters.txt` | `voterId;name;department;username;password;eligible;hasVoted;votedElectionId` | ≥ 7 columns; column 8 optional for legacy files (`""` default) |
| `data/candidates.txt` | `candidateId;name;department;electionId;slogan` | ≥ 4 columns; slogan optional |
| `data/elections.txt` | `electionId;name;description;startTime;endTime;status` | dates ISO-8601 local date-time; status ∈ `DRAFT\|OPEN\|CLOSED`; all three must parse |
| `data/votes.txt` | `voteId;electionId;candidateId;voterId;timestamp` | ISO-8601 timestamp **required** (`VOTE-XXXXXXXX`, 8 uppercase chars) |
| `data/audit.log` | `yyyy-MM-dd HH:mm:ss \| EVENT \| DETAILS` | append-only; audit failures never break a business action |

**Write rules:** every free-text value passes through `sanitize()` which replaces `;` with `,`
and CR/LF with a space; `#`-prefixed lines and blank lines are comments; record repair happens on
the next write (malformed lines that were skipped on read are not re-emitted).

**Demo seed (fresh `data/` only):** admin `admin/admin123` (`ADM001`, Campus Election Officer),
election `E001` *Student Council President 2026* (`OPEN`, `now-1h → now+1d`), candidates `C001–C003`,
voters `V001–V004` eligible + `V005` ineligible, all with password `voter123`.

---

## 5. Business Rule Matrix

| # | Rule | Enforced in | Verified by |
| :- | :--- | :--- | :--- |
| 1 | Only registered, eligible voters may vote | `VotingService.castVoteAt` | 7, 21 |
| 2 | One vote **per voter per election** | `VotingService.castVoteAt` (cached flag + ballot records), `hasVoterCastVote` | 8, 17 |
| 3 | Voting only while status is `OPEN` | `VotingService.castVoteAt` | 10, 11, 21 |
| 4 | Voting before start time rejected | `Election.isBeforeStart` via `castVoteAt` | 10, 21 |
| 5 | Voting after end time rejected | `Election.isAfterEnd` via `castVoteAt` | 11, 21 |
| 6 | Candidate must exist for the target election | `castVoteAt` + `getCandidatesForElection` | 9 |
| 7 | Only administrators configure elections/participants | UI role routing (`Admin` → `AdminDashboard`) | 1, 3 |
| 8 | Closed elections never accept ballots | `castVoteAt` (`CLOSED` branch) | 11 |

Window semantics: bounds are **inclusive** — a ballot at exactly `startTime` or exactly `endTime`
is accepted; ±1 second outside is rejected.

---

## 6. Bug Register (found and removed)

| ID | Severity | Symptom | Root cause | Fix | Verified by |
| :--- | :--- | :--- | :--- | :--- | :--- |
| B-01 | **Critical** | A voter who voted in one election could never vote in any other election; the portal greyed out the entire ballot | Duplicate detection used the global `Voter.hasVoted` flag and `hasVoterCastVote` ignored the `electionId` argument | Scope every duplicate check to `(voterId, electionId)`, using ballot records as the authoritative source | Test 17 |
| B-02 | **Critical** | A failed ballot write still marked the voter as having voted → vote lost **and** voter permanently blocked | Writers swallowed `IOException`; `VotingService` mutated the voter cache without checking that the ballot was persisted | Writers return `boolean`; the ballot is persisted **before** the cache is updated, and the cached update is audited when it fails | Tests 7, 15 (+ code review) |
| B-03 | High | A `;` inside a name/slogan shifted record columns and silently corrupted stored rows | Values were written verbatim into a `;`-delimited file | `sanitize()` on every write (delimiter + CR/LF), documented as the storage contract | Test 19 |
| B-04 | High | One corrupt line hid **every** record after it; the next write then erased them permanently | `ElectionStatus.valueOf` threw inside the file loop and a broad `catch (Exception)` abandoned the whole file | Per-record validation with skip-and-warn; election/ballot records need parseable dates and status; broad catch replaced with `IOException` | Test 19 |
| B-05 | High | A student registered with the username `admin` could never log in (the admin record shadows them) | `addVoter` checked voter-to-voter username collisions only | `addVoter` also rejects usernames already held by an administrator | Test 18 |
| B-06 | Medium | Deleting election `E001` erased unrelated history such as `E0010` | Purge used `String.contains` (substring match) | Whole-token, case-insensitive matching (`[A-Za-z0-9_-]+`, hyphens preserved) | Test 20 |
| B-07 | Medium | Portal showed `● READY TO VOTE` and enabled the ballot while the election was still `CLOSED`/before start/after end; `Election.isCurrentlyActive` was dead code | The UI checked status and eligibility only, never the time window | Single `canVote()` gate (`isCurrentlyActive`) + 7-state badge machine | Test 21 |
| B-08 | Medium | Every refresh reset the admin's election selection; a title containing `[`/`]` produced a wrong election ID | Selectors were cleared/rebuilt and IDs were parsed out of the display string | Parallel ID lists, index-based resolution, selection restored by ID | UI smoke + code review |
| B-09 | Medium | Suggested participant IDs collided after a deletion (`C003` suggested twice) and mis-formatted beyond 9 records (`C0010`) | `"C00" + (size + 1)` | `suggestNextId(prefix, ids)` → highest numeric suffix + 1, `%03d` | UI smoke + code review |
| B-10 | Low | The documented "Official result dialog" could never be opened (class never instantiated) | Dead component | Results panel button opens `ResultDialog` for the selected election | UI smoke |
| B-11 | Low | `ElectionResult` leaked its mutable internal winner list; null lookups were unsafe | Encapsulation gap | Unmodifiable copies, defensive setter, null-safe lookups | Test 14 (+ review) |
| B-12 | Low | Persistence failures were invisible to callers (only `stderr`) | `void` writers | `boolean` returns + service-level failure exceptions | Compile-level contract + 19 |
| B-13 | Low | `ui` read storage directly, contradicting the documented separation of concerns | Missing service accessor | `ElectionService.getAllCandidates()`; UI now uses services only | `grep` verification |
| B-14 | Low | 47 `javac -Xlint:all` warnings (missing `serialVersionUID`, non-final leaf frames, unused resource in `try`, constructor `this`-escape) | Hygiene | `serialVersionUID` on exceptions/frames/nested panel, `final` leaf frames, explicit `flush()` on log truncation, private demo seeder → **27 remaining, all documented** (§11) | Lint run |
| B-15 | Low | README claimed a 15-point suite, linked to `/Users/hitesh_pundhir25/...` absolute paths, and gave a `javac` command that fails in `bash` | Stale docs | README corrected to 24 tests, relative links, portable build command | Docs review |
| B-16 | **High** | All four dashboard metric cards displayed `0` forever — Eligible Voters, Candidates, Votes Cast and Turnout were built once with hard-coded zeros and never rebound, so the overview contradicted the rest of the app | Metric cards were plain panels returned by `createStatCard` with no way to update their labels | New `UITheme.StatCard` with `setValue`/`setSubtitle`; `refreshDashboardMetrics()` rebinds all four from `getAllVoters`, `getCandidatesForElection` and the tally on every refresh | Smoke: 4 cards bound, then 0 → 1 vote and 0.00% → 25.00% after a ballot |
| B-17 | Medium | At the minimum window size the Election panel (and other tall panels) was clipped with no way to scroll to the hidden controls | Content panels were added directly to the `CardLayout` container | `wrapContentScroll()` wraps every content panel in a border-less scroll pane with a 16 px unit increment | Smoke: all panels remain reachable |
| B-18 | Medium | The whole type scale silently fell back to a platform default on any machine without **Segoe UI** (macOS/Linux), so the design rendered differently than specified | All fonts were hard-coded to the `"Segoe UI"` family name, which Java resolves only if installed | `UI_FONT_FAMILY` probes `GraphicsEnvironment.getAvailableFontFamilyNames()` against a candidate list (Inter → Segoe UI Variable → Segoe UI → SF Pro Text → Helvetica Neue → …) and falls back to the logical sans-serif font | Compile + smoke (fonts resolve on macOS) |
| B-19 | Medium | Open / Close election executed immediately with a single click — an accidental click could start or end campus voting — and clicking Open on an already-open election produced a pointless DB write | Only Delete had a confirmation dialog | Both actions now guard the already-open / already-closed case and require an explicit confirmation that restates the voting window | Smoke: confirmed open path reported in the inventory line |
| B-20 | Low | Live counters and passive panel refreshes wrote a `Results generated` audit entry each time, so the audit trail filled with duplicate tally rows and hid the real events | The only tally entry point always audited | Split the engine: `calculateResult` (audited, used for official views) and `previewResult` (silent, used by dashboard counters) | Test 24 |
| B-21 | Low | Rosters could not be searched, gave no indication of how many rows were shown, and rendered an empty grid when nothing matched | No sorting/filtering, no count, no empty state | `TableRowSorter` filter applied on every keystroke, a live `n rows` / `n of m` count, and explicit empty-state messages that differ for "no data" vs "no filter match" | Smoke: 3 → 1 → 3 rows around a filter |
| B-22 | Low | Keyboard and assistive-technology affordances were missing: `Enter` only worked inside the password field, dialogs had no default button, `Esc` did nothing, labels were not associated with their fields | No default-button, key-binding or `setLabelFor` wiring | Root-pane default button + initial focus on login, `Alt+L` mnemonic, `Esc` closes the result dialog, `label.setLabelFor(field)` on every form field, tooltips on demo-credential buttons | Smoke: default button present, labels resolve to their fields |
| B-23 | Low | Audit entries read `Winner: Winner: Arjun Sharma (1 votes)` — the tally line prefixed the outcome with a label that the outcome already carried | `" | Winner: " + getWinnerSummary()`, and the summary itself starts with `Winner: ` (or `TIE between: ` / `No votes recorded yet.`) | Append the summary as-is, so one line reads `… \| Total Votes: 1 \| Winner: Arjun Sharma (1 votes)` | Manual audit-log inspection (the defect was visible in a real session log) |
| B-24 | **High** | The admin dashboard read `● OPEN FOR VOTING` — and counted the election as `open` in the inventory line — for an election whose voting window had already elapsed, while the voter portal for the same record said `■ VOTING WINDOW ENDED` and refused the ballot. The two halves of the app contradicted each other about whether voting was live | The badge rendered the *stored* `ElectionStatus` only; `Election.isCurrentlyActive(now)` was consulted by the voter portal alone | The badge derives from the clock as well as the flag: `● OPEN FOR VOTING` only inside the window, otherwise `▲ OPEN · WINDOW ENDED <elapsed>` / `▲ OPEN · STARTS <...>`, and the *Needs Your Attention* row offers `Close election` | Polish smoke: elapsed election renders `▲ OPEN · WINDOW ENDED 4d ago` and a fix button; audit probe item flipped from fail to pass |
| B-25 | **High** | The error shown after a rejected sign-in disappeared immediately, because `handleLogin` cleared the password field *after* displaying the message — and the field's own change listener dismissed the banner. The user saw a blank card and no reason for the failure | Ordering: `showLoginError(...)` then `passwordField.setText("")` | Clear the rejected password first, then show the message | Polish smoke: banner renders 31 px of 26 px needed (2 wrapped lines), nothing overflows the card |
| B-26 | Medium | `Submit Official Ballot` was enabled before any candidate was chosen, so the primary action on the screen was a dead end that produced a modal telling the voter what they had not done. Acknowledgment of a recorded ballot existed only in a modal, leaving no on-screen evidence afterwards | Enablement keyed on the gate alone, and success reported only via `JOptionPane` | Selection-gated enablement + one `refreshBallotState()` that drives button and hint together + a persistent `BALLOT RECORDED` receipt panel (vote ID, candidate, timestamp) | Polish smoke: disabled → `Ready to submit: Priya Mehta…` → real submission → receipt visible with the vote ID |
| B-27 | Low | The credential card was pinned to a fixed height, so a wrapped two-line error banner had no room and would clip under a different font metric or longer message | `setPreferredSize`/`setMaximumSize` both fixed at 310 px | Card sized to its content (`syncLoginCardHeight()` re-caps `maximumSize` at the current preferred height whenever the banner toggles) | Polish smoke: error rendered in full, button bottom 261 px inside a 300 px card |

---

## 7. Refinement Log (per iteration)

### Iteration 1 — Storage & service correctness  (`FileManager`, `AuditLogger`, `ElectionResult`, `ElectionService`, `VotingService`)
Removed B-01…B-06, B-11, B-12. Introduced the resilience contract: per-record skip-and-warn,
delimiter sanitization, boolean write results, ballot-before-flag ordering, token-scoped purge,
unmodifiable result collections.
**Verification:** `javac` clean; suite **16/16 PASS** (existing behaviour preserved, no regression).

### Iteration 2 — UI correctness & reachability  (`VoterDashboard`, `AdminDashboard`)
Removed B-07, B-08, B-09, B-10 and the repeated per-card storage reads (ballot enablement is now
resolved once per render, then passed down). Added the frame `serialVersionUID`/`final` hygiene.
**Verification:** `javac` clean; suite **16/16 PASS**; throwaway headful smoke harness constructed
all four windows on the EDT (`constructed: LoginFrame, AdminDashboard, VoterDashboard, ResultDialog → UI SMOKE OK`), then deleted.

### Iteration 3 — Regression coverage  (`VotumTestSuite`)
Added tests 17–21, one per class of fixed defect (multi-election voting, username collision,
corrupt-store resilience + delimiter safety, audit purge precision, inclusive window boundaries),
plus the `appendLine` / `containsToken` helpers.
**Verification:** suite **21/21 PASS**, exit code 0.

### Iteration 4 — Layering, hygiene & documentation  (`AdminDashboard`, `ElectionService`, exceptions, `UITheme`, `README.md`)
Removed B-13, B-14, B-15. UI reads now go through services; lint warnings 47 → 27; README rewritten
to match measured behaviour.
**Verification:** `javac -Xlint:all` → 0 errors; suite **21/21 PASS**;
`grep` confirms no UI→storage reads.

### Iteration 5 — UI/UX defect pass  (`UITheme`, `AdminDashboard`)
Removed B-16, B-17, B-18: metric cards rebound to live data, every content panel scroll-wrapped,
platform font probe. The four dead counter cards were the most visible symptom of a UI that
contradicted its own data.
**Verification:** `javac` clean · suite **21/21** · headful smoke bound the cards (`4` eligible,
`3` candidates, `0.00%` turnout) and then observed them rebind after a real ballot (`1`, `25.00%`).

### Iteration 6 — Administrator create-election  (`AdminDashboard`, new `ui/DateTimeInput`)
Delivered the requested capability by wiring the new form to the **existing**
`ElectionService.createElection` contract: auto-suggested ID, description, validated `yyyy-MM-dd HH:mm`
window with quick presets, inline errors, DRAFT result, optional immediate open, and selection of the
new election.
**Verification:** tests 22–23 (window validation + full lifecycle) · smoke drove the real form:
missing title refused, reversed window refused, valid window persisted as DRAFT and auto-selected.

### Iteration 7 — Interaction polish  (`AdminDashboard`, `VoterDashboard`, `LoginFrame`, `ResultDialog`, `ResultService`)
Removed B-19, B-20, B-21, B-22: confirmations for Open/Close, non-auditing `previewResult` for
counters, roster filters with counts and empty states, keyboard/accessibility wiring, and a voter-side
window countdown.
**Verification:** tests 24 · smoke 30/30 (filters, inventory line, status line, badge priority,
countdown, default button).

### Iteration 9 — Experience pass  (`LoginFrame`, `VoterDashboard`, `AdminDashboard`)
Walked the first-run experience end to end and made the interface acknowledge its own actions and
say what to do next: content-sized login card with a wrapped, self-dismissing error banner (B-27,
B-25), busy state on sign-in, password reveal toggle, Caps Lock hint, Enter hint; selection-gated
ballot with one source of truth for button + hint, persistent ballot receipt, actionable empty
states, and `Enter` to submit (B-26); a clock-aware admin status badge (B-24) and a live *Needs Your
Attention* card whose rows navigate straight to the fix.
**Verification:** compile clean · suite **24/24** · polish smoke **13/13** driving the real windows
(the vote was cast through the real UI with the confirmation dialog answered programmatically) ·
audit probe 10/14 (remaining 4 are the documented algorithmic/scope items of §12 of Part II §6).

### Iteration 8 — Re-review, docs and report
Re-read every diff for regressions, re-ran the full protocol, inspected a real session's audit log and
produced Part II below. That log inspection surfaced B-23 (a duplicated `Winner:` label in tally
entries), which was fixed and re-verified.
**Verification:** compile clean · lint **29 documented warnings, 0 errors** · suite **24/24** ·
smoke **30/30** · layering grep empty.

---

## 8. Verification Protocol (reproduce the evidence)

```bash
# 1. Compile (no warnings beyond the documented set)
javac -d bin $(find src -name "*.java")

# 2. Lint audit — expect 0 errors, 27 [serial] + 2 [this-escape] warnings
javac -Xlint:all -d /tmp/votum-lint $(find src -name "*.java")

# 3. Full behaviour suite — expect "24 Passed, 0 Failed" and exit code 0
java -cp bin com.votum.test.VotumTestSuite

# 4. Architecture invariant — expect no output
grep -rn "fileManager.load\|new File(" src/com/votum/ui/

# 5. Manual UI acceptance (from the project root so data/ resolves)
java -cp bin com.votum.Main        # admin/admin123 · voter1/voter123
```

`data_test/` is removed on both ends of the suite run, so a failed test never leaves stray state.

---

## 9. Accepted Behaviour (deliberate, not a defect)

1. **Plain-text credentials** — required by the coursework brief (no external libraries, no hashing framework). Documented in README §4.
2. **Official tallies are audited, passive ones are not** — opening the results panel or the result dialog records a `Results generated` entry; the dashboard's live counters use `previewResult` and stay silent, so the trail reflects decisions rather than window paint events.
3. **`FileManager` seeds demo data whenever a store file is missing/empty** — makes first-run and recovery deterministic; deleting `data/` restores the documented demo state.
4. **Relative `data/` directory** — the process working directory defines the store; always launch from the project root (both `.sh`/`.bat` scripts do).
5. **Voter-roll `Status` column shows `VOTED (E002)`** — a single cached flag cannot represent ballots in several elections, so the election ID is displayed explicitly.

---

## 10. Verification Status Summary

| Check | Result |
| :--- | :--- |
| Compile (JDK 26) | clean, 0 errors |
| `-Xlint:all` | 29 warnings, all documented (§11), 0 errors |
| Automated suite | **24 / 24 PASS** |
| UI smoke (headful, driven) | **30 / 30 checks**: 5 windows construct, metric binding, filters, create-election flow, confirmations, badge priority, countdown |
| Experience smoke (headful, driven) | **13 / 13 checks**: banner sizing and dismissal, password reveal, busy state, selection-gated ballot, receipt after a real submission, clock-aware admin badge, actionable guidance appearing and clearing |
| Architecture invariant | UI → service only (no `fileManager.load*` / `new File(` under `ui/`) |
| Feature parity vs README §2 | complete: 9 admin capabilities (incl. election creation), 5 voter capabilities, 8 rules, 24 verification checks |
| External dependencies | none (JDK only) |

---

## 11. Residual Warnings (accepted, with reason)

| Warning | Count | Why accepted |
| :--- | :--- | :--- |
| `[serial] non-transient instance field of a serializable class declared with a non-serializable type` | 27 | `JFrame`/`JDialog` fields holding service references. The application never serializes windows; marking those fields `transient` would introduce a real `null`-after-deserialization defect instead of a theoretical one. |
| `[this-escape]` in `FileManager` | 2 | The constructor calls the (now private) demo seeder, which calls the `synchronized` public `saveAdmins`. Making the class `final` would silence it but would block the documented v2 subclassing path (SQLite storage); the hazard is unreachable in this codebase. |

---

## 12. Scope Guard (Do-Not-List)

1. **No new features** beyond §1's in-scope list; every change above repairs existing behaviour.
2. **No omitted features** — all 27 types, 16 → 21 checks and every README §2 capability remain.
3. **No external dependencies** — JDK only (`javax.swing`, `java.time`, `java.util`, `java.io`).
4. **No layer inversions** — `model` stays I/O-free, `storage` stays rule-free, `service` stays UI-free, `ui` stays file-free.
5. **No silent data loss** — corrupt input is skipped loudly; valid data is never discarded; a failed write must be reported to the caller.
6. **No `transient` service fields, no widened access** to satisfy linters.

---

## 13. Extension Points (documented roadmap, not implemented)

| Roadmap item | Clean insertion point |
| :--- | :--- |
| v2 SQLite/MySQL via JDBC | Subclass/replace `FileManager` (`load*`/`save*` contract is already abstract-friendly) |
| v3 Web/mobile portal | Reuse `service` + `model` unchanged; replace `ui` |
| v4 College SSO/OTP | Add an authentication strategy behind `AuthenticationService.login` |
| v5 SHA-256 ballot chaining | Extend `Vote` with a `previousHash` field and compute it inside `VotingService.castVoteAt` before `addVote` |
| v6 Department-scoped elections | Filter in `ElectionService.getCandidatesForElection` + a rule branch in `castVoteAt` |

---

# Part II — Design & Analysis Report

*Requested deliverable: UI/UX improvement, admin election creation, full-system analysis — bullet points and diagrams, ≤ 2 pages, produced through an iterative loop rather than one pass.*

## 1. Method and scope

- **Method:** audit → prioritise (P0 defects / P1 feature / P2 polish / P3 experience) → implement one iteration at a time → verify each cycle (`javac`, `-Xlint:all`, 24-test suite, 30-check headful smoke, 13-check experience smoke) → re-read the diff → next cycle. Correctness claims were re-derived by *probing the running application* (a headful harness that drives the real windows and asserts on rendered results), not by re-reading the code.
- **Scope:** presentation and interaction only. No stored format, model field, service signature or business rule was altered; every new control calls a contract that already existed and was already tested.
- **Analysis basis:** all 28 classes read line by line, 8 business rules traced to their enforcement points, 5 data stores verified against their loaders, 27 defects logged, 14 adversarial probes run against the live UI.

## 2. System analysis in brief

```text
LAYERS      ui (6)  ──▶  service (4)  ──▶  storage (2)  ──▶  model (8)      + exception (6), test (1)
RULES       8 business rules, all enforced in the service layer, all covered by the suite
DATA        5 delimiter-separated text stores + append-only audit log; formats frozen
QUALITY     24/24 automated checks · 0 compile errors · 29 documented lint warnings · 0 dependencies
```

- **Strengths:** clean layer separation (UI reaches no file APIs — grep-verified), a single authoritative rule layer, deterministic demo seeding, and a suite that now covers error paths, corrupt storage and boundaries rather than only happy paths.
- **Risks found:** (1) a plain-text, non-transactional store — mitigated by write-then-verify and skip-and-warn reads; (2) plain-text credentials, mandated by the coursework brief; (3) audit growth — now bounded because passive tallies no longer log; (4) repeated file reads on refresh — acceptable at campus scale (< 10³ records) and hidden behind a status line.
- **UI/UX diagnosis:** the interface was *structurally* sound but *informational* failings dominated — it displayed zeros where data existed, hid controls at small window sizes, depended on one platform font, let destructive actions fire on a single click, and gave no search or feedback.

## 3. UI/UX changes (audit → outcome)

| Priority | Finding | Outcome |
| :--- | :--- | :--- |
| P0 | Four metric cards permanently read `0` | `StatCard` + live rebinding: eligible voters, candidates, ballots and turnout now reflect the selected election |
| P0 | Tall panels clipped at minimum size | All content panels scroll-wrapped |
| P0 | Type scale fell back wherever `Segoe UI` was absent | JDK font-family probe with a candidate list |
| P0 | Empty rosters showed a blank grid | Distinct empty states for "no data" vs "no filter match" |
| P1 | No way to create an election from the UI | Create-election form (§4) |
| P2 | Open/Close fired on one click | Confirmations naming the election and its window, plus already-open/closed guards |
| P2 | Rosters unsorted, unsearchable, uncounted | Per-keystroke filter, live `n rows` / `n of m` count |
| P2 | No feedback after actions | Status line naming the last action + timestamp; election inventory line |
| P2 | Keyboard/accessibility gaps | Default button, initial focus, `Alt+L`, `Esc` to close, `labelFor` on every field, tooltips |
| P2 | Voters could not tell how long remained | Countdown line: "Voting closes in 23h 40m" |
| P3 | The admin badge claimed `OPEN FOR VOTING` for an election whose window had elapsed, contradicting the voter portal | Clock-aware badge (§3.5) + a *Needs Your Attention* row offering `Close election` |
| P3 | A rejected sign-in showed no error at all (the password reset dismissed the banner) | Banner shown after the field is cleared, wrapped and content-sized (B-25, B-27) |
| P3 | The primary ballot action was enabled before anything was selected, and success left no trace | Selection gates `Submit`; one `refreshBallotState()` drives button + hint; a persistent `BALLOT RECORDED` receipt |
| P3 | Administrators had to hunt for what needed doing | Live *Needs Your Attention* card, each row naming the consequence and navigating to the fix |

## 4. Feature specification — Admin: Create Election

```text
[Create New Election]  ID (auto E00n) · Title · Description · Starts · Ends · quick windows
        │
        ├─ inline validation (mirrors the service): required ID/title/dates · pattern yyyy-MM-dd HH:mm
        │                                          · End strictly after Start · duplicate ID
        ▼
  ElectionService.createElection(...)   ← existing, unchanged, re-validates everything
        │
        ▼  status = DRAFT, audit entry written
  refresh ─▶ new election auto-selected · inventory line updates · status line confirms
        │
        └─ "Open the voting window now?" ─▶ ElectionService.openElection(...)   (existing)
```

- **Inputs:** Election ID (auto-suggested as the smallest unused `E###`), Title, Description (optional), **Voting Starts**, **Voting Ends** — both `yyyy-MM-dd HH:mm` with a live valid/invalid border — plus quick-window presets (opens now → +1 day; today 09:00 → 18:00; clear).
- **Rules (client mirrors, service enforces):** ID and title required; dates required and parseable; **End strictly after Start**; duplicate ID refused including case differences.
- **Behaviour:** created as `DRAFT` (the service contract) → audit-logged → automatically selected in all three selectors → optional one-click open → a window entirely in the past is accepted but explicitly warned about.
- **Edge cases covered:** duplicate ID, `start == end`, reversed window, unparseable or blank dates, past window, blank title, first-ever election (empty state with a call to action), several simultaneous elections.
- **Compatibility:** adds `ui/DateTimeInput` and one non-auditing `ResultService.previewResult` method; no model, storage-format or existing signature changes.

## 5. Iteration evidence

| Cycle | Focus | Evidence |
| :--- | :--- | :--- |
| 5 | P0 UI defects | compile clean · 21/21 · smoke: cards bound `4 / 3 / 0 / 0.00%`, then `1 / 25.00%` after a ballot |
| 6 | Create election | 22–23 pass · smoke: title refusal, window refusal, DRAFT persisted + auto-selected |
| 7 | Interaction polish | 24 passes · smoke 30/30 (filters, confirmations, status line, countdown, badge priority) |
| 8 | Re-review + report | 24/24 · smoke 30/30 · lint 0 errors |
| 9 | Experience pass | 24/24 · **experience smoke 13/13** (banner sizing, password reveal, busy state, selection-gated ballot, real submission + receipt, clock-aware badge, guidance appearing and clearing) |

## 6. Recommendation and next steps

- **Adopt as-is:** the loop is closed — the UI no longer contradicts the data it displays, election creation is available without touching the rule layer, and the interface now names its own state and the next action.
- **Open items found by probing, deliberately not changed here** (they are algorithm/storage decisions, not presentation, so fixing them inside a UI pass would have hidden a rule change in a polish diff):
  1. **Turnout can read 400 %** — the numerator is historical ballots while the denominator is the *current* eligible roll, so marking voters ineligible after they have voted inflates it. Fix by dividing by an eligibility snapshot (or clamping with a stated reason).
  2. **The distribution can fail to sum** — a ballot whose candidate is no longer registered still counts in `totalVotes` but has no bar. Reconcile as `totalVotes == Σ tallies + unattributed`.
  3. **Store writes are not atomic** — 8 `new FileWriter(...)` call sites truncate in place; a crash mid-`saveVotes` can truncate the ballot book. Write to `*.tmp` then `Files.move(..., ATOMIC_MOVE)`.
  4. **The portal still targets the first `OPEN` election** — with two open elections a voter can never choose.
- **Next, cheaper first:** (1) tick the voter countdown on a timer instead of a single render; (2) per-election turnout column in the voter roll; (3) commit the headful harness into `test/` behind an `isHeadless()` skip so the UI layer stops being unguarded.
- **Deferred:** the earlier "capture the application's own stdout/stderr into a buffer with a `data/console.log` copy and a viewer dialog" item was dropped from this plan on request; it remains a self-contained follow-up.

