# VOTUM 2.0 — Comprehensive Project Audit & Defect Analysis

**Course:** B.Tech CSE — Object Oriented Programming (CSC2104)  
**System:** VOTUM — Digital Campus Election Management System  
**Audit Date:** 2026-10-29  
**Status:** COMPLETE AUDIT (Baseline Verification & Defect Catalog)

---

## 1. Executive Summary

This document presents the complete architectural, behavioural, and code-level audit of the **VOTUM** digital campus election management system. The audit evaluates the current implementation against:
1. The **CSC2104 Course Handout** (Manipal University Jaipur, Department of Computer Science & Engineering).
2. The **MASTER.md** architectural and layer blueprint.
3. The **README.md** functional specifications and user contracts.
4. The verified runtime source code in `src/com/votum/`.

The system is a pure Java Swing desktop application requiring JDK 17+ with zero external runtime dependencies. It replaces manual paper ballots with a multi-role, verified digital election workflow.

---

## 2. Existing System Inventory

### 2.1 File & Layer Architecture
The existing project structure consists of 29 Java source files across 6 packages:
- **Root Entry Point:** `src/com/votum/Main.java`
- **Model Layer (`src/com/votum/model/`):**
  - `User.java`: Base user with username, password, role (`ADMIN`/`VOTER`).
  - `Admin.java`: Extends `User`, adds `adminId`, `fullName`, `email`.
  - `Voter.java`: Extends `User`, adds `voterId`, `name`, `department`, `eligible`, `hasVoted`, `votedElectionId`.
  - `Candidate.java`: Contesting student profile with `candidateId`, `name`, `department`, `electionId`, `slogan`.
  - `Election.java`: Campus election with start/end `LocalDateTime`, `ElectionStatus`.
  - `ElectionStatus.java`: Strong typing enum (`DRAFT`, `OPEN`, `CLOSED`).
  - `Vote.java`: Immutable-by-convention ballot record with `UUID`-based `voteId`, timestamp.
  - `ElectionResult.java`: Tally container, turnout percentage calculation, tie and winner calculation.
- **Exception Layer (`src/com/votum/exception/`):**
  - `VotumException.java`: Root checked application exception.
  - `InvalidLoginException.java`: Blank credentials or invalid username/password.
  - `DuplicateVoteException.java`: Multi-vote prevention per voter per election.
  - `ElectionClosedException.java`: Out-of-window, DRAFT, or CLOSED election vote rejection.
  - `InvalidCandidateException.java`: Unregistered or election-mismatched candidate.
  - `InvalidElectionException.java`: Validation and persistence failures for elections/participants.
- **Storage Layer (`src/com/votum/storage/`):**
  - `FileManager.java`: Delimiter-separated plain-text file I/O, fault-tolerant reader, data sanitization, cascade election deletion, demo data seeder.
  - `AuditLogger.java`: Append-only timestamped audit log (`audit.log`), election log purge, log clearing.
- **Service Layer (`src/com/votum/service/`):**
  - `AuthenticationService.java`: Credentials validation, role-based polymorphic user resolution, audit emission.
  - `ElectionService.java`: Election lifecycle, candidate and voter registration, cascade cleanup coordination.
  - `VotingService.java`: Core 8-rule enforcement engine, inclusive time-window verification, duplicate ballot checking.
  - `ResultService.java`: Vote aggregation, turnout %, tie detection, non-auditing preview tally vs. official certified results.
- **UI Presentation Layer (`src/com/votum/ui/`):**
  - `UITheme.java`: Centralized **Editorial Grid** design system — paper/ink/red palette, serif display type, `EditorialButton`, `RevealPanel`, `HorizontalVoteBar`, `StatCard`, `EditorialRadioButton`, `EditorialCheckBox`, `EditorialComboBoxUI`, editorial table and scrollbar styling.
  - `DateTimeInput.java`: Validating `yyyy-MM-dd HH:mm` text component with live border feedback and static validation utilities.
  - `LoginFrame.java`: Civic branding, 38px inputs, Caps Lock indicator, password toggle, 3-button quick fill.
  - `AdminDashboard.java`: 6-section sidebar control center (Overview, Elections, Candidates, Voters, Results, Audit Trail).
  - `VoterDashboard.java`: Student portal, countdown timer, candidate ballot cards, single-vote gate, confirmation dialog, scroll-proof persistent ballot receipt.
  - `ResultDialog.java`: Official election outcome modal dialog.
- **Test Layer (`src/com/votum/test/`):**
  - `VotumTestSuite.java`: 24-point domain and business-rule verification suite.
  - `UISmokeTestSuite.java`: 27-point UI, interaction, and component smoke suite.

---

## 3. Business Rules & Operational Invariants

