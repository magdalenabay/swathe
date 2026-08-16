# Swathe

Shaped area mining for Minecraft **1.21.10** on Fabric.

Break blocks in `1x1`, `3x3x3`, `5x5x5` or a custom shape, with a live outline preview.

The idea isn't original. This is a Fabric version of **Nylad59**'s Forge/NeoForge mod
[Zone Miner](https://www.curseforge.com/minecraft/mc-mods/zone-miner). The feature set is
theirs and the credit for it belongs to them. Go look at the original.

## Controls

| Key | Action |
|-----|--------|
| `Y` | Cycle mode: 1x1 → 3x3x3 → 5x5x5 → Custom |
| `U` | Open the area settings screen |

Both rebindable under Options → Controls → Swathe. Settings live in `config/swathe.json`.

## Two ways it can break blocks

The mod works whether or not the server has it installed, by using a different mechanism
in each case. It picks automatically, with no configuration.

### Server-side (singleplayer, or a server with the mod)

The whole shape breaks at once. The client sends its shape over a custom payload, and the
server runs each block through the same sequence vanilla's own `ServerPlayerGameMode` uses
— `playerWillDestroy` → `removeBlock` → `destroy` → `mineBlock` → `playerDestroy` — so
drops, XP, durability, block entities and advancements all behave exactly as if the block
had been mined by hand. `PlayerBlockBreakEvents.BEFORE` is re-fired per block, so claim and
protection mods keep their veto.

No reach limit, no sequential mining. **Singleplayer gets this path for free**, because the
integrated server is a real server running the same code.

### Client-side fallback (vanilla servers)

A client alone cannot delete blocks — the server owns the world. So instead the mod queues
the extra blocks and drives Minecraft's *own* mining loop over them one at a time, as if you
had aimed at each block and held attack. The server sees ordinary, correctly-timed mining, so
everything still behaves normally with nothing installed server-side.

Two consequences fall out of that:

- **Blocks break sequentially**, not at once. A 5x5x5 takes noticeably longer than one block.
- **Blocks past your reach are skipped** until they come into range. Walk forward while
  holding attack and the rest finishes.

Detection is automatic: `ClientPlayNetworking.canSend` is only true when the server declared
it can receive the mod's channel.

## Behaviour

- Releasing attack cancels the remainder of a client-side area break.
- Swapping tools mid-break cancels the remainder.
- Extra blocks only break when the held tool actually speeds them up, so a pickaxe will not
  drag dirt along with the stone. Toggleable as *Tool check*.
- Unbreakable blocks (bedrock, barriers) and liquids are skipped.
- The preview only outlines blocks that will *actually* break — it runs the same filter the
  miner does.
- Shapes are capped at 512 blocks, and the server re-clamps whatever the client sends.

## Installing

Drop the jar in `mods/`. Client-only works on any server. Installing it server-side as well
upgrades everyone who has the mod to the instant, unlimited-reach path.

## Building

Needs JDK 21.

```bash
./gradlew build
```

Output: `build/libs/swathe-<version>.jar`.

## Credits

[Zone Miner](https://www.curseforge.com/minecraft/mc-mods/zone-miner) by **Nylad59** —
the mod this one copies the idea from. Forge and NeoForge only, which is why this exists.

## Licence

MIT. See [LICENSE](LICENSE).
