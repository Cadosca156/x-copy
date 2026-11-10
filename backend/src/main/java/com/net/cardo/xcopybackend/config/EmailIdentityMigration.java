package com.net.cardo.xcopybackend.config;

import com.net.cardo.xcopybackend.auth.EmailAddress;
import java.sql.*;
import javax.sql.DataSource;

/** Runs before Hibernate. Adds identity metadata, never rewrites or merges delivery addresses. */
public final class EmailIdentityMigration {
    private final DataSource dataSource;
    public EmailIdentityMigration(DataSource dataSource) { this.dataSource = dataSource; }

    public void migrate() {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                execute(connection, "SET LOCAL statement_timeout = '120s'");
                execute(connection, "SET LOCAL lock_timeout = '5s'");
                execute(connection, "SELECT pg_advisory_xact_lock(1480806224, 1)");
                try (var statement = connection.createStatement();var result = statement.executeQuery("SELECT to_regclass('users')")) {
                    result.next();
                    if (result.getString(1) == null) { connection.commit();return; } // Hibernate creates new installations.
                }
                execute(connection, "LOCK TABLE users IN SHARE ROW EXCLUSIVE MODE");
                execute(connection, "ALTER TABLE users ADD COLUMN IF NOT EXISTS email_key varchar(512)");
                try (var select = connection.createStatement();
                     var update = connection.prepareStatement("UPDATE users SET email_key = ? WHERE id = ? AND email_key IS DISTINCT FROM ?")) {
                    select.setFetchSize(256);
                    try (var result = select.executeQuery("SELECT id, email FROM users")) {
                        int pending = 0;
                        while (result.next()) {
                            String key = EmailAddress.key(result.getString("email"));
                            if (key == null || key.isEmpty() || key.length() > 512) throw new IllegalStateException("Email identity migration blocked: invalid existing email; inspect account data");
                            update.setString(1, key);update.setObject(2, result.getObject("id"));update.setString(3, key);update.addBatch();
                            if (++pending == 256) { update.executeBatch();pending = 0; }
                        }
                        if (pending > 0) update.executeBatch();
                    }
                }
                try (var statement = connection.createStatement();var duplicates = statement.executeQuery(
                        "SELECT 1 FROM users GROUP BY email_key HAVING count(*) > 1 LIMIT 1")) {
                    if (duplicates.next()) throw new IllegalStateException("Email identity migration blocked: case-insensitive duplicate accounts; resolve ownership before deployment; no accounts were merged");
                }
                execute(connection, "ALTER TABLE users ALTER COLUMN email_key SET NOT NULL");
                try (var statement = connection.createStatement();var constraint = statement.executeQuery(
                        "SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conrelid = 'users'::regclass AND conname = 'users_email_key_unique'")) {
                    if (!constraint.next()) execute(connection, "ALTER TABLE users ADD CONSTRAINT users_email_key_unique UNIQUE (email_key)");
                    else if (!"UNIQUE (email_key)".equals(constraint.getString(1))) throw new IllegalStateException("Unexpected users_email_key_unique constraint; inspect schema");
                }
                connection.commit();
            } catch (Exception failure) {
                connection.rollback();
                if (failure instanceof IllegalStateException invalid) throw invalid;
                throw new IllegalStateException("Email identity migration failed; changes were rolled back", failure);
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("Cannot initialize email identities", failure);
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement()) { statement.execute(sql); }
    }
}
