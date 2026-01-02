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

    public Flower findByFlowerId(long flowerId) {
        String sql = "SELECT * FROM flowerdetails WHERE flower_id = ?";
        try {
            return jdbc.queryForObject(sql, flowerRowMapper(), flowerId);
        } catch (EmptyResultDataAccessException e) {
            throw new FlowerNotFoundException(flowerId);
        }
    }

    public boolean existsByName(String flowerName) {
        String sql = "SELECT COUNT(*) FROM flowerdetails WHERE flower_name = ?";
        Integer count = jdbc.queryForObject(sql, Integer.class, flowerName);
        return count != null && count > 0;
    }

    public boolean existsById(long id) {
        String sql = "SELECT COUNT(*) FROM flowerdetails WHERE flower_id = ?";
        Integer count = jdbc.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    public void validateExists(long id) {
        if (!existsById(id)) {
            throw new FlowerNotFoundException("Flower with id " + id + " not found");
        }
    }

    public List<Flower> findAllFlower() {
        String sql = "SELECT * FROM flowerdetails";
        return jdbc.query(sql, flowerRowMapper());
    }

    public List<Flower> findBySpecies(String species) {
        String sql = "SELECT * FROM flowerdetails WHERE species = ?";
        return jdbc.query(sql, flowerRowMapper(), species);
    }

    public List<Flower> findByColor(String color) {
        String sql = "SELECT * FROM flowerdetails WHERE color = ?";
        return jdbc.query(sql, flowerRowMapper(), color);
    }

    public List<Flower> findByAutoSchedulingTrue() {
        String sql = "SELECT * FROM flowerdetails WHERE auto_scheduling = true";
        return jdbc.query(sql, flowerRowMapper());
    }

    public boolean deleteFlower(long id) {
        String sql = "DELETE FROM flowerdetails WHERE flower_id = ?";
        int rowsAffected = jdbc.update(sql, id);
        if (rowsAffected == 0) {
            throw new FlowerNotFoundException(id);
        }
        return true;
    }

    public long count() {
        String sql = "SELECT COUNT(*) FROM flowerdetails";
        Long count = jdbc.queryForObject(sql, Long.class);
        return count != null ? count : 0;
    }

    private Flower insert(Flower flower) {
        String sql = """
        INSERT INTO flowerdetails 
        (flower_name, species, color, planting_date, grid_position, 
         water_frequency_days, fertilize_frequency_days, prune_frequency_days,
         last_watered, last_fertilized, last_pruned_date, max_height, 
         growth_rate, auto_scheduling) 
        VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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

        return savedFlower;
    }

    private void update(Flower flower) {
        String sql = """
        UPDATE flowerdetails 
        SET flower_name = ?, species = ?, color = ?, planting_date = ?, grid_position = ?,
            water_frequency_days = ?, fertilize_frequency_days = ?, prune_frequency_days = ?,
            last_watered = ?, last_fertilized = ?, last_pruned_date = ?, 
            max_height = ?, growth_rate = ?, auto_scheduling = ?
        WHERE flower_id = ?
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
                flower.getFlower_id()
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

            return flower;
        };
    }
}