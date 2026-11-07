package cc.sighs.oelib.config.ui.entries;

import cc.sighs.oelib.config.ui.screen.ConfigScreen;
import cc.sighs.oelib.config.ui.widget.ExpandAnimation;
import cc.sighs.oelib.config.util.ConfigGuiUtil;
import com.flechazo.hkt.Maybe;
import com.flechazo.optics.Fold;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A collapsible entry for editing a JSON array field.
 *
 * <p>When collapsed, only the field label and an expand arrow are visible.
 * When expanded, each element is rendered as an editable text box (or
 * checkbox for booleans), with up/down/delete buttons for reordering and
 * removal, and an add button to append new elements.
 */
public class ListEntry extends AbstractConfigEntry<JsonArray> {
    private final String keyPath;
    private final JsonObject working;
    private final int controlWidth;
    private final int rowHeight;
    private final Component label;
    private final Maybe<Component> tooltip;
    private final JsonArray array;
    private static final Fold<ListEntry, AbstractWidget> ROWS = Fold.sum(
            widgetFold(entry -> entry.valueBoxes),
            widgetFold(entry -> entry.toggleBoxes),
            widgetFold(entry -> entry.delButtons),
            widgetFold(entry -> entry.upButtons),
            widgetFold(entry -> entry.downButtons));
    private static final Fold<ListEntry, AbstractWidget> WIDGETS = AbstractConfigEntry.<ListEntry>widgetFold(
            entry -> Arrays.asList(entry.addBtn, entry.resetButton)).plus(ROWS);

    private final List<EditBox> valueBoxes = new ArrayList<>();
    private final List<@Nullable Checkbox> toggleBoxes = new ArrayList<>();
    private final List<Button> delButtons = new ArrayList<>();
    private final List<Button> upButtons = new ArrayList<>();
    private final List<Button> downButtons = new ArrayList<>();
    private boolean created = false;
    private Screen screen;
    private @Nullable Button addBtn;
    private @Nullable Button resetButton;
    private JsonArray defaultArray;
    private final ExpandAnimation expansion = new ExpandAnimation(false);
    private int expX, expY, expW, expH;

    /**
     * Constructs a list entry.
     *
     * @param keyPath      the dotted path to the array field in the working JSON
     * @param label        the display label
     * @param working      the working JSON object
     * @param controlWidth the width of the edit controls
     * @param rowHeight    the row height
     * @param tooltip      the optional tooltip component
     */
    public ListEntry(
            String keyPath, Component label, JsonObject working,
            int controlWidth, int rowHeight, Maybe<Component> tooltip) {
        this.keyPath = keyPath;
        this.label = label;
        this.tooltip = tooltip;
        this.working = working;
        this.controlWidth = controlWidth;
        this.rowHeight = rowHeight;
        var value = ConfigGuiUtil.getPath(working, keyPath);
        this.array = value != null && value.isJsonArray() ? value.getAsJsonArray() : new JsonArray();
    }

    @Override
    public int getItemHeight() {
        return rowHeight + expansion.reveal(rowHeight * array.size());
    }

    /**
     * Attaches this entry to a screen, creating widgets.
     *
     * @param screen   the parent screen
     * @param x        the x position
     * @param y        the y position
     * @param defaults the default values JSON object
     */
    public void attach(Screen screen, int x, int y, JsonObject defaults) {
        if (created) return;
        this.screen = screen;
        int resetX = screen.width - 80;

        var def = ConfigGuiUtil.getPath(defaults, keyPath);
        defaultArray = def != null && def.isJsonArray() ? def.getAsJsonArray() : new JsonArray();

        addBtn = Button.builder(Component.literal("+"), b -> {
            array.add(new JsonPrimitive(""));
            ConfigGuiUtil.setPath(working, keyPath, array);
            rebuildRows(x, y, resetX);
            updateResetButtonState();
            if (screen instanceof ConfigScreen cs) cs.markDirty();
        }).bounds(resetX - 24, y, 22, 20).build();
        own(screen, addBtn);

        resetButton = Button.builder(Component.translatable("config.oelib.reset"), b -> {
            while (!array.isEmpty()) {
                array.remove(array.size() - 1);
            }
            for (JsonElement el : defaultArray) array.add(el);
            ConfigGuiUtil.setPath(working, keyPath, array);
            rebuildRows(x, y, resetX);
            updateResetButtonState();
            if (screen instanceof ConfigScreen cs) cs.markDirty();
        }).bounds(resetX, y, 72, 20).build();
        own(screen, resetButton);

        rebuildRows(x, y, resetX);
        updateResetButtonState();
        created = true;
    }

