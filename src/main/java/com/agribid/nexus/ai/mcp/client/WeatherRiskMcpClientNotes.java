package com.agribid.nexus.ai.mcp.client;

/**
 * There is no Java code to write for the client connection itself —
 * spring-ai-starter-mcp-client auto-configures a SyncMcpToolCallbackProvider
 * bean purely from properties. This class exists only to document the
 * required application.properties entries in one discoverable place:
 *
 *   spring.ai.mcp.client.streamable-http.connections.weather-risk.url=http://weather-risk-service:8081
 *
 * Once that property is set, the auto-configured SyncMcpToolCallbackProvider
 * bean (injected directly into SpringAiConfig.negotiationChatClient) exposes
 * the remote Weather-Risk server's tools through the exact same
 * ChatClient.defaultToolCallbacks(...) interface used for our own internal
 * @Tool beans — Gemini can't tell the difference between an internal tool
 * and a tool federated in from a completely separate service, which is the
 * entire point of consuming MCP rather than hardcoding a weather API call.
 *
 * If the property above is absent (e.g. in a local dev profile with no
 * weather-risk service running), Spring AI's auto-configuration simply
 * registers zero remote tools rather than failing startup — reserve-price
 * and negotiation calls still work, just without weather-risk grounding.
 */
public final class WeatherRiskMcpClientNotes {
    private WeatherRiskMcpClientNotes() {
    }
}