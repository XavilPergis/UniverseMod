package net.xavil.hawklib.client.gl;

public interface BufferSliceable {

	GlBuffer.Slice slice();

	default GlBuffer.Slice slice(long offset, long length) {
		return slice().slice(offset, length);
	}

	default GlBuffer.Slice sliceFrom(long index) {
		final var slice = slice();
		return slice.slice(index, slice.size - index);
	}

	default GlBuffer.Slice sliceTo(long index) {
		return slice().slice(0, index);
	}

}