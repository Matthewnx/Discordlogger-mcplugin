# DiscordLogger

A lightweight Minecraft server plugin that sends important server and player activity logs directly to Discord using Discord Webhooks.

DiscordLogger allows server administrators to monitor what is happening on their Minecraft server without constantly checking the server console. Player activity, gameplay events, server status, and other events can be automatically forwarded to separate Discord channels through configurable webhooks.

## ✨ Features

* 📡 Send Minecraft events directly to Discord
* 🔗 Separate Discord webhook for each event type
* ⚡ Asynchronous Discord requests to avoid blocking the Minecraft server
* 📦 Automatically buffers and batches messages
* ✂️ Automatically splits messages to respect Discord's 2000-character message limit
* 🕐 Adds timestamps to logged events
* 📍 Records player coordinates for relevant events
* 🌍 Detects world changes
* 💬 Logs player chat messages
* 🎮 Logs game mode changes
* 🏆 Logs completed advancements
* ⚔️ Logs mob kills
* 🌦️ Logs weather changes
* 💥 Logs broken items
* 🔌 Sends server startup and shutdown notifications

## 📋 Logged Events

DiscordLogger currently supports the following events:

| Event            | Description                                         |
| ---------------- | --------------------------------------------------- |
| 🟢 Server Status | Server startup and shutdown                         |
| ✅ Player Join    | Logs when a player joins the server                 |
| ❌ Player Quit    | Logs when a player leaves                           |
| ☠️ Player Death  | Logs player deaths                                  |
| 🌍 World Change  | Logs when a player changes worlds                   |
| ⌨️ Commands      | Logs commands executed by players                   |
| 🎮 Game Mode     | Logs game mode changes                              |
| ⛏️ Block Break   | Logs blocks broken by players                       |
| ⚒️ Crafting      | Logs crafted items and quantities                   |
| 📥 Item Pickup   | Logs items picked up from the ground or inventories |
| 📥 Item Drop     | Logs items dropped by players                       |
| 💬 Chat          | Logs player chat messages                           |
| 🏆 Advancements  | Logs completed Minecraft advancements               |
| ⚔️ Mob Kills     | Logs mobs killed by players                         |
| 🌦️ Weather      | Logs weather changes                                |
| 💥 Item Break    | Logs when a player's item breaks                    |

## 📸 Example

DiscordLogger sends readable messages to Discord using the configured webhook for each event.

Example:

```text
⛏️ PlayerName, Diamond Ore Ra Dar (120, 64, -342), world Mine kard! [14:32:18]
```

Another example:

```text
🌍 PlayerName az Overworld be Nether raft! [15:04:27]
```

Chat messages are also forwarded:

```text
[15:10:42] 💬 PlayerName: Hello everyone!
```

*its corrently on finglish which is combination of english and farsi but im working on a english version too.

## ⚙️ Configuration

Webhook URLs are configured in the plugin's configuration file.

The plugin uses an event-based webhook structure:

```yaml
webhooks:
  svstatus: "YOUR_WEBHOOK_URL"
  join: "YOUR_WEBHOOK_URL"
  quit: "YOUR_WEBHOOK_URL"
  death: "YOUR_WEBHOOK_URL"
  worldchange: "YOUR_WEBHOOK_URL"
  usecommand: "YOUR_WEBHOOK_URL"
  gamemode: "YOUR_WEBHOOK_URL"
  breakblock: "YOUR_WEBHOOK_URL"
  craft: "YOUR_WEBHOOK_URL"
  pickup: "YOUR_WEBHOOK_URL"
  drop: "YOUR_WEBHOOK_URL"
  chat: "YOUR_WEBHOOK_URL"
  advancement: "YOUR_WEBHOOK_URL"
  mobkills: "YOUR_WEBHOOK_URL"
  weather: "YOUR_WEBHOOK_URL"
  itembreak: "YOUR_WEBHOOK_URL"
```

Each event type can use its own Discord webhook.

This allows you to organize your server logs into different Discord channels.

For example:

```text
#server-status
#player-activity
#chat-logs
#gameplay-logs
#admin-logs
```

with each channel receiving only the events you want.

> **Important:** Never publish your real Discord webhook URLs publicly. Treat webhook URLs like passwords and keep them out of GitHub.

## 🚀 Installation

1. Download the latest plugin `.jar` from the **Releases** section.
2. Stop your Minecraft server.
3. Place the `.jar` file inside your server's:

```text
plugins/
```

directory.

4. Start the server.
5. Open the generated configuration file.
6. Add your Discord webhook URLs.
7. Restart the server.

The plugin will automatically load the configured webhooks when the server starts.

## 🔧 How It Works

