package com.openclassrooms.tourguide.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

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
    private ExecutorService executorService = Executors.newFixedThreadPool(500);

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

    public void calculateRewards1(User user) {
//        logger.info("IN");
        List<VisitedLocation> userLocations = new CopyOnWriteArrayList<>(user.getVisitedLocations());
        List<Attraction> attractions = gpsUtil.getAttractions();

        userLocations.forEach(visitedLocation -> {
            List<CompletableFuture<Void>> completableFutureList = new ArrayList<>();
            attractions.forEach(attraction -> {
                if (user.getUserRewards().stream().filter(r -> r.attraction.attractionName.equals(attraction.attractionName)).count() == 0) {
                    CompletableFuture<Void> ending = CompletableFuture.runAsync(() -> {
                        if (nearAttraction(visitedLocation, attraction)) {
                            CompletableFuture<UserReward> userReward = CompletableFuture.supplyAsync(() -> {
                                int points = getRewardPoints(attraction, user);
                                return points;
                            }, executorService).thenApplyAsync(points -> {
                                UserReward reward = new UserReward(visitedLocation, attraction, points);
                                return reward;
                            });
                            CompletableFuture.completedFuture(userReward).thenRun(() -> user.addUserReward(userReward.join()));
                        }
                    }, executorService);
                    completableFutureList.add(ending);
                }
            });
            CompletableFuture.allOf(completableFutureList.toArray(new CompletableFuture[completableFutureList.size()])).join();
        });

        /*for (VisitedLocation visitedLocation: userLocations) {
            for (Attraction attraction : attractions) {
                if (user.getUserRewards().stream().filter(r -> r.attraction.attractionName.equals(attraction.attractionName)).count() == 0) {
                    if (nearAttraction(visitedLocation, attraction)) {
                        user.addUserReward(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
                    }
                }
            }
        }*/
//        logger.info("OUT");
    }

    public void calculateRewards(User user) {
        List<VisitedLocation> userLocations = new CopyOnWriteArrayList<>(user.getVisitedLocations());
        List<Attraction> attractions = gpsUtil.getAttractions();

        /*for (VisitedLocation visitedLocation : userLocations) {
            List<CompletableFuture<Void>> completableFutureList = new ArrayList<>();
            for (Attraction attraction : attractions) {
                if (user.getUserRewards().stream().filter(r -> r.attraction.attractionName.equals(attraction.attractionName)).count() == 0) {
                    CompletableFuture<Void> ending = CompletableFuture.runAsync(() -> {
                        if (nearAttraction(visitedLocation, attraction)) {
                            CompletableFuture<UserReward> userReward = CompletableFuture.supplyAsync(() -> {
                                int points = getRewardPoints(attraction, user);
                                return points;
                            }, executorService).thenApplyAsync(points -> {
                                UserReward reward = new UserReward(visitedLocation, attraction, points);
                                return reward;
                            });
                            CompletableFuture.completedFuture(userReward).thenRun(() -> user.addUserReward(userReward.join()));
                        }
                    }, executorService);
                    completableFutureList.add(ending);
                }
            }
            CompletableFuture.allOf(completableFutureList.toArray(new CompletableFuture[completableFutureList.size()])).join();
        }*/

//        CompletableFuture.supplyAsync(() -> {
//            logger.info(user.getUserName());
        for (VisitedLocation visitedLocation : userLocations) {
            List<CompletableFuture<Void>> completableFutureList = new ArrayList<>();
            for (Attraction attraction : attractions) {
                if (user.getUserRewards().stream().filter(r -> r.attraction.attractionName.equals(attraction.attractionName)).count() == 0) {
                    CompletableFuture<Void> ending = CompletableFuture.runAsync(() -> {
                        if (nearAttraction(visitedLocation, attraction)) {
                            CompletableFuture<UserReward> userReward = CompletableFuture.supplyAsync(() -> {
                                int points = getRewardPoints(attraction, user);
                                return points;
                            }, executorService).thenApplyAsync(points -> {
                                UserReward reward = new UserReward(visitedLocation, attraction, points);
                                return reward;
                            });
                            CompletableFuture.completedFuture(userReward).thenRun(() -> user.addUserReward(userReward.join()));
                        }
                    }, executorService);
                    completableFutureList.add(ending);
                }
            }
            CompletableFuture.allOf(completableFutureList.toArray(new CompletableFuture[completableFutureList.size()])).join();
        }
            /*return this;
        }).join();*/

        /*// ORIG
        for (VisitedLocation visitedLocation: userLocations) {
            for (Attraction attraction : attractions) {
                if (user.getUserRewards().stream().filter(r -> r.attraction.attractionName.equals(attraction.attractionName)).count() == 0) {
                    if (nearAttraction(visitedLocation, attraction)) {
                        user.addUserReward(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
                    }
                }
            }
        }*/
    }

    public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
        return getDistance(attraction, location) > attractionProximityRange ? false : true;
    }

    private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
        return getDistance(attraction, visitedLocation.location) > proximityBuffer ? false : true;
    }

    private int getRewardPoints(Attraction attraction, User user) {
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
