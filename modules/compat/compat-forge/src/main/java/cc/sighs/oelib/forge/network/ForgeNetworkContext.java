package cc.sighs.oelib.forge.network;

import cc.sighs.oelib.network.api.INetworkContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

@Deprecated(since = "0.3.0")
public class ForgeNetworkContext extends cc.sighs.oelib.network.forge.ForgeNetworkContext {
    @Deprecated(since = "0.3.0")
    public ForgeNetworkContext(NetworkEvent.Context context) { super(context); }
}
