package com.overyourhead.craftandfind.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.overyourhead.craftandfind.mixin.client.PoseStackAccessor;

import java.util.Deque;
import java.util.List;

/** Isolates side panels from transforms left behind by container integrations. */
final class GuiPoseSnapshot {
    private final List<PoseStack.Pose> frames;

    GuiPoseSnapshot(PoseStack stack) {
        // Minecraft 1.21.1 copy() includes trustedNormals as well as both matrices.
        frames = poses(stack).stream().map(PoseStack.Pose::copy).toList();
    }

    boolean restore(PoseStack stack) {
        Deque<PoseStack.Pose> current = poses(stack);
        boolean changed = current.size() != frames.size();
        int index = 0;
        for (PoseStack.Pose frame : current) {
            if (index >= frames.size()) {
                break;
            }
            PoseStack.Pose saved = frames.get(index++);
            if (!frame.pose().equals(saved.pose()) || !frame.normal().equals(saved.normal())) {
                changed = true;
            }
        }
        // Always restore all state, including the normal-matrix trust flag.
        // Fresh copies keep this snapshot reusable for the final cleanup.
        current.clear();
        for (PoseStack.Pose frame : frames) {
            current.addLast(frame.copy());
        }
        return changed;
    }

    private static Deque<PoseStack.Pose> poses(PoseStack stack) {
        return ((PoseStackAccessor) (Object) stack).craftandfind$getPoses();
    }
}
