package com.brinza.notary.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;

/**
 * Java-based (rather than plain SQL) so the requested_at + duration_minutes backfill math
 * is done in portable Java instead of database-specific interval arithmetic.
 */
@Component
public class V11__AddAppointmentEndedAt extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        try (Statement ddl = connection.createStatement()) {
            ddl.execute("ALTER TABLE appointments ADD COLUMN ended_at TIMESTAMP");
        }

        try (Statement select = connection.createStatement();
             ResultSet rs = select.executeQuery(
                     "SELECT a.id AS appointment_id, a.requested_at, s.duration_minutes " +
                             "FROM appointments a JOIN services s ON a.service_id = s.id");
             PreparedStatement update = connection.prepareStatement(
                     "UPDATE appointments SET ended_at = ? WHERE id = ?")) {
            while (rs.next()) {
                Timestamp requestedAt = rs.getTimestamp("requested_at");
                int durationMinutes = rs.getInt("duration_minutes");
                update.setTimestamp(1, new Timestamp(requestedAt.getTime() + durationMinutes * 60_000L));
                update.setLong(2, rs.getLong("appointment_id"));
                update.executeUpdate();
            }
        }

        try (Statement ddl = connection.createStatement()) {
            ddl.execute("ALTER TABLE appointments ALTER COLUMN ended_at SET NOT NULL");
        }
    }
}
