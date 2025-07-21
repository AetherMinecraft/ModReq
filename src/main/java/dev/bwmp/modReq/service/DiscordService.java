package dev.bwmp.modReq.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.bwmp.modReq.ModReq;
import dev.bwmp.modReq.config.ConfigManager;
import dev.bwmp.modReq.model.ModRequest;
import dev.bwmp.modReq.model.ModRequestNote;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DiscordService {

  private final ModReq plugin;
  private final ConfigManager configManager;
  private final HttpClient httpClient;

  public DiscordService(ModReq plugin) {
    this.plugin = plugin;
    this.configManager = plugin.getConfigManager();
    this.httpClient = HttpClient.newHttpClient();
  }

  public CompletableFuture<Void> sendRequestNotification(ModRequest request, String eventType, String staffMember,
      List<ModRequestNote> notes) {
    if (!isDiscordEnabled()) {
      return CompletableFuture.completedFuture(null);
    }

    return CompletableFuture.runAsync(() -> {
      try {
        JsonObject payload = createWebhookPayload(request, eventType, staffMember, notes);
        sendWebhook(payload);
      } catch (Exception e) {
        plugin.getLogger().warning("Failed to send Discord notification: " + e.getMessage());
      }
    });
  }

  public CompletableFuture<Void> sendRequestCreated(ModRequest request) {
    return sendRequestNotification(request, "created", null, null);
  }

  public CompletableFuture<Void> sendRequestCompleted(ModRequest request, String staffMember,
      List<ModRequestNote> notes) {
    return sendRequestNotification(request, "completed", staffMember, notes);
  }

  public CompletableFuture<Void> sendRequestElevated(ModRequest request, String staffMember,
      List<ModRequestNote> notes) {
    return sendRequestNotification(request, "elevated", staffMember, notes);
  }

  public CompletableFuture<Void> sendRequestClosed(ModRequest request, String staffMember,
      List<ModRequestNote> notes) {
    return sendRequestNotification(request, "closed", staffMember, notes);
  }

  private boolean isDiscordEnabled() {
    return configManager.getBoolean("discord.enabled", false) &&
        !configManager.getString("discord.webhook_url", "").isEmpty();
  }

  private JsonObject createWebhookPayload(ModRequest request, String eventType, String staffMember,
      List<ModRequestNote> notes) {
    JsonObject payload = new JsonObject();
    JsonArray embeds = new JsonArray();
    JsonObject embed = new JsonObject();

    String color = getEmbedColor(eventType);
    embed.addProperty("color", Integer.parseInt(color.substring(1), 16));

    String title = getEmbedTitle(eventType, request.getId());
    embed.addProperty("title", title);

    embed.addProperty("description", request.getDescription());

    JsonArray fields = new JsonArray();

    JsonObject playerField = new JsonObject();
    playerField.addProperty("name", "Player");
    playerField.addProperty("value", request.getPlayerName());
    playerField.addProperty("inline", true);
    fields.add(playerField);

    JsonObject statusField = new JsonObject();
    statusField.addProperty("name", "Status");
    statusField.addProperty("value", request.getStatus().name());
    statusField.addProperty("inline", true);
    fields.add(statusField);

    if (request.getWorldName() != null) {
      JsonObject locationField = new JsonObject();
      locationField.addProperty("name", "Location");
      locationField.addProperty("value", String.format("%s (%d, %d, %d)",
          request.getWorldName(), (int) request.getX(), (int) request.getY(), (int) request.getZ()));
      locationField.addProperty("inline", false);
      fields.add(locationField);
    }

    if (staffMember != null) {
      JsonObject staffField = new JsonObject();
      staffField.addProperty("name", eventType.equals("completed") ? "Completed by" : "Elevated by");
      staffField.addProperty("value", staffMember);
      staffField.addProperty("inline", true);
      fields.add(staffField);
    }

    if (notes != null && !notes.isEmpty()) {
      StringBuilder notesText = new StringBuilder();
      for (ModRequestNote note : notes) {
        if (notesText.length() > 0)
          notesText.append("\n");
        notesText.append("**").append(note.getAuthorName()).append("**: ").append(note.getContent());
      }

      JsonObject notesField = new JsonObject();
      notesField.addProperty("name", "Notes");
      notesField.addProperty("value", notesText.toString());
      notesField.addProperty("inline", false);
      fields.add(notesField);
    }

    embed.add("fields", fields);

    embed.addProperty("timestamp", Instant.now().toString());

    JsonObject footer = new JsonObject();
    footer.addProperty("text", "ModReq #" + request.getId());
    embed.add("footer", footer);

    embeds.add(embed);
    payload.add("embeds", embeds);

    return payload;
  }

  private String getEmbedColor(String eventType) {
    switch (eventType) {
      case "created":
        return configManager.getString("discord.opened_embed_color", "#00FF00");
      case "elevated":
        return configManager.getString("discord.elevated_embed_color", "#0000FF");
      case "completed":
        return configManager.getString("discord.closed_embed_color", "#FF0000");
      case "closed":
        return configManager.getString("discord.closed_embed_color", "#303030");
      default:
        return "#FFFFFF";
    }
  }

  private String getEmbedTitle(String eventType, int requestId) {
    switch (eventType) {
      case "created":
        return "New ModReq Created - #" + requestId;
      case "elevated":
        return "ModReq Elevated - #" + requestId;
      case "completed":
        return "ModReq Completed - #" + requestId;
      case "closed":
        return "ModReq Closed - #" + requestId;
      default:
        return "ModReq Update - #" + requestId;
    }
  }

  private void sendWebhook(JsonObject payload) throws IOException, InterruptedException {
    String webhookUrl = configManager.getString("discord.webhook_url", "");
    if (webhookUrl.isEmpty()) {
      return;
    }

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(webhookUrl))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
        .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      plugin.getLogger().warning("Discord webhook failed with status: " + response.statusCode() +
          " - " + response.body());
    }
  }
}
