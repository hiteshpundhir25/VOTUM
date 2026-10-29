# VOTUM 2.0 — Quality Assurance & Testing Specification

**Product:** VOTUM — Digital Campus Election Management System  
**Test Harness:** Pure Java Native Assertion Test Harness (JDK Standard Library Only)  
**Total Automated Checks:** 51 Automated Tests (24 Domain Logic + 27 UI & Interaction Checks)  

---

## 1. Testing Philosophy & Architecture

In strict adherence to the CSC2104 course constraints, VOTUM does not rely on third-party test runners (such as JUnit, TestNG, or Mockito). Instead, the application features a purpose-built, high-coverage Java test framework built directly using standard Java language constructs.

### Key Architectural Tenets:
1. **Zero External Dependencies**: All assertions, reporting, and lifecycle hooks are implemented in pure Java.
2. **Total Storage Isolation**: Unit and domain tests run against a dedicated temporary test directory (`data_test/`). The primary production data directory (`data/`) is never modified or polluted by test runs.
3. **Headless & Display Robustness**: The UI smoke suite safely queries Swing components, layouts, colors, and accessibility attributes without requiring a display server or manual mouse interaction.

---

## 2. Test Execution Commands

### macOS / Linux
```bash
# Using root delegating script:
./test.sh

# Or using the dedicated scripts directory:
./scripts/test.sh
```

### Windows
```cmd
REM Using root batch script:
test.bat

REM Or using the scripts directory:
scripts\test.bat
```

---

## 3. Domain Logic Verification Suite (`VotumTestSuite`)

The domain test suite verifies all election rules, data models, exception handling, and edge cases across 24 discrete tests:

| Test ID | Test Name | Business Rule / Concept Verified |
| :---: | :--- | :--- |
| **01** | Admin Login Valid | Verifies admin authentication succeeds with valid credentials. |
| **02** | Invalid Login Rejection | Confirms incorrect passwords throw `InvalidLoginException`. |
| **03** | Voter Login Valid | Verifies student voter authentication using student ID and password. |
| **04** | Election Window Validation | Ensures `endTime` before `startTime` is rejected with `InvalidElectionException`. |
| **05** | Candidate Registration | Verifies candidates correctly register and associate with election ID. |
| **06** | Voter Registration | Confirms student registration creates voter record with `hasVoted = false`. |
| **07** | Valid Ballot Submission | Tests end-to-end vote casting, candidate increment, and voter state update. |
| **08** | Duplicate Vote Blocking | Confirms voting twice in the same election throws `DuplicateVoteException`. |
| **09** | Invalid Candidate Selection | Rejects voting for a non-existent candidate or candidate from another election. |
| **10** | Voting Before Start Time | Confirms voting prior to start time throws `ElectionClosedException`. |
| **11** | Voting After End Time | Confirms voting after end time throws `ElectionClosedException`. |
| **12** | Vote Counting Accuracy | Verifies candidate vote counts match total cast ballots in storage. |
| **13** | Winner & Turnout Calculation | Calculates correct winner and percentage turnout based on registered voters. |
| **14** | Tie Detection | Confirms equal top vote counts trigger tie detection in `ElectionResult`. |
| **15** | Data Persistence & Reload | Writes data, destroys in-memory instance, reloads from disk, verifies state. |
| **16** | Cascading Election Deletion | Deleting election cascades deletion of candidates, votes, and resets voter flag. |
| **17** | Multi-Election Voting Allowed | Verifies voting in Election A does not prevent voting in Election B. |
| **18** | Username Collision Prevention | Rejects voter registration if the username conflicts with an admin. |
| **19** | Corrupt Record Fault Tolerance | Proves corrupt lines in data files are skipped while preserving valid rows. |
| **20** | Whole-Identifier Audit Purge | Confirms audit log purge targets exact ID tokens rather than partial substrings. |
| **21** | Boundary Window Precision | Tests ballots cast exactly on edge timestamps (`startTime` and `endTime`). |
| **22** | Election Creation Parity | Verifies UI validation logic mirrors backend service validation. |
| **23** | End-to-End Election Lifecycle | Exercises full transition: `DRAFT` → `OPEN` → cast ballots → `CLOSED`. |
| **24** | Preview vs Calculate Result | Verifies result preview matches calculation and maintains audit integrity. |

