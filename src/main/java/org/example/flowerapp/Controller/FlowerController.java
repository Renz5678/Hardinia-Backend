package org.example.flowerapp.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.flowerapp.DTO.FlowerRequestDTO;
import org.example.flowerapp.DTO.FlowerResponseDTO;
import org.example.flowerapp.Services.FlowerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/flowers")
@RequiredArgsConstructor
public class FlowerController {

    private final FlowerService flowerService;

    @PostMapping
    public ResponseEntity<FlowerResponseDTO> createFlower(@Valid @RequestBody FlowerRequestDTO dto) {
        FlowerResponseDTO created = flowerService.addNewFlower(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<FlowerResponseDTO>> getAllFlowers() {
        List<FlowerResponseDTO> flowers = flowerService.getAllFlowers();
        return ResponseEntity.ok(flowers);
    }

    @GetMapping("/number_of_flowers")
    public ResponseEntity<Long> getNumberOfFlowers() {
        return ResponseEntity.ok(flowerService.getFlowerCount());
    }

    @GetMapping("/{flower_id}")
    public ResponseEntity<FlowerResponseDTO> getFlowerById(@PathVariable("flower_id") long flowerId) {
        FlowerResponseDTO flower = flowerService.getFlowerById(flowerId);
        return ResponseEntity.ok(flower);
    }

    @GetMapping("/species/{species}")
    public ResponseEntity<List<FlowerResponseDTO>> getFlowersBySpecies(@PathVariable String species) {
        List<FlowerResponseDTO> flowers = flowerService.getAllFlowersBySpecies(species);
        return ResponseEntity.ok(flowers);
    }

    @GetMapping("/color/{color}")
    public ResponseEntity<List<FlowerResponseDTO>> getFlowersByColor(@PathVariable String color) {
        List<FlowerResponseDTO> flowers = flowerService.getAllFlowerByColor(color);
        return ResponseEntity.ok(flowers);
    }

    @PutMapping("/{flower_id}")
    public ResponseEntity<FlowerResponseDTO> updateFlower(
            @Valid @RequestBody FlowerRequestDTO dto,
            @PathVariable("flower_id") long flowerId) {
        FlowerResponseDTO updated = flowerService.updateFlower(dto, flowerId);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{flower_id}")
    public ResponseEntity<Void> deleteFlower(@PathVariable("flower_id") long flowerId) {
        flowerService.deleteFlower(flowerId);
        return ResponseEntity.noContent().build();
    }
}