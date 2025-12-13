package org.example.flowerapp.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.flowerapp.DTO.GrowthRequestDTO;
import org.example.flowerapp.DTO.GrowthResponseDTO;
import org.example.flowerapp.Models.Enums.GrowthStage;
import org.example.flowerapp.Services.GrowthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/growth")
@RequiredArgsConstructor
public class GrowthController {
    private final GrowthService growthService;

    @PostMapping
    public ResponseEntity<GrowthResponseDTO> createNewGrowthDetail(
            @Valid @RequestBody GrowthRequestDTO dto) {
        GrowthResponseDTO created = growthService.addNewGrowth(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<GrowthResponseDTO>> getAllGrowthDetails() {
        List<GrowthResponseDTO> growthList = growthService.getAllGrowthDetails();
        return ResponseEntity.ok(growthList);
    }

    @GetMapping("/{growth_id}")
    public ResponseEntity<GrowthResponseDTO> getGrowthById(
            @PathVariable("growth_id") long growthId) {
        GrowthResponseDTO growth = growthService.getGrowthById(growthId);
        return ResponseEntity.ok(growth);
    }

    @GetMapping("/stage/{growth_stage}")
    public ResponseEntity<List<GrowthResponseDTO>> getGrowthByStage(
            @PathVariable("growth_stage") GrowthStage stage) {
        List<GrowthResponseDTO> growth = growthService.getGrowthByStage(stage);
        return ResponseEntity.ok(growth);
    }

    @GetMapping("/color-changes")
    public ResponseEntity<List<GrowthResponseDTO>> getGrowthByColorChanges(
            @RequestParam boolean colorChanges) {
        List<GrowthResponseDTO> growth = growthService.getGrowthByColorChanges(colorChanges);
        return ResponseEntity.ok(growth);
    }

    @PutMapping("/{growth_id}")
    public ResponseEntity<GrowthResponseDTO> updateGrowthDetails(
            @Valid @RequestBody GrowthRequestDTO dto,
            @PathVariable("growth_id") long growthId) {
        GrowthResponseDTO updated = growthService.updateGrowth(dto, growthId);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{growth_id}")
    public ResponseEntity<Void> deleteGrowth(@PathVariable("growth_id") long growthId) {
        growthService.deleteGrowth(growthId);
        return ResponseEntity.noContent().build();
    }
}