package cc.sighs.oelib.fabric.network;

import cc.sighs.oelib.network.api.INetworkContext;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerPlayer;

@Deprecated(since = "0.3.0")
public class FabricServerNetworkContext extends cc.sighs.oelib.network.fabric.FabricServerNetworkContext {
    @Deprecated(since = "0.3.0")
    public FabricServerNetworkContext(ServerPlayNetworking.Context context) { super(context); }
}
