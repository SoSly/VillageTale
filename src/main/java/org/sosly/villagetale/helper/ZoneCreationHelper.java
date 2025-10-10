package org.sosly.villagetale.helper;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ZoneCreationHelper {
    private static ZoneCreationHelper instance;
    private final Set<UUID> playersInCreationMode = new HashSet<>();

    private ZoneCreationHelper() {
    }

    public static ZoneCreationHelper getInstance() {
        if (instance == null) {
            instance = new ZoneCreationHelper();
        }
        return instance;
    }

    public void setCreationMode(UUID playerId, boolean active) {
        if (active) {
            playersInCreationMode.add(playerId);
        } else {
            playersInCreationMode.remove(playerId);
        }
    }

    public boolean isInCreationMode(UUID playerId) {
        return playersInCreationMode.contains(playerId);
    }

    public void removePlayer(UUID playerId) {
        playersInCreationMode.remove(playerId);
    }
}
