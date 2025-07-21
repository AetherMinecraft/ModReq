package dev.bwmp.modReq.command.subcommands;

import dev.bwmp.modReq.ModReq;
import dev.bwmp.modReq.command.SubCommand;
import dev.bwmp.modReq.util.TextUtil;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class ReloadCommand extends SubCommand {

  private final ModReq plugin;

  public ReloadCommand(ModReq plugin) {
    super("reload", "modreq.admin", "Reload the plugin configuration", "/modreq reload", false);
    this.plugin = plugin;
  }

  @Override
  public boolean execute(CommandSender sender, String[] args) {
    try {
      plugin.getConfigManager().reloadConfig();
      sender.sendMessage(TextUtil.success("ModReq configuration reloaded successfully!"));
    } catch (Exception e) {
      sender.sendMessage(TextUtil.error("Failed to reload configuration: " + e.getMessage()));
      plugin.getLogger().severe("Failed to reload configuration: " + e.getMessage());
      e.printStackTrace();
    }

    return true;
  }

  @Override
  public List<String> getTabCompletions(CommandSender sender, String[] args) {
    return new ArrayList<>();
  }
}
