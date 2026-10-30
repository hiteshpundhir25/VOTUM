package com.votum.ui;

import com.votum.exception.VotumException;
import com.votum.model.Candidate;
import com.votum.model.Election;
import com.votum.model.ElectionStatus;
import com.votum.model.Vote;
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
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

/**
 * The student's ballot paper.
 *
 * <p>Laid out like a special election issue: the lead story names the election,
 * the deck explains it in serif body text, and the candidate profiles run down
 * the page in numbered columns. The right-hand sidebar is the ballot itself —
 * status, guidance, the submit action and the receipt all stay in one place.</p>
 *
 * <p>The election business rules live in {@link VotingService}; this screen only
 * presents them. The single gate is {@link #canVote()}: eligible, not already
 * voted in this election, and the clock inside the voting window.</p>
 */
public final class VoterDashboard extends JFrame {
    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int ASIDE_WIDTH = 330;
    private static final int HINT_WIDTH = 250;
    private static final Font FONT_LEAD = new Font(UITheme.DISPLAY_FONT_FAMILY, Font.BOLD, 34);
    private static final Font FONT_CANDIDATE = new Font(UITheme.DISPLAY_FONT_FAMILY, Font.BOLD, 20);
    private static final Font FONT_QUOTE_SMALL = new Font(UITheme.DISPLAY_FONT_FAMILY, Font.ITALIC, 14);

    private final transient Voter voter;
    private final transient FileManager fileManager;
    private final transient AuditLogger auditLogger;
    private final transient ElectionService electionService;
    private final transient VotingService votingService;
    private final transient ResultService resultService;
    private final transient AuthenticationService authService;

    private transient Election currentElection;
    private ButtonGroup candidateButtonGroup;
    private JButton submitVoteButton;
    private JLabel statusBadgeLabel;
    private JLabel ballotHintLabel;
    private JPanel ballotReceiptPanel;
    private JPanel ballotCardPanel;
    private final transient List<JPanel> candidateRowPanels = new ArrayList<>();

    public VoterDashboard(Voter voter, FileManager fileManager, AuditLogger auditLogger,
                          ElectionService electionService, VotingService votingService,
                          ResultService resultService, AuthenticationService authService) {
        this.voter = voter;
        this.fileManager = fileManager;
        this.auditLogger = auditLogger;
        this.electionService = electionService;
        this.votingService = votingService;
        this.resultService = resultService;
        this.authService = authService;

        this.currentElection = electionService.getActiveOrFirstElection();

        initUI();
    }

    // =========================================================================
    // PAGE STRUCTURE
    // =========================================================================

    private void initUI() {
        setTitle("VOTUM — Student Voting Portal (" + voter.getName() + ")");
        setUndecorated(true);
        setAlwaysOnTop(true);
        setMinimumSize(new Dimension(920, 700));
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds(0, 0, screenSize.width, screenSize.height);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(UITheme.PAPER);
        setLayout(new BorderLayout(0, 0));

        add(buildMasthead(), BorderLayout.NORTH);

        JPanel page = new JPanel();
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
        page.setBackground(UITheme.PAPER);
        page.setBorder(new EmptyBorder(28, 32, 32, 32));

        JPanel lead = buildElectionLead();
        lead.setAlignmentX(Component.LEFT_ALIGNMENT);
        page.add(lead);
        page.add(Box.createVerticalStrut(26));

        ballotCardPanel = buildBallotFeature();
        ballotCardPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel sidebar = buildBallotSidebar();
        sidebar.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel grid = UITheme.createEditorialColumns(ballotCardPanel, sidebar, ASIDE_WIDTH);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        page.add(grid);

        add(UITheme.wrapContentScroll(page), BorderLayout.CENTER);

        getRootPane().setDefaultButton(submitVoteButton);
        getRootPane().registerKeyboardAction(
                e -> handleExitApplication(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        refreshBallotState();
    }

    /** Masthead with the student's name where a newspaper prints the edition. */
    private JPanel buildMasthead() {
        JPanel topBar = new JPanel(new BorderLayout(24, 0));
        topBar.setBackground(UITheme.PAPER);
        topBar.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 2, 0, UITheme.INK),
                new EmptyBorder(14, 32, 13, 32)
        ));

