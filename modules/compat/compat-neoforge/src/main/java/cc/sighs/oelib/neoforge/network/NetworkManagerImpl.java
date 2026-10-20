package cc.sighs.oelib.neoforge.network;

import cc.sighs.oelib.network.OELibNetwork;
import cc.sighs.oelib.network.api.INetworkPacket;
import cc.sighs.oelib.network.api.NetworkAutoRegistration;
import cc.sighs.oelib.network.api.NetworkPacket;
import cc.sighs.oelib.network.api.NetworkPacketTypes;
import cc.sighs.oelib.network.serialization.NetworkSerialization;
import cc.sighs.oelib.network.spi.INetworkManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Deprecated(since = "0.3.0")
public class NetworkManagerImpl extends cc.sighs.oelib.network.neoforge.NetworkManagerImpl {
    @Deprecated(since = "0.3.0")
    public NetworkManagerImpl() {}
}
