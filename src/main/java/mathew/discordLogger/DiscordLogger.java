package mathew.discordLogger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.GameMode;



import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;


import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

//main class
public class DiscordLogger extends JavaPlugin implements Listener {

    private final Map<String, String> webhooks = new ConcurrentHashMap<>();
    private final Map<String, List<String>> messageBuffer = new ConcurrentHashMap<>();

    public void queueDiscord(String eventType, String message) {

        messageBuffer.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(message);

    }

    public void startFlushTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            for (Map.Entry<String, List<String>> entry : messageBuffer.entrySet()) {
                String eventType = entry.getKey();
                List<String> messages = entry.getValue();

                if (messages.isEmpty()) continue;

                // Create a copy and clear the buffer
                List<String> messagesToSend = new ArrayList<>(messages);
                messages.clear();

                // Combine all messages
                StringBuilder combined = new StringBuilder();
                for (String msg : messagesToSend) {
                    if (combined.length() + msg.length() + 1 > 2000) {
                        // Send current chunk if adding this message would exceed limit
                        sendDiscord(eventType, combined.toString());
                        combined = new StringBuilder();
                    }

                    if (combined.length() > 0) {
                        combined.append("\n");
                    }
                    combined.append(msg);
                }

                // Send any remaining messages
                if (combined.length() > 0) {
                    sendDiscord(eventType, combined.toString());
                }
            }
        }, 40L, 40L); // 2s interval
    }

    public void flushAll() {
        for (Map.Entry<String, List<String>> entry : messageBuffer.entrySet()) {
            String eventType = entry.getKey();
            List<String> messages = entry.getValue();

            if (messages.isEmpty()) continue;

            // Create a copy and clear the buffer
            List<String> messagesToSend = new ArrayList<>(messages);
            messages.clear();

            // Combine all messages
            StringBuilder combined = new StringBuilder();
            for (String msg : messagesToSend) {
                if (combined.length() + msg.length() + 1 > 2000) {
                    // Send current chunk if adding this message would exceed limit
                    sendDiscord(eventType, combined.toString());
                    combined = new StringBuilder();
                }

                if (combined.length() > 0) {
                    combined.append("\n");
                }
                combined.append(msg);
            }

            // Send any remaining messages
            if (combined.length() > 0) {
                sendDiscord(eventType, combined.toString());
            }
        }
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();

        // Load webhooks from config
        FileConfiguration config = getConfig();
        ConfigurationSection section = config.getConfigurationSection("webhooks");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String webhookUrl = section.getString(key);
                if (webhookUrl != null && !webhookUrl.isEmpty()) {
                    webhooks.put(key, webhookUrl);
                    getLogger().info("Loaded webhook for " + key);
                }
            }
        }

        getLogger().info("Loaded webhooks: " + webhooks.keySet());

        Bukkit.getPluginManager().registerEvents(this, this);
        startFlushTask();

        // Send startup log
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        queueDiscord("svstatus", "🟢 Server Run Shod! [" + time + "]");
    }

    @Override
    public void onDisable() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        queueDiscord("svstatus","🔴 Server off Shod! [" + time + "]");
        flushAll();
    }

    // Send data to discord through the webhook
    private void sendDiscord(String eventType, String content) {
        try {
            FileConfiguration config = getConfig();
            String url = config.getString("webhooks." + eventType);

            if (url == null) return;

            URL webhookUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) webhookUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            // Proper JSON escaping
            String escapedContent = content
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");

            String json = "{\"content\":\"" + escapedContent + "\"}";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes("UTF-8"));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 200 && responseCode != 204) {
                getLogger().warning("Discord webhook for " + eventType + " returned response code: " + responseCode);
            }

            conn.disconnect();
        } catch (Exception e) {
            getLogger().warning("Failed to send Discord webhook for " + eventType + ": " + e.getMessage());
        }
    }

    private String Bold(String message) {
        return "**" + message + "**";
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        queueDiscord("join","✅ " + Bold(e.getPlayer().getName()) + " Be Server Join shod!" +  " [" + time + "] ");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        queueDiscord("quit","❌ " + Bold(e.getPlayer().getName()) + " az server left dad! " +  " [" + time + "] ");
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        queueDiscord("death","☠️ " + Bold(e.getDeathMessage()) + " [" + time + "] ");
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        Player player = e.getPlayer();
        String from = formatWorldName(e.getFrom().getName());   // previous world name
        String to = formatWorldName(player.getWorld().getName());   // new world name
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        queueDiscord("worldchange","🌍 " + Bold(player.getName()) + " az  " + from + " be " + to + " raft! " + " [" + time + "] ");
    }
    private String formatWorldName(String world) {
        switch (world) {
            case "world": return "Overworld";
            case "world_nether": return "Nether";
            case "world_the_end": return "The End";
            default: return world; // fallback for custom worlds
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        queueDiscord("usecommand","⌨️ " + Bold(e.getPlayer().getName()) + " Az: " + e.getMessage() + " estefade kard! " + " [" + time + "] ");
    }
    @EventHandler
    public void onGamemodeChange(PlayerGameModeChangeEvent e) {
        Player player = e.getPlayer();
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        String from = player.getGameMode().toString();   // old gamemode
        String to = e.getNewGameMode().toString();  // new gamemode

        queueDiscord("gamemode","🎮 " + Bold(player.getName()) + " gamemode khod ra az " + from + " be " + to + " Switch Kard! " +  " [" + time + "] ");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Player player = e.getPlayer();
        //bock attibutes
        Location loc = e.getBlock().getLocation();
        //location of block
        String coords = "(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
        String world = loc.getWorld().getName(); // world name

        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        queueDiscord("breakblock","⛏️ " + Bold(player.getName()) + ", " +  e.getBlock().getType() +"  Ra Dar " + coords +", " + world + " Mine kard! " +  " [" + time + "] ");
    }

    @EventHandler
    public void onCraft(CraftItemEvent e) {
        if (e.getWhoClicked() instanceof org.bukkit.entity.Player player) {

            int amount = e.getRecipe().getResult().getAmount();
            if (e.isShiftClick()) {
                int resultAmount = e.getRecipe().getResult().getAmount();
                int possibleCrafts = Integer.MAX_VALUE;

                    for (ItemStack item : e.getInventory().getMatrix()) {
                    if (item != null && item.getType() != Material.AIR) {
                        possibleCrafts = Math.min(possibleCrafts, item.getAmount());
                    }
                }
                amount = possibleCrafts * resultAmount;
            }
            //crafted item properties
            String item = e.getRecipe().getResult().getType().toString();
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            queueDiscord("craft","⚒️ " + amount + ", " + item + " Tavasot " + Bold(player.getName()) + " Craft Shod! "  + " [" + time + "] ");
        }
    }

    @EventHandler
    public void onPickup(PlayerPickupItemEvent e) {
        //pick item from ground
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        Player player = e.getPlayer();
        ItemStack stack = e.getItem().getItemStack();
        int amount = stack.getAmount();
        String item = stack.getType().toString();
        //calculate exact position of player at doing this event
        Location loc = player.getLocation();
        String coords = "(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";

        queueDiscord("pickup","📥 " + Bold(player.getName())  +", " + amount + "x " + item + " Ra Dar " + coords + " bardasht!" + " [" + time + "] ");
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        //pick item from inventory or anyting execpt ground
        if (!(e.getWhoClicked() instanceof Player player)) return;

        if (e.getAction() == InventoryAction.PICKUP_ALL ||
                e.getAction() == InventoryAction.PICKUP_SOME ||
                e.getAction() == InventoryAction.PICKUP_HALF ||
                e.getAction() == InventoryAction.PICKUP_ONE) {

            ItemStack item = e.getCurrentItem();
            if (item == null || item.getType() == Material.AIR) return;

            //item attibutes
            int amount = item.getAmount();
            String name = item.getType().toString();
            String source = e.getInventory().getType().toString(); // CHEST, BARREL, FURNACE, etc.

            //calculate exact position of player at doing this event
            Location loc = player.getLocation();
            String coords = "(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";

            queueDiscord("pickup","📥 " + Bold(player.getName()) + ", " + amount + "x " + name + " ra  az " + source + " bardasht dar " + coords);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {

        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        Player player = e.getPlayer();

        //item artibutes
        ItemStack stack = e.getItemDrop().getItemStack();
        int amount = stack.getAmount();
        String item = stack.getType().toString();

        //calculate exact position of player at doing this event
        Location loc = player.getLocation();
        String coords = "(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";

        queueDiscord("drop","📥 " + Bold(player.getName())  +", " + amount + "x " + item + " Ra Dar " + coords + " drop kard!" + " [" + time + "] ");
    }
    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        String player = e.getPlayer().getName();
        String msg = e.getMessage();
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        queueDiscord("chat"," [" + time + "] " + " 💬 " + Bold(player) + ": " + msg);
    }
}
