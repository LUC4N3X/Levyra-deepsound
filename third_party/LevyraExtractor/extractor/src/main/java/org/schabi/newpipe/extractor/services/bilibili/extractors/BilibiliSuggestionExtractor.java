package org.schabi.newpipe.extractor.services.bilibili.extractors;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.suggestion.SuggestionExtractor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.schabi.newpipe.extractor.services.bilibili.BilibiliService.GET_SUGGESTION_URL;
import static org.schabi.newpipe.extractor.services.bilibili.BilibiliService.WWW_REFERER;
import static org.schabi.newpipe.extractor.services.bilibili.BilibiliService.getHeaders;

public class BilibiliSuggestionExtractor extends SuggestionExtractor {
    public BilibiliSuggestionExtractor(StreamingService service) {
        super(service);
    }

    @Override
    public List<String> suggestionList(String query) throws IOException, ExtractionException {
        final String response = NewPipe.getDownloader().get(GET_SUGGESTION_URL + query, getHeaders(WWW_REFERER)).responseBody();
        List<String> resultList = new ArrayList<>();
        try {
            JsonArray respObject = JsonParser.object().from(response).getObject("result").getArray("tag");
            for(int i = 0; i < respObject.size(); i++){
                resultList.add(respObject.getObject(i).getString("value"));
            }
        } catch (JsonParserException e) {
            e.printStackTrace();
        }
        return resultList;
    }
}
