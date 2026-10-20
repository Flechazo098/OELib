package cc.sighs.oelib.fabric.network;

import cc.sighs.oelib.network.OELibNetwork;
import cc.sighs.oelib.platform.Platform;
import cc.sighs.oelib.network.api.*;
import cc.sighs.oelib.network.serialization.NetworkSerialization;
import cc.sighs.oelib.network.spi.INetworkManager;
import cc.sighs.oelib.network.util.NetworkUtil;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Deprecated(since = "0.3.0")
public class NetworkManagerImpl extends cc.sighs.oelib.network.fabric.NetworkManagerImpl {
    @Deprecated(since = "0.3.0")
    public NetworkManagerImpl() {}
}
