package net.xavil.hawklib.client.gl;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL45C;

import net.xavil.hawklib.client.flexible.BufferLayout;
import net.xavil.hawklib.client.flexible.BufferLayoutSet;
import net.xavil.hawklib.client.gl.shader.ShaderAttributeSet;
import net.xavil.hawklib.collections.interfaces.MutableMap;
import net.xavil.ultraviolet.Mod;

public final class GlVertexArray extends GlObject {

	private static final MutableMap<ShaderAttributeSet, MutableMap<BufferLayoutSet, GlVertexArray>> VAO_CACHE = MutableMap
			.hashMap();

	private ShaderAttributeSet attribSet;
	private BufferLayoutSet layoutSet;

	public GlVertexArray(int id, boolean owned) {
		super(ObjectType.VERTEX_ARRAY, id, owned);
	}

	public GlVertexArray() {
		super(ObjectType.VERTEX_ARRAY, GL45C.glCreateVertexArrays(), true);
	}

	public void enableAttribute(int attribIndex, boolean enabled) {
		if (enabled)
			GL45C.glEnableVertexArrayAttrib(this.id, attribIndex);
		else
			GL45C.glDisableVertexArrayAttrib(this.id, attribIndex);
	}

	public void bindVertexBuffer(GlBuffer.Slice buffer, int bindingIndex, int stride) {
		GL45C.glVertexArrayVertexBuffer(this.id, bindingIndex, buffer.buffer.id, buffer.offset, stride);
	}

	public void bindElementBuffer(@Nullable GlBuffer buffer) {
		GL45C.glVertexArrayElementBuffer(this.id, buffer == null ? 0 : buffer.id);
	}

	public void setInstanceRate(int attribIndex, int instanceRate) {
		GL45C.glVertexArrayBindingDivisor(this.id, attribIndex, instanceRate);
	}

	public void setInstanceRate(int attribIndex, ShaderAttributeSet.InstanceRate instanceRate) {
		setInstanceRate(attribIndex, switch (instanceRate) {
			case PER_VERTEX -> 0;
			case PER_INSTANCE -> 1;
		});
	}

	public void setAttribFormat(int attribIndex, BufferLayout.AttributeType attribType, int componentCount,
			ComponentType componentType, int byteOffset) {
		switch (attribType) {
			case FLOAT -> GL45C.glVertexArrayAttribFormat(this.id,
					attribIndex,
					componentCount, componentType.gl, componentType.isNormalized,
					byteOffset);
			case INT -> GL45C.glVertexArrayAttribIFormat(this.id,
					attribIndex,
					componentCount, componentType.gl,
					byteOffset);
		}
	}

	public void setAttribBinding(int attribIndex, int bindingIndex) {
		GL45C.glVertexArrayAttribBinding(this.id, attribIndex, bindingIndex);
	}

	private static void compatCheckFailed(ShaderAttributeSet attribSet, BufferLayoutSet layoutSet) {
		Mod.LOGGER.error("Attribute set was incompatible with buffer layout set.");
		Mod.LOGGER.info("Attributes needed:");
		for (final var attrib : attribSet.attributes.iterable()) {
			Mod.LOGGER.info("- '{}': {}x{} {} ({})", attrib.name,
					attrib.attribType.componentCount,
					attrib.attribType.attribSlotCount,
					attrib.attribType.attribType.name,
					attrib.attrib);
		}
		Mod.LOGGER.info("Attributes provided:");
		for (final var entry : layoutSet.attributeSources.entries().iterable()) {
			final var attribSource = entry.getOrThrow();
			final var sourceLayout = layoutSet.layouts.get(attribSource.bufferIndex);
			final var sourceElement = sourceLayout.elements.get(attribSource.elementIndex);

			Mod.LOGGER.info("- {} -> {}[{}]: {}x{} {} ({})",
					entry.key, attribSource.bufferIndex, attribSource.elementIndex,
					sourceElement.componentCount, sourceElement.attribSlotOffset,
					sourceElement.attribType.name, sourceElement.attribute);
		}
		Mod.LOGGER.info("Complete Buffer Set Layout:");
		for (int i = 0; i < layoutSet.layouts.size(); ++i) {
			final var layout = layoutSet.layouts.get(i);
			Mod.LOGGER.info("- Buffer {}:", i);
			for (final var element : layout.elements.iterable()) {
				Mod.LOGGER.info("\t- {}x{} {} ({})",
						element.componentCount,
						element.attribSlotOffset, element.attribType.name, element.attribute);
			}
		}
		throw new IllegalArgumentException("buffer layout set was incompatible with shader attribute set.");
	}

	private static boolean isCompatible(
			ShaderAttributeSet.BuiltAttribute attrib,
			BufferLayout.BuiltElement element) {
		boolean compatible = true;
		compatible &= attrib.attrib == element.attribute;
		compatible &= attrib.attribIndex == element.attribSlotOffset;
		compatible &= attrib.attribType.attribType == element.attribType;
		compatible &= attrib.attribType.componentCount == element.componentCount;
		compatible &= attrib.attribType.attribSlotCount == element.attribSlotCount;
		return compatible;
	}

	private void setupCachedState(ShaderAttributeSet attribSet, BufferLayoutSet layoutSet) {
		Mod.LOGGER.info(" ===== VAO SETUP ===== ");
		this.attribSet = attribSet;
		this.layoutSet = layoutSet;

		for (final var attrib : attribSet.attributes.iterable()) {
			final var attribSourceRef = layoutSet.attributeSources.getOrThrow(attrib.attrib);

			final var sourceLayout = layoutSet.layouts.get(attribSourceRef.bufferIndex);
			final var sourceElement = sourceLayout.elements.get(attribSourceRef.elementIndex);

			if (!isCompatible(attrib, sourceElement))
				compatCheckFailed(attribSet, layoutSet);

			for (int i = 0; i < sourceElement.attribSlotCount; ++i) {
				final var attribIndex = attrib.attribIndex + i;
				// Mod.LOGGER.info();
				enableAttribute(attribIndex, true);
				Mod.LOGGER.info("enable {}", attribIndex);
				setAttribFormat(attribIndex,
						sourceElement.attribType, sourceElement.componentCount,
						sourceElement.type, sourceElement.byteOffset);
				Mod.LOGGER.info("format {} = attribType:{} componentCount:{} componentType:{} byteOffset:{}",
						attribIndex, sourceElement.attribType, sourceElement.componentCount, sourceElement.type,
						sourceElement.byteOffset);
				setAttribBinding(attribIndex, attribSourceRef.bufferIndex);
				Mod.LOGGER.info("binding {} = buffer {}", attribIndex, attribSourceRef.bufferIndex);
				setInstanceRate(attribIndex, attrib.instanceRate);
				Mod.LOGGER.info("instance rate {} = {}", attribIndex, attrib.instanceRate);
			}
		}
	}

	public static GlVertexArray cachedVertexArray(ShaderAttributeSet attribSet, BufferLayoutSet layoutSet) {
		final var layoutSets = VAO_CACHE.entry(attribSet).orInsertWith(MutableMap::hashMap);
		if (!layoutSets.containsKey(layoutSet)) {
			final var vao = new GlVertexArray();
			vao.setupCachedState(attribSet, layoutSet);
			layoutSets.insert(layoutSet, vao);
		}
		return layoutSets.getOrThrow(layoutSet);
	}

	public void bind() {
		GlManager.bindVertexArray(this.id);
	}

}
