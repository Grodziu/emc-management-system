package pl.emcmanagement.ui.dialog;

import pl.emcmanagement.model.ClimateMeasurement;
import pl.emcmanagement.ui.style.EmcUiTheme;
import pl.emcmanagement.util.ClimateChartFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.util.List;

public class ClimateChartDialog extends JDialog {
    public ClimateChartDialog(Window owner,
                              String sourceLabel,
                              LocalDate startDate,
                              LocalDate endDate,
                              List<ClimateMeasurement> measurements) {
        super(owner, "Wykres warunkow klimatycznych", ModalityType.APPLICATION_MODAL);

        setSize(1080, 760);
        setMinimumSize(new Dimension(920, 620));
        setLocationRelativeTo(owner);

        JPanel root = EmcUiTheme.createPatternPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(14, 14, 14, 14));
        setContentPane(root);

        JPanel header = EmcUiTheme.createCardPanel(new BorderLayout(0, 6));
        JLabel title = new JLabel("Warunki klimatyczne | " + valueOrDash(sourceLabel));
        title.setFont(EmcUiTheme.TITLE_FONT);
        JLabel subtitle = new JLabel("Zakres: " + valueOrDash(startDate) + " - " + valueOrDash(endDate) + " | probki: " + measurements.size());
        subtitle.setForeground(EmcUiTheme.TEXT_MUTED);
        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);
        root.add(header, BorderLayout.NORTH);

        BufferedImage chartImage = ClimateChartFactory.createChartImage(
                valueOrDash(sourceLabel),
                measurements,
                980,
                560
        );
        JLabel imageLabel = new JLabel(new ImageIcon(chartImage));
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        JScrollPane scrollPane = new JScrollPane(imageLabel);
        EmcUiTheme.styleScrollPane(scrollPane);
        root.add(scrollPane, BorderLayout.CENTER);
    }

    private String valueOrDash(Object value) {
        return value == null ? "---" : value.toString();
    }
}
