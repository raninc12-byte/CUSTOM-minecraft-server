# Skyblock Plugin

A custom Skyblock plugin for **Paper 1.20+** Minecraft servers, written in Java 17.

## Features

- **Shop System** — `/shop` GUI with categories, pagination, and buy/sell/sellall
- **Island System** — `/is create|home|invite|accept|leave|level|top|delete`
- **Admin Panel** — `/admin` GUI with server info, player manager, world control, maintenance
- **Economy** — SQLite-backed player balances, `/balance`, `/pay`
- **GUI Framework** — Custom InventoryHolder-based click routing (no string matching)
- **Cross-platform** — Runs identically on Linux, Windows, and macOS

## Project Structure

```
src/main/java/com/gulis/skyblock/
├── core/         # Main plugin, config, database
├── gui/          # Abstract GUI base + listener + manager
├── economy/      # Balance management, /balance, /pay
├── shop/         # ShopManager, ShopGUI, /shop /buy /sell /sellall
├── island/       # Island, IslandManager, IslandGenerator, /is
└── admin/        # AdminGUI, PlayerManagerGUI, ServerControlGUI, /admin
```

## Build

Requires JDK 17 and Gradle (the wrapper is included).

```bash
# Linux / macOS
./gradlew build

# Windows
gradlew.bat build
```

The compiled jar is at `build/libs/Skyblock-1.0.0.jar` (a fat jar with bundled dependencies).

## Install

1. Download Paper 1.20.6 from <https://papermc.io/downloads/paper>
2. Copy `Skyblock-1.0.0.jar` into your server's `plugins/` folder
3. Start the server once to generate the default config files
4. Edit `plugins/Skyblock/config.yml`, `shops.yml`, `messages.yml` as needed
5. Restart the server

## Commands

| Command | Description |
|---------|-------------|
| `/is` `/island` | Main island command |
| `/shop` | Open the shop GUI |
| `/buy <item> [amount]` | Buy an item |
| `/sell <item> [amount]` | Sell an item |
| `/sellall` | Sell all sellable items |
| `/balance` `/bal` | Show your balance |
| `/pay <player> <amount>` | Pay another player |
| `/admin` `/panel` | Open the admin panel (ops) |

## Permissions

| Permission | Default |
|-----------|---------|
| `skyblock.island` | true |
| `skyblock.shop` | true |
| `skyblock.shop.buy` | true |
| `skyblock.shop.sell` | true |
| `skyblock.shop.sellall` | true |
| `skyblock.economy` | true |
| `skyblock.economy.pay` | true |
| `skyblock.admin` | op |
| `skyblock.*` | op (grants all) |

## Configuration

- `config.yml` — economy starting balance, island world/spacing, admin settings
- `shops.yml` — shop categories and item prices
- `messages.yml` — player-facing messages

All files support color codes via `&` (e.g. `&a` for green).

## Auto-Restart Wrapper (Linux)

To make the in-game restart button actually restart the server, wrap the Paper jar in a shell script:

```bash
#!/bin/bash
# start.sh
while true; do
    java -Xms2G -Xmx4G -jar paper-1.20.6.jar --nogui
    echo "Server restarting in 5 seconds..."
    sleep 5
done
```

Make it executable: `chmod +x start.sh`, then run `./start.sh`.

## Development

The plugin uses Paper's Adventure API for modern text components. To add a new GUI:

1. Extend `GUI` and implement `build()` and `handleClick()`
2. Open it via `new MyGUI(plugin, player).open()` and register with `GUIManager`
3. The `GUIListener` will automatically route clicks back to your handler
