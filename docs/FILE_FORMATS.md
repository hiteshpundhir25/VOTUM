# VOTUM 2.0 — File Formats & Persistence Specification

**Product:** VOTUM — Digital Campus Election Management System  
**Storage Engine:** Plain-Text Semicolon-Delimited Flat Files & Pipe-Delimited Logs  
**Target Directory:** `data/`  

---

## 1. Storage Design Philosophy

VOTUM uses a zero-dependency, flat-file persistence layer designed for student-level clarity and reliability:
- No external relational database (MySQL, PostgreSQL, SQLite) or JDBC drivers.
- Standard Java character streams (`BufferedReader`, `BufferedWriter`, `FileReader`, `FileWriter`).
- Strict delimiter separation using semicolons (`;`) for data files and pipe (` | `) for audit logs.
- Invariant sanitization on write to prevent delimiter injection.
- Resilient parsing on read to skip corrupt lines without application crash.

---

## 2. File Specifications

### 2.1 `users.txt` — Administrative Credentials

Stores administrative accounts authorized to manage elections and candidates.

| Column Index | Field Name | Data Type | Nullable | Example | Validation & Description |
| :---: | :--- | :--- | :---: | :--- | :--- |
| 0 | `username` | String | No | `admin` | Unique admin login username. |
| 1 | `password` | String | No | `admin123` | Authentication password. |
| 2 | `adminId` | String | No | `ADM001` | Unique administrative identification ID. |
| 3 | `fullName` | String | No | `Campus Election Officer` | Officer full name. |
| 4 | `email` | String | No | `admin@votum.edu` | Official institutional email address. |

**File Sample:**
```text
admin;admin123;ADM001;Campus Election Officer;admin@votum.edu
```

---

### 2.2 `voters.txt` — Registered Student Voters

Stores student voter records, login credentials, and eligibility/participation flags.

| Column Index | Field Name | Data Type | Nullable | Example | Validation & Description |
| :---: | :--- | :--- | :---: | :--- | :--- |
| 0 | `voterId` | String | No | `V001` | Unique student registration ID. |
| 1 | `fullName` | String | No | `Aarav Gupta` | Full student name. |
| 2 | `department` | String | No | `Computer Science` | Student academic department or branch. |
| 3 | `username` | String | No | `voter1` | Student login username. |
| 4 | `password` | String | No | `voter123` | Student login password. |
| 5 | `isEligible` | boolean | No | `true` | Whether voter is eligible to participate. |
| 6 | `hasVoted` | boolean | No | `false` | Global participation flag (`true` or `false`). |
| 7 | `votedElectionId` | String | Yes | `E001` | ID of election voted in (empty if unvoted). |

**File Sample:**
```text
V001;Aarav Gupta;Computer Science;voter1;voter123;true;false;
V002;Ananya Singh;Information Technology;voter2;voter123;true;false;
V003;Rohan Nair;Mechanical Engg;voter3;voter123;true;false;
V004;Sneha Patel;Electronics & Comm.;voter4;voter123;true;false;
V005;Dev Sharma;Computer Science;voter5;voter123;false;false;
```

---

### 2.3 `candidates.txt` — Election Candidates

Stores candidates standing for election and their campaign slogans.

| Column Index | Field Name | Data Type | Nullable | Example | Validation & Description |
| :---: | :--- | :--- | :---: | :--- | :--- |
| 0 | `candidateId` | String | No | `C001` | Unique candidate identifier. |
| 1 | `fullName` | String | No | `Arjun Sharma` | Full candidate name. |
| 2 | `department` | String | No | `Computer Science` | Academic department. |
| 3 | `electionId` | String | No | `E001` | Foreign key referencing `elections.txt`. |
| 4 | `slogan` | String | Yes | `Better Campus Wi-Fi & Tech Labs` | Campaign manifesto or slogan summary. |

