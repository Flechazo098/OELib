package cc.sighs.oelib.data;

import cc.sighs.oelib.data.api.DataDriven;
import cc.sighs.oelib.data.api.DataValidator;
import cc.sighs.oelib.data.api.DeferredBatch;
import cc.sighs.oelib.data.api.DataDeferredEvent;
import cc.sighs.oelib.platform.Platform;
import net.minecraft.server.MinecraftServer;
import cc.sighs.oelib.data.spi.IDataManager;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Provides access to registered data, validation batches, and associated caches.
 *
 * <p>Effective entries have passed validation. Pending entries are available through
 * {@link #getDeferredBatch(Class)} and {@link #getDataList(Class)} but are excluded
 * from effective-data queries and synchronization. Returned data values are not copied.
 *
 * <p>Registration must complete before resource loading begins. Data mutations must
 * run on the thread owning the relevant logical side and must not overlap a resource load.
 */
public final class DataManager {
    private static final IDataManager IMPL;

    static {
        IMPL = ServiceLoader.load(IDataManager.class)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No IDataManager implementation found"));
    }

    @Deprecated(since = "0.3.0")
    public static MinecraftServer getServer() {
        return Platform.getCurrentServer();
    }

    @Deprecated(since = "0.3.0")
    public static boolean isModLoaded(String modId) {
        return Platform.isModLoaded(modId);
    }

    private DataManager() {
    }

    /**
     * Registers a data type for resource loading and access through this class.
     *
     * <p>Repeated registration preserves the existing data manager and its entries.
     * The type must declare its data-loading configuration and provide a compatible codec.
     * Registration alone does not load resources.
     *
     * @param <T> the data type
     * @param dataClass the non-null class annotated with {@link DataDriven}
     * @throws IllegalArgumentException if the class is not annotated with {@code DataDriven}
     */
    @ApiStatus.Internal
    public static <T> void register(Class<T> dataClass) {
        IMPL.register(dataClass);
    }

    /**
     * Adds a namespace to the resources accepted for a data type.
     *
     * <p>The namespace supplements the type's declared namespace. Repeated additions
     * have no additional effect. This registration may precede type registration and
     * does not reload resources that have already been processed.
     *
     * @param <T> the data type
     * @param dataClass the non-null data class
     * @param namespace the non-null resource namespace to accept
     */
    @ApiStatus.Internal
    public static <T> void registerNamespace(Class<T> dataClass, String namespace) {
        IMPL.registerNamespace(dataClass, namespace);
    }

    /**
     * Registers a validator for entries originating in a specific namespace.
     *
     * <p>The binding must be registered before the namespace is first validated.
     * The validator must have an accessible no-argument constructor. This method
     * does not revalidate existing entries. If the data type is unregistered, the
     * namespace is null or blank, or the validator class is null, it has no effect.
     *
     * @param <T> the registered data type
     * @param dataClass the non-null data class
     * @param namespace the resource namespace to associate with the validator
     * @param validatorClass the class implementing validation for values of type {@code T}
     */
    @ApiStatus.Internal
    public static <T> void registerNamespaceValidator(Class<T> dataClass, String namespace,
                                                      Class<? extends DataValidator<?>> validatorClass) {
        IMPL.registerNamespaceValidator(dataClass, namespace, validatorClass);
    }

    /**
     * Returns the effective entries for a registered data type.
     *
     * @param <T> the registered data type
     * @param dataClass the non-null registered class
     * @return a snapshot excluding entries awaiting validation; an empty map if unregistered
     * @see #getDeferredBatch(Class)
     */
    public static <T> Map<Identifier, T> getAllData(Class<T> dataClass) {
        return IMPL.getAllData(dataClass);
    }

    /**
     * Returns a handle to the current load's pending entries.
     *
     * <p>The handle expires on a subsequent resource load or server shutdown. Obtaining
     * it neither validates entries nor schedules a retry. Retry operations must run
     * on the owning server thread after server initialization.
     *
     * @param <T> the registered data type
     * @param dataClass the non-null registered class
     * @return a non-null batch, which may contain no pending entries
     * @throws IllegalArgumentException if the class has not been registered
     * @see DataDeferredEvent
     */
    public static <T> DeferredBatch<T> getDeferredBatch(Class<T> dataClass) {
        return IMPL.getDeferredBatch(dataClass);
    }

    /**
     * Returns the effective value associated with a source ID.
     *
     * @param <T> the registered data type
     * @param dataClass the non-null data class
     * @param location the non-null source ID
     * @return the value, or {@code null} if the type is unregistered, the source is
     *         absent, or the entry is still awaiting validation
     */
    public static <T> T getData(Class<T> dataClass, Identifier location) {
        return IMPL.getData(dataClass, location);
    }

    /**
     * Returns effective and pending values for a data type.
     *
     * <p>Pending values have not passed final validation. This method retains its
     * historical behavior; use {@link #getAllData(Class)} for effective values only.
     *
     * @param <T> the registered data type
     * @param dataClass the non-null registered class
     * @return a snapshot including pending values; an empty list if unregistered
     */
    public static <T> List<T> getDataList(Class<T> dataClass) {
        return IMPL.getDataList(dataClass);
    }

    /**
     * Returns the values associated with a cache key.
     *
     * <p>The returned set is not a snapshot and may reflect subsequent additions.
     * Callers must treat it as read-only and query again after a cache clear or rebuild.
     * Cached values are not independently validated by this method.
     *
     * @param <T> the registered data type
     * @param dataClass the non-null data class
     * @param cacheKey the non-null cache key
     * @return the cached values, or an empty set if the key or data type is unregistered
     * @throws UnsupportedOperationException if caching is disabled for a registered type
     */
    public static <T> Set<T> getCachedData(Class<T> dataClass, String cacheKey) {
        return IMPL.getCachedData(dataClass, cacheKey);
    }

    /**
     * Adds a value to the set associated with a cache key.
     *
     * <p>Equal values are retained only once. This operation does not validate the
     * value, add an effective entry, publish a data event, or synchronize clients.
     * It has no effect if the type is unregistered or caching is disabled.
     *
     * @param <T> the registered data type
     * @param dataClass the non-null data class
     * @param cacheKey the non-null cache key
     * @param data the non-null value to cache
     */
    public static <T> void addToCache(Class<T> dataClass, String cacheKey, T data) {
        IMPL.addToCache(dataClass, cacheKey, data);
    }

    /**
     * Removes all cached associations for a data type.
     *
     * <p>Effective and pending entries remain unchanged. This operation does not
     * publish a data event or synchronize clients and has no effect for an unregistered type.
     *
     * @param <T> the registered data type
     * @param dataClass the non-null data class
     */
    public static <T> void clearCache(Class<T> dataClass) {
        IMPL.clearCache(dataClass);
    }

    /**
     * Applies an authoritative server snapshot on the logical client.
     *
     * <p>On a client connected to a remote server, the snapshot replaces effective
     * entries, discards pending entries, invalidates retained batch handles, and rebuilds
     * enabled caches without local validation. An empty snapshot clears previous entries.
     * When an integrated server is active, its effective data, caches, and pending batches
     * are preserved; client notifications describe the current shared effective snapshot.
     *
     * <p>For a registered type, this method publishes client data-change notifications
     * after the update, including for an empty or unchanged snapshot. The caller must
     * execute it on the client thread. An unregistered type is ignored.
     *
     * @param <T> the registered data type
     * @param dataClass the non-null data class
     * @param data the non-null authoritative snapshot with non-null source IDs and values
     */
    public static <T> void updateClientData(Class<T> dataClass, Map<Identifier, T> data) {
        IMPL.updateClientData(dataClass, data);
    }

    /**
     * Applies a client snapshot whose data type is determined at runtime.
     *
     * <p>This method has the same update and notification semantics as
     * {@link #updateClientData(Class, Map)}. The caller must ensure that every value
     * is compatible with {@code dataClass}; this method does not check value types.
     * The caller must execute it on the client thread. An unregistered type is ignored.
     *
     * @param dataClass the non-null class describing every value in the snapshot
     * @param data the non-null authoritative snapshot with non-null source IDs and values
     */
    public static void updateClientDataRaw(Class<?> dataClass, Map<Identifier, ?> data) {
        IMPL.updateClientDataRaw(dataClass, data);
    }
}