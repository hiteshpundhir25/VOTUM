# VOTUM — Project Changelog & Architectural Evolution

All notable changes, structural reorganizations, and feature enhancements for **VOTUM — Digital Campus Election Management System** are documented in this file.

---

## [Version 2.1.0] — 2026-10-30 (Editorial Grid Interface)

### Changed
- **Design System Rebuilt (`UITheme`)**:
  - Replaced the dark slate palette with the **Editorial Grid** language: warm beige paper (`#F5EEE1`), black ink, one red accent (`#DC2626`), warm grey metadata and deeper beige sidebars/table headers. Playfair Display / Georgia serif headlines, uppercase sans kickers and hairline column rules replace rounded dark cards. Small red text uses `#B91C1C` for AA contrast on beige; white `ON_DARK` is kept for text on ink and accent blocks.
  - Added `RevealPanel` (screens fade and rise into view), animated metric counters, growing vote bars and hover-inverting flat buttons. Motion honours `-Dvotum.motion=off`, `-Dvotum.reduceMotion=true` and the platform reduced-motion setting.
  - Added fully custom `EditorialRadioButton`, `EditorialCheckBox` and `EditorialComboBoxUI` controls so no platform (purple) glyphs leak into the interface; focus rings are drawn in the accent red.
- **Screen Redesigns**:
  - `LoginFrame` reads as a newspaper front page: masthead, edition line, lead story, pull quote and quick-fill strip.
  - `AdminDashboard` gains a masthead, numbered contents-style sidebar, editorial section headers, a lead/desk-notes asymmetric overview and a data band of metric cards.
  - `VoterDashboard` becomes a ballot paper: lead election story, numbered candidate profiles with pull quotes, and a marked-up ballot sidebar.
  - `ResultDialog` becomes a certified results page; `DateTimeInput` follows the paper input style.

### Fixed
- Desk notes and attention rows now wrap with measured font metrics instead of fixed HTML widths, so no copy is clipped in the narrow sidebar.
- Voter table columns, candidate table sizing and audit header rules were corrected at the 1080px minimum window width.
- Removed a duplicated divider above the winner block in the result dialog and a right-aligned stray label in the results data card.

---

## [Version 2.0.0] — 2026-10-29 (Academic & Architecture Refinement)

### Added
- **Formal OOP Interface Contracts (`com.votum.model`)**:
  - `Identifiable`: Standardized contract requiring `String getId()` implemented by `User`, `Candidate`, `Election`, and `Vote`.
  - `AuthenticatableUser`: Contract enforcing `String getUsername()`, `boolean verifyPassword(String)`, and `String getRole()`.
- **Abstract User Dynamic Dispatch**:
  - `public abstract class User`: Base abstraction with abstract methods `public abstract String getDisplayName()` and `public abstract String getId()`.
  - Polymorphic runtime implementations in `Admin` (returning `"Administrator (" + username + ")"`) and `Voter` (returning `name + " (" + department + ")"`).
- **Automated Verification Harness (`com.votum.test`)**:
  - Native assertion harness with zero third-party dependencies.
  - `VotumTestSuite.java`: 24 comprehensive domain tests covering election lifecycle, time boundaries, duplicate vote blocking, tie detection, data corruption recovery, and cascading deletions.
  - `UISmokeTestSuite.java`: 27 automated UI checks verifying theme constants, button factories, responsive card layouts, dialog accessibility, and state reloads.
- **Demo Reset Workflow**:
  - Added `resetToDemoData()` in `FileManager` and `resetDemoEnvironment()` in `ElectionService`.
  - Added **[ ↺ Reset Demo Data ]** and **[ ↻ Refresh ]** buttons in the `AdminDashboard` header.
- **Cross-Platform Script Suite (`scripts/`)**:
  - Added `scripts/compile.sh`, `scripts/compile.bat` with `-Xlint:all`.
  - Added `scripts/run.sh`, `scripts/run.bat`.
  - Added `scripts/test.sh`, `scripts/test.bat` executing both test suites sequentially.
  - Created root wrapper scripts for seamless backward-compatible execution.
- **Comprehensive Documentation Suite (`docs/`)**:
  - `docs/PROJECT_AUDIT.md`: System audit covering screens, workflows, and edge cases.
  - `docs/CSC2104_ALIGNMENT.md`: Course syllabus mapping to Course Outcomes CSC2104.1 through CSC2104.4.
  - `docs/ARCHITECTURE.md`: 4-tier architectural specification and sequence diagrams.
  - `docs/FILE_FORMATS.md`: Detailed flat-file storage layouts, schemas, and fault-tolerance rules.
  - `docs/TESTING.md`: Quality assurance handbook and manual test procedures.
  - `docs/USER_GUIDE.md`: Step-by-step user manual for Administrators and Student Voters.
  - `docs/IMPLEMENTATION_PLAN.md`: Strategic engineering roadmap.
  - `data/README.md`: Data directory structure, delimiter policies, and record specifications.
  - `.gitignore`: Ignoring build directories, compiled class files, test caches, and OS artifacts.

### Changed
- **UI Architecture & Presentation Decoupling**:
  - Completely eliminated direct file system and storage references (`new File(...)`, `FileManager`) from all UI classes (`LoginFrame`, `AdminDashboard`, `VoterDashboard`, `ResultDialog`, `DateTimeInput`).
  - All UI state operations routed exclusively through `AuthenticationService`, `ElectionService`, `VotingService`, and `ResultService`.
- **Restrained Student-Appropriate UI Design**:
  - Redesigned visual appearance to match a high-quality student coursework project rather than an overly complex corporate enterprise tool.
  - Cohesive dark slate theme (`UITheme`) with restrained accent colors (Electric Blue, Emerald Green, Amber Warning, Crimson Error).
  - Uniform button heights (36px), input fields (38px), and table rows (36px).
  - Responsive candidate voting card layout in `VoterDashboard`.
  - Informative `ResultDialog` featuring horizontal bar charts (`HorizontalVoteBar`) and explicit tie notifications.
- **Storage Resilience & Fault Tolerance**:
  - Enhanced `FileManager` parser to skip malformed lines (bad date formats, invalid column counts, unknown enums) without crashing.
  - Implemented safe delimiter escaping to prevent injection attacks when saving candidate or election names containing semicolons.
- **Cleaned Workspace**:
  - Purged stray `.class` files from source directories (`src/com/votum/...`).
  - Configured output compilation directory strictly to `bin/`.

---

## [Version 1.0.0] — Initial Legacy Implementation

### Features
- Initial Java Swing desktop application.
- Basic administrator authentication and student voter login.
- Basic election creation with start and end dates.
- Candidate registration and simple vote tallying.
- Semicolon-delimited file storage in `data/*.txt`.
- Console-based audit logging.

