package cc.sighs.oelib.data.api;

import cc.sighs.oelib.event.Event;
import cc.sighs.oelib.event.LogicalSide;
import net.minecraft.resources.Identifier;

import java.util.Map;

/**
 * Announces a committed effective-data snapshot.
 *
 * <p>The event is posted after effective data and associated caches are updated.
 * The map is unmodifiable; its values are not copied. Pending entries are excluded.
 * A completed resource reload posts this event even if the snapshot is empty or unchanged.
 * A deferred retry posts it only when at least one entry becomes effective.
 *
 * @param <T> the registered data type
 * @param dataClass the non-null registered class
 * @param side the logical side whose effective data changed
 * @param cause the non-null source of the update
 * @param data the effective data keyed by source ID
 */
public record DataChangedEvent<T>(Class<T> dataClass, LogicalSide side, Cause cause,
                                  Map<Identifier, T> data) implements Event {
    /** Identifies the operation that produced an effective-data snapshot. */
    public enum Cause {
        /** A resource load completed, including the initial load. */
        RELOAD,
        /** An explicit retry accepted at least one pending entry. */
        DEFERRED_RESOLVED,
        /** The client received an authoritative server snapshot. */
        REMOTE_SYNC
    }

    /**
     * Creates an event containing an unmodifiable copy of the effective-data map.
     *
     * @param dataClass the registered class
     * @param side the logical side of the update
     * @param cause the operation that produced the update
     * @param data the non-null effective-data map
     */
    public DataChangedEvent {
        data = Map.copyOf(data);
    }
}
