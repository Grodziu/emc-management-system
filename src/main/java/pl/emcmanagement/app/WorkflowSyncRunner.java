package pl.emcmanagement.app;

import pl.emcmanagement.service.WorkflowConsistencyService;

public class WorkflowSyncRunner {
    public static void main(String[] args) {
        new WorkflowConsistencyService().synchronizeAll();
        System.out.println("WORKFLOW_SYNC_OK");
    }
}
