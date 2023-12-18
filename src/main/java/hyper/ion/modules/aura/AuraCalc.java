package hyper.ion.modules.aura;

import hyper.ion.util.Utilz;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.DamageUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Class for handling all logic for bed/anchor aura
 */
public class AuraCalc {

    private AuraMode MODE;
    private AuraPlace CURRENT;

    private int RADIUS, HEIGHT;

    private int RANGE;

    /**
     * Creates a new AuraCalc for use in a bed/anchor aura module
     * @param mode The AuraMode (BED or ANCHOR)
     * @param radius The maximum radius to check around the target for placements
     * @param height The maximum height to check around the target for placements
     * @param range The maximum range (from the user) for placements/explosions
     */
    public AuraCalc(AuraMode mode, int radius, int height, int range) {
        MODE = mode;
        RADIUS = radius;
        HEIGHT = height;
        RANGE = range;
    }

    /**
     * Calculates a placement for the given target
     * @param target Current player target
     * @param minDmg The min damage (to the target) required for a placement
     * @param maxDmg The max damage (to the user) allowed for a placement
     */
    public AuraPlace calculate(PlayerEntity target, double minDmg, double maxDmg) {
        if (CURRENT != null && CURRENT.isValid(target, minDmg, maxDmg)) return CURRENT;
        CURRENT = getBestPos(getPlaceableLocations(target), target, minDmg, maxDmg);
        return CURRENT;
    }

    /**
     * Gets a list of valid placement positions based on the AuraMode
     * @param target The player target
     */
    private ArrayList<BlockPos> getPlaceableLocations(PlayerEntity target) {
        ArrayList<BlockPos> good = new ArrayList<>();
        ArrayList<BlockPos> toCheck = Utilz.getSphere(target.getBlockPos(), RADIUS, HEIGHT);
        toCheck.removeIf(p -> !Utilz.isAir(p)); // remove invalid locations
        if (MODE == AuraMode.BED) toCheck.removeIf(p -> !isValidBedPlace(p)); // remove invalid locations (specific to bed placements) if necessary
        BlockPos playerPos = mc.player.getBlockPos();
        toCheck.removeIf(p -> Utilz.distanceBetween(playerPos, p) > RANGE); // remove out of range locations
        return good;
    }

    /**
     * Checks (in all directions) if a bed can be placed at a given position
     * @param pos The position to check
     */
    private boolean isValidBedPlace(BlockPos pos) {
        Block north = Utilz.getBlock(pos.north());
        Block south = Utilz.getBlock(pos.south());
        Block east = Utilz.getBlock(pos.east());
        Block west = Utilz.getBlock(pos.west());
        return !Utilz.isAir(north) && !Utilz.isAir(south) && !Utilz.isAir(east) && !Utilz.isAir(west);
    }

    /**
     * Gets the best placement from a list of valid placement positions
     * @param filtered An ArrayList of BlockPos pre-checked for validity
     * @param target The player target
     * @param minDmg The min damage (to the target) required for a placement
     * @param maxDmg The max damage (to the user) allowed for a placement
     */
    private AuraPlace getBestPos(ArrayList<BlockPos> filtered, PlayerEntity target, double minDmg, double maxDmg) {
        AuraPlace bestPlace = null;
        double bestDmg = 0.0D;
        int iter = 0;
        for (BlockPos pos : filtered) {
            AuraPlace currentPlace = null;
            if (MODE == AuraMode.BED) currentPlace = new AuraPlace(pos, target, AuraMode.BED);
            else currentPlace = new AuraPlace(pos, target, AuraMode.ANCHOR);
            if (currentPlace.dmg >= minDmg && currentPlace.selfDmg <= maxDmg) {
                if (currentPlace.dmg > bestDmg) bestPlace = currentPlace;
            }
            iter++;
            if (iter >= 5 && bestPlace != null) break;
        }
        return bestPlace;
    }

    /**
     * Class for handling bed/anchor aura placement stuff
     */
    public class AuraPlace {
        private BlockPos pos;
        private AuraMode mode;
        private Vec3d vecPos;
        private double dmg, selfDmg, yaw, pitch;

        public AuraPlace(BlockPos pos, PlayerEntity target, AuraMode mode) {
            this.pos = pos;
            this.mode = mode;
            vecPos = Utils.vec3d(pos);
            if (mode == AuraMode.BED) {
                dmg = DamageUtils.bedDamage(target, vecPos);
                selfDmg = DamageUtils.bedDamage(mc.player, vecPos);
            }
            else {
                dmg = DamageUtils.anchorDamage(target, vecPos);
                selfDmg = DamageUtils.anchorDamage(target, vecPos);
            }
            yaw = Rotations.getYaw(pos);
            pitch = Rotations.getPitch(pos);
        }

        public double getDmg() { return dmg; }

        public double getSelfDmg() { return selfDmg; }

