package pl.emcmanagement.service;

import pl.emcmanagement.enums.Role;
import pl.emcmanagement.model.Leg;
import pl.emcmanagement.model.Project;
import pl.emcmanagement.model.User;

public class PermissionService {
    public boolean isAdministrator(User currentUser) {
        return currentUser != null && currentUser.getRole() == Role.ADMIN;
    }

    public boolean canEditProject(User currentUser, Project project) {
        if (currentUser == null || project == null) {
            return false;
        }
        if (isAdministrator(currentUser)) {
            return true;
        }
        return currentUser.getRole() == Role.VE && project.getVe() != null && project.getVe().getId() == currentUser.getId();
    }

    public boolean canAddLeg(User currentUser, Project project) {
        return canEditProject(currentUser, project);
    }

    public boolean canAddDut(User currentUser, Project project) {
        if (currentUser == null || project == null) {
            return false;
        }
        if (isAdministrator(currentUser)) {
            return true;
        }
        if (currentUser.getRole() == Role.TE) {
            return project.getTe() != null && project.getTe().getId() == currentUser.getId();
        }
        return canEditProject(currentUser, project);
    }

    public boolean canChangeLegStatus(User currentUser, Leg leg) {
        if (currentUser == null || leg == null) {
            return false;
        }
        if (isAdministrator(currentUser) || currentUser.getRole() == Role.TE) {
            return true;
        }
        return currentUser.getRole() == Role.TT
                && leg.getAssignedTt() != null
                && leg.getAssignedTt().getId() == currentUser.getId();
    }

    public boolean canEditDutResults(User currentUser, Leg leg) {
        if (currentUser == null || leg == null) {
            return false;
        }
        if (isAdministrator(currentUser) || currentUser.getRole() == Role.TE) {
            return true;
        }
        return currentUser.getRole() == Role.TT
                && leg.getAssignedTt() != null
                && leg.getAssignedTt().getId() == currentUser.getId();
    }

    public boolean canAssignProjectTechnicians(User currentUser) {
        return currentUser != null && (isAdministrator(currentUser) || currentUser.getRole() == Role.TE);
    }

    public boolean canAssignLegTechnician(User currentUser) {
        return currentUser != null && (isAdministrator(currentUser) || currentUser.getRole() == Role.TE);
    }

    public boolean canManageLegDutAssignments(User currentUser, Project project) {
        return currentUser != null
                && (canEditProject(currentUser, project) || currentUser.getRole() == Role.TE || isAdministrator(currentUser));
    }

    public boolean canManageEquipment(User currentUser, Project project) {
        return currentUser != null && (currentUser.getRole() == Role.TE || isAdministrator(currentUser));
    }

    public boolean canManageEquipmentCatalog(User currentUser) {
        return currentUser != null && (currentUser.getRole() == Role.TE || isAdministrator(currentUser));
    }

    public boolean canManageUsers(User currentUser) {
        return isAdministrator(currentUser);
    }

    public boolean canManageClimateFiles(User currentUser) {
        return currentUser != null && (currentUser.getRole() == Role.TE || isAdministrator(currentUser));
    }
}
