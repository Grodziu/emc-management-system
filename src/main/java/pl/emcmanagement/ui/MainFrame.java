package pl.emcmanagement.ui;

import pl.emcmanagement.dao.UserDao;
import pl.emcmanagement.database.AppConfig;
import pl.emcmanagement.enums.Role;
import pl.emcmanagement.enums.TestStatus;
import pl.emcmanagement.model.Project;
import pl.emcmanagement.model.User;
import pl.emcmanagement.service.PermissionService;
import pl.emcmanagement.service.ProjectService;
import pl.emcmanagement.ui.dialog.AuditLogDialog;
import pl.emcmanagement.ui.dialog.ProjectDetailsDialog;
import pl.emcmanagement.ui.dialog.ReservationCalendarDialog;
import pl.emcmanagement.ui.style.EmcUiTheme;
import pl.emcmanagement.util.BrandLogoFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class MainFrame extends JFrame {
    private final User currentUser;
    private final ProjectService projectService = new ProjectService();
    private final PermissionService permissionService = new PermissionService();
    private final UserDao userDao = new UserDao();

    private final DefaultTableModel explorerTableModel = new DefaultTableModel(
            new Object[]{"EWR", "Klient", "Urzadzenie", "VE", "TE", "Status"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable explorerTable = new JTable(explorerTableModel);
    private final TableRowSorter<DefaultTableModel> explorerSorter = new TableRowSorter<>(explorerTableModel);

    private final JLabel roleInfoLabel = new JLabel();
    private final JLabel projectHeaderLabel = new JLabel("Wybierz projekt z eksploratora");
    private final JLabel projectStatusLabel = new JLabel("Status projektu");
    private final JLabel brandLogoLabel = new JLabel("Logo", SwingConstants.CENTER);
    private final JTextArea veArea = createSummaryArea(2);
    private final JTextArea teArea = createSummaryArea(2);
    private final JTextArea ttArea = createSummaryArea(4);
    private final JPanel teamCardsPanel = new JPanel(new GridLayout(1, 3, 10, 0));

    private final JTextField ewrFilterField = new JTextField();
    private final JTextField veFilterField = new JTextField();
    private final JTextField teFilterField = new JTextField();
    private final JComboBox<String> sortByCombo = new JComboBox<>(new String[]{"EWR", "VE", "TE", "Klient", "Status"});
    private final JComboBox<String> sortDirectionCombo = new JComboBox<>(new String[]{"Rosnaco", "Malejaco"});

    private final JButton addUserButton = new JButton("Dodaj uzytkownika");
    private final JButton auditLogButton = new JButton("Audit log");
    private final JButton reservationCalendarButton = new JButton("Kalendarz");
    private final JButton logoutButton = new JButton("Wyloguj");

    private final List<Project> explorerProjects = new ArrayList<>();
    private Project currentProject;

    public MainFrame(User currentUser) {
        this.currentUser = currentUser;

        setTitle(AppConfig.get("app.name") + " - " + currentUser.getFullName());
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1360, 820);
        setMinimumSize(new Dimension(1180, 720));
        setLocationRelativeTo(null);

        JPanel root = EmcUiTheme.createPatternPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));
        setContentPane(root);

        root.add(buildTopPanel(), BorderLayout.NORTH);
        root.add(buildCenterPanel(), BorderLayout.CENTER);

        applyTheme();
        configureTables();
        configureActions();
        updateActionAvailability();
        loadProjects(null);
    }

    private Component buildTopPanel() {
        JPanel panel = EmcUiTheme.createCardPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(10, 92));

        JLabel title = new JLabel("Eksplorator projektow EWR");
        title.setFont(EmcUiTheme.TITLE_FONT);
        roleInfoLabel.setText("<html><span style='font-size:12px;color:#62707f;'>Aktywny profil:</span> "
                + "<span style='font-size:13px;font-weight:700;color:#1a232c;'>" + currentUser.getFullName() + "</span>"
                + "<span style='font-size:12px;color:#62707f;'>  |  rola: " + currentUser.getRole().name() + "</span></html>");
        roleInfoLabel.setForeground(EmcUiTheme.TEXT_MUTED);

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.add(title);
        textPanel.add(Box.createVerticalStrut(4));
        textPanel.add(roleInfoLabel);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 4));
        actionsPanel.setOpaque(false);
        actionsPanel.add(EmcUiTheme.createPkBadgePanel(420));
        actionsPanel.add(auditLogButton);
        actionsPanel.add(reservationCalendarButton);
        actionsPanel.add(addUserButton);
        actionsPanel.add(logoutButton);

        panel.add(textPanel, BorderLayout.WEST);
        panel.add(actionsPanel, BorderLayout.EAST);
        return panel;
    }

    private Component buildCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;

        gbc.gridy = 0;
        gbc.weighty = 0.58;
        panel.add(buildExplorerPanel(), gbc);

        gbc.gridy = 1;
        gbc.weighty = 0.42;
        gbc.insets = new Insets(12, 0, 0, 0);
        panel.add(buildProjectSummaryPanel(), gbc);
        return panel;
    }

    private Component buildExplorerPanel() {
        JPanel panel = EmcUiTheme.createCardPanel(new BorderLayout(0, 10));

        JLabel sectionTitle = new JLabel("Eksplorator projektow");
        sectionTitle.setFont(EmcUiTheme.SECTION_FONT);
        panel.add(sectionTitle, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);
        content.add(buildExplorerControlsPanel(), BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(explorerTable);
        EmcUiTheme.styleScrollPane(scrollPane);
        content.add(scrollPane, BorderLayout.CENTER);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private Component buildExplorerControlsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 0;

        addFilterControl(panel, gbc, 0, "Filtr EWR:", ewrFilterField, 0.9);
        addFilterControl(panel, gbc, 2, "Filtr VE:", veFilterField, 0.9);
        addFilterControl(panel, gbc, 4, "Filtr TE:", teFilterField, 0.9);

        gbc.gridy = 1;
        addFilterControl(panel, gbc, 0, "Sortuj po:", sortByCombo, 0.55);
        addFilterControl(panel, gbc, 2, "Kierunek:", sortDirectionCombo, 0.45);

        JLabel hintLabel = new JLabel("Kliknij projekt, aby pokazac podglad. Dwuklik otwiera pelne szczegoly projektu.");
        hintLabel.setForeground(EmcUiTheme.TEXT_MUTED);
        gbc.gridx = 4;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        panel.add(hintLabel, gbc);
        return panel;
    }

    private Component buildProjectSummaryPanel() {
        JPanel panel = EmcUiTheme.createCardPanel(new BorderLayout(0, 12));
        panel.setPreferredSize(new Dimension(10, 286));

        JLabel sectionTitle = new JLabel("Podglad projektu");
        sectionTitle.setFont(EmcUiTheme.SECTION_FONT);
        panel.add(sectionTitle, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);

        JPanel topRow = new JPanel(new BorderLayout(14, 0));
        topRow.setOpaque(false);

        JPanel logoPanel = new JPanel(new GridBagLayout());
        logoPanel.setOpaque(false);
        brandLogoLabel.setPreferredSize(new Dimension(118, 78));
        brandLogoLabel.setMinimumSize(new Dimension(118, 78));
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

        JPanel headingPanel = new JPanel();
        headingPanel.setOpaque(false);
        headingPanel.setLayout(new BoxLayout(headingPanel, BoxLayout.Y_AXIS));
        headingPanel.add(projectHeaderLabel);
        headingPanel.add(Box.createVerticalStrut(8));
        headingPanel.add(projectStatusLabel);
        infoPanel.add(headingPanel, BorderLayout.NORTH);
        topRow.add(infoPanel, BorderLayout.CENTER);

        content.add(topRow, BorderLayout.NORTH);
        content.add(buildContactSummaryPanel(), BorderLayout.CENTER);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private Component buildContactSummaryPanel() {
        teamCardsPanel.setOpaque(false);
        teamCardsPanel.setPreferredSize(new Dimension(10, 154));
        teamCardsPanel.removeAll();
        teamCardsPanel.add(createContactCard("VE", veArea, new Color(27, 111, 170)));
        teamCardsPanel.add(createContactCard("TE", teArea, new Color(91, 139, 61)));
        teamCardsPanel.add(createContactCard("TT", ttArea, new Color(182, 122, 48)));
        return teamCardsPanel;
    }

    private void configureTables() {
        EmcUiTheme.styleTable(explorerTable);
        explorerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        explorerTable.setFillsViewportHeight(true);
        explorerTable.setAutoCreateRowSorter(false);
        explorerTable.setRowSorter(explorerSorter);
        explorerTable.getColumnModel().getColumn(0).setPreferredWidth(110);
        explorerTable.getColumnModel().getColumn(1).setPreferredWidth(140);
        explorerTable.getColumnModel().getColumn(2).setPreferredWidth(260);
        explorerTable.getColumnModel().getColumn(3).setPreferredWidth(200);
        explorerTable.getColumnModel().getColumn(4).setPreferredWidth(200);
        explorerTable.getColumnModel().getColumn(5).setPreferredWidth(160);
        explorerTable.getColumnModel().getColumn(5).setCellRenderer(EmcUiTheme.createStatusRenderer());
    }

    private void configureActions() {
        addUserButton.addActionListener(e -> showAddUserDialog());
        auditLogButton.addActionListener(e -> new AuditLogDialog(this, null, null, "wszystkie dostepne projekty").setVisible(true));
        reservationCalendarButton.addActionListener(e -> new ReservationCalendarDialog(this, null, "wszystkie dostepne projekty").setVisible(true));
        logoutButton.addActionListener(e -> performLogout());

        explorerTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onExplorerSelectionChanged();
            }
        });

        explorerTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(event)) {
                    int row = explorerTable.rowAtPoint(event.getPoint());
                    if (row >= 0) {
                        explorerTable.setRowSelectionInterval(row, row);
                        openCurrentProjectDetailsDialog();
                    }
                }
            }
        });

        DocumentListener filterListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                applyExplorerFilters();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                applyExplorerFilters();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                applyExplorerFilters();
            }
        };
        ewrFilterField.getDocument().addDocumentListener(filterListener);
        veFilterField.getDocument().addDocumentListener(filterListener);
        teFilterField.getDocument().addDocumentListener(filterListener);
        sortByCombo.addActionListener(e -> applyExplorerFilters());
        sortDirectionCombo.addActionListener(e -> applyExplorerFilters());
    }

    private void onExplorerSelectionChanged() {
        int selectedViewRow = explorerTable.getSelectedRow();
        if (selectedViewRow < 0) {
            clearProjectSummary();
            return;
        }

        int selectedModelRow = explorerTable.convertRowIndexToModel(selectedViewRow);
        if (selectedModelRow < 0 || selectedModelRow >= explorerProjects.size()) {
            clearProjectSummary();
            return;
        }

        showProjectSummary(explorerProjects.get(selectedModelRow));
    }

    private void openCurrentProjectDetailsDialog() {
        if (currentProject == null) {
            return;
        }

        new ProjectDetailsDialog(this, currentUser, currentProject.getId(), this::reloadCurrentProject)
                .setVisible(true);
    }

    private void loadProjects(Integer preferredProjectId) {
        explorerProjects.clear();
        explorerTableModel.setRowCount(0);

        List<Project> projects = projectService.getProjectsForUser(currentUser);
        for (Project project : projects) {
            explorerProjects.add(project);
            explorerTableModel.addRow(new Object[]{
                    project.getEwrNumber(),
                    valueOrDash(project.getBrand()),
                    buildProjectDeviceLabel(project),
                    userLabel(project.getVe()),
                    userLabel(project.getTe()),
                    project.getStatus() == null ? "---" : project.getStatus().getDisplayName()
            });
        }

        applyExplorerFiltersInternal();

        if (preferredProjectId != null && selectProjectInExplorer(preferredProjectId)) {
            return;
        }

        explorerTable.clearSelection();
        clearProjectSummary();
    }

    private void reloadCurrentProject() {
        loadProjects(currentProject == null ? null : currentProject.getId());
    }

    private void clearProjectSummary() {
        currentProject = null;
        projectHeaderLabel.setText("Wybierz projekt z eksploratora");
        veArea.setText("---");
        teArea.setText("---");
        ttArea.setText("---");
        brandLogoLabel.setIcon(null);
        brandLogoLabel.setText("Logo");
        updateStatusChip(null);
        refreshContactCards();
    }

    private void showProjectSummary(Project project) {
        currentProject = project;
        projectHeaderLabel.setText(project.getEwrNumber() + " | " + buildProjectTitle(project));
        veArea.setText(userDetails(project.getVe()));
        teArea.setText(userDetails(project.getTe()));
        ttArea.setText(project.getTtUsers().isEmpty()
                ? "---"
                : project.getTtUsers().stream().map(this::userDetails).reduce((left, right) -> left + "\n" + right).orElse("---"));

        brandLogoLabel.setText("");
        brandLogoLabel.setIcon(BrandLogoFactory.createLogo(project.getBrand(), 108, 64));
        updateStatusChip(project.getStatus());
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

    private void updateActionAvailability() {
        boolean canManageUsers = permissionService.canManageUsers(currentUser);
        addUserButton.setVisible(canManageUsers);
        addUserButton.setEnabled(canManageUsers);
        auditLogButton.setEnabled(true);
        reservationCalendarButton.setEnabled(true);
        logoutButton.setEnabled(true);
    }

    private void applyExplorerFilters() {
        Integer selectedProjectId = currentProject == null ? null : currentProject.getId();
        applyExplorerFiltersInternal();

        if (selectedProjectId != null && selectProjectInExplorer(selectedProjectId)) {
            return;
        }

        explorerTable.clearSelection();
        clearProjectSummary();
    }

    private void applyExplorerFiltersInternal() {
        List<RowFilter<DefaultTableModel, Object>> filters = new ArrayList<>();
        addContainsFilter(filters, 0, ewrFilterField.getText());
        addContainsFilter(filters, 3, veFilterField.getText());
        addContainsFilter(filters, 4, teFilterField.getText());

        explorerSorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));

        int sortColumn = switch ((String) sortByCombo.getSelectedItem()) {
            case "VE" -> 3;
            case "TE" -> 4;
            case "Klient" -> 1;
            case "Status" -> 5;
            default -> 0;
        };
        SortOrder sortOrder = sortDirectionCombo.getSelectedIndex() == 1 ? SortOrder.DESCENDING : SortOrder.ASCENDING;
        explorerSorter.setSortKeys(List.of(new RowSorter.SortKey(sortColumn, sortOrder)));
        explorerSorter.sort();
    }

    private void addContainsFilter(List<RowFilter<DefaultTableModel, Object>> filters, int columnIndex, String value) {
        String trimmed = value == null ? "" : value.trim();
        if (!trimmed.isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(trimmed), columnIndex));
        }
    }

    private boolean selectProjectInExplorer(int projectId) {
        for (int modelIndex = 0; modelIndex < explorerProjects.size(); modelIndex++) {
            if (explorerProjects.get(modelIndex).getId() == projectId) {
                int viewIndex = explorerTable.convertRowIndexToView(modelIndex);
                if (viewIndex < 0) {
                    return false;
                }
                explorerTable.setRowSelectionInterval(viewIndex, viewIndex);
                explorerTable.scrollRectToVisible(explorerTable.getCellRect(viewIndex, 0, true));
                return true;
            }
        }
        return false;
    }

    private String buildProjectDeviceLabel(Project project) {
        StringBuilder builder = new StringBuilder();
        appendIfPresent(builder, project.getDeviceName());
        appendIfPresent(builder, project.getShortDescription());
        return builder.length() == 0 ? "---" : builder.toString();
    }

    private String buildProjectTitle(Project project) {
        StringBuilder builder = new StringBuilder();
        appendIfPresent(builder, project.getBrand());
        appendIfPresent(builder, project.getDeviceName());
        appendIfPresent(builder, project.getShortDescription());
        return builder.length() == 0 ? "---" : builder.toString();
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

    private String userLabel(User user) {
        return user == null ? "---" : user.getFullName();
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

    private void addFilterControl(JPanel panel, GridBagConstraints gbc, int column, String label, Component field, double weight) {
        gbc.gridx = column;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = column + 1;
        gbc.weightx = weight;
        panel.add(field, gbc);
    }

    private void applyTheme() {
        EmcUiTheme.stylePrimaryButton(addUserButton);
        EmcUiTheme.styleSurfaceButton(auditLogButton);
        EmcUiTheme.styleSurfaceButton(reservationCalendarButton);
        EmcUiTheme.styleSurfaceButton(logoutButton);

        EmcUiTheme.styleTextField(ewrFilterField);
        EmcUiTheme.styleTextField(veFilterField);
        EmcUiTheme.styleTextField(teFilterField);
        EmcUiTheme.styleComboBox(sortByCombo);
        EmcUiTheme.styleComboBox(sortDirectionCombo);
        refreshContactCards();
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
        buildContactSummaryPanel();
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

            JLabel label = new JLabel("<html><div style='width:270px;'>"
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

    private void showAddUserDialog() {
        if (!permissionService.canManageUsers(currentUser)) {
            return;
        }

        JTextField firstNameField = new JTextField();
        JTextField lastNameField = new JTextField();
        JTextField emailField = new JTextField();
        JTextField loginField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JComboBox<Role> roleCombo = new JComboBox<>(Role.values());
        roleCombo.setSelectedItem(Role.TT);

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        addFormRow(formPanel, gbc, "Imie:", firstNameField);
        addFormRow(formPanel, gbc, "Nazwisko:", lastNameField);
        addFormRow(formPanel, gbc, "E-mail:", emailField);
        addFormRow(formPanel, gbc, "Login:", loginField);
        addFormRow(formPanel, gbc, "Haslo:", passwordField);
        addFormRow(formPanel, gbc, "Rola:", roleCombo);
        EmcUiTheme.styleFormTree(formPanel);

        int result = JOptionPane.showConfirmDialog(
                this,
                formPanel,
                "Dodaj nowego uzytkownika",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        User user = new User();
        user.setFirstName(firstNameField.getText().trim());
        user.setLastName(lastNameField.getText().trim());
        user.setEmail(emailField.getText().trim());
        user.setLogin(loginField.getText().trim());
        user.setPassword(new String(passwordField.getPassword()));
        user.setRole((Role) roleCombo.getSelectedItem());

        if (user.getFirstName().isBlank() || user.getLastName().isBlank() || user.getEmail().isBlank()
                || user.getLogin().isBlank() || user.getPassword().isBlank() || user.getRole() == null) {
            JOptionPane.showMessageDialog(this, "Wszystkie pola uzytkownika sa wymagane.", "Walidacja", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int userId = userDao.insertUser(user);
            user.setId(userId);
            new pl.emcmanagement.service.AuditLogService().log(
                    currentUser,
                    "USER_CREATED",
                    "USER",
                    userId,
                    null,
                    null,
                    null,
                    "Dodano nowego uzytkownika " + user.getFullName(),
                    "Login: " + user.getLogin() + " | rola: " + user.getRole().name()
            );
            JOptionPane.showMessageDialog(this, "Uzytkownik zostal dodany.", "Sukces", JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException exception) {
            showError("Nie udalo sie dodac uzytkownika.", exception);
        }
    }

    private void performLogout() {
        int result = JOptionPane.showConfirmDialog(
                this,
                "Czy na pewno chcesz sie wylogowac?",
                "Wylogowanie",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (result != JOptionPane.YES_OPTION) {
            return;
        }

        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
        dispose();
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

    private void showError(String message, Exception exception) {
        JOptionPane.showMessageDialog(
                this,
                message + "\n\n" + exception.getMessage(),
                "Blad",
                JOptionPane.ERROR_MESSAGE
        );
    }
}
