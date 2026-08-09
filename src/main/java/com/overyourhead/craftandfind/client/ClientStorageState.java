package com.overyourhead.craftandfind.client;

import com.overyourhead.craftandfind.common.storage.StorageItemEntry;

import java.util.List;

public final class ClientStorageState {
    private static int containerId = -1;
    private static List<StorageItemEntry> entries = List.of();

    private ClientStorageState() {
    }

    public static void update(int newContainerId, List<StorageItemEntry> newEntries) {
        containerId = newContainerId;
        entries = List.copyOf(newEntries);
    }

    public static boolean isActive(int checkedContainerId) {
        return containerId == checkedContainerId;
    }

    public static List<StorageItemEntry> entries(int checkedContainerId) {
        return isActive(checkedContainerId) ? entries : List.of();
    }

    public static void clear(int checkedContainerId) {
        if (containerId == checkedContainerId) {
            containerId = -1;
            entries = List.of();
        }
    }
}
