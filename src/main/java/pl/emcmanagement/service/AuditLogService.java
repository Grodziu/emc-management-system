package pl.emcmanagement.service;

import pl.emcmanagement.dao.AuditLogDao;
import pl.emcmanagement.model.AuditLogEntry;
import pl.emcmanagement.model.User;

import java.util.List;

public class AuditLogService {
    private final AuditLogDao auditLogDao = new AuditLogDao();

    public void log(User actor,
                    String actionType,
                    String entityType,
                    Integer entityId,
                    Integer projectId,
                    Integer legId,
                    Integer stepId,
                    String summary,
                    String details) {
        AuditLogEntry entry = new AuditLogEntry();
        entry.setActorUserId(actor == null ? null : actor.getId());
        entry.setActorName(actor == null ? "System" : actor.getFullName());
        entry.setActorRole(actor == null || actor.getRole() == null ? "SYSTEM" : actor.getRole().name());
        entry.setActionType(actionType);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setProjectId(projectId);
        entry.setLegId(legId);
        entry.setStepId(stepId);
        entry.setSummary(summary);
        entry.setDetails(details);
        auditLogDao.insert(entry);
    }

    public List<AuditLogEntry> findRecent(Integer projectId, Integer legId, int limit) {
        return auditLogDao.findRecent(projectId, legId, limit);
    }
}
