package com.example.supportassist;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * Manages the real-time WebSocket connection for SupportAssist.
 */
public class WebSocketManager {
    private static final String TAG = "WebSocketManager";
    
    private static WebSocketManager instance;
    private final OkHttpClient client;
    private final Gson gson;
    private WebSocket webSocket;
    private final Handler mainHandler;
    
    private boolean isAuth = false;
    private String currentToken;
    private final Set<String> activeSubscriptions = new HashSet<>();
    private final Set<MessageListener> listeners = new HashSet<>();
    
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_DELAY = 30000;
    private boolean isClosing = false;

    public interface MessageListener {
        void onMessageCreated(String ticketId, Message message);
        void onConnectionStateChanged(boolean connected);
    }

    private WebSocketManager() {
        client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS) // Keep connection open
                .pingInterval(30, TimeUnit.SECONDS)   // Send heartbeat every 30s
                .build();
        gson = new Gson();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized WebSocketManager getInstance() {
        if (instance == null) {
            instance = new WebSocketManager();
        }
        return instance;
    }

    /**
     * Connects to the WebSocket server using the provided token.
     */
    public void connect(String token) {
        if (token == null || token.isEmpty()) {
            disconnect();
            return;
        }

        // If already connected with the same token, ensure auth is attempted if missing
        if (webSocket != null && token.equals(currentToken)) {
            if (!isAuth && !isClosing) authenticate();
            return;
        }
        
        // Close existing if token changed
        if (webSocket != null) {
            webSocket.close(1000, "New token or manual re-connection");
        }
        
        this.currentToken = token;
        this.isClosing = false;
        this.isAuth = false;
        
        String url = ApiClient.getWebSocketUrl();
        Log.d(TAG, "Connecting to WebSocket: " + url);
        Request request = new Request.Builder().url(url).build();
        webSocket = client.newWebSocket(request, new SupportAssistWebSocketListener());
    }

    private void authenticate() {
        if (webSocket == null || currentToken == null) return;
        
        JsonObject auth = new JsonObject();
        auth.addProperty("type", "auth");
        auth.addProperty("token", currentToken);
        webSocket.send(auth.toString());
        Log.d(TAG, "Sent WebSocket auth request");
    }

    /**
     * Subscribe to a specific ticket's message events.
     */
    public void subscribe(String ticketId) {
        if (ticketId == null) return;
        activeSubscriptions.add(ticketId);
        if (isAuth && webSocket != null) {
            JsonObject sub = new JsonObject();
            sub.addProperty("type", "subscribe");
            sub.addProperty("ticketId", ticketId);
            webSocket.send(sub.toString());
            Log.d(TAG, "Subscribing to ticket: " + ticketId);
        }
    }

    /**
     * Unsubscribe from a ticket when leaving the screen.
     */
    public void unsubscribe(String ticketId) {
        if (ticketId == null) return;
        activeSubscriptions.remove(ticketId);
        if (isAuth && webSocket != null) {
            JsonObject unsub = new JsonObject();
            unsub.addProperty("type", "unsubscribe");
            unsub.addProperty("ticketId", ticketId);
            webSocket.send(unsub.toString());
            Log.d(TAG, "Unsubscribing from ticket: " + ticketId);
        }
    }

    public void addListener(MessageListener listener) {
        listeners.add(listener);
    }

    public void removeListener(MessageListener listener) {
        listeners.remove(listener);
    }

    /**
     * Gracefully disconnect from the server (e.g., on logout).
     */
    public void disconnect() {
        isClosing = true;
        if (webSocket != null) {
            webSocket.close(1000, "User logout or disconnect");
            webSocket = null;
        }
        isAuth = false;
        currentToken = null;
        activeSubscriptions.clear();
    }

    private class SupportAssistWebSocketListener extends WebSocketListener {
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            Log.d(TAG, "WebSocket Opened");
            reconnectAttempts = 0;
            authenticate();
            notifyConnectionState(true);
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            Log.d(TAG, "WS Received: " + text);
            try {
                JsonObject json = gson.fromJson(text, JsonObject.class);
                String type = json.has("type") ? json.get("type").getAsString() : "";

                switch (type) {
                    case "authenticated":
                        isAuth = true;
                        Log.d(TAG, "WS Authenticated. Restoring subscriptions...");
                        for (String id : activeSubscriptions) {
                            subscribe(id);
                        }
                        break;
                    case "ticket.message.created":
                        String tId = json.has("ticketId") ? json.get("ticketId").getAsString() : null;
                        Message msg = gson.fromJson(json.get("message"), Message.class);
                        mainHandler.post(() -> {
                            for (MessageListener l : listeners) l.onMessageCreated(tId, msg);
                        });
                        break;
                    case "ping":
                        // Server heartbeat
                        JsonObject pong = new JsonObject();
                        pong.addProperty("type", "pong");
                        webSocket.send(pong.toString());
                        break;
                    case "error":
                        Log.e(TAG, "WebSocket error from server: " + text);
                        break;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing WebSocket message", e);
            }
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            Log.d(TAG, "WebSocket Closed: " + reason);
            handleDisconnect(webSocket);
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            Log.e(TAG, "WebSocket Failure", t);
            handleDisconnect(webSocket);
        }

        private void handleDisconnect(WebSocket ws) {
            isAuth = false;
            if (WebSocketManager.this.webSocket == ws) {
                WebSocketManager.this.webSocket = null;
            }
            notifyConnectionState(false);
            if (!isClosing) attemptReconnect();
        }
    }

    private void notifyConnectionState(boolean connected) {
        mainHandler.post(() -> {
            for (MessageListener l : listeners) l.onConnectionStateChanged(connected);
        });
    }

    private void attemptReconnect() {
        if (isClosing || currentToken == null) return;
        reconnectAttempts++;
        // Exponential backoff
        long delay = Math.min((long) Math.pow(2, reconnectAttempts) * 1000, MAX_RECONNECT_DELAY);
        Log.d(TAG, "Attempting reconnect in " + delay + "ms");
        mainHandler.postDelayed(() -> connect(currentToken), delay);
    }
}
