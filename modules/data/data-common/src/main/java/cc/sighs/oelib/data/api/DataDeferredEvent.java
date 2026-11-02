package cc.sighs.oelib.data.api;

import cc.sighs.oelib.event.Event;
import cc.sighs.oelib.event.LogicalSide;

/**
 * Announces pending data after a resource load has committed.
 *
 * <p>This event is posted on the OELib event bus once per completed load with pending
 * entries. Receiving it does not retry validation. The consumer chooses when to retry
 * the batch. Continued deferral during a retry does not post this event again.
 *
 * <p>The event may arrive before a server is available. The batch must be retried on
 * its owning server thread after server initialization. Listeners may retain the handle;
 * a later load or server shutdown invalidates it.
 *
 * @param <T> the registered data type
 * @param dataClass the non-null registered class
 * @param side the logical side that owns the pending data; currently {@code SERVER}
 * @param batch the non-null handle to this load's pending entries
 * @see DeferredBatch
 */
public record DataDeferredEvent<T>(Class<T> dataClass, LogicalSide side, DeferredBatch<T> batch) implements Event {
}
