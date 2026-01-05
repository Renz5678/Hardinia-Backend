package org.example.flowerapp.Repository;

import org.example.flowerapp.Exceptions.EntityNotFoundExceptions.MaintenanceNotFoundException;
import org.example.flowerapp.Models.Enums.MaintenanceType;
import org.example.flowerapp.Models.Flower;
import org.example.flowerapp.Models.Maintenance;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Repository
public class MaintenanceRepository {
    private final JdbcTemplate jdbc;
    private final FlowerRepository flowerRepository;

    public MaintenanceRepository(JdbcTemplate jdbc, FlowerRepository flowerRepository) {
        this.jdbc = jdbc;
        this.flowerRepository = flowerRepository;
    }

    public Maintenance save(Maintenance maintenance) {
        if (maintenance.getTask_id() == 0) {
            return insert(maintenance);
        } else {
            update(maintenance);
            return maintenance;
        }
    }

    public Maintenance findByTaskIdAndUserId(long taskId, String userId) {
        String sql = "SELECT * FROM maintenance WHERE task_id = ? AND user_id = ?";
        try {
            return jdbc.queryForObject(sql, maintenanceRowMapper(), taskId, UUID.fromString(userId));
        } catch (EmptyResultDataAccessException e) {
            throw new MaintenanceNotFoundException(taskId);
        }
    }

    public List<Maintenance> findAllMaintenanceByUserId(String userId) {
        String sql = "SELECT * FROM maintenance WHERE user_id = ?";
        return jdbc.query(sql, maintenanceRowMapper(), UUID.fromString(userId));
    }

    public List<Maintenance> findByFlowerIdAndUserId(long flowerId, String userId) {
        String sql = "SELECT * FROM maintenance WHERE flower_id = ? AND user_id = ?";
        return jdbc.query(sql, maintenanceRowMapper(), flowerId, UUID.fromString(userId));
    }

    public List<Maintenance> findByMaintenanceTypeAndUserId(MaintenanceType maintenanceType, String userId) {
        String sql = "SELECT * FROM maintenance WHERE maintenance_type = ? AND user_id = ?";
        return jdbc.query(sql, maintenanceRowMapper(), maintenanceType.name(), UUID.fromString(userId));
    }

    public List<Maintenance> findByMaintenanceDateAndUserId(LocalDateTime dateTime, String userId) {
        String sql = "SELECT * FROM maintenance WHERE maintenance_date = ? AND user_id = ?";
        return jdbc.query(sql, maintenanceRowMapper(), Timestamp.valueOf(dateTime), UUID.fromString(userId));
    }

    // New method required by GrowthAutomationService
    public List<Maintenance> findByFlowerAndCompletedFalseAndDueDateBefore(Flower flower, LocalDateTime dateTime) {
        String sql = """
        SELECT * FROM maintenance 
        WHERE flower_id = ? 
        AND user_id = ?
        AND completed = false 
        AND due_date < ?
        ORDER BY due_date ASC
        """;
        return jdbc.query(sql, maintenanceRowMapper(), flower.getFlower_id(),
                UUID.fromString(flower.getUserId()), Timestamp.valueOf(dateTime));
    }

    // Alternative method using flower ID
    public List<Maintenance> findByFlowerIdAndCompletedFalseAndDueDateBeforeAndUserId(
            long flowerId, LocalDateTime dateTime, String userId) {
        String sql = """
        SELECT * FROM maintenance 
        WHERE flower_id = ? 
        AND user_id = ?
        AND completed = false 
        AND due_date < ?
        ORDER BY due_date ASC
        """;
        return jdbc.query(sql, maintenanceRowMapper(), flowerId,
                UUID.fromString(userId), Timestamp.valueOf(dateTime));
    }

    public boolean deleteMaintenance(long id, String userId) {
        String sql = "DELETE FROM maintenance WHERE task_id = ? AND user_id = ?";
        return jdbc.update(sql, id, UUID.fromString(userId)) != 0;
    }

