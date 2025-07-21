package dev.bwmp.modReq.command.subcommands;

import dev.bwmp.modReq.ModReq;
import dev.bwmp.modReq.command.SubCommand;
import dev.bwmp.modReq.util.TextUtil;
import dev.bwmp.modReq.model.ModRequest;
import dev.bwmp.modReq.model.ModRequestNote;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NoteCommand extends SubCommand {

  private final ModReq plugin;

  public NoteCommand(ModReq plugin) {
    super("note", "modreq.mod", "Add a note to a mod request", "/modreq note <id> <message>", true);
    this.plugin = plugin;
  }

  @Override
  public boolean execute(CommandSender sender, String[] args) {
    if (args.length < 2) {
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
    String noteText = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

    if (noteText.length() > 500) {
      player.sendMessage(TextUtil.error("Note too long! Maximum 500 characters."));
      return true;
    }

    ModRequestNote note = new ModRequestNote();
    note.setRequestId(requestId);
    note.setAuthorId(player.getUniqueId());
    note.setAuthorName(player.getName());
    note.setContent(noteText);

    plugin.getModRequestService().addNote(requestId, player, noteText).thenAccept(addedNote -> {
      plugin.getServer().getScheduler().runTask(plugin, () -> {
        if (addedNote != null) {
          player.sendMessage(TextUtil.success("Note added to request #" + requestId));

          plugin.getServer().getOnlinePlayers().stream()
              .filter(p -> p.hasPermission("modreq.mod") && !p.equals(player))
              .forEach(p -> p.sendMessage(TextUtil.prefixed(
                  "&f" + player.getName() + " &7added a note to request #" + requestId)));
        } else {
          player.sendMessage(TextUtil.error("Failed to add note to request #" + requestId + ". It may not exist."));
        }
      });
    }).exceptionally(throwable -> {
      player.sendMessage(TextUtil.error("An error occurred while adding the note."));
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
