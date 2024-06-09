package net.xavil.ultraviolet.client.screen.layer;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.FormattedText;
import net.xavil.hawklib.client.HawkDrawStates;
import net.xavil.hawklib.client.HawkShaders;
import net.xavil.hawklib.client.camera.CachedCamera;
import net.xavil.hawklib.client.camera.RenderMatricesSnapshot;
import net.xavil.hawklib.client.flexible.BufferLayout;
import net.xavil.hawklib.client.flexible.BufferRenderer;
import net.xavil.hawklib.client.flexible.PrimitiveType;
import net.xavil.hawklib.client.gl.GlPerf;
import net.xavil.hawklib.client.screen.HawkScreen;
import net.xavil.hawklib.client.screen.HawkScreen.RenderContext;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.impl.VectorFloat;
import net.xavil.hawklib.math.ColorRgba;
import net.xavil.hawklib.math.TransformStack;
import net.xavil.hawklib.math.matrices.Mat4;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.hawklib.math.matrices.interfaces.Mat4Access;
import net.xavil.ultraviolet.client.screen.RenderHelper;

public final class ScreenLayerGpuTimers extends HawkScreen.Layer2d {

    private CachedCamera camera = new CachedCamera();

    public ScreenLayerGpuTimers(HawkScreen attachedScreen) {
        super(attachedScreen);
    }

    private void setupCamera(RenderContext ctx) {
        // setup camera for ortho UI
        final var window = Minecraft.getInstance().getWindow();

        final double frustumDepth = 400;

        final var projMat = new Mat4.Mutable();
        final var projLR = window.getGuiScaledWidth();
        final var projTB = window.getGuiScaledHeight();
        Mat4.setOrthographicProjection(projMat, 0, projLR, 0, -projTB, -frustumDepth, 0);

        final var inverseViewMat = new Mat4.Mutable();
        inverseViewMat.loadIdentity();
        inverseViewMat.appendTranslation(Vec3.ZERO.withZ(0.5 * frustumDepth));

        this.camera.load(inverseViewMat, projMat, 1);
    }

    private static final class GlobalNodeRenderInfo {
        public final TransformStack tfm;
        public final TextBuilder textSink;
        public final Vector<Vec3> linePositions = new Vector<>();
        public final float rootTimeCpu;
        public final float rootTimeGpu;

        public GlobalNodeRenderInfo(TransformStack tfm, TextBuilder textSink,
                float rootTimeCpu, float rootTimeGpu) {
            this.tfm = tfm;
            this.textSink = textSink;
            this.rootTimeCpu = rootTimeCpu;
            this.rootTimeGpu = rootTimeGpu;
        }
    }

