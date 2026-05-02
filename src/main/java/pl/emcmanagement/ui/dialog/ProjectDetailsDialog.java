package pl.emcmanagement.ui.dialog;

import pl.emcmanagement.dao.DutSampleDao;
import pl.emcmanagement.dao.LegDao;
import pl.emcmanagement.dao.LegTestStepDao;
import pl.emcmanagement.dao.ProjectDao;
import pl.emcmanagement.dao.UserDao;
import pl.emcmanagement.dao.DutMediaDao;
import pl.emcmanagement.enums.DutMediaType;
import pl.emcmanagement.enums.AccreditationStatus;
import pl.emcmanagement.enums.TestStatus;
import pl.emcmanagement.enums.TestType;
import pl.emcmanagement.model.DutMedia;
import pl.emcmanagement.model.DutSample;
import pl.emcmanagement.model.Leg;
import pl.emcmanagement.model.Project;
import pl.emcmanagement.model.User;
import pl.emcmanagement.service.AuditLogService;
import pl.emcmanagement.service.PermissionService;
import pl.emcmanagement.service.ProjectReportPdfService;
import pl.emcmanagement.service.ProjectService;
import pl.emcmanagement.service.WorkflowConsistencyService;
import pl.emcmanagement.ui.style.EmcUiTheme;
import pl.emcmanagement.util.DesktopActions;
import pl.emcmanagement.util.ImageStorageUtil;
import pl.emcmanagement.util.BrandLogoFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class ProjectDetailsDialog extends JDialog {
    private static final String NO_TECHNICIAN_LABEL = "--- brak przypisania ---";
    private static final String DEFAULT_PCA_URL = "https://www.pca.gov.pl/";

    private final User currentUser;
    private final int projectId;
    private final Runnable onDataChanged;

    private final PermissionService permissionService = new PermissionService();
    private final AuditLogService auditLogService = new AuditLogService();
    private final ProjectService projectService = new ProjectService();
    private final ProjectReportPdfService projectReportPdfService = new ProjectReportPdfService();
    private final WorkflowConsistencyService workflowConsistencyService = new WorkflowConsistencyService();
    private final ProjectDao projectDao = new ProjectDao();
    private final LegDao legDao = new LegDao();
    private final LegTestStepDao legTestStepDao = new LegTestStepDao();
    private final DutSampleDao dutSampleDao = new DutSampleDao();
    private final DutMediaDao dutMediaDao = new DutMediaDao();
    private final UserDao userDao = new UserDao();

    private final DefaultTableModel legsTableModel = new DefaultTableModel(
            new Object[]{"Status", "LEG", "Typ", "Akred.", "DUT", "ISO", "Norma klienta", "Test plan", "TT"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable legsTable = new JTable(legsTableModel);

    private final JLabel projectHeaderLabel = new JLabel("Ladowanie projektu...");
    private final JLabel projectStatusLabel = new JLabel("Status projektu");
    private final JLabel brandLogoLabel = new JLabel("Logo", SwingConstants.CENTER);
    private final JTextArea veArea = createSummaryArea(2);
    private final JTextArea teArea = createSummaryArea(2);
    private final JTextArea ttArea = createSummaryArea(4);
    private final JPanel teamCardsPanel = new JPanel(new GridLayout(1, 3, 10, 0));

    private final JButton addLegButton = new JButton("Modyfikuj LEG");
    private final JButton addDutButton = new JButton("Modyfikuj DUT");
    private final JButton auditLogButton = new JButton("Audit log");
    private final JButton reservationCalendarButton = new JButton("Kalendarz");
    private final JButton reportButton = new JButton("Raport PDF");
    private final JButton assignProjectTtButton = new JButton("Modyfikuj TT w projekcie");
    private final JButton refreshButton = new JButton("Odswiez dane");

    private final List<Leg> currentLegs = new ArrayList<>();
    private Project currentProject;

    public ProjectDetailsDialog(Window owner, User currentUser, int projectId, Runnable onDataChanged) {
        super(owner, "Szczegoly projektu", ModalityType.APPLICATION_MODAL);
        this.currentUser = currentUser;
        this.projectId = projectId;
        this.onDataChanged = onDataChanged == null ? () -> { } : onDataChanged;

        setSize(1260, 780);
        setMinimumSize(new Dimension(1060, 700));
        setLocationRelativeTo(owner);

        JPanel root = EmcUiTheme.createPatternPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(14, 14, 14, 14));
        setContentPane(root);

        root.add(buildSummaryPanel(), BorderLayout.NORTH);
        root.add(buildLegsPanel(), BorderLayout.CENTER);
        root.add(buildBottomInfoPanel(), BorderLayout.SOUTH);

        applyTheme();
        configureTable();
        configureActions();
        reloadProject();
    }

    private Component buildSummaryPanel() {
        JPanel panel = EmcUiTheme.createCardPanel(new BorderLayout(0, 12));

        JPanel summary = new JPanel(new BorderLayout(0, 12));
        summary.setOpaque(false);

        JPanel topRow = new JPanel(new BorderLayout(14, 0));
        topRow.setOpaque(false);

        JPanel logoPanel = new JPanel(new GridBagLayout());
        logoPanel.setOpaque(false);
        brandLogoLabel.setPreferredSize(new Dimension(124, 84));
        brandLogoLabel.setMinimumSize(new Dimension(124, 84));
        brandLogoLabel.setOpaque(true);
        brandLogoLabel.setBackground(Color.WHITE);
        brandLogoLabel.setBorder(EmcUiTheme.createInnerBorder());
        logoPanel.add(brandLogoLabel);
        topRow.add(logoPanel, BorderLayout.WEST);

        JPanel infoPanel = new JPanel(new BorderLayout(0, 10));
        infoPanel.setOpaque(false);

        projectHeaderLabel.setFont(projectHeaderLabel.getFont().deriveFont(Font.BOLD, 24f));
        projectStatusLabel.setFont(projectStatusLabel.getFont().deriveFont(Font.BOLD, 13f));
        projectStatusLabel.setOpaque(true);
        projectStatusLabel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.add(projectHeaderLabel);
        heading.add(Box.createVerticalStrut(8));
        heading.add(projectStatusLabel);
        infoPanel.add(heading, BorderLayout.NORTH);
        topRow.add(infoPanel, BorderLayout.CENTER);
        topRow.add(EmcUiTheme.createPkBadgePanel(460), BorderLayout.EAST);

        summary.add(topRow, BorderLayout.NORTH);
        summary.add(buildContactsPanel(), BorderLayout.CENTER);

        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);

        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftActions.setOpaque(false);
        leftActions.add(addLegButton);
        leftActions.add(addDutButton);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightActions.setOpaque(false);
        rightActions.add(auditLogButton);
        rightActions.add(reservationCalendarButton);
        rightActions.add(reportButton);
        rightActions.add(assignProjectTtButton);
        rightActions.add(refreshButton);

        actions.add(leftActions, BorderLayout.WEST);
        actions.add(rightActions, BorderLayout.EAST);

        panel.add(summary, BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private Component buildContactsPanel() {
        teamCardsPanel.setOpaque(false);
        teamCardsPanel.setPreferredSize(new Dimension(10, 156));
        teamCardsPanel.removeAll();
        teamCardsPanel.add(createContactCard("VE", veArea, new Color(27, 111, 170)));
        teamCardsPanel.add(createContactCard("TE", teArea, new Color(91, 139, 61)));
        teamCardsPanel.add(createContactCard("TT", ttArea, new Color(182, 122, 48)));
        return teamCardsPanel;
    }

    private Component buildLegsPanel() {
        JPanel panel = EmcUiTheme.createCardPanel(new BorderLayout(0, 10));

        JLabel title = new JLabel("Lista LEGow");
        title.setFont(EmcUiTheme.SECTION_FONT);

        JLabel hint = new JLabel("Dwuklik na LEGu otwiera jego szczegoly i testy.");
        hint.setForeground(EmcUiTheme.TEXT_MUTED);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(title, BorderLayout.WEST);
        top.add(hint, BorderLayout.EAST);

        panel.add(top, BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(legsTable);
        EmcUiTheme.styleScrollPane(scrollPane);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private Component buildBottomInfoPanel() {
        JPanel panel = EmcUiTheme.createCardPanel(new BorderLayout(0, 8));
        JLabel title = new JLabel("Uprawnienia aktualnej roli");
        title.setFont(EmcUiTheme.SECTION_FONT);
        JTextArea infoArea = new JTextArea();
        EmcUiTheme.styleInfoArea(infoArea);
        infoArea.setText(buildPermissionText());
        panel.add(title, BorderLayout.NORTH);
        panel.add(infoArea, BorderLayout.CENTER);
        return panel;
    }

    private void applyTheme() {
        EmcUiTheme.styleSurfaceButton(addLegButton);
        EmcUiTheme.styleSurfaceButton(addDutButton);
        EmcUiTheme.styleSurfaceButton(auditLogButton);
        EmcUiTheme.styleSurfaceButton(reservationCalendarButton);
        EmcUiTheme.stylePrimaryButton(reportButton);
        EmcUiTheme.stylePrimaryButton(assignProjectTtButton);
        EmcUiTheme.styleSurfaceButton(refreshButton);
    }

    private void configureTable() {
        EmcUiTheme.styleTable(legsTable);
        legsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        legsTable.setFillsViewportHeight(true);
        legsTable.getColumnModel().getColumn(0).setCellRenderer(EmcUiTheme.createStatusRenderer());
    }

    private void configureActions() {
        addLegButton.addActionListener(e -> showManageLegDialog());
        addDutButton.addActionListener(e -> showManageDutDialog());
        auditLogButton.addActionListener(e -> openAuditLog());
        reservationCalendarButton.addActionListener(e -> openReservationCalendar());
        reportButton.addActionListener(e -> generateProjectReport());
        assignProjectTtButton.addActionListener(e -> showAssignProjectTechnicianDialog());
        refreshButton.addActionListener(e -> reloadProject());

        legsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(event)) {
                    openSelectedLegDetails();
                }
            }
        });
    }

    private void reloadProject() {
        currentProject = projectService.getProjectsForUser(currentUser).stream()
                .filter(project -> project.getId() == projectId)
                .findFirst()
                .orElse(null);

        if (currentProject == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Wybrany projekt nie jest juz dostepny dla tego uzytkownika.",
                    "Brak projektu",
                    JOptionPane.WARNING_MESSAGE
            );
            dispose();
            onDataChanged.run();
            return;
        }

        setTitle("Szczegoly projektu - " + currentProject.getEwrNumber());
        showProjectSummary();
        loadLegs();
        updateActionAvailability();
        onDataChanged.run();
    }

    private void showProjectSummary() {
        projectHeaderLabel.setText(currentProject.getEwrNumber() + " | " + buildProjectTitle(currentProject));
        veArea.setText(userDetails(currentProject.getVe()));
        teArea.setText(userDetails(currentProject.getTe()));
        ttArea.setText(currentProject.getTtUsers().isEmpty()
                ? "---"
                : currentProject.getTtUsers().stream().map(this::userDetails).reduce((left, right) -> left + "\n" + right).orElse("---"));

        brandLogoLabel.setText("");
        brandLogoLabel.setIcon(BrandLogoFactory.createLogo(currentProject.getBrand(), 114, 68));
        updateStatusChip(currentProject.getStatus());
        refreshContactCards();
    }

    private void updateStatusChip(TestStatus status) {
        String text = status == null ? "Status projektu: ---" : "Status projektu: " + status.getDisplayName();
        projectStatusLabel.setText(text);
        projectStatusLabel.setForeground(Color.WHITE);

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
        projectStatusLabel.setBackground(background);
    }

    private void loadLegs() {
        currentLegs.clear();
        currentLegs.addAll(projectService.getLegsForProject(currentProject.getId()));

        legsTableModel.setRowCount(0);
        for (Leg leg : currentLegs) {
            legsTableModel.addRow(new Object[]{
                    leg.getStatus().getDisplayName(),
                    leg.getLegCode(),
                    leg.getTestType().name(),
                    leg.getAccreditation().name(),
                    leg.getDutCount(),
                    leg.getIsoStandardName(),
                    leg.getClientStandardName(),
                    leg.getTestPlanName(),
                    leg.getAssignedTt() == null ? "---" : leg.getAssignedTt().getFullName()
            });
        }
    }

    private void updateActionAvailability() {
        boolean hasProject = currentProject != null;
        addLegButton.setEnabled(hasProject && permissionService.canAddLeg(currentUser, currentProject));
        addDutButton.setEnabled(hasProject && permissionService.canAddDut(currentUser, currentProject));
        auditLogButton.setEnabled(hasProject);
        reservationCalendarButton.setEnabled(hasProject);
        reportButton.setEnabled(hasProject);
        assignProjectTtButton.setEnabled(hasProject && permissionService.canAssignProjectTechnicians(currentUser));
        refreshButton.setEnabled(hasProject);
    }

    private void openSelectedLegDetails() {
        int selectedRow = legsTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= currentLegs.size() || currentProject == null) {
            return;
        }

        Leg leg = currentLegs.get(selectedRow);
        new LegDetailsDialog(this, currentUser, currentProject, leg, this::reloadProject)
                .setVisible(true);
    }

    private void showManageLegDialog() {
        int selectedViewRow = legsTable.getSelectedRow();
        if (selectedViewRow < 0) {
            showAddLegDialog();
            return;
        }

        int result = JOptionPane.showOptionDialog(
                this,
                "Wybierz, czy chcesz edytowac zaznaczony LEG, dodac nowy czy usunac aktualnie wybrany.",
                "Modyfikuj LEG",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                new Object[]{"Edytuj zaznaczony", "Dodaj nowy", "Usun zaznaczony", "Anuluj"},
                "Edytuj zaznaczony"
        );

        if (result == 0) {
            int modelRow = legsTable.convertRowIndexToModel(selectedViewRow);
            if (modelRow >= 0 && modelRow < currentLegs.size()) {
                showEditLegDialog(currentLegs.get(modelRow));
            }
            return;
        }
        if (result != 1) {
            if (result != 2) {
                return;
            }
        } else {
            showAddLegDialog();
            return;
        }

        int modelRow = legsTable.convertRowIndexToModel(selectedViewRow);
        if (modelRow < 0 || modelRow >= currentLegs.size()) {
            return;
        }

        Leg selectedLeg = currentLegs.get(modelRow);
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Usunac LEG " + selectedLeg.getLegCode() + " z projektu?",
                "Usuwanie LEGu",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            legDao.deleteLeg(selectedLeg.getId());
            workflowConsistencyService.synchronizeAll();
            auditLogService.log(
                    currentUser,
                    "LEG_DELETED",
                    "LEG",
                    selectedLeg.getId(),
                    currentProject.getId(),
                    selectedLeg.getId(),
                    null,
                    "Usunieto LEG z projektu " + currentProject.getEwrNumber(),
                    selectedLeg.getLegCode() + " | " + selectedLeg.getTestType().name()
            );
            reloadProject();
            JOptionPane.showMessageDialog(this, "LEG zostal usuniety.", "Sukces", JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException exception) {
            showError("Nie udalo sie usunac LEGu.", exception);
        }
    }

    private void showManageDutDialog() {
        if (currentProject == null) {
            return;
        }

        List<DutSample> currentSamples = dutSampleDao.findByProjectId(currentProject.getId());
        DefaultListModel<DutSample> listModel = new DefaultListModel<>();
        currentSamples.forEach(listModel::addElement);
        JList<DutSample> dutList = new JList<>(listModel);
        dutList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        dutList.setVisibleRowCount(Math.min(12, Math.max(6, currentSamples.size())));

        JTextField newDutField = new JTextField();
        EmcUiTheme.styleTextField(newDutField);
        JButton addDutRowButton = new JButton("Dodaj DUT");
        JButton removeDutRowButton = new JButton("Usun DUT");
        JButton mediaButton = new JButton("Zdjecia DUT");
        EmcUiTheme.stylePrimaryButton(addDutRowButton);
        EmcUiTheme.styleSurfaceButton(removeDutRowButton);
        EmcUiTheme.styleSurfaceButton(mediaButton);

        List<Integer> removedIds = new ArrayList<>();
        List<String> addedCodes = new ArrayList<>();

        addDutRowButton.addActionListener(e -> {
            String sampleCode = newDutField.getText().trim();
            if (sampleCode.isBlank()) {
                JOptionPane.showMessageDialog(this, "Wpisz numer DUT.", "Walidacja", JOptionPane.WARNING_MESSAGE);
                return;
            }
            boolean existsInList = IntStream.range(0, listModel.size())
                    .mapToObj(listModel::getElementAt)
                    .anyMatch(sample -> sample.getSampleCode().equalsIgnoreCase(sampleCode));
            if (existsInList) {
                JOptionPane.showMessageDialog(this, "Ten DUT jest juz na liscie projektu.", "Informacja", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            DutSample sample = new DutSample();
            sample.setId(0);
            sample.setProjectId(currentProject.getId());
            sample.setSampleCode(sampleCode);
            String cleaned = sampleCode.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
            if (cleaned.length() > 10) {
                cleaned = cleaned.substring(0, 10);
            }
            if (cleaned.length() < 10) {
                cleaned = cleaned + "X".repeat(10 - cleaned.length());
            }
            sample.setSerialNumber("SN" + cleaned);
            listModel.addElement(sample);
            addedCodes.add(sampleCode);
            newDutField.setText("");
            dutList.setSelectedIndex(listModel.size() - 1);
        });

        removeDutRowButton.addActionListener(e -> {
            DutSample selectedSample = dutList.getSelectedValue();
            if (selectedSample == null) {
                JOptionPane.showMessageDialog(this, "Zaznacz DUT z listy.", "Informacja", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if (selectedSample.getId() > 0) {
                removedIds.add(selectedSample.getId());
            } else {
                addedCodes.removeIf(code -> code.equalsIgnoreCase(selectedSample.getSampleCode()));
            }
            listModel.removeElement(selectedSample);
        });

        mediaButton.addActionListener(e -> {
            DutSample selectedSample = dutList.getSelectedValue();
            if (selectedSample == null) {
                JOptionPane.showMessageDialog(this, "Zaznacz DUT z listy.", "Informacja", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if (selectedSample.getId() <= 0) {
                JOptionPane.showMessageDialog(
                        this,
                        "Najpierw zapisz nowo dodany DUT w projekcie. Zdjecia mozna dodac po utworzeniu wpisu w bazie.",
                        "Informacja",
                        JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }
            showDutMediaDialog(selectedSample);
        });

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.add(new JLabel("DUT-y przypisane do projektu. Zaznacz z listy, aby usunac, albo dodaj nowy numer."), BorderLayout.NORTH);

        JScrollPane listScrollPane = new JScrollPane(dutList);
        listScrollPane.setPreferredSize(new Dimension(420, 280));
        EmcUiTheme.styleScrollPane(listScrollPane);
        panel.add(listScrollPane, BorderLayout.CENTER);

        JPanel addPanel = new JPanel(new BorderLayout(8, 0));
        addPanel.setOpaque(false);
        addPanel.add(newDutField, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionPanel.setOpaque(false);
        actionPanel.add(mediaButton);
        actionPanel.add(addDutRowButton);
        actionPanel.add(removeDutRowButton);
        addPanel.add(actionPanel, BorderLayout.EAST);

        panel.add(addPanel, BorderLayout.SOUTH);
        EmcUiTheme.styleFormTree(panel);

        int result = JOptionPane.showConfirmDialog(this, panel, "Modyfikuj DUT w projekcie " + currentProject.getEwrNumber(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        if (removedIds.isEmpty() && addedCodes.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nie wprowadzono zadnych zmian DUT.", "Informacja", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            dutSampleDao.deleteSamplesFromProject(currentProject.getId(), removedIds);
            int inserted = dutSampleDao.insertSamples(currentProject.getId(), addedCodes);
            workflowConsistencyService.synchronizeAll();
            auditLogService.log(
                    currentUser,
                    "PROJECT_DUTS_UPDATED",
                    "PROJECT",
                    currentProject.getId(),
                    currentProject.getId(),
                    null,
                    null,
                    "Zaktualizowano DUT-y projektu " + currentProject.getEwrNumber(),
                    "Usuniete: " + removedIds.size() + " | dodane: " + inserted
            );
            reloadProject();
            JOptionPane.showMessageDialog(this, "Lista DUT w projekcie zostala zaktualizowana.", "Sukces", JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException exception) {
            showError("Nie udalo sie zaktualizowac DUT-ow projektu.", exception);
        }
    }

    private void showDutMediaDialog(DutSample sample) {
        JDialog dialog = new JDialog(this, "Zdjecia DUT " + sample.getSampleCode(), Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(760, 420);
        dialog.setMinimumSize(new Dimension(720, 360));
        dialog.setLocationRelativeTo(this);

        JPanel root = EmcUiTheme.createPatternPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(14, 14, 14, 14));
        dialog.setContentPane(root);

        JLabel title = new JLabel("Zdjecia DUT: " + sample.getSampleCode() + " | serial: " + valueOrDash(sample.getSerialNumber()));
        title.setFont(EmcUiTheme.SECTION_FONT.deriveFont(18f));

        JPanel content = EmcUiTheme.createCardPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0;

        Map<DutMediaType, DutMedia> mediaByType = new EnumMap<>(DutMediaType.class);
        for (DutMedia media : dutMediaDao.findBySampleId(sample.getId())) {
            mediaByType.put(media.getMediaType(), media);
        }

        for (DutMediaType mediaType : DutMediaType.values()) {
            gbc.gridx = 0;
            gbc.weightx = 0;
            content.add(new JLabel(mediaType.getDisplayName() + ":"), gbc);

            JLabel fileLabel = new JLabel(currentMediaLabel(mediaByType.get(mediaType)));
            fileLabel.setForeground(EmcUiTheme.TEXT_MUTED);
            gbc.gridx = 1;
            gbc.weightx = 1;
            content.add(fileLabel, gbc);

            JButton uploadButton = new JButton("Dodaj");
            JButton openButton = new JButton("Otworz");
            JButton removeButton = new JButton("Usun");
            EmcUiTheme.stylePrimaryButton(uploadButton);
            EmcUiTheme.styleSurfaceButton(openButton);
            EmcUiTheme.styleSurfaceButton(removeButton);

            uploadButton.addActionListener(e -> {
                JFileChooser chooser = createImageChooser();
                if (chooser.showOpenDialog(dialog) != JFileChooser.APPROVE_OPTION) {
                    return;
                }
                try {
                    Path path = chooser.getSelectedFile().toPath();
                    ImageStorageUtil.StoredImage storedImage = ImageStorageUtil.prepareForDatabase(path);
                    byte[] data = storedImage.fileData();
                    dutMediaDao.upsertMedia(sample.getId(), mediaType, storedImage.fileName(), data);
                    DutMedia media = new DutMedia();
                    media.setDutSampleId(sample.getId());
                    media.setMediaType(mediaType);
                    media.setFileName(storedImage.fileName());
                    media.setFileData(data);
                    mediaByType.put(mediaType, media);
                    fileLabel.setText(currentMediaLabel(media));
                } catch (Exception exception) {
                    showError("Nie udalo sie zapisac zdjecia DUT.", exception);
                }
            });

            openButton.addActionListener(e -> {
                DutMedia media = mediaByType.get(mediaType);
                if (media == null) {
                    JOptionPane.showMessageDialog(dialog, "Brak pliku dla tej pozycji.", "Informacja", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                DesktopActions.openStoredDocument(dialog, media.getFileName(), media.getFileData());
            });

            removeButton.addActionListener(e -> {
                DutMedia media = mediaByType.get(mediaType);
                if (media == null) {
                    return;
                }
                dutMediaDao.deleteMedia(sample.getId(), mediaType);
                mediaByType.remove(mediaType);
                fileLabel.setText(currentMediaLabel(null));
            });

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            buttons.setOpaque(false);
            buttons.add(uploadButton);
            buttons.add(openButton);
            buttons.add(removeButton);

            gbc.gridx = 2;
            gbc.weightx = 0;
            content.add(buttons, gbc);
            gbc.gridy++;
        }

        JButton closeButton = new JButton("Zamknij");
        EmcUiTheme.styleGhostButton(closeButton);
        closeButton.addActionListener(e -> dialog.dispose());

        root.add(title, BorderLayout.NORTH);
        root.add(content, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        footer.setOpaque(false);
        footer.add(closeButton);
        root.add(footer, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private JFileChooser createImageChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Pliki graficzne", "png", "jpg", "jpeg", "bmp", "gif"
        ));
        return chooser;
    }

    private String currentMediaLabel(DutMedia media) {
        return media == null || trimToNull(media.getFileName()) == null ? "---" : media.getFileName();
    }

    private String buildPermissionText() {
        if (currentUser.getRole() == pl.emcmanagement.enums.Role.VE) {
            return "VE moze modyfikowac tylko swoje projekty, zarzadzac lista LEG i DUT oraz przygotowywac konfiguracje badan.";
        }
        if (currentUser.getRole() == pl.emcmanagement.enums.Role.TE) {
            return "TE moze modyfikowac TT w swoim projekcie, zarzadzac DUT-ami, sprzetem oraz edytowac kroki i wyniki DUT.";
        }
        if (permissionService.isAdministrator(currentUser)) {
            return "Administrator moze zarzadzac wszystkimi projektami, testami, sprzetem i uzytkownikami.";
        }
        return "TT moze zmieniac statusy tylko swoich przypisanych LEGow oraz edytowac wyniki DUT dla swoich testow.";
    }

    private String buildProjectTitle(Project project) {
        StringBuilder builder = new StringBuilder();
        appendIfPresent(builder, project.getBrand());
        appendIfPresent(builder, project.getDeviceName());
        appendIfPresent(builder, project.getShortDescription());
        return builder.toString().trim();
    }

    private void appendIfPresent(StringBuilder builder, String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(' ');
        }
        builder.append(trimmed);
    }

    private String userDetails(User user) {
        return user == null ? "---" : user.getFullName() + " - " + user.getEmail();
    }

    private String valueOrDash(String value) {
        if (value == null) {
            return "---";
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? "---" : trimmed;
    }

    private JTextArea createSummaryArea(int rows) {
        JTextArea area = new JTextArea(rows, 1);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setBorder(BorderFactory.createEmptyBorder());
        area.setFocusable(false);
        area.setForeground(EmcUiTheme.TEXT_PRIMARY);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return area;
    }

    private void refreshContactCards() {
        buildContactsPanel();
        teamCardsPanel.revalidate();
        teamCardsPanel.repaint();
    }

    private JPanel createContactCard(String role, JTextArea valueArea, Color accent) {
        JPanel card = EmcUiTheme.createCardPanel(new BorderLayout(12, 0));
        card.setPreferredSize(new Dimension(220, 150));

        JLabel avatarLabel = new JLabel(EmcUiTheme.createAvatarIcon(role, accent, 40));
        avatarLabel.setVerticalAlignment(SwingConstants.TOP);

        JPanel textPanel = createContactContent(valueArea.getText());
        JScrollPane scrollPane = createContactScrollPane(textPanel);

        card.add(avatarLabel, BorderLayout.WEST);
        card.add(scrollPane, BorderLayout.CENTER);
        return card;
    }


    private JPanel createContactContent(String rawText) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        addContactRows(panel, rawText);
        return panel;
    }

    private JScrollPane createContactScrollPane(JPanel content) {
        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(12);
        return scrollPane;
    }

    private void addContactRows(JPanel panel, String rawText) {
        List<String> rows = rawText == null ? List.of() : rawText.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();

        if (rows.isEmpty() || (rows.size() == 1 && rows.get(0).equals("---"))) {
            JLabel placeholder = new JLabel("---");
            placeholder.setFont(new Font("Segoe UI", Font.BOLD, 15));
            placeholder.setForeground(EmcUiTheme.TEXT_MUTED);
            panel.add(placeholder);
            return;
        }

        for (int i = 0; i < rows.size(); i++) {
            String row = rows.get(i);
            String[] parts = row.split("\\s-\\s", 2);
            String name = parts.length > 0 ? parts[0].trim() : row;
            String email = parts.length > 1 ? parts[1].trim() : "";

            JLabel label = new JLabel("<html><div style='width:285px;'>"
                    + "<span style='font-size:15px;font-weight:700;color:#1a232c;'>" + escapeHtml(name) + "</span>"
                    + (email.isBlank()
                    ? ""
                    : "<br><span style='font-size:11px;color:#62707f;'>" + escapeHtml(email) + "</span>")
                    + "</div></html>");
            panel.add(label);
            if (i < rows.size() - 1) {
                panel.add(Box.createVerticalStrut(8));
            }
        }
    }

    private String escapeHtml(String text) {
        return valueOrDash(text)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private void showAddLegDialog() {
        if (currentProject == null) {
            return;
        }

        JTextField legCodeField = new JTextField();
        JComboBox<TestType> testTypeCombo = new JComboBox<>(TestType.values());
        JComboBox<AccreditationStatus> accreditationCombo = new JComboBox<>(AccreditationStatus.values());
        JTextField startDateField = new JTextField();
        JTextField endDateField = new JTextField();
        JComboBox<Object> technicianCombo = new JComboBox<>();
        technicianCombo.addItem(NO_TECHNICIAN_LABEL);
        for (User technician : currentProject.getTtUsers()) {
            technicianCombo.addItem(technician);
        }
        JTextField isoNameField = new JTextField();
        JTextField clientNameField = new JTextField();
        JTextField testPlanNameField = new JTextField();
        JTextArea stepNamesArea = new JTextArea(6, 24);

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        addFormRow(formPanel, gbc, "Kod LEGu:", legCodeField);
        addFormRow(formPanel, gbc, "Typ testu:", testTypeCombo);
        addFormRow(formPanel, gbc, "Akredytacja:", accreditationCombo);
        addFormRow(formPanel, gbc, "Start (YYYY-MM-DD):", startDateField);
        addFormRow(formPanel, gbc, "Koniec (YYYY-MM-DD):", endDateField);
        addFormRow(formPanel, gbc, "TT dla LEGu:", technicianCombo);
        addFormRow(formPanel, gbc, "ISO:", isoNameField);
        addFormRow(formPanel, gbc, "Norma klienta:", clientNameField);
        addFormRow(formPanel, gbc, "Test plan:", testPlanNameField);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        formPanel.add(new JLabel("Kroki testowe (1 na linie):"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        formPanel.add(new JScrollPane(stepNamesArea), gbc);
        EmcUiTheme.styleFormTree(formPanel);

        int result = JOptionPane.showConfirmDialog(
                this,
                formPanel,
                "Nowy LEG dla projektu " + currentProject.getEwrNumber(),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String legCode = legCodeField.getText().trim();
        if (legCode.isBlank()) {
            JOptionPane.showMessageDialog(this, "Kod LEGu jest wymagany.", "Walidacja", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Leg leg = new Leg();
            leg.setProjectId(currentProject.getId());
            leg.setLegCode(legCode);
            leg.setTestType((TestType) testTypeCombo.getSelectedItem());
            leg.setAccreditation((AccreditationStatus) accreditationCombo.getSelectedItem());
            leg.setStartDate(parseOptionalDate(startDateField.getText()));
            leg.setEndDate(parseOptionalDate(endDateField.getText()));
            if (technicianCombo.getSelectedItem() instanceof User technician) {
                leg.setAssignedTt(technician);
            }
            leg.setIsoStandardName(trimToNull(isoNameField.getText()));
            leg.setIsoFilePath(null);
            leg.setClientStandardName(trimToNull(clientNameField.getText()));
            leg.setClientFilePath(null);
            leg.setTestPlanName(trimToNull(testPlanNameField.getText()));
            leg.setTestPlanFilePath(null);
            leg.setPcaUrl(DEFAULT_PCA_URL);

            int legId = legDao.insertLeg(leg);
            List<String> stepNames = stepNamesArea.getText()
                    .lines()
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .toList();
            if (!stepNames.isEmpty()) {
                legTestStepDao.insertSteps(legId, stepNames);
            }

            auditLogService.log(
                    currentUser,
                    "LEG_CREATED",
                    "LEG",
                    legId,
                    currentProject.getId(),
                    legId,
                    null,
                    "Dodano LEG do projektu " + currentProject.getEwrNumber(),
                    legCode + " | " + leg.getTestType().name() + " | kroki: " + stepNames.size()
            );
            reloadProject();
            JOptionPane.showMessageDialog(this, "LEG zostal dodany.", "Sukces", JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException exception) {
            showError("Nie udalo sie dodac LEGu.", exception);
        }
    }

    private void showEditLegDialog(Leg selectedLeg) {
        if (currentProject == null || selectedLeg == null) {
            return;
        }

        Leg editableLeg = legDao.findById(selectedLeg.getId());

        JTextField legCodeField = new JTextField(valueOrDash(editableLeg.getLegCode()).equals("---") ? "" : editableLeg.getLegCode());
        JComboBox<TestType> testTypeCombo = new JComboBox<>(TestType.values());
        testTypeCombo.setSelectedItem(editableLeg.getTestType());
        JComboBox<AccreditationStatus> accreditationCombo = new JComboBox<>(AccreditationStatus.values());
        accreditationCombo.setSelectedItem(editableLeg.getAccreditation());
        JTextField startDateField = new JTextField(editableLeg.getStartDate() == null ? "" : editableLeg.getStartDate().toString());
        JTextField endDateField = new JTextField(editableLeg.getEndDate() == null ? "" : editableLeg.getEndDate().toString());
        JComboBox<Object> technicianCombo = new JComboBox<>();
        technicianCombo.addItem(NO_TECHNICIAN_LABEL);
        for (User technician : currentProject.getTtUsers()) {
            technicianCombo.addItem(technician);
        }
        if (editableLeg.getAssignedTt() != null) {
            for (int i = 1; i < technicianCombo.getItemCount(); i++) {
                Object item = technicianCombo.getItemAt(i);
                if (item instanceof User technician && technician.getId() == editableLeg.getAssignedTt().getId()) {
                    technicianCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
        JTextField isoNameField = new JTextField(valueOrDash(editableLeg.getIsoStandardName()).equals("---") ? "" : editableLeg.getIsoStandardName());
        JTextField clientNameField = new JTextField(valueOrDash(editableLeg.getClientStandardName()).equals("---") ? "" : editableLeg.getClientStandardName());
        JTextField testPlanNameField = new JTextField(valueOrDash(editableLeg.getTestPlanName()).equals("---") ? "" : editableLeg.getTestPlanName());

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        addFormRow(formPanel, gbc, "Kod LEGu:", legCodeField);
        addFormRow(formPanel, gbc, "Typ testu:", testTypeCombo);
        addFormRow(formPanel, gbc, "Akredytacja:", accreditationCombo);
        addFormRow(formPanel, gbc, "Start (YYYY-MM-DD):", startDateField);
        addFormRow(formPanel, gbc, "Koniec (YYYY-MM-DD):", endDateField);
        addFormRow(formPanel, gbc, "TT dla LEGu:", technicianCombo);
        addFormRow(formPanel, gbc, "ISO:", isoNameField);
        addFormRow(formPanel, gbc, "Norma klienta:", clientNameField);
        addFormRow(formPanel, gbc, "Test plan:", testPlanNameField);

        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        JLabel hint = new JLabel("Nazwy i harmonogram krokow testowych edytujesz w Szczegolach LEGu.");
        hint.setForeground(EmcUiTheme.TEXT_MUTED);
        formPanel.add(hint, gbc);
        EmcUiTheme.styleFormTree(formPanel);

        int result = JOptionPane.showConfirmDialog(
                this,
                formPanel,
                "Edytuj LEG " + editableLeg.getLegCode(),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String legCode = legCodeField.getText().trim();
        if (legCode.isBlank()) {
            JOptionPane.showMessageDialog(this, "Kod LEGu jest wymagany.", "Walidacja", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            editableLeg.setLegCode(legCode);
            editableLeg.setTestType((TestType) testTypeCombo.getSelectedItem());
            editableLeg.setAccreditation((AccreditationStatus) accreditationCombo.getSelectedItem());
            editableLeg.setStartDate(parseOptionalDate(startDateField.getText()));
            editableLeg.setEndDate(parseOptionalDate(endDateField.getText()));
            editableLeg.setAssignedTt(technicianCombo.getSelectedItem() instanceof User technician ? technician : null);
            editableLeg.setIsoStandardName(trimToNull(isoNameField.getText()));
            editableLeg.setIsoFilePath(null);
            editableLeg.setClientStandardName(trimToNull(clientNameField.getText()));
            editableLeg.setClientFilePath(null);
            editableLeg.setTestPlanName(trimToNull(testPlanNameField.getText()));
            editableLeg.setTestPlanFilePath(null);
            editableLeg.setPcaUrl(DEFAULT_PCA_URL);

            legDao.updateLeg(editableLeg);
            auditLogService.log(
                    currentUser,
                    "LEG_UPDATED",
                    "LEG",
                    editableLeg.getId(),
                    currentProject.getId(),
                    editableLeg.getId(),
                    null,
                    "Zaktualizowano LEG w projekcie " + currentProject.getEwrNumber(),
                    editableLeg.getLegCode() + " | " + editableLeg.getTestType().name()
            );
            reloadProject();
            JOptionPane.showMessageDialog(this, "LEG zostal zaktualizowany.", "Sukces", JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException exception) {
            showError("Nie udalo sie zaktualizowac LEGu.", exception);
        }
    }

    private void showAddDutDialog() {
        if (currentProject == null) {
            return;
        }

        JTextArea sampleCodesArea = new JTextArea(8, 24);
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JLabel("Wpisz kody probek DUT, po jednej na linie:"), BorderLayout.NORTH);
        panel.add(new JScrollPane(sampleCodesArea), BorderLayout.CENTER);
        EmcUiTheme.styleFormTree(panel);

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Dodaj DUT do projektu " + currentProject.getEwrNumber(),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        List<String> sampleCodes = sampleCodesArea.getText()
                .lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();

        if (sampleCodes.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Podaj przynajmniej jeden kod DUT.", "Walidacja", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int inserted = dutSampleDao.insertSamples(currentProject.getId(), sampleCodes);
            auditLogService.log(
                    currentUser,
                    "PROJECT_DUTS_CREATED",
                    "PROJECT",
                    currentProject.getId(),
                    currentProject.getId(),
                    null,
                    null,
                    "Dodano DUT-y do projektu " + currentProject.getEwrNumber(),
                    "Nowe probki: " + inserted
            );
            reloadProject();
            JOptionPane.showMessageDialog(
                    this,
                    "Dodano " + inserted + " nowych DUT-ow do projektu.",
                    "Sukces",
                    JOptionPane.INFORMATION_MESSAGE
            );
        } catch (RuntimeException exception) {
            showError("Nie udalo sie dodac DUT-ow.", exception);
        }
    }

    private void showAssignProjectTechnicianDialog() {
        if (currentProject == null) {
            return;
        }

        List<User> technicians = userDao.findAllTechnicians();
        JList<User> technicianList = new JList<>(technicians.toArray(User[]::new));
        technicianList.setVisibleRowCount(Math.min(10, Math.max(4, technicians.size())));
        technicianList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        List<Integer> selectedIndexes = new ArrayList<>();
        for (int index = 0; index < technicians.size(); index++) {
            int technicianId = technicians.get(index).getId();
            if (currentProject.getTtUsers().stream().anyMatch(existing -> existing.getId() == technicianId)) {
                selectedIndexes.add(index);
            }
        }
        technicianList.setSelectedIndices(selectedIndexes.stream().mapToInt(Integer::intValue).toArray());

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JLabel("Zaznacz technikow TT, ktorzy maja pozostac przypisani do projektu:"), BorderLayout.NORTH);
        panel.add(new JScrollPane(technicianList), BorderLayout.CENTER);
        EmcUiTheme.styleFormTree(panel);

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Modyfikuj TT w projekcie " + currentProject.getEwrNumber(),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            List<Integer> selectedTechnicianIds = technicianList.getSelectedValuesList().stream()
                    .map(User::getId)
                    .toList();
            projectDao.replaceTechniciansForProject(currentProject.getId(), selectedTechnicianIds);
            auditLogService.log(
                    currentUser,
                    "PROJECT_TT_UPDATED",
                    "PROJECT",
                    currentProject.getId(),
                    currentProject.getId(),
                    null,
                    null,
                    "Zmieniono przypisanie TT w projekcie " + currentProject.getEwrNumber(),
                    "Liczba TT: " + selectedTechnicianIds.size()
            );
            reloadProject();
            JOptionPane.showMessageDialog(this, "Lista TT dla projektu zostala zaktualizowana.", "Sukces", JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException exception) {
            showError("Nie udalo sie zapisac zmian TT w projekcie.", exception);
        }
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, String label, Component field) {
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(field, gbc);
        gbc.gridy++;
    }

    private LocalDate parseOptionalDate(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(trimmed);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Niepoprawny format daty: " + trimmed + ". Uzyj YYYY-MM-DD.");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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

    private void openAuditLog() {
        if (currentProject == null) {
            return;
        }
        new AuditLogDialog(this, currentProject.getId(), null, currentProject.getEwrNumber()).setVisible(true);
    }

    private void openReservationCalendar() {
        new ReservationCalendarDialog(this, null, "wszystkie rezerwacje EWR").setVisible(true);
    }

    private void generateProjectReport() {
        if (currentProject == null) {
            return;
        }
        try {
            Path reportPath = projectReportPdfService.generateProjectReport(currentProject);
            auditLogService.log(
                    currentUser,
                    "PROJECT_REPORT_GENERATED",
                    "PROJECT",
                    currentProject.getId(),
                    currentProject.getId(),
                    null,
                    null,
                    "Wygenerowano raport PDF projektu " + currentProject.getEwrNumber(),
                    reportPath.getFileName().toString()
            );
            DesktopActions.openFile(this, reportPath.toString());
        } catch (RuntimeException exception) {
            showError("Nie udalo sie wygenerowac raportu PDF.", exception);
        }
    }
}
