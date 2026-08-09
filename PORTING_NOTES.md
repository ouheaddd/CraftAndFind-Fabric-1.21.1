# NeoForge -> Fabric 1.21.1 port notes

The goal of this port is behavioral parity, not a redesign.

## Preserved as-is

- `assets/craftandfind/**`
- `data/craftandfind/**`
- `pack.mcmeta`
- `craftandfind.mixins.json`
- vanilla/Minecraft-facing workbench, menu, storage, recipe-book and GUI logic wherever no loader API was involved

## Loader substitutions

- NeoForge `@Mod` / event bus -> Fabric `ModInitializer` + `ClientModInitializer`
- NeoForge deferred registries -> vanilla registry registration during Fabric initialization
- NeoForge payload registration / `PacketDistributor` -> Fabric payload registries + play networking
- NeoForge client render events -> Fabric world/HUD render callbacks
- NeoForge `ModConfigSpec` -> small TOML reader with the same settings/default ranges
- NeoForge `Player#getPersistentData()` -> Fabric Data Attachment API with persistent/copy-on-death data
- NeoForge player-clone event -> Fabric `ServerPlayerEvents.COPY_FROM`

## Build verification note

The source tree has been statically checked for remaining NeoForge imports and syntax-level Java errors. A full Gradle/Minecraft compile was not possible in the generation environment because binary dependency downloads (Gradle/Fabric/Minecraft artifacts) are blocked there. The included wrapper setup scripts fetch the official wrapper on a normal internet-connected machine, after which `gradlew build` performs the authoritative compile/remap check.
