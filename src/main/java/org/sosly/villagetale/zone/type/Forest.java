package org.sosly.villagetale.zone.type;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IZoneShape;
import org.sosly.villagetale.data.Tree;
import org.sosly.villagetale.helper.TreeHelper;

public class Forest extends AbstractZoneType {
    public static final ResourceLocation ID = new ResourceLocation(VillageTale.MOD_ID, "forest");

    private final Set<Tree> trees = ConcurrentHashMap.newKeySet();
    private final Set<BlockPos> treeBasePositions = ConcurrentHashMap.newKeySet();
    private final Set<BlockPos> dirtyPositions = ConcurrentHashMap.newKeySet();
    private boolean initialized = false;
    private transient IZoneShape cachedShape;

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public boolean isPOI(Level level, BlockPos pos) {
        if (!initialized && cachedShape != null) {
            scanForTrees(level, cachedShape);
        }
        return treeBasePositions.contains(pos);
    }

    @Override
    public void initialize(Level level, IZoneShape shape) {
        this.cachedShape = shape;
        if (!initialized) {
            scanForTrees(level, shape);
        }
    }

    private void scanForTrees(Level level, IZoneShape shape) {
        if (shape == null || level == null) {
            return;
        }

        Set<BlockPos> processedBases = new HashSet<>();
        
        BlockPos.betweenClosedStream(shape.getStartPosition().offset(-64, -64, -64), 
                                     shape.getStartPosition().offset(64, 64, 64))
            .filter(shape::containsPosition)
            .filter(pos -> {
                BlockState state = level.getBlockState(pos);
                return state.is(BlockTags.LOGS) && isValidTreeBase(level, pos);
            })
            .forEach(pos -> {
                if (!processedBases.contains(pos)) {
                    Optional<Tree> tree = TreeHelper.findTree(level, pos);
                    tree.ifPresent(t -> {
                        if (shape.containsPosition(t.getBase())) {
                            trees.add(t);
                            treeBasePositions.add(t.getBase());
                            t.getBlocks().forEach(processedBases::add);
                        }
                    });
                }
            });
        
        initialized = true;
        VillageTale.LOGGER.debug("Forest zone initialized with {} trees", trees.size());
    }

    private boolean isValidTreeBase(Level level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        return belowState.is(BlockTags.DIRT) || 
               belowState.is(Blocks.GRASS_BLOCK) || 
               belowState.is(Blocks.PODZOL) || 
               belowState.is(Blocks.MYCELIUM);
    }

    public void markDirty(BlockPos pos) {
        dirtyPositions.add(pos);
    }

    public void revalidateDirtyPositions(Level level, IZoneShape shape) {
        if (dirtyPositions.isEmpty()) {
            return;
        }

        Set<BlockPos> toProcess = new HashSet<>(dirtyPositions);
        dirtyPositions.clear();

        Set<Tree> treesToRemove = new HashSet<>();
        Set<Tree> treesToAdd = new HashSet<>();

        for (BlockPos dirtyPos : toProcess) {
            trees.stream()
                .filter(tree -> tree.getBlocks().contains(dirtyPos))
                .forEach(tree -> {
                    Optional<Tree> revalidated = TreeHelper.findTree(level, tree.getBase());
                    if (revalidated.isEmpty() || !revalidated.get().equals(tree)) {
                        treesToRemove.add(tree);
                        revalidated.ifPresent(treesToAdd::add);
                    }
                });

            if (level.getBlockState(dirtyPos).is(BlockTags.LOGS) && isValidTreeBase(level, dirtyPos)) {
                Optional<Tree> newTree = TreeHelper.findTree(level, dirtyPos);
                newTree.ifPresent(tree -> {
                    if (shape.containsPosition(tree.getBase()) && !trees.contains(tree)) {
                        treesToAdd.add(tree);
                    }
                });
            }
        }

        treesToRemove.forEach(tree -> {
            trees.remove(tree);
            treeBasePositions.remove(tree.getBase());
        });

        treesToAdd.forEach(tree -> {
            trees.add(tree);
            treeBasePositions.add(tree.getBase());
        });
    }

    public Set<Tree> getTrees() {
        return new HashSet<>(trees);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("initialized", initialized);
        
        if (!trees.isEmpty()) {
            ListTag treesList = new ListTag();
            for (Tree tree : trees) {
                CompoundTag treeTag = new CompoundTag();
                treeTag.putLong("base", tree.getBase().asLong());
                
                ListTag blocksList = new ListTag();
                for (BlockPos block : tree.getBlocks()) {
                    CompoundTag blockTag = new CompoundTag();
                    blockTag.putLong("pos", block.asLong());
                    blocksList.add(blockTag);
                }
                treeTag.put("blocks", blocksList);
                treesList.add(treeTag);
            }
            tag.put("trees", treesList);
        }
        
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt == null) {
            return;
        }
        
        initialized = nbt.getBoolean("initialized");
        
        trees.clear();
        treeBasePositions.clear();
        
        if (nbt.contains("trees")) {
            ListTag treesList = nbt.getList("trees", 10);
            for (int i = 0; i < treesList.size(); i++) {
                CompoundTag treeTag = treesList.getCompound(i);
                BlockPos base = BlockPos.of(treeTag.getLong("base"));
                
                Set<BlockPos> blocks = new HashSet<>();
                ListTag blocksList = treeTag.getList("blocks", 10);
                for (int j = 0; j < blocksList.size(); j++) {
                    CompoundTag blockTag = blocksList.getCompound(j);
                    blocks.add(BlockPos.of(blockTag.getLong("pos")));
                }
                
                Tree tree = new Tree(blocks, base);
                trees.add(tree);
                treeBasePositions.add(base);
            }
        }
    }
}
