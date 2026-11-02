package cc.sighs.oelib.renderer.neoforge;

import cc.sighs.oelib.renderer.FluidRef;
import cc.sighs.oelib.renderer.FluidRenderAttributes;
import cc.sighs.oelib.renderer.spi.IFluidRenderers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

public final class FluidRenderersImpl implements IFluidRenderers {
    @Override
    public FluidRenderAttributes resolve(FluidRef ref) {
        if (ref == null) return null;
        var fluid = ref.getFluid();
        if (fluid == null || fluid == Fluids.EMPTY) return null;
        var props = IClientFluidTypeExtensions.of(fluid);
        FluidStack stack = new FluidStack(fluid, 1000);
        var stillTex = props.getStillTexture(stack);
        if (stillTex == null) return null;
        var sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(stillTex);
        if (sprite == null || MissingTextureAtlasSprite.getLocation().equals(sprite.contents().name())) {
            return null;
        }
        int color = props.getTintColor(stack);
        return new FluidRenderAttributes(sprite, color);
    }
}
