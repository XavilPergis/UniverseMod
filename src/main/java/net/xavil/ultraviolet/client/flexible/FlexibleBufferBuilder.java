package net.xavil.ultraviolet.client.flexible;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.MemoryTracker;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;

import net.minecraft.util.Mth;
import net.xavil.ultraviolet.client.gl.DrawState;
import net.xavil.ultraviolet.client.gl.shader.ShaderProgram;
import net.xavil.util.Assert;
import net.xavil.util.math.Color;
import net.xavil.util.math.matrices.interfaces.Vec3Access;

public final class FlexibleBufferBuilder implements FlexibleVertexConsumer {

	private static final int GROWTH_SIZE = 0x200000;
	private static final Logger LOGGER = LogUtils.getLogger();

	private int vertexStartByteOffset = 0;
	private int vertexByteOffset = 0;
	private VertexFormat currentVertexFormat = null;
	private FlexibleVertexMode currentMode = null;
	private ByteBuffer buffer;
	private int vertexCount = 0;
	private ElementDispatch dispatch = new ElementDispatch();

	// finished buffers
	private final List<FinishedBuffer> finishedBuffers = Lists.newArrayList();
	private int poppedBufferCount = 0;

	// @formatter:off
	private static final float   BYTE_MAX =      (float)    Byte.MAX_VALUE;
	private static final float  SHORT_MAX =      (float)   Short.MAX_VALUE;
	private static final float    INT_MAX =      (float) Integer.MAX_VALUE;
	private static final float  UBYTE_MAX = 2f * (float)    Byte.MAX_VALUE;
	private static final float USHORT_MAX = 2f * (float)   Short.MAX_VALUE;
	private static final float   UINT_MAX = 2f * (float) Integer.MAX_VALUE;
	// @formatter:on

	private static ElementConsumer writerForType(VertexFormatElement elem, ByteBuffer buf) {
		final var normalized = switch (elem.getUsage()) {
			case NORMAL, COLOR, UV -> true;
			default -> false;
		};
		// @formatter:off
		if (!normalized) return switch (elem.getType()) {
			case BYTE   -> (off, n) -> buf.put     (off,  (byte) n);
			case UBYTE  -> (off, n) -> buf.put     (off,  (byte) n);
			case SHORT  -> (off, n) -> buf.putShort(off, (short) n);
			case USHORT -> (off, n) -> buf.putShort(off, (short) n);
			case INT    -> (off, n) -> buf.putInt  (off,   (int) n);
			case UINT   -> (off, n) -> buf.putInt  (off,   (int) n);
			case FLOAT  -> (off, n) -> buf.putFloat(off,         n);
			default     -> (k, n) -> {};
		};
		return switch (elem.getType()) {
			case BYTE   -> (off, n) -> buf.put     (off,  (byte) (n *   BYTE_MAX));
			case UBYTE  -> (off, n) -> buf.put     (off,  (byte) (n *  UBYTE_MAX));
			case SHORT  -> (off, n) -> buf.putShort(off, (short) (n *  SHORT_MAX));
			case USHORT -> (off, n) -> buf.putShort(off, (short) (n * USHORT_MAX));
			case INT    -> (off, n) -> buf.putInt  (off,   (int) (n *    INT_MAX));
			case UINT   -> (off, n) -> buf.putInt  (off,   (int) (n *   UINT_MAX));
			case FLOAT  -> (off, n) -> buf.putFloat(off,         (n             ));
			default     -> (k, n) -> {};
		};
		// @formatter:on
	}

	private static interface ElementConsumer {
		void emit(int relativeOffset, float value);
	}

	private static class ElementHolder {
		public final ElementConsumer consumer;
		public final VertexFormatElement element;
		public final int elementOffset;
		public float c0, c1, c2, c3;
		public final int c0o, c1o, c2o, c3o;

		public ElementHolder(ElementConsumer consumer, VertexFormatElement element, int elementOffset) {
			this.consumer = consumer;
			this.element = element;
			this.elementOffset = elementOffset;

			final var elementSize = element.getType().getSize();
			this.c0o = (0 * elementSize) + elementOffset;
			this.c1o = (1 * elementSize) + elementOffset;
			this.c2o = (2 * elementSize) + elementOffset;
			this.c3o = (3 * elementSize) + elementOffset;
		}

