package com.feel.gems.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side tracking of shadow clone owner UUIDs for skin rendering.
 */
public final class ClientShadowCloneState {
    private static final Map<Integer, UUID> CLONE_OWNERS = new ConcurrentHashMap<>();
    
    private ClientShadowCloneState() {
    }
    
    /**
     * Register a shadow clone's owner for skin lookup.
     */
    public static void setOwner(int entityId, UUID ownerUuid) {
        if (ownerUuid != null) {
            CLONE_OWNERS.put(entityId, ownerUuid);
        }
    }
    
    /**
     * Get the owner UUID for a shadow clone entity.
     */
    public static UUID getOwner(int entityId) {
        return CLONE_OWNERS.get(entityId);
    }
    
    /**
     * Remove tracking for an entity (called when entity is removed).
     */
    public static void remove(int entityId) {
        CLONE_OWNERS.remove(entityId);
    }
    
    /**
     * Clear all tracked clones (called on disconnect).
     */
    public static void reset() {
        CLONE_OWNERS.clear();
    }
}
