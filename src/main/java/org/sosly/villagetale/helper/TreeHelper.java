package org.sosly.villagetale.helper;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.config.CommonConfig;
import org.sosly.villagetale.data.Tree;
import org.sosly.villagetale.entity.Villager;

public class TreeHelper {
    private static final int MAX_TREE_BLOCKS = 256;

    public static Optional<Tree> findTree(Level level, BlockPos startPos) {
        BlockState startState = level.getBlockState(startPos);
        if (!isLog(startState)) {
            return Optional.empty();
        }

        Tree tree = getConnectedTreeBlocks(level, startPos);

        boolean hasLeaves = tree.getBlocks().stream()
                .map(level::getBlockState)
                .anyMatch(TreeHelper::isLeaves);

        if (!hasLeaves) {
            return Optional.empty();
        }

        return Optional.of(tree);
    }


    private static Tree getConnectedTreeBlocks(Level level, BlockPos startPos) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> toCheck = new LinkedList<>();
        BlockPos lowestLog = startPos;

        toCheck.add(startPos);
        visited.add(startPos);

        while (!toCheck.isEmpty() && visited.size() < MAX_TREE_BLOCKS) {
            BlockPos current = toCheck.poll();
            BlockState currentState = level.getBlockState(current);

            if (isLog(currentState) && current.getY() < lowestLog.getY()) {
                lowestLog = current;
            }

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }

                        BlockPos adjacent = current.offset(dx, dy, dz);
                        if (visited.contains(adjacent)) {
                            continue;
                        }

                        BlockState adjacentState = level.getBlockState(adjacent);

                        if (isLog(currentState) && isLog(adjacentState)) {
                            visited.add(adjacent);
                            toCheck.add(adjacent);
                            if (adjacent.getY() < lowestLog.getY()) {
                                lowestLog = adjacent;
                            }
                        } else if (isLog(currentState) && isLeaves(adjacentState)) {
                            visited.add(adjacent);
                        }
                    }
                }
            }
        }

        return new Tree(visited, lowestLog);
    }

    private static boolean isLog(BlockState state) {
        return state.is(BlockTags.LOGS);
    }

    private static boolean isLeaves(BlockState state) {
        return state.is(BlockTags.LEAVES);
    }

    public static Optional<Tree> findNearestTree(ServerLevel level, Villager villager) {
        BlockPos villagerPos = villager.blockPosition();
        int scanRadius = (int) CommonConfig.scanRadius;

        Optional<UUID> villageUUID = villager.getVillage();
        if (!villageUUID.isPresent()) {
            return Optional.empty();
        }

        IVillageZone zone = VillagesHelper.getWorkplaceZone(level, villager);
        if (zone == null) {
            return Optional.empty();
        }

        Optional<Tree> nearestTree = Optional.empty();
        double nearestDistance = Double.MAX_VALUE;

        for (int x = -scanRadius; x <= scanRadius; x++) {
            for (int z = -scanRadius; z <= scanRadius; z++) {
                for (int y = -scanRadius; y <= scanRadius; y++) {
                    BlockPos checkPos = villagerPos.offset(x, y, z);

                    if (!isLog(level.getBlockState(checkPos))) {
                        continue;
                    }

                    Optional<Tree> tree = findTree(level, checkPos);
                    if (tree.isEmpty()) {
                        continue;
                    }

                    BlockPos basePos = tree.get().getBase();

                    if (!zone.containsPosition(basePos)) {
                        continue;
                    }

                    double distance = basePos.distSqr(villagerPos);
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearestTree = tree;
                    }
                }
            }
        }

        return nearestTree;
    }
}

