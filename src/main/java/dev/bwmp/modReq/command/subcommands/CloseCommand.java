package dev.bwmp.modReq.command.subcommands;

import dev.bwmp.modReq.ModReq;
import dev.bwmp.modReq.command.SubCommand;
import dev.bwmp.modReq.model.ModRequest;
import dev.bwmp.modReq.util.TextUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CloseCommand extends SubCommand {

  private final ModReq plugin;

  public CloseCommand(ModReq plugin) {
    super("close", "modreq.mod", "Close a mod request", "/modreq close <id>", true);
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

    plugin.getModRequestService().closeRequest(requestId, player).thenAccept(request -> {
      plugin.getServer().getScheduler().runTask(plugin, () -> {
        if (request != null) {
          player.sendMessage(TextUtil.success("Successfully closed request #" + requestId));

          plugin.getDiscordService().sendRequestClosed(request, player.getName(), request.getNotes());

          plugin.getServer().getOnlinePlayers().stream()
              .filter(p -> p.hasPermission("modreq.mod") && !p.equals(player))
              .forEach(
                  p -> p.sendMessage(TextUtil.prefixed("&f" + player.getName() + " &7closed request #" + requestId)));
        } else {
          player.sendMessage(TextUtil.error("Failed to close request #" + requestId + ". It may not exist."));
        }
      });
    }).exceptionally(throwable -> {
      player.sendMessage(TextUtil.error("An error occurred while closing the request."));
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
