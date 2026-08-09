package com.overyourhead.craftandfind.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.overyourhead.craftandfind.common.storage.StorageHighlightTarget;
import com.overyourhead.craftandfind.config.CraftAndFindClientConfig;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.List;

/**
 * Client-only storage locator effect.
 *
 * Every matching container gets editable sparkle textures. The container with
 * the largest amount gets the editable marker texture and the selected item.
 */
public final class StorageHighlightRenderer {
    private static List<StorageHighlightTarget> targets = List.of();
    private static ItemStack highlightedStack = ItemStack.EMPTY;
    private static long startedAt;
    private static long markerVisibleUntil;
    private static long particlesVisibleUntil;
    private static FixedGuiMarkerProjection fixedGuiMarkerProjection;
    private static float throughWallBlend;
    private static long lastVisibilityUpdateMillis;

    private StorageHighlightRenderer() {
    }

    public static void show(ItemStack stack, List<StorageHighlightTarget> newTargets) {
        if (!CraftAndFindClientConfig.enabled()) {
            clear();
            return;
        }

        highlightedStack = stack.copyWithCount(1);
        targets = List.copyOf(newTargets);
        startedAt = Util.getMillis();
        throughWallBlend = 0.0F;
        lastVisibilityUpdateMillis = startedAt;
        markerVisibleUntil = startedAt + CraftAndFindClientConfig.markerDurationMillis();
        particlesVisibleUntil = markerVisibleUntil
                + CraftAndFindClientConfig.extraParticleDurationMillis();

        if (CraftAndFindClientConfig.enableSounds()) {
            playFeedbackSounds();
        }
    }

    public static void render(WorldRenderContext context) {
        fixedGuiMarkerProjection = null;

        if (!CraftAndFindClientConfig.enabled()) {
            clear();
            return;
        }

        long now = Util.getMillis();
        if (targets.isEmpty() || highlightedStack.isEmpty() || now > particlesVisibleUntil) {
            clear();
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        PoseStack poseStack = context.matrixStack();
        if (minecraft.level == null || poseStack == null) {
            return;
        }

        Camera camera = context.camera();
        Vec3 cameraPosition = camera.getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        float elapsedSeconds = (now - startedAt) / 1_000.0F;
        float particleAlpha = visibilityAlpha(
                now,
                particlesVisibleUntil,
                StorageHighlightStyle.PARTICLE_FADE_OUT_MILLIS
        );
        float markerAlpha = visibilityAlpha(
                now,
                markerVisibleUntil,
                StorageHighlightStyle.MARKER_FADE_OUT_MILLIS
        );

        StorageHighlightTarget primary = targets.getFirst();
        boolean primaryLoaded = isAreaLoaded(minecraft, primary);
        boolean primaryOccluded = primaryLoaded && isAreaOccluded(minecraft, camera, primary);
        updateThroughWallBlend(primaryOccluded, now);
        float wallVisibilityBlend = smoothStep(throughWallBlend);

        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);

        int renderedTargets = targets.size();
        if (CraftAndFindClientConfig.enableParticles()
                && CraftAndFindClientConfig.particleAmount() > 0.0D) {
            for (int index = 0; index < renderedTargets; index++) {
                StorageHighlightTarget target = targets.get(index);
                if (!isAreaLoaded(minecraft, target)) {
                    continue;
                }

                renderParticles(
                        poseStack,
                        buffers,
                        camera,
                        target,
                        index == 0,
                        elapsedSeconds,
                        particleAlpha,
                        index == 0 ? wallVisibilityBlend : 0.0F
                );
            }
        }

        if (CraftAndFindClientConfig.showItemMarker()
                && now <= markerVisibleUntil
                && markerAlpha > 0.0F
                && primaryLoaded) {
            renderMarker(
                    minecraft,
                    poseStack,
                    buffers,
                    camera,
                    primary,
                    markerAlpha,
                    wallVisibilityBlend,
                    context.positionMatrix(),
                    context.projectionMatrix(),
                    minecraft.getWindow().getGuiScaledWidth(),
                    minecraft.getWindow().getGuiScaledHeight(),
                    now
            );
        }

        poseStack.popPose();
        buffers.endBatch();
    }

