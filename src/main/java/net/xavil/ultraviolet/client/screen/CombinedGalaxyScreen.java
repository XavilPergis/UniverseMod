package net.xavil.ultraviolet.client.screen;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Mth;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.client.HawkDrawStates;
import net.xavil.hawklib.client.camera.CachedCamera;
import net.xavil.hawklib.client.camera.FirstPersonCameraControls;
import net.xavil.hawklib.client.camera.MotionSmoother;
import net.xavil.hawklib.client.camera.RenderMatricesSnapshot;
import net.xavil.hawklib.client.flexible.BufferLayout;
import net.xavil.hawklib.client.flexible.BufferRenderer;
import net.xavil.hawklib.client.flexible.IndexPattern;
import net.xavil.hawklib.client.screen.HawkScreen;
import net.xavil.hawklib.math.ColorRgba;
import net.xavil.hawklib.math.Quat;
import net.xavil.hawklib.math.matrices.Mat4;
import net.xavil.hawklib.math.matrices.Vec2;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.ultraviolet.client.UltravioletShaders;
import net.xavil.ultraviolet.client.screen.layer.ScreenLayerBackground;
import net.xavil.ultraviolet.common.universe.NearbyObjectTracker;
import net.xavil.ultraviolet.common.universe.id.UniversePosition;
import net.xavil.ultraviolet.common.universe.universe.ClientUniverse;
import net.xavil.ultraviolet.common.universe.universe.UniverseSectorTicket;
import net.xavil.ultraviolet.common.universe.universe.UniverseSectorTicketInfo;
import net.xavil.ultraviolet.mixin.accessor.MinecraftClientAccessor;
import net.xavil.ultraviolet.mixin.accessor.MouseHandlerAccessor;

public class CombinedGalaxyScreen extends HawkScreen {

    private final ClientUniverse universe;
    private final NearbyObjectTracker nearbyTracker;
    private UniversePosition anchor;
    private final UniverseSectorTicket<UniverseSectorTicketInfo.Multi> universeTicket;
    private final MotionSmoother<UniversePosition> cameraPos = new MotionSmoother<>(0.5,
            MotionSmoother.Interpolator.UNIVERSE_POS, UniversePosition.ZERO);
    private final MotionSmoother<Quat> cameraOrientation = new MotionSmoother<>(0.5,
            MotionSmoother.Interpolator.QUAT, Quat.IDENTITY);
    private final MotionSmoother<Double> moveSpeed = new MotionSmoother<>(0.5,
            MotionSmoother.Interpolator.DOUBLE, 1.0);
    private boolean shouldGrabMouse = true;

    private FirstPersonCameraControls controls = new FirstPersonCameraControls();

    public CombinedGalaxyScreen(Screen previousScreen, UniversePosition position) {
        super(new TranslatableComponent("narrator.screen.combined"), previousScreen);

        this.layers.push(new ScreenLayerBackground(this, ColorRgba.BLACK));

        this.anchor = UniversePosition.ZERO;
        this.universe = MinecraftClientAccessor.getUniverse();
        this.universeTicket = this.universe.sectorManager.createSectorTicket(this.disposer,
                UniverseSectorTicketInfo.visual(Vec3.ZERO));
        this.nearbyTracker = new NearbyObjectTracker(this.universe);
        this.cameraPos.set(position);
    }

    @Override
    public boolean keyPressed(Keypress keypress) {
        if (super.keyPressed(keypress))
            return true;
        if (this.controls.updateFromKeypress(keypress))
            return true;
        if (keypress.keyCode == GLFW.GLFW_KEY_TAB) {
            this.shouldGrabMouse = !this.shouldGrabMouse;
        }
        return false;
    }

    @Override
    public boolean keyReleased(Keypress keypress) {
        if (super.keyReleased(keypress))
            return true;
        if (this.controls.updateFromKeypress(keypress))
            return true;
        return false;
    }

    private double prevMouseX, prevMouseY;
    private boolean hasPrevMousePos = false;

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (!hasPrevMousePos) {
            this.prevMouseX = mouseX;
            this.prevMouseY = mouseY;
            hasPrevMousePos = true;
            return;
        }

        final var deltaX = mouseX - this.prevMouseX;
        final var deltaY = mouseY - this.prevMouseY;
        this.prevMouseX = mouseX;
        this.prevMouseY = mouseY;

