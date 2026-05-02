package pl.emcmanagement.ui.dialog;

import pl.emcmanagement.dao.ReservationCalendarDao;
import pl.emcmanagement.model.EquipmentReservationEntry;
import pl.emcmanagement.model.LabReservationEntry;
import pl.emcmanagement.ui.style.EmcUiTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ReservationCalendarDialog extends JDialog {
    private static final String[] LABS = {"LabA", "LabB", "LabC", "LabD", "LabE"};
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final Integer projectId;
    private final String scopeLabel;
    private final ReservationCalendarDao reservationCalendarDao = new ReservationCalendarDao();

    private final JTextField startDateField = new JTextField(LocalDate.now().toString(), 12);
    private final JComboBox<Integer> daysCombo = new JComboBox<>(new Integer[]{30, 45, 60});
    private final JTextField ewrFilterField = new JTextField();
    private final JTextField equipmentFilterField = new JTextField();
    private final JButton refreshButton = new JButton("Odswiez");

    private final DefaultTableModel labTableModel = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel equipmentCalendarModel = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable labTable = new JTable(labTableModel);
    private final JTable equipmentTable = new JTable(equipmentCalendarModel);

    public ReservationCalendarDialog(Window owner, Integer projectId, String scopeLabel) {
        super(owner, "Kalendarz rezerwacji", ModalityType.APPLICATION_MODAL);
        this.projectId = projectId;
        this.scopeLabel = scopeLabel == null ? "wszystkie dostepne projekty" : scopeLabel;

        setSize(1440, 860);
        setMinimumSize(new Dimension(1180, 720));
        setLocationRelativeTo(owner);

        JPanel root = EmcUiTheme.createPatternPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(14, 14, 14, 14));
        setContentPane(root);

        root.add(buildControlPanel(), BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(2, 1, 0, 12));
        center.setOpaque(false);
        center.add(buildLabPanel());
        center.add(buildEquipmentPanel());
        root.add(center, BorderLayout.CENTER);

        configureTables();
        configureActions();
        loadCalendars();
    }

    private Component buildControlPanel() {
        JPanel panel = EmcUiTheme.createCardPanel(new BorderLayout(0, 10));

        JLabel title = new JLabel("Global reservation calendar for labs and equipment");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        JLabel subtitle = new JLabel("Zakres: " + scopeLabel + " | wspolny widok dla wszystkich EWR");
        subtitle.setForeground(EmcUiTheme.TEXT_MUTED);

        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        labels.add(title);
        labels.add(Box.createVerticalStrut(6));
        labels.add(subtitle);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        controls.setOpaque(false);
        daysCombo.setSelectedItem(30);
        EmcUiTheme.styleTextField(startDateField);
        EmcUiTheme.styleComboBox(daysCombo);
        EmcUiTheme.styleTextField(ewrFilterField);
        EmcUiTheme.styleTextField(equipmentFilterField);
        EmcUiTheme.stylePrimaryButton(refreshButton);

        ewrFilterField.setPreferredSize(new Dimension(160, 36));
        equipmentFilterField.setPreferredSize(new Dimension(220, 36));

        controls.add(new JLabel("Start:"));
        controls.add(startDateField);
        controls.add(new JLabel("Dni:"));
        controls.add(daysCombo);
        controls.add(new JLabel("Filtr EWR:"));
        controls.add(ewrFilterField);
        controls.add(new JLabel("Filtr sprzetu:"));
        controls.add(equipmentFilterField);
        controls.add(refreshButton);

        panel.add(labels, BorderLayout.NORTH);
        panel.add(controls, BorderLayout.SOUTH);
        return panel;
    }

    private Component buildLabPanel() {
        JPanel panel = EmcUiTheme.createCardPanel(new BorderLayout(0, 10));
        JLabel title = new JLabel("Laboratory occupancy");
        title.setFont(EmcUiTheme.SECTION_FONT);
        panel.add(title, BorderLayout.NORTH);

        labTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JScrollPane scrollPane = new JScrollPane(labTable);
        EmcUiTheme.styleScrollPane(scrollPane);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private Component buildEquipmentPanel() {
        JPanel panel = EmcUiTheme.createCardPanel(new BorderLayout(0, 10));
        JLabel title = new JLabel("Equipment reservations across all EWRs");
        title.setFont(EmcUiTheme.SECTION_FONT);
        panel.add(title, BorderLayout.NORTH);

        equipmentTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JScrollPane scrollPane = new JScrollPane(equipmentTable);
        EmcUiTheme.styleScrollPane(scrollPane);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void configureTables() {
        EmcUiTheme.styleTable(labTable);
        EmcUiTheme.styleTable(equipmentTable);
        labTable.setFillsViewportHeight(true);
        equipmentTable.setFillsViewportHeight(true);

        ReservationCellRenderer renderer = new ReservationCellRenderer();
        labTable.setDefaultRenderer(Object.class, renderer);
        equipmentTable.setDefaultRenderer(Object.class, renderer);
    }

    private void configureActions() {
        DocumentListener liveReloadListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                loadCalendars();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                loadCalendars();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                loadCalendars();
            }
        };

        startDateField.addActionListener(e -> loadCalendars());
        ewrFilterField.getDocument().addDocumentListener(liveReloadListener);
        equipmentFilterField.getDocument().addDocumentListener(liveReloadListener);
        daysCombo.addActionListener(e -> loadCalendars());
        refreshButton.addActionListener(e -> loadCalendars());
    }

    private void loadCalendars() {
        LocalDate startDate = parseStartDate();
        int dayCount = (Integer) daysCombo.getSelectedItem();
        LocalDate endDate = startDate.plusDays(dayCount - 1L);
        List<LocalDate> dates = IntStream.range(0, dayCount)
                .mapToObj(startDate::plusDays)
                .toList();

        String ewrFilter = normalizeFilter(ewrFilterField.getText());
        String equipmentFilter = normalizeFilter(equipmentFilterField.getText());

        List<LabReservationEntry> labEntries = reservationCalendarDao.findLabReservations(startDate, endDate, projectId).stream()
                .filter(entry -> matchesEwr(entry.getEwrNumber(), ewrFilter))
                .toList();

        List<EquipmentReservationEntry> equipmentEntries = reservationCalendarDao.findEquipmentReservations(startDate, endDate, projectId).stream()
                .filter(entry -> matchesEwr(entry.getEwrNumber(), ewrFilter))
                .filter(entry -> matchesEquipment(entry, equipmentFilter))
                .toList();

        rebuildLabTable(dates, labEntries);
        rebuildEquipmentCalendar(dates, equipmentEntries);
    }

    private void rebuildLabTable(List<LocalDate> dates, List<LabReservationEntry> entries) {
        Object[] columns = new Object[dates.size() + 1];
        columns[0] = "Lab";
        for (int index = 0; index < dates.size(); index++) {
            columns[index + 1] = DATE_FORMAT.format(dates.get(index));
        }
        labTableModel.setDataVector(new Object[0][0], columns);

        Map<String, Map<LocalDate, List<LabReservationEntry>>> grouped = new LinkedHashMap<>();
        for (String lab : LABS) {
            grouped.put(lab, new LinkedHashMap<>());
        }
        for (LabReservationEntry entry : entries) {
            grouped.computeIfAbsent(entry.getRoomCode(), ignored -> new LinkedHashMap<>())
                    .computeIfAbsent(entry.getReservationDate(), ignored -> new ArrayList<>())
                    .add(entry);
        }

        int maxLines = 1;
        for (String lab : LABS) {
            Object[] row = new Object[dates.size() + 1];
            row[0] = lab;
            Map<LocalDate, List<LabReservationEntry>> byDate = grouped.getOrDefault(lab, Map.of());
            for (int index = 0; index < dates.size(); index++) {
                List<LabReservationEntry> dayEntries = byDate.getOrDefault(dates.get(index), List.of());
                ReservationCell cell = ReservationCell.fromLabEntries(dayEntries);
                row[index + 1] = cell;
                maxLines = Math.max(maxLines, cell.lineCount());
            }
            labTableModel.addRow(row);
        }

        configureCalendarColumns(labTable, 92, maxLines);
    }

    private void rebuildEquipmentCalendar(List<LocalDate> dates, List<EquipmentReservationEntry> entries) {
        Object[] columns = new Object[dates.size() + 1];
        columns[0] = "Sprzet";
        for (int index = 0; index < dates.size(); index++) {
            columns[index + 1] = DATE_FORMAT.format(dates.get(index));
        }
        equipmentCalendarModel.setDataVector(new Object[0][0], columns);

        Map<String, EquipmentCalendarRow> rows = new LinkedHashMap<>();
        for (EquipmentReservationEntry entry : entries) {
            String key = entry.getEquipmentCode() + " | " + entry.getEquipmentName();
            EquipmentCalendarRow row = rows.computeIfAbsent(key, ignored ->
                    new EquipmentCalendarRow(key, entry.getEquipmentCode(), entry.getEquipmentName()));
            LocalDate pointer = entry.getReservedFrom();
            while (pointer != null && !pointer.isAfter(entry.getReservedTo())) {
                if (!pointer.isBefore(dates.get(0)) && !pointer.isAfter(dates.get(dates.size() - 1))) {
                    row.byDate.computeIfAbsent(pointer, ignored -> new ArrayList<>()).add(entry);
                }
                pointer = pointer.plusDays(1);
            }
        }

        int maxLines = 1;
        rows.values().stream()
                .sorted(Comparator.comparing(EquipmentCalendarRow::equipmentCode, String.CASE_INSENSITIVE_ORDER))
                .forEach(rowData -> {
                    Object[] row = new Object[dates.size() + 1];
                    row[0] = rowData.label();
                    for (int index = 0; index < dates.size(); index++) {
                        List<EquipmentReservationEntry> dayEntries = rowData.byDate().getOrDefault(dates.get(index), List.of());
                        ReservationCell cell = ReservationCell.fromEquipmentEntries(dayEntries);
                        row[index + 1] = cell;
                        if (cell.lineCount() > rowData.maxLines()) {
                            rowData.setMaxLines(cell.lineCount());
                        }
                    }
                    equipmentCalendarModel.addRow(row);
                });

        for (EquipmentCalendarRow row : rows.values()) {
            maxLines = Math.max(maxLines, row.maxLines);
        }

        configureCalendarColumns(equipmentTable, 240, maxLines);
    }

    private void configureCalendarColumns(JTable table, int firstColumnWidth, int maxLines) {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        if (table.getColumnModel().getColumnCount() == 0) {
            return;
        }
        table.getColumnModel().getColumn(0).setPreferredWidth(firstColumnWidth);
        table.getColumnModel().getColumn(0).setMinWidth(firstColumnWidth);
        for (int index = 1; index < table.getColumnModel().getColumnCount(); index++) {
            table.getColumnModel().getColumn(index).setPreferredWidth(110);
            table.getColumnModel().getColumn(index).setMinWidth(110);
        }
        table.setRowHeight(Math.max(30, 22 + (maxLines - 1) * 18));
    }

    private LocalDate parseStartDate() {
        String raw = startDateField.getText().trim();
        try {
            return raw.isEmpty() ? LocalDate.now() : LocalDate.parse(raw);
        } catch (DateTimeParseException exception) {
            JOptionPane.showMessageDialog(
                    this,
                    "Niepoprawna data startu. Uzyj formatu YYYY-MM-DD.",
                    "Walidacja",
                    JOptionPane.WARNING_MESSAGE
            );
            LocalDate fallback = LocalDate.now();
            startDateField.setText(fallback.toString());
            return fallback;
        }
    }

    private boolean matchesEwr(String ewrNumber, String filter) {
        return filter == null || (ewrNumber != null && ewrNumber.toUpperCase(Locale.ROOT).contains(filter));
    }

    private boolean matchesEquipment(EquipmentReservationEntry entry, String filter) {
        if (filter == null) {
            return true;
        }
        String code = entry.getEquipmentCode() == null ? "" : entry.getEquipmentCode().toUpperCase(Locale.ROOT);
        String name = entry.getEquipmentName() == null ? "" : entry.getEquipmentName().toUpperCase(Locale.ROOT);
        return code.contains(filter) || name.contains(filter);
    }

    private String normalizeFilter(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private static final class EquipmentCalendarRow {
        private final String label;
        private final String equipmentCode;
        private final String equipmentName;
        private final Map<LocalDate, List<EquipmentReservationEntry>> byDate = new LinkedHashMap<>();
        private int maxLines = 1;

        private EquipmentCalendarRow(String label, String equipmentCode, String equipmentName) {
            this.label = label;
            this.equipmentCode = equipmentCode;
            this.equipmentName = equipmentName;
        }

        public String label() {
            return label;
        }

        public String equipmentCode() {
            return equipmentCode;
        }

        public String equipmentName() {
            return equipmentName;
        }

        public Map<LocalDate, List<EquipmentReservationEntry>> byDate() {
            return byDate;
        }

        public int maxLines() {
            return maxLines;
        }

        public void setMaxLines(int maxLines) {
            this.maxLines = maxLines;
        }
    }

    private static final class ReservationCell {
        private final List<String> lines;
        private final List<String> tooltipLines;

        private ReservationCell(List<String> lines, List<String> tooltipLines) {
            this.lines = lines;
            this.tooltipLines = tooltipLines;
        }

        static ReservationCell fromLabEntries(List<LabReservationEntry> entries) {
            if (entries == null || entries.isEmpty()) {
                return new ReservationCell(List.of("wolne"), List.of("wolne"));
            }
            Set<String> ewrLines = entries.stream()
                    .map(LabReservationEntry::getEwrNumber)
                    .filter(value -> value != null && !value.isBlank())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            List<String> tooltip = entries.stream()
                    .map(LabReservationEntry::getDisplayLabel)
                    .distinct()
                    .toList();
            return new ReservationCell(new ArrayList<>(ewrLines), tooltip);
        }

        static ReservationCell fromEquipmentEntries(List<EquipmentReservationEntry> entries) {
            if (entries == null || entries.isEmpty()) {
                return new ReservationCell(List.of("wolne"), List.of("wolne"));
            }
            Set<String> ewrLines = entries.stream()
                    .map(EquipmentReservationEntry::getEwrNumber)
                    .filter(value -> value != null && !value.isBlank())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            List<String> tooltip = entries.stream()
                    .map(entry -> entry.getEwrNumber() + " | " + entry.getLegCode() + " | " + entry.getStepLabel())
                    .distinct()
                    .toList();
            return new ReservationCell(new ArrayList<>(ewrLines), tooltip);
        }

        boolean isFree() {
            return lines.size() == 1 && "wolne".equalsIgnoreCase(lines.get(0));
        }

        int reservationCount() {
            return isFree() ? 0 : lines.size();
        }

        int lineCount() {
            return Math.max(1, lines.size());
        }

        String textHtml() {
            return "<html><div style='text-align:center;'>" + String.join("<br>", lines) + "</div></html>";
        }

        String tooltipHtml() {
            return "<html><div style='width:260px;'>" + String.join("<br>", tooltipLines) + "</div></html>";
        }
    }

    private static final class ReservationCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(column == 0 ? SwingConstants.LEFT : SwingConstants.CENTER);
            setVerticalAlignment(SwingConstants.CENTER);
            setFont(new Font("Segoe UI", column == 0 ? Font.BOLD : Font.PLAIN, 12));

            if (column == 0) {
                setText(value == null ? "---" : value.toString());
                setToolTipText(null);
                setBackground(isSelected ? new Color(216, 232, 244) : Color.WHITE);
                setForeground(EmcUiTheme.TEXT_PRIMARY);
                return this;
            }

            ReservationCell cell = value instanceof ReservationCell reservationCell
                    ? reservationCell
                    : new ReservationCell(List.of("wolne"), List.of("wolne"));
            setText(cell.textHtml());
            setToolTipText(cell.tooltipHtml());
            setForeground(EmcUiTheme.TEXT_PRIMARY);

            if (cell.isFree()) {
                setBackground(isSelected ? new Color(228, 239, 249) : Color.WHITE);
                setForeground(EmcUiTheme.TEXT_MUTED);
            } else if (cell.reservationCount() == 1) {
                setBackground(new Color(225, 243, 229));
            } else if (cell.reservationCount() == 2) {
                setBackground(new Color(252, 240, 211));
            } else {
                setBackground(new Color(247, 224, 221));
            }
            return this;
        }
    }
}
