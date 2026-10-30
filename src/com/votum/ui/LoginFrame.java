package com.votum.ui;

import com.votum.exception.InvalidLoginException;
import com.votum.model.Admin;
import com.votum.model.Candidate;
import com.votum.model.Election;
import com.votum.model.ElectionStatus;
import com.votum.model.User;
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
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * The VOTUM front page.
 *
 * <p>The sign-in screen borrows the structure of a magazine cover: a masthead
 * across the top, a double rule, a serif headline on the left and an "in this
 * issue" sidebar on the right holding the live election record. The form sits
 * directly on the paper instead of inside a floating card, which is how an
 * editorial page handles a subscription box.</p>
 *
 * <p>Everything below the presentation is unchanged: authentication still goes
 * through {@link AuthenticationService}, the Enter key still submits, the demo
 * quick-fill buttons still exist, and failed logins still clear the password
 * and show an error.</p>
 */
public final class LoginFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    private static final int ASIDE_WIDTH = 340;
    private static final int SIDEBAR_TEXT_WIDTH = 280;
    private static final DateTimeFormatter WINDOW_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter EDITION_DATE =
            DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH);

    private final transient FileManager fileManager;
    private final transient AuditLogger auditLogger;
    private final transient ElectionService electionService;
    private final transient VotingService votingService;
    private final transient ResultService resultService;
    private final transient AuthenticationService authService;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JLabel errorLabel;
    private JPanel errorBanner;
    private JLabel capsLockLabel;
    private JLabel bareHintLabel;
    private boolean passwordHidden = true;
    private boolean signingIn = false;

    public LoginFrame(FileManager fileManager, AuditLogger auditLogger, ElectionService electionService,
                      VotingService votingService, ResultService resultService, AuthenticationService authService) {
        this.fileManager = fileManager;
        this.auditLogger = auditLogger;
        this.electionService = electionService;
        this.votingService = votingService;
        this.resultService = resultService;
        this.authService = authService;

        initUI();
    }

    // =========================================================================
    // PAGE STRUCTURE
    // =========================================================================

    private void initUI() {
        setTitle("VOTUM — Digital Campus Election System");
        setSize(1120, 800);
        setMinimumSize(new Dimension(960, 700));
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(UITheme.PAPER);

        JPanel page = new JPanel(new BorderLayout(0, 0));
        page.setBackground(UITheme.PAPER);
        page.setBorder(new EmptyBorder(26, 36, 30, 36));

        page.add(buildMasthead(), BorderLayout.NORTH);

        JPanel feature = buildSignInColumn();
        JPanel aside = buildLeadStoryColumn();

        JPanel body = UITheme.createEditorialColumns(feature, aside, ASIDE_WIDTH);
        body.setBorder(new EmptyBorder(26, 0, 0, 0));

        page.add(body, BorderLayout.CENTER);

        // A full-screen window keeps the same readable measure as the 1120px
        // default; the extra width becomes symmetric beige margin instead of
        // stretching the login fields across the whole screen.
        add(UITheme.wrapContentScroll(new UITheme.CenteredPage(page, 1120)), BorderLayout.CENTER);
        add(new UITheme.CenteredPage(buildColophon(), 1120), BorderLayout.SOUTH);

        getRootPane().setDefaultButton(loginButton);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                usernameField.requestFocusInWindow();
            }
        });
    }

    /** The colophon line that closes the page. */
    private JPanel buildColophon() {
        JPanel footer = new JPanel(new BorderLayout(16, 0));
        footer.setBackground(UITheme.PAPER);
        footer.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, UITheme.RULE),
                new EmptyBorder(9, 36, 10, 36)
        ));

        JLabel left = UITheme.createKicker("DIGITAL CAMPUS ELECTION SYSTEM");
        left.setForeground(UITheme.MUTED);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        rightPanel.setOpaque(false);

        JLabel right = new JLabel("CAMPUS ELECTION AUTHORITY");
        right.setFont(UITheme.FONT_SMALL);
        right.setForeground(UITheme.FAINT);

        JButton exitBtn = UITheme.createDangerButton("✕ Exit Application");
        exitBtn.setFont(UITheme.FONT_SMALL_BOLD);
        exitBtn.setBorder(new EmptyBorder(6, 14, 6, 14));
        exitBtn.setToolTipText("Close and exit the VOTUM system completely");
        exitBtn.addActionListener(e -> handleExitApplication());

        rightPanel.add(right);
        rightPanel.add(exitBtn);

        footer.add(left, BorderLayout.WEST);
        footer.add(rightPanel, BorderLayout.EAST);
        return footer;
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

    /** Masthead, edition line and the double rule that opens the page. */
    private JPanel buildMasthead() {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setOpaque(false);

        JPanel topLine = new JPanel(new BorderLayout(24, 0));
        topLine.setOpaque(false);
        topLine.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel brand = new JPanel();
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setOpaque(false);

        JLabel logo = new JLabel("VOTUM");
        logo.setFont(UITheme.FONT_MASTHEAD);
        logo.setForeground(UITheme.INK);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel tagline = UITheme.createKicker("DIGITAL CAMPUS ELECTION SYSTEM");
        tagline.setAlignmentX(Component.LEFT_ALIGNMENT);

        brand.add(logo);
        brand.add(Box.createVerticalStrut(2));
        brand.add(tagline);

        JPanel edition = new JPanel();
        edition.setLayout(new BoxLayout(edition, BoxLayout.Y_AXIS));
        edition.setOpaque(false);

        JLabel editionKicker = UITheme.createKicker("CAMPUS ELECTION EDITION");
        editionKicker.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JLabel dateLabel = new JLabel(LocalDate.now().format(EDITION_DATE).toUpperCase(Locale.ROOT));
        dateLabel.setFont(UITheme.FONT_SMALL);
        dateLabel.setForeground(UITheme.MUTED);
        dateLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JLabel authority = new JLabel("CAMPUS ELECTION AUTHORITY");
        authority.setFont(UITheme.FONT_SMALL);
        authority.setForeground(UITheme.FAINT);
        authority.setAlignmentX(Component.RIGHT_ALIGNMENT);

        edition.add(editionKicker);
        edition.add(Box.createVerticalStrut(6));
        edition.add(dateLabel);
        edition.add(Box.createVerticalStrut(3));
        edition.add(authority);

        topLine.add(brand, BorderLayout.WEST);
        topLine.add(edition, BorderLayout.EAST);

        block.add(topLine);
        block.add(Box.createVerticalStrut(14));
        block.add(UITheme.createRule(2, UITheme.INK));
        block.add(Box.createVerticalStrut(3));
        block.add(UITheme.createRule(1, UITheme.RULE));
        return block;
    }

    /** The feature column: headline, deck and the sign-in form. */
    private JPanel buildSignInColumn() {
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setOpaque(false);
        column.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel kicker = UITheme.createKicker("THE CAMPUS BALLOT");
        kicker.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel headline = new JLabel("Sign in to VOTUM");
        headline.setFont(UITheme.FONT_DISPLAY);
        headline.setForeground(UITheme.INK);
        headline.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel deck = new JLabel("Secure \u2022 Verified \u2022 Campus-Wide Democratic Governance");
        deck.setFont(UITheme.FONT_BODY);
        deck.setForeground(UITheme.MUTED);
        deck.setAlignmentX(Component.LEFT_ALIGNMENT);

        column.add(kicker);
        column.add(Box.createVerticalStrut(8));
        column.add(headline);
        column.add(Box.createVerticalStrut(6));
        column.add(deck);
        column.add(Box.createVerticalStrut(24));

        column.add(buildCredentialsForm());
        column.add(Box.createVerticalStrut(22));
        column.add(UITheme.createRule(1, UITheme.RULE));
        column.add(Box.createVerticalStrut(16));
        column.add(buildDemoStrip());
        return column;
    }

    /** Username, password, error banner and the primary action. */
    private JPanel buildCredentialsForm() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);
        form.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Error banner: a red edge and a pale wash, hidden until needed.
        errorLabel = new JLabel(" ");
        errorLabel.setFont(UITheme.FONT_SMALL_BOLD);
        errorLabel.setForeground(UITheme.DANGER);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        errorBanner = new JPanel(new BorderLayout());
        errorBanner.setBackground(UITheme.DANGER_TINT);
        errorBanner.setBorder(new CompoundBorder(
                new MatteBorder(0, 3, 0, 0, UITheme.DANGER),
                new EmptyBorder(10, 12, 10, 12)
        ));
        errorBanner.setAlignmentX(Component.LEFT_ALIGNMENT);
        errorBanner.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        errorBanner.add(errorLabel, BorderLayout.CENTER);
        errorBanner.setVisible(false);

        JLabel userLabel = new JLabel("Username or Student ID");
        userLabel.setFont(UITheme.FONT_SMALL_BOLD);
        userLabel.setForeground(UITheme.MUTED);
        userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        usernameField = UITheme.createTextField(20);
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        userLabel.setLabelFor(usernameField);

        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(UITheme.FONT_SMALL_BOLD);
        passLabel.setForeground(UITheme.MUTED);

        JButton revealButton = UITheme.createLinkButton("Show");
        revealButton.setToolTipText("Show or hide the password you are typing");
        revealButton.addActionListener(e -> {
            passwordHidden = !passwordHidden;
            passwordField.setEchoChar(passwordHidden ? '\u2022' : (char) 0);
            revealButton.setText(passwordHidden ? "Show" : "Hide");
        });

        JPanel passHeader = new JPanel(new BorderLayout());
        passHeader.setOpaque(false);
        passHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        passHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        passHeader.add(passLabel, BorderLayout.WEST);
        passHeader.add(revealButton, BorderLayout.EAST);

        passwordField = UITheme.createPasswordField(20);
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        passLabel.setLabelFor(passwordField);

        capsLockLabel = new JLabel(" ");
        capsLockLabel.setFont(UITheme.FONT_SMALL);
        capsLockLabel.setForeground(UITheme.WARNING);
        capsLockLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        capsLockLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        passwordField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                updateCapsLockHint();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                updateCapsLockHint();
            }
        });
        passwordField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                updateCapsLockHint();
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                capsLockLabel.setText(" ");
            }
        });

        loginButton = UITheme.createPrimaryButton("SIGN IN TO VOTUM");
        loginButton.setFont(UITheme.FONT_BUTTON);
        loginButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        loginButton.setPreferredSize(new Dimension(420, 46));
        loginButton.setMnemonic(KeyEvent.VK_L);
        loginButton.addActionListener((ActionEvent e) -> handleLogin());

        bareHintLabel = new JLabel("Press Enter to sign in  \u2022  Alt+L shortcut");
        bareHintLabel.setFont(UITheme.FONT_SMALL);
        bareHintLabel.setForeground(UITheme.FAINT);
        bareHintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        bareHintLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));

        form.add(errorBanner);
        form.add(Box.createVerticalStrut(10));
        form.add(userLabel);
        form.add(Box.createVerticalStrut(5));
        form.add(usernameField);
        form.add(Box.createVerticalStrut(16));
        form.add(passHeader);
        form.add(Box.createVerticalStrut(5));
        form.add(passwordField);
        form.add(capsLockLabel);
        form.add(Box.createVerticalStrut(16));
        form.add(loginButton);
        form.add(Box.createVerticalStrut(8));
        form.add(bareHintLabel);

        usernameField.addActionListener(e -> passwordField.requestFocusInWindow());
        passwordField.addActionListener(e -> handleLogin());

        DocumentListener dismissError = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                clearLoginError();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                clearLoginError();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                clearLoginError();
            }
        };
        usernameField.getDocument().addDocumentListener(dismissError);
        passwordField.getDocument().addDocumentListener(dismissError);

        return form;
    }

    /** Press credentials: the same three quick-fill accounts as before, now text links. */
    private JPanel buildDemoStrip() {
        JPanel strip = new JPanel();
        strip.setLayout(new BoxLayout(strip, BoxLayout.Y_AXIS));
        strip.setOpaque(false);
        strip.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel header = UITheme.createKicker("DEMO QUICK-FILL ACCESS");
        header.setForeground(UITheme.MUTED);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton fillAdmin = UITheme.createLinkButton("Admin (admin)");
        fillAdmin.setToolTipText("Fill administrative demo credentials (admin / admin123)");
        fillAdmin.addActionListener(e -> fillCredentials("admin", "admin123"));

        JButton fillVoter = UITheme.createLinkButton("Voter 1 (Eligible)");
        fillVoter.setToolTipText("Fill eligible student credentials (voter1 / voter123)");
        fillVoter.addActionListener(e -> fillCredentials("voter1", "voter123"));

        JButton fillIneligible = UITheme.createLinkButton("Voter 5 (Ineligible)");
        fillIneligible.setToolTipText("Fill ineligible student credentials (voter5 / voter123)");
        fillIneligible.addActionListener(e -> fillCredentials("voter5", "voter123"));

        row.add(fillAdmin);
        row.add(fillVoter);
        row.add(fillIneligible);

        strip.add(header);
        strip.add(Box.createVerticalStrut(4));
        strip.add(row);
        return strip;
    }

    private void fillCredentials(String username, String password) {
        usernameField.setText(username);
        passwordField.setText(password);
        clearLoginError();
        loginButton.requestFocusInWindow();
    }

    /**
     * The sidebar: the live lead election, its voting window, status and one
     * candidate slogan treated as a pull quote. No invented content — every
     * value comes from the election service.
     */
    private JPanel buildLeadStoryColumn() {
        JPanel aside = new JPanel();
        aside.setLayout(new BoxLayout(aside, BoxLayout.Y_AXIS));
        aside.setOpaque(false);
        aside.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel kicker = UITheme.createKicker("IN THIS ISSUE");
        kicker.setAlignmentX(Component.LEFT_ALIGNMENT);
        aside.add(kicker);
        aside.add(Box.createVerticalStrut(10));

        Election lead = electionService.getActiveOrFirstElection();
        if (lead == null) {
            JLabel emptyTitle = new JLabel("No election on the record");
            emptyTitle.setFont(UITheme.FONT_HEADER);
            emptyTitle.setForeground(UITheme.INK);
            emptyTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel emptyDeck = UITheme.createWrappedLabel(
                    "Sign in as an administrator to configure the first campus election.",
                    UITheme.FONT_BODY, UITheme.MUTED, SIDEBAR_TEXT_WIDTH);
            emptyDeck.setAlignmentX(Component.LEFT_ALIGNMENT);

            aside.add(emptyTitle);
            aside.add(Box.createVerticalStrut(8));
            aside.add(emptyDeck);
            return aside;
        }

        JLabel title = UITheme.createWrappedLabel(
                lead.getName(), new Font(UITheme.DISPLAY_FONT_FAMILY, Font.BOLD, 22),
                UITheme.INK, SIDEBAR_TEXT_WIDTH);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        aside.add(title);
        aside.add(Box.createVerticalStrut(10));

        String description = (lead.getDescription() == null || lead.getDescription().trim().isEmpty())
                ? "No description recorded for this election."
                : lead.getDescription();
        JLabel deck = UITheme.createWrappedLabel(description, UITheme.FONT_SERIF_BODY, UITheme.MUTED, SIDEBAR_TEXT_WIDTH);
        deck.setAlignmentX(Component.LEFT_ALIGNMENT);
        aside.add(deck);
        aside.add(Box.createVerticalStrut(16));
        aside.add(UITheme.createRule(1, UITheme.RULE));
        aside.add(Box.createVerticalStrut(14));

        if (lead.getStartTime() != null && lead.getEndTime() != null) {
            JLabel windowKicker = UITheme.createKicker("VOTING WINDOW");
            windowKicker.setForeground(UITheme.MUTED);
            windowKicker.setAlignmentX(Component.LEFT_ALIGNMENT);
            aside.add(windowKicker);
            aside.add(Box.createVerticalStrut(4));

            JLabel window = new JLabel(lead.getStartTime().format(WINDOW_FORMAT)
                    + "  \u2013  " + lead.getEndTime().format(WINDOW_FORMAT));
            window.setFont(UITheme.FONT_SMALL);
            window.setForeground(UITheme.INK);
            window.setAlignmentX(Component.LEFT_ALIGNMENT);
            aside.add(window);
            aside.add(Box.createVerticalStrut(12));
        }

        JLabel statusKicker = UITheme.createKicker("STATUS");
        statusKicker.setForeground(UITheme.MUTED);
        statusKicker.setAlignmentX(Component.LEFT_ALIGNMENT);
        aside.add(statusKicker);
        aside.add(Box.createVerticalStrut(6));
        aside.add(buildLeadStatusBadge(lead));

        Candidate quoteCandidate = firstSloganCandidate(lead);
        if (quoteCandidate != null) {
            aside.add(Box.createVerticalStrut(22));
            String attribution = "\u2014 " + quoteCandidate.getName() + ", " + quoteCandidate.getDepartment();
            JPanel quote = UITheme.createPullQuote(quoteCandidate.getSlogan(), attribution, SIDEBAR_TEXT_WIDTH);
            quote.setAlignmentX(Component.LEFT_ALIGNMENT);
            aside.add(quote);
        }

        aside.add(Box.createVerticalStrut(18));
        JLabel colophon = new JLabel("Election ID " + lead.getElectionId());
        colophon.setFont(UITheme.FONT_SMALL);
        colophon.setForeground(UITheme.FAINT);
        colophon.setAlignmentX(Component.LEFT_ALIGNMENT);
        aside.add(colophon);

        return aside;
    }

    private JLabel buildLeadStatusBadge(Election election) {
        ElectionStatus status = election.getStatus();
        LocalDateTime now = LocalDateTime.now();

        Color background;
        Color foreground;
        String text;

        if (status == ElectionStatus.OPEN
                && election.getEndTime() != null
                && election.getEndTime().isBefore(now)) {
            background = UITheme.DANGER_TINT;
            foreground = UITheme.DANGER;
            text = "\u25B2  WINDOW ENDED";
        } else if (status == ElectionStatus.OPEN) {
            background = UITheme.SUCCESS_TINT;
            foreground = UITheme.SUCCESS;
            text = "\u25CF  OPEN FOR VOTING";
        } else if (status == ElectionStatus.CLOSED) {
            background = UITheme.DANGER_TINT;
            foreground = UITheme.DANGER;
            text = "\u25A0  CLOSED";
        } else {
            background = UITheme.WARNING_TINT;
            foreground = UITheme.WARNING;
            text = "\u25B2  DRAFT";
        }

        JLabel badge = UITheme.createBadge(text, background, foreground);
        badge.setAlignmentX(Component.LEFT_ALIGNMENT);
        badge.setMaximumSize(new Dimension(badge.getPreferredSize().width, 28));
        return badge;
    }

    /** First candidate on the ballot who wrote a slogan, used only when one exists. */
    private Candidate firstSloganCandidate(Election election) {
        List<Candidate> candidates = electionService.getCandidatesForElection(election.getElectionId());
        for (Candidate candidate : candidates) {
            if (candidate.getSlogan() != null && !candidate.getSlogan().trim().isEmpty()) {
                return candidate;
            }
        }
        return null;
    }

    // =========================================================================
    // SIGN-IN BEHAVIOUR (unchanged)
    // =========================================================================

    private void updateCapsLockHint() {
        boolean on;
        try {
            on = Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_CAPS_LOCK);
        } catch (Exception unsupported) {
            on = false;
        }
        capsLockLabel.setText(on ? "\u26A0  Caps Lock is on" : " ");
    }

    private void showLoginError(String message) {
        String safe = UITheme.escapeHtml(message == null ? "Sign-in failed." : message);
        errorLabel.setText("<html><div style='width:460px;'>\u26A0 " + safe + "</div></html>");
        errorBanner.setVisible(true);
        revalidateCard();
    }

    private void clearLoginError() {
        if (errorBanner != null && errorBanner.isVisible()) {
            errorBanner.setVisible(false);
            revalidateCard();
        }
    }

    private void revalidateCard() {
        if (errorBanner == null || errorBanner.getParent() == null) {
            return;
        }
        errorBanner.getParent().revalidate();
        errorBanner.getParent().repaint();
    }

    private void handleLogin() {
        if (signingIn) {
            return;
        }
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        setSigningIn(true);
        SwingUtilities.invokeLater(() -> completeLogin(username, password));
    }

    private void completeLogin(String username, String password) {
        try {
            User user = authService.login(username, password);
            clearLoginError();

            if (user instanceof Admin) {
                AdminDashboard adminDashboard = new AdminDashboard(
                        (Admin) user, fileManager, auditLogger, electionService, votingService, resultService, authService
                );
                adminDashboard.setExtendedState(JFrame.MAXIMIZED_BOTH);
                adminDashboard.setVisible(true);
                dispose();
            } else if (user instanceof Voter) {
                VoterDashboard voterDashboard = new VoterDashboard(
                        (Voter) user, fileManager, auditLogger, electionService, votingService, resultService, authService
                );
                voterDashboard.setExtendedState(JFrame.MAXIMIZED_BOTH);
                voterDashboard.setVisible(true);
                dispose();
            } else {
                setSigningIn(false);
            }
        } catch (InvalidLoginException ex) {
            setSigningIn(false);
            // Clear the rejected password first so its change listener cannot
            // dismiss the error banner that we are about to show.
            passwordField.setText("");
            showLoginError(ex.getMessage());
            passwordField.requestFocusInWindow();
        }
    }

    private void setSigningIn(boolean busy) {
        signingIn = busy;
        loginButton.setEnabled(!busy);
        usernameField.setEnabled(!busy);
        passwordField.setEnabled(!busy);
        loginButton.setText(busy ? "SIGNING IN\u2026" : "SIGN IN TO VOTUM");
        bareHintLabel.setText(busy
                ? "Checking your credentials with campus election authority\u2026"
                : "Press Enter to sign in  \u2022  Alt+L shortcut");
        setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }
}
