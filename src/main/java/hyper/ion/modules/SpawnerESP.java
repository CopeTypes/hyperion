package hyper.ion.modules;

import hyper.ion.Hyperion;
import hyper.ion.util.WorldScanner;
import hyper.ion.util.WorldUtils;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.SpawnerBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector3d;

import java.util.List;

public class SpawnerESP extends Module {
    // todo are there any more naturally occurring spawners? or is it better to include all in a list..

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("scan-range")
        .description("How far to scan around you")
        .defaultValue(100)
        .min(1)
        .build()
    );

    private final Setting<Integer> scanDelay = sgGeneral.add(new IntSetting.Builder()
        .name("scan-delay")
        .description("How many milliseconds between updates")
        .defaultValue(1000)
        .min(20)
        .build()
    );

    private final Setting<Boolean> spawnerTags = sgGeneral.add(new BoolSetting.Builder()
        .name("spawner-tags")
        .description("Render the type of mob spawned above the spawner")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> tagScale = sgGeneral.add(new DoubleSetting.Builder()
        .name("tag-scale")
        .description("The scale of the spawner tag.")
        .defaultValue(1)
        .min(0)
        .visible(spawnerTags::get)
        .build()
    );

    private final Setting<Boolean> tagsOnly = sgGeneral.add(new BoolSetting.Builder()
        .name("tags-only")
        .description("Only render spawner tags, not the block.")
        .defaultValue(false)
        .visible(spawnerTags::get)
        .build()
    );

    private final Setting<SettingColor> zombieColor = sgRender.add(new ColorSetting.Builder()
        .name("zombie-color")
        .description("The color of zombie spawners.")
        .defaultValue(new SettingColor(140, 140, 140, 255))
        .build()
    );

    private final Setting<SettingColor> skeletonColor = sgRender.add(new ColorSetting.Builder()
        .name("skeleton-color")
        .description("The color of skeleton spawners.")
        .defaultValue(new SettingColor(140, 140, 140, 255))
        .build()
    );

    private final Setting<SettingColor> spiderColor = sgRender.add(new ColorSetting.Builder()
        .name("spider-color")
        .description("The color of spider spawners.")
        .defaultValue(new SettingColor(140, 140, 140, 255))
        .build()
    );

    private final Setting<SettingColor> caveSpiderColor = sgRender.add(new ColorSetting.Builder()
        .name("cave-spider-color")
        .description("The color of cave spider spawners.")
        .defaultValue(new SettingColor(140, 140, 140, 255))
        .build()
    );

    private final Setting<SettingColor> blazeColor = sgRender.add(new ColorSetting.Builder()
        .name("blaze-color")
        .description("The color of blaze spawners.")
        .defaultValue(new SettingColor(140, 140, 140, 255))
        .build()
    );

    private final Setting<SettingColor> magmaCubeColor = sgRender.add(new ColorSetting.Builder()
        .name("magma-cube-color")
        .description("The color of zombie spawners.")
        .defaultValue(new SettingColor(140, 140, 140, 255))
        .build()
    );

    private final Setting<SettingColor> silverFishColor = sgRender.add(new ColorSetting.Builder()
        .name("silverfish-color")
        .description("The color of zombie spawners.")
        .defaultValue(new SettingColor(140, 140, 140, 255))
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    public SpawnerESP() {
        super(Hyperion.CATEGORY, "spawner-esp", "Renders locations of spawners around you");
    }

    private final WorldScanner scanner = new WorldScanner(Blocks.SPAWNER, range.get());

    @Override
    public void onActivate() {
        scanner.reset();
        scanner.start();
    }

    @Override
    public void onDeactivate() {
        scanner.stop();
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (tagsOnly.get()) return;
        List<BlockPos> renderResults = scanner.getBlocks();
        if (renderResults == null) return;
        renderResults.forEach(bp -> renderBlock(event, bp));
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (!spawnerTags.get()) return;
        List<BlockPos> renderResults = scanner.getBlocks();
        if (renderResults == null) return;
        renderResults.forEach(this::renderTag);
    }

    private void renderBlock(Render3DEvent renderEvent, BlockPos pos) {
        Block b = mc.world.getBlockState(pos).getBlock();
        if (b instanceof SpawnerBlock) {
            EntityType<?> e = WorldUtils.getSpawnerType(pos);
            if (e == null) return;
            SettingColor color;
            if (e == EntityType.ZOMBIE) color = zombieColor.get();
            else if (e == EntityType.SKELETON) color = skeletonColor.get();
            else if (e == EntityType.SPIDER) color = spiderColor.get();
            else if (e == EntityType.CAVE_SPIDER) color = caveSpiderColor.get();
            else if (e == EntityType.BLAZE) color = blazeColor.get();
            else if (e == EntityType.MAGMA_CUBE) color = magmaCubeColor.get();
            else if (e == EntityType.SILVERFISH) color = silverFishColor.get();
            else color = zombieColor.get();
            renderEvent.renderer.box(pos, color, color, shapeMode.get(), 0);
        }
    }

    private static final Color BACKGROUND = new Color(0, 0, 0, 75);
    private static final Color TEXT = new Color(255, 255, 255);

    private final Vector3d tagPos = new Vector3d();
    private void renderTag(BlockPos pos) {
        Block b = mc.world.getBlockState(pos).getBlock();
        if (b instanceof SpawnerBlock) {
            EntityType<?> e = WorldUtils.getSpawnerType(pos);
            if (e == null) return;
            String tagText = getTagText(e);
            Utils.set(tagPos, Utils.vec3d(pos));
            if (NametagUtils.to2D(tagPos, tagScale.get())) {
                TextRenderer text = TextRenderer.get();
                NametagUtils.begin(tagPos);
                text.beginBig();
                double w = text.getWidth(tagText);
                double x = -w / 2;
                double y = -text.getHeight();
                Renderer2D.COLOR.begin();
                Renderer2D.COLOR.quad(x - 1, y - 1, w + 2, text.getHeight() + 2, BACKGROUND);
                Renderer2D.COLOR.render(null);
                text.render(tagText, x, y, TEXT);
                text.end();
                NametagUtils.end();
            }
        }
    }

    private String getTagText(EntityType<?> e) {
        String text;
        if (e == EntityType.ZOMBIE) text = "Zombie Spawner";
        else if (e == EntityType.SKELETON) text = "Skeleton Spawner";
        else if (e == EntityType.SPIDER) text = "Spider Spawner";
        else if (e == EntityType.CAVE_SPIDER) text = "Cave Spider Spawner";
        else if (e == EntityType.BLAZE) text = "Blaze Spawner";
        else if (e == EntityType.MAGMA_CUBE) text = "Magma Cube Spawner";
        else if (e == EntityType.SILVERFISH) text = "Zombie Spawner";
        else text = "Spawner";
        return text;
    }
}
