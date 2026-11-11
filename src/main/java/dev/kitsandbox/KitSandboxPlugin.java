package dev.kitsandbox;

import java.io.File;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

public final class KitSandboxPlugin extends JavaPlugin implements CommandExecutor, Listener {
    private static final long SHARE_LIFETIME_MILLIS = 30 * 60 * 1000L;
    private static final String SHARE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom random = new SecureRandom();
    private final Map<String, SharedKit> sharedKits = new HashMap<>();
    private KitStorage storage;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            throw new IllegalStateException("Could not create plugin data folder.");
        }
        storage = new KitStorage(new File(getDataFolder(), "kits.yml"));
        PluginCommand kitCommand = Objects.requireNonNull(getCommand("kit"), "kit command missing from plugin.yml");
        kitCommand.setExecutor(this);
        for (int slot = 1; slot <= 9; slot++) {
            Objects.requireNonNull(getCommand("k" + slot), "k" + slot + " command missing from plugin.yml")
                    .setExecutor(this);
        }
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String commandName = command.getName().toLowerCase(Locale.ROOT);
        if (commandName.matches("k[1-9]")) {
            Player player = requirePlayer(sender);
            if (player != null) {
                loadKit(player, Character.digit(commandName.charAt(1), 10));
            }
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        if (action.equals("load")) {
            return handleShareLoad(sender, args);
        }
        if (args.length != 2) {
            sendHelp(sender);
            return true;
        }

        Integer slot = parseSlot(args[1]);
        if (slot == null) {
            sender.sendMessage(ChatColor.RED + "Kit number must be between 1 and 9.");
            return true;
        }
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }

        switch (action) {
            case "save" -> saveKit(player, slot);
            case "delete" -> deleteKit(player, slot);
            case "view" -> viewKit(player, slot);
            case "share" -> shareKit(player, slot);
            default -> loadKit(player, slot);
        }
        return true;
    }

    private boolean handleShareLoad(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /kit load <slot 1-9> <sharing code>");
            return true;
        }
        Integer slot = parseSlot(args[1]);
        if (slot == null) {
            sender.sendMessage(ChatColor.RED + "Kit number must be between 1 and 9.");
            return true;
        }
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }

        String code = args[2].toUpperCase(Locale.ROOT);
        SharedKit sharedKit = getActiveShare(code);
        if (sharedKit == null) {
            player.sendMessage(ChatColor.RED + "That kit sharing code is invalid or has expired.");
            return true;
        }
        if (!storage.saveKit(player.getUniqueId(), slot, sharedKit.kit())) {
            player.sendMessage(ChatColor.RED + "Could not save the shared kit. Check the server log.");
            return true;
        }
        sharedKits.remove(code);
        player.sendMessage(ChatColor.GREEN + "Shared kit imported into slot " + slot + ".");
        return true;
    }

    private void saveKit(Player player, int slot) {
        if (storage.saveKit(player.getUniqueId(), slot, player.getInventory())) {
            player.sendMessage(ChatColor.GREEN + "Your current inventory was saved to kit " + slot + ".");
        } else {
            player.sendMessage(ChatColor.RED + "Could not save your kit. Check the server log.");
        }
    }

    private void loadKit(Player player, int slot) {
        KitSnapshot kit = storage.getKit(player.getUniqueId(), slot);
        if (kit == null) {
            player.sendMessage(ChatColor.RED + "You have no kit saved in slot " + slot + ".");
            return;
        }
        PlayerInventory inventory = player.getInventory();
        inventory.setStorageContents(kit.storage());
        inventory.setArmorContents(kit.armor());
        inventory.setExtraContents(kit.extra());
        player.sendMessage(ChatColor.GREEN + "Loaded kit " + slot + ".");
    }

    private void deleteKit(Player player, int slot) {
        if (!storage.hasKit(player.getUniqueId(), slot)) {
            player.sendMessage(ChatColor.RED + "You have no kit saved in slot " + slot + ".");
            return;
        }
        if (storage.deleteKit(player.getUniqueId(), slot)) {
            player.sendMessage(ChatColor.GREEN + "Deleted kit " + slot + ".");
        } else {
            player.sendMessage(ChatColor.RED + "Could not delete the kit. Check the server log.");
        }
    }

    private void viewKit(Player player, int slot) {
        KitSnapshot kit = storage.getKit(player.getUniqueId(), slot);
        if (kit == null) {
            player.sendMessage(ChatColor.RED + "You have no kit saved in slot " + slot + ".");
            return;
        }

        KitPreviewHolder holder = new KitPreviewHolder();
        Inventory preview = Bukkit.createInventory(holder, 54, ChatColor.GOLD + "Kit " + slot);
        holder.setInventory(preview);
        ItemStack[] storageItems = kit.storage();
        for (int index = 0; index < storageItems.length; index++) {
            preview.setItem(index, storageItems[index]);
        }
        ItemStack[] armor = kit.armor();
        for (int index = 0; index < armor.length; index++) {
            preview.setItem(45 + index, armor[index]);
        }
        ItemStack[] extra = kit.extra();
        if (extra.length > 0) {
            preview.setItem(50, extra[0]);
        }
        player.openInventory(preview);
    }

    private void shareKit(Player player, int slot) {
        KitSnapshot kit = storage.getKit(player.getUniqueId(), slot);
        if (kit == null) {
            player.sendMessage(ChatColor.RED + "You have no kit saved in slot " + slot + ".");
            return;
        }
        removeExpiredShares();
        String code;
        do {
            code = createShareCode();
        } while (sharedKits.containsKey(code));
        long expiresAt = System.currentTimeMillis() + SHARE_LIFETIME_MILLIS;
        sharedKits.put(code, new SharedKit(kit, expiresAt));
        player.sendMessage(ChatColor.GREEN + "Sharing code: " + ChatColor.GOLD + code
                + ChatColor.GREEN + " (expires in 30 minutes). Share it with the recipient.");
    }

    private SharedKit getActiveShare(String code) {
        removeExpiredShares();
        return sharedKits.get(code);
    }

    private void removeExpiredShares() {
        long now = System.currentTimeMillis();
        Iterator<SharedKit> iterator = sharedKits.values().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().expiresAt() <= now) {
                iterator.remove();
            }
        }
    }

    private String createShareCode() {
        StringBuilder code = new StringBuilder(6);
        for (int index = 0; index < 6; index++) {
            code.append(SHARE_ALPHABET.charAt(random.nextInt(SHARE_ALPHABET.length())));
        }
        return code.toString();
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender == null) {
            return null;
        }
        if (sender instanceof Player player) {
            return player;
        }
        sender.sendMessage(ChatColor.RED + "This command can only be used by a player.");
        return null;
    }

    private Integer parseSlot(String value) {
        try {
            int slot = Integer.parseInt(value);
            return slot >= 1 && slot <= 9 ? slot : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "Kit commands:");
        sender.sendMessage(ChatColor.YELLOW + "/kit save <1-9> Save your current inventory");
        sender.sendMessage(ChatColor.YELLOW + "/kit <1-9> Load a kit (or /k1 through /k9)");
        sender.sendMessage(ChatColor.YELLOW + "/kit view|delete|share <1-9>");
        sender.sendMessage(ChatColor.YELLOW + "/kit load <your slot 1-9> <sharing code>");
    }

    @EventHandler
    public void onPreviewClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof KitPreviewHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPreviewDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof KitPreviewHolder) {
            event.setCancelled(true);
        }
    }

    private record SharedKit(KitSnapshot kit, long expiresAt) {
    }
}