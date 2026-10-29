# VOTUM 2.0 — Software Architecture Specification

**Product:** VOTUM — Digital Campus Election Management System  
**Version:** 2.0 (Student Edition)  
**Target Environment:** Java SE 17+ (Standard JDK, Java Swing)  

---

## 1. Architectural Style & Overview

VOTUM is built following a classic **Four-Tier Layered Architecture** with strict unidirectional dependencies. The system separates user presentation, business logic execution, data persistence, and domain representations into distinct, decoupled packages.

```mermaid
flowchart TD
    subgraph UI_Layer ["Presentation Layer (Swing GUI)"]
        LF[LoginFrame]
        AD[AdminDashboard]
        VD[VoterDashboard]
        RD[ResultDialog]
        DTI[DateTimeInput]
        UT[UITheme]
    end

    subgraph Service_Layer ["Business Service Layer"]
        AUTH[AuthenticationService]
        ELEC[ElectionService]
        VOTE[VotingService]
        RES[ResultService]
    end

    subgraph Storage_Layer ["Data Access & Storage Layer"]
        FM[FileManager (Singleton)]
        AL[AuditLogger]
    end

    subgraph Model_Layer ["Domain Model Layer"]
        USER[User <abstract>]
        ADMIN[Admin]
        VOTER[Voter]
        EL[Election]
        CAN[Candidate]
        VT[Vote]
        ER[ElectionResult]
        INT[Interfaces: Identifiable, AuthenticatableUser]
    end

    subgraph Storage_Files ["Disk Storage (data/*.txt, data/audit.log)"]
        F1[(users.txt)]
        F2[(voters.txt)]
        F3[(candidates.txt)]
        F4[(elections.txt)]
        F5[(votes.txt)]
        F6[(audit.log)]
    end

    UI_Layer --> Service_Layer
    Service_Layer --> Storage_Layer
    Service_Layer --> Model_Layer
    Storage_Layer --> Model_Layer
    Storage_Layer --> Storage_Files
```

### Dependency Rules

1. **Downward Dependencies Only**: Presentation components invoke Services; Services call Storage and manipulate Models; Storage persists Models to disk.
2. **Zero Storage Access in UI**: UI classes (`com.votum.ui.*`) **never** instantiate `File`, `FileReader`, `BufferedReader`, or invoke `FileManager` directly. All state queries and mutative actions pass through service classes.
3. **Pure Domain Models**: Model classes contain state attributes, getters, setters, and entity-level invariants. They contain zero I/O or Swing dependencies.
4. **Exception-Driven Control Flow**: Services communicate operational failures to the UI layer exclusively using strongly-typed checked exceptions (`VotumException` hierarchy).

---

## 2. Package Decomposition & Responsibilities

| Package | Key Classes | Core Responsibilities |
| :--- | :--- | :--- |
| **`com.votum`** | [`Main.java`](../src/com/votum/Main.java) | Application entry point; initializes Swing look-and-feel, performs storage pre-flight validation, and launches [`LoginFrame`](../src/com/votum/ui/LoginFrame.java) on the Event Dispatch Thread (EDT). |
| **`com.votum.model`** | [`Identifiable.java`](../src/com/votum/model/Identifiable.java)<br>[`AuthenticatableUser.java`](../src/com/votum/model/AuthenticatableUser.java)<br>[`User.java`](../src/com/votum/model/User.java)<br>[`Admin.java`](../src/com/votum/model/Admin.java)<br>[`Voter.java`](../src/com/votum/model/Voter.java)<br>[`Election.java`](../src/com/votum/model/Election.java)<br>[`Candidate.java`](../src/com/votum/model/Candidate.java)<br>[`Vote.java`](../src/com/votum/model/Vote.java)<br>[`ElectionStatus.java`](../src/com/votum/model/ElectionStatus.java)<br>[`ElectionResult.java`](../src/com/votum/model/ElectionResult.java) | Encapsulates campus election entities. Implements OOP contracts (abstraction, inheritance, interface implementation). Ensures state invariants (e.g. non-null IDs, validated voting windows). |
| **`com.votum.service`** | [`AuthenticationService.java`](../src/com/votum/service/AuthenticationService.java)<br>[`ElectionService.java`](../src/com/votum/service/ElectionService.java)<br>[`VotingService.java`](../src/com/votum/service/VotingService.java)<br>[`ResultService.java`](../src/com/votum/service/ResultService.java) | Coordinates election business workflows. Validates credentials, creates elections, executes cascading deletions, verifies single-vote rules and voting windows, tabulates votes, and calculates winners/ties. |
| **`com.votum.storage`** | [`FileManager.java`](../src/com/votum/storage/FileManager.java)<br>[`AuditLogger.java`](../src/com/votum/storage/AuditLogger.java) | Handles flat-file persistence in `data/`. Implements Thread-safe singleton pattern, delimiter sanitization, malformed record skipping, and complete demo-data restoration. |
| **`com.votum.exception`** | [`VotumException.java`](../src/com/votum/exception/VotumException.java)<br>[`InvalidLoginException.java`](../src/com/votum/exception/InvalidLoginException.java)<br>[`InvalidElectionException.java`](../src/com/votum/exception/InvalidElectionException.java)<br>[`InvalidCandidateException.java`](../src/com/votum/exception/InvalidCandidateException.java)<br>[`ElectionClosedException.java`](../src/com/votum/exception/ElectionClosedException.java)<br>[`DuplicateVoteException.java`](../src/com/votum/exception/DuplicateVoteException.java) | Structured checked exception hierarchy for predictable error reporting without unhandled runtime exceptions. |
| **`com.votum.ui`** | [`UITheme.java`](../src/com/votum/ui/UITheme.java)<br>[`DateTimeInput.java`](../src/com/votum/ui/DateTimeInput.java)<br>[`LoginFrame.java`](../src/com/votum/ui/LoginFrame.java)<br>[`AdminDashboard.java`](../src/com/votum/ui/AdminDashboard.java)<br>[`VoterDashboard.java`](../src/com/votum/ui/VoterDashboard.java)<br>[`ResultDialog.java`](../src/com/votum/ui/ResultDialog.java) | Java Swing presentation layer built on the **Editorial Grid** design system: warm beige paper canvas (`#F5EEE1`), black ink, one red accent, serif display headlines, numbered contents navigation, hairline rules, magazine-style feature/sidebar grids and restrained motion. Layouts are responsive, contrast meets WCAG AA, focus states are explicit, and user feedback is immediate. |
| **`com.votum.test`** | [`VotumTestSuite.java`](../src/com/votum/test/VotumTestSuite.java)<br>[`UISmokeTestSuite.java`](../src/com/votum/test/UISmokeTestSuite.java) | Verification harness covering 24 domain logic tests and 27 UI & interaction smoke tests. Zero third-party testing dependencies. |

