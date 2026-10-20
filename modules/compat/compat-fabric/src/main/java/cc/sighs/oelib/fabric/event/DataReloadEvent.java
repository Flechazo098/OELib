package cc.sighs.oelib.fabric.event;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.resources.ResourceLocation;

@Deprecated(since = "0.3.0")
public interface DataReloadEvent extends cc.sighs.oelib.event.fabric.DataReloadEvent {
    @Deprecated(since = "0.3.0")
    Event<DataReloadEvent> EVENT = new Event<>() {
        { invoker = (type, loaded, invalid) -> cc.sighs.oelib.event.fabric.DataReloadEvent.EVENT.invoker().onDataReload(type, loaded, invalid); }
        @Override
        public void register(DataReloadEvent listener) { cc.sighs.oelib.event.fabric.DataReloadEvent.EVENT.register(listener); }
        @Override
        public void register(ResourceLocation phase, DataReloadEvent listener) { cc.sighs.oelib.event.fabric.DataReloadEvent.EVENT.register(phase, listener); }
        @Override
        public void addPhaseOrdering(ResourceLocation first, ResourceLocation second) { cc.sighs.oelib.event.fabric.DataReloadEvent.EVENT.addPhaseOrdering(first, second); }
    };

    @Override
    @Deprecated(since = "0.3.0")
    void onDataReload(Class<?> dataClass, int loadedCount, int invalidCount);
}