		public void emit(int vertexBase) {
			// @formatter:off
			switch (element.getCount()) {
				case 4: this.consumer.emit(vertexBase + this.c3o, this.c3);
				case 3: this.consumer.emit(vertexBase + this.c2o, this.c2);
				case 2: this.consumer.emit(vertexBase + this.c1o, this.c1);
				case 1: this.consumer.emit(vertexBase + this.c0o, this.c0);
				default:
			}
			// @formatter:on
		}

	}

	private static class ElementDispatch {
		public ElementHolder[] activeHolders;

		public ElementHolder positionHolder;
		public ElementHolder colorHolder;
		public ElementHolder normalHolder;
		public ElementHolder uv0Holder;
		public ElementHolder uv1Holder;
		public ElementHolder uv2Holder;

		private void addHolder(VertexFormatElement element, ElementHolder holder) {
			// @formatter:off
			switch (element.getUsage()) {
				case POSITION -> this.positionHolder = holder;
				case COLOR -> this.colorHolder = holder;
				case NORMAL -> this.normalHolder = holder;
				case UV -> {
					switch (element.getIndex()) {
						case 0 -> this.uv0Holder = holder;
						case 1 -> this.uv1Holder = holder;
						case 2 -> this.uv2Holder = holder;
						default -> {}
					}
				}
				default -> {}
			}
			// @formatter:on
		}

		public void resetHolders() {
			this.activeHolders = null;
			this.positionHolder = this.colorHolder = this.normalHolder = this.uv0Holder = this.uv1Holder = this.uv2Holder = null;
		}

		public void setup(VertexFormat format, ByteBuffer buf) {
			resetHolders();
			final var holdersList = new ArrayList<ElementHolder>();
			int elementOffset = 0;
			for (final var element : format.getElements()) {
				final var writer = writerForType(element, buf);
				if (element.getUsage() != VertexFormatElement.Usage.PADDING) {
					final var holder = new ElementHolder(writer, element, elementOffset);
					holdersList.add(holder);
					addHolder(element, holder);
				}
				elementOffset += element.getByteSize();
			}
			this.activeHolders = holdersList.toArray(ElementHolder[]::new);
		}

		public void emit(int vertexBase) {
			for (final var holder : this.activeHolders)
				holder.emit(vertexBase);
		}
	}

	public FlexibleBufferBuilder(int maxFaceCount) {
		this.buffer = MemoryTracker.create(maxFaceCount * 6);
	}

	public void begin(FlexibleVertexMode mode, VertexFormat format) {
		this.dispatch.setup(format, this.buffer);
		this.currentVertexFormat = format;
		this.currentMode = mode;
		this.vertexStartByteOffset = Mth.roundToward(this.vertexByteOffset, 4);
	}

	public void begin(VertexFormat.Mode mode, VertexFormat format) {
		begin(FlexibleVertexMode.from(mode), format);
	}

	public boolean isBuilding() {
		return this.currentVertexFormat != null;
	}

	public void end() {
		Assert.isTrue(isBuilding());
		final var indexCount = this.currentMode.indexCount(this.vertexCount);
		final var indexType = VertexFormat.IndexType.least(indexCount);
		final var finished = new FinishedBuffer(this.currentVertexFormat, this.currentMode, indexType,
				this.vertexStartByteOffset, this.vertexCount, indexCount, true);
		this.vertexCount = 0;
		this.currentVertexFormat = null;
		this.currentMode = null;

		this.finishedBuffers.add(finished);
	}

	public ByteBuffer backingBufferView() {
		return this.buffer.asReadOnlyBuffer();
	}

	public void draw(ShaderProgram shader, DrawState drawState) {
		Assert.isTrue(!isBuilding());
		BufferRenderer.draw(shader, this, drawState);
	}

	public void reset() {
		this.vertexByteOffset = 0;
		this.vertexStartByteOffset = 0;
		this.poppedBufferCount = 0;
		this.finishedBuffers.clear();
	}

	public Pair<FinishedBuffer, ByteBuffer> popFinished() {
		Assert.isTrue(this.poppedBufferCount < this.finishedBuffers.size());
		final var finished = this.finishedBuffers.get(this.poppedBufferCount++);
		if (this.poppedBufferCount == this.finishedBuffers.size()) {
			reset();
		}

		final var bufferSlice = this.buffer.slice(finished.parentBufferOffset(), finished.byteCount());
		return Pair.of(finished, bufferSlice);
	}

	@Override
	public void endVertex() {
		if (this.currentVertexFormat == null)
			throw new RuntimeException("cannot add vertices to FlexibleBufferBuilder; it is not currently building!");
		try {
			emitVertex();
		} catch (Throwable t) {
			var msg = "Buffer Builder Error";
			msg += "Current Capacity: " + this.buffer.capacity();
			throw new RuntimeException(msg, t);
		}
	}

