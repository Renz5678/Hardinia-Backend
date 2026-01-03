package org.example.flowerapp.Services;

import lombok.RequiredArgsConstructor;
import org.example.flowerapp.DTO.FlowerRequestDTO;
import org.example.flowerapp.DTO.FlowerResponseDTO;
import org.example.flowerapp.Exceptions.BusinessLogicExceptions.DuplicateFlowerException;
import org.example.flowerapp.Models.Flower;
import org.example.flowerapp.Repository.FlowerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FlowerService {

    private final FlowerRepository flowerRepository;

    @Transactional
    public FlowerResponseDTO addNewFlower(FlowerRequestDTO dto, String userId) {
        checkDuplicateFlowerName(dto.flowerName(), userId);

        Flower flower = new Flower();
        flower.setUserId(userId);  // Set userId
        flower.setFlowerName(dto.flowerName());
        flower.setSpecies(dto.species());
        flower.setColor(dto.color());
        flower.setPlantingDate(dto.plantingDate());
        flower.setGridPosition(dto.gridPosition());

        // Set maintenance scheduling fields
        flower.setWaterFrequencyDays(dto.waterFrequencyDays());
        flower.setFertilizeFrequencyDays(dto.fertilizeFrequencyDays());
        flower.setPruneFrequencyDays(dto.pruneFrequencyDays());
        flower.setLastWateredDate(dto.lastWateredDate());
        flower.setLastFertilizedDate(dto.lastFertilizedDate());
        flower.setLastPrunedDate(dto.lastPrunedDate());
        flower.setAutoScheduling(dto.autoScheduling() != null ? dto.autoScheduling() : true);

        // Set growth fields
        flower.setMaxHeight(dto.maxHeight());
        flower.setGrowthRate(dto.growthRate());

        Flower saved = flowerRepository.save(flower);

        return mapToResponseDTO(saved);
    }

    @Transactional
    public FlowerResponseDTO updateFlower(FlowerRequestDTO dto, long id, String userId) {
        Flower flower = findFlowerByIdOrThrow(id, userId);

        // Check for duplicate name only if the name is being changed
        if (!flower.getFlowerName().equals(dto.flowerName())) {
            checkDuplicateFlowerName(dto.flowerName(), userId);
        }

        flower.setFlowerName(dto.flowerName());
        flower.setSpecies(dto.species());
        flower.setColor(dto.color());
        flower.setPlantingDate(dto.plantingDate());
        flower.setGridPosition(dto.gridPosition());

        // Update maintenance scheduling fields
        flower.setWaterFrequencyDays(dto.waterFrequencyDays());
        flower.setFertilizeFrequencyDays(dto.fertilizeFrequencyDays());
        flower.setPruneFrequencyDays(dto.pruneFrequencyDays());
        flower.setLastWateredDate(dto.lastWateredDate());
        flower.setLastFertilizedDate(dto.lastFertilizedDate());
        flower.setLastPrunedDate(dto.lastPrunedDate());
        if (dto.autoScheduling() != null) {
            flower.setAutoScheduling(dto.autoScheduling());
        }

        // Update growth fields
        flower.setMaxHeight(dto.maxHeight());
        flower.setGrowthRate(dto.growthRate());

        Flower updated = flowerRepository.save(flower);

        return mapToResponseDTO(updated);
    }

    public FlowerResponseDTO getFlowerById(long id, String userId) {
        Flower flower = findFlowerByIdOrThrow(id, userId);
        return mapToResponseDTO(flower);
    }

    public List<FlowerResponseDTO> getAllFlowers(String userId) {
        return flowerRepository.findAllFlowerByUserId(userId)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    public long getFlowerCount(String userId) {
        return flowerRepository.countByUserId(userId);
    }

    public List<FlowerResponseDTO> getAllFlowersBySpecies(String species, String userId) {
        return flowerRepository.findBySpeciesAndUserId(species, userId)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    public List<FlowerResponseDTO> getAllFlowerByColor(String color, String userId) {
        return flowerRepository.findByColorAndUserId(color, userId)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    @Transactional
    public void deleteFlower(long id, String userId) {
        findFlowerByIdOrThrow(id, userId);
        flowerRepository.deleteFlower(id, userId);
    }

    private void checkDuplicateFlowerName(String flowerName, String userId) {
        if (flowerRepository.existsByNameAndUserId(flowerName, userId)) {
            throw new DuplicateFlowerException("Flower name already exists!");
        }
    }

    private Flower findFlowerByIdOrThrow(long id, String userId) {
        flowerRepository.validateExists(id, userId);
        return flowerRepository.findByFlowerIdAndUserId(id, userId);
    }

    private FlowerResponseDTO mapToResponseDTO(Flower flower) {
        return new FlowerResponseDTO(
                flower.getFlower_id(),
                flower.getFlowerName(),
                flower.getSpecies(),
                flower.getColor(),
                flower.getPlantingDate(),
                flower.getGridPosition(),
                flower.getWaterFrequencyDays(),
                flower.getFertilizeFrequencyDays(),
                flower.getPruneFrequencyDays(),
                flower.getLastWateredDate(),
                flower.getLastFertilizedDate(),
                flower.getLastPrunedDate(),
                flower.isAutoScheduling(),
                flower.getMaxHeight(),
                flower.getGrowthRate()
        );
    }
}