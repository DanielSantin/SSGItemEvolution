# SSG ItemEvolution

[![GitHub Repo](https://img.shields.io/badge/repo-SSGItemEvolution-blue)](https://github.com/DanielSantin/SSGItemEvolution/)

**SSG ItemEvolution** is a Minecraft plugin that adds custom item evolution and enchantment management. Players can apply and store enchantments on books, configure item progressions via YAML, and enhance gameplay with a flexible API compatible with other plugins like Nexo.

---

## Features

- Custom item evolutions via `BetterItemsEvolutions.yml`
- Custom enchantment management
- Tracks item uses, levels, and points
- Adventure Component-based lore (supports modern Bukkit API)
- Integrates with other plugins like Nexo

---

## Requirements

- Minecraft 1.21.x
- PaperMC 1.21.1-R0.1-SNAPSHOT
- Java 21

---

## Build

To build the plugin jar, run:
```bash
.\gradlew clean shadowJar
```
The final jar will be located at:
```bash
build/libs/SSGItemEvolution.jar
```

## Installation
1. Copy the jar into your server's plugins folder.
2. Start or restart the server.
3. The plugin will automatically create necessary configuration files.

## Configuration
* BetterItemsEvolutions.yml – Define custom item evolutions.
* config.yml – Standard plugin configuration (generated on first run).

## Usage
Custom items and enchantments are managed via persistent data container.
Lore, levels, points, and progression are automatically updated.

## License
This project is licensed under the MIT License – see the LICENSE
