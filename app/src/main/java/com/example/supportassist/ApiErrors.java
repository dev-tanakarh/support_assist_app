package com.example.supportassist;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import retrofit2.Response;

/**
 * Retrofit only runs the success converter on 2xx responses — for anything
 * else, response.body() is null and the actual JSON (including the
 * backend's "message" field, e.g. "This account has been deactivated.")
 * sits unparsed in response.errorBody(). This pulls it out.
 */
public final class ApiErrors {
    private ApiErrors() { }

    public static String extractMessage(Response<?> response, String fallback) {
        try {
            if (response.errorBody() != null) {
                String raw = response.errorBody().string();
                JsonObject obj = new Gson().fromJson(raw, JsonObject.class);
                if (obj != null && obj.has("message") && !obj.get("message").isJsonNull()) {
                    return obj.get("message").getAsString();
                }
            }
        } catch (Exception ignored) {
            // Malformed/unreadable error body — just fall through to the fallback below.
        }
        return fallback;
    }
}
