// GrowthRepository.java
package org.example.flowerapp.Repository;

import org.example.flowerapp.Exceptions.EntityNotFoundExceptions.FlowerNotFoundException;
import org.example.flowerapp.Exceptions.EntityNotFoundExceptions.GrowthNotFoundException;
import org.example.flowerapp.Exceptions.EntityNotFoundExceptions.MaintenanceNotFoundException;
import org.example.flowerapp.Models.Enums.FlowerColor;
import org.example.flowerapp.Models.Enums.GrowthStage;
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
import java.util.*;

@Repository
public class GrowthRepository {
    private final JdbcTemplate jdbc;
    private final FlowerRepository flowerRepository;

    public GrowthRepository(JdbcTemplate jdbc, FlowerRepository flowerRepository) {
        this.jdbc = jdbc;
        this.flowerRepository = flowerRepository;
    }

    public Growth save(Growth growth) {
        if (growth.getGrowth_id() == 0) {
            return insert(growth);
        } else {
            update(growth);
            return growth;
        }
    }

    public Growth findByGrowthIdAndUserId(long id, String userId) {
        String sql = "SELECT * FROM growthdetails WHERE growth_id = ? AND user_id = ?";
        try {
            return jdbc.queryForObject(sql, growthRowMapper(), id, UUID.fromString(userId));
        } catch (EmptyResultDataAccessException e) {
            throw new GrowthNotFoundException(id);
        }
    }

    public List<Growth> findAllGrowthByUserId(String userId) {
        String sql = "SELECT * FROM growthdetails WHERE user_id = ? ORDER BY recorded_at DESC";
        return jdbc.query(sql, growthRowMapper(), UUID.fromString(userId));
    }

    // Alias for JPA-style naming (used in integration tests)
    public List<Growth> findAllByUserId(String userId) {
        return findAllGrowthByUserId(userId);
    }

    public List<Growth> findByFlowerIdAndUserId(long flowerId, String userId) {
        String sql = "SELECT * FROM growthdetails WHERE flower_id = ? AND user_id = ? ORDER BY recorded_at DESC";
        return jdbc.query(sql, growthRowMapper(), flowerId, UUID.fromString(userId));
    }

    // Find all growth records for a specific flower, ordered by recorded date (newest first)
    // Used in integration tests
    public List<Growth> findByFlowerAndUserIdOrderByRecordedAtDesc(Flower flower, String userId) {
        return findByFlowerIdAndUserId(flower.getFlower_id(), userId);
    }

    // Find the most recent growth record for a specific flower
    // Used in integration tests
    public Optional<Growth> findTopByFlowerAndUserIdOrderByRecordedAtDesc(Flower flower, String userId) {
        Growth latestGrowth = findLatestByFlowerIdAndUserId(flower.getFlower_id(), userId);
        return Optional.ofNullable(latestGrowth);
    }

    public List<Growth> findAll() {
        String sql = "SELECT * FROM growthdetails";

        try {
            return jdbc.query(sql, growthRowMapper());
        } catch (EmptyResultDataAccessException e) {
            return new ArrayList<>();
        }
    }

    // Find latest growth record for a specific flower (used by GrowthAutomationService)
    public Growth findLatestByFlowerIdAndUserId(long flowerId, String userId) {
        String sql = "SELECT * FROM growthdetails WHERE flower_id = ? AND user_id = ? ORDER BY recorded_at DESC LIMIT 1";
        try {
            return jdbc.queryForObject(sql, growthRowMapper(), flowerId, UUID.fromString(userId));
        } catch (EmptyResultDataAccessException e) {
            return null; // No growth record exists yet
        }
    }

    public List<Growth> findByStageAndUserId(GrowthStage stage, String userId) {
        String sql = "SELECT * FROM growthdetails WHERE stage = ? AND user_id = ? ORDER BY recorded_at DESC";
        return jdbc.query(sql, growthRowMapper(), stage.getGrowthStage(), UUID.fromString(userId));
    }

    public List<Growth> findByColorChangesAndUserId(boolean colorChanges, String userId) {
        String sql = "SELECT * FROM growthdetails WHERE color_changes = ? AND user_id = ? ORDER BY recorded_at DESC";
        return jdbc.query(sql, growthRowMapper(), colorChanges, UUID.fromString(userId));
    }

    public void deleteGrowth(long id, String userId) {
        String sql = "DELETE FROM growthdetails WHERE growth_id = ? AND user_id = ?";
        int rowsAffected = jdbc.update(sql, id, UUID.fromString(userId));
        if (rowsAffected == 0) {
            throw new GrowthNotFoundException(id);
        }
    }

