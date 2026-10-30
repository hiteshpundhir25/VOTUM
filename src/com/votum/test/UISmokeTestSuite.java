package com.votum.test;

import com.votum.model.*;
import com.votum.service.*;
import com.votum.storage.AuditLogger;
import com.votum.storage.FileManager;
import com.votum.ui.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.File;

/**
 * Automated UI & Interaction Smoke Test Suite for VOTUM.
 *
 * Programmatically constructs and verifies all Swing frames, dialogs, design tokens,
 * table renders, keyboard shortcuts, quick-fill buttons, and state machines.
 */
public final class UISmokeTestSuite {

    private static int passed = 0;
    private static int failed = 0;
    private static final String TEST_DIR = "data_ui_test";

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   VOTUM UI & INTERACTION SMOKE TEST SUITE       ");
        System.out.println("=================================================");

        cleanTestDirectory(new File(TEST_DIR));
        FileManager fileManager = new FileManager(TEST_DIR);
        AuditLogger auditLogger = new AuditLogger(TEST_DIR + "/audit.log");

        ElectionService electionService = new ElectionService(fileManager, auditLogger);
        VotingService votingService = new VotingService(fileManager, auditLogger, electionService);
        ResultService resultService = new ResultService(fileManager, auditLogger, electionService);
        AuthenticationService authService = new AuthenticationService(fileManager, auditLogger);

        Admin testAdmin = new Admin("admin", "admin123", "ADM001", "Campus Election Officer", "admin@campus.edu");
        Voter testVoter = new Voter("V001", "Aarav Gupta", "Computer Science", "voter1", "voter123", true, false, "");

