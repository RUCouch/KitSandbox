# KitSandbox

A Paper plugin for nine personal inventory kits and temporary kit sharing on sandbox PvP servers.

## Requirements

- Java 21
- Maven 3.9 or newer
- Paper 1.21.4

## Build

From the project directory, run:

```powershell
mvn package
```

Copy `target/kit-sandbox-1.0.0.jar` into the server's `plugins` directory, then restart the Paper server. Kits are stored in `plugins/KitSandbox/kits.yml`. Sharing codes are held in memory and are cleared when the server restarts.

## Commands

- `/kit save <1-9>` or `/k save <1-9>` saves your storage inventory, armor, and offhand.
- `/kit <1-9>` or `/k1` through `/k9` loads one of your kits, replacing your current inventory.
- `/kit view <1-9>` opens a read-only preview.
- `/kit delete <1-9>` deletes one of your saved kits.
- `/kit share <1-9>` generates a unique six-character code valid for 30 minutes.
- `/kit load <your-slot-1-9> <sharing-code>` imports a shared kit into your selected slot. A code is consumed after a successful import.

All kit commands are available by default through the `kitsandbox.use` permission.