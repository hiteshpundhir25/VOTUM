package com.votum.ui;

import com.votum.exception.InvalidElectionException;
import com.votum.model.Admin;
import com.votum.model.Candidate;
import com.votum.model.Election;
import com.votum.model.ElectionResult;
import com.votum.model.ElectionStatus;
import com.votum.model.Voter;
import com.votum.service.AuthenticationService;
import com.votum.service.ElectionService;
import com.votum.service.ResultService;
import com.votum.service.VotingService;
import com.votum.storage.AuditLogger;
import com.votum.storage.FileManager;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * ============================================================================
 * AdminDashboard (Administrator & Election Officer Control Center)
 * ============================================================================
 * 
 * Course: CSC2104 - Object Oriented Programming (Java)
 * 
 * Demonstrates:
 *  1. CARDLAYOUT TAB MANAGEMENT:
 *     - Uses java.awt.CardLayout to switch seamlessly between 6 panels:
 *       (1) Dashboard Overview, (2) Election Manager, (3) Candidate Manager,
 *       (4) Voter Roll Manager, (5) Results & Certified Tally, (6) Audit Trail.
 *  2. CANDIDATE & VOTER MANAGEMENT:
 *     - Candidate: Register new candidate, modify candidate details, remove candidate.
 *     - Voter: Register student, toggle eligibility (eligible <-> ineligible), delete record.
 *  3. MULTITHREADING (SwingWorker / Background Thread):
 *     - Runs a background daemon thread that polls election statistics every 3 seconds 
 *       and refreshes UI metrics safely via SwingUtilities.invokeLater.
 *  4. DATA SYNCHRONIZATION:
 *     - Automatically refreshes tables and forms whenever an action completes.
 */
