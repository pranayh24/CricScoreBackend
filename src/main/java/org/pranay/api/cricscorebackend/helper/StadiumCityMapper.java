package org.pranay.api.cricscorebackend.helper;

import java.util.HashMap;
import java.util.Map;

public class StadiumCityMapper {
    private static final Map<String, String> stadiumToCity = new HashMap<String, String>() {{
        // Major Indian Stadiums
        put("Narendra Modi Stadium", "Ahmedabad");
        put("M Chinnaswamy Stadium", "Bangalore");
        put("Arun Jaitley Stadium", "Delhi");
        put("Eden Gardens", "Kolkata");
        put("Wankhede Stadium", "Mumbai");
        put("MA Chidambaram Stadium", "Chennai");
        put("Punjab Cricket Association Stadium", "Chandigarh");
        put("Rajiv Gandhi International Stadium", "Hyderabad");
        put("Bharat Ratna Shri Atal Bihari Vajpayee Ekana Cricket Stadium", "Lucknow");
        put("Holkar Stadium", "Indore");

        // Major International Stadiums
        put("Melbourne Cricket Ground", "Melbourne");
        put("Sydney Cricket Ground", "Sydney");
        put("The Gabba", "Brisbane");
        put("Adelaide Oval", "Adelaide");
        put("WACA Ground", "Perth");
        put("Lord's Cricket Ground", "London");
        put("The Oval", "London");
        put("Trent Bridge", "Nottingham");
        put("Headingley", "Leeds");
        put("Old Trafford", "Manchester");
        put("Eden Park", "Auckland");
        put("Basin Reserve", "Wellington");
        put("Hagley Oval", "Christchurch");
        put("R Premadasa Stadium", "Colombo");
        put("Pallekele International Cricket Stadium", "Kandy");
        put("Galle International Stadium", "Galle");
        put("Shere Bangla National Stadium", "Dhaka");
        put("Zahur Ahmed Chowdhury Stadium", "Chattogram");
        put("National Stadium", "Karachi");
        put("Gaddafi Stadium", "Lahore");
        put("Rawalpindi Cricket Stadium", "Rawalpindi");
        put("SuperSport Park", "Centurion");
        put("The Wanderers Stadium", "Johannesburg");
        put("Newlands", "Cape Town");
        put("Kensington Oval", "Bridgetown");
        put("Sabina Park", "Kingston");
        put("Queens Sports Club", "Bulawayo");
        put("Harare Sports Club", "Harare");
        put("Dubai International Cricket Stadium", "Dubai");
        put("Sheikh Zayed Stadium", "Abu Dhabi");
    }};

    // Method to get city from full venue string
    public static String getCityFromVenue(String matchNumberVenue) {
        if (matchNumberVenue == null) return "";

        // Split by bullet point if present
        String[] parts = matchNumberVenue.split("•");
        String venue = parts.length > 1 ? parts[1].trim() : matchNumberVenue.trim();

        // Remove "at" prefix if present
        venue = venue.replaceAll("(?i)^\\s*at\\s+", "").trim();

        // Try to find an exact match first
        for (Map.Entry<String, String> entry : stadiumToCity.entrySet()) {
            if (venue.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        // If no match found, try to extract city from the venue string
        String[] venueParts = venue.split(",");
        if (venueParts.length > 0) {
            // Take the last part as it usually contains the city name
            String lastPart = venueParts[venueParts.length - 1].trim();
            // Remove any parenthetical content
            lastPart = lastPart.replaceAll("\\s*\\(.*\\)", "").trim();
            return lastPart;
        }

        return "";
    }

    // Method to add new stadium-city mapping
    public static void addStadiumMapping(String stadium, String city) {
        stadiumToCity.put(stadium, city);
    }

    // Method to get all mappings
    public static Map<String, String> getAllMappings() {
        return new HashMap<>(stadiumToCity);
    }
}