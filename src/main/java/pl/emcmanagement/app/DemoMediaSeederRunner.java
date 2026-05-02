package pl.emcmanagement.app;

import pl.emcmanagement.dao.DutMediaDao;
import pl.emcmanagement.dao.StepMediaDao;
import pl.emcmanagement.database.DbConnection;
import pl.emcmanagement.enums.DutMediaType;
import pl.emcmanagement.enums.StepMediaKind;
import pl.emcmanagement.enums.StepSetupSlot;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DemoMediaSeederRunner {
    private static final DutMediaDao DUT_MEDIA_DAO = new DutMediaDao();
    private static final StepMediaDao STEP_MEDIA_DAO = new StepMediaDao();

    private DemoMediaSeederRunner() {
    }

    public static void main(String[] args) throws Exception {
        String ewr = args.length > 0 ? args[0] : "EWR105176";
        DemoProjectData project = loadProject(ewr);
        if (project == null) {
            throw new IllegalStateException("Nie znaleziono projektu " + ewr);
        }

        Path outputRoot = Paths.get("out", "demo-media", ewr);
        Files.createDirectories(outputRoot);

        seedDutMedia(project, outputRoot.resolve("dut"));
        seedStepMedia(project, outputRoot.resolve("steps"));

        System.out.println(outputRoot.toAbsolutePath());
    }

    private static void seedDutMedia(DemoProjectData project, Path outputDir) throws IOException {
        Files.createDirectories(outputDir);
        for (int index = 0; index < project.samples().size(); index++) {
            DemoSample sample = project.samples().get(index);
            if (index == 0) {
                upsertDutMedia(sample, DutMediaType.FRONT_VIEW, outputDir, "front",
                        createDutPhoto(project, sample, "Front view", new Color(36, 86, 152), true));
                upsertDutMedia(sample, DutMediaType.BACK_VIEW, outputDir, "back",
                        createDutPhoto(project, sample, "Back view", new Color(54, 64, 95), true));
                upsertDutMedia(sample, DutMediaType.CONNECTOR_VIEW, outputDir, "connector",
                        createConnectorPhoto(project, sample));
            }
            upsertDutMedia(sample, DutMediaType.LABEL, outputDir, "label",
                    createLabelPhoto(project, sample));
        }
    }

    private static void seedStepMedia(DemoProjectData project, Path outputDir) throws IOException {
        Files.createDirectories(outputDir);
        for (DemoStep step : project.steps()) {
            Path stepDir = outputDir.resolve(step.legCode() + "-" + step.stepOrder());
            Files.createDirectories(stepDir);

            for (StepSetupSlot slot : StepSetupSlot.values()) {
                BufferedImage image = createSetupPhoto(project, step, slot);
                byte[] data = encodePng(image);
                Path file = stepDir.resolve(slot.getSlotCode().toLowerCase(Locale.ROOT) + ".png");
                Files.write(file, data);
                STEP_MEDIA_DAO.upsertMedia(
                        step.id(),
                        StepMediaKind.SETUP,
                        slot.getSlotCode(),
                        slot.getDisplayName(),
                        slot.getSortOrder(),
                        file.getFileName().toString(),
                        data
                );
            }

            for (int verificationIndex = 1; verificationIndex <= 2; verificationIndex++) {
                BufferedImage image = createWaveformCapture(project, step, verificationIndex);
                byte[] data = encodePng(image);
                String slotCode = String.format("VERIFICATION_%03d", verificationIndex);
                String displayName = "Verification waveform " + verificationIndex;
                Path file = stepDir.resolve(slotCode.toLowerCase(Locale.ROOT) + ".png");
                Files.write(file, data);
                STEP_MEDIA_DAO.upsertMedia(
                        step.id(),
                        StepMediaKind.VERIFICATION,
                        slotCode,
                        displayName,
                        verificationIndex,
                        file.getFileName().toString(),
                        data
                );
            }
        }
    }

    private static void upsertDutMedia(DemoSample sample,
                                       DutMediaType mediaType,
                                       Path outputDir,
                                       String suffix,
                                       BufferedImage image) throws IOException {
        byte[] data = encodePng(image);
        Path file = outputDir.resolve(sample.sampleCode() + "-" + suffix + ".png");
        Files.write(file, data);
        DUT_MEDIA_DAO.upsertMedia(sample.id(), mediaType, file.getFileName().toString(), data);
    }

    private static DemoProjectData loadProject(String ewrNumber) {
        String projectSql = """
                SELECT p.id, p.ewr_number, p.brand, p.device_name
                FROM projects p
                WHERE p.ewr_number = ?
                """;
        String samplesSql = """
                SELECT id, sample_code, serial_number
                FROM dut_samples
                WHERE project_id = ?
                ORDER BY sample_code
                """;
        String stepsSql = """
                SELECT s.id, l.leg_code, l.test_type, s.step_order, s.step_name, COALESCE(s.test_room, 'LabA') AS test_room
                FROM leg_test_steps s
                JOIN project_legs l ON l.id = s.leg_id
                WHERE l.project_id = ?
                ORDER BY l.leg_code, s.step_order
                """;

        try (Connection connection = DbConnection.getConnection();
             PreparedStatement projectStatement = connection.prepareStatement(projectSql)) {
            projectStatement.setString(1, ewrNumber);
            try (ResultSet projectResult = projectStatement.executeQuery()) {
                if (!projectResult.next()) {
                    return null;
                }
                int projectId = projectResult.getInt("id");
                List<DemoSample> samples = new ArrayList<>();
                List<DemoStep> steps = new ArrayList<>();

                try (PreparedStatement sampleStatement = connection.prepareStatement(samplesSql)) {
                    sampleStatement.setInt(1, projectId);
                    try (ResultSet sampleResult = sampleStatement.executeQuery()) {
                        while (sampleResult.next()) {
                            samples.add(new DemoSample(
                                    sampleResult.getInt("id"),
                                    sampleResult.getString("sample_code"),
                                    sampleResult.getString("serial_number")
                            ));
                        }
                    }
                }

                try (PreparedStatement stepStatement = connection.prepareStatement(stepsSql)) {
                    stepStatement.setInt(1, projectId);
                    try (ResultSet stepResult = stepStatement.executeQuery()) {
                        while (stepResult.next()) {
                            steps.add(new DemoStep(
                                    stepResult.getInt("id"),
                                    stepResult.getString("leg_code"),
                                    stepResult.getString("test_type"),
                                    stepResult.getInt("step_order"),
                                    stepResult.getString("step_name"),
                                    stepResult.getString("test_room")
                            ));
                        }
                    }
                }

                return new DemoProjectData(
                        projectId,
                        projectResult.getString("ewr_number"),
                        projectResult.getString("brand"),
                        projectResult.getString("device_name"),
                        samples,
                        steps
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas ladowania danych demo projektu.", e);
        }
    }

    private static BufferedImage createDutPhoto(DemoProjectData project,
                                                DemoSample sample,
                                                String viewName,
                                                Color accent,
                                                boolean showLaptop) {
        int width = 1800;
        int height = 1100;
        BufferedImage image = createCanvas(width, height, new Color(236, 241, 247));
        Graphics2D g = image.createGraphics();
        applyQuality(g);

        paintWorkshopBackdrop(g, width, height, accent);
        g.setColor(new Color(58, 66, 78));
        g.fill(new RoundRectangle2D.Float(190, 690, 420, 210, 28, 28));
        g.setColor(new Color(25, 33, 44));
        g.fillRect(240, 745, 320, 92);
        g.setColor(new Color(11, 86, 148));
        g.fillRect(255, 760, 290, 62);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Segoe UI", Font.BOLD, 34));
        g.drawString(project.brand(), 284, 805);

        if (showLaptop) {
            paintLaptop(g, 730, 360, 760, 460, accent, project, sample);
        }

        paintCableBundle(g, 505, 780, 745, 960);
        paintHeaderText(g, project.ewrNumber() + " | " + sample.sampleCode(), viewName, sample.serialNumber());
        g.dispose();
        return image;
    }

    private static BufferedImage createConnectorPhoto(DemoProjectData project, DemoSample sample) {
        int width = 1800;
        int height = 1100;
        BufferedImage image = createCanvas(width, height, new Color(242, 245, 249));
        Graphics2D g = image.createGraphics();
        applyQuality(g);

        GradientPaint background = new GradientPaint(0, 0, new Color(225, 232, 240), width, height, new Color(246, 248, 251));
        g.setPaint(background);
        g.fillRect(0, 0, width, height);

        g.setColor(new Color(56, 62, 74));
        g.fill(new RoundRectangle2D.Float(180, 240, 1440, 520, 40, 40));
        g.setColor(new Color(189, 196, 204));
        g.fill(new RoundRectangle2D.Float(250, 310, 1290, 380, 26, 26));
        g.setColor(new Color(54, 78, 118));
        for (int i = 0; i < 22; i++) {
            g.fillRoundRect(315 + i * 48, 480, 28, 84, 8, 8);
        }
        g.setColor(new Color(230, 171, 58));
        for (int i = 0; i < 8; i++) {
            g.fillRoundRect(348 + i * 120, 388, 20, 66, 8, 8);
        }
        paintHeaderText(g, sample.sampleCode() + " | Connector view", project.brand() + " harness interface", sample.serialNumber());
        g.dispose();
        return image;
    }

    private static BufferedImage createLabelPhoto(DemoProjectData project, DemoSample sample) {
        int width = 1300;
        int height = 950;
        BufferedImage image = createCanvas(width, height, Color.WHITE);
        Graphics2D g = image.createGraphics();
        applyQuality(g);

        g.setColor(new Color(235, 239, 244));
        g.fillRoundRect(80, 70, 1140, 800, 28, 28);
        g.setColor(new Color(34, 45, 64));
        g.setStroke(new BasicStroke(5f));
        g.drawRoundRect(80, 70, 1140, 800, 28, 28);

        g.setFont(new Font("Segoe UI", Font.BOLD, 54));
        g.drawString(project.brand(), 150, 185);
        g.setFont(new Font("Segoe UI", Font.BOLD, 46));
        g.drawString(project.deviceName().toUpperCase(Locale.ROOT), 150, 260);

        g.setFont(new Font("Consolas", Font.PLAIN, 34));
        g.drawString("Sample code: " + sample.sampleCode(), 150, 365);
        g.drawString("Serial no.: " + sample.serialNumber(), 150, 425);
        g.drawString("Project: " + project.ewrNumber(), 150, 485);
        g.drawString("Part no.: FCAM-" + sample.sampleCode().substring(3), 150, 545);
        g.drawString("Hardware rev.: A2", 150, 605);
        g.drawString("Software rev.: 25.4", 150, 665);

        paintQrLikeTag(g, 940, 160, 180, 180);
        paintFooterLabel(g, 150, 760);
        g.dispose();
        return image;
    }

    private static BufferedImage createSetupPhoto(DemoProjectData project, DemoStep step, StepSetupSlot slot) {
        int width = 1800;
        int height = 1050;
        BufferedImage image = createCanvas(width, height, new Color(229, 234, 239));
        Graphics2D g = image.createGraphics();
        applyQuality(g);

        Color accent = accentForTestType(step.testType());
        paintWorkshopBackdrop(g, width, height, accent);
        paintRoomLabel(g, step.testRoom(), accent, width);

        switch (slot) {
            case GENERAL_VIEW -> paintGeneralSetup(g, project, step, accent, width, height);
            case CLOSER_VIEW -> paintCloserSetup(g, project, step, accent, width, height);
            case DUT_VIEW -> paintDutCloseup(g, project, step, accent, width, height);
            case AUXILIARY_EQUIPMENT -> paintAuxEquipment(g, step, accent, width, height);
        }

        paintHeaderText(g, project.ewrNumber() + " | " + step.legCode() + " | Test " + step.stepOrder(), slot.getDisplayName(), step.stepName());
        g.dispose();
        return image;
    }

    private static BufferedImage createWaveformCapture(DemoProjectData project, DemoStep step, int index) {
        int width = 1800;
        int height = 1020;
        BufferedImage image = createCanvas(width, height, new Color(7, 16, 28));
        Graphics2D g = image.createGraphics();
        applyQuality(g);

        g.setColor(new Color(7, 16, 28));
        g.fillRect(0, 0, width, height);
        g.setColor(new Color(37, 58, 86));
        for (int x = 120; x <= width - 80; x += 80) {
            g.drawLine(x, 120, x, height - 120);
        }
        for (int y = 120; y <= height - 120; y += 70) {
            g.drawLine(120, y, width - 80, y);
        }

        g.setColor(Color.WHITE);
        g.setFont(new Font("Segoe UI", Font.BOLD, 42));
        g.drawString(project.brand() + " " + step.stepName() + " | Capture " + index, 120, 82);
        g.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        g.setColor(new Color(189, 200, 215));
        g.drawString("Applied level", width - 330, 120);
        g.drawString("Limit", width - 330, 158);

        g.setStroke(new BasicStroke(4f));
        g.setColor(new Color(232, 69, 74));
        g.drawLine(width - 410, 111, width - 350, 111);
        g.setColor(new Color(113, 219, 129));
        g.drawLine(width - 410, 149, width - 350, 149);

        Path2D signal = new Path2D.Float();
        signal.moveTo(120, 670);
        double base = 640 - step.stepOrder() * 28;
        for (int x = 120; x <= width - 80; x += 16) {
            double normalized = (x - 120d) / (width - 200d);
            double amplitude = 120 + (index * 20);
            double y = base
                    + Math.sin(normalized * 18d) * amplitude * 0.18d
                    + Math.sin(normalized * 57d) * amplitude * 0.11d
                    + Math.max(0, normalized - 0.35d) * 90d;
            signal.lineTo(x, y);
        }
        g.setColor(new Color(56, 182, 255));
        g.draw(signal);

        g.setColor(new Color(240, 201, 89));
        g.drawLine(120, 430, width - 80, 430);
        g.setFont(new Font("Segoe UI", Font.BOLD, 22));
        g.drawString(step.testType() + " | " + step.testRoom(), width - 300, height - 84);
        g.drawString(index == 1 ? "PASS window" : "Verification trace", width - 300, height - 54);
        g.dispose();
        return image;
    }

    private static BufferedImage createCanvas(int width, int height, Color background) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(background);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return image;
    }

    private static void applyQuality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private static void paintWorkshopBackdrop(Graphics2D g, int width, int height, Color accent) {
        GradientPaint gradient = new GradientPaint(0, 0, new Color(241, 245, 250), width, height, new Color(225, 231, 238));
        g.setPaint(gradient);
        g.fillRect(0, 0, width, height);

        g.setColor(new Color(198, 206, 214));
        g.fillRect(0, height - 230, width, 230);
        g.setColor(new Color(180, 188, 198));
        for (int i = 0; i < 18; i++) {
            g.drawLine(i * 120, height - 230, i * 150, height);
        }

        g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 28));
        for (int i = 0; i < 7; i++) {
            g.fillRoundRect(80 + i * 240, 120 + (i % 2) * 28, 120, 42, 18, 18);
        }
    }

    private static void paintLaptop(Graphics2D g,
                                    int x,
                                    int y,
                                    int width,
                                    int height,
                                    Color accent,
                                    DemoProjectData project,
                                    DemoSample sample) {
        g.setColor(new Color(36, 43, 56));
        g.fillRoundRect(x, y, width, height, 28, 28);
        g.setColor(new Color(18, 24, 34));
        g.fillRoundRect(x + 42, y + 44, width - 84, height - 138, 16, 16);

        g.setColor(new Color(29, 48, 83));
        g.fillRoundRect(x + 60, y + 64, width - 120, 120, 16, 16);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Segoe UI", Font.BOLD, 56));
        g.drawString(project.brand() + " FCAM", x + 100, y + 142);
        g.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        g.drawString(sample.sampleCode() + " | " + sample.serialNumber(), x + 100, y + 176);

        g.setColor(accent);
        g.fillRoundRect(x + 84, y + 222, 360, 190, 18, 18);
        g.setColor(new Color(92, 205, 132));
        g.fillOval(x + 110, y + 248, 92, 92);
        g.setColor(new Color(255, 202, 92));
        g.fillOval(x + 236, y + 284, 120, 76);
        g.setColor(new Color(88, 176, 255));
        g.fillRoundRect(x + 410, y + 222, 190, 190, 18, 18);

        g.setColor(new Color(39, 46, 58));
        g.fillRoundRect(x - 40, y + height - 28, width + 80, 36, 14, 14);
    }

    private static void paintCableBundle(Graphics2D g, int x1, int y1, int x2, int y2) {
        g.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(77, 89, 105));
        for (int i = 0; i < 6; i++) {
            g.drawLine(x1 + i * 10, y1 + i * 8, x2 + i * 16, y2 - i * 9);
        }
    }

    private static void paintHeaderText(Graphics2D g, String title, String subtitle, String detail) {
        g.setColor(new Color(22, 34, 50));
        g.setFont(new Font("Segoe UI", Font.BOLD, 54));
        g.drawString(title, 96, 92);
        g.setFont(new Font("Segoe UI", Font.BOLD, 34));
        g.drawString(subtitle, 96, 138);
        g.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        g.drawString(detail, 98, 176);
    }

    private static void paintRoomLabel(Graphics2D g, String room, Color accent, int width) {
        g.setColor(new Color(255, 255, 255, 210));
        g.fillRoundRect(width - 330, 68, 240, 62, 22, 22);
        g.setColor(accent);
        g.setStroke(new BasicStroke(3f));
        g.drawRoundRect(width - 330, 68, 240, 62, 22, 22);
        g.setFont(new Font("Segoe UI", Font.BOLD, 30));
        g.drawString(room, width - 250, 110);
    }

    private static void paintGeneralSetup(Graphics2D g, DemoProjectData project, DemoStep step, Color accent, int width, int height) {
        g.setColor(new Color(58, 66, 78));
        g.fillRoundRect(170, 290, 920, 520, 28, 28);
        g.setColor(new Color(32, 39, 49));
        g.fillOval(520, 620, 360, 100);
        g.setColor(new Color(169, 177, 186));
        g.fillRoundRect(540, 430, 330, 190, 22, 22);
        g.setColor(new Color(240, 243, 246));
        g.fillRoundRect(598, 470, 214, 108, 12, 12);
        g.setColor(new Color(54, 78, 118));
        g.fillRoundRect(1240, 270, 300, 420, 18, 18);
        g.setColor(new Color(225, 233, 245));
        for (int i = 0; i < 4; i++) {
            g.fillRoundRect(1275, 320 + i * 80, 230, 48, 10, 10);
        }
        g.setColor(accent);
        g.fillRoundRect(1130, 760, 470, 84, 24, 24);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Segoe UI", Font.BOLD, 28));
        g.drawString(step.stepName(), 1168, 814);
    }

    private static void paintCloserSetup(Graphics2D g, DemoProjectData project, DemoStep step, Color accent, int width, int height) {
        g.setColor(new Color(70, 78, 90));
        g.fillRoundRect(220, 270, 1320, 560, 36, 36);
        g.setColor(new Color(181, 187, 194));
        g.fillRoundRect(460, 350, 760, 320, 28, 28);
        g.setColor(new Color(248, 250, 252));
        g.fillRoundRect(540, 420, 600, 176, 18, 18);
        g.setColor(new Color(36, 86, 152));
        g.setFont(new Font("Segoe UI", Font.BOLD, 70));
        g.drawString(project.brand(), 600, 510);
        g.setColor(new Color(35, 43, 54));
        g.setFont(new Font("Consolas", Font.PLAIN, 44));
        g.drawString(project.deviceName().toUpperCase(Locale.ROOT), 600, 570);
        g.setColor(accent);
        g.setStroke(new BasicStroke(12f));
        g.drawLine(1260, 390, 1400, 600);
        g.drawLine(1400, 600, 1530, 760);
    }

    private static void paintDutCloseup(Graphics2D g, DemoProjectData project, DemoStep step, Color accent, int width, int height) {
        g.setColor(new Color(245, 247, 250));
        g.fillRoundRect(180, 240, 1440, 650, 40, 40);
        g.setColor(new Color(219, 226, 234));
        g.fillRoundRect(260, 320, 1240, 490, 32, 32);
        g.setColor(new Color(52, 60, 73));
        g.fillRoundRect(400, 380, 900, 250, 26, 26);
        g.setColor(new Color(188, 197, 209));
        g.fillRoundRect(460, 440, 780, 130, 18, 18);
        g.setColor(new Color(43, 86, 143));
        for (int i = 0; i < 16; i++) {
            g.fillRoundRect(520 + i * 42, 475, 18, 64, 8, 8);
        }
        g.setColor(accent);
        g.fillRoundRect(1040, 680, 360, 74, 22, 22);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Segoe UI", Font.BOLD, 28));
        g.drawString("Observed functional class: A", 1070, 727);
    }

    private static void paintAuxEquipment(Graphics2D g, DemoStep step, Color accent, int width, int height) {
        for (int i = 0; i < 3; i++) {
            int x = 220 + i * 430;
            g.setColor(new Color(60, 67, 79));
            g.fillRoundRect(x, 300, 280, 470, 20, 20);
            g.setColor(new Color(212, 221, 234));
            g.fillRoundRect(x + 22, 330, 236, 160, 16, 16);
            g.setColor(new Color(31, 41, 58));
            for (int line = 0; line < 4; line++) {
                g.fillRoundRect(x + 34, 540 + line * 40, 214, 18, 8, 8);
            }
            g.setColor(accent);
            g.fillOval(x + 182, 612, 34, 34);
        }
        g.setColor(new Color(244, 247, 250));
        g.fillRoundRect(1320, 330, 250, 300, 18, 18);
        g.setColor(new Color(36, 84, 148));
        g.setFont(new Font("Segoe UI", Font.BOLD, 28));
        g.drawString(step.stepName(), 1350, 410);
        g.drawString("Generator stack", 1350, 455);
        g.drawString(step.testRoom(), 1350, 500);
    }

    private static void paintQrLikeTag(Graphics2D g, int x, int y, int width, int height) {
        g.setColor(new Color(41, 47, 57));
        g.fillRect(x, y, width, height);
        g.setColor(Color.WHITE);
        for (int row = 0; row < 12; row++) {
            for (int col = 0; col < 12; col++) {
                if ((row + col) % 3 == 0 || row < 2 || col < 2 || row > 9 || col > 9) {
                    g.fillRect(x + 10 + col * 13, y + 10 + row * 13, 9, 9);
                }
            }
        }
    }

    private static void paintFooterLabel(Graphics2D g, int x, int y) {
        g.setColor(new Color(74, 84, 98));
        g.setFont(new Font("Segoe UI", Font.BOLD, 28));
        g.drawString("Validated for EMC Test Lab Manager demo package", x, y);
    }

    private static byte[] encodePng(BufferedImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private static Color accentForTestType(String testType) {
        if (testType == null) {
            return new Color(43, 86, 143);
        }
        return switch (testType) {
            case "EMC" -> new Color(43, 86, 143);
            case "ELE" -> new Color(183, 95, 46);
            case "FT" -> new Color(71, 146, 104);
            default -> new Color(43, 86, 143);
        };
    }

    private record DemoProjectData(
            int projectId,
            String ewrNumber,
            String brand,
            String deviceName,
            List<DemoSample> samples,
            List<DemoStep> steps
    ) {
    }

    private record DemoSample(int id, String sampleCode, String serialNumber) {
    }

    private record DemoStep(
            int id,
            String legCode,
            String testType,
            int stepOrder,
            String stepName,
            String testRoom
    ) {
    }
}
