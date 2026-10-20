package cc.sighs.oelib.icon;

import java.util.List;
import java.util.Optional;

@Deprecated(since = "0.3.0")
public final class DynamicIconRegistry {
    private DynamicIconRegistry() {}
    @Deprecated(since = "0.3.0")
    public static Builder forMod(String modId) { return new Builder(cc.sighs.oelib.misc.icon.DynamicIconRegistry.forMod(modId)); }
    @Deprecated(since = "0.3.0")
    public static boolean hasConfig(String modId) { return cc.sighs.oelib.misc.icon.DynamicIconRegistry.hasConfig(modId); }
    @Deprecated(since = "0.3.0")
    public static void register(String modId, List<IconVariant> variants) {
        cc.sighs.oelib.misc.icon.DynamicIconRegistry.register(modId, variants == null ? null : variants.stream().map(value -> value.delegate).toList());
    }
    @Deprecated(since = "0.3.0")
    public static Optional<String> getSelectedFabricPath(String modId) { return cc.sighs.oelib.misc.icon.DynamicIconRegistry.getSelectedFabricPath(modId); }
    @Deprecated(since = "0.3.0")
    public static Optional<String> getSelectedForgePath(String modId) { return cc.sighs.oelib.misc.icon.DynamicIconRegistry.getSelectedForgePath(modId); }
    @Deprecated(since = "0.3.0")
    public static final class Builder {
        private final cc.sighs.oelib.misc.icon.DynamicIconRegistry.Builder delegate;
        private Builder(cc.sighs.oelib.misc.icon.DynamicIconRegistry.Builder delegate) { this.delegate = delegate; }
        @Deprecated(since = "0.3.0")
        public Builder addName(String value, int weight) { delegate.addName(value, weight); return this; }
        @Deprecated(since = "0.3.0")
        public Builder addPath(String value, int weight) { delegate.addPath(value, weight); return this; }
        @Deprecated(since = "0.3.0")
        public Builder addNameFabric(String value, int weight) { delegate.addNameFabric(value, weight); return this; }
        @Deprecated(since = "0.3.0")
        public Builder addPathFabric(String value, int weight) { delegate.addPathFabric(value, weight); return this; }
        @Deprecated(since = "0.3.0")
        public Builder addNameForge(String value, int weight) { delegate.addNameForge(value, weight); return this; }
        @Deprecated(since = "0.3.0")
        public Builder addPathForge(String value, int weight) { delegate.addPathForge(value, weight); return this; }
        @Deprecated(since = "0.3.0")
        public void register() { delegate.register(); }
    }
}
