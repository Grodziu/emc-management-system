package pl.emcmanagement.util;

import java.sql.Date;
import java.time.LocalDate;

public final class JdbcMappers {
    private JdbcMappers() {
    }

    public static LocalDate toLocalDate(Date value) {
        return value == null ? null : value.toLocalDate();
    }

    public static Date toSqlDate(LocalDate value) {
        return value == null ? null : Date.valueOf(value);
    }
}
