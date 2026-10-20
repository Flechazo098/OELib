package cc.sighs.oelib.forge.event;



@Deprecated(since = "0.3.0")
public class DataReloadEvent extends cc.sighs.oelib.event.forge.DataReloadEvent {
    @Deprecated(since = "0.3.0")
    public DataReloadEvent(Class<?> dataClass, int loadedCount, int invalidCount) {
        super(dataClass, loadedCount, invalidCount);
    }
    @Override
    @Deprecated(since = "0.3.0")
    public Class<?> getDataClass() { return super.getDataClass(); }
    @Override
    @Deprecated(since = "0.3.0")
    public int getLoadedCount() { return super.getLoadedCount(); }
    @Override
    @Deprecated(since = "0.3.0")
    public int getInvalidCount() { return super.getInvalidCount(); }
    @Override
    @Deprecated(since = "0.3.0")
    public boolean isDataType(Class<?> type) { return super.isDataType(type); }
}
