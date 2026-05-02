package pl.emcmanagement.service;

import pl.emcmanagement.dao.LegDao;
import pl.emcmanagement.dao.ProjectDao;
import pl.emcmanagement.model.Leg;
import pl.emcmanagement.model.Project;
import pl.emcmanagement.model.User;

import java.util.List;

public class ProjectService {
    private final ProjectDao projectDao = new ProjectDao();
    private final LegDao legDao = new LegDao();

    public List<Project> getProjectsForUser(User user) {
        return projectDao.findProjectsForUser(user);
    }

    public List<Leg> getLegsForProject(int projectId) {
        return legDao.findLegsByProjectId(projectId);
    }
}
