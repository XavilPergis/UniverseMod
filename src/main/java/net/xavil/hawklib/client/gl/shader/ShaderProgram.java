package net.xavil.hawklib.client.gl.shader;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL45C;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Matrix4f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.xavil.hawklib.Assert;
import net.xavil.hawklib.HawkLib;
import net.xavil.hawklib.client.gl.GlBuffer;
import net.xavil.hawklib.client.gl.GlFragmentWrites;
import net.xavil.hawklib.client.gl.GlManager;
import net.xavil.hawklib.client.gl.GlObject;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.MutableMap;
import net.xavil.hawklib.collections.iterator.Iterator;
import net.xavil.hawklib.math.matrices.interfaces.Mat4Access;

public final class ShaderProgram extends GlObject implements UniformHolder {

	private ResourceLocation loadedFrom = null;
	private boolean areUniformsDirty = false;
	private boolean hasTextureUniforms = false;
	private final MutableMap<String, UniformSlot> uniforms = MutableMap.hashMap();
	private final MutableMap<String, StorageBufferSlot> storageBuffers = MutableMap.hashMap();
	private ShaderAttributeSet attributeSet;
	private GlFragmentWrites fragmentWrites;

	private ShaderInstance wrappedVanillaShader;

	public ShaderProgram(int id, boolean owned) {
		super(ObjectType.PROGRAM, id, owned);
	}

	public ShaderProgram(ShaderInstance imported) {
		super(ObjectType.PROGRAM, imported.getId(), false);
		this.wrappedVanillaShader = imported;
		this.attributeSet = ShaderAttributeSet.fromVanilla(imported.getVertexFormat());
		queryUniforms();
		setupFragLocations(GlFragmentWrites.VANILLA);
	}

	public ShaderProgram(@Nullable ResourceLocation loadedFrom) {
		super(ObjectType.PROGRAM, GL45C.glCreateProgram(), true);
		this.loadedFrom = loadedFrom;
	}

	@Override
	public String debugDescription() {
		var desc = super.debugDescription();
		if (this.loadedFrom != null)
			desc += " (" + this.loadedFrom.toString() + ")";
		return desc;
	}

	@Override
	public void close() {
		super.close();
	}

	public ShaderAttributeSet attributeSet() {
		return this.attributeSet;
	}

	public Iterator<UniformSlot> uniforms() {
		return this.uniforms.values();
	}

	public GlFragmentWrites fragmentWrites() {
		return this.fragmentWrites;
	}

	public ShaderInstance getWrappedVanillaShader() {
		return this.wrappedVanillaShader;
	}

	public void attachShader(ShaderStage shader) {
		GlStateManager.glAttachShader(this.id, shader.id);
	}

	public String infoLog() {
		return GL45C.glGetProgramInfoLog(this.id);
	}

	public boolean link(ShaderAttributeSet attributeSet, GlFragmentWrites fragmentWrites) {
		if (!this.owned) {
			HawkLib.LOGGER.warn("{}: linking ShaderProgram that is not owned!", toString());
		}
		GL45C.glLinkProgram(this.id);
		final var status = GL45C.glGetProgrami(this.id, GL45C.GL_LINK_STATUS);
		if (status == GL45C.GL_FALSE)
			return false;
		this.attributeSet = attributeSet;
		this.uniforms.clear();
		this.hasTextureUniforms = false;
		queryUniforms();
		setupAttribBindings(attributeSet);
		setupFragLocations(fragmentWrites);
		return true;
	}

	private void queryUniforms() {
		final var uniformProperties = new ResourceQueryHelper() {
			final Param type = addQueryParam(GL45C.GL_TYPE);
			final Param arraySize = addQueryParam(GL45C.GL_ARRAY_SIZE);
			final Param blockIndex = addQueryParam(GL45C.GL_BLOCK_INDEX);
			final Param location = addQueryParam(GL45C.GL_LOCATION);
		};

		final var storageProperties = new ResourceQueryHelper() {
			final Param bufferBinding = addQueryParam(GL45C.GL_BUFFER_BINDING);
		};

		final var activeUniformCount = GL45C.glGetProgramInterfacei(this.id, GL45C.GL_UNIFORM,
				GL45C.GL_ACTIVE_RESOURCES);
		for (int i = 0; i < activeUniformCount; ++i) {
			uniformProperties.query(this, GL45C.GL_UNIFORM, i);
			// this uniform is part of a uniform block; skip it for now
			if (uniformProperties.blockIndex.get() != -1)
				continue;
			final var name = GL45C.glGetProgramResourceName(this.id, GL45C.GL_UNIFORM, i);
			final var type = UniformSlot.Type.from(uniformProperties.type.get());
			final var arraySize = uniformProperties.arraySize.get();
			final var location = uniformProperties.location.get();

			this.uniforms.insertAndGet(name, new UniformSlot(type, name, arraySize, location));
			this.hasTextureUniforms |= type.isTexture;
		}

		final var activeStorageBufferCount = GL45C.glGetProgramInterfacei(this.id, GL45C.GL_SHADER_STORAGE_BLOCK,
				GL45C.GL_ACTIVE_RESOURCES);
		for (int i = 0; i < activeStorageBufferCount; ++i) {
			storageProperties.query(this, GL45C.GL_SHADER_STORAGE_BLOCK, i);
			final var name = GL45C.glGetProgramResourceName(this.id, GL45C.GL_SHADER_STORAGE_BLOCK, i);
			final var bufferBinding = storageProperties.bufferBinding.get();
			this.storageBuffers.insertAndGet(name, new StorageBufferSlot(name, bufferBinding));
			// Mod.LOGGER.error("shader storage block {} '{}': binding={}", i, name,
			// bufferBinding);
		}

	}

