package cc.sighs.oelib.network.api;

import cc.sighs.oelib.misc.util.AnnotationScanUtil;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public final class NetworkAutoRegistration {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Set<Class<? extends INetworkPacket<?>>> REGISTERED_PACKET_CLASSES = ConcurrentHashMap.newKeySet();
    private static final Object REGISTRATION_LOCK = new Object();
    private static volatile boolean collected;
    private static final List<Consumer<Class<? extends INetworkPacket<?>>>> PACKET_LISTENERS = new CopyOnWriteArrayList<>();

    @Deprecated(since = "0.3.0")
    public static void addPacketRegistrationListener(Consumer<Class<? extends INetworkPacket<?>>> listener) {
        Objects.requireNonNull(listener, "listener");
        Set<Class<? extends INetworkPacket<?>>> existing;
        synchronized (REGISTRATION_LOCK) {
            PACKET_LISTENERS.add(listener);
            existing = collected ? Set.copyOf(REGISTERED_PACKET_CLASSES) : Set.of();
        }
        existing.forEach(listener);
    }

    @Deprecated(since = "0.3.0")
    public static void removePacketRegistrationListener(Consumer<Class<? extends INetworkPacket<?>>> listener) {
        PACKET_LISTENERS.remove(listener);
    }
    private static final MethodHandles.Lookup INTERNAL_LOOKUP = MethodHandles.lookup();
    private static final Map<String, MethodHandles.Lookup> PACKAGE_LOOKUPS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, MethodHandles.Lookup> PACKET_LOOKUPS = new ConcurrentHashMap<>();

    private NetworkAutoRegistration() {
    }

    /**
     * @deprecated OELib now scans each mod file once and queries its global annotation index.
     * Package registration is no longer required for annotation discovery.
     */
    @Deprecated(forRemoval = true)
    public static void registerBasePackage(String basePackage) {
        registerBasePackage(basePackage, INTERNAL_LOOKUP);
    }

    public static void registerBasePackage(String basePackage, MethodHandles.Lookup lookup) {
        if (basePackage == null || basePackage.isEmpty()) return;
        if (lookup == null) throw new IllegalArgumentException("lookup cannot be null");
        PACKAGE_LOOKUPS.put(basePackage, lookup);
        PACKET_LOOKUPS.replaceAll((packetClass, previous) -> resolveLookup(packetClass));
    }

    public static MethodHandles.Lookup lookupForPacketClass(Class<?> packetClass) {
        return PACKET_LOOKUPS.computeIfAbsent(packetClass, NetworkAutoRegistration::resolveLookup);
    }

    private static MethodHandles.Lookup resolveLookup(Class<?> packetClass) {
        String name = packetClass.getName();
        MethodHandles.Lookup best = INTERNAL_LOOKUP;
        int longest = -1;
        for (var entry : PACKAGE_LOOKUPS.entrySet()) {
            if (name.startsWith(entry.getKey() + ".") && entry.getKey().length() > longest) {
                best = entry.getValue();
                longest = entry.getKey().length();
            }
        }
        return best;
    }

    public static Set<Class<? extends INetworkPacket<?>>> findAllAnnotatedPackets() {
        List<Consumer<Class<? extends INetworkPacket<?>>>> listeners = List.of();
        Set<Class<? extends INetworkPacket<?>>> snapshot;
        synchronized (REGISTRATION_LOCK) {
            if (!collected) {
                collectPackets();
                collected = true;
                listeners = List.copyOf(PACKET_LISTENERS);
            }
            snapshot = Set.copyOf(REGISTERED_PACKET_CLASSES);
        }
        for (var listener : listeners) snapshot.forEach(listener);
        return snapshot;
    }

    private static void collectPackets() {
        Predicate<Class<?>> filter = AnnotationScanUtil.nonAbstractNonInterface()
                .and(INetworkPacket.class::isAssignableFrom)
                .and(CustomPacketPayload.class::isAssignableFrom);
        Set<Class<?>> classes = AnnotationScanUtil.findAnnotatedClasses(NetworkPacket.class, filter);

        int added = 0;
        int skipped = 0;
        for (Class<?> clazz : classes) {
            @SuppressWarnings("unchecked")
            Class<? extends INetworkPacket<?>> packetClass = (Class<? extends INetworkPacket<?>>) clazz;
            if (!REGISTERED_PACKET_CLASSES.add(packetClass)) {
                skipped++;
                continue;
            }
            added++;
            LOGGER.debug(
                    "[NetworkAutoReg] Found packet: {} (chunkThreshold={})",
                    packetClass.getName(), getChunkThreshold(packetClass)
            );
        }

        LOGGER.info(
                "[NetworkAutoReg] Found: {} | added: {} | skipped: {}",
                classes.size(), added, skipped
        );
    }

    public static int getChunkThreshold(Class<?> clazz) {
        NetworkPacket annotation = clazz.getAnnotation(NetworkPacket.class);
        if (annotation == null) {
            return 0;
        }

        int threshold = annotation.chunkThreshold();
        if (threshold == 0) {
            return 0;
        }
        if (threshold < NetworkPacket.MIN_CHUNK_SIZE) {
            throw new IllegalArgumentException("Packet " + clazz.getName() + " has chunkThreshold=" + threshold
                    + ", but positive chunk thresholds must be at least " + NetworkPacket.MIN_CHUNK_SIZE + " bytes");
        }

        int maximum = annotation.side() == Side.CLIENT
                ? NetworkPacket.MAX_CLIENTBOUND_CHUNK_SIZE
                : NetworkPacket.MAX_SERVERBOUND_CHUNK_SIZE;
        if (threshold > maximum) {
            throw new IllegalArgumentException("Packet " + clazz.getName() + " has chunkThreshold=" + threshold
                    + ", exceeding the safe maximum of " + maximum + " bytes for side " + annotation.side());
        }
        return threshold;
    }
}
