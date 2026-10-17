package cc.sighs.oelib.config.neoforge;

import cc.sighs.oelib.config.ClientConfigManager;
import cc.sighs.oelib.config.OELibConfig;
import cc.sighs.oelib.config.ServerConfigManager;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

@EventBusSubscriber(modid = OELibConfig.MOD_ID)
public class ConfigHotReloadListeners {

    @SubscribeEvent
    public static void registerServerListener(AddServerReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(OELibConfig.MOD_ID, "config_hot_reload_server"), new ServerConfigManager());
    }

    @EventBusSubscriber(modid = OELibConfig.MOD_ID, value = Dist.CLIENT)
    static class ClientConfigHotReloadListeners {
        @SubscribeEvent
        public static void registerClientListener(AddClientReloadListenersEvent event) {
            event.addListener(Identifier.fromNamespaceAndPath(OELibConfig.MOD_ID, "config_hot_reload_client"), new ClientConfigManager());
        }
    }
}