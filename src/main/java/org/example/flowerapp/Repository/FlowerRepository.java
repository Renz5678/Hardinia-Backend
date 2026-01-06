package org.example.flowerapp.Repository;

import org.example.flowerapp.Exceptions.EntityNotFoundExceptions.FlowerNotFoundException;
import org.example.flowerapp.Models.Enums.FlowerColor;
import org.example.flowerapp.Models.Flower;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class FlowerRepository {
    private final JdbcTemplate jdbc;

    public FlowerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbc = jdbcTemplate;
    }

    public Flower save(Flower flower) {
        if (flower.getFlower_id() == 0) {
            return insert(flower);
        } else {
            update(flower);
            return flower;
        }
    }

    public Flower findByFlowerIdAndUserId(long flowerId, String userId) {
        String sql = "SELECT * FROM flowerdetails WHERE flower_id = ? AND user_id = ?";
        try {
            return jdbc.queryForObject(sql, flowerRowMapper(), flowerId, UUID.fromString(userId));
        } catch (EmptyResultDataAccessException e) {
            throw new FlowerNotFoundException(flowerId);
        }
    }

    public boolean existsByNameAndUserId(String flowerName, String userId) {
        String sql = "SELECT COUNT(*) FROM flowerdetails WHERE flower_name = ? AND user_id = ?";
        Integer count = jdbc.queryForObject(sql, Integer.class, flowerName, UUID.fromString(userId));
        return count != null && count > 0;
    }

    public boolean existsByIdAndUserId(long id, String userId) {
        String sql = "SELECT COUNT(*) FROM flowerdetails WHERE flower_id = ? AND user_id = ?";
        Integer count = jdbc.queryForObject(sql, Integer.class, id, UUID.fromString(userId));
        return count != null && count > 0;
    }

    public void validateExists(long id, String userId) {
        if (!existsByIdAndUserId(id, userId)) {
            throw new FlowerNotFoundException("Flower with id " + id + " not found");
        }
    }

    public List<Flower> findAllFlowerByUserId(String userId) {
        String sql = "SELECT * FROM flowerdetails WHERE user_id = ?";
        return jdbc.query(sql, flowerRowMapper(), UUID.fromString(userId));
    }

    public List<Flower> findBySpeciesAndUserId(String species, String userId) {
        String sql = "SELECT * FROM flowerdetails WHERE species = ? AND user_id = ?";
        return jdbc.query(sql, flowerRowMapper(), species, UUID.fromString(userId));
    }

    public List<Flower> findByColorAndUserId(String color, String userId) {
        String sql = "SELECT * FROM flowerdetails WHERE color = ? AND user_id = ?";
        return jdbc.query(sql, flowerRowMapper(), color, UUID.fromString(userId));
    }

    public List<Flower> findByAutoSchedulingTrueAndUserId(String userId) {
        String sql = "SELECT * FROM flowerdetails WHERE auto_scheduling = true AND user_id = ?";
        return jdbc.query(sql, flowerRowMapper(), UUID.fromString(userId));
    }

    public boolean deleteFlower(long id, String userId) {
        String sql = "DELETE FROM flowerdetails WHERE flower_id = ? AND user_id = ?";
        int rowsAffected = jdbc.update(sql, id, UUID.fromString(userId));
        if (rowsAffected == 0) {
            throw new FlowerNotFoundException(id);
        }
        return true;
    }

    public long countByUserId(String userId) {
        String sql = "SELECT COUNT(*) FROM flowerdetails WHERE user_id = ?";
        Long count = jdbc.queryForObject(sql, Long.class, UUID.fromString(userId));
        return count != null ? count : 0;
    }

    public List<Flower> findAllFlower() {
        String sql = "SELECT * FROM flowerdetails";
        return jdbc.query(sql, flowerRowMapper());
    }

    public Optional<Flower> findByFlowerIdAndUserId(Long flowerId, String userId) {
        String sql = "SELECT * FROM flowerdetails WHERE flower_id = ? AND user_id = ?";

        try {
            Flower flower = jdbc.queryForObject(sql, new Object[]{flowerId, userId}, flowerRowMapper());
            return Optional.of(flower);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    private Flower insert(Flower flower) {
        String sql = """
        INSERT INTO flowerdetails 
        (flower_name, species, color, planting_date, grid_position, 
         water_frequency_days, fertilize_frequency_days, prune_frequency_days,
         last_watered, last_fertilized, last_pruned_date, max_height, 
         growth_rate, auto_scheduling, user_id) 
        VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, flower.getFlowerName());
            ps.setString(2, flower.getSpecies());
            ps.setString(3, flower.getColor() != null ? flower.getColor().getColorName() : null);
            ps.setTimestamp(4, flower.getPlantingDate() != null ? Timestamp.valueOf(flower.getPlantingDate()) : null);
            ps.setObject(5, flower.getGridPosition(), java.sql.Types.INTEGER);

            // Maintenance frequency fields
            ps.setObject(6, flower.getWaterFrequencyDays(), java.sql.Types.INTEGER);
            ps.setObject(7, flower.getFertilizeFrequencyDays(), java.sql.Types.INTEGER);
            ps.setObject(8, flower.getPruneFrequencyDays(), java.sql.Types.INTEGER);
            ps.setTimestamp(9, flower.getLastWateredDate() != null ? Timestamp.valueOf(flower.getLastWateredDate()) : null);
            ps.setTimestamp(10, flower.getLastFertilizedDate() != null ? Timestamp.valueOf(flower.getLastFertilizedDate()) : null);
            ps.setTimestamp(11, flower.getLastPrunedDate() != null ? Timestamp.valueOf(flower.getLastPrunedDate()) : null);

            // Growth fields
            ps.setObject(12, flower.getMaxHeight(), java.sql.Types.DOUBLE);
            ps.setObject(13, flower.getGrowthRate(), java.sql.Types.DOUBLE);
            ps.setBoolean(14, flower.isAutoScheduling());

            // User ID
            ps.setObject(15, UUID.fromString(flower.getUserId()), java.sql.Types.OTHER);

            return ps;
        }, keyHolder);

        Long generatedId = (Long) Objects.requireNonNull(keyHolder.getKeys()).get("flower_id");

        // Use no-arg constructor + setters
        Flower savedFlower = new Flower();
        savedFlower.setFlower_id(generatedId);
        savedFlower.setFlowerName(flower.getFlowerName());
        savedFlower.setSpecies(flower.getSpecies());
        savedFlower.setColor(flower.getColor());
        savedFlower.setPlantingDate(flower.getPlantingDate());
        savedFlower.setGridPosition(flower.getGridPosition());
        savedFlower.setWaterFrequencyDays(flower.getWaterFrequencyDays());
        savedFlower.setFertilizeFrequencyDays(flower.getFertilizeFrequencyDays());
        savedFlower.setPruneFrequencyDays(flower.getPruneFrequencyDays());
        savedFlower.setLastWateredDate(flower.getLastWateredDate());
        savedFlower.setLastFertilizedDate(flower.getLastFertilizedDate());
        savedFlower.setLastPrunedDate(flower.getLastPrunedDate());
        savedFlower.setMaxHeight(flower.getMaxHeight());
        savedFlower.setGrowthRate(flower.getGrowthRate());
        savedFlower.setAutoScheduling(flower.isAutoScheduling());
        savedFlower.setUserId(flower.getUserId());

        return savedFlower;
    }

    public List<Flower> findByAutoSchedulingTrue() {
        String sql = "SELECT * FROM flowerdetails WHERE auto_scheduling = true";
        return jdbc.query(sql, flowerRowMapper());
    }

    private void update(Flower flower) {
        String sql = """
        UPDATE flowerdetails 
        SET flower_name = ?, species = ?, color = ?, planting_date = ?, grid_position = ?,
            water_frequency_days = ?, fertilize_frequency_days = ?, prune_frequency_days = ?,
            last_watered = ?, last_fertilized = ?, last_pruned_date = ?, 
            max_height = ?, growth_rate = ?, auto_scheduling = ?
        WHERE flower_id = ? AND user_id = ?
        """;

        jdbc.update(sql,
                flower.getFlowerName(),
                flower.getSpecies(),
                flower.getColor() != null ? flower.getColor().getColorName() : null,
                flower.getPlantingDate() != null ? Timestamp.valueOf(flower.getPlantingDate()) : null,
                flower.getGridPosition(),
                flower.getWaterFrequencyDays(),
                flower.getFertilizeFrequencyDays(),
                flower.getPruneFrequencyDays(),
                flower.getLastWateredDate() != null ? Timestamp.valueOf(flower.getLastWateredDate()) : null,
                flower.getLastFertilizedDate() != null ? Timestamp.valueOf(flower.getLastFertilizedDate()) : null,
                flower.getLastPrunedDate() != null ? Timestamp.valueOf(flower.getLastPrunedDate()) : null,
                flower.getMaxHeight(),
                flower.getGrowthRate(),
                flower.isAutoScheduling(),
                flower.getFlower_id(),
                UUID.fromString(flower.getUserId())
        );
    }

    private RowMapper<Flower> flowerRowMapper() {
        return (rs, i) -> {
            Flower flower = new Flower();
            flower.setFlower_id(rs.getLong("flower_id"));
            flower.setFlowerName(rs.getString("flower_name"));
            flower.setSpecies(rs.getString("species"));
            flower.setGridPosition(rs.getInt("grid_position"));

            String colorStr = rs.getString("color");
            flower.setColor(colorStr != null ? FlowerColor.valueOf(colorStr.toUpperCase()) : null);

            Timestamp plantingTs = rs.getTimestamp("planting_date");
            flower.setPlantingDate(plantingTs != null ? plantingTs.toLocalDateTime() : null);

            // Map maintenance scheduling fields
            Integer waterFreq = (Integer) rs.getObject("water_frequency_days");
            flower.setWaterFrequencyDays(waterFreq);

            Integer fertilizeFreq = (Integer) rs.getObject("fertilize_frequency_days");
            flower.setFertilizeFrequencyDays(fertilizeFreq);

            Integer pruneFreq = (Integer) rs.getObject("prune_frequency_days");
            flower.setPruneFrequencyDays(pruneFreq);

            Timestamp lastWateredTs = rs.getTimestamp("last_watered");
            flower.setLastWateredDate(lastWateredTs != null ? lastWateredTs.toLocalDateTime() : null);

            Timestamp lastFertilizedTs = rs.getTimestamp("last_fertilized");
            flower.setLastFertilizedDate(lastFertilizedTs != null ? lastFertilizedTs.toLocalDateTime() : null);

            Timestamp lastPrunedTs = rs.getTimestamp("last_pruned_date");
            flower.setLastPrunedDate(lastPrunedTs != null ? lastPrunedTs.toLocalDateTime() : null);

            // Map growth fields
            Double maxHeight = (Double) rs.getObject("max_height");
            flower.setMaxHeight(maxHeight);

            Double growthRate = (Double) rs.getObject("growth_rate");
            flower.setGrowthRate(growthRate);

            flower.setAutoScheduling(rs.getBoolean("auto_scheduling"));

            // Map user_id - Handle both UUID (PostgreSQL) and String (H2)
            Object userIdObj = rs.getObject("user_id");
            if (userIdObj instanceof UUID) {
                flower.setUserId(((UUID) userIdObj).toString());
            } else if (userIdObj instanceof String) {
                flower.setUserId((String) userIdObj);
            } else if (userIdObj != null) {
                flower.setUserId(userIdObj.toString());
            } else {
                flower.setUserId(null);
            }

            return flower;
        };
    }
}