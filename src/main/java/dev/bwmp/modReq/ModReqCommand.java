package dev.bwmp.modReq;

import dev.bwmp.modReq.command.SubCommand;
import dev.bwmp.modReq.command.subcommands.*;
import dev.bwmp.modReq.util.TextUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;
import java.util.stream.Collectors;

public class ModReqCommand implements CommandExecutor, TabCompleter {

	@SuppressWarnings("unused")
	private final ModReq plugin;
	private final Map<String, SubCommand> subCommands;

	public ModReqCommand(ModReq plugin) {
		this.plugin = plugin;
		this.subCommands = new HashMap<>();

		registerSubCommand(new CreateCommand(plugin));
		registerSubCommand(new ListCommand(plugin));
		registerSubCommand(new InfoCommand(plugin));
		registerSubCommand(new ClaimCommand(plugin));
		registerSubCommand(new UnclaimCommand(plugin));
		registerSubCommand(new DoneCommand(plugin));
		registerSubCommand(new CloseCommand(plugin));
		registerSubCommand(new ElevateCommand(plugin));
		registerSubCommand(new TeleportCommand(plugin));
		registerSubCommand(new NoteCommand(plugin));
		registerSubCommand(new ReloadCommand(plugin));
		registerSubCommand(new HelpCommand(plugin, subCommands));
	}

	private void registerSubCommand(SubCommand subCommand) {
		subCommands.put(subCommand.getName().toLowerCase(), subCommand);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length == 0) {
			SubCommand helpCommand = subCommands.get("help");
			if (helpCommand != null) {
				return helpCommand.execute(sender, new String[0]);
			}
			return true;
		}

		String subCommandName = args[0].toLowerCase();
		SubCommand subCommand = subCommands.get(subCommandName);

		if (subCommand != null) {
			if (!subCommand.hasPermission(sender)) {
				subCommand.sendNoPermission(sender);
				return true;
			}

			if (!subCommand.checkPlayer(sender)) {
				return true;
			}

			String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
			return subCommand.execute(sender, subArgs);
		}

		if (!(sender instanceof org.bukkit.entity.Player)) {
			sender.sendMessage(TextUtil.error("Only players can create mod requests!"));
			return true;
		}

		if (!sender.hasPermission("modreq.use")) {
			sender.sendMessage(TextUtil.error("You don't have permission to create mod requests!"));
			return true;
		}

		CreateCommand createCommand = (CreateCommand) subCommands.get("create");
		if (createCommand != null) {
			return createCommand.execute(sender, args);
		}

		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (args.length == 1) {
			String partial = args[0].toLowerCase();
			return subCommands.keySet().stream()
					.filter(name -> name.startsWith(partial))
					.filter(name -> subCommands.get(name).hasPermission(sender))
					.sorted()
					.collect(Collectors.toList());
		}

		if (args.length > 1) {
			String subCommandName = args[0].toLowerCase();
			SubCommand subCommand = subCommands.get(subCommandName);

			if (subCommand != null && subCommand.hasPermission(sender)) {
				String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
				return subCommand.getTabCompletions(sender, subArgs);
			}
		}

		return new ArrayList<>();
	}
}