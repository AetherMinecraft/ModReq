package dev.bwmp.modReq.command.subcommands;

import dev.bwmp.modReq.ModReq;
import dev.bwmp.modReq.command.SubCommand;
import dev.bwmp.modReq.util.TextUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CreateCommand extends SubCommand {

  private final ModReq plugin;

  public CreateCommand(ModReq plugin) {
    super("create", "modreq.use", "Create a new mod request", "/modreq create <description>", true);
    this.plugin = plugin;
  }

  @Override
  public boolean execute(CommandSender sender, String[] args) {
    if (args.length == 0) {
      sendUsage(sender);
      return true;
    }

    Player player = (Player) sender;
    String description = String.join(" ", args);

    if (description.length() > 500) {
      player.sendMessage(TextUtil.error("Description too long! Maximum 500 characters."));
      return true;
    }

    plugin.getModRequestService().createRequest(player, description).thenAccept(request -> {
      if (request != null) {
        player.sendMessage(TextUtil.success("Your mod request has been created! ID: #" + request.getId()));

        plugin.getDiscordService().sendRequestCreated(request);

        plugin.getServer().getScheduler().runTask(plugin, () -> {
          plugin.getServer().getOnlinePlayers().stream()
              .filter(p -> p.hasPermission("modreq.mod"))
              .forEach(p -> {
                p.sendMessage(TextUtil.prefixed("&f" + player.getName() + " &7created a new request"));
                p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
              });
        });
      } else {
        player.sendMessage(TextUtil.error("Failed to create mod request. Please try again."));
      }
    }).exceptionally(throwable -> {
      if (throwable.getCause() instanceof IllegalStateException) {
        player.sendMessage(TextUtil.error(throwable.getCause().getMessage()));
      } else {
        player.sendMessage(TextUtil.error("An error occurred while creating your request."));
        throwable.printStackTrace();
      }
      return null;
    });

    return true;
  }

  @Override
  public List<String> getTabCompletions(CommandSender sender, String[] args) {
    return new ArrayList<>();
  }
}
