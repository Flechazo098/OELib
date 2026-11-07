package cc.sighs.oelib.config.ui.widget;

import cc.sighs.oelib.config.ui.entries.AbstractConfigEntry;
import cc.sighs.oelib.config.ui.entries.PathGroupEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * A smooth-scrolling list widget for the auto-generated config screen.
 *
 * <p>Extends {@link DynamicElementListWidget} with a frame-rate-independent
 * scrolling implementation that uses exponential smoothing for a natural
 * feel. The scroll bounds are recalculated on every frame based on the
 * total height of all child entries.
 */
public class DynamicEntryListWidget extends DynamicElementListWidget {
    /** The texture id for the vertical header separator. */
    public static final ResourceLocation VERTICAL_HEADER_SEPARATOR = new ResourceLocation("minecraft", "textures/gui/menu_list_background.png");
    private final Scroller scroller = new Scroller();
    private int[] shown = new int[0];
    private final Map<String, PathGroupEntry> groups = new HashMap<>();
    private int groupsBuiltFor = -1;
    private int cursor;

    /**
     * Constructs a scrolling list widget.
     *
     * @param minecraft the Minecraft instance
     * @param width     the total width
     * @param height    the total height
     * @param top       the top y coordinate of the visible area
     * @param bottom    the bottom y coordinate of the visible area
     */
    public DynamicEntryListWidget(Minecraft minecraft, int width, int height, int top, int bottom) {
        super(minecraft, width, height, top, bottom);
    }

    /**
     * Scrolls to the given pixel offset.
     *
     * @param pixels   the target scroll offset
     * @param animated {@code true} for smooth scrolling, {@code false} for instant
     */
    public void scrollTo(int pixels, boolean animated) {
        scroller.scrollTo(pixels, animated);
    }

    /**
     * Offsets the scroll position by the given amount.
     *
     * @param pixels the offset, positive to scroll down
     */
    public void offset(double pixels) {
        scroller.offset(pixels);
    }

    /**
     * Returns the current scroll offset in pixels.
     *
     * @return the scroll offset
     */
    public int getScrollOffset() {
        return scroller.currentInt();
    }

    public double getScrollFraction() {
        return scroller.fraction();
    }

    public void updateScroll() {
        refreshScrollBounds();
        scroller.update();
    }

    /**
     * Returns the target scroll offset (may differ during smooth scrolling).
     *
     * @return the target scroll offset
     */
    public int getScrollTargetOffset() {
        return scroller.targetInt();
    }

    /**
     * Recalculates the scroll bounds based on the total height of all
     * child entries.
     */
    public void refreshScrollBounds() {
        layout();
        int totalHeight = 0;
        for (int height : shown) {
            totalHeight += height;
        }
        int maxScroll = Math.max(0, totalHeight - (bottom - top));
        scroller.setMax(maxScroll);
    }

    public int shownHeight(int index) {
        return index < shown.length ? shown[index] : children.get(index).getItemHeight();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int y = top - scroller.currentInt();
        int screenWidth = graphics.guiWidth();
        for (int i = 0; i < children.size(); i++) {
            AbstractConfigEntry<?> entry = children.get(i);
            int full = entry.getItemHeight();
            int h = shown[i];
            boolean clipped = h < full || entry.overflows();
            entry.setShownHeight(h);
            entry.setClip(y, y + h, clipped);
            boolean hovered = h > 0 && mouseY >= y && mouseY < y + h && mouseX >= left && mouseX < left + width
                    && mouseY >= top && mouseY < bottom;
            if (clipped) graphics.enableScissor(0, y - 1, screenWidth, y + h);
            entry.render(graphics, i, y, left + 12, getItemWidth(), full, mouseX, mouseY, hovered, delta);
            entry.renderWidgets(graphics, mouseX, mouseY, delta);
            if (clipped) graphics.disableScissor();
            y += h;
        }
    }

    private void layout() {
        int count = children.size();
        if (shown.length != count) shown = new int[count];
        if (groupsBuiltFor != count) {
            groups.clear();
            for (AbstractConfigEntry<?> entry : children) {
                if (entry instanceof PathGroupEntry group) groups.put(group.stateKey(), group);
            }
            groupsBuiltFor = count;
        }
        cursor = 0;
        place(Integer.MAX_VALUE, null);
    }

    private int place(int allowance, @Nullable String owner) {
        int used = 0;
        while (cursor < children.size() && inside(children.get(cursor), owner)) {
            int index = cursor++;
            AbstractConfigEntry<?> entry = children.get(index);
            int room = Math.max(0, allowance - used);
            int height = Math.min(entry.getItemHeight(), room);
            shown[index] = height;
            used += height;
            if (entry instanceof PathGroupEntry group) {
                int contents = naturalHeight(cursor, group.stateKey());
                int open = (int) Math.round(contents * group.progress());
                used += place(Math.min(open, Math.max(0, allowance - used)), group.stateKey());
            }
        }
        return used;
    }

    private int naturalHeight(int start, String owner) {
        int height = 0;
        int index = start;
        while (index < children.size() && inside(children.get(index), owner)) {
            AbstractConfigEntry<?> entry = children.get(index++);
            height += entry.getItemHeight();
            if (entry instanceof PathGroupEntry group) {
                height += (int) Math.round(naturalHeight(index, group.stateKey()) * group.progress());
                while (index < children.size() && inside(children.get(index), group.stateKey())) index++;
            }
        }
        return height;
    }

    private boolean inside(AbstractConfigEntry<?> entry, @Nullable String owner) {
        if (owner == null) return true;
        for (String key = entry.groupKey(); key != null; ) {
            if (key.equals(owner)) return true;
            PathGroupEntry parent = groups.get(key);
            key = parent == null ? null : parent.groupKey();
        }
        return false;
    }

    private static final class Scroller {
        private static final double OMEGA = 22.0;
        private static final double SNAP_EPSILON = 0.01;
        private static final double SNAP_VELOCITY = 0.5;

        private double value;
        private double velocity;
        private double target;
        private int max;
        private long lastUpdateNanos = System.nanoTime();

        void setMax(int max) {
            this.max = Math.max(0, max);
            clampAll();
        }

        void scrollTo(double t, boolean animated) {
            this.target = t;
            if (!animated) {
                this.value = t;
                this.velocity = 0.0;
            }
            clampAll();
        }

        void offset(double delta) {
            this.target = clamp(this.target + delta);
        }

        void update() {
            long now = System.nanoTime();
            double dt = (now - this.lastUpdateNanos) / 1_000_000_000.0;
            this.lastUpdateNanos = now;
            if (dt <= 0.0) {
                return;
            }
            dt = Math.min(dt, 0.25);
            double d = value - target;
            double c = velocity + OMEGA * d;
            double e = Math.exp(-OMEGA * dt);
            value = target + (d + c * dt) * e;
            velocity = (c - OMEGA * (d + c * dt)) * e;
            if (Math.abs(value - target) < SNAP_EPSILON && Math.abs(velocity) < SNAP_VELOCITY) {
                value = target;
                velocity = 0.0;
            }
            double clamped = clamp(value);
            if (clamped != value) {
                value = clamped;
                velocity = 0.0;
            }
        }

        int currentInt() {
            return (int) Math.floor(value);
        }

        double fraction() {
            return value - Math.floor(value);
        }

        int targetInt() {
            return (int) Math.round(target);
        }

        private double clamp(double v) {
            return Math.min(max, Math.max(0, v));
        }

        private void clampAll() {
            target = clamp(target);
            double clamped = clamp(value);
            if (clamped != value) {
                value = clamped;
                velocity = 0.0;
            }
        }
    }
}
