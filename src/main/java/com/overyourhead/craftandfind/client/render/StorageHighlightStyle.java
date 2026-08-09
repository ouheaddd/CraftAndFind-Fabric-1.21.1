package com.overyourhead.craftandfind.client.render;

import com.overyourhead.craftandfind.CraftAndFindMod;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Non-configurable visual and sound tuning for storage-location feedback.
 *
 * Texture files live under:
 * assets/craftandfind/textures/effect/storage_highlight/
 *
 * User-facing options live in CraftAndFindClientConfig. These constants are
 * internal layout, animation, texture and sound-shape values.
 */
public final class StorageHighlightStyle {
    public static final long FADE_IN_MILLIS = 180L;
    public static final long MARKER_FADE_OUT_MILLIS = 850L;
    public static final long PARTICLE_FADE_OUT_MILLIS = 850L;

    public static final int MAX_SOUND_CONTAINERS = 3;

    public static final int MAIN_PARTICLE_COUNT = 16;
    public static final int SECONDARY_PARTICLE_COUNT = 9;
    public static final float MAIN_PARTICLE_ALPHA = 1.0F;
    public static final float SECONDARY_PARTICLE_ALPHA = 0.72F;
    public static final float DOUBLE_CHEST_PARTICLE_MULTIPLIER = 1.25F;
    public static final float PARTICLE_MIN_SIZE = 0.055F;
    public static final float PARTICLE_MAX_SIZE = 0.145F;
    public static final float PARTICLE_RADIUS_MIN = 0.47F;
    public static final float PARTICLE_RADIUS_MAX = 0.72F;
    public static final float PARTICLE_VERTICAL_DRIFT = 0.24F;

    /** Smooth time when the primary marker enters or leaves wall-visibility mode. */
    public static final long THROUGH_WALL_TRANSITION_MILLIS = 220L;
    /** Final opacity of the primary frame and item while hidden by blocks. */
    public static final float THROUGH_WALL_MARKER_ALPHA = 0.42F;
    /** GUI depth used by the through-wall frame, behind the item. */
    public static final float THROUGH_WALL_FRAME_GUI_Z = 190.0F;

    public static final float MARKER_WIDTH = 0.56F;
    public static final float MARKER_HEIGHT = 0.66F;
    /** Extra distance beyond the camera-facing side/corner of the block. */
    public static final float MARKER_SURFACE_GAP = 0.10F;
    public static final float MARKER_CENTER_Y = 0.55F;
    public static final float MARKER_TOP_Y = 1.20F;
    public static final float MARKER_TOP_CAMERA_OFFSET = 0.06F;
    /** Marker is fully on the front/side at or beyond this horizontal distance. */
    public static final float MARKER_TOP_TRANSITION_FAR = 1.65F;
    /** Marker is fully above the block at or inside this horizontal distance. */
    public static final float MARKER_TOP_TRANSITION_NEAR = 0.85F;
    public static final float MARKER_ITEM_SCALE = 0.35F;
    public static final float MARKER_ITEM_Y = 0.055F;
    /**
     * When true, 3D items are rendered in a real screen-space GUI pass at the
     * projected center of the world marker. This removes world perspective and
     * camera-dependent side changes completely. The marker frame remains the
     * same camera-facing world billboard.
     */
    public static final boolean FIXED_GUI_ITEM_ORIENTATION = true;
    /** GUI depth used by the screen-space 3D item. */
    public static final float FIXED_GUI_ITEM_Z = 200.0F;
    /** Used only when FIXED_GUI_ITEM_ORIENTATION is false. */
    public static final float GUI3D_YAW_DEGREES = 18.0F;
    /** Used only when FIXED_GUI_ITEM_ORIENTATION is false. */
    public static final float GUI3D_PITCH_DEGREES = -12.0F;
    /** Absolute depth offset used to keep the item in front of the marker texture. */
    public static final float MARKER_ITEM_Z = 0.025F;

    public static final float CLICK_VOLUME = 0.34F;
    public static final float CLICK_PITCH = 1.15F;
    public static final float PRIMARY_CHIME_VOLUME = 0.50F;
    public static final float SECONDARY_CHIME_VOLUME = 0.28F;
    public static final float CHIME_BASE_PITCH = 1.18F;
    public static final float CHIME_PITCH_STEP = 0.10F;

    public static final ResourceLocation MARKER_TEXTURE = texture("marker");
    public static final List<ResourceLocation> PARTICLE_TEXTURES = List.of(
            texture("particle_0"),
            texture("particle_1"),
            texture("particle_2"),
            texture("particle_3"),
            texture("particle_4"),
            texture("particle_5"),
            texture("particle_6")
    );

    private StorageHighlightStyle() {
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(
                CraftAndFindMod.MOD_ID,
                "textures/effect/storage_highlight/" + name + ".png"
        );
    }
}
