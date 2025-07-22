# ModReq

I kinda just wanted a more updated version of another modreq plugin i was using and wanted to add some features that i thought would be useful.

## Installation

1. Download the latest `modreq.jar` from the releases page
2. Place it in your server's `plugins/` directory
3. Start your server to generate the default configuration
4. Configure the plugin in `plugins/ModReq/config.yml`
5. Restart your server or use `/modreq reload`

## Commands

### Player Commands
- `/modreq <description>` - Create a new mod request (shortcut for `/modreq create <description>`)
- `/modreq create <description>` - Create a new mod request
- `/modreq list [page]` - List your mod requests (shows all with open requests first)
- `/modreq info <id>` - View detailed information about a request
- `/modreq close <id>` - Close your own request

### Staff Commands (requires `modreq.mod` permission)
- `/modreq list [all|status|player] [page]` - List mod requests with filters
- `/modreq claim <id>` - Claim a request
- `/modreq unclaim <id>` - Unclaim a request you've claimed
- `/modreq done <id>` - Mark a request as completed
- `/modreq elevate <id>` - Escalate a request for admin attention
- `/modreq teleport <id>` - Teleport to the request location
- `/modreq note <id> <message>` - Add a note to a request

### Admin Commands (requires `modreq.admin` permission)
- `/modreq claim <id> -f` - Force claim a request from another staff member
- `/modreq reload` - Reload the plugin configuration

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `modreq.use` | Create and manage own requests | All players |
| `modreq.mod` | Staff commands (claim, done, etc.) | OP |
| `modreq.admin` | Admin commands (force claim, reload) | OP |

## Interactive Features

### Smart List Command
The list command provides an intelligent viewing experience:
- **Player View**: Shows all your requests with open ones first
- **Staff View**: Filter by status, player, or view all requests
- **Pagination**: Configurable page size (default: 10 requests per page)
- **Clickable Entries**: Click any request to view detailed info

### Interactive Info Command
The info command provides one-click actions:
- **Clickable Location**: Click coordinates to teleport (staff only)
- **Action Buttons**: Context-sensitive buttons based on your permissions:
  - `[Claim]` - If request is unclaimed
  - `[Unclaim]` - If you claimed the request
  - `[Force Claim]` - For admins if claimed by someone else
  - `[Mark Done]` - For staff to complete requests
  - `[Close]` - For request owners to close their requests

### Time Display
All timestamps show relative time with full precision on hover:
- "5 minutes ago" (hover shows: "July 21, 2025 2:30:45 PM")
- "2 hours ago" (hover shows full timestamp)
- "3 days ago" (hover shows full timestamp)

## API Usage

ModReq provides a simple API for other plugins to integrate with:

```java
// Get the ModReq plugin instance
ModReq modReq = (ModReq) Bukkit.getPluginManager().getPlugin("ModReq");

// Create a request programmatically
ModRequestService service = modReq.getModRequestService();
service.createRequest(player, "Automated request", player.getLocation());

// Get all open requests
CompletableFuture<List<ModRequest>> requests = service.getOpenRequests();
```

## Requirements

- **Minecraft**: 1.19+ (PaperMC recommended)
- **Java**: 21+
- **Database**: H2 (embedded) or MySQL 8.0+

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

- **Issues**: Report bugs on the [GitHub Issues](https://github.com/AetherMinecraft/ModReq/issues) page