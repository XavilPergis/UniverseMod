package net.xavil.hawklib.client.flexible.vertex;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import javax.annotation.OverridingMethodsMustInvokeSuper;

import net.xavil.hawklib.client.flexible.BufferLayout;
import net.xavil.hawklib.client.flexible.VertexAttributeConsumer;
import net.xavil.hawklib.collections.interfaces.MutableMap;
import net.xavil.hawklib.math.ColorRgba;
import net.xavil.hawklib.math.matrices.interfaces.Vec3Access;

public abstract class VertexDispatcher implements VertexAttributeConsumer {

    protected VertexBuilder builder;

    public final void setup(VertexBuilder builder, BufferLayout layout, ByteBuffer buf) {
        this.builder = builder;

        final var ctx = new AttributeRegistrationContext();
        registerAttributes(ctx);

        for (final var element : layout.elements.iterable()) {
            switch (element.attribType) {
                case FLOAT ->
                    ctx.floatAttribs.getOrThrow(element.attribute).accept(new ElementInfo.Float(buf, element));
                case INT -> ctx.intAttribs.getOrThrow(element.attribute).accept(new ElementInfo.Int(buf, element));
            }
        }
    }

    protected abstract void registerAttributes(VertexDispatcher.AttributeRegistrationContext ctx);

    @Override
    public final void endVertex() {
        this.builder.endVertex();
    }

    public final FilledBuffer end() {
        return this.builder.end();
    }

    protected static final class AttributeRegistrationContext {
        private final MutableMap<BufferLayout.Attribute, Consumer<ElementInfo.Float>> floatAttribs = MutableMap
                .identityHashMap();
        private final MutableMap<BufferLayout.Attribute, Consumer<ElementInfo.Int>> intAttribs = MutableMap
                .identityHashMap();

        public final void registerFloat(BufferLayout.Attribute usage, Consumer<ElementInfo.Float> setter) {
            this.floatAttribs.insert(usage, setter);
        }

        public final void registerInt(BufferLayout.Attribute usage, Consumer<ElementInfo.Int> setter) {
            this.intAttribs.insert(usage, setter);
        }
    }

    public static class Generic extends VertexDispatcher implements VertexAttributeConsumer.Generic {

        public ElementInfo.Float position;
        public ElementInfo.Float color;
        public ElementInfo.Float normal;
        public ElementInfo.Float uv0, uv1, uv2;

        @Override
        @OverridingMethodsMustInvokeSuper
        protected void registerAttributes(AttributeRegistrationContext ctx) {
            ctx.registerFloat(BufferLayout.Attribute.POSITION, info -> this.position = info);
            ctx.registerFloat(BufferLayout.Attribute.COLOR, info -> this.color = info);
            ctx.registerFloat(BufferLayout.Attribute.NORMAL, info -> this.normal = info);
            ctx.registerFloat(BufferLayout.Attribute.UV0, info -> this.uv0 = info);
            ctx.registerFloat(BufferLayout.Attribute.UV1, info -> this.uv1 = info);
            ctx.registerFloat(BufferLayout.Attribute.UV2, info -> this.uv2 = info);
        }

        @Override
        public VertexDispatcher.Generic vertex(double x, double y, double z) {
            if (this.position != null)
                this.position.setFloats(0, (float) x, (float) y, (float) z);
            return this;
        }

        @Override
        public VertexDispatcher.Generic vertex(float x, float y, float z) {
            if (this.position != null)
                this.position.setFloats(0, x, y, z);
            return this;
        }

        @Override
        public VertexDispatcher.Generic vertex(Vec3Access pos) {
            if (this.position != null)
                this.position.setFloats(0, (float) pos.x(), (float) pos.y(),
                        (float) pos.z());
            return this;
        }

        @Override
        public VertexDispatcher.Generic color(ColorRgba color) {
            if (this.color != null)
                this.color.setFloats(0, color.r, color.g, color.b, color.a);
            return this;
        }

        @Override
        public VertexDispatcher.Generic color(float r, float g, float b, float a) {
            if (this.color != null)
                this.color.setFloats(0, r, g, b, a);
            return this;
        }

        @Override
        public VertexDispatcher.Generic uv0(float u, float v) {
            if (this.uv0 != null)
                this.uv0.setFloats(0, u, v);
            return this;
        }

        @Override
        public VertexDispatcher.Generic uv1(float u, float v) {
            if (this.uv1 != null)
                this.uv1.setFloats(0, u, v);
            return this;
        }

        @Override
        public VertexDispatcher.Generic uv2(float u, float v) {
            if (this.uv2 != null)
                this.uv2.setFloats(0, u, v);
            return this;
        }

        @Override
        public VertexDispatcher.Generic normal(double x, double y, double z) {
            if (this.normal != null)
                this.normal.setFloats(0, (float) x, (float) y, (float) z);
            return this;
        }

        @Override
        public VertexDispatcher.Generic normal(float x, float y, float z) {
            if (this.normal != null)
                this.normal.setFloats(0, x, y, z);
            return this;
        }

        @Override
        public VertexDispatcher.Generic normal(Vec3Access norm) {
            if (this.normal != null)
                this.normal.setFloats(0, (float) norm.x(), (float) norm.y(),
                        (float) norm.z());
            return this;
        }

    }

}