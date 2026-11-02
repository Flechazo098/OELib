package cc.sighs.oelib.data.api;

import net.minecraft.resources.Identifier;

import java.util.Map;

/**
 * Describes the outcome of a deferred validation attempt.
 *
 * <p>The rejected entries are an unmodifiable snapshot. An expired batch returns
 * {@link Status#STALE}, zero counts, and no rejected entries without validating data.
 *
 * @param status whether the batch was current when the results were committed
 * @param accepted the number of values accepted by this attempt
 * @param remaining the number of pending entries in the entire batch after this attempt
 * @param rejected the source IDs rejected by this attempt and their failure reasons
 */
public record DeferredRetryResult(Status status, int accepted, int remaining,
                                  Map<Identifier, String> rejected) {
    /** Indicates whether a retry belongs to the current resource-load generation. */
    public enum Status {
        /** The attempt completed against the current generation, possibly accepting no entries. */
        COMPLETED,
        /** The handle expired; no results from this attempt were committed. */
        STALE
    }

    /**
     * Creates a result with an unmodifiable copy of the rejected entries.
     *
     * @param status the outcome for this load
     * @param accepted the number accepted
     * @param remaining the number still pending
     * @param rejected the non-null rejected source IDs and reasons
     */
    public DeferredRetryResult {
        rejected = Map.copyOf(rejected);
    }
}
