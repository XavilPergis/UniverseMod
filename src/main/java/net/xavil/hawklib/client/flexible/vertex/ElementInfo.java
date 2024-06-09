package net.xavil.hawklib.client.flexible.vertex;

import java.nio.ByteBuffer;

import net.xavil.hawklib.client.flexible.BufferLayout;
import net.xavil.hawklib.client.gl.ComponentType;
import net.xavil.hawklib.math.matrices.interfaces.Mat4Access;

public abstract class ElementInfo {

    public final BufferLayout.BuiltElement element;
	public final int[] offsets;

	public ElementInfo(BufferLayout.BuiltElement element) {
		this.element = element;
		final var componentCount = element.attribSlotCount * element.componentCount;
		this.offsets = new int[componentCount];
		for (int i = 0; i < componentCount; ++i) {
			this.offsets[i] = (i * element.type.byteSize) + element.byteOffset;
		}
	}

	public static final class Float extends ElementInfo {
    	public final ByteBuffer buffer;
    	public final ComponentType.Writer writer;
    
    	public Float(ByteBuffer buffer, BufferLayout.BuiltElement element) {
    		super(element);
    		this.buffer = buffer;
    		this.writer = element.type.writer;
    	}
    
    	public void setFloat(int vertexBase, int component, float value) {
    		this.writer.writeFloat(this.buffer, vertexBase + this.offsets[component], value);
    	}
    
    	public void setFloats(int vertexBase, float c0) {
    		int i = 0;
    		this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], c0);
    	}
    
    	public void setFloats(int vertexBase, float c0, float c1) {
    		int i = 0;
    		this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], c0);
    		this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], c1);
    	}
    
    	public void setFloats(int vertexBase, float c0, float c1, float c2) {
    		int i = 0;
    		this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], c0);
    		this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], c1);
    		this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], c2);
    	}
    
    	public void setFloats(int vertexBase, float c0, float c1, float c2, float c3) {
    		int i = 0;
    		this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], c0);
    		this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], c1);
    		this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], c2);
    		this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], c3);
    	}
    
    	public void setFloats(int vertexBase, Mat4Access value) {
    		int i = 0;
    		this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], (float) value.r0c0());
    		this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], (float) value.r1c0());
    		this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], (float) value.r2c0());
    		this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], (float) value.r3c0());
    		this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], (float) value.r0c1());
    		this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], (float) value.r1c1());
    		this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], (float) value.r2c1());
    		this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], (float) value.r3c1());
    		this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], (float) value.r0c2());
    		this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], (float) value.r1c2());
    		this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], (float) value.r2c2());
    		this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], (float) value.r3c2());
    		this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], (float) value.r0c3());
    		this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], (float) value.r1c3());
    		this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], (float) value.r2c3());
    		this.writer.writeFloat(this.buffer, vertexBase + this.offsets[i++], (float) value.r3c3());
    	}
    }

    public static final class Int extends ElementInfo {
    	public final ByteBuffer buffer;
    	public final ComponentType.Writer writer;
    
    	public Int(ByteBuffer buffer, BufferLayout.BuiltElement element) {
    		super(element);
    		this.buffer = buffer;
    		this.writer = element.type.writer;
    	}
    
    	public void setInt(int vertexBase, int component, int value) {
    		this.writer.writeInt(this.buffer, vertexBase + this.offsets[component], value);
    	}
    }

}