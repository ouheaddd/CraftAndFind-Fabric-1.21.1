package com.overyourhead.craftandfind.common.menu;

import com.overyourhead.craftandfind.common.network.payload.GhostRecipePayload;
import com.overyourhead.craftandfind.common.network.payload.StorageSnapshotPayload;
import com.overyourhead.craftandfind.common.recipe.NearbyServerPlaceRecipe;
import com.overyourhead.craftandfind.common.storage.NearbyStorage;
import com.overyourhead.craftandfind.common.storage.StorageItemEntry;
import com.overyourhead.craftandfind.config.CraftAndFindServerConfig;
import com.overyourhead.craftandfind.core.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.List;

public final class StorageWorkbenchMenu extends CraftingMenu {
    private static final int FIRST_INPUT_SLOT = 1;
    private static final int INPUT_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = 36;

    private final ContainerLevelAccess workbenchAccess;
    private final BlockPos workbenchPos;
    private final Level level;
    private final ServerPlayer serverPlayer;
    private NearbyStorage cachedStorage;
    private List<StorageItemEntry> lastSentSnapshot = List.of();
    private int snapshotTicker;
    private int containerScanTicker;
    private boolean hasSentSnapshot;

    public StorageWorkbenchMenu(
            int containerId,
            Inventory playerInventory,
            ContainerLevelAccess access,
            BlockPos workbenchPos
    ) {
        super(containerId, playerInventory, access);
        this.workbenchAccess = access;
        this.workbenchPos = workbenchPos.immutable();
        this.level = playerInventory.player.level();
        this.serverPlayer = playerInventory.player instanceof ServerPlayer player ? player : null;
        this.cachedStorage = serverPlayer == null
                ? NearbyStorage.empty(workbenchPos)
                : NearbyStorage.scan(level, workbenchPos, CraftAndFindServerConfig.searchRadius());
        this.snapshotTicker = CraftAndFindServerConfig.contentRefreshIntervalTicks();
        this.containerScanTicker = 0;

        if (serverPlayer != null) {
            PersistentCraftingGrid.load(serverPlayer, workbenchPos, this);
        }
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (serverPlayer != null) {
            PersistentCraftingGrid.save(serverPlayer, workbenchPos, this);
        }
    }

    @Override
    public void removed(Player player) {
        if (player instanceof ServerPlayer closingPlayer) {
            PersistentCraftingGrid.save(closingPlayer, workbenchPos, this);
        }

        // CraftingMenu#removed normally returns the grid to the player. The
        // storage workbench intentionally keeps it laid out for the next open.
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(workbenchAccess, player, ModBlocks.STORAGE_WORKBENCH);
    }

    @Override
    public void fillCraftSlotsStackedContents(StackedContents contents) {
        super.fillCraftSlotsStackedContents(contents);
        if (serverPlayer != null) {
            storage().account(contents);
        }
    }

    @Override
    public void handlePlacement(boolean placeAll, RecipeHolder<?> recipe, ServerPlayer player) {
        if (recipe.value() instanceof CraftingRecipe) {
            @SuppressWarnings("unchecked")
            RecipeHolder<CraftingRecipe> craftingRecipe = (RecipeHolder<CraftingRecipe>) recipe;
            NearbyStorage storage = refreshStorage();

            // Recipe-book selection replaces the previous layout, while simply
            // closing/reopening the workbench still preserves it. Return the old
            // grid to nearby storage first and use the player inventory as fallback.
            if (!returnGridForRecipeSelection(player, storage)) {
                super.broadcastChanges();
                sendSnapshot();
                return;
            }

            if (!canCraft(player, craftingRecipe, storage)) {
                // Flush real slot/storage changes first. The ghost packet must be
                // last because a storage refresh clears the client's old ghost.
                super.broadcastChanges();
                sendSnapshot();
                ServerPlayNetworking.send(
                        player,
                        new GhostRecipePayload(containerId, craftingRecipe.id())
                );
                return;
            }

            beginPlacingRecipe();
            try {
                new NearbyServerPlaceRecipe(this, storage)
                        .recipeClicked(player, craftingRecipe, placeAll);
            } finally {
                finishPlacingRecipe(craftingRecipe);
            }
            sendSnapshot();
            return;
        }

        super.handlePlacement(placeAll, recipe, player);
    }

