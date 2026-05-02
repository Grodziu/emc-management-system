package pl.emcmanagement.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public final class DatabaseHealthCheck {
    private DatabaseHealthCheck() {
    }

    public static void verifyConnection() {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1");
             ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                throw new IllegalStateException("Zapytanie kontrolne nie zwróciło wyniku.");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Brak połączenia z bazą danych. Sprawdź XAMPP, MySQL i app.properties.", e);
        }
    }
}