        JPanel brand = new JPanel();
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setOpaque(false);

        JLabel logo = new JLabel("VOTUM");
        logo.setFont(UITheme.FONT_LOGO);
        logo.setForeground(UITheme.INK);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel kicker = UITheme.createKicker("STUDENT VOTING PORTAL");
        kicker.setAlignmentX(Component.LEFT_ALIGNMENT);

        brand.add(logo);
        brand.add(kicker);

        JPanel student = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        student.setOpaque(false);

        JLabel identity = new JLabel(voter.getName() + "  \u00B7  " + voter.getVoterId()
                + "  \u00B7  " + voter.getDepartment());
        identity.setFont(UITheme.FONT_SMALL);
        identity.setForeground(UITheme.MUTED);

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

        student.add(identity);
        student.add(logoutBtn);
        student.add(exitBtn);

        topBar.add(brand, BorderLayout.WEST);
        topBar.add(student, BorderLayout.EAST);
        return topBar;
    }

    /** Lead story: election name, deck, voting window and status. */
    private JPanel buildElectionLead() {
        JPanel lead = new JPanel();
        lead.setLayout(new BoxLayout(lead, BoxLayout.Y_AXIS));
        lead.setOpaque(false);
        lead.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel kicker = UITheme.createKicker("THE BALLOT");
        kicker.setAlignmentX(Component.LEFT_ALIGNMENT);
        lead.add(kicker);
        lead.add(Box.createVerticalStrut(8));

        String electionName = (currentElection != null) ? currentElection.getName() : "No Active Campus Election";
        JLabel title = UITheme.createWrappedLabel(electionName, FONT_LEAD, UITheme.INK, 640);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        lead.add(title);
        lead.add(Box.createVerticalStrut(10));

        String description;
        if (currentElection == null) {
            description = "There are currently no active elections configured on campus.";
        } else if (currentElection.getDescription() == null || currentElection.getDescription().trim().isEmpty()) {
            description = "No description recorded for this election.";
        } else {
            description = currentElection.getDescription();
        }
        JLabel deck = UITheme.createWrappedLabel(description, UITheme.FONT_SERIF_BODY, UITheme.MUTED, 640);
        deck.setAlignmentX(Component.LEFT_ALIGNMENT);
        lead.add(deck);
        lead.add(Box.createVerticalStrut(16));

        JPanel meta = new JPanel(new BorderLayout(24, 0));
        meta.setOpaque(false);
        meta.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel windowBlock = new JPanel();
        windowBlock.setLayout(new BoxLayout(windowBlock, BoxLayout.Y_AXIS));
        windowBlock.setOpaque(false);

        if (currentElection != null
                && currentElection.getStartTime() != null
                && currentElection.getEndTime() != null) {
            JLabel windowKicker = UITheme.createKicker("VOTING WINDOW");
            windowKicker.setForeground(UITheme.MUTED);
            windowKicker.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel window = new JLabel(currentElection.getStartTime().format(DATE_FORMATTER)
                    + "  \u2013  " + currentElection.getEndTime().format(DATE_FORMATTER));
            window.setFont(UITheme.FONT_SMALL_BOLD);
            window.setForeground(UITheme.INK);
            window.setAlignmentX(Component.LEFT_ALIGNMENT);

            windowBlock.add(windowKicker);
            windowBlock.add(Box.createVerticalStrut(4));
            windowBlock.add(window);
        }

        String countdown = buildWindowCountdown();
        if (!countdown.isEmpty()) {
            JLabel countdownLabel = new JLabel(countdown);
            countdownLabel.setFont(UITheme.FONT_SMALL);
            countdownLabel.setForeground(UITheme.MUTED);
            countdownLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            windowBlock.add(Box.createVerticalStrut(4));
            windowBlock.add(countdownLabel);
        }

        meta.add(windowBlock, BorderLayout.WEST);
        lead.add(meta);
        lead.add(Box.createVerticalStrut(18));
        lead.add(UITheme.createRule(2, UITheme.INK));
        return lead;
    }

    /** The numbered candidate profiles running down the feature column. */
    private JPanel buildBallotFeature() {
        JPanel feature = new JPanel();
        feature.setLayout(new BoxLayout(feature, BoxLayout.Y_AXIS));
        feature.setOpaque(false);
        feature.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel kicker = UITheme.createKicker("CANDIDATES ON THE BALLOT");
        kicker.setForeground(UITheme.MUTED);
        kicker.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel instruction = new JLabel("Select one candidate card, then submit your ballot in the sidebar.");
        instruction.setFont(UITheme.FONT_BODY);
        instruction.setForeground(UITheme.MUTED);
        instruction.setAlignmentX(Component.LEFT_ALIGNMENT);

        feature.add(kicker);
        feature.add(Box.createVerticalStrut(6));
        feature.add(instruction);
        feature.add(Box.createVerticalStrut(16));

        candidateButtonGroup = new ButtonGroup();
        candidateRowPanels.clear();

        boolean ballotEnabled = canVote();
        List<Candidate> candidates = (currentElection != null)
                ? electionService.getCandidatesForElection(currentElection.getElectionId())
                : new ArrayList<>();

        if (currentElection == null) {
            feature.add(buildEmptyState(
                    "There is no campus election to vote in right now. You will be able to cast a ballot "
                            + "as soon as the election officer opens an election."));
        } else if (candidates.isEmpty()) {
            feature.add(buildEmptyState(
                    "No candidates have been registered for this election yet. The election administrator "
                            + "must register candidates before voting opens."));
        } else {
            int number = 1;
            for (Candidate candidate : candidates) {
                JPanel row = createCandidateCard(candidate, ballotEnabled, number);
                row.setAlignmentX(Component.LEFT_ALIGNMENT);
                candidateRowPanels.add(row);
                feature.add(row);
                feature.add(Box.createVerticalStrut(12));
                number++;
            }
            feature.add(Box.createVerticalStrut(4));
            feature.add(UITheme.createKicker(candidates.size() + " candidates on this ballot"));
        }

        return feature;
    }

    private JPanel buildEmptyState(String message) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new CompoundBorder(
                new MatteBorder(2, 0, 2, 0, UITheme.INK),
                new EmptyBorder(16, 0, 16, 0)
        ));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = UITheme.createWrappedLabel(message, UITheme.FONT_SERIF_BODY, UITheme.MUTED, 540);
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    /** Sidebar: status, guidance, submit action and the persistent receipt. */
    private JPanel buildBallotSidebar() {
        JPanel aside = new JPanel();
        aside.setLayout(new BoxLayout(aside, BoxLayout.Y_AXIS));
        aside.setOpaque(false);
        aside.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel kicker = UITheme.createKicker("YOUR BALLOT");
        kicker.setAlignmentX(Component.LEFT_ALIGNMENT);
        aside.add(kicker);
        aside.add(Box.createVerticalStrut(12));

        JLabel statusKicker = UITheme.createKicker("BALLOT STATUS");
        statusKicker.setForeground(UITheme.MUTED);
        statusKicker.setAlignmentX(Component.LEFT_ALIGNMENT);
        aside.add(statusKicker);
        aside.add(Box.createVerticalStrut(6));
        statusBadgeLabel = UITheme.createBadge("", UITheme.PAPER_ALT, UITheme.MUTED);
        updateVoterStatusBadge();
        statusBadgeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        aside.add(statusBadgeLabel);
        aside.add(Box.createVerticalStrut(16));
        aside.add(UITheme.createRule(1, UITheme.RULE));
        aside.add(Box.createVerticalStrut(16));

        JLabel instructionKicker = UITheme.createKicker("INSTRUCTIONS");
        instructionKicker.setForeground(UITheme.MUTED);
        instructionKicker.setAlignmentX(Component.LEFT_ALIGNMENT);
        aside.add(instructionKicker);
        aside.add(Box.createVerticalStrut(8));

        ballotHintLabel = new JLabel(" ");
        ballotHintLabel.setFont(UITheme.FONT_BODY);
        ballotHintLabel.setForeground(UITheme.MUTED);
        ballotHintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        ballotHintLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        aside.add(ballotHintLabel);
        aside.add(Box.createVerticalStrut(18));

        submitVoteButton = UITheme.createPrimaryButton("SUBMIT OFFICIAL BALLOT");
        submitVoteButton.setFont(UITheme.FONT_BUTTON);
        submitVoteButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        submitVoteButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        submitVoteButton.setPreferredSize(new Dimension(270, 46));
        submitVoteButton.addActionListener(e -> handleVoteSubmission());
        submitVoteButton.setEnabled(false);
        aside.add(submitVoteButton);
        aside.add(Box.createVerticalStrut(18));

        // Persistent receipt, shown after a ballot is recorded.
        ballotReceiptPanel = new JPanel();
        ballotReceiptPanel.setLayout(new BoxLayout(ballotReceiptPanel, BoxLayout.Y_AXIS));
        ballotReceiptPanel.setBackground(UITheme.SUCCESS_TINT);
        ballotReceiptPanel.setBorder(new CompoundBorder(
                new MatteBorder(0, 4, 0, 0, UITheme.SUCCESS),
                new EmptyBorder(14, 14, 14, 14)
        ));
        ballotReceiptPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        ballotReceiptPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        ballotReceiptPanel.setVisible(false);
        aside.add(ballotReceiptPanel);

        return aside;
    }

    // =========================================================================
    // CANDIDATE CARDS
    // =========================================================================

    private JPanel createCandidateCard(Candidate candidate, boolean ballotEnabled, int position) {
        JPanel row = new JPanel(new BorderLayout(16, 0));
        row.setBackground(UITheme.PAPER);
        row.setBorder(unselectedCandidateBorder());
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Section number, like a profile in a magazine.
        JLabel indexLabel = new JLabel(String.format(Locale.ROOT, "%02d", position));
        indexLabel.setFont(new Font(UITheme.DISPLAY_FONT_FAMILY, Font.BOLD, 26));
        indexLabel.setForeground(UITheme.ACCENT);
        indexLabel.setHorizontalAlignment(SwingConstants.LEFT);
        indexLabel.setVerticalAlignment(SwingConstants.TOP);

        JPanel indexBox = new JPanel(new BorderLayout());
        indexBox.setOpaque(false);
        indexBox.setPreferredSize(new Dimension(46, 10));
        indexBox.add(indexLabel, BorderLayout.NORTH);

        JPanel details = new JPanel();
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.setOpaque(false);
        details.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel name = new JLabel(candidate.getName());
        name.setFont(FONT_CANDIDATE);
        name.setForeground(UITheme.INK);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel department = UITheme.createKicker(candidate.getDepartment()
                + "  \u00B7  CANDIDATE " + candidate.getCandidateId());
        department.setForeground(UITheme.MUTED);
        department.setAlignmentX(Component.LEFT_ALIGNMENT);

        details.add(name);
        details.add(Box.createVerticalStrut(4));
        details.add(department);

        if (candidate.getSlogan() != null && !candidate.getSlogan().trim().isEmpty()) {
            JLabel slogan = UITheme.createWrappedLabel(
                    "\u201C" + candidate.getSlogan().trim() + "\u201D",
                    FONT_QUOTE_SMALL, UITheme.FAINT, 300);
            slogan.setAlignmentX(Component.LEFT_ALIGNMENT);
            slogan.setBorder(new CompoundBorder(
                    new MatteBorder(1, 0, 1, 0, UITheme.RULE),
                    new EmptyBorder(7, 0, 7, 0)
            ));
            details.add(Box.createVerticalStrut(10));
            details.add(slogan);
        }

        // Selection control and its label.
        JRadioButton radio = UITheme.createRadioButton();
        radio.setActionCommand(candidate.getCandidateId());
        radio.setOpaque(false);
        radio.setEnabled(ballotEnabled);
        radio.setFont(UITheme.FONT_BODY);
        radio.getAccessibleContext().setAccessibleName("Select " + candidate.getName());
        radio.setToolTipText("Select " + candidate.getName() + " for this ballot");
        candidateButtonGroup.add(radio);

        JLabel choiceTag = UITheme.createKicker("SELECT");
        choiceTag.setForeground(UITheme.FAINT);
        choiceTag.setBorder(new EmptyBorder(6, 0, 0, 0));

        JPanel choiceBox = new JPanel();
        choiceBox.setLayout(new BoxLayout(choiceBox, BoxLayout.Y_AXIS));
        choiceBox.setOpaque(false);
        choiceBox.setPreferredSize(new Dimension(88, 10));
        radio.setAlignmentX(Component.CENTER_ALIGNMENT);
        choiceTag.setAlignmentX(Component.CENTER_ALIGNMENT);
        choiceBox.add(radio);
        choiceBox.add(choiceTag);

        row.add(indexBox, BorderLayout.WEST);
        row.add(details, BorderLayout.CENTER);
        row.add(choiceBox, BorderLayout.EAST);

        row.putClientProperty("choiceTag", choiceTag);
        row.putClientProperty("indexLabel", indexLabel);

        MouseAdapter selectCard = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (radio.isEnabled()) {
                    radio.setSelected(true);
                    highlightSelectedCard(row);
                    refreshBallotState();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (radio.isEnabled() && !radio.isSelected()) {
                    row.setBackground(UITheme.PAPER_ALT);
                    row.repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (radio.isEnabled() && !radio.isSelected()) {
                    row.setBackground(UITheme.PAPER);
                    row.repaint();
                }
            }
        };
        attachMouseBehaviour(row, selectCard);

        radio.addActionListener(e -> {
            highlightSelectedCard(row);
            refreshBallotState();
        });

        return row;
    }

    /** Adds the card click/hover behaviour to a component and all of its children. */
    private void attachMouseBehaviour(Component component, MouseAdapter adapter) {
        component.addMouseListener(adapter);
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                attachMouseBehaviour(child, adapter);
            }
        }
    }

    private javax.swing.border.Border selectedCandidateBorder() {
        return new CompoundBorder(
                new MatteBorder(1, 4, 1, 1, UITheme.ACCENT),
                new EmptyBorder(15, 16, 15, 18)
        );
    }

    private javax.swing.border.Border unselectedCandidateBorder() {
        return new CompoundBorder(
                new LineBorder(UITheme.RULE, 1),
                new EmptyBorder(15, 19, 15, 18)
        );
    }

    private void highlightSelectedCard(JPanel selectedRow) {
        for (JPanel rowPanel : candidateRowPanels) {
            JLabel tag = (JLabel) rowPanel.getClientProperty("choiceTag");
            JLabel index = (JLabel) rowPanel.getClientProperty("indexLabel");
            boolean isSelected = rowPanel == selectedRow;

            rowPanel.setBackground(isSelected ? UITheme.ACCENT_TINT : UITheme.PAPER);
            rowPanel.setBorder(isSelected ? selectedCandidateBorder() : unselectedCandidateBorder());
            if (tag != null) {
                tag.setText(isSelected ? "\u2713 SELECTED" : "SELECT");
                tag.setForeground(isSelected ? UITheme.ACCENT_DARK : UITheme.FAINT);
            }
            if (index != null) {
                index.setForeground(isSelected ? UITheme.ACCENT_DARK : UITheme.ACCENT);
            }
            rowPanel.revalidate();
            rowPanel.repaint();
        }
    }

    // =========================================================================
    // BALLOT STATE
    // =========================================================================

    private void refreshBallotState() {
        if (submitVoteButton == null || ballotHintLabel == null) {
            return;
        }
        if (ballotReceiptPanel != null && ballotReceiptPanel.isVisible()) {
            submitVoteButton.setEnabled(false);
            return;
        }

        boolean open = isVotingOpenNow();
        boolean alreadyVoted = hasAlreadyVoted();
        boolean hasSelection = candidateButtonGroup != null && candidateButtonGroup.getSelection() != null;

        submitVoteButton.setEnabled(open && !alreadyVoted && hasSelection);

        if (!voter.isEligible()) {
            setBallotHint("You are marked as ineligible, so a ballot cannot be submitted. "
                    + "Contact the election officer if this is incorrect.", UITheme.DANGER);
        } else if (alreadyVoted) {
            setBallotHint("Your ballot for this election has already been recorded. "
                    + "Duplicate votes are strictly prohibited.", UITheme.SUCCESS);
        } else if (currentElection == null) {
            setBallotHint("There is no election open for voting right now.", UITheme.MUTED);
        } else if (!open) {
            setBallotHint(buildWindowCountdown(), UITheme.WARNING);
        } else if (!hasSelection) {
            setBallotHint("Select a candidate profile to continue \u2014 then press Enter or click "
                    + "'Submit Official Ballot'.", UITheme.MUTED);
        } else {
            String chosen = describeSelectedCandidate(false);
            setBallotHint("Selected choice: <b>" + UITheme.escapeHtml(chosen) + "</b>. "
                    + "Review it carefully \u2014 submission is final.", UITheme.ACCENT_DARK);
        }
    }

    private void setBallotHint(String text, Color color) {
        ballotHintLabel.setText("<html><div style='width:" + HINT_WIDTH + "px;'>" + text + "</div></html>");
        ballotHintLabel.setForeground(color);
        // A label whose maximum height is unbounded swallows all of the
        // sidebar's spare vertical space and pushes the button to the floor.
        Dimension preferred = ballotHintLabel.getPreferredSize();
        ballotHintLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferred.height));
        ballotHintLabel.setVerticalAlignment(SwingConstants.TOP);
    }

    private String describeSelectedCandidate(boolean withId) {
        ButtonModel model = (candidateButtonGroup == null) ? null : candidateButtonGroup.getSelection();
        if (model == null) {
            return "no candidate selected";
        }
        String candidateId = model.getActionCommand();
        if (currentElection != null) {
            for (Candidate candidate : electionService.getCandidatesForElection(currentElection.getElectionId())) {
                if (candidate.getCandidateId().equalsIgnoreCase(candidateId)) {
                    return (withId ? candidate.getName() + " (" + candidate.getCandidateId() + ")" : candidate.getName());
                }
            }
        }
        return candidateId;
    }

    private void showBallotReceipt(Vote vote, String candidateName) {
        if (ballotReceiptPanel == null) {
            return;
        }
        ballotReceiptPanel.removeAll();

        JLabel title = UITheme.createKicker("OFFICIAL BALLOT RECORDED");
        title.setForeground(UITheme.SUCCESS);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel detail = new JLabel("Receipt " + vote.getVoteId());
        detail.setFont(UITheme.FONT_BODY_BOLD);
        detail.setForeground(UITheme.INK);
        detail.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel candidate = new JLabel(candidateName);
        candidate.setFont(UITheme.FONT_SMALL);
        candidate.setForeground(UITheme.INK);
        candidate.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel timestamp = new JLabel(vote.getTimestamp().format(DATE_FORMATTER));
        timestamp.setFont(UITheme.FONT_SMALL);
        timestamp.setForeground(UITheme.MUTED);
        timestamp.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel next = UITheme.createWrappedLabel(
                "Your vote has been securely recorded. Final election results will be certified by the "
                        + "election officer once the voting window closes.",
                UITheme.FONT_SMALL, UITheme.MUTED, HINT_WIDTH);
        next.setAlignmentX(Component.LEFT_ALIGNMENT);

        ballotReceiptPanel.add(title);
        ballotReceiptPanel.add(Box.createVerticalStrut(8));
        ballotReceiptPanel.add(detail);
        ballotReceiptPanel.add(Box.createVerticalStrut(4));
        ballotReceiptPanel.add(candidate);
        ballotReceiptPanel.add(Box.createVerticalStrut(2));
        ballotReceiptPanel.add(timestamp);
        ballotReceiptPanel.add(Box.createVerticalStrut(8));
        ballotReceiptPanel.add(next);
        ballotReceiptPanel.setVisible(true);
        ballotReceiptPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                ballotReceiptPanel.getPreferredSize().height));

        if (ballotCardPanel != null) {
            ballotCardPanel.revalidate();
            ballotCardPanel.repaint();
        }
        ballotReceiptPanel.revalidate();
        ballotReceiptPanel.repaint();
    }

    // =========================================================================
    // TIME WINDOW
    // =========================================================================

    private String buildWindowCountdown() {
        if (currentElection == null
                || currentElection.getStartTime() == null
                || currentElection.getEndTime() == null) {
            return "";
        }
        LocalDateTime now = LocalDateTime.now();

        if (currentElection.getStatus() == ElectionStatus.CLOSED) {
            return "This election has been officially closed by the election officer.";
        }
        if (currentElection.isBeforeStart(now)) {
            return "Voting opens in " + humanizeDuration(Duration.between(now, currentElection.getStartTime())) + ".";
        }
        if (currentElection.isAfterEnd(now)) {
            return "The voting window ended "
                    + humanizeDuration(Duration.between(currentElection.getEndTime(), now)) + " ago.";
        }
        return "Voting closes in " + humanizeDuration(Duration.between(now, currentElection.getEndTime())) + ".";
    }

    private static String humanizeDuration(Duration duration) {
        long totalMinutes = Math.max(duration.toMinutes(), 0);
        long days = totalMinutes / (24 * 60);
        long hours = (totalMinutes % (24 * 60)) / 60;
        long minutes = totalMinutes % 60;
        if (days > 0) {
            return days + "d " + hours + "h";
        }
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }

    private boolean isVotingOpenNow() {
        return currentElection != null && currentElection.isCurrentlyActive(LocalDateTime.now());
    }

    private boolean hasAlreadyVoted() {
        return currentElection != null
                && votingService.hasVoterCastVote(voter.getVoterId(), currentElection.getElectionId());
    }

    private boolean canVote() {
        return voter.isEligible() && !hasAlreadyVoted() && isVotingOpenNow();
    }

    // =========================================================================
    // STATUS BADGE (7-state machine, unchanged logic)
    // =========================================================================

    private void updateVoterStatusBadge() {
        LocalDateTime now = LocalDateTime.now();

        if (!voter.isEligible()) {
            statusBadgeLabel.setText("STATUS: INELIGIBLE");
            statusBadgeLabel.setBackground(UITheme.DANGER_TINT);
            statusBadgeLabel.setForeground(UITheme.DANGER);
        } else if (hasAlreadyVoted()) {
            statusBadgeLabel.setText("\u2713 VOTE SUBMITTED");
            statusBadgeLabel.setBackground(UITheme.SUCCESS_TINT);
            statusBadgeLabel.setForeground(UITheme.SUCCESS);
        } else if (currentElection == null) {
            statusBadgeLabel.setText("\u25A0 NO ACTIVE ELECTION");
            statusBadgeLabel.setBackground(UITheme.PAPER_ALT);
            statusBadgeLabel.setForeground(UITheme.MUTED);
        } else if (currentElection.getStatus() != ElectionStatus.OPEN) {
            statusBadgeLabel.setText("\u25A0 ELECTION CLOSED");
            statusBadgeLabel.setBackground(UITheme.PAPER_ALT);
            statusBadgeLabel.setForeground(UITheme.MUTED);
        } else if (currentElection.isBeforeStart(now)) {
            statusBadgeLabel.setText("\u25B2 VOTING OPENS " + currentElection.getStartTime().format(DATE_FORMATTER));
            statusBadgeLabel.setBackground(UITheme.WARNING_TINT);
            statusBadgeLabel.setForeground(UITheme.WARNING);
        } else if (currentElection.isAfterEnd(now)) {
            statusBadgeLabel.setText("\u25A0 VOTING WINDOW ENDED");
            statusBadgeLabel.setBackground(UITheme.DANGER_TINT);
            statusBadgeLabel.setForeground(UITheme.DANGER);
        } else {
            statusBadgeLabel.setText("\u25CF READY TO VOTE");
            statusBadgeLabel.setBackground(UITheme.ACCENT_TINT);
            statusBadgeLabel.setForeground(UITheme.ACCENT_DARK);
        }
        statusBadgeLabel.revalidate();
    }

    // =========================================================================
    // SUBMISSION (unchanged business flow)
    // =========================================================================

    private void handleVoteSubmission() {
        if (currentElection == null) {
            JOptionPane.showMessageDialog(this, "No active election available to vote in.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!isVotingOpenNow()) {
            JOptionPane.showMessageDialog(this,
                    "Voting is not open right now. This election is not accepting ballots.",
                    "Voting Unavailable", JOptionPane.WARNING_MESSAGE);
            updateVoterStatusBadge();
            submitVoteButton.setEnabled(false);
            return;
        }

        ButtonModel selectedModel = candidateButtonGroup.getSelection();
        if (selectedModel == null) {
            setBallotHint("Select a candidate profile to continue.", UITheme.WARNING);
            return;
        }

        String candidateId = selectedModel.getActionCommand();
        Candidate chosenCandidate = null;
        for (Candidate candidate : electionService.getCandidatesForElection(currentElection.getElectionId())) {
            if (candidate.getCandidateId().equalsIgnoreCase(candidateId)) {
                chosenCandidate = candidate;
                break;
            }
        }

        String candidateName = (chosenCandidate != null) ? chosenCandidate.getName() : candidateId;

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Confirm Your Official Ballot:\n\n"
                        + "Candidate: " + candidateName + " [" + candidateId + "]\n"
                        + "Election:  " + currentElection.getName() + "\n\n"
                        + "WARNING: Once submitted, your vote is final and cannot be changed or re-cast.",
                "Confirm Ballot Submission",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                Vote castVote = votingService.castVote(voter.getVoterId(),
                        currentElection.getElectionId(), candidateId);

                voter.setHasVoted(true);
                voter.setVotedElectionId(currentElection.getElectionId());

                JOptionPane.showMessageDialog(
                        this,
                        "\u2713 Your vote has been successfully cast and recorded!\n\n"
                                + "Vote ID: " + castVote.getVoteId() + "\n"
                                + "Timestamp: " + castVote.getTimestamp().format(DATE_FORMATTER) + "\n\n"
                                + "Thank you for participating in your campus election.",
                        "Ballot Confirmed",
                        JOptionPane.INFORMATION_MESSAGE
                );

                showBallotReceipt(castVote, candidateName);
                updateVoterStatusBadge();
                submitVoteButton.setEnabled(false);
                Enumeration<AbstractButton> elements = candidateButtonGroup.getElements();
                while (elements.hasMoreElements()) {
                    elements.nextElement().setEnabled(false);
                }
                setBallotHint("Thank you \u2014 your ballot has been recorded. No further action is required.",
                        UITheme.SUCCESS);

            } catch (VotumException ex) {
                boolean transientFailure = ex.getMessage() != null
                        && ex.getMessage().toLowerCase(Locale.ROOT).contains("storage");
                setBallotHint(transientFailure
                        ? "Your ballot could not be saved, so nothing was recorded. You can safely press Submit again."
                        : ex.getMessage(), UITheme.DANGER);
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Voting Error", JOptionPane.ERROR_MESSAGE);
                refreshBallotState();
            }
        }
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to log out of the Student Voting Portal?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm == JOptionPane.YES_OPTION) {
            LoginFrame loginFrame = new LoginFrame(fileManager, auditLogger, electionService,
                    votingService, resultService, authService);
            loginFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
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
}
