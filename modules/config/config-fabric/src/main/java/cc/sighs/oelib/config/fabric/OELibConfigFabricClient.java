package cc.sighs.oelib.config.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import cc.sighs.oelib.config.OELibConfig;
import net.minecraft.server.packs.PackType;

public class OELibConfigFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(Identifier.fromNamespaceAndPath(OELibConfig.MOD_ID, "config_hot_reload_client"), new ClientConfigHotReloadListeners());
    }
}