---

## 3. Data Flow Workflows

### 3.1 Authentication Workflow
1. User enters username and password in [`LoginFrame`](../src/com/votum/ui/LoginFrame.java).
2. `LoginFrame` delegates authentication to [`AuthenticationService.login(username, password)`](../src/com/votum/service/AuthenticationService.java).
3. `AuthenticationService` queries `FileManager` for matching Admin or Student Voter records.
4. Password verification is performed polymorphically via [`AuthenticatableUser.verifyPassword()`](../src/com/votum/model/AuthenticatableUser.java).
5. If invalid, an [`InvalidLoginException`](../src/com/votum/exception/InvalidLoginException.java) is thrown and rendered inline in the UI.
6. If valid, the authenticated [`User`](../src/com/votum/model/User.java) is returned, logged via [`AuditLogger`](../src/com/votum/storage/AuditLogger.java), and the appropriate role-based dashboard is displayed.

### 3.2 Ballot Casting Workflow
1. Student selects an active election and chooses a candidate card in [`VoterDashboard`](../src/com/votum/ui/VoterDashboard.java).
2. When the voter clicks **[ Submit Ballot ]**, a modal confirmation dialog requires final verification.
3. On confirmation, [`VotingService.castVote(voterId, electionId, candidateId)`](../src/com/votum/service/VotingService.java) is invoked.
4. `VotingService` performs atomic business validations:
   - Verifies the election status is `OPEN`.
   - Checks that current time is within `[startTime, endTime]` (throws [`ElectionClosedException`](../src/com/votum/exception/ElectionClosedException.java)).
   - Verifies the voter has not already voted in this election (throws [`DuplicateVoteException`](../src/com/votum/exception/DuplicateVoteException.java)).
   - Verifies the candidate exists and is assigned to the specified election (throws [`InvalidCandidateException`](../src/com/votum/exception/InvalidCandidateException.java)).
5. In a thread-synchronized block, the vote record is persisted, the candidate vote count is incremented, and the voter status is updated.
6. The audit logger records the vote submission, and the dashboard transitions into a post-voting success state.

### 3.3 Election Deletion Cascade Workflow
1. Administrator selects an election in [`AdminDashboard`](../src/com/votum/ui/AdminDashboard.java) and requests deletion.
2. An explicit warning confirmation dialog is presented.
3. On confirmation, [`ElectionService.deleteElection(electionId)`](../src/com/votum/service/ElectionService.java) executes:
   - Removes the election from `elections.txt`.
   - Cascades removal of all associated candidates from `candidates.txt`.
   - Cascades removal of all ballot records for this election from `votes.txt`.
   - Identifies all voters who only voted in this election and resets their `hasVoted` flag to `false`.
   - Purges related election entries from `audit.log`.
   - Flushes all changes to disk and records an administrative audit entry.

---

## 4. Concurrency & Threading Model

- **Swing Event Dispatch Thread (EDT)**: All UI creation, event handling, and widget updates execute strictly on the EDT to comply with Swing threading rules (`SwingUtilities.invokeLater()`).
- **Live Background Refresh**: The Admin Dashboard features a managed background worker (`LiveRefreshWorker`) implementing `Runnable`. It periodically checks for newly cast ballots, updates dashboard metrics, and schedules UI refreshes back onto the EDT using `SwingUtilities.invokeLater()`.
- **Storage Synchronization**: In-memory caching and disk-writing methods in `FileManager` and `VotingService` are marked `synchronized` or execute within synchronized blocks, preventing race conditions or corrupted file writes.

