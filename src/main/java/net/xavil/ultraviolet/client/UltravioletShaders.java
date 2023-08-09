package net.xavil.ultraviolet.client;

import net.minecraft.resources.ResourceLocation;
import net.xavil.hawklib.client.HawkShaders;
import net.xavil.ultraviolet.Mod;

public class UltravioletShaders extends HawkShaders {

	protected UltravioletShaders() {
	}

	public static final ResourceLocation SHADER_PLANET = Mod.namespaced("main/planet.glsl");
	public static final ResourceLocation SHADER_RING = Mod.namespaced("main/ring.glsl");
	public static final ResourceLocation SHADER_STAR_BILLBOARD_REALISTIC = Mod.namespaced("main/star_billboard_realistic.glsl");
	public static final ResourceLocation SHADER_STAR_BILLBOARD_UI = Mod.namespaced("main/star_billboard_ui.glsl");
	public static final ResourceLocation SHADER_STAR = Mod.namespaced("main/star.glsl");
	public static final ResourceLocation SHADER_GALAXY_PARTICLE = Mod.namespaced("main/galaxy_particle.glsl");
	public static final ResourceLocation SHADER_SKYBOX = Mod.namespaced("main/skybox.glsl");

	public static final ResourceLocation SHADER_BLOOM_DOWNSAMPLE = Mod.namespaced("post/bloom/downsample.glsl");
	public static final ResourceLocation SHADER_BLOOM_UPSAMPLE = Mod.namespaced("post/bloom/upsample.glsl");
	public static final ResourceLocation SHADER_MAIN_POSTPROCESS = Mod.namespaced("post/main_post.glsl");

}
