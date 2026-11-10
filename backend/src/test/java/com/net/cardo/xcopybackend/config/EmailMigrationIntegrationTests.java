package com.net.cardo.xcopybackend.config;

import java.sql.*;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EmailMigrationIntegrationTests {
    @Autowired DataSource source;
    private Connection connection;
    private String schema;
    private String originalSchema;

    @BeforeEach void oldSchema() throws Exception {
        connection = source.getConnection();originalSchema = connection.getSchema();
        schema = "email_test_" + UUID.randomUUID().toString().replace("-", "");
        execute("CREATE SCHEMA " + schema);connection.setSchema(schema);
        execute("CREATE TABLE users (id uuid PRIMARY KEY, email varchar(255) NOT NULL UNIQUE)");
    }
    @AfterEach void cleanup() throws Exception {
        if (connection == null) return;
        try {
            connection.rollback();connection.setAutoCommit(true);connection.setSchema(originalSchema);
            execute("DROP SCHEMA " + schema + " CASCADE");
        } finally { connection.close(); }
    }
    private void execute(String sql) throws SQLException { try (var statement = connection.createStatement()) { statement.execute(sql); } }
    private void insert(String email) throws Exception {
        try (var statement = connection.prepareStatement("INSERT INTO users(id,email) VALUES (?,?)")) {
            statement.setObject(1, UUID.randomUUID());statement.setString(2, email);statement.executeUpdate();
        }
    }
    private EmailIdentityMigration migration() { return new EmailIdentityMigration(new SingleConnectionDataSource(connection, true)); }

    @Test void existingAddressesAreBackfilledWithoutRewritingDeliveryCaseAndMigrationCanRepeat() throws Exception {
        insert(" Owner.Name+Tag@Example.TEST ");insert("Second@example.test");
        migration().migrate();migration().migrate();
        try (var statement = connection.createStatement();var rows = statement.executeQuery("SELECT email,email_key FROM users ORDER BY email")) {
            assertTrue(rows.next());assertEquals(" Owner.Name+Tag@Example.TEST ", rows.getString(1));assertEquals("owner.name+tag@example.test", rows.getString(2));
            assertTrue(rows.next());assertEquals("second@example.test", rows.getString(2));assertFalse(rows.next());
        }
        try (var statement = connection.prepareStatement("INSERT INTO users(id,email,email_key) VALUES (?,?,?)")) {
            statement.setObject(1, UUID.randomUUID());statement.setString(2, "OWNER.NAME+TAG@example.test");statement.setString(3, "owner.name+tag@example.test");
            assertEquals("23505", assertThrows(SQLException.class, statement::executeUpdate).getSQLState());
        }
    }

    @Test void conflictingLegacyAccountsAreNotMergedOrRewrittenAndSchemaChangesRollBack() throws Exception {
        insert("Owner@example.test");insert("owner@EXAMPLE.TEST");
        var failure = assertThrows(IllegalStateException.class, () -> migration().migrate());
        assertTrue(failure.getMessage().contains("duplicate accounts"));assertFalse(failure.getMessage().contains("Owner@example.test"));
        try (var statement = connection.createStatement();var result = statement.executeQuery("SELECT count(*) FROM users")) { result.next();assertEquals(2,result.getInt(1)); }
        try (var statement = connection.createStatement();var result = statement.executeQuery("SELECT count(*) FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = 'users' AND column_name = 'email_key'")) {
            result.next();assertEquals(0,result.getInt(1));
        }
    }
}
