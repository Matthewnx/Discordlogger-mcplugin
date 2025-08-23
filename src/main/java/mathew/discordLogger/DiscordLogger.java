package mathew.discordLogger;

import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.jetbrains.annotations.NotNull;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

//main class
public class DiscordLogger extends JavaPlugin implements Listener {

    private final Map<String, String> webhooks = new ConcurrentHashMap<>();
    private final Map<String, List<String>> messageBuffer = new ConcurrentHashMap<>();

    public void queueDiscord(String eventType, String message) {

        messageBuffer.computeIfAbsent(eventType, k -> Collections.synchronizedList(new ArrayList<>())).add(message);

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

    // Send data to discord through the webhook
    private void sendDiscord(String eventType, String content) {
        try {
            saveDefaultConfig();
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
        queueDiscord("svstatus", "🟢 Server Run Shod!" + Bold(" [" + time + "]"));
    }

    @Override
    public void onDisable() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        queueDiscord("svstatus","🔴 Server off Shod" + Bold(" [" + time + "]"));
        flushAll();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        queueDiscord("join","✅ " + Bold(e.getPlayer().getName()) + " Be Server Join shod!" +  Bold(" [" + time + "] "));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        queueDiscord("quit","❌ " + Bold(e.getPlayer().getName()) + " az server left dad! " +  Bold(" [" + time + "] "));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        queueDiscord("death","☠️ " + Bold(e.getDeathMessage()) + "," + Bold(" [" + time + "] "));
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        Player player = e.getPlayer();
        String from = formatWorldName(e.getFrom().getName());   // previous world name
        String to = formatWorldName(player.getWorld().getName());   // new world name
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        queueDiscord("worldchange","🌍 " + Bold(player.getName()) + " az  " + Bold(from) + " be " + Bold(to) + " raft! " + Bold(" [" + time + "] "));
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
        queueDiscord("usecommand","⌨️ " + Bold(e.getPlayer().getName()) + " Az: " + Bold(e.getMessage()) + " estefade kard! " + Bold(" [" + time + "] "));
    }
    @EventHandler
    public void onGamemodeChange(PlayerGameModeChangeEvent e) {
        Player player = e.getPlayer();
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        String from = player.getGameMode().toString();   // old gamemode
        String to = e.getNewGameMode().toString();  // new gamemode

        queueDiscord("gamemode","🎮 " + Bold(player.getName()) + " gamemode khod ra az " + Bold(from) + " be " + Bold(to) + " Switch Kard! " +  Bold(" [" + time + "] "));
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
        queueDiscord("breakblock","⛏️ " + Bold(player.getName()) + ", " +  Bold(prettyItemName(e.getBlock().getType()).toString()) +"  Ra Dar " + Bold(coords) +", " + Bold(world) + " Mine kard! " +  Bold(" [" + time + "] ") );
    }

    @EventHandler
    public void onCraft(CraftItemEvent e) {

        if (e.getWhoClicked() instanceof org.bukkit.entity.Player player) {

            int amount = e.getRecipe().getResult().getAmount();
            //calculate stack operation
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
            String item = prettyItemName(e.getRecipe().getResult().getType()).toString();
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            queueDiscord("craft","⚒️ " + Bold(String.valueOf(amount)) + ", " + Bold(item) + " Tavasot " + Bold(player.getName()) + " Craft Shod! "  + Bold(" [" + time + "] "));
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

        queueDiscord("pickup","📥 " + Bold(player.getName())  +", " + Bold(String.valueOf(amount)) + "x " + Bold(item) + " Ra Dar " + Bold(coords) + " bardasht!" + Bold(" [" + time + "] "));
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
            String name = prettyItemName(item.getType());
            String source = e.getInventory().getType().toString(); // CHEST, BARREL, FURNACE, etc.

            //calculate exact position of player at doing this event
            Location loc = player.getLocation();
            String coords = "(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

            queueDiscord("pickup","📥 " + Bold(player.getName()) + ", " + Bold(String.valueOf(amount)) + "x " + Bold(name) + " ra  az " + Bold(source) + " bardasht dar " + Bold(coords) + "." + Bold(" [" + time + "] "));
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {

        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        Player player = e.getPlayer();

        //item artibutes
        ItemStack stack = e.getItemDrop().getItemStack();
        int amount = stack.getAmount();
        String item = prettyItemName(stack.getType());

        //calculate exact position of player at doing this event
        Location loc = player.getLocation();
        String coords = "(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";

        queueDiscord("drop","📥 " + Bold(player.getName())  +", " + Bold(String.valueOf(amount)) + "x " + Bold(item) + " Ra Dar " + Bold(coords) + " drop kard!" + Bold(" [" + time + "] "));
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        String player = e.getPlayer().getName();
        String msg = e.getMessage();
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        queueDiscord("chat",Bold(" [" + time + "] ") + " 💬 " + Bold(player) + ": " + msg);
    }

    @EventHandler
    public void onAdvancementDone(PlayerAdvancementDoneEvent e) {
        //main attributes
        Player player = e.getPlayer();
        Advancement advancement = e.getAdvancement();

        NamespacedKey key = advancement.getKey();

        // ignore recipes and hidden advancements
        if (key.getKey().startsWith("recipes/")) {
            return;
        }
        if (advancement.getDisplay() == null || advancement.getDisplay().isHidden()) {
            return;
        }

        // get readable title
        String display = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(advancement.getDisplay().title());

        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        queueDiscord("advancement", "🏆 " + Bold(player.getName()) + " Achievement: " + Bold(display) + " daryaft kard " + Bold("[" + time + "]")
        );
    }

    // fix entity name
    private String prettifyName(@NotNull EntityType type) {
        String raw = type.name().toLowerCase(); // raw
        String[] parts = raw.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            sb.append(Character.toUpperCase(p.charAt(0)))
                    .append(p.substring(1))
                    .append(" ");
        }
        return sb.toString().trim(); // unified
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;

        String mobName = prettifyName(e.getEntity().getType());
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        queueDiscord("mobkills", "⚔️ " + Bold(killer.getName()) + " yek " + Bold(mobName) + " ro kosht!" + Bold(" [" + time + "] "));
    }

    @EventHandler
    public void onVillagerTrade(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!(e.getInventory() instanceof MerchantInventory merchantInv)) return;
        if (e.getSlotType() != InventoryType.SlotType.RESULT) return;

        MerchantRecipe recipe = merchantInv.getSelectedRecipe();
        if (recipe == null) return;

        // Get villager type
        String villagerType;
        if (merchantInv.getMerchant() instanceof Villager villager) {
            String prof = villager.getProfession().toString().toLowerCase().replace("_", " ");
            villagerType = Character.toUpperCase(prof.charAt(0)) + prof.substring(1);
        } else {
            villagerType = "Villager";
        }

        ItemStack resultProto = recipe.getResult();

        // Calculate how many trades were performed
        int tradesPerformed = 1;

        // If shift-clicked, calculate maximum possible trades
        if (e.isShiftClick()) {
            int maxPossibleTrades = Integer.MAX_VALUE;

            // Check ingredient limitations
            for (ItemStack ingredient : recipe.getIngredients()) {
                if (ingredient != null && ingredient.getType() != Material.AIR) {
                    int available = countMaterial(player.getInventory(), ingredient.getType());
                    int required = ingredient.getAmount();
                    maxPossibleTrades = Math.min(maxPossibleTrades, available / required);
                }
            }

            // Check inventory space for result
            int resultSpace = calculateSpaceForItem(player.getInventory(), resultProto);
            maxPossibleTrades = Math.min(maxPossibleTrades, resultSpace / resultProto.getAmount());

            tradesPerformed = Math.max(1, maxPossibleTrades);
        }

        // Calculate costs
        StringBuilder costBuilder = new StringBuilder();
        for (ItemStack ingredient : recipe.getIngredients()) {
            if (ingredient != null && ingredient.getType() != Material.AIR) {
                int totalUsed = ingredient.getAmount() * tradesPerformed;
                costBuilder.append(totalUsed)
                        .append("x ")
                        .append(prettyItemName(ingredient.getType()))
                        .append(", ");
            }
        }

        String costs = costBuilder.length() > 2 ?
                costBuilder.substring(0, costBuilder.length() - 2) : "??";

        // Prepare log message
        Location loc = player.getLocation();
        String coords = "(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        int totalResultAmount = resultProto.getAmount() * tradesPerformed;
        String resultName = totalResultAmount + "x " + prettyItemName(resultProto.getType());

        queueDiscord("trade",
                "🤝 " + Bold(player.getName()) + " ba yek " + Bold(villagerType) +
                        " dar " + Bold(coords) + " trade kard va " + Bold(resultName) +
                        " ra da ezaye → " + Bold(costs) + " gereft " + Bold(" [" + time + "]")
        );
    }

    // Helper method to calculate inventory space for an item
    private int calculateSpaceForItem(Inventory inv, ItemStack item) {
        int space = 0;
        for (ItemStack stack : inv.getStorageContents()) {
            if (stack == null || stack.getType() == Material.AIR) {
                space += item.getMaxStackSize();
            } else if (stack.isSimilar(item)) {
                space += item.getMaxStackSize() - stack.getAmount();
            }
        }
        return space;
    }

    // Keep your existing helper methods:
    private int countMaterial(Inventory inv, Material mat) {
        int total = 0;
        for (ItemStack s : inv.getContents()) {
            if (s != null && s.getType() == mat) total += s.getAmount();
        }
        return total;
    }

    private String prettyItemName(Material mat) {
        return mat.toString().toLowerCase().replace("_", " ");
    }
}