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

//main class
public class DiscordLogger extends JavaPlugin implements Listener {

    private final Map<String, List<String>> messageBuffer = new ConcurrentHashMap<>();
    private final Map<String, String> webhooks = new HashMap<>();

    public void queueDiscord(String eventType, String message) {
        messageBuffer.computeIfAbsent(eventType, k -> new ArrayList<>()).add(message);
    }
    public void startFlushTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            for (Map.Entry<String, List<String>> entry : messageBuffer.entrySet()) {
                List<String> msgs = entry.getValue();
                if (!msgs.isEmpty()) {
                    String combined = String.join("\n", msgs);
                    sendDiscord(entry.getKey(), combined);
                    msgs.clear();
                }
            }
        }, 20L, 40L); // runs every 10s
    }
    public void flushAll() {
        // snapshot + clear under lock to avoid ConcurrentModification
        Map<String, String> toSend = new HashMap<>();
        synchronized (messageBuffer) {
            for (Map.Entry<String, List<String>> entry : messageBuffer.entrySet()) {
                List<String> msgs = entry.getValue();
                if (msgs != null && !msgs.isEmpty()) {
                    toSend.put(entry.getKey(), String.join("\n", msgs));
                    msgs.clear();
                }
            }
        }
        // send outside the lock
        for (Map.Entry<String, String> e : toSend.entrySet()) {
            try {
                sendDiscord(e.getKey(), e.getValue());
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // load webhooks from config
        FileConfiguration config = getConfig();
        ConfigurationSection section = config.getConfigurationSection("webhooks");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                webhooks.put(key, section.getString(key));
            }
        }

        // register events
        Bukkit.getPluginManager().registerEvents(this, this);

        // start the async flushing task
        startFlushTask();

        // send startup log
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        queueDiscord("svstatus", "🟢 Server Run Shod! [" + time + "]");
    }

    @Override
    public void onDisable() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        queueDiscord("svstatus","🔴 Server off Shod! [" + time + "]");
        flushAll();
    }

    //send data to discord trow the webhook
    //you can also bypass queqe with using this directly
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

            String json = "{\"content\":\"" + content.replace("\"", "\\\"") + "\"}";
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes());
            }

            conn.getInputStream().close();
        } catch (Exception ignored) {}
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
