package org.example.flowerapp.Repository;

import org.example.flowerapp.Exceptions.EntityNotFoundExceptions.GrowthNotFoundException;
import org.example.flowerapp.Models.Enums.GrowthStage;
import org.example.flowerapp.Models.Flower;
import org.example.flowerapp.Models.Growth;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;

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
        String sql = "SELECT * FROM growthdetails";
        return jdbc.query(sql, growthRowMapper());
    }

    public List<Growth> findByStage(GrowthStage stage) {
        String sql = "SELECT * FROM growthdetails WHERE stage = ?";
        return jdbc.query(sql, growthRowMapper(), stage.getGrowthStage());
    }

    public List<Growth> findByColorChanges(boolean colorChanges) {
        String sql = "SELECT * FROM growthdetails WHERE color_changes = ?";
        return jdbc.query(sql, growthRowMapper(), colorChanges);
    }

    public void deleteGrowth(long id) {
        String sql = "DELETE FROM growthdetails WHERE growth_id = ?";
        int rowsAffected = jdbc.update(sql, id);
        if (rowsAffected == 0) {
            throw new GrowthNotFoundException(id);
        }
    }

    private Growth insert(Growth growth) {
        String sql = "INSERT INTO growthdetails (flower_id, stage, height, color_changes, notes) VALUES(?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, growth.getFlower().getFlower_id());
            ps.setString(2, getGrowthStageString(growth));
            ps.setDouble(3, growth.getHeight());
            ps.setBoolean(4, growth.isColorChanges());
            ps.setString(5, growth.getNotes());
            return ps;
        }, keyHolder);

        growth.setGrowth_id(Objects.requireNonNull(keyHolder.getKey()).longValue());
        return growth;
    }

    private void update(Growth growth) {
        String sql = "UPDATE growthdetails SET stage = ?, height = ?, color_changes = ?, notes = ? WHERE growth_id = ?";
        jdbc.update(sql,
                getGrowthStageString(growth),
                growth.getHeight(),
                growth.isColorChanges(),
                growth.getNotes(),
                growth.getGrowth_id());
    }

    private RowMapper<Growth> growthRowMapper() {
        return (rs, i) -> {
            Growth growth = new Growth();
            growth.setGrowth_id(rs.getLong("growth_id"));

            Flower flower = flowerRepository.findByFlowerId(rs.getLong("flower_id"));
            growth.setFlower(flower);

            String stageStr = rs.getString("stage");
            growth.setStage(stageStr != null ? GrowthStage.valueOf(stageStr) : null);

            growth.setHeight(rs.getDouble("height"));
            growth.setColorChanges(rs.getBoolean("color_changes"));
            growth.setNotes(rs.getString("notes"));

            return growth;
        };
    }

    private String getGrowthStageString(Growth growth) {
        return growth.getStage() != null ? growth.getStage().getGrowthStage() : null;
    }
}