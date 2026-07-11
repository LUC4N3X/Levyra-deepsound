package org.schabi.newpipe.extractor.services.niconico.extractors;

import com.grack.nanojson.JsonObject;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.bulletComments.BulletCommentsExtractor;
import org.schabi.newpipe.extractor.bulletComments.BulletCommentsInfoItem;
import org.schabi.newpipe.extractor.bulletComments.BulletCommentsInfoItemsCollector;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NiconicoBulletCommentsExtractor extends BulletCommentsExtractor {

    private JsonObject watch;
    @Nonnull
    private final NiconicoWatchDataCache watchDataCache;
    @Nonnull
    private final NiconicoCommentsCache commentsCache;
    private boolean isLive = true;
    private ScheduledExecutorService executor;
    private ScheduledFuture<?> future;
    private String lastContinuation;
    private final CopyOnWriteArrayList<byte[]> messages = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Integer> IDList= new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<String> URLList= new CopyOnWriteArrayList<>();

    public NiconicoBulletCommentsExtractor(
            final StreamingService service,
            final ListLinkHandler uiHandler,
            @Nonnull final NiconicoWatchDataCache watchDataCache,
            @Nonnull final NiconicoCommentsCache commentsCache) {
        super(service, uiHandler);
        this.watchDataCache = watchDataCache;
        this.commentsCache = commentsCache;
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        if(watchDataCache.getThreadServer() == null){
            isLive = false;
            this.watch = watchDataCache.refreshAndGetWatchData(downloader, getId());
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor();
        future = executor.scheduleAtFixedRate(this::fetchMessage, 1000, 10000, TimeUnit.MILLISECONDS);
    }

    private void fetchMessage() {
        if(watchDataCache.getThreadServer() == null){
            return;
        }
        try {
            byte[] response = getDownloader().get(watchDataCache.getThreadServer() + "?at=" + (System.currentTimeMillis() / 1000)).rawResponseBody();
            try {
//                List<String> segments = BulletComment.ADAPTER.decode(response).segments;
                String urls = new String(response, StandardCharsets.UTF_8);
                String urlPattern = "(https://).*?(?=[^a-zA-Z0-9./_-])";
                Pattern pattern = Pattern.compile(urlPattern);
                Matcher matcher = pattern.matcher(urls);
                while (matcher.find()) {
                    String url = matcher.group();
                    if(url.contains("/segment/")) {
                        new Thread(() -> {
                            try {
                                messages.addAll(decodeString(getDownloader().get(url).rawResponseBody()));
                            } catch (IOException | ReCaptchaException e) {
                                throw new RuntimeException(e);
                            }
                        }).start();
                    }
                }
//                List<String> segments = BulletComment.ADAPTER.decode(response).segments;
//                for (String segment : segments) {
//                    response = getDownloader().get(segment).rawResponseBody();
//                    messages.add(response);
//                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (IOException | ReCaptchaException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<BulletCommentsInfoItem> getLiveMessages() throws ParsingException {
        if(!isLive) {
            return null;
        }
        final BulletCommentsInfoItemsCollector collector =
                new BulletCommentsInfoItemsCollector(getServiceId());
//        for(final byte[] s: messages) {
//            try {
//                List<BulletComment.MessageItem> decodeMessages = BulletComment.ADAPTER.decode(s).items;
//                for (final BulletComment.MessageItem message : decodeMessages) {
//                    collector.commit(new NiconicoBulletCommentsNewInfoItemExtractor(message));
//                }
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        }
        for (final byte[] s: messages) {
            int id = decodeVarInt(s, (byte) 0x40);
            if(id == -1) {
                continue;
            }
            if (!IDList.contains(id)) {
                collector.commit(new NiconicoBulletCommentsNewInfoItemExtractor(s, watchDataCache.getStartAt()));
                IDList.add(id);
            }
        }
        messages.clear();
        return new InfoItemsPage<>(collector, null).getItems();
    }

    @Nonnull
    @Override
    public InfoItemsPage<BulletCommentsInfoItem> getInitialPage()
            throws IOException, ExtractionException {
        final BulletCommentsInfoItemsCollector collector =
                new BulletCommentsInfoItemsCollector(getServiceId());
        if(getId().contains("live.nicovideo.jp")){
            return new InfoItemsPage<>(collector, null);
        }
        for (final JsonObject comment : commentsCache
                .getComments(watch, getDownloader(), getId())) {
            collector.commit(new NiconicoBulletCommentsInfoItemExtractor(comment, getUrl(), watchDataCache.getStartAt(), false));
        }
        return new InfoItemsPage<>(collector, null);
    }

    @Override
    public InfoItemsPage<BulletCommentsInfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        return null;
    }
    @Override
    public boolean isLive() {
        return isLive;
    }

    @Override
    public void disconnect() {
        if(future != null && !future.isCancelled()){
            future.cancel(true);
        }
    }
    @Override
    public void reconnect() {
        if(!isDisabled() && future != null && future.isCancelled()){
            future = executor.scheduleAtFixedRate(this::fetchMessage, 1000, 10000, TimeUnit.MILLISECONDS);
        }
    }

    /*
    Byte helpers
     */

    public static int decodeVarInt(byte[] data, byte startByte) {
        int start = -1;
        for (int i = 0; i < data.length; i++) {
            if (data[i] == startByte) {
                start = i + 1;
                break;
            }
        }
        if(start == -1) {
            return -1;
        }
        int position = start;

        while (position < data.length) {
            int value = data[position] & 0xFF;
            if ((value & 0x80) == 0) {
                break;
            }
            position++;
        }

        int result = 0;
        for (int i = 0; i < position + 1 - start; i++) {
            result |= (data[start + i] & 0x7f) << (i * 7);
        }

        return result;
    }


    public static List<byte[]> splitByByte(byte[] array, byte delimiter) {
        List<byte[]> byteArrays = new ArrayList<>();
        int start = 0;

        for (int i = 0; i < array.length; i++) {
            if (array[i] == delimiter) {
                byte[] part = Arrays.copyOfRange(array, start, i);
                byteArrays.add(part);
                start = i + 1;
            }
        }

        if (start < array.length) {
            byte[] part = Arrays.copyOfRange(array, start, array.length);
            byteArrays.add(part);
        }

        return byteArrays;
    }

    public static boolean contains(byte[] array, byte[] sequence) {
        if (sequence.length == 0) {
            return true;
        }
        if (sequence.length > array.length) {
            return false;
        }
        for (int i = 0; i <= array.length - sequence.length; i++) {
            boolean found = true;
            for (int j = 0; j < sequence.length; j++) {
                if (array[i + j] != sequence[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return true;
            }
        }
        return false;
    }


    public static ArrayList<byte[]> decodeString(byte[] decodedBytes) { //type 0: meta, 1: segment
        ArrayList<byte[]> result = new ArrayList<>();

        try {

            // Split the decoded string into lines
            List<byte[]> lines = splitByByte(decodedBytes, (byte) 0x0A);

            for (byte[] line : lines) {
                if (contains(line, new byte[]{(byte) 0x61, (byte) 0x3A})) {
                    result.add(line);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }
}
