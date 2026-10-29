# VOTUM 2.0 — Academic Syllabus Alignment Report

**Course Code:** CSC2104 — Object Oriented Programming  
**Programme:** B.Tech in Computer Science and Engineering (3rd Semester)  
**Institution:** Department of Computer Science & Engineering, Manipal University Jaipur  
**Reference Document:** `Updated Object Oriented Programming CSC2104-Handout.docx`  
**Application:** VOTUM — Digital Campus Election Management System  

---

## 1. Course Outcomes (CO) Mapping

The following table demonstrates how VOTUM directly satisfies the course outcomes specified in the CSC2104 Course Handout:

| Course Outcome | Description | Attainment Mechanism in VOTUM |
| :--- | :--- | :--- |
| **CSC2104.1** | Define object-oriented programming concepts using class and objects to solve problems. | Clean domain models (`Election`, `Candidate`, `Vote`, `ElectionResult`, `Voter`, `Admin`) representing real-world election entities with constructors, accessors, and state invariants. |
| **CSC2104.2** | Apply the principles of encapsulation and abstraction via objects and classes. | Strict private encapsulation across all models, method overloading in constructors, state validation in mutators, defensive copying of collections, and clean service abstractions. |
| **CSC2104.3** | Demonstrate the use of inheritance, polymorphism, and interfaces. | Abstract `User` hierarchy extended by `Admin` and `Voter`, interfaces `Identifiable` and `AuthenticatableUser`, dynamic method dispatch on `getDisplayName()`, and polymorphic return types. |
| **CSC2104.4** | Analyse advanced programming concepts such as exception handling, multithreading, and garbage collection. | Checked custom exception hierarchy (`VotumException` and 5 subclasses), thread-safe storage synchronization, managed background daemon thread (`LiveRefreshWorker`), and automatic object lifecycle management. |

---

## 2. Topic-by-Topic Compliance Matrix

| Syllabus Topic | Actual Implementation in VOTUM | Primary File(s) / Class | Educational Explanation & Design Rationale | Verification Method |
| :--- | :--- | :--- | :--- | :--- |
| **1. Variables, Data Types & Control Flow** | Use of primitives (`int`, `boolean`, `double`), reference types (`String`, `LocalDateTime`), `if-else` branching, and `for-each` loops. | [`Election.java`](../src/com/votum/model/Election.java), [`VotingService.java`](../src/com/votum/service/VotingService.java) | Boundary validation, election time checks, and status evaluation are governed by explicit conditional branching and robust type checks. | Automated domain suite (`VotumTestSuite.test4, test10, test21`) |
| **2. Arrays & Collections** | Single-dimensional arrays, `List<T>`, `ArrayList<T>`, `Map<K, V>`, `LinkedHashMap<K, V>`. | [`ElectionResult.java`](../src/com/votum/model/ElectionResult.java), [`FileManager.java`](../src/com/votum/storage/FileManager.java) | Storing and tallying candidate votes preserves insertion order with `LinkedHashMap`. Unmodifiable views protect internal collections. | Unit tests (`test12, test13, test14`) and code inspection |
| **3. Constructors & Method Overloading** | Default, overloaded, and parameterized constructors (`Candidate(id, name, dept, electionId)` and `Candidate(..., slogan)`). | [`Candidate.java`](../src/com/votum/model/Candidate.java), [`Voter.java`](../src/com/votum/model/Voter.java) | Allows flexible object initialization while enforcing invariant defaults (e.g., slogan defaults to `""`). | Unit tests (`test5, test6`) |
| **4. Encapsulation & Access Modifiers** | All class fields are `private`; public accessors enforce constraints. `final` applied to constants and immutable classes. | [`Election.java`](../src/com/votum/model/Election.java), [`User.java`](../src/com/votum/model/User.java) | Fields cannot be corrupted by external layers; access is controlled strictly through business methods. | Architecture check (zero direct field writes from external classes) |
| **5. Abstract Classes & Dynamic Dispatch** | `public abstract class User` with abstract method `public abstract String getDisplayName();` and `public abstract String getId();`. | [`User.java`](../src/com/votum/model/User.java), [`Admin.java`](../src/com/votum/model/Admin.java), [`Voter.java`](../src/com/votum/model/Voter.java) | Demonstrates abstraction: `User` cannot be instantiated directly. Calling `u.getDisplayName()` dynamically dispatches to either `Admin` or `Voter`. | Unit tests (`test1, test3`) |
| **6. Interfaces** | `Identifiable` (contract: `String getId()`) and `AuthenticatableUser` (contract: `getUsername()`, `verifyPassword()`, `getRole()`). | [`Identifiable.java`](../src/com/votum/model/Identifiable.java), [`AuthenticatableUser.java`](../src/com/votum/model/AuthenticatableUser.java) | Establishes decoupled contracts. All identifiable domain entities implement `Identifiable`; authenticatable actors implement `AuthenticatableUser`. | Compilation & type verification |
| **7. Inheritance (`extends` & `super`)** | `Admin extends User` and `Voter extends User`, chaining credentials via `super(username, password, role)`. | [`Admin.java`](../src/com/votum/model/Admin.java), [`Voter.java`](../src/com/votum/model/Voter.java) | Reuses authentication credentials and eliminates code duplication while permitting role-specific attributes. | Unit tests (`test1, test3`) |
| **8. Runtime Polymorphism** | `AuthenticationService.login()` returns polymorphic `User`. Swing displays rely on polymorphic method dispatch. | [`AuthenticationService.java`](../src/com/votum/service/AuthenticationService.java), [`LoginFrame.java`](../src/com/votum/ui/LoginFrame.java) | Caller interacts with base `User` reference; role-specific behavior executes at runtime. | Domain tests (`test1, test2, test3`) |
| **9. File I/O & Persistence** | Character streams using `BufferedReader`, `BufferedWriter`, `FileReader`, `FileWriter`, and `File` APIs. | [`FileManager.java`](../src/com/votum/storage/FileManager.java), [`AuditLogger.java`](../src/com/votum/storage/AuditLogger.java) | Stores semicolon-delimited records in `data/*.txt` and append-only audit trail in `data/audit.log`. Delimiters sanitized on write; malformed lines skipped on read. | Unit tests (`test15, test19, test20`) |
| **10. Exception Handling** | Custom checked exception hierarchy rooted at `VotumException` with 5 specific domain subclasses. | [`src/com/votum/exception/`](../src/com/votum/exception/) | Guarantees clean error propagation from storage/service layers to UI without crashing the application. | Exception tests (`test2, test8, test9, test10, test11`) |
| **11. Multithreading & Synchronization** | Managed background daemon thread (`LiveRefreshWorker` implementing `Runnable`) with cooperative cancellation (`volatile boolean running`). | [`AdminDashboard.java`](../src/com/votum/ui/AdminDashboard.java), [`VotingService.java`](../src/com/votum/service/VotingService.java) | Prevents UI freezing during live metric updates. Thread-safe file writes enforced via `synchronized` methods. | UI Smoke tests & thread inspection |
| **12. Garbage Collection & Memory Management** | Automatic memory management using normal object lifecycle; explicit listener/timer cleanup on window disposal. | [`UITheme.java`](../src/com/votum/ui/UITheme.java), [`AdminDashboard.java`](../src/com/votum/ui/AdminDashboard.java) | Prevents memory leaks by stopping background threads and timers when windows are closed (`dispose()`). | Window lifecycle smoke tests |

