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
        String sql = "INSERT INTO flowerdetails (flower_name, species, color, planting_date) VALUES(?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, flower.getFlowerName());
            ps.setString(2, flower.getSpecies());
            ps.setString(3, flower.getColor() != null ? flower.getColor().getColorName() : null);
            ps.setTimestamp(4, flower.getPlantingDate() != null ? Timestamp.valueOf(flower.getPlantingDate()) : null);
            return ps;
        }, keyHolder);

        // Extract flower_id from the keys map
        Long generatedId = (Long) Objects.requireNonNull(keyHolder.getKeys()).get("flower_id");

        return new Flower(
                generatedId,
                flower.getFlowerName(),
                flower.getSpecies(),
                flower.getColor(),
                flower.getPlantingDate()
        );
    }

    private void update(Flower flower) {
        String sql = "UPDATE flowerdetails SET flower_name = ?, species = ?, color = ?, planting_date = ? WHERE flower_id = ?";
        jdbc.update(sql,
                flower.getFlowerName(),
                flower.getSpecies(),
                flower.getColor() != null ? flower.getColor().getColorName() : null,
                flower.getPlantingDate(),
                flower.getFlower_id());
    }

    private RowMapper<Flower> flowerRowMapper() {
        return (rs, i) -> {
            Flower flower = new Flower();
            flower.setFlower_id(rs.getLong("flower_id"));
            flower.setFlowerName(rs.getString("flower_name"));
            flower.setSpecies(rs.getString("species"));

            String colorStr = rs.getString("color");
            flower.setColor(colorStr != null ? FlowerColor.valueOf(colorStr.toUpperCase()) : null);

            Timestamp plantingTs = rs.getTimestamp("planting_date");
            flower.setPlantingDate(plantingTs != null ? plantingTs.toLocalDateTime() : null);

            return flower;
        };
    }
}