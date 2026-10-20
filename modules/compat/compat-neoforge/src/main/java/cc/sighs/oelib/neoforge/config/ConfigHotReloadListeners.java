package cc.sighs.oelib.neoforge.config;

import cc.sighs.oelib.config.ClientConfigManager;
import cc.sighs.oelib.config.OELibConfig;
import cc.sighs.oelib.config.ServerConfigManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

@Deprecated(since = "0.3.0")
public class ConfigHotReloadListeners extends cc.sighs.oelib.config.neoforge.ConfigHotReloadListeners {
    @Deprecated(since = "0.3.0")
    public ConfigHotReloadListeners() {}
}
