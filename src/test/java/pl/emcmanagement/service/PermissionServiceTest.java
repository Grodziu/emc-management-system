package pl.emcmanagement.service;

import org.junit.jupiter.api.Test;
import pl.emcmanagement.enums.Role;
import pl.emcmanagement.model.Leg;
import pl.emcmanagement.model.Project;
import pl.emcmanagement.model.User;

import static org.junit.jupiter.api.Assertions.*;

class PermissionServiceTest {
    private final PermissionService permissionService = new PermissionService();

    @Test
    void administratorHasFullAccessToMainOperations() {
        User admin = user(1, Role.ADMIN);
        Project project = project(user(2, Role.VE), user(3, Role.TE));
        Leg leg = leg(user(4, Role.TT));

        assertTrue(permissionService.canEditProject(admin, project));
        assertTrue(permissionService.canAddLeg(admin, project));
        assertTrue(permissionService.canAddDut(admin, project));
        assertTrue(permissionService.canChangeLegStatus(admin, leg));
        assertTrue(permissionService.canEditDutResults(admin, leg));
        assertTrue(permissionService.canAssignProjectTechnicians(admin));
        assertTrue(permissionService.canAssignLegTechnician(admin));
        assertTrue(permissionService.canManageLegDutAssignments(admin, project));
        assertTrue(permissionService.canManageEquipment(admin, project));
        assertTrue(permissionService.canManageEquipmentCatalog(admin));
        assertTrue(permissionService.canManageUsers(admin));
        assertTrue(permissionService.canManageClimateFiles(admin));
    }

    @Test
    void veCanEditOnlyOwnProject() {
        User ve = user(11, Role.VE);
        Project ownProject = project(ve, user(30, Role.TE));
        Project foreignProject = project(user(12, Role.VE), user(30, Role.TE));

        assertTrue(permissionService.canEditProject(ve, ownProject));
        assertTrue(permissionService.canAddLeg(ve, ownProject));
        assertTrue(permissionService.canAddDut(ve, ownProject));
        assertTrue(permissionService.canManageLegDutAssignments(ve, ownProject));

        assertFalse(permissionService.canEditProject(ve, foreignProject));
        assertFalse(permissionService.canAddLeg(ve, foreignProject));
        assertFalse(permissionService.canAddDut(ve, foreignProject));
        assertFalse(permissionService.canManageEquipment(ve, ownProject));
        assertFalse(permissionService.canManageEquipmentCatalog(ve));
        assertFalse(permissionService.canManageClimateFiles(ve));
    }

    @Test
    void teCanManageEquipmentAndOwnProjectDutButNotEditProjectAsVe() {
        User te = user(20, Role.TE);
        Project ownTeProject = project(user(21, Role.VE), te);
        Project foreignTeProject = project(user(22, Role.VE), user(23, Role.TE));
        Leg anyLeg = leg(user(24, Role.TT));

        assertFalse(permissionService.canEditProject(te, ownTeProject));
        assertTrue(permissionService.canAddDut(te, ownTeProject));
        assertFalse(permissionService.canAddDut(te, foreignTeProject));
        assertTrue(permissionService.canChangeLegStatus(te, anyLeg));
        assertTrue(permissionService.canEditDutResults(te, anyLeg));
        assertTrue(permissionService.canAssignProjectTechnicians(te));
        assertTrue(permissionService.canAssignLegTechnician(te));
        assertTrue(permissionService.canManageEquipment(te, ownTeProject));
        assertTrue(permissionService.canManageEquipmentCatalog(te));
        assertTrue(permissionService.canManageClimateFiles(te));
        assertTrue(permissionService.canManageLegDutAssignments(te, ownTeProject));
        assertFalse(permissionService.canManageUsers(te));
    }

    @Test
    void ttCanWorkOnlyOnAssignedLeg() {
        User assignedTt = user(31, Role.TT);
        User differentTt = user(32, Role.TT);
        Leg assignedLeg = leg(assignedTt);
        Leg foreignLeg = leg(differentTt);
        Project project = project(user(41, Role.VE), user(42, Role.TE));

        assertTrue(permissionService.canChangeLegStatus(assignedTt, assignedLeg));
        assertTrue(permissionService.canEditDutResults(assignedTt, assignedLeg));
        assertFalse(permissionService.canChangeLegStatus(assignedTt, foreignLeg));
        assertFalse(permissionService.canEditDutResults(assignedTt, foreignLeg));
        assertFalse(permissionService.canManageEquipmentCatalog(assignedTt));
        assertFalse(permissionService.canManageClimateFiles(assignedTt));
        assertFalse(permissionService.canManageLegDutAssignments(assignedTt, project));
    }

    @Test
    void nullInputsAreRejectedGracefully() {
        assertFalse(permissionService.canEditProject(null, null));
        assertFalse(permissionService.canAddDut(null, null));
        assertFalse(permissionService.canChangeLegStatus(null, null));
        assertFalse(permissionService.canEditDutResults(null, null));
        assertFalse(permissionService.canAssignProjectTechnicians(null));
        assertFalse(permissionService.canAssignLegTechnician(null));
        assertFalse(permissionService.canManageEquipmentCatalog(null));
        assertFalse(permissionService.canManageClimateFiles(null));
    }

    private static User user(int id, Role role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setFirstName(role.name());
        user.setLastName("User");
        return user;
    }

    private static Project project(User ve, User te) {
        Project project = new Project();
        project.setVe(ve);
        project.setTe(te);
        return project;
    }

    private static Leg leg(User tt) {
        Leg leg = new Leg();
        leg.setAssignedTt(tt);
        return leg;
    }
}