---

## 4. UI & Interaction Smoke Test Suite (`UISmokeTestSuite`)

The UI smoke test suite verifies the presentation layer, accessibility, theme tokens, and component behavior across 27 automated checks:

| Check # | UI Area | Verification Target |
| :---: | :--- | :--- |
| **01** | UITheme | Editorial Grid palette constants (Paper, Ink, Accent, Muted, Rule, status tints) resolved. |
| **02** | UITheme | Cross-platform font fallback probe executes successfully. |
| **03** | UITheme | Editorial button factory produces flat paper/ink buttons with correct cursor styling. |
| **04** | UITheme | Input fields configured with consistent 38px height and paper/ink border styling. |
| **05** | UITheme | Reusable card containers and `StatCard` widgets construct without errors. |
| **06** | UITheme | Square status labels dynamically apply proper semantic colours (`OPEN`, `CLOSED`). |
| **07** | UITheme | Editorial table view model sets proper 36px row height and alternating paper tint. |
| **08** | UITheme | `HorizontalVoteBar` constructs safely and clamps percentage boundaries [0, 100]. |
| **09** | DateTimeInput | Rejects null or unselected start/end dates. |
| **10** | DateTimeInput | Rejects reversed time windows (`end < start`). |
| **11** | DateTimeInput | Successfully constructs and validates valid time windows. |
| **12** | LoginFrame | Constructs with title containing "VOTUM". |
| **13** | LoginFrame | Enforces minimum desktop dimensions (440 x 560). |
| **14** | LoginFrame | Binds Enter key to trigger authentication via default button. |
| **15** | AdminDashboard | Constructs with admin title and layout hierarchy. |
| **16** | AdminDashboard | Enforces desktop minimum bounds (1050 x 680). |
| **17** | VoterDashboard | Constructs with student title and active election panel. |
| **18** | VoterDashboard | Binds Enter key for default ballot submission. |
| **19** | ResultDialog | Constructs modal dialog with election results and bar chart. |
| **20** | ResultDialog | Binds Enter/Escape to dismiss result dialog. |
| **21** | Presentation Decoupling | Verifies UI reloads data exclusively through service layer. |
| **22** | Interface Contract | Verifies `Identifiable` contract implemented across models. |
| **23** | Polymorphism | Dynamic method dispatch verified on `User.getDisplayName()`. |
| **24** | Interface Contract | `AuthenticatableUser` contract verified across authenticatable actors. |
| **25** | Demo Reset | Restores baseline active election `E001` in storage. |
| **26** | Demo Reset | Restores 3 baseline candidates in storage. |
| **27** | Demo Reset | Restores 5 baseline student voters in storage. |

---

## 5. Manual Testing Checklist

For manual QA during development or live viva evaluation:

1. **Clean Start**: Launch `./run.sh`. Verify the Login window opens centered with demo credentials displayed clearly in the helper panel.
2. **Student Login**: Click "Fill Student (V101)" and "Sign In". Confirm Voter Dashboard opens with student greeting, active elections list, and candidate selection cards.
3. **Ballot Submission**: Select candidate "Aryan Gupta" and click "Submit Ballot". Confirm confirmation modal appears, submit, and verify success banner and disabled voting state.
4. **Duplicate Vote Guard**: Attempt to vote again; confirm the system clearly indicates the student has already participated.
5. **Admin Login**: Sign out, log in as `admin` / `admin123`. Verify 4 stat cards update with live turnout (20% turnout: 1 of 5 voters).
6. **Live Results**: Click "View Results" on the election row. Verify winner badge and graphical vote distribution bar render accurately.
7. **Demo Data Reset**: Click the **[ ↺ Reset Demo Data ]** button in the admin header. Confirm statistics reset back to baseline.

