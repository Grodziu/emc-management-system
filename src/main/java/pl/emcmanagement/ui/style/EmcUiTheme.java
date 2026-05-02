package pl.emcmanagement.ui.style;

import pl.emcmanagement.enums.TestStatus;
import pl.emcmanagement.util.PkLogoFactory;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public final class EmcUiTheme {
    public static final Color APP_BACKGROUND = new Color(242, 247, 252);
    public static final Color CARD_BACKGROUND = new Color(255, 255, 255, 244);
    public static final Color CARD_BORDER = new Color(214, 224, 236);
    public static final Color PRIMARY = new Color(18, 98, 150);
    public static final Color PRIMARY_DARK = new Color(12, 65, 105);
    public static final Color PRIMARY_SOFT = new Color(225, 238, 248);
    public static final Color TEXT_PRIMARY = new Color(26, 35, 44);
    public static final Color TEXT_MUTED = new Color(98, 111, 126);
    public static final Color SUCCESS = new Color(80, 167, 111);
    public static final Color SUCCESS_SOFT = new Color(224, 245, 231);
    public static final Color INFO = new Color(78, 146, 214);
    public static final Color INFO_SOFT = new Color(224, 238, 250);
    public static final Color PLANNED = new Color(109, 133, 176);
    public static final Color WARNING = new Color(210, 149, 58);
    public static final Color WARNING_SOFT = new Color(251, 239, 215);
    public static final Color DANGER = new Color(193, 73, 71);
    public static final Color DANGER_SOFT = new Color(250, 227, 226);
    public static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font SECTION_FONT = new Font("Segoe UI", Font.BOLD, 14);

    private EmcUiTheme() {
    }

    public static JPanel createPatternPanel(LayoutManager layout) {
        PatternPanel panel = new PatternPanel(layout);
        panel.setBackground(APP_BACKGROUND);
        return panel;
    }

    public static JPanel createCardPanel(LayoutManager layout) {
        CardPanel panel = new CardPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    public static void configureUiDefaults() {
        UIManager.put("Panel.background", APP_BACKGROUND);
        UIManager.put("OptionPane.background", APP_BACKGROUND);
        UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
        UIManager.put("OptionPane.foreground", TEXT_PRIMARY);
        UIManager.put("Button.font", new Font("Segoe UI", Font.BOLD, 13));
        UIManager.put("Label.font", new Font("Segoe UI", Font.PLAIN, 13));
        UIManager.put("TextField.font", new Font("Segoe UI", Font.PLAIN, 13));
        UIManager.put("TextArea.font", new Font("Segoe UI", Font.PLAIN, 13));
        UIManager.put("ComboBox.font", new Font("Segoe UI", Font.PLAIN, 13));
        UIManager.put("Table.font", new Font("Segoe UI", Font.PLAIN, 13));
        UIManager.put("TableHeader.font", new Font("Segoe UI", Font.BOLD, 12));
    }

    public static Border createCardBorder() {
        return BorderFactory.createCompoundBorder(
                new RoundedLineBorder(CARD_BORDER, 22),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        );
    }

    public static Border createInnerBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(228, 234, 241)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        );
    }

    public static void stylePrimaryButton(AbstractButton button) {
        styleButton(button, PRIMARY, Color.WHITE, new Color(11, 81, 124));
    }

    public static void styleSecondaryButton(AbstractButton button) {
        styleButton(button, Color.WHITE, TEXT_PRIMARY, PRIMARY_SOFT);
        button.setBorder(BorderFactory.createCompoundBorder(
                new RoundedLineBorder(new Color(193, 210, 226), 18),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
    }

    public static void styleGhostButton(AbstractButton button) {
        styleButton(button, new Color(244, 248, 252), TEXT_PRIMARY, new Color(231, 239, 247));
    }

    public static void styleSurfaceButton(AbstractButton button) {
        styleButton(button, PRIMARY_SOFT, PRIMARY_DARK, new Color(207, 225, 241));
        button.setBorder(BorderFactory.createCompoundBorder(
                new RoundedLineBorder(new Color(188, 208, 226), 18),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
    }

    private static void styleButton(AbstractButton button, Color background, Color foreground, Color rollover) {
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setForeground(foreground);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBorder(BorderFactory.createCompoundBorder(
                new RoundedLineBorder(new Color(background.getRGB()), 18),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
        button.setBackground(background);
        button.putClientProperty("emc.rollover", rollover);
        button.putClientProperty("emc.background", background);
        button.putClientProperty("emc.foreground", foreground);
        button.putClientProperty("emc.pressed", blend(background, PRIMARY_DARK, 0.18f));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        if (!Boolean.TRUE.equals(button.getClientProperty("emc.hover.installed"))) {
            button.getModel().addChangeListener(new ButtonHoverListener(button));
            button.putClientProperty("emc.hover.installed", Boolean.TRUE);
        }
        updateButtonState(button);
    }

    public static void styleTextField(JTextField field) {
        field.setBorder(BorderFactory.createCompoundBorder(
                new RoundedLineBorder(new Color(203, 217, 231), 12),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)
        ));
        field.setBackground(Color.WHITE);
        field.setForeground(TEXT_PRIMARY);
        field.setDisabledTextColor(TEXT_PRIMARY);
        field.setCaretColor(TEXT_PRIMARY);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    }

    public static void styleTextArea(JTextArea area) {
        area.setBorder(BorderFactory.createCompoundBorder(
                new RoundedLineBorder(new Color(203, 217, 231), 12),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        area.setBackground(Color.WHITE);
        area.setForeground(TEXT_PRIMARY);
        area.setCaretColor(TEXT_PRIMARY);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    }

    public static void styleInfoArea(JTextArea area) {
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setForeground(TEXT_MUTED);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        area.setBorder(BorderFactory.createEmptyBorder());
        area.setFocusable(false);
    }

    public static void styleFormTree(Component component) {
        if (component instanceof JTextArea textArea) {
            styleTextArea(textArea);
        } else if (component instanceof JTextField textField) {
            styleTextField(textField);
        } else if (component instanceof JComboBox<?> comboBox) {
            styleComboBox(comboBox);
        } else if (component instanceof JScrollPane scrollPane) {
            styleScrollPane(scrollPane);
        } else if (component instanceof JPanel panel) {
            panel.setOpaque(false);
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                styleFormTree(child);
            }
        }
    }

    public static void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setBackground(Color.WHITE);
        comboBox.setForeground(TEXT_PRIMARY);
        comboBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    }

    public static void styleTable(JTable table) {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setForeground(TEXT_PRIMARY);
        table.setRowHeight(28);
        table.setGridColor(new Color(232, 238, 244));
        table.setSelectionBackground(new Color(216, 232, 244));
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new HeaderRenderer());
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 32));
    }

    public static void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(219, 228, 238)));
        scrollPane.getViewport().setBackground(Color.WHITE);
    }

    public static void styleTabbedPane(JTabbedPane tabbedPane) {
        tabbedPane.setOpaque(false);
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabbedPane.setForeground(TEXT_PRIMARY);
        tabbedPane.setBorder(BorderFactory.createEmptyBorder());
        tabbedPane.setUI(new BasicTabbedPaneUI() {
            @Override
            protected void installDefaults() {
                super.installDefaults();
                tabInsets = new Insets(8, 16, 8, 16);
                selectedTabPadInsets = new Insets(0, 0, 0, 0);
                tabAreaInsets = new Insets(0, 0, 8, 0);
                contentBorderInsets = new Insets(0, 0, 0, 0);
            }

            @Override
            protected void paintTabBackground(Graphics graphics, int tabPlacement, int tabIndex, int x, int y, int width, int height, boolean isSelected) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isSelected ? PRIMARY : new Color(233, 240, 247));
                g2.fillRoundRect(x + 1, y + 2, width - 2, height - 3, 14, 14);
                g2.dispose();
            }

            @Override
            protected void paintText(Graphics graphics, int tabPlacement, Font font, FontMetrics metrics, int tabIndex, String title, Rectangle textRect, boolean isSelected) {
                graphics.setFont(font);
                graphics.setColor(isSelected ? Color.WHITE : TEXT_PRIMARY);
                graphics.drawString(title, textRect.x, textRect.y + metrics.getAscent());
            }

            @Override
            protected void paintTabBorder(Graphics graphics, int tabPlacement, int tabIndex, int x, int y, int width, int height, boolean isSelected) {
            }

            @Override
            protected void paintContentBorder(Graphics graphics, int tabPlacement, int selectedIndex) {
            }

            @Override
            protected void paintFocusIndicator(Graphics graphics, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect, Rectangle textRect, boolean isSelected) {
            }

            @Override
            protected int calculateTabHeight(int tabPlacement, int tabIndex, int fontHeight) {
                return 36;
            }
        });
    }

    public static DefaultTableCellRenderer createStatusRenderer() {
        return new StatusChipRenderer();
    }

    public static Icon createAvatarIcon(String text, Color accent, int size) {
        Image image = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = (Graphics2D) image.getGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 42));
        g2.fillOval(0, 0, size - 1, size - 1);
        g2.setColor(accent);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(0, 0, size - 1, size - 1);
        g2.setFont(new Font("Segoe UI", Font.BOLD, Math.max(11, size / 2 - 2)));
        FontMetrics metrics = g2.getFontMetrics();
        int x = (size - metrics.stringWidth(text)) / 2;
        int y = ((size - metrics.getHeight()) / 2) + metrics.getAscent();
        g2.drawString(text, x, y);
        g2.dispose();
        return new ImageIcon(image);
    }

    public static JPanel createPkBadgePanel() {
        return createPkBadgePanel(380);
    }

    public static JPanel createPkBadgePanel(int preferredWidth) {
        JPanel panel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 244));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                g2.setColor(new Color(211, 222, 235));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                g2.dispose();
                super.paintComponent(graphics);
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        panel.setPreferredSize(new Dimension(preferredWidth, 52));
        panel.setMinimumSize(new Dimension(preferredWidth - 20, 52));
        panel.setMaximumSize(new Dimension(preferredWidth + 20, 52));

        JLabel iconLabel = new JLabel(PkLogoFactory.createLogo(preferredWidth - 36, 40));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setVerticalAlignment(SwingConstants.CENTER);
        panel.add(iconLabel);
        return panel;
    }

    private static final class PatternPanel extends JPanel {
        private PatternPanel(LayoutManager layout) {
            super(layout);
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            GradientPaint gradient = new GradientPaint(0, 0, new Color(248, 251, 255), getWidth(), getHeight(), APP_BACKGROUND);
            g2.setPaint(gradient);
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setColor(new Color(201, 214, 228, 72));
            for (int x = 40; x < getWidth(); x += 120) {
                g2.drawLine(x, 0, x, 90);
                g2.drawLine(x + 28, 16, x + 28, 112);
                g2.drawOval(x - 3, 78, 6, 6);
            }

            g2.setStroke(new BasicStroke(1.4f));
            g2.setColor(new Color(167, 188, 210, 96));
            int midY = Math.max(160, getHeight() - 180);
            for (int x = 0; x < getWidth(); x += 12) {
                int y = (int) (midY + Math.sin(x / 20.0) * 22);
                int nextX = x + 12;
                int nextY = (int) (midY + Math.sin(nextX / 20.0) * 22);
                g2.drawLine(x, y, nextX, nextY);
            }
            g2.dispose();
        }
    }

    private static final class CardPanel extends JPanel {
        private CardPanel(LayoutManager layout) {
            super(layout);
            setBorder(createCardBorder());
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(181, 196, 215, 38));
            g2.fillRoundRect(4, 7, getWidth() - 8, getHeight() - 4, 24, 24);
            g2.setColor(CARD_BACKGROUND);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 8, 24, 24);
            g2.setColor(CARD_BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 8, 24, 24);
            g2.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class RoundedLineBorder extends AbstractBorder {
        private final Color color;
        private final int radius;

        private RoundedLineBorder(Color color, int radius) {
            this.color = color;
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.draw(new RoundRectangle2D.Float(x, y, width - 1, height - 1, radius, radius));
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component component) {
            return new Insets(1, 1, 1, 1);
        }
    }

    private static void updateButtonState(AbstractButton button) {
        ButtonModel model = button.getModel();
        Color base = (Color) button.getClientProperty("emc.background");
        Color rollover = (Color) button.getClientProperty("emc.rollover");
        Color pressed = (Color) button.getClientProperty("emc.pressed");
        Color foreground = (Color) button.getClientProperty("emc.foreground");
        if (!button.isEnabled()) {
            button.setBackground(blend(base, Color.WHITE, 0.58f));
            button.setForeground(TEXT_MUTED);
            return;
        }

        button.setForeground(foreground);
        if (model.isPressed()) {
            button.setBackground(pressed);
        } else if (model.isRollover()) {
            button.setBackground(rollover);
        } else {
            button.setBackground(base);
        }
    }

    private static Color blend(Color base, Color overlay, float ratio) {
        float clamped = Math.max(0f, Math.min(1f, ratio));
        int red = Math.round(base.getRed() * (1 - clamped) + overlay.getRed() * clamped);
        int green = Math.round(base.getGreen() * (1 - clamped) + overlay.getGreen() * clamped);
        int blue = Math.round(base.getBlue() * (1 - clamped) + overlay.getBlue() * clamped);
        return new Color(red, green, blue);
    }

    private static final class ButtonHoverListener implements ChangeListener {
        private final AbstractButton button;

        private ButtonHoverListener(AbstractButton button) {
            this.button = button;
        }

        @Override
        public void stateChanged(ChangeEvent event) {
            updateButtonState(button);
        }
    }

    private static final class HeaderRenderer extends DefaultTableCellRenderer {
        private HeaderRenderer() {
            setOpaque(true);
            setHorizontalAlignment(SwingConstants.LEFT);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, false, false, row, column);
            setBackground(PRIMARY_DARK);
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 12));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(9, 48, 78)),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)
            ));
            setText(value == null ? "" : value.toString());
            return this;
        }
    }

    private static final class StatusChipRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String status = value == null ? "" : value.toString();
            Color foreground = Color.WHITE;
            Color background = new Color(175, 183, 194);

            if (status.equalsIgnoreCase(TestStatus.PASSED.getDisplayName())) {
                background = SUCCESS;
            } else if (status.equalsIgnoreCase(TestStatus.ONGOING.getDisplayName())) {
                background = INFO;
            } else if (status.equalsIgnoreCase("Planned")) {
                background = PLANNED;
            } else if (status.equalsIgnoreCase(TestStatus.DATA_IN_ANALYSIS.getDisplayName())) {
                background = WARNING;
            } else if (status.equalsIgnoreCase(TestStatus.FAILED.getDisplayName())) {
                background = DANGER;
            } else if (status.equalsIgnoreCase(TestStatus.NOT_STARTED.getDisplayName())) {
                background = new Color(142, 154, 170);
            }

            setHorizontalAlignment(SwingConstants.CENTER);
            setForeground(foreground);
            setBackground(background);
            setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            setOpaque(true);
            return this;
        }
    }
}
