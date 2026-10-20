package cc.sighs.oelib.fabric.data;

import cc.sighs.oelib.data.api.DataValidator;
import cc.sighs.oelib.platform.Platform;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Deprecated(since = "0.3.0")
public class DataManager<T> {
    private final cc.sighs.oelib.data.fabric.DataManager<T> delegate;

    private static final Map<Class<?>, DataManager<?>> WRAPPERS = new ConcurrentHashMap<>();

    private DataManager(cc.sighs.oelib.data.fabric.DataManager<T> delegate) { this.delegate = delegate; }

    @SuppressWarnings("unchecked")
    private static <T> DataManager<T> wrap(Class<T> type, cc.sighs.oelib.data.fabric.DataManager<T> delegate) {
        if (delegate == null) return null;
        return (DataManager<T>) WRAPPERS.compute(type, (key, previous) ->
                previous != null && previous.delegate == delegate ? previous : new DataManager<>(delegate));
    }

    @Deprecated(since = "0.3.0")
    public static <T> DataManager<T> register(Class<T> type) {
        return wrap(type, cc.sighs.oelib.data.fabric.DataManager.register(type));
    }

    @Deprecated(since = "0.3.0")
    public static <T> DataManager<T> get(Class<T> type) {
        return wrap(type, cc.sighs.oelib.data.fabric.DataManager.get(type));
    }

    @Deprecated(since = "0.3.0")
    public static <T> void registerNamespace(Class<T> type, String namespace) {
        cc.sighs.oelib.data.fabric.DataManager.registerNamespace(type, namespace);
    }

    @Deprecated(since = "0.3.0")
    public static <T> void registerNamespaceValidator(Class<T> type, String namespace, Class<? extends DataValidator<?>> validator) {
        cc.sighs.oelib.data.fabric.DataManager.registerNamespaceValidator(type, namespace, validator);
    }

    @Deprecated(since = "0.3.0")
    public Map<Identifier, T> getAllData() {
        return delegate.getAllData();
    }

    @Deprecated(since = "0.3.0")
    public T getData(Identifier id) {
        return delegate.getData(id);
    }

    @Deprecated(since = "0.3.0")
    public List<T> getDataList() {
        return delegate.getDataList();
    }

    @Deprecated(since = "0.3.0")
    public Set<T> getCachedData(String key) {
        return delegate.getCachedData(key);
    }

    @Deprecated(since = "0.3.0")
    public void addToCache(String key, T value) {
        delegate.addToCache(key, value);
    }

    @Deprecated(since = "0.3.0")
    public void clearCache() {
        delegate.clearCache();
    }

    @Deprecated(since = "0.3.0")
    public void updateClientData(Map<Identifier, T> data) {
        delegate.updateClientData(data);
    }

    @Deprecated(since = "0.3.0")
    public void syncToPlayer(ServerPlayer player) {
        delegate.syncToPlayer(player);
    }

    @Deprecated(since = "0.3.0")
    public static MinecraftServer getCurrentServer() {
        return Platform.getCurrentServer();
    }
}
