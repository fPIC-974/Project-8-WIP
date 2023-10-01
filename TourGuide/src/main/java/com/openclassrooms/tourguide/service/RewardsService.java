package com.openclassrooms.tourguide.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import com.openclassrooms.tourguide.tracker.Tracker;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

@Service
public class RewardsService {
    private Logger logger = LoggerFactory.getLogger(RewardsService.class);
    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

    // proximity in miles
    private int defaultProximityBuffer = 10;
    private int proximityBuffer = defaultProximityBuffer;
    private int attractionProximityRange = 200;
    private final GpsUtil gpsUtil;
    private final RewardCentral rewardsCentral;
    private ExecutorService executorService = Executors.newFixedThreadPool(150);
    List<CompletableFuture<?>> completableFutures = new ArrayList<CompletableFuture<?>>();

    public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
        this.gpsUtil = gpsUtil;
        this.rewardsCentral = rewardCentral;
    }

    public void setProximityBuffer(int proximityBuffer) {
        this.proximityBuffer = proximityBuffer;
    }

    public void setDefaultProximityBuffer() {
        proximityBuffer = defaultProximityBuffer;
    }

    public void calculateRewards(User user) {
        CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(() -> {
            List<VisitedLocation> userLocations = new CopyOnWriteArrayList<>(user.getVisitedLocations());
            List<Attraction> attractions = gpsUtil.getAttractions();

            userLocations.forEach(visitedLocation -> {
                ExecutorService localExecutor = Executors.newFixedThreadPool(15);

                attractions.forEach(attraction -> {
                    if (user.getUserRewards().stream().noneMatch(r -> r.attraction.attractionName.equals(attraction.attractionName))) {
                        if (nearAttraction(visitedLocation, attraction)) {
                            CompletableFuture<UserReward> userReward = CompletableFuture.supplyAsync(() -> {
                                return getRewardPoints(attraction, user);
                            }, localExecutor).thenApplyAsync(points -> {
                                return new UserReward(visitedLocation, attraction, points);
                            });
                            CompletableFuture.completedFuture(userReward).thenRun(() -> user.addUserReward(userReward.join()));
                        }
                    }
                });
                localExecutor.shutdown();
            });
        }, executorService);
        completableFutures.add(completableFuture);
    }

    // Check if all threads in ExecutorService pool are done
    public boolean isExecutorEmpty() {
        List<CompletableFuture<?>> threadSafeList = new CopyOnWriteArrayList<>(completableFutures);
        boolean allDone = true;

        for (CompletableFuture<?> completableFuture: threadSafeList) {
            allDone &= completableFuture.isDone();
        }

        return allDone;
    }

    public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
        return getDistance(attraction, location) > attractionProximityRange ? false : true;
    }

    private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
        return getDistance(attraction, visitedLocation.location) > proximityBuffer ? false : true;
    }

    public int getRewardPoints(Attraction attraction, User user) {
        return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
    }

    public double getDistance(Location loc1, Location loc2) {
        double lat1 = Math.toRadians(loc1.latitude);
        double lon1 = Math.toRadians(loc1.longitude);
        double lat2 = Math.toRadians(loc2.latitude);
        double lon2 = Math.toRadians(loc2.longitude);

        double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

        double nauticalMiles = 60 * Math.toDegrees(angle);
        double statuteMiles = STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
        return statuteMiles;
    }

}
