package dev.bwmp.modReq.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import dev.bwmp.modReq.ModReq;
import dev.bwmp.modReq.util.TextUtil;

public class PlayerJoin implements Listener {
  
  

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    
    if (player.hasPermission("modreq.mod")) {
      ModReq.getInstance().getModRequestService().getOpenRequests().thenAccept(openRequests -> {
        int count = openRequests.size();
        if (count > 0) {
          player.sendMessage(TextUtil.format("&eThere are &c" + String.valueOf(count) + "&e open mod requests. Use &f/modreq list&e to view them."));
        }
      });
    }
  }

}
