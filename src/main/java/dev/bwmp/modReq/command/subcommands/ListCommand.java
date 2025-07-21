package dev.bwmp.modReq.command.subcommands;

import dev.bwmp.modReq.ModReq;
import dev.bwmp.modReq.command.SubCommand;
import dev.bwmp.modReq.model.ModRequest;
import dev.bwmp.modReq.model.ModRequestStatus;
import dev.bwmp.modReq.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.format.NamedTextColor;
public class ListCommand extends SubCommand {

  private final ModReq plugin;

  public ListCommand(ModReq plugin) {
    super("list", "modreq.use", "List active mod requests. Use 'all' to include completed/closed requests.",
        "/modreq list [all|status|player] [page]", false);
    this.plugin = plugin;
  }

  @Override
  public boolean execute(CommandSender sender, String[] args) {
    ModRequestStatus filterStatus = null;
    String filterPlayer = null;
    boolean showAll = sender.hasPermission("modreq.mod");
    boolean showAllStatuses = false;
    int page = 1;

    for (String arg : args) {
      if (arg.equalsIgnoreCase("all")) {
        showAllStatuses = true;
        continue;
      }

      try {
        int parsedPage = Integer.parseInt(arg);
        if (parsedPage > 0) {
          page = parsedPage;
          continue;
        }
      } catch (NumberFormatException ignored) {

      }

      try {
        filterStatus = ModRequestStatus.valueOf(arg.toUpperCase());
        continue;
      } catch (IllegalArgumentException ignored) {

      }

      filterPlayer = arg;
    }

    if (!showAll && filterPlayer == null && sender instanceof Player) {
      filterPlayer = sender.getName();
    }

    final String finalFilterPlayer = filterPlayer;
    final ModRequestStatus finalFilterStatus = filterStatus;
    final boolean finalShowAllStatuses = showAllStatuses;
    final int finalPage = page;

    CompletableFuture<List<ModRequest>> requestsFuture;
    if (finalFilterStatus != null || finalShowAllStatuses) {
      // User specified a status filter or wants all statuses
      requestsFuture = plugin.getModRequestService().getRequests(finalFilterStatus, finalFilterPlayer);
    } else if (finalFilterPlayer != null && !finalFilterPlayer.isEmpty()) {
      // Viewing a specific player's requests (including self) - show ALL their requests
      requestsFuture = plugin.getModRequestService().getRequests(null, finalFilterPlayer);
    } else {
      // Default case - show only active requests
      requestsFuture = plugin.getModRequestService().getActiveRequests(finalFilterPlayer);
    }

    requestsFuture.thenAccept(requests -> {
      plugin.getServer().getScheduler().runTask(plugin, () -> {
        if (requests.isEmpty()) {
          if (finalFilterStatus != null || finalFilterPlayer != null) {
            sender.sendMessage(TextUtil.info("No mod requests found matching your criteria."));
          } else {
            sender.sendMessage(TextUtil.info("No mod requests found."));
          }
          return;
        }

        displayPaginatedRequests(sender, requests, finalPage, finalFilterStatus, finalFilterPlayer,
            finalShowAllStatuses);
      });
    }).exceptionally(throwable -> {
      sender.sendMessage(TextUtil.error("Failed to load mod requests."));
      throwable.printStackTrace();
      return null;
    });

    return true;
  }

