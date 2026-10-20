package cc.sighs.oelib.icon;



@Deprecated(since = "0.3.0")
public final class IconVariant {
    @Deprecated(since = "0.3.0")
    public enum Platform { COMMON, FABRIC, NeoForge }
    final cc.sighs.oelib.misc.icon.IconVariant delegate;
    private IconVariant(cc.sighs.oelib.misc.icon.IconVariant delegate) { this.delegate = delegate; }
    @Deprecated(since = "0.3.0")
    public static IconVariant name(String value, int weight) { return new IconVariant(cc.sighs.oelib.misc.icon.IconVariant.name(value, weight)); }
    @Deprecated(since = "0.3.0")
    public static IconVariant path(String value, int weight) { return new IconVariant(cc.sighs.oelib.misc.icon.IconVariant.path(value, weight)); }
    @Deprecated(since = "0.3.0")
    public static IconVariant nameFabric(String value, int weight) { return new IconVariant(cc.sighs.oelib.misc.icon.IconVariant.nameFabric(value, weight)); }
    @Deprecated(since = "0.3.0")
    public static IconVariant pathFabric(String value, int weight) { return new IconVariant(cc.sighs.oelib.misc.icon.IconVariant.pathFabric(value, weight)); }
    @Deprecated(since = "0.3.0")
    public static IconVariant nameNeoForge(String value, int weight) { return new IconVariant(cc.sighs.oelib.misc.icon.IconVariant.nameNeoForge(value, weight)); }
    @Deprecated(since = "0.3.0")
    public static IconVariant pathNeoForge(String value, int weight) { return new IconVariant(cc.sighs.oelib.misc.icon.IconVariant.pathNeoForge(value, weight)); }
    @Deprecated(since = "0.3.0")
    public String getName() { return delegate.getName(); }
    @Deprecated(since = "0.3.0")
    public String getPath() { return delegate.getPath(); }
    @Deprecated(since = "0.3.0")
    public int getWeight() { return delegate.getWeight(); }
    @Deprecated(since = "0.3.0")
    public Platform getPlatform() { return Platform.valueOf(delegate.getPlatform().name()); }
}
