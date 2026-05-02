package pl.emcmanagement.ui.dialog;

import pl.emcmanagement.dao.ClimateLogDao;
import pl.emcmanagement.dao.DutResultRow;
import pl.emcmanagement.dao.DutSampleDao;
import pl.emcmanagement.dao.DutTestResultDao;
import pl.emcmanagement.dao.LegDao;
import pl.emcmanagement.dao.LegTestStepDao;
import pl.emcmanagement.dao.MeasurementEquipmentDao;
import pl.emcmanagement.dao.StepMediaDao;
import pl.emcmanagement.dao.UserDao;
import pl.emcmanagement.enums.ObservedFunctionalClass;
import pl.emcmanagement.enums.StepMediaKind;
import pl.emcmanagement.enums.StepSetupSlot;
import pl.emcmanagement.enums.TestStatus;
import pl.emcmanagement.model.ClimateDataset;
import pl.emcmanagement.model.ClimateMeasurement;
import pl.emcmanagement.model.DutSample;
import pl.emcmanagement.model.Leg;
import pl.emcmanagement.model.LegTestStep;
import pl.emcmanagement.model.MeasurementEquipment;
import pl.emcmanagement.model.Project;
import pl.emcmanagement.model.StepMedia;
import pl.emcmanagement.model.User;
import pl.emcmanagement.service.ClimateDataImportService;
import pl.emcmanagement.service.PermissionService;
import pl.emcmanagement.service.AuditLogService;
import pl.emcmanagement.ui.style.EmcUiTheme;
import pl.emcmanagement.util.DesktopActions;
import pl.emcmanagement.util.ImageStorageUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

public class LegDetailsDialog extends JDialog {
    private static final String NO_TECHNICIAN_LABEL = "--- brak przypisania ---";
    private static final String[] ROOM_OPTIONS = {"", "LabA", "LabB", "LabC", "LabD", "LabE"};
    private static final int STEP_STATUS_COLUMN = 2;
    private static final int STEP_START_COLUMN = 3;
    private static final int STEP_END_COLUMN = 4;
    private static final int STEP_ROOM_COLUMN = 5;
    private static final String PLANNED_STATUS_LABEL = "Planned";

    private final User currentUser;
    private final Project project;
    private final Runnable onDataChanged;

    private final PermissionService permissionService = new PermissionService();
    private final LegDao legDao = new LegDao();
    private final LegTestStepDao legTestStepDao = new LegTestStepDao();
    private final DutTestResultDao dutTestResultDao = new DutTestResultDao();
    private final DutSampleDao dutSampleDao = new DutSampleDao();
    private final MeasurementEquipmentDao equipmentDao = new MeasurementEquipmentDao();
    private final ClimateLogDao climateLogDao = new ClimateLogDao();
    private final StepMediaDao stepMediaDao = new StepMediaDao();
    private final ClimateDataImportService climateDataImportService = new ClimateDataImportService();
    private final UserDao userDao = new UserDao();
    private final AuditLogService auditLogService = new AuditLogService();

    private Leg leg;

    private final JTable stepsTable = new JTable();
    private final JTable dutTable = new JTable();
    private final JTable equipmentTable = new JTable();
    private final JTable climateTable = new JTable();
    private final JTable setupMediaTable = new JTable();
    private final JTable verificationTable = new JTable();

    private DefaultTableModel stepsTableModel;
    private DefaultTableModel dutTableModel;
    private DefaultTableModel equipmentTableModel;
    private DefaultTableModel climateTableModel;
    private DefaultTableModel setupMediaTableModel;
    private DefaultTableModel verificationTableModel;

    private final JLabel selectedStepLabel = new JLabel("Wybierz krok testowy, aby zobaczyc przypisane DUT-y.");
    private final JLabel dutModeLabel = new JLabel("Zakres DUT: domyslnie z calego LEGu.");
    private final JLabel selectedEquipmentStepLabel = new JLabel("Wybierz krok testowy, aby przypisywac sprzet.");
    private final JLabel equipmentHintLabel = new JLabel("Widok pokazuje sprzet przypisany tylko do wybranego testu.");
    private final JLabel selectedClimateStepLabel = new JLabel("Wybierz krok testowy, aby pobrac warunki klimatyczne.");
    private final JLabel climateFilesLabel = new JLabel("Pliki: ---");
    private final JLabel climateRangeLabel = new JLabel("Zakres: ---");
    private final JLabel climateTempLabel = new JLabel("Temperatura: ---");
    private final JLabel climateHumidityLabel = new JLabel("Wilgotnosc: ---");
    private final JLabel selectedSetupStepLabel = new JLabel("Wybierz krok testowy, aby zarzadzac zdjeciami setupu.");
    private final JLabel selectedVerificationStepLabel = new JLabel("Wybierz krok testowy, aby zarzadzac weryfikacjami.");
    private final JLabel legTitleLabel = new JLabel("---");
    private final JLabel legStatusChipLabel = new JLabel("Status: ---");
    private final JLabel statusValueLabel = new JLabel("---");
    private final JLabel startDateValueLabel = new JLabel("---");
    private final JLabel endDateValueLabel = new JLabel("---");
    private final JLabel legCodeValueLabel = new JLabel("---");
    private final JLabel typeValueLabel = new JLabel("---");
    private final JLabel accreditationValueLabel = new JLabel("---");
    private final JLabel isoValueLabel = new JLabel("---");
    private final JLabel clientValueLabel = new JLabel("---");
    private final JLabel testPlanValueLabel = new JLabel("---");
    private final JLabel technicianValueLabel = new JLabel("---");

    private final JButton saveStepButton = new JButton("Zapisz krok");
    private final JButton saveDutButton = new JButton("Zapisz wyniki DUT");
    private final JButton assignLegTechnicianButton = new JButton("Przypisz TT do LEGu");
    private final JButton assignDutButton = new JButton("DUT-y LEGu");
    private final JButton manageStepDutButton = new JButton("Wyjatek dla testu");
    private final JButton resetStepDutButton = new JButton("Przywroc z LEGu");
    private final JButton assignExistingEquipmentButton = new JButton("Przypisz z katalogu");
    private final JButton removeEquipmentFromTestButton = new JButton("Usun z testu");
    private final JButton addEquipmentButton = new JButton("Katalog sprzetu");
    private final JButton loadClimateButton = new JButton("Pobierz warunki");
    private final JButton previewClimateChartButton = new JButton("Podglad wykresu");
    private final JButton importClimateFilesButton = new JButton("Importuj z pulpitu");
    private final JButton uploadSetupMediaButton = new JButton("Dodaj / podmien");
    private final JButton openSetupMediaButton = new JButton("Otworz");
    private final JButton removeSetupMediaButton = new JButton("Usun");
    private final JButton uploadVerificationButton = new JButton("Dodaj plik");
    private final JButton openVerificationButton = new JButton("Otworz");
    private final JButton removeVerificationButton = new JButton("Usun");
    private final JButton openIsoButton = new JButton("Norma ISO");
    private final JButton openClientButton = new JButton("Norma klienta");
    private final JButton openTestPlanButton = new JButton("Test plan");
    private final JButton pcaButton = new JButton("PCA");
    private final JButton mailButton = new JButton("E-mail VE");

    private final List<LegTestStep> currentSteps = new ArrayList<>();
    private final List<DutResultRow> currentDutResults = new ArrayList<>();
    private final List<MeasurementEquipment> currentEquipment = new ArrayList<>();
    private final List<ClimateMeasurement> currentClimateMeasurements = new ArrayList<>();
    private final List<StepMedia> currentSetupMedia = new ArrayList<>();
    private final List<StepMedia> currentVerificationMedia = new ArrayList<>();

    private LegTestStep selectedStep;
    private String currentClimateRoomCode;
    private String currentClimateSourceLabel;
    private LocalDate currentClimateStartDate;
    private LocalDate currentClimateEndDate;

