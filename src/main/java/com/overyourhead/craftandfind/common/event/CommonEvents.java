package com.overyourhead.craftandfind.common.event;

import com.overyourhead.craftandfind.common.data.ModDataAttachments;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;

public final class CommonEvents {
    private CommonEvents() {
    }

    public static void register() {
        ServerPlayerEvents.COPY_FROM.register((original, clone, alive) -> {
            // Data Attachment API handles death respawns through copyOnDeath().
            // COPY_FROM also covers live player replacement (for example End return).
            if (alive) {
                ModDataAttachments.copy(original, clone);
            }
        });
    }
}
