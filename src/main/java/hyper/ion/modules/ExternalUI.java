package hyper.ion.modules;

import hyper.ion.Hyperion;
import hyper.ion.util.Sorter;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ExternalUI extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    // todo settings for text color, background color, more?

    private final Setting<Boolean> showCoords = sgGeneral.add(new BoolSetting.Builder()
        .name("coords")
        .description("Display your current coordinates.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showBiome = sgGeneral.add(new BoolSetting.Builder()
        .name("biome")
        .description("Display the biome you're currently in.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showModules = sgGeneral.add(new BoolSetting.Builder()
        .name("modules")
        .description("Display currently active modules.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showMedia = sgGeneral.add(new BoolSetting.Builder()
        .name("media")
        .description("Display currently playing media (Spotify etc.)")
        .defaultValue(true)
        .build()
    );

    private final Setting<Sorter.SortMode> moduleSortMode = sgGeneral.add(new EnumSetting.Builder<Sorter.SortMode>()
        .name("module-sort-mode")
        .defaultValue(Sorter.SortMode.Shortest)
        .visible(showModules::get)
        .build());

    private final Setting<Integer> port = sgGeneral.add(new IntSetting.Builder()
        .name("port")
        .description("The port used to communicate with the external hud (leave alone if unsure)")
        .defaultValue(8888)
        .min(8888)
        .build()
    );

    public ExternalUI() {
        super(Hyperion.CATEGORY, "external-ui", "Displays a customizable external UI (useful for streaming)");
    }

    //text:lines|like|this
    //textColor:r:g:b
    //backgroundColor:r:g:b
    //title:text
    //quit

    private final File EXTERNAL_HUD = new File(Hyperion.ROOT, "external-hud.jar");
    private Process ExternalHudProcess;

    private boolean connected;

    @Override
    public void onActivate() {
        startHud();
    }

    @Override
    public void onDeactivate() {
        if (ExternalHudProcess != null && ExternalHudProcess.isAlive()) ExternalHudProcess.destroy();
        ExternalHudProcess = null;
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (!connected) return;
        if (!ExternalHudProcess.isAlive()) {
            warning("External hud was shut down, disabling.");
            toggle();
        }
    }

    private void startHud() {
        if (!EXTERNAL_HUD.exists()) {
            //todo make docs lol
            error("external-hud.jar not found. Read the docs (here) to setup.");
            toggle();
            return;
        }
        Hyperion.THREAD.execute(this::hudThread);
    }

    private void hudThread() {
        String[] command = {"cmd", "/c" , "java -jar " +  "\"" + EXTERNAL_HUD.getAbsolutePath() + "\"", port.get().toString()};
        //info(command);
        try {
            info("Starting external hud...");
            ExternalHudProcess = new ProcessBuilder(command).start();
        } catch (IOException e) {
            e.printStackTrace();
            error("Unable to start external hud.");
            toggle();
        }

        try {Thread.sleep(1000);} catch (InterruptedException ignored) {}

        info("Connecting to external hud...");
        try (Socket socket = new Socket("localhost", port.get())) {
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            Thread.sleep(1000);
            info("Connected!");
            connected = true;
            while (this.isActive()) {
                Thread.sleep(250);
                writer.println("title: " + getTitleText());
                writer.println("text:" + getHudText());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private String getHudText() {
        ArrayList<String> hudText = new ArrayList<>();
        if (showBiome.get()) hudText.add("Biome: " + getBiome());
        if (showMedia.get() & Hyperion.MEDIA_SCANNER.hasMedia()) hudText.add(Hyperion.MEDIA_SCANNER.getMedia());
        if (showCoords.get()) hudText.addAll(getCoords());
        if (showModules.get()) hudText.addAll(getModules());
        return String.join("|", hudText);
    }

    private String getTitleText() {
        //todo make this a setting probably a starscript thing
        return mc.player.getEntityName() + " @ "
            + Math.round(PlayerUtils.getTotalHealth()) + "HP | "
            + Utils.getWorldName() + " | "
            + PlayerUtils.getPing() + "ms";
    }

    private String getBiome() {
        BlockPos.Mutable blockPos = new BlockPos.Mutable();
        blockPos.set(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        Identifier id = mc.world.getRegistryManager().get(RegistryKeys.BIOME).getId(mc.world.getBiome(blockPos).value());
        if (id == null) return "Unknown";
        return Arrays.stream(id.getPath().split("_")).map(StringUtils::capitalize).collect(Collectors.joining(" "));
    }

    private ArrayList<String> getModules() {
        ArrayList<String> moduleList = new ArrayList<>();
        ArrayList<String> ml = new ArrayList<>();
        Modules.get().getList().forEach(module -> {
            if (module.isActive()) ml.add(module.title);
        });
        moduleList.add("");
        moduleList.add("[Modules]");
        moduleList.addAll(Sorter.sort(ml, moduleSortMode.get()));
        return moduleList;
    }

    private ArrayList<String> getCoords() {
        Freecam freecam = Modules.get().get(Freecam.class);
        double x, y, z;
        x = freecam.isActive() ? mc.gameRenderer.getCamera().getPos().x : mc.player.getX();
        y = freecam.isActive() ? mc.gameRenderer.getCamera().getPos().y - mc.player.getEyeHeight(mc.player.getPose()) : mc.player.getY();
        z = freecam.isActive() ? mc.gameRenderer.getCamera().getPos().z : mc.player.getZ();
        String right1 = String.format("%.1f %.1f %.1f", x, y, z);
        String right2 = null;
        String dimension = null;
        switch (PlayerUtils.getDimension()) {
            case Overworld -> {
                right2 = String.format("%.1f %.1f %.1f", x / 8.0, y, z / 8.0);
                dimension = "Nether Coords: ";
            }
            case Nether -> {
                right2 = String.format("%.1f %.1f %.1f", x * 8.0, y, z * 8.0);
                dimension = "Overworld Coords: ";
            }
        }
        ArrayList<String> s = new ArrayList<>();
        s.add("");
        s.add("Coords: " + right1);
        if (right2 != null) s.add(dimension + right2);
        return s;
    }

}
