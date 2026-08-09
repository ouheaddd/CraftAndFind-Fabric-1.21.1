package com.overyourhead.craftandfind.common.data;

import com.overyourhead.craftandfind.CraftAndFindMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/** Fabric replacement for NeoForge's Player#getPersistentData(). */
public final class ModDataAttachments {
    public static final AttachmentType<CompoundTag> PLAYER_PERSISTENT_DATA = AttachmentRegistry.create(
            ResourceLocation.fromNamespaceAndPath(CraftAndFindMod.MOD_ID, "player_persistent_data"),
            builder -> builder
                    .initializer(CompoundTag::new)
                    .persistent(CompoundTag.CODEC)
                    .copyOnDeath()
    );

    private ModDataAttachments() {
    }

    public static void bootstrap() {
        // Touching the class registers the attachment type during mod initialization.
    }

    public static CompoundTag get(Player player) {
        return target(player).getAttachedOrCreate(PLAYER_PERSISTENT_DATA);
    }

    public static void set(Player player, CompoundTag data) {
        target(player).setAttached(PLAYER_PERSISTENT_DATA, data);
    }

    public static void copy(Player original, Player clone) {
        CompoundTag data = target(original).getAttached(PLAYER_PERSISTENT_DATA);
        if (data != null) {
            target(clone).setAttached(PLAYER_PERSISTENT_DATA, data.copy());
        }
    }

    private static AttachmentTarget target(Player player) {
        return (AttachmentTarget) (Object) player;
    }
}
