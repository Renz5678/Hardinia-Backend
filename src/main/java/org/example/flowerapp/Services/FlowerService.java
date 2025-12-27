package org.example.flowerapp.Services;

import lombok.RequiredArgsConstructor;
import org.example.flowerapp.DTO.FlowerRequestDTO;
import org.example.flowerapp.DTO.FlowerResponseDTO;
import org.example.flowerapp.Exceptions.BusinessLogicExceptions.DuplicateFlowerException;
import org.example.flowerapp.Exceptions.EntityNotFoundExceptions.FlowerNotFoundException;
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
    public FlowerResponseDTO addNewFlower(FlowerRequestDTO dto) {
        checkDuplicateFlowerName(dto.flowerName());

        Flower flower = new Flower();
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

        Flower saved = flowerRepository.save(flower);

        return mapToResponseDTO(saved);
    }

    @Transactional
    public FlowerResponseDTO updateFlower(FlowerRequestDTO dto, long id) {
        Flower flower = findFlowerByIdOrThrow(id);

        // Check for duplicate name only if the name is being changed
        if (!flower.getFlowerName().equals(dto.flowerName())) {
            checkDuplicateFlowerName(dto.flowerName());
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

        Flower updated = flowerRepository.save(flower);

        return mapToResponseDTO(updated);
    }

    public FlowerResponseDTO getFlowerById(long id) {
        Flower flower = findFlowerByIdOrThrow(id);
        return mapToResponseDTO(flower);
    }

    public List<FlowerResponseDTO> getAllFlowers() {
        return flowerRepository.findAllFlower()
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    public long getFlowerCount() {
        return flowerRepository.findAllFlower().size();
    }

    public List<FlowerResponseDTO> getAllFlowersBySpecies(String species) {
        return flowerRepository.findBySpecies(species)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    public List<FlowerResponseDTO> getAllFlowerByColor(String color) {
        return flowerRepository.findByColor(color)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    @Transactional
    public void deleteFlower(long id) {
        findFlowerByIdOrThrow(id);
        flowerRepository.deleteFlower(id);
    }

    private void checkDuplicateFlowerName(String flowerName) {
        if (flowerRepository.existsByName(flowerName)) {
            throw new DuplicateFlowerException("Flower name already exists!");
        }
    }

    private Flower findFlowerByIdOrThrow(long id) {
        flowerRepository.validateExists(id);
        return flowerRepository.findByFlowerId(id);
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
                flower.isAutoScheduling()
        );
    }
}