The application enforces 8 fundamental election business rules:
1. **Rule 1 — Registered & Eligible Only:** Only registered voters marked `eligible == true` can cast ballots.
2. **Rule 2 — Single Vote Per Election:** Each voter can cast at most one ballot per election. Stored ballots in `votes.txt` serve as the authoritative truth.
3. **Rule 3 — Election Must Be OPEN:** Voting is rejected if election status is `DRAFT` or `CLOSED`.
4. **Rule 4 — Start Time Boundary:** Voting before `startTime` is rejected (`ElectionClosedException`).
5. **Rule 5 — End Time Boundary:** Voting after `endTime` is rejected (`ElectionClosedException`). Inclusive boundary contract: voting at exact second of `startTime` and `endTime` is valid.
6. **Rule 6 — Candidate-Election Association:** Candidate must exist and be registered for the target election.
7. **Rule 7 — Admin-Only Governance:** Only authenticated `Admin` accounts can configure elections, candidates, and voter rolls.
8. **Rule 8 — Closed Elections Immutable:** Closed elections permanently refuse new ballots.

---

## 4. Storage Formats & Guarantees

All persistent data is stored in delimiter-separated UTF-8 plain-text files in `data/`:
- `data/users.txt`: `username;password;adminId;fullName;email`
- `data/voters.txt`: `voterId;name;department;username;password;eligible;hasVoted;votedElectionId`
- `data/candidates.txt`: `candidateId;name;department;electionId;slogan`
- `data/elections.txt`: `electionId;name;description;startTime;endTime;status`
- `data/votes.txt`: `voteId;electionId;candidateId;voterId;timestamp`
- `data/audit.log`: `yyyy-MM-dd HH:mm:ss | EVENT | DETAILS`

**Storage Guarantees:**
- **Delimiter Sanitization:** Any semicolon (`;`) in user input is converted to a comma (`,`), and newlines/returns are replaced with spaces to avoid column-shifting.
- **Fault-Tolerant Reads:** Malformed rows are skipped individually with warnings; healthy rows continue to load.
- **Ballot Before Flag:** Ballot records are persisted to disk *prior* to updating in-memory voter flags.
- **Token-Scoped Audit Purge:** Deleting election `E001` purges only whole-token occurrences of `E001`, leaving `E0010` untouched.

---

## 5. Confirmed Defects & Deficiencies (P0–P3)

### Priority P0 (Critical Correctness / Academic Requirement Failures)
1. **[P0-1] Missing Explicit Demo Reset Workflow:**
   - *Issue:* While `FileManager` automatically seeds demo records if `data/` files are empty, there is no deliberate, user-confirmed "Reset Demo Environment" button or service method.
   - *Impact:* Evaluators cannot cleanly restore demo credentials and baseline records after running live test votes without manually deleting files on disk.
   - *Remedy:* Implement `FileManager.resetToDemoData()`, wire it to an intentional, confirmed UI button in `AdminDashboard`, and record the event in `audit.log`.
2. **[P0-2] Zero Interfaces in the Entire Codebase:**
   - *Issue:* Course Outcome CSC2104.3 explicitly requires: *"Demonstrate the use of inheritance, polymorphism, and interfaces."* The existing codebase has 0 interfaces.
   - *Impact:* Direct failure of course syllabus alignment and viva vulnerability.
   - *Remedy:* Introduce meaningful, non-bloated interfaces (`Identifiable`, `AuthenticatableUser`) that define polymorphic contracts in `model` and are utilized in `service` and `storage`.

### Priority P1 (Architecture & Syllabus Gaps)
1. **[P1-1] `User` Class is Concrete Rather Than Abstract:**
   - *Issue:* In VOTUM, all users are either `Admin` or `Voter`. No standalone `User` exists. Yet `User` is currently concrete, missing the opportunity to demonstrate `abstract class` and `abstract` method contracts (CSC2104.3).
   - *Impact:* Misses syllabus demonstration of abstract classes and dynamic method dispatch.
   - *Remedy:* Declare `public abstract class User implements AuthenticatableUser, Identifiable` and introduce abstract methods `getDisplayName()` and `getId()`.
2. **[P1-2] Absence of Managed Background Multithreading:**
   - *Issue:* The UI executes all file reads and writes directly on the Swing Event Dispatch Thread (EDT). No background thread or `Runnable` is used for periodic live updates or long operations.
   - *Impact:* Violates CSC2104.4 multithreading requirement; risks UI freezing during I/O.
   - *Remedy:* Implement a controlled, daemon background refresh worker (`LiveRefreshWorker` implementing `Runnable`) in `AdminDashboard` using cooperative thread termination (`volatile boolean running`) and safe `SwingUtilities.invokeLater` updates.

