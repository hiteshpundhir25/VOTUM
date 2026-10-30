package com.votum.test;

import com.votum.exception.*;
import com.votum.model.*;
import com.votum.service.*;
import com.votum.storage.*;
import com.votum.ui.DateTimeInput;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Automated Verification Suite for Votum.
 *
 * Covers the eight election business rules, every exception path, persistence and
 * corrupt-store resilience, per-election vote scoping, the administrator create-election
 * window validation, and the non-auditing result preview used by the live dashboard.
 * Everything runs in an isolated directory without touching production data.
 */
public class VotumTestSuite {
    private static final String TEST_DATA_DIR = "data_test";
    private static int passedCount = 0;
    private static int failedCount = 0;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   VOTUM AUTOMATED VERIFICATION TEST SUITE       ");
        System.out.println("=================================================\n");

        cleanTestDirectory();

        try {
            test1_AdminLogin();
            test2_InvalidLogin();
            test3_VoterLogin();
            test4_ElectionCreation();
            test5_CandidateCreation();
            test6_VoterCreation();
            test7_ValidVote();
            test8_DuplicateVoteRejection();
            test9_InvalidCandidateRejection();
            test10_VotingBeforeStartTime();
            test11_VotingAfterEndTime();
            test12_VoteCounting();
            test13_WinnerCalculation();
            test14_TieCalculation();
            test15_DataPersistenceAndReload();
            test16_DeleteElection();
            test17_PerElectionVoting();
            test18_AdminUsernameCollision();
            test19_CorruptRecordResilience();
            test20_AuditLogPurgePrecision();
            test21_VotingWindowBoundaries();
            test22_ElectionWindowValidation();
            test23_CreateElectionLifecycle();
            test24_ResultPreviewDoesNotAudit();
            test25_AuditProtectionPasswordSecurity();
            test26_CandidateRemoval();
            test27_VoterEligibilityToggle();
            test28_VoterDeletion();
            test29_CandidateUpdate();
        } finally {
            cleanTestDirectory();
        }

        System.out.println("\n-------------------------------------------------");
        System.out.println("TEST SUMMARY: " + passedCount + " Passed, " + failedCount + " Failed.");
        if (failedCount == 0) {
            System.out.println("RESULT: ALL " + passedCount + " TESTS PASSED SUCCESSFULLY! [100% OK]");
        } else {
            System.out.println("RESULT: SOME TESTS FAILED.");
        }
        System.out.println("=================================================");

