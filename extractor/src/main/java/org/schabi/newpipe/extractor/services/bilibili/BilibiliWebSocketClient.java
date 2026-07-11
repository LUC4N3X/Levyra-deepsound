package org.schabi.newpipe.extractor.services.bilibili;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class BilibiliWebSocketClient {
    private final String token;
    WrappedWebSocketClient webSocketClient;
    long id;
    private final AtomicBoolean shouldStop = new AtomicBoolean(false);

    private final ArrayList<JsonObject> messages  = new ArrayList<>();

    public class WrappedWebSocketClient extends WebSocketClient {
        private ScheduledExecutorService executor;

        public WrappedWebSocketClient() throws URISyntaxException {
            super(new URI("wss://broadcastlv.chat.bilibili.com/sub"), BilibiliService.getWebSocketHeaders());
            this.setConnectionLostTimeout(0);
        }
        public byte[] encode(String data, int op) throws IOException {
            byte[] dataByte = data.getBytes(StandardCharsets.UTF_8);
            int length = dataByte.length;
            byte[] result = {0, 0, 0, 0, 0, 16, 0, 1, 0, 0, 0, (byte) op, 0, 0, 0, 1};
            for(int i = 0; i < 4; i++){
                result[i] = (byte) ((16 + length)/Math.pow(256, 3 - i));
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
            outputStream.write(result);
            outputStream.write(dataByte);
            return outputStream.toByteArray();
        }
        public int readInt(byte[] bytes, int start, int len){
            int result = 0;
            for(int i=len - 1;i >= 0;i--){
                result += Math.pow(256,len - i - 1) * bytes[start + i];
            }
            return result;
        }
        public String authPacket(){
            return  String.format("{\"uid\":0,\"roomid\":%s,\"protover\":3,\"platform\":\"web\",\"clientver\":\"1.4.0\",\"type\":2, \"key\":\"%s\"}", id, token);
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            try {
                send(encode(authPacket(), 7));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            executor = Executors.newSingleThreadScheduledExecutor();
            executor.scheduleAtFixedRate(() -> {
                try {
                    while (!shouldStop.get()) {
                        send(encode("",2));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }, 30000, 30000, TimeUnit.MILLISECONDS);
        }

        @Override
        public void onMessage(String message) {

        }

        @Override
        public void onMessage(ByteBuffer byteBuffer) {
            byte[] data = byteBuffer.array();
            int op = readInt(data,8,4);
            if(op != 5){
                return ;
            }
            int type = readInt(data, 6,2);
            byte[] body = Arrays.copyOfRange(data, 16, data.length);
            String rawJson = null;
            if(type == 0){
                rawJson = new String(body);
            } else if (type == 2) {
                rawJson = new String(utils.decompressZlib(body));
            } else if (type == 3) {
                try {
                    rawJson = utils.decompressBrotli(body);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            final String regex = "[\\x00-\\x1f]+";

            String[] groups = rawJson.split(regex);
            for(String group:groups){
                JsonObject result = null;
                try{
                    result = JsonParser.object().from(group);
                    messages.add(result);
                } catch (JsonParserException ignored) {

                }
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            System.out.println(code);
            if(code != -1 && !shouldStop.get()){
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
            System.out.println(ex.fillInStackTrace());
        }
        public void stopTimer() {
            try {
                executor.shutdown();
            } catch (Exception ignored){

            }

        }
    }
    public BilibiliWebSocketClient(long id, String token) throws URISyntaxException {
        this.id = id;
        this.token = token;
        webSocketClient = new WrappedWebSocketClient();
    }

    public WrappedWebSocketClient getWebSocketClient() {
        return webSocketClient;
    }
    public void wrappedReconnect() throws URISyntaxException, InterruptedException {
        webSocketClient.stopTimer();
        webSocketClient = new WrappedWebSocketClient();
        webSocketClient.connectBlocking();
    }
    public ArrayList<JsonObject> getMessages() {
        ArrayList<JsonObject> temp = (ArrayList<JsonObject>) messages.clone();
        messages.clear();
        return temp;
    }
    public void disconnect(){
        shouldStop.set(true);
        webSocketClient.closeConnection(-1, "Scheduled terminate");
        webSocketClient.stopTimer();
    }
}
