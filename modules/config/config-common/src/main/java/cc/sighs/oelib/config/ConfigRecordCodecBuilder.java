package cc.sighs.oelib.config;

import cc.sighs.oelib.config.field.ConfigField;
import cc.sighs.oelib.config.model.*;
import cc.sighs.oelib.config.util.ConfigCodecUtil;
import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

@Deprecated(since = "0.3.0")
public final class ConfigRecordCodecBuilder {
    private ConfigRecordCodecBuilder() {}

    @Deprecated(since = "0.3.0")
    public static <T> ConfigUnit<T> createClient(Identifier id,
            Function<RecordCodecBuilder.Instance<T>, ? extends App<RecordCodecBuilder.Mu<T>, T>> builder,
            Consumer<ConfigMeta.Builder> customizer) {
        return create(id, builder, customizer, ConfigSide.CLIENT);
    }

    @Deprecated(since = "0.3.0")
    public static <T> ConfigUnit<T> create(Identifier id,
            Function<RecordCodecBuilder.Instance<T>, ? extends App<RecordCodecBuilder.Mu<T>, T>> builder,
            Consumer<ConfigMeta.Builder> customizer) {
        return create(id, builder, customizer, ConfigSide.SERVER);
    }

    private static <T> ConfigUnit<T> create(Identifier id,
            Function<RecordCodecBuilder.Instance<T>, ? extends App<RecordCodecBuilder.Mu<T>, T>> builder,
            Consumer<ConfigMeta.Builder> customizer, ConfigSide side) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(builder, "builder");
        List<ConfigValueMeta> previousFields = ConfigField.CURRENT_FIELDS.get();
        Identifier previousId = ConfigField.CURRENT_CONFIG_ID.get();
        List<ConfigValueMeta> fields = new ArrayList<>();
        try {
            ConfigField.CURRENT_FIELDS.set(fields);
            ConfigField.CURRENT_CONFIG_ID.set(id);
            var codec = RecordCodecBuilder.create(builder);
            var meta = ConfigMeta.builder(id).format(ConfigStorageFormat.TOML).side(side);
            if (customizer != null) customizer.accept(meta);
            var configCodec = new ConfigCodec<>(codec, meta.build(), List.copyOf(fields));
            return ConfigUnit.of(configCodec, ConfigCodecUtil.deriveDefault(codec));
        } finally {
            if (previousFields == null) ConfigField.CURRENT_FIELDS.remove();
            else ConfigField.CURRENT_FIELDS.set(previousFields);
            if (previousId == null) ConfigField.CURRENT_CONFIG_ID.remove();
            else ConfigField.CURRENT_CONFIG_ID.set(previousId);
        }
    }
}
