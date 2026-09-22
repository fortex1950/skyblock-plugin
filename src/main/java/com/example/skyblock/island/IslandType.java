package com.example.skyblock.island;

public enum IslandType {
    CLASSIC("Klasicni", "Travnati SkyBlock otok s drvetom, sanducem, kravama za razmnozavanje "
            + "i manjim pjescanim otocicima s kaktusima okolo.");

    private final String displayName;
    private final String description;

    IslandType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /** Postoji samo jedan tip otoka, pa svaki prepoznati unos (ili prazan) vraca CLASSIC. */
    public static IslandType fromString(String s) {
        return CLASSIC;
    }
}
