package cc.sighs.oelib.data.api;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.function.Predicate;

/**
 * Represents pending data from one resource-load generation.
 *
 * <p>A new resource load or server shutdown invalidates this handle. Successful
 * retries remove accepted entries, so retrying the same handle does not accept an
 * entry twice. No lifecycle event or timer is implicitly selected for retrying.
 *
 * @param <T> the registered data type
 */
public interface DeferredBatch<T> {
    /**
     * Returns the generation associated with this handle.
     *
     * @return an identifier meaningful only within the owning data manager
     */
    long generation();

    /**
     * Determines whether this handle still refers to the current load.
     *
     * @return {@code true} if no subsequent load or shutdown has invalidated it
     */
    boolean isCurrent();

    /**
     * Returns a snapshot of the entries still pending in this batch.
     *
     * @return an unmodifiable map, or an empty map if this handle has expired;
     *         entry values are not deep-copied
     */
    Map<ResourceLocation, DeferredEntry<T>> entries();

    /**
     * Revalidates pending entries selected by their source IDs.
     *
     * <p>The caller must use the owning server thread after server initialization.
     * Accepted entries become effective before change notifications are delivered.
     * Rejected entries are removed and reported; entries deferred again remain pending.
     * A stale handle returns {@code STALE} without invoking the predicate or validator.
     *
     * <p>If validation or selection throws, this attempt commits no validation results.
     * A newer load supersedes the attempt. Notification failures occur after commit
     * and do not undo accepted results. Predicates must not mutate the owning manager.
     *
     * @param sources the non-null predicate selecting entries to retry
     * @return the accepted count, remaining count, rejection reasons, and status
     * @throws IllegalStateException if called on the wrong thread, before server
     *         initialization, recursively during a retry, or if validation fails exceptionally
     */
    DeferredRetryResult retry(Predicate<ResourceLocation> sources);

    /**
     * Revalidates every remaining entry in this batch.
     *
     * @return the result of retrying all pending source IDs
     * @throws IllegalStateException if retrying is not permitted or validation fails exceptionally
     * @see #retry(Predicate)
     */
    default DeferredRetryResult retry() {
        return retry(source -> true);
    }
}