    public boolean existsByFlowerAndTypeAndDateRange(long flowerId, MaintenanceType type,
                                                     LocalDateTime start, LocalDateTime end, String userId) {
        String sql = """
        SELECT COUNT(*) FROM maintenance 
        WHERE flower_id = ? 
        AND user_id = ?
        AND maintenance_type = ? 
        AND maintenance_date BETWEEN ? AND ?
        """;

        Integer count = jdbc.queryForObject(sql, Integer.class,
                flowerId,
                UUID.fromString(userId),
                type.name(),
                Timestamp.valueOf(start),
                Timestamp.valueOf(end)
        );

        return count != null && count > 0;
    }

    public List<Maintenance> findIncompleteByFlowerIdAndUserId(long flowerId, String userId) {
        String sql = "SELECT * FROM maintenance WHERE flower_id = ? AND user_id = ? AND completed = false";
        return jdbc.query(sql, maintenanceRowMapper(), flowerId, UUID.fromString(userId));
    }

    public List<Maintenance> findByCompletedStatusAndUserId(boolean completed, String userId) {
        String sql = "SELECT * FROM maintenance WHERE completed = ? AND user_id = ?";
        return jdbc.query(sql, maintenanceRowMapper(), completed, UUID.fromString(userId));
    }

    public List<Maintenance> findByCompletedStatus(boolean completed) {
        String sql = "SELECT * FROM maintenance WHERE completed = ?";
        return jdbc.query(sql, maintenanceRowMapper(), completed);
    }

    public boolean existsByFlowerAndTypeAndCompleted(long flowerId, MaintenanceType type,
                                                     boolean completed, String userId) {
        String sql = """
        SELECT COUNT(*) > 0 
        FROM maintenance 
        WHERE flower_id = ? 
        AND user_id = ?
        AND maintenance_type = ? 
        AND completed = ?
        AND auto_generated = true
        """;
        Boolean result = jdbc.queryForObject(sql, Boolean.class, flowerId,
                UUID.fromString(userId), type.name(), completed);
        return result != null && result;
    }

    public long countOverdueTasksByUserId(String userId) {
        String sql = """
        SELECT COUNT(*) FROM maintenance 
        WHERE user_id = ?
        AND completed = false 
        AND due_date < ?
        """;
        Long count = jdbc.queryForObject(sql, Long.class, UUID.fromString(userId),
                Timestamp.valueOf(LocalDateTime.now()));
        return count != null ? count : 0;
    }

    public List<Maintenance> findByMaintenanceType(MaintenanceType maintenanceType) {
        String sql = """
        SELECT * 
        FROM maintenance 
        WHERE maintenance_type = ?
        """;

        return jdbc.query(sql, maintenanceRowMapper(), maintenanceType.name());
    }

    public boolean existsByFlowerAndMaintenanceTypeAndCompletedFalse(
            Flower flower,
            MaintenanceType maintenanceType) {

        String sql = """
        SELECT COUNT(*) 
        FROM maintenance 
        WHERE flower_id = ? 
        AND maintenance_type = ? 
        AND completed = false
        AND user_id = ?
        """;

        Long count = jdbc.queryForObject(
                sql,
                Long.class,
                flower.getFlower_id(),
                maintenanceType.name(),
                UUID.fromString(flower.getUserId())
        );

        return count != null && count > 0;
    }

