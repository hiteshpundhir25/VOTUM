package com.votum.ui;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * VOTUM design system: THE EDITORIAL GRID.
 *
 * <p>The visual language is borrowed from a printed newspaper or magazine:
 * a warm beige paper canvas, black ink, one red accent, a serif display face,
 * hairline column rules, kickers above headlines and generous paper space.
 * The interface is built from a handful of small factories so every screen
 * stays consistent.</p>
 *
 * <p><b>Accessibility.</b> Body copy meets WCAG AA on the beige paper
 * ({@link #MUTED} is 5.3:1, {@link #FAINT} is 7.0:1, {@link #ACCENT_DARK}
 * is 5.6:1). The brighter {@link #ACCENT} is reserved for rules, bars,
 * borders and large display type; small text uses {@link #ACCENT_DARK}.
 * Text placed on ink or accent blocks uses {@link #ON_DARK}, so white on
 * {@code #DC2626} still reaches 4.8:1. Focus rings and visible keyboard
 * focus are kept on every custom control.</p>
 *
 * <p><b>Motion.</b> Motion is intentionally light: content rises into view,
 * metric numbers count up and vote bars grow. Animations are disabled when
 * the {@code votum.reduceMotion} system property is {@code true}, when the
 * platform reports reduced motion, or when {@code -Dvotum.motion=off} is set.</p>
 *
 * <p>This class is intentionally static: it is a toolbox of colours, fonts
 * and factory methods, never an object that the application instantiates.</p>
 */
public final class UITheme {

    private UITheme() {
        // Design tokens and factory methods only.
    }

    // =========================================================================
    // 1. PALETTE — WARM BEIGE PAPER, INK AND ONE RED ACCENT
    // =========================================================================

    /** Page background: a warm, light beige paper. */
    public static final Color PAPER = new Color(0xF5EEE1);
    /** Deeper tint used for sidebars, table headers and alternate rows. */
    public static final Color PAPER_ALT = new Color(0xEDE3D2);
    /** Sunken paper used for fields, audit logs and quiet panels. */
    public static final Color PAPER_SUNKEN = new Color(0xF1E8D9);

    /** Text and icons placed on ink, accent or status fills. Pure white for AA contrast. */
    public static final Color ON_DARK = new Color(0xFFFFFF);

    /** Primary text and strong rules. */
    public static final Color INK = new Color(0x000000);
    /** Secondary body text (5.3:1 on the beige paper). */
    public static final Color MUTED = new Color(0x6A6155);
    /** Small metadata text (7.0:1 on the beige paper). */
    public static final Color FAINT = new Color(0x574F45);
    /** Disabled controls only; not used for readable content. */
    public static final Color DISABLED = new Color(0x988D7D);

    /** The single editorial accent, used for rules, bars and large type. */
    public static final Color ACCENT = new Color(0xDC2626);
    /** Accessible red for small text on beige (5.6:1). */
    public static final Color ACCENT_DARK = new Color(0xB91C1C);
    /** Pressed / deepest red. */
    public static final Color ACCENT_DEEP = new Color(0x991B1B);
    /** Pale warm-red wash for selected rows and error banners. */
    public static final Color ACCENT_TINT = new Color(0xF2DDD2);
    /** Soft warm-red wash for hover backgrounds. */
    public static final Color ACCENT_WASH = new Color(0xFAEDE3);

    /** Hairline rule used between columns and around cards. */
    public static final Color RULE = new Color(0xD8CAB3);
    /** Lighter hairline used inside tables. */
    public static final Color RULE_FAINT = new Color(0xE5DAC7);
    /** Darker rule for hover states. */
    public static final Color RULE_DARK = new Color(0xB5A48B);
    /** Focus ring colour. */
    public static final Color FOCUS_RING = new Color(0xDC2626);

    /** Successful / open state (5.6:1 on beige). */
    public static final Color SUCCESS = new Color(0x0F6B4F);
    public static final Color SUCCESS_TINT = new Color(0xE3EDDF);
    public static final Color SUCCESS_BORDER = new Color(0xA9C6AF);

    /** Attention / draft state (5.1:1 on beige). */
    public static final Color WARNING = new Color(0x8A5A00);
    public static final Color WARNING_TINT = new Color(0xF5E7C9);
    public static final Color WARNING_BORDER = new Color(0xDDC79C);

    /** Error / closed state (5.6:1 on beige). */
    public static final Color DANGER = new Color(0xB91C1C);
    public static final Color DANGER_TINT = new Color(0xF2DBD3);
    public static final Color DANGER_BORDER = new Color(0xD9B2A7);

    // =========================================================================
    // 2. TYPOGRAPHY — SERIF DISPLAY, SANS DECK, SERIF LONG-FORM
    // =========================================================================

    /** Sans-serif family resolved from the fonts actually installed. */
    public static final String UI_FONT_FAMILY = resolveFontFamily(
            new String[]{
                    "Inter", "Segoe UI Variable Text", "Segoe UI", "SF Pro Text", "Helvetica Neue",
                    "Noto Sans", "Cantarell", "Roboto", "Arial", "DejaVu Sans"
            },
            Font.SANS_SERIF
    );

    /** Editorial display family: Playfair Display when available, then Georgia. */
    public static final String DISPLAY_FONT_FAMILY = resolveFontFamily(
            new String[]{
                    "Playfair Display", "Georgia", "Times New Roman", "Noto Serif",
                    "Liberation Serif", "DejaVu Serif"
            },
            Font.SERIF
    );

    public static final Font FONT_MASTHEAD = new Font(DISPLAY_FONT_FAMILY, Font.BOLD, 38);
    public static final Font FONT_LOGO = new Font(DISPLAY_FONT_FAMILY, Font.BOLD, 30);
    public static final Font FONT_DISPLAY = new Font(DISPLAY_FONT_FAMILY, Font.BOLD, 25);
    public static final Font FONT_HEADER = new Font(DISPLAY_FONT_FAMILY, Font.BOLD, 18);
    public static final Font FONT_QUOTE = new Font(DISPLAY_FONT_FAMILY, Font.ITALIC, 19);
    public static final Font FONT_SERIF_BODY = new Font(DISPLAY_FONT_FAMILY, Font.PLAIN, 14);
    public static final Font FONT_METRIC = new Font(DISPLAY_FONT_FAMILY, Font.BOLD, 30);

    public static final Font FONT_SUBHEADER = new Font(UI_FONT_FAMILY, Font.BOLD, 13);
    public static final Font FONT_BODY = new Font(UI_FONT_FAMILY, Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD = new Font(UI_FONT_FAMILY, Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font(UI_FONT_FAMILY, Font.PLAIN, 11);
    public static final Font FONT_SMALL_BOLD = new Font(UI_FONT_FAMILY, Font.BOLD, 11);
    public static final Font FONT_KICKER = new Font(UI_FONT_FAMILY, Font.BOLD, 10);
    public static final Font FONT_BUTTON = new Font(UI_FONT_FAMILY, Font.BOLD, 12);
    public static final Font FONT_MONO = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    /**
     * Resolves the first installed family from a preference list.
     * Keeps the editorial type scale stable on macOS, Windows and Linux.
     */
    private static String resolveFontFamily(String[] candidates, String logicalFallback) {
        Set<String> installed = new HashSet<>();
        try {
            for (String family : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
                installed.add(family.toLowerCase(Locale.ROOT));
            }
        } catch (Throwable headlessOrRestricted) {
            assert headlessOrRestricted != null;
            return logicalFallback;
        }
        for (String candidate : candidates) {
            if (installed.contains(candidate.toLowerCase(Locale.ROOT))) {
                return candidate;
            }
        }
        return logicalFallback;
    }

    // =========================================================================
    // 3. MOTION — SHORT, RESPECTFUL OF REDUCED-MOTION SETTINGS
    // =========================================================================

    private static Boolean motionEnabled;

    /** True when entrance animations may run on this machine. */
    public static boolean isMotionEnabled() {
        if (motionEnabled == null) {
            boolean enabled = true;

            String mode = System.getProperty("votum.motion");
            if (mode != null && ("off".equalsIgnoreCase(mode)
                    || "false".equalsIgnoreCase(mode)
                    || "reduced".equalsIgnoreCase(mode))) {
                enabled = false;
            }

            String reduce = System.getProperty("votum.reduceMotion");
            if ("true".equalsIgnoreCase(reduce) || "1".equals(reduce)) {
                enabled = false;
            }

            try {
                Object platformSetting = Toolkit.getDefaultToolkit().getDesktopProperty("awt.reduceMotion");
                if (Boolean.TRUE.equals(platformSetting)) {
                    enabled = false;
                }
            } catch (Throwable ignored) {
                assert ignored != null;
                // Desktop properties are optional; keep the default.
            }

            motionEnabled = enabled;
        }
        return motionEnabled;
    }

    // =========================================================================
    // 4. BUTTONS — FLAT INK BLOCKS AND UNDERLINED TEXT LINKS
    // =========================================================================

    /**
     * A square, flat button with a one-pixel ink edge.
     * The primary button is a solid black block that turns red on hover;
     * the secondary button is an outline that inverts to black.
     */
    public static final class EditorialButton extends JButton {
        private static final long serialVersionUID = 1L;

        private final Color normalBg;
        private final Color hoverBg;
        private final Color pressedBg;
        private final Color normalFg;
        private final Color hoverFg;
        private final Color borderColor;
        private boolean hovered;

        public EditorialButton(String text, Color normalBg, Color hoverBg, Color pressedBg,
                               Color normalFg, Color hoverFg, Color borderColor) {
            super(text);
            this.normalBg = normalBg;
            this.hoverBg = hoverBg;
            this.pressedBg = pressedBg;
            this.normalFg = normalFg;
            this.hoverFg = hoverFg;
            this.borderColor = borderColor;

            setFont(FONT_BUTTON);
            setForeground(normalFg);
            setBackground(normalBg);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(10, 18, 10, 18));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }
            });

            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            Color fill;
            Color textColour;
            Color border;

            if (!isEnabled()) {
                fill = PAPER_ALT;
                textColour = DISABLED;
                border = RULE;
            } else if (getModel().isPressed()) {
                fill = pressedBg;
                textColour = hoverFg;
                border = pressedBg;
            } else if (hovered) {
                fill = hoverBg;
                textColour = hoverFg;
                border = hoverBg;
            } else {
                fill = normalBg;
                textColour = normalFg;
                border = borderColor;
            }

            g2.setColor(fill);
            g2.fillRect(0, 0, width, height);

            g2.setColor(border);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRect(0, 0, width - 1, height - 1);

            if (isFocusOwner() && isEnabled()) {
                g2.setColor(isDark(fill) ? ON_DARK : ACCENT);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRect(3, 3, width - 7, height - 7);
            }

            g2.setFont(getFont());
            g2.setColor(textColour);
            drawCentredText(g2, getText(), width, height);
            g2.dispose();
        }
    }

    /** A text link in the editorial style: no box, a red underline on hover or focus. */
    public static final class LinkButton extends JButton {
        private static final long serialVersionUID = 1L;
        private boolean hovered;

        public LinkButton(String text) {
            super(text);
            setFont(FONT_SMALL_BOLD);
            setForeground(INK);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setBorder(new EmptyBorder(4, 2, 4, 2));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }
            });
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(getFont());
            g2.setColor(isEnabled() ? (hovered || isFocusOwner() ? ACCENT_DARK : INK) : DISABLED);
            FontMetrics fm = g2.getFontMetrics();
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(getText(), 2, y);

            int underlineY = y + fm.getDescent() + 1;
            g2.setColor(hovered || isFocusOwner() ? ACCENT : RULE_DARK);
            g2.drawLine(2, underlineY, 2 + fm.stringWidth(getText()), underlineY);

            if (isFocusOwner()) {
                g2.setColor(ACCENT);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRect(1, 1, getWidth() - 3, getHeight() - 3);
            }
            g2.dispose();
        }
    }

    public static JButton createPrimaryButton(String text) {
        return new EditorialButton(text, INK, ACCENT, ACCENT_DEEP, ON_DARK, ON_DARK, INK);
    }

    public static JButton createDangerButton(String text) {
        return new EditorialButton(text, DANGER, ACCENT_DEEP, new Color(0x7F1D1D), ON_DARK, ON_DARK, DANGER);
    }

    public static JButton createSuccessButton(String text) {
        return new EditorialButton(text, SUCCESS, new Color(0x0B5740), new Color(0x08432F), ON_DARK, ON_DARK, SUCCESS);
    }

    public static JButton createSecondaryButton(String text) {
        return new EditorialButton(text, PAPER, INK, INK, INK, ON_DARK, INK);
    }

    public static JButton createStyledButton(String text, Color normalBg, Color hoverBg, Color fgColor) {
        Color hoverFg = isDark(hoverBg) ? ON_DARK : INK;
        return new EditorialButton(text, normalBg, hoverBg, normalBg.darker(), fgColor, hoverFg, normalBg.darker());
    }

    public static JButton createLinkButton(String text) {
        return new LinkButton(text);
    }

    /**
     * A ballot mark drawn in the design system: an ink ring that becomes a red
     * dot when selected. Keyboard focus adds a thin red frame around the mark.
     */
    public static final class EditorialRadioButton extends JRadioButton {
        private static final long serialVersionUID = 1L;

        public EditorialRadioButton() {
            super();
            setOpaque(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorder(new EmptyBorder(1, 1, 1, 1));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setIcon(new RadioMark(false, false));
            setSelectedIcon(new RadioMark(true, false));
            setDisabledIcon(new RadioMark(false, true));
            setDisabledSelectedIcon(new RadioMark(true, true));
            setPreferredSize(new Dimension(20, 20));
            setMaximumSize(new Dimension(20, 20));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (isFocusOwner()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(ACCENT);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                g2.dispose();
            }
        }

        private static final class RadioMark implements Icon {
            private final boolean selected;
            private final boolean disabled;

            private RadioMark(boolean selected, boolean disabled) {
                this.selected = selected;
                this.disabled = disabled;
            }

            @Override
            public int getIconWidth() {
                return 16;
            }

            @Override
            public int getIconHeight() {
                return 16;
            }

            @Override
            public void paintIcon(Component component, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color ring = disabled ? DISABLED : (selected ? ACCENT : RULE_DARK);
                g2.setColor(ring);
                g2.setStroke(new BasicStroke(selected ? 1.8f : 1.2f));
                g2.drawOval(x + 1, y + 1, 13, 13);
                if (selected) {
                    g2.setColor(ring);
                    g2.fillOval(x + 5, y + 5, 6, 6);
                }
                g2.dispose();
            }
        }
    }

    public static JRadioButton createRadioButton() {
        return new EditorialRadioButton();
    }

    /** A square ballot checkbox: ink frame, red fill, white check. */
    public static final class EditorialCheckBox extends JCheckBox {
        private static final long serialVersionUID = 1L;

        public EditorialCheckBox(String text, boolean selected) {
            super(text == null ? "" : text, selected);
            setOpaque(false);
            setFocusPainted(false);
            setBorder(new EmptyBorder(2, 2, 2, 2));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setIcon(new CheckMark(false, false));
            setSelectedIcon(new CheckMark(true, false));
            setDisabledIcon(new CheckMark(false, true));
            setDisabledSelectedIcon(new CheckMark(true, true));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (isFocusOwner()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(ACCENT);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                g2.dispose();
            }
        }

        private static final class CheckMark implements Icon {
            private final boolean selected;
            private final boolean disabled;

            private CheckMark(boolean selected, boolean disabled) {
                this.selected = selected;
                this.disabled = disabled;
            }

            @Override
            public int getIconWidth() {
                return 17;
            }

            @Override
            public int getIconHeight() {
                return 17;
            }

            @Override
            public void paintIcon(Component component, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (selected) {
                    g2.setColor(disabled ? DISABLED : ACCENT);
                    g2.fillRect(x + 1, y + 1, 14, 14);
                }

                g2.setColor(disabled ? DISABLED : (selected ? ACCENT : RULE_DARK));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRect(x + 1, y + 1, 14, 14);

                if (selected) {
                    g2.setColor(ON_DARK);
                    g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(x + 4, y + 8, x + 7, y + 11);
                    g2.drawLine(x + 7, y + 11, x + 13, y + 4);
                }
                g2.dispose();
            }
        }
    }

    public static JCheckBox createCheckBox(String text, boolean selected) {
        return new EditorialCheckBox(text, selected);
    }

    // =========================================================================
    // 5. SIDEBAR NAVIGATION — A NUMBERED MAGAZINE CONTENTS LIST
    // =========================================================================

    private static final String NAV_ACTIVE_KEY = "votum.nav.active";

    /** Marks a sidebar button as the current section. */
    public static void setNavActive(JButton button, boolean active) {
        if (button == null) {
            return;
        }
        button.putClientProperty(NAV_ACTIVE_KEY, active);
        button.repaint();
    }

    /**
     * A contents-list entry: a section number, a hairline underline and a red
     * edge marker for the current section.
     */
    public static JButton createSidebarButton(String text, boolean active) {
        JButton button = new JButton(text) {
            private static final long serialVersionUID = 1L;
            private boolean hovered;

            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        hovered = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        hovered = false;
                        repaint();
                    }
                });
                addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusGained(FocusEvent e) {
                        repaint();
                    }

                    @Override
                    public void focusLost(FocusEvent e) {
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int width = getWidth();
                int height = getHeight();
                boolean isActive = Boolean.TRUE.equals(getClientProperty(NAV_ACTIVE_KEY));

                g2.setColor(isActive ? PAPER : (hovered ? ACCENT_WASH : PAPER_ALT));
                g2.fillRect(0, 0, width, height);

                g2.setColor(RULE_FAINT);
                g2.drawLine(0, height - 1, width, height - 1);

                if (isActive) {
                    g2.setColor(ACCENT);
                    g2.fillRect(0, 0, 4, height);
                }

                if (isFocusOwner()) {
                    g2.setColor(ACCENT);
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawRect(5, 3, width - 8, height - 7);
                }

                String label = getText() == null ? "" : getText().toUpperCase(Locale.ROOT);
                g2.setFont(FONT_SMALL_BOLD);
                FontMetrics fm = g2.getFontMetrics();
                int baseline = (height - fm.getHeight()) / 2 + fm.getAscent();
                int x = 22;

                int split = label.indexOf("  ");
                if (split > 0) {
                    g2.setColor(ACCENT_DARK);
                    String number = label.substring(0, split);
                    g2.drawString(number, x, baseline);
                    x += fm.stringWidth(number) + 10;
                    label = label.substring(split).trim();
                }

                g2.setColor(isActive ? INK : (hovered ? INK : MUTED));
                g2.drawString(label, x, baseline);
                g2.dispose();
            }
        };

        button.setFont(FONT_SMALL_BOLD);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(0, 18, 0, 12));
        button.setPreferredSize(new Dimension(210, 42));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        button.setBackground(active ? PAPER : PAPER_ALT);
        button.putClientProperty(NAV_ACTIVE_KEY, active);
        return button;
    }

    // =========================================================================
    // 6. EDITORIAL BUILDING BLOCKS — KICKERS, RULES, HEADERS, PULL QUOTES
    // =========================================================================

    /**
     * A small uppercase label with a little extra letter spacing.
     * Used above headlines ("THE CAMPUS BALLOT") and for metadata.
     */
    public static final class KickerLabel extends JLabel {
        private static final long serialVersionUID = 1L;
        private static final float TRACKING = 1.1f;

        public KickerLabel(String text) {
            super(text == null ? "" : text.toUpperCase(Locale.ROOT));
            setFont(FONT_KICKER);
            // Small red text needs the darker accent to stay AA on beige.
            setForeground(ACCENT_DARK);
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            String text = getText() == null ? "" : getText();
            int textWidth = fm.stringWidth(text) + Math.max(0, text.length() - 1) * Math.round(TRACKING);
            Insets insets = getInsets();
            return new Dimension(textWidth + insets.left + insets.right + 2,
                    fm.getHeight() + insets.top + insets.bottom);
        }

        // JLabel reports minimum and maximum sizes from its untracked text,
        // which clips the extra letter spacing. Report the tracked size for all
        // three so layout managers never squeeze a kicker.
        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(getFont());
            g2.setColor(getForeground());

            FontMetrics fm = g2.getFontMetrics();
            Insets insets = getInsets();
            int y = insets.top + (getHeight() - insets.top - insets.bottom - fm.getHeight()) / 2 + fm.getAscent();
            int x = insets.left;
            String text = getText() == null ? "" : getText();
            for (int i = 0; i < text.length(); i++) {
                String character = String.valueOf(text.charAt(i));
                g2.drawString(character, x, y);
                x += fm.stringWidth(character) + Math.round(TRACKING);
            }
            g2.dispose();
        }
    }

    public static JLabel createKicker(String text) {
        return new KickerLabel(text);
    }

    /** A hairline rule that stretches to the width of its container. */
    public static JPanel createRule(int thickness, Color color) {
        JPanel rule = new JPanel();
        rule.setOpaque(true);
        rule.setBackground(color);
        rule.setMinimumSize(new Dimension(1, thickness));
        rule.setPreferredSize(new Dimension(1, thickness));
        rule.setMaximumSize(new Dimension(Integer.MAX_VALUE, thickness));
        rule.setAlignmentX(Component.LEFT_ALIGNMENT);
        return rule;
    }

    /** A one-pixel vertical rule used between columns. */
    public static JPanel createVerticalRule(Color color) {
        JPanel rule = new JPanel();
        rule.setOpaque(true);
        rule.setBackground(color);
        rule.setMinimumSize(new Dimension(1, 1));
        rule.setPreferredSize(new Dimension(1, 1));
        rule.setMaximumSize(new Dimension(1, Integer.MAX_VALUE));
        return rule;
    }

    /**
     * The standard section header: kicker, serif headline, sans deck and a
     * two-pixel rule underneath.
     */
    public static JPanel createSectionHeader(String kicker, String title, String deck) {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel kickerLabel = createKicker(kicker);
        kickerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel(title == null ? "" : title);
        titleLabel.setFont(FONT_DISPLAY);
        titleLabel.setForeground(INK);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(kickerLabel);
        header.add(Box.createVerticalStrut(6));
        header.add(titleLabel);

        if (deck != null && !deck.isEmpty()) {
            JLabel deckLabel = new JLabel(deck);
            deckLabel.setFont(FONT_BODY);
            deckLabel.setForeground(MUTED);
            deckLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            header.add(Box.createVerticalStrut(6));
            header.add(deckLabel);
        }

        header.add(Box.createVerticalStrut(14));
        header.add(createRule(2, INK));
        return header;
    }

    /**
     * A pull quote: two heavy rules, italic serif quotation and a kicker
     * attribution. Lines are wrapped manually because Swing's HTML view does
     * not reliably measure a fixed-width div inside a JLabel.
     */
    public static JPanel createPullQuote(String quote, String attribution, int widthPx) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBorder(new CompoundBorder(
                new MatteBorder(2, 0, 2, 0, INK),
                new EmptyBorder(12, 0, 12, 0)
        ));

        JLabel quoteLabel = new JLabel(wrapHtml("\u201C" + (quote == null ? "" : quote) + "\u201D",
                FONT_QUOTE, widthPx));
        quoteLabel.setFont(FONT_QUOTE);
        quoteLabel.setForeground(INK);
        quoteLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(quoteLabel);

        if (attribution != null && !attribution.isEmpty()) {
            JLabel attributionLabel = createKicker(attribution);
            attributionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(Box.createVerticalStrut(8));
            panel.add(attributionLabel);
        }
        return panel;
    }

    /** A label whose text wraps at a fixed pixel width. */
    public static JLabel createWrappedLabel(String text, Font font, Color color, int widthPx) {
        JLabel label = new JLabel(wrapHtml(text == null ? "" : text, font, widthPx));
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    /**
     * Breaks text into explicit HTML lines measured with real font metrics.
     * This keeps long election names and descriptions inside their column and
     * lets the label report an exact preferred height to its layout manager.
     */
    private static String wrapHtml(String text, Font font, int widthPx) {
        String source = text == null ? "" : text.trim();
        if (source.isEmpty()) {
            return "";
        }

        JLabel measuringLabel = new JLabel();
        measuringLabel.setFont(font);
        FontMetrics metrics = measuringLabel.getFontMetrics(font);

        int limit = Math.max(widthPx, 80);
        StringBuilder html = new StringBuilder("<html>");
        int lineWidth = 0;
        boolean firstWordOnLine = true;

        String[] paragraphs = source.split("\\n", -1);
        for (int p = 0; p < paragraphs.length; p++) {
            if (p > 0) {
                html.append("<br>");
                lineWidth = 0;
                firstWordOnLine = true;
            }
            String[] words = paragraphs[p].trim().split("\\s+");
            for (String word : words) {
                if (word.isEmpty()) {
                    continue;
                }
                int wordWidth = metrics.stringWidth(word);
                int spaceWidth = metrics.stringWidth(" ");
                if (!firstWordOnLine && lineWidth + spaceWidth + wordWidth > limit) {
                    html.append("<br>");
                    lineWidth = 0;
                    firstWordOnLine = true;
                }
                if (!firstWordOnLine) {
                    html.append(' ');
                    lineWidth += spaceWidth;
                }
                html.append(escapeHtml(word));
                lineWidth += wordWidth;
                firstWordOnLine = false;
            }
        }
        return html.append("</html>").toString();
    }

    /**
     * A page is a vertical stack of full-width editorial rows. GridBag is used
     * instead of BoxLayout so the header, lead story and data band always start
     * at the left margin and stay top-aligned.
     */
    public static JPanel createPage(int top, int left, int bottom, int right) {
        JPanel page = new JPanel(new GridBagLayout());
        page.setBackground(PAPER);
        page.setBorder(new EmptyBorder(top, left, bottom, right));
        page.setAlignmentX(Component.LEFT_ALIGNMENT);
        return page;
    }

    /**
     * Centres a fixed-width editorial page inside a wider window while still
     * shrinking with the window. Used by the login front page so a full-screen
     * window keeps a readable measure instead of stretching the form fields.
     */
    public static final class CenteredPage extends JPanel {
        private static final long serialVersionUID = 1L;

        private final JComponent page;
        private final int maximumWidth;

        public CenteredPage(JComponent page, int maximumWidth) {
            super(null);
            this.page = page;
            this.maximumWidth = Math.max(320, maximumWidth);
            setBackground(PAPER);
            add(page);
        }

        @Override
        public void doLayout() {
            int width = Math.min(getWidth(), maximumWidth);
            int x = (getWidth() - width) / 2;
            page.setBounds(x, 0, width, getHeight());
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension pageSize = page.getPreferredSize();
            return new Dimension(Math.min(pageSize.width, maximumWidth), pageSize.height);
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(0, 0);
        }
    }

    /** Adds one full-width row to a page created with {@link #createPage}. */
    public static void addPageRow(JPanel page, Component row, int gapAfter) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = GridBagConstraints.RELATIVE;
        constraints.weightx = 1.0;
        constraints.weighty = 0.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, 0, gapAfter, 0);
        page.add(row, constraints);
    }

    /** Escapes text before it is placed inside a Swing HTML label. */
    public static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    /**
     * Places a feature column and a sidebar next to each other, separated by a
     * hairline column rule. This is the asymmetric grid the whole design rests on.
     */
    public static JPanel createEditorialColumns(JComponent feature, JComponent sidebar, int sidebarWidth) {
        JPanel grid = new JPanel(new BorderLayout(0, 0));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel sidebarShell = new JPanel(new BorderLayout()) {
            private static final long serialVersionUID = 1L;

            @Override
            public Dimension getPreferredSize() {
                // Width is fixed by the grid; height follows the live sidebar
                // content so a receipt or a long note is never clipped.
                Dimension content = sidebar.getPreferredSize();
                return new Dimension(sidebarWidth, Math.max(content.height, 1));
            }
        };
        sidebarShell.setOpaque(false);
        sidebarShell.setBorder(new CompoundBorder(
                new MatteBorder(0, 1, 0, 0, INK),
                new EmptyBorder(0, 28, 0, 0)
        ));
        sidebarShell.add(sidebar, BorderLayout.CENTER);

        // A fixed gutter keeps text, fields and cards from touching the column rule.
        JPanel featureShell = new JPanel(new BorderLayout());
        featureShell.setOpaque(false);
        featureShell.setBorder(new EmptyBorder(0, 0, 0, 24));
        featureShell.add(feature, BorderLayout.CENTER);

        grid.add(featureShell, BorderLayout.CENTER);
        grid.add(sidebarShell, BorderLayout.EAST);
        return grid;
    }

    // =========================================================================
    // 7. CARDS, METRICS AND STATUS LABELS
    // =========================================================================

    /** A flat paper card outlined with a one-pixel hairline. */
    public static JPanel createCardPanel() {
        JPanel panel = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(getBackground());
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(RULE);
                g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBackground(PAPER);
        panel.setBorder(new EmptyBorder(18, 22, 18, 22));
        return panel;
    }

    /**
     * A live metric block in the editorial data band: an ink rule, a small
     * accent block, a kicker, a large serif number and a caption.
     */
    public static final class StatCard extends JPanel {
        private static final long serialVersionUID = 1L;

        private static final Pattern NUMERIC = Pattern.compile("^(-?\\d+(?:\\.\\d+)?)(.*)$");

        private final JLabel valueLabel;
        private final JLabel subtitleLabel;
        private final Color accentColor;
        private Timer valueTimer;

        public StatCard(String label, String value, String subtitle, Color accentColor) {
            this.accentColor = (accentColor != null) ? accentColor : ACCENT;
            setLayout(new BorderLayout(0, 0));
            setOpaque(false);
            setBackground(PAPER);
            setBorder(new EmptyBorder(16, 18, 14, 18));
            setMinimumSize(new Dimension(120, 104));

            JPanel text = new JPanel();
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            text.setOpaque(false);

            JLabel titleLabel = createKicker(label == null ? "" : label);
            titleLabel.setForeground(MUTED);
            titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            valueLabel = new JLabel(value == null ? "0" : value);
            valueLabel.setFont(FONT_METRIC);
            valueLabel.setForeground(INK);
            valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            subtitleLabel = new JLabel(subtitle == null ? "" : subtitle);
            subtitleLabel.setFont(FONT_SMALL);
            subtitleLabel.setForeground(FAINT);
            subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            text.add(titleLabel);
            text.add(Box.createVerticalStrut(4));
            text.add(valueLabel);
            text.add(Box.createVerticalStrut(2));
            text.add(subtitleLabel);

            add(text, BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(PAPER);
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setColor(RULE);
            g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

            g2.setColor(INK);
            g2.fillRect(0, 0, getWidth(), 3);

            g2.setColor(accentColor);
            g2.fillRect(0, 0, Math.min(30, getWidth()), 3);

            g2.dispose();
            super.paintComponent(g);
        }

        /** Updates the number, counting up when motion is allowed. */
        public void setValue(String value) {
            String next = value == null ? "0" : value;
            if (valueTimer != null) {
                valueTimer.stop();
            }
            if (!isMotionEnabled()) {
                valueLabel.setText(next);
                return;
            }

            Matcher target = NUMERIC.matcher(next.trim());
            Matcher current = NUMERIC.matcher(valueLabel.getText() == null ? "" : valueLabel.getText().trim());
            if (!target.matches() || !current.matches()) {
                valueLabel.setText(next);
                return;
            }

            double from;
            double to;
            int decimals;
            try {
                from = Double.parseDouble(current.group(1));
                to = Double.parseDouble(target.group(1));
                decimals = decimalPlaces(target.group(1));
            } catch (NumberFormatException malformed) {
                assert malformed != null;
                valueLabel.setText(next);
                return;
            }
            String suffix = target.group(2);

            final int steps = 18;
            final double step = (to - from) / steps;
            final double startValue = from;
            final int digits = decimals;
            final String tail = suffix;
            final int[] tick = {0};

            valueTimer = new Timer(16, event -> {
                tick[0]++;
                double shown = startValue + step * tick[0];
                if (tick[0] >= steps) {
                    shown = to;
                    ((Timer) event.getSource()).stop();
                }
                valueLabel.setText(String.format(Locale.ROOT, "%." + digits + "f", shown) + tail);
            });
            valueTimer.start();
        }

        public void setSubtitle(String subtitle) {
            subtitleLabel.setText(subtitle == null ? "" : subtitle);
        }

        private static int decimalPlaces(String number) {
            int dot = number.indexOf('.');
            return dot < 0 ? 0 : number.length() - dot - 1;
        }
    }

    public static StatCard createStatCard(String label, String value, String subtitle, Color accentColor) {
        return new StatCard(label, value, subtitle, accentColor);
    }

    /**
     * A square status label with a tinted background, matching hairline border
     * and accessible dark text.
     */
    public static JLabel createBadge(String text, Color bg, Color fg) {
        JLabel label = new JLabel(text == null ? "" : text, SwingConstants.CENTER) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(getBackground());
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(mix(getForeground(), PAPER, 0.35f));
                g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        label.setFont(FONT_SMALL_BOLD);
        label.setBackground(bg);
        label.setForeground(fg);
        label.setOpaque(false);
        label.setBorder(new EmptyBorder(5, 10, 5, 10));
        return label;
    }

    // =========================================================================
    // 8. INPUTS, TABLES AND SCROLLBARS
    // =========================================================================

    public static JTextField createTextField(int columns) {
        JTextField field = new JTextField(columns);
        styleInput(field);
        return field;
    }

    public static JPasswordField createPasswordField(int columns) {
        JPasswordField field = new JPasswordField(columns);
        styleInput(field);
        return field;
    }

    private static void styleInput(JTextField field) {
        field.setFont(FONT_BODY);
        field.setBackground(PAPER);
        field.setForeground(INK);
        field.setCaretColor(INK);
        field.setSelectionColor(ACCENT_TINT);
        field.setSelectedTextColor(INK);
        field.setPreferredSize(new Dimension(field.getPreferredSize().width, 38));
        field.setBorder(inputBorder(RULE));

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(inputBorder(ACCENT));
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(inputBorder(RULE));
            }
        });
    }

    private static javax.swing.border.Border inputBorder(Color colour) {
        return new CompoundBorder(
                new LineBorder(colour, 1),
                new EmptyBorder(8, 11, 8, 11)
        );
    }

    /** A paper-toned combo box used by the election selectors. */
    public static JComboBox<String> createComboBox() {
        JComboBox<String> combo = new JComboBox<>();
        styleComboBox(combo);
        return combo;
    }

    /** Applies the editorial styling to a combo box created elsewhere. */
    public static void styleComboBox(JComboBox<String> combo) {
        combo.setUI(new EditorialComboBoxUI());
        combo.setFont(FONT_BODY);
        combo.setBackground(PAPER);
        combo.setForeground(INK);
        combo.setBorder(new LineBorder(RULE, 1));
        combo.setPreferredSize(new Dimension(220, 36));
        combo.setMaximumRowCount(8);
        combo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        combo.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                label.setFont(FONT_BODY);
                label.setBorder(new EmptyBorder(0, 10, 0, 6));
                if (index < 0) {
                    label.setBackground(PAPER);
                    label.setForeground(INK);
                } else {
                    label.setBackground(isSelected ? ACCENT_TINT : PAPER);
                    label.setForeground(isSelected ? ACCENT_DARK : INK);
                }
                return label;
            }
        });

        combo.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                combo.setBorder(new LineBorder(ACCENT, 1));
            }

            @Override
            public void focusLost(FocusEvent e) {
                combo.setBorder(new LineBorder(RULE, 1));
            }
        });
    }

    /** Replaces the platform arrow with a square ink chevron. */
    private static final class EditorialComboBoxUI extends BasicComboBoxUI {
        @Override
        protected JButton createArrowButton() {
            JButton arrow = new JButton() {
                private static final long serialVersionUID = 1L;

                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(PAPER);
                    g2.fillRect(0, 0, getWidth(), getHeight());

                    g2.setColor(RULE);
                    g2.drawLine(0, 5, 0, getHeight() - 6);

                    g2.setColor(getModel().isRollover() ? ACCENT : INK);
                    g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int cx = getWidth() / 2;
                    int cy = getHeight() / 2;
                    g2.drawLine(cx - 4, cy - 2, cx, cy + 2);
                    g2.drawLine(cx, cy + 2, cx + 4, cy - 2);
                    g2.dispose();
                }
            };
            arrow.setOpaque(false);
            arrow.setContentAreaFilled(false);
            arrow.setBorderPainted(false);
            arrow.setFocusable(false);
            arrow.setPreferredSize(new Dimension(30, 34));
            arrow.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            arrow.getAccessibleContext().setAccessibleName("Show election list");
            return arrow;
        }

        @Override
        public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
            g.setColor(PAPER);
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }
    }

    /** A thin, square scrollbar: ink thumb on a paper track. */
    public static final class EditorialScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = RULE_DARK;
            this.trackColor = PAPER_ALT;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return zeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return zeroButton();
        }

        private JButton zeroButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            return button;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            g.setColor(PAPER_ALT);
            g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }
            g.setColor(isThumbRollover() ? INK : thumbColor);
            g.fillRect(thumbBounds.x + 2, thumbBounds.y + 2, thumbBounds.width - 4, thumbBounds.height - 4);
        }
    }

    public static JScrollPane wrapScrollPane(JComponent component) {
        JScrollPane scroll = new JScrollPane(component);
        scroll.setBorder(new LineBorder(RULE, 1));
        scroll.getViewport().setBackground(PAPER);
        scroll.getVerticalScrollBar().setUI(new EditorialScrollBarUI());
        scroll.getHorizontalScrollBar().setUI(new EditorialScrollBarUI());
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(10, 10));
        scroll.getHorizontalScrollBar().setPreferredSize(new Dimension(10, 10));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    /**
     * Wraps a whole screen in a scroll pane and a reveal host, so switching
     * sections plays the short rise-into-view animation.
     */
    public static JScrollPane wrapContentScroll(JComponent content) {
        RevealPanel host = new RevealPanel();
        host.add(content, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(host);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUI(new EditorialScrollBarUI());
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(10, 10));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(PAPER);

        host.addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && host.isShowing()) {
                host.play();
            }
        });
        return scroll;
    }

    /**
     * A panel that fades and lifts its child into place.
     * Used for whole screens and, with a delay, for staggered cards.
     */
    public static final class RevealPanel extends JPanel implements Scrollable {
        private static final long serialVersionUID = 1L;

        private float progress = 1f;
        private Timer animator;
        private int delayMs;

        public RevealPanel() {
            super(new BorderLayout());
            setOpaque(false);
        }

        /** Adds a start delay (milliseconds) for staggered reveals. */
        public void setDelay(int delayMs) {
            this.delayMs = Math.max(0, delayMs);
        }

        /** Plays the rise-into-view animation. */
        public void play() {
            if (animator != null) {
                animator.stop();
            }
            if (!isMotionEnabled()) {
                progress = 1f;
                repaint();
                return;
            }

            progress = 0f;
            animator = new Timer(16, event -> {
                progress = Math.min(1f, progress + 0.075f);
                repaint();
                if (progress >= 1f) {
                    ((Timer) event.getSource()).stop();
                }
            });
            animator.setInitialDelay(delayMs);
            animator.start();
        }

        @Override
        public void removeNotify() {
            if (animator != null) {
                animator.stop();
            }
            super.removeNotify();
        }

        @Override
        protected void paintChildren(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            float alpha = Math.max(0f, Math.min(1f, progress));
            g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
            g2.translate(0, Math.round((1f - alpha) * 14f));
            super.paintChildren(g2);
            g2.dispose();
        }

        // Scrollable keeps the page exactly as wide as the viewport, so the
        // editorial columns never overflow horizontally.
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(visibleRect.height - 32, 32);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /**
     * Styles a table as an editorial data grid: uppercase header, ink
     * underline, hairline rows and square status labels.
     */
    public static JTable styleTable(JTable table) {
        table.setFont(FONT_BODY);
        table.setRowHeight(36);
        table.setBackground(PAPER);
        table.setForeground(INK);
        table.setGridColor(RULE_FAINT);
        table.setSelectionBackground(ACCENT_TINT);
        table.setSelectionForeground(INK);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);
        table.setBorder(null);

        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_SMALL_BOLD);
        header.setBackground(PAPER_ALT);
        header.setForeground(INK);
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 40));

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                label.setText(value == null ? "" : value.toString().toUpperCase(Locale.ROOT));
                label.setFont(FONT_SMALL_BOLD);
                label.setForeground(INK);
                label.setBackground(PAPER_ALT);
                label.setHorizontalAlignment(SwingConstants.LEFT);
                label.setBorder(new CompoundBorder(
                        new MatteBorder(0, 0, 2, 0, INK),
                        new EmptyBorder(10, 10, 10, 10)
                ));
                return label;
            }
        };
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }

        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                String textValue = (value == null) ? "" : value.toString();
                String columnName = t.getColumnName(column);

                boolean eligibilityColumn = "Eligible?".equalsIgnoreCase(columnName);
                boolean statusColumn = "Ballot Status".equalsIgnoreCase(columnName);

                Color rowBackground = (row % 2 == 0) ? PAPER : PAPER_SUNKEN;
                if (!isSelected && (eligibilityColumn || statusColumn)) {
                    JPanel badgeCell = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 6));
                    badgeCell.setOpaque(true);
                    badgeCell.setBackground(rowBackground);

                    JLabel badge;
                    if ("YES".equalsIgnoreCase(textValue)) {
                        badge = createBadge("ELIGIBLE", SUCCESS_TINT, SUCCESS);
                    } else if ("NO".equalsIgnoreCase(textValue)) {
                        badge = createBadge("INELIGIBLE", DANGER_TINT, DANGER);
                    } else if (textValue.startsWith("VOTED")) {
                        badge = createBadge(textValue, SUCCESS_TINT, SUCCESS);
                    } else if ("NOT YET".equalsIgnoreCase(textValue)) {
                        badge = createBadge("PENDING", WARNING_TINT, WARNING);
                    } else {
                        badge = createBadge(textValue, PAPER_ALT, MUTED);
                    }
                    badge.setToolTipText(textValue);
                    badgeCell.add(badge);
                    return badgeCell;
                }

                Component component = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                JLabel label = (JLabel) component;

                boolean centered = column == 0 || eligibilityColumn || statusColumn || column == t.getColumnCount() - 1;
                label.setHorizontalAlignment(centered ? SwingConstants.CENTER : SwingConstants.LEFT);
                label.setBorder(new EmptyBorder(0, 10, 0, 10));
                label.setBackground(isSelected ? ACCENT_TINT : rowBackground);
                label.setForeground(INK);
                // Long departments and slogans are truncated by the column; the
                // tooltip keeps the full value available without widening the grid.
                label.setToolTipText(textValue.isEmpty() ? null : textValue);
                return label;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }

        return table;
    }

    // =========================================================================
    // 9. ANIMATED VOTE BAR — FLAT RULE, RED FILL
    // =========================================================================

    /**
     * A candidate tally: serif name, exact count and percentage, and a flat
     * red bar that grows into place.
     */
    public static final class HorizontalVoteBar extends JPanel {
        private static final long serialVersionUID = 1L;

        private final double percentage;
        private double animatedProgress;

        public HorizontalVoteBar(String candidateName, int votes, int totalVotes) {
            this.percentage = (totalVotes > 0) ? ((double) votes / totalVotes) * 100.0 : 0.0;

            setLayout(new BorderLayout(8, 8));
            setOpaque(false);
            setPreferredSize(new Dimension(420, 58));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));

            JPanel labels = new JPanel(new BorderLayout());
            labels.setOpaque(false);

            JLabel nameLabel = new JLabel(candidateName == null ? "" : candidateName);
            nameLabel.setFont(new Font(DISPLAY_FONT_FAMILY, Font.BOLD, 15));
            nameLabel.setForeground(INK);

            JLabel countLabel = createKicker(votes + (votes == 1 ? " vote" : " votes")
                    + "   " + String.format(Locale.ROOT, "%.1f", percentage) + "%");
            countLabel.setForeground(ACCENT_DARK);

            labels.add(nameLabel, BorderLayout.WEST);
            labels.add(countLabel, BorderLayout.EAST);
            add(labels, BorderLayout.NORTH);

            JPanel bar = new JPanel() {
                private static final long serialVersionUID = 1L;

                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(PAPER_ALT);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setColor(RULE);
                    g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

                    int fillWidth = (int) (getWidth() * (animatedProgress / 100.0));
                    if (fillWidth > 0) {
                        g2.setColor(ACCENT);
                        g2.fillRect(0, 0, fillWidth, getHeight());
                    }
                    g2.dispose();
                }
            };
            bar.setOpaque(false);
            bar.setPreferredSize(new Dimension(400, 10));
            add(bar, BorderLayout.CENTER);

            startAnimation(bar);
        }

        private void startAnimation(JPanel bar) {
            if (!isMotionEnabled()) {
                animatedProgress = percentage;
                bar.repaint();
                return;
            }
            final int steps = 20;
            final double stepDelta = percentage / steps;
            final int[] step = {0};
            Timer timer = new Timer(16, event -> {
                step[0]++;
                animatedProgress = Math.min(percentage, stepDelta * step[0]);
                bar.repaint();
                if (step[0] >= steps) {
                    ((Timer) event.getSource()).stop();
                }
            });
            timer.setInitialDelay(80);
            timer.start();
        }
    }

    // =========================================================================
    // 10. SMALL COLOUR HELPERS
    // =========================================================================

    /** True when white text is the readable choice on this background. */
    public static boolean isDark(Color colour) {
        if (colour == null) {
            return false;
        }
        double luminance = (0.2126 * channel(colour.getRed())
                + 0.7152 * channel(colour.getGreen())
                + 0.0722 * channel(colour.getBlue()));
        return luminance < 0.45;
    }

    /** Blends two colours; {@code ratio} is how much of {@code a} to keep. */
    public static Color mix(Color a, Color b, float ratio) {
        float keep = Math.max(0f, Math.min(1f, ratio));
        return new Color(
                Math.round(a.getRed() * keep + b.getRed() * (1f - keep)),
                Math.round(a.getGreen() * keep + b.getGreen() * (1f - keep)),
                Math.round(a.getBlue() * keep + b.getBlue() * (1f - keep))
        );
    }

    private static double channel(int value) {
        double scaled = value / 255.0;
        return scaled <= 0.03928 ? scaled / 12.92 : Math.pow((scaled + 0.055) / 1.055, 2.4);
    }

    private static void drawCentredText(Graphics2D g2, String text, int width, int height) {
        if (text == null || text.isEmpty()) {
            return;
        }
        FontMetrics metrics = g2.getFontMetrics();
        String fitted = fitText(text, metrics, Math.max(0, width - 20));
        int x = (width - metrics.stringWidth(fitted)) / 2;
        int y = (height - metrics.getHeight()) / 2 + metrics.getAscent();
        g2.drawString(fitted, x, y);
    }

    private static String fitText(String text, FontMetrics metrics, int availableWidth) {
        if (metrics.stringWidth(text) <= availableWidth) {
            return text;
        }
        String ellipsis = "\u2026";
        int end = text.length();
        while (end > 0 && metrics.stringWidth(text.substring(0, end) + ellipsis) > availableWidth) {
            end--;
        }
        return text.substring(0, Math.max(end, 0)) + ellipsis;
    }
}
