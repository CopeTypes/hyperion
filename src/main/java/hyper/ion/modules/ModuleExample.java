package hyper.ion.modules;

import hyper.ion.Hyperion;
import meteordevelopment.meteorclient.systems.modules.Module;

public class ModuleExample extends Module {
    public ModuleExample() {
        super(Hyperion.CATEGORY, "example", "An example module in a custom category.");
    }
}
