package pl.emcmanagement.dao;

import pl.emcmanagement.database.DbConnection;
import pl.emcmanagement.enums.Role;
import pl.emcmanagement.model.User;
import pl.emcmanagement.util.PasswordHasher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao {
    public Optional<User> authenticate(String login, String password) {
        String sql = "SELECT id, first_name, last_name, email, login, password, role FROM users WHERE login = ?";

        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, login);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    User user = mapUser(resultSet);
                    if (PasswordHasher.matches(password, user.getPassword())) {
                        if (!PasswordHasher.isHashed(user.getPassword())) {
                            String hashedPassword = PasswordHasher.hash(password);
                            updatePassword(connection, user.getId(), hashedPassword);
                            user.setPassword(hashedPassword);
                        }
                        return Optional.of(user);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas logowania uzytkownika.", e);
        }
        return Optional.empty();
    }

    public List<User> findTechniciansByProjectId(int projectId) {
        String sql = """
                SELECT u.id, u.first_name, u.last_name, u.email, u.login, u.password, u.role
                FROM project_tt_assignments pta
                JOIN users u ON u.id = pta.tt_user_id
                WHERE pta.project_id = ?
                ORDER BY u.last_name, u.first_name
                """;

        List<User> users = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, projectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    users.add(mapUser(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas pobierania technikow projektu.", e);
        }
        return users;
    }

    public List<User> findAllTechnicians() {
        String sql = """
                SELECT id, first_name, last_name, email, login, password, role
                FROM users
                WHERE role = 'TT'
                ORDER BY last_name, first_name
                """;

        List<User> users = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                users.add(mapUser(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas pobierania listy technikow.", e);
        }
        return users;
    }

    public List<User> findAllUsers() {
        String sql = """
                SELECT id, first_name, last_name, email, login, password, role
                FROM users
                ORDER BY last_name, first_name
                """;

        List<User> users = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                users.add(mapUser(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas pobierania listy uzytkownikow.", e);
        }
        return users;
    }

    public int insertUser(User user) {
        String sql = """
                INSERT INTO users (first_name, last_name, email, login, password, role)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, user.getFirstName());
            statement.setString(2, user.getLastName());
            statement.setString(3, user.getEmail());
            statement.setString(4, user.getLogin());
            statement.setString(5, PasswordHasher.ensureHashed(user.getPassword()));
            statement.setString(6, user.getRole().name());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas dodawania uzytkownika.", e);
        }

        throw new IllegalStateException("Nie udalo sie pobrac ID nowego uzytkownika.");
    }

    public int migrateLegacyPasswords() {
        String selectSql = "SELECT id, password FROM users";
        String updateSql = "UPDATE users SET password = ? WHERE id = ?";
        int migrated = 0;

        try (Connection connection = DbConnection.getConnection();
             PreparedStatement selectStatement = connection.prepareStatement(selectSql);
             PreparedStatement updateStatement = connection.prepareStatement(updateSql);
             ResultSet resultSet = selectStatement.executeQuery()) {
            while (resultSet.next()) {
                int userId = resultSet.getInt("id");
                String storedPassword = resultSet.getString("password");
                if (storedPassword == null || storedPassword.isBlank() || PasswordHasher.isHashed(storedPassword)) {
                    continue;
                }
                updateStatement.setString(1, PasswordHasher.hash(storedPassword));
                updateStatement.setInt(2, userId);
                updateStatement.addBatch();
                migrated++;
            }
            if (migrated > 0) {
                updateStatement.executeBatch();
            }
            return migrated;
        } catch (SQLException e) {
            throw new RuntimeException("Blad podczas migracji hasel uzytkownikow.", e);
        }
    }

    private void updatePassword(Connection connection, int userId, String hashedPassword) throws SQLException {
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, hashedPassword);
            statement.setInt(2, userId);
            statement.executeUpdate();
        }
    }

    private User mapUser(ResultSet resultSet) throws SQLException {
        User user = new User();
        user.setId(resultSet.getInt("id"));
        user.setFirstName(resultSet.getString("first_name"));
        user.setLastName(resultSet.getString("last_name"));
        user.setEmail(resultSet.getString("email"));
        user.setLogin(resultSet.getString("login"));
        user.setPassword(resultSet.getString("password"));
        user.setRole(Role.valueOf(resultSet.getString("role")));
        return user;
    }
}
