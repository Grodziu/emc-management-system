package pl.emcmanagement.app;

import pl.emcmanagement.service.ClimateDataImportService;

import java.nio.file.Path;

public final class DemoClimateImportRunner {
    private DemoClimateImportRunner() {
    }

    public static void main(String[] args) {
        Path demoClimateDirectory = Path.of("src", "main", "resources", "demo", "climate");
        int imported = new ClimateDataImportService().importClimateFilesFromDirectory(demoClimateDirectory);
        System.out.println("DEMO_CLIMATE_IMPORTED=" + imported);
    }
}
