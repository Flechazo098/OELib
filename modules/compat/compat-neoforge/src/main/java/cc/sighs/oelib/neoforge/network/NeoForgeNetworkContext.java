package cc.sighs.oelib.neoforge.network;

import cc.sighs.oelib.network.api.INetworkContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@Deprecated(since = "0.3.0")
public class NeoForgeNetworkContext extends cc.sighs.oelib.network.neoforge.NeoForgeNetworkContext {
    @Deprecated(since = "0.3.0")
    public NeoForgeNetworkContext(IPayloadContext context) { super(context); }
}
