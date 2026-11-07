package cc.sighs.oelib.config.ui.entries;

import cc.sighs.oelib.config.ui.screen.ConfigScreen;
import com.flechazo.hkt.Maybe;
import com.flechazo.optics.Fold;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Common base for entries in the auto-generated configuration screen.
 *
 * <p>Each entry represents one logical row: a field, a category header,
 * a divider, or a group toggle. Subclasses implement
 * {@link #render(GuiGraphics, int, int, int, int, int, int, int, boolean, float)}
 * to draw themselves and {@link #getItemHeight()} to declare their height.
 *
 * @param <T> the value type associated with this entry
 */
public abstract class AbstractConfigEntry<T> implements GuiEventListener, NarratableEntry {
    @Nullable
    protected Consumer<T> saveCallback;
    @Nullable
    private String groupKey;
    private int shownHeight = -1;
    private int clipTop;
    private int clipBottom = Integer.MAX_VALUE;
    private boolean clipping;

    @Nullable
    public String groupKey() {
        return groupKey;
    }

    public void setGroupKey(@Nullable String groupKey) {
        this.groupKey = groupKey;
    }

    public void setShownHeight(int shownHeight) {
        this.shownHeight = shownHeight;
    }

    protected int shownHeight() {
        return shownHeight < 0 ? getItemHeight() : shownHeight;
    }

    public void setClip(int top, int bottom, boolean clipping) {
        this.clipTop = top;
        this.clipBottom = bottom;
        this.clipping = clipping;
    }

    public int clipTop() {
        return clipTop;
    }

    public int clipBottom() {
        return clipBottom;
    }

    public boolean clipping() {
        return clipping;
    }

    public boolean overflows() {
        return false;
    }

    public List<? extends AbstractWidget> widgets() {
        return Collections.emptyList();
    }

    public void renderWidgets(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    }

    protected static <S> Fold<S, AbstractWidget> widgetFold(Function<? super S, ? extends Iterable<? extends @Nullable AbstractWidget>> getAll) {
        Function<S, Iterable<AbstractWidget>> present = source -> {
            List<AbstractWidget> widgets = new ArrayList<>();
            for (AbstractWidget widget : getAll.apply(source)) {
                if (widget != null) widgets.add(widget);
            }
            return widgets;
        };
        return Fold.of(present);
    }

    protected static void renderAll(GuiGraphics graphics, int mouseX, int mouseY, float delta, List<? extends AbstractWidget> widgets) {
        for (AbstractWidget widget : widgets) {
            if (widget != null) widget.render(graphics, mouseX, mouseY, delta);
        }
    }

    protected static void renderAll(GuiGraphics graphics, int mouseX, int mouseY, float delta,
                                    @Nullable AbstractWidget first, @Nullable AbstractWidget second) {
        if (first != null) first.render(graphics, mouseX, mouseY, delta);
        if (second != null) second.render(graphics, mouseX, mouseY, delta);
    }

    protected static void renderAll(GuiGraphics graphics, int mouseX, int mouseY, float delta,
                                    @Nullable AbstractWidget first, @Nullable AbstractWidget second, @Nullable AbstractWidget third) {
        renderAll(graphics, mouseX, mouseY, delta, first, second);
        if (third != null) third.render(graphics, mouseX, mouseY, delta);
    }

    protected static <W extends AbstractWidget> W own(Screen screen, W widget) {
        return screen instanceof ConfigScreen configScreen ? configScreen.addEntryWidget(widget) : screen.addRenderableWidget(widget);
    }

    /**
     * Returns the height of this entry in pixels.
     *
     * @return the item height
     */
    public abstract int getItemHeight();

    /**
     * Renders this entry at the given position.
     *
     * @param graphics    the graphics context
     * @param index       the index of this entry in the list
     * @param y           the y coordinate of the top of this entry
     * @param x           the x coordinate of the left of this entry
     * @param entryWidth  the available width for this entry
     * @param entryHeight the height of this entry
     * @param mouseX      the current mouse x
     * @param mouseY      the current mouse y
     * @param isHovered   {@code true} if the mouse is over this entry
     * @param delta       the partial tick delta
     */
    public abstract void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta);

    /**
     * Renders overlay elements for this entry, called after all entries
     * have been rendered.
     *
     * @param graphics the graphics context
     * @param mouseX   the current mouse x
     * @param mouseY   the current mouse y
     * @param delta    the partial tick delta
     */
    public void lateRender(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    }

    /**
     * Returns the display name for this entry.
     *
     * @return the field name component
     */
    public abstract Component getFieldName();

    /**
     * Returns the current value of this entry.
     *
     * @return the value
     */
    public abstract T getValue();

    /**
     * Returns the default value of this entry.
     *
     * @return the default value, or an empty value
     */
    public abstract Maybe<T> getDefaultValue();

    /**
     * Returns a configuration error message, if any.
     *
     * @return the error message, or an empty value
     */
    public Maybe<Component> getConfigError() {
        return Maybe.none();
    }

    /**
     * Returns {@code true} if this entry has been edited from its original
     * state or has a configuration error.
     *
     * @return {@code true} if edited
     */
    public boolean isEdited() {
        return getConfigError().isDefined();
    }

    /**
     * Returns child GUI event listeners for this entry.
     *
     * @return the children, or an empty list
     */
    public List<? extends GuiEventListener> children() {
        return Collections.emptyList();
    }

    /**
     * Returns child narratable entries.
     *
     * @return the narratables, or an empty list
     */
    public List<? extends NarratableEntry> narratables() {
        return Collections.emptyList();
    }

    /**
     * Persists the current value via the registered save callback.
     */
    public void save() {
        if (saveCallback != null) {
            saveCallback.accept(getValue());
        }
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    public void setFocused(boolean focused) {
    }

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {
    }
}
