package cc.sighs.oelib.config.fabric;

import cc.sighs.oelib.config.OELibConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import cc.sighs.oelib.config.OELibConfig;
import net.minecraft.server.packs.PackType;

public class OELibConfigFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        OELibConfig.init();
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(Identifier.fromNamespaceAndPath(OELibConfig.MOD_ID, "config_hot_reload"), new ConfigHotReloadListeners());
    }
}