    private float renderPerfNode(GlobalNodeRenderInfo info, GlPerf.TimerTreeNode node,
            float parentTimeCpu, float parentTimeGpu) {
        float totalTimeCpu = node.timer.windowedAverageCpuTime(), totalTimeGpu = node.timer.windowedAverageGpuTime();
        float selfTimeCpu = node.timer.windowedAverageCpuTime(), selfTimeGpu = node.timer.windowedAverageGpuTime();
        for (final var child : node.children().iterable()) {
            selfTimeCpu -= child.timer.windowedAverageCpuTime();
            selfTimeGpu -= child.timer.windowedAverageGpuTime();
        }
        selfTimeCpu = Math.max(0, selfTimeCpu);
        selfTimeGpu = Math.max(0, selfTimeGpu);

        final float rootPercentCpu = totalTimeCpu / info.rootTimeCpu, rootPercentGpu = totalTimeGpu / info.rootTimeGpu;
        final float parentPercentCpu = totalTimeCpu / parentTimeCpu, parentPercentGpu = totalTimeGpu / parentTimeGpu;

        final var color = totalTimeCpu > 0.0069 || totalTimeGpu > 0.0069 ? ColorRgba.RED : ColorRgba.WHITE;
        info.textSink.baseColor = color.withA(1 - node.removalPercent());

        // self total %parent %root
        final var baseX = info.textSink.cursorX;
        info.textSink.emit(info.tfm.current(), node.timer.name);

        info.textSink.cursorX = 200;
        if (!node.children().isEmpty()) {
            info.textSink.emit(info.tfm.current(), String.format("%.2f/%.2f ms", 1e3 * selfTimeCpu, 1e3 * selfTimeGpu));
        }

        info.textSink.cursorX += 110;
        info.textSink.emit(info.tfm.current(), String.format("%.2f/%.2f ms", 1e3 * totalTimeCpu, 1e3 * totalTimeGpu));

        info.textSink.baseColor = ColorRgba.lerp((float) Math.pow(parentPercentGpu, 0.5), color, ColorRgba.CYAN)
                .withA(1 - node.removalPercent());
        info.textSink.cursorX += 110;
        info.textSink.emit(info.tfm.current(),
                String.format("%.1f/%.1f", 100 * parentPercentCpu, 100 * parentPercentGpu));

        info.textSink.baseColor = ColorRgba.lerp((float) Math.pow(rootPercentGpu, 0.5), color, ColorRgba.CYAN)
                .withA(1 - node.removalPercent());
        info.textSink.cursorX += 110;
        info.textSink.emit(info.tfm.current(), String.format("%.1f/%.1f", 100 * rootPercentCpu, 100 * rootPercentGpu));

        info.textSink.cursorX = baseX;
        info.textSink.cursorNewline();

        final var directChildrenHeights = new VectorFloat();
        double minHeight = info.textSink.cursorY, maxHeight = info.textSink.cursorY;

        final float indentAmount = 9f;

        info.textSink.cursorIndent(indentAmount);
        for (final var child : node.children().iterable()) {
            directChildrenHeights.push((float) (info.textSink.cursorY - info.textSink.lastSizeY / 2));
            maxHeight = info.textSink.cursorY;
            renderPerfNode(info, child, totalTimeCpu, totalTimeGpu);
        }
        info.textSink.cursorIndent(-indentAmount);

        if (!directChildrenHeights.isEmpty()) {
            final var x = (float) info.textSink.cursorX + indentAmount / 2;
            final double yl = minHeight, yh = maxHeight;
            info.linePositions.push(new Vec3(x, yl - 1, 0));
            info.linePositions.push(new Vec3(x, yh - 4.5f, 0));

            for (int i = 0; i < directChildrenHeights.size(); ++i) {
                final var y = directChildrenHeights.get(i);
                final float xl = x, xh = x + 4f;
                info.linePositions.push(new Vec3(xl, y, 0));
                info.linePositions.push(new Vec3(xh, y, 0));
            }
        }

        return 0;
    }

    @Override
    public void render(RenderContext ctx) {
        GlPerf.push("ScreenLayerGpuTimers");

        setupCamera(ctx);

        final var snapshot = RenderMatricesSnapshot.capture();
        this.camera.applyProjection();
        this.camera.applyView();

        // ctx.currentTexture.framebuffer.clear();
        final var sink = new TextBuilder();
        final var tfm = new TransformStack();
        tfm.appendTransform(Mat4Access.from(ctx.poseStack.last().pose()));
        // tfm.appendTranslation(new Vec3(1.02, 0.98, 0));

        GlPerf.push("explicit");
        sink.emitNewline(tfm.current(), FormattedText.of("Explicit Timers"));
        sink.emitNewline(tfm.current(),
                FormattedText.of(String.format("%d Objects", GlPerf.ROOT_OBJECT_METRICS.totalObjects())));

        GlPerf.swap("implicit");
        // GlPerf.push("implicit");
        // sink.emitNewline(tfm.current(), FormattedText.of("Implicit Timers"));
        final var rootCpu = GlPerf.ROOT.timer.windowedAverageCpuTime();
        final var rootGpu = GlPerf.ROOT.timer.windowedAverageGpuTime();

        final var info = new GlobalNodeRenderInfo(tfm, sink, rootCpu, rootGpu);
        renderPerfNode(info, GlPerf.ROOT, rootCpu, rootGpu);
        sink.draw(BufferRenderer.IMMEDIATE_BUILDER, HawkDrawStates.DRAW_STATE_DIRECT_ALPHA_BLENDING, 1f);

        final var builder = BufferRenderer.IMMEDIATE_BUILDER.beginGeneric(PrimitiveType.LINE_DUPLICATED,
                BufferLayout.POSITION_COLOR_NORMAL);
        for (int i = 0; i < info.linePositions.size() - 1;) {
            final var start = info.linePositions.get(i++);
            final var end = info.linePositions.get(i++);
            RenderHelper.addLine(builder, start, end, ColorRgba.WHITE);
        }
        RenderSystem.lineWidth(2);
        builder.end().draw(HawkShaders.SHADER_VANILLA_RENDERTYPE_LINES.get(),
                HawkDrawStates.DRAW_STATE_DIRECT_ALPHA_BLENDING);

        GlPerf.pop();

        snapshot.restore();

        GlPerf.pop();
    }

}