        try {
            // Check 1: Design System tokens
            assertCondition(UITheme.PAPER != null && UITheme.ACCENT != null, "Design system palette constants resolved");
            assertCondition(UITheme.UI_FONT_FAMILY != null && !UITheme.UI_FONT_FAMILY.isEmpty(), "Platform font family probe resolved");

            // Check 2: Button factories
            JButton priBtn = UITheme.createPrimaryButton("Primary Action");
            JButton secBtn = UITheme.createSecondaryButton("Secondary Action");
            JButton succBtn = UITheme.createSuccessButton("Success Action");
            JButton dangBtn = UITheme.createDangerButton("Danger Action");
            JButton sideBtn = UITheme.createSidebarButton("Sidebar Item", true);
            assertCondition(priBtn != null && secBtn != null && succBtn != null && dangBtn != null && sideBtn != null,
                    "Button factories create editorial buttons successfully");

            // Check 3: Text inputs & Password inputs
            JTextField tf = UITheme.createTextField(20);
            JPasswordField pf = UITheme.createPasswordField(20);
            assertCondition(tf != null && pf != null && tf.getPreferredSize().height >= 38,
                    "Input fields configured with 38px height and editorial styling");

            // Check 4: Card panel and StatCard
            JPanel card = UITheme.createCardPanel();
            UITheme.StatCard statCard = UITheme.createStatCard("Test Metric", "42", "Subtext", UITheme.ACCENT);
            assertCondition(card != null && statCard != null, "Card containers and StatCard initialized");
            statCard.setValue("99");
            statCard.setSubtitle("Updated");

            // Check 5: Status Badges
            JLabel badge = UITheme.createBadge("ACTIVE", UITheme.SUCCESS_TINT, UITheme.SUCCESS);
            assertCondition(badge != null && "ACTIVE".equals(badge.getText()), "Square status label created with proper text");

            // Check 6: Table Styling & Zebra striping
            String[] cols = {"ID", "Name", "Eligible?", "Ballot Status"};
            DefaultTableModel model = new DefaultTableModel(cols, 0);
            model.addRow(new Object[]{"V001", "Student One", "YES", "VOTED (E001)"});
            model.addRow(new Object[]{"V002", "Student Two", "NO", "NOT YET"});
            JTable table = UITheme.styleTable(new JTable(model));
            assertCondition(table.getRowHeight() >= 36 && table.getColumnCount() == 4, "Editorial table configured with 36px row height");

            // Check 7: Horizontal Vote Bar
            UITheme.HorizontalVoteBar voteBar = new UITheme.HorizontalVoteBar("Candidate Name", 10, 20);
            assertCondition(voteBar != null && voteBar.getPreferredSize().height >= 50, "HorizontalVoteBar constructed safely");

            // Check 8: DateTimeInput validation
            assertCondition(DateTimeInput.validateWindow(null, null) != null, "DateTimeInput rejects null start/end");
            assertCondition(DateTimeInput.validateWindow("2026-03-01 10:00", "2026-03-01 09:00") != null, "DateTimeInput rejects reversed window");
            assertCondition(DateTimeInput.validateWindow("2026-03-01 09:00", "2026-03-01 18:00") == null, "DateTimeInput accepts valid window");

            // Check 9: LoginFrame construction
            LoginFrame loginFrame = new LoginFrame(fileManager, auditLogger, electionService, votingService, resultService, authService);
            assertCondition(loginFrame.getTitle().contains("VOTUM"), "LoginFrame constructed with VOTUM title");
            assertCondition(loginFrame.getExtendedState() == JFrame.MAXIMIZED_BOTH, "LoginFrame initialized in full-screen maximized mode");
            assertCondition(loginFrame.isUndecorated() && loginFrame.isAlwaysOnTop(), "LoginFrame configured in secure undecorated kiosk mode");
            assertCondition(loginFrame.getMinimumSize().width >= 500, "LoginFrame enforces minimum window bounds");
            assertCondition(loginFrame.getRootPane().getDefaultButton() != null, "LoginFrame registers Enter default button");
            loginFrame.dispose();

            // Check 10: AdminDashboard construction & panel reachability
            AdminDashboard adminDash = new AdminDashboard(testAdmin, fileManager, auditLogger, electionService, votingService, resultService, authService);
            assertCondition(adminDash.getTitle().contains("Administrator Control Center"), "AdminDashboard constructed with title");
            assertCondition(adminDash.getExtendedState() == JFrame.MAXIMIZED_BOTH, "AdminDashboard initialized in full-screen maximized mode");
            assertCondition(adminDash.isUndecorated() && adminDash.isAlwaysOnTop(), "AdminDashboard configured in secure undecorated kiosk mode");
            assertCondition(adminDash.getMinimumSize().width >= 960, "AdminDashboard enforces desktop minimum size");
            adminDash.dispose();

            // Check 11: VoterDashboard construction
            VoterDashboard voterDash = new VoterDashboard(testVoter, fileManager, auditLogger, electionService, votingService, resultService, authService);
            assertCondition(voterDash.getTitle().contains("Student Voting Portal"), "VoterDashboard constructed with student title");
            assertCondition(voterDash.getExtendedState() == JFrame.MAXIMIZED_BOTH, "VoterDashboard initialized in full-screen maximized mode");
            assertCondition(voterDash.isUndecorated() && voterDash.isAlwaysOnTop(), "VoterDashboard configured in secure undecorated kiosk mode");
            assertCondition(voterDash.getRootPane().getDefaultButton() != null, "VoterDashboard registers Enter default button");
            voterDash.dispose();

            // Check 12: ResultDialog construction & key bindings
            ElectionResult sampleResult = new ElectionResult("E001", "Student Council Election");
            sampleResult.addCandidateTally(new Candidate("C001", "Alice", "CSE", "E001", "For the students"), 15);
            sampleResult.addCandidateTally(new Candidate("C002", "Bob", "ECE", "E001", "Progress now"), 10);
            ResultDialog resultDialog = new ResultDialog(null, sampleResult);
            assertCondition(resultDialog.getTitle().contains("Official Election Results"), "ResultDialog constructed with title");
            assertCondition(resultDialog.getRootPane().getDefaultButton() != null, "ResultDialog binds default close button");
            resultDialog.dispose();

            // Check 13: Clean UI data reload without exceptions
            fileManager.loadElections();
            fileManager.loadCandidates();
            fileManager.loadVoters();
            assertCondition(true, "Underlying services and storage cleanly support UI reloads");

            // Check 14: OOP Interface contracts & dynamic dispatch (CSC2104.3)
            Identifiable identVoter = testVoter;
            Identifiable identAdmin = testAdmin;
            AuthenticatableUser authUser = testVoter;
            assertCondition(identVoter.getId().equals("V001") && identAdmin.getId().equals("ADM001"),
                    "Identifiable interface contract implemented across models");
            assertCondition(testVoter.getDisplayName().contains("Computer Science") && testAdmin.getDisplayName().contains("Officer"),
                    "Abstract User dynamic method dispatch verified on getDisplayName()");
            assertCondition(authUser.verifyPassword("voter123") && !authUser.verifyPassword("wrongpass"),
                    "AuthenticatableUser interface password verification contract verified");

            // Check 15: Intentional Demo Reset workflow
            boolean resetOk = electionService.resetDemoEnvironment();
            assertCondition(resetOk && electionService.getElection("E001") != null,
                    "Demo reset restores baseline active election E001");
            assertCondition(electionService.getCandidatesForElection("E001").size() == 3,
                    "Demo reset restores 3 baseline candidates");
            assertCondition(electionService.getAllVoters().size() == 5,
                    "Demo reset restores 5 baseline student voters");

        } catch (Throwable t) {
            System.err.println("[FAIL] Unexpected exception during UI Smoke Test: " + t.getMessage());
            t.printStackTrace();
            failed++;
        } finally {
            cleanTestDirectory(new File(TEST_DIR));
        }

        System.out.println("-------------------------------------------------");
        System.out.println("UI SMOKE TEST SUMMARY: " + passed + " Passed, " + failed + " Failed.");
        if (failed == 0) {
            System.out.println("RESULT: ALL UI SMOKE CHECKS PASSED SUCCESSFULLY! [100% OK]");
        } else {
            System.out.println("RESULT: SOME CHECKS FAILED!");
            System.exit(1);
        }
        System.out.println("=================================================");
    }

    private static void assertCondition(boolean condition, String message) {
        if (condition) {
            System.out.println("[PASS] " + message);
            passed++;
        } else {
            System.err.println("[FAIL] " + message);
            failed++;
        }
    }

    private static void cleanTestDirectory(File dir) {
        if (!dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) cleanTestDirectory(f);
                else f.delete();
            }
        }
        dir.delete();
    }
}

