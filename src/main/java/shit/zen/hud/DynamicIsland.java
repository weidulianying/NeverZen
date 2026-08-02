package shit.zen.hud;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import net.minecraft.util.Mth;
import shit.zen.ClientBase;
import shit.zen.event.impl.Render2DEvent;
import shit.zen.render.Paint;
import shit.zen.render.Renderer;
import shit.zen.render.RoundedRectangle;
import shit.zen.utils.animation.SpringAnimation;
import shit.zen.utils.render.RenderUtil;

/** Dynamic Island — pill container at top-center. */
public class DynamicIsland {

    public static final class ActiveElementSelector {
        private final DynamicIsland owner;
        public ActiveElementSelector(DynamicIsland owner) { this.owner = owner; }
        public IHudElement visible() {
            for (IHudElement e : owner.elements) if (e.isVisible()) return e;
            return null;
        }
    }

    final List<IHudElement> elements = Arrays.asList(new TabListHud(), new ScaffoldHud(), new EventAlertHud(), new AutoPlayHud(), new WatermarkHud());
    private final ActiveElementSelector selector = new ActiveElementSelector(this);
    private final SpringAnimation widthAnim  = new SpringAnimation(300f, 1.2f, 20f, 170f);
    private final SpringAnimation heightAnim = new SpringAnimation(300f, 1.2f, 20f, 18f);
    private final SpringAnimation transAnim  = new SpringAnimation(250f, 1.0f, 22f, 1f);
    private IHudElement active, outgoing;
    private long lastTime;

    public void onRender2D(Render2DEvent e) {
        if (ClientBase.mc == null || ClientBase.mc.player == null) return;

        long now = System.currentTimeMillis();
        if (lastTime == 0L) lastTime = now;
        float dt = Math.min((now - lastTime) / 1000f, 0.033f);
        lastTime = now;

        IHudElement visible = selector.visible();
        if (active != visible) {
            outgoing = active; active = visible;
            transAnim.reset(0f); transAnim.setTargetValue(1f);
            if (outgoing == null) {
                IHudElement.Size sz = active.getHudAlignment();
                widthAnim.reset(sz.width()); heightAnim.reset(sz.height());
                transAnim.reset(1f);
            }
        }

        IHudElement.Size sz = active.getHudAlignment();
        float tw = sz.width(), th = sz.height();
        float p = transAnim.getValue();
        if (outgoing != null && p < 1f) {
            IHudElement.Size os = outgoing.getHudAlignment();
            tw = Mth.lerp(p, os.width(), sz.width());
            th = Mth.lerp(p, os.height(), sz.height());
        }
        widthAnim.setTargetValue(tw); heightAnim.setTargetValue(th);
        widthAnim.update(dt); heightAnim.update(dt); transAnim.update(dt);

        float iw = Math.max(0, widthAnim.getValue() + 30);
        float ih = Math.max(0, heightAnim.getValue() + 3);
        float ix = (ClientBase.mc.getWindow().getGuiScaledWidth() - iw) / 2f;
        float iy = 12f;
        float radius = 12f;
        final float fy = iy;

        if (active.hasBackground()) {
            Renderer.renderConsumer(dc -> {
                try (Paint paint = new Paint()) {
                    paint.setColor(new Color(0, 0, 0, 40).getRGB());
                    dc.drawRoundedRect(RoundedRectangle.ofXYWHR(ix, fy, iw, ih, radius), paint);
                }
                dc.save();
                dc.clipRoundedRect(RoundedRectangle.ofXYWHR(ix, fy, iw, ih, radius), true);
                active.render(dc, ix, fy, iw, ih, p);
                dc.restore();
            });
        }
        RenderUtil.pushScissor((int) ix, (int) iy, (int) iw, (int) ih);
        active.renderGui(e.guiGraphics(), e.poseStack(), ix, iy, iw, ih, p);
        RenderUtil.popScissor();
        if (p >= 1f) outgoing = null;
    }
}
