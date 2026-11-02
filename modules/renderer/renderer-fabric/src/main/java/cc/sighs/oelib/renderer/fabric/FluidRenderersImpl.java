package cc.sighs.oelib.renderer.fabric;

import cc.sighs.oelib.renderer.FluidRef;
import cc.sighs.oelib.renderer.FluidRenderAttributes;
import cc.sighs.oelib.renderer.spi.IFluidRenderers;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.world.level.material.Fluids;

@SuppressWarnings("UnstableApiUsage")
public class FluidRenderersImpl implements IFluidRenderers {
    @Override
    public FluidRenderAttributes resolve(FluidRef ref) {
        if (ref == null) return null;
        var fluid = ref.getFluid();
        if (fluid == null || fluid == Fluids.EMPTY) return null;
        var variant = FluidVariant.of(fluid);
        var sprite = FluidVariantRendering.getSprite(variant);
        if (sprite == null || MissingTextureAtlasSprite.getLocation().equals(sprite.contents().name())) {
            return null;
        }
        int color = FluidVariantRendering.getColor(variant);
        return new FluidRenderAttributes(sprite, color);
    }
}
