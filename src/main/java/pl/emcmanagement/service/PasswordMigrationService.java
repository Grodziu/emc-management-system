package pl.emcmanagement.service;

import pl.emcmanagement.dao.UserDao;

public class PasswordMigrationService {
    private final UserDao userDao = new UserDao();

    public int migrateLegacyPasswords() {
        return userDao.migrateLegacyPasswords();
    }
}
