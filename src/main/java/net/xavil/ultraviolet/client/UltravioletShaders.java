package net.xavil.ultraviolet.client;

import java.util.function.Supplier;

import net.minecraft.resources.ResourceLocation;
import net.xavil.hawklib.client.HawkRendering;
import net.xavil.hawklib.client.HawkShaders;
import net.xavil.hawklib.client.flexible.BufferLayout;
import net.xavil.hawklib.client.gl.GlFragmentWrites;
import net.xavil.hawklib.client.gl.shader.ShaderAttributeSet;
import net.xavil.hawklib.client.gl.shader.ShaderAttributeSet.InstanceRate;
import net.xavil.hawklib.client.gl.shader.ShaderProgram;
import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.client.screen.layer.TextBuilder;

public class UltravioletShaders extends HawkShaders {

	protected UltravioletShaders() {
	}

	// @formatter:off
	public static final ResourceLocation SHADER_CELESTIAL_NODE_LOCATION = Mod.namespaced("main/celestial_node.glsl");
	public static final ResourceLocation SHADER_RING_LOCATION = Mod.namespaced("main/ring.glsl");
	public static final ResourceLocation SHADER_STAR_BILLBOARD_REALISTIC_LOCATION = Mod.namespaced("main/star_billboard_realistic.glsl");
	public static final ResourceLocation SHADER_STAR_BILLBOARD_UI_LOCATION = Mod.namespaced("main/star_billboard_ui.glsl");
	public static final ResourceLocation SHADER_STAR_LOCATION = Mod.namespaced("main/star.glsl");
	public static final ResourceLocation SHADER_GALAXY_PARTICLE_LOCATION = Mod.namespaced("main/galaxy_particle.glsl");
	public static final ResourceLocation SHADER_SKYBOX_LOCATION = Mod.namespaced("main/skybox.glsl");
	
	public static final ResourceLocation SHADER_BLOOM_DOWNSAMPLE_LOCATION = Mod.namespaced("post/bloom/downsample.glsl");
	public static final ResourceLocation SHADER_BLOOM_UPSAMPLE_LOCATION = Mod.namespaced("post/bloom/upsample.glsl");
	public static final ResourceLocation SHADER_MAIN_POSTPROCESS_LOCATION = Mod.namespaced("post/main_post.glsl");
	public static final ResourceLocation SHADER_UN_VANILLA_LOCATION = Mod.namespaced("post/unvanilla.glsl");

	public static final ResourceLocation SHADER_ATMOSPHERE_LOCATION = Mod.namespaced("post/atmosphere.glsl");
	public static final ResourceLocation SHADER_GRAVITATIONAL_LENSING_LOCATION = Mod.namespaced("post/gravitational_lensing.glsl");

	public static final ResourceLocation SHADER_UI_POINTS_LOCATION = Mod.namespaced("main/ui/points_generic.glsl");
	public static final ResourceLocation SHADER_UI_QUADS_LOCATION = Mod.namespaced("main/ui/quads_generic.glsl");
	public static final ResourceLocation SHADER_TEXT_LOCATION = Mod.namespaced("main/text.glsl");

	public static final ResourceLocation SHADER_COMPUTE_LUMINANCE_HISTOGRAM_LOCATION = Mod.namespaced("compute/luminance_histogram.glsl");
	public static final ResourceLocation SHADER_COMPUTE_LUMINANCE_PREPROCESS_LOCATION = Mod.namespaced("compute/luminance_preprocess.glsl");

