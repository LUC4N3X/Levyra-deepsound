package org.schabi.newpipe.extractor.services.bilibili.extractors;

import com.grack.nanojson.JsonParser;
import org.jsoup.nodes.Element;
import org.schabi.newpipe.extractor.bulletComments.BulletCommentsInfoItem;
import org.schabi.newpipe.extractor.bulletComments.BulletCommentsInfoItemExtractor;
import org.schabi.newpipe.extractor.exceptions.ParsingException;

import java.time.Duration;

public class BilibiliBulletCommentsInfoItemExtractor implements BulletCommentsInfoItemExtractor {
    private final Element element;
    private final String[] attr;

    public BilibiliBulletCommentsInfoItemExtractor(Element element) {
        this.element = element;
        attr = element.attr("p").split(",");
    }

    @Override
    public String getCommentText() throws ParsingException {
        String text = element.text();
        try{
            String possibleText = JsonParser.array().from(text).getString(4);
            return possibleText == null || possibleText.isEmpty() ? text : possibleText;
        } catch (Exception e) {
            return text;
        }
    }

    @Override
    public int getArgbColor() throws ParsingException {
        return Integer.parseInt(attr[3]) + 0xFF000000;
    }

    @Override
    public BulletCommentsInfoItem.Position getPosition() throws ParsingException {
        if(attr[1].equals("4")){
            return BulletCommentsInfoItem.Position.BOTTOM;
        } else if (attr[1].equals("5")) {
            return BulletCommentsInfoItem.Position.TOP;
        }
        return BulletCommentsInfoItem.Position.REGULAR;
    }

    @Override
    public double getRelativeFontSize() throws ParsingException {
        switch (attr[2]){
            case "18":
                return 0.5;
            case "25":
            default:
                return 0.6;
            case "36":
                return 0.7;
        }
    }

    @Override
    public Duration getDuration() throws ParsingException {
        return Duration.ofMillis((long) (Double.parseDouble(attr[0])*1000) + 2500); //2500 for sync
    }
}
