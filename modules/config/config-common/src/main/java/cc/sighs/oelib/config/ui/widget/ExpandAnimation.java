package cc.sighs.oelib.config.ui.widget;

import org.joml.Matrix3x2fStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ExpandAnimation {
    public static final long DURATION_NANOS = 200_000_000L;

    private boolean expanded;
    private double from;
    private double to;
    private long start;

    public ExpandAnimation(boolean expanded) {
        this.expanded = expanded;
        this.from = this.to = expanded ? 1.0 : 0.0;
    }

    public boolean expanded() {
        return expanded;
    }

    public void set(boolean value) {
        if (value == expanded) return;
        from = progress();
        to = value ? 1.0 : 0.0;
        expanded = value;
        start = System.nanoTime();
    }

    public void toggle() {
        set(!expanded);
    }

    public double progress() {
        double t = (System.nanoTime() - start) / (double) DURATION_NANOS;
        if (t >= 1.0) return to;
        double eased = 1.0 - Math.pow(1.0 - Math.max(0.0, t), 3.0);
        return from + (to - from) * eased;
    }

    public int reveal(int fullHeight) {
        return (int) Math.round(fullHeight * progress());
    }

    public void drawArrow(GuiGraphicsExtractor graphics, Font font, int x, int y, int colour) {
        String arrow = "▸";
        float halfWidth = font.width(arrow) / 2.0F;
        float halfHeight = (font.lineHeight - 1) / 2.0F;
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x + halfWidth, y + halfHeight);
        pose.rotate((float) (Math.PI / 2.0 * progress()));
        pose.translate(-halfWidth, -halfHeight);
        graphics.text(font, arrow, 0, 0, colour);
        pose.popMatrix();
    }
}
