package pl.emcmanagement.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;
import pl.emcmanagement.dao.ClimateLogDao;
import pl.emcmanagement.dao.DutMediaDao;
import pl.emcmanagement.dao.DutSampleDao;
import pl.emcmanagement.dao.DutResultRow;
import pl.emcmanagement.dao.DutTestResultDao;
import pl.emcmanagement.dao.LegDao;
import pl.emcmanagement.dao.LegTestStepDao;
import pl.emcmanagement.dao.MeasurementEquipmentDao;
import pl.emcmanagement.dao.StepMediaDao;
import pl.emcmanagement.enums.AccreditationStatus;
import pl.emcmanagement.enums.DutMediaType;
import pl.emcmanagement.enums.StepMediaKind;
import pl.emcmanagement.enums.TestStatus;
import pl.emcmanagement.model.ClimateDataset;
import pl.emcmanagement.model.ClimateMeasurement;
import pl.emcmanagement.model.DutMedia;
import pl.emcmanagement.model.DutSample;
import pl.emcmanagement.model.Leg;
import pl.emcmanagement.model.LegTestStep;
import pl.emcmanagement.model.MeasurementEquipment;
import pl.emcmanagement.model.Project;
import pl.emcmanagement.model.StepMedia;
import pl.emcmanagement.model.User;
import pl.emcmanagement.util.BrandLogoFactory;
import pl.emcmanagement.util.ClimateChartFactory;
import pl.emcmanagement.util.ImageStorageUtil;
import pl.emcmanagement.util.PcaLogoFactory;
import pl.emcmanagement.util.PkLogoFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

final class ProjectReportWriter {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter REPORT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final float TOC_LEFT = 42f;
    private static final float TOC_WIDTH = PDRectangle.A4.getWidth() - 84f;
    private static final float TOC_TITLE_Y = PDRectangle.A4.getHeight() - 56f;
    private static final float TOC_ENTRY_LINE_HEIGHT = 14f;
    private static final float TOC_ENTRY_SPACING = 2f;

    private final LegDao legDao;
    private final LegTestStepDao legTestStepDao;
    private final DutTestResultDao dutTestResultDao;
    private final MeasurementEquipmentDao measurementEquipmentDao;
    private final ClimateLogDao climateLogDao;
    private final DutSampleDao dutSampleDao;
    private final StepMediaDao stepMediaDao;
    private final DutMediaDao dutMediaDao;

    ProjectReportWriter(LegDao legDao,
                        LegTestStepDao legTestStepDao,
                        DutTestResultDao dutTestResultDao,
                        MeasurementEquipmentDao measurementEquipmentDao,
                        ClimateLogDao climateLogDao,
                        DutSampleDao dutSampleDao,
                        StepMediaDao stepMediaDao,
                        DutMediaDao dutMediaDao) {
        this.legDao = legDao;
        this.legTestStepDao = legTestStepDao;
        this.dutTestResultDao = dutTestResultDao;
        this.measurementEquipmentDao = measurementEquipmentDao;
        this.climateLogDao = climateLogDao;
        this.dutSampleDao = dutSampleDao;
        this.stepMediaDao = stepMediaDao;
        this.dutMediaDao = dutMediaDao;
    }

