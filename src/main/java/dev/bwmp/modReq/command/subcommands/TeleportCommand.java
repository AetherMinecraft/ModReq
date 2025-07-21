package dev.bwmp.modReq.command.subcommands;

import dev.bwmp.modReq.ModReq;
import dev.bwmp.modReq.command.SubCommand;
import dev.bwmp.modReq.model.ModRequest;
import dev.bwmp.modReq.util.TextUtil;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TeleportCommand extends SubCommand {

  private final ModReq plugin;

  public TeleportCommand(ModReq plugin) {
    super("teleport", "modreq.mod", "Teleport to a mod request location", "/modreq teleport <id>", true);
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

    Player player = (Player) sender;

    plugin.getModRequestService().getRequest(requestId).thenAccept(request -> {
      plugin.getServer().getScheduler().runTask(plugin, () -> {
        if (request == null) {
          player.sendMessage(TextUtil.error("Request #" + requestId + " not found."));
          return;
        }

        Location location = request.getLocation();
        if (location == null) {
          player.sendMessage(TextUtil.error("No location data for request #" + requestId + " or world not loaded."));
          return;
        }

        location.add(0, 1, 0);

        player.teleport(location);
        player
            .sendMessage(TextUtil.success("Teleported to request #" + requestId + " by &f" + request.getPlayerName()));
        player.sendMessage(TextUtil.info("Description: &f" + request.getDescription()));
      });
    }).exceptionally(throwable -> {
      player.sendMessage(TextUtil.error("An error occurred while getting the request location."));
      throwable.printStackTrace();
      return null;
    });

    return true;
  }

  @Override
  public List<String> getTabCompletions(CommandSender sender, String[] args) {
    List<String> completions = new ArrayList<>();
    if (args.length == 1) {
      try {
        List<ModRequest> requests = plugin.getModRequestService().getOpenRequests().get();
        Player player = (Player) sender;

        for (ModRequest request : requests) {
          if (request.getClaimedBy() != null && request.getClaimedBy().equals(player.getUniqueId())) {
            completions.add(String.valueOf(request.getId()));
          }
        }
      } catch (Exception e) {
        plugin.getLogger().warning("Error getting tab completions for unclaim command: " + e.getMessage());
      }
    }
    return completions;
  }
}
