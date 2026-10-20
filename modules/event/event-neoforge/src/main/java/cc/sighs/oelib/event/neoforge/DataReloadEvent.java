package cc.sighs.oelib.event.neoforge;


import net.neoforged.bus.api.Event;

public class DataReloadEvent extends Event {

    @org.jetbrains.annotations.ApiStatus.Internal
    public static DataReloadEvent create(Class<?> dataClass, int loadedCount, int invalidCount) {
        return FactoryHolder.INSTANCE.create(dataClass, loadedCount, invalidCount);
    }

    @org.jetbrains.annotations.ApiStatus.Internal
    public interface Factory {
        DataReloadEvent create(Class<?> dataClass, int loadedCount, int invalidCount);
    }

    private static final class FactoryHolder {
        private static final Factory INSTANCE = java.util.ServiceLoader.load(Factory.class)
                .findFirst().orElse(DataReloadEvent::new);
    }

    private final Class<?> dataClass;
    private final int loadedCount;
    private final int invalidCount;

    public DataReloadEvent(Class<?> dataClass, int loadedCount, int invalidCount) {
        this.dataClass = dataClass;
        this.loadedCount = loadedCount;
        this.invalidCount = invalidCount;
    }

    public Class<?> getDataClass() {
        return dataClass;
    }

    public int getLoadedCount() {
        return loadedCount;
    }

    public int getInvalidCount() {
        return invalidCount;
    }

    public boolean isDataType(Class<?> clazz) {
        return dataClass.equals(clazz);
    }
}