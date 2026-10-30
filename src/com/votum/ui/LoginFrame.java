package com.votum.ui;

import com.votum.exception.InvalidLoginException;
import com.votum.model.Admin;
import com.votum.model.User;
import com.votum.model.Voter;
import com.votum.service.AuthenticationService;
import com.votum.service.ElectionService;
import com.votum.service.ResultService;
import com.votum.service.VotingService;
import com.votum.storage.AuditLogger;
import com.votum.storage.FileManager;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * The VOTUM front page and authentication portal.
 *
 * <p>
 * Designed with an authoritative broadsheet editorial aesthetic: an
 * edge-to-edge
 * institutional masthead across the top, a double rule border, a left-hand
 * civic
 * integrity exhibit featuring an animated geometric electoral compass, security
 * pillars,
 * and an aligned credential authentication terminal with quick-fill profile
 * cards.
 * </p>
 */
public final class LoginFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter EDITION_DATE = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy",
            Locale.ENGLISH);

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

    private CivicSealAnimationPanel sealAnimationPanel;

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
    // PAGE STRUCTURE & FULL-SCREEN KIOSK LAYOUT
    // =========================================================================

    private void initUI() {
        setTitle("VOTUM — Digital Campus Election System");
        setUndecorated(true);
        setAlwaysOnTop(true);
        setMinimumSize(new Dimension(960, 700));
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds(0, 0, screenSize.width, screenSize.height);
        setExtendedState(Frame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(UITheme.PAPER);

        JPanel page = new JPanel(new BorderLayout(0, 20));
        page.setBackground(UITheme.PAPER);
        page.setBorder(new EmptyBorder(20, 48, 20, 48));

        page.add(buildMasthead(), BorderLayout.NORTH);

        // Body: Two balanced columns grouped in a centered measure to eliminate
        // disalignment
        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.52;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.insets = new Insets(0, 0, 0, 24);
        body.add(buildBrandAndCivicShowcase(), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.48;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.insets = new Insets(0, 24, 0, 0);
        body.add(buildSignInTerminal(), gbc);

        // CenteredPage (1260px) groups the left showcase and right card cleanly in the
        // middle of any display
        page.add(new UITheme.CenteredPage(body, 1260), BorderLayout.CENTER);

        // Responsive scroll wrapper ensuring full adaptive scaling
        add(UITheme.wrapContentScroll(page), BorderLayout.CENTER);
        add(buildColophon(), BorderLayout.SOUTH);

        getRootPane().setDefaultButton(loginButton);
        getRootPane().registerKeyboardAction(
                e -> handleExitApplication(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                usernameField.requestFocusInWindow();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                if (sealAnimationPanel != null) {
                    sealAnimationPanel.stopAnimation();
                }
            }
        });
    }

    /**
     * Masthead, edition line and the double rule that opens the page edge-to-edge.
     */
    private JPanel buildMasthead() {
        JPanel block = new JPanel(new BorderLayout(0, 14));
        block.setBackground(UITheme.PAPER);

        JPanel row = new JPanel(new BorderLayout(24, 0));
        row.setOpaque(false);

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);

        JLabel logo = new JLabel("VOTUM");
        logo.setFont(UITheme.FONT_LOGO);
        logo.setForeground(UITheme.INK);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel kicker = UITheme.createKicker("DIGITAL CAMPUS ELECTION SYSTEM \u00B7 DEMOCRATIC GOVERNANCE");
        kicker.setAlignmentX(Component.LEFT_ALIGNMENT);

        titleBlock.add(logo);
        titleBlock.add(Box.createVerticalStrut(2));
        titleBlock.add(kicker);

        JPanel meta = new JPanel();
        meta.setLayout(new BoxLayout(meta, BoxLayout.Y_AXIS));
        meta.setOpaque(false);

        JLabel edition = new JLabel("CAMPUS ELECTION EDITION");
        edition.setFont(UITheme.FONT_SMALL_BOLD);
        edition.setForeground(UITheme.ACCENT);
        edition.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JLabel date = new JLabel(LocalDate.now().format(EDITION_DATE));
        date.setFont(UITheme.FONT_SMALL);
        date.setForeground(UITheme.MUTED);
        date.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JLabel authority = new JLabel("OFFICIAL BALLOTING PORTAL");
        authority.setFont(UITheme.FONT_SMALL);
        authority.setForeground(UITheme.FAINT);
        authority.setAlignmentX(Component.RIGHT_ALIGNMENT);

        meta.add(edition);
        meta.add(Box.createVerticalStrut(2));
        meta.add(date);
        meta.add(Box.createVerticalStrut(2));
        meta.add(authority);

        row.add(titleBlock, BorderLayout.WEST);
        row.add(meta, BorderLayout.EAST);

        // Heavy masthead double rule: stretches 100% from under VOTUM all the way to
        // the right
        JPanel rulesPanel = new JPanel();
        rulesPanel.setLayout(new BoxLayout(rulesPanel, BoxLayout.Y_AXIS));
        rulesPanel.setOpaque(false);
        rulesPanel.add(UITheme.createRule(2, UITheme.INK));
        rulesPanel.add(Box.createVerticalStrut(3));
        rulesPanel.add(UITheme.createRule(1, UITheme.RULE));

        block.add(row, BorderLayout.CENTER);
        block.add(rulesPanel, BorderLayout.SOUTH);
        return block;
    }

    /**
     * The colophon line that closes the page with edge-to-edge anchor and cleaned
     * items.
     */
    private JPanel buildColophon() {
        JPanel footer = new JPanel(new BorderLayout(16, 0));
        footer.setBackground(UITheme.PAPER);
        footer.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, UITheme.RULE),
                new EmptyBorder(10, 48, 10, 48)));

        // Cleaned left kicker (CSC2104 removed as requested)
        JLabel left = UITheme.createKicker("VOTUM DIGITAL CAMPUS ELECTION MANAGEMENT SYSTEM");
        left.setForeground(UITheme.MUTED);

        // Cleaned center helper (SECURE KIOSK EXAM MODE ACTIVE removed as requested)
        JLabel center = new JLabel("PRESS ESC TO EXIT");
        center.setFont(UITheme.FONT_SMALL);
        center.setForeground(UITheme.FAINT);
        center.setHorizontalAlignment(SwingConstants.CENTER);

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
        footer.add(center, BorderLayout.CENTER);
        footer.add(rightPanel, BorderLayout.EAST);
        return footer;
    }

    // =========================================================================
    // LEFT COLUMN: CIVIC SHOWCASE, ANIMATED INTEGRITY SEAL & GUARANTEE PILLARS
    // =========================================================================

    private JPanel buildBrandAndCivicShowcase() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Matching top padding of 26px to align perfectly with the right card's
        // internal content
        panel.setBorder(new EmptyBorder(26, 0, 0, 0));

        JLabel kicker = UITheme.createKicker("INSTITUTIONAL ELECTION DESK \u00B7 DEMOCRATIC INFRASTRUCTURE");
        kicker.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel(
                "<html>Democracy in Action.<br><span style='color:#DC2626;'>Verified by Design.</span></html>");
        title.setFont(new Font(UITheme.DISPLAY_FONT_FAMILY, Font.BOLD, 32));
        title.setForeground(UITheme.INK);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel description = new JLabel("<html><div style='width:520px; line-height:1.5;'>"
                + "Welcome to VOTUM, the official democratic election portal. "
                + "Engineered for student body governance with end-to-end auditability, "
                + "clock-synchronized voting windows, and tamper-evident ballot verification."
                + "</div></html>");
        description.setFont(UITheme.FONT_BODY);
        description.setForeground(UITheme.MUTED);
        description.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Animated Civic Seal Canvas (Refined with genuine voting drop cycle)
        sealAnimationPanel = new CivicSealAnimationPanel();
        sealAnimationPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sealAnimationPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        sealAnimationPanel.setPreferredSize(new Dimension(540, 260));

        // Three Security Guarantee Cards
        JPanel guarantees = buildGuaranteeCards();
        guarantees.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Architecture Badges Bar
        JPanel archBadges = buildArchitectureBadges();
        archBadges.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(kicker);
        panel.add(Box.createVerticalStrut(8));
        panel.add(title);
        panel.add(Box.createVerticalStrut(10));
        panel.add(description);
        panel.add(Box.createVerticalStrut(16));
        panel.add(sealAnimationPanel);
        panel.add(Box.createVerticalStrut(16));
        panel.add(guarantees);
        panel.add(Box.createVerticalStrut(14));
        panel.add(archBadges);

        return panel;
    }

    private JPanel buildGuaranteeCards() {
        JPanel container = new JPanel(new GridLayout(1, 3, 12, 0));
        container.setOpaque(false);
        container.setMaximumSize(new Dimension(Integer.MAX_VALUE, 115));
        container.setPreferredSize(new Dimension(540, 115));

        container.add(createGuaranteeCard(
                "TAMPER-EVIDENT AUDIT",
                "Every cast ballot is appended to chronological disk logs in real time."));
        container.add(createGuaranteeCard(
                "STRICT WINDOW LOCK",
                "Voting locks automatically at scheduled deadlines with zero leakage."));
        container.add(createGuaranteeCard(
                "ONE-STUDENT GUARANTEE",
                "Duplicate voting attempts are caught and prevented with immediate rejection."));

        return container;
    }

    private JPanel createGuaranteeCard(String kickerText, String bodyText) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(UITheme.PAPER_ALT);
        card.setBorder(new CompoundBorder(
                new LineBorder(UITheme.RULE, 1),
                new EmptyBorder(10, 12, 10, 12)));

        JLabel kicker = UITheme.createKicker(kickerText);
        kicker.setFont(new Font(UITheme.UI_FONT_FAMILY, Font.BOLD, 9));
        kicker.setForeground(UITheme.ACCENT);
        kicker.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel desc = UITheme.createWrappedLabel(bodyText, UITheme.FONT_SMALL, UITheme.MUTED, 150);
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(kicker);
        card.add(Box.createVerticalStrut(4));
        card.add(desc);
        return card;
    }

    private JPanel buildArchitectureBadges() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        panel.add(UITheme.createBadge("☕ Pure Java 17+", UITheme.PAPER_ALT, UITheme.INK));
        panel.add(UITheme.createBadge("📁 Flat-File Persistence", UITheme.PAPER_ALT, UITheme.MUTED));
        panel.add(UITheme.createBadge("🛡 Zero Third-Party Libs", UITheme.PAPER_ALT, UITheme.MUTED));
        panel.add(UITheme.createBadge("🔒 Cryptographic Audit", UITheme.SUCCESS_TINT, UITheme.SUCCESS));

        return panel;
    }

    // =========================================================================
    // RIGHT COLUMN: EXPANDED CREDENTIAL TERMINAL & QUICK-FILL PROFILE GRID
    // =========================================================================

    private JPanel buildSignInTerminal() {
        JPanel card = UITheme.createCardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new CompoundBorder(
                new LineBorder(UITheme.RULE, 1),
                new EmptyBorder(28, 32, 28, 32)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel terminalKicker = UITheme.createKicker("OFFICIAL ACCESS DESK");
        terminalKicker.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel terminalHeadline = new JLabel("Sign In to VOTUM");
        terminalHeadline.setFont(UITheme.FONT_HEADER);
        terminalHeadline.setForeground(UITheme.INK);
        terminalHeadline.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel terminalHint = new JLabel("Enter your student ID or administrator credentials to proceed.");
        terminalHint.setFont(UITheme.FONT_BODY);
        terminalHint.setForeground(UITheme.MUTED);
        terminalHint.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(terminalKicker);
        card.add(Box.createVerticalStrut(6));
        card.add(terminalHeadline);
        card.add(Box.createVerticalStrut(4));
        card.add(terminalHint);
        card.add(Box.createVerticalStrut(18));

        card.add(buildCredentialsForm());
        card.add(Box.createVerticalStrut(18));
        card.add(UITheme.createRule(1, UITheme.RULE));
        card.add(Box.createVerticalStrut(14));
        card.add(buildDemoGrid());
        card.add(Box.createVerticalStrut(16));

        JPanel footnotePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        footnotePanel.setOpaque(false);
        footnotePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        footnotePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

        JLabel footnote = new JLabel("🔒  Official registry verified  \u2022  Cryptographic audit stream");
        footnote.setFont(UITheme.FONT_SMALL);
        footnote.setForeground(UITheme.FAINT);
        footnotePanel.add(footnote);
        card.add(footnotePanel);

        return card;
    }

    private JPanel buildCredentialsForm() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);
        form.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Error banner
        errorLabel = new JLabel(" ");
        errorLabel.setFont(UITheme.FONT_SMALL_BOLD);
        errorLabel.setForeground(UITheme.DANGER);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        errorBanner = new JPanel(new BorderLayout());
        errorBanner.setBackground(UITheme.DANGER_TINT);
        errorBanner.setBorder(new CompoundBorder(
                new MatteBorder(0, 3, 0, 0, UITheme.DANGER),
                new EmptyBorder(10, 14, 10, 14)));
        errorBanner.setAlignmentX(Component.LEFT_ALIGNMENT);
        errorBanner.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        errorBanner.add(errorLabel, BorderLayout.CENTER);
        errorBanner.setVisible(false);

        JLabel userLabel = new JLabel("Username or Student ID");
        userLabel.setFont(UITheme.FONT_SMALL_BOLD);
        userLabel.setForeground(UITheme.MUTED);
        userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        usernameField = UITheme.createTextField(24);
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        usernameField.setPreferredSize(new Dimension(460, 44));
        userLabel.setLabelFor(usernameField);

        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(UITheme.FONT_SMALL_BOLD);
        passLabel.setForeground(UITheme.MUTED);

        JButton revealButton = UITheme.createLinkButton("Show");
        revealButton.setToolTipText("Show or hide password characters");
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

        passwordField = UITheme.createPasswordField(24);
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        passwordField.setPreferredSize(new Dimension(460, 44));
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

        loginButton = UITheme.createPrimaryButton("SIGN IN TO VOTUM \u2192");
        loginButton.setFont(UITheme.FONT_BUTTON);
        loginButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        loginButton.setPreferredSize(new Dimension(460, 46));
        loginButton.setMnemonic(KeyEvent.VK_L);
        loginButton.addActionListener((ActionEvent e) -> handleLogin());

        bareHintLabel = new JLabel("Press Enter to sign in  \u2022  Alt+L shortcut  \u2022  Esc to exit");
        bareHintLabel.setFont(UITheme.FONT_SMALL);
        bareHintLabel.setForeground(UITheme.FAINT);
        bareHintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        bareHintLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));

        form.add(errorBanner);
        form.add(Box.createVerticalStrut(8));
        form.add(userLabel);
        form.add(Box.createVerticalStrut(5));
        form.add(usernameField);
        form.add(Box.createVerticalStrut(14));
        form.add(passHeader);
        form.add(Box.createVerticalStrut(5));
        form.add(passwordField);
        form.add(capsLockLabel);
        form.add(Box.createVerticalStrut(14));
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

    private JPanel buildDemoGrid() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        JLabel header = UITheme.createKicker("DEMO QUICK-FILL ACCESS (CLICK TO FILL)");
        header.setForeground(UITheme.MUTED);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel grid = new JPanel(new GridLayout(2, 2, 10, 8));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 76));

        grid.add(createDemoProfileButton("🏛 Admin Desk", "admin", "admin123", "Officer"));
        grid.add(createDemoProfileButton("🗳 Voter 1 (Eligible)", "voter1", "voter123", "CS Student"));
        grid.add(createDemoProfileButton("🗳 Voter 2 (Eligible)", "voter2", "voter234", "EE Student"));
        grid.add(createDemoProfileButton("🔒 Voter 5 (Ineligible)", "voter5", "voter567", "Voted"));

        panel.add(header);
        panel.add(Box.createVerticalStrut(8));
        panel.add(grid);
        return panel;
    }

    private JButton createDemoProfileButton(String title, String user, String pass, String roleHint) {
        JButton btn = UITheme.createSecondaryButton(title);
        btn.setFont(UITheme.FONT_SMALL_BOLD);
        btn.setToolTipText("Quick-fill " + user + " (" + roleHint + ")");
        btn.addActionListener(e -> {
            fillCredentials(user, pass);
            bareHintLabel.setText("Filled credentials for " + user + " (" + roleHint + ")");
            bareHintLabel.setForeground(UITheme.SUCCESS);
        });
        return btn;
    }

    private void fillCredentials(String username, String password) {
        usernameField.setText(username);
        passwordField.setText(password);
        clearLoginError();
        loginButton.requestFocusInWindow();
    }

    private void handleExitApplication() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to exit the VOTUM application?",
                "Confirm Exit",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            if (sealAnimationPanel != null) {
                sealAnimationPanel.stopAnimation();
            }
            System.exit(0);
        }
    }

    // =========================================================================
    // SIGN-IN BEHAVIOUR & DISPATCH
    // =========================================================================

    private void updateCapsLockHint() {
        boolean on = false;
        try {
            on = Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_CAPS_LOCK);
        } catch (Exception unsupported) {
            if (unsupported != null) { on = false; }
        }
        capsLockLabel.setText(on ? "\u26A0  Caps Lock is on" : " ");
    }

    private void showLoginError(String message) {
        String safe = UITheme.escapeHtml(message == null ? "Sign-in failed." : message);
        errorLabel.setText("<html><div style='width:420px;'>\u26A0 " + safe + "</div></html>");
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
                if (sealAnimationPanel != null) {
                    sealAnimationPanel.stopAnimation();
                }
                AdminDashboard adminDashboard = new AdminDashboard(
                        (Admin) user, fileManager, auditLogger, electionService, votingService, resultService,
                        authService);
                adminDashboard.setExtendedState(Frame.MAXIMIZED_BOTH);
                adminDashboard.setVisible(true);
                dispose();
            } else if (user instanceof Voter) {
                if (sealAnimationPanel != null) {
                    sealAnimationPanel.stopAnimation();
                }
                VoterDashboard voterDashboard = new VoterDashboard(
                        (Voter) user, fileManager, auditLogger, electionService, votingService, resultService,
                        authService);
                voterDashboard.setExtendedState(Frame.MAXIMIZED_BOTH);
                voterDashboard.setVisible(true);
                dispose();
            } else {
                setSigningIn(false);
            }
        } catch (InvalidLoginException ex) {
            setSigningIn(false);
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
        loginButton.setText(busy ? "SIGNING IN\u2026" : "SIGN IN TO VOTUM \u2192");
        bareHintLabel.setText(busy
                ? "Verifying credentials with campus election authority\u2026"
                : "Press Enter to sign in  \u2022  Alt+L shortcut  \u2022  Esc to exit");
        setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    // =========================================================================
    // ENHANCED GENUINE CIVIC SEAL & DEMOCRATIC COMPASS ANIMATION CANVAS
    // =========================================================================

    private static final class CivicSealAnimationPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

        private double rotationAngle = 0.0;
        private double innerAngle = 0.0;
        private double sweepAngle = 0.0;
        private double voteCycle = 0.0; // 0.0 to 1.0 vote-casting cycle
        private final Timer animTimer;

        public CivicSealAnimationPanel() {
            setOpaque(true);
            setBackground(UITheme.PAPER_ALT);
            setBorder(new CompoundBorder(
                    new LineBorder(UITheme.RULE, 1),
                    new EmptyBorder(14, 18, 14, 18)));

            animTimer = new Timer(33, e -> {
                // 1. Dual counter-rotating astrolabe dial rings
                rotationAngle = (rotationAngle + 0.35) % 360.0;
                innerAngle = (innerAngle - 0.22 + 360.0) % 360.0;
                sweepAngle = (sweepAngle + 0.95) % 360.0;

                // 2. Genuine vote casting cycle (~3.5 seconds)
                voteCycle += 0.0095;
                if (voteCycle >= 1.0) {
                    voteCycle = 0.0;
                }
                repaint();
            });
            animTimer.start();
        }

        public void stopAnimation() {
            if (animTimer != null && animTimer.isRunning()) {
                animTimer.stop();
            }
        }

        @Override
        public void removeNotify() {
            super.removeNotify();
            stopAnimation();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int cx = w / 2;
            int cy = h / 2 - 16;

            // 1. Fixed Outer Hairline Ring
            g2.setColor(UITheme.RULE);
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawOval(cx - 78, cy - 78, 156, 156);

            // 2. Soft Orbiting Audit Scanner Sweep
            double sweepRad = Math.toRadians(sweepAngle);
            int sweepX = cx + (int) (Math.cos(sweepRad) * 78);
            int sweepY = cy + (int) (Math.sin(sweepRad) * 78);
            g2.setColor(new Color(UITheme.ACCENT.getRed(), UITheme.ACCENT.getGreen(), UITheme.ACCENT.getBlue(), 35));
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawArc(cx - 78, cy - 78, 156, 156, (int) -sweepAngle, 30);
            g2.setColor(UITheme.ACCENT);
            g2.fillOval(sweepX - 2, sweepY - 2, 4, 4);

            // 3. Rotating Outer Compass Ticks (36 radial marks, clockwise)
            g2.setStroke(new BasicStroke(1.0f));
            double rad1 = Math.toRadians(rotationAngle);
            for (int i = 0; i < 36; i++) {
                double tickAngle = rad1 + (i * Math.PI / 18);
                int innerR = (i % 9 == 0) ? 68 : 72;
                int outerR = 78;
                int x1 = cx + (int) (Math.cos(tickAngle) * innerR);
                int y1 = cy + (int) (Math.sin(tickAngle) * innerR);
                int x2 = cx + (int) (Math.cos(tickAngle) * outerR);
                int y2 = cy + (int) (Math.sin(tickAngle) * outerR);

                g2.setColor((i % 9 == 0) ? UITheme.ACCENT : UITheme.MUTED);
                g2.drawLine(x1, y1, x2, y2);
            }

            // 4. Counter-Rotating Inner Dial (12 diamond markers, counter-clockwise)
            g2.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{3.0f, 4.0f}, 0.0f));
            g2.setColor(UITheme.RULE_DARK);
            g2.drawOval(cx - 64, cy - 64, 128, 128);

            double rad2 = Math.toRadians(innerAngle);
            g2.setStroke(new BasicStroke(1.0f));
            for (int i = 0; i < 12; i++) {
                double dotAngle = rad2 + (i * Math.PI / 6);
                int r = 64;
                int dx = cx + (int) (Math.cos(dotAngle) * r);
                int dy = cy + (int) (Math.sin(dotAngle) * r);
                g2.setColor((i % 3 == 0) ? UITheme.ACCENT : UITheme.RULE_DARK);
                g2.fillRect(dx - 1, dy - 1, 3, 3);
            }

            // 5. Stylized Central Emblem: Medallion + Ballot Box + Genuine Vote Drop Cycle
            g2.setStroke(new BasicStroke(1.5f));
            g2.setColor(UITheme.PAPER);
            g2.fillOval(cx - 42, cy - 42, 84, 84);
            g2.setColor(UITheme.RULE);
            g2.drawOval(cx - 42, cy - 42, 84, 84);

            // Compute authentic ballot casting physics:
            // 0.00 - 0.35: Hovering above slot, ready
            // 0.35 - 0.65: Drops smoothly into slot
            // 0.65 - 0.78: In the box; slot emits verified audit ripple
            // 0.78 - 1.00: New ballot smoothly fades in above
            int ballotY;
            float ballotAlpha = 1.0f;
            boolean showAuditPulse = false;
            float auditPulseProg = 0.0f;

            if (voteCycle < 0.35) {
                double bob = Math.sin(voteCycle / 0.35 * Math.PI) * 2.0;
                ballotY = (int) (cy - 22 + bob);
                ballotAlpha = 1.0f;
            } else if (voteCycle < 0.65) {
                double dropP = (voteCycle - 0.35) / 0.30;
                ballotY = (int) (-22 + dropP * dropP * 24.0) + cy;
                ballotAlpha = 1.0f;
            } else if (voteCycle < 0.78) {
                ballotY = cy + 2;
                ballotAlpha = 0.0f;
                showAuditPulse = true;
                auditPulseProg = (float) ((voteCycle - 0.65) / 0.13);
            } else {
                ballotY = cy - 22;
                ballotAlpha = (float) ((voteCycle - 0.78) / 0.22);
            }

            // Draw ballot paper (if visible)
            if (ballotAlpha > 0.01f) {
                Composite origComp = g2.getComposite();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1.0f, ballotAlpha)));

                g2.setColor(UITheme.PAPER_ALT);
                g2.fillRect(cx - 9, ballotY, 18, 18);
                g2.setColor(UITheme.ACCENT);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRect(cx - 9, ballotY, 18, 18);

                // Checkmark on ballot
                g2.drawLine(cx - 5, ballotY + 9, cx - 1, ballotY + 13);
                g2.drawLine(cx - 1, ballotY + 13, cx + 6, ballotY + 6);

                g2.setComposite(origComp);
            }

            // Draw ballot box body (drawn in front of descending ballot so it slips inside!)
            g2.setColor(UITheme.INK);
            g2.fillRoundRect(cx - 20, cy - 6, 40, 28, 5, 5);

            // Slot opening
            g2.setColor(UITheme.PAPER);
            g2.fillRect(cx - 11, cy - 6, 22, 3);
            g2.setColor(UITheme.RULE_DARK);
            g2.drawRect(cx - 11, cy - 6, 22, 3);

            // Audit pulse: green concentric ripple from the slot upon ballot entry
            if (showAuditPulse) {
                int rSlot = (int) (6 + 22 * auditPulseProg);
                int alphaSlot = Math.max(0, Math.min(255, (int) (180 * (1.0f - auditPulseProg))));
                g2.setColor(new Color(UITheme.SUCCESS.getRed(), UITheme.SUCCESS.getGreen(), UITheme.SUCCESS.getBlue(), alphaSlot));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(cx - rSlot, cy - 4 - rSlot / 2, rSlot * 2, rSlot);
            }

            // 6. Real-time Status Caption with breathing green beacon
            g2.setFont(UITheme.FONT_SMALL_BOLD);
            float beaconAlpha = (float) ((Math.sin(rotationAngle * 0.1) + 1.0) / 2.0);
            int beaconAlphaInt = Math.max(0, Math.min(255, (int) (120 + 135 * beaconAlpha)));
            g2.setColor(new Color(UITheme.SUCCESS.getRed(), UITheme.SUCCESS.getGreen(), UITheme.SUCCESS.getBlue(), beaconAlphaInt));
            g2.fillOval(cx - 148, h - 22, 7, 7);

            g2.setColor(UITheme.ACCENT);
            String liveClock = LocalTime.now().format(TIME_FORMAT);
            String caption = "LIVE BALLOT ENGINE ACTIVE  \u00B7  " + liveClock + "  \u00B7  AUDIT LOG SYNCHRONIZED";
            g2.drawString(caption, cx - 136, h - 16);

            g2.dispose();
        }
    }
}
