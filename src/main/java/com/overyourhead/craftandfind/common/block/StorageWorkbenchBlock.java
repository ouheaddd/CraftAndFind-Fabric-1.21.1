package com.overyourhead.craftandfind.common.block;

import com.overyourhead.craftandfind.common.menu.PersistentCraftingGrid;
import com.overyourhead.craftandfind.common.menu.StorageWorkbenchMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class StorageWorkbenchBlock extends CraftingTableBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<StorageWorkbenchPart> PART =
            EnumProperty.create("part", StorageWorkbenchPart.class);

    private static final Component TITLE = Component.translatable("container.crafting");
    private static final int UPDATE_FLAGS = 3;
    private static final int REMOVE_FLAGS = 35;

    private static final VoxelShape LOWER_SIDE_SHAPE = Shapes.block();

    private static final VoxelShape UPPER_NORTH_SHAPE = Block.box(0.0, 0.0, 13.0, 16.0, 16.0, 16.0);
    private static final VoxelShape UPPER_EAST_SHAPE = Block.box(0.0, 0.0, 0.0, 3.0, 16.0, 16.0);
    private static final VoxelShape UPPER_SOUTH_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 3.0);
    private static final VoxelShape UPPER_WEST_SHAPE = Block.box(13.0, 0.0, 0.0, 16.0, 16.0, 16.0);

    private static final VoxelShape LOWER_MAIN_NORTH_SHAPE = Shapes.or(
            Block.box(0.0, 14.0, 0.0, 16.0, 16.0, 16.0),
            Block.box(0.0, 6.0, 0.0, 16.0, 8.0, 16.0),
            Block.box(12.0, 0.0, 1.0, 15.0, 14.0, 4.0),
            Block.box(12.0, 0.0, 12.0, 15.0, 14.0, 15.0)
    );
    private static final VoxelShape LOWER_MAIN_EAST_SHAPE = Shapes.or(
            Block.box(0.0, 14.0, 0.0, 16.0, 16.0, 16.0),
            Block.box(0.0, 6.0, 0.0, 16.0, 8.0, 16.0),
            Block.box(12.0, 0.0, 12.0, 15.0, 14.0, 15.0),
            Block.box(1.0, 0.0, 12.0, 4.0, 14.0, 15.0)
    );
    private static final VoxelShape LOWER_MAIN_SOUTH_SHAPE = Shapes.or(
            Block.box(0.0, 14.0, 0.0, 16.0, 16.0, 16.0),
            Block.box(0.0, 6.0, 0.0, 16.0, 8.0, 16.0),
            Block.box(1.0, 0.0, 12.0, 4.0, 14.0, 15.0),
            Block.box(1.0, 0.0, 1.0, 4.0, 14.0, 4.0)
    );
    private static final VoxelShape LOWER_MAIN_WEST_SHAPE = Shapes.or(
            Block.box(0.0, 14.0, 0.0, 16.0, 16.0, 16.0),
            Block.box(0.0, 6.0, 0.0, 16.0, 8.0, 16.0),
            Block.box(1.0, 0.0, 1.0, 4.0, 14.0, 4.0),
            Block.box(12.0, 0.0, 1.0, 15.0, 14.0, 4.0)
    );

    public StorageWorkbenchBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, StorageWorkbenchPart.LOWER_MAIN));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos mainPos = context.getClickedPos();
        BlockPos sidePos = sidePos(mainPos, facing);
        BlockPos upperMainPos = mainPos.above();
        BlockPos upperSidePos = sidePos.above();
        Level level = context.getLevel();

        if (!canOccupy(level, mainPos)
                || !canOccupy(level, sidePos)
                || !canOccupy(level, upperMainPos)
                || !canOccupy(level, upperSidePos)) {
            return null;
        }

        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(PART, StorageWorkbenchPart.LOWER_MAIN);
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            LivingEntity placer,
            ItemStack stack
    ) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) {
            return;
        }

        PersistentCraftingGrid.beginNewWorkbench((ServerLevel) level, pos);

        Direction facing = state.getValue(FACING);
        BlockPos sidePos = sidePos(pos, facing);

        level.setBlock(
                sidePos,
                defaultBlockState().setValue(FACING, facing).setValue(PART, StorageWorkbenchPart.LOWER_SIDE),
                UPDATE_FLAGS
        );
        level.setBlock(
                pos.above(),
                defaultBlockState().setValue(FACING, facing).setValue(PART, StorageWorkbenchPart.UPPER_MAIN),
                UPDATE_FLAGS
        );
        level.setBlock(
                sidePos.above(),
                defaultBlockState().setValue(FACING, facing).setValue(PART, StorageWorkbenchPart.UPPER_SIDE),
                UPDATE_FLAGS
        );
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockState result = super.playerWillDestroy(level, pos, state, player);

        if (!level.isClientSide && state.getValue(PART) != StorageWorkbenchPart.LOWER_MAIN) {
            BlockPos mainPos = mainPos(pos, state);
            BlockState mainState = level.getBlockState(mainPos);
            if (isExpectedPart(mainState, state.getValue(FACING), StorageWorkbenchPart.LOWER_MAIN)) {
                level.destroyBlock(mainPos, !player.isCreative(), player);
            }
        }

        return result;
    }

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston
    ) {
        if (state.getBlock() != newState.getBlock() && !level.isClientSide) {
            Direction facing = state.getValue(FACING);
            StorageWorkbenchPart part = state.getValue(PART);
            BlockPos mainPos = mainPos(pos, state);

            if (part == StorageWorkbenchPart.LOWER_MAIN) {
                PersistentCraftingGrid.invalidate((ServerLevel) level, mainPos);
                removeOtherParts(level, mainPos, facing);
            } else {
                BlockState mainState = level.getBlockState(mainPos);
                if (isExpectedPart(mainState, facing, StorageWorkbenchPart.LOWER_MAIN)) {
                    level.destroyBlock(mainPos, true);
                }
            }
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        BlockPos mainPos = mainPos(pos, state);
        BlockState mainState = level.getBlockState(mainPos);
        if (!isExpectedPart(mainState, state.getValue(FACING), StorageWorkbenchPart.LOWER_MAIN)) {
            return null;
        }

        return new SimpleMenuProvider(
                (containerId, inventory, player) -> new StorageWorkbenchMenu(
                        containerId,
                        inventory,
                        ContainerLevelAccess.create(level, mainPos),
                        mainPos
                ),
                TITLE
        );
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        StorageWorkbenchPart part = state.getValue(PART);
        Direction facing = state.getValue(FACING);

        return switch (part) {
            case LOWER_MAIN -> lowerMainShape(facing);
            case LOWER_SIDE -> LOWER_SIDE_SHAPE;
            case UPPER_MAIN, UPPER_SIDE -> upperShape(facing);
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static boolean canOccupy(Level level, BlockPos pos) {
        return !level.isOutsideBuildHeight(pos)
                && level.getWorldBorder().isWithinBounds(pos)
                && level.getBlockState(pos).canBeReplaced();
    }

    private static BlockPos sidePos(BlockPos mainPos, Direction facing) {
        return mainPos.relative(facing.getCounterClockWise());
    }

    private static BlockPos mainPos(BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        return switch (state.getValue(PART)) {
            case LOWER_MAIN -> pos;
            case LOWER_SIDE -> pos.relative(facing.getClockWise());
            case UPPER_MAIN -> pos.below();
            case UPPER_SIDE -> pos.below().relative(facing.getClockWise());
        };
    }

    private boolean isExpectedPart(
            BlockState state,
            Direction facing,
            StorageWorkbenchPart expectedPart
    ) {
        return state.is(this)
                && state.getValue(FACING) == facing
                && state.getValue(PART) == expectedPart;
    }

    private void removeOtherParts(Level level, BlockPos mainPos, Direction facing) {
        removePart(level, sidePos(mainPos, facing), facing, StorageWorkbenchPart.LOWER_SIDE);
        removePart(level, mainPos.above(), facing, StorageWorkbenchPart.UPPER_MAIN);
        removePart(level, sidePos(mainPos, facing).above(), facing, StorageWorkbenchPart.UPPER_SIDE);
    }

    private void removePart(
            Level level,
            BlockPos pos,
            Direction facing,
            StorageWorkbenchPart expectedPart
    ) {
        BlockState state = level.getBlockState(pos);
        if (isExpectedPart(state, facing, expectedPart)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), REMOVE_FLAGS);
        }
    }

    private static VoxelShape upperShape(Direction facing) {
        return switch (facing) {
            case NORTH -> UPPER_NORTH_SHAPE;
            case EAST -> UPPER_EAST_SHAPE;
            case SOUTH -> UPPER_SOUTH_SHAPE;
            case WEST -> UPPER_WEST_SHAPE;
            default -> UPPER_NORTH_SHAPE;
        };
    }

    private static VoxelShape lowerMainShape(Direction facing) {
        return switch (facing) {
            case NORTH -> LOWER_MAIN_NORTH_SHAPE;
            case EAST -> LOWER_MAIN_EAST_SHAPE;
            case SOUTH -> LOWER_MAIN_SOUTH_SHAPE;
            case WEST -> LOWER_MAIN_WEST_SHAPE;
            default -> LOWER_MAIN_NORTH_SHAPE;
        };
    }
}
