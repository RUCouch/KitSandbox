package dev.kitsandbox;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

final class KitStorage {
    private static final Logger LOGGER = Logger.getLogger(KitStorage.class.getName());

    private final File file;
    private final YamlConfiguration data;

    KitStorage(File file) {
        this.file = file;
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    boolean hasKit(UUID playerId, int slot) {
        return data.getBoolean(basePath(playerId, slot) + ".saved");
    }

    KitSnapshot getKit(UUID playerId, int slot) {
        String base = basePath(playerId, slot);
        if (!data.getBoolean(base + ".saved")) {
            return null;
        }
        return new KitSnapshot(
                readItems(base + ".storage", 36),
                readItems(base + ".armor", 4),
                readItems(base + ".extra", 1));
    }

    boolean saveKit(UUID playerId, int slot, PlayerInventory inventory) {
        String base = basePath(playerId, slot);
        data.set(base, null);
        data.set(base + ".saved", true);
        writeItems(base + ".storage", inventory.getStorageContents());
        writeItems(base + ".armor", inventory.getArmorContents());
        writeItems(base + ".extra", inventory.getExtraContents());
        return persist();
    }

    boolean saveKit(UUID playerId, int slot, KitSnapshot kit) {
        String base = basePath(playerId, slot);
        data.set(base, null);
        data.set(base + ".saved", true);
        writeItems(base + ".storage", kit.storage());
        writeItems(base + ".armor", kit.armor());
        writeItems(base + ".extra", kit.extra());
        return persist();
    }

    boolean deleteKit(UUID playerId, int slot) {
        data.set(basePath(playerId, slot), null);
        return persist();
    }

    private ItemStack[] readItems(String path, int size) {
        ItemStack[] items = new ItemStack[size];
        for (int index = 0; index < size; index++) {
            items[index] = data.getItemStack(path + "." + index);
        }
        return items;
    }

    private void writeItems(String path, ItemStack[] items) {
        for (int index = 0; index < items.length; index++) {
            ItemStack item = items[index];
            data.set(path + "." + index, item == null || item.getType().isAir() ? null : item.clone());
        }
    }

    private boolean persist() {
        try {
            data.save(file);
            return true;
        } catch (IOException exception) {
            LOGGER.severe(() -> "Could not save kits.yml: " + exception.getMessage());
            return false;
        }
    }

    private String basePath(UUID playerId, int slot) {
        return "players." + playerId + ".kits." + slot;
    }
}