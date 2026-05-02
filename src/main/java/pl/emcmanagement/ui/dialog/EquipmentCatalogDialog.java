package pl.emcmanagement.ui.dialog;

import pl.emcmanagement.dao.MeasurementEquipmentDao;
import pl.emcmanagement.model.MeasurementEquipment;
import pl.emcmanagement.model.User;
import pl.emcmanagement.service.AuditLogService;
import pl.emcmanagement.service.PermissionService;
import pl.emcmanagement.ui.style.EmcUiTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EquipmentCatalogDialog extends JDialog {
    private final User currentUser;
    private final Integer projectId;
    private final Integer legId;
    private final Runnable onCatalogChanged;

    private final PermissionService permissionService = new PermissionService();
    private final AuditLogService auditLogService = new AuditLogService();
    private final MeasurementEquipmentDao equipmentDao = new MeasurementEquipmentDao();

    private final DefaultTableModel equipmentTableModel = new DefaultTableModel(
            new Object[]{"ID", "Kod", "Nazwa", "Kategoria", "Wlasnosc LAB", "Producent", "Model", "Serial", "Kalibracja", "Lokalizacja", "Czujnik klimatu"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 0 ? Integer.class : String.class;
        }
    };
    private final JTable equipmentTable = new JTable(equipmentTableModel);
    private final TableRowSorter<DefaultTableModel> equipmentSorter = new TableRowSorter<>(equipmentTableModel);

    private final JTextField idFilterField = new JTextField();
    private final JTextField codeFilterField = new JTextField();
    private final JTextField nameFilterField = new JTextField();
    private final JTextField categoryFilterField = new JTextField();
    private final JComboBox<String> labOwnedFilterCombo = new JComboBox<>(new String[]{"Wszystkie", "YES", "NO"});
    private final JTextField locationFilterField = new JTextField();

    private final JButton addButton = new JButton("Dodaj");
    private final JButton editButton = new JButton("Edytuj");
    private final JButton deleteButton = new JButton("Usun");
    private final JButton refreshButton = new JButton("Odswiez");
    private final JButton closeButton = new JButton("Zamknij");

    private final List<MeasurementEquipment> currentEquipment = new ArrayList<>();

    public EquipmentCatalogDialog(Window owner, User currentUser, Integer projectId, Integer legId, Runnable onCatalogChanged) {
        super(owner, "Katalog sprzetu pomiarowego", ModalityType.APPLICATION_MODAL);
        this.currentUser = currentUser;
        this.projectId = projectId;
        this.legId = legId;
        this.onCatalogChanged = onCatalogChanged == null ? () -> { } : onCatalogChanged;

        setSize(1260, 760);
        setMinimumSize(new Dimension(1060, 660));
        setLocationRelativeTo(owner);

        JPanel root = EmcUiTheme.createPatternPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(14, 14, 14, 14));
        setContentPane(root);

        root.add(buildFiltersPanel(), BorderLayout.NORTH);

        JScrollPane tableScrollPane = new JScrollPane(equipmentTable);
        EmcUiTheme.styleScrollPane(tableScrollPane);
        root.add(tableScrollPane, BorderLayout.CENTER);

        root.add(buildActionsPanel(), BorderLayout.SOUTH);

        configureTable();
        configureActions();
        applyTheme();
        loadEquipment();
    }

    private Component buildFiltersPanel() {
        JPanel panel = EmcUiTheme.createCardPanel(new BorderLayout(0, 10));

        JLabel title = new JLabel("Filtry katalogu");
        title.setFont(EmcUiTheme.SECTION_FONT);
        panel.add(title, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 0;

        addFilterRow(grid, gbc, 0, "ID:", idFilterField, 0.25);
        addFilterRow(grid, gbc, 2, "Kod:", codeFilterField, 0.65);
        addFilterRow(grid, gbc, 4, "Nazwa:", nameFilterField, 1.0);

        gbc.gridy = 1;
        addFilterRow(grid, gbc, 0, "Kategoria:", categoryFilterField, 0.65);
        addFilterRow(grid, gbc, 2, "Wlasnosc LAB:", labOwnedFilterCombo, 0.25);
        addFilterRow(grid, gbc, 4, "Lokalizacja:", locationFilterField, 0.5);

        JLabel hintLabel = new JLabel("Filtrowanie dziala na zywo. Dwuklik na wierszu otwiera edycje.");
        hintLabel.setForeground(EmcUiTheme.TEXT_MUTED);

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setOpaque(false);
        content.add(grid, BorderLayout.CENTER);
        content.add(hintLabel, BorderLayout.SOUTH);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private Component buildActionsPanel() {
        JPanel panel = EmcUiTheme.createCardPanel(new BorderLayout(0, 10));

        JTextArea infoArea = new JTextArea("TE i Administrator moga dodawac, edytowac i usuwac sprzet. Wlasnosc LAB oraz kalibracja sa utrzymywane bezposrednio w bazie, a usuniecie z katalogu usuwa tez wszystkie przypisania do testow.");
        EmcUiTheme.styleInfoArea(infoArea);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(addButton);
        actions.add(editButton);
        actions.add(deleteButton);
        actions.add(refreshButton);
        actions.add(closeButton);

        panel.add(infoArea, BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private void configureTable() {
        EmcUiTheme.styleTable(equipmentTable);
        equipmentTable.setFillsViewportHeight(true);
        equipmentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        equipmentSorter.setComparator(0, Comparator.comparingInt(value -> {
            if (value instanceof Number number) {
                return number.intValue();
            }
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return Integer.MAX_VALUE;
            }
        }));
        equipmentTable.setRowSorter(equipmentSorter);
    }

    private void configureActions() {
        DocumentListener filterListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilters();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilters();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilters();
            }
        };

        idFilterField.getDocument().addDocumentListener(filterListener);
        codeFilterField.getDocument().addDocumentListener(filterListener);
        nameFilterField.getDocument().addDocumentListener(filterListener);
        categoryFilterField.getDocument().addDocumentListener(filterListener);
        locationFilterField.getDocument().addDocumentListener(filterListener);
        labOwnedFilterCombo.addActionListener(e -> applyFilters());

        equipmentTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonState();
            }
        });

        equipmentTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                if (event.getClickCount() == 2 && editButton.isEnabled()) {
                    editSelectedEquipment();
                }
            }
        });

        addButton.addActionListener(e -> addEquipment());
        editButton.addActionListener(e -> editSelectedEquipment());
        deleteButton.addActionListener(e -> deleteSelectedEquipment());
        refreshButton.addActionListener(e -> loadEquipment());
        closeButton.addActionListener(e -> dispose());
        updateButtonState();
    }

    private void applyTheme() {
        EmcUiTheme.styleTextField(idFilterField);
        EmcUiTheme.styleTextField(codeFilterField);
        EmcUiTheme.styleTextField(nameFilterField);
        EmcUiTheme.styleTextField(categoryFilterField);
        EmcUiTheme.styleTextField(locationFilterField);
        EmcUiTheme.styleComboBox(labOwnedFilterCombo);
        EmcUiTheme.stylePrimaryButton(addButton);
        EmcUiTheme.styleSurfaceButton(editButton);
        EmcUiTheme.styleSurfaceButton(deleteButton);
        EmcUiTheme.styleSurfaceButton(refreshButton);
        EmcUiTheme.styleGhostButton(closeButton);
    }

    private void loadEquipment() {
        currentEquipment.clear();
        currentEquipment.addAll(equipmentDao.findAll());

        equipmentTableModel.setRowCount(0);
        for (MeasurementEquipment equipment : currentEquipment) {
            equipmentTableModel.addRow(new Object[]{
                    equipment.getId(),
                    equipment.getEquipmentCode(),
                    equipment.getEquipmentName(),
                    equipment.getCategory(),
                    equipment.isLabOwned() ? "YES" : "NO",
                    equipment.getManufacturer(),
                    equipment.getModel(),
                    equipment.getSerialNumber(),
                    equipment.getCalibrationValidUntil() == null ? "---" : equipment.getCalibrationValidUntil(),
                    equipment.getLocation(),
                    equipment.getClimateSensorCode() == null ? "---" : equipment.getClimateSensorCode()
            });
        }

        applyFilters();
        updateButtonState();
    }

    private void applyFilters() {
        List<RowFilter<DefaultTableModel, Object>> filters = new ArrayList<>();
        addContainsFilter(filters, 0, idFilterField.getText());
        addContainsFilter(filters, 1, codeFilterField.getText());
        addContainsFilter(filters, 2, nameFilterField.getText());
        addContainsFilter(filters, 3, categoryFilterField.getText());
        addContainsFilter(filters, 9, locationFilterField.getText());

        String labOwned = labOwnedFilterCombo.getSelectedItem() == null ? "Wszystkie" : labOwnedFilterCombo.getSelectedItem().toString();
        if (!"Wszystkie".equals(labOwned)) {
            filters.add(RowFilter.regexFilter("^" + labOwned + "$", 4));
        }

        equipmentSorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
    }

    private void addContainsFilter(List<RowFilter<DefaultTableModel, Object>> filters, int columnIndex, String value) {
        String trimmed = value == null ? "" : value.trim();
        if (!trimmed.isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(trimmed), columnIndex));
        }
    }

    private void updateButtonState() {
        boolean canManageCatalog = permissionService.canManageEquipmentCatalog(currentUser);
        boolean hasSelection = getSelectedEquipment() != null;
        addButton.setEnabled(canManageCatalog);
        editButton.setEnabled(canManageCatalog && hasSelection);
        deleteButton.setEnabled(canManageCatalog && hasSelection);
        refreshButton.setEnabled(true);
    }

    private void addEquipment() {
        MeasurementEquipment equipment = new MeasurementEquipment();
        if (!showEquipmentEditor(equipment, "Dodaj sprzet do katalogu")) {
            return;
        }

        try {
            int equipmentId = equipmentDao.insertEquipment(equipment);
            loadEquipment();
            auditLogService.log(
                    currentUser,
                    "EQUIPMENT_CREATED",
                    "EQUIPMENT",
                    equipmentId,
                    projectId,
                    legId,
                    null,
                    "Dodano sprzet do katalogu",
                    equipment.getEquipmentCode() + " | " + equipment.getEquipmentName()
            );
            onCatalogChanged.run();
            JOptionPane.showMessageDialog(this, "Sprzet zostal dodany do bazy.", "Sukces", JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException exception) {
            showError("Nie udalo sie dodac sprzetu.", exception);
        }
    }

    private void editSelectedEquipment() {
        MeasurementEquipment selectedEquipment = getSelectedEquipment();
        if (selectedEquipment == null) {
            JOptionPane.showMessageDialog(this, "Wybierz sprzet z tabeli.", "Informacja", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        MeasurementEquipment editableCopy = copyOf(selectedEquipment);
        if (!showEquipmentEditor(editableCopy, "Edytuj sprzet w katalogu")) {
            return;
        }

        try {
            equipmentDao.updateEquipment(editableCopy);
            loadEquipment();
            auditLogService.log(
                    currentUser,
                    "EQUIPMENT_UPDATED",
                    "EQUIPMENT",
                    editableCopy.getId(),
                    projectId,
                    legId,
                    null,
                    "Zaktualizowano sprzet w katalogu",
                    editableCopy.getEquipmentCode() + " | " + editableCopy.getEquipmentName()
            );
            onCatalogChanged.run();
            JOptionPane.showMessageDialog(this, "Dane sprzetu zostaly zapisane.", "Sukces", JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException exception) {
            showError("Nie udalo sie zapisac zmian sprzetu.", exception);
        }
    }

    private void deleteSelectedEquipment() {
        MeasurementEquipment selectedEquipment = getSelectedEquipment();
        if (selectedEquipment == null) {
            JOptionPane.showMessageDialog(this, "Wybierz sprzet z tabeli.", "Informacja", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int result = JOptionPane.showConfirmDialog(
                this,
                "Usuniecie sprzetu z bazy usunie go rowniez ze wszystkich testow.\n\n"
                        + selectedEquipment.getEquipmentCode() + " | " + selectedEquipment.getEquipmentName(),
                "Usuwanie sprzetu",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (result != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            equipmentDao.deleteEquipment(selectedEquipment.getId());
            loadEquipment();
            auditLogService.log(
                    currentUser,
                    "EQUIPMENT_DELETED",
                    "EQUIPMENT",
                    selectedEquipment.getId(),
                    projectId,
                    legId,
                    null,
                    "Usunieto sprzet z katalogu",
                    selectedEquipment.getEquipmentCode() + " | " + selectedEquipment.getEquipmentName()
            );
            onCatalogChanged.run();
            JOptionPane.showMessageDialog(this, "Sprzet zostal usuniety z bazy.", "Sukces", JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException exception) {
            showError("Nie udalo sie usunac sprzetu.", exception);
        }
    }

    private MeasurementEquipment getSelectedEquipment() {
        int selectedViewRow = equipmentTable.getSelectedRow();
        if (selectedViewRow < 0) {
            return null;
        }

        int selectedModelRow = equipmentTable.convertRowIndexToModel(selectedViewRow);
        if (selectedModelRow < 0 || selectedModelRow >= currentEquipment.size()) {
            return null;
        }
        return currentEquipment.get(selectedModelRow);
    }

    private boolean showEquipmentEditor(MeasurementEquipment equipment, String title) {
        JTextField codeField = new JTextField(valueOrEmpty(equipment.getEquipmentCode()));
        JTextField nameField = new JTextField(valueOrEmpty(equipment.getEquipmentName()));
        JTextField categoryField = new JTextField(valueOrEmpty(equipment.getCategory()));
        JComboBox<String> labOwnedCombo = new JComboBox<>(new String[]{"YES", "NO"});
        labOwnedCombo.setSelectedItem(equipment.isLabOwned() ? "YES" : "NO");
        JTextField manufacturerField = new JTextField(valueOrEmpty(equipment.getManufacturer()));
        JTextField modelField = new JTextField(valueOrEmpty(equipment.getModel()));
        JTextField serialField = new JTextField(valueOrEmpty(equipment.getSerialNumber()));
        JTextField calibrationField = new JTextField(equipment.getCalibrationValidUntil() == null ? "---" : equipment.getCalibrationValidUntil().toString());
        JTextField locationField = new JTextField(valueOrEmpty(equipment.getLocation()));
        JTextField climateSensorField = new JTextField(valueOrEmpty(equipment.getClimateSensorCode()));
        JTextArea notesArea = new JTextArea(valueOrEmpty(equipment.getNotes()), 4, 24);

        EmcUiTheme.styleTextField(codeField);
        EmcUiTheme.styleTextField(nameField);
        EmcUiTheme.styleTextField(categoryField);
        EmcUiTheme.styleComboBox(labOwnedCombo);
        EmcUiTheme.styleTextField(manufacturerField);
        EmcUiTheme.styleTextField(modelField);
        EmcUiTheme.styleTextField(serialField);
        EmcUiTheme.styleTextField(calibrationField);
        EmcUiTheme.styleTextField(locationField);
        EmcUiTheme.styleTextField(climateSensorField);
        EmcUiTheme.styleTextArea(notesArea);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 0;

        addFormRow(formPanel, gbc, "Kod sprzetu:", codeField);
        addFormRow(formPanel, gbc, "Nazwa:", nameField);
        addFormRow(formPanel, gbc, "Kategoria:", categoryField);
        addFormRow(formPanel, gbc, "Wlasnosc LAB:", labOwnedCombo);
        addFormRow(formPanel, gbc, "Producent:", manufacturerField);
        addFormRow(formPanel, gbc, "Model:", modelField);
        addFormRow(formPanel, gbc, "Serial:", serialField);
        addFormRow(formPanel, gbc, "Kalibracja (YYYY-MM-DD lub ---):", calibrationField);
        addFormRow(formPanel, gbc, "Lokalizacja:", locationField);
        addFormRow(formPanel, gbc, "Kod czujnika klimatu:", climateSensorField);

        gbc.gridx = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Notatki:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        formPanel.add(new JScrollPane(notesArea), gbc);

        int result = JOptionPane.showConfirmDialog(
                this,
                formPanel,
                title,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (result != JOptionPane.OK_OPTION) {
            return false;
        }

        String code = codeField.getText().trim();
        String name = nameField.getText().trim();
        String category = categoryField.getText().trim();
        if (code.isBlank() || name.isBlank() || category.isBlank()) {
            JOptionPane.showMessageDialog(this, "Kod, nazwa i kategoria sa wymagane.", "Walidacja", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        equipment.setEquipmentCode(code);
        equipment.setEquipmentName(name);
        equipment.setCategory(category);
        equipment.setLabOwned("YES".equals(labOwnedCombo.getSelectedItem()));
        equipment.setManufacturer(trimToNull(manufacturerField.getText()));
        equipment.setModel(trimToNull(modelField.getText()));
        equipment.setSerialNumber(trimToNull(serialField.getText()));
        equipment.setCalibrationValidUntil(parseOptionalDate(calibrationField.getText()));
        equipment.setLocation(trimToNull(locationField.getText()));
        equipment.setClimateSensorCode(trimToNull(climateSensorField.getText()));
        equipment.setNotes(trimToNull(notesArea.getText()));
        return true;
    }

    private MeasurementEquipment copyOf(MeasurementEquipment source) {
        MeasurementEquipment copy = new MeasurementEquipment();
        copy.setId(source.getId());
        copy.setEquipmentCode(source.getEquipmentCode());
        copy.setEquipmentName(source.getEquipmentName());
        copy.setCategory(source.getCategory());
        copy.setLabOwned(source.isLabOwned());
        copy.setManufacturer(source.getManufacturer());
        copy.setModel(source.getModel());
        copy.setSerialNumber(source.getSerialNumber());
        copy.setCalibrationValidUntil(source.getCalibrationValidUntil());
        copy.setLocation(source.getLocation());
        copy.setClimateSensorCode(source.getClimateSensorCode());
        copy.setNotes(source.getNotes());
        return copy;
    }

    private void addFilterRow(JPanel panel, GridBagConstraints gbc, int column, String label, Component field, double weight) {
        gbc.gridx = column;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = column + 1;
        gbc.weightx = weight;
        panel.add(field, gbc);
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, String label, Component field) {
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(field, gbc);
        gbc.gridy++;
    }

    private LocalDate parseOptionalDate(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty() || trimmed.equals("---")) {
            return null;
        }
        try {
            return LocalDate.parse(trimmed);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Niepoprawny format daty: " + trimmed + ". Uzyj YYYY-MM-DD albo ---.");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private void showError(String message, Exception exception) {
        JOptionPane.showMessageDialog(
                this,
                message + "\n\n" + exception.getMessage(),
                "Blad",
                JOptionPane.ERROR_MESSAGE
        );
    }
}
