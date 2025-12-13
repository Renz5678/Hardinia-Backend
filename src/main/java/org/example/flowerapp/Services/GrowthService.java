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
    public GrowthResponseDTO addNewGrowth(GrowthRequestDTO dto) {
        Flower flower = findFlowerByIdOrThrow(dto.flower_id());

        Growth growth = new Growth();
        updateGrowthFromDTO(growth, dto, flower);

        Growth saved = growthRepository.save(growth);
        return mapToResponseDTO(saved);
    }

    @Transactional
    public GrowthResponseDTO updateGrowth(GrowthRequestDTO dto, long id) {
        Growth growth = findGrowthByIdOrThrow(id);
        Flower flower = findFlowerByIdOrThrow(dto.flower_id());

        updateGrowthFromDTO(growth, dto, flower);

        Growth saved = growthRepository.save(growth);
        return mapToResponseDTO(saved);
    }

    public GrowthResponseDTO getGrowthById(long id) {
        Growth growth = findGrowthByIdOrThrow(id);
        return mapToResponseDTO(growth);
    }

    public List<GrowthResponseDTO> getAllGrowthDetails() {
        return growthRepository.findAllGrowth()
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    public List<GrowthResponseDTO> getGrowthByStage(GrowthStage stage) {
        return growthRepository.findByStage(stage)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    public List<GrowthResponseDTO> getGrowthByColorChanges(boolean colorChanges) {
        return growthRepository.findByColorChanges(colorChanges)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    @Transactional
    public void deleteGrowth(long id) {
        findGrowthByIdOrThrow(id);
        growthRepository.deleteGrowth(id);
    }

    private void updateGrowthFromDTO(Growth growth, GrowthRequestDTO dto, Flower flower) {
        growth.setFlower(flower);
        growth.setStage(dto.stage());
        growth.setHeight(dto.height());
        growth.setColorChanges(dto.colorChanges());
        growth.setNotes(dto.notes());
    }

    private Flower findFlowerByIdOrThrow(long flowerId) {
        if (flowerRepository.existsById(flowerId)) {
            throw new FlowerNotFoundException("Flower with id " + flowerId + " not found");
        }
        return flowerRepository.findByFlowerId(flowerId);
    }

    private Growth findGrowthByIdOrThrow(long id) {
        Growth growth = growthRepository.findByGrowthId(id);
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
                growth.getNotes()
        );
    }
}