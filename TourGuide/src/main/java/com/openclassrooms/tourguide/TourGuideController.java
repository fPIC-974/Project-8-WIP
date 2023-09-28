package com.openclassrooms.tourguide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.openclassrooms.tourguide.service.RewardsService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;

import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

import tripPricer.Provider;

@RestController
public class TourGuideController {

	@Autowired
	TourGuideService tourGuideService;

    @Autowired
    RewardsService rewardsService;
	
    @RequestMapping("/")
    public String index() {
        return "Greetings from TourGuide!";
    }
    
    @RequestMapping("/getLocation") 
    public VisitedLocation getLocation(@RequestParam String userName) throws ExecutionException, InterruptedException {
    	return tourGuideService.getUserLocation(getUser(userName));
    }
    
    //  TODO: Change this method to no longer return a List of Attractions.
 	//  Instead: Get the closest five tourist attractions to the user - no matter how far away they are.
 	//  Return a new JSON object that contains:
    	// Name of Tourist attraction, 
        // Tourist attractions lat/long, 
        // The user's location lat/long, 
        // The distance in miles between the user's location and each of the attractions.
        // The reward points for visiting each Attraction.
        //    Note: Attraction reward points can be gathered from RewardsCentral
    @RequestMapping("/getNearbyAttractions") 
    public String getNearbyAttractions(@RequestParam String userName) throws ExecutionException, InterruptedException {
    	VisitedLocation visitedLocation = tourGuideService.getUserLocation(getUser(userName));

        List<Attraction> attractionList = tourGuideService.getNearByAttractions(visitedLocation);
        List<JSONObject> jsonAttractionList = new ArrayList<>();

        attractionList.parallelStream().forEach(a -> {
            Map<String, String> attraction = new HashMap<>();
            attraction.put("attraction", a.attractionName);
            attraction.put("aLatitude", String.valueOf(a.latitude));
            attraction.put("aLongitude", String.valueOf(a.longitude));
            attraction.put("uLatitude", String.valueOf(visitedLocation.location.latitude));
            attraction.put("uLongitude", String.valueOf(visitedLocation.location.longitude));
            attraction.put("distance", String.valueOf(rewardsService.getDistance(visitedLocation.location, a)));
            attraction.put("points", String.valueOf(rewardsService.getRewardPoints(a, getUser(userName))));

            jsonAttractionList.add(new JSONObject(attraction));
        });

        return jsonAttractionList.toString();
    }
    
    @RequestMapping("/getRewards") 
    public List<UserReward> getRewards(@RequestParam String userName) {
    	return tourGuideService.getUserRewards(getUser(userName));
    }
       
    @RequestMapping("/getTripDeals")
    public List<Provider> getTripDeals(@RequestParam String userName) {
    	return tourGuideService.getTripDeals(getUser(userName));
    }
    
    private User getUser(String userName) {
    	return tourGuideService.getUser(userName);
    }
   

}