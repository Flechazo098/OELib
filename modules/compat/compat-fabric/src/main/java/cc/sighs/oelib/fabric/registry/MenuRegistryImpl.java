package cc.sighs.oelib.fabric.registry;

import cc.sighs.oelib.registry.api.ExtendedMenuProvider;
import cc.sighs.oelib.registry.api.ExtendedMenuTypeFactory;
import cc.sighs.oelib.registry.spi.IMenuRegistry;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.networking.v1.FriendlyByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.Nullable;
import java.util.function.Function;

@Deprecated(since = "0.3.0")
public class MenuRegistryImpl extends cc.sighs.oelib.registry.fabric.MenuRegistryImpl {
    @Deprecated(since = "0.3.0")
    public MenuRegistryImpl() {}
}