    private Maintenance insert(Maintenance maintenance) {
        String sql = """
        INSERT INTO maintenance 
        (flower_id, maintenance_type, maintenance_date, due_date, notes, performed_by, 
         created_at, completed, completed_at, auto_generated, user_id) 
        VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, maintenance.getFlower().getFlower_id());
            ps.setString(2, maintenance.getTaskType() != null ? maintenance.getTaskType().name() : null);
            ps.setTimestamp(3, maintenance.getScheduledDate() != null ? Timestamp.valueOf(maintenance.getScheduledDate()) : null);
            ps.setTimestamp(4, maintenance.getDueDate() != null ? Timestamp.valueOf(maintenance.getDueDate()) : null);
            ps.setString(5, maintenance.getNotes());
            ps.setString(6, maintenance.getPerformedBy());
            ps.setTimestamp(7, maintenance.getCreatedAt() != null ? Timestamp.valueOf(maintenance.getCreatedAt()) : null);
            ps.setBoolean(8, maintenance.isCompleted());
            ps.setTimestamp(9, maintenance.getCompletedAt() != null ? Timestamp.valueOf(maintenance.getCompletedAt()) : null);
            ps.setBoolean(10, maintenance.isAutoGenerated());
            ps.setObject(11, UUID.fromString(maintenance.getUserId()), java.sql.Types.OTHER);
            return ps;
        }, keyHolder);

        Long generatedId = (Long) Objects.requireNonNull(keyHolder.getKeys()).get("task_id");
        maintenance.setTask_id(generatedId);
        return maintenance;
    }

    private void update(Maintenance maintenance) {
        String sql = """
        UPDATE maintenance 
        SET maintenance_type = ?, maintenance_date = ?, due_date = ?, notes = ?, 
            performed_by = ?, created_at = ?, completed = ?, completed_at = ?, auto_generated = ?
        WHERE task_id = ? AND user_id = ?
        """;
        jdbc.update(sql,
                maintenance.getTaskType() != null ? maintenance.getTaskType().name() : null,
                maintenance.getScheduledDate() != null ? Timestamp.valueOf(maintenance.getScheduledDate()) : null,
                maintenance.getDueDate() != null ? Timestamp.valueOf(maintenance.getDueDate()) : null,
                maintenance.getNotes(),
                maintenance.getPerformedBy(),
                maintenance.getCreatedAt() != null ? Timestamp.valueOf(maintenance.getCreatedAt()) : null,
                maintenance.isCompleted(),
                maintenance.getCompletedAt() != null ? Timestamp.valueOf(maintenance.getCompletedAt()) : null,
                maintenance.isAutoGenerated(),
                maintenance.getTask_id(),
                UUID.fromString(maintenance.getUserId()));
    }

    private RowMapper<Maintenance> maintenanceRowMapper() {
        return (rs, i) -> {
            Maintenance maintenance = new Maintenance();
            maintenance.setTask_id(rs.getLong("task_id"));

            // Get user_id - Handle both UUID (PostgreSQL) and String (H2)
            String userId = null;
            Object userIdObj = rs.getObject("user_id");
            if (userIdObj instanceof UUID) {
                userId = ((UUID) userIdObj).toString();
            } else if (userIdObj instanceof String) {
                userId = (String) userIdObj;
            } else if (userIdObj != null) {
                userId = userIdObj.toString();
            }
            maintenance.setUserId(userId);

            // Fetch flower with userId
            Flower flower = flowerRepository.findByFlowerIdAndUserId(rs.getLong("flower_id"), userId);
            maintenance.setFlower(flower);
            maintenance.setTaskType(MaintenanceType.valueOf(rs.getString("maintenance_type")));

            Timestamp maintenanceTs = rs.getTimestamp("maintenance_date");
            maintenance.setScheduledDate(maintenanceTs != null ? maintenanceTs.toLocalDateTime() : null);

            Timestamp dueDateTs = rs.getTimestamp("maintenance_date");
            maintenance.setDueDate(dueDateTs != null ? dueDateTs.toLocalDateTime() : null);

            maintenance.setNotes(rs.getString("notes"));
            maintenance.setPerformedBy(rs.getString("performed_by"));

            Timestamp createdTs = rs.getTimestamp("created_at");
            maintenance.setCreatedAt(createdTs != null ? createdTs.toLocalDateTime() : null);

            maintenance.setCompleted(rs.getBoolean("completed"));

            Timestamp completedTs = rs.getTimestamp("completed_at");
            maintenance.setCompletedAt(completedTs != null ? completedTs.toLocalDateTime() : null);

            maintenance.setAutoGenerated(rs.getBoolean("auto_generated"));

            return maintenance;
        };
    }
}