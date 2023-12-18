package hyper.ion.util;

import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.BedItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Utilz {

    private static final Item ANCHOR = Items.RESPAWN_ANCHOR;
    private static final Item GLOWSTONE = Items.GLOWSTONE_DUST;

    public static boolean isHoldingBed() {
        return mc.player.getMainHandStack().getItem() instanceof BedItem;
    }

    public static boolean isHoldingItem(Item item) {
        return mc.player.getMainHandStack().getItem() == item;
    }

    public static FindItemResult find(Item item) {
        return InvUtils.find(item);
    }

    public static FindItemResult findInHotbar(Item item) {
        return InvUtils.findInHotbar(item);
    }

    public static FindItemResult findBed() {
        return InvUtils.find(is -> is.getItem() instanceof BedItem);
    }

    public static FindItemResult findBedInHotbar() {
        return InvUtils.findInHotbar(is -> is.getItem() instanceof BedItem);
    }

    public static FindItemResult findAnchor() {
        return find(ANCHOR);
    }

    public static FindItemResult findAnchorInHotbar() {
        return findInHotbar(ANCHOR);
    }

    public static FindItemResult findGS() {
        return find(GLOWSTONE);
    }

    public static FindItemResult findGSInHotbar() {
        return findInHotbar(GLOWSTONE);
    }



    public static BlockState getState(BlockPos pos) {
        return mc.world.getBlockState(pos);
    }

    public static Block getBlock(BlockPos pos) {
        return getState(pos).getBlock();
    }

    public static boolean isAir(Block block) {
        return block == Blocks.AIR;
    }

    public static boolean isAir(BlockPos pos) {
        return isAir(getBlock(pos));
    }


    public static ArrayList<BlockPos> getSphere(BlockPos centerPos, int radius, int height) {
        ArrayList<BlockPos> blocks = new ArrayList<>();
        for (int i = centerPos.getX() - radius; i < centerPos.getX() + radius; i++) {
            for (int j = centerPos.getY() - height; j < centerPos.getY() + height; j++) {
                for (int k = centerPos.getZ() - radius; k < centerPos.getZ() + radius; k++) {
                    BlockPos pos = new BlockPos(i, j, k);
                    if (distanceBetween(centerPos, pos) <= radius && !blocks.contains(pos)) blocks.add(pos);
                }
            }
        }
        return blocks;
    }


    public static double distanceBetween(BlockPos pos1, BlockPos pos2) {
        double d = pos1.getX() - pos2.getX();
        double e = pos1.getY() - pos2.getY();
        double f = pos1.getZ() - pos2.getZ();
        return MathHelper.sqrt((float) (d * d + e * e + f * f));
    }
}