        /**
         * Checks if the current pos is still valid to use
         * @param target The player target
         * @param minDmg The minimum damage the placement must do
         */
        public boolean isValid(PlayerEntity target, double minDmg, double maxDmg) {
            if (mode == AuraMode.BED) return DamageUtils.bedDamage(target, vecPos) >= minDmg && DamageUtils.bedDamage(mc.player, vecPos) < maxDmg;
            else return DamageUtils.anchorDamage(target, vecPos) >= minDmg && DamageUtils.anchorDamage(mc.player, vecPos) < maxDmg;
        }

        /**
         * Handles placing the bed/anchor at the current pos
         * @param rotate Rotate on placement
         */
        public boolean place(boolean rotate) {
            if (mode == AuraMode.BED) return placeBed(rotate);
            else return placeAnchor(rotate, false); // todo make preCharge a setting somewhere
        }

        /**
         * Handles placing a bed at the current pos
         * @param rotate Rotate when placing the bed
         */
        private boolean placeBed(boolean rotate) {
            FindItemResult bed = Utilz.findBedInHotbar();
            if (!bed.found()) return false;
            if (!Utilz.isHoldingBed()) InvUtils.swap(bed.slot(), false);
            if (rotate) {
                AtomicBoolean placed = new AtomicBoolean(false);
                Rotations.rotate(yaw, pitch, () -> placed.set(placeItem(pos, bed, true)));
                return placed.get();
            }
            else return placeItem(pos, bed, false);
        }

        /**
         * Handles placing a respawn anchor at the current pos
         * @param rotate Rotate when placing the anchor
         * @param preCharge Charge the anchor in the same tick
         */
        private boolean placeAnchor(boolean rotate, boolean preCharge) {
            FindItemResult anchor = Utilz.findAnchorInHotbar();
            if (!anchor.found()) return false;
            if (!Utilz.isHoldingItem(Items.RESPAWN_ANCHOR)) InvUtils.swap(anchor.slot(), false);
            if (rotate) {
                AtomicBoolean placed = new AtomicBoolean(false);
                Rotations.rotate(yaw, pitch, () -> {
                    placed.set(placeItem(pos, anchor, true));
                    if (preCharge) chargeAnchor(false);
                });
                return placed.get();
            }
            else {
                boolean placed = placeItem(pos, anchor, false);
                if (preCharge) chargeAnchor(false);
                return placed;
            }
        }

        private boolean placeItem(BlockPos pos, FindItemResult item, boolean rotate) { // generic placing method
            return BlockUtils.place(pos, item, rotate, 50, true);
        }

        /**
         * Handles exploding the bed/anchor at the current pos
         * @param rotate Rotate on explosion
         */
        public boolean explode(boolean rotate) {
            if (mode == AuraMode.BED) return explodeBed(rotate);
            return explodeAnchor(rotate);
        }

        private boolean explodeBed(boolean rotate) { // handle exploding beds
            if (!(Utilz.getBlock(pos) instanceof BedBlock)) return false;
            boolean sneakPrev = mc.player.isSneaking();
            if (sneakPrev) mc.player.setSneaking(false);
            if (rotate) {
                Rotations.rotate(yaw, pitch, () -> mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, makeHitResult()));
            } else mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, makeHitResult());
            mc.player.setSneaking(sneakPrev);
            return true;
        }

        private boolean explodeAnchor(boolean rotate) { // handle exploding anchors
            if (Utilz.getBlock(pos) != Blocks.RESPAWN_ANCHOR) return false;
            if (!isAnchorCharged(pos)) if (!chargeAnchor(rotate)) return false;
            FindItemResult anchor = Utilz.findAnchorInHotbar();
            if (!anchor.found()) return false;
            if (!Utilz.isHoldingItem(Items.RESPAWN_ANCHOR)) InvUtils.swap(anchor.slot(), false);
            if (rotate) {
                Rotations.rotate(yaw, pitch, () -> mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, makeHitResult()));
            } else mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, makeHitResult());
            return true;
        }

        private boolean chargeAnchor(boolean rotate) { // handle charging anchors
            FindItemResult glowstone = Utilz.findGSInHotbar();
            if (!glowstone.found()) return false;
            if (!Utilz.isHoldingItem(Items.GLOWSTONE_DUST)) InvUtils.swap(glowstone.slot(), false);
            if (rotate) {
                Rotations.rotate(yaw, pitch, () -> mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, makeHitResult()));
            } else mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, makeHitResult());
            return true;
        }

        private boolean isAnchorCharged(BlockPos pos) { // check if an anchor is ready to be exploded
            BlockState state = mc.world.getBlockState(pos);
            if (state.getBlock() instanceof RespawnAnchorBlock) {
                int charge = state.get(RespawnAnchorBlock.CHARGES);
                return charge > 0;
            }
            return false;
        }

        private BlockHitResult makeHitResult() { // generic hit result generation for exploding anchors/beds
            return new BlockHitResult(vecPos, Direction.UP, pos, false);
        }

    }

    public enum AuraMode { BED, ANCHOR }
}
