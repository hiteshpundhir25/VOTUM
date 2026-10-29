# VOTUM — Data Storage Directory

This directory contains the flat text-based data files used for persistence in **VOTUM — Digital Campus Election Management System**.

## Storage Architecture

VOTUM uses standard Java file I/O (`java.io.BufferedReader`, `java.io.BufferedWriter`, `java.io.FileReader`, `java.io.FileWriter`) without any external database libraries or dependencies.

- **Delimiter**: Semicolon (`;`) is used as the column delimiter across flat data stores (`users.txt`, `voters.txt`, `candidates.txt`, `elections.txt`, `votes.txt`). `audit.log` uses pipe delimiter (` | `).
- **Sanitization**: Any user input containing `;` is sanitized to `,` on write to prevent column injection and file corruption.
- **Fault-Tolerance**: If a file is missing on startup, VOTUM initializes empty file stores or baseline demo fixtures. If a line in a data file is malformed (e.g., incorrect column count, unparseable date/status), the parser logs a warning, skips the corrupt line, and continues loading remaining valid records without crashing.

---

## File Specifications

### 1. `users.txt`
Stores administrative and staff credentials.
- **Format**: `username;password;adminId;fullName;email`
- **Example**: `admin;admin123;ADM001;Campus Election Officer;admin@votum.edu`
- **Fields**:
  - `username` (String): Unique admin login username
  - `password` (String): Admin password
  - `adminId` (String): Administrative identification code (e.g. `ADM001`)
  - `fullName` (String): Full name of the election official
  - `email` (String): Official contact email address

### 2. `voters.txt`
Stores registered student voter profiles, authentication credentials, and voting status.
- **Format**: `voterId;fullName;department;username;password;isEligible;hasVoted;votedElectionId`
- **Example**: `V001;Aarav Gupta;Computer Science;voter1;voter123;true;false;`
- **Fields**:
  - `voterId` (String): Unique student registration number (e.g. `V001`)
  - `fullName` (String): Full student name
  - `department` (String): Department / Branch (e.g. `Computer Science`)
  - `username` (String): Student login username (e.g. `voter1`)
  - `password` (String): Student login password (e.g. `voter123`)
  - `isEligible` (boolean): Whether voter is currently eligible (`true` or `false`)
  - `hasVoted` (boolean): Global participation flag (`true` or `false`)
  - `votedElectionId` (String): ID of election voted in (empty if unvoted)

### 3. `candidates.txt`
Stores candidates running in campus elections.
- **Format**: `candidateId;fullName;department;electionId;slogan`
- **Example**: `C001;Arjun Sharma;Computer Science;E001;Better Campus Wi-Fi & Tech Labs`
- **Fields**:
  - `candidateId` (String): Unique candidate ID (e.g. `C001`)
  - `fullName` (String): Candidate name
  - `department` (String): Candidate academic department
  - `electionId` (String): Foreign key referencing `elections.txt`
  - `slogan` (String): Candidate campaign slogan or manifesto summary

### 4. `elections.txt`
Stores election configurations and voting windows.
- **Format**: `electionId;title;description;startTime;endTime;status`
- **Example**: `E001;Student Council President 2026;Annual general election for student body president;2026-10-29T00:49:06;2026-11-05T01:49:06;OPEN`
- **Fields**:
  - `electionId` (String): Unique election ID (e.g. `E001`)
  - `title` (String): Election name
  - `description` (String): Summary / objective
  - `startTime` (ISO-8601): Formatted timestamp (e.g. `2026-10-29T00:49:06`)
  - `endTime` (ISO-8601): Formatted timestamp strictly after `startTime`
  - `status` (Enum): Lifecycle status (`DRAFT`, `OPEN`, or `CLOSED`)

### 5. `votes.txt`
Stores individual ballot audit records.
- **Format**: `voteId;electionId;candidateId;voterId;timestamp`
- **Example**: `VOTE-b9a1...;E001;C001;V001;2026-10-29T01:15:22`
- **Fields**:
  - `voteId` (String): Unique ballot ID (e.g. `VOTE-uuid`)
  - `electionId` (String): Election foreign key
  - `candidateId` (String): Candidate foreign key
  - `voterId` (String): Voter foreign key
  - `timestamp` (ISO-8601): Exact submission timestamp

### 6. `audit.log`
Chronological operational and security audit log.
- **Format**: `YYYY-MM-DD HH:MM:SS | EVENT | DETAILS`
- **Example**: `2026-10-29 01:54:37 | VOTE_CAST | Voter V001 voted in election E001 for candidate C001`

---

## Resetting Demo Data

To reset all data files back to the clean university demo fixtures:
- **From UI**: Click the **[ ↺ Reset Demo Data ]** button in the header of the Administrator Dashboard.
- **From Code**: Call `FileManager.getInstance().resetToDemoData()`.
- **Manual**: Run `./scripts/test.sh` which exercises and verifies demo data restoration.
