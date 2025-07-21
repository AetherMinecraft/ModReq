package dev.bwmp.modReq.command.subcommands;

import dev.bwmp.modReq.ModReq;
import dev.bwmp.modReq.command.SubCommand;
import dev.bwmp.modReq.model.ModRequest;
import dev.bwmp.modReq.model.ModRequestNote;
import dev.bwmp.modReq.model.ModRequestStatus;
import dev.bwmp.modReq.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class InfoCommand extends SubCommand {

  private final ModReq plugin;

  public InfoCommand(ModReq plugin) {
    super("info", "modreq.use", "View detailed information about a mod request", "/modreq info <id>", false);
    this.plugin = plugin;
  }

  @Override
  public boolean execute(CommandSender sender, String[] args) {
    if (args.length != 1) {
      sendUsage(sender);
      return true;
    }

    int requestId;
    try {
      requestId = Integer.parseInt(args[0]);
    } catch (NumberFormatException e) {
      sender.sendMessage(TextUtil.error("Invalid request ID: " + args[0]));
      return true;
    }

    plugin.getModRequestService().getRequestWithNotes(requestId).thenAccept(request -> {
      plugin.getServer().getScheduler().runTask(plugin, () -> {
        if (request == null) {
          sender.sendMessage(TextUtil.error("Request #" + requestId + " not found."));
          return;
        }

        boolean canViewAll = sender.hasPermission("modreq.mod");
        boolean isOwner = sender instanceof org.bukkit.entity.Player &&
            ((org.bukkit.entity.Player) sender).getUniqueId().equals(request.getPlayerId());

        if (!canViewAll && !isOwner) {
          sender.sendMessage(TextUtil.error("You can only view your own requests!"));
          return;
        }

        displayRequestInfo(sender, request);
      });
    }).exceptionally(throwable -> {
      sender.sendMessage(TextUtil.error("An error occurred while retrieving the request."));
      throwable.printStackTrace();
      return null;
    });

    return true;
  }

  private void displayRequestInfo(CommandSender sender, ModRequest request) {
    String statusColor = getStatusColor(request.getStatus());
    boolean isPlayer = sender instanceof Player;
    boolean isMod = sender.hasPermission("modreq.mod");
    boolean isAdmin = sender.hasPermission("modreq.admin");
    boolean isOwner = isPlayer && ((Player) sender).getUniqueId().equals(request.getPlayerId());
    boolean isClaimedByUser = isPlayer && request.isClaimed() &&
        ((Player) sender).getUniqueId().equals(request.getClaimedBy());

    sender.sendMessage(TextUtil.highlight("═══ ModReq #" + request.getId() + " ═══"));
    sender.sendMessage(TextUtil.info("Player: &f" + request.getPlayerName()));

    String statusText = statusColor + request.getStatus().name();
    if (request.isClaimed()) {
      statusText += " &7(claimed by &f" + request.getClaimedByName() + "&7)";
    }
    sender.sendMessage(TextUtil.info("Status: " + statusText));

    sender.sendMessage(Component.text("Created: ", NamedTextColor.AQUA)
        .append(TextUtil.relativeTime(request.getCreatedAt(), "&f")));
    sender.sendMessage(Component.text("Updated: ", NamedTextColor.AQUA)
        .append(TextUtil.relativeTime(request.getUpdatedAt(), "&f")));

    if (request.getClosedAt() != null) {
      Component closedComponent = Component.text("Closed: ", NamedTextColor.AQUA)
          .append(TextUtil.relativeTime(request.getClosedAt(), "&f"));
      
      // Add who closed/completed it
      if (request.getStatus() == dev.bwmp.modReq.model.ModRequestStatus.COMPLETED && request.getCompletedByName() != null) {
        closedComponent = closedComponent
            .append(Component.text(" by ", NamedTextColor.GRAY))
            .append(Component.text(request.getCompletedByName(), NamedTextColor.WHITE));
      } else if (request.getStatus() == dev.bwmp.modReq.model.ModRequestStatus.CLOSED && request.getClosedByName() != null) {
        closedComponent = closedComponent
            .append(Component.text(" by ", NamedTextColor.GRAY))
            .append(Component.text(request.getClosedByName(), NamedTextColor.WHITE));
      }
      
      sender.sendMessage(closedComponent);
    }

    sender.sendMessage(TextUtil.info("Description:"));
    sender.sendMessage(TextUtil.format("  &f" + request.getDescription()));

    if (request.getWorldName() != null) {
      Component locationText = Component.text("Location: ", NamedTextColor.AQUA)
          .append(Component.text(request.getWorldName() + " ", NamedTextColor.WHITE))
          .append(Component.text("(" + String.format("%.0f, %.0f, %.0f",
              request.getX(), request.getY(), request.getZ()) + ")", NamedTextColor.GRAY));

      if (isPlayer && isMod) {
        locationText = locationText
            .clickEvent(ClickEvent.runCommand("/modreq teleport " + request.getId()))
            .hoverEvent(HoverEvent.showText(Component.text("Click to teleport to this location", NamedTextColor.GREEN)))
            .decorate(TextDecoration.UNDERLINED);
      }

      sender.sendMessage(locationText);
    }

    if (request.getNotes() != null && !request.getNotes().isEmpty()) {
      sender.sendMessage(TextUtil.info("Notes:"));
      for (ModRequestNote note : request.getNotes()) {
        Component noteComponent = Component.text("  [", NamedTextColor.GRAY)
            .append(TextUtil.relativeTime(note.getCreatedAt(), "&7"))
            .append(Component.text("] ", NamedTextColor.GRAY))
            .append(Component.text(note.getAuthorName(), NamedTextColor.WHITE))
            .append(Component.text(": ", NamedTextColor.GRAY))
            .append(Component.text(note.getContent(), NamedTextColor.WHITE));
        sender.sendMessage(noteComponent);
      }
    } else {
      sender.sendMessage(TextUtil.info("Notes: &8None"));
    }

    if (isPlayer) {
      Component actionBar = Component.empty();
      boolean hasActions = false;

      if (isMod) {
        if (!request.isClaimed()) {
          actionBar = actionBar
              .append(Component.text("[Claim]", NamedTextColor.GREEN)
                  .clickEvent(ClickEvent.runCommand("/modreq claim " + request.getId()))
                  .hoverEvent(HoverEvent.showText(Component.text("Click to claim this request", NamedTextColor.GREEN)))
                  .decorate(TextDecoration.BOLD));
          hasActions = true;
        } else if (isClaimedByUser) {
          actionBar = actionBar
              .append(Component.text("[Unclaim]", NamedTextColor.YELLOW)
                  .clickEvent(ClickEvent.runCommand("/modreq unclaim " + request.getId()))
                  .hoverEvent(
                      HoverEvent.showText(Component.text("Click to unclaim this request", NamedTextColor.YELLOW)))
                  .decorate(TextDecoration.BOLD));
          hasActions = true;
        } else if (isAdmin) {
          actionBar = actionBar
              .append(Component.text("[Force Claim]", NamedTextColor.RED)
                  .clickEvent(ClickEvent.runCommand("/modreq claim " + request.getId() + " -f"))
                  .hoverEvent(
                      HoverEvent.showText(Component.text("Click to force claim this request", NamedTextColor.RED)))
                  .decorate(TextDecoration.BOLD));
          hasActions = true;
        }

        if (request.getStatus() != ModRequestStatus.COMPLETED &&
            request.getStatus() != ModRequestStatus.CLOSED) {
          if (hasActions) {
            actionBar = actionBar.append(Component.text(" ", NamedTextColor.WHITE));
          }
          actionBar = actionBar
              .append(Component.text("[Mark Done]", NamedTextColor.BLUE)
                  .clickEvent(ClickEvent.runCommand("/modreq done " + request.getId()))
                  .hoverEvent(HoverEvent
                      .showText(Component.text("Click to mark this request as completed", NamedTextColor.BLUE)))
                  .decorate(TextDecoration.BOLD));
          hasActions = true;
        }
      }

      if (isOwner && request.getStatus() != ModRequestStatus.CLOSED) {
        if (hasActions) {
          actionBar = actionBar.append(Component.text(" ", NamedTextColor.WHITE));
        }
        actionBar = actionBar
            .append(Component.text("[Close]", NamedTextColor.GRAY)
                .clickEvent(ClickEvent.runCommand("/modreq close " + request.getId()))
                .hoverEvent(HoverEvent.showText(Component.text("Click to close this request", NamedTextColor.GRAY)))
                .decorate(TextDecoration.BOLD));
        hasActions = true;
      }

      if (hasActions) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Actions: ", NamedTextColor.AQUA).append(actionBar));
      }
    }

    sender.sendMessage(TextUtil.highlight("═".repeat(20)));
  }

  private String getStatusColor(ModRequestStatus status) {
    switch (status) {
      case OPEN:
        return "&a";
      case ELEVATED:
        return "&c";
      case COMPLETED:
        return "&2";
      case CLOSED:
        return "&8";
      default:
        return "&7";
    }
  }

  @Override
  public List<String> getTabCompletions(CommandSender sender, String[] args) {
    List<String> completions = new ArrayList<>();
    try {
      List<ModRequest> requests = plugin.getModRequestService().getOpenRequests().get();
      for (ModRequest request : requests) {
        completions.add(String.valueOf(request.getId()));
      }
    } catch (Exception e) {
      plugin.getLogger().warning("Error getting tab completions for claim command: " + e.getMessage());
    }
    return completions;
  }
}