    public static void renderFixedGuiItem(GuiGraphics guiGraphics) {
        if (!CraftAndFindClientConfig.enabled()) {
            return;
        }

        long now = Util.getMillis();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        FixedGuiMarkerProjection projection = fixedGuiMarkerProjection;
        if (projection == null
                || highlightedStack.isEmpty()
                || now > markerVisibleUntil
                || now - projection.updatedAtMillis() > 250L) {
            guiGraphics.flush();
            return;
        }

        if (projection.frameAlpha() > 0) {
            renderFixedGuiFrame(guiGraphics, projection);
            guiGraphics.flush();
        }

        if (projection.itemAlpha() > 0) {
            PoseStack guiPose = guiGraphics.pose();
            guiPose.pushPose();
            guiPose.translate(
                    projection.itemX(),
                    projection.itemY(),
                    StorageHighlightStyle.FIXED_GUI_ITEM_Z
            );
            guiPose.scale(projection.itemScale(), -projection.itemScale(), projection.itemScale());

            MultiBufferSource.BufferSource guiBuffers = guiGraphics.bufferSource();
            MultiBufferSource alphaBuffers = renderType -> new AlphaOverrideVertexConsumer(
                    guiBuffers.getBuffer(
                            projection.itemAlpha() < 255
                                    ? Sheets.translucentItemSheet()
                                    : renderType
                    ),
                    projection.itemAlpha()
            );

            minecraft.getItemRenderer().renderStatic(
                    highlightedStack,
                    ItemDisplayContext.GUI,
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    guiPose,
                    alphaBuffers,
                    minecraft.level,
                    0
            );

            guiPose.popPose();
        }

        guiGraphics.flush();
    }

    private static void renderParticles(
            PoseStack poseStack,
            MultiBufferSource.BufferSource buffers,
            Camera camera,
            StorageHighlightTarget target,
            boolean primary,
            float elapsedSeconds,
            float globalAlpha,
            float wallBlend
    ) {
        int baseParticleCount = primary
                ? StorageHighlightStyle.MAIN_PARTICLE_COUNT
                : StorageHighlightStyle.SECONDARY_PARTICLE_COUNT;
        float areaMultiplier = target.isDoubleChest()
                ? StorageHighlightStyle.DOUBLE_CHEST_PARTICLE_MULTIPLIER
                : 1.0F;
        int particleCount = Math.round(
                baseParticleCount
                        * areaMultiplier
                        * (float) CraftAndFindClientConfig.particleAmount()
        );
        if (particleCount <= 0) {
            return;
        }
        float roleAlpha = primary
                ? StorageHighlightStyle.MAIN_PARTICLE_ALPHA
                : StorageHighlightStyle.SECONDARY_PARTICLE_ALPHA;

        double centerX = target.centerX();
        double centerZ = target.centerZ();
        double halfWidth = target.width() * 0.5D;
        double halfDepth = target.depth() * 0.5D;
        long areaSeed = mix(target.minPos().asLong() * 31L + target.maxPos().asLong());

        for (int index = 0; index < particleCount; index++) {
            long seed = mix(areaSeed + 0x9E3779B97F4A7C15L * (index + 1L));
            int variant = Math.floorMod((int) seed, StorageHighlightStyle.PARTICLE_TEXTURES.size());
            ResourceLocation texture = StorageHighlightStyle.PARTICLE_TEXTURES.get(variant);

            double lifetime = 1.10D + unit(seed + 11L) * 0.95D;
            double phaseOffset = unit(seed + 23L) * lifetime;
            double phase = ((elapsedSeconds + phaseOffset) % lifetime) / lifetime;
            float lifeAlpha = (float) Math.sin(Math.PI * phase);
            float worldVisibility = primary ? 1.0F - wallBlend : 1.0F;
            int worldAlpha = Math.round(
                    255.0F * globalAlpha * roleAlpha * lifeAlpha * worldVisibility
            );
            if (worldAlpha <= 3) {
                continue;
            }

            double angle = unit(seed + 37L) * Math.PI * 2.0D;
            double directionX = Math.cos(angle);
            double directionZ = Math.sin(angle);
            double edgeDistance = distanceToAreaEdge(directionX, directionZ, halfWidth, halfDepth);
            double edgeOffset = lerp(
                    StorageHighlightStyle.PARTICLE_RADIUS_MIN - 0.5D,
                    StorageHighlightStyle.PARTICLE_RADIUS_MAX - 0.5D,
                    unit(seed + 41L)
            );
            double wobble = Math.sin(elapsedSeconds * 2.2D + unit(seed + 43L) * Math.PI * 2.0D) * 0.035D;
            double radius = edgeDistance + edgeOffset + wobble;
            double x = centerX + directionX * radius;
            double z = centerZ + directionZ * radius;
            double y = target.minPos().getY()
                    + 0.12D
                    + unit(seed + 47L) * 0.78D
                    + phase * StorageHighlightStyle.PARTICLE_VERTICAL_DRIFT;

            float sizeT = (variant + 0.5F) / StorageHighlightStyle.PARTICLE_TEXTURES.size();
            float size = (float) lerp(
                    StorageHighlightStyle.PARTICLE_MIN_SIZE,
                    StorageHighlightStyle.PARTICLE_MAX_SIZE,
                    sizeT
            );
            size *= 0.82F + (float) unit(seed + 53L) * 0.36F;
            if (!primary) {
                size *= 0.84F;
            }

            renderTexturedBillboard(
                    poseStack,
                    buffers,
                    camera,
                    texture,
                    new Vec3(x, y, z),
                    size,
                    size,
                    worldAlpha
            );
        }
    }

