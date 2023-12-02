package hyper.ion;

import hyper.ion.commands.CommandExample;
import hyper.ion.hud.HudExample;
import hyper.ion.modules.ExternalUI;
import hyper.ion.modules.ModuleExample;
import com.mojang.logging.LogUtils;
import hyper.ion.modules.SpawnerESP;
import hyper.ion.util.MediaScanner;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.Spam;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Hyperion extends MeteorAddon {
    private static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Example");
    public static final HudGroup HUD_GROUP = new HudGroup("Example");

    public static final File ROOT = new File(FabricLoader.getInstance().getGameDir().toString(), "hyperion");
    public static final ExecutorService THREAD = Executors.newCachedThreadPool();
    public static final MediaScanner MEDIA_SCANNER = new MediaScanner();

    @Override
    public void onInitialize() {
        LOG.info("Initializing Hyperion");
        if (!ROOT.exists()) ROOT.mkdirs();
        addModules();
        addCommands();
        addHud();
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "hyper.ion";
    }

    public static void log(String msg) { LOG.info(msg); }
    public static void warn(String msg) { LOG.warn(msg); }
    public static void error(String msg) { LOG.error(msg); }

    private void addModules() {
        Modules.get().add(new ExternalUI());
        Modules.get().add(new SpawnerESP());
    }

    private void addCommands() {
        //Commands.add(new CommandExample());
    }

    private void addHud() {
        //Hud.get().register(HudExample.INFO);
    }
}