    private void rebuildRows(int x, int y, int resetX) {
        if (screen == null) return;

        clearWidgets();
        valueBoxes.clear();
        toggleBoxes.clear();
        delButtons.clear();
        upButtons.clear();
        downButtons.clear();

        int rowY = y + rowHeight;
        int vx = x + 40;
        int vw = Math.max(60, resetX - vx - 86);

        for (int i = 0; i < array.size(); i++) {
            var el = array.get(i);
            var vb = ConfigGuiUtil.createEditBox(ConfigGuiUtil.jsonToString(el), vx, rowY, vw);
            own(screen, vb);

            Checkbox cb = null;
            if (el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isBoolean()) {
                cb = Checkbox.builder(Component.empty(), Minecraft.getInstance().font)
                        .selected(el.getAsBoolean())
                        .onValueChange((box, selected) -> {
                            int idx = valueBoxes.indexOf(vb);
                            if (idx >= 0) {
                                array.set(idx, new JsonPrimitive(selected));
                                ConfigGuiUtil.setPath(working, keyPath, array);
                                updateResetButtonState();
                                if (screen instanceof ConfigScreen cs) cs.markDirty();
                            }
                        }).build();
                own(screen, cb);
            }

            int deleteX = resetX - 21;
            int downX = deleteX - 22;
            int upX = downX - 22;

            var up = Button.builder(Component.literal("▲"), b -> moveUp(vb)).bounds(upX, rowY, 20, 20).build();
            var down = Button.builder(Component.literal("▼"), b -> moveDown(vb)).bounds(downX, rowY, 20, 20).build();
            var del = Button.builder(Component.literal("x"), b -> removeItem(vb)).bounds(deleteX, rowY, 20, 20).build();

            own(screen, up);
            own(screen, down);
            own(screen, del);

            vb.setResponder(str -> {
                int idx = valueBoxes.indexOf(vb);
                if (idx >= 0) {
                    var prev = ConfigGuiUtil.jsonToString(array.get(idx));
                    if (!prev.equals(str)) {
                        array.set(idx, ConfigGuiUtil.parsePrimitive(str));
                        ConfigGuiUtil.setPath(working, keyPath, array);
                        updateResetButtonState();
                        if (screen instanceof ConfigScreen cs) cs.markDirty();
                    }
                }
            });

            valueBoxes.add(vb);
            toggleBoxes.add(cb);
            delButtons.add(del);
            upButtons.add(up);
            downButtons.add(down);
            rowY += rowHeight;
        }
    }

    private void moveUp(EditBox vb) {
        int idx = valueBoxes.indexOf(vb);
        if (idx > 0 && idx < array.size()) {
            var cur = array.get(idx);
            var prev = array.get(idx - 1);
            array.set(idx - 1, cur);
            array.set(idx, prev);
            ConfigGuiUtil.setPath(working, keyPath, array);
            rebuildRows(vb.getX() - 40, vb.getY() - rowHeight, vb.getX() + vb.getWidth() + 54);
            updateResetButtonState();
            if (screen instanceof ConfigScreen cs) cs.markDirty();
        }
    }

    private void moveDown(EditBox vb) {
        int idx = valueBoxes.indexOf(vb);
        if (idx >= 0 && idx < array.size() - 1) {
            var cur = array.get(idx);
            var next = array.get(idx + 1);
            array.set(idx + 1, cur);
            array.set(idx, next);
            ConfigGuiUtil.setPath(working, keyPath, array);
            rebuildRows(vb.getX() - 40, vb.getY() - rowHeight, vb.getX() + vb.getWidth() + 54);
            updateResetButtonState();
            if (screen instanceof ConfigScreen cs) cs.markDirty();
        }
    }

    private void removeItem(EditBox vb) {
        int idx = valueBoxes.indexOf(vb);
        if (idx >= 0 && idx < array.size()) {
            array.remove(idx);
            ConfigGuiUtil.setPath(working, keyPath, array);
            rebuildRows(vb.getX() - 40, vb.getY() - rowHeight, vb.getX() + vb.getWidth() + 54);
            updateResetButtonState();
            if (screen instanceof ConfigScreen cs) cs.markDirty();
        }
    }