    Path generate(Project project) {
        if (project == null) {
            throw new IllegalArgumentException("No project was provided for PDF generation.");
        }

        List<LegReportData> legReports = loadLegReports(project);
        validateReportEligibility(legReports);
        List<DutSampleMediaBundle> dutMediaBundles = loadDutMedia(project);
        boolean hasAccreditedLeg = legReports.stream()
                .map(LegReportData::leg)
                .anyMatch(leg -> leg.getAccreditation() == AccreditationStatus.YES);
        boolean draft = isDraft(legReports);
        String reportReference = "EMC-" + safe(project.getEwrNumber()) + "-" + REPORT_DATE_FORMAT.format(LocalDate.now());
        int maxDutCount = legReports.stream()
                .flatMap(legReport -> legReport.steps().stream())
                .mapToInt(stepReport -> stepReport.results().size())
                .max()
                .orElse(0);

        Path outputDirectory = Path.of("generated-reports");
        try {
            Files.createDirectories(outputDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Could not create the report output directory.", e);
        }

        Path reportPath = outputDirectory.resolve(sanitizeFileName(reportReference + ".pdf"));
        try (PDDocument document = new PDDocument();
             ReportCanvas canvas = new ReportCanvas(document)) {
            int tocPageCount = estimateTableOfContentsPageCount(buildPlannedTableOfContentsTitles(legReports));
            List<TocEntry> tocEntries = new ArrayList<>();
            renderCover(canvas, project, legReports, reportReference, hasAccreditedLeg, draft, maxDutCount);
            tocEntries.add(new TocEntry("Cover page", 1));
            List<PDPage> tocPages = reserveTableOfContentsPages(canvas, tocPageCount);
            tocEntries.add(new TocEntry("Table of contents", 2));
            canvas.newPage();
            tocEntries.add(new TocEntry("DUT visual identification", canvas.getCurrentPageNumber()));
            renderDutGallery(canvas, project, dutMediaBundles);
            tocEntries.add(new TocEntry("Test result summary matrix", renderResultSummaryMatrix(canvas, project, dutMediaBundles, legReports)));
            for (LegReportData legReport : legReports) {
                for (StepReportData stepReport : legReport.steps()) {
                    canvas.newPage();
                    tocEntries.add(new TocEntry(
                            safe(legReport.leg().getLegCode()) + " | Test " + stepReport.step().getStepOrder() + ". " + safe(stepReport.step().getStepName()),
                            document.getNumberOfPages()
                    ));
                    renderStepSection(canvas, project, legReport.leg(), stepReport);
                }
            }
            canvas.newPage();
            tocEntries.add(new TocEntry("Final statement", document.getNumberOfPages()));
            renderFinalStatement(canvas, project, legReports, hasAccreditedLeg, draft);
            canvas.close();
            renderTableOfContents(document, tocPages, tocEntries);
            if (draft) {
                addDraftWatermark(document);
            }
            addFooters(document, reportReference);
            document.save(reportPath.toFile());
        } catch (IOException e) {
            throw new RuntimeException("Could not generate the PDF report.", e);
        }
        return reportPath;
    }

    private List<LegReportData> loadLegReports(Project project) {
        return legDao.findLegsByProjectId(project.getId()).stream()
                .map(leg -> new LegReportData(leg, loadStepReports(leg)))
                .toList();
    }

    private List<StepReportData> loadStepReports(Leg leg) {
        return legTestStepDao.findByLegId(leg.getId()).stream()
                .map(step -> {
                    List<MeasurementEquipment> equipment = measurementEquipmentDao.findByStepId(step.getId());
                    return new StepReportData(
                            step,
                            dutTestResultDao.findResultsByStepId(step.getId()),
                            equipment,
                            climateLogDao.findMeasurements(
                                    step.getTestRoom(),
                                    resolveClimateSensorCode(equipment),
                                    effectiveStepStart(leg, step),
                                    effectiveStepEnd(leg, step)
                            ),
                            stepMediaDao.findByStepIdAndKind(step.getId(), StepMediaKind.SETUP),
                            stepMediaDao.findByStepIdAndKind(step.getId(), StepMediaKind.VERIFICATION)
                    );
                })
                .toList();
    }

    private List<DutSampleMediaBundle> loadDutMedia(Project project) {
        List<DutSample> samples = dutSampleDao.findByProjectId(project.getId());
        List<DutMedia> media = dutMediaDao.findByProjectId(project.getId());
        return samples.stream()
                .map(sample -> {
                    List<DutMedia> sampleMedia = media.stream()
                            .filter(item -> item.getDutSampleId() == sample.getId())
                            .toList();
                    return new DutSampleMediaBundle(sample, sampleMedia);
                })
                .toList();
    }

    private LocalDate effectiveStepStart(Leg leg, LegTestStep step) {
        return step.getStartDate() != null ? step.getStartDate() : leg.getStartDate();
    }

    private LocalDate effectiveStepEnd(Leg leg, LegTestStep step) {
        if (step.getEndDate() != null) {
            return step.getEndDate();
        }
        if (leg.getEndDate() != null) {
            return leg.getEndDate();
        }
        LocalDate fallbackStart = effectiveStepStart(leg, step);
        return fallbackStart == null ? null : LocalDate.now();
    }

    private String resolveClimateSensorCode(List<MeasurementEquipment> equipment) {
        return equipment.stream()
                .map(MeasurementEquipment::getClimateSensorCode)
                .filter(code -> code != null && !code.isBlank())
                .map(String::trim)
                .findFirst()
                .orElse(null);
    }

    private void validateReportEligibility(List<LegReportData> legReports) {
        List<String> blockers = new ArrayList<>();
        for (LegReportData legReport : legReports) {
            if (!isAllowedReportStatus(legReport.leg().getStatus())) {
                blockers.add("LEG " + safe(legReport.leg().getLegCode()) + " has no status defined");
            }
            for (StepReportData stepReport : legReport.steps()) {
                if (!isAllowedReportStatus(stepReport.step().getStatus())) {
                    blockers.add("Test " + stepReport.step().getStepOrder() + ". " + safe(stepReport.step().getStepName())
                            + " has no status defined");
                }
                for (DutResultRow result : stepReport.results()) {
                    if (!isAllowedReportStatus(result.getResultStatus())) {
                        blockers.add("DUT " + safe(result.getSampleCode()) + " in test " + stepReport.step().getStepOrder()
                                + " has no result status defined");
                    }
                }
            }
        }
        if (!blockers.isEmpty()) {
            throw new IllegalStateException(
                    "PDF generation requires every LEG, test and DUT row to have a defined status.\n- "
                            + String.join("\n- ", blockers)
            );
        }
    }

    private boolean isAllowedReportStatus(TestStatus status) {
        return status != null;
    }

    private boolean isDraft(List<LegReportData> legReports) {
        for (LegReportData legReport : legReports) {
            if (!isFinalReportStatus(legReport.leg().getStatus())) {
                return true;
            }
            for (StepReportData stepReport : legReport.steps()) {
                if (!isFinalReportStatus(stepReport.step().getStatus())) {
                    return true;
                }
                boolean anyDraftDut = stepReport.results().stream()
                        .anyMatch(row -> !isFinalReportStatus(row.getResultStatus()));
                if (anyDraftDut) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isFinalReportStatus(TestStatus status) {
        return status == TestStatus.PASSED || status == TestStatus.FAILED;
    }

    private void renderCover(ReportCanvas canvas,
                             Project project,
                             List<LegReportData> legReports,
                             String reportReference,
                             boolean hasAccreditedLeg,
                             boolean draft,
                             int maxDutCount) throws IOException {
        BufferedImage pkLogo = toBufferedImage(PkLogoFactory.createLogo(280, 100));
        BufferedImage brandLogo = toBufferedImage(BrandLogoFactory.createLogo(project.getBrand(), 180, 96));
        BufferedImage pcaLogo = hasAccreditedLeg ? PcaLogoFactory.createImage(120, 188) : null;

        canvas.drawImage(pkLogo, canvas.getLeft(), canvas.getCursorY(), 220f, 70f);
        if (brandLogo != null) {
            canvas.drawImage(brandLogo, canvas.getRight() - 130f, canvas.getCursorY(), 130f, 70f);
        }
        canvas.moveDown(86f);

        canvas.drawTitle("EMC TEST REPORT");
        canvas.drawParagraph(
                "This document was generated by EMC Management System as a consolidated laboratory report for the selected EWR project.",
                PDType1Font.HELVETICA,
                11.5f,
                ReportCanvas.TEXT_MUTED
        );
        canvas.drawSpacer(8f);
        canvas.drawStatusBadge(draft ? "DRAFT" : displayStatus(project.getStatus()), canvas.statusColor(project.getStatus(), draft));
        canvas.drawSpacer(14f);

        List<String[]> projectFacts = new ArrayList<>();
        projectFacts.add(new String[]{"Report reference", reportReference});
        projectFacts.add(new String[]{"Issue date", formatDate(LocalDate.now())});
        projectFacts.add(new String[]{"Project / EWR", safe(project.getEwrNumber()) + " | " + buildProjectTitle(project)});
        projectFacts.add(new String[]{"Client / brand", safe(project.getBrand())});
        projectFacts.add(new String[]{"Device under test", joinNonBlank(project.getDeviceName(), project.getShortDescription())});
        projectFacts.add(new String[]{"Project status", displayStatus(project.getStatus())});
        projectFacts.add(new String[]{"Project dates", formatRange(project.getStartDate(), project.getEndDate())});
        projectFacts.add(new String[]{"Maximum DUT count", String.valueOf(maxDutCount)});
        projectFacts.add(new String[]{"VE", userSummary(project.getVe())});
        projectFacts.add(new String[]{"TE", userSummary(project.getTe())});
        projectFacts.add(new String[]{"TT", joinUsers(project.getTtUsers())});
        projectFacts.add(new String[]{"LEG count", String.valueOf(legReports.size())});
        projectFacts.add(new String[]{"Accreditation scope", hasAccreditedLeg
                ? "The project contains accredited LEG scope. PCA mark is shown only for accredited scope."
                : "No accredited LEG scope is stored in the project."});
        canvas.drawFactTable(projectFacts);

        canvas.drawSpacer(12f);
        canvas.drawSection("Report scope");
        canvas.drawParagraph(
                "The report contains project identification, personnel assignment, applicable standards, test timing, DUT results, equipment reservations and climatic conditions captured for every reported test step.",
                PDType1Font.HELVETICA,
                10.5f,
                ReportCanvas.TEXT_PRIMARY
        );

        if (pcaLogo != null) {
            canvas.drawSpacer(8f);
            canvas.drawSection("Accreditation mark");
            float topY = canvas.getCursorY();
            float logoWidth = 58f;
            float logoHeight = 90f;
            float textX = canvas.getLeft() + logoWidth + 18f;
            float textWidth = canvas.getContentWidth() - logoWidth - 18f;

            canvas.drawImage(pcaLogo, canvas.getLeft(), topY, logoWidth, logoHeight);
            canvas.drawParagraphAt(
                    "Authorized by " + safe(project.getTe() == null ? null : project.getTe().getFullName()),
                    PDType1Font.HELVETICA_BOLD,
                    10.5f,
                    textX,
                    topY - 2f,
                    textWidth,
                    ReportCanvas.TEXT_PRIMARY
            );
            canvas.drawParagraphAt(
                    generateSignature(project.getTe()),
                    PDType1Font.HELVETICA_OBLIQUE,
                    16f,
                    textX,
                    topY - 24f,
                    textWidth,
                    new Color(29, 74, 126)
            );

            List<String> noteLines = canvas.wrapText(
                    "The PCA badge is displayed because at least one LEG is marked as accredited. The badge applies only to the scope explicitly identified as accredited.",
                    PDType1Font.HELVETICA,
                    8.8f,
                    textWidth
            );
            float noteHeight = noteLines.size() * (8.8f + ReportCanvas.LINE_GAP);
            canvas.drawParagraphAt(
                    "The PCA badge is displayed because at least one LEG is marked as accredited. The badge applies only to the scope explicitly identified as accredited.",
                    PDType1Font.HELVETICA,
                    8.8f,
                    textX,
                    topY - 42f,
                    textWidth,
                    ReportCanvas.TEXT_MUTED
            );
            canvas.moveDown(Math.max(logoHeight, 42f + noteHeight) + 10f);
        }
    }

    private void renderDutGallery(ReportCanvas canvas,
                                  Project project,
                                  List<DutSampleMediaBundle> dutMediaBundles) throws IOException {
        canvas.drawSection("2. DUT visual identification");

        List<DutSampleMediaBundle> bundlesWithMedia = dutMediaBundles.stream()
                .filter(bundle -> !bundle.media().isEmpty())
                .toList();
        if (bundlesWithMedia.isEmpty()) {
            canvas.drawParagraph(
                    "No DUT photographs are stored in the database for this project.",
                    PDType1Font.HELVETICA,
                    10f,
                    ReportCanvas.TEXT_MUTED
            );
        } else {
            DutSampleMediaBundle primaryBundle = bundlesWithMedia.get(0);
            List<ImageBlock> primaryBlocks = new ArrayList<>();
            addDutImageBlock(primaryBlocks, primaryBundle, DutMediaType.FRONT_VIEW);
            addDutImageBlock(primaryBlocks, primaryBundle, DutMediaType.BACK_VIEW);
            addDutImageBlock(primaryBlocks, primaryBundle, DutMediaType.CONNECTOR_VIEW);
            addDutImageBlock(primaryBlocks, primaryBundle, DutMediaType.LABEL);
            renderImageGrid(canvas, primaryBlocks, 2, 120f);

            List<ImageBlock> labelBlocks = new ArrayList<>();
            for (int index = 1; index < bundlesWithMedia.size(); index++) {
                addDutImageBlock(labelBlocks, bundlesWithMedia.get(index), DutMediaType.LABEL);
            }
            if (!labelBlocks.isEmpty()) {
                float firstRowHeight = computeImageGridFirstRowHeight(canvas, labelBlocks, 2, 112f, 9.5f);
                canvas.ensureSpace(8f + 40f + Math.max(24f, firstRowHeight));
                canvas.drawSpacer(8f);
                canvas.drawSection("3. Remaining DUT labels");
                renderImageGrid(canvas, labelBlocks, 2, 112f);
            }
        }

    }

    private int renderResultSummaryMatrix(ReportCanvas canvas,
                                          Project project,
                                          List<DutSampleMediaBundle> dutMediaBundles,
                                          List<LegReportData> legReports) throws IOException {
        List<DutSample> projectSamples = dutMediaBundles.stream()
                .map(DutSampleMediaBundle::sample)
                .toList();

        canvas.drawSpacer(10f);
        if (projectSamples.isEmpty()) {
            canvas.ensureSpace(38f);
            int startPage = canvas.getCurrentPageNumber();
            canvas.drawSection("4. Test result summary matrix");
            canvas.drawParagraph("No DUT samples are stored for this project.", PDType1Font.HELVETICA, 9.5f, ReportCanvas.TEXT_MUTED);
            return startPage;
        }

        String[] headers = new String[projectSamples.size() + 1];
        float[] widths = new float[projectSamples.size() + 1];
        headers[0] = "Test";
        widths[0] = 0.42f;
        float dutWidth = projectSamples.isEmpty() ? 0.58f : 0.58f / projectSamples.size();
        for (int index = 0; index < projectSamples.size(); index++) {
            headers[index + 1] = projectSamples.get(index).getSampleCode();
            widths[index + 1] = dutWidth;
        }

        List<String[]> rows = new ArrayList<>();
        for (LegReportData legReport : legReports) {
            for (StepReportData stepReport : legReport.steps()) {
                Map<String, TestStatus> statusBySample = new HashMap<>();
                for (DutResultRow row : stepReport.results()) {
                    statusBySample.put(row.getSampleCode(), row.getResultStatus());
                }
                String[] row = new String[headers.length];
                row[0] = safe(legReport.leg().getLegCode()) + " | " + stepReport.step().getStepOrder() + ". " + safe(stepReport.step().getStepName());
                for (int index = 0; index < projectSamples.size(); index++) {
                    row[index + 1] = summaryMatrixSymbol(statusBySample.get(projectSamples.get(index).getSampleCode()));
                }
                rows.add(row);
            }
        }

        int startPage = renderSectionTable(
                canvas,
                0f,
                "4. Test result summary matrix",
                headers,
                widths,
                rows,
                9f,
                9f
        );
        canvas.drawSpacer(6f);
        canvas.drawOverallResultSummary("Results summary", displayStatus(project.getStatus()), project.getStatus());
        canvas.drawSpacer(10f);
        canvas.drawParagraph(
                "Legend: P = Passed, F = Failed, A = Data in analysis, - = no DUT result recorded for the selected test.",
                PDType1Font.HELVETICA,
                8.8f,
                ReportCanvas.TEXT_MUTED
        );
        return startPage;
    }

    private void renderExecutiveSummary(ReportCanvas canvas,
                                        Project project,
                                        List<LegReportData> legReports,
                                        String reportReference,
                                        boolean hasAccreditedLeg,
                                        boolean draft) throws IOException {
        canvas.drawSection("1. Project overview");
        List<String[]> overviewRows = new ArrayList<>();
        overviewRows.add(new String[]{"Report reference", reportReference});
        overviewRows.add(new String[]{"System", "Politechnika Krakowska | EMC Management System"});
        overviewRows.add(new String[]{"Project", safe(project.getEwrNumber()) + " | " + buildProjectTitle(project)});
        overviewRows.add(new String[]{"Overall status", draft ? "Data in analysis / Draft report" : displayStatus(project.getStatus())});
        overviewRows.add(new String[]{"Project schedule", formatRange(project.getStartDate(), project.getEndDate())});
        overviewRows.add(new String[]{"VE", userSummary(project.getVe())});
        overviewRows.add(new String[]{"TE", userSummary(project.getTe())});
        overviewRows.add(new String[]{"TT", joinUsers(project.getTtUsers())});
        overviewRows.add(new String[]{"Accreditation", hasAccreditedLeg ? "Accredited scope present in selected project." : "No accredited scope in selected project."});
        canvas.drawFactTable(overviewRows);

        canvas.drawSpacer(12f);
        canvas.drawSection("2. LEG overview");
        List<String[]> legRows = new ArrayList<>();
        for (LegReportData legReport : legReports) {
            Leg leg = legReport.leg();
            legRows.add(new String[]{
                    safe(leg.getLegCode()),
                    leg.getTestType() == null ? "---" : leg.getTestType().name(),
                    displayStatus(leg.getStatus()),
                    leg.getAccreditation() == AccreditationStatus.YES ? "YES" : "NO",
                    String.valueOf(leg.getDutCount()),
                    safe(leg.getAssignedTt() == null ? null : leg.getAssignedTt().getFullName()),
                    formatDate(leg.getStartDate()),
                    formatDate(leg.getEndDate())
            });
        }
        canvas.drawTable(
                new String[]{"LEG", "Type", "Status", "Accred.", "DUT", "Assigned TT", "Start", "End"},
                new float[]{0.10f, 0.08f, 0.16f, 0.09f, 0.06f, 0.22f, 0.145f, 0.145f},
                legRows,
                9.5f,
                9.5f
        );

        canvas.drawSpacer(12f);
        canvas.drawSection("3. Standards and supporting documents");
        List<String[]> standardRows = new ArrayList<>();
        for (LegReportData legReport : legReports) {
            Leg leg = legReport.leg();
            standardRows.add(new String[]{
                    safe(leg.getLegCode()),
                    safe(leg.getIsoStandardName()),
                    safe(leg.getClientStandardName()),
                    safe(leg.getTestPlanName()),
                    safe(leg.getPcaUrl())
            });
        }
        canvas.drawTable(
                new String[]{"LEG", "ISO standard", "Client standard", "Test plan", "PCA reference"},
                new float[]{0.10f, 0.20f, 0.26f, 0.24f, 0.20f},
                standardRows,
                9.5f,
                9.5f
        );
    }

    private void renderStepSection(ReportCanvas canvas,
                                   Project project,
                                   Leg leg,
                                   StepReportData stepReport) throws IOException {
        LegTestStep step = stepReport.step();
        boolean accredited = leg.getAccreditation() == AccreditationStatus.YES;

        canvas.drawTitle(safe(leg.getLegCode()) + " | Test " + step.getStepOrder() + ". " + safe(step.getStepName()));
        canvas.drawStatusBadge(displayStepStatus(step), canvas.statusColor(step.getStatus(), false));
        canvas.drawSpacer(8f);

        canvas.drawSection("1. Test identification");
        List<String[]> idRows = new ArrayList<>();
        idRows.add(new String[]{"Project / EWR", safe(project.getEwrNumber()) + " | " + buildProjectTitle(project)});
        idRows.add(new String[]{"LEG", safe(leg.getLegCode())});
        idRows.add(new String[]{"Test type", leg.getTestType() == null ? "---" : leg.getTestType().name()});
        idRows.add(new String[]{"Test step", step.getStepOrder() + ". " + safe(step.getStepName())});
        idRows.add(new String[]{"Status", displayStepStatus(step)});
        idRows.add(new String[]{"Schedule", formatRange(effectiveStepStart(leg, step), effectiveStepEnd(leg, step))});
        idRows.add(new String[]{"Laboratory room", safe(step.getTestRoom())});
        idRows.add(new String[]{"Accredited scope", accredited ? "YES" : "NO"});
        idRows.add(new String[]{"VE", userSummary(project.getVe())});
        idRows.add(new String[]{"TE", userSummary(project.getTe())});
        idRows.add(new String[]{"Assigned TT", safe(leg.getAssignedTt() == null ? null : leg.getAssignedTt().getFullName())});
        idRows.add(new String[]{"ISO standard", safe(leg.getIsoStandardName())});
        idRows.add(new String[]{"Client standard", safe(leg.getClientStandardName())});
        idRows.add(new String[]{"Test plan", safe(leg.getTestPlanName())});
        canvas.drawFactTable(idRows);

        renderDutResultsTable(canvas, stepReport.results());
        renderEquipmentTable(canvas, stepReport.equipment());
        renderClimateSummaryTable(canvas, stepReport);
        renderStepMediaSection(canvas, "5. Test setup photographs", stepReport.setupMedia());
        renderStepMediaSection(canvas, "6. Verification captures", stepReport.verificationMedia());
    }

    private void renderDutResultsTable(ReportCanvas canvas, List<DutResultRow> rows) throws IOException {
        List<String[]> tableRows = new ArrayList<>();
        for (DutResultRow row : rows) {
            tableRows.add(new String[]{
                    safe(row.getSampleCode()),
                    safe(row.getSerialNumber()),
                    row.getObservedFunctionalClass() == null ? "---" : row.getObservedFunctionalClass().getDisplayName(),
                    displayStatus(row.getResultStatus()),
                    formatDate(row.getExecutionDate()),
                    safe(row.getComment())
            });
        }
        renderSectionTable(
                canvas,
                10f,
                "2. DUT results",
                new String[]{"DUT", "Serial No.", "Observed class", "Result", "Date", "Comment"},
                new float[]{0.14f, 0.16f, 0.15f, 0.12f, 0.10f, 0.33f},
                tableRows,
                9.2f,
                9.1f
        );
    }

    private void renderEquipmentTable(ReportCanvas canvas, List<MeasurementEquipment> equipment) throws IOException {
        List<String[]> rows = new ArrayList<>();
        for (MeasurementEquipment item : equipment) {
            rows.add(new String[]{
                    safe(item.getEquipmentCode()),
                    safe(item.getEquipmentName()),
                    safe(item.getCategory()),
                    formatDate(item.getReservedFrom()),
                    formatDate(item.getReservedTo()),
                    safe(item.getLocation()),
                    item.isLabOwned() ? "YES" : "NO",
                    item.getCalibrationValidUntil() == null ? "---" : formatDate(item.getCalibrationValidUntil())
            });
        }
        renderSectionTable(
                canvas,
                10f,
                "3. Measurement equipment",
                new String[]{"Code", "Equipment", "Category", "From", "To", "Location", "Owned", "Calibration"},
                new float[]{0.14f, 0.21f, 0.09f, 0.12f, 0.12f, 0.10f, 0.09f, 0.13f},
                rows,
                8.2f,
                8.0f
        );
    }

    private void renderClimateSummaryTable(ReportCanvas canvas, StepReportData stepReport) throws IOException {
        ClimateDataset dataset = stepReport.climateDataset();
        ClimateStats stats = ClimateStats.from(dataset.getMeasurements());
        List<String[]> rows = new ArrayList<>();

        String sourceLabel = safe(dataset.getSourceDescription() == null ? stepReport.step().getTestRoom() : dataset.getSourceDescription());
        if (!dataset.getMeasurements().isEmpty()) {
            rows.add(new String[]{
                    sourceLabel,
                    String.valueOf(dataset.getMeasurements().size()),
                    formatNumber(stats.temperatureMin()),
                    formatNumber(stats.temperatureAvg()),
                    formatNumber(stats.temperatureMax()),
                    formatNumber(stats.humidityMin()),
                    formatNumber(stats.humidityAvg()),
                    formatNumber(stats.humidityMax())
            });
        }
        renderSectionTable(
                canvas,
                10f,
                "4. Climatic conditions",
                new String[]{"Source", "Samples", "Temp min\n[\u00B0C]", "Temp avg\n[\u00B0C]", "Temp max\n[\u00B0C]", "RH min\n[%]", "RH avg\n[%]", "RH max\n[%]"},
                new float[]{0.17f, 0.09f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.14f},
                rows,
                8.6f,
                8.5f
        );

        if (!dataset.getMeasurements().isEmpty()) {
            canvas.drawSpacer(6f);
            BufferedImage chart = ClimateChartFactory.createCombinedChartImage(
                    "Temperature / humidity trend",
                    dataset.getMeasurements(),
                    1480,
                    560
            );
            ScaledImageDimensions chartDimensions = scaleImageWithin(chart, canvas.getContentWidth(), 220f);
            float chartBlockHeight = chartDimensions.height() + 8f;
            canvas.ensureSpace(chartBlockHeight);
            float topY = canvas.getCursorY();
            canvas.drawImageFrame(canvas.getLeft(), topY, chartDimensions.width(), chartDimensions.height());
            canvas.drawImage(chart, canvas.getLeft(), topY, canvas.getContentWidth(), 220f);
            canvas.moveDown(chartBlockHeight);
        }

        canvas.drawSpacer(4f);
        canvas.drawParagraph(
                "Source files: " + (dataset.getSourceFilenames().isEmpty()
                        ? "---"
                        : dataset.getSourceFilenames().stream().sorted().collect(Collectors.joining(", "))),
                PDType1Font.HELVETICA,
                7.6f,
                ReportCanvas.TEXT_MUTED
        );
    }

    private int renderSectionTable(ReportCanvas canvas,
                                   float spacer,
                                   String title,
                                   String[] headers,
                                   float[] widthFractions,
                                   List<String[]> rows,
                                   float bodyFontSize,
                                   float headerFontSize) throws IOException {
        float[] columnWidths = canvas.scaleColumns(widthFractions, canvas.getContentWidth());
        float headerHeight = canvas.computeRowHeight(headers, columnWidths, PDType1Font.HELVETICA_BOLD, headerFontSize);
        float firstRowHeight = rows == null || rows.isEmpty()
                ? canvas.computeRowHeight(
                new String[]{"No data available"},
                new float[]{canvas.getContentWidth()},
                PDType1Font.HELVETICA,
                bodyFontSize
        )
                : canvas.computeRowHeight(rows.get(0), columnWidths, PDType1Font.HELVETICA, bodyFontSize);
        float tableBodyHeight = 0f;
        if (rows == null || rows.isEmpty()) {
            tableBodyHeight = canvas.computeRowHeight(
                    new String[]{"No data available"},
                    new float[]{canvas.getContentWidth()},
                    PDType1Font.HELVETICA,
                    bodyFontSize
            );
        } else {
            for (String[] row : rows) {
                tableBodyHeight += canvas.computeRowHeight(row, columnWidths, PDType1Font.HELVETICA, bodyFontSize);
            }
        }
        float fullSectionHeight = spacer + 28f + headerHeight + tableBodyHeight + 8f;
        float usablePageHeight = ReportCanvas.PAGE_SIZE.getHeight() - ReportCanvas.MARGIN_TOP - ReportCanvas.MARGIN_BOTTOM;
        if (fullSectionHeight <= usablePageHeight) {
            canvas.ensureSpace(fullSectionHeight);
        } else {
            canvas.ensureSpace(spacer + 28f + headerHeight + firstRowHeight + 8f);
        }
        int startPage = canvas.getCurrentPageNumber();
        canvas.drawSpacer(spacer);
        canvas.drawSection(title);
        canvas.drawTable(headers, widthFractions, rows, bodyFontSize, headerFontSize);
        return startPage;
    }

    private List<String> buildPlannedTableOfContentsTitles(List<LegReportData> legReports) {
        List<String> titles = new ArrayList<>();
        titles.add("Cover page");
        titles.add("Table of contents");
        titles.add("DUT visual identification");
        titles.add("Test result summary matrix");
        for (LegReportData legReport : legReports) {
            for (StepReportData stepReport : legReport.steps()) {
                titles.add(safe(legReport.leg().getLegCode()) + " | Test " + stepReport.step().getStepOrder() + ". " + safe(stepReport.step().getStepName()));
            }
        }
        titles.add("Final statement");
        return titles;
    }

    private int estimateTableOfContentsPageCount(List<String> titles) throws IOException {
        int pageCount = 1;
        float y = TOC_TITLE_Y - 28f;
        for (String title : titles) {
            List<String> lines = wrapForOverlay(title, PDType1Font.HELVETICA, 10.5f, TOC_WIDTH - 42f);
            float entryHeight = lines.size() * TOC_ENTRY_LINE_HEIGHT + TOC_ENTRY_SPACING;
            if (y - entryHeight < ReportCanvas.MARGIN_BOTTOM) {
                pageCount++;
                y = TOC_TITLE_Y - 28f;
            }
            y -= entryHeight;
        }
        return Math.max(1, pageCount);
    }

    private List<PDPage> reserveTableOfContentsPages(ReportCanvas canvas, int pageCount) throws IOException {
        List<PDPage> pages = new ArrayList<>();
        for (int index = 0; index < pageCount; index++) {
            canvas.newPage();
            pages.add(canvas.page);
        }
        return pages;
    }

    private float drawTableOfContentsHeader(PDPageContentStream stream, int pageIndex) throws IOException {
        stream.beginText();
        stream.setFont(PDType1Font.HELVETICA_BOLD, 18f);
        stream.setNonStrokingColor(28, 38, 50);
        stream.newLineAtOffset(TOC_LEFT, TOC_TITLE_Y);
        stream.showText(pageIndex == 0 ? "1. Table of contents" : "1. Table of contents (cont.)");
        stream.endText();

        stream.setNonStrokingColor(17, 69, 113);
        stream.addRect(TOC_LEFT, TOC_TITLE_Y - 9f, 170f, 2.2f);
        stream.fill();
        return TOC_TITLE_Y - 28f;
    }

    private void renderStepMediaSection(ReportCanvas canvas, String title, List<StepMedia> media) throws IOException {
        if (media == null || media.isEmpty()) {
            canvas.drawSpacer(10f);
            canvas.drawSection(title);
            canvas.drawParagraph("No files uploaded for this section.", PDType1Font.HELVETICA, 9.5f, ReportCanvas.TEXT_MUTED);
            return;
        }

        float sectionImageMaxHeight = ReportCanvas.PAGE_SIZE.getHeight()
                - ReportCanvas.MARGIN_TOP
                - ReportCanvas.MARGIN_BOTTOM
                - 110f;

        BufferedImage firstImage = media.stream()
                .map(item -> toBufferedImage(item.getFileData()))
                .filter(image -> image != null)
                .findFirst()
                .orElse(null);
        float firstBlockHeight = 0f;
        if (firstImage != null) {
            ScaledImageDimensions firstDimensions = scaleImageWithin(firstImage, canvas.getContentWidth(), sectionImageMaxHeight);
            int captionLines = canvas.wrapText(
                    media.stream()
                            .map(StepMedia::getDisplayName)
                            .filter(name -> name != null && !name.isBlank())
                            .findFirst()
                            .orElse("---"),
                    PDType1Font.HELVETICA_BOLD,
                    10f,
                    canvas.getContentWidth()
            ).size();
            firstBlockHeight = firstDimensions.height() + 14f + captionLines * (10f + ReportCanvas.LINE_GAP) + 12f;
        }

        canvas.ensureSpace(10f + 28f + Math.max(28f, firstBlockHeight));
        canvas.drawSpacer(10f);
        canvas.drawSection(title);

        for (StepMedia item : media) {
            BufferedImage image = toBufferedImage(item.getFileData());
            if (image == null) {
                continue;
            }
            ScaledImageDimensions dimensions = scaleImageWithin(image, canvas.getContentWidth(), sectionImageMaxHeight);
            List<String> captionLines = canvas.wrapText(item.getDisplayName(), PDType1Font.HELVETICA_BOLD, 10f, canvas.getContentWidth());
            float captionHeight = captionLines.size() * (10f + ReportCanvas.LINE_GAP);
            float blockHeight = dimensions.height() + 14f + captionHeight + 12f;
            canvas.ensureSpace(blockHeight);
            float topY = canvas.getCursorY();
            canvas.drawImageFrame(canvas.getLeft(), topY, dimensions.width(), dimensions.height());
            if (isBlankOrTransparent(image)) {
                canvas.drawFramePlaceholder(
                        canvas.getLeft(),
                        topY,
                        dimensions.width(),
                        dimensions.height(),
                        "Stored image is transparent or blank"
                );
            } else {
                canvas.drawImage(image, canvas.getLeft(), topY, canvas.getContentWidth(), sectionImageMaxHeight);
            }
            canvas.drawParagraphAt(
                    item.getDisplayName(),
                    PDType1Font.HELVETICA_BOLD,
                    10f,
                    canvas.getLeft(),
                    topY - dimensions.height() - 12f,
                    canvas.getContentWidth(),
                    ReportCanvas.TEXT_PRIMARY
            );
            canvas.moveDown(blockHeight);
        }
    }

    private void renderFinalStatement(ReportCanvas canvas,
                                      Project project,
                                      List<LegReportData> legReports,
                                      boolean hasAccreditedLeg,
                                      boolean draft) throws IOException {
        long passedLegs = legReports.stream()
                .filter(legReport -> legReport.leg().getStatus() == TestStatus.PASSED)
                .count();
        long failedLegs = legReports.stream()
                .filter(legReport -> legReport.leg().getStatus() == TestStatus.FAILED)
                .count();
        long analysisLegs = legReports.stream()
                .filter(legReport -> legReport.leg().getStatus() == TestStatus.DATA_IN_ANALYSIS)
                .count();

        long passedResults = legReports.stream()
                .flatMap(legReport -> legReport.steps().stream())
                .flatMap(stepReport -> stepReport.results().stream())
                .filter(row -> row.getResultStatus() == TestStatus.PASSED)
                .count();
        long failedResults = legReports.stream()
                .flatMap(legReport -> legReport.steps().stream())
                .flatMap(stepReport -> stepReport.results().stream())
                .filter(row -> row.getResultStatus() == TestStatus.FAILED)
                .count();
        long draftResults = legReports.stream()
                .flatMap(legReport -> legReport.steps().stream())
                .flatMap(stepReport -> stepReport.results().stream())
                .filter(row -> row.getResultStatus() == TestStatus.DATA_IN_ANALYSIS)
                .count();

        canvas.drawTitle("Report conclusion");
        canvas.drawStatusBadge(draft ? "DRAFT" : displayStatus(project.getStatus()), canvas.statusColor(project.getStatus(), draft));
        canvas.drawSpacer(10f);

        canvas.drawSection("1. Final statement");
        canvas.drawParagraph(
                "This report consolidates the records stored in EMC Management System for the selected project, including personnel assignment, test scope, DUT results, equipment reservations and climatic conditions linked to every reported test.",
                PDType1Font.HELVETICA,
                11f,
                ReportCanvas.TEXT_PRIMARY
        );

        List<String[]> summaryRows = new ArrayList<>();
        summaryRows.add(new String[]{"Project / EWR", safe(project.getEwrNumber()) + " | " + buildProjectTitle(project)});
        summaryRows.add(new String[]{"Project result", draft ? "Data in analysis / Draft report" : displayStatus(project.getStatus())});
        summaryRows.add(new String[]{"Passed LEG", String.valueOf(passedLegs)});
        summaryRows.add(new String[]{"Failed LEG", String.valueOf(failedLegs)});
        summaryRows.add(new String[]{"LEG in analysis", String.valueOf(analysisLegs)});
        summaryRows.add(new String[]{"Passed DUT results", String.valueOf(passedResults)});
        summaryRows.add(new String[]{"Failed DUT results", String.valueOf(failedResults)});
        summaryRows.add(new String[]{"DUT results in analysis", String.valueOf(draftResults)});
        summaryRows.add(new String[]{"Generated on", formatDate(LocalDate.now())});
        summaryRows.add(new String[]{"Accreditation note", hasAccreditedLeg
                ? "PCA mark is shown only for LEG scope flagged as accredited."
                : "No accredited LEG scope is included in this report."});
        canvas.drawFactTable(summaryRows);

        canvas.drawSpacer(12f);
        canvas.drawSection("2. Release conditions");
        canvas.drawParagraph(
                draft
                        ? "The report contains at least one record that is not yet in a final state. Therefore the report is marked as DRAFT on every page and should be treated as an in-progress laboratory record."
                        : "All reported LEG and DUT records are in final states. The report may be used as a complete internal project package for the selected EMC campaign.",
                PDType1Font.HELVETICA,
                10.5f,
                ReportCanvas.TEXT_PRIMARY
        );
    }

    private void renderTableOfContents(PDDocument document, List<PDPage> tocPages, List<TocEntry> tocEntries) throws IOException {
        if (tocPages == null || tocPages.isEmpty()) {
            return;
        }

        int pageIndex = 0;
        PDPageContentStream stream = new PDPageContentStream(document, tocPages.get(pageIndex), AppendMode.APPEND, true, true);
        float y = drawTableOfContentsHeader(stream, pageIndex);
        try {
            for (TocEntry entry : tocEntries) {
                List<String> lines = wrapForOverlay(entry.title(), PDType1Font.HELVETICA, 10.5f, TOC_WIDTH - 42f);
                float entryHeight = lines.size() * TOC_ENTRY_LINE_HEIGHT + TOC_ENTRY_SPACING;
                if (y - entryHeight < ReportCanvas.MARGIN_BOTTOM) {
                    stream.close();
                    pageIndex++;
                    if (pageIndex >= tocPages.size()) {
                        throw new IllegalStateException("Reserved table-of-contents pages were insufficient.");
                    }
                    stream = new PDPageContentStream(document, tocPages.get(pageIndex), AppendMode.APPEND, true, true);
                    y = drawTableOfContentsHeader(stream, pageIndex);
                }

                for (int index = 0; index < lines.size(); index++) {
                    String line = lines.get(index);
                    stream.beginText();
                    stream.setFont(PDType1Font.HELVETICA, 10.5f);
                    stream.setNonStrokingColor(28, 38, 50);
                    stream.newLineAtOffset(TOC_LEFT, y);
                    stream.showText(line);
                    stream.endText();

                    if (index == 0) {
                        String pageNumber = String.valueOf(entry.pageNumber());
                        float pageWidth = textWidth(PDType1Font.HELVETICA_BOLD, 10.5f, pageNumber);
                        stream.beginText();
                        stream.setFont(PDType1Font.HELVETICA_BOLD, 10.5f);
                        stream.newLineAtOffset(TOC_LEFT + TOC_WIDTH - pageWidth, y);
                        stream.showText(pageNumber);
                        stream.endText();
                    }
                    y -= TOC_ENTRY_LINE_HEIGHT;
                }
                y -= TOC_ENTRY_SPACING;
            }
        } finally {
            stream.close();
        }
    }

    private void addDraftWatermark(PDDocument document) throws IOException {
        for (PDPage page : document.getPages()) {
            try (PDPageContentStream stream = new PDPageContentStream(document, page, AppendMode.APPEND, true, true)) {
                PDExtendedGraphicsState state = new PDExtendedGraphicsState();
                state.setNonStrokingAlphaConstant(0.11f);
                stream.setGraphicsStateParameters(state);
                stream.setNonStrokingColor(165, 172, 180);
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA_BOLD, 88f);
                float centerX = page.getMediaBox().getWidth() / 2f;
                float centerY = page.getMediaBox().getHeight() / 2f;
                stream.setTextMatrix(Matrix.getRotateInstance(Math.toRadians(42), centerX, centerY));
                float offset = textWidth(PDType1Font.HELVETICA_BOLD, 88f, "DRAFT") / 2f;
                stream.newLineAtOffset(-offset, 0f);
                stream.showText("DRAFT");
                stream.endText();
            }
        }
    }

    private void addFooters(PDDocument document, String reportReference) throws IOException {
        int pageCount = document.getNumberOfPages();
        for (int index = 0; index < pageCount; index++) {
            PDPage page = document.getPage(index);
            try (PDPageContentStream stream = new PDPageContentStream(document, page, AppendMode.APPEND, true, true)) {
                stream.setFont(PDType1Font.HELVETICA, 8.5f);
                stream.setNonStrokingColor(98, 111, 126);

                stream.beginText();
                stream.newLineAtOffset(42f, 20f);
                stream.showText(reportReference);
                stream.endText();

                String pageText = "Page " + (index + 1) + " / " + pageCount;
                float width = textWidth(PDType1Font.HELVETICA, 8.5f, pageText);
                stream.beginText();
                stream.newLineAtOffset(page.getMediaBox().getWidth() - 42f - width, 20f);
                stream.showText(pageText);
                stream.endText();
            }
        }
    }

    private BufferedImage toBufferedImage(Icon icon) {
        if (icon == null) {
            return null;
        }
        int width = Math.max(1, icon.getIconWidth());
        int height = Math.max(1, icon.getIconHeight());
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        icon.paintIcon(null, g2, 0, 0);
        g2.dispose();
        return image;
    }

    private BufferedImage toBufferedImage(byte[] content) {
        if (content == null || content.length == 0) {
            return null;
        }
        try {
            return ImageStorageUtil.decodeImage(content);
        } catch (IOException e) {
            return null;
        }
    }

    private void addDutImageBlock(List<ImageBlock> target, DutSampleMediaBundle bundle, DutMediaType mediaType) {
        bundle.media().stream()
                .filter(media -> media.getMediaType() == mediaType)
                .findFirst()
                .ifPresent(media -> {
                    BufferedImage image = toBufferedImage(media.getFileData());
                    if (image != null) {
                        target.add(new ImageBlock(
                                bundle.sample().getSampleCode() + " " + mediaType.getDisplayName(),
                                image
                        ));
                    }
                });
    }

    private void renderImageGrid(ReportCanvas canvas,
                                 List<ImageBlock> blocks,
                                 int columns,
                                 float maxImageHeight) throws IOException {
        if (blocks == null || blocks.isEmpty()) {
            canvas.drawParagraph("No files uploaded.", PDType1Font.HELVETICA, 9.5f, ReportCanvas.TEXT_MUTED);
            return;
        }

        float gap = 14f;
        float columnWidth = (canvas.getContentWidth() - gap * (columns - 1)) / columns;
        for (int index = 0; index < blocks.size(); index += columns) {
            int count = Math.min(columns, blocks.size() - index);
            float rowWidth = count * columnWidth + Math.max(0, count - 1) * gap;
            float rowStartX = canvas.getLeft() + (canvas.getContentWidth() - rowWidth) / 2f;
            List<ScaledImageDimensions> dimensions = new ArrayList<>();
            float rowHeight = 0f;
            for (int offset = 0; offset < count; offset++) {
                ImageBlock block = blocks.get(index + offset);
                ScaledImageDimensions scaled = scaleImageWithin(block.image(), columnWidth, maxImageHeight);
                dimensions.add(scaled);
                float captionHeight = canvas.wrapText(block.caption(), PDType1Font.HELVETICA_BOLD, 9.5f, columnWidth).size()
                        * (9.5f + ReportCanvas.LINE_GAP);
                rowHeight = Math.max(rowHeight, scaled.height() + 16f + captionHeight + 14f);
            }
            canvas.ensureSpace(rowHeight);
            float topY = canvas.getCursorY();
            for (int offset = 0; offset < count; offset++) {
                ImageBlock block = blocks.get(index + offset);
                float columnLeft = rowStartX + offset * (columnWidth + gap);
                ScaledImageDimensions scaled = dimensions.get(offset);
                float imageX = columnLeft + (columnWidth - scaled.width()) / 2f;
                canvas.drawImageFrame(imageX, topY, scaled.width(), scaled.height());
                if (isBlankOrTransparent(block.image())) {
                    canvas.drawFramePlaceholder(imageX, topY, scaled.width(), scaled.height(), "Stored image is transparent or blank");
                } else {
                    canvas.drawImage(block.image(), imageX, topY, scaled.width(), scaled.height());
                }
                canvas.drawParagraphAt(
                        block.caption(),
                        PDType1Font.HELVETICA_BOLD,
                        9.5f,
                        columnLeft,
                        topY - scaled.height() - 16f,
                        columnWidth,
                        ReportCanvas.TEXT_PRIMARY
                );
            }
            canvas.moveDown(rowHeight);
        }
    }

    private float computeImageGridFirstRowHeight(ReportCanvas canvas,
                                                 List<ImageBlock> blocks,
                                                 int columns,
                                                 float maxImageHeight,
                                                 float captionFontSize) throws IOException {
        if (blocks == null || blocks.isEmpty()) {
            return 24f;
        }
        float gap = 14f;
        float columnWidth = (canvas.getContentWidth() - gap * (columns - 1)) / columns;
        int count = Math.min(columns, blocks.size());
        float rowHeight = 0f;
        for (int offset = 0; offset < count; offset++) {
            ImageBlock block = blocks.get(offset);
            ScaledImageDimensions scaled = scaleImageWithin(block.image(), columnWidth, maxImageHeight);
            float captionHeight = canvas.wrapText(block.caption(), PDType1Font.HELVETICA_BOLD, captionFontSize, columnWidth).size()
                    * (captionFontSize + ReportCanvas.LINE_GAP);
            rowHeight = Math.max(rowHeight, scaled.height() + 18f + captionHeight + 14f);
        }
        return rowHeight;
    }

    private ScaledImageDimensions scaleImageWithin(BufferedImage image, float maxWidth, float maxHeight) {
        if (image == null) {
            return new ScaledImageDimensions(0f, 0f);
        }
        float scale = Math.min(maxWidth / image.getWidth(), maxHeight / image.getHeight());
        return new ScaledImageDimensions(image.getWidth() * scale, image.getHeight() * scale);
    }

    private boolean isBlankOrTransparent(BufferedImage image) {
        if (image == null) {
            return true;
        }
        int visibleSamples = 0;
        int samples = 0;
        int stepX = Math.max(1, image.getWidth() / 48);
        int stepY = Math.max(1, image.getHeight() / 48);
        for (int y = 0; y < image.getHeight(); y += stepY) {
            for (int x = 0; x < image.getWidth(); x += stepX) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xFF;
                int red = (argb >>> 16) & 0xFF;
                int green = (argb >>> 8) & 0xFF;
                int blue = argb & 0xFF;
                samples++;
                if (alpha > 18 && (red < 248 || green < 248 || blue < 248)) {
                    visibleSamples++;
                }
            }
        }
        return samples == 0 || ((double) visibleSamples / (double) samples) < 0.01d;
    }

    private String summaryMatrixSymbol(TestStatus status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case PASSED -> "P";
            case FAILED -> "F";
            case DATA_IN_ANALYSIS -> "A";
            default -> "-";
        };
    }

    private String generateSignature(User teUser) {
        String name = teUser == null ? "Authorized TE" : safe(teUser.getFullName());
        return "/" + name.replace(' ', '_') + "/";
    }

    private List<String> wrapForOverlay(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : safe(text).split("\\s+")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (textWidth(font, fontSize, candidate) <= maxWidth) {
                current.setLength(0);
                current.append(candidate);
            } else {
                if (!current.isEmpty()) {
                    lines.add(current.toString());
                }
                current.setLength(0);
                current.append(word);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines.isEmpty() ? List.of("---") : lines;
    }

    private String buildProjectTitle(Project project) {
        return safe(joinNonBlank(project.getBrand(), project.getDeviceName(), project.getShortDescription()));
    }

    private String userSummary(User user) {
        return user == null ? "---" : safe(user.getFullName()) + " | " + safe(user.getEmail());
    }

    private String joinUsers(List<User> users) {
        if (users == null || users.isEmpty()) {
            return "---";
        }
        return users.stream()
                .map(this::userSummary)
                .collect(Collectors.joining("; "));
    }

    private String formatRange(LocalDate startDate, LocalDate endDate) {
        return formatDate(startDate) + " - " + formatDate(endDate);
    }

    private String formatDate(LocalDate date) {
        return date == null ? "---" : DATE_FORMAT.format(date);
    }

    private String formatNumber(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private String displayStatus(TestStatus status) {
        return status == null ? "---" : status.getDisplayName();
    }

    private String displayStepStatus(LegTestStep step) {
        if (step == null || step.getStatus() == null) {
            return "---";
        }
        if (step.getStatus() == TestStatus.NOT_STARTED && step.getStartDate() != null && step.getEndDate() != null) {
            return "Planned";
        }
        return step.getStatus().getDisplayName();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "---" : value.trim();
    }

    private String joinNonBlank(String... values) {
        String joined = Arrays.stream(values)
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" "));
        return joined.isBlank() ? "---" : joined;
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[\\\\/:*?\"<>|]+", "_");
    }

    private static float textWidth(PDFont font, float fontSize, String text) throws IOException {
        return font.getStringWidth(text) / 1000f * fontSize;
    }

    private record StepReportData(
            LegTestStep step,
            List<DutResultRow> results,
            List<MeasurementEquipment> equipment,
            ClimateDataset climateDataset,
            List<StepMedia> setupMedia,
            List<StepMedia> verificationMedia
    ) {
    }

    private record LegReportData(Leg leg, List<StepReportData> steps) {
    }

    private record DutSampleMediaBundle(DutSample sample, List<DutMedia> media) {
    }

    private record TocEntry(String title, int pageNumber) {
    }

    private record ImageBlock(String caption, BufferedImage image) {
    }

    private record ScaledImageDimensions(float width, float height) {
    }

    private record ClimateStats(
            double temperatureMin,
            double temperatureAvg,
            double temperatureMax,
            double humidityMin,
            double humidityAvg,
            double humidityMax
    ) {
        private static ClimateStats from(List<ClimateMeasurement> measurements) {
            if (measurements == null || measurements.isEmpty()) {
                return new ClimateStats(0d, 0d, 0d, 0d, 0d, 0d);
            }
            double tempMin = measurements.stream().mapToDouble(ClimateMeasurement::getTemperature).min().orElse(0d);
            double tempMax = measurements.stream().mapToDouble(ClimateMeasurement::getTemperature).max().orElse(0d);
            double tempAvg = measurements.stream().mapToDouble(ClimateMeasurement::getTemperature).average().orElse(0d);
            double humidityMin = measurements.stream().mapToDouble(ClimateMeasurement::getHumidity).min().orElse(0d);
            double humidityMax = measurements.stream().mapToDouble(ClimateMeasurement::getHumidity).max().orElse(0d);
            double humidityAvg = measurements.stream().mapToDouble(ClimateMeasurement::getHumidity).average().orElse(0d);
            return new ClimateStats(tempMin, tempAvg, tempMax, humidityMin, humidityAvg, humidityMax);
        }
    }

    private static final class ReportCanvas implements Closeable {
        private static final PDRectangle PAGE_SIZE = PDRectangle.A4;
        private static final float MARGIN_LEFT = 42f;
        private static final float MARGIN_RIGHT = 42f;
        private static final float MARGIN_TOP = 42f;
        private static final float MARGIN_BOTTOM = 34f;
        private static final float LINE_GAP = 3f;
        private static final float CELL_PADDING_X = 5f;
        private static final float CELL_PADDING_Y = 5f;

        private static final Color TEXT_PRIMARY = new Color(28, 38, 50);
        private static final Color TEXT_MUTED = new Color(98, 111, 126);
        private static final Color HEADER_FILL = new Color(17, 69, 113);
        private static final Color TABLE_ALT = new Color(244, 248, 252);
        private static final Color BORDER = new Color(212, 222, 232);

        private final PDDocument document;
        private PDPage page;
        private PDPageContentStream stream;
        private float cursorY;

        private ReportCanvas(PDDocument document) throws IOException {
            this.document = document;
            newPage();
        }

        private void newPage() throws IOException {
            closeStream();
            page = new PDPage(PAGE_SIZE);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            cursorY = PAGE_SIZE.getHeight() - MARGIN_TOP;
        }

        private void ensureSpace(float height) throws IOException {
            if (cursorY - height < MARGIN_BOTTOM) {
                newPage();
            }
        }

        private float getLeft() {
            return MARGIN_LEFT;
        }

        private float getRight() {
            return PAGE_SIZE.getWidth() - MARGIN_RIGHT;
        }

        private float getContentWidth() {
            return getRight() - getLeft();
        }

        private float getCursorY() {
            return cursorY;
        }

        private int getCurrentPageNumber() {
            return document.getNumberOfPages();
        }

        private void moveDown(float amount) {
            cursorY -= amount;
        }

        private void drawSpacer(float amount) {
            cursorY -= amount;
        }

        private void drawTitle(String text) throws IOException {
            List<String> lines = wrapText(text, PDType1Font.HELVETICA_BOLD, 24f, getContentWidth());
            ensureSpace(lines.size() * 28f + 8f);
            for (String line : lines) {
                drawText(line, PDType1Font.HELVETICA_BOLD, 24f, getLeft(), cursorY, TEXT_PRIMARY);
                cursorY -= 28f;
            }
            cursorY -= 2f;
        }

        private void drawSection(String text) throws IOException {
            ensureSpace(28f);
            drawText(text, PDType1Font.HELVETICA_BOLD, 15f, getLeft(), cursorY, TEXT_PRIMARY);
            cursorY -= 7f;
            stream.setNonStrokingColor(17, 69, 113);
            stream.addRect(getLeft(), cursorY - 2f, Math.min(getContentWidth(), 190f), 2.2f);
            stream.fill();
            cursorY -= 12f;
        }

        private void drawStatusBadge(String text, Color background) throws IOException {
            ensureSpace(28f);
            float width = Math.max(96f, textWidth(PDType1Font.HELVETICA_BOLD, 12f, safeText(text)) + 22f);
            float height = 22f;
            float y = cursorY - height + 4f;
            stream.setNonStrokingColor(background);
            stream.addRect(getLeft(), y, width, height);
            stream.fill();
            drawText(safeText(text), PDType1Font.HELVETICA_BOLD, 12f, getLeft() + 11f, y + 7f, Color.WHITE);
            cursorY -= 28f;
        }

        private void drawFactTable(List<String[]> rows) throws IOException {
            drawTable(
                    new String[]{"Field", "Value"},
                    new float[]{0.24f, 0.76f},
                    rows,
                    9.8f,
                    9.8f
            );
        }

        private void drawOverallResultSummary(String label, String value, TestStatus status) throws IOException {
            float labelWidth = getContentWidth() * 0.32f;
            float valueWidth = getContentWidth() - labelWidth;
            float[] widths = new float[]{labelWidth, valueWidth};
            String[] values = new String[]{label, value};
            float rowHeight = computeRowHeight(values, widths, PDType1Font.HELVETICA_BOLD, 10.5f);

            ensureSpace(rowHeight + 2f);
            float y = cursorY - rowHeight;

            stream.setNonStrokingColor(TABLE_ALT);
            stream.addRect(getLeft(), y, labelWidth, rowHeight);
            stream.fill();

            CellRenderStyle valueStyle = resolveCellStyle(value);
            stream.setNonStrokingColor(valueStyle.background() == null ? Color.WHITE : valueStyle.background());
            stream.addRect(getLeft() + labelWidth, y, valueWidth, rowHeight);
            stream.fill();

            stream.setStrokingColor(BORDER);
            stream.addRect(getLeft(), y, labelWidth, rowHeight);
            stream.stroke();
            stream.addRect(getLeft() + labelWidth, y, valueWidth, rowHeight);
            stream.stroke();

            drawCell(label, getLeft(), y, labelWidth, rowHeight, PDType1Font.HELVETICA_BOLD, 10.5f, TEXT_PRIMARY, false);
            drawCell(
                    value,
                    getLeft() + labelWidth,
                    y,
                    valueWidth,
                    rowHeight,
                    PDType1Font.HELVETICA_BOLD,
                    10.5f,
                    valueStyle.text() == null ? TEXT_PRIMARY : valueStyle.text(),
                    true
            );
            cursorY = y;
        }

        private void drawParagraph(String text, PDFont font, float fontSize, Color color) throws IOException {
            List<String> lines = wrapText(text, font, fontSize, getContentWidth());
            float lineHeight = fontSize + LINE_GAP;
            ensureSpace(lines.size() * lineHeight + 2f);
            for (String line : lines) {
                drawText(line, font, fontSize, getLeft(), cursorY, color);
                cursorY -= lineHeight;
            }
        }

        private void drawParagraphAt(String text,
                                     PDFont font,
                                     float fontSize,
                                     float x,
                                     float topY,
                                     float width,
                                     Color color) throws IOException {
            List<String> lines = wrapText(text, font, fontSize, width);
            float lineHeight = fontSize + LINE_GAP;
            float y = topY;
            for (String line : lines) {
                drawText(line, font, fontSize, x, y, color);
                y -= lineHeight;
            }
        }

        private void drawTable(String[] headers,
                               float[] widthFractions,
                               List<String[]> rows,
                               float bodyFontSize,
                               float headerFontSize) throws IOException {
            float[] columnWidths = scaleColumns(widthFractions, getContentWidth());
            float headerHeight = computeRowHeight(headers, columnWidths, PDType1Font.HELVETICA_BOLD, headerFontSize);
            float minimumBlockHeight = headerHeight + computeMinimalRowHeight(rows, columnWidths, bodyFontSize) + 8f;

            ensureSpace(minimumBlockHeight);
            drawTableHeader(headers, columnWidths, headerFontSize);

            if (rows.isEmpty()) {
                drawBodyRow(new String[]{"No data available"}, new float[]{getContentWidth()}, 0, bodyFontSize);
                cursorY -= 8f;
                return;
            }

            for (int index = 0; index < rows.size(); index++) {
                float rowHeight = computeRowHeight(rows.get(index), columnWidths, PDType1Font.HELVETICA, bodyFontSize);
                if (cursorY - rowHeight < MARGIN_BOTTOM) {
                    newPage();
                    drawTableHeader(headers, columnWidths, headerFontSize);
                }
                drawBodyRow(rows.get(index), columnWidths, index, bodyFontSize);
            }
            cursorY -= 8f;
        }

        private float computeMinimalRowHeight(List<String[]> rows, float[] columnWidths, float bodyFontSize) throws IOException {
            if (rows.isEmpty()) {
                return computeRowHeight(new String[]{"No data available"}, new float[]{getContentWidth()}, PDType1Font.HELVETICA, bodyFontSize);
            }
            return computeRowHeight(rows.get(0), columnWidths, PDType1Font.HELVETICA, bodyFontSize);
        }

        private void drawTableHeader(String[] headers, float[] columnWidths, float fontSize) throws IOException {
            float headerHeight = computeRowHeight(headers, columnWidths, PDType1Font.HELVETICA_BOLD, fontSize);
            ensureSpace(headerHeight);
            float y = cursorY - headerHeight;
            float x = getLeft();

            stream.setNonStrokingColor(HEADER_FILL);
            stream.addRect(getLeft(), y, getContentWidth(), headerHeight);
            stream.fill();

            for (int index = 0; index < headers.length; index++) {
                drawCell(headers[index], x, y, columnWidths[index], headerHeight, PDType1Font.HELVETICA_BOLD, fontSize, Color.WHITE, false);
                x += columnWidths[index];
            }

            stream.setStrokingColor(BORDER);
            stream.addRect(getLeft(), y, getContentWidth(), headerHeight);
            stream.stroke();
            cursorY = y;
        }

        private void drawBodyRow(String[] row, float[] columnWidths, int rowIndex, float fontSize) throws IOException {
            float rowHeight = computeRowHeight(row, columnWidths, PDType1Font.HELVETICA, fontSize);
            float y = cursorY - rowHeight;
            float x = getLeft();

            stream.setNonStrokingColor(rowIndex % 2 == 0 ? TABLE_ALT : Color.WHITE);
            stream.addRect(getLeft(), y, getContentWidth(), rowHeight);
            stream.fill();

            for (int column = 0; column < columnWidths.length; column++) {
                String value = column < row.length ? row[column] : "---";
                CellRenderStyle style = resolveCellStyle(value);
                if (style.background() != null) {
                    stream.setNonStrokingColor(style.background());
                    stream.addRect(x, y, columnWidths[column], rowHeight);
                    stream.fill();
                }
                drawCell(
                        value,
                        x,
                        y,
                        columnWidths[column],
                        rowHeight,
                        PDType1Font.HELVETICA,
                        fontSize,
                        style.text() == null ? TEXT_PRIMARY : style.text(),
                        style.centered()
                );
                stream.setStrokingColor(BORDER);
                stream.addRect(x, y, columnWidths[column], rowHeight);
                stream.stroke();
                x += columnWidths[column];
            }
            cursorY = y;
        }

        private void drawCell(String text,
                              float x,
                              float y,
                              float width,
                              float height,
                              PDFont font,
                              float fontSize,
                              Color color,
                              boolean centered) throws IOException {
            float innerWidth = Math.max(10f, width - 2 * CELL_PADDING_X);
            List<String> lines = wrapText(text, font, fontSize, innerWidth);
            float lineHeight = fontSize + LINE_GAP;
            float textY = centered && lines.size() == 1
                    ? y + ((height - fontSize) / 2f) + 1f
                    : y + height - CELL_PADDING_Y - fontSize;

            for (String line : lines) {
                float textX = x + CELL_PADDING_X;
                if (centered) {
                    float lineWidth = textWidth(font, fontSize, line);
                    textX = x + Math.max(CELL_PADDING_X, (width - lineWidth) / 2f);
                }
                drawText(line, font, fontSize, textX, textY, color);
                textY -= lineHeight;
            }
        }

        private void drawImage(BufferedImage image, float x, float topY, float maxWidth, float maxHeight) throws IOException {
            if (image == null) {
                return;
            }
            float scale = Math.min(maxWidth / image.getWidth(), maxHeight / image.getHeight());
            float drawWidth = image.getWidth() * scale;
            float drawHeight = image.getHeight() * scale;
            var pdImage = org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory.createFromImage(document, image);
            stream.drawImage(pdImage, x, topY - drawHeight, drawWidth, drawHeight);
        }

        private void drawImageFrame(float x, float topY, float width, float height) throws IOException {
            float y = topY - height;
            stream.setNonStrokingColor(new Color(232, 237, 242));
            stream.addRect(x, y, width, height);
            stream.fill();
            stream.setStrokingColor(BORDER);
            stream.addRect(x, y, width, height);
            stream.stroke();
        }

        private void drawFramePlaceholder(float x, float topY, float width, float height, String text) throws IOException {
            float innerWidth = Math.max(40f, width - 16f);
            List<String> lines = wrapText(text, PDType1Font.HELVETICA_OBLIQUE, 9f, innerWidth);
            float totalHeight = lines.size() * (9f + LINE_GAP);
            float textY = topY - (height / 2f) + (totalHeight / 2f);
            drawParagraphAt(text, PDType1Font.HELVETICA_OBLIQUE, 9f, x + 8f, textY, innerWidth, TEXT_MUTED);
        }

        private void drawText(String text, PDFont font, float fontSize, float x, float y, Color color) throws IOException {
            stream.beginText();
            stream.setFont(font, fontSize);
            stream.setNonStrokingColor(color);
            stream.newLineAtOffset(x, y);
            stream.showText(safeText(text));
            stream.endText();
        }

        private float computeRowHeight(String[] row, float[] columnWidths, PDFont font, float fontSize) throws IOException {
            float maxLines = 1f;
            for (int index = 0; index < columnWidths.length; index++) {
                String value = index < row.length ? row[index] : "---";
                float innerWidth = Math.max(10f, columnWidths[index] - 2 * CELL_PADDING_X);
                maxLines = Math.max(maxLines, wrapText(value, font, fontSize, innerWidth).size());
            }
            return maxLines * (fontSize + LINE_GAP) + 2 * CELL_PADDING_Y + 2f;
        }

        private float[] scaleColumns(float[] widthFractions, float totalWidth) {
            float sum = 0f;
            for (float fraction : widthFractions) {
                sum += fraction;
            }
            float[] widths = new float[widthFractions.length];
            for (int index = 0; index < widthFractions.length; index++) {
                widths[index] = totalWidth * (widthFractions[index] / sum);
            }
            return widths;
        }

        private List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
            String normalized = safeText(text).replace("\r", "");
            List<String> lines = new ArrayList<>();
            for (String paragraph : normalized.split("\n", -1)) {
                if (paragraph.isBlank()) {
                    lines.add("");
                    continue;
                }
                StringBuilder currentLine = new StringBuilder();
                for (String word : paragraph.trim().split("\\s+")) {
                    if (textWidth(font, fontSize, word) > maxWidth) {
                        if (!currentLine.isEmpty()) {
                            lines.add(currentLine.toString());
                            currentLine.setLength(0);
                        }
                        lines.addAll(splitLongWord(word, font, fontSize, maxWidth));
                        continue;
                    }
                    String candidate = currentLine.isEmpty() ? word : currentLine + " " + word;
                    if (textWidth(font, fontSize, candidate) <= maxWidth) {
                        currentLine.setLength(0);
                        currentLine.append(candidate);
                    } else {
                        if (!currentLine.isEmpty()) {
                            lines.add(currentLine.toString());
                        }
                        currentLine.setLength(0);
                        currentLine.append(word);
                    }
                }
                if (!currentLine.isEmpty()) {
                    lines.add(currentLine.toString());
                }
            }
            return lines.isEmpty() ? List.of("---") : lines;
        }

        private List<String> splitLongWord(String word, PDFont font, float fontSize, float maxWidth) throws IOException {
            List<String> lines = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            for (int index = 0; index < word.length(); index++) {
                String next = current + String.valueOf(word.charAt(index));
                if (!current.isEmpty() && textWidth(font, fontSize, next) > maxWidth) {
                    lines.add(current.toString());
                    current.setLength(0);
                }
                current.append(word.charAt(index));
            }
            if (!current.isEmpty()) {
                lines.add(current.toString());
            }
            return lines;
        }

        private CellRenderStyle resolveCellStyle(String value) {
            String normalized = safeText(value).trim();
            return switch (normalized) {
                case "P", "Passed" -> new CellRenderStyle(new Color(82, 170, 111), Color.WHITE, true);
                case "F", "Failed" -> new CellRenderStyle(new Color(201, 79, 79), Color.WHITE, true);
                case "A", "Data in analysis" -> new CellRenderStyle(new Color(214, 151, 49), Color.WHITE, true);
                default -> new CellRenderStyle(null, null, false);
            };
        }

        private Color statusColor(TestStatus status, boolean draft) {
            if (draft) {
                return new Color(214, 151, 49);
            }
            if (status == null) {
                return new Color(142, 154, 170);
            }
            return switch (status) {
                case PASSED -> new Color(72, 168, 106);
                case ONGOING -> new Color(63, 131, 203);
                case DATA_IN_ANALYSIS -> new Color(214, 151, 49);
                case FAILED -> new Color(190, 74, 69);
                case NOT_STARTED -> new Color(142, 154, 170);
            };
        }

        @Override
        public void close() throws IOException {
            closeStream();
        }

        private void closeStream() throws IOException {
            if (stream != null) {
                stream.close();
                stream = null;
            }
        }

        private static String safeText(String value) {
            return value == null || value.isBlank() ? "---" : value.trim();
        }

        private record CellRenderStyle(Color background, Color text, boolean centered) {
        }
    }
}
