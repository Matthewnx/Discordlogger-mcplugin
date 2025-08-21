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
import java.util.HashMap;
import java.util.Map;

public class DiscordLogger extends JavaPlugin implements Listener {

    private Map<String, String> webhooks = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        ConfigurationSection section = config.getConfigurationSection("webhooks");
        if(section != null) {
            for (String key : section.getKeys(false)) {
                webhooks.put(key, section.getString(key));
            }
        }
        Bukkit.getPluginManager().registerEvents(this, this);
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        sendDiscord("🟢 Server Run Shod!" + " [" + time + "] ");
    }

    @Override
    public void onDisable() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        sendDiscord("🔴 Server off Shod!" + " [" + time + "] ");
    }

    private void sendDiscord(String eventType,String message) {
        String url = webhooks.get(eventType);
        if (url == null || url.isEmpty()) return;

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                URL webhook = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) webhook.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                String json = "{\"content\":\"" + message + "\"}";
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }

                connection.getInputStream().close();
            } catch (Exception ignored) {}
        });
    }

    private String Bold(String message) {
        return "**" + message + "**";
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        sendDiscord("✅ " + Bold(e.getPlayer().getName()) + " Be Server Join shod!" +  " [" + time + "] ");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        sendDiscord("❌ " + Bold(e.getPlayer().getName()) + " az server left dad! " +  " [" + time + "] ");
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        sendDiscord("☠️ " + Bold(e.getDeathMessage()) + " [" + time + "] ");
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        Player player = e.getPlayer();
        String from = formatWorldName(e.getFrom().getName());   // previous world name
        String to = formatWorldName(player.getWorld().getName());   // new world name
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        sendDiscord("🌍 " + Bold(player.getName()) + " az  " + from + " be " + to + " raft! " + " [" + time + "] ");
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
        sendDiscord("⌨️ " + Bold(e.getPlayer().getName()) + " Az: " + e.getMessage() + " estefade kard! " + " [" + time + "] ");
    }
    @EventHandler
    public void onGamemodeChange(PlayerGameModeChangeEvent e) {
        Player player = e.getPlayer();

        String from = player.getGameMode().toString();   // old gamemode
        String to = e.getNewGameMode().toString();  // new gamemode

        sendDiscord("🎮 " + Bold(player.getName()) + " gamemode khod ra az " + from + " be " + to + " Switch Kard! ");
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
        sendDiscord("⛏️ " + Bold(player.getName()) + ", " +  e.getBlock().getType() +"  Ra Dar " + coords +", " + world + " Mine kard! " +  " [" + time + "] ");
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
            sendDiscord("⚒️ " + amount + ", " + item + " Tavasot " + Bold(player.getName()) + " Craft Shod! "  + " [" + time + "] ");
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

        sendDiscord("📥 " + Bold(player.getName())  +", " + amount + "x " + item + " Ra Dar " + coords + " bardasht!" + " [" + time + "] ");
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

            sendDiscord("📥 " + Bold(player.getName()) + ", " + amount + "x " + name + " ra  az " + source + " bardasht dar " + coords);
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

        sendDiscord("📥 " + Bold(player.getName())  +", " + amount + "x " + item + " Ra Dar " + coords + " drop kard!" + " [" + time + "] ");
    }
    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        String player = e.getPlayer().getName();
        String msg = e.getMessage();
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        sendDiscord(" [" + time + "] " + " 💬 " + Bold(player) + ": " + msg);
    }
}
