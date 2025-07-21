package dev.bwmp.modReq.command.subcommands;

import dev.bwmp.modReq.ModReq;
import dev.bwmp.modReq.command.SubCommand;
import dev.bwmp.modReq.model.ModRequest;
import dev.bwmp.modReq.util.TextUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ClaimCommand extends SubCommand {

  private final ModReq plugin;

  public ClaimCommand(ModReq plugin) {
    super("claim", "modreq.mod", "Claim a mod request", "/modreq claim [-f] <id>", true);
    this.plugin = plugin;
  }

  @Override
  public boolean execute(CommandSender sender, String[] args) {
    if (args.length < 1 || args.length > 2) {
      sendUsage(sender);
      return true;
    }

    boolean forceClaim = false;
    int requestId;

    if (args.length == 2 && args[1].equals("-f")) {
      if (!sender.hasPermission("modreq.admin")) {
        sender.sendMessage(TextUtil.error("You don't have permission to force claim requests!"));
        return true;
      }
      forceClaim = true;
      try {
        requestId = Integer.parseInt(args[0]);
      } catch (NumberFormatException e) {
        sender.sendMessage(TextUtil.error("Invalid request ID: " + args[1]));
        return true;
      }
    } else if (args.length == 1) {
      try {
        requestId = Integer.parseInt(args[0]);
      } catch (NumberFormatException e) {
        sender.sendMessage(TextUtil.error("Invalid request ID: " + args[0]));
        return true;
      }
    } else {
      sendUsage(sender);
      return true;
    }

    Player player = (Player) sender;
    final boolean finalForceClaim = forceClaim;

    CompletableFuture<Boolean> claimFuture = finalForceClaim
        ? plugin.getModRequestService().forceClaimRequest(requestId, player)
        : plugin.getModRequestService().claimRequest(requestId, player);

    claimFuture.thenAccept(success -> {
      plugin.getServer().getScheduler().runTask(plugin, () -> {
        if (success) {
          String message = finalForceClaim ? "force claimed" : "claimed";
          player.sendMessage(TextUtil.success("Successfully " + message + " request #" + requestId));

          plugin.getServer().getOnlinePlayers().stream()
              .filter(p -> p.hasPermission("modreq.mod") && !p.equals(player))
              .forEach(p -> p.sendMessage(
                  TextUtil.prefixed("&f" + player.getName() + " &7" + message + " request #" + requestId)));
        } else {
          if (finalForceClaim) {
            player.sendMessage(TextUtil.error("Failed to force claim request #" + requestId + ". It may not exist."));
          } else {
            player.sendMessage(TextUtil.error("Failed to claim request #" + requestId
                + ". It may not exist or already be claimed. Use &f-f &cto force claim."));
          }
        }
      });
    }).exceptionally(throwable -> {
      player.sendMessage(TextUtil.error("An error occurred while claiming the request."));
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
        for (ModRequest request : requests) {
          completions.add(String.valueOf(request.getId()));
        }
      } catch (Exception e) {
        plugin.getLogger().warning("Error getting tab completions for claim command: " + e.getMessage());
      }
    }

    if (args.length == 2) {
      if (sender.hasPermission("modreq.admin") && "-f".startsWith(args[0].toLowerCase())) {
        completions.add("-f");
      }
    }

    return completions;
  }
}
