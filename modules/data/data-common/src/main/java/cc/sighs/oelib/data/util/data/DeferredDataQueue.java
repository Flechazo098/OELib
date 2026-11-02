package cc.sighs.oelib.data.util.data;

import cc.sighs.oelib.data.api.DataValidator;
import cc.sighs.oelib.data.api.DeferredBatch;
import cc.sighs.oelib.data.api.DeferredEntry;
import cc.sighs.oelib.data.api.DeferredRetryResult;
import com.flechazo.hkt.Try;
import com.flechazo.hkt.business.effect.IO;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Coordinates explicitly requested validation for one data manager.
 *
 * <p>Callers serialize lifecycle mutations on the owning thread. Validation and
 * commit notifications must not run concurrently with another load.
 *
 * @param <T> the managed data type
 */
public final class DeferredDataQueue<T> {
    private final Map<ResourceLocation, T> loaded;
    private final Map<ResourceLocation, T> pending;
    private final Map<ResourceLocation, String> reasons = new HashMap<>();
    private final BiFunction<T, ResourceLocation, DataValidator.ValidationResult> validate;
    private final Runnable checkThread;
    private final Consumer<DeferredRetryResult> committed;
    private long generation;
    private boolean retrying;

    /**
     * Creates a coordinator for the supplied effective and pending entries.
     *
     * @param loaded      the non-null effective-data destination
     * @param pending     the non-null pending-data destination
     * @param validate    the non-null validator for each value and source ID
     * @param checkThread the non-null action rejecting calls from an invalid execution context
     * @param committed   the non-null notification invoked after acceptance or rejection is committed
     */
    public DeferredDataQueue(Map<ResourceLocation, T> loaded, Map<ResourceLocation, T> pending,
                             BiFunction<T, ResourceLocation, DataValidator.ValidationResult> validate,
                             Runnable checkThread, Consumer<DeferredRetryResult> committed) {
        this.loaded = loaded;
        this.pending = pending;
        this.validate = validate;
        this.checkThread = checkThread;
        this.committed = committed;
    }

    /**
     * Invalidates existing handles and removes all pending entries.
     *
     * <p>Effective entries are left unchanged.
     */
    public synchronized void invalidate() {
        generation++;
        pending.clear();
        reasons.clear();
    }

    /**
     * Retains a value for explicitly requested validation.
     *
     * @param source the non-null source ID
     * @param data   the non-null pending value
     * @param reason the deferral reason, or {@code null} for a default reason
     */
    public synchronized void defer(ResourceLocation source, T data, String reason) {
        pending.put(source, data);
        reasons.put(source, reason == null ? "Validation deferred" : reason);
    }

    /**
     * Returns a handle associated with the current generation.
     *
     * @return a non-null handle that expires on invalidation
     */
    public synchronized DeferredBatch<T> batch() {
        final long token = generation;
        return new DeferredBatch<>() {
            public long generation() {
                return token;
            }

            public boolean isCurrent() {
                synchronized (DeferredDataQueue.this) {
                    return token == generation;
                }
            }

            public Map<ResourceLocation, DeferredEntry<T>> entries() {
                synchronized (DeferredDataQueue.this) {
                    return token == generation ? snapshot() : Map.of();
                }
            }

            public DeferredRetryResult retry(Predicate<ResourceLocation> sources) {
                return retryBatch(token, sources);
            }
        };
    }

    private Map<ResourceLocation, DeferredEntry<T>> snapshot() {
        Map<ResourceLocation, DeferredEntry<T>> result = new LinkedHashMap<>();
        pending.forEach((source, data) -> result.put(source, new DeferredEntry<>(data, reasons.get(source))));
        return Map.copyOf(result);
    }

    private DeferredRetryResult stale() {
        return new DeferredRetryResult(DeferredRetryResult.Status.STALE, 0, 0, Map.of());
    }

    private DeferredRetryResult retryBatch(long token, Predicate<ResourceLocation> sources) {
        Objects.requireNonNull(sources);
        Map<ResourceLocation, DeferredEntry<T>> candidates;
        synchronized (this) {
            if (token != generation) return stale();
        }
        checkThread.run();
        synchronized (this) {
            if (token != generation) return stale();
            if (retrying) throw new IllegalStateException("Deferred validation cannot recursively retry itself");
            candidates = snapshot();
            retrying = true;
        }
        return IO.delay(() -> retryCandidates(token, sources, candidates))
                .guarantee(IO.exec(this::finishRetry))
                .unsafeRunAttempt()
                .getOrThrow();
    }

    private synchronized void finishRetry() {
        retrying = false;
    }

    private DeferredRetryResult retryCandidates(long token, Predicate<ResourceLocation> sources,
                                                Map<ResourceLocation, DeferredEntry<T>> candidates) {
        Map<ResourceLocation, T> accepted = new HashMap<>();
        Map<ResourceLocation, String> rejected = new HashMap<>(), waiting = new HashMap<>();
        for (var entry : candidates.entrySet()) {
            if (!sources.test(entry.getKey())) continue;
            var result = Try.of(() -> Objects.requireNonNull(validate.apply(entry.getValue().data(), entry.getKey())))
                    .getOrThrow(error -> new IllegalStateException("Deferred validation failed for " + entry.getKey(), error));
            if (!result.valid())
                rejected.put(entry.getKey(), Objects.toString(result.message(), "Validation failed"));
            else if (result.deferrable())
                waiting.put(entry.getKey(), Objects.toString(result.message(), "Validation deferred"));
            else accepted.put(entry.getKey(), entry.getValue().data());
        }
        DeferredRetryResult result;
        synchronized (this) {
            if (token != generation) return stale();
            loaded.putAll(accepted);
            accepted.keySet().forEach(key -> {
                pending.remove(key);
                reasons.remove(key);
            });
            rejected.keySet().forEach(key -> {
                pending.remove(key);
                reasons.remove(key);
            });
            reasons.putAll(waiting);
            result = new DeferredRetryResult(DeferredRetryResult.Status.COMPLETED, accepted.size(), pending.size(), rejected);
        }
        if (!accepted.isEmpty() || !rejected.isEmpty()) committed.accept(result);
        return result;
    }
}
