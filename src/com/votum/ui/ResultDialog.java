package com.votum.ui;

import com.votum.model.Candidate;
import com.votum.model.ElectionResult;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Locale;
import java.util.Map;

/**
 * The certified results page.
 *
 * <p>Structured like the results spread of a newspaper: a kicker, the election
 * name, a heavy rule, the winner's name as the headline, then the numbers and
 * the vote distribution. The dialog keeps its keyboard contract: Enter closes,
 * Escape closes, and the close button is the default button.</p>
 */
public final class ResultDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    public ResultDialog(Frame parent, ElectionResult result) {
        super(parent, "Official Election Results — " + result.getElectionName(), true);
        initUI(result);
    }

    private void initUI(ElectionResult result) {
        setSize(760, 720);
        setMinimumSize(new Dimension(720, 620));
        setAlwaysOnTop(true);
        setLocationRelativeTo(getParent());
        getContentPane().setBackground(UITheme.PAPER);
        setLayout(new BorderLayout(0, 0));

        JPanel page = UITheme.createPage(30, 34, 26, 34);

        // Masthead of the result page
        JLabel kicker = UITheme.createKicker("CERTIFIED RESULT  \u00B7  " + result.getElectionId());
        kicker.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = UITheme.createWrappedLabel(
                result.getElectionName(), UITheme.FONT_DISPLAY, UITheme.INK, 620);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel byline = new JLabel("Official tally prepared by the campus election authority");
        byline.setFont(UITheme.FONT_SMALL);
        byline.setForeground(UITheme.MUTED);
        byline.setAlignmentX(Component.LEFT_ALIGNMENT);

        UITheme.addPageRow(page, kicker, 8);
        UITheme.addPageRow(page, title, 6);
        UITheme.addPageRow(page, byline, 16);
        // The winner block carries its own top rule, so no separate divider is needed.
        UITheme.addPageRow(page, buildWinnerBlock(result), 22);

        // Metrics band
        JPanel metrics = new JPanel(new GridLayout(1, 3, 12, 0));
        metrics.setOpaque(false);
        metrics.setMaximumSize(new Dimension(Integer.MAX_VALUE, 116));
        metrics.setAlignmentX(Component.LEFT_ALIGNMENT);
        metrics.add(new UITheme.StatCard("Total Votes", String.valueOf(result.getTotalVotes()),
                "Official ballots", UITheme.ACCENT));
        metrics.add(new UITheme.StatCard("Eligible Voters", String.valueOf(result.getEligibleVotersCount()),
                "Certified roll", UITheme.INK));
        metrics.add(new UITheme.StatCard("Turnout Rate", result.getTurnoutPercentage() + "%",
                "Student participation", UITheme.SUCCESS));
        UITheme.addPageRow(page, metrics, 20);

        JLabel distribution = UITheme.createKicker("VOTE DISTRIBUTION");
        distribution.setForeground(UITheme.MUTED);
        distribution.setAlignmentX(Component.LEFT_ALIGNMENT);
        UITheme.addPageRow(page, distribution, 10);
        UITheme.addPageRow(page, buildDistribution(result), 0);

        // Footer controls
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 0));
        bottom.setBackground(UITheme.PAPER);
        bottom.setBorder(new CompoundBorder(
                new MatteBorder(2, 0, 0, 0, UITheme.INK),
                new EmptyBorder(14, 34, 18, 34)
        ));

        JButton closeButton = UITheme.createPrimaryButton("Close Results");
        closeButton.setFont(UITheme.FONT_BUTTON);
        closeButton.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(closeButton);
        bottom.add(closeButton);

        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        UITheme.RevealPanel reveal = new UITheme.RevealPanel();
        reveal.add(page, BorderLayout.CENTER);
        reveal.setBackground(UITheme.PAPER);
        add(reveal, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                reveal.play();
            }
        });
    }

    /** The winner, tie or zero-vote statement, set as the headline of the page. */
    private JPanel buildWinnerBlock(ElectionResult result) {
        JPanel block = new JPanel(new BorderLayout());
        block.setOpaque(false);
        block.setAlignmentX(Component.LEFT_ALIGNMENT);
        block.setBorder(new CompoundBorder(
                new MatteBorder(2, 0, 2, 0, UITheme.INK),
                new EmptyBorder(16, 0, 16, 0)
        ));

        JLabel outcome = new JLabel();
        outcome.setFont(UITheme.FONT_DISPLAY);
        outcome.setHorizontalAlignment(SwingConstants.LEFT);

        if (result.isTie()) {
            outcome.setText(result.getWinnerSummary().toUpperCase(Locale.ROOT));
            outcome.setForeground(UITheme.WARNING);
        } else if (result.getTotalVotes() > 0 && !result.getWinningCandidates().isEmpty()) {
            outcome.setText(result.getWinnerSummary());
            outcome.setForeground(UITheme.ACCENT_DARK);
        } else {
            outcome.setText("No ballots recorded yet for this election.");
            outcome.setForeground(UITheme.MUTED);
        }

        block.add(outcome, BorderLayout.CENTER);
        return block;
    }

    /** Candidate bars in a plain scroll pane; the bars themselves animate. */
    private JScrollPane buildDistribution(ElectionResult result) {
        JPanel bars = new JPanel();
        bars.setLayout(new BoxLayout(bars, BoxLayout.Y_AXIS));
        bars.setBackground(UITheme.PAPER);
        bars.setBorder(new EmptyBorder(2, 0, 2, 0));

        Map<String, Integer> counts = result.getCandidateVoteCounts();
        if (counts.isEmpty()) {
            JLabel empty = new JLabel("No candidate records found for this election.");
            empty.setFont(UITheme.FONT_SERIF_BODY);
            empty.setForeground(UITheme.MUTED);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            bars.add(empty);
        } else {
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                String candidateId = entry.getKey();
                int votes = entry.getValue();
                Candidate candidate = result.getCandidate(candidateId);
                String label = (candidate != null)
                        ? candidate.getName() + " (" + candidate.getDepartment() + ")"
                        : candidateId;

                UITheme.HorizontalVoteBar bar = new UITheme.HorizontalVoteBar(label, votes, result.getTotalVotes());
                bar.setAlignmentX(Component.LEFT_ALIGNMENT);
                bars.add(bar);
                bars.add(Box.createVerticalStrut(10));
            }
        }

        JScrollPane scroll = new JScrollPane(bars);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UITheme.PAPER);
        scroll.getVerticalScrollBar().setUI(new UITheme.EditorialScrollBarUI());
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(10, 10));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        scroll.setPreferredSize(new Dimension(600, 210));
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 210));
        return scroll;
    }
}
