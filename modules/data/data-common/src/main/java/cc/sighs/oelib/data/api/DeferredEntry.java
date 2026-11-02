package cc.sighs.oelib.data.api;

/**
 * Describes a value awaiting validation.
 *
 * <p>The value is not copied. The reason describes the most recent deferral.
 *
 * @param <T> the data type
 * @param data the non-null pending value
 * @param reason the non-null reason validation was deferred
 */
public record DeferredEntry<T>(T data, String reason) {
}
