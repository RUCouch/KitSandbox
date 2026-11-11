package dev.kitsandbox;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

final class KitPreviewHolder implements InventoryHolder {
    private Inventory inventory;

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}