        this.cameraOrientation.target = Quat.axisAngle(Vec3.XP, 0.005 * deltaY).mul(this.cameraOrientation.target);
        this.cameraOrientation.target = Quat.axisAngle(Vec3.YP, 0.005 * deltaX).mul(this.cameraOrientation.target);
    }

    @Override
    public boolean mouseScrolled(Vec2 mousePos, double scrollDelta) {
        final var scrollSpeed = 1.06;
        if (scrollDelta > 0) {
            this.moveSpeed.target *= scrollSpeed;
        } else if (scrollDelta < 0) {
            this.moveSpeed.target /= scrollSpeed;
        }
        this.moveSpeed.target = Mth.clamp(this.moveSpeed.target, 1e-1, 1e22);
        return true;
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void onClose() {
        super.onClose();
        this.client.mouseHandler.releaseMouse();
    }

    @Override
    public void renderScreenPostLayers(RenderContext ctx) {
        super.renderScreenPostLayers(ctx);

        if (this.shouldGrabMouse) {
            MouseHandlerAccessor.grabMouse(false);
        } else {
            this.client.mouseHandler.releaseMouse();
        }

        double dx = 0, dy = 0, dz = 0;
        if (this.controls.isForwardPressed)
            dz -= 1;
        if (this.controls.isBackwardPressed)
            dz += 1;
        if (this.controls.isLeftPressed)
            dx -= 1;
        if (this.controls.isRightPressed)
            dx += 1;
        if (this.controls.isDownPressed)
            dy -= 1;
        if (this.controls.isUpPressed)
            dy += 1;

        Vec3 offset = new Vec3(dx, dy, dz);
        offset = this.cameraOrientation.current.inverse().transform(offset);
        offset = offset.mul(this.moveSpeed.current);
        offset = offset.mul(ctx.deltaTime);
        if (offset.length() > 0) {
            final var offset2 = UniversePosition.from(offset, 1);
            this.cameraPos.target = this.cameraPos.target.add(offset2);
        }

        this.cameraPos.tick(ctx.deltaTime);
        this.cameraOrientation.tick(ctx.deltaTime);
        this.moveSpeed.tick(ctx.deltaTime);

        this.universeTicket.info.centerPos = this.cameraPos.current.relativeTo(UniversePosition.ZERO, Units.Zu_PER_u);
        this.nearbyTracker.update(this.cameraPos.current);

        this.anchor = UniversePosition.ZERO;
        if (this.nearbyTracker.getGalaxyIn() != null) {
            final var galaxyPos = this.nearbyTracker.getGalaxyIn().position;
            this.anchor = galaxyPos;
        }

        final var snapshot = RenderMatricesSnapshot.capture();
        final var cam = new CachedCamera();
        final var window = Minecraft.getInstance().getWindow();
        final var aspectRatio = (float) window.getWidth() / (float) window.getHeight();
        final var projMat = Mat4.perspectiveProjection(Math.toRadians(90.0), aspectRatio, 1e-4, 1e5);
        final var camPos = this.cameraPos.current.relativeTo(this.anchor, Units.Zu_PER_u);
        cam.load(camPos, this.cameraOrientation.current, projMat, Units.u_PER_Zu);
        cam.applyProjection();
        CachedCamera.applyView(cam.viewMatrix);
        // cam.applyView();

        final var builder = BufferRenderer.IMMEDIATE_BUILDER.beginGeneric(
                IndexPattern.QUADS, BufferLayout.POSITION_COLOR_TEX);

        this.universeTicket.attachedManager.enumerate(this.universeTicket, sector -> {
            if (sector.initialElements == null)
                return;
            for (int i = 0; i < sector.initialElements.size(); ++i) {
                final var elem = sector.initialElements.get(i);
                final var pos = elem.pos().relativeTo(this.anchor, Units.Zu_PER_u);
                final var size = (float) (elem.info().radius * Units.Zu_PER_Tu);
                final var xo = elem.info().orientation.transform(Vec3.XP).mul(size);
                final var yo = elem.info().orientation.transform(Vec3.YP).mul(size);
                final var nn = pos.sub(xo).sub(yo);
                final var np = pos.sub(xo).add(yo);
                final var pn = pos.add(xo).sub(yo);
                final var pp = pos.add(xo).add(yo);
                boolean isGalaxyin = false;
                if (this.nearbyTracker.getGalaxyIn() != null) {
                    final var gid = this.nearbyTracker.getGalaxyIn().galaxyId;
                    if (gid.sectorPos().equals(sector.pos) && gid.id() == i) {
                        isGalaxyin = true;
                    }
                }
                var color = ColorRgba.RED;
                if (isGalaxyin)
                    color = ColorRgba.CYAN;
                color = color.withA(0.05f);
                builder.vertex(pn).color(color).uv0(1, 0).endVertex();
                builder.vertex(nn).color(color).uv0(0, 0).endVertex();
                builder.vertex(np).color(color).uv0(0, 1).endVertex();
                builder.vertex(pp).color(color).uv0(1, 1).endVertex();
            }
        });

        builder.vertex(Vec3.ZERO.add(Vec3.XP).sub(Vec3.ZP)).color(ColorRgba.GREEN.withA(0.1f)).uv0(1, 0).endVertex();
        builder.vertex(Vec3.ZERO.sub(Vec3.XP).sub(Vec3.ZP)).color(ColorRgba.GREEN.withA(0.1f)).uv0(0, 0).endVertex();
        builder.vertex(Vec3.ZERO.sub(Vec3.XP).add(Vec3.ZP)).color(ColorRgba.GREEN.withA(0.1f)).uv0(0, 1).endVertex();
        builder.vertex(Vec3.ZERO.add(Vec3.XP).add(Vec3.ZP)).color(ColorRgba.GREEN.withA(0.1f)).uv0(1, 1).endVertex();

        final var shader = UltravioletShaders.SHADER_UI_QUADS.get();
        shader.setupDefaultShaderUniforms();
        builder.end().draw(UltravioletShaders.SHADER_UI_QUADS.get(),
                HawkDrawStates.DRAW_STATE_DIRECT_ALPHA_BLENDING);

        snapshot.restore();
    }

}
