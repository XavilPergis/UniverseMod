package net.xavil.hawklib.client.flexible;

import java.nio.ByteBuffer;

import com.mojang.blaze3d.vertex.VertexFormat;

import it.unimi.dsi.fastutil.ints.IntConsumer;
import net.xavil.hawklib.Assert;
import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.client.gl.GlBuffer;

public final class IndexPattern {

    public final PrimitiveType primitiveType;
    public final int duplicationCount;
    public final SequentialIndexBufferPool indexPool;

    public IndexPattern(PrimitiveType primitiveType, int duplicationCount, SequentialIndexBufferPool indexPool) {
        this.primitiveType = primitiveType;
        this.duplicationCount = duplicationCount;
        this.indexPool = indexPool;
    }

    public static IndexPattern forPrimitiveType(PrimitiveType primitiveType) {
        return new IndexPattern(primitiveType, 0, null);
    }

    // @formatter:off
    private static final SequentialIndexBufferPool QUADS_POOL = new SequentialIndexBufferPool(0, 1, 2, 2, 3, 0);
    private static final SequentialIndexBufferPool LINES_POOL = new SequentialIndexBufferPool(0, 1, 2, 3, 2, 1);

    // start at bottom right, emit counterclockwise
    public static final IndexPattern QUADS = new IndexPattern(PrimitiveType.TRIANGLE, 0, QUADS_POOL);
    public static final IndexPattern VANILLA_LINE_STRIP = new IndexPattern(PrimitiveType.TRIANGLE_STRIP, 1, null);
    public static final IndexPattern VANILLA_LINES = new IndexPattern(PrimitiveType.TRIANGLE, 1, LINES_POOL);
    public static final IndexPattern POINT_DUPLICATED = new IndexPattern(PrimitiveType.TRIANGLE, 3, QUADS_POOL);
	// @formatter:on

    public static final class SequentialIndexBufferPool implements Disposable {
        private final int[] indexPattern;
        private final int maxIndexInPattern;
        private final int verticesPerPrimitive;

        private int currentIndexCountVirtual;
        private int currentIndexCountPhysical;
        private VertexFormat.IndexType currentIndexType;
        private final GlBuffer.GrowableBuffer currentBuffer;

        public SequentialIndexBufferPool(int... indexPattern) {
            this.indexPattern = indexPattern;
            this.currentBuffer = new GlBuffer.GrowableBuffer();

            int maxIndex = -1;
            for (final var idx : indexPattern)
                maxIndex = Math.max(maxIndex, idx);
            this.maxIndexInPattern = maxIndex;
            this.verticesPerPrimitive = maxIndex + 1;
        }

        @Override
        public void close() {
            if (this.currentBuffer != null)
                this.currentBuffer.close();
        }

        private static IntConsumer getIndexWriter(ByteBuffer buffer, VertexFormat.IndexType type) {
            return switch (type) {
                case BYTE -> index -> buffer.put((byte) index);
                case SHORT -> index -> buffer.putShort((short) index);
                case INT -> index -> buffer.putInt(index);
            };
        }

        private void updateBufferIfNeeded(int vertexCount) {
            if (vertexCount <= this.currentIndexCountVirtual)
                return;

            final var desiredVertexCount = 2 * vertexCount;

            final var primitiveCount = desiredVertexCount / this.verticesPerPrimitive;
            final var indexCount = primitiveCount * this.indexPattern.length;
            final var indexType = VertexFormat.IndexType.least(indexCount);

            this.currentBuffer.setContents(indexCount * indexType.bytes, sink -> {
                final var writer = getIndexWriter(sink, indexType);
                for (int i = 0; i < primitiveCount; ++i) {
                    final var baseIndex = this.verticesPerPrimitive * i;
                    for (int j = 0; j < this.indexPattern.length; ++j)
                        writer.accept(baseIndex + this.indexPattern[j]);
                }
            });

            this.currentIndexType = indexType;
            this.currentIndexCountVirtual = desiredVertexCount;
            this.currentIndexCountPhysical = indexCount;
        }

        private int elementCount(int vertexCount) {
            return vertexCount / this.verticesPerPrimitive * this.indexPattern.length;
        }

    }

    public Mesh.IndexBuffer getIndexBuffer(int vertexCount) {
        if (this.indexPool == null)
            return null;

        // we dont support anything else like line strips right now (not that that would
        // be particularly useful lol)
        Assert.isEqual(this.primitiveType.primitiveSize, this.primitiveType.primitiveStride);

        this.indexPool.updateBufferIfNeeded(vertexCount);
        if (this.indexPool.currentIndexCountPhysical <= 0)
            return null;

        return new Mesh.IndexBuffer(
                this.indexPool.currentBuffer.slice().buffer,
                this.indexPool.elementCount(vertexCount),
                this.indexPool.currentIndexType);
    }

}