    private void clearWidgets() {
        for (AbstractWidget widget : ROWS.getAll(this)) {
            screen.removeWidget(widget);
        }
    }

    private void updateResetButtonState() {
        if (resetButton == null) return;
        resetButton.active = defaultArray != null && !array.equals(defaultArray);
    }

    /**
     * Toggles the expanded state if the given coordinates hit the arrow icon.
     *
     * @param mx the mouse x
     * @param my the mouse y
     * @return {@code true} if toggled
     */
    public boolean toggleIfHit(double mx, double my) {
        if (mx >= expX && mx <= expX + expW && my >= expY && my <= expY + expH) {
            expansion.toggle();
            return true;
        }
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        graphics.drawString(Minecraft.getInstance().font, label, x + 4, y + 6, 0xFFFFFFFF);
        if (isHovered && screen instanceof ConfigScreen cs) {
            if (mouseY >= y && mouseY <= y + rowHeight) {
                tooltip.ifPresent(value -> cs.setHoverTooltip(value, mouseX, mouseY));
            }
        }
        int resetX = Minecraft.getInstance().screen.width - 80;
        int addX = resetX - 24;
        expX = addX - 12;
        expY = y + 2;
        expW = 12;
        expH = 16;
        expansion.drawArrow(graphics, Minecraft.getInstance().font, expX + 2, expY + 4, 0xFFFFFFFF);

        int revealedBottom = y + Math.min(getItemHeight(), shownHeight());
        boolean headerShown = revealedBottom > y;
        if (addBtn != null) {
            addBtn.visible = headerShown;
            addBtn.setX(addX);
            addBtn.setY(y);
        }
        if (resetButton != null) {
            resetButton.visible = headerShown;
            resetButton.setX(resetX);
            resetButton.setY(y);
        }

        int rowY = y + rowHeight;
        int vx = x + 40;
        int topBound = 0;
        int bottomBound = Minecraft.getInstance().screen.height - 32;
        if (screen instanceof ConfigScreen cs) {
            topBound = cs.getContentTop();
            bottomBound = cs.getContentBottom();
        }
        for (int i = 0; i < valueBoxes.size(); i++) {
            var vb = valueBoxes.get(i);
            var cb = toggleBoxes.get(i);
            var del = delButtons.get(i);
            var up = upButtons.get(i);
            var down = downButtons.get(i);
            boolean visible = rowY < revealedBottom
                    && rowY + rowHeight > topBound && rowY <= bottomBound - 20;
            vb.visible = visible && cb == null;
            if (cb != null) cb.visible = visible;
            del.visible = visible;
            up.visible = visible;
            down.visible = visible;
            if (visible) {
                int vw = Math.max(60, resetX - vx - 86);
                if (cb == null) {
                    vb.setX(vx);
                    vb.setY(rowY);
                    vb.setWidth(vw);
                } else {
                    cb.setX(vx);
                    cb.setY(rowY);
                    cb.setWidth(20);
                }
                int deleteX = resetX - 21;
                int downX = deleteX - 22;
                int upX = downX - 22;
                del.setX(deleteX);
                del.setY(rowY);
                down.setX(downX);
                down.setY(rowY);
                up.setX(upX);
                up.setY(rowY);
            }
            rowY += rowHeight;
        }
    }

    @Override
    public boolean overflows() {
        return expansion.progress() > 0.0 && expansion.progress() < 1.0;
    }

    @Override
    public void renderWidgets(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderAll(graphics, mouseX, mouseY, delta, addBtn, resetButton);
        renderAll(graphics, mouseX, mouseY, delta, valueBoxes);
        renderAll(graphics, mouseX, mouseY, delta, toggleBoxes);
        renderAll(graphics, mouseX, mouseY, delta, delButtons);
        renderAll(graphics, mouseX, mouseY, delta, upButtons);
        renderAll(graphics, mouseX, mouseY, delta, downButtons);
    }

    @Override
    public List<? extends AbstractWidget> widgets() {
        return WIDGETS.getAll(this);
    }

    @Override
    public Component getFieldName() {
        return label;
    }

    @Override
    public JsonArray getValue() {
        return array;
    }

    @Override
    public Maybe<JsonArray> getDefaultValue() {
        return Maybe.some(array);
    }
}
