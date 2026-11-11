package dev.kitsandbox;

import org.bukkit.inventory.ItemStack;

final class KitSnapshot {
    private final ItemStack[] storage;
    private final ItemStack[] armor;
    private final ItemStack[] extra;

    KitSnapshot(ItemStack[] storage, ItemStack[] armor, ItemStack[] extra) {
        this.storage = cloneItems(storage);
        this.armor = cloneItems(armor);
        this.extra = cloneItems(extra);
    }

    ItemStack[] storage() {
        return cloneItems(storage);
    }

    ItemStack[] armor() {
        return cloneItems(armor);
    }

    ItemStack[] extra() {
        return cloneItems(extra);
    }

    private static ItemStack[] cloneItems(ItemStack[] items) {
        ItemStack[] copy = new ItemStack[items.length];
        for (int index = 0; index < items.length; index++) {
            copy[index] = items[index] == null ? null : items[index].clone();
        }
        return copy;
    }
}