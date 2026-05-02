package pl.emcmanagement.ui;

import pl.emcmanagement.database.AppConfig;
import pl.emcmanagement.model.User;
import pl.emcmanagement.service.AuthService;
import pl.emcmanagement.ui.style.EmcUiTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Optional;

public class LoginFrame extends JFrame {
    private final JTextField loginField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final AuthService authService = new AuthService();

    public LoginFrame() {
        setTitle(AppConfig.get("app.name") + " - Logowanie");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(540, 360);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel root = EmcUiTheme.createPatternPanel(new GridBagLayout());
        root.setBorder(new EmptyBorder(24, 24, 24, 24));
        setContentPane(root);

        JPanel card = EmcUiTheme.createCardPanel(new BorderLayout(0, 18));
        card.setPreferredSize(new Dimension(470, 286));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("EMC Management System");
        title.setFont(EmcUiTheme.TITLE_FONT.deriveFont(26f));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Logowanie do laboratorium EMC");
        subtitle.setForeground(EmcUiTheme.TEXT_MUTED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(EmcUiTheme.createPkBadgePanel(360));
        header.add(Box.createVerticalStrut(14));
        header.add(title);
        header.add(Box.createVerticalStrut(6));
        header.add(subtitle);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 6, 8, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("Login:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(loginField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        form.add(new JLabel("Haslo:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(passwordField, gbc);

        JButton loginButton = new JButton("Zaloguj");
        loginButton.addActionListener(e -> performLogin());
        getRootPane().setDefaultButton(loginButton);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setOpaque(false);
        actions.add(loginButton);

        card.add(header, BorderLayout.NORTH);
        card.add(form, BorderLayout.CENTER);
        card.add(actions, BorderLayout.SOUTH);
        root.add(card);

        EmcUiTheme.styleTextField(loginField);
        EmcUiTheme.styleTextField(passwordField);
        EmcUiTheme.stylePrimaryButton(loginButton);
    }

    private void performLogin() {
        Optional<User> userOptional = authService.login(loginField.getText(), new String(passwordField.getPassword()));
        if (userOptional.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Niepoprawny login lub haslo.",
                    "Blad logowania",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        MainFrame mainFrame = new MainFrame(userOptional.get());
        mainFrame.setVisible(true);
        dispose();
    }
}