**File Sample:**
```text
C001;Arjun Sharma;Computer Science;E001;Better Campus Wi-Fi & Tech Labs
C002;Priya Mehta;Information Technology;E001;Transparent Student Representation
C003;Rahul Verma;Electronics & Comm.;E001;Sports & Cultural Fest Upgrades
```

---

### 2.4 `elections.txt` — Elections & Voting Windows

Stores campus election metadata, status, and active time windows.

| Column Index | Field Name | Data Type | Nullable | Example | Validation & Description |
| :---: | :--- | :--- | :---: | :--- | :--- |
| 0 | `electionId` | String | No | `E001` | Unique election identifier. |
| 1 | `title` | String | No | `Student Council President 2026` | Public title of the election. |
| 2 | `description` | String | Yes | `Annual general election for student body president` | Purpose or department scope. |
| 3 | `startTime` | ISO-8601 | No | `2026-10-29T00:49:06` | Must parse with `LocalDateTime.parse()`. |
| 4 | `endTime` | ISO-8601 | No | `2026-11-05T01:49:06` | Must be strictly after `startTime`. |
| 5 | `status` | Enum | No | `OPEN` | One of `DRAFT`, `OPEN`, `CLOSED`. |

**File Sample:**
```text
E001;Student Council President 2026;Annual general election for student body president;2026-10-29T00:49:06;2026-11-05T01:49:06;OPEN
```

---

### 2.5 `votes.txt` — Cast Ballots

Stores immutable ballot records representing completed votes.

| Column Index | Field Name | Data Type | Nullable | Example | Validation & Description |
| :---: | :--- | :--- | :---: | :--- | :--- |
| 0 | `voteId` | String | No | `VOTE-b9a1...` | Unique UUID or prefix string. |
| 1 | `electionId` | String | No | `E001` | Foreign key referencing election in `elections.txt`. |
| 2 | `candidateId` | String | No | `C001` | Foreign key referencing candidate in `candidates.txt`. |
| 3 | `voterId` | String | No | `V001` | Foreign key referencing voter in `voters.txt`. |
| 4 | `timestamp` | ISO-8601 | No | `2026-10-29T01:15:22` | Exact vote cast timestamp. |

---

### 2.6 `audit.log` — Security & Operational Audit Log

Append-only event log recording administrative and voter actions.

| Column Index | Field Name | Data Type | Example | Description |
| :---: | :--- | :--- | :--- | :--- |
| 0 | `timestamp` | Formatted Date | `2026-10-29 01:54:37` | Formatted timestamp (`yyyy-MM-dd HH:mm:ss`). |
| 1 | `event` | String | `VOTE_CAST` | Event type (e.g. `LOGIN`, `CREATE_ELECTION`, `VOTE_CAST`). |
| 2 | `details` | String | `Voter V001 voted in election E001` | Human-readable event description. |

**Log Sample:**
```text
2026-10-29 01:49:06 | DEMO_RESET | Reset database to clean demonstration fixtures
2026-10-29 01:50:12 | LOGIN | User admin logged into administrative console
2026-10-29 01:52:45 | VOTE_CAST | Voter V001 cast ballot for candidate C001 in election E001
```

---

## 3. Delimiter Handling & Injection Prevention

1. **Sanitization on Input**: Any user-entered field (such as election title, description, candidate name) is sanitized before writing by replacing semicolons (`;`) with commas (`,`) and carriage returns/newlines with spaces.
2. **Safe Serialization**: All record-writing methods in `FileManager` write lines formatted explicitly with `";"`.

---

## 4. Fault Tolerance & Corrupt Record Handling

During file loading, `FileManager` wraps line parsing in guarded blocks:
- **Missing File**: Created automatically on first read/write.
- **Incorrect Column Count**: Lines with fewer than expected columns are reported via `warnMalformedRecord()` and skipped.
- **Unparseable Dates or Enums**: Lines where date formatting or enum parsing fails are skipped with a descriptive warning message.
- **Healthy Record Preservation**: One corrupt line never prevents the remaining valid records in the file from loading.
