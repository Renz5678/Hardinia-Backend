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
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

    public Growth findByGrowthId(long id) {
        String sql = "SELECT * FROM growthdetails WHERE growth_id = ?";
        try {
            return jdbc.queryForObject(sql, growthRowMapper(), id);
        } catch (EmptyResultDataAccessException e) {
            throw new GrowthNotFoundException(id);
        }
    }

    public List<Growth> findAllGrowth() {
        String sql = "SELECT * FROM growthdetails ORDER BY recorded_at DESC";
        return jdbc.query(sql, growthRowMapper());
    }

    // Alias for JPA-style naming (used in integration tests)
    public List<Growth> findAll() {
        return findAllGrowth();
    }

    public List<Growth> findByFlower_FlowerId(long flowerId) {
        String sql = "SELECT * FROM growthdetails WHERE flower_id = ? ORDER BY recorded_at DESC";
        return jdbc.query(sql, growthRowMapper(), flowerId);
    }

    // Find all growth records for a specific flower, ordered by recorded date (newest first)
    // Used in integration tests
    public List<Growth> findByFlowerOrderByRecordedAtDesc(Flower flower) {
        return findByFlower_FlowerId(flower.getFlower_id());
    }

    // Find the most recent growth record for a specific flower
    // Used in integration tests
    public Optional<Growth> findTopByFlowerOrderByRecordedAtDesc(Flower flower) {
        Growth latestGrowth = findLatestByFlowerId(flower.getFlower_id());
        return Optional.ofNullable(latestGrowth);
    }

    // Find latest growth record for a specific flower (used by GrowthAutomationService)
    public Growth findLatestByFlowerId(long flowerId) {
        String sql = "SELECT * FROM growthdetails WHERE flower_id = ? ORDER BY recorded_at DESC LIMIT 1";
        try {
            return jdbc.queryForObject(sql, growthRowMapper(), flowerId);
        } catch (EmptyResultDataAccessException e) {
            return null; // No growth record exists yet
        }
    }

    public List<Growth> findByStage(GrowthStage stage) {
        String sql = "SELECT * FROM growthdetails WHERE stage = ? ORDER BY recorded_at DESC";
        return jdbc.query(sql, growthRowMapper(), stage.getGrowthStage());
    }

    public List<Growth> findByColorChanges(boolean colorChanges) {
        String sql = "SELECT * FROM growthdetails WHERE color_changes = ? ORDER BY recorded_at DESC";
        return jdbc.query(sql, growthRowMapper(), colorChanges);
    }

    public void deleteGrowth(long id) {
        String sql = "DELETE FROM growthdetails WHERE growth_id = ?";
        int rowsAffected = jdbc.update(sql, id);
        if (rowsAffected == 0) {
            throw new GrowthNotFoundException(id);
        }
    }

    // Delete a growth object (used in integration tests)
    public void delete(Growth growth) {
        deleteGrowth(growth.getGrowth_id());
    }

    // Delete all growth records for a specific flower
    public void deleteByFlowerId(long flowerId) {
        String sql = "DELETE FROM growthdetails WHERE flower_id = ?";
        jdbc.update(sql, flowerId);
    }

    private Growth insert(Growth growth) {
        String sql = """
        INSERT INTO growthdetails 
        (flower_id, stage, height, color_changes, notes, recorded_at, growth_since_last) 
        VALUES(?, ?, ?, ?, ?, ?, ?)
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
        WHERE growth_id = ?
        """;

        jdbc.update(sql,
                getGrowthStageString(growth),
                growth.getHeight(),
                growth.isColorChanges(),
                growth.getNotes(),
                growth.getRecordedAt() != null ? Timestamp.valueOf(growth.getRecordedAt()) : null,
                growth.getGrowthSinceLast(),
                growth.getGrowth_id());
    }

    private RowMapper<Growth> growthRowMapper() {
        return (rs, i) -> {
            Growth growth = new Growth();
            growth.setGrowth_id(rs.getLong("growth_id"));

            Flower flower = flowerRepository.findByFlowerId(rs.getLong("flower_id"));
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