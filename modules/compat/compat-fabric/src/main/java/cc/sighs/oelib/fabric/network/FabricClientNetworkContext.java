package cc.sighs.oelib.fabric.network;

import cc.sighs.oelib.network.api.INetworkContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerPlayer;

@Deprecated(since = "0.3.0")
public class FabricClientNetworkContext extends cc.sighs.oelib.network.fabric.FabricClientNetworkContext {
    @Deprecated(since = "0.3.0")
    public FabricClientNetworkContext(ClientPlayNetworking.Context context) { super(context); }
}
