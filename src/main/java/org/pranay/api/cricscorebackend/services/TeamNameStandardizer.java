package org.pranay.api.cricscorebackend.services;

import java.util.HashMap;
import java.util.Map;

public class TeamNameStandardizer {
    private static final Map<String, String> TEAM_NAME_MAPPING = new HashMap<>();

    static {
        TEAM_NAME_MAPPING.put("AUS", "Australia");
        TEAM_NAME_MAPPING.put("PAK", "Pakistan");
        TEAM_NAME_MAPPING.put("IND", "India");
        TEAM_NAME_MAPPING.put("ENG", "England");
        TEAM_NAME_MAPPING.put("NZ", "New Zealand");
        TEAM_NAME_MAPPING.put("SA", "South Africa");
        TEAM_NAME_MAPPING.put("WI", "West Indies");
        TEAM_NAME_MAPPING.put("SL", "Sri Lanka");
        TEAM_NAME_MAPPING.put("BAN", "Bangladesh");
        TEAM_NAME_MAPPING.put("AFG", "Afghanistan");
        TEAM_NAME_MAPPING.put("ZIM", "Zimbabwe");
        TEAM_NAME_MAPPING.put("IRE", "Ireland");
        TEAM_NAME_MAPPING.put("SCO", "Scotland");
        TEAM_NAME_MAPPING.put("UAE", "United Arab Emirates");
        TEAM_NAME_MAPPING.put("NEP", "Nepal");
        // Add more mappings as needed
    }

    public static String standardizeTeamName(String teamName) {
        if (teamName == null) return null;

        // First check if it's already a full name
        String cleanedName = teamName.trim();
        if (TEAM_NAME_MAPPING.containsValue(cleanedName)) {
            return cleanedName;
        }

        // Then check if it's a code that needs to be converted
        String upperCaseTeam = cleanedName.toUpperCase();
        return TEAM_NAME_MAPPING.getOrDefault(upperCaseTeam, cleanedName);
    }
}