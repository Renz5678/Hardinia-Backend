package org.example.flowerapp.Services;

import lombok.RequiredArgsConstructor;
import org.example.flowerapp.DTO.MaintenanceRequestDTO;
import org.example.flowerapp.DTO.MaintenanceResponseDTO;
import org.example.flowerapp.Exceptions.EntityNotFoundExceptions.FlowerNotFoundException;
import org.example.flowerapp.Exceptions.EntityNotFoundExceptions.MaintenanceNotFoundException;
import org.example.flowerapp.Models.Enums.MaintenanceType;
import org.example.flowerapp.Models.Flower;
import org.example.flowerapp.Models.Maintenance;
import org.example.flowerapp.Repository.FlowerRepository;
import org.example.flowerapp.Repository.MaintenanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private final MaintenanceRepository maintenanceRepository;
    private final FlowerRepository flowerRepository;

    @Transactional
    public MaintenanceResponseDTO addNewMaintenance(MaintenanceRequestDTO dto, String userId) {
        Flower flower = findFlowerByIdOrThrow(dto.flower_id(), userId);

        Maintenance maintenance = new Maintenance();
        maintenance.setFlower(flower);
        maintenance.setUserId(userId);  // Set userId on new maintenance
        maintenance.setTaskType(dto.maintenanceType());
        maintenance.setScheduledDate(dto.maintenanceDate());
        maintenance.setNotes(dto.notes());
        maintenance.setPerformedBy(dto.performedBy());
        maintenance.setCreatedAt(LocalDateTime.now());

        Maintenance saved = maintenanceRepository.save(maintenance);

        return mapToResponseDTO(saved);
    }

    @Transactional
    public MaintenanceResponseDTO updateMaintenance(MaintenanceRequestDTO dto, long taskId, String userId) {
        Maintenance maintenance = findMaintenanceByIdOrThrow(taskId, userId);
        Flower flower = findFlowerByIdOrThrow(dto.flower_id(), userId);

        maintenance.setFlower(flower);
        maintenance.setTaskType(dto.maintenanceType());
        maintenance.setScheduledDate(dto.maintenanceDate());
        maintenance.setNotes(dto.notes());
        maintenance.setPerformedBy(dto.performedBy());

        Maintenance saved = maintenanceRepository.save(maintenance);

        return mapToResponseDTO(saved);
    }

    public MaintenanceResponseDTO getMaintenanceById(long taskId, String userId) {
        Maintenance maintenance = findMaintenanceByIdOrThrow(taskId, userId);
        return mapToResponseDTO(maintenance);
    }

    public List<MaintenanceResponseDTO> getAllMaintenance(String userId) {
        return maintenanceRepository.findAllMaintenanceByUserId(userId)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    public List<MaintenanceResponseDTO> getMaintenanceByFlowerId(long flowerId, String userId) {
        return maintenanceRepository.findByFlowerIdAndUserId(flowerId, userId)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    public List<MaintenanceResponseDTO> getMaintenanceByType(MaintenanceType maintenanceType, String userId) {
        return maintenanceRepository.findByMaintenanceTypeAndUserId(maintenanceType, userId)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    public List<MaintenanceResponseDTO> getMaintenanceByDate(LocalDateTime dateTime, String userId) {
        return maintenanceRepository.findByMaintenanceDateAndUserId(dateTime, userId)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    @Transactional
    public void deleteMaintenance(long taskId, String userId) {
        findMaintenanceByIdOrThrow(taskId, userId);
        boolean deleted = maintenanceRepository.deleteMaintenance(taskId, userId);

        if (!deleted) {
            throw new MaintenanceNotFoundException(taskId);
        }
    }

    private Flower findFlowerByIdOrThrow(long flowerId, String userId) {
        if (!flowerRepository.existsByIdAndUserId(flowerId, userId)) {
            throw new FlowerNotFoundException("Flower with id " + flowerId + " not found");
        }
        return flowerRepository.findByFlowerIdAndUserId(flowerId, userId);
    }

    private Maintenance findMaintenanceByIdOrThrow(long taskId, String userId) {
        try {
            return maintenanceRepository.findByTaskIdAndUserId(taskId, userId);
        } catch (MaintenanceNotFoundException e) {
            throw new MaintenanceNotFoundException("Maintenance task with id " + taskId + " not found");
        }
    }

    private MaintenanceResponseDTO mapToResponseDTO(Maintenance maintenance) {
        return new MaintenanceResponseDTO(
                maintenance.getTask_id(),
                maintenance.getFlower().getFlower_id(),
                maintenance.getTaskType(),
                maintenance.getScheduledDate(),
                maintenance.getNotes(),
                maintenance.getPerformedBy(),
                maintenance.getCreatedAt()
        );
    }
}