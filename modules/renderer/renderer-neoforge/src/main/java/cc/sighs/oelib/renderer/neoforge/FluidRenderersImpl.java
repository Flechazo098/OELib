package cc.sighs.oelib.renderer.neoforge;

import cc.sighs.oelib.renderer.FluidRef;
import cc.sighs.oelib.renderer.FluidRenderAttributes;
import cc.sighs.oelib.renderer.FluidRenderers;
import cc.sighs.oelib.renderer.spi.IFluidRenderers;

public final class FluidRenderersImpl implements IFluidRenderers {
    @Override
    public FluidRenderAttributes resolve(FluidRef ref) {
        return FluidRenderers.resolveDefaultAttributes(ref.getFluid());
    }
}
