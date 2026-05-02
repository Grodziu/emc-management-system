package pl.emcmanagement.service;

import pl.emcmanagement.dao.ClimateLogDao;
import pl.emcmanagement.dao.DutMediaDao;
import pl.emcmanagement.dao.DutSampleDao;
import pl.emcmanagement.dao.DutTestResultDao;
import pl.emcmanagement.dao.LegDao;
import pl.emcmanagement.dao.LegTestStepDao;
import pl.emcmanagement.dao.MeasurementEquipmentDao;
import pl.emcmanagement.dao.StepMediaDao;
import pl.emcmanagement.model.Project;

import java.nio.file.Path;

public class ProjectReportPdfService {
    private final LegDao legDao = new LegDao();
    private final LegTestStepDao legTestStepDao = new LegTestStepDao();
    private final DutTestResultDao dutTestResultDao = new DutTestResultDao();
    private final MeasurementEquipmentDao measurementEquipmentDao = new MeasurementEquipmentDao();
    private final ClimateLogDao climateLogDao = new ClimateLogDao();
    private final DutSampleDao dutSampleDao = new DutSampleDao();
    private final StepMediaDao stepMediaDao = new StepMediaDao();
    private final DutMediaDao dutMediaDao = new DutMediaDao();

    public Path generateProjectReport(Project project) {
        return new ProjectReportWriter(
                legDao,
                legTestStepDao,
                dutTestResultDao,
                measurementEquipmentDao,
                climateLogDao,
                dutSampleDao,
                stepMediaDao,
                dutMediaDao
        ).generate(project);
    }
}
