package org.schabi.newpipe.extractor.services.niconico;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.schabi.newpipe.extractor.services.bilibili.BilibiliWebSocketClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Map;

import javax.annotation.Nonnull;

public class NicoWebSocketClient  {
    WrappedWebSocketClient webSocketClient;
    private final URI serverUri;
    private final Map<String, String> httpHeaders;

    private String serverUrl;
    private String threadId;
    public String url;
    private int retryTimes = 0;
    private Map<String, String> pageIndicators;

    private final ArrayList<JsonObject> messages  = new ArrayList<>();
    public class WrappedWebSocketClient extends WebSocketClient {
        public int type;
        private boolean shouldSkip = false;

    public WrappedWebSocketClient() {
            super(serverUri, httpHeaders);
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            send("{\"type\":\"startWatching\",\"data\":{\"stream\":{\"quality\":\"super_high\",\"protocol\":\"hls+fmp4\",\"latency\":\"low\",\"chasePlay\":false},\"room\":{\"protocol\":\"webSocket\",\"commentable\":true},\"reconnect\":false}}");
        }

        @Override
        public void onMessage(String message) {
            if (message.equals("{\"type\":\"ping\"}")){
                send("{\"type\":\"pong\"}");
                send("{\"type\":\"keepSeat\"}");
            }
            else {
                try {
                    JsonObject data = JsonParser.object().from(message);
                    if (data.has("chat") && !shouldSkip
                            && !data.getObject("chat").getString("content").startsWith("/nicoad")
                            && !data.getObject("chat").getString("content").startsWith("/info")
                            && ! data.getObject("chat").getString("content").startsWith("/gift")) {
                        messages.add(data.getObject("chat"));
                    }else if(data.has("type")){
                        if(data.getString("type").equals("stream")){
                            url = data.getObject("data").getString("uri");
                        }else if(data.getString("type").equals("messageServer")){
                            serverUrl = data.getObject("data").getString("viewUri");
                        }
                    } else if(data.has("ping")){
                        String content = data.getObject("ping").getString("content");
                        if(content.equals("pf:0")){
                            shouldSkip = false;
                        }
                    }
                } catch (JsonParserException ignored) {
                    ;
                }
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            if(type != 0 && code != -1){
                try {
                    wrappedReconnect();
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public void onError(Exception ex) {
            System.out.println(ex);
        }
    }
    public NicoWebSocketClient(URI serverUri, Map<String, String> httpHeaders){
        this.serverUri = serverUri;
        this.httpHeaders = httpHeaders;
        webSocketClient = new WrappedWebSocketClient();
    }
    public WrappedWebSocketClient getWebSocketClient() {
        return webSocketClient;
    }
    public void wrappedReconnect() throws URISyntaxException, InterruptedException {
        webSocketClient = new WrappedWebSocketClient();
        webSocketClient.connect();
    }
    public ArrayList<JsonObject> getMessages() {
        ArrayList<JsonObject> temp = (ArrayList<JsonObject>) messages.clone();
        messages.clear();
        return temp;
    }
    public void disconnect(){
        webSocketClient.closeConnection(-1, "Scheduled terminate");
    }
    public boolean hasUrl(){
        return url !=  null;
    }

    public String getUrl() {
        return url;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }
}
