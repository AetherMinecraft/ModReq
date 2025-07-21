package dev.bwmp.modReq.command.subcommands;

import dev.bwmp.modReq.ModReq;
import dev.bwmp.modReq.command.SubCommand;
import dev.bwmp.modReq.model.ModRequest;
import dev.bwmp.modReq.util.TextUtil;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class UnclaimCommand extends SubCommand {

  private final ModReq plugin;

  public UnclaimCommand(ModReq plugin) {
    super("unclaim", "modreq.mod", "Unclaim a mod request", "/modreq unclaim <id>", true);
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

    plugin.getModRequestService().unclaimRequest(requestId).thenAccept(success -> {
      plugin.getServer().getScheduler().runTask(plugin, () -> {
        if (success) {
          player.sendMessage(TextUtil.success("Successfully unclaimed request #" + requestId));
        } else {
          player.sendMessage(TextUtil
              .error("Failed to unclaim request #" + requestId + ". It may not exist or not be claimed by you."));
        }
      });
    }).exceptionally(throwable -> {
      player.sendMessage(TextUtil.error("An error occurred while unclaiming the request."));
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