	private void setupAttribBindings(ShaderAttributeSet attributeSet) {
		for (int i = 0; i < attributeSet.attributes.size(); ++i) {
			final var attrib = attributeSet.attributes.get(i);
			GL45C.glBindAttribLocation(this.id, attrib.attribIndex, attrib.name);
		}
	}

	private void setupFragLocations(GlFragmentWrites fragmentWrites) {
		final var count = fragmentWrites.getFragmentWriteCount();
		for (int i = 0; i < count; ++i) {
			GL45C.glBindFragDataLocation(this.id, i, fragmentWrites.getFragmentWriteName(i));
		}
		this.fragmentWrites = fragmentWrites;
	}

	@Override
	public UniformSlot getSlot(String uniformName) {
		return this.uniforms.getOrNull(uniformName);
	}

	@Override
	public void markDirty(boolean dirty) {
		this.areUniformsDirty |= dirty;
	}

	public void setStorageBuffer(String blockName, GlBuffer.Slice bufferSlice) {
		final var slot = this.storageBuffers.get(blockName).unwrapOrNull();
		if (slot == null)
			return;
		slot.bufferSlice = bufferSlice;
	}

	public void bind() {
		GlManager.useProgram(this.id);
		// normal uniforms
		if (this.hasTextureUniforms || this.areUniformsDirty) {
			// since texture unit binding can change sorta whenever, we always have to check
			// that the texture currently stored in a sampler uniform is the active texture
			// for the current texture unit.
			final var uploadContext = new UniformSlot.UploadContext();
			this.uniforms.values().forEach(slot -> slot.upload(this, uploadContext));
			this.areUniformsDirty = false;
		}

		this.storageBuffers.values().forEach(slot -> slot.bind());

		// apply vanilla shader stuff
		if (this.wrappedVanillaShader != null) {
			this.wrappedVanillaShader.apply();
		}
	}

	public static void unbind() {
		GlManager.useProgram(0);
	}

	public void setupDefaultShaderUniforms() {
		setupDefaultShaderUniforms(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix());
	}

	private void setupDefaultShaderUniforms(Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
		setUniformf("uViewMatrix", Mat4Access.from(modelViewMatrix));
		setUniformf("uProjectionMatrix", Mat4Access.from(projectionMatrix));

		final var window = Minecraft.getInstance().getWindow();
		setUniformf("uScreenSize", (float) window.getWidth(), (float) window.getHeight());

		setupDefaultVanillaUniforms(this.getWrappedVanillaShader());
	}

	private static void setupDefaultVanillaUniforms(@Nullable ShaderInstance shader) {
		if (shader == null)
			return;
		if (shader.MODEL_VIEW_MATRIX != null)
			shader.MODEL_VIEW_MATRIX.set(RenderSystem.getModelViewMatrix());
		if (shader.PROJECTION_MATRIX != null)
			shader.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
		if (shader.INVERSE_VIEW_ROTATION_MATRIX != null)
			shader.INVERSE_VIEW_ROTATION_MATRIX.set(RenderSystem.getInverseViewRotationMatrix());
		if (shader.COLOR_MODULATOR != null)
			shader.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
		if (shader.FOG_START != null)
			shader.FOG_START.set(RenderSystem.getShaderFogStart());
		if (shader.FOG_END != null)
			shader.FOG_END.set(RenderSystem.getShaderFogEnd());
		if (shader.FOG_COLOR != null)
			shader.FOG_COLOR.set(RenderSystem.getShaderFogColor());
		if (shader.FOG_SHAPE != null)
			shader.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
		if (shader.TEXTURE_MATRIX != null)
			shader.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
		if (shader.GAME_TIME != null)
			shader.GAME_TIME.set(RenderSystem.getShaderGameTime());
		final var window = Minecraft.getInstance().getWindow();
		if (shader.SCREEN_SIZE != null)
			shader.SCREEN_SIZE.set((float) window.getWidth(), (float) window.getHeight());
		if (shader.LINE_WIDTH != null)
			shader.LINE_WIDTH.set(RenderSystem.getShaderLineWidth());
		RenderSystem.setupShaderLights(shader);
	}

	private static final class StorageBufferSlot {
		public final String blockName;
		public final int bindingIndex;

		public GlBuffer.Slice bufferSlice = null;

		public StorageBufferSlot(String blockName, int bindingIndex) {
			this.blockName = blockName;
			this.bindingIndex = bindingIndex;
		}

		public void bind() {
			Assert.isNotNull(this.bufferSlice);
			this.bufferSlice.bindRange(GlBuffer.Type.SHADER_STORAGE, this.bindingIndex);
			this.bufferSlice = null;
		}
	}

	// me when i overengineer everything
	private static abstract class ResourceQueryHelper {
		public static final class Param {
			public final int key;
			private int value;

			public Param(int key) {
				this.key = key;
			}

			public int get() {
				return this.value;
			}
		}

		private final Vector<Param> params = new Vector<>();
		private int[] propertyKeys, propertyValues;

		protected Param addQueryParam(int queryKey) {
			final var param = new Param(queryKey);
			this.params.push(param);
			return param;
		}

		public void query(ShaderProgram program, int iface, int index) {
			if (this.propertyKeys == null || this.propertyKeys.length != this.params.size()) {
				this.propertyKeys = new int[this.params.size()];
				for (int i = 0; i < this.params.size(); ++i)
					this.propertyKeys[i] = this.params.get(i).key;
			}
			if (this.propertyValues == null || this.propertyValues.length != this.params.size())
				this.propertyValues = new int[this.params.size()];
			GL45C.glGetProgramResourceiv(program.id, iface, index, this.propertyKeys, null, this.propertyValues);
			for (int i = 0; i < this.params.size(); ++i)
				this.params.get(i).value = this.propertyValues[i];
		}
	}

}
