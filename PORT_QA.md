# Port QA summary

Checks performed before packaging:

- Minecraft metadata targets exactly `1.21.1`.
- `fabric.mod.json` parses successfully and points at the Fabric common/client entrypoints.
- All JSON files in the project parse successfully.
- All 68 files that existed under the original `src/main/resources` are present and SHA-256 identical to the NeoForge source project.
- The only additional resource file is `fabric.mod.json`.
- No original Java source file was removed: original 40 -> Fabric port 43.
- 27 original Java files remain byte-for-byte identical.
- 13 original Java files were changed only where loader integration or its direct call sites required it.
- 3 Fabric glue/helper classes were added (`CraftAndFindClient`, `ModDataAttachments`, `SimpleTomlConfig`).
- No `net.neoforged`, `DeferredRegister`, `PacketDistributor`, `ModConfigSpec`, NeoForge `@Mod`, or `neoforge.mods.toml` references remain in the compilable port tree.
- No generated `.class`, Gradle build directory, run directory, or javac scratch files are included.

A full Gradle/Minecraft compile/remap was not possible in the generation environment because binary dependency downloads are unavailable there. Run `setup-wrapper.bat` (Windows) or `./setup-wrapper.sh` (Linux/macOS), then `gradlew.bat build` / `./gradlew build` on a normal internet-connected Java 21 machine for the authoritative build check.
