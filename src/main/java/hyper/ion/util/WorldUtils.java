package hyper.ion.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class WorldUtils {

    public static List<BlockPos> scan(List<Block> allowed, int range) {
        List<BlockPos> results = new ArrayList<>();
        BlockPos playerPos = mc.player.getBlockPos();

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos targetPos = playerPos.add(x, y, z);
                    ChunkPos chunkPos = new ChunkPos(targetPos.getX() >> 4, targetPos.getZ() >> 4);
                    if (mc.world.getChunkManager().isChunkLoaded(chunkPos.x, chunkPos.z)) {
                        Block currentBlock = mc.world.getBlockState(targetPos).getBlock();
                        if (allowed.contains(currentBlock)) results.add(targetPos.toImmutable());
                    }
                }
            }
        }

        return results;
    }

    public static List<BlockPos> scanAround(List<Block> allowed, BlockPos start, int range) {
        List<BlockPos> blockPosList = new ArrayList<>();

        int startX = start.getX() - range;
        int startZ = start.getZ() - range;
        int endX = start.getX() + range;
        int endZ = start.getZ() + range;

        for (int x = startX; x <= endX; x++) {
            for (int z = startZ; z <= endZ; z++) {
                BlockPos blockPos = new BlockPos(x, start.getY(), z);
                if (allowed.contains(mc.world.getBlockState(blockPos).getBlock())) blockPosList.add(blockPos);
            }
        }

        return blockPosList;
    }

    private static BlockState getState(BlockPos pos) {
        return mc.world.getBlockState(pos);
    }

    public static Block getBlock(BlockPos pos) {
        return getState(pos).getBlock();
    }

    public static boolean isWater(BlockPos pos) {
        return getBlock(pos) == Blocks.WATER;
    }

    public static boolean isInRenderDistance(BlockPos pos) {
        int chunkX = (pos.getX() / 16);
        int chunkZ = (pos.getZ() / 16);
        return (mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ));
    }

    public static int distance(BlockPos first, BlockPos second) {
        return Math.abs(first.getX() - second.getX()) + Math.abs(first.getY() - second.getY()) + Math.abs(first.getZ() - second.getZ());
    }

    public static boolean isSpawnerChest(BlockPos pos, int checkDist) {
        if (mc.world.getBlockState(pos).getBlock() instanceof ChestBlock cb) {
            List<BlockPos> nearbySpawner = scanAround(List.of(Blocks.SPAWNER), pos, 5);
            if (nearbySpawner.isEmpty()) return false;
            for (BlockPos spawner : nearbySpawner) {
                double dist = pos.getSquaredDistance(spawner.getX(), spawner.getY(), spawner.getZ());
                if (dist <= checkDist) return true;
            }
            return false;
        }
        return false;
    }

    public static EntityType<?> getSpawnerType(BlockPos pos) {
        BlockEntity blockEntity = mc.world.getBlockEntity(pos);
        if (blockEntity instanceof MobSpawnerBlockEntity be) {
            NbtCompound data = new NbtCompound();
            be.getLogic().writeNbt(data);
            if (data.contains("SpawnData")) {
                NbtCompound spawnTag = data.getCompound("SpawnData");
                if (spawnTag.contains("entity")) {
                    NbtCompound entity = spawnTag.getCompound("entity");
                    String id = entity.getString("id");
                    Optional<EntityType<?>> type = EntityType.get(id);
                    return type.orElse(null);
                }
            }
        }
        return null;
    }

}
