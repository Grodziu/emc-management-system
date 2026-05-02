package pl.emcmanagement.service;

import pl.emcmanagement.dao.UserDao;
import pl.emcmanagement.model.User;

import java.util.Optional;

public class AuthService {
    private final UserDao userDao = new UserDao();

    public Optional<User> login(String login, String password) {
        if (login == null || login.isBlank() || password == null || password.isBlank()) {
            return Optional.empty();
        }
        return userDao.authenticate(login.trim(), password);
    }
}