    private boolean returnGridForRecipeSelection(ServerPlayer player, NearbyStorage storage) {
        Inventory inventory = player.getInventory();

        for (int index = 0; index < INPUT_SLOT_COUNT; index++) {
            Slot slot = getSlot(FIRST_INPUT_SLOT + index);
            ItemStack original = slot.getItem();
            if (original.isEmpty()) {
                continue;
            }

            ItemStack remaining = storage.insert(original);
            remaining = insertIntoPlayerInventory(inventory, remaining);

            slot.set(remaining);
            if (!remaining.isEmpty()) {
                // Nothing is destroyed if both storage and player inventory are
                // full: the unmovable remainder stays in the persistent grid and
                // the recipe switch is cancelled instead of drawing over it.
                inventory.setChanged();
                return false;
            }
        }

        inventory.setChanged();
        return true;
    }

    private static ItemStack insertIntoPlayerInventory(Inventory inventory, ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack remaining = stack.copy();
        boolean changed = false;
        for (int pass = 0; pass < 2 && !remaining.isEmpty(); pass++) {
            for (int slot = 0; slot < PLAYER_INVENTORY_SLOT_COUNT && !remaining.isEmpty(); slot++) {
                ItemStack existing = inventory.getItem(slot);
                boolean matching = !existing.isEmpty()
                        && ItemStack.isSameItemSameComponents(existing, remaining);
                if ((pass == 0 && !matching) || (pass == 1 && !existing.isEmpty())) {
                    continue;
                }

                int limit = Math.min(inventory.getMaxStackSize(), remaining.getMaxStackSize());
                int room = existing.isEmpty() ? limit : limit - existing.getCount();
                if (room <= 0) {
                    continue;
                }

                int moved = Math.min(room, remaining.getCount());
                if (existing.isEmpty()) {
                    inventory.setItem(slot, remaining.copyWithCount(moved));
                } else {
                    existing.grow(moved);
                }
                remaining.shrink(moved);
                changed = true;
            }
        }

        if (changed) {
            inventory.setChanged();
        }
        return remaining;
    }

    private boolean canCraft(
            ServerPlayer player,
            RecipeHolder<CraftingRecipe> recipe,
            NearbyStorage storage
    ) {
        StackedContents contents = new StackedContents();
        player.getInventory().fillStackedContents(contents);
        super.fillCraftSlotsStackedContents(contents);
        storage.account(contents);
        return contents.canCraft(recipe.value(), null);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (serverPlayer == null) {
            return;
        }

        containerScanTicker++;
        if (containerScanTicker >= CraftAndFindServerConfig.containerScanIntervalTicks()) {
            refreshStorage();
        }

        snapshotTicker++;
        if (snapshotTicker >= CraftAndFindServerConfig.contentRefreshIntervalTicks()) {
            snapshotTicker = 0;
            sendSnapshot();
        }
    }

    /** Performs a full configured-radius scan and restarts the container scan timer. */
    public NearbyStorage refreshStorage() {
        if (serverPlayer != null) {
            cachedStorage = NearbyStorage.scan(level, workbenchPos, CraftAndFindServerConfig.searchRadius());
            containerScanTicker = 0;
        }
        return cachedStorage;
    }

    public NearbyStorage storage() {
        return cachedStorage;
    }

    public BlockPos workbenchPos() {
        return workbenchPos;
    }

    /** Sends only real storage changes, avoiding duplicate packets and client refreshes. */
    private void sendSnapshot() {
        if (serverPlayer == null) {
            return;
        }

        List<StorageItemEntry> snapshot = cachedStorage.snapshot();
        if (hasSentSnapshot && sameSnapshot(lastSentSnapshot, snapshot)) {
            return;
        }

        ServerPlayNetworking.send(
                serverPlayer,
                new StorageSnapshotPayload(containerId, snapshot)
        );
        lastSentSnapshot = snapshot;
        hasSentSnapshot = true;
    }

    private static boolean sameSnapshot(
            List<StorageItemEntry> first,
            List<StorageItemEntry> second
    ) {
        if (first.size() != second.size()) {
            return false;
        }

        for (int index = 0; index < first.size(); index++) {
            StorageItemEntry firstEntry = first.get(index);
            StorageItemEntry secondEntry = second.get(index);
            if (firstEntry.count() != secondEntry.count()) {
                return false;
            }

            ItemStack firstStack = firstEntry.stack();
            ItemStack secondStack = secondEntry.stack();
            if (!ItemStack.isSameItemSameComponents(firstStack, secondStack)) {
                return false;
            }
        }

        return true;
    }
}
