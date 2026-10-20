package cc.sighs.oelib.compat.neoforge;



@Deprecated(since = "0.3.0")
public final class LegacyDataReloadEventFactory implements cc.sighs.oelib.event.neoforge.DataReloadEvent.Factory {
    public LegacyDataReloadEventFactory() {}
    @Override
    public cc.sighs.oelib.event.neoforge.DataReloadEvent create(Class<?> type, int loaded, int invalid) {
        return new cc.sighs.oelib.neoforge.event.DataReloadEvent(type, loaded, invalid);
    }
}