---

## 3. Academic Defense (Viva Reference Questions & Answers)

### Q1: Why did you make `User` an abstract class?
> **Answer:** In the VOTUM domain, there is no generic user. Every system participant is either an `Admin` (Election Officer) or a `Voter` (Student). Making `User` abstract prevents improper instantiation of a naked `User` while defining a common contract (`getDisplayName()`, `getId()`, `getUsername()`, `verifyPassword()`).

### Q2: How does VOTUM demonstrate polymorphism?
> **Answer:** Through both dynamic method dispatch and polymorphic variable assignments:
> 1. `AuthenticationService.login(username, password)` returns a reference of type `User`. The calling code in `LoginFrame` handles the returned object polymorphically before routing to the appropriate dashboard.
> 2. Calling `user.getDisplayName()` dynamically executes `Admin.getDisplayName()` or `Voter.getDisplayName()` based on the actual object created at runtime.

### Q3: What interfaces did you create and why are they justified?
> **Answer:** We created two domain-aligned interfaces:
> 1. `Identifiable`: Specifies `String getId()`. Implemented by all domain models (`User`, `Admin`, `Voter`, `Candidate`, `Election`, `Vote`) to allow uniform identity retrieval in storage and logging.
> 2. `AuthenticatableUser`: Specifies credentials verification (`getUsername()`, `verifyPassword()`, `getRole()`). Decouples the authentication engine from concrete user representations.

### Q4: How is multithreading implemented safely with Java Swing?
> **Answer:** Swing is single-threaded; all UI component mutations must occur on the Event Dispatch Thread (EDT). In `AdminDashboard`, live metric polling runs on a separate worker thread (`LiveRefreshWorker` implementing `Runnable`). When new data is calculated, the worker schedules UI component updates using `SwingUtilities.invokeLater()`. When the window is closed, the thread terminates cooperatively via a `volatile boolean running` flag and thread interruption.

### Q5: How does VOTUM prevent data corruption during concurrent operations?
> **Answer:** All file-mutating operations in `FileManager` and ballot validation in `VotingService` use the `synchronized` keyword. This guarantees mutually exclusive access to shared disk files and memory caches, preventing race conditions or interleaved writes.

