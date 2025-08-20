package mathew.discordLogger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
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


import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DiscordLogger extends JavaPlugin implements Listener {

    private String webhookUrl;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        webhookUrl = getConfig().getString("webhook");
        Bukkit.getPluginManager().registerEvents(this, this);
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        sendDiscord("🟢 Server Run Shod!" + "[" + time + "] ");
    }

    @Override
    public void onDisable() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        sendDiscord("🔴 Server off Shod!" + "[" + time + "] ");
    }

    private void sendDiscord(String message) {
        if (webhookUrl == null || webhookUrl.isEmpty()) return;

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                String json = "{\"content\": \"" + message.replace("\"", "\\\"") + "\"}";
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes());
                }
                conn.getInputStream().close();
            } catch (Exception ignored) {}
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        sendDiscord("✅ " + e.getPlayer().getName() + " Be Server Join shod!" +  "[" + time + "] ");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        sendDiscord("❌ " + e.getPlayer().getName() + " az server left dad! " +  "[" + time + "] ");
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        sendDiscord("☠️ " + e.getDeathMessage() + "[" + time + "] ");
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        sendDiscord("⌨️ " + e.getPlayer().getName() + " Az: " + e.getMessage() + " estefade kard! " + "[" + time + "] ");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        sendDiscord("⛏️ " + e.getPlayer().getName() + " broke " + e.getBlock().getType() + "[" + time + "] ");
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        sendDiscord("🧱 " + e.getPlayer().getName() + " placed " + e.getBlock().getType() + "[" + time + "] ");
    }

    @EventHandler
    public void onCraft(CraftItemEvent e) {
        if (e.getWhoClicked() instanceof org.bukkit.entity.Player p) {

            int amount = e.getRecipe().getResult().getAmount();
            if (e.isShiftClick()) {
                int maxCraftable = e.getInventory().getMaxStackSize();
                int resultAmount = e.getRecipe().getResult().getAmount();
                int possibleCrafts = Integer.MAX_VALUE;

                    for (ItemStack item : e.getInventory().getMatrix()) {
                    if (item != null && item.getType() != Material.AIR) {
                        possibleCrafts = Math.min(possibleCrafts, item.getAmount());
                    }
                }
                amount = possibleCrafts * resultAmount;
            }
            String item = e.getRecipe().getResult().getType().toString();
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            sendDiscord("⚒️ " + amount + ", " + item + " Tavasot " + p.getName() + " Craft Shod! "  + "[" + time + "] ");
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

        sendDiscord("📥 " + player.getName()  +", " + amount + "x " + item + " Ra Dar " + coords + " bardasht!" + "[" + time + "] ");
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

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
            Location loc = p.getLocation();
            String coords = "(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";

            sendDiscord("📥 " + p.getName() + " took " + amount + "x " + name + " from " + source + " at " + coords);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        Player player = e.getPlayer();
        ItemStack stack = e.getItemDrop().getItemStack();
        int amount = stack.getAmount();
        String item = stack.getType().toString();
        Location loc = player.getLocation();
        String coords = "(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";

        sendDiscord("📥 " + player.getName()  +", " + amount + "x " + item + " Ra Dar " + coords + " drop kard!" + "[" + time + "] ");
    }
    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        String player = e.getPlayer().getName();
        String msg = e.getMessage();
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        sendDiscord("[" + time + "] " + " 💬 " + player + ": " + msg);
    }
}