    // Delete a growth object (used in integration tests)
    public void delete(Growth growth) {
        deleteGrowth(growth.getGrowth_id(), growth.getUserId());
    }

    // Delete all growth records for a specific flower
    public void deleteByFlowerIdAndUserId(long flowerId, String userId) {
        String sql = "DELETE FROM growthdetails WHERE flower_id = ? AND user_id = ?";
        jdbc.update(sql, flowerId, UUID.fromString(userId));
    }

    public Optional<Growth> findTopByFlowerOrderByRecordedAtDesc(Flower flower) {
        String userId = flower.getUserId();
        Growth latestGrowth = findLatestByFlowerIdAndUserId(flower.getFlower_id(), userId);
        return Optional.ofNullable(latestGrowth);
    }

    public Growth findLatestByFlowerId(Long flowerId) {
        String sql = "SELECT * FROM growthdetails WHERE flower_id = ? ORDER BY recorded_at DESC LIMIT 1";
        try {
            return jdbc.queryForObject(sql, growthRowMapper(), flowerId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<Growth> findByFlowerId(Long flowerId) {
        String sql = "SELECT * FROM growthdetails WHERE flower_id = ? ORDER BY recorded_at DESC";
        return jdbc.query(sql, growthRowMapper(), flowerId);
    }

    private Growth insert(Growth growth) {
        String sql = """
        INSERT INTO growthdetails 
        (flower_id, stage, height, color_changes, notes, recorded_at, growth_since_last, user_id) 
        VALUES(?, ?, ?, ?, ?, ?, ?, ?)
        """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, growth.getFlower().getFlower_id());
            ps.setString(2, getGrowthStageString(growth));
            ps.setDouble(3, growth.getHeight());
            ps.setBoolean(4, growth.isColorChanges());
            ps.setString(5, growth.getNotes());
            ps.setTimestamp(6, growth.getRecordedAt() != null ?
                    Timestamp.valueOf(growth.getRecordedAt()) : Timestamp.valueOf(LocalDateTime.now()));
            ps.setObject(7, growth.getGrowthSinceLast(), java.sql.Types.DOUBLE);
            ps.setObject(8, UUID.fromString(growth.getUserId()), java.sql.Types.OTHER);
            return ps;
        }, keyHolder);

        Long generatedId = (Long) Objects.requireNonNull(keyHolder.getKeys()).get("growth_id");
        growth.setGrowth_id(generatedId);
        return growth;
    }

    private void update(Growth growth) {
        String sql = """
        UPDATE growthdetails 
        SET stage = ?, height = ?, color_changes = ?, notes = ?, recorded_at = ?, growth_since_last = ? 
        WHERE growth_id = ? AND user_id = ?
        """;

        jdbc.update(sql,
                getGrowthStageString(growth),
                growth.getHeight(),
                growth.isColorChanges(),
                growth.getNotes(),
                growth.getRecordedAt() != null ? Timestamp.valueOf(growth.getRecordedAt()) : null,
                growth.getGrowthSinceLast(),
                growth.getGrowth_id(),
                UUID.fromString(growth.getUserId()));
    }

    private RowMapper<Growth> growthRowMapper() {
        return (rs, i) -> {
            Growth growth = new Growth();
            growth.setGrowth_id(rs.getLong("growth_id"));

            // Get user_id first
            Object userIdObj = rs.getObject("user_id");
            String userId;
            if (userIdObj instanceof UUID) {
                userId = ((UUID) userIdObj).toString();
            } else if (userIdObj instanceof String) {
                userId = (String) userIdObj;
            } else if (userIdObj != null) {
                userId = userIdObj.toString();
            } else {
                userId = null;
            }
            growth.setUserId(userId);

            // Fetch flower with userId
            Flower flower = flowerRepository.findByFlowerIdAndUserId(rs.getLong("flower_id"), userId);
            growth.setFlower(flower);

            String stageStr = rs.getString("stage");
            growth.setStage(stageStr != null ? GrowthStage.fromString(stageStr) : null);

            growth.setHeight(rs.getDouble("height"));
            growth.setColorChanges(rs.getBoolean("color_changes"));
            growth.setNotes(rs.getString("notes"));

            Timestamp recordedTs = rs.getTimestamp("recorded_at");
            growth.setRecordedAt(recordedTs != null ? recordedTs.toLocalDateTime() : null);

            Double growthSinceLast = (Double) rs.getObject("growth_since_last");
            growth.setGrowthSinceLast(growthSinceLast);

            return growth;
        };
    }

    private String getGrowthStageString(Growth growth) {
        return growth.getStage() != null ? growth.getStage().getGrowthStage() : null;
    }
}