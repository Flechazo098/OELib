package cc.sighs.oelib.data.util.data;

import cc.sighs.oelib.data.OELibData;
import cc.sighs.oelib.data.api.DataValidator;
import cc.sighs.oelib.data.api.DeferredEntry;
import com.flechazo.hkt.Try;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@ApiStatus.Internal
public final class DataLoadPipeline {

    public static <T> Counts load(Class<T> dataClass, Codec<T> codec,
                                  Map<ResourceLocation, JsonElement> resources, boolean supportArray,
                                  BiFunction<T, ResourceLocation, DataValidator.ValidationResult> validator,
                                  BiConsumer<ResourceLocation, T> accepted,
                                  BiConsumer<ResourceLocation, DeferredEntry<T>> deferred) {
        int[] counts = new int[Disposition.values().length];
        resources.forEach((source, json) -> Try.run(() -> elements(source, json, supportArray)
                .forEach(entry -> {
                    var decoded = codec.parse(JsonOps.INSTANCE, entry.getValue());
                    var disposition = decoded.result().map(value -> {
                        var validation = validator.apply(value, entry.getKey());
                        var outcome = disposition(validation);
                        switch (outcome) {
                            case ACCEPTED -> accepted.accept(entry.getKey(), value);
                            case DEFERRED -> deferred.accept(entry.getKey(),
                                    new DeferredEntry<>(value, Objects.requireNonNullElse(
                                            validation.message(), "Validation deferred")));
                            case REJECTED -> OELibData.LOGGER.warn("Invalid {} data in {}: {}",
                                    dataClass.getSimpleName(), entry.getKey(), validation.message());
                        }
                        return outcome;
                    }).orElseGet(() -> {
                        OELibData.LOGGER.error("Failed to parse {} data from {}: {}",
                                dataClass.getSimpleName(), entry.getKey(), decoded.error().orElse(null));
                        return Disposition.REJECTED;
                    });
                    counts[disposition.ordinal()]++;
                })).peekFailure(error -> {
            counts[Disposition.REJECTED.ordinal()]++;
            OELibData.LOGGER.error("Error loading {} data from {}", dataClass.getSimpleName(), source, error);
        }));
        return new Counts(counts[Disposition.ACCEPTED.ordinal()], counts[Disposition.DEFERRED.ordinal()],
                counts[Disposition.REJECTED.ordinal()]);
    }

    private static Stream<Map.Entry<ResourceLocation, JsonElement>> elements(
            ResourceLocation source, JsonElement json, boolean supportArray) {
        if (!supportArray || !json.isJsonArray()) return Stream.of(Map.entry(source, json));
        var array = json.getAsJsonArray();
        return IntStream.range(0, array.size()).mapToObj(index -> Map.entry(
                ResourceLocation.fromNamespaceAndPath(source.getNamespace(), source.getPath() + "_" + index),
                array.get(index)));
    }

    private static Disposition disposition(DataValidator.ValidationResult validation) {
        if (!validation.valid()) return Disposition.REJECTED;
        return validation.deferrable() ? Disposition.DEFERRED : Disposition.ACCEPTED;
    }

    private enum Disposition {
        ACCEPTED,
        DEFERRED,
        REJECTED
    }

    public record Counts(int accepted, int deferred, int rejected) {
    }
}
