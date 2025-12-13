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
    public MaintenanceResponseDTO addNewMaintenance(MaintenanceRequestDTO dto) {
        Flower flower = findFlowerByIdOrThrow(dto.flower_id());

        Maintenance maintenance = new Maintenance();
        maintenance.setFlower(flower);
        maintenance.setTaskType(dto.maintenanceType());
        maintenance.setScheduledDate(dto.maintenanceDate());
        maintenance.setNotes(dto.notes());
        maintenance.setPerformedBy(dto.performedBy());
        maintenance.setCreatedAt(LocalDateTime.now());

        Maintenance saved = maintenanceRepository.save(maintenance);

        return mapToResponseDTO(saved);
    }

    @Transactional
    public MaintenanceResponseDTO updateMaintenance(MaintenanceRequestDTO dto, long taskId) {
        Maintenance maintenance = findMaintenanceByIdOrThrow(taskId);
        Flower flower = findFlowerByIdOrThrow(dto.flower_id());

        maintenance.setFlower(flower);
        maintenance.setTaskType(dto.maintenanceType());
        maintenance.setScheduledDate(dto.maintenanceDate());
        maintenance.setNotes(dto.notes());
        maintenance.setPerformedBy(dto.performedBy());

        Maintenance saved = maintenanceRepository.save(maintenance);

        return mapToResponseDTO(saved);
    }

    public MaintenanceResponseDTO getMaintenanceById(long taskId) {
        Maintenance maintenance = findMaintenanceByIdOrThrow(taskId);
        return mapToResponseDTO(maintenance);
    }

    public List<MaintenanceResponseDTO> getAllMaintenance() {
        return maintenanceRepository.findAllMaintenance()
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    public List<MaintenanceResponseDTO> getMaintenanceByFlowerId(long flowerId) {
        return maintenanceRepository.findByFlowerId(flowerId)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    public List<MaintenanceResponseDTO> getMaintenanceByType(MaintenanceType maintenanceType) {
        return maintenanceRepository.findByMaintenanceType(maintenanceType)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    public List<MaintenanceResponseDTO> getMaintenanceByDate(LocalDateTime dateTime) {
        return maintenanceRepository.findByMaintenanceDate(dateTime)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    @Transactional
    public void deleteMaintenance(long taskId) {
        findMaintenanceByIdOrThrow(taskId);
        boolean deleted = maintenanceRepository.deleteMaintenance(taskId);

        if (!deleted) {
            throw new MaintenanceNotFoundException(taskId);
        }
    }

    private Flower findFlowerByIdOrThrow(long flowerId) {
        if (flowerRepository.existsById(flowerId)) {
            throw new FlowerNotFoundException("Flower with id " + flowerId + " not found");
        }
        return flowerRepository.findByFlowerId(flowerId);
    }

    private Maintenance findMaintenanceByIdOrThrow(long taskId) {
        try {
            return maintenanceRepository.findByTaskId(taskId);
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