DiscordLogger listens for Minecraft server events using the Bukkit/Spigot event system.

When an event occurs:

```text
Minecraft Event
      ↓
DiscordLogger
      ↓
Event Message
      ↓
Message Buffer
      ↓
Batch / Split Messages
      ↓
Discord Webhook
      ↓
Discord Channel
```

Messages are first placed into an in-memory buffer instead of immediately sending a Discord request for every event.

The plugin periodically flushes the buffer asynchronously.

This reduces the number of HTTP requests made to Discord when many events happen within a short period.

The plugin also checks the Discord message length and splits large batches before they exceed Discord's 2000-character message limit.

## ⚡ Asynchronous Logging

Discord requests are performed asynchronously so that network communication with Discord does not unnecessarily block the Minecraft server's main thread.

The plugin periodically processes buffered messages every two seconds.

HTTP connections use connection and read timeouts to prevent a failed Discord request from waiting indefinitely.

## 🛡️ Webhook Security

Discord webhook URLs provide access to the associated webhook, so **do not commit your real webhook URLs to GitHub.**

Use placeholder values in your public repository:

```yaml
webhooks:
  svstatus: "YOUR_WEBHOOK_URL"
  join: "YOUR_WEBHOOK_URL"
  quit: "YOUR_WEBHOOK_URL"
```

If you accidentally expose a webhook URL, delete or regenerate the webhook from Discord.

## 🧩 Supported Minecraft Events

The plugin currently listens for Bukkit events including:

* `PlayerJoinEvent`
* `PlayerQuitEvent`
* `PlayerDeathEvent`
* `PlayerChangedWorldEvent`
* `PlayerCommandPreprocessEvent`
* `PlayerGameModeChangeEvent`
* `BlockBreakEvent`
* `CraftItemEvent`
* `PlayerPickupItemEvent`
* `InventoryClickEvent`
* `PlayerDropItemEvent`
* `AsyncPlayerChatEvent`
* `PlayerAdvancementDoneEvent`
* `EntityDeathEvent`
* `WeatherChangeEvent`
* `PlayerItemBreakEvent`

## 🗂️ Event Categories

Each event is assigned its own category/key.

For example:

```java
queueDiscord("join", message);
```

sends the message to the webhook configured under:

```yaml
webhooks:
  join: "YOUR_WEBHOOK_URL"
```

This makes the logging system flexible and allows individual event types to be routed to different Discord channels.

## 🌍 World Detection

The plugin automatically converts Minecraft's default world names into more readable names:

```text
world          → Overworld
world_nether   → Nether
world_the_end  → The End
```

Custom world names are preserved.

## 🏆 Advancement Logging

Recipe unlocks and hidden advancements are ignored.

For visible advancements, DiscordLogger extracts the readable advancement title before sending it to Discord.

## 📍 Player Locations

Events such as block breaking, item pickup, and item dropping include the player's Minecraft coordinates.

Example:

```text
(120, 64, -342)
```

This can make gameplay logs much easier to investigate.

## 📦 Message Buffering

Instead of sending every event immediately, messages are stored temporarily by event type.

For example:

```text
join
 ├── Player1 joined
 ├── Player2 joined
 └── Player3 joined

breakblock
 ├── Player1 broke Stone
 ├── Player2 broke Oak Log
 └── Player1 broke Iron Ore
```

The plugin periodically combines these messages before sending them to Discord.

This also helps reduce unnecessary webhook requests when multiple events happen quickly.

## 📜 License

Add your preferred license here.

For example:

```text
MIT License
```

See the `LICENSE` file for the complete license text.

## 🤝 Contributing

Contributions, bug reports, and suggestions are welcome.

If you find a bug or have an idea for a new logging event:

1. Open an issue.
2. Describe the problem or feature.
3. Include relevant server/plugin information.
4. If possible, submit a pull request with your changes.

## 🛠️ Development

The plugin is written in Java and uses the Bukkit/Spigot API.

Main plugin class:

```text
mathew.discordLogger.DiscordLogger
```

The main class implements Bukkit's `Listener` interface and registers the event listeners when the plugin is enabled.

## 📌 Roadmap

Potential future improvements:

* [ ] Discord embeds
* [ ] Configurable message formats
* [ ] Enable/disable individual logging events
* [ ] Discord bot integration
* [ ] Better item and entity names
* [ ] Player command filtering
* [ ] Log filtering
* [ ] Configurable flush interval
* [ ] Rich Discord embeds with player/world information
* [ ] Support for additional Minecraft server platforms
* [ ] add multi language support

---

**DiscordLogger** — Keep an eye on your Minecraft server, directly from Discord. ⛏️