### Priority P2 (UI/UX & Operational Usability Defects)
1. **[P2-1] Expired Demo Election in Shipped Data:**
   - *Issue:* In `data/elections.txt`, the pre-seeded election `E001` had an end date in September 2026. Anyone logging in with demo credentials today encounters an expired voting window and is blocked from casting a test ballot.
   - *Impact:* First-time users trying out the student voter portal immediately encounter an error dialog.
   - *Remedy:* Refresh the demo baseline window in `FileManager` and `data/elections.txt` to remain currently active and open for live student voting.
2. **[P2-2] Window Resize Reflow in Admin Election Management:**
   - *Issue:* At compact laptop screen sizes (1100×700), horizontal scrolling can occur if forms and tables do not wrap flexibly.
   - *Impact:* Minor layout cramping on smaller displays.
   - *Remedy:* Standardize responsive scroll wrappers and flexible grid layouts.

### Priority P3 (Visual Polish, Warnings & Documentation)
1. **[P3-1] Unsynchronized Project Documentation:**
   - *Issue:* Documentation files do not yet reference the newly added UI smoke suite, explicit demo reset procedure, or course topic traceability.
   - *Remedy:* Update `README.md` and `MASTER.md` with complete, verified details.
2. **[P3-2] Compiler Warnings Under `-Xlint:all`:**
   - *Issue:* Swing classes and `FileManager` previously emitted 29 compiler warnings.
   - *Remedy:* Enforce a strict 0-warning baseline via `@SuppressWarnings("serial")` and `@SuppressWarnings("this-escape")`.

---

## 6. Requirements Traceability Matrix

| Requirement | Description | Controlling Layer | Implementation Class | Verification Test |
| :--- | :--- | :--- | :--- | :--- |
| **REQ-AUTH-01** | Admin authentication | Service | `AuthenticationService.java` | `VotumTestSuite.test1` |
| **REQ-AUTH-02** | Voter authentication | Service | `AuthenticationService.java` | `VotumTestSuite.test3` |
| **REQ-AUTH-03** | Invalid password rejection | Service | `AuthenticationService.java` | `VotumTestSuite.test2` |
| **REQ-ELEC-01** | Election creation (DRAFT) | Service / Storage | `ElectionService.java` | `VotumTestSuite.test4, 23` |
| **REQ-ELEC-02** | Election window validation | Service / UI | `Election.java`, `DateTimeInput.java` | `VotumTestSuite.test22` |
| **REQ-ELEC-03** | Cascade election deletion | Service / Storage | `ElectionService.java`, `FileManager.java`| `VotumTestSuite.test16` |
| **REQ-VOTE-01** | Single-vote per election | Service / Storage | `VotingService.java` | `VotumTestSuite.test8, 17` |
| **REQ-VOTE-02** | Time-window enforcement | Service / Model | `VotingService.java`, `Election.java` | `VotumTestSuite.test10, 11, 21` |
| **REQ-VOTE-03** | Candidate validation | Service | `VotingService.java` | `VotumTestSuite.test9` |
| **REQ-TALLY-01**| Vote counting & turnout % | Service / Model | `ResultService.java`, `ElectionResult.java` | `VotumTestSuite.test12, 13` |
| **REQ-TALLY-02**| Tie detection | Service / Model | `ResultService.java`, `ElectionResult.java` | `VotumTestSuite.test14` |
| **REQ-AUDIT-01**| Append-only logging | Storage | `AuditLogger.java` | `VotumTestSuite.test20, 24` |
| **REQ-DATA-01** | Malformed record resilience| Storage | `FileManager.java` | `VotumTestSuite.test19` |
| **REQ-DATA-02** | Intentional demo reset | Storage / UI | `FileManager.java`, `AdminDashboard.java` | UI Smoke Test & Manual Verify |
| **REQ-OOP-01**  | Polymorphism & Abstract User| Model | `User.java`, `Admin.java`, `Voter.java` | `VotumTestSuite.test1, 3` |
| **REQ-OOP-02**  | Interface Contracts | Model | `Identifiable.java`, `AuthenticatableUser.java`| Compilation & Test Suite |
| **REQ-OOP-03**  | Safe Multithreading | UI / Service | `AdminDashboard.LiveRefreshWorker` | Programmatic UI Verification |

---

## 7. Audit Conclusion & Action Plan

The core domain logic of VOTUM is solid, but the project requires four targeted enhancements to achieve 100% compliance with CSC2104 course requirements and production-grade software quality:
1. Introduce clean OOP interfaces (`Identifiable`, `AuthenticatableUser`) and convert `User` into an abstract class with dynamic method dispatch.
2. Implement a dedicated, confirmed "Reset Demo Environment" workflow in `FileManager` and `AdminDashboard`.
3. Add a cooperative background multithreading worker in `AdminDashboard` for live metric synchronization.
4. Refresh demo baseline dates and ensure zero-warning compilation under `-Xlint:all`.

