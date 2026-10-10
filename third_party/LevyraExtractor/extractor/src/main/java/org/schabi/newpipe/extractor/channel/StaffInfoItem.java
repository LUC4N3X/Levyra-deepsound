package org.schabi.newpipe.extractor.channel;

import org.schabi.newpipe.extractor.InfoItem;

public class StaffInfoItem extends InfoItem {

    /**
     * The contribution or job of this staff
     */
    private final String title;

    public StaffInfoItem(int serviceId, String url, String name, String title, String avatarThumbnail) {
        super(InfoType.STAFF, serviceId, url, name);
        this.title = title;
        this.setThumbnailUrl(avatarThumbnail);
    }

    public String getTitle() {
        return title;
    }


    public ChannelInfoItem toChannelInfoItem() {
        return new ChannelInfoItem(getServiceId(), getUrl(), getName());
    }
}
