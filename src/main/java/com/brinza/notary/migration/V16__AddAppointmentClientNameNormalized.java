package com.brinza.notary.migration;

import com.brinza.notary.service.SearchTextNormalizer;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Java-based (rather than plain SQL) so existing rows are folded with the very same
 * {@link SearchTextNormalizer} the application writes new rows with — neither H2 nor plain
 * portable SQL can strip accents on its own.
 */
@Component
public class V16__AddAppointmentClientNameNormalized extends BaseJavaMigration {

    private static final Logger log = LoggerFactory.getLogger(V16__AddAppointmentClientNameNormalized.class);

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        try (Statement ddl = connection.createStatement()) {
            ddl.execute("ALTER TABLE appointments ADD COLUMN client_name_normalized VARCHAR(255)");
            log.debug("Added client_name_normalized column to appointments table");
        }

        int updatedRows = 0;
        try (Statement select = connection.createStatement();
             ResultSet rs = select.executeQuery("SELECT id, client_name FROM appointments");
             PreparedStatement update = connection.prepareStatement(
                     "UPDATE appointments SET client_name_normalized = ? WHERE id = ?")) {
            while (rs.next()) {
                String normalized = SearchTextNormalizer.normalize(rs.getString("client_name"));
                update.setString(1, normalized == null ? "" : normalized);
                update.setLong(2, rs.getLong("id"));
                update.executeUpdate();
                updatedRows++;
            }
        }
        log.debug("Backfilled client_name_normalized for {} appointment(s)", updatedRows);

        try (Statement ddl = connection.createStatement()) {
            ddl.execute("ALTER TABLE appointments ALTER COLUMN client_name_normalized SET NOT NULL");
            log.debug("Set client_name_normalized column to NOT NULL");
        }
    }
}