    private static void renderMarker(
            Minecraft minecraft,
            PoseStack poseStack,
            MultiBufferSource.BufferSource buffers,
            Camera camera,
            StorageHighlightTarget target,
            float globalAlpha,
            float wallBlend,
            Matrix4f modelViewMatrix,
            Matrix4f projectionMatrix,
            int guiWidth,
            int guiHeight,
            long now
    ) {
        Vec3 cameraPosition = camera.getPosition();
        Vec3 sideCenter = new Vec3(
                target.centerX(),
                target.minPos().getY() + StorageHighlightStyle.MARKER_CENTER_Y,
                target.centerZ()
        );
        Vec3 horizontalToCamera = new Vec3(
                cameraPosition.x - sideCenter.x,
                0.0D,
                cameraPosition.z - sideCenter.z
        );

        Vec3 directionToCamera = horizontalToCamera.lengthSqr() > 0.0001D
                ? horizontalToCamera.normalize()
                : new Vec3(0.0D, 0.0D, 1.0D);

        double blockSurfaceDistance = distanceToAreaEdge(
                directionToCamera.x,
                directionToCamera.z,
                target.width() * 0.5D,
                target.depth() * 0.5D
        );
        Vec3 sidePosition = sideCenter.add(
                directionToCamera.scale(blockSurfaceDistance + StorageHighlightStyle.MARKER_SURFACE_GAP)
        );
        Vec3 topPosition = new Vec3(
                target.centerX(),
                target.minPos().getY() + StorageHighlightStyle.MARKER_TOP_Y,
                target.centerZ()
        ).add(directionToCamera.scale(StorageHighlightStyle.MARKER_TOP_CAMERA_OFFSET));

        float topBlend = markerTopBlend((float) horizontalToCamera.length());
        Vec3 markerPosition = lerp(sidePosition, topPosition, topBlend);
        Vec3 throughWallPosition = new Vec3(
                target.centerX(),
                target.minPos().getY() + StorageHighlightStyle.MARKER_CENTER_Y,
                target.centerZ()
        );

        boolean gui3d = minecraft.getItemRenderer()
                .getModel(highlightedStack, minecraft.level, minecraft.player, 0)
                .isGui3d();
        boolean fixedGui3d = gui3d && StorageHighlightStyle.FIXED_GUI_ITEM_ORIENTATION;
        float worldVisibility = 1.0F - wallBlend;
        int worldFrameAlpha = Math.round(255.0F * globalAlpha * worldVisibility);
        int throughWallAlpha = Math.round(
                255.0F
                        * globalAlpha
                        * StorageHighlightStyle.THROUGH_WALL_MARKER_ALPHA
                        * wallBlend
        );
        int guiItemAlpha = fixedGui3d
                ? Math.round(
                        255.0F
                                * globalAlpha
                                * (float) lerp(
                                        1.0D,
                                        StorageHighlightStyle.THROUGH_WALL_MARKER_ALPHA,
                                        wallBlend
                                )
                )
                : throughWallAlpha;
        boolean captureGuiMarker = fixedGui3d || throughWallAlpha > 3;

        FixedGuiMarkerGeometry visibleGeometry = renderMarkerFrame(
                poseStack,
                buffers,
                camera,
                markerPosition,
                worldFrameAlpha,
                captureGuiMarker,
                modelViewMatrix,
                projectionMatrix,
                guiWidth,
                guiHeight
        );
        FixedGuiMarkerGeometry wallGeometry = wallBlend > 0.0F && captureGuiMarker
                ? captureMarkerGeometry(
                        poseStack,
                        camera,
                        throughWallPosition,
                        modelViewMatrix,
                        projectionMatrix,
                        guiWidth,
                        guiHeight
                )
                : visibleGeometry;
        FixedGuiMarkerGeometry geometry = blendGeometry(
                visibleGeometry,
                wallGeometry,
                wallBlend
        );

        if (geometry != null && captureGuiMarker) {
            fixedGuiMarkerProjection = new FixedGuiMarkerProjection(
                    geometry.frameX(),
                    geometry.frameY(),
                    geometry.frameWidth(),
                    geometry.frameHeight(),
                    geometry.itemX(),
                    geometry.itemY(),
                    geometry.itemScale(),
                    throughWallAlpha,
                    guiItemAlpha,
                    now
            );
        }

        if (!fixedGui3d) {
            renderMarkerItem(
                    minecraft,
                    poseStack,
                    buffers,
                    camera,
                    markerPosition,
                    globalAlpha * worldVisibility,
                    gui3d
            );
        }
    }

