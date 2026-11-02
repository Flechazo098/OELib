package cc.sighs.oelib.data.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

/**
 * Interface for data validators.
 * <p>
 * Implement this interface to provide custom validation logic for data-driven types.
 * </p>
 *
 * @param <T> the data type
 */
public interface DataValidator<T> {

    /**
     * Validates the correctness of the given data.
     *
     * @param data   the data to validate
     * @param source the file location from which the data originates
     * @return the validation result
     */
    ValidationResult validate(T data, ResourceLocation source);

    /**
     * Context-aware data validator interface.
     * <p>
     * Validators implementing this interface can access the server instance,
     * enabling them to use contextual information such as registry lookups.
     * </p>
     *
     * @param <T> the data type
     */
    interface ServerContextAware<T> extends DataValidator<T> {

        /**
         * Validates data using server context.
         *
         * @param data   the data to validate
         * @param source the file location from which the data originates
         * @param server the server instance (may be null, e.g., on client side)
         * @return the validation result
         */
        ValidationResult validateWithContext(T data, ResourceLocation source, MinecraftServer server);

        /**
         * Default implementation: uses context-aware validation if a server instance is available;
         * otherwise falls back to basic validation.
         */
        @Override
        default ValidationResult validate(T data, ResourceLocation source) {
            return validateWithContext(data, source, null);
        }
    }

    /**
     * Represents the result of validating one data entry.
     *
     * @param valid whether the entry is accepted or retained for deferred validation
     * @param message the failure or deferral reason, or {@code null}
     * @param deferrable whether an otherwise valid entry requires a later validation attempt
     */
    record ValidationResult(boolean valid, String message, boolean deferrable) {

        /**
         * Creates a result accepting the entry as effective data.
         *
         * @return a valid result that requires no further validation
         */
        public static ValidationResult success() {
            return new ValidationResult(true, null, false);
        }

        /**
         * Creates a failed validation result.
         *
         * @param message the failure reason, or {@code null}
         * @return an invalid result that does not retain the entry for retry
         */
        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message, false);
        }

        /**
         * Creates a result retaining the entry for a later validation attempt.
         *
         * <p>The entry remains pending until an explicit retry accepts or rejects it,
         * or a subsequent load or server shutdown discards it. Pending entries are
         * excluded from effective-data snapshots and client synchronization.
         * Returning this result does not schedule a retry.
         *
         * @param message the deferral reason, or {@code null} for a default reason
         * @return a valid result marked for deferred validation
         * @see DataDeferredEvent
         * @see DeferredBatch
         */
        public static ValidationResult deferred(String message) {
            return new ValidationResult(true, message, true);
        }
    }

    /**
     * Default no-op validator implementation.
     */
    class NoValidator implements DataValidator<Object> {
        @Override
        public ValidationResult validate(Object data, ResourceLocation source) {
            return ValidationResult.success();
        }
    }
}