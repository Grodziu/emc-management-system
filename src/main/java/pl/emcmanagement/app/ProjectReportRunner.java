package pl.emcmanagement.app;

import pl.emcmanagement.enums.Role;
import pl.emcmanagement.model.Project;
import pl.emcmanagement.model.User;
import pl.emcmanagement.service.ProjectReportPdfService;
import pl.emcmanagement.service.ProjectService;

import java.nio.file.Path;
import java.util.List;

public final class ProjectReportRunner {
    private ProjectReportRunner() {
    }

    public static void main(String[] args) {
        String targetEwr = args.length > 0 ? args[0].trim() : null;

        User adminView = new User();
        adminView.setRole(Role.ADMIN);

        List<Project> projects = new ProjectService().getProjectsForUser(adminView);
        Project project = projects.stream()
                .filter(item -> targetEwr == null || item.getEwrNumber().equalsIgnoreCase(targetEwr))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono projektu do raportu."));

        Path reportPath = new ProjectReportPdfService().generateProjectReport(project);
        System.out.println(reportPath.toAbsolutePath());
    }
}