	public static final Supplier<ShaderProgram> SHADER_CELESTIAL_NODE = () -> getShader(SHADER_CELESTIAL_NODE_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_RING = () -> getShader(SHADER_RING_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_STAR_BILLBOARD_REALISTIC = () -> getShader(SHADER_STAR_BILLBOARD_REALISTIC_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_STAR_BILLBOARD_UI = () -> getShader(SHADER_STAR_BILLBOARD_UI_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_STAR = () -> getShader(SHADER_STAR_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_GALAXY_PARTICLE = () -> getShader(SHADER_GALAXY_PARTICLE_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_SKYBOX = () -> getShader(SHADER_SKYBOX_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_BLOOM_DOWNSAMPLE = () -> getShader(SHADER_BLOOM_DOWNSAMPLE_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_BLOOM_UPSAMPLE = () -> getShader(SHADER_BLOOM_UPSAMPLE_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_MAIN_POSTPROCESS = () -> getShader(SHADER_MAIN_POSTPROCESS_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_ATMOSPHERE = () -> getShader(SHADER_ATMOSPHERE_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_GRAVITATIONAL_LENSING = () -> getShader(SHADER_GRAVITATIONAL_LENSING_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_UI_POINTS = () -> getShader(SHADER_UI_POINTS_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_UI_QUADS = () -> getShader(SHADER_UI_QUADS_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_TEXT = () -> getShader(SHADER_TEXT_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_UN_VANILLA = () -> getShader(SHADER_UN_VANILLA_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_COMPUTE_LUMINANCE_HISTOGRAM = () -> getShader(SHADER_COMPUTE_LUMINANCE_HISTOGRAM_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_COMPUTE_LUMINANCE_PREPROCESS = () -> getShader(SHADER_COMPUTE_LUMINANCE_PREPROCESS_LOCATION);
	// @formatter:on

	// for text shader
	public static final ShaderAttributeSet POSITION_COLOR_TEX_BATCHID = ShaderAttributeSet.builder()
			.attrib("aPos", BufferLayout.Attribute.POSITION, ShaderAttributeSet.FLOAT3, InstanceRate.PER_VERTEX)
			.attrib("aColor", BufferLayout.Attribute.COLOR, ShaderAttributeSet.FLOAT4, InstanceRate.PER_VERTEX)
			.attrib("aTexCoord0", BufferLayout.Attribute.UV0, ShaderAttributeSet.FLOAT2, InstanceRate.PER_VERTEX)
			.attrib("aBatchId", TextBuilder.USAGE_BATCH_ID, ShaderAttributeSet.INT1, InstanceRate.PER_VERTEX)
			.build();

	public static void registerShaders(HawkRendering.ShaderSink acceptor) {
		// @formatter:off
		acceptor.accept(SHADER_STAR_BILLBOARD_REALISTIC_LOCATION, ShaderAttributeSet.POSITION_COLOR_TEX, GlFragmentWrites.COLOR_ONLY);
		acceptor.accept(SHADER_STAR_BILLBOARD_UI_LOCATION,        ShaderAttributeSet.POSITION_COLOR, GlFragmentWrites.COLOR_ONLY);
		acceptor.accept(SHADER_CELESTIAL_NODE_LOCATION,           ShaderAttributeSet.POSITION_TEX_COLOR_NORMAL, GlFragmentWrites.COLOR_ONLY);
		acceptor.accept(SHADER_RING_LOCATION,                     ShaderAttributeSet.POSITION_TEX_COLOR_NORMAL, GlFragmentWrites.COLOR_ONLY);
		acceptor.accept(SHADER_GALAXY_PARTICLE_LOCATION,          ShaderAttributeSet.POSITION_COLOR_TEX, GlFragmentWrites.COLOR_ONLY);
		acceptor.accept(SHADER_SKYBOX_LOCATION,                   ShaderAttributeSet.POSITION, GlFragmentWrites.COLOR_ONLY);
		acceptor.accept(SHADER_BLIT_LOCATION,                     ShaderAttributeSet.POSITION_TEX, GlFragmentWrites.COLOR_ONLY);

		acceptor.accept(SHADER_BLOOM_DOWNSAMPLE_LOCATION, ShaderAttributeSet.POSITION_TEX, GlFragmentWrites.COLOR_ONLY);
		acceptor.accept(SHADER_BLOOM_UPSAMPLE_LOCATION,   ShaderAttributeSet.POSITION_TEX, GlFragmentWrites.COLOR_ONLY);
		acceptor.accept(SHADER_MAIN_POSTPROCESS_LOCATION, ShaderAttributeSet.POSITION_TEX, GlFragmentWrites.COLOR_ONLY);
		acceptor.accept(SHADER_UN_VANILLA_LOCATION,       ShaderAttributeSet.POSITION_TEX, GlFragmentWrites.COLOR_ONLY);

		acceptor.accept(SHADER_ATMOSPHERE_LOCATION,            ShaderAttributeSet.POSITION_TEX, GlFragmentWrites.COLOR_ONLY);
		acceptor.accept(SHADER_GRAVITATIONAL_LENSING_LOCATION, ShaderAttributeSet.POSITION_TEX, GlFragmentWrites.COLOR_ONLY);

		acceptor.accept(SHADER_UI_POINTS_LOCATION, ShaderAttributeSet.POSITION_COLOR_TEX, GlFragmentWrites.COLOR_ONLY);
		acceptor.accept(SHADER_UI_QUADS_LOCATION,  ShaderAttributeSet.POSITION_COLOR_TEX, GlFragmentWrites.COLOR_ONLY);
		acceptor.accept(SHADER_TEXT_LOCATION,      POSITION_COLOR_TEX_BATCHID,      GlFragmentWrites.COLOR_ONLY);

		acceptor.accept(SHADER_COMPUTE_LUMINANCE_HISTOGRAM_LOCATION, ShaderAttributeSet.EMPTY, GlFragmentWrites.EMPTY);
		acceptor.accept(SHADER_COMPUTE_LUMINANCE_PREPROCESS_LOCATION, ShaderAttributeSet.EMPTY, GlFragmentWrites.EMPTY);
		// @formatter:on
	}
}
