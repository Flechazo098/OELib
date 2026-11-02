package cc.sighs.oelib.data.fabric;

import cc.sighs.oelib.data.OELibData;
import cc.sighs.oelib.data.api.DeferredBatch;
import cc.sighs.oelib.data.api.DeferredRetryResult;
import cc.sighs.oelib.data.api.DataDeferredEvent;
import cc.sighs.oelib.data.api.DataChangedEvent;
import cc.sighs.oelib.data.util.data.DeferredDataQueue;
import cc.sighs.oelib.data.util.data.DataLoadPipeline;
import com.flechazo.hkt.Try;
import cc.sighs.oelib.event.EventBus;
import cc.sighs.oelib.event.LogicalSide;
import cc.sighs.oelib.data.api.DataDriven;
import cc.sighs.oelib.data.api.DataValidator;
import cc.sighs.oelib.data.net.DataSyncPacket;
import cc.sighs.oelib.data.util.CodecUtils;
import cc.sighs.oelib.event.fabric.DataReloadEvent;
import cc.sighs.oelib.platform.Platform;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

public class DataManager<T> implements SimpleResourceReloadListener<Map<ResourceLocation, JsonElement>> {
    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final Map<Class<?>, DataManager<?>> managers = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Set<String>> runtimeRegisteredNamespaces = new ConcurrentHashMap<>();
    private static boolean serverStarted = false;