    public LegDetailsDialog(Window owner, User currentUser, Project project, Leg leg, Runnable onDataChanged) {
        super(owner, "Szczegoly LEGu - " + leg.getLegCode(), ModalityType.APPLICATION_MODAL);
        this.currentUser = currentUser;
        this.project = project;
        this.leg = leg;
        this.onDataChanged = onDataChanged == null ? () -> { } : onDataChanged;

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int dialogWidth = Math.min(1220, Math.max(980, screenSize.width - 140));
        int dialogHeight = Math.min(780, Math.max(680, screenSize.height - 160));
        setSize(dialogWidth, dialogHeight);
        setMinimumSize(new Dimension(960, 660));
        setLocationRelativeTo(owner);

        JPanel root = EmcUiTheme.createPatternPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));
        setContentPane(root);

        root.add(buildHeaderPanel(), BorderLayout.NORTH);
        root.add(buildCenterPanel(), BorderLayout.CENTER);
        root.add(buildFooterPanel(), BorderLayout.SOUTH);

        applyTheme();
        configureListeners();
        loadData(null);
    }

    private Component buildHeaderPanel() {
        JPanel wrapper = EmcUiTheme.createCardPanel(new BorderLayout(0, 10));

        JPanel topRow = new JPanel(new BorderLayout(12, 0));
        topRow.setOpaque(false);

        legTitleLabel.setFont(legTitleLabel.getFont().deriveFont(Font.BOLD, 24f));
        legStatusChipLabel.setFont(legStatusChipLabel.getFont().deriveFont(Font.BOLD, 13f));
        legStatusChipLabel.setOpaque(true);
        legStatusChipLabel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.add(legTitleLabel);
        titlePanel.add(Box.createVerticalStrut(8));
        titlePanel.add(legStatusChipLabel);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
        actions.add(assignLegTechnicianButton);

        topRow.add(titlePanel, BorderLayout.CENTER);
        topRow.add(actions, BorderLayout.EAST);

        JPanel metaGrid = new JPanel(new GridLayout(2, 4, 10, 10));
        metaGrid.setOpaque(false);
        metaGrid.add(createMetaCard("Start", startDateValueLabel));
        metaGrid.add(createMetaCard("Koniec", endDateValueLabel));
        metaGrid.add(createMetaCard("Typ", typeValueLabel));
        metaGrid.add(createMetaCard("Akredytacja", accreditationValueLabel));
        metaGrid.add(createMetaCard("Norma ISO", isoValueLabel));
        metaGrid.add(createMetaCard("TT", technicianValueLabel));
        metaGrid.add(createMetaCard("Norma klienta", clientValueLabel));
        metaGrid.add(createMetaCard("Test plan", testPlanValueLabel));

        wrapper.add(topRow, BorderLayout.NORTH);
        wrapper.add(metaGrid, BorderLayout.CENTER);
        return wrapper;
    }

    private Component buildCenterPanel() {
        stepsTableModel = new DefaultTableModel(new Object[]{"Lp.", "Nazwa kroku", "Status", "Start", "Koniec", "Pomieszczenie"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column >= STEP_START_COLUMN
                        && column <= STEP_ROOM_COLUMN
                        && permissionService.canChangeLegStatus(currentUser, leg);
            }
        };
        stepsTable.setModel(stepsTableModel);
        stepsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        stepsTable.setRowHeight(22);
        stepsTable.setFillsViewportHeight(true);
        stepsTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        JTextField dateEditorField = new JTextField();
        EmcUiTheme.styleTextField(dateEditorField);
        stepsTable.getColumnModel().getColumn(STEP_START_COLUMN).setCellEditor(new DefaultCellEditor(dateEditorField));
        JTextField endDateEditorField = new JTextField();
        EmcUiTheme.styleTextField(endDateEditorField);
        stepsTable.getColumnModel().getColumn(STEP_END_COLUMN).setCellEditor(new DefaultCellEditor(endDateEditorField));
        JComboBox<String> roomEditor = new JComboBox<>(ROOM_OPTIONS);
        EmcUiTheme.styleComboBox(roomEditor);
        stepsTable.getColumnModel().getColumn(STEP_ROOM_COLUMN).setCellEditor(new DefaultCellEditor(roomEditor));

        dutTableModel = new DefaultTableModel(new Object[]{"DUT", "Observed functional class", "Result", "Date", "Comment"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column > 0 && canEditCurrentDutResults();
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return switch (columnIndex) {
                    case 1 -> ObservedFunctionalClass.class;
                    case 2 -> TestStatus.class;
                    case 3 -> String.class;
                    default -> String.class;
                };
            }
        };
        dutTable.setModel(dutTableModel);
        dutTable.setRowHeight(22);
        dutTable.setFillsViewportHeight(true);
        dutTable.getColumnModel().getColumn(1)
                .setCellEditor(new DefaultCellEditor(new JComboBox<>(ObservedFunctionalClass.values())));
        dutTable.getColumnModel().getColumn(2)
                .setCellEditor(new DefaultCellEditor(new JComboBox<>(TestStatus.values())));
        JTextField executionDateField = new JTextField();
        EmcUiTheme.styleTextField(executionDateField);
        dutTable.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(executionDateField));

        equipmentTableModel = new DefaultTableModel(new Object[]{"Kod", "Nazwa", "Kategoria", "Rezerwacja od", "Rezerwacja do", "Lokalizacja"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        equipmentTable.setModel(equipmentTableModel);
        equipmentTable.setRowHeight(22);
        equipmentTable.setFillsViewportHeight(true);

        climateTableModel = new DefaultTableModel(new Object[]{"Data", "Czas", "Temp [C]", "RH [%]", "Plik"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        climateTable.setModel(climateTableModel);
        climateTable.setRowHeight(22);
        climateTable.setFillsViewportHeight(true);

        setupMediaTableModel = new DefaultTableModel(new Object[]{"Pozycja", "Plik"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        setupMediaTable.setModel(setupMediaTableModel);
        setupMediaTable.setRowHeight(22);
        setupMediaTable.setFillsViewportHeight(true);

        verificationTableModel = new DefaultTableModel(new Object[]{"Pozycja", "Plik"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        verificationTable.setModel(verificationTableModel);
        verificationTable.setRowHeight(22);
        verificationTable.setFillsViewportHeight(true);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Test flow", buildStepsPanel());
        tabs.addTab("DUT", buildDutPanel());
        tabs.addTab("Sprzet pomiarowy", buildEquipmentPanel());
        tabs.addTab("Warunki klimatyczne", buildClimatePanel());
        tabs.addTab("Zdjecia setupu", buildSetupMediaPanel());
        tabs.addTab("Weryfikacje", buildVerificationPanel());
        EmcUiTheme.styleTabbedPane(tabs);
        return tabs;
    }

    private Component buildFooterPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        panel.setOpaque(false);

        openIsoButton.addActionListener(e -> DesktopActions.openStoredDocument(this, leg.getIsoFileName(), leg.getIsoFileData()));
        openClientButton.addActionListener(e -> DesktopActions.openStoredDocument(this, leg.getClientFileName(), leg.getClientFileData()));
        openTestPlanButton.addActionListener(e -> DesktopActions.openStoredDocument(this, leg.getTestPlanFileName(), leg.getTestPlanFileData()));
        pcaButton.addActionListener(e -> DesktopActions.openUrl(this, leg.getPcaUrl()));
        mailButton.addActionListener(e -> DesktopActions.sendMail(this, project.getVe() == null ? null : project.getVe().getEmail()));

        panel.add(openIsoButton);
        panel.add(openClientButton);
        panel.add(openTestPlanButton);
        panel.add(pcaButton);
        panel.add(mailButton);
        wrapper.add(panel, BorderLayout.EAST);
        return wrapper;
    }

    private Component buildStepsPanel() {
        JPanel panel = EmcUiTheme.createCardPanel(new BorderLayout(0, 10));

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);

        JLabel title = new JLabel("Test flow / kroki podrzedne");
        title.setFont(EmcUiTheme.SECTION_FONT);
        JLabel hint = new JLabel("Status wynika tylko z DUT-ow. Edytuj bezposrednio Start, Koniec i Pomieszczenie w tabeli.");
        hint.setForeground(EmcUiTheme.TEXT_MUTED);

        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        labels.add(title);
        labels.add(Box.createVerticalStrut(4));
        labels.add(hint);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
        actions.add(saveStepButton);

        header.add(labels, BorderLayout.CENTER);
        header.add(actions, BorderLayout.EAST);

        JScrollPane scrollPane = new JScrollPane(stepsTable);
        EmcUiTheme.styleScrollPane(scrollPane);

        panel.add(header, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private Component buildDutPanel() {
        JPanel panel = EmcUiTheme.createCardPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        selectedStepLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));

        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("DUT-y / wyniki probek");
        title.setFont(EmcUiTheme.SECTION_FONT);
        labels.add(title);
        labels.add(Box.createVerticalStrut(4));
        labels.add(selectedStepLabel);
        labels.add(Box.createVerticalStrut(2));
        labels.add(dutModeLabel);
        header.add(labels, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(assignDutButton);
        actions.add(manageStepDutButton);
        actions.add(resetStepDutButton);
        actions.add(saveDutButton);
        header.add(actions, BorderLayout.EAST);

        panel.add(header, BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(dutTable);
        EmcUiTheme.styleScrollPane(scrollPane);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private Component buildEquipmentPanel() {
        JPanel panel = EmcUiTheme.createCardPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        selectedEquipmentStepLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));

        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Sprzet pomiarowy dla testu");
        title.setFont(EmcUiTheme.SECTION_FONT);
        labels.add(title);
        labels.add(Box.createVerticalStrut(4));
        labels.add(selectedEquipmentStepLabel);
        labels.add(Box.createVerticalStrut(2));
        labels.add(equipmentHintLabel);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(assignExistingEquipmentButton);
        actions.add(removeEquipmentFromTestButton);
        actions.add(addEquipmentButton);
        header.add(labels, BorderLayout.CENTER);
        header.add(actions, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(equipmentTable);
        EmcUiTheme.styleScrollPane(scrollPane);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private Component buildClimatePanel() {
        JPanel panel = EmcUiTheme.createCardPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);

        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Warunki klimatyczne");
        title.setFont(EmcUiTheme.SECTION_FONT);
        labels.add(title);
        labels.add(Box.createVerticalStrut(4));
        labels.add(selectedClimateStepLabel);
        labels.add(Box.createVerticalStrut(4));
        labels.add(climateFilesLabel);
        header.add(labels, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(importClimateFilesButton);
        actions.add(loadClimateButton);
        actions.add(previewClimateChartButton);
        header.add(actions, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        JPanel summaryPanel = new JPanel(new GridLayout(1, 3, 8, 0));
        summaryPanel.setOpaque(false);
        summaryPanel.add(createSummaryCard("Zakres", climateRangeLabel));
        summaryPanel.add(createSummaryCard("Temperatura", climateTempLabel));
        summaryPanel.add(createSummaryCard("Wilgotnosc", climateHumidityLabel));

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        center.add(summaryPanel, BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(climateTable);
        EmcUiTheme.styleScrollPane(scrollPane);
        center.add(scrollPane, BorderLayout.CENTER);
        panel.add(center, BorderLayout.CENTER);
        return panel;
    }

    private Component buildSetupMediaPanel() {
        JPanel panel = EmcUiTheme.createCardPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Zdjecia setupu testu");
        title.setFont(EmcUiTheme.SECTION_FONT);
        labels.add(title);
        labels.add(Box.createVerticalStrut(4));
        labels.add(selectedSetupStepLabel);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(uploadSetupMediaButton);
        actions.add(openSetupMediaButton);
        actions.add(removeSetupMediaButton);

        header.add(labels, BorderLayout.CENTER);
        header.add(actions, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(setupMediaTable);
        EmcUiTheme.styleScrollPane(scrollPane);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private Component buildVerificationPanel() {
        JPanel panel = EmcUiTheme.createCardPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Weryfikacje / screeny");
        title.setFont(EmcUiTheme.SECTION_FONT);
        labels.add(title);
        labels.add(Box.createVerticalStrut(4));
        labels.add(selectedVerificationStepLabel);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(uploadVerificationButton);
        actions.add(openVerificationButton);
        actions.add(removeVerificationButton);

        header.add(labels, BorderLayout.CENTER);
        header.add(actions, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(verificationTable);
        EmcUiTheme.styleScrollPane(scrollPane);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void configureListeners() {
        stepsTable.getSelectionModel().addListSelectionListener(this::onStepSelected);
        equipmentTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updatePermissionState();
            }
        });
        setupMediaTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updatePermissionState();
            }
        });
        verificationTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updatePermissionState();
            }
        });
        saveStepButton.addActionListener(e -> saveSelectedStep());
        saveDutButton.addActionListener(e -> saveDutResults());
        assignLegTechnicianButton.addActionListener(e -> assignTechnicianToLeg());
        assignDutButton.addActionListener(e -> manageDutAssignments());
        manageStepDutButton.addActionListener(e -> manageStepSpecificDutAssignments());
        resetStepDutButton.addActionListener(e -> resetStepDutsToLegDefaults());
        assignExistingEquipmentButton.addActionListener(e -> assignExistingEquipment());
        removeEquipmentFromTestButton.addActionListener(e -> removeEquipmentFromCurrentTest());
        addEquipmentButton.addActionListener(e -> openEquipmentCatalogDialog());
        loadClimateButton.addActionListener(e -> loadClimateConditionsForSelectedStep());
        previewClimateChartButton.addActionListener(e -> openClimateChartPreview());
        importClimateFilesButton.addActionListener(e -> importClimateFilesFromDesktop());
        uploadSetupMediaButton.addActionListener(e -> uploadSetupMedia());
        openSetupMediaButton.addActionListener(e -> openSelectedSetupMedia());
        removeSetupMediaButton.addActionListener(e -> removeSelectedSetupMedia());
        uploadVerificationButton.addActionListener(e -> uploadVerificationMedia());
        openVerificationButton.addActionListener(e -> openSelectedVerificationMedia());
        removeVerificationButton.addActionListener(e -> removeSelectedVerificationMedia());
    }

    private void onStepSelected(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }

        int selectedRow = stepsTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= currentSteps.size()) {
            selectedStep = null;
            clearDutTable();
            clearEquipmentTable();
            clearClimateTable();
            clearSetupMediaTable();
            clearVerificationTable();
            selectedStepLabel.setText("Wybierz krok testowy, aby zobaczyc przypisane DUT-y.");
            dutModeLabel.setText("Zakres DUT: domyslnie z calego LEGu.");
            updateEquipmentTargetLabel();
            updateClimateTargetLabel();
            selectedSetupStepLabel.setText("Wybierz krok testowy, aby zarzadzac zdjeciami setupu.");
            selectedVerificationStepLabel.setText("Wybierz krok testowy, aby zarzadzac weryfikacjami.");
            updatePermissionState();
            return;
        }

        selectedStep = currentSteps.get(selectedRow);
        selectedStepLabel.setText("Wybrany krok: " + selectedStep.getStepOrder() + ". " + selectedStep.getStepName());
        loadDutResultsForStep(selectedStep.getId());
        loadEquipmentForSelectedStep();
        loadSetupMediaForSelectedStep();
        loadVerificationMediaForSelectedStep();
        clearClimateTable();
        updateEquipmentTargetLabel();
        updateClimateTargetLabel();
        updatePermissionState();
    }

    private void loadData(Integer preferredStepId) {
        leg = legDao.findById(leg.getId());
        setTitle("Szczegoly LEGu - " + leg.getLegCode());
        updateHeaderLabels();
        loadSteps(preferredStepId);
        updatePermissionState();
    }

    private void loadSteps(Integer preferredStepId) {
        currentSteps.clear();
        currentSteps.addAll(legTestStepDao.findByLegId(leg.getId()));

        stepsTableModel.setRowCount(0);
        for (LegTestStep step : currentSteps) {
            stepsTableModel.addRow(new Object[]{
                    step.getStepOrder(),
                    step.getStepName(),
                    getDisplayedStepStatus(step),
                    step.getStartDate() == null ? "" : step.getStartDate().toString(),
                    step.getEndDate() == null ? "" : step.getEndDate().toString(),
                    step.getTestRoom() == null ? "" : step.getTestRoom()
            });
        }

        if (currentSteps.isEmpty()) {
            selectedStep = null;
            clearDutTable();
            clearEquipmentTable();
            clearClimateTable();
            clearSetupMediaTable();
            clearVerificationTable();
            selectedStepLabel.setText("Brak krokow testowych dla tego LEGu.");
            updateEquipmentTargetLabel();
            updateClimateTargetLabel();
            return;
        }

        int rowToSelect = 0;
        if (preferredStepId != null) {
            for (int i = 0; i < currentSteps.size(); i++) {
                if (currentSteps.get(i).getId() == preferredStepId) {
                    rowToSelect = i;
                    break;
                }
            }
        }
        stepsTable.setRowSelectionInterval(rowToSelect, rowToSelect);
    }

    private void loadDutResultsForStep(int stepId) {
        currentDutResults.clear();
        currentDutResults.addAll(dutTestResultDao.findResultsByStepId(stepId));
        boolean customStepAssignments = dutSampleDao.hasCustomAssignmentsForStep(stepId);
        dutModeLabel.setText(customStepAssignments
                ? "Zakres DUT: wyjatek ustawiony tylko dla tego testu."
                : "Zakres DUT: domyslnie dziedziczony z calego LEGu.");

        dutTableModel.setRowCount(0);
        for (DutResultRow row : currentDutResults) {
            dutTableModel.addRow(new Object[]{
                    row.getSampleCode(),
                    row.getObservedFunctionalClass(),
                    row.getResultStatus(),
                    row.getExecutionDate() == null ? "" : row.getExecutionDate().toString(),
                    row.getComment()
            });
        }
    }

    private void loadEquipmentForSelectedStep() {
        currentEquipment.clear();
        equipmentTableModel.setRowCount(0);
        if (selectedStep == null) {
            return;
        }

        currentEquipment.addAll(equipmentDao.findByStepId(selectedStep.getId()));
        for (MeasurementEquipment equipment : currentEquipment) {
            equipmentTableModel.addRow(new Object[]{
                    equipment.getEquipmentCode(),
                    equipment.getEquipmentName(),
                    equipment.getCategory(),
                    equipment.getReservedFrom(),
                    equipment.getReservedTo(),
                    equipment.getLocation()
            });
        }
    }

    private void updateHeaderLabels() {
        statusValueLabel.setText(leg.getStatus().getDisplayName());
        startDateValueLabel.setText(valueOrDash(leg.getStartDate()));
        endDateValueLabel.setText(valueOrDash(leg.getEndDate()));
        legCodeValueLabel.setText(valueOrDash(leg.getLegCode()));
        typeValueLabel.setText(leg.getTestType() == null ? "---" : leg.getTestType().name());
        accreditationValueLabel.setText(leg.getAccreditation() == null ? "---" : leg.getAccreditation().name());
        isoValueLabel.setText(valueOrDash(leg.getIsoStandardName()));
        clientValueLabel.setText(asWrappedHtml(valueOrDash(leg.getClientStandardName())));
        testPlanValueLabel.setText(asWrappedHtml(valueOrDash(leg.getTestPlanName())));
        technicianValueLabel.setText(asWrappedHtml(leg.getAssignedTt() == null
                ? "---"
                : leg.getAssignedTt().getFullName() + " - " + leg.getAssignedTt().getEmail()));
        openIsoButton.setEnabled(leg.getIsoFileData() != null && leg.getIsoFileData().length > 0);
        openClientButton.setEnabled(leg.getClientFileData() != null && leg.getClientFileData().length > 0);
        openTestPlanButton.setEnabled(leg.getTestPlanFileData() != null && leg.getTestPlanFileData().length > 0);
        pcaButton.setEnabled(trimToNull(leg.getPcaUrl()) != null);
        mailButton.setEnabled(project.getVe() != null && trimToNull(project.getVe().getEmail()) != null);

        legTitleLabel.setText(valueOrDash(leg.getLegCode()) + " | " + (leg.getTestType() == null ? "---" : leg.getTestType().name()));
        updateLegStatusChip(leg.getStatus());
    }

    private void updatePermissionState() {
        boolean canChangeSchedule = selectedStep != null && permissionService.canChangeLegStatus(currentUser, leg);
        saveStepButton.setEnabled(canChangeSchedule);
        stepsTable.repaint();

        boolean canEditDutResults = selectedStep != null && canEditCurrentDutResults() && !currentDutResults.isEmpty();
        saveDutButton.setEnabled(canEditDutResults);

        assignLegTechnicianButton.setEnabled(permissionService.canAssignLegTechnician(currentUser));
        assignDutButton.setEnabled(permissionService.canManageLegDutAssignments(currentUser, project));
        manageStepDutButton.setEnabled(permissionService.canManageLegDutAssignments(currentUser, project) && selectedStep != null);
        resetStepDutButton.setEnabled(permissionService.canManageLegDutAssignments(currentUser, project)
                && selectedStep != null
                && dutSampleDao.hasCustomAssignmentsForStep(selectedStep.getId()));

        boolean canManageEquipment = permissionService.canManageEquipment(currentUser, project);
        boolean canManageEquipmentCatalog = permissionService.canManageEquipmentCatalog(currentUser);
        boolean stepSelected = selectedStep != null;
        assignExistingEquipmentButton.setEnabled(canManageEquipment && stepSelected);
        removeEquipmentFromTestButton.setEnabled(canManageEquipment && stepSelected && equipmentTable.getSelectedRow() >= 0);
        addEquipmentButton.setEnabled(canManageEquipmentCatalog);
        boolean canManageClimate = permissionService.canManageClimateFiles(currentUser);
        importClimateFilesButton.setEnabled(canManageClimate);
        loadClimateButton.setEnabled(stepSelected && canManageClimate);
        previewClimateChartButton.setEnabled(canManageClimate && !currentClimateMeasurements.isEmpty());

        boolean canManageMedia = selectedStep != null && (permissionService.canEditDutResults(currentUser, leg) || permissionService.isAdministrator(currentUser));
        uploadSetupMediaButton.setEnabled(canManageMedia);
        openSetupMediaButton.setEnabled(stepSelected && getSelectedSetupMedia() != null);
        removeSetupMediaButton.setEnabled(canManageMedia && getSelectedSetupMedia() != null);
        uploadVerificationButton.setEnabled(canManageMedia);
        openVerificationButton.setEnabled(stepSelected && getSelectedVerificationMedia() != null);
        removeVerificationButton.setEnabled(canManageMedia && getSelectedVerificationMedia() != null);
    }

    private boolean canEditCurrentDutResults() {
        return permissionService.canEditDutResults(currentUser, leg);
    }

    private void saveSelectedStep() {
        if (selectedStep == null) {
            return;
        }
        if (!permissionService.canChangeLegStatus(currentUser, leg)) {
            JOptionPane.showMessageDialog(this, "Nie masz uprawnien do edycji terminu i pomieszczenia tego kroku.", "Brak uprawnien", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            stopTableEditing(stepsTable);
            int selectedRow = stepsTable.getSelectedRow();
            if (selectedRow < 0 || selectedRow >= currentSteps.size()) {
                JOptionPane.showMessageDialog(this, "Wybierz krok testowy, ktory chcesz zapisac.", "Informacja", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            LegTestStep stepToSave = currentSteps.get(selectedRow);
            LocalDate chosenStart = parseOptionalDateValue(stepsTableModel.getValueAt(selectedRow, STEP_START_COLUMN));
            LocalDate chosenEnd = parseOptionalDateValue(stepsTableModel.getValueAt(selectedRow, STEP_END_COLUMN));
            String chosenRoom = trimToNull(stepsTableModel.getValueAt(selectedRow, STEP_ROOM_COLUMN));

            if (chosenStart == null && chosenEnd != null) {
                JOptionPane.showMessageDialog(this, "Nie mozna ustawic daty konca bez daty startu.", "Walidacja", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (chosenStart != null && chosenEnd != null && chosenEnd.isBefore(chosenStart)) {
                JOptionPane.showMessageDialog(this, "Data konca kroku nie moze byc wczesniejsza niz data startu.", "Walidacja", JOptionPane.WARNING_MESSAGE);
                return;
            }

            stepToSave.setStartDate(chosenStart);
            stepToSave.setEndDate(chosenEnd);
            stepToSave.setTestRoom(chosenRoom);
            legTestStepDao.updateStepSchedule(stepToSave.getId(), stepToSave.getLegId(), chosenStart, chosenEnd, chosenRoom);
            loadData(stepToSave.getId());
            auditLogService.log(
                    currentUser,
                    "STEP_SCHEDULE_UPDATED",
                    "STEP",
                    stepToSave.getId(),
                    project.getId(),
                    leg.getId(),
                    stepToSave.getId(),
                    "Zaktualizowano harmonogram kroku " + stepToSave.getStepOrder() + ". " + stepToSave.getStepName(),
                    "Start: " + valueOrDash(chosenStart) + " | Koniec: " + valueOrDash(chosenEnd) + " | Lab: " + valueOrDash(chosenRoom)
            );
            onDataChanged.run();
            JOptionPane.showMessageDialog(this, "Krok testowy zostal zaktualizowany.", "Sukces", JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException e) {
            showError("Nie udalo sie zapisac kroku testowego.", e);
        }
    }

    private void saveDutResults() {
        if (selectedStep == null) {
            return;
        }
        if (!canEditCurrentDutResults()) {
            JOptionPane.showMessageDialog(this, "Nie masz uprawnien do edycji wynikow DUT w tym LEGu.", "Brak uprawnien", JOptionPane.WARNING_MESSAGE);
            return;
        }

        stopTableEditing(dutTable);

        try {
            List<DutResultRow> rowsToSave = new ArrayList<>();
            for (int rowIndex = 0; rowIndex < currentDutResults.size(); rowIndex++) {
                DutResultRow row = currentDutResults.get(rowIndex);
                LocalDate executionDate = parseOptionalDateValue(dutTableModel.getValueAt(rowIndex, 3));
                validateExecutionDate(executionDate);
                row.setObservedFunctionalClass((ObservedFunctionalClass) dutTableModel.getValueAt(rowIndex, 1));
                row.setResultStatus((TestStatus) dutTableModel.getValueAt(rowIndex, 2));
                row.setExecutionDate(executionDate);
                row.setComment(trimToNull(dutTableModel.getValueAt(rowIndex, 4)));
                rowsToSave.add(row);
            }

            dutTestResultDao.upsertResults(selectedStep.getId(), rowsToSave);
            loadData(selectedStep.getId());
            auditLogService.log(
                    currentUser,
                    "DUT_RESULTS_UPDATED",
                    "STEP",
                    selectedStep.getId(),
                    project.getId(),
                    leg.getId(),
                    selectedStep.getId(),
                    "Zapisano wyniki DUT dla kroku " + selectedStep.getStepOrder() + ". " + selectedStep.getStepName(),
                    "Liczba DUT: " + rowsToSave.size()
            );
            onDataChanged.run();
            JOptionPane.showMessageDialog(this, "Wyniki DUT zostaly zapisane.", "Sukces", JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException e) {
            showError("Nie udalo sie zapisac wynikow DUT.", e);
        }
    }

    private void assignTechnicianToLeg() {
        if (!permissionService.canAssignLegTechnician(currentUser)) {
            JOptionPane.showMessageDialog(this, "Tylko TE lub Administrator moze przypisywac technika do LEGu.", "Brak uprawnien", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<User> projectTechnicians = userDao.findTechniciansByProjectId(project.getId());
        if (projectTechnicians.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Najpierw przypisz TT do projektu.", "Informacja", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JComboBox<Object> technicianCombo = new JComboBox<>();
        technicianCombo.addItem(NO_TECHNICIAN_LABEL);
        for (User technician : projectTechnicians) {
            technicianCombo.addItem(technician);
        }

        if (leg.getAssignedTt() != null) {
            for (int i = 1; i < technicianCombo.getItemCount(); i++) {
                Object item = technicianCombo.getItemAt(i);
                if (item instanceof User technician && technician.getId() == leg.getAssignedTt().getId()) {
                    technicianCombo.setSelectedIndex(i);
                    break;
                }
            }
        }

        JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
        panel.add(new JLabel("Wybierz technika TT dla LEGu:"));
        panel.add(technicianCombo);
        EmcUiTheme.styleFormTree(panel);

        int result = JOptionPane.showConfirmDialog(this, panel, "Przypisz TT do LEGu", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        Object selected = technicianCombo.getSelectedItem();
        Integer technicianId = selected instanceof User technician ? technician.getId() : null;

        try {
            legDao.assignTechnicianToLeg(leg.getId(), technicianId);
            loadData(selectedStep == null ? null : selectedStep.getId());
            auditLogService.log(
                    currentUser,
                    "LEG_TT_UPDATED",
                    "LEG",
                    leg.getId(),
                    project.getId(),
                    leg.getId(),
                    null,
                    "Zmieniono przypisanie TT dla LEGu " + leg.getLegCode(),
                    selected instanceof User technician
                            ? "Nowy TT: " + technician.getFullName() + " | " + technician.getEmail()
                            : "Usunieto przypisanie TT"
            );
            onDataChanged.run();
            JOptionPane.showMessageDialog(this, "Przypisanie TT zostalo zapisane.", "Sukces", JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException e) {
            showError("Nie udalo sie zapisac przypisania TT.", e);
        }
    }

    private void manageDutAssignments() {
        if (!permissionService.canManageLegDutAssignments(currentUser, project)) {
            JOptionPane.showMessageDialog(this, "Nie masz uprawnien do zarzadzania DUT-ami w tym LEGu.", "Brak uprawnien", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<DutSample> projectSamples = dutSampleDao.findByProjectId(project.getId());
        if (projectSamples.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ten projekt nie ma jeszcze zadnych DUT-ow.", "Informacja", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Set<Integer> assignedIds = new HashSet<>(dutSampleDao.findAssignedSampleIdsByLegId(leg.getId()));
        JList<DutSample> sampleList = new JList<>(projectSamples.toArray(DutSample[]::new));
        sampleList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        int[] selectedIndices = IntStream.range(0, projectSamples.size())
                .filter(index -> assignedIds.contains(projectSamples.get(index).getId()))
                .toArray();
        sampleList.setSelectedIndices(selectedIndices);

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JLabel("Wybierz DUT-y przypisane do tego LEGu:"), BorderLayout.NORTH);
        panel.add(new JScrollPane(sampleList), BorderLayout.CENTER);
        EmcUiTheme.styleFormTree(panel);

        int result = JOptionPane.showConfirmDialog(this, panel, "Przypisz DUT-y do LEGu", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        List<Integer> selectedSampleIds = sampleList.getSelectedValuesList().stream()
                .map(DutSample::getId)
                .toList();

        try {
            dutSampleDao.replaceAssignmentsForLeg(leg.getId(), selectedSampleIds);
            Integer preferredStepId = selectedStep == null ? null : selectedStep.getId();
            loadData(preferredStepId);
            auditLogService.log(
                    currentUser,
                    "LEG_DUT_ASSIGNMENTS_UPDATED",
                    "LEG",
                    leg.getId(),
                    project.getId(),
                    leg.getId(),
                    null,
                    "Zmieniono domyslne DUT-y LEGu " + leg.getLegCode(),
                    "Wybrane probki: " + selectedSampleIds.size()
            );
            onDataChanged.run();
            JOptionPane.showMessageDialog(this, "Przypisanie DUT-ow zostalo zapisane.", "Sukces", JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException e) {
            showError("Nie udalo sie zapisac przypisania DUT-ow.", e);
        }
    }

    private void manageStepSpecificDutAssignments() {
        if (selectedStep == null) {
            JOptionPane.showMessageDialog(this, "Najpierw wybierz krok testowy.", "Informacja", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (!permissionService.canManageLegDutAssignments(currentUser, project)) {
            JOptionPane.showMessageDialog(this, "Nie masz uprawnien do zmiany DUT-ow dla tego testu.", "Brak uprawnien", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<DutSample> projectSamples = dutSampleDao.findByProjectId(project.getId());
        if (projectSamples.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ten projekt nie ma jeszcze zadnych DUT-ow.", "Informacja", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Set<Integer> assignedIds = dutSampleDao.hasCustomAssignmentsForStep(selectedStep.getId())
                ? new HashSet<>(dutSampleDao.findAssignedSampleIdsByStepId(selectedStep.getId()))
                : new HashSet<>(dutSampleDao.findAssignedSampleIdsByLegId(leg.getId()));

        JList<DutSample> sampleList = new JList<>(projectSamples.toArray(DutSample[]::new));
        sampleList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        int[] selectedIndices = IntStream.range(0, projectSamples.size())
                .filter(index -> assignedIds.contains(projectSamples.get(index).getId()))
                .toArray();
        sampleList.setSelectedIndices(selectedIndices);

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JLabel("Wyjatek DUT dla testu " + selectedStep.getStepOrder() + ". " + selectedStep.getStepName()), BorderLayout.NORTH);
        panel.add(new JScrollPane(sampleList), BorderLayout.CENTER);
        EmcUiTheme.styleFormTree(panel);

        int result = JOptionPane.showConfirmDialog(this, panel, "DUT-y tylko dla wybranego testu", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        List<Integer> selectedSampleIds = sampleList.getSelectedValuesList().stream()
                .map(DutSample::getId)
                .toList();

        try {
            dutSampleDao.replaceAssignmentsForStep(selectedStep.getId(), selectedSampleIds);
            loadData(selectedStep.getId());
            auditLogService.log(
                    currentUser,
                    "STEP_DUT_ASSIGNMENTS_UPDATED",
                    "STEP",
                    selectedStep.getId(),
                    project.getId(),
                    leg.getId(),
                    selectedStep.getId(),
                    "Ustawiono wyjatek DUT dla kroku " + selectedStep.getStepOrder() + ". " + selectedStep.getStepName(),
                    "Wybrane probki: " + selectedSampleIds.size()
            );
            onDataChanged.run();
            JOptionPane.showMessageDialog(this, "Wyjatek DUT dla testu zostal zapisany.", "Sukces", JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException e) {
            showError("Nie udalo sie zapisac wyjatku DUT.", e);
        }
    }

    private void resetStepDutsToLegDefaults() {
        if (selectedStep == null) {
            JOptionPane.showMessageDialog(this, "Najpierw wybierz krok testowy.", "Informacja", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (!permissionService.canManageLegDutAssignments(currentUser, project)) {
            JOptionPane.showMessageDialog(this, "Nie masz uprawnien do tej operacji.", "Brak uprawnien", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!dutSampleDao.hasCustomAssignmentsForStep(selectedStep.getId())) {
            JOptionPane.showMessageDialog(this, "Ten test juz korzysta z domyslnych DUT-ow LEGu.", "Informacja", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int result = JOptionPane.showConfirmDialog(
                this,
                "Przywrocic domyslne DUT-y z calego LEGu dla testu " + selectedStep.getStepOrder() + ". " + selectedStep.getStepName() + "?",
                "Przywracanie DUT-ow",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (result != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            dutSampleDao.clearAssignmentsForStep(selectedStep.getId());
            loadData(selectedStep.getId());
            auditLogService.log(
                    currentUser,
                    "STEP_DUT_ASSIGNMENTS_RESET",
                    "STEP",
                    selectedStep.getId(),
                    project.getId(),
                    leg.getId(),
                    selectedStep.getId(),
                    "Przywrocono domyslne DUT-y LEGu dla testu",
                    selectedStep.getStepOrder() + ". " + selectedStep.getStepName()
            );
            onDataChanged.run();
            JOptionPane.showMessageDialog(this, "Test ponownie korzysta z DUT-ow przypisanych do LEGu.", "Sukces", JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException e) {
            showError("Nie udalo sie przywrocic DUT-ow LEGu.", e);
        }
    }

    private void addEquipmentToCatalog() {
        if (!permissionService.canManageEquipmentCatalog(currentUser)) {
            JOptionPane.showMessageDialog(this, "Tylko TE i Administrator moga zarzadzac katalogiem sprzetu.", "Brak uprawnien", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JTextField codeField = new JTextField();
        JTextField nameField = new JTextField();
        JTextField categoryField = new JTextField();
        JTextField manufacturerField = new JTextField();
        JTextField modelField = new JTextField();
        JTextField serialField = new JTextField();
        JTextField calibrationField = new JTextField();
        JTextField locationField = new JTextField();
        JTextArea notesArea = new JTextArea(4, 24);

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 0;
        addEditorRow(formPanel, gbc, "Kod sprzetu:", codeField);
        addEditorRow(formPanel, gbc, "Nazwa:", nameField);
        addEditorRow(formPanel, gbc, "Kategoria:", categoryField);
        addEditorRow(formPanel, gbc, "Producent:", manufacturerField);
        addEditorRow(formPanel, gbc, "Model:", modelField);
        addEditorRow(formPanel, gbc, "Serial:", serialField);
        addEditorRow(formPanel, gbc, "Kalibracja (YYYY-MM-DD):", calibrationField);
        addEditorRow(formPanel, gbc, "Lokalizacja:", locationField);

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
                "Dodaj sprzet do bazy",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String code = codeField.getText().trim();
        String name = nameField.getText().trim();
        String category = categoryField.getText().trim();
        if (code.isBlank() || name.isBlank() || category.isBlank()) {
            JOptionPane.showMessageDialog(this, "Kod, nazwa i kategoria sprzetu sa wymagane.", "Walidacja", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            MeasurementEquipment equipment = new MeasurementEquipment();
            equipment.setEquipmentCode(code);
            equipment.setEquipmentName(name);
            equipment.setCategory(category);
            equipment.setManufacturer(trimToNull(manufacturerField.getText()));
            equipment.setModel(trimToNull(modelField.getText()));
            equipment.setSerialNumber(trimToNull(serialField.getText()));
            equipment.setCalibrationValidUntil(parseOptionalDate(calibrationField.getText()));
            equipment.setLocation(trimToNull(locationField.getText()));
            equipment.setNotes(trimToNull(notesArea.getText()));

            equipmentDao.insertEquipment(equipment);
            loadData(selectedStep == null ? null : selectedStep.getId());
            onDataChanged.run();
            JOptionPane.showMessageDialog(this, "Sprzet zostal dodany do bazy.", "Sukces", JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException e) {
            showError("Nie udalo sie dodac sprzetu.", e);
        }
    }

    private void assignExistingEquipment() {
        if (!permissionService.canManageEquipment(currentUser, project)) {
            JOptionPane.showMessageDialog(this, "Nie masz uprawnien do zarzadzania sprzetem.", "Brak uprawnien", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!ensureEquipmentAssignmentContext()) {
            return;
        }

        LocalDate reservationStart = getEffectiveStepStartDate();
        LocalDate reservationEnd = getEffectiveStepEndDate();

        JTextField identifierField = new JTextField();
        JTextField searchField = new JTextField();
        DefaultListModel<MeasurementEquipment> listModel = new DefaultListModel<>();
        JList<MeasurementEquipment> equipmentList = new JList<>(listModel);
        equipmentList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        Runnable refreshAvailableEquipment = () -> {
            String searchUpper = searchField.getText() == null ? "" : searchField.getText().trim().toUpperCase();
            listModel.clear();
            for (MeasurementEquipment equipment : equipmentDao.findAll()) {
                if (!searchUpper.isBlank()
                        && !equipment.getEquipmentCode().toUpperCase().contains(searchUpper)
                        && !equipment.getEquipmentName().toUpperCase().contains(searchUpper)
                        && (equipment.getCategory() == null || !equipment.getCategory().toUpperCase().contains(searchUpper))
                        && (equipment.getManufacturer() == null || !equipment.getManufacturer().toUpperCase().contains(searchUpper))
                        && (equipment.getModel() == null || !equipment.getModel().toUpperCase().contains(searchUpper))) {
                    continue;
                }
                listModel.addElement(equipment);
            }
        };
        refreshAvailableEquipment.run();

        DocumentListener searchListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshAvailableEquipment.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshAvailableEquipment.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refreshAvailableEquipment.run();
            }
        };
        searchField.getDocument().addDocumentListener(searchListener);

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        JPanel form = new JPanel(new GridLayout(0, 1, 6, 6));
        form.add(new JLabel("Przypisanie do testu: " + selectedStep.getStepOrder() + ". " + selectedStep.getStepName()));
        form.add(new JLabel("Okno rezerwacji: " + reservationStart + " - " + reservationEnd));
        form.add(new JLabel("ID lub kod sprzetu:"));
        form.add(identifierField);
        form.add(new JLabel("Wyszukaj na liscie:"));
        form.add(searchField);
        panel.add(form, BorderLayout.NORTH);
        panel.add(new JScrollPane(equipmentList), BorderLayout.CENTER);
        EmcUiTheme.styleFormTree(panel);

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Przypisz sprzet do wybranego testu",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        MeasurementEquipment selectedEquipment;
        String identifier = identifierField.getText().trim();
        if (!identifier.isBlank()) {
            selectedEquipment = equipmentDao.findByIdentifier(identifier);
            if (selectedEquipment == null) {
                JOptionPane.showMessageDialog(this, "Nie znaleziono dostepnego sprzetu o podanym ID lub kodzie.", "Informacja", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
        } else {
            selectedEquipment = equipmentList.getSelectedValue();
        }

        if (selectedEquipment == null) {
            JOptionPane.showMessageDialog(this, "Wybierz sprzet z listy albo podaj jego ID/kod.", "Walidacja", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<String> conflicts = equipmentDao.findConflictsForStep(selectedStep.getId(), selectedEquipment.getId(), reservationStart, reservationEnd);
        if (!conflicts.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Wybrany sprzet jest juz zarezerwowany:\n- " + String.join("\n- ", conflicts),
                    "Konflikt rezerwacji",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        try {
            equipmentDao.assignToStep(selectedStep.getId(), selectedEquipment.getId(), reservationStart, reservationEnd);
            loadData(selectedStep == null ? null : selectedStep.getId());
            auditLogService.log(
                    currentUser,
                    "STEP_EQUIPMENT_ASSIGNED",
                    "STEP",
                    selectedStep.getId(),
                    project.getId(),
                    leg.getId(),
                    selectedStep.getId(),
                    "Przypisano sprzet do testu " + selectedStep.getStepOrder() + ". " + selectedStep.getStepName(),
                    selectedEquipment.getEquipmentCode() + " | " + selectedEquipment.getEquipmentName()
            );
            onDataChanged.run();
            JOptionPane.showMessageDialog(this, "Sprzet zostal przypisany do wybranego testu.", "Sukces", JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException e) {
            showError("Nie udalo sie przypisac sprzetu.", e);
        }
    }

    private void openEquipmentCatalogDialog() {
        if (!permissionService.canManageEquipmentCatalog(currentUser)) {
            JOptionPane.showMessageDialog(this, "Tylko TE i Administrator moga zarzadzac katalogiem sprzetu.", "Brak uprawnien", JOptionPane.WARNING_MESSAGE);
            return;
        }

        new EquipmentCatalogDialog(
                this,
                currentUser,
                project.getId(),
                leg.getId(),
                () -> {
                    loadData(selectedStep == null ? null : selectedStep.getId());
                    onDataChanged.run();
                }
        ).setVisible(true);
    }

    private void removeEquipmentFromCurrentTest() {
        if (!permissionService.canManageEquipment(currentUser, project)) {
            JOptionPane.showMessageDialog(this, "Nie masz uprawnien do usuwania sprzetu z testu.", "Brak uprawnien", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (selectedStep == null) {
            JOptionPane.showMessageDialog(this, "Najpierw wybierz krok testowy.", "Informacja", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        MeasurementEquipment selectedEquipment = getSelectedEquipmentFromTable();
        if (selectedEquipment == null) {
            JOptionPane.showMessageDialog(this, "Wybierz sprzet z tabeli przypisanej do wybranego testu.", "Informacja", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int result = JOptionPane.showConfirmDialog(
                this,
                "Usunac sprzet " + selectedEquipment.getEquipmentCode() + " z testu " + selectedStep.getStepOrder() + ". " + selectedStep.getStepName() + "?",
                "Usuwanie przypisania",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (result != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            equipmentDao.removeFromStep(selectedStep.getId(), selectedEquipment.getId());
            loadData(selectedStep.getId());
            auditLogService.log(
                    currentUser,
                    "STEP_EQUIPMENT_REMOVED",
                    "STEP",
                    selectedStep.getId(),
                    project.getId(),
                    leg.getId(),
                    selectedStep.getId(),
                    "Usunieto sprzet z testu " + selectedStep.getStepOrder() + ". " + selectedStep.getStepName(),
                    selectedEquipment.getEquipmentCode() + " | " + selectedEquipment.getEquipmentName()
            );
            onDataChanged.run();
            JOptionPane.showMessageDialog(this, "Sprzet zostal usuniety z wybranego testu.", "Sukces", JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException e) {
            showError("Nie udalo sie usunac sprzetu z testu.", e);
        }
    }

    private void editSelectedEquipmentInCatalog() {
        if (!permissionService.canManageEquipmentCatalog(currentUser)) {
            JOptionPane.showMessageDialog(this, "Tylko TE i Administrator moga edytowac katalog sprzetu.", "Brak uprawnien", JOptionPane.WARNING_MESSAGE);
            return;
        }

        MeasurementEquipment selectedEquipment = getSelectedEquipmentFromTable();
        if (selectedEquipment == null) {
            JOptionPane.showMessageDialog(this, "Wybierz sprzet z tabeli LEGu.", "Informacja", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (!showEquipmentCatalogEditor(selectedEquipment, "Edytuj sprzet w bazie")) {
            return;
        }

        try {
            equipmentDao.updateEquipment(selectedEquipment);
            loadData(selectedStep == null ? null : selectedStep.getId());
            onDataChanged.run();
            JOptionPane.showMessageDialog(this, "Dane sprzetu zostaly zaktualizowane.", "Sukces", JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException e) {
            showError("Nie udalo sie zapisac zmian sprzetu.", e);
        }
    }

    private void deleteSelectedEquipmentFromCatalog() {
        if (!permissionService.canManageEquipmentCatalog(currentUser)) {
            JOptionPane.showMessageDialog(this, "Tylko TE i Administrator moga usuwac sprzet z bazy.", "Brak uprawnien", JOptionPane.WARNING_MESSAGE);
            return;
        }

        MeasurementEquipment selectedEquipment = getSelectedEquipmentFromTable();
        if (selectedEquipment == null) {
            JOptionPane.showMessageDialog(this, "Wybierz sprzet z tabeli LEGu.", "Informacja", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int result = JOptionPane.showConfirmDialog(
                this,
                "Usuniecie sprzetu z bazy usunie go rowniez ze wszystkich testow. Kontynuowac?\n\n"
                        + selectedEquipment.getEquipmentCode() + " | " + selectedEquipment.getEquipmentName(),
                "Usuwanie sprzetu z bazy",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (result != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            equipmentDao.deleteEquipment(selectedEquipment.getId());
            loadData(selectedStep == null ? null : selectedStep.getId());
            onDataChanged.run();
            JOptionPane.showMessageDialog(this, "Sprzet zostal usuniety z bazy.", "Sukces", JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException e) {
            showError("Nie udalo sie usunac sprzetu z bazy.", e);
        }
    }

    private boolean ensureEquipmentAssignmentContext() {
        if (selectedStep == null) {
            JOptionPane.showMessageDialog(this, "Najpierw wybierz krok testowy, do ktorego chcesz przypisac sprzet.", "Informacja", JOptionPane.INFORMATION_MESSAGE);
            return false;
        }

        LocalDate reservationStart = getEffectiveStepStartDate();
        LocalDate reservationEnd = getEffectiveStepEndDate();
        if (reservationStart == null || reservationEnd == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Aby przypisac sprzet, ustaw daty Start i Koniec dla kroku testowego lub dla calego LEGu.",
                    "Brak terminu rezerwacji",
                    JOptionPane.WARNING_MESSAGE
            );
            return false;
        }
        if (reservationEnd.isBefore(reservationStart)) {
            JOptionPane.showMessageDialog(this, "Data konca rezerwacji nie moze byc wczesniejsza niz data startu.", "Walidacja", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    private void updateEquipmentTargetLabel() {
        if (selectedStep == null) {
            selectedEquipmentStepLabel.setText("Wybierz krok testowy, aby przypisywac sprzet.");
            return;
        }

        LocalDate reservationStart = getEffectiveStepStartDate();
        LocalDate reservationEnd = getEffectiveStepEndDate();
        String reservationLabel = (reservationStart == null || reservationEnd == null)
                ? "ustaw daty kroku lub LEGu, aby wlaczyc rezerwacje"
                : "rezerwacja: " + reservationStart + " - " + reservationEnd;

        selectedEquipmentStepLabel.setText(
                "Przypisania do testu: " + selectedStep.getStepOrder() + ". " + selectedStep.getStepName() + " | " + reservationLabel
        );
    }

    private void updateClimateTargetLabel() {
        if (selectedStep == null) {
            selectedClimateStepLabel.setText("Wybierz krok testowy, aby pobrac warunki klimatyczne.");
            return;
        }

        LocalDate climateStart = getEffectiveClimateStartDate();
        LocalDate climateEnd = getEffectiveClimateEndDate();
        String roomLabel = trimToNull(selectedStep.getTestRoom()) == null ? "pomieszczenie nieustawione" : selectedStep.getTestRoom();
        String rangeLabel = climateStart == null || climateEnd == null
                ? "zakres dat nieustalony"
                : climateStart + " - " + climateEnd;

        selectedClimateStepLabel.setText(
                "Krok " + selectedStep.getStepOrder() + ". " + selectedStep.getStepName() + " | " + roomLabel + " | " + rangeLabel
        );
    }

    private LocalDate getEffectiveStepStartDate() {
        if (selectedStep == null) {
            return null;
        }
        return selectedStep.getStartDate() != null ? selectedStep.getStartDate() : leg.getStartDate();
    }

    private LocalDate getEffectiveStepEndDate() {
        if (selectedStep == null) {
            return null;
        }
        return selectedStep.getEndDate() != null ? selectedStep.getEndDate() : leg.getEndDate();
    }

    private LocalDate getEffectiveClimateStartDate() {
        if (selectedStep == null) {
            return null;
        }
        LocalDate start = selectedStep.getStartDate() != null ? selectedStep.getStartDate() : leg.getStartDate();
        LocalDate end = selectedStep.getEndDate() != null ? selectedStep.getEndDate() : leg.getEndDate();
        if (start == null) {
            return end;
        }
        return start;
    }

    private LocalDate getEffectiveClimateEndDate() {
        if (selectedStep == null) {
            return null;
        }
        LocalDate start = selectedStep.getStartDate() != null ? selectedStep.getStartDate() : leg.getStartDate();
        LocalDate end = selectedStep.getEndDate() != null ? selectedStep.getEndDate() : leg.getEndDate();
        if (end == null) {
            return start == null ? null : LocalDate.now();
        }
        return end;
    }

    private void importClimateFilesFromDesktop() {
        if (!permissionService.canManageClimateFiles(currentUser)) {
            JOptionPane.showMessageDialog(this, "Tylko TE i Administrator moga importowac pliki klimatyczne do bazy.", "Brak uprawnien", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int imported = climateDataImportService.importDesktopClimateFiles();
            auditLogService.log(
                    currentUser,
                    "CLIMATE_FILES_IMPORTED",
                    "CLIMATE",
                    null,
                    project.getId(),
                    leg.getId(),
                    selectedStep == null ? null : selectedStep.getId(),
                    "Zaimportowano pliki warunkow klimatycznych",
                    "Liczba plikow: " + imported
            );
            JOptionPane.showMessageDialog(
                    this,
                    imported > 0
                            ? "Zaimportowano lub odswiezono " + imported + " plikow klimatycznych."
                            : "Nie znaleziono plikow klimatycznych na pulpicie lub w OneDrive.",
                    "Import warunkow klimatycznych",
                    JOptionPane.INFORMATION_MESSAGE
            );
        } catch (RuntimeException e) {
            showError("Nie udalo sie zaimportowac plikow klimatycznych.", e);
        }
    }

    private void loadClimateConditionsForSelectedStep() {
        if (!permissionService.canManageClimateFiles(currentUser)) {
            JOptionPane.showMessageDialog(this, "Tylko TE i Administrator moga pobierac warunki klimatyczne.", "Brak uprawnien", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (selectedStep == null) {
            JOptionPane.showMessageDialog(this, "Najpierw wybierz krok testowy.", "Informacja", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String roomCode = trimToNull(selectedStep.getTestRoom());
        String sensorCode = resolveClimateSensorCodeForSelectedStep();
        LocalDate climateStart = getEffectiveClimateStartDate();
        LocalDate climateEnd = getEffectiveClimateEndDate();

        if (roomCode == null) {
            JOptionPane.showMessageDialog(this, "Ustaw pomieszczenie dla kroku testowego, aby pobrac warunki klimatyczne.", "Brak pomieszczenia", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (climateStart == null || climateEnd == null) {
            JOptionPane.showMessageDialog(this, "Ustaw daty kroku lub LEGu, aby pobrac warunki klimatyczne.", "Brak zakresu dat", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (climateEnd.isBefore(climateStart)) {
            JOptionPane.showMessageDialog(this, "Zakres dat dla warunkow klimatycznych jest niepoprawny.", "Walidacja", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            ClimateDataset dataset = climateLogDao.findMeasurements(roomCode, sensorCode, climateStart, climateEnd);
            currentClimateMeasurements.clear();
            currentClimateMeasurements.addAll(dataset.getMeasurements());
            refreshClimateTable(dataset, roomCode, climateStart, climateEnd);
        } catch (RuntimeException e) {
            showError("Nie udalo sie pobrac warunkow klimatycznych.", e);
        }
    }

    private void refreshClimateTable(ClimateDataset dataset, String roomCode, LocalDate startDate, LocalDate endDate) {
        DecimalFormat format = new DecimalFormat("0.0");
        currentClimateRoomCode = roomCode;
        currentClimateSourceLabel = dataset.getSourceDescription();
        currentClimateStartDate = startDate;
        currentClimateEndDate = endDate;
        currentClimateMeasurements.sort((left, right) -> {
            int dateCompare = left.getMeasurementDate().compareTo(right.getMeasurementDate());
            if (dateCompare != 0) {
                return dateCompare;
            }
            return left.getMeasurementTime().compareTo(right.getMeasurementTime());
        });

        climateTableModel.setRowCount(0);
        double minTemperature = Double.MAX_VALUE;
        double maxTemperature = Double.MIN_VALUE;
        double sumTemperature = 0;
        double minHumidity = Double.MAX_VALUE;
        double maxHumidity = Double.MIN_VALUE;
        double sumHumidity = 0;

        for (ClimateMeasurement measurement : currentClimateMeasurements) {
            climateTableModel.addRow(new Object[]{
                    measurement.getMeasurementDate(),
                    measurement.getMeasurementTime(),
                    format.format(measurement.getTemperature()),
                    format.format(measurement.getHumidity()),
                    measurement.getSourceFilename()
            });
            minTemperature = Math.min(minTemperature, measurement.getTemperature());
            maxTemperature = Math.max(maxTemperature, measurement.getTemperature());
            sumTemperature += measurement.getTemperature();
            minHumidity = Math.min(minHumidity, measurement.getHumidity());
            maxHumidity = Math.max(maxHumidity, measurement.getHumidity());
            sumHumidity += measurement.getHumidity();
        }

        String sourceLabel = dataset.getSourceDescription() == null ? roomCode : dataset.getSourceDescription();
        climateFilesLabel.setText(dataset.getSourceFilenames().isEmpty()
                ? "Pliki: brak dopasowanych plikow w bazie dla " + sourceLabel
                : "Pliki: " + String.join(", ", dataset.getSourceFilenames()));
        climateRangeLabel.setText("Zakres: " + sourceLabel + " | " + startDate + " - " + endDate + " | probki: " + currentClimateMeasurements.size());

        if (currentClimateMeasurements.isEmpty()) {
            climateTempLabel.setText("Temperatura: brak probek 06:00-22:00, pon-pt");
            climateHumidityLabel.setText("Wilgotnosc: brak probek 06:00-22:00, pon-pt");
            updatePermissionState();
            return;
        }

        double avgTemperature = sumTemperature / currentClimateMeasurements.size();
        double avgHumidity = sumHumidity / currentClimateMeasurements.size();
        climateTempLabel.setText("Temperatura: avg " + format.format(avgTemperature) + " | min " + format.format(minTemperature) + " | max " + format.format(maxTemperature));
        climateHumidityLabel.setText("Wilgotnosc: avg " + format.format(avgHumidity) + " | min " + format.format(minHumidity) + " | max " + format.format(maxHumidity));
        updatePermissionState();
    }

    private void openClimateChartPreview() {
        if (currentClimateMeasurements.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Najpierw pobierz warunki klimatyczne dla wybranego kroku.", "Brak danych", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        new ClimateChartDialog(this, currentClimateSourceLabel == null ? currentClimateRoomCode : currentClimateSourceLabel, currentClimateStartDate, currentClimateEndDate, currentClimateMeasurements)
                .setVisible(true);
    }

    private void loadSetupMediaForSelectedStep() {
        currentSetupMedia.clear();
        setupMediaTableModel.setRowCount(0);
        if (selectedStep == null) {
            selectedSetupStepLabel.setText("Wybierz krok testowy, aby zarzadzac zdjeciami setupu.");
            return;
        }

        currentSetupMedia.addAll(stepMediaDao.findByStepIdAndKind(selectedStep.getId(), StepMediaKind.SETUP));
        Map<String, StepMedia> bySlot = new HashMap<>();
        for (StepMedia media : currentSetupMedia) {
            bySlot.put(media.getSlotCode(), media);
        }
        for (StepSetupSlot slot : StepSetupSlot.values()) {
            StepMedia media = bySlot.get(slot.getSlotCode());
            setupMediaTableModel.addRow(new Object[]{slot.getDisplayName(), media == null ? "---" : media.getFileName()});
        }
        selectedSetupStepLabel.setText("Wybrany krok: " + selectedStep.getStepOrder() + ". " + selectedStep.getStepName());
    }

    private void loadVerificationMediaForSelectedStep() {
        currentVerificationMedia.clear();
        verificationTableModel.setRowCount(0);
        if (selectedStep == null) {
            selectedVerificationStepLabel.setText("Wybierz krok testowy, aby zarzadzac weryfikacjami.");
            return;
        }
        currentVerificationMedia.addAll(stepMediaDao.findByStepIdAndKind(selectedStep.getId(), StepMediaKind.VERIFICATION));
        for (StepMedia media : currentVerificationMedia) {
            verificationTableModel.addRow(new Object[]{media.getDisplayName(), media.getFileName()});
        }
        selectedVerificationStepLabel.setText("Wybrany krok: " + selectedStep.getStepOrder() + ". " + selectedStep.getStepName());
    }

    private void uploadSetupMedia() {
        if (selectedStep == null) {
            JOptionPane.showMessageDialog(this, "Najpierw wybierz krok testowy.", "Informacja", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int row = setupMediaTable.getSelectedRow();
        if (row < 0 || row >= StepSetupSlot.values().length) {
            JOptionPane.showMessageDialog(this, "Zaznacz pozycje setupu, ktora chcesz uzupelnic.", "Informacja", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser chooser = createImageChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        StepSetupSlot slot = StepSetupSlot.values()[row];
        try {
            Path path = chooser.getSelectedFile().toPath();
            ImageStorageUtil.StoredImage storedImage = ImageStorageUtil.prepareForDatabase(path);
            stepMediaDao.upsertMedia(
                    selectedStep.getId(),
                    StepMediaKind.SETUP,
                    slot.getSlotCode(),
                    slot.getDisplayName(),
                    slot.getSortOrder(),
                    storedImage.fileName(),
                    storedImage.fileData()
            );
            loadSetupMediaForSelectedStep();
        } catch (Exception exception) {
            showError("Nie udalo sie zapisac zdjecia setupu.", exception);
        }
    }

    private void openSelectedSetupMedia() {
        StepMedia media = getSelectedSetupMedia();
        if (media == null) {
            JOptionPane.showMessageDialog(this, "Dla tej pozycji nie ma jeszcze pliku.", "Informacja", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        DesktopActions.openStoredDocument(this, media.getFileName(), media.getFileData());
    }

    private void removeSelectedSetupMedia() {
        StepMedia media = getSelectedSetupMedia();
        if (media == null) {
            return;
        }
        stepMediaDao.deleteMedia(media.getId());
        loadSetupMediaForSelectedStep();
    }

    private void uploadVerificationMedia() {
        if (selectedStep == null) {
            JOptionPane.showMessageDialog(this, "Najpierw wybierz krok testowy.", "Informacja", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser chooser = createImageChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        int nextOrder = currentVerificationMedia.stream()
                .mapToInt(StepMedia::getSortOrder)
                .max()
                .orElse(0) + 1;
        String displayName = "Verification waveform " + nextOrder;
        String slotCode = String.format("VERIFICATION_%03d", nextOrder);
        try {
            Path path = chooser.getSelectedFile().toPath();
            ImageStorageUtil.StoredImage storedImage = ImageStorageUtil.prepareForDatabase(path);
            stepMediaDao.upsertMedia(
                    selectedStep.getId(),
                    StepMediaKind.VERIFICATION,
                    slotCode,
                    displayName,
                    nextOrder,
                    storedImage.fileName(),
                    storedImage.fileData()
            );
            loadVerificationMediaForSelectedStep();
        } catch (Exception exception) {
            showError("Nie udalo sie zapisac pliku weryfikacji.", exception);
        }
    }

    private void openSelectedVerificationMedia() {
        StepMedia media = getSelectedVerificationMedia();
        if (media == null) {
            JOptionPane.showMessageDialog(this, "Zaznacz plik weryfikacji.", "Informacja", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        DesktopActions.openStoredDocument(this, media.getFileName(), media.getFileData());
    }

    private void removeSelectedVerificationMedia() {
        StepMedia media = getSelectedVerificationMedia();
        if (media == null) {
            return;
        }
        stepMediaDao.deleteMedia(media.getId());
        loadVerificationMediaForSelectedStep();
    }

    private String resolveClimateSensorCodeForSelectedStep() {
        if (selectedStep == null || currentEquipment.isEmpty()) {
            return null;
        }
        return currentEquipment.stream()
                .map(MeasurementEquipment::getClimateSensorCode)
                .map(this::trimToNull)
                .filter(code -> code != null)
                .findFirst()
                .orElse(null);
    }

    private MeasurementEquipment getSelectedEquipmentFromTable() {
        int selectedRow = equipmentTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= currentEquipment.size()) {
            return null;
        }
        return currentEquipment.get(selectedRow);
    }

    private StepMedia getSelectedSetupMedia() {
        if (selectedStep == null) {
            return null;
        }
        int row = setupMediaTable.getSelectedRow();
        if (row < 0 || row >= StepSetupSlot.values().length) {
            return null;
        }
        String slotCode = StepSetupSlot.values()[row].getSlotCode();
        return currentSetupMedia.stream()
                .filter(media -> slotCode.equals(media.getSlotCode()))
                .findFirst()
                .orElse(null);
    }

    private StepMedia getSelectedVerificationMedia() {
        int row = verificationTable.getSelectedRow();
        if (row < 0 || row >= currentVerificationMedia.size()) {
            return null;
        }
        return currentVerificationMedia.get(row);
    }

    private boolean showEquipmentCatalogEditor(MeasurementEquipment equipment, String title) {
        JTextField codeField = new JTextField(equipment.getEquipmentCode());
        JTextField nameField = new JTextField(equipment.getEquipmentName());
        JTextField categoryField = new JTextField(equipment.getCategory());
        JTextField manufacturerField = new JTextField(valueOrEmpty(equipment.getManufacturer()));
        JTextField modelField = new JTextField(valueOrEmpty(equipment.getModel()));
        JTextField serialField = new JTextField(valueOrEmpty(equipment.getSerialNumber()));
        JTextField calibrationField = new JTextField(equipment.getCalibrationValidUntil() == null ? "" : equipment.getCalibrationValidUntil().toString());
        JTextField locationField = new JTextField(valueOrEmpty(equipment.getLocation()));
        JTextField climateSensorField = new JTextField(valueOrEmpty(equipment.getClimateSensorCode()));
        JTextArea notesArea = new JTextArea(valueOrEmpty(equipment.getNotes()), 4, 24);

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 0;
        addEditorRow(formPanel, gbc, "Kod sprzetu:", codeField);
        addEditorRow(formPanel, gbc, "Nazwa:", nameField);
        addEditorRow(formPanel, gbc, "Kategoria:", categoryField);
        addEditorRow(formPanel, gbc, "Producent:", manufacturerField);
        addEditorRow(formPanel, gbc, "Model:", modelField);
        addEditorRow(formPanel, gbc, "Serial:", serialField);
        addEditorRow(formPanel, gbc, "Kalibracja (YYYY-MM-DD):", calibrationField);
        addEditorRow(formPanel, gbc, "Lokalizacja:", locationField);
        addEditorRow(formPanel, gbc, "Kod czujnika klimatu:", climateSensorField);

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
            JOptionPane.showMessageDialog(this, "Kod, nazwa i kategoria sprzetu sa wymagane.", "Walidacja", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        equipment.setEquipmentCode(code);
        equipment.setEquipmentName(name);
        equipment.setCategory(category);
        equipment.setManufacturer(trimToNull(manufacturerField.getText()));
        equipment.setModel(trimToNull(modelField.getText()));
        equipment.setSerialNumber(trimToNull(serialField.getText()));
        equipment.setCalibrationValidUntil(parseOptionalDate(calibrationField.getText()));
        equipment.setLocation(trimToNull(locationField.getText()));
        equipment.setClimateSensorCode(trimToNull(climateSensorField.getText()));
        equipment.setNotes(trimToNull(notesArea.getText()));
        return true;
    }

    private void addEditorRow(JPanel panel, GridBagConstraints gbc, String label, Component field) {
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(field, gbc);
        gbc.gridy++;
    }

    private void clearEquipmentTable() {
        currentEquipment.clear();
        equipmentTableModel.setRowCount(0);
    }

    private void clearDutTable() {
        currentDutResults.clear();
        dutTableModel.setRowCount(0);
    }

    private void clearClimateTable() {
        currentClimateMeasurements.clear();
        currentClimateRoomCode = null;
        currentClimateSourceLabel = null;
        currentClimateStartDate = null;
        currentClimateEndDate = null;
        climateTableModel.setRowCount(0);
        climateFilesLabel.setText("Pliki: ---");
        climateRangeLabel.setText("Zakres: ---");
        climateTempLabel.setText("Temperatura: ---");
        climateHumidityLabel.setText("Wilgotnosc: ---");
        previewClimateChartButton.setEnabled(false);
    }

    private void clearSetupMediaTable() {
        currentSetupMedia.clear();
        setupMediaTableModel.setRowCount(0);
        selectedSetupStepLabel.setText("Wybierz krok testowy, aby zarzadzac zdjeciami setupu.");
    }

    private void clearVerificationTable() {
        currentVerificationMedia.clear();
        verificationTableModel.setRowCount(0);
        selectedVerificationStepLabel.setText("Wybierz krok testowy, aby zarzadzac weryfikacjami.");
    }

    private JFileChooser createImageChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Pliki graficzne", "png", "jpg", "jpeg", "bmp", "gif"
        ));
        return chooser;
    }

    private void stopTableEditing(JTable table) {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
    }

    private LocalDate parseOptionalDate(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty() || trimmed.equals("---")) {
            return null;
        }
        try {
            return LocalDate.parse(trimmed);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Niepoprawny format daty: " + trimmed + ". Uzyj YYYY-MM-DD.");
        }
    }

    private LocalDate parseOptionalDateValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        return parseOptionalDate(value.toString());
    }

    private void validateExecutionDate(LocalDate executionDate) {
        if (executionDate == null) {
            return;
        }
        if (leg.getStartDate() != null && executionDate.isBefore(leg.getStartDate())) {
            throw new IllegalArgumentException("Data DUT nie moze byc wczesniejsza niz start LEGu: " + leg.getStartDate());
        }
        if (leg.getEndDate() != null && executionDate.isAfter(leg.getEndDate())) {
            throw new IllegalArgumentException("Data DUT nie moze byc pozniejsza niz koniec LEGu: " + leg.getEndDate());
        }
    }

    private String trimToNull(Object value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.toString().trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String valueOrDash(Object value) {
        if (value == null) {
            return "---";
        }
        String text = value.toString().trim();
        return text.isEmpty() ? "---" : text;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private String getDisplayedStepStatus(LegTestStep step) {
        if (step == null || step.getStatus() == null) {
            return "---";
        }
        if (step.getStatus() == TestStatus.NOT_STARTED
                && step.getStartDate() != null
                && step.getEndDate() != null) {
            return PLANNED_STATUS_LABEL;
        }
        return step.getStatus().getDisplayName();
    }

    private void showError(String message, Exception exception) {
        JOptionPane.showMessageDialog(
                this,
                message + "\n\n" + buildErrorDetails(exception),
                "Blad",
                JOptionPane.ERROR_MESSAGE
        );
    }

    private String buildErrorDetails(Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && !message.isBlank()) {
                if (!builder.isEmpty()) {
                    builder.append("\n");
                }
                builder.append(message.trim());
            }
            current = current.getCause();
        }
        return builder.isEmpty() ? "Brak dodatkowych szczegolow bledu." : builder.toString();
    }

    private JPanel createSummaryCard(String title, JLabel valueLabel) {
        JPanel card = EmcUiTheme.createCardPanel(new BorderLayout(0, 4));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(EmcUiTheme.SECTION_FONT.deriveFont(13f));
        titleLabel.setForeground(EmcUiTheme.TEXT_PRIMARY);
        valueLabel.setForeground(new Color(69, 82, 95));
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createMetaCard(String title, JLabel valueLabel) {
        JPanel card = EmcUiTheme.createCardPanel(new BorderLayout(0, 6));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(EmcUiTheme.SECTION_FONT.deriveFont(13f));
        titleLabel.setForeground(EmcUiTheme.TEXT_MUTED);
        valueLabel.setForeground(EmcUiTheme.TEXT_PRIMARY);
        valueLabel.setVerticalAlignment(SwingConstants.TOP);
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private void updateLegStatusChip(TestStatus status) {
        String text = status == null ? "Status: ---" : "Status: " + status.getDisplayName();
        legStatusChipLabel.setText(text);
        legStatusChipLabel.setForeground(Color.WHITE);

        Color background = new Color(142, 154, 170);
        if (status == TestStatus.PASSED) {
            background = EmcUiTheme.SUCCESS;
        } else if (status == TestStatus.ONGOING) {
            background = EmcUiTheme.INFO;
        } else if (status == TestStatus.DATA_IN_ANALYSIS) {
            background = EmcUiTheme.WARNING;
        } else if (status == TestStatus.FAILED) {
            background = EmcUiTheme.DANGER;
        }
        legStatusChipLabel.setBackground(background);
    }

    private String asWrappedHtml(String text) {
        String escaped = text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
        return "<html><div style='width:220px;'>" + escaped + "</div></html>";
    }

    private void applyTheme() {
        EmcUiTheme.styleTable(stepsTable);
        EmcUiTheme.styleTable(dutTable);
        EmcUiTheme.styleTable(equipmentTable);
        EmcUiTheme.styleTable(climateTable);
        EmcUiTheme.styleTable(setupMediaTable);
        EmcUiTheme.styleTable(verificationTable);
        stepsTable.getColumnModel().getColumn(STEP_STATUS_COLUMN).setCellRenderer(EmcUiTheme.createStatusRenderer());
        dutTable.getColumnModel().getColumn(2).setCellRenderer(EmcUiTheme.createStatusRenderer());
        selectedStepLabel.setForeground(EmcUiTheme.TEXT_PRIMARY);
        dutModeLabel.setForeground(EmcUiTheme.TEXT_MUTED);
        selectedEquipmentStepLabel.setForeground(EmcUiTheme.TEXT_PRIMARY);
        equipmentHintLabel.setForeground(EmcUiTheme.TEXT_MUTED);
        selectedClimateStepLabel.setForeground(EmcUiTheme.TEXT_PRIMARY);
        climateFilesLabel.setForeground(EmcUiTheme.TEXT_MUTED);

        EmcUiTheme.stylePrimaryButton(saveStepButton);
        EmcUiTheme.stylePrimaryButton(saveDutButton);
        EmcUiTheme.styleSurfaceButton(assignLegTechnicianButton);
        EmcUiTheme.styleSurfaceButton(assignDutButton);
        EmcUiTheme.styleSurfaceButton(manageStepDutButton);
        EmcUiTheme.styleSurfaceButton(resetStepDutButton);
        EmcUiTheme.styleSurfaceButton(assignExistingEquipmentButton);
        EmcUiTheme.styleSurfaceButton(removeEquipmentFromTestButton);
        EmcUiTheme.styleSurfaceButton(addEquipmentButton);
        EmcUiTheme.styleSurfaceButton(importClimateFilesButton);
        EmcUiTheme.stylePrimaryButton(loadClimateButton);
        EmcUiTheme.styleSurfaceButton(previewClimateChartButton);
        EmcUiTheme.stylePrimaryButton(uploadSetupMediaButton);
        EmcUiTheme.styleSurfaceButton(openSetupMediaButton);
        EmcUiTheme.styleSurfaceButton(removeSetupMediaButton);
        EmcUiTheme.stylePrimaryButton(uploadVerificationButton);
        EmcUiTheme.styleSurfaceButton(openVerificationButton);
        EmcUiTheme.styleSurfaceButton(removeVerificationButton);
        EmcUiTheme.styleSurfaceButton(openIsoButton);
        EmcUiTheme.styleSurfaceButton(openClientButton);
        EmcUiTheme.styleSurfaceButton(openTestPlanButton);
        EmcUiTheme.styleSurfaceButton(pcaButton);
        EmcUiTheme.styleSurfaceButton(mailButton);
    }
}
