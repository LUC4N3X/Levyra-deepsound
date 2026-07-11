package org.schabi.newpipe.extractor.services.bilibili;

import com.grack.nanojson.JsonObject;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class WatchDataCache {
    private long cid;
    private long roomId;
    private long startTime;
    private String bvid;

    // Fuck you auto-enqueueing
    private long lastCid;
    private String currentUrl;
    private String lastUrl;
    private final Map<String, Long> cidMap = new HashMap<>();
    private final Map<String, String> bvidMap = new HashMap<>();

    public long getCid(String id) {
        return cidMap.get(id);
    }

    public void setCid(String id, long cid) {
        this.cid = cid;
        cidMap.put(id, cid);
    }

    public long getRoomId() {
        return roomId;
    }

    public void setRoomId(long roomId) {
        this.roomId = roomId;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setBvid(String id, String bvid) {
        this.bvid = bvid;
        bvidMap.put(id, bvid);
    }

    public String getBvid(String id) {
        return bvidMap.get(id);
    }

    public void init(String url){
        if(url.equals(currentUrl)){
            return ;
        }
        lastCid = cid;
        lastUrl = currentUrl;
        currentUrl = url;
    }

    public long getLastCid() {
        return lastCid;
    }

    public String getLastUrl() {
        return lastUrl;
    }

    public String getCurrentUrl() {
        return currentUrl;
    }
}