	private void emitVertex() {
		ensureBufferCapacity(this.currentVertexFormat.getVertexSize() * this.currentMode.duplicationCount);
		for (int i = 0; i < this.currentMode.duplicationCount; ++i) {
			this.dispatch.emit(this.vertexByteOffset);
			this.vertexByteOffset += this.currentVertexFormat.getVertexSize();
		}
		this.vertexCount += this.currentMode.duplicationCount;
	}

	@Override
	public FlexibleBufferBuilder vertex(double x, double y, double z) {
		dispatch.positionHolder.c0 = (float) x;
		dispatch.positionHolder.c1 = (float) y;
		dispatch.positionHolder.c2 = (float) z;
		return this;
	}

	@Override
	public FlexibleBufferBuilder vertex(float x, float y, float z) {
		dispatch.positionHolder.c0 = x;
		dispatch.positionHolder.c1 = y;
		dispatch.positionHolder.c2 = z;
		return this;
	}

	@Override
	public FlexibleBufferBuilder vertex(Vec3Access pos) {
		dispatch.positionHolder.c0 = (float) pos.x();
		dispatch.positionHolder.c1 = (float) pos.y();
		dispatch.positionHolder.c2 = (float) pos.z();
		return this;
	}

	@Override
	public FlexibleBufferBuilder color(Color color) {
		dispatch.colorHolder.c0 = color.r();
		dispatch.colorHolder.c1 = color.g();
		dispatch.colorHolder.c2 = color.b();
		dispatch.colorHolder.c3 = color.a();
		return this;
	}

	@Override
	public FlexibleBufferBuilder color(float r, float g, float b, float a) {
		dispatch.colorHolder.c0 = r;
		dispatch.colorHolder.c1 = g;
		dispatch.colorHolder.c2 = b;
		dispatch.colorHolder.c3 = a;
		return this;
	}

	@Override
	public FlexibleBufferBuilder uv0(float u, float v) {
		dispatch.uv0Holder.c0 = u;
		dispatch.uv0Holder.c1 = v;
		return this;
	}

	@Override
	public FlexibleBufferBuilder uv1(float u, float v) {
		dispatch.uv1Holder.c0 = u;
		dispatch.uv1Holder.c1 = v;
		return this;
	}

	@Override
	public FlexibleBufferBuilder uv2(float u, float v) {
		dispatch.uv2Holder.c0 = u;
		dispatch.uv2Holder.c1 = v;
		return this;
	}

	@Override
	public FlexibleBufferBuilder normal(double x, double y, double z) {
		dispatch.normalHolder.c0 = (float) x;
		dispatch.normalHolder.c1 = (float) y;
		dispatch.normalHolder.c2 = (float) z;
		return this;
	}

	@Override
	public FlexibleBufferBuilder normal(float x, float y, float z) {
		dispatch.normalHolder.c0 = x;
		dispatch.normalHolder.c1 = y;
		dispatch.normalHolder.c2 = z;
		return this;
	}

	@Override
	public FlexibleBufferBuilder normal(Vec3Access norm) {
		dispatch.normalHolder.c0 = (float) norm.x();
		dispatch.normalHolder.c1 = (float) norm.y();
		dispatch.normalHolder.c2 = (float) norm.z();
		return this;
	}

	private static int roundUp(int bytesToGrow) {
		int growthBucketSize = GROWTH_SIZE;
		if (bytesToGrow == 0)
			return growthBucketSize;
		if (bytesToGrow < 0)
			growthBucketSize = -growthBucketSize;
		int bytesAboveBucketStart = bytesToGrow % growthBucketSize;
		if (bytesAboveBucketStart == 0)
			return bytesToGrow;
		return bytesToGrow + growthBucketSize - bytesAboveBucketStart;
	}

	private void ensureBufferCapacity(int additionalBytes) {
		if (this.vertexByteOffset + additionalBytes <= this.buffer.capacity())
			return;
		final var oldSize = this.buffer.capacity();
		final var newSize = oldSize + roundUp(additionalBytes);
		LOGGER.info("Needed to grow FlexibleBufferBuilder buffer: Old size {} bytes, new size {} bytes.",
				oldSize, newSize);
		this.buffer = MemoryTracker.resize(this.buffer, newSize);
		this.buffer.rewind();
	}

}