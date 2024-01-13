package net.xavil.ultraviolet.client;

import java.util.function.Supplier;

import net.minecraft.resources.ResourceLocation;
import net.xavil.hawklib.client.HawkRendering;
import net.xavil.hawklib.client.HawkShaders;
import net.xavil.hawklib.client.gl.GlFragmentWrites;
import net.xavil.hawklib.client.gl.shader.AttributeSet;
import net.xavil.hawklib.client.gl.shader.ShaderProgram;
import net.xavil.ultraviolet.Mod;

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

	public static final ResourceLocation SHADER_ATMOSPHERE_LOCATION = Mod.namespaced("post/atmosphere.glsl");
	public static final ResourceLocation SHADER_GRAVITATIONAL_LENSING_LOCATION = Mod.namespaced("post/gravitational_lensing.glsl");

	public static final ResourceLocation SHADER_UI_POINTS_LOCATION = Mod.namespaced("main/ui/points_generic.glsl");

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
	// @formatter:on

	public static void registerShaders(HawkRendering.ShaderSink acceptor) {
		// @formatter:off
		acceptor.accept(SHADER_STAR_BILLBOARD_REALISTIC_LOCATION, AttributeSet.POSITION_COLOR_TEX, GlFragmentWrites.COLOR_ONLY);
		acceptor.accept(SHADER_STAR_BILLBOARD_UI_LOCATION,        AttributeSet.POSITION_COLOR, GlFragmentWrites.COLOR_ONLY);
		acceptor.accept(SHADER_CELESTIAL_NODE_LOCATION,           AttributeSet.POSITION_TEX_COLOR_NORMAL, GlFragmentWrites.COLOR_ONLY);
		acceptor.accept(SHADER_RING_LOCATION,                     AttributeSet.POSITION_TEX_COLOR_NORMAL, GlFragmentWrites.COLOR_ONLY);
		acceptor.accept(SHADER_GALAXY_PARTICLE_LOCATION,          AttributeSet.POSITION_COLOR_TEX, GlFragmentWrites.COLOR_ONLY);
		acceptor.accept(SHADER_SKYBOX_LOCATION,                   AttributeSet.POSITION, GlFragmentWrites.COLOR_ONLY);
		acceptor.accept(SHADER_BLIT_LOCATION,                     AttributeSet.POSITION_TEX, GlFragmentWrites.COLOR_ONLY);

		acceptor.accept(SHADER_BLOOM_DOWNSAMPLE_LOCATION, AttributeSet.POSITION_TEX, GlFragmentWrites.COLOR_ONLY);
		acceptor.accept(SHADER_BLOOM_UPSAMPLE_LOCATION,   AttributeSet.POSITION_TEX, GlFragmentWrites.COLOR_ONLY);
		acceptor.accept(SHADER_MAIN_POSTPROCESS_LOCATION, AttributeSet.POSITION_TEX, GlFragmentWrites.COLOR_ONLY);

		acceptor.accept(SHADER_ATMOSPHERE_LOCATION,            AttributeSet.POSITION_TEX, GlFragmentWrites.COLOR_ONLY);
		acceptor.accept(SHADER_GRAVITATIONAL_LENSING_LOCATION, AttributeSet.POSITION_TEX, GlFragmentWrites.COLOR_ONLY);

		acceptor.accept(SHADER_UI_POINTS_LOCATION, AttributeSet.POSITION_COLOR, GlFragmentWrites.COLOR_ONLY);
		// @formatter:on
	}
}
