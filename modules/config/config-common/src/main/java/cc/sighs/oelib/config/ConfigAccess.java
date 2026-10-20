package cc.sighs.oelib.config;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.util.Objects;
import java.util.function.Function;

@Deprecated(since = "0.3.0")
public class ConfigAccess<T> {
    private final ConfigUnit<T> unit;

    @Deprecated(since = "0.3.0")
    public ConfigAccess(ConfigUnit<T> unit) {
        this.unit = Objects.requireNonNull(unit, "unit");
    }

    @Deprecated(since = "0.3.0")
    public <V> void set(Accessor<T, V> getter, V value) {
        Objects.requireNonNull(getter, "getter");
        Objects.requireNonNull(value, "value");
        T current = unit.get();
        try {
            var replacement = getter.getClass().getDeclaredMethod("writeReplace");
            replacement.setAccessible(true);
            var lambda = (SerializedLambda) replacement.invoke(getter);
            Class<?> type = current.getClass();
            if (!type.isRecord()) throw new IllegalArgumentException("ConfigAccess requires a record");
            var components = type.getRecordComponents();
            Class<?>[] types = new Class<?>[components.length];
            Object[] values = new Object[components.length];
            boolean found = false;
            for (int i = 0; i < components.length; i++) {
                var component = components[i];
                types[i] = component.getType();
                if (component.getName().equals(lambda.getImplMethodName())) {
                    values[i] = value;
                    found = true;
                } else {
                    var accessor = component.getAccessor();
                    accessor.setAccessible(true);
                    values[i] = accessor.invoke(current);
                }
            }
            if (!found) throw new IllegalArgumentException("Expected a record component method reference");
            var constructor = type.getDeclaredConstructor(types);
            constructor.setAccessible(true);
            @SuppressWarnings("unchecked") T updated = (T) constructor.newInstance(values);
            ConfigLifecycle.replace(unit, updated, true);
        } catch (ReflectiveOperationException error) {
            throw new IllegalArgumentException("Cannot resolve config accessor", error);
        }
    }

    @Deprecated(since = "0.3.0")
    @FunctionalInterface
    public interface Accessor<T, R> extends Function<T, R>, Serializable {}
}
