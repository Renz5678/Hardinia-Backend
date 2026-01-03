package org.example.flowerapp.Services;

import lombok.RequiredArgsConstructor;
import org.example.flowerapp.DTO.GrowthRequestDTO;
import org.example.flowerapp.DTO.GrowthResponseDTO;
import org.example.flowerapp.Exceptions.EntityNotFoundExceptions.FlowerNotFoundException;
import org.example.flowerapp.Exceptions.EntityNotFoundExceptions.GrowthNotFoundException;
import org.example.flowerapp.Models.Enums.GrowthStage;
import org.example.flowerapp.Models.Flower;
import org.example.flowerapp.Models.Growth;
import org.example.flowerapp.Repository.FlowerRepository;
import org.example.flowerapp.Repository.GrowthRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GrowthService {

    private final GrowthRepository growthRepository;
    private final FlowerRepository flowerRepository;

    @Transactional
    public GrowthResponseDTO addNewGrowth(GrowthRequestDTO dto, String userId) {
        Flower flower = findFlowerByIdOrThrow(dto.flower_id(), userId);

        Growth growth = new Growth();
        growth.setUserId(userId);  // Set userId on new growth
        updateGrowthFromDTO(growth, dto, flower);

        Growth saved = growthRepository.save(growth);
        return mapToResponseDTO(saved);
    }

    @Transactional
    public GrowthResponseDTO updateGrowth(GrowthRequestDTO dto, long id, String userId) {
        Growth growth = findGrowthByIdOrThrow(id, userId);
        Flower flower = findFlowerByIdOrThrow(dto.flower_id(), userId);

        updateGrowthFromDTO(growth, dto, flower);

        Growth saved = growthRepository.save(growth);
        return mapToResponseDTO(saved);
    }

    public List<GrowthResponseDTO> getGrowthByFlowerId(long flowerId, String userId) {
        findFlowerByIdOrThrow(flowerId, userId);
        return growthRepository.findByFlowerIdAndUserId(flowerId, userId)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    public GrowthResponseDTO getGrowthById(long id, String userId) {
        Growth growth = findGrowthByIdOrThrow(id, userId);
        return mapToResponseDTO(growth);
    }

    public List<GrowthResponseDTO> getAllGrowthDetails(String userId) {
        return growthRepository.findAllGrowthByUserId(userId)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    public List<GrowthResponseDTO> getGrowthByStage(GrowthStage stage, String userId) {
        return growthRepository.findByStageAndUserId(stage, userId)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    public List<GrowthResponseDTO> getGrowthByColorChanges(boolean colorChanges, String userId) {
        return growthRepository.findByColorChangesAndUserId(colorChanges, userId)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    @Transactional
    public void deleteGrowth(long id, String userId) {
        findGrowthByIdOrThrow(id, userId);
        growthRepository.deleteGrowth(id, userId);
    }

    private void updateGrowthFromDTO(Growth growth, GrowthRequestDTO dto, Flower flower) {
        growth.setFlower(flower);
        growth.setStage(dto.stage());
        growth.setHeight(dto.height());
        growth.setColorChanges(dto.colorChanges());
        growth.setNotes(dto.notes());
    }

    private Flower findFlowerByIdOrThrow(long flowerId, String userId) {
        if (!flowerRepository.existsByIdAndUserId(flowerId, userId)) {
            throw new FlowerNotFoundException("Flower with id " + flowerId + " not found");
        }
        return flowerRepository.findByFlowerIdAndUserId(flowerId, userId);
    }

    private Growth findGrowthByIdOrThrow(long id, String userId) {
        Growth growth = growthRepository.findByGrowthIdAndUserId(id, userId);
        if (growth == null) {
            throw new GrowthNotFoundException("Growth Details with id " + id + " not found");
        }
        return growth;
    }

    private GrowthResponseDTO mapToResponseDTO(Growth growth) {
        return new GrowthResponseDTO(
                growth.getGrowth_id(),
                growth.getFlower().getFlower_id(),
                growth.getStage(),
                growth.getHeight(),
                growth.isColorChanges(),
                growth.getNotes(),
                growth.getRecordedAt(),
                growth.getGrowthSinceLast()
        );
    }
}