# OrbitalStrike Minecraft Plugin

A Plugin for **Minecraft 1.21 - 26.2** featuring Orbitals:
###### Should work on any Spigot fork like Paper or Purpur  if not Message me on Discord or open a issue here

- `/orbital nuke` → **Massive crater with 10 rings + center TNT**
- `/orbital stab` → **Instant full-depth tunnel to bedrock**
- `/orbital give [playername] [type]` | Gives other Players a Rod |
- `/orbital dogs` → **50+ wolves ready to Help you**
- `/orbital stasis [coords]` → **if you need a quick escape**
- `/orbital totem [coords]` → **Totem of Undying that teleports you on pop**
- `/orbital wither` → **Wither skulls rain from above for 30 seconds, tracking nearby players**
- `/orbital chunkeater` → **Armorstand ready to destroy the whole Chunk**
---

## Features

| Feature | Description                                                                                     |
|--------|-------------------------------------------------------------------------------------------------|
| **Nuke Strike** | 10 rings + center, all TNT drops simultaneously from above, explodes **2 seconds after impact** |
| **Stab Strike** | Instant tunnel straight down to bedrock                                                         |
| **Dogs Strike** | Summons **50+ tamed wolves** with **Speed II + Strength II and Armor**                          |
| **Wither Cannon** | Wither skulls from above for **30 seconds**, tracks the nearest player (never the caster)        |
| **Totem Stasis** | Vanilla-looking **Totem stasis** — pops and teleports you to the coords you set                 |
| **Chunkeater** | Powerful Armorstand that destroys a whole Chunk                                                 |
| **One-Time Use** | Rod breaks after single use                                                                     |
| **Fully Configurable** | `config.yml` for rings, yield, height, delay, TNT block damage                                  |
| **Permission System** | `orbital.use` — easy with LuckPerms                                                             |
| **No Cooldown** | Spam allowed (can crash or lag the Server)                                                      |

---

## Installation

1. Download `OrbitalStrike-1.7.0.jar`
2. Place it in your `plugins/` folder
3. Go into **"spigot.yml"** and set **"max-tnt-per-tick"** to **1000** else it might cause problems
4. **Start the server**
5. `plugins/OrbitalStrike/config.yml` is auto-generated

---

## Commands

| Command | Description |
|--------|-------------|
| `/orbital nuke` | Gives you a **Nuke Rod** |
| `/orbital stab` | Gives you a **Stab Rod** |
| `/orbital dogs` | Gives you a **Dog Rod** |
| `/orbital wither` | Gives you a **Wither Rod** |
| `/orbital give [playername] [type]` | Gives other Players a Rod |
| `/orbital stasis [coords]` | Gives you a **Stasis Rod** |
| `/orbital totem [coords]` | Gives you a **Totem stasis** |
| `/orbital chunkeater` | Gives you a **Chunkeater Armorstand** |

> **Permission:** `orbital.use`  
> → Default: **OPs only**  
> → Should work with any Perms Plugin

---

## Configuration (`config.yml`)

```yaml
permission: "orbital.use"

disabled-worlds:
  - ""

cooldowns:
  enabled: false
  nuke: 300
  stab: 120
  dogs: 180
  chunkeater: 600
  stasis: 240
  wither: 240
  totem: 240

rod:
  distance: 100
  throw-rod: true

nuke:
  rings: 10
  height: 15
  yield: 6.0
  tnt-per-ring-base: 40
  tnt-per-ring-increase: 2
  fuse-ticks: 160
  center-tnt: true
  Animated-rings: true
  damaged-rings: true
  break-blocks: true   # false = damages players, not blocks

stab:
  yield: 4.0
  break-blocks: true   # false = damages players, not blocks

dogs:
  count: 50
  radius: 5.0
  effect-duration: 2400
  effects:
    - "SPEED:1"
    - "STRENGTH:2"

wither:
  charged: false   # blue charged skulls
  range: 8         # length of the rain (along where you look)
  width: 8         # 1 = a line, same as range = a square
  speed: 1.2       # skull travel speed
  skulls: 3        # skulls spawned per burst
  duration-ticks: 600  # how long it runs (20 ticks = 1s, 600 = 30s)
```


<p align="center">
 <a href="https://discord.com/users/1092033992288653424" target="_blank"><img src="https://img.shields.io/badge/Discord-Juliaan.py-blue?style=for-the-badge&logo=discord" /></a>
</p>
