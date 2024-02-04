package net.xavil.hawklib.client.gl.shader;

import java.nio.IntBuffer;

import org.lwjgl.opengl.GL45C;
import org.lwjgl.system.MemoryStack;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.math.Matrix4f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.xavil.hawklib.HawkLib;
import net.xavil.hawklib.client.gl.GlFragmentWrites;
import net.xavil.hawklib.client.gl.GlManager;
import net.xavil.hawklib.client.gl.GlObject;
import net.xavil.hawklib.collections.interfaces.MutableMap;
import net.xavil.hawklib.collections.iterator.Iterator;
import net.xavil.hawklib.math.matrices.interfaces.Mat4Access;

public final class ShaderProgram extends GlObject implements UniformHolder {

	public ShaderProgram(int id, boolean owned) {
		super(id, owned);
	}

	public ShaderProgram(ShaderInstance imported) {
		super(imported.getId(), false);
		this.wrappedVanillaShader = imported;
		queryUniforms();
		setupAttribBindings(imported.getVertexFormat(), false);
		setupFragLocations(GlFragmentWrites.VANILLA);
	}

	public ShaderProgram() {
		super(GL45C.glCreateProgram(), true);
	}

	public ShaderProgram(ResourceLocation loadedFrom) {
		this();
		this.loadedFrom = loadedFrom;
	}

	@Override
	protected void destroy() {
		GL45C.glDeleteProgram(this.id);
	}

	@Override
	public ObjectType objectType() {
		return ObjectType.PROGRAM;
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
		// this.uniformBlocks.values().forEach(UniformBuffer::close);
	}

	public static final class AttributeSlot {
		public final String name;
		public final AttributeSet.Attribute element;

		public AttributeSlot(String name, AttributeSet.Attribute element) {
			this.name = name;
			this.element = element;
		}
	}

	private ResourceLocation loadedFrom = null;
	private boolean areUniformsDirty = false;
	private boolean hasAnySamplerUniform = false;
	private final MutableMap<String, UniformSlot> uniforms = MutableMap.hashMap();
	private final MutableMap<String, AttributeSlot> attributes = MutableMap.hashMap();
	private AttributeSet attributeSet;
	private GlFragmentWrites fragmentWrites;

	private ShaderInstance wrappedVanillaShader;

	public AttributeSet attributeSet() {
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

	public boolean link(AttributeSet attributeSet, GlFragmentWrites fragmentWrites) {
		if (!this.owned) {
			HawkLib.LOGGER.warn("{}: linking ShaderProgram that is not owned!", toString());
		}
		GL45C.glLinkProgram(this.id);
		final var status = GL45C.glGetProgrami(this.id, GL45C.GL_LINK_STATUS);
		if (status == GL45C.GL_FALSE)
			return false;
		this.attributeSet = attributeSet;
		this.uniforms.clear();
		this.attributes.clear();
		this.hasAnySamplerUniform = false;
		queryUniforms();
		setupAttribBindings(attributeSet, true);
		setupFragLocations(fragmentWrites);
		return true;
	}

	public UniformSlot queryUniform(int index) {
		try (final var stack = MemoryStack.stackPush()) {
			final IntBuffer sizeBuffer = stack.mallocInt(1), typeBuffer = stack.mallocInt(1);
			final var name = GL45C.glGetActiveUniform(this.id, index, sizeBuffer, typeBuffer);
			final var type = UniformSlot.Type.from(typeBuffer.get(0));
			final var size = sizeBuffer.get(0);

			// filter out shader builtins
			if (name.startsWith("gl_"))
				return null;
			// TODO: support structs in some capacity
			if (name.endsWith("[0]"))
				name.substring(name.length() - "[0]".length());

			final var location = GL45C.glGetUniformLocation(this.id, name);
			return new UniformSlot(type, name, size, location);
		}
	}

	private void queryUniforms() {
		final var uniformCount = GL45C.glGetProgrami(this.id, GL45C.GL_ACTIVE_UNIFORMS);
		for (int i = 0; i < uniformCount; ++i) {
			final var slot = queryUniform(i);
			if (slot != null) {
				this.uniforms.insert(slot.name, slot);
				this.hasAnySamplerUniform |= slot.type.componentType == UniformSlot.ComponentType.SAMPLER;
			}
		}
	}

	private void setupAttribBindings(VertexFormat format, boolean bind) {
		final var attribNames = format.getElementAttributeNames();
		final var attribs = format.getElements();
		for (int i = 0; i < attribs.size(); ++i) {
			final var attrib = attribs.get(i);
			final var attribName = attribNames.get(i);
			if (attrib.getUsage() == VertexFormatElement.Usage.PADDING)
				continue;
			if (bind)
				GL45C.glBindAttribLocation(this.id, i, attribName);
		}
	}

	private void setupAttribBindings(AttributeSet attributeSet, boolean bind) {
		// FIXME: matrix attributes take up multiple attribute slots, which is not
		// accounted for here.
		for (int i = 0; i < attributeSet.attributes.size(); ++i) {
			final var attrib = attributeSet.attributes.get(i);
			if (bind)
				GL45C.glBindAttribLocation(this.id, i, attrib.name);
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

	// public void setUniformBlock(String blockName, UniformBuffer buffer) {
	// this.uniformBlocks.insert(blockName, buffer);
	// }

	public void bind() {
		GlManager.useProgram(this.id);
		// normal uniforms
		if (this.hasAnySamplerUniform || this.areUniformsDirty) {
			// since texture unit binding can change sorta whenever, we always have to check
			// that the texture currently stored in a sampler uniform is the active texture
			// for the current texture unit.
			final var uploadContext = new UniformSlot.UploadContext();
			this.uniforms.values().forEach(slot -> slot.upload(this, uploadContext));
			this.areUniformsDirty = false;
		}
		// TODO: uniform buffers

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

		// final var near = projectionMatrix.m23 / (projectionMatrix.m22 - 1.0);
		// final var far = projectionMatrix.m23 / (projectionMatrix.m22 + 1.0);
		// shader.setUniform("uCameraNear", near);
		// shader.setUniform("uCameraFar", far);

		setUniformf("uViewMatrix", Mat4Access.from(modelViewMatrix));
		setUniformf("uProjectionMatrix", Mat4Access.from(projectionMatrix));

		final var window = Minecraft.getInstance().getWindow();
		setUniformf("uScreenSize", (float) window.getWidth(), (float) window.getHeight());

		setupDefaultVanillaUniforms(this.getWrappedVanillaShader());
	}

	private static void setupDefaultVanillaUniforms(ShaderInstance shader) {
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

}
