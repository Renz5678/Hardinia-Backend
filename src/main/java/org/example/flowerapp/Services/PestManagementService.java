package org.example.flowerapp.Services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.flowerapp.Models.Flower;
import org.example.flowerapp.Models.Maintenance;
import org.example.flowerapp.Models.Enums.MaintenanceType;
import org.example.flowerapp.Repository.FlowerRepository;
import org.example.flowerapp.Repository.MaintenanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@Slf4j
public class PestManagementService {

    private final FlowerRepository flowerRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final Random random;

    // 30% chance for pest infestation
    private static final double PEST_PROBABILITY = 0.30;

    // Constructor for production use
    @Autowired
    public PestManagementService(FlowerRepository flowerRepository,
                                 MaintenanceRepository maintenanceRepository) {
        this(flowerRepository, maintenanceRepository, new Random());
    }

    // Constructor for testing with injectable Random
    public PestManagementService(FlowerRepository flowerRepository,
                                 MaintenanceRepository maintenanceRepository,
                                 Random random) {
        this.flowerRepository = flowerRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.random = random;
    }

    // Run daily at 3 AM to check for pest infestations
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void checkForPestInfestations() {
        log.info("Starting daily pest infestation check...");

        // Get all active flowers (you can add additional filtering if needed)
        List<Flower> allFlowers = flowerRepository.findAllFlower();
        int infestationCount = 0;

        for (Flower flower : allFlowers) {
            try {
                // Check if flower already has an active pest control task
                boolean hasActivePestTask = maintenanceRepository
                        .existsByFlowerAndMaintenanceTypeAndCompletedFalse(
                                flower,
                                MaintenanceType.PEST_CONTROL
                        );

                if (hasActivePestTask) {
                    log.debug("Flower ID {} already has active pest control task. Skipping.",
                            flower.getFlower_id());
                    continue;
                }

                // Roll the dice - 30% chance of pest infestation
                if (shouldGetPestInfestation()) {
                    createPestControlTask(flower);
                    infestationCount++;
                    log.info("Pest infestation detected for flower ID {} ({})",
                            flower.getFlower_id(), flower.getFlowerName());
                }

            } catch (Exception e) {
                log.error("Error checking pest infestation for flower ID {}: {}",
                        flower.getFlower_id(), e.getMessage());
            }
        }

        log.info("Pest infestation check completed. {} new infestations detected out of {} flowers.",
                infestationCount, allFlowers.size());
    }

    /**
     * Determines if a flower should get a pest infestation (30% chance)
     */
    private boolean shouldGetPestInfestation() {
        return random.nextDouble() < PEST_PROBABILITY;
    }

    /**
     * Creates a pest control maintenance task for the given flower
     * This is called internally by the scheduled job (no auth needed)
     */
    private void createPestControlTask(Flower flower) {
        Maintenance pestTask = new Maintenance();

        pestTask.setFlower(flower);
        pestTask.setUserId(flower.getUserId());
        pestTask.setTaskType(MaintenanceType.PEST_CONTROL);
        pestTask.setScheduledDate(LocalDateTime.now());
        pestTask.setDueDate(LocalDateTime.now().plusDays(3)); // 3 days to treat pests
        pestTask.setCompleted(false);
        pestTask.setNotes("Treat " + flower.getFlowerName() + " with pesticide");
        pestTask.setPerformedBy("System");

        maintenanceRepository.save(pestTask);

        log.info("Created pest control task for flower ID {} ({})",
                flower.getFlower_id(), flower.getFlowerName());
    }

    /**
     * Manual trigger for testing purposes (can be called from a controller endpoint)
     */
    @Transactional
    public String triggerPestCheckManually() {
        log.info("Manual pest check triggered");
        checkForPestInfestations();
        return "Pest infestation check completed. Check logs for details.";
    }

    /**
     * Get pest infestation statistics
     */
    public PestStatistics getPestStatistics() {
        List<Maintenance> allPestTasks = maintenanceRepository
                .findByMaintenanceType(MaintenanceType.PEST_CONTROL);

        long activePestTasks = allPestTasks.stream()
                .filter(task -> !task.isCompleted())
                .count();

        long completedPestTasks = allPestTasks.stream()
                .filter(Maintenance::isCompleted)
                .count();

        return new PestStatistics(
                allPestTasks.size(),
                activePestTasks,
                completedPestTasks
        );
    }

    /**
     * Inner class for pest statistics
     */
    public static class PestStatistics {
        private final long totalPestTasks;
        private final long activePestTasks;
        private final long completedPestTasks;

        public PestStatistics(long totalPestTasks, long activePestTasks, long completedPestTasks) {
            this.totalPestTasks = totalPestTasks;
            this.activePestTasks = activePestTasks;
            this.completedPestTasks = completedPestTasks;
        }

        public long getTotalPestTasks() { return totalPestTasks; }
        public long getActivePestTasks() { return activePestTasks; }
        public long getCompletedPestTasks() { return completedPestTasks; }
    }
}