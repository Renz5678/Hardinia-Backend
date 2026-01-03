package org.example.flowerapp.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.flowerapp.DTO.GrowthRequestDTO;
import org.example.flowerapp.DTO.GrowthResponseDTO;
import org.example.flowerapp.Models.Enums.GrowthStage;
import org.example.flowerapp.Services.GrowthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/growth")
@RequiredArgsConstructor
public class GrowthController {
    private final GrowthService growthService;

    @PostMapping
    public ResponseEntity<GrowthResponseDTO> createNewGrowthDetail(
            @Valid @RequestBody GrowthRequestDTO dto,
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        GrowthResponseDTO created = growthService.addNewGrowth(dto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<GrowthResponseDTO>> getAllGrowthDetails(
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        List<GrowthResponseDTO> growthList = growthService.getAllGrowthDetails(userId);
        return ResponseEntity.ok(growthList);
    }

    @GetMapping("/{growth_id}")
    public ResponseEntity<GrowthResponseDTO> getGrowthById(
            @PathVariable("growth_id") long growthId,
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        GrowthResponseDTO growth = growthService.getGrowthById(growthId, userId);
        return ResponseEntity.ok(growth);
    }

    @GetMapping("/flower/{flower_id}")
    public ResponseEntity<List<GrowthResponseDTO>> getGrowthByFlowerId(
            @PathVariable("flower_id") long flowerId,
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        List<GrowthResponseDTO> growth = growthService.getGrowthByFlowerId(flowerId, userId);
        return ResponseEntity.ok(growth);
    }

    @GetMapping("/stage/{growth_stage}")
    public ResponseEntity<List<GrowthResponseDTO>> getGrowthByStage(
            @PathVariable("growth_stage") GrowthStage stage,
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        List<GrowthResponseDTO> growth = growthService.getGrowthByStage(stage, userId);
        return ResponseEntity.ok(growth);
    }

    @GetMapping("/color-changes")
    public ResponseEntity<List<GrowthResponseDTO>> getGrowthByColorChanges(
            @RequestParam boolean colorChanges,
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        List<GrowthResponseDTO> growth = growthService.getGrowthByColorChanges(colorChanges, userId);
        return ResponseEntity.ok(growth);
    }

    @PutMapping("/{growth_id}")
    public ResponseEntity<GrowthResponseDTO> updateGrowthDetails(
            @Valid @RequestBody GrowthRequestDTO dto,
            @PathVariable("growth_id") long growthId,
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        GrowthResponseDTO updated = growthService.updateGrowth(dto, growthId, userId);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{growth_id}")
    public ResponseEntity<Void> deleteGrowth(
            @PathVariable("growth_id") long growthId,
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        growthService.deleteGrowth(growthId, userId);
        return ResponseEntity.noContent().build();
    }
}