        if (failedCount > 0) {
            System.exit(1);
        }
    }

    private static void pass(String testName) {
        passedCount++;
        System.out.println("[PASS] " + testName);
    }

    private static void fail(String testName, String reason) {
        failedCount++;
        System.err.println("[FAIL] " + testName + " -> " + reason);
    }

    private static void cleanTestDirectory() {
        File dir = new File(TEST_DATA_DIR);
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
            dir.delete();
        }
    }

    private static TestContext createIsolatedContext() {
        cleanTestDirectory();
        FileManager fileManager = new FileManager(TEST_DATA_DIR);
        AuditLogger auditLogger = new AuditLogger(TEST_DATA_DIR + File.separator + "audit.log");
        ElectionService electionService = new ElectionService(fileManager, auditLogger);
        VotingService votingService = new VotingService(fileManager, auditLogger, electionService);
        ResultService resultService = new ResultService(fileManager, auditLogger, electionService);
        AuthenticationService authService = new AuthenticationService(fileManager, auditLogger);

        return new TestContext(fileManager, auditLogger, electionService, votingService, resultService, authService);
    }

    private static class TestContext {
        final FileManager fileManager;
        final AuditLogger auditLogger;
        final ElectionService electionService;
        final VotingService votingService;
        final ResultService resultService;
        final AuthenticationService authService;

        TestContext(FileManager fm, AuditLogger al, ElectionService es, VotingService vs, ResultService rs, AuthenticationService as) {
            this.fileManager = fm;
            this.auditLogger = al;
            this.electionService = es;
            this.votingService = vs;
            this.resultService = rs;
            this.authService = as;
        }
    }

    // -------------------------------------------------------------
    // Test 1: Admin Login
    // -------------------------------------------------------------
    private static void test1_AdminLogin() {
        TestContext ctx = createIsolatedContext();
        try {
            User user = ctx.authService.login("admin", "admin123");
            if (user instanceof Admin && user.isAdmin()) {
                pass("Test 1: Admin Login with valid credentials");
            } else {
                fail("Test 1: Admin Login", "Returned user is not an Admin instance");
            }
        } catch (Exception e) {
            fail("Test 1: Admin Login", e.getMessage());
        }
    }

    // -------------------------------------------------------------
    // Test 2: Invalid Login
    // -------------------------------------------------------------
    private static void test2_InvalidLogin() {
        TestContext ctx = createIsolatedContext();
        try {
            ctx.authService.login("admin", "wrongpassword");
            fail("Test 2: Invalid Login", "Should have thrown InvalidLoginException for wrong password");
        } catch (InvalidLoginException e) {
            assert e != null;
            pass("Test 2: Invalid Login correctly rejected");
        } catch (Exception e) {
            fail("Test 2: Invalid Login", "Unexpected exception type: " + e.getClass().getName());
        }
    }

    // -------------------------------------------------------------
    // Test 3: Voter Login
    // -------------------------------------------------------------
    private static void test3_VoterLogin() {
        TestContext ctx = createIsolatedContext();
        try {
            User user = ctx.authService.login("voter1", "voter123");
            if (user instanceof Voter && user.isVoter()) {
                pass("Test 3: Voter Login with valid credentials");
            } else {
                fail("Test 3: Voter Login", "Returned user is not a Voter instance");
            }
        } catch (Exception e) {
            fail("Test 3: Voter Login", e.getMessage());
        }
    }

    // -------------------------------------------------------------
    // Test 4: Election Creation & Validation
    // -------------------------------------------------------------
    private static void test4_ElectionCreation() {
        TestContext ctx = createIsolatedContext();
        try {
            LocalDateTime now = LocalDateTime.now();
            Election e = ctx.electionService.createElection("TEST-E01", "CSE Department Representative", "Desc",
                    now.minusHours(1), now.plusHours(5));

            if (e != null && "TEST-E01".equals(e.getElectionId()) && e.getStatus() == ElectionStatus.DRAFT) {
                // Test invalid election: end before start
                try {
                    ctx.electionService.createElection("BAD-E", "Bad", "Desc", now.plusHours(5), now.plusHours(1));
                    fail("Test 4: Election Creation", "Should reject end time before start time");
                    return;
                } catch (InvalidElectionException expected) {
                    assert expected != null;
                    // expected
                }
                pass("Test 4: Election Creation & Start/End time validation");
            } else {
                fail("Test 4: Election Creation", "Election not created properly");
            }
        } catch (Exception e) {
            fail("Test 4: Election Creation", e.getMessage());
        }
    }

    // -------------------------------------------------------------
    // Test 5: Candidate Creation
    // -------------------------------------------------------------
    private static void test5_CandidateCreation() {
        TestContext ctx = createIsolatedContext();
        try {
            Candidate c = ctx.electionService.addCandidate("TEST-C01", "Kavya Reddy", "CSE", "E001", "Better Labs");
            List<Candidate> list = ctx.electionService.getCandidatesForElection("E001");
            boolean found = list.stream().anyMatch(cand -> cand.getCandidateId().equals("TEST-C01"));
            if (c != null && found) {
                pass("Test 5: Candidate Registration");
            } else {
                fail("Test 5: Candidate Registration", "Candidate not found in election list");
            }
        } catch (Exception e) {
            fail("Test 5: Candidate Registration", e.getMessage());
        }
    }

    // -------------------------------------------------------------
    // Test 6: Voter Creation
    // -------------------------------------------------------------
    private static void test6_VoterCreation() {
        TestContext ctx = createIsolatedContext();
        try {
            Voter v = ctx.electionService.addVoter("TEST-V01", "Manish Joshi", "IT", "manish", "pass123", true);
            List<Voter> voters = ctx.electionService.getAllVoters();
            boolean found = voters.stream().anyMatch(voter -> voter.getVoterId().equals("TEST-V01"));
            if (v != null && found) {
                pass("Test 6: Voter Registration");
            } else {
                fail("Test 6: Voter Registration", "Voter not found in voters list");
            }
        } catch (Exception e) {
            fail("Test 6: Voter Registration", e.getMessage());
        }
    }

    // -------------------------------------------------------------
    // Test 7: Valid Vote Casting
    // -------------------------------------------------------------
    private static void test7_ValidVote() {
        TestContext ctx = createIsolatedContext();
        try {
            Vote vote = ctx.votingService.castVote("V001", "E001", "C001");
            if (vote != null && "E001".equals(vote.getElectionId()) && "C001".equals(vote.getCandidateId())) {
                if (ctx.votingService.hasVoterCastVote("V001", "E001")) {
                    pass("Test 7: Valid Vote casting and voter state updated");
                } else {
                    fail("Test 7: Valid Vote", "Voter hasVoted flag was not updated");
                }
            } else {
                fail("Test 7: Valid Vote", "Vote object incorrect");
            }
        } catch (Exception e) {
            fail("Test 7: Valid Vote", e.getMessage());
        }
    }

    // -------------------------------------------------------------
    // Test 8: Duplicate Vote Rejection (Single Vote rule)
    // -------------------------------------------------------------
    private static void test8_DuplicateVoteRejection() {
        TestContext ctx = createIsolatedContext();
        try {
            ctx.votingService.castVote("V001", "E001", "C001");
            // Attempt 2nd vote
            ctx.votingService.castVote("V001", "E001", "C002");
            fail("Test 8: Duplicate Vote", "Should have thrown DuplicateVoteException on second vote");
        } catch (DuplicateVoteException e) {
            assert e != null;
            pass("Test 8: Duplicate Vote correctly blocked with DuplicateVoteException");
        } catch (Exception e) {
            fail("Test 8: Duplicate Vote", "Wrong exception thrown: " + e.getClass().getName());
        }
    }

    // -------------------------------------------------------------
    // Test 9: Invalid Candidate Selection
    // -------------------------------------------------------------
    private static void test9_InvalidCandidateRejection() {
        TestContext ctx = createIsolatedContext();
        try {
            ctx.votingService.castVote("V002", "E001", "NON_EXISTENT_CANDIDATE");
            fail("Test 9: Invalid Candidate", "Should have thrown InvalidCandidateException");
        } catch (InvalidCandidateException e) {
            assert e != null;
            pass("Test 9: Invalid Candidate selection correctly rejected");
        } catch (Exception e) {
            fail("Test 9: Invalid Candidate", "Wrong exception thrown: " + e.getClass().getName());
        }
    }

    // -------------------------------------------------------------
    // Test 10: Voting Before Start Time
    // -------------------------------------------------------------
    private static void test10_VotingBeforeStartTime() {
        TestContext ctx = createIsolatedContext();
        try {
            LocalDateTime now = LocalDateTime.now();
            // Setup future election
            Election futureElection = new Election("FUT-01", "Future Election", "Desc",
                    now.plusDays(1), now.plusDays(2), ElectionStatus.OPEN);
            ctx.fileManager.saveOrUpdateElection(futureElection);
            ctx.fileManager.addCandidate(new Candidate("FC-01", "Future Cand", "CS", "FUT-01"));

            // Attempt voting right now (which is before start)
            ctx.votingService.castVote("V002", "FUT-01", "FC-01");
            fail("Test 10: Voting Before Start", "Should have rejected vote before start time");
        } catch (ElectionClosedException e) {
            assert e != null;
            pass("Test 10: Voting before start time rejected with ElectionClosedException");
        } catch (Exception e) {
            fail("Test 10: Voting Before Start", "Wrong exception: " + e.getClass().getName());
        }
    }

    // -------------------------------------------------------------
    // Test 11: Voting After End Time
    // -------------------------------------------------------------
    private static void test11_VotingAfterEndTime() {
        TestContext ctx = createIsolatedContext();
        try {
            LocalDateTime now = LocalDateTime.now();
            // Setup past election
            Election pastElection = new Election("PAST-01", "Past Election", "Desc",
                    now.minusDays(5), now.minusDays(1), ElectionStatus.OPEN);
            ctx.fileManager.saveOrUpdateElection(pastElection);
            ctx.fileManager.addCandidate(new Candidate("PC-01", "Past Cand", "CS", "PAST-01"));

            // Attempt voting right now (which is after end time)
            ctx.votingService.castVote("V002", "PAST-01", "PC-01");
            fail("Test 11: Voting After End", "Should have rejected vote after end time");
        } catch (ElectionClosedException e) {
            assert e != null;
            pass("Test 11: Voting after end time rejected with ElectionClosedException");
        } catch (Exception e) {
            fail("Test 11: Voting After End", "Wrong exception: " + e.getClass().getName());
        }
    }

    // -------------------------------------------------------------
    // Test 12: Vote Counting
    // -------------------------------------------------------------
    private static void test12_VoteCounting() {
        TestContext ctx = createIsolatedContext();
        try {
            ctx.votingService.castVote("V001", "E001", "C001");
            ctx.votingService.castVote("V002", "E001", "C002");
            ctx.votingService.castVote("V003", "E001", "C002");

            ElectionResult result = ctx.resultService.calculateResult("E001");
            if (result.getTotalVotes() == 3 &&
                result.getVotesFor("C001") == 1 &&
                result.getVotesFor("C002") == 2 &&
                result.getVotesFor("C003") == 0) {
                pass("Test 12: Vote Counting matches accurately");
            } else {
                fail("Test 12: Vote Counting", "Vote counts do not match expected totals");
            }
        } catch (Exception e) {
            fail("Test 12: Vote Counting", e.getMessage());
        }
    }

    // -------------------------------------------------------------
    // Test 13: Winner Calculation & Turnout
    // -------------------------------------------------------------
    private static void test13_WinnerCalculation() {
        TestContext ctx = createIsolatedContext();
        try {
            ctx.votingService.castVote("V001", "E001", "C002");
            ctx.votingService.castVote("V002", "E001", "C002");
            ctx.votingService.castVote("V003", "E001", "C001");

            ElectionResult result = ctx.resultService.calculateResult("E001");
            if (!result.isTie() && result.getWinningCandidates().size() == 1) {
                Candidate winner = result.getWinningCandidates().get(0);
                if ("C002".equals(winner.getCandidateId()) && result.getTurnoutPercentage() > 0) {
                    pass("Test 13: Winner Calculation & Turnout % correct");
                } else {
                    fail("Test 13: Winner Calculation", "Wrong winner identified: " + winner.getName());
                }
            } else {
                fail("Test 13: Winner Calculation", "Expected single winner, got: " + result.getWinnerSummary());
            }
        } catch (Exception e) {
            fail("Test 13: Winner Calculation", e.getMessage());
        }
    }

    // -------------------------------------------------------------
    // Test 14: Tie Calculation
    // -------------------------------------------------------------
    private static void test14_TieCalculation() {
        TestContext ctx = createIsolatedContext();
        try {
            ctx.votingService.castVote("V001", "E001", "C001");
            ctx.votingService.castVote("V002", "E001", "C002");

            ElectionResult result = ctx.resultService.calculateResult("E001");
            if (result.isTie() && result.getWinningCandidates().size() == 2) {
                pass("Test 14: Tie Detection correctly handles equal votes");
            } else {
                fail("Test 14: Tie Detection", "Failed to detect tie: " + result.getWinnerSummary());
            }
        } catch (Exception e) {
            fail("Test 14: Tie Detection", e.getMessage());
        }
    }

    // -------------------------------------------------------------
    // Test 15: Data Persistence & Reload
    // -------------------------------------------------------------
    private static void test15_DataPersistenceAndReload() {
        TestContext ctx = createIsolatedContext();
        try {
            ctx.votingService.castVote("V001", "E001", "C001");

            // Create a completely new instance pointing to same storage
            FileManager reloadedManager = new FileManager(TEST_DATA_DIR);
            List<Vote> persistedVotes = reloadedManager.loadVotes();
            List<Voter> persistedVoters = reloadedManager.loadVoters();

            boolean voteFound = persistedVotes.stream().anyMatch(v -> v.getVoterId().equals("V001") && v.getCandidateId().equals("C001"));
            boolean voterMarkedVoted = persistedVoters.stream().anyMatch(v -> v.getVoterId().equals("V001") && v.hasVoted());

            if (voteFound && voterMarkedVoted) {
                pass("Test 15: Data Persistence and Disk Reload verified");
            } else {
                fail("Test 15: Data Persistence", "Data was not persisted across reload");
            }
        } catch (Exception e) {
            fail("Test 15: Data Persistence", e.getMessage());
        }
    }

    // -------------------------------------------------------------
    // Test 16: Delete Election (Cascades candidates, votes, resets voter status)
    // -------------------------------------------------------------
    private static void test16_DeleteElection() {
        TestContext ctx = createIsolatedContext();
        try {
            // Cast a vote first
            ctx.votingService.castVote("V001", "E001", "C001");

            // Verify before deletion
            boolean voterVotedBefore = ctx.fileManager.loadVoters().stream()
                    .anyMatch(v -> v.getVoterId().equals("V001") && v.hasVoted());
            int candidatesBefore = ctx.electionService.getCandidatesForElection("E001").size();
            int votesBefore = ctx.fileManager.loadVotes().size();

            if (!voterVotedBefore || candidatesBefore == 0 || votesBefore == 0) {
                fail("Test 16: Delete Election", "Setup state invalid");
                return;
            }

            // Perform Delete
            boolean deleted = ctx.electionService.deleteElection("E001");
            if (!deleted) {
                fail("Test 16: Delete Election", "deleteElection returned false");
                return;
            }

            // Verify election removed
            Election e = ctx.electionService.getElection("E001");
            List<Candidate> candidatesAfter = ctx.electionService.getCandidatesForElection("E001");
            List<Vote> votesAfter = ctx.fileManager.loadVotes();
            boolean voterReset = ctx.fileManager.loadVoters().stream()
                    .anyMatch(v -> v.getVoterId().equals("V001") && !v.hasVoted());

            if (e == null && candidatesAfter.isEmpty() && votesAfter.isEmpty() && voterReset) {
                pass("Test 16: Delete Election cascading removal of candidates, votes, and reset voter status");
            } else {
                fail("Test 16: Delete Election", "Election or associated data was not completely cleaned up");
            }
        } catch (Exception e) {
            fail("Test 16: Delete Election", e.getMessage());
        }
    }

    // -------------------------------------------------------------
    // Test 17: Single-vote rule is scoped per election
    // -------------------------------------------------------------
    private static void test17_PerElectionVoting() {
        TestContext ctx = createIsolatedContext();
        try {
            LocalDateTime now = LocalDateTime.now();
            ctx.electionService.createElection("E002", "Hostel Council Election", "Second campus election",
                    now.minusHours(1), now.plusHours(2));
            ctx.electionService.openElection("E002");
            ctx.electionService.addCandidate("C101", "Meera Iyer", "CSE", "E002", "Louder Hostel Voice");

            // Same voter voting once in each of two different elections must be allowed
            ctx.votingService.castVote("V001", "E001", "C001");
            ctx.votingService.castVote("V001", "E002", "C101");

            boolean blockedInFirst = false;
            try {
                ctx.votingService.castVote("V001", "E001", "C002");
            } catch (DuplicateVoteException expected) {
                assert expected != null;
                blockedInFirst = true;
            }

            boolean blockedInSecond = false;
            try {
                ctx.votingService.castVote("V001", "E002", "C101");
            } catch (DuplicateVoteException expected) {
                assert expected != null;
                blockedInSecond = true;
            }

            boolean scopedCorrectly = ctx.votingService.hasVoterCastVote("V001", "E001")
                    && ctx.votingService.hasVoterCastVote("V001", "E002")
                    && !ctx.votingService.hasVoterCastVote("V002", "E001");

            if (blockedInFirst && blockedInSecond && scopedCorrectly) {
                pass("Test 17: Single-vote rule enforced per election (multi-election voting allowed)");
            } else {
                fail("Test 17: Per-Election Voting", "blockedInFirst=" + blockedInFirst
                        + ", blockedInSecond=" + blockedInSecond + ", scopedCorrectly=" + scopedCorrectly);
            }
        } catch (Exception e) {
            fail("Test 17: Per-Election Voting", e.getMessage());
        }
    }

    // -------------------------------------------------------------
    // Test 18: Voter username may not shadow an administrator account
    // -------------------------------------------------------------
    private static void test18_AdminUsernameCollision() {
        TestContext ctx = createIsolatedContext();
        try {
            boolean rejected = false;
            try {
                ctx.electionService.addVoter("V900", "Duplicate Officer", "CSE", "admin", "secret123", true);
            } catch (InvalidElectionException expected) {
                assert expected != null;
                rejected = true;
            }

            boolean notPersisted = ctx.fileManager.loadVoters().stream()
                    .noneMatch(v -> "admin".equalsIgnoreCase(v.getUsername()));

            if (rejected && notPersisted) {
                pass("Test 18: Voter username colliding with an admin account is rejected");
            } else {
                fail("Test 18: Admin Username Collision", "rejected=" + rejected + ", notPersisted=" + notPersisted);
            }
        } catch (Exception e) {
            fail("Test 18: Admin Username Collision", e.getMessage());
        }
    }

    // -------------------------------------------------------------
    // Test 19: Malformed records are skipped and delimiters are sanitized
    // -------------------------------------------------------------
    private static void test19_CorruptRecordResilience() {
        TestContext ctx = createIsolatedContext();
        try {
            appendLine(TEST_DATA_DIR + File.separator + "elections.txt",
                    "E-BAD;Broken Election;Desc;not-a-date;also-bad;NOT_A_STATUS");
            appendLine(TEST_DATA_DIR + File.separator + "elections.txt", "E-SHORT;Only;Three");

            List<Election> electionsAfter = ctx.fileManager.loadElections();
            boolean healthyRecordSurvived = electionsAfter.stream()
                    .anyMatch(e -> "E001".equals(e.getElectionId()));
            boolean corruptRecordsSkipped = electionsAfter.stream()
                    .noneMatch(e -> e.getElectionId().startsWith("E-BAD") || e.getElectionId().startsWith("E-SHORT"));

            // A ';' inside free text must never shift the remaining columns of a record
            ctx.electionService.addCandidate("C900", "Semicolon; Tester", "CSE", "E001", "Win; Win");
            Candidate roundTripped = ctx.fileManager.loadCandidates().stream()
                    .filter(c -> "C900".equals(c.getCandidateId()))
                    .findFirst().orElse(null);

            boolean columnsIntact = roundTripped != null
                    && "CSE".equals(roundTripped.getDepartment())
                    && "E001".equals(roundTripped.getElectionId())
                    && roundTripped.getName().indexOf(';') < 0
                    && roundTripped.getSlogan().indexOf(';') < 0;

            if (healthyRecordSurvived && corruptRecordsSkipped && columnsIntact) {
                pass("Test 19: Corrupt records skipped, healthy data preserved, delimiter-safe round trip");
            } else {
                fail("Test 19: Corrupt Record Resilience", "healthyRecordSurvived=" + healthyRecordSurvived
                        + ", corruptRecordsSkipped=" + corruptRecordsSkipped + ", columnsIntact=" + columnsIntact);
            }
        } catch (Exception e) {
            fail("Test 19: Corrupt Record Resilience", e.getMessage());
        }
    }

    // -------------------------------------------------------------
    // Test 20: Log purge matches whole identifiers only
    // -------------------------------------------------------------
    private static void test20_AuditLogPurgePrecision() {
        TestContext ctx = createIsolatedContext();
        try {
            LocalDateTime now = LocalDateTime.now();
            // 'E0010' contains 'E001' as a substring and must survive the E001 purge
            ctx.electionService.createElection("E0010", "Parallel Campus Election", "Desc",
                    now.minusHours(1), now.plusHours(1));
            ctx.auditLogger.log("Parallel election notice", "ID: E0010 | keep this entry");

            ctx.electionService.deleteElection("E001");
            List<String> logsAfter = ctx.auditLogger.readAllLogs();

            boolean e001HistoryPurged = logsAfter.stream()
                    .noneMatch(l -> l.contains("Election created") && containsToken(l, "E001"));
            boolean e0010HistoryKept = logsAfter.stream().anyMatch(l -> containsToken(l, "E0010"));
            boolean deletionRecorded = logsAfter.stream()
                    .anyMatch(l -> l.contains("Election deleted") && containsToken(l, "E001"));

            if (e001HistoryPurged && e0010HistoryKept && deletionRecorded) {
                pass("Test 20: Audit purge removes only whole-identifier matches");
            } else {
                fail("Test 20: Audit Purge Precision", "e001HistoryPurged=" + e001HistoryPurged
                        + ", e0010HistoryKept=" + e0010HistoryKept + ", deletionRecorded=" + deletionRecorded);
            }
        } catch (Exception e) {
            fail("Test 20: Audit Purge Precision", e.getMessage());
        }
    }

    // -------------------------------------------------------------
    // Test 21: Voting window boundaries (inclusive ends, strict outside)
    // -------------------------------------------------------------
    private static void test21_VotingWindowBoundaries() {
        TestContext ctx = createIsolatedContext();
        try {
            LocalDateTime start = LocalDateTime.of(2026, 3, 1, 9, 0);
            LocalDateTime end = start.plusHours(2);

            ctx.electionService.createElection("WIN-01", "Boundary Election", "Desc", start, end);
            ctx.electionService.openElection("WIN-01");
            ctx.electionService.addCandidate("WC-01", "Boundary Candidate", "CSE", "WIN-01", "On Time");

            // Exactly at the opening instant the ballot is accepted
            ctx.votingService.castVoteAt("V001", "WIN-01", "WC-01", start);
            // Exactly at the closing instant the ballot is still accepted
            ctx.votingService.castVoteAt("V002", "WIN-01", "WC-01", end);

            boolean tooLateRejected = false;
            try {
                ctx.votingService.castVoteAt("V003", "WIN-01", "WC-01", end.plusSeconds(1));
            } catch (ElectionClosedException expected) {
                assert expected != null;
                tooLateRejected = true;
            }

            boolean tooEarlyRejected = false;
            try {
                ctx.votingService.castVoteAt("V003", "WIN-01", "WC-01", start.minusSeconds(1));
            } catch (ElectionClosedException expected) {
                assert expected != null;
                tooEarlyRejected = true;
            }

            int recorded = ctx.resultService.calculateResult("WIN-01").getTotalVotes();

            if (tooLateRejected && tooEarlyRejected && recorded == 2) {
                pass("Test 21: Voting window boundaries enforced exactly (2 ballots at the edges)");
            } else {
                fail("Test 21: Voting Window Boundaries", "tooLateRejected=" + tooLateRejected
                        + ", tooEarlyRejected=" + tooEarlyRejected + ", recorded=" + recorded);
            }
        } catch (Exception e) {
            fail("Test 21: Voting Window Boundaries", e.getMessage());
        }
    }

    // -------------------------------------------------------------
    // Test 22: Create-election window validation (shared with the admin form)
    // -------------------------------------------------------------
    private static void test22_ElectionWindowValidation() {
        try {
            boolean validAccepted = DateTimeInput.validateWindow("2026-03-01 09:00", "2026-03-01 18:00") == null;
            boolean blanksRejected = DateTimeInput.validateWindow("", "2026-03-01 18:00") != null
                    && DateTimeInput.validateWindow("2026-03-01 09:00", "   ") != null;
            boolean malformedRejected = DateTimeInput.validateWindow("01-03-2026 09:00", "2026-03-01 18:00") != null;
            boolean equalRejected = DateTimeInput.validateWindow("2026-03-01 09:00", "2026-03-01 09:00") != null;
            boolean reversedRejected = DateTimeInput.validateWindow("2026-03-01 18:00", "2026-03-01 09:00") != null;
            boolean parsing = DateTimeInput.parse("2026-03-01 09:00") != null
                    && DateTimeInput.parse("not a date") == null
                    && DateTimeInput.parse("") == null;

            LocalDateTime now = LocalDateTime.now();
            boolean pastFlag = DateTimeInput.isEntirelyInThePast(now.minusDays(2), now.minusDays(1), now);
            boolean futureFlag = DateTimeInput.isEntirelyInTheFuture(now.plusDays(1), now.plusDays(2), now);
            boolean currentNotPast = !DateTimeInput.isEntirelyInThePast(now.minusHours(1), now.plusHours(1), now);

            if (validAccepted && blanksRejected && malformedRejected && equalRejected
                    && reversedRejected && parsing && pastFlag && futureFlag && currentNotPast) {
                pass("Test 22: Create-election window validation mirrors the service rules exactly");
            } else {
                fail("Test 22: Election Window Validation", "validAccepted=" + validAccepted
                        + ", blanksRejected=" + blanksRejected + ", malformedRejected=" + malformedRejected
                        + ", equalRejected=" + equalRejected + ", reversedRejected=" + reversedRejected
                        + ", parsing=" + parsing + ", pastFlag=" + pastFlag
                        + ", futureFlag=" + futureFlag + ", currentNotPast=" + currentNotPast);
            }
        } catch (Exception e) {
            fail("Test 22: Election Window Validation", e.getMessage());
        }
    }

    // -------------------------------------------------------------
    // Test 23: Full create -> open -> vote -> close lifecycle of a new election
    // -------------------------------------------------------------
    private static void test23_CreateElectionLifecycle() {
        TestContext ctx = createIsolatedContext();
        try {
            LocalDateTime now = LocalDateTime.now();

            // Exactly the call the administrator form makes
            Election created = ctx.electionService.createElection("E002", "Hostel Council Election",
                    "Second campus election", now.minusHours(1), now.plusHours(2));
            boolean createdAsDraft = created.getStatus() == ElectionStatus.DRAFT
                    && ctx.electionService.getElection("E002") != null;

            boolean duplicateRejected = false;
            try {
                ctx.electionService.createElection("E002", "Duplicate Title", "Dup", now, now.plusHours(1));
            } catch (InvalidElectionException expected) {
                assert expected != null;
                duplicateRejected = true;
            }

            ctx.electionService.addCandidate("C101", "Meera Iyer", "CSE", "E002", "Louder Hostel Voice");
            ctx.electionService.openElection("E002");
            ctx.votingService.castVote("V002", "E002", "C101");
            ctx.electionService.closeElection("E002");

            boolean closedRejectsVotes = false;
            try {
                ctx.votingService.castVote("V003", "E002", "C101");
            } catch (ElectionClosedException expected) {
                assert expected != null;
                closedRejectsVotes = true;
            }

            boolean tallyCorrect = ctx.resultService.calculateResult("E002").getTotalVotes() == 1;
            boolean rosterScoped = ctx.electionService.getCandidatesForElection("E002").size() == 1;

            if (createdAsDraft && duplicateRejected && closedRejectsVotes && tallyCorrect && rosterScoped) {
                pass("Test 23: New election lifecycle (DRAFT -> OPEN -> ballot -> CLOSED) works end to end");
            } else {
                fail("Test 23: Create Election Lifecycle", "createdAsDraft=" + createdAsDraft
                        + ", duplicateRejected=" + duplicateRejected + ", closedRejectsVotes=" + closedRejectsVotes
                        + ", tallyCorrect=" + tallyCorrect + ", rosterScoped=" + rosterScoped);
            }
        } catch (Exception e) {
            fail("Test 23: Create Election Lifecycle", e.getMessage());
        }
    }

    // -------------------------------------------------------------
    // Test 24: Live dashboard previews tally without flooding the audit trail
    // -------------------------------------------------------------
    private static void test24_ResultPreviewDoesNotAudit() {
        TestContext ctx = createIsolatedContext();
        try {
            ctx.votingService.castVote("V001", "E001", "C001");
            ctx.votingService.castVote("V002", "E001", "C002");

            int logsBefore = ctx.auditLogger.readAllLogs().size();

            ElectionResult preview = ctx.resultService.previewResult("E001");
            int logsAfterPreview = ctx.auditLogger.readAllLogs().size();

            ElectionResult official = ctx.resultService.calculateResult("E001");
            int logsAfterOfficial = ctx.auditLogger.readAllLogs().size();

            boolean sameNumbers = preview.getTotalVotes() == official.getTotalVotes()
                    && preview.getVotesFor("C001") == official.getVotesFor("C001")
                    && preview.getVotesFor("C002") == official.getVotesFor("C002")
                    && preview.getTurnoutPercentage() == official.getTurnoutPercentage();
            boolean previewIsSilent = logsAfterPreview == logsBefore;
            boolean officialIsAudited = logsAfterOfficial > logsAfterPreview;

            // The tally entry must state the outcome once, e.g. "Total Votes: 2 | Winner: …"
            String tallyEntry = ctx.auditLogger.readAllLogs().stream()
                    .filter(line -> line.contains("Results generated"))
                    .reduce((first, second) -> second)
                    .orElse("");
            boolean tallyEntryReadable = tallyEntry.contains("Total Votes: 2")
                    && !tallyEntry.contains("Winner: Winner:")
                    && (tallyEntry.contains("Winner: ") || tallyEntry.contains("TIE between: "));

            if (sameNumbers && previewIsSilent && officialIsAudited && tallyEntryReadable) {
                pass("Test 24: previewResult matches calculateResult, leaves the audit trail untouched, and audits a readable tally");
            } else {
                fail("Test 24: Result Preview", "sameNumbers=" + sameNumbers
                        + ", previewIsSilent=" + previewIsSilent + ", officialIsAudited=" + officialIsAudited
                        + ", tallyEntryReadable=" + tallyEntryReadable + ", tallyEntry=['" + tallyEntry + "']");
            }
        } catch (Exception e) {
            fail("Test 24: Result Preview", e.getMessage());
        }
    }

    // -------------------------------------------------------------
    // Test 25: Audit Protection Password setup, SHA-256 verification, and persistence
    // -------------------------------------------------------------
    private static void test25_AuditProtectionPasswordSecurity() {
        TestContext ctx = createIsolatedContext();
        try {
            // Initially, no password should be configured
            boolean initialHasPwd = ctx.fileManager.hasAuditProtectionPassword();
            boolean initialVerifyRandom = ctx.fileManager.verifyAuditProtectionPassword("any123");

            // Empty/null password rejection
            boolean rejectEmpty = !ctx.fileManager.setAuditProtectionPassword("");
            boolean rejectNull = !ctx.fileManager.setAuditProtectionPassword(null);

            // Set valid password
            String plainPassword = "AdminSecurePass!99";
            boolean setOk = ctx.fileManager.setAuditProtectionPassword(plainPassword);

            // Now password should be reported as set
            boolean hasPwdAfter = ctx.fileManager.hasAuditProtectionPassword();

            // Correct password verifies successfully
            boolean verifyCorrect = ctx.fileManager.verifyAuditProtectionPassword(plainPassword);

            // Incorrect password is strictly rejected
            boolean verifyWrong = !ctx.fileManager.verifyAuditProtectionPassword("WrongPassword123");
            boolean verifyCase = !ctx.fileManager.verifyAuditProtectionPassword(plainPassword.toLowerCase(Locale.ROOT));

            // Persistence check: reload fresh FileManager instance pointing to same directory
            FileManager reloadedManager = new FileManager(TEST_DATA_DIR);
            boolean persistsHasPwd = reloadedManager.hasAuditProtectionPassword();
            boolean persistsVerify = reloadedManager.verifyAuditProtectionPassword(plainPassword);
            boolean persistsRejectWrong = !reloadedManager.verifyAuditProtectionPassword("WrongPassword123");

            // Clear password check
            boolean cleared = reloadedManager.clearAuditProtectionPassword();
            boolean hasPwdAfterClear = reloadedManager.hasAuditProtectionPassword();

            if (!initialHasPwd && !initialVerifyRandom && rejectEmpty && rejectNull
                    && setOk && hasPwdAfter && verifyCorrect && verifyWrong && verifyCase
                    && persistsHasPwd && persistsVerify && persistsRejectWrong
                    && cleared && !hasPwdAfterClear) {
                pass("Test 25: Audit protection password setup, SHA-256 digest hashing, and persistence verified");
            } else {
                fail("Test 25: Audit Protection Password",
                        "initialHasPwd=" + initialHasPwd + ", setOk=" + setOk + ", hasPwdAfter=" + hasPwdAfter
                        + ", verifyCorrect=" + verifyCorrect + ", verifyWrong=" + verifyWrong
                        + ", persistsVerify=" + persistsVerify + ", cleared=" + cleared);
            }
        } catch (Exception e) {
            fail("Test 25: Audit Protection Password", e.getMessage());
        }
    }

    // -------------------------------------------------------------
    // Test 26: Candidate removal cascades ballot purge and audits removal
    // -------------------------------------------------------------
    private static void test26_CandidateRemoval() {
        TestContext ctx = createIsolatedContext();
        try {
            // Register a candidate C999
            ctx.electionService.addCandidate("C999", "Candidate To Remove", "CSE", "E001", "Short term campaign");
            boolean candidateExistsBefore = ctx.fileManager.loadCandidates().stream()
                    .anyMatch(c -> "C999".equals(c.getCandidateId()));

            // Cast a vote for C999 from V001
            ctx.votingService.castVote("V001", "E001", "C999");
            boolean voteRecorded = ctx.fileManager.loadVotes().stream()
                    .anyMatch(v -> "C999".equals(v.getCandidateId()));

            // Remove candidate C999
            boolean removed = ctx.electionService.removeCandidate("C999");

            // Candidate should be gone
            boolean candidateGone = ctx.fileManager.loadCandidates().stream()
                    .noneMatch(c -> "C999".equals(c.getCandidateId()));

            // Associated ballot for C999 should be purged
            boolean votePurged = ctx.fileManager.loadVotes().stream()
                    .noneMatch(v -> "C999".equals(v.getCandidateId()));

            // Affected voter V001 should have hasVoted reset
            Voter v001 = ctx.fileManager.loadVoters().stream()
                    .filter(v -> "V001".equals(v.getVoterId()))
                    .findFirst().orElse(null);
            boolean voterReset = v001 != null && !v001.hasVoted() && "".equals(v001.getVotedElectionId());

            // Audit log should contain entry
            boolean audited = ctx.auditLogger.readAllLogs().stream()
                    .anyMatch(l -> l.contains("Candidate removed") && l.contains("C999"));

            if (candidateExistsBefore && voteRecorded && removed && candidateGone && votePurged && voterReset && audited) {
                pass("Test 26: Candidate removal cascades ballot purge, resets voter state, and logs audit record");
            } else {
                fail("Test 26: Candidate Removal", "removed=" + removed + ", candidateGone=" + candidateGone
                        + ", votePurged=" + votePurged + ", voterReset=" + voterReset + ", audited=" + audited);
            }
        } catch (Exception e) {
            fail("Test 26: Candidate Removal", e.getMessage());
        }
    }

    // -------------------------------------------------------------
    // Test 27: Voter eligibility toggling (Eligible <-> Ineligible)
    // -------------------------------------------------------------
    private static void test27_VoterEligibilityToggle() {
        TestContext ctx = createIsolatedContext();
        try {
            // V005 starts as Ineligible in baseline
            Voter v005Initial = ctx.fileManager.loadVoters().stream()
                    .filter(v -> "V005".equals(v.getVoterId()))
                    .findFirst().orElse(null);
            boolean startsIneligible = v005Initial != null && !v005Initial.isEligible();

            // Toggle V005 to ELIGIBLE
            boolean markEligibleOk = ctx.electionService.setVoterEligibility("V005", true);
            Voter v005AfterEligible = ctx.fileManager.loadVoters().stream()
                    .filter(v -> "V005".equals(v.getVoterId()))
                    .findFirst().orElse(null);
            boolean isNowEligible = v005AfterEligible != null && v005AfterEligible.isEligible();

            // Toggle V001 (eligible) to INELIGIBLE
            boolean markIneligibleOk = ctx.electionService.setVoterEligibility("V001", false);
            Voter v001AfterIneligible = ctx.fileManager.loadVoters().stream()
                    .filter(v -> "V001".equals(v.getVoterId()))
                    .findFirst().orElse(null);
            boolean isNowIneligible = v001AfterIneligible != null && !v001AfterIneligible.isEligible();

            // Verify Ineligible voter cannot vote
            boolean voteRejected = false;
            try {
                ctx.votingService.castVote("V001", "E001", "C001");
            } catch (com.votum.exception.VotumException expected) {
                assert expected != null;
                voteRejected = true;
            }

            // Verify audit logging
            boolean auditedEligible = ctx.auditLogger.readAllLogs().stream()
                    .anyMatch(l -> l.contains("Voter eligibility updated") && l.contains("V005") && l.contains("true"));
            boolean auditedIneligible = ctx.auditLogger.readAllLogs().stream()
                    .anyMatch(l -> l.contains("Voter eligibility updated") && l.contains("V001") && l.contains("false"));

            if (startsIneligible && markEligibleOk && isNowEligible && markIneligibleOk && isNowIneligible
                    && voteRejected && auditedEligible && auditedIneligible) {
                pass("Test 27: Voter eligibility toggle updates status, enforces ballot permission, and records audit trail");
            } else {
                fail("Test 27: Voter Eligibility Toggle", "startsIneligible=" + startsIneligible
                        + ", markEligibleOk=" + markEligibleOk + ", isNowEligible=" + isNowEligible
                        + ", voteRejected=" + voteRejected);
            }
        } catch (Exception e) {
            fail("Test 27: Voter Eligibility Toggle", e.getMessage());
        }
    }

    // -------------------------------------------------------------
    // Test 28: Voter record deletion cascades ballot purge and audits action
    // -------------------------------------------------------------
    private static void test28_VoterDeletion() {
        TestContext ctx = createIsolatedContext();
        try {
            // Add a temporary voter V999
            ctx.electionService.addVoter("V999", "Temp Student", "Physics", "voter999", "voter123", true);
            boolean voterExists = ctx.fileManager.loadVoters().stream()
                    .anyMatch(v -> "V999".equals(v.getVoterId()));

            // Cast ballot from V999
            ctx.votingService.castVote("V999", "E001", "C001");
            boolean ballotExists = ctx.fileManager.loadVotes().stream()
                    .anyMatch(v -> "V999".equals(v.getVoterId()));

            // Delete voter V999
            boolean deleted = ctx.electionService.deleteVoter("V999");

            // Verify V999 is gone from voter list
            boolean voterGone = ctx.fileManager.loadVoters().stream()
                    .noneMatch(v -> "V999".equals(v.getVoterId()));

            // Verify V999's ballot was purged
            boolean ballotPurged = ctx.fileManager.loadVotes().stream()
                    .noneMatch(v -> "V999".equals(v.getVoterId()));

            // Verify audit trail
            boolean audited = ctx.auditLogger.readAllLogs().stream()
                    .anyMatch(l -> l.contains("Voter record deleted") && l.contains("V999"));

            if (voterExists && ballotExists && deleted && voterGone && ballotPurged && audited) {
                pass("Test 28: Voter deletion permanently purges record, cascades ballot removal, and logs audit record");
            } else {
                fail("Test 28: Voter Deletion", "deleted=" + deleted + ", voterGone=" + voterGone
                        + ", ballotPurged=" + ballotPurged + ", audited=" + audited);
            }
        } catch (Exception e) {
            fail("Test 28: Voter Deletion", e.getMessage());
        }
    }

    // -------------------------------------------------------------
    // Test 29: Candidate profile update and audit logging
    // -------------------------------------------------------------
    private static void test29_CandidateUpdate() {
        TestContext ctx = createIsolatedContext();
        try {
            // Register a candidate C888
            ctx.electionService.addCandidate("C888", "Initial Candidate Name", "Mech", "E001", "Initial Slogan");

            // Update candidate details
            Candidate updated = ctx.electionService.updateCandidate("C888", "Revised Candidate Name", "Aero", "E001", "Revised Visionary Slogan");

            // Verify memory object
            boolean memoryMatch = "Revised Candidate Name".equals(updated.getName())
                    && "Aero".equals(updated.getDepartment())
                    && "Revised Visionary Slogan".equals(updated.getSlogan());

            // Verify disk persistence
            Candidate loaded = ctx.fileManager.loadCandidates().stream()
                    .filter(c -> "C888".equals(c.getCandidateId()))
                    .findFirst().orElse(null);

            boolean diskMatch = loaded != null
                    && "Revised Candidate Name".equals(loaded.getName())
                    && "Aero".equals(loaded.getDepartment())
                    && "Revised Visionary Slogan".equals(loaded.getSlogan());

            // Verify audit logging
            boolean audited = ctx.auditLogger.readAllLogs().stream()
                    .anyMatch(l -> l.contains("Candidate updated") && l.contains("C888") && l.contains("Revised Candidate Name"));

            // Verify validation prevents invalid updates
            boolean blankNameRejected = false;
            try {
                ctx.electionService.updateCandidate("C888", "", "Aero", "E001", "Slogan");
            } catch (InvalidElectionException expected) {
                assert expected != null;
                blankNameRejected = true;
            }

            if (memoryMatch && diskMatch && audited && blankNameRejected) {
                pass("Test 29: Candidate profile update modifies fields, persists to disk, and records audit record");
            } else {
                fail("Test 29: Candidate Update", "memoryMatch=" + memoryMatch + ", diskMatch=" + diskMatch
                        + ", audited=" + audited + ", blankNameRejected=" + blankNameRejected);
            }
        } catch (Exception e) {
            fail("Test 29: Candidate Update", e.getMessage());
        }
    }

    /**
     * Appends a raw line to a persisted data file (used to simulate corrupted storage).
     */
    private static void appendLine(String filePath, String line) {
        try (FileWriter writer = new FileWriter(filePath, true)) {
            writer.write(line);
            writer.write(System.lineSeparator());
        } catch (IOException e) {
            fail("Test helper", "Unable to append test line: " + e.getMessage());
        }
    }

    /**
     * Token comparison mirroring the audit log purge semantics.
     */
    private static boolean containsToken(String text, String token) {
        for (String part : text.split("[^A-Za-z0-9_-]+")) {
            if (part.equalsIgnoreCase(token)) {
                return true;
            }
        }
        return false;
    }
}
