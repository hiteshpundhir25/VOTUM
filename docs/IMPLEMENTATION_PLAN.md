# VOTUM 2.0 — Implementation Roadmap & Execution Plan

**Project:** VOTUM — Digital Campus Election Management System  
**Course:** CSC2104 Object Oriented Programming (Manipal University Jaipur)  
**Plan Date:** 2026-10-29  
**Execution Strategy:** Sequential multi-stage refactoring with continuous regression verification.

---

## Stage Breakdown & Milestones

### Stage 1 — Repository Inspection & Academic Alignment (Complete)
- [x] Full source code inspection across `model`, `exception`, `storage`, `service`, `ui`, `test`.
- [x] Read course handout `Updated Object Oriented Programming CSC2104-Handout.docx`.
- [x] Produced `docs/PROJECT_AUDIT.md`.
- [x] Produced `docs/CSC2104_ALIGNMENT.md`.
- [x] Produced `docs/IMPLEMENTATION_PLAN.md`.

### Stage 2 — Baseline Verification (Complete)
- [x] Verify clean compilation with JDK: `javac -Xlint:all -d bin -cp src $(find src -name "*.java")`.
- [x] Run automated domain suite: `java -cp bin com.votum.test.VotumTestSuite` (24/24 PASS).
- [x] Run automated UI smoke suite: `java -cp bin com.votum.test.UISmokeTestSuite` (21/21 PASS).
- [x] Record 0 errors, 0 warnings baseline.

### Stage 3 — Architecture & Syllabus Reconstruction (Complete)
- [x] Create interface `Identifiable` in `com.votum.model`.
- [x] Create interface `AuthenticatableUser` in `com.votum.model`.
- [x] Refactor `User` to `public abstract class User implements AuthenticatableUser, Identifiable`.
- [x] Add abstract methods `public abstract String getDisplayName()` and `public abstract String getId()`.
- [x] Implement `getDisplayName()` and `getId()` in `Admin` and `Voter`.
- [x] Implement `Identifiable` in `Candidate`, `Election`, and `Vote`.
- [x] Verify domain tests pass immediately after model refactoring.

### Stage 4 — Explicit Demo Reset Workflow & Storage Enhancements (Complete)
- [x] Implement `FileManager.resetToDemoData()` to restore the official baseline:
  - Admin `admin / admin123` (`ADM001`).
  - Active Election `E001` with an open voting window spanning from current time minus 1 hour to plus 7 days.
  - 3 Candidates: Arjun Sharma (`C001`), Priya Mehta (`C002`), Rahul Verma (`C003`).
  - 5 Voters: Aarav (`V001`), Ananya (`V002`), Rohan (`V003`), Sneha (`V004`) as eligible, Dev (`V005`) as ineligible. All unvoted (`hasVoted=false`).
  - Empty `votes.txt`.
  - Log `"Demo environment reset"` in `audit.log`.
- [x] Add explicit unit and smoke verification for `resetToDemoData()`.

### Stage 5 — Safe Multithreading & Responsiveness (Complete)
- [x] Create a controlled background daemon worker `LiveRefreshWorker` implementing `Runnable` in `AdminDashboard`.
- [x] Implement cooperative cancellation using `volatile boolean running` and `interrupt()`.
- [x] Ensure all GUI updates are posted strictly on the Swing EDT via `SwingUtilities.invokeLater()`.
- [x] Clean up and terminate the background thread upon window disposal (`dispose()`).

### Stage 6 — User Interface & Interaction Enhancements (Complete)
- [x] In `AdminDashboard`:
  - Add a dedicated **[ ↺ Reset Demo Data ]** button in the header/overview with a two-step confirmation dialog.
  - Bind the background worker to update live metrics automatically.
  - Enhance attention alerts with direct navigation actions.
- [x] In `LoginFrame`:
  - Verify 3 quick-fill buttons (`Admin`, `Voter 1`, `Voter 5`) work seamlessly with the reset baseline.
  - Maintain busy state and inline validation feedback.
- [x] In `VoterDashboard`:
  - Verify candidate cards display active selection with `✓ SELECTED` pill badges.
  - Confirm ballot submission produces a persistent, scroll-proof receipt.
- [x] In `ResultDialog`:
  - Verify winner, tie, and zero-vote states with animated vote distribution bars.

### Stage 7 — Full Automated Verification & Regression Testing (Complete)
- [x] Run domain test suite (`VotumTestSuite` - 24 cases).
- [x] Run UI smoke test suite (`UISmokeTestSuite` - expanded to 27 cases).
- [x] Verify zero compiler warnings under `javac -Xlint:all`.
- [x] Perform architecture boundary check: `grep -rn "fileManager.load\|new File(" src/com/votum/ui/` returns 0 matches.

### Stage 8 — Documentation & Deliverables (Complete)
- [x] Update `README.md` to reflect new interfaces, abstract `User`, demo reset procedure, and multithreading.
- [x] Update `MASTER.md` with refined class contracts and syllabus mapping.
- [x] Finalize `docs/CSC2104_ALIGNMENT.md` with exact implementation links.
- [x] Generate comprehensive Final Acceptance Report.
