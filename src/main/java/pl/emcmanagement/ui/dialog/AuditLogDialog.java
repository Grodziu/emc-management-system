package pl.emcmanagement.ui.dialog;

import pl.emcmanagement.model.AuditLogEntry;
import pl.emcmanagement.service.AuditLogService;
import pl.emcmanagement.ui.style.EmcUiTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class AuditLogDialog extends JDialog {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Integer projectId;
    private final Integer legId;
    private final AuditLogService auditLogService = new AuditLogService();

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Data", "Uzytkownik", "Rola", "Akcja", "Obiekt", "Kontekst", "Podsumowanie", "Szczegoly"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
    private final JTextField filterField = new JTextField();
    private final JLabel scopeLabel = new JLabel();

    private final List<AuditLogEntry> currentEntries = new ArrayList<>();

    public AuditLogDialog(Window owner, Integer projectId, Integer legId, String contextLabel) {
        super(owner, "Audit log", ModalityType.APPLICATION_MODAL);
        this.projectId = projectId;
        this.legId = legId;

        setSize(1220, 700);
        setMinimumSize(new Dimension(960, 620));
        setLocationRelativeTo(owner);

        JPanel root = EmcUiTheme.createPatternPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(14, 14, 14, 14));
        setContentPane(root);

        root.add(buildTopPanel(contextLabel), BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(table);
        EmcUiTheme.styleScrollPane(scrollPane);
        root.add(scrollPane, BorderLayout.CENTER);

        root.add(buildBottomPanel(), BorderLayout.SOUTH);

        configureTable();
        configureActions();
        loadEntries();
    }

    private Component buildTopPanel(String contextLabel) {
        JPanel panel = EmcUiTheme.createCardPanel(new BorderLayout(0, 10));

        JLabel title = new JLabel("Audit log zmian");
        title.setFont(EmcUiTheme.TITLE_FONT);

        String scopeText = contextLabel == null || contextLabel.isBlank()
                ? "Zakres: wszystkie widoczne wpisy"
                : "Zakres: " + contextLabel;
        scopeLabel.setText(scopeText);
        scopeLabel.setForeground(EmcUiTheme.TEXT_MUTED);

        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.add(title);
        titlePanel.add(Box.createVerticalStrut(4));
        titlePanel.add(scopeLabel);

        JPanel filterPanel = new JPanel(new BorderLayout(8, 0));
        filterPanel.setOpaque(false);
        filterPanel.add(new JLabel("Szukaj:"), BorderLayout.WEST);
        filterPanel.add(filterField, BorderLayout.CENTER);

        panel.add(titlePanel, BorderLayout.NORTH);
        panel.add(filterPanel, BorderLayout.SOUTH);
        return panel;
    }

    private Component buildBottomPanel() {
        JPanel panel = EmcUiTheme.createCardPanel(new BorderLayout());
        JTextArea infoArea = new JTextArea("Audit log pokazuje ostatnie operacje wykonane w projektach: zmiany TT, DUT, LEGov, sprzetu, wynikow, importu klimatu i generowania raportow.");
        EmcUiTheme.styleInfoArea(infoArea);
        panel.add(infoArea, BorderLayout.CENTER);
        return panel;
    }

    private void configureTable() {
        EmcUiTheme.styleTable(table);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSorter(sorter);
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(1).setPreferredWidth(170);
        table.getColumnModel().getColumn(2).setPreferredWidth(70);
        table.getColumnModel().getColumn(3).setPreferredWidth(150);
        table.getColumnModel().getColumn(4).setPreferredWidth(120);
        table.getColumnModel().getColumn(5).setPreferredWidth(170);
        table.getColumnModel().getColumn(6).setPreferredWidth(260);
        table.getColumnModel().getColumn(7).setPreferredWidth(320);
        sorter.setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.DESCENDING)));
    }

    private void configureActions() {
        EmcUiTheme.styleTextField(filterField);
        filterField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                applyFilter();
            }
        });
    }

    private void loadEntries() {
        currentEntries.clear();
        currentEntries.addAll(auditLogService.findRecent(projectId, legId, 500));

        tableModel.setRowCount(0);
        for (AuditLogEntry entry : currentEntries) {
            String context = buildContext(entry);
            tableModel.addRow(new Object[]{
                    entry.getCreatedAt() == null ? "---" : DATE_TIME_FORMATTER.format(entry.getCreatedAt()),
                    valueOrDash(entry.getActorName()),
                    valueOrDash(entry.getActorRole()),
                    valueOrDash(entry.getActionType()),
                    valueOrDash(entry.getEntityType()) + (entry.getEntityId() == null ? "" : " #" + entry.getEntityId()),
                    context,
                    valueOrDash(entry.getSummary()),
                    valueOrDash(entry.getDetails())
            });
        }
        applyFilter();
    }

    private String buildContext(AuditLogEntry entry) {
        List<String> parts = new ArrayList<>();
        if (entry.getProjectId() != null) {
            parts.add("projekt #" + entry.getProjectId());
        }
        if (entry.getLegId() != null) {
            parts.add("LEG #" + entry.getLegId());
        }
        if (entry.getStepId() != null) {
            parts.add("test #" + entry.getStepId());
        }
        return parts.isEmpty() ? "---" : String.join(" | ", parts);
    }

    private void applyFilter() {
        String filter = filterField.getText() == null ? "" : filterField.getText().trim();
        if (filter.isEmpty()) {
            sorter.setRowFilter(null);
            return;
        }
        sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(filter)));
    }

    private String valueOrDash(String value) {
        if (value == null || value.isBlank()) {
            return "---";
        }
        return value;
    }
}
