package org.example.flowerapp.Repository;

import com.sun.tools.javac.Main;
import org.example.flowerapp.Exceptions.EntityNotFoundExceptions.MaintenanceNotFoundException;
import org.example.flowerapp.Models.Enums.MaintenanceType;
import org.example.flowerapp.Models.Flower;
import org.example.flowerapp.Models.Growth;
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
import java.util.ArrayList;
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
        String sql = "SELECT * FROM maintenance WHERE task_id = ? AND user_id = ?::uuid";
        try {
            return jdbc.queryForObject(sql, maintenanceRowMapper(), taskId, userId);
        } catch (EmptyResultDataAccessException e) {
            throw new MaintenanceNotFoundException(taskId);
        }
    }

    public List<Maintenance> findAll() {
        String sql = "SELECT * FROM maintenance";

        try {
            return jdbc.query(sql, maintenanceRowMapper());
        } catch (EmptyResultDataAccessException e) {
            return new ArrayList<>();
        }
    }

    public List<Maintenance> findAllMaintenanceByUserId(String userId) {
        String sql = "SELECT * FROM maintenance WHERE user_id = ?::uuid";
        try {
            List<Maintenance> results = jdbc.query(sql, maintenanceRowMapper(), userId);
            System.out.println("Repository found " + results.size() + " maintenance records for user: " + userId);
            return results;
        } catch (Exception e) {
            System.err.println("Error querying maintenance: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public List<Maintenance> findByFlowerId(Long flowerId) {
        String sql = "SELECT * FROM maintenance WHERE flower_id = ?";

        try {
            return jdbc.query(sql, maintenanceRowMapper(), flowerId);
        } catch (EmptyResultDataAccessException e) {
            return new ArrayList<>();
        }
    }

    public List<Maintenance> findByFlowerIdAndUserId(long flowerId, String userId) {
        String sql = "SELECT * FROM maintenance WHERE flower_id = ? AND user_id = ?::uuid";
        return jdbc.query(sql, maintenanceRowMapper(), flowerId, userId);
    }

    public List<Maintenance> findByMaintenanceTypeAndUserId(MaintenanceType maintenanceType, String userId) {
        String sql = "SELECT * FROM maintenance WHERE maintenance_type = ? AND user_id = ?::uuid";
        return jdbc.query(sql, maintenanceRowMapper(), maintenanceType.name(), userId);
    }

    public List<Maintenance> findByMaintenanceDateAndUserId(LocalDateTime dateTime, String userId) {
        String sql = "SELECT * FROM maintenance WHERE maintenance_date = ? AND user_id = ?::uuid";
        return jdbc.query(sql, maintenanceRowMapper(), Timestamp.valueOf(dateTime), userId);
    }

    public List<Maintenance> findByFlowerAndCompletedFalseAndDueDateBefore(Flower flower, LocalDateTime dateTime) {
        String sql = """
        SELECT * FROM maintenance 
        WHERE flower_id = ? 
        AND user_id = ?::uuid
        AND completed = false 
        AND due_date < ?
        ORDER BY due_date ASC
        """;
        return jdbc.query(sql, maintenanceRowMapper(), flower.getFlower_id(),
                flower.getUserId(), Timestamp.valueOf(dateTime));
    }

    public List<Maintenance> findByFlowerIdAndCompletedFalseAndDueDateBeforeAndUserId(
            long flowerId, LocalDateTime dateTime, String userId) {
        String sql = """
        SELECT * FROM maintenance 
        WHERE flower_id = ? 
        AND user_id = ?::uuid
        AND completed = false 
        AND due_date < ?
        ORDER BY due_date ASC
        """;
        return jdbc.query(sql, maintenanceRowMapper(), flowerId, userId, Timestamp.valueOf(dateTime));
    }

    public boolean deleteMaintenance(long id, String userId) {
        String sql = "DELETE FROM maintenance WHERE task_id = ? AND user_id = ?::uuid";
        return jdbc.update(sql, id, userId) != 0;
    }

    public void delete(Maintenance maintenance) {
        String sql = "DELETE FROM maintenance WHERE task_id = ?";
        jdbc.update(sql, maintenance.getTask_id());
    }

    public boolean existsByFlowerAndTypeAndDateRange(long flowerId, MaintenanceType type,
                                                     LocalDateTime start, LocalDateTime end, String userId) {
        String sql = """
        SELECT COUNT(*) FROM maintenance 
        WHERE flower_id = ? 
        AND user_id = ?::uuid
        AND maintenance_type = ? 
        AND maintenance_date BETWEEN ? AND ?
        """;

        Integer count = jdbc.queryForObject(sql, Integer.class,
                flowerId,
                userId,
                type.name(),
                Timestamp.valueOf(start),
                Timestamp.valueOf(end)
        );

        return count != null && count > 0;
    }

    public List<Maintenance> findIncompleteByFlowerIdAndUserId(long flowerId, String userId) {
        String sql = "SELECT * FROM maintenance WHERE flower_id = ? AND user_id = ?::uuid AND completed = false";
        return jdbc.query(sql, maintenanceRowMapper(), flowerId, userId);
    }

    public List<Maintenance> findByCompletedStatusAndUserId(boolean completed, String userId) {
        String sql = "SELECT * FROM maintenance WHERE completed = ? AND user_id = ?::uuid";
        return jdbc.query(sql, maintenanceRowMapper(), completed, userId);
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
        AND user_id = ?::uuid
        AND maintenance_type = ? 
        AND completed = ?
        AND auto_generated = true
        """;
        Boolean result = jdbc.queryForObject(sql, Boolean.class, flowerId, userId, type.name(), completed);
        return result != null && result;
    }

    public long countOverdueTasksByUserId(String userId) {
        String sql = """
        SELECT COUNT(*) FROM maintenance 
        WHERE user_id = ?::uuid
        AND completed = false 
        AND due_date < ?
        """;
        Long count = jdbc.queryForObject(sql, Long.class, userId, Timestamp.valueOf(LocalDateTime.now()));
        return count != null ? count : 0;
    }

    public List<Maintenance> findByMaintenanceType(MaintenanceType maintenanceType) {
        String sql = "SELECT * FROM maintenance WHERE maintenance_type = ?";
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
        AND user_id = ?::uuid
        """;

        Long count = jdbc.queryForObject(
                sql,
                Long.class,
                flower.getFlower_id(),
                maintenanceType.name(),
                flower.getUserId()
        );

        return count != null && count > 0;
    }

    public List<Maintenance> findByCompletedStatusExcludingDead(boolean completed) {
        String sql = """
        SELECT m.* FROM maintenance m
        WHERE m.completed = ?
        AND m.flower_id NOT IN (
            SELECT g.flower_id 
            FROM growth g
            WHERE g.recorded_at = (
                SELECT MAX(g2.recorded_at) 
                FROM growth g2 
                WHERE g2.flower_id = g.flower_id
            )
            AND g.stage = 'DEAD'
        )
        """;

        try {
            return jdbc.query(sql, maintenanceRowMapper(), completed);
        } catch (EmptyResultDataAccessException e) {
            return new ArrayList<>();
        }
    }

    /**
     * Find incomplete maintenance tasks for a specific user excluding those for dead flowers
     */
    public List<Maintenance> findByCompletedStatusAndUserIdExcludingDead(boolean completed, String userId) {
        String sql = """
        SELECT m.* FROM maintenance m
        WHERE m.completed = ? 
        AND m.user_id = ?::uuid
        AND m.flower_id NOT IN (
            SELECT g.flower_id 
            FROM growth g
            WHERE g.recorded_at = (
                SELECT MAX(g2.recorded_at) 
                FROM growth g2 
                WHERE g2.flower_id = g.flower_id
            )
            AND g.stage = 'DEAD'
        )
        """;

        try {
            return jdbc.query(sql, maintenanceRowMapper(), completed, userId);
        } catch (EmptyResultDataAccessException e) {
            return new ArrayList<>();
        }
    }

    /**
     * Find incomplete maintenance tasks for a specific flower excluding dead status
     * (Optional - useful for consistency)
     */
    public List<Maintenance> findIncompleteByFlowerIdAndUserIdExcludingDead(long flowerId, String userId) {
        String sql = """
        SELECT m.* FROM maintenance m
        WHERE m.flower_id = ? 
        AND m.user_id = ?::uuid 
        AND m.completed = false
        AND m.flower_id NOT IN (
            SELECT g.flower_id 
            FROM growth g
            WHERE g.recorded_at = (
                SELECT MAX(g2.recorded_at) 
                FROM growth g2 
                WHERE g2.flower_id = g.flower_id
            )
            AND g.stage = 'DEAD'
        )
        """;

        try {
            return jdbc.query(sql, maintenanceRowMapper(), flowerId, userId);
        } catch (EmptyResultDataAccessException e) {
            return new ArrayList<>();
        }
    }

    private Maintenance insert(Maintenance maintenance) {
        String sql = """
        INSERT INTO maintenance 
        (flower_id, maintenance_type, maintenance_date, notes, performed_by, 
         created_at, completed, completed_at, auto_generated, user_id) 
        VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?::uuid)
        """;
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, maintenance.getFlower().getFlower_id());
            ps.setString(2, maintenance.getTaskType() != null ? maintenance.getTaskType().name() : null);
            ps.setTimestamp(3, maintenance.getScheduledDate() != null ? Timestamp.valueOf(maintenance.getScheduledDate()) : null);
            ps.setString(4, maintenance.getNotes());
            ps.setString(5, maintenance.getPerformedBy());
            ps.setTimestamp(6, maintenance.getCreatedAt() != null ? Timestamp.valueOf(maintenance.getCreatedAt()) : null);
            ps.setBoolean(7, maintenance.isCompleted());
            ps.setTimestamp(8, maintenance.getCompletedAt() != null ? Timestamp.valueOf(maintenance.getCompletedAt()) : null);
            ps.setBoolean(9, maintenance.isAutoGenerated());
            ps.setString(10, maintenance.getUserId());
            return ps;
        }, keyHolder);

        Long generatedId = (Long) Objects.requireNonNull(keyHolder.getKeys()).get("task_id");
        maintenance.setTask_id(generatedId);
        return maintenance;
    }

    private void update(Maintenance maintenance) {
        String sql = """
        UPDATE maintenance 
        SET maintenance_type = ?, maintenance_date = ?, notes = ?, 
            performed_by = ?, created_at = ?, completed = ?, completed_at = ?, auto_generated = ?
        WHERE task_id = ? AND user_id = ?::uuid
        """;
        jdbc.update(sql,
                maintenance.getTaskType() != null ? maintenance.getTaskType().name() : null,
                maintenance.getScheduledDate() != null ? Timestamp.valueOf(maintenance.getScheduledDate()) : null,
                maintenance.getNotes(),
                maintenance.getPerformedBy(),
                maintenance.getCreatedAt() != null ? Timestamp.valueOf(maintenance.getCreatedAt()) : null,
                maintenance.isCompleted(),
                maintenance.getCompletedAt() != null ? Timestamp.valueOf(maintenance.getCompletedAt()) : null,
                maintenance.isAutoGenerated(),
                maintenance.getTask_id(),
                maintenance.getUserId());
    }

    private RowMapper<Maintenance> maintenanceRowMapper() {
        return (rs, i) -> {
            try {
                Maintenance maintenance = new Maintenance();
                maintenance.setTask_id(rs.getLong("task_id"));

                // Get user_id as string
                Object userIdObj = rs.getObject("user_id");
                String userId = null;
                if (userIdObj != null) {
                    if (userIdObj instanceof UUID) {
                        userId = userIdObj.toString();
                    } else {
                        userId = userIdObj.toString();
                    }
                }
                maintenance.setUserId(userId);

                // Fetch flower - handle potential null
                long flowerId = rs.getLong("flower_id");
                try {
                    Flower flower = flowerRepository.findByFlowerIdAndUserId(flowerId, userId);
                    if (flower == null) {
                        System.err.println("WARNING: Could not find flower " + flowerId + " for user " + userId);
                        // Create a minimal flower object to prevent null pointer
                        flower = new Flower();
                        flower.setFlower_id(flowerId);
                        flower.setUserId(userId);
                    }
                    maintenance.setFlower(flower);
                } catch (Exception e) {
                    System.err.println("Error fetching flower " + flowerId + ": " + e.getMessage());
                    // Create minimal flower to prevent failure
                    Flower flower = new Flower();
                    flower.setFlower_id(flowerId);
                    flower.setUserId(userId);
                    maintenance.setFlower(flower);
                }

                maintenance.setTaskType(MaintenanceType.valueOf(rs.getString("maintenance_type")));

                Timestamp maintenanceTs = rs.getTimestamp("maintenance_date");
                maintenance.setScheduledDate(maintenanceTs != null ? maintenanceTs.toLocalDateTime() : null);

                Timestamp dueDateTs = rs.getTimestamp("due_date");
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
            } catch (Exception e) {
                System.err.println("Error mapping maintenance row: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Failed to map maintenance", e);
            }
        };
    }
}