  private void displayPaginatedRequests(CommandSender sender, List<ModRequest> allRequests, int page,
      ModRequestStatus filterStatus, String filterPlayer, boolean showAllStatuses) {

    if (filterPlayer != null && !filterPlayer.isEmpty()) {
      allRequests.sort((r1, r2) -> {
        boolean r1IsOpen = r1.getStatus() == ModRequestStatus.OPEN || r1.getStatus() == ModRequestStatus.ELEVATED;
        boolean r2IsOpen = r2.getStatus() == ModRequestStatus.OPEN || r2.getStatus() == ModRequestStatus.ELEVATED;

        if (r1IsOpen && !r2IsOpen) return -1;
        if (!r1IsOpen && r2IsOpen) return 1;

        return r2.getCreatedAt().compareTo(r1.getCreatedAt());
      });
    }
    
    int pageSize = plugin.getConfigManager().getListPageSize();
    int totalRequests = allRequests.size();
    int totalPages = (int) Math.ceil((double) totalRequests / pageSize);

    if (page < 1)
      page = 1;
    if (page > totalPages)
      page = totalPages;

    int startIndex = (page - 1) * pageSize;
    int endIndex = Math.min(startIndex + pageSize, totalRequests);

    String filterDescription = "";
    if (filterStatus != null) {
      filterDescription += " [" + filterStatus.name() + "]";
    }
    if (filterPlayer != null) {
      filterDescription += " [Player: " + filterPlayer + "]";
    }
    if (showAllStatuses) {
      filterDescription += " [All Statuses]";
    } else if (filterPlayer != null && !filterPlayer.isEmpty()) {
      filterDescription += " [Open First]";
    }

    sender.sendMessage(
        TextUtil.highlight("=== Mod Requests" + filterDescription + " (Page " + page + "/" + totalPages + ") ==="));

    List<ModRequest> pageRequests = allRequests.subList(startIndex, endIndex);
    for (ModRequest request : pageRequests) {
      Component requestLine = Component
          .text("#" + request.getId() + " ", NamedTextColor.WHITE)
          .append(Component.text("[" + request.getStatus().name() + "] ",
              NamedTextColor.GRAY))
          .append(Component.text(request.getPlayerName(), NamedTextColor.WHITE));

      if (request.isClaimed()) {
        requestLine = requestLine
            .append(Component.text(" (claimed by ", NamedTextColor.GRAY))
            .append(Component.text(request.getClaimedByName(), NamedTextColor.WHITE))
            .append(Component.text(")", NamedTextColor.GRAY));
      }

      requestLine = requestLine
          .clickEvent(ClickEvent.runCommand("/modreq info " + request.getId()))
          .hoverEvent(HoverEvent.showText(
              Component.text("Click to view request details", NamedTextColor.GREEN)));

      sender.sendMessage(requestLine);

      Component timeAndDesc = Component.text("  ", NamedTextColor.GRAY)
          .append(TextUtil.relativeTime(request.getCreatedAt(), "&7"))
          .append(
              Component.text(" - " + request.getDescription(), NamedTextColor.AQUA));
      sender.sendMessage(timeAndDesc);

      if (request.getWorldName() != null) {
        sender.sendMessage(TextUtil.info("  Location: " + request.getWorldName() + " " +
            String.format("%.0f,%.0f,%.0f", request.getX(), request.getY(), request.getZ())));
      }
    }

    if (totalPages > 1) {
      Component navigation = Component.empty();

      if (page > 1) {
        String prevCommand = buildPageCommand(page - 1, filterStatus, filterPlayer, showAllStatuses);
        navigation = navigation
            .append(Component.text("[← Previous] ", NamedTextColor.YELLOW)
                .clickEvent(ClickEvent.runCommand(prevCommand))
                .hoverEvent(HoverEvent.showText(
                    Component.text("Go to page " + (page - 1), NamedTextColor.GREEN))));
      }

      navigation = navigation
          .append(Component.text("Page " + page + "/" + totalPages + " ",
              NamedTextColor.GRAY));

      if (page < totalPages) {
        String nextCommand = buildPageCommand(page + 1, filterStatus, filterPlayer, showAllStatuses);
        navigation = navigation
            .append(Component.text("[Next →]", NamedTextColor.YELLOW)
                .clickEvent(ClickEvent.runCommand(nextCommand))
                .hoverEvent(HoverEvent.showText(
                    Component.text("Go to page " + (page + 1), NamedTextColor.GREEN))));
      }

      sender.sendMessage(navigation);
    }

    sender.sendMessage(TextUtil.info("Showing " + pageRequests.size() + " of " + totalRequests + " requests"));
  }

  private String buildPageCommand(int page, ModRequestStatus filterStatus, String filterPlayer,
      boolean showAllStatuses) {
    StringBuilder command = new StringBuilder("/modreq list");

    if (showAllStatuses) {
      command.append(" all");
    }
    if (filterStatus != null) {
      command.append(" ").append(filterStatus.name().toLowerCase());
    }
    if (filterPlayer != null && !filterPlayer.isEmpty()) {
      command.append(" ").append(filterPlayer);
    }

    command.append(" ").append(page);

    return command.toString();
  }

  @Override
  public List<String> getTabCompletions(CommandSender sender, String[] args) {
    List<String> completions = new ArrayList<>();

    if (args.length == 1) {
      String partial = args[0].toLowerCase();

      if ("all".startsWith(partial)) {
        completions.add("all");
      }

      for (ModRequestStatus status : ModRequestStatus.values()) {
        if (status.name().toLowerCase().startsWith(partial)) {
          completions.add(status.name().toLowerCase());
        }
      }

      if (sender.hasPermission("modreq.mod")) {
        plugin.getServer().getOnlinePlayers().stream()
            .map(Player::getName)
            .filter(name -> name.toLowerCase().startsWith(partial))
            .forEach(completions::add);
      }
    } else if (args.length == 2) {
      completions.add("1");
      completions.add("2");
      completions.add("3");
    }

    return completions;
  }
}
