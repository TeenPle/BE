package com.shu.backend.global.websocket;

import com.fasterxml.jackson.databind.JsonNode;

public record ChatRealtimeMessage(
        String originId,
        String destination,
        JsonNode payload
) {
}