    private static FixedGuiMarkerGeometry renderMarkerFrame(
            PoseStack poseStack,
            MultiBufferSource.BufferSource buffers,
            Camera camera,
            Vec3 markerPosition,
            int alpha,
            boolean captureGuiMarker,
            Matrix4f modelViewMatrix,
            Matrix4f projectionMatrix,
            int guiWidth,
            int guiHeight
    ) {
        poseStack.pushPose();
        poseStack.translate(markerPosition.x, markerPosition.y, markerPosition.z);
        poseStack.mulPose(camera.rotation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        Matrix4f markerMatrix = new Matrix4f(poseStack.last().pose());
        if (alpha > 3) {
            VertexConsumer markerConsumer = buffers.getBuffer(
                    RenderType.entityTranslucentEmissive(StorageHighlightStyle.MARKER_TEXTURE)
            );
            drawQuad(
                    markerMatrix,
                    markerConsumer,
                    StorageHighlightStyle.MARKER_WIDTH,
                    StorageHighlightStyle.MARKER_HEIGHT,
                    alpha
            );
        }

        FixedGuiMarkerGeometry projected = captureGuiMarker
                ? projectFixedGuiMarker(
                        markerMatrix,
                        modelViewMatrix,
                        projectionMatrix,
                        guiWidth,
                        guiHeight
                )
                : null;
        poseStack.popPose();
        return projected;
    }

    private static FixedGuiMarkerGeometry captureMarkerGeometry(
            PoseStack poseStack,
            Camera camera,
            Vec3 markerPosition,
            Matrix4f modelViewMatrix,
            Matrix4f projectionMatrix,
            int guiWidth,
            int guiHeight
    ) {
        poseStack.pushPose();
        poseStack.translate(markerPosition.x, markerPosition.y, markerPosition.z);
        poseStack.mulPose(camera.rotation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        Matrix4f markerMatrix = new Matrix4f(poseStack.last().pose());
        FixedGuiMarkerGeometry geometry = projectFixedGuiMarker(
                markerMatrix,
                modelViewMatrix,
                projectionMatrix,
                guiWidth,
                guiHeight
        );
        poseStack.popPose();
        return geometry;
    }

    private static FixedGuiMarkerGeometry blendGeometry(
            FixedGuiMarkerGeometry visible,
            FixedGuiMarkerGeometry throughWall,
            float amount
    ) {
        if (visible == null) {
            return throughWall;
        }
        if (throughWall == null) {
            return visible;
        }
        float blend = smoothStep(Math.max(0.0F, Math.min(1.0F, amount)));
        return new FixedGuiMarkerGeometry(
                lerp(visible.frameX(), throughWall.frameX(), blend),
                lerp(visible.frameY(), throughWall.frameY(), blend),
                lerp(visible.frameWidth(), throughWall.frameWidth(), blend),
                lerp(visible.frameHeight(), throughWall.frameHeight(), blend),
                lerp(visible.itemX(), throughWall.itemX(), blend),
                lerp(visible.itemY(), throughWall.itemY(), blend),
                lerp(visible.itemScale(), throughWall.itemScale(), blend)
        );
    }

    private static void renderFixedGuiFrame(
            GuiGraphics guiGraphics,
            FixedGuiMarkerProjection projection
    ) {
        PoseStack guiPose = guiGraphics.pose();
        guiPose.pushPose();
        guiPose.translate(
                projection.frameX(),
                projection.frameY(),
                StorageHighlightStyle.THROUGH_WALL_FRAME_GUI_Z
        );

        VertexConsumer frameConsumer = guiGraphics.bufferSource().getBuffer(
                RenderType.entityTranslucentEmissive(StorageHighlightStyle.MARKER_TEXTURE)
        );
        drawGuiQuad(
                guiPose.last().pose(),
                frameConsumer,
                projection.frameWidth(),
                projection.frameHeight(),
                projection.frameAlpha()
        );
        guiPose.popPose();
    }

    private static void drawGuiQuad(
            Matrix4f matrix,
            VertexConsumer consumer,
            float width,
            float height,
            int alpha
    ) {
        float halfWidth = width * 0.5F;
        float halfHeight = height * 0.5F;

        vertex(consumer, matrix, -halfWidth, halfHeight, 0.0F, 0.0F, 1.0F, alpha);
        vertex(consumer, matrix, halfWidth, halfHeight, 0.0F, 1.0F, 1.0F, alpha);
        vertex(consumer, matrix, halfWidth, -halfHeight, 0.0F, 1.0F, 0.0F, alpha);
        vertex(consumer, matrix, -halfWidth, -halfHeight, 0.0F, 0.0F, 0.0F, alpha);
    }

    private static void renderMarkerItem(
            Minecraft minecraft,
            PoseStack poseStack,
            MultiBufferSource.BufferSource buffers,
            Camera camera,
            Vec3 markerPosition,
            float alpha,
            boolean gui3d
    ) {
        int itemAlpha = Math.round(255.0F * alpha);

        MultiBufferSource alphaBuffers = renderType -> {
            RenderType effectiveRenderType = gui3d && itemAlpha < 255
                    ? Sheets.translucentItemSheet()
                    : renderType;
            return new AlphaOverrideVertexConsumer(
                    buffers.getBuffer(effectiveRenderType),
                    itemAlpha
            );
        };

        poseStack.pushPose();
        poseStack.translate(markerPosition.x, markerPosition.y, markerPosition.z);
        poseStack.mulPose(camera.rotation());
        // Flat generated items already used the correct orientation in the
        // first version, so preserve its texture-facing correction. True 3D
        // GUI models must be viewed from the opposite side or they show their
        // back and underside instead of the normal inventory angle.
        if (!gui3d) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        } else {
            // Legacy/manual world-space mode, used only when the fixed GUI
            // projection toggle is disabled.
            poseStack.mulPose(Axis.YP.rotationDegrees(StorageHighlightStyle.GUI3D_YAW_DEGREES));
            poseStack.mulPose(Axis.XP.rotationDegrees(StorageHighlightStyle.GUI3D_PITCH_DEGREES));
        }
        poseStack.translate(
                0.0F,
                StorageHighlightStyle.MARKER_ITEM_Y,
                gui3d
                        ? StorageHighlightStyle.MARKER_ITEM_Z
                        : -StorageHighlightStyle.MARKER_ITEM_Z
        );
        float itemScale = StorageHighlightStyle.MARKER_ITEM_SCALE;
        poseStack.scale(itemScale, itemScale, itemScale);

        minecraft.getItemRenderer().renderStatic(
                highlightedStack,
                ItemDisplayContext.GUI,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                alphaBuffers,
                minecraft.level,
                0
        );
        poseStack.popPose();
    }

    private static FixedGuiMarkerGeometry projectFixedGuiMarker(
            Matrix4f markerMatrix,
            Matrix4f modelViewMatrix,
            Matrix4f projectionMatrix,
            int guiWidth,
            int guiHeight
    ) {
        float itemHalfSize = StorageHighlightStyle.MARKER_ITEM_SCALE * 0.5F;
        ScreenPoint itemCenter = projectLocalPoint(
                markerMatrix,
                modelViewMatrix,
                projectionMatrix,
                0.0F,
                StorageHighlightStyle.MARKER_ITEM_Y,
                0.0F,
                guiWidth,
                guiHeight
        );
        ScreenPoint itemLeft = projectLocalPoint(
                markerMatrix,
                modelViewMatrix,
                projectionMatrix,
                -itemHalfSize,
                StorageHighlightStyle.MARKER_ITEM_Y,
                0.0F,
                guiWidth,
                guiHeight
        );
        ScreenPoint itemRight = projectLocalPoint(
                markerMatrix,
                modelViewMatrix,
                projectionMatrix,
                itemHalfSize,
                StorageHighlightStyle.MARKER_ITEM_Y,
                0.0F,
                guiWidth,
                guiHeight
        );
        ScreenPoint itemTop = projectLocalPoint(
                markerMatrix,
                modelViewMatrix,
                projectionMatrix,
                0.0F,
                StorageHighlightStyle.MARKER_ITEM_Y + itemHalfSize,
                0.0F,
                guiWidth,
                guiHeight
        );
        ScreenPoint itemBottom = projectLocalPoint(
                markerMatrix,
                modelViewMatrix,
                projectionMatrix,
                0.0F,
                StorageHighlightStyle.MARKER_ITEM_Y - itemHalfSize,
                0.0F,
                guiWidth,
                guiHeight
        );

        ScreenBillboardProjection frame = projectBillboard(
                markerMatrix,
                modelViewMatrix,
                projectionMatrix,
                StorageHighlightStyle.MARKER_WIDTH,
                StorageHighlightStyle.MARKER_HEIGHT,
                guiWidth,
                guiHeight
        );
        if (itemCenter == null
                || itemLeft == null
                || itemRight == null
                || itemTop == null
                || itemBottom == null
                || frame == null) {
            return null;
        }

        float projectedWidth = Math.abs(itemRight.x() - itemLeft.x());
        float projectedHeight = Math.abs(itemBottom.y() - itemTop.y());
        float itemScale = Math.max(1.0F, Math.min(projectedWidth, projectedHeight));
        return new FixedGuiMarkerGeometry(
                frame.centerX(),
                frame.centerY(),
                frame.width(),
                frame.height(),
                itemCenter.x(),
                itemCenter.y(),
                itemScale
        );
    }

    private static ScreenBillboardProjection projectBillboard(
            Matrix4f modelMatrix,
            Matrix4f modelViewMatrix,
            Matrix4f projectionMatrix,
            float width,
            float height,
            int guiWidth,
            int guiHeight
    ) {
        ScreenPoint center = projectLocalPoint(
                modelMatrix, modelViewMatrix, projectionMatrix,
                0.0F, 0.0F, 0.0F, guiWidth, guiHeight
        );
        ScreenPoint left = projectLocalPoint(
                modelMatrix, modelViewMatrix, projectionMatrix,
                -width * 0.5F, 0.0F, 0.0F, guiWidth, guiHeight
        );
        ScreenPoint right = projectLocalPoint(
                modelMatrix, modelViewMatrix, projectionMatrix,
                width * 0.5F, 0.0F, 0.0F, guiWidth, guiHeight
        );
        ScreenPoint top = projectLocalPoint(
                modelMatrix, modelViewMatrix, projectionMatrix,
                0.0F, height * 0.5F, 0.0F, guiWidth, guiHeight
        );
        ScreenPoint bottom = projectLocalPoint(
                modelMatrix, modelViewMatrix, projectionMatrix,
                0.0F, -height * 0.5F, 0.0F, guiWidth, guiHeight
        );
        if (center == null || left == null || right == null || top == null || bottom == null) {
            return null;
        }
        return new ScreenBillboardProjection(
                center.x(),
                center.y(),
                Math.max(1.0F, Math.abs(right.x() - left.x())),
                Math.max(1.0F, Math.abs(bottom.y() - top.y()))
        );
    }

    private static ScreenPoint projectLocalPoint(
            Matrix4f modelMatrix,
            Matrix4f modelViewMatrix,
            Matrix4f projectionMatrix,
            float x,
            float y,
            float z,
            int guiWidth,
            int guiHeight
    ) {
        Vector4f clip = new Vector4f(x, y, z, 1.0F);
        // Match the exact world-render transform chain used by the marker:
        // local/model pose -> level model-view (camera) -> projection.
        // Omitting modelViewMatrix made the GUI item follow a different
        // trajectory than its world-space frame and only line up by accident.
        modelMatrix.transform(clip);
        modelViewMatrix.transform(clip);
        projectionMatrix.transform(clip);
        if (clip.w <= 0.0001F) {
            return null;
        }

        float inverseW = 1.0F / clip.w;
        float ndcX = clip.x * inverseW;
        float ndcY = clip.y * inverseW;
        if (ndcX < -1.25F || ndcX > 1.25F || ndcY < -1.25F || ndcY > 1.25F) {
            return null;
        }

        float screenX = (ndcX * 0.5F + 0.5F) * guiWidth;
        float screenY = (0.5F - ndcY * 0.5F) * guiHeight;
        return new ScreenPoint(screenX, screenY);
    }

    private static void renderTexturedBillboard(
            PoseStack poseStack,
            MultiBufferSource.BufferSource buffers,
            Camera camera,
            ResourceLocation texture,
            Vec3 position,
            float width,
            float height,
            int alpha
    ) {
        poseStack.pushPose();
        poseStack.translate(position.x, position.y, position.z);
        poseStack.mulPose(camera.rotation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucentEmissive(texture));
        drawQuad(poseStack.last().pose(), consumer, width, height, alpha);
        poseStack.popPose();
    }

    private static void drawQuad(
            Matrix4f matrix,
            VertexConsumer consumer,
            float width,
            float height,
            int alpha
    ) {
        float halfWidth = width * 0.5F;
        float halfHeight = height * 0.5F;

        vertex(consumer, matrix, -halfWidth, -halfHeight, 0.0F, 0.0F, 1.0F, alpha);
        vertex(consumer, matrix, halfWidth, -halfHeight, 0.0F, 1.0F, 1.0F, alpha);
        vertex(consumer, matrix, halfWidth, halfHeight, 0.0F, 1.0F, 0.0F, alpha);
        vertex(consumer, matrix, -halfWidth, halfHeight, 0.0F, 0.0F, 0.0F, alpha);
    }

    private static void vertex(
            VertexConsumer consumer,
            Matrix4f matrix,
            float x,
            float y,
            float z,
            float u,
            float v,
            int alpha
    ) {
        consumer.addVertex(matrix, x, y, z)
                .setColor(255, 255, 255, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 0.0F, 1.0F);
    }

    private static void playFeedbackSounds() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        float configuredVolume = CraftAndFindClientConfig.soundVolume();
        if (configuredVolume <= 0.0F) {
            return;
        }

        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
                SoundEvents.UI_BUTTON_CLICK.value(),
                StorageHighlightStyle.CLICK_PITCH,
                StorageHighlightStyle.CLICK_VOLUME * configuredVolume
        ));

        int soundCount = Math.min(targets.size(), StorageHighlightStyle.MAX_SOUND_CONTAINERS);
        for (int index = 0; index < soundCount; index++) {
            StorageHighlightTarget target = targets.get(index);
            float volume = index == 0
                    ? StorageHighlightStyle.PRIMARY_CHIME_VOLUME
                    : StorageHighlightStyle.SECONDARY_CHIME_VOLUME;
            float pitch = StorageHighlightStyle.CHIME_BASE_PITCH
                    + StorageHighlightStyle.CHIME_PITCH_STEP * index;

            minecraft.level.playLocalSound(
                    target.centerX(),
                    target.centerY(),
                    target.centerZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.BLOCKS,
                    volume * configuredVolume,
                    pitch,
                    false
            );
        }
    }

    private static boolean isAreaLoaded(Minecraft minecraft, StorageHighlightTarget target) {
        return minecraft.level != null
                && minecraft.level.hasChunkAt(target.minPos())
                && minecraft.level.hasChunkAt(target.maxPos());
    }

    private static boolean isAreaOccluded(
            Minecraft minecraft,
            Camera camera,
            StorageHighlightTarget target
    ) {
        if (minecraft.level == null || minecraft.player == null) {
            return false;
        }

        Vec3 start = camera.getPosition();
        Vec3 end = new Vec3(target.centerX(), target.centerY(), target.centerZ());
        BlockHitResult hit = minecraft.level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                minecraft.player
        ));
        if (hit.getType() == HitResult.Type.MISS) {
            return false;
        }

        BlockPos hitPos = hit.getBlockPos();
        return hitPos.getX() < target.minPos().getX()
                || hitPos.getX() > target.maxPos().getX()
                || hitPos.getY() < target.minPos().getY()
                || hitPos.getY() > target.maxPos().getY()
                || hitPos.getZ() < target.minPos().getZ()
                || hitPos.getZ() > target.maxPos().getZ();
    }

    private static void updateThroughWallBlend(boolean occluded, long now) {
        long elapsed = Math.max(0L, now - lastVisibilityUpdateMillis);
        lastVisibilityUpdateMillis = now;
        float step = Math.min(
                1.0F,
                elapsed / (float) StorageHighlightStyle.THROUGH_WALL_TRANSITION_MILLIS
        );
        float target = occluded ? 1.0F : 0.0F;
        if (throughWallBlend < target) {
            throughWallBlend = Math.min(target, throughWallBlend + step);
        } else if (throughWallBlend > target) {
            throughWallBlend = Math.max(target, throughWallBlend - step);
        }
    }

    /** Distance from the area center to its rectangular edge along a direction. */
    private static double distanceToAreaEdge(
            double directionX,
            double directionZ,
            double halfWidth,
            double halfDepth
    ) {
        double xDistance = Math.abs(directionX) > 1.0E-6D
                ? halfWidth / Math.abs(directionX)
                : Double.POSITIVE_INFINITY;
        double zDistance = Math.abs(directionZ) > 1.0E-6D
                ? halfDepth / Math.abs(directionZ)
                : Double.POSITIVE_INFINITY;
        return Math.min(xDistance, zDistance);
    }

    private static float visibilityAlpha(long now, long visibleUntil, long fadeOutMillis) {
        float fadeIn = Math.min(1.0F, (now - startedAt) / (float) StorageHighlightStyle.FADE_IN_MILLIS);
        float fadeOut = Math.min(1.0F, (visibleUntil - now) / (float) fadeOutMillis);
        return smoothStep(Math.max(0.0F, Math.min(fadeIn, fadeOut)));
    }

    private static float markerTopBlend(float horizontalDistance) {
        float range = StorageHighlightStyle.MARKER_TOP_TRANSITION_FAR
                - StorageHighlightStyle.MARKER_TOP_TRANSITION_NEAR;
        float amount = 1.0F - (horizontalDistance - StorageHighlightStyle.MARKER_TOP_TRANSITION_NEAR) / range;
        return smoothStep(Math.max(0.0F, Math.min(1.0F, amount)));
    }

    private static float smoothStep(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    private static void clear() {
        targets = List.of();
        highlightedStack = ItemStack.EMPTY;
        fixedGuiMarkerProjection = null;
        throughWallBlend = 0.0F;
        lastVisibilityUpdateMillis = 0L;
    }

    private static long mix(long value) {
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ value >>> 31;
    }

    private static double unit(long seed) {
        return (mix(seed) >>> 11) * 0x1.0p-53;
    }

    private static float lerp(float from, float to, float amount) {
        return from + (to - from) * amount;
    }

    private static double lerp(double from, double to, double amount) {
        return from + (to - from) * amount;
    }

    private static Vec3 lerp(Vec3 from, Vec3 to, float amount) {
        return new Vec3(
                lerp(from.x, to.x, amount),
                lerp(from.y, to.y, amount),
                lerp(from.z, to.z, amount)
        );
    }

    private record ScreenPoint(float x, float y) {
    }

    private record ScreenBillboardProjection(
            float centerX,
            float centerY,
            float width,
            float height
    ) {
    }

    private record FixedGuiMarkerGeometry(
            float frameX,
            float frameY,
            float frameWidth,
            float frameHeight,
            float itemX,
            float itemY,
            float itemScale
    ) {
    }

    private record FixedGuiMarkerProjection(
            float frameX,
            float frameY,
            float frameWidth,
            float frameHeight,
            float itemX,
            float itemY,
            float itemScale,
            int frameAlpha,
            int itemAlpha,
            long updatedAtMillis
    ) {
    }


    /**
     * Keeps the item's original RGB values while multiplying every submitted
     * vertex alpha by the marker fade. This also works for custom item
     * renderers because it wraps the complete MultiBufferSource passed to
     * ItemRenderer#renderStatic.
     */
    private static final class AlphaOverrideVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final int alpha;

        private AlphaOverrideVertexConsumer(VertexConsumer delegate, int alpha) {
            this.delegate = delegate;
            this.alpha = Math.max(0, Math.min(255, alpha));
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int originalAlpha) {
            int multipliedAlpha = originalAlpha * alpha / 255;
            delegate.setColor(red, green, blue, multipliedAlpha);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            delegate.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            delegate.setNormal(x, y, z);
            return this;
        }
    }
}
