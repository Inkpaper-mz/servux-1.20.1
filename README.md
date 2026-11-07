# Servux 1.20.1 — Litematica server-side paste

Unofficial 1.20.1 backport of Servux's Litematica data provider.

It lets a Litematica client send a schematic placement to the server, which writes the blocks directly instead of running `/fill`. The command path is still there as a fallback, so servers without this mod keep working normally.

This is not an official masa or Sakura release. It is based on the `fabric_1.20.x` Servux branch and the LTS/1.21 Litematica provider, with the networking and placement code adapted to 1.20.1.

## What it does

- Adds the `servux:litematics` channel.
- Answers the client's metadata request on join/world load.
- Accepts a `LitematicaPaste` request with the placement NBT (origin, rotation, mirror, sub-regions).
- Pastes with the 1.21 placement code, so rotated and mirrored builds keep their orientation.
- Sends a paste result back to the client.

## Requirements

- Minecraft 1.20.1
- Fabric Loader
- Fabric API (the jar bundles `fabric-api-base`, `fabric-networking-api-v1`, and `fabric-permissions-api`)
- Java 17+

The client also needs a Litematica 1.20.1 build with Servux paste support. The client-side change is in this PR: https://github.com/sakura-ryoko/litematica/pull/423

## Install

1. Drop the jar into the server's `mods` folder.
2. Remove any older Servux jar with the same mod id.
3. Start the server and check the log for `servux 0.1.2-pos`.

## Configuration

`config/servux.json`:

```json
{
  "litematic_data": {
    "permission_level": 0,
    "permission_level_paste": 0
  }
}
```

- `permission_level`: level needed for metadata/basic data.
- `permission_level_paste`: level needed to paste.
- `0` means no OP requirement. `2` means OP level 2.
- With LuckPerms the nodes are `servux.provider.litematic_data` and `servux.provider.litematic_data.paste`.
- The player still has to be in creative mode, same as the 1.21 behavior.

Restart the server after changing the file. There is no live `/servux` command in this port yet.

## Known limits

- Placement NBT is capped at 1 MB by the packet splitter. Larger schematics will fail; this is the next thing to fix.
- The paste runs on the server thread. Small and medium builds are fine, but very large builds can cause a tick spike.
- If the client hasn't received the metadata yet, the first paste can still go through the command path. Pressing paste again uses Servux.
- The client-side fallback message may be in English even on a Chinese client; translations live in the Litematica PR.

## Build

```bash
./gradlew build
```

The wrapper points at Gradle 9.3.0. Loom 1.13 needs a recent JDK; JDK 21 was used for the release build. The compiled jar ends up in `build/libs`.

## Credits and license

- Servux by masa.
- The 1.21 Litematica provider code comes from Sakura's LTS branch.
- Litematica by masa.
- Licensed under LGPLv3; see `LICENSE.txt`. The full source for this build is in this repository.

If you redistribute the jar, keep the license and the source available, and don't present it as an official Servux build.