    static {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            serverStarted = true;
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            endServerSession();
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            if (server != null) {
                for (DataManager<?> manager : managers.values()) {
                    if (manager.annotation.syncToClient()) {
                        manager.syncToPlayer(player);
                    }
                }
            }
        });

    }

    private final Class<T> dataClass;
    private final DataDriven annotation;
    private final Codec<T> codec;
    private final Map<ResourceLocation, T> loadedData = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, T> deferredData = new ConcurrentHashMap<>();
    private final DeferredDataQueue<T> deferred = new DeferredDataQueue<>(loadedData, deferredData,
            this::validateData, this::checkRetryThread, this::onDeferredCommitted);
    private final Map<String, Set<T>> cache = new ConcurrentHashMap<>();
    private final Map<String, Class<? extends DataValidator<?>>> namespaceValidatorClasses = new ConcurrentHashMap<>();
    private final Map<String, DataValidator<T>> namespaceValidators = new ConcurrentHashMap<>();
    private final DataValidator<T> defaultValidator;

    private DataManager(Class<T> dataClass) {
        this.dataClass = dataClass;
        this.annotation = dataClass.getAnnotation(DataDriven.class);
        this.codec = CodecUtils.getCodec(dataClass);
        this.defaultValidator = createValidator(annotation.validator());

        for (DataDriven.ValidatorBinding binding : annotation.namespaceValidators()) {
            if (binding != null && binding.namespace() != null && !binding.namespace().isBlank() && binding.validator() != null) {
                namespaceValidatorClasses.put(binding.namespace(), binding.validator());
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> DataManager<T> register(Class<T> dataClass) {
        if (!dataClass.isAnnotationPresent(DataDriven.class)) {
            throw new IllegalArgumentException("Class " + dataClass.getSimpleName() + " must be annotated with @DataDriven");
        }

        return (DataManager<T>) managers.computeIfAbsent(dataClass, DataManager::new);
    }

    @SuppressWarnings("unchecked")
    public static <T> DataManager<T> get(Class<T> dataClass) {
        return (DataManager<T>) managers.get(dataClass);
    }

    public static <T> void registerNamespace(Class<T> dataClass, String namespace) {
        runtimeRegisteredNamespaces
                .computeIfAbsent(dataClass, k -> ConcurrentHashMap.newKeySet())
                .add(namespace);
    }

    public static <T> void registerNamespaceValidator(Class<T> dataClass, String namespace, Class<? extends DataValidator<?>> validatorClass) {
        DataManager<T> manager = get(dataClass);
        if (manager != null && namespace != null && !namespace.isBlank() && validatorClass != null) {
            manager.namespaceValidatorClasses.put(namespace, validatorClass);
        }
    }

    public static MinecraftServer getCurrentServer() {
        return Platform.getCurrentServer();
    }

    private static String getFolder(Class<?> dataClass) {
        DataDriven annotation = dataClass.getAnnotation(DataDriven.class);
        return annotation.folder();
    }

    private static String getModId(Class<?> dataClass) {
        DataDriven annotation = dataClass.getAnnotation(DataDriven.class);
        return annotation.modid();
    }

    @Override
    public ResourceLocation getFabricId() {
        String classPath = dataClass.getName()
                .toLowerCase(Locale.ROOT)
                .replace('.', '/')
                .replace('$', '_');
        return new ResourceLocation(OELibData.MOD_ID, "data_manager/" + classPath);
    }

    @Override
    public CompletableFuture<Map<ResourceLocation, JsonElement>> load(ResourceManager manager, ProfilerFiller profiler, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            Map<ResourceLocation, JsonElement> data = new HashMap<>();
            String folder = getFolder(dataClass);
            String expectedNamespace = getModId(dataClass);

            manager.listResources(folder, path -> path.getPath().endsWith(".json")).forEach((source, resource) -> {
                if (!expectedNamespace.isEmpty() && !source.getNamespace().equals(expectedNamespace)) {
                    return;
                }
                Try.run(() -> {
                    try (var reader = resource.openAsReader()) {
                        data.put(source, GSON.fromJson(reader, JsonElement.class));
                    }
                }).peekFailure(error -> OELibData.LOGGER.error("Failed to load JSON from {}", source, error));
            });

            return data;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> apply(Map<ResourceLocation, JsonElement> data, ResourceManager manager, ProfilerFiller profiler, Executor executor) {
        return CompletableFuture.runAsync(() -> {
            loadedData.clear();
            deferred.invalidate();
            clearCache();

            Codec<T> currentCodec = CodecUtils.getCodec(dataClass);

            Set<String> allowNamespaces = new HashSet<>();
            if (!annotation.modid().isEmpty()) allowNamespaces.add(annotation.modid());
            Set<String> runtimeSet = runtimeRegisteredNamespaces.getOrDefault(dataClass, Collections.emptySet());
            allowNamespaces.addAll(runtimeSet);

            Map<ResourceLocation, JsonElement> filteredObject = data.entrySet().stream()
                    .filter(entry -> allowNamespaces.isEmpty() || allowNamespaces.contains(entry.getKey().getNamespace()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            OELibData.LOGGER.info("Loading {} data from {} files (namespaces: {})",
                    dataClass.getSimpleName(), filteredObject.size(), allowNamespaces);

            var counts = DataLoadPipeline.load(dataClass, currentCodec, filteredObject, annotation.supportArray(),
                    this::validateData,
                    (source, value) -> {
                        loadedData.put(source, value);
                        if (annotation.enableCache()) buildCache(value);
                    },
                    (source, entry) -> deferred.defer(source, entry.data(), entry.reason()));

            OELibData.LOGGER.info("Loaded {} valid {} entries, {} deferred entries, {} invalid entries were skipped",
                    counts.accepted(), dataClass.getSimpleName(), counts.deferred(), counts.rejected());

            DataReloadEvent.EVENT.invoker().onDataReload(dataClass, counts.accepted() + counts.deferred(), counts.rejected());
            EventBus.post(new DataChangedEvent<>(dataClass, LogicalSide.SERVER, DataChangedEvent.Cause.RELOAD, getAllData()));
            if (annotation.syncToClient() && serverStarted) {
                syncToAllPlayers();
            }
            var pendingBatch = deferred.batch();
            if (!pendingBatch.entries().isEmpty()) {
                EventBus.post(new DataDeferredEvent<>(dataClass, LogicalSide.SERVER, pendingBatch));
            }
        }, executor);
    }

    public DeferredBatch<T> getDeferredBatch() { return deferred.batch(); }

    private void checkRetryThread() {
        var server = Platform.getCurrentServer();
        if (server == null || !server.isSameThread()) {
            throw new IllegalStateException("Retry deferred data on the owning server thread after server initialization");
        }
    }

    private void onDeferredCommitted(DeferredRetryResult result) {
        result.rejected().forEach((source, reason) ->
                OELibData.LOGGER.warn("Rejected deferred {} at {}: {}", dataClass.getSimpleName(), source, reason));
        if (result.accepted() > 0) {
            clearCache();
            if (annotation.enableCache()) loadedData.values().forEach(this::buildCache);
            DataReloadEvent.EVENT.invoker().onDataReload(dataClass, loadedData.size(), result.rejected().size());
            EventBus.post(new DataChangedEvent<>(dataClass, LogicalSide.SERVER,
                    DataChangedEvent.Cause.DEFERRED_RESOLVED, getAllData()));
            if (annotation.syncToClient()) syncToAllPlayers();
        }
    }

    private static void endServerSession() {
        serverStarted = false;
        for (DataManager<?> manager : managers.values()) {
            manager.deferred.invalidate();
            manager.loadedData.clear();
            manager.clearCache();
        }
    }

    private DataValidator.ValidationResult validateData(T data, ResourceLocation source) {
        var validator = getValidatorForNamespace(source.getNamespace());
        if (validator instanceof DataValidator.ServerContextAware<T> contextual) {
            return contextual.validateWithContext(data, source, Platform.getCurrentServer());
        } else {
            return validator.validate(data, source);
        }
    }

    public Map<ResourceLocation, T> getAllData() {
        return new HashMap<>(loadedData);
    }

    public T getData(ResourceLocation location) {
        return loadedData.get(location);
    }

    public List<T> getDataList() {
        List<T> result = new ArrayList<>(loadedData.values());
        result.addAll(deferredData.values());
        return result;
    }

    public Set<T> getCachedData(String cacheKey) {
        if (!annotation.enableCache()) {
            throw new UnsupportedOperationException("Cache is disabled for " + dataClass.getSimpleName());
        }
        return cache.getOrDefault(cacheKey, Collections.emptySet());
    }

    public void updateClientData(Map<ResourceLocation, T> data) {
        // An integrated server shares this manager with its client; do not overwrite server pending state.
        if (Platform.getCurrentServer() != null) {
            var snapshot = getAllData();
            DataReloadEvent.EVENT.invoker().onDataReload(dataClass, snapshot.size(), 0);
            EventBus.post(new DataChangedEvent<>(dataClass, LogicalSide.CLIENT,
                    DataChangedEvent.Cause.REMOTE_SYNC, snapshot));
            return;
        }
        deferred.invalidate();
        loadedData.clear();
        loadedData.putAll(data);
        clearCache();

        if (annotation.enableCache()) {
            for (Map.Entry<ResourceLocation, T> entry : data.entrySet()) {
                buildCache(entry.getValue());
            }
        }

        OELibData.LOGGER.debug("Updated client data for {}: {} entries", dataClass.getSimpleName(), data.size());

        DataReloadEvent.EVENT.invoker().onDataReload(dataClass, data.size(), 0);
        EventBus.post(new DataChangedEvent<>(dataClass, LogicalSide.CLIENT, DataChangedEvent.Cause.REMOTE_SYNC, getAllData()));
    }

    public void addToCache(String cacheKey, T data) {
        if (annotation.enableCache()) {
            cache.computeIfAbsent(cacheKey, k -> ConcurrentHashMap.newKeySet()).add(data);
        }
    }

    public void clearCache() {
        cache.clear();
    }

    protected void buildCache(T data) {
        addToCache("all", data);
    }

    @SuppressWarnings("unchecked")
    private DataValidator<T> getValidatorForNamespace(String namespace) {
        if (namespace == null) return defaultValidator;
        return namespaceValidators.computeIfAbsent(namespace, ns -> {
            Class<? extends DataValidator<?>> type = namespaceValidatorClasses.get(ns);
            if (type == null) return defaultValidator;
            return Try.of(() -> (DataValidator<T>) type.getDeclaredConstructor().newInstance())
                    .peekFailure(error -> OELibData.LOGGER.warn(
                            "Failed to instantiate validator for namespace '{}', fallback to default", ns, error))
                    .orElse(defaultValidator);
        });
    }

    private void syncToAllPlayers() {
        Try.run(() -> {
            if (Platform.getCurrentServer() != null) {
                new DataSyncPacket<>(dataClass, getAllData()).sendToAll();
                OELibData.LOGGER.debug("Synced {} data to all players", dataClass.getSimpleName());
            }
        }).peekFailure(error -> OELibData.LOGGER.error(
                "Failed to sync {} data to all players", dataClass.getSimpleName(), error));
    }

    public void syncToPlayer(ServerPlayer player) {
        if (player != null && annotation.syncToClient()) {
            Try.run(() -> {
                new DataSyncPacket<>(dataClass, getAllData()).sendTo(player);
                OELibData.LOGGER.debug("Synced {} data to player: {}", dataClass.getSimpleName(), player.getName().getString());
            }).peekFailure(error -> OELibData.LOGGER.error("Failed to sync {} data to player {}",
                    dataClass.getSimpleName(), player.getName().getString(), error));
        }
    }

    @SuppressWarnings("unchecked")
    private DataValidator<T> createValidator(Class<? extends DataValidator<?>> validatorClass) {
        if (validatorClass == DataValidator.NoValidator.class) {
            return (DataValidator<T>) new DataValidator.NoValidator();
        }
        return Try.of(() -> (DataValidator<T>) validatorClass.getDeclaredConstructor().newInstance())
                .peekFailure(error -> OELibData.LOGGER.warn("Failed to create validator {}, using no validator",
                        validatorClass.getSimpleName(), error))
                .orElseGet(() -> (DataValidator<T>) new DataValidator.NoValidator());
    }
}
