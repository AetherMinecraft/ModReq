package dev.bwmp.modReq.command.subcommands;

import dev.bwmp.modReq.ModReq;
import dev.bwmp.modReq.command.SubCommand;
import dev.bwmp.modReq.util.TextUtil;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HelpCommand extends SubCommand {

  private final Map<String, SubCommand> subCommands;

  public HelpCommand(ModReq plugin, Map<String, SubCommand> subCommands) {
    super("help", null, "Show help for ModReq commands", "/modreq help [command]", false);
    this.subCommands = subCommands;
  }

  @Override
  public boolean execute(CommandSender sender, String[] args) {
    if (args.length == 0) {

      sender.sendMessage(TextUtil.highlight("=== ModReq Help ==="));
      sender.sendMessage(TextUtil.format("&f/modreq <message> &7- Create a new mod request"));
      sender.sendMessage(TextUtil.info("Available commands:"));

      for (SubCommand subCommand : subCommands.values()) {

        if (subCommand.getName().equals("create")) {
          continue;
        }
        if (subCommand.hasPermission(sender)) {
          sender.sendMessage(TextUtil.format("&f/modreq " + subCommand.getName() + " &7- " + subCommand.getDescription()));
        }
      }

      sender.sendMessage(TextUtil.info("Use &f/modreq help <command> &7for detailed usage."));
      return true;
    }

    String commandName = args[0].toLowerCase();
    SubCommand subCommand = subCommands.get(commandName);

    if (subCommand == null) {
      sender.sendMessage(TextUtil.error("Unknown command: " + args[0]));
      return true;
    }

    if (!subCommand.hasPermission(sender)) {
      sendNoPermission(sender);
      return true;
    }

    sender.sendMessage(TextUtil.highlight("=== " + subCommand.getName() + " ==="));
    sender.sendMessage(TextUtil.info("Description: &f" + subCommand.getDescription()));
    sender.sendMessage(TextUtil.info("Usage: &f" + subCommand.getUsage()));
    if (subCommand.getPermission() != null) {
      sender.sendMessage(TextUtil.info("Permission: &f" + subCommand.getPermission()));
    }

    return true;
  }

  @Override
  public List<String> getTabCompletions(CommandSender sender, String[] args) {
    if (args.length == 1) {
      List<String> completions = new ArrayList<>();
      String partial = args[0].toLowerCase();

      for (String commandName : subCommands.keySet()) {
        if (commandName.startsWith(partial) && subCommands.get(commandName).hasPermission(sender)) {
          completions.add(commandName);
        }
      }

      return completions;
    }

    return new ArrayList<>();
  }
}