public final class AdminDashboard extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter UI_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter STATUS_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final String CANDIDATE_EMPTY_MESSAGE =
            "No candidates registered for this election yet. Register participants using the form on the left.";
    private static final String VOTER_EMPTY_MESSAGE =
            "No registered voters found. Enroll students using the registration form on the left.";

    private final transient Admin admin;
    private final transient FileManager fileManager;
    private final transient AuditLogger auditLogger;
    private final transient ElectionService electionService;
    private final transient VotingService votingService;
    private final transient ResultService resultService;
    private final transient AuthenticationService authService;

    // Shell & Navigation Components
    private CardLayout cardLayout;
    private JPanel mainContentPanel;
    private JButton btnNavDashboard;
    private JButton btnNavElection;
    private JButton btnNavCandidates;
    private JButton btnNavVoters;
    private JButton btnNavResults;
    private JButton btnNavAudit;
    private String currentNavKey = "DASHBOARD";

    // Dashboard Overview Header & Status
    private JLabel statActiveElectionName;
    private JLabel statActiveElectionWindow;
    private JLabel statElectionStatusBadge;

    // Election Selectors & Display Labels
    private JComboBox<String> electionSelectorForManage;
    private JComboBox<String> electionSelectorForCandidates;
    private JComboBox<String> electionSelectorForResults;
    private JLabel electionIdDisplayLbl;
    private JLabel electionNameDisplayLbl;
    private JLabel electionDescDisplayLbl;
    private JLabel electionStartDisplayLbl;
    private JLabel electionEndDisplayLbl;
    private JLabel currentElectionStatusLabel;

    // Management Tables & Results
    private DefaultTableModel candidateTableModel;
    private DefaultTableModel voterTableModel;
    private JTextArea auditTextArea;
    private JPanel resultsBarsContainer;
    private JLabel resultsWinnerBanner;

    // Live Dashboard Stat Cards
    private UITheme.StatCard statEligibleVotersCard;
    private UITheme.StatCard statCandidatesCard;
    private UITheme.StatCard statVotesCastCard;
    private UITheme.StatCard statTurnoutCard;

    // Create Election Form Fields
    private JTextField newElectionIdField;
    private JTextField newElectionNameField;
    private JTextField newElectionDescField;
    private DateTimeInput newElectionStartInput;
    private DateTimeInput newElectionEndInput;
    private JLabel createElectionErrorLabel;

    // Inventory & Actionable Guidance
    private JLabel electionInventoryLabel;
    private JPanel attentionPanel;

    // Searchable Rosters
    private JTable candidateTable;
    private JTable voterTable;
    private transient TableRowSorter<DefaultTableModel> candidateSorter;
    private transient TableRowSorter<DefaultTableModel> voterSorter;
    private JTextField candidateFilterField;
    private JTextField voterFilterField;
    private JLabel candidateCountLabel;
    private JLabel voterCountLabel;
    private JLabel candidateEmptyLabel;
    private JLabel voterEmptyLabel;
    private JTextField candidateIdField;
    private JTextField voterIdField;

    // Shell Status Feedback
    private JLabel statusLineLabel;

    // Selection Preservation Across Refreshes
    private String pendingElectionSelectionId;
    private final transient List<String> manageElectionIds = new ArrayList<>();
    private final transient List<String> candidateElectionIds = new ArrayList<>();
    private final transient List<String> resultsElectionIds = new ArrayList<>();

    // Background Multithreading Worker (CSC2104.4)
    private final transient LiveRefreshWorker refreshWorker = new LiveRefreshWorker();
    private final transient Thread refreshThread = new Thread(refreshWorker, "VOTUM-LiveMetricWorker");

    public AdminDashboard(Admin admin, FileManager fileManager, AuditLogger auditLogger,
                          ElectionService electionService, VotingService votingService,
                          ResultService resultService, AuthenticationService authService) {
        this.admin = admin;
        this.fileManager = fileManager;
        this.auditLogger = auditLogger;
        this.electionService = electionService;
        this.votingService = votingService;
        this.resultService = resultService;
        this.authService = authService;

        initUI();
        refreshAllData();

        // Start daemon background refresh worker for live turnout metrics
        refreshThread.setDaemon(true);
        refreshThread.start();
    }

    private void initUI() {
        setTitle("VOTUM — Administrator Control Center");
        setUndecorated(true);
        setAlwaysOnTop(true);
        setMinimumSize(new Dimension(1080, 680));
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds(0, 0, screenSize.width, screenSize.height);
        setExtendedState(Frame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(UITheme.PAPER);
        setLayout(new BorderLayout(0, 0));

        // 1. MASTHEAD — MASTHEAD TYPE, ONE HAIRLINE RULE, NO ROUNDED CHROME
        JPanel topBar = new JPanel(new BorderLayout(24, 0));
        topBar.setBackground(UITheme.PAPER);
        topBar.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 2, 0, UITheme.INK),
                new EmptyBorder(14, 26, 13, 26)
        ));

        JPanel brandPanel = new JPanel();
        brandPanel.setLayout(new BoxLayout(brandPanel, BoxLayout.Y_AXIS));
        brandPanel.setOpaque(false);

        JLabel logoLbl = new JLabel("VOTUM");
        logoLbl.setFont(UITheme.FONT_LOGO);
        logoLbl.setForeground(UITheme.INK);
        logoLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subBrandLbl = UITheme.createKicker("ADMINISTRATION  \u00B7  ELECTION DESK");
        subBrandLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        brandPanel.add(logoLbl);
        brandPanel.add(subBrandLbl);

        JPanel rightUserPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightUserPanel.setOpaque(false);

        JLabel userLbl = new JLabel("Officer: " + admin.getFullName() + " (" + admin.getUsername() + ")");
        userLbl.setFont(UITheme.FONT_SMALL);
        userLbl.setForeground(UITheme.MUTED);

        JButton logoutBtn = UITheme.createSecondaryButton("Logout");
        logoutBtn.setFont(UITheme.FONT_SMALL_BOLD);
        logoutBtn.setBorder(new EmptyBorder(8, 14, 8, 14));
        logoutBtn.setToolTipText("Return to the student/administrator login portal");
        logoutBtn.addActionListener(e -> handleLogout());

        JButton exitBtn = UITheme.createDangerButton("✕ Exit Application");
        exitBtn.setFont(UITheme.FONT_SMALL_BOLD);
        exitBtn.setBorder(new EmptyBorder(8, 14, 8, 14));
        exitBtn.setToolTipText("Close and exit the VOTUM system completely");
        exitBtn.addActionListener(e -> handleExitApplication());

        JButton refreshTopBtn = UITheme.createSecondaryButton("\u21BB Refresh");
        refreshTopBtn.setFont(UITheme.FONT_SMALL_BOLD);
        refreshTopBtn.setBorder(new EmptyBorder(8, 14, 8, 14));
        refreshTopBtn.addActionListener(e -> {
            refreshAllData();
            setStatus("Data reloaded from disk at " + LocalDateTime.now().format(STATUS_TIME_FORMAT));
        });

        JButton resetDemoBtn = UITheme.createSecondaryButton("\u21BA Reset Demo Data");
        resetDemoBtn.setFont(UITheme.FONT_SMALL_BOLD);
        resetDemoBtn.setBorder(new EmptyBorder(8, 14, 8, 14));
        resetDemoBtn.setToolTipText("Restore baseline demo accounts, candidates, active election, and clear ballots");
        resetDemoBtn.addActionListener(e -> handleResetDemoData());

        rightUserPanel.add(userLbl);
        rightUserPanel.add(refreshTopBtn);
        rightUserPanel.add(resetDemoBtn);
        rightUserPanel.add(logoutBtn);
        rightUserPanel.add(exitBtn);

        topBar.add(brandPanel, BorderLayout.WEST);
        topBar.add(rightUserPanel, BorderLayout.EAST);

        // 2. CONTENTS NAVIGATION — NUMBERED LIKE A MAGAZINE INDEX
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(UITheme.PAPER_ALT);
        sidebar.setPreferredSize(new Dimension(232, Integer.MAX_VALUE));
        sidebar.setBorder(new MatteBorder(0, 0, 0, 1, UITheme.RULE));

        JLabel menuTitle = UITheme.createKicker("SECTIONS");
        menuTitle.setForeground(UITheme.MUTED);
        menuTitle.setBorder(new EmptyBorder(26, 22, 14, 12));
        menuTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnNavDashboard = createNavButton("01  Overview", "DASHBOARD");
        btnNavElection = createNavButton("02  Elections", "ELECTION");
        btnNavCandidates = createNavButton("03  Candidates", "CANDIDATES");
        btnNavVoters = createNavButton("04  Voter Roll", "VOTERS");
        btnNavResults = createNavButton("05  Live Results", "RESULTS");
        btnNavAudit = createNavButton("06  Audit Trail", "AUDIT");

        sidebar.add(menuTitle);
        sidebar.add(btnNavDashboard);
        sidebar.add(btnNavElection);
        sidebar.add(btnNavCandidates);
        sidebar.add(btnNavVoters);
        sidebar.add(btnNavResults);
        sidebar.add(btnNavAudit);
        sidebar.add(Box.createVerticalGlue());

        // 3. MAIN CONTENT (CardLayout with scroll wrappers)
        cardLayout = new CardLayout();
        mainContentPanel = new JPanel(cardLayout);
        mainContentPanel.setBackground(UITheme.PAPER);

        mainContentPanel.add(wrapContentScroll(createDashboardOverviewPanel()), "DASHBOARD");
        mainContentPanel.add(wrapContentScroll(createElectionPanel()), "ELECTION");
        mainContentPanel.add(wrapContentScroll(createCandidatesPanel()), "CANDIDATES");
        mainContentPanel.add(wrapContentScroll(createVotersPanel()), "VOTERS");
        mainContentPanel.add(wrapContentScroll(createResultsPanel()), "RESULTS");
        mainContentPanel.add(createAuditLogPanel(), "AUDIT");

        // 4. BOTTOM STATUS BAR
        statusLineLabel = new JLabel("Ready");
        statusLineLabel.setFont(UITheme.FONT_SMALL);
        statusLineLabel.setForeground(UITheme.MUTED);

        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(UITheme.PAPER_ALT);
        statusBar.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, UITheme.RULE),
                new EmptyBorder(7, 26, 7, 26)
        ));
        statusBar.add(statusLineLabel, BorderLayout.WEST);

        add(topBar, BorderLayout.NORTH);
        add(sidebar, BorderLayout.WEST);
        add(mainContentPanel, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        updateNavSelection("DASHBOARD");

        getRootPane().registerKeyboardAction(
                e -> handleExitApplication(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    private JButton createNavButton(String text, String key) {
        JButton btn = UITheme.createSidebarButton(text, false);
        btn.addActionListener(e -> switchCard(key));
        return btn;
    }

    private void switchCard(String key) {
        currentNavKey = key;
        updateNavSelection(key);
        cardLayout.show(mainContentPanel, key);
        refreshAllData();
        setStatus("Viewing " + navLabel(key));
    }

    private String navLabel(String key) {
        switch (key) {
            case "ELECTION":
                return "election configuration & lifecycle";
            case "CANDIDATES":
                return "candidate roster";
            case "VOTERS":
                return "voter roll";
            case "RESULTS":
                return "live election results";
            case "AUDIT":
                return "system audit trail";
            default:
                return "election overview dashboard";
        }
    }

    private void updateNavSelection(String activeKey) {
        updateSingleNavBtn(btnNavDashboard, activeKey.equals("DASHBOARD"));
        updateSingleNavBtn(btnNavElection, activeKey.equals("ELECTION"));
        updateSingleNavBtn(btnNavCandidates, activeKey.equals("CANDIDATES"));
        updateSingleNavBtn(btnNavVoters, activeKey.equals("VOTERS"));
        updateSingleNavBtn(btnNavResults, activeKey.equals("RESULTS"));
        updateSingleNavBtn(btnNavAudit, activeKey.equals("AUDIT"));
    }

    private void updateSingleNavBtn(JButton btn, boolean active) {
        UITheme.setNavActive(btn, active);
    }

    // =========================================================================
    // 1. DASHBOARD OVERVIEW PANEL
    // =========================================================================
    private JPanel createDashboardOverviewPanel() {
        JPanel page = UITheme.createPage(28, 30, 30, 30);

        JPanel header = UITheme.createSectionHeader(
                "FRONT PAGE",
                "Campus Election Dashboard Overview",
                "Real-time summary of active election metrics, participant counts, and turnout rate.");
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        UITheme.addPageRow(page, header, 22);

        // Lead story, feature column.
        JPanel activeElectionCard = UITheme.createCardPanel();
        activeElectionCard.setLayout(new BoxLayout(activeElectionCard, BoxLayout.Y_AXIS));
        activeElectionCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel activeTag = UITheme.createKicker("LEAD ELECTION");
        activeTag.setAlignmentX(Component.LEFT_ALIGNMENT);

        statActiveElectionName = new JLabel("Loading Election...");
        statActiveElectionName.setFont(UITheme.FONT_HEADER);
        statActiveElectionName.setForeground(UITheme.INK);
        statActiveElectionName.setAlignmentX(Component.LEFT_ALIGNMENT);

        statActiveElectionWindow = new JLabel(" ");
        statActiveElectionWindow.setFont(UITheme.FONT_SMALL);
        statActiveElectionWindow.setForeground(UITheme.MUTED);
        statActiveElectionWindow.setAlignmentX(Component.LEFT_ALIGNMENT);

        statElectionStatusBadge = UITheme.createBadge("STATUS: -", UITheme.PAPER_ALT, UITheme.MUTED);
        statElectionStatusBadge.setAlignmentX(Component.LEFT_ALIGNMENT);

        activeElectionCard.add(activeTag);
        activeElectionCard.add(Box.createVerticalStrut(6));
        activeElectionCard.add(statActiveElectionName);
        activeElectionCard.add(Box.createVerticalStrut(4));
        activeElectionCard.add(statActiveElectionWindow);
        activeElectionCard.add(Box.createVerticalStrut(14));
        activeElectionCard.add(statElectionStatusBadge);

        // Desk notes, sidebar column.
        attentionPanel = new JPanel();
        attentionPanel.setLayout(new BoxLayout(attentionPanel, BoxLayout.Y_AXIS));
        attentionPanel.setOpaque(false);
        attentionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel aside = new JPanel();
        aside.setLayout(new BoxLayout(aside, BoxLayout.Y_AXIS));
        aside.setOpaque(false);
        aside.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel asideKicker = UITheme.createKicker("DESK NOTES");
        asideKicker.setAlignmentX(Component.LEFT_ALIGNMENT);
        aside.add(asideKicker);
        aside.add(Box.createVerticalStrut(10));
        aside.add(attentionPanel);

        JPanel grid = UITheme.createEditorialColumns(activeElectionCard, aside, 322);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        UITheme.addPageRow(page, grid, 24);

        // The data band, full width under the lead and the sidebar.
        JLabel bandKicker = UITheme.createKicker("THE NUMBERS");
        bandKicker.setAlignmentX(Component.LEFT_ALIGNMENT);
        UITheme.addPageRow(page, bandKicker, 8);

        JPanel statsGrid = new JPanel(new GridLayout(1, 4, 12, 0));
        statsGrid.setOpaque(false);
        statsGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 116));
        statsGrid.setAlignmentX(Component.LEFT_ALIGNMENT);

        statEligibleVotersCard = new UITheme.StatCard("Eligible Voters", "0", "Registered on roll", UITheme.INK);
        statCandidatesCard = new UITheme.StatCard("Candidates", "0", "Contesting election", UITheme.ACCENT);
        statVotesCastCard = new UITheme.StatCard("Votes Cast", "0", "Official ballots", UITheme.SUCCESS);
        statTurnoutCard = new UITheme.StatCard("Voter Turnout", "0.00%", "Participation rate", UITheme.ACCENT_DARK);

        statsGrid.add(statEligibleVotersCard);
        statsGrid.add(statCandidatesCard);
        statsGrid.add(statVotesCastCard);
        statsGrid.add(statTurnoutCard);

        UITheme.addPageRow(page, statsGrid, 22);

        // Quick Administrative Actions Card
        JPanel quickActionsCard = UITheme.createCardPanel();
        quickActionsCard.setLayout(new BoxLayout(quickActionsCard, BoxLayout.Y_AXIS));

        JLabel qaTitle = new JLabel("Quick Administrative Actions");
        qaTitle.setFont(UITheme.FONT_HEADER);
        qaTitle.setForeground(UITheme.INK);
        qaTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel btnRow = new JPanel(new GridLayout(0, 3, 10, 8));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton goCreateElectionBtn = UITheme.createSuccessButton("Create New Election");
        JButton goElectionBtn = UITheme.createPrimaryButton("Manage Election Status");
        JButton goCandidatesBtn = UITheme.createSecondaryButton("Register Candidate");
        JButton goVotersBtn = UITheme.createSecondaryButton("Register Voter");
        JButton goResultsBtn = UITheme.createSecondaryButton("View Live Results");

        goCreateElectionBtn.addActionListener(e -> {
            switchCard("ELECTION");
            if (newElectionIdField != null) {
                newElectionIdField.requestFocusInWindow();
            }
        });
        goElectionBtn.addActionListener(e -> switchCard("ELECTION"));
        goCandidatesBtn.addActionListener(e -> switchCard("CANDIDATES"));
        goVotersBtn.addActionListener(e -> switchCard("VOTERS"));
        goResultsBtn.addActionListener(e -> switchCard("RESULTS"));

        btnRow.add(goCreateElectionBtn);
        btnRow.add(goElectionBtn);
        btnRow.add(goCandidatesBtn);
        btnRow.add(goVotersBtn);
        btnRow.add(goResultsBtn);
        JPanel emptyActionCell = new JPanel();
        emptyActionCell.setOpaque(false);
        btnRow.add(emptyActionCell);

        quickActionsCard.add(qaTitle);
        quickActionsCard.add(Box.createVerticalStrut(8));
        quickActionsCard.add(btnRow);

        UITheme.addPageRow(page, quickActionsCard, 0);
        return page;
    }

    /**
     * Rebuilds the "Needs Your Attention" strip from authoritative live election data.
     */
    private void refreshAttentionPanel() {
        if (attentionPanel == null) {
            return;
        }
        attentionPanel.removeAll();

        JPanel card = UITheme.createCardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JLabel title = new JLabel("Needs Your Attention");
        title.setFont(UITheme.FONT_HEADER);
        title.setForeground(UITheme.INK);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(8));

        LocalDateTime now = LocalDateTime.now();
        int items = 0;

        for (Election e : electionService.getAllElections()) {
            int candidateCount = electionService.getCandidatesForElection(e.getElectionId()).size();
            boolean open = e.getStatus() == ElectionStatus.OPEN;

            if (candidateCount == 0 && e.getStatus() != ElectionStatus.CLOSED) {
                addAttentionRow(card, UITheme.WARNING,
                        e.getName() + " [" + e.getElectionId() + "] has no candidates registered",
                        "The ballot is empty, so nobody can be voted for. Register at least one candidate before voting begins.",
                        "Add candidates", "CANDIDATES", e.getElectionId());
                items++;
            }

            if (open && e.getEndTime() != null && e.getEndTime().isBefore(now)) {
                addAttentionRow(card, UITheme.DANGER,
                        e.getName() + " [" + e.getElectionId() + "] is still flagged OPEN but its window ended "
                                + describeAgo(e.getEndTime(), now),
                        "No voter can cast a ballot in it any more. Close it to finalise the official count.",
                        "Close election", "ELECTION", e.getElectionId());
                items++;
            } else if (open && e.getStartTime() != null && e.getStartTime().isAfter(now)) {
                addAttentionRow(card, UITheme.WARNING,
                        e.getName() + " [" + e.getElectionId() + "] opens "
                                + e.getStartTime().format(UI_DATE_FORMAT),
                        "The ballot remains locked until the voting window begins.",
                        "Review", "ELECTION", e.getElectionId());
                items++;
            }
        }

        if (items == 0) {
            // Measured wrapping keeps the note inside the narrow desk-notes column.
            JPanel fineRow = new JPanel();
            fineRow.setLayout(new BoxLayout(fineRow, BoxLayout.X_AXIS));
            fineRow.setOpaque(false);
            fineRow.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel tick = new JLabel("\u2713");
            tick.setFont(UITheme.FONT_BODY_BOLD);
            tick.setForeground(UITheme.SUCCESS);
            tick.setAlignmentY(Component.TOP_ALIGNMENT);

            JLabel fine = UITheme.createWrappedLabel(
                    "Every configured election is in good order \u2014 candidates registered and the voting window operational.",
                    UITheme.FONT_BODY, UITheme.MUTED, 210);
            fine.setAlignmentX(Component.LEFT_ALIGNMENT);
            fine.setAlignmentY(Component.TOP_ALIGNMENT);

            fineRow.add(tick);
            fineRow.add(Box.createHorizontalStrut(7));
            fineRow.add(fine);
            fineRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, fineRow.getPreferredSize().height));
            card.add(fineRow);
        }

        attentionPanel.add(card);
        attentionPanel.revalidate();
        attentionPanel.repaint();
    }

    /**
     * One desk-note line. The action sits under the copy as a text link so the
     * narrow sidebar never has to split its width between prose and a button.
     */
    private void addAttentionRow(JPanel parent, Color accent, String headline, String detail,
                                 String actionText, String targetPanel, String electionId) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, UITheme.RULE_FAINT),
                new EmptyBorder(2, 0, 12, 0)
        ));

        JLabel head = UITheme.createWrappedLabel("\u25CF  " + headline,
                UITheme.FONT_BODY_BOLD, accent, 214);
        head.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel why = UITheme.createWrappedLabel(detail, UITheme.FONT_SMALL, UITheme.MUTED, 214);
        why.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton action = UITheme.createLinkButton(actionText + " \u2192");
        action.setAlignmentX(Component.LEFT_ALIGNMENT);
        action.setToolTipText("Open the " + navLabel(targetPanel) + " for " + electionId);
        action.addActionListener(e -> {
            selectElectionForPanel(targetPanel, electionId);
            switchCard(targetPanel);
        });

        row.add(head);
        row.add(Box.createVerticalStrut(3));
        row.add(why);
        row.add(Box.createVerticalStrut(6));
        row.add(action);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        parent.add(row);
        parent.add(Box.createVerticalStrut(10));
    }

    private void selectElectionForPanel(String panelKey, String electionId) {
        if ("CANDIDATES".equals(panelKey)) {
            selectById(electionSelectorForCandidates, candidateElectionIds, electionId);
        } else if ("RESULTS".equals(panelKey)) {
            selectById(electionSelectorForResults, resultsElectionIds, electionId);
        } else {
            selectById(electionSelectorForManage, manageElectionIds, electionId);
        }
    }

    private void selectById(JComboBox<String> combo, List<String> electionIds, String electionId) {
        if (combo == null || electionIds == null || electionId == null) {
            return;
        }
        for (int i = 0; i < electionIds.size(); i++) {
            if (electionIds.get(i).equalsIgnoreCase(electionId)) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    private static String describeAgo(LocalDateTime past, LocalDateTime now) {
        long minutes = Math.max(java.time.Duration.between(past, now).toMinutes(), 0);
        if (minutes < 60) {
            return minutes + "m ago";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + "h ago";
        }
        return (hours / 24) + "d ago";
    }

    // =========================================================================
    // 2. ELECTION PANEL (Configuration & Lifecycle)
    // =========================================================================
    private JPanel createElectionPanel() {
        JPanel panel = UITheme.createPage(28, 30, 30, 30);

        JPanel header = UITheme.createSectionHeader(
                "ELECTION DESK",
                "Election Configuration & Status Management",
                "Create new campus elections or manage the voting status and schedule of an existing election.");
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        electionInventoryLabel = new JLabel(" ");
        electionInventoryLabel.setFont(UITheme.FONT_SMALL_BOLD);
        electionInventoryLabel.setForeground(UITheme.ACCENT);

        JPanel createCard = createNewElectionCard();

        // Selected Election Details & Lifecycle Actions Card
        JPanel card = UITheme.createCardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        // Selector Row
        JPanel selectorRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        selectorRow.setOpaque(false);
        selectorRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel selectLbl = new JLabel("Selected Election:");
        selectLbl.setFont(UITheme.FONT_BODY_BOLD);
        selectLbl.setForeground(UITheme.INK);

        electionSelectorForManage = styleComboBox(new JComboBox<>());
        electionSelectorForManage.setPreferredSize(new Dimension(420, 36));
        electionSelectorForManage.addActionListener(e -> updateElectionStatusDisplay());

        selectorRow.add(selectLbl);
        selectorRow.add(electionSelectorForManage);

        // Details Grid Panel
        JPanel detailsPanel = new JPanel(new GridLayout(3, 2, 16, 14));
        detailsPanel.setBackground(UITheme.PAPER_ALT);
        detailsPanel.setBorder(new CompoundBorder(
                new LineBorder(UITheme.RULE, 1),
                new EmptyBorder(16, 18, 16, 18)
        ));
        detailsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        detailsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        electionIdDisplayLbl = new JLabel("-");
        electionNameDisplayLbl = new JLabel("-");
        electionDescDisplayLbl = new JLabel("-");
        electionStartDisplayLbl = new JLabel("-");
        electionEndDisplayLbl = new JLabel("-");
        currentElectionStatusLabel = new JLabel("-");

        electionIdDisplayLbl.setFont(UITheme.FONT_BODY);
        electionIdDisplayLbl.setForeground(UITheme.INK);
        electionNameDisplayLbl.setFont(UITheme.FONT_BODY_BOLD);
        electionNameDisplayLbl.setForeground(UITheme.INK);
        electionDescDisplayLbl.setFont(UITheme.FONT_BODY);
        electionDescDisplayLbl.setForeground(UITheme.MUTED);
        electionStartDisplayLbl.setFont(UITheme.FONT_BODY);
        electionStartDisplayLbl.setForeground(UITheme.INK);
        electionEndDisplayLbl.setFont(UITheme.FONT_BODY);
        electionEndDisplayLbl.setForeground(UITheme.INK);
        currentElectionStatusLabel.setFont(UITheme.FONT_HEADER);

        detailsPanel.add(createDetailItem("Election ID", electionIdDisplayLbl));
        detailsPanel.add(createDetailItem("Election Title", electionNameDisplayLbl));
        detailsPanel.add(createDetailItem("Voting Start Time", electionStartDisplayLbl));
        detailsPanel.add(createDetailItem("Voting End Time", electionEndDisplayLbl));
        detailsPanel.add(createDetailItem("Description", electionDescDisplayLbl));
        detailsPanel.add(createDetailItem("Official Voting Status", currentElectionStatusLabel));

        // Action Buttons Row
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton openBtn = UITheme.createSuccessButton("Open Election (Start Voting)");
        JButton closeBtn = UITheme.createDangerButton("Close Election (Stop Voting)");
        JButton deleteBtn = UITheme.createDangerButton("Delete Election");
        JButton refreshBtn = UITheme.createSecondaryButton("Refresh Status");

        openBtn.addActionListener(e -> handleOpenElection());
        closeBtn.addActionListener(e -> handleCloseElection());
        deleteBtn.addActionListener(e -> handleDeleteElection());
        refreshBtn.addActionListener(e -> refreshAllData());

        btnRow.add(openBtn);
        btnRow.add(closeBtn);
        btnRow.add(deleteBtn);
        btnRow.add(refreshBtn);

        // Notice Box
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(UITheme.PAPER_ALT);
        infoPanel.setBorder(new CompoundBorder(
                new MatteBorder(0, 3, 0, 0, UITheme.ACCENT),
                new EmptyBorder(12, 14, 12, 14)
        ));
        infoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel noticeKicker = UITheme.createKicker("ADMINISTRATOR NOTICE");
        noticeKicker.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel infoLbl = UITheme.createWrappedLabel(
                "Elections are created as DRAFT. When ready, click 'Open Election' to enable student voting. "
                        + "Deleting an election permanently cascades to associated candidate and ballot records.",
                UITheme.FONT_SMALL, UITheme.MUTED, 720);
        infoLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        infoPanel.add(noticeKicker);
        infoPanel.add(Box.createVerticalStrut(6));
        infoPanel.add(infoLbl);

        card.add(selectorRow);
        card.add(Box.createVerticalStrut(14));
        card.add(detailsPanel);
        card.add(Box.createVerticalStrut(16));
        card.add(btnRow);
        card.add(Box.createVerticalStrut(14));
        card.add(infoPanel);

        UITheme.addPageRow(panel, header, 12);
        UITheme.addPageRow(panel, electionInventoryLabel, 16);
        UITheme.addPageRow(panel, createCard, 18);
        UITheme.addPageRow(panel, card, 0);

        return panel;
    }

    private JPanel createNewElectionCard() {
        JPanel card = UITheme.createCardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel header = new JLabel("Create New Election");
        header.setFont(UITheme.FONT_HEADER);
        header.setForeground(UITheme.INK);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel hint = new JLabel("New elections are created as DRAFT. Configure the roster and open voting when ready.");
        hint.setFont(UITheme.FONT_BODY);
        hint.setForeground(UITheme.MUTED);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);

        newElectionIdField = UITheme.createTextField(14);
        newElectionNameField = UITheme.createTextField(24);
        newElectionDescField = UITheme.createTextField(24);

        LocalDateTime defaultStart = LocalDateTime.now().withSecond(0).withNano(0);
        newElectionStartInput = new DateTimeInput(null);
        newElectionStartInput.setDateTime(defaultStart);
        newElectionEndInput = new DateTimeInput(null);
        newElectionEndInput.setDateTime(defaultStart.plusDays(1));

        // Organized 2-row layout without dummy spacer cells
        JPanel row1 = new JPanel(new GridLayout(1, 3, 14, 0));
        row1.setOpaque(false);
        row1.setAlignmentX(Component.LEFT_ALIGNMENT);
        row1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
        row1.add(labeledField("Election ID", newElectionIdField));
        row1.add(labeledField("Election Title", newElectionNameField));
        row1.add(labeledField("Description (optional)", newElectionDescField));

        JPanel row2 = new JPanel(new GridLayout(1, 2, 14, 0));
        row2.setOpaque(false);
        row2.setAlignmentX(Component.LEFT_ALIGNMENT);
        row2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
        row2.add(labeledField("Voting Starts (" + DateTimeInput.PATTERN + ")", newElectionStartInput));
        row2.add(labeledField("Voting Ends (" + DateTimeInput.PATTERN + ")", newElectionEndInput));

        JPanel presetsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        presetsRow.setOpaque(false);
        presetsRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel presetsLbl = new JLabel("Quick windows:");
        presetsLbl.setFont(UITheme.FONT_BODY_BOLD);
        presetsLbl.setForeground(UITheme.MUTED);

        JButton startNowPreset = UITheme.createSecondaryButton("Opens now \u2192 +1 day");
        startNowPreset.setFont(UITheme.FONT_SMALL_BOLD);
        startNowPreset.addActionListener(e -> {
            LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
            newElectionStartInput.setDateTime(now);
            newElectionEndInput.setDateTime(now.plusDays(1));
            hideCreateElectionError();
        });

        JButton sameDayPreset = UITheme.createSecondaryButton("Today 09:00 \u2192 18:00");
        sameDayPreset.setFont(UITheme.FONT_SMALL_BOLD);
        sameDayPreset.addActionListener(e -> {
            LocalDateTime today = LocalDateTime.now().withHour(9).withMinute(0).withSecond(0).withNano(0);
            newElectionStartInput.setDateTime(today);
            newElectionEndInput.setDateTime(today.withHour(18));
            hideCreateElectionError();
        });

        JButton clearPreset = UITheme.createSecondaryButton("Clear window");
        clearPreset.setFont(UITheme.FONT_SMALL_BOLD);
        clearPreset.addActionListener(e -> {
            newElectionStartInput.setText("");
            newElectionEndInput.setText("");
            hideCreateElectionError();
        });

        presetsRow.add(presetsLbl);
        presetsRow.add(startNowPreset);
        presetsRow.add(sameDayPreset);
        presetsRow.add(clearPreset);

        createElectionErrorLabel = new JLabel(" ");
        createElectionErrorLabel.setFont(UITheme.FONT_SMALL_BOLD);
        createElectionErrorLabel.setForeground(UITheme.DANGER);
        createElectionErrorLabel.setBackground(UITheme.DANGER_TINT);
        createElectionErrorLabel.setOpaque(true);
        createElectionErrorLabel.setBorder(new CompoundBorder(
                new LineBorder(UITheme.DANGER_BORDER, 1),
                new EmptyBorder(6, 12, 6, 12)
        ));
        createElectionErrorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        createElectionErrorLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        createElectionErrorLabel.setVisible(false);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actionRow.setOpaque(false);
        actionRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton createBtn = UITheme.createSuccessButton("Create Election (DRAFT)");
        createBtn.addActionListener(e -> handleCreateElection());

        JButton resetBtn = UITheme.createSecondaryButton("Reset Form");
        resetBtn.addActionListener(e -> {
            resetCreateElectionForm();
            hideCreateElectionError();
            setStatus("Create-election form reset");
        });

        actionRow.add(createBtn);
        actionRow.add(resetBtn);

        card.add(header);
        card.add(Box.createVerticalStrut(4));
        card.add(hint);
        card.add(Box.createVerticalStrut(14));
        card.add(row1);
        card.add(Box.createVerticalStrut(8));
        card.add(row2);
        card.add(Box.createVerticalStrut(10));
        card.add(presetsRow);
        card.add(Box.createVerticalStrut(12));
        card.add(createElectionErrorLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(actionRow);
        return card;
    }

    private JPanel labeledField(String labelText, JComponent field) {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setOpaque(false);

        JLabel label = new JLabel(labelText);
        label.setFont(UITheme.FONT_SMALL_BOLD);
        label.setForeground(UITheme.MUTED);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setLabelFor(field);

        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        wrapper.add(label);
        wrapper.add(Box.createVerticalStrut(3));
        wrapper.add(field);
        return wrapper;
    }

    private JPanel createDetailItem(String title, JLabel valueLabel) {
        JPanel itemPanel = new JPanel(new BorderLayout(4, 2));
        itemPanel.setOpaque(false);
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(UITheme.FONT_SMALL_BOLD);
        titleLbl.setForeground(UITheme.MUTED);
        itemPanel.add(titleLbl, BorderLayout.NORTH);
        itemPanel.add(valueLabel, BorderLayout.CENTER);
        return itemPanel;
    }

    // =========================================================================
    // 3. CANDIDATES PANEL
    // =========================================================================
    private JPanel createCandidatesPanel() {
        JPanel page = new JPanel(new BorderLayout(0, 20));
        page.setBackground(UITheme.PAPER);
        page.setBorder(new EmptyBorder(28, 30, 30, 30));
        page.add(UITheme.createSectionHeader(
                "ROSTER",
                "Candidates",
                "Register participants for an election and search the complete candidate roster."),
                BorderLayout.NORTH);

        JPanel panel = new JPanel(new BorderLayout(20, 0));
        panel.setBackground(UITheme.PAPER);

        // Form Card (West)
        JPanel formCard = UITheme.createCardPanel();
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));

        JLabel header = new JLabel("Create / Register Candidate");
        header.setFont(UITheme.FONT_HEADER);
        header.setForeground(UITheme.INK);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        candidateIdField = UITheme.createTextField(15);
        candidateIdField.setText(suggestNextId("C", loadCandidateIds()));
        JTextField cNameField = UITheme.createTextField(15);
        JTextField cDeptField = UITheme.createTextField(15);
        JTextField cSloganField = UITheme.createTextField(15);

        electionSelectorForCandidates = styleComboBox(new JComboBox<>());
        electionSelectorForCandidates.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        electionSelectorForCandidates.addActionListener(e -> refreshCandidatesTable());

        JButton addCandidateBtn = UITheme.createPrimaryButton("+ Register Candidate");
        addCandidateBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        addCandidateBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        JButton editCandidateBtn = UITheme.createSecondaryButton("✎ Modify Candidate");
        editCandidateBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        editCandidateBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        editCandidateBtn.setToolTipText("Edit or modify candidate name, department, or campaign slogan");
        editCandidateBtn.addActionListener(e -> handleEditCandidate());

        JButton removeCandidateBtn = UITheme.createDangerButton("🗑 Remove Candidate");
        removeCandidateBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        removeCandidateBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        removeCandidateBtn.setToolTipText("Remove or disqualify a candidate from the roster and purge their votes");
        removeCandidateBtn.addActionListener(e -> handleRemoveCandidate());

        addCandidateBtn.addActionListener(e -> {
            String selectedElection = getSelectedElectionId(electionSelectorForCandidates, candidateElectionIds);
            if (selectedElection == null) {
                JOptionPane.showMessageDialog(this,
                        "No election available to register candidates into.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                electionService.addCandidate(
                        candidateIdField.getText().trim(),
                        cNameField.getText().trim(),
                        cDeptField.getText().trim(),
                        selectedElection,
                        cSloganField.getText().trim()
                );
                JOptionPane.showMessageDialog(this,
                        "Candidate registered successfully!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                cNameField.setText("");
                cDeptField.setText("");
                cSloganField.setText("");
                candidateIdField.setText(suggestNextId("C", loadCandidateIds()));
                refreshAllData();
            } catch (InvalidElectionException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Registration Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        formCard.add(header);
        formCard.add(Box.createVerticalStrut(12));
        addFormField(formCard, "Target Election:", electionSelectorForCandidates);
        addFormField(formCard, "Candidate ID:", candidateIdField);
        addFormField(formCard, "Full Name:", cNameField);
        addFormField(formCard, "Department:", cDeptField);
        addFormField(formCard, "Campaign Slogan:", cSloganField);
        formCard.add(Box.createVerticalStrut(10));
        formCard.add(addCandidateBtn);
        formCard.add(Box.createVerticalStrut(8));
        formCard.add(editCandidateBtn);
        formCard.add(Box.createVerticalStrut(8));
        formCard.add(removeCandidateBtn);

        // Table Card (Center)
        JPanel tableCard = UITheme.createCardPanel();
        tableCard.setLayout(new BorderLayout(10, 10));

        JLabel tableHeader = new JLabel("Registered Candidate Roster");
        tableHeader.setFont(UITheme.FONT_HEADER);
        tableHeader.setForeground(UITheme.INK);

        String[] cols = {"Candidate ID", "Full Name", "Department", "Campaign Slogan"};
        candidateTableModel = new DefaultTableModel(cols, 0) {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        candidateTable = styleTable(new JTable(candidateTableModel));
        candidateSorter = new TableRowSorter<>(candidateTableModel);
        candidateTable.setRowSorter(candidateSorter);
        int[] candidateWidths = {110, 160, 150, 180};
        for (int i = 0; i < candidateTable.getColumnCount(); i++) {
            candidateTable.getColumnModel().getColumn(i).setPreferredWidth(candidateWidths[i]);
        }

        candidateTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && candidateTable.getSelectedRow() != -1) {
                    handleEditCandidate();
                }
            }
        });

        candidateFilterField = UITheme.createTextField(14);
        candidateFilterField.setPreferredSize(new Dimension(200, 34));
        candidateFilterField.setToolTipText("Filter candidates: matches any column, case-insensitive");
        candidateCountLabel = createCountLabel();
        candidateEmptyLabel = createEmptyStateLabel();

        candidateFilterField.getDocument().addDocumentListener(new FilterChangeListener(
                candidateTable, candidateSorter, candidateFilterField, candidateCountLabel,
                candidateEmptyLabel, CANDIDATE_EMPTY_MESSAGE));

        tableCard.add(buildRosterHeader(tableHeader, candidateFilterField, candidateCountLabel), BorderLayout.NORTH);
        tableCard.add(wrapScrollPane(candidateTable), BorderLayout.CENTER);
        tableCard.add(candidateEmptyLabel, BorderLayout.SOUTH);

        panel.add(formCard, BorderLayout.WEST);
        panel.add(tableCard, BorderLayout.CENTER);
        page.add(panel, BorderLayout.CENTER);
        return page;
    }

    // =========================================================================
    // 4. VOTERS PANEL
    // =========================================================================
    private JPanel createVotersPanel() {
        JPanel page = new JPanel(new BorderLayout(0, 20));
        page.setBackground(UITheme.PAPER);
        page.setBorder(new EmptyBorder(28, 30, 30, 30));
        page.add(UITheme.createSectionHeader(
                "ELECTORATE",
                "Voter Roll",
                "Enroll students, mark eligibility and audit every ballot status."),
                BorderLayout.NORTH);

        JPanel panel = new JPanel(new BorderLayout(20, 0));
        panel.setBackground(UITheme.PAPER);

        // Form Card (West)
        JPanel formCard = UITheme.createCardPanel();
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));

        JLabel header = new JLabel("Register Eligible Voter");
        header.setFont(UITheme.FONT_HEADER);
        header.setForeground(UITheme.INK);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        voterIdField = UITheme.createTextField(15);
        voterIdField.setText(suggestNextId("V", loadVoterIds()));
        JTextField vNameField = UITheme.createTextField(15);
        JTextField vDeptField = UITheme.createTextField(15);
        JTextField vUsernameField = UITheme.createTextField(15);
        JPasswordField vPasswordField = UITheme.createPasswordField(15);
        vPasswordField.setText("voter123");

        JCheckBox eligibleCheck = UITheme.createCheckBox("Eligible to Vote in Elections", true);
        eligibleCheck.setFont(UITheme.FONT_BODY);
        eligibleCheck.setForeground(UITheme.INK);
        eligibleCheck.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton addVoterBtn = UITheme.createPrimaryButton("+ Register Voter");
        addVoterBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        addVoterBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        addVoterBtn.addActionListener(e -> {
            try {
                electionService.addVoter(
                        voterIdField.getText().trim(),
                        vNameField.getText().trim(),
                        vDeptField.getText().trim(),
                        vUsernameField.getText().trim(),
                        new String(vPasswordField.getPassword()),
                        eligibleCheck.isSelected()
                );
                JOptionPane.showMessageDialog(this, "Voter registered successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                vNameField.setText("");
                vDeptField.setText("");
                vUsernameField.setText("");
                voterIdField.setText(suggestNextId("V", loadVoterIds()));
                refreshAllData();
            } catch (InvalidElectionException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Voter Registration Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        formCard.add(header);
        formCard.add(Box.createVerticalStrut(12));
        addFormField(formCard, "Voter ID:", voterIdField);
        addFormField(formCard, "Full Name:", vNameField);
        addFormField(formCard, "Department:", vDeptField);
        addFormField(formCard, "Login Username:", vUsernameField);
        addFormField(formCard, "Login Password:", vPasswordField);
        formCard.add(Box.createVerticalStrut(4));
        formCard.add(eligibleCheck);
        formCard.add(Box.createVerticalStrut(12));
        formCard.add(addVoterBtn);

        // Table Card (Center)
        JPanel tableCard = UITheme.createCardPanel();
        tableCard.setLayout(new BorderLayout(10, 10));

        JLabel tableHeader = new JLabel("Official Registered Voter Roll");
        tableHeader.setFont(UITheme.FONT_HEADER);
        tableHeader.setForeground(UITheme.INK);

        String[] cols = {"Voter ID", "Full Name", "Department", "Username", "Eligible?", "Ballot Status"};
        voterTableModel = new DefaultTableModel(cols, 0) {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        voterTable = styleTable(new JTable(voterTableModel));
        voterSorter = new TableRowSorter<>(voterTableModel);
        voterTable.setRowSorter(voterSorter);
        int[] voterWidths = {75, 125, 150, 80, 85, 125};
        for (int i = 0; i < voterTable.getColumnCount(); i++) {
            voterTable.getColumnModel().getColumn(i).setPreferredWidth(voterWidths[i]);
        }

        voterFilterField = UITheme.createTextField(14);
        voterFilterField.setPreferredSize(new Dimension(200, 34));
        voterFilterField.setToolTipText("Filter voter roll: matches any column, case-insensitive");
        voterCountLabel = createCountLabel();
        voterEmptyLabel = createEmptyStateLabel();

        voterFilterField.getDocument().addDocumentListener(new FilterChangeListener(
                voterTable, voterSorter, voterFilterField, voterCountLabel, voterEmptyLabel, VOTER_EMPTY_MESSAGE));

        JPanel voterFooterPanel = new JPanel(new BorderLayout(10, 8));
        voterFooterPanel.setOpaque(false);
        voterFooterPanel.setBorder(new EmptyBorder(6, 0, 0, 0));
        voterFooterPanel.add(voterEmptyLabel, BorderLayout.NORTH);

        JPanel voterActionBar = new JPanel(new BorderLayout(8, 6));
        voterActionBar.setOpaque(false);

        JLabel voterActionPrompt = UITheme.createKicker("MANAGE VOTER RECORD (SELECT A ROW ABOVE):");
        voterActionPrompt.setForeground(UITheme.MUTED);

        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnGroup.setOpaque(false);

        JButton btnMarkEligible = UITheme.createSuccessButton("✓ Mark Eligible");
        btnMarkEligible.setFont(UITheme.FONT_SMALL_BOLD);
        btnMarkEligible.setToolTipText("Mark selected voter as Eligible to participate in elections");
        btnMarkEligible.addActionListener(e -> handleUpdateVoterEligibility(true));

        JButton btnMarkIneligible = UITheme.createSecondaryButton("✕ Mark Ineligible");
        btnMarkIneligible.setFont(UITheme.FONT_SMALL_BOLD);
        btnMarkIneligible.setToolTipText("Mark selected voter as Ineligible (bar from voting)");
        btnMarkIneligible.addActionListener(e -> handleUpdateVoterEligibility(false));

        JButton btnDeleteVoter = UITheme.createDangerButton("🗑 Delete Voter");
        btnDeleteVoter.setFont(UITheme.FONT_SMALL_BOLD);
        btnDeleteVoter.setToolTipText("Permanently delete this voter record and purge any cast ballots");
        btnDeleteVoter.addActionListener(e -> handleDeleteVoter());

        btnGroup.add(btnMarkEligible);
        btnGroup.add(btnMarkIneligible);
        btnGroup.add(btnDeleteVoter);

        voterActionBar.add(voterActionPrompt, BorderLayout.NORTH);
        voterActionBar.add(btnGroup, BorderLayout.CENTER);

        voterFooterPanel.add(voterActionBar, BorderLayout.SOUTH);

        tableCard.add(buildRosterHeader(tableHeader, voterFilterField, voterCountLabel), BorderLayout.NORTH);
        tableCard.add(wrapScrollPane(voterTable), BorderLayout.CENTER);
        tableCard.add(voterFooterPanel, BorderLayout.SOUTH);

        panel.add(formCard, BorderLayout.WEST);
        panel.add(tableCard, BorderLayout.CENTER);
        page.add(panel, BorderLayout.CENTER);
        return page;
    }

    // =========================================================================
    // 5. RESULTS PANEL (With Horizontal Animated Vote Bars)
    // =========================================================================
    private JPanel createResultsPanel() {
        JPanel panel = UITheme.createPage(28, 30, 30, 30);

        JPanel header = UITheme.createSectionHeader(
                "RESULTS DESK",
                "Official Election Results & Vote Tally",
                "Live graphical breakdown of votes, turnout metrics, and official winner declaration.");
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Control Card
        JPanel controlCard = UITheme.createCardPanel();
        controlCard.setLayout(new BorderLayout(12, 12));
        controlCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 75));

        JPanel selectorBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        selectorBox.setOpaque(false);

        JLabel selectLbl = new JLabel("Select Election:");
        selectLbl.setFont(UITheme.FONT_BODY_BOLD);
        selectLbl.setForeground(UITheme.INK);

        electionSelectorForResults = styleComboBox(new JComboBox<>());
        electionSelectorForResults.setPreferredSize(new Dimension(360, 36));
        electionSelectorForResults.addActionListener(e -> renderResultsDisplay());

        JButton refreshResultsBtn = UITheme.createPrimaryButton("Refresh Results");
        refreshResultsBtn.addActionListener(e -> renderResultsDisplay());

        JButton officialResultBtn = UITheme.createSecondaryButton("Official Result Dialog");
        officialResultBtn.addActionListener(e -> openOfficialResultDialog());

        selectorBox.add(selectLbl);
        selectorBox.add(electionSelectorForResults);
        selectorBox.add(refreshResultsBtn);
        selectorBox.add(officialResultBtn);

        controlCard.add(selectorBox, BorderLayout.CENTER);

        // Winner declaration: a headline between two heavy rules, not a card.
        JPanel winnerCard = new JPanel(new BorderLayout(8, 8));
        winnerCard.setOpaque(false);
        winnerCard.setBorder(new CompoundBorder(
                new MatteBorder(2, 0, 2, 0, UITheme.INK),
                new EmptyBorder(14, 0, 14, 0)
        ));

        resultsWinnerBanner = new JLabel("Calculating results...", SwingConstants.LEFT);
        resultsWinnerBanner.setFont(UITheme.FONT_DISPLAY);
        resultsWinnerBanner.setForeground(UITheme.INK);
        winnerCard.add(resultsWinnerBanner, BorderLayout.CENTER);

        // Dynamic Results Bar Container
        resultsBarsContainer = UITheme.createCardPanel();
        resultsBarsContainer.setLayout(new BoxLayout(resultsBarsContainer, BoxLayout.Y_AXIS));

        UITheme.addPageRow(panel, header, 18);
        UITheme.addPageRow(panel, controlCard, 18);
        UITheme.addPageRow(panel, winnerCard, 16);

        JLabel distributionKicker = UITheme.createKicker("VOTE DISTRIBUTION");
        distributionKicker.setForeground(UITheme.MUTED);
        UITheme.addPageRow(panel, distributionKicker, 10);
        UITheme.addPageRow(panel, resultsBarsContainer, 0);

        return panel;
    }

    private void renderResultsDisplay() {
        String eId = getSelectedElectionId(electionSelectorForResults, resultsElectionIds);
        if (eId == null) {
            resultsWinnerBanner.setText("No election available to calculate results.");
            resultsWinnerBanner.setForeground(UITheme.MUTED);
            resultsBarsContainer.removeAll();
            resultsBarsContainer.add(new JLabel("Please select or configure an election."));
            resultsBarsContainer.revalidate();
            resultsBarsContainer.repaint();
            return;
        }

        ElectionResult result = resultService.calculateResult(eId);
        resultsBarsContainer.removeAll();

        // Winner declaration, in the paper's own words.
        if (result.isTie()) {
            resultsWinnerBanner.setText(result.getWinnerSummary().toUpperCase(Locale.ROOT));
            resultsWinnerBanner.setForeground(UITheme.WARNING);
        } else if (result.getTotalVotes() > 0 && !result.getWinningCandidates().isEmpty()) {
            resultsWinnerBanner.setText(result.getWinnerSummary());
            resultsWinnerBanner.setForeground(UITheme.ACCENT_DARK);
        } else {
            resultsWinnerBanner.setText("No votes recorded yet for this election.");
            resultsWinnerBanner.setForeground(UITheme.MUTED);
        }

        // Metrics band
        JPanel metricsRow = new JPanel(new GridLayout(1, 3, 12, 0));
        metricsRow.setOpaque(false);
        metricsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 116));

        metricsRow.add(new UITheme.StatCard("Total Votes", String.valueOf(result.getTotalVotes()),
                "Official ballots", UITheme.ACCENT));
        metricsRow.add(new UITheme.StatCard("Eligible Voters", String.valueOf(result.getEligibleVotersCount()),
                "Certified roll", UITheme.INK));
        metricsRow.add(new UITheme.StatCard("Turnout Rate", result.getTurnoutPercentage() + "%",
                "Student participation", UITheme.SUCCESS));

        resultsBarsContainer.add(metricsRow);
        resultsBarsContainer.add(Box.createVerticalStrut(20));

        // Horizontal Progress Bars for each candidate
        for (Map.Entry<String, Integer> entry : result.getCandidateVoteCounts().entrySet()) {
            String cId = entry.getKey();
            int votes = entry.getValue();
            Candidate c = result.getCandidate(cId);
            String candDisplayName = (c != null) ? c.getName() + " (" + c.getDepartment() + ")" : cId;

            UITheme.HorizontalVoteBar bar = new UITheme.HorizontalVoteBar(candDisplayName, votes, result.getTotalVotes());
            resultsBarsContainer.add(bar);
            resultsBarsContainer.add(Box.createVerticalStrut(10));
        }

        resultsBarsContainer.revalidate();
        resultsBarsContainer.repaint();
    }

    // =========================================================================
    // 6. AUDIT LOG PANEL
    // =========================================================================
    private JPanel createAuditLogPanel() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBackground(UITheme.PAPER);
        panel.setBorder(new EmptyBorder(24, 28, 24, 28));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JPanel titleBox = new JPanel();
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.setOpaque(false);

        JPanel headerPanel = UITheme.createSectionHeader(
                "RECORD",
                "System Audit Trail",
                "Chronological, tamper-evident log of administrative and voting transactions.");
        titleBox.add(headerPanel);

        JPanel logBtnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        logBtnRow.setOpaque(false);

        JButton refreshBtn = UITheme.createSecondaryButton("Refresh Logs");
        JButton changePwdBtn = UITheme.createSecondaryButton("🔑 Change Audit Password");
        changePwdBtn.setToolTipText("Change or configure the Audit Protection Password");
        JButton clearBtn = UITheme.createDangerButton("🔒 Delete Audit Logs");
        clearBtn.setToolTipText("Permanently delete audit logs (Protected by Audit Security Password)");

        refreshBtn.addActionListener(e -> {
            refreshAuditLogs();
            setStatus("Audit log refreshed");
            JOptionPane.showMessageDialog(this, "Audit logs refreshed.", "Refreshed", JOptionPane.INFORMATION_MESSAGE);
        });

        changePwdBtn.addActionListener(e -> handleChangeAuditPassword());
        clearBtn.addActionListener(e -> handleDeleteAuditLogs());

        logBtnRow.add(refreshBtn);
        logBtnRow.add(changePwdBtn);
        logBtnRow.add(clearBtn);

        topRow.add(titleBox, BorderLayout.CENTER);

        // Keep the section rule full-width: the log actions sit below it
        // rather than squeezing the header into the remaining column.
        JPanel auditHeader = new JPanel(new BorderLayout(0, 10));
        auditHeader.setOpaque(false);
        auditHeader.add(topRow, BorderLayout.NORTH);
        auditHeader.add(logBtnRow, BorderLayout.SOUTH);

        auditTextArea = new JTextArea();
        auditTextArea.setEditable(false);
        auditTextArea.setFont(UITheme.FONT_MONO);
        auditTextArea.setBackground(UITheme.PAPER_SUNKEN);
        auditTextArea.setForeground(UITheme.INK);
        auditTextArea.setCaretColor(UITheme.INK);
        auditTextArea.setBorder(new EmptyBorder(12, 12, 12, 12));

        JScrollPane scrollPane = wrapScrollPane(auditTextArea);

        panel.add(auditHeader, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // HELPERS & ACTION HANDLERS
    // =========================================================================

    private void addFormField(JPanel panel, String labelText, JComponent field) {
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(UITheme.FONT_SMALL_BOLD);
        lbl.setForeground(UITheme.MUTED);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setLabelFor(field);

        field.setFont(UITheme.FONT_BODY);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(lbl);
        panel.add(Box.createVerticalStrut(3));
        panel.add(field);
        panel.add(Box.createVerticalStrut(10));
    }

    private JComboBox<String> styleComboBox(JComboBox<String> combo) {
        UITheme.styleComboBox(combo);
        return combo;
    }

    private JTable styleTable(JTable table) {
        return UITheme.styleTable(table);
    }

    private JScrollPane wrapScrollPane(JComponent comp) {
        return UITheme.wrapScrollPane(comp);
    }

    private JScrollPane wrapContentScroll(JComponent content) {
        return UITheme.wrapContentScroll(content);
    }

    private void setStatus(String message) {
        if (statusLineLabel == null) {
            return;
        }
        statusLineLabel.setText(message + "   \u2022   " + LocalDateTime.now().format(STATUS_TIME_FORMAT));
    }

    private void refreshDashboardMetrics() {
        if (statEligibleVotersCard == null) {
            return;
        }

        int totalVoters = 0;
        int eligibleVoters = 0;
        for (Voter voter : electionService.getAllVoters()) {
            totalVoters++;
            if (voter.isEligible()) {
                eligibleVoters++;
            }
        }

        String selectedId = getSelectedElectionId(electionSelectorForManage, manageElectionIds);
        Election target = (selectedId != null)
                ? electionService.getElection(selectedId)
                : electionService.getActiveOrFirstElection();

        int candidateCount = 0;
        int votesCast = 0;
        double turnout = 0.0;
        if (target != null) {
            candidateCount = electionService.getCandidatesForElection(target.getElectionId()).size();
            ElectionResult result = resultService.previewResult(target.getElectionId());
            votesCast = result.getTotalVotes();
            turnout = result.getTurnoutPercentage();
        }

        statEligibleVotersCard.setValue(String.valueOf(eligibleVoters));
        statEligibleVotersCard.setSubtitle(eligibleVoters == totalVoters
                ? "Registered on roll"
                : eligibleVoters + " eligible of " + totalVoters + " on roll");

        statCandidatesCard.setValue(String.valueOf(candidateCount));
        statCandidatesCard.setSubtitle(target == null ? "No election selected" : "Contesting " + target.getElectionId());

        statVotesCastCard.setValue(String.valueOf(votesCast));
        statVotesCastCard.setSubtitle(target == null ? "No election selected" : "Ballots in " + target.getElectionId());

        statTurnoutCard.setValue(String.format(Locale.ROOT, "%.2f%%", turnout));
        statTurnoutCard.setSubtitle(target == null ? "No election selected" : "Participation rate");
    }

    private void updateElectionInventory() {
        if (electionInventoryLabel == null) {
            return;
        }
        List<Election> elections = electionService.getAllElections();
        if (elections.isEmpty()) {
            electionInventoryLabel.setText("No elections configured yet \u2014 create the first one below.");
            electionInventoryLabel.setForeground(UITheme.WARNING);
            return;
        }

        int open = 0;
        int draft = 0;
        int closed = 0;
        for (Election election : elections) {
            switch (election.getStatus()) {
                case OPEN:
                    open++;
                    break;
                case CLOSED:
                    closed++;
                    break;
                default:
                    draft++;
                    break;
            }
        }
        electionInventoryLabel.setText(elections.size() + " configured   \u2022   " + open + " open   \u2022   "
                + draft + " draft   \u2022   " + closed + " closed");
        electionInventoryLabel.setForeground(UITheme.ACCENT);
    }

    private JPanel buildRosterHeader(JLabel titleLabel, JTextField filterField, JLabel countLabel) {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(titleLabel);
        header.add(Box.createVerticalStrut(8));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controls.setOpaque(false);
        controls.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel filterLbl = UITheme.createKicker("FILTER");
        filterLbl.setForeground(UITheme.MUTED);
        filterLbl.setLabelFor(filterField);

        controls.add(filterLbl);
        controls.add(filterField);
        controls.add(countLabel);
        header.add(controls);
        return header;
    }

    private JLabel createCountLabel() {
        JLabel label = new JLabel("0 rows");
        label.setFont(UITheme.FONT_SMALL_BOLD);
        label.setForeground(UITheme.MUTED);
        return label;
    }

    private JLabel createEmptyStateLabel() {
        JLabel label = new JLabel();
        label.setFont(UITheme.FONT_BODY);
        label.setBorder(new EmptyBorder(10, 4, 4, 4));
        label.setVisible(false);
        return label;
    }

    private void applyRosterFilter(TableRowSorter<DefaultTableModel> sorter, JTextField filterField,
                                   JTable table, JLabel countLabel, JLabel emptyLabel, String emptyMessage) {
        if (sorter == null || table == null) {
            return;
        }
        String text = (filterField == null || filterField.getText() == null) ? "" : filterField.getText().trim();
        sorter.setRowFilter(text.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(text)));

        int visibleRows = table.getRowCount();
        int storedRows = table.getModel().getRowCount();

        if (countLabel != null) {
            countLabel.setText(visibleRows + (visibleRows == 1 ? " row" : " rows")
                    + (visibleRows == storedRows ? "" : " of " + storedRows));
        }
        if (emptyLabel != null) {
            if (storedRows == 0) {
                emptyLabel.setText("<html><span style='color:#6B7280;'>" + emptyMessage + "</span></html>");
                emptyLabel.setVisible(true);
            } else if (visibleRows == 0) {
                emptyLabel.setText("<html><span style='color:#6B7280;'>No entries match the current filter.</span></html>");
                emptyLabel.setVisible(true);
            } else {
                emptyLabel.setVisible(false);
            }
        }
    }

    private final class FilterChangeListener implements DocumentListener {
        private final JTable table;
        private final TableRowSorter<DefaultTableModel> sorter;
        private final JTextField field;
        private final JLabel countLabel;
        private final JLabel emptyLabel;
        private final String emptyMessage;

        FilterChangeListener(JTable table, TableRowSorter<DefaultTableModel> sorter, JTextField field,
                             JLabel countLabel, JLabel emptyLabel, String emptyMessage) {
            this.table = table;
            this.sorter = sorter;
            this.field = field;
            this.countLabel = countLabel;
            this.emptyLabel = emptyLabel;
            this.emptyMessage = emptyMessage;
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            refresh();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            refresh();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            refresh();
        }

        private void refresh() {
            applyRosterFilter(sorter, field, table, countLabel, emptyLabel, emptyMessage);
        }
    }

    private void handleCreateElection() {
        hideCreateElectionError();

        String electionId = newElectionIdField.getText() == null ? "" : newElectionIdField.getText().trim();
        String name = newElectionNameField.getText() == null ? "" : newElectionNameField.getText().trim();
        String description = newElectionDescField.getText() == null ? "" : newElectionDescField.getText().trim();
        String startText = newElectionStartInput.getText();
        String endText = newElectionEndInput.getText();

        if (electionId.isEmpty()) {
            showCreateElectionError("Election ID is required.");
            newElectionIdField.requestFocusInWindow();
            return;
        }
        if (name.isEmpty()) {
            showCreateElectionError("Election title is required.");
            newElectionNameField.requestFocusInWindow();
            return;
        }

        String windowError = DateTimeInput.validateWindow(startText, endText);
        if (windowError != null) {
            showCreateElectionError(windowError);
            newElectionStartInput.requestFocusInWindow();
            return;
        }

        if (electionService.getElection(electionId) != null) {
            showCreateElectionError("An election with ID '" + electionId + "' already exists.");
            newElectionIdField.requestFocusInWindow();
            return;
        }

        LocalDateTime start = DateTimeInput.parse(startText);
        LocalDateTime end = DateTimeInput.parse(endText);

        try {
            Election created = electionService.createElection(electionId, name, description, start, end);
            pendingElectionSelectionId = created.getElectionId();
            resetCreateElectionForm();
            refreshAllData();
            setStatus("Election '" + created.getName() + "' created as DRAFT");

            String warning = DateTimeInput.isEntirelyInThePast(start, end, LocalDateTime.now())
                    ? "\n\nWarning: this voting window is entirely in the past, so no voter will be able to cast a ballot."
                    : "";

            int openNow = JOptionPane.showConfirmDialog(
                    this,
                    "Election '" + created.getName() + "' [" + created.getElectionId() + "] was created as DRAFT."
                            + warning + "\n\nOpen the voting window now?",
                    "Open For Voting?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (openNow == JOptionPane.YES_OPTION) {
                electionService.openElection(created.getElectionId());
                refreshAllData();
                setStatus("Election '" + created.getName() + "' is now OPEN for voting");
            }
        } catch (InvalidElectionException ex) {
            showCreateElectionError(ex.getMessage());
        }
    }

    private void resetCreateElectionForm() {
        if (newElectionIdField == null) {
            return;
        }
        newElectionNameField.setText("");
        newElectionDescField.setText("");

        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        newElectionStartInput.setDateTime(now);
        newElectionEndInput.setDateTime(now.plusDays(1));
        refreshElectionIdSuggestion();
    }

    private void refreshElectionIdSuggestion() {
        if (newElectionIdField == null) {
            return;
        }
        List<String> existingIds = new ArrayList<>();
        for (Election election : electionService.getAllElections()) {
            existingIds.add(election.getElectionId());
        }
        newElectionIdField.setText(suggestNextId("E", existingIds));
    }

    private void showCreateElectionError(String message) {
        if (createElectionErrorLabel == null) {
            return;
        }
        createElectionErrorLabel.setText("\u2022 " + message);
        createElectionErrorLabel.setVisible(true);
        if (createElectionErrorLabel.getParent() != null) {
            createElectionErrorLabel.getParent().revalidate();
            createElectionErrorLabel.getParent().repaint();
        }
        setStatus("Create election failed: " + message);
    }

    private void hideCreateElectionError() {
        if (createElectionErrorLabel == null) {
            return;
        }
        createElectionErrorLabel.setText(" ");
        createElectionErrorLabel.setVisible(false);
    }

    private void handleOpenElection() {
        String eId = getSelectedElectionId(electionSelectorForManage, manageElectionIds);
        if (eId == null) return;

        Election election = electionService.getElection(eId);
        if (election == null) {
            setStatus("Election not found: " + eId);
            return;
        }
        if (election.getStatus() == ElectionStatus.OPEN) {
            JOptionPane.showMessageDialog(this, "Election '" + election.getName() + "' is already OPEN for voting.",
                    "Already Open", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String window = (election.getStartTime() != null && election.getEndTime() != null)
                ? "\n\nVoting window: " + election.getStartTime().format(UI_DATE_FORMAT)
                        + "  \u2192  " + election.getEndTime().format(UI_DATE_FORMAT)
                : "";
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Open '" + election.getName() + "' for voting?" + window
                        + "\n\nEach eligible voter will be able to cast a single ballot while the clock is inside this window.",
                "Confirm Open Election",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            electionService.openElection(eId);
            refreshAllData();
            setStatus("Election '" + election.getName() + "' is now OPEN for voting");
            JOptionPane.showMessageDialog(this,
                    "Election is now OPEN for voting!",
                    "Election Opened", JOptionPane.INFORMATION_MESSAGE);
        } catch (InvalidElectionException ex) {
            setStatus("Open election failed: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleCloseElection() {
        String eId = getSelectedElectionId(electionSelectorForManage, manageElectionIds);
        if (eId == null) return;

        Election election = electionService.getElection(eId);
        if (election == null) {
            setStatus("Election not found: " + eId);
            return;
        }
        if (election.getStatus() == ElectionStatus.CLOSED) {
            JOptionPane.showMessageDialog(this, "Election '" + election.getName() + "' is already CLOSED.",
                    "Already Closed", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Close '" + election.getName() + "' and stop accepting ballots?\n\n"
                        + "Cast ballots are kept and the count is final for this election.",
                "Confirm Close Election",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            electionService.closeElection(eId);
            refreshAllData();
            setStatus("Election '" + election.getName() + "' is now CLOSED");
            JOptionPane.showMessageDialog(this,
                    "Election is now CLOSED. Voting has been stopped.",
                    "Election Closed", JOptionPane.INFORMATION_MESSAGE);
        } catch (InvalidElectionException ex) {
            setStatus("Close election failed: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDeleteElection() {
        String eId = getSelectedElectionId(electionSelectorForManage, manageElectionIds);
        if (eId == null) {
            JOptionPane.showMessageDialog(this, "No election selected to delete.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Election election = electionService.getElection(eId);
        String electionName = (election != null) ? election.getName() : eId;

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to permanently delete this election?\n\n" +
                "Election: " + electionName + " [" + eId + "]\n\n" +
                "WARNING: This will permanently remove:\n" +
                " • The election record\n" +
                " • All candidates registered under this election\n" +
                " • All votes cast in this election\n" +
                " • Reset the voting status of affected voters\n\n" +
                "This action cannot be undone.",
                "Confirm Delete Election",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                electionService.deleteElection(eId);
                JOptionPane.showMessageDialog(
                        this,
                        "Election '" + electionName + "' and all associated candidate and vote records have been deleted.",
                        "Election Deleted",
                        JOptionPane.INFORMATION_MESSAGE
                );
                refreshAllData();
                setStatus("Election '" + electionName + "' and its data were deleted");
            } catch (InvalidElectionException ex) {
                setStatus("Delete election failed: " + ex.getMessage());
                JOptionPane.showMessageDialog(this,
                        "Failed to delete election: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updateElectionStatusDisplay() {
        updateElectionInventory();

        String eId = getSelectedElectionId(electionSelectorForManage, manageElectionIds);
        if (eId == null) {
            electionIdDisplayLbl.setText("-");
            electionNameDisplayLbl.setText("-");
            electionDescDisplayLbl.setText("-");
            electionStartDisplayLbl.setText("-");
            electionEndDisplayLbl.setText("-");
            currentElectionStatusLabel.setText("NO ACTIVE ELECTION");
            currentElectionStatusLabel.setForeground(UITheme.MUTED);

            statActiveElectionName.setText("No Active Election");
            statActiveElectionWindow.setText("No voting window configured.");
            statElectionStatusBadge.setText("NO ELECTION");
            statElectionStatusBadge.setBackground(UITheme.PAPER_ALT);
            statElectionStatusBadge.setForeground(UITheme.MUTED);
            return;
        }
        Election e = electionService.getElection(eId);
        if (e != null) {
            electionIdDisplayLbl.setText(e.getElectionId());
            electionNameDisplayLbl.setText(e.getName());
            electionDescDisplayLbl.setText(
                    e.getDescription() != null && !e.getDescription().isEmpty()
                            ? e.getDescription()
                            : "Standard campus election");
            electionStartDisplayLbl.setText(e.getStartTime() != null ? e.getStartTime().format(UI_DATE_FORMAT) : "-");
            electionEndDisplayLbl.setText(e.getEndTime() != null ? e.getEndTime().format(UI_DATE_FORMAT) : "-");

            statActiveElectionName.setText(e.getName() + " [" + e.getElectionId() + "]");
            statActiveElectionWindow.setText(e.getStartTime() != null && e.getEndTime() != null
                    ? "Voting window " + e.getStartTime().format(UI_DATE_FORMAT)
                            + "  \u2013  " + e.getEndTime().format(UI_DATE_FORMAT)
                    : "No voting window configured.");

            if (e.getStatus() == ElectionStatus.OPEN) {
                LocalDateTime clock = LocalDateTime.now();
                boolean windowEnded = e.getEndTime() != null && e.getEndTime().isBefore(clock);
                boolean windowUpcoming = e.getStartTime() != null && e.getStartTime().isAfter(clock);

                if (windowEnded) {
                    currentElectionStatusLabel.setText("\u25B2 OPEN \u00B7 WINDOW ENDED "
                            + describeAgo(e.getEndTime(), clock));
                    currentElectionStatusLabel.setForeground(UITheme.DANGER);

                    statElectionStatusBadge.setText("\u25B2 STATUS: WINDOW ENDED");
                    statElectionStatusBadge.setBackground(UITheme.DANGER_TINT);
                    statElectionStatusBadge.setForeground(UITheme.DANGER);
                } else if (windowUpcoming) {
                    currentElectionStatusLabel.setText("\u25B2 OPEN \u00B7 STARTS "
                            + e.getStartTime().format(UI_DATE_FORMAT));
                    currentElectionStatusLabel.setForeground(UITheme.WARNING);

                    statElectionStatusBadge.setText("\u25B2 STATUS: OPENS LATER");
                    statElectionStatusBadge.setBackground(UITheme.WARNING_TINT);
                    statElectionStatusBadge.setForeground(UITheme.WARNING);
                } else {
                    currentElectionStatusLabel.setText("\u25CF OPEN FOR VOTING");
                    currentElectionStatusLabel.setForeground(UITheme.SUCCESS);

                    statElectionStatusBadge.setText("\u25CF STATUS: OPEN");
                    statElectionStatusBadge.setBackground(UITheme.SUCCESS_TINT);
                    statElectionStatusBadge.setForeground(UITheme.SUCCESS);
                }
            } else if (e.getStatus() == ElectionStatus.CLOSED) {
                currentElectionStatusLabel.setText("■ CLOSED");
                currentElectionStatusLabel.setForeground(UITheme.DANGER);

                statElectionStatusBadge.setText("■ STATUS: CLOSED");
                statElectionStatusBadge.setBackground(UITheme.DANGER_TINT);
                statElectionStatusBadge.setForeground(UITheme.DANGER);
            } else {
                currentElectionStatusLabel.setText("▲ DRAFT");
                currentElectionStatusLabel.setForeground(UITheme.WARNING);

                statElectionStatusBadge.setText("▲ STATUS: DRAFT");
                statElectionStatusBadge.setBackground(UITheme.WARNING_TINT);
                statElectionStatusBadge.setForeground(UITheme.WARNING);
            }
        }
    }

    private String getSelectedElectionId(JComboBox<String> combo, List<String> electionIds) {
        if (combo == null || electionIds == null) return null;
        int index = combo.getSelectedIndex();
        if (index < 0 || index >= electionIds.size()) return null;
        return electionIds.get(index);
    }

    private void populateElectionSelector(JComboBox<String> combo, List<String> electionIds,
                                          List<Election> elections, String previouslySelectedId) {
        if (combo == null || electionIds == null) return;

        combo.removeAllItems();
        electionIds.clear();

        for (Election e : elections) {
            electionIds.add(e.getElectionId());
            combo.addItem(e.getName() + " [" + e.getElectionId() + "]");
        }

        if (previouslySelectedId != null) {
            for (int i = 0; i < electionIds.size(); i++) {
                if (electionIds.get(i).equalsIgnoreCase(previouslySelectedId)) {
                    combo.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void refreshAllData() {
        List<Election> elections = electionService.getAllElections();

        String previousManageId = (pendingElectionSelectionId != null)
                ? pendingElectionSelectionId
                : getSelectedElectionId(electionSelectorForManage, manageElectionIds);
        String previousCandidateId = getSelectedElectionId(electionSelectorForCandidates, candidateElectionIds);
        String previousResultsId = getSelectedElectionId(electionSelectorForResults, resultsElectionIds);

        populateElectionSelector(electionSelectorForManage, manageElectionIds, elections, previousManageId);
        populateElectionSelector(electionSelectorForCandidates, candidateElectionIds, elections, previousCandidateId);
        populateElectionSelector(electionSelectorForResults, resultsElectionIds, elections, previousResultsId);
        pendingElectionSelectionId = null;

        updateElectionStatusDisplay();
        refreshCandidatesTable();
        refreshVotersTable();
        refreshAuditLogs();
        refreshAttentionPanel();

        if (newElectionIdField != null && newElectionIdField.getText().trim().isEmpty()) {
            refreshElectionIdSuggestion();
        }
        if (candidateIdField != null && (candidateIdField.getText().trim().isEmpty() || candidateIdField.getText().trim().startsWith("C"))) {
            candidateIdField.setText(suggestNextId("C", loadCandidateIds()));
        }
        if (voterIdField != null && (voterIdField.getText().trim().isEmpty() || voterIdField.getText().trim().startsWith("V"))) {
            voterIdField.setText(suggestNextId("V", loadVoterIds()));
        }
        if ("DASHBOARD".equals(currentNavKey)) {
            refreshDashboardMetrics();
        }
        if ("RESULTS".equals(currentNavKey)) {
            renderResultsDisplay();
        }
    }

    private String suggestNextId(String prefix, List<String> existingIds) {
        int highest = 0;
        for (String id : existingIds) {
            if (id == null || !id.regionMatches(true, 0, prefix, 0, prefix.length())) continue;
            String suffix = id.substring(prefix.length()).trim();
            try {
                highest = Math.max(highest, Integer.parseInt(suffix));
            } catch (NumberFormatException e) {
                // Skip non-numeric suffix (e.g. custom IDs) when auto-incrementing
            }
        }
        return String.format(Locale.ROOT, "%s%03d", prefix, highest + 1);
    }

    private List<String> loadCandidateIds() {
        List<String> ids = new ArrayList<>();
        for (Candidate c : electionService.getAllCandidates()) {
            ids.add(c.getCandidateId());
        }
        return ids;
    }

    private List<String> loadVoterIds() {
        List<String> ids = new ArrayList<>();
        for (Voter v : electionService.getAllVoters()) {
            ids.add(v.getVoterId());
        }
        return ids;
    }

    private void handleEditCandidate() {
        String targetCandidateId = null;

        int selectedViewRow = candidateTable.getSelectedRow();
        if (selectedViewRow != -1) {
            int modelRow = candidateTable.convertRowIndexToModel(selectedViewRow);
            targetCandidateId = (String) candidateTableModel.getValueAt(modelRow, 0);
        } else {
            String currentElectionId = getSelectedElectionId(electionSelectorForCandidates, candidateElectionIds);
            List<Candidate> candidates = (currentElectionId != null)
                    ? electionService.getCandidatesForElection(currentElectionId)
                    : electionService.getAllCandidates();

            if (candidates.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "There are no candidates available to modify.",
                        "No Candidates",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String[] candidateOptions = new String[candidates.size()];
            for (int i = 0; i < candidates.size(); i++) {
                Candidate c = candidates.get(i);
                candidateOptions[i] = c.getCandidateId() + " \u2014 " + c.getName() + " (" + c.getDepartment() + ")";
            }

            String selectedOption = (String) JOptionPane.showInputDialog(
                    this,
                    "Select the candidate you want to edit / modify:\n(Tip: You can also select or double-click a row in the table)",
                    "Modify Candidate",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    candidateOptions,
                    candidateOptions[0]);

            if (selectedOption == null) {
                return;
            }

            for (Candidate c : candidates) {
                if (selectedOption.startsWith(c.getCandidateId() + " \u2014 ")) {
                    targetCandidateId = c.getCandidateId();
                    break;
                }
            }
        }

        if (targetCandidateId == null) {
            return;
        }

        Candidate candidateToEdit = null;
        for (Candidate c : electionService.getAllCandidates()) {
            if (c.getCandidateId().equalsIgnoreCase(targetCandidateId)) {
                candidateToEdit = c;
                break;
            }
        }

        if (candidateToEdit == null) {
            JOptionPane.showMessageDialog(this, "Candidate not found.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        openModifyCandidateDialog(candidateToEdit);
    }

    private void openModifyCandidateDialog(Candidate candidate) {
        JDialog dialog = new JDialog(this, "Modify Candidate \u2014 " + candidate.getName() + " (" + candidate.getCandidateId() + ")", true);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(UITheme.PAPER);
        dialog.setResizable(false);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(UITheme.PAPER);
        content.setBorder(new EmptyBorder(24, 28, 20, 28));

        JLabel kicker = UITheme.createKicker("ROSTER MANAGEMENT \u00B7 CANDIDATE PROFILE");
        kicker.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Edit Candidate Details");
        title.setFont(UITheme.FONT_HEADER);
        title.setForeground(UITheme.INK);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel desc = new JLabel("<html><div style='width:420px; line-height:1.4; color:#52525B;'>"
                + "Modify the candidate's full name, academic department, and campaign slogan below. "
                + "Candidate ID and target election are locked to maintain election integrity.</div></html>");
        desc.setFont(UITheme.FONT_BODY);
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel idLabel = new JLabel("Candidate ID: " + candidate.getCandidateId() + "   \u2022   Target Election: " + candidate.getElectionId());
        idLabel.setFont(UITheme.FONT_SMALL_BOLD);
        idLabel.setForeground(UITheme.INK);
        idLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel nameLabel = new JLabel("Full Name:");
        nameLabel.setFont(UITheme.FONT_SMALL_BOLD);
        nameLabel.setForeground(UITheme.MUTED);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField nameField = UITheme.createTextField(20);
        nameField.setText(candidate.getName());
        nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        nameField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel deptLabel = new JLabel("Department / Faculty:");
        deptLabel.setFont(UITheme.FONT_SMALL_BOLD);
        deptLabel.setForeground(UITheme.MUTED);
        deptLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField deptField = UITheme.createTextField(20);
        deptField.setText(candidate.getDepartment());
        deptField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        deptField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sloganLabel = new JLabel("Campaign Slogan / Statement:");
        sloganLabel.setFont(UITheme.FONT_SMALL_BOLD);
        sloganLabel.setForeground(UITheme.MUTED);
        sloganLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField sloganField = UITheme.createTextField(20);
        sloganField.setText(candidate.getSlogan());
        sloganField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        sloganField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel errLabel = new JLabel(" ");
        errLabel.setFont(UITheme.FONT_SMALL_BOLD);
        errLabel.setForeground(UITheme.DANGER);
        errLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(kicker);
        content.add(Box.createVerticalStrut(4));
        content.add(title);
        content.add(Box.createVerticalStrut(8));
        content.add(desc);
        content.add(Box.createVerticalStrut(14));
        content.add(idLabel);
        content.add(Box.createVerticalStrut(14));
        content.add(nameLabel);
        content.add(Box.createVerticalStrut(6));
        content.add(nameField);
        content.add(Box.createVerticalStrut(12));
        content.add(deptLabel);
        content.add(Box.createVerticalStrut(6));
        content.add(deptField);
        content.add(Box.createVerticalStrut(12));
        content.add(sloganLabel);
        content.add(Box.createVerticalStrut(6));
        content.add(sloganField);
        content.add(Box.createVerticalStrut(6));
        content.add(errLabel);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setBackground(UITheme.PAPER_ALT);
        btnRow.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, UITheme.RULE),
                new EmptyBorder(12, 28, 12, 28)
        ));

        JButton cancelBtn = UITheme.createSecondaryButton("Cancel");
        JButton saveBtn = UITheme.createPrimaryButton("\u2713 Save Changes");

        cancelBtn.addActionListener(e -> dialog.dispose());

        saveBtn.addActionListener(e -> {
            String newName = nameField.getText().trim();
            String newDept = deptField.getText().trim();
            String newSlogan = sloganField.getText().trim();

            if (newName.isEmpty()) {
                errLabel.setText("\u26A0 Full Name cannot be empty.");
                nameField.requestFocusInWindow();
                return;
            }
            if (newDept.isEmpty()) {
                errLabel.setText("\u26A0 Department cannot be empty.");
                deptField.requestFocusInWindow();
                return;
            }

            try {
                electionService.updateCandidate(candidate.getCandidateId(), newName, newDept, candidate.getElectionId(), newSlogan);
                dialog.dispose();
                JOptionPane.showMessageDialog(this,
                        "Candidate " + candidate.getCandidateId() + " (" + newName + ") updated successfully!",
                        "Changes Saved",
                        JOptionPane.INFORMATION_MESSAGE);
                refreshAllData();
            } catch (InvalidElectionException ex) {
                errLabel.setText("\u26A0 " + ex.getMessage());
            }
        });

        btnRow.add(cancelBtn);
        btnRow.add(saveBtn);

        dialog.add(content, BorderLayout.CENTER);
        dialog.add(btnRow, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(saveBtn);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void handleRemoveCandidate() {
        String targetCandidateId = null;
        String targetCandidateName = null;

        int selectedViewRow = candidateTable.getSelectedRow();
        if (selectedViewRow != -1) {
            int modelRow = candidateTable.convertRowIndexToModel(selectedViewRow);
            targetCandidateId = (String) candidateTableModel.getValueAt(modelRow, 0);
            targetCandidateName = (String) candidateTableModel.getValueAt(modelRow, 1);
        } else {
            String currentElectionId = getSelectedElectionId(electionSelectorForCandidates, candidateElectionIds);
            List<Candidate> candidates = (currentElectionId != null)
                    ? electionService.getCandidatesForElection(currentElectionId)
                    : electionService.getAllCandidates();

            if (candidates.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "There are no candidates available to remove.",
                        "No Candidates",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String[] candidateOptions = new String[candidates.size()];
            for (int i = 0; i < candidates.size(); i++) {
                Candidate c = candidates.get(i);
                candidateOptions[i] = c.getCandidateId() + " \u2014 " + c.getName() + " (" + c.getDepartment() + ")";
            }

            String selectedOption = (String) JOptionPane.showInputDialog(
                    this,
                    "Select the candidate to remove / disqualify from the roster:\n(Tip: You can also select a row in the table directly)",
                    "Remove Candidate",
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    candidateOptions,
                    candidateOptions[0]);

            if (selectedOption == null) {
                return;
            }

            for (Candidate c : candidates) {
                if (selectedOption.startsWith(c.getCandidateId() + " \u2014 ")) {
                    targetCandidateId = c.getCandidateId();
                    targetCandidateName = c.getName();
                    break;
                }
            }
        }

        if (targetCandidateId == null) {
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to remove / disqualify candidate:\n\n"
                        + "   Candidate ID: " + targetCandidateId + "\n"
                        + "   Name: " + targetCandidateName + "\n\n"
                        + "WARNING: This permanently removes the candidate from the election\n"
                        + "and purges any ballots cast for them. This action cannot be undone.",
                "Confirm Candidate Removal",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                electionService.removeCandidate(targetCandidateId);
                JOptionPane.showMessageDialog(
                        this,
                        "Candidate " + targetCandidateName + " (" + targetCandidateId + ") was removed successfully.",
                        "Candidate Removed",
                        JOptionPane.INFORMATION_MESSAGE);
                refreshAllData();
            } catch (InvalidElectionException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error Removing Candidate", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleUpdateVoterEligibility(boolean eligible) {
        int selectedViewRow = voterTable.getSelectedRow();
        if (selectedViewRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a student voter from the table above to change their eligibility.",
                    "No Voter Selected",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int modelRow = voterTable.convertRowIndexToModel(selectedViewRow);
        String voterId = (String) voterTableModel.getValueAt(modelRow, 0);
        String voterName = (String) voterTableModel.getValueAt(modelRow, 1);
        String currentEligible = (String) voterTableModel.getValueAt(modelRow, 4);

        if (eligible && "YES".equalsIgnoreCase(currentEligible)) {
            JOptionPane.showMessageDialog(this,
                    "Voter " + voterName + " (" + voterId + ") is already marked as ELIGIBLE.",
                    "Already Eligible",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (!eligible && "NO".equalsIgnoreCase(currentEligible)) {
            JOptionPane.showMessageDialog(this,
                    "Voter " + voterName + " (" + voterId + ") is already marked as INELIGIBLE.",
                    "Already Ineligible",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String targetStatus = eligible ? "ELIGIBLE" : "INELIGIBLE";
        String prompt = eligible
                ? "Mark student " + voterName + " (" + voterId + ") as ELIGIBLE to vote?"
                : "Mark student " + voterName + " (" + voterId + ") as INELIGIBLE?\n"
                        + "This will prevent them from casting ballots in any election until re-enabled.";

        int confirm = JOptionPane.showConfirmDialog(
                this,
                prompt,
                "Confirm Eligibility Update",
                JOptionPane.YES_NO_OPTION,
                eligible ? JOptionPane.QUESTION_MESSAGE : JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                electionService.setVoterEligibility(voterId, eligible);
                JOptionPane.showMessageDialog(
                        this,
                        "Voter " + voterName + " (" + voterId + ") has been updated to " + targetStatus + ".",
                        "Eligibility Updated",
                        JOptionPane.INFORMATION_MESSAGE);
                refreshAllData();
            } catch (InvalidElectionException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Update Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleDeleteVoter() {
        int selectedViewRow = voterTable.getSelectedRow();
        if (selectedViewRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a voter from the table above to delete their record.",
                    "No Voter Selected",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int modelRow = voterTable.convertRowIndexToModel(selectedViewRow);
        String voterId = (String) voterTableModel.getValueAt(modelRow, 0);
        String voterName = (String) voterTableModel.getValueAt(modelRow, 1);
        String username = (String) voterTableModel.getValueAt(modelRow, 3);
        String ballotStatus = (String) voterTableModel.getValueAt(modelRow, 5);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to PERMANENTLY DELETE voter:\n\n"
                        + "   Voter ID: " + voterId + "\n"
                        + "   Name: " + voterName + "\n"
                        + "   Username: " + username + "\n"
                        + "   Ballot Status: " + ballotStatus + "\n\n"
                        + "WARNING: This removes the voter from the roll and permanently purges\n"
                        + "any cast ballots. This action cannot be undone.",
                "Confirm Voter Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.ERROR_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                electionService.deleteVoter(voterId);
                JOptionPane.showMessageDialog(
                        this,
                        "Voter record for " + voterName + " (" + voterId + ") has been permanently deleted.",
                        "Voter Deleted",
                        JOptionPane.INFORMATION_MESSAGE);
                refreshAllData();
            } catch (InvalidElectionException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error Deleting Voter", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void openOfficialResultDialog() {
        String eId = getSelectedElectionId(electionSelectorForResults, resultsElectionIds);
        if (eId == null) {
            JOptionPane.showMessageDialog(this, "No election available to declare results for.",
                    "No Election Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ResultDialog dialog = new ResultDialog(this, resultService.calculateResult(eId));
        dialog.setVisible(true);
    }

    private void refreshCandidatesTable() {
        if (candidateTableModel == null) return;
        candidateTableModel.setRowCount(0);
        String eId = getSelectedElectionId(electionSelectorForCandidates, candidateElectionIds);
        List<Candidate> candidates = (eId != null)
                ? electionService.getCandidatesForElection(eId)
                : electionService.getAllCandidates();
        for (Candidate c : candidates) {
            candidateTableModel.addRow(new Object[]{
                    c.getCandidateId(),
                    c.getName(),
                    c.getDepartment(),
                    c.getSlogan()
            });
        }
        applyRosterFilter(candidateSorter, candidateFilterField, candidateTable, candidateCountLabel,
                candidateEmptyLabel, CANDIDATE_EMPTY_MESSAGE);
    }

    private void refreshVotersTable() {
        if (voterTableModel == null) return;
        voterTableModel.setRowCount(0);
        for (Voter v : electionService.getAllVoters()) {
            voterTableModel.addRow(new Object[]{
                    v.getVoterId(),
                    v.getName(),
                    v.getDepartment(),
                    v.getUsername(),
                    v.isEligible() ? "YES" : "NO",
                    v.hasVoted()
                            ? (v.getVotedElectionId() == null || v.getVotedElectionId().isEmpty()
                                    ? "VOTED"
                                    : "VOTED (" + v.getVotedElectionId() + ")")
                            : "NOT YET"
            });
        }
        applyRosterFilter(voterSorter, voterFilterField, voterTable, voterCountLabel,
                voterEmptyLabel, VOTER_EMPTY_MESSAGE);
    }

    private void refreshAuditLogs() {
        if (auditTextArea == null) return;
        List<String> logs = auditLogger.readAllLogs();
        if (logs.isEmpty()) {
            auditTextArea.setText("(No audit log entries recorded yet. Click 'Refresh Logs' to reload.)\n");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = logs.size() - 1; i >= 0; i--) { // latest first
            sb.append(logs.get(i)).append("\n");
        }
        auditTextArea.setText(sb.toString());
        auditTextArea.setCaretPosition(0);
    }

    // =========================================================================
    // AUDIT LOG DELETION SECURITY (OPTION 3: PASSWORD PROTECTED AUDIT DELETION)
    // =========================================================================

    private void handleDeleteAuditLogs() {
        if (!fileManager.hasAuditProtectionPassword()) {
            promptSetupAuditPasswordAndProceed();
        } else {
            promptVerifyAuditPasswordAndDelete();
        }
    }

    private void promptSetupAuditPasswordAndProceed() {
        JDialog dialog = new JDialog(this, "Set Audit Protection Password — First-Time Setup", true);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(UITheme.PAPER);
        dialog.setResizable(false);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(UITheme.PAPER);
        content.setBorder(new EmptyBorder(24, 28, 20, 28));

        JLabel kicker = UITheme.createKicker("SECURITY CONFIGURATION \u00B7 FIRST-TIME SETUP");
        kicker.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Protect System Audit Trail");
        title.setFont(UITheme.FONT_HEADER);
        title.setForeground(UITheme.INK);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel desc = new JLabel("<html><div style='width:420px; line-height:1.4; color:#52525B;'>"
                + "Audit logs are tamper-evident institutional records. Before any logs can be deleted, "
                + "you must establish an <b>Audit Protection Password</b>. This password will be required "
                + "for all future log deletion attempts.</div></html>");
        desc.setFont(UITheme.FONT_BODY);
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel l1 = new JLabel("Create Audit Protection Password (min. 4 characters):");
        l1.setFont(UITheme.FONT_SMALL_BOLD);
        l1.setForeground(UITheme.MUTED);
        l1.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPasswordField p1Field = UITheme.createPasswordField(20);
        p1Field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        p1Field.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel l2 = new JLabel("Confirm Audit Protection Password:");
        l2.setFont(UITheme.FONT_SMALL_BOLD);
        l2.setForeground(UITheme.MUTED);
        l2.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPasswordField p2Field = UITheme.createPasswordField(20);
        p2Field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        p2Field.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel errLabel = new JLabel(" ");
        errLabel.setFont(UITheme.FONT_SMALL_BOLD);
        errLabel.setForeground(UITheme.DANGER);
        errLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(kicker);
        content.add(Box.createVerticalStrut(4));
        content.add(title);
        content.add(Box.createVerticalStrut(8));
        content.add(desc);
        content.add(Box.createVerticalStrut(18));
        content.add(l1);
        content.add(Box.createVerticalStrut(6));
        content.add(p1Field);
        content.add(Box.createVerticalStrut(12));
        content.add(l2);
        content.add(Box.createVerticalStrut(6));
        content.add(p2Field);
        content.add(Box.createVerticalStrut(6));
        content.add(errLabel);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setBackground(UITheme.PAPER_ALT);
        btnRow.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, UITheme.RULE),
                new EmptyBorder(12, 28, 12, 28)
        ));

        JButton cancelBtn = UITheme.createSecondaryButton("Cancel");
        JButton submitBtn = UITheme.createPrimaryButton("Set Password & Continue \u2192");

        cancelBtn.addActionListener(e -> dialog.dispose());

        Runnable submitAction = () -> {
            String pass1 = new String(p1Field.getPassword()).trim();
            String pass2 = new String(p2Field.getPassword()).trim();

            if (pass1.isEmpty() || pass1.length() < 4) {
                errLabel.setText("\u26A0 Password must be at least 4 characters long.");
                p1Field.requestFocusInWindow();
                return;
            }
            if (!pass1.equals(pass2)) {
                errLabel.setText("\u26A0 Passwords do not match. Please re-enter.");
                p2Field.setText("");
                p2Field.requestFocusInWindow();
                return;
            }

            boolean ok = fileManager.setAuditProtectionPassword(pass1);
            if (!ok) {
                errLabel.setText("\u26A0 Failed to persist security configuration to disk.");
                return;
            }

            auditLogger.log("Audit security configured", "Audit protection password set by Admin: " + admin.getUsername());
            dialog.dispose();

            int proceed = JOptionPane.showConfirmDialog(
                    this,
                    "Audit Protection Password has been successfully configured!\n\n" +
                    "Do you want to proceed and permanently delete all current audit logs now?",
                    "Confirm Delete Logs",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (proceed == JOptionPane.YES_OPTION) {
                auditLogger.clearAllLogs();
                refreshAuditLogs();
                setStatus("Audit logs permanently deleted");
                JOptionPane.showMessageDialog(this, "All audit logs have been permanently deleted.", "Audit Logs Cleared", JOptionPane.INFORMATION_MESSAGE);
            } else {
                setStatus("Audit protection password configured; logs retained");
                JOptionPane.showMessageDialog(this, "Audit protection password is now active. Your logs remain intact.", "Protected", JOptionPane.INFORMATION_MESSAGE);
            }
        };

        submitBtn.addActionListener(e -> submitAction.run());
        p1Field.addActionListener(e -> p2Field.requestFocusInWindow());
        p2Field.addActionListener(e -> submitAction.run());

        dialog.getRootPane().registerKeyboardAction(
                e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        btnRow.add(cancelBtn);
        btnRow.add(submitBtn);

        dialog.add(content, BorderLayout.CENTER);
        dialog.add(btnRow, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        p1Field.requestFocusInWindow();
        dialog.setVisible(true);
    }

    private void promptVerifyAuditPasswordAndDelete() {
        JDialog dialog = new JDialog(this, "Authorize Audit Log Deletion", true);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(UITheme.PAPER);
        dialog.setResizable(false);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(UITheme.PAPER);
        content.setBorder(new EmptyBorder(24, 28, 20, 28));

        JLabel kicker = UITheme.createKicker("RESTRICTED ACTION \u00B7 PASSWORD REQUIRED");
        kicker.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Delete System Audit Trail");
        title.setFont(UITheme.FONT_HEADER);
        title.setForeground(UITheme.INK);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel warningBox = new JPanel(new BorderLayout());
        warningBox.setBackground(UITheme.DANGER_TINT);
        warningBox.setBorder(new CompoundBorder(
                new MatteBorder(0, 3, 0, 0, UITheme.DANGER),
                new EmptyBorder(10, 12, 10, 12)
        ));
        warningBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        warningBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        JLabel warningLbl = new JLabel("<html><b>PERMANENT DESTRUCTION WARNING:</b><br>"
                + "This action will permanently erase all administrative actions, ballot receipts, "
                + "and event logs from disk. This cannot be undone.</html>");
        warningLbl.setFont(UITheme.FONT_SMALL);
        warningLbl.setForeground(UITheme.DANGER);
        warningBox.add(warningLbl, BorderLayout.CENTER);

        JLabel promptLbl = new JLabel("Enter Audit Protection Password to authorize deletion:");
        promptLbl.setFont(UITheme.FONT_SMALL_BOLD);
        promptLbl.setForeground(UITheme.MUTED);
        promptLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPasswordField passField = UITheme.createPasswordField(20);
        passField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        passField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel errLabel = new JLabel(" ");
        errLabel.setFont(UITheme.FONT_SMALL_BOLD);
        errLabel.setForeground(UITheme.DANGER);
        errLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(kicker);
        content.add(Box.createVerticalStrut(4));
        content.add(title);
        content.add(Box.createVerticalStrut(10));
        content.add(warningBox);
        content.add(Box.createVerticalStrut(16));
        content.add(promptLbl);
        content.add(Box.createVerticalStrut(6));
        content.add(passField);
        content.add(Box.createVerticalStrut(6));
        content.add(errLabel);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setBackground(UITheme.PAPER_ALT);
        btnRow.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, UITheme.RULE),
                new EmptyBorder(12, 28, 12, 28)
        ));

        JButton cancelBtn = UITheme.createSecondaryButton("Cancel");
        JButton deleteBtn = UITheme.createDangerButton("\uD83D\uDD12 Authorize & Delete Permanently");

        cancelBtn.addActionListener(e -> dialog.dispose());

        Runnable authorizeAction = () -> {
            String entered = new String(passField.getPassword()).trim();
            if (entered.isEmpty()) {
                errLabel.setText("\u26A0 Please enter the Audit Protection Password.");
                passField.requestFocusInWindow();
                return;
            }

            if (!fileManager.verifyAuditProtectionPassword(entered)) {
                auditLogger.log("Audit delete authorization failed", "Invalid password attempt by Admin: " + admin.getUsername());
                errLabel.setText("\u26A0 Access Denied: Incorrect password. Deletion blocked.");
                setStatus("Audit log deletion rejected: invalid password");
                passField.setText("");
                passField.requestFocusInWindow();
                return;
            }

            // Authorized!
            dialog.dispose();
            auditLogger.clearAllLogs();
            refreshAuditLogs();
            setStatus("Audit logs permanently deleted");
            JOptionPane.showMessageDialog(
                    this,
                    "Authorization successful.\nAll audit logs have been permanently deleted from disk.",
                    "Audit Logs Deleted",
                    JOptionPane.INFORMATION_MESSAGE
            );
        };

        deleteBtn.addActionListener(e -> authorizeAction.run());
        passField.addActionListener(e -> authorizeAction.run());

        dialog.getRootPane().registerKeyboardAction(
                e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        btnRow.add(cancelBtn);
        btnRow.add(deleteBtn);

        dialog.add(content, BorderLayout.CENTER);
        dialog.add(btnRow, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        passField.requestFocusInWindow();
        dialog.setVisible(true);
    }

    private void handleChangeAuditPassword() {
        if (!fileManager.hasAuditProtectionPassword()) {
            promptSetupAuditPasswordAndProceed();
            return;
        }

        JDialog dialog = new JDialog(this, "Change Audit Protection Password", true);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(UITheme.PAPER);
        dialog.setResizable(false);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(UITheme.PAPER);
        content.setBorder(new EmptyBorder(24, 28, 20, 28));

        JLabel kicker = UITheme.createKicker("SECURITY CONFIGURATION \u00B7 CREDENTIAL UPDATE");
        kicker.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Update Audit Protection Password");
        title.setFont(UITheme.FONT_HEADER);
        title.setForeground(UITheme.INK);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel currLbl = new JLabel("Current Password:");
        currLbl.setFont(UITheme.FONT_SMALL_BOLD);
        currLbl.setForeground(UITheme.MUTED);
        currLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPasswordField currField = UITheme.createPasswordField(20);
        currField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        currField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel newLbl = new JLabel("New Password (min. 4 characters):");
        newLbl.setFont(UITheme.FONT_SMALL_BOLD);
        newLbl.setForeground(UITheme.MUTED);
        newLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPasswordField newField = UITheme.createPasswordField(20);
        newField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        newField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel confLbl = new JLabel("Confirm New Password:");
        confLbl.setFont(UITheme.FONT_SMALL_BOLD);
        confLbl.setForeground(UITheme.MUTED);
        confLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPasswordField confField = UITheme.createPasswordField(20);
        confField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        confField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel errLabel = new JLabel(" ");
        errLabel.setFont(UITheme.FONT_SMALL_BOLD);
        errLabel.setForeground(UITheme.DANGER);
        errLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(kicker);
        content.add(Box.createVerticalStrut(4));
        content.add(title);
        content.add(Box.createVerticalStrut(14));
        content.add(currLbl);
        content.add(Box.createVerticalStrut(4));
        content.add(currField);
        content.add(Box.createVerticalStrut(10));
        content.add(newLbl);
        content.add(Box.createVerticalStrut(4));
        content.add(newField);
        content.add(Box.createVerticalStrut(10));
        content.add(confLbl);
        content.add(Box.createVerticalStrut(4));
        content.add(confField);
        content.add(Box.createVerticalStrut(6));
        content.add(errLabel);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setBackground(UITheme.PAPER_ALT);
        btnRow.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, UITheme.RULE),
                new EmptyBorder(12, 28, 12, 28)
        ));

        JButton cancelBtn = UITheme.createSecondaryButton("Cancel");
        JButton updateBtn = UITheme.createPrimaryButton("Update Password");

        cancelBtn.addActionListener(e -> dialog.dispose());

        Runnable updateAction = () -> {
            String curr = new String(currField.getPassword()).trim();
            String n1 = new String(newField.getPassword()).trim();
            String n2 = new String(confField.getPassword()).trim();

            if (!fileManager.verifyAuditProtectionPassword(curr)) {
                errLabel.setText("\u26A0 Current password is incorrect.");
                currField.setText("");
                currField.requestFocusInWindow();
                return;
            }
            if (n1.isEmpty() || n1.length() < 4) {
                errLabel.setText("\u26A0 New password must be at least 4 characters.");
                newField.requestFocusInWindow();
                return;
            }
            if (!n1.equals(n2)) {
                errLabel.setText("\u26A0 New passwords do not match.");
                confField.setText("");
                confField.requestFocusInWindow();
                return;
            }

            boolean ok = fileManager.setAuditProtectionPassword(n1);
            if (!ok) {
                errLabel.setText("\u26A0 Failed to write updated password to disk.");
                return;
            }

            auditLogger.log("Audit security updated", "Audit protection password updated by Admin: " + admin.getUsername());
            dialog.dispose();
            setStatus("Audit protection password updated");
            JOptionPane.showMessageDialog(this, "Audit Protection Password has been successfully updated.", "Password Updated", JOptionPane.INFORMATION_MESSAGE);
        };

        updateBtn.addActionListener(e -> updateAction.run());
        confField.addActionListener(e -> updateAction.run());

        dialog.getRootPane().registerKeyboardAction(
                e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        btnRow.add(cancelBtn);
        btnRow.add(updateBtn);

        dialog.add(content, BorderLayout.CENTER);
        dialog.add(btnRow, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        currField.requestFocusInWindow();
        dialog.setVisible(true);
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to log out of the Admin Dashboard?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm == JOptionPane.YES_OPTION) {
            LoginFrame loginFrame = new LoginFrame(
                    fileManager, auditLogger, electionService,
                    votingService, resultService, authService);
            loginFrame.setExtendedState(Frame.MAXIMIZED_BOTH);
            loginFrame.setVisible(true);
            dispose();
        }
    }

    private void handleExitApplication() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to exit the VOTUM application?",
                "Confirm Exit",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }

    private void handleResetDemoData() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to restore the official demonstration environment?\n\n" +
                "This action will:\n" +
                " • Reset default administrator credentials (admin / admin123)\n" +
                " • Restore active demo election 'E001' with an active voting window\n" +
                " • Restore 3 default student candidates (C001, C002, C003)\n" +
                " • Restore 5 default student voter accounts (voter1–voter5)\n" +
                " • Purge all test ballots and reset turnout to 0%\n" +
                " • Record the reset event in the audit log\n\n" +
                "Any custom elections or votes cast during testing will be replaced.",
                "Confirm Demo Environment Reset",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            boolean ok = electionService.resetDemoEnvironment();
            if (ok) {
                pendingElectionSelectionId = "E001";
                refreshAllData();
                setStatus("Demonstration environment restored to official baseline");
                JOptionPane.showMessageDialog(
                        this,
                        "Demonstration environment has been successfully restored to the baseline.\n\n" +
                        "Election E001 is now OPEN and active for student voting.",
                        "Demo Reset Successful",
                        JOptionPane.INFORMATION_MESSAGE
                );
            } else {
                setStatus("Demo environment reset failed");
                JOptionPane.showMessageDialog(
                        this,
                        "Failed to write baseline records to disk. Please check directory permissions.",
                        "Reset Failed",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    @Override
    public void dispose() {
        refreshWorker.stop();
        refreshThread.interrupt();
        super.dispose();
    }

    /**
     * Managed background daemon worker that periodically updates live dashboard metrics
     * without freezing the Swing Event Dispatch Thread (EDT).
     *
     * Demonstrates CSC2104 Course Outcomes:
     *  - Implementing Runnable interface
     *  - Thread lifecycle management: start(), run(), interrupt()
     *  - Safe cooperative thread termination using a volatile boolean running flag
     *  - Swing EDT thread-safety using SwingUtilities.invokeLater()
     */
    private final class LiveRefreshWorker implements Runnable {
        private volatile boolean running = true;
        private static final long REFRESH_INTERVAL_MS = 15000;

        public void stop() {
            running = false;
        }

        @Override
        public void run() {
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(REFRESH_INTERVAL_MS);
                    if (!running || !isDisplayable()) {
                        break;
                    }
                    SwingUtilities.invokeLater(() -> {
                        if (isDisplayable() && isVisible()) {
                            refreshDashboardMetrics();
                            refreshAttentionPanel();
                        }
                    });
                } catch (InterruptedException ex) {
                    // Restore interrupt status and exit refresh thread on window dispose
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
}
