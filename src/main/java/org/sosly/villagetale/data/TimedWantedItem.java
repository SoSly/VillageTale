package org.sosly.villagetale.data;

import org.sosly.villagetale.api.IWantedItem;

public class TimedWantedItem {
    private final IWantedItem item;
    private final long expiryTime;
    
    public TimedWantedItem(IWantedItem item, long expiryTime) {
        this.item = item;
        this.expiryTime = expiryTime;
    }
    
    public IWantedItem getItem() {
        return item;
    }
    
    public long getExpiryTime() {
        return expiryTime;
    }
    
    public boolean isExpired(long currentTime) {
        return currentTime >= expiryTime;
    }
    
    public boolean matches(IWantedItem other) {
        if (other == null) {
            return false;
        }
        return item.getClass().equals(other.getClass());
    }
}
