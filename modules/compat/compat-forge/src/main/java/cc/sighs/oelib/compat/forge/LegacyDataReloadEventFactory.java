package cc.sighs.oelib.compat.forge;



@Deprecated(since = "0.3.0")
public final class LegacyDataReloadEventFactory implements cc.sighs.oelib.event.forge.DataReloadEvent.Factory {
    public LegacyDataReloadEventFactory() {}
    @Override
    public cc.sighs.oelib.event.forge.DataReloadEvent create(Class<?> type, int loaded, int invalid) {
        return new cc.sighs.oelib.forge.event.DataReloadEvent(type, loaded, invalid);
    }
}
