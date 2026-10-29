# VOTUM 2.0 — End-User Operation & Administration Manual

**Product:** VOTUM — Digital Campus Election Management System  
**Version:** 2.0 (Student Edition)  
**Author:** Department of Computer Science & Engineering, Manipal University Jaipur  

---

## 1. System Requirements & Launch Instructions

### Prerequisites
- Java Runtime Environment (JRE) or Java Development Kit (JDK) **version 17 or higher**.
- Any desktop operating system supporting Java Swing: macOS, Windows 10/11, or Linux.
- Minimum display resolution: 1280 x 800 (recommended).

### Launching the Application

**On macOS / Linux:**
Open a terminal in the project directory and execute:
```bash
./run.sh
# Or alternatively:
./scripts/run.sh
```

**On Windows:**
Double-click `run.bat` in File Explorer, or open Command Prompt:
```cmd
run.bat
```

---

## 2. Default Demo Credentials

For quick evaluation and viva demonstrations, VOTUM includes pre-configured accounts:

| Role | Username | Password | Student ID / Full Name | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **Administrator** | `admin` | `admin123` | `ADM001` - Campus Election Officer | Full administrative authority across all modules. |
| **Student Voter 1** | `voter1` | `voter123` | `V001` - Aarav Gupta (Computer Science) | Eligible active student voter. |
| **Student Voter 2** | `voter2` | `voter123` | `V002` - Ananya Singh (Information Tech) | Eligible active student voter. |
| **Student Voter 3** | `voter3` | `voter123` | `V003` - Rohan Nair (Mechanical Engg) | Eligible active student voter. |
| **Student Voter 4** | `voter4` | `voter123` | `V004` - Sneha Patel (Electronics & Comm) | Eligible active student voter. |
| **Student Voter 5** | `voter5` | `voter123` | `V005` - Dev Sharma (Computer Science) | Ineligible voter (for testing eligibility guard). |

> **Tip:** The Login screen features convenience helper buttons (**[ Admin ]**, **[ Voter 1 (Eligible) ]**, and **[ Voter 5 (Ineligible) ]**) to populate the credentials with one click.

---

## 3. Student Voter Guide

### Step 1: Authentication
1. Launch VOTUM. The login screen displays username and password input fields.
2. Enter your student login credentials (e.g. `voter1` and `voter123`) or click **[ Voter 1 (Eligible) ]**.
3. Click **[ Sign In ]** or press **Enter**.

### Step 2: The Voter Dashboard
Upon sign-in, your personalized student dashboard appears:
- **Welcome Banner**: Displays your full name, student ID, and registered department.
- **Active Elections List**: Shows elections currently open for balloting.
- **Participation Status**: Displays a green badge `[ Ready to Vote ]` if you haven't voted, or `[ Vote Recorded ]` if you have already cast your ballot.

### Step 3: Selecting a Candidate & Submitting a Ballot
1. Click on the desired election from the left-hand election selector.
2. In the candidate section, browse the candidate cards. Each card displays the candidate's name, department, candidate ID, and campaign slogan.
3. Select your chosen candidate by clicking on their card (the card highlights with a bright border).
4. Click the prominent **[ Submit Ballot ]** button.
5. A confirmation dialog appears asking you to verify your choice. Review the candidate name and click **[ Confirm ]**.
6. A success message confirms your vote has been securely recorded. The dashboard refreshes immediately, disabling further voting in that election to enforce the one-voter-one-vote rule.

### Step 4: Signing Out
When finished, click **[ Sign Out ]** in the top-right corner to return to the login screen.

---

## 4. Administrator Guide

### 4.1 Overview & Live Metrics
Upon logging in as `admin`, the executive overview provides an immediate snapshot:
- **Stat Cards**:
  - **Total Elections**: Total number of configured campus elections.
  - **Active Elections**: Number of elections currently in `OPEN` status.
  - **Registered Voters**: Total registered student electorate.
  - **Overall Turnout**: Percentage and ratio of students who have voted.
- **Attention Panel**: Highlights immediate operational notices (e.g. elections closing soon or unvoted status).
- **Background Auto-Refresh**: An asynchronous worker thread periodically updates vote counts and turnout without interrupting UI interaction.

---

### 4.2 Managing Elections
Navigate to the **Elections** tab:
- **Create New Election**: Click **[ + Create Election ]**. Enter an Election ID (e.g., `E002`), title, description, and configure the start and end dates/times using the dropdown controls.
- **Change Election Status**: Select an election row and toggle its status between `DRAFT`, `OPEN`, and `CLOSED`.
- **Delete Election**: Click **[ Delete Election ]**. A security dialog warns that deleting an election triggers a **cascading cleanup**:
  1. Associated candidate rosters are deleted.
  2. All ballots cast in that election are purged.
  3. Student voter statuses are recalculated automatically.

---

### 4.3 Candidate Management
Navigate to the **Candidates** tab:
- **Register Candidate**: Enter a Candidate ID (e.g., `C101`), candidate name, academic department, and choose the target election from the dropdown. Click **[ Register Candidate ]**.
- **View Rosters**: Filter candidates by election or search by candidate name to see accumulated vote counts.

---

### 4.4 Student Voter Registry
Navigate to the **Voters** tab:
- **Enroll New Student**: Enter the student ID (e.g., `V106`), full name, and academic department. Click **[ Enroll Voter ]**.
- **Track Turnout**: The voter table displays each student's current status: `[ Voted ]` (green) or `[ Not Voted ]` (gray).
- **Filter & Search**: Quickly search for any student by ID, name, or department.

---

### 4.5 Official Election Results & Tallies
Navigate to the **Results** tab:
1. Select the election from the selector.
2. Click **[ View Full Official Result ]**.
3. A modal **Result Dialog** presents:
   - Winner banner with trophy icon (or clear `[ TIE DETECTED ]` badge if top candidates share equal votes).
   - Total ballots cast and official voter turnout percentage.
   - Graphical horizontal vote distribution bar for each candidate.
   - Comprehensive tally table with vote counts and percentage shares.

---

### 4.6 Audit Trail & System Reset
- **Audit Logs Tab**: Inspect the append-only operational log recording every login, election creation, candidate registration, and ballot cast.
- **Reset Demo Data Button**: Located in the top header bar **[ ↺ Reset Demo Data ]**. Click this at any time during an evaluation to restore the clean baseline demonstration environment.
