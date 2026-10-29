package com.votum.ui;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * A validating date-time entry field built only from the JDK.
 *
 * The control parses the fixed pattern {@value #PATTERN} and paints its own validity state, so a
 * typo is caught while the administrator types instead of surfacing as an exception on submit.
 * Both the parsing and the rule checks live in static, display-free methods ({@link #parse(String)},
 * {@link #validateWindow(String, String)}, {@link #isEntirelyInThePast(LocalDateTime, LocalDateTime, LocalDateTime)})
 * so they can be verified by the automated suite without a graphical environment.
 */
public final class DateTimeInput extends JTextField {

    private static final long serialVersionUID = 1L;

    /** The only accepted input pattern; mirrors the format shown in the field tooltip. */
    public static final String PATTERN = "yyyy-MM-dd HH:mm";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(PATTERN);

    private boolean validState = true;

    public DateTimeInput(String initialText) {
        super(initialText == null ? "" : initialText, 18);
        setFont(UITheme.FONT_MONO);
        setBackground(UITheme.PAPER);
        setForeground(UITheme.INK);
        setCaretColor(UITheme.INK);
        setSelectionColor(UITheme.ACCENT_TINT);
        setSelectedTextColor(UITheme.INK);
        setToolTipText("Format: " + PATTERN + "  (example: 2026-03-01 09:00)");
        setPreferredSize(new java.awt.Dimension(getPreferredSize().width, 38));
        addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                revalidateStyle();
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                revalidateStyle();
            }
        });
        getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                revalidateStyle();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                revalidateStyle();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                revalidateStyle();
            }
        });
        revalidateStyle();
    }

    /**
     * Parses the entered value.
     *
     * @return the parsed value, or {@code null} when the field is empty or malformed
     */
    public LocalDateTime parseDateTime() {
        return parse(getText());
    }

    /** Sets the field from a value, leaving it empty for {@code null}. */
    public void setDateTime(LocalDateTime value) {
        setText(value == null ? "" : value.format(FORMATTER));
    }

    /** True when something has been typed. */
    public boolean hasText() {
        return getText() != null && !getText().trim().isEmpty();
    }

    /** True when the current content parses to a date-time. */
    public boolean isValidDateTime() {
        return parseDateTime() != null;
    }

    /** Current validity, for callers that want to react without re-parsing. */
    public boolean isValidState() {
        return validState;
    }

    private void revalidateStyle() {
        String text = getText() == null ? "" : getText().trim();
        validState = text.isEmpty() || parse(text) != null;
        applyBorder(validState ? (isFocusOwner() ? UITheme.ACCENT : UITheme.RULE) : UITheme.DANGER);
    }

    private void applyBorder(java.awt.Color borderColor) {
        setBorder(new CompoundBorder(
                new LineBorder(borderColor, 1),
                new EmptyBorder(8, 11, 8, 11)
        ));
    }

    // ==========================================
    // DISPLAY-FREE VALIDATION (shared with the automated suite)
    // ==========================================

    /**
     * Parses text in the {@value #PATTERN} format.
     *
     * @return the parsed value, or {@code null} for blank or malformed input
     */
    public static LocalDateTime parse(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(text.trim(), FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Validates a voting window exactly as {@code ElectionService.createElection} will.
     *
     * @return an administrator-friendly error message, or {@code null} when the window is valid
     */
    public static String validateWindow(String startText, String endText) {
        boolean startBlank = startText == null || startText.trim().isEmpty();
        boolean endBlank = endText == null || endText.trim().isEmpty();
        if (startBlank || endBlank) {
            return "Both Start and End date-time are required.";
        }

        LocalDateTime start = parse(startText);
        LocalDateTime end = parse(endText);
        if (start == null || end == null) {
            return "Use the format " + PATTERN + " (for example 2026-03-01 09:00).";
        }
        if (!end.isAfter(start)) {
            return "End time must be strictly after Start time.";
        }
        return null;
    }

    /**
     * True when the whole window has already elapsed; the service still allows such an election
     * (useful for recording history), so the UI warns instead of blocking.
     */
    public static boolean isEntirelyInThePast(LocalDateTime start, LocalDateTime end, LocalDateTime now) {
        if (start == null || end == null || now == null) {
            return false;
        }
        return end.isBefore(now);
    }

    /**
     * True when the window has not opened yet, i.e. it is scheduled for the future.
     */
    public static boolean isEntirelyInTheFuture(LocalDateTime start, LocalDateTime end, LocalDateTime now) {
        if (start == null || end == null || now == null) {
            return false;
        }
        return start.isAfter(now);
    }
}
