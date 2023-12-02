package hyper.ion.util;


import hyper.ion.Hyperion;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

// This is definitely not optimized code, but it works even when mods conflict with Meteor's BlockIterator.
public class WorldScanner {

    private int RANGE = 256;
    private boolean scanning = false;
    private final List<BlockPos> RESULTS = new ArrayList<>();
    private final List<BlockPos> RESULTS_SWAP = new ArrayList<>();
    private final List<Block> ALLOWED_BLOCKS = new ArrayList<>();
    private boolean useSwap = false;

    public WorldScanner(ArrayList<Block> allowedBlocks) {
        ALLOWED_BLOCKS.addAll(allowedBlocks);
    }

    public WorldScanner(ArrayList<Block> allowedBlocks, int range) {
        ALLOWED_BLOCKS.addAll(allowedBlocks);
        RANGE = range;
    }

    public WorldScanner(Block singleAllowed, int range) {
        ALLOWED_BLOCKS.add(singleAllowed);
        RANGE = range;
    }

    public void setAllowed(ArrayList<Block> allowedBlocks) {
        resetAllowed();
        ALLOWED_BLOCKS.addAll(allowedBlocks);
    }

    public void addAllowed(ArrayList<Block> allowedBlocks) { ALLOWED_BLOCKS.addAll(allowedBlocks); }

    public void addAllowed(Block block) { ALLOWED_BLOCKS.add(block); }

    public void resetAllowed() { ALLOWED_BLOCKS.clear(); }

    public void reset() {
        if (scanning) return;
        RESULTS.clear();
        RESULTS_SWAP.clear();
    }

    public void start() {
        if (scanning) return;
        scanning = true;
        Hyperion.THREAD.execute(this::scanThread);
    }

    public void stop() {
        scanning = false;
    }

    private void scanThread() {
        while (scanning) {
            RESULTS_SWAP.addAll(RESULTS);
            useSwap = true;

            List<BlockPos> results = WorldUtils.scan(ALLOWED_BLOCKS, RANGE);
            RESULTS.clear();
            RESULTS.addAll(results);
            useSwap = false;
            RESULTS_SWAP.clear();
        }
    }

    public List<BlockPos> getBlocks() {
        return getResultsSafely();
    }

    private List<BlockPos> getResultsSafely() {
        if (useSwap) return new ArrayList<>(RESULTS_SWAP);
        else return new ArrayList<>(RESULTS);
    }
}
