package net.xavil.ultraviolet.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.client.gl.shader.ShaderProgram;
import net.xavil.ultraviolet.mixin.accessor.GameRendererAccessor;

public final class Shaders {

	private Shaders() {
	}

	public static final ResourceLocation SHADER_PLANET = Mod.namespaced("main/planet.glsl");
	public static final ResourceLocation SHADER_RING = Mod.namespaced("main/ring.glsl");
	public static final ResourceLocation SHADER_STAR_BILLBOARD = Mod.namespaced("main/star_billboard.glsl");
	public static final ResourceLocation SHADER_STAR = Mod.namespaced("main/star.glsl");
	public static final ResourceLocation SHADER_GALAXY_PARTICLE = Mod.namespaced("main/galaxy_particle.glsl");
	public static final ResourceLocation SHADER_SKYBOX = Mod.namespaced("main/skybox.glsl");
	public static final ResourceLocation SHADER_BLIT = Mod.namespaced("main/blit.glsl");

	public static final ResourceLocation SHADER_BLOOM_DOWNSAMPLE = Mod.namespaced("post/bloom/downsample.glsl");
	public static final ResourceLocation SHADER_BLOOM_UPSAMPLE = Mod.namespaced("post/bloom/upsample.glsl");
	public static final ResourceLocation SHADER_MAIN_POSTPROCESS = Mod.namespaced("post/main_post.glsl");

	// @formatter:off
	public static final String SHADER_VANILLA_BLOCK = "block";
	public static final String SHADER_VANILLA_NEW_ENTITY = "new_entity";
	public static final String SHADER_VANILLA_PARTICLE = "particle";
	public static final String SHADER_VANILLA_POSITION = "position";
	public static final String SHADER_VANILLA_POSITION_COLOR = "position_color";
	public static final String SHADER_VANILLA_POSITION_COLOR_LIGHTMAP = "position_color_lightmap";
	public static final String SHADER_VANILLA_POSITION_COLOR_TEX = "position_color_tex";
	public static final String SHADER_VANILLA_POSITION_COLOR_TEX_LIGHTMAP = "position_color_tex_lightmap";
	public static final String SHADER_VANILLA_POSITION_TEX = "position_tex";
	public static final String SHADER_VANILLA_POSITION_TEX_COLOR = "position_tex_color";
	public static final String SHADER_VANILLA_POSITION_TEX_COLOR_NORMAL = "position_tex_color_normal";
	public static final String SHADER_VANILLA_POSITION_TEX_LIGHTMAP_COLOR = "position_tex_lightmap_color";
	public static final String SHADER_VANILLA_RENDERTYPE_SOLID = "rendertype_solid";
	public static final String SHADER_VANILLA_RENDERTYPE_CUTOUT_MIPPED = "rendertype_cutout_mipped";
	public static final String SHADER_VANILLA_RENDERTYPE_CUTOUT = "rendertype_cutout";
	public static final String SHADER_VANILLA_RENDERTYPE_TRANSLUCENT = "rendertype_translucent";
	public static final String SHADER_VANILLA_RENDERTYPE_TRANSLUCENT_MOVING_BLOCK = "rendertype_translucent_moving_block";
	public static final String SHADER_VANILLA_RENDERTYPE_TRANSLUCENT_NO_CRUMBLING = "rendertype_translucent_no_crumbling";
	public static final String SHADER_VANILLA_RENDERTYPE_ARMOR_CUTOUT_NO_CULL = "rendertype_armor_cutout_no_cull";
	public static final String SHADER_VANILLA_RENDERTYPE_ENTITY_SOLID = "rendertype_entity_solid";
	public static final String SHADER_VANILLA_RENDERTYPE_ENTITY_CUTOUT = "rendertype_entity_cutout";
	public static final String SHADER_VANILLA_RENDERTYPE_ENTITY_CUTOUT_NO_CULL = "rendertype_entity_cutout_no_cull";
	public static final String SHADER_VANILLA_RENDERTYPE_ENTITY_CUTOUT_NO_CULL_Z_OFFSET = "rendertype_entity_cutout_no_cull_z_offset";
	public static final String SHADER_VANILLA_RENDERTYPE_ITEM_ENTITY_TRANSLUCENT_CULL = "rendertype_item_entity_translucent_cull";
	public static final String SHADER_VANILLA_RENDERTYPE_ENTITY_TRANSLUCENT_CULL = "rendertype_entity_translucent_cull";
	public static final String SHADER_VANILLA_RENDERTYPE_ENTITY_TRANSLUCENT = "rendertype_entity_translucent";
	public static final String SHADER_VANILLA_RENDERTYPE_ENTITY_SMOOTH_CUTOUT = "rendertype_entity_smooth_cutout";
	public static final String SHADER_VANILLA_RENDERTYPE_BEACON_BEAM = "rendertype_beacon_beam";
	public static final String SHADER_VANILLA_RENDERTYPE_ENTITY_DECAL = "rendertype_entity_decal";
	public static final String SHADER_VANILLA_RENDERTYPE_ENTITY_NO_OUTLINE = "rendertype_entity_no_outline";
	public static final String SHADER_VANILLA_RENDERTYPE_ENTITY_SHADOW = "rendertype_entity_shadow";
	public static final String SHADER_VANILLA_RENDERTYPE_ENTITY_ALPHA = "rendertype_entity_alpha";
	public static final String SHADER_VANILLA_RENDERTYPE_EYES = "rendertype_eyes";
	public static final String SHADER_VANILLA_RENDERTYPE_ENERGY_SWIRL = "rendertype_energy_swirl";
	public static final String SHADER_VANILLA_RENDERTYPE_LEASH = "rendertype_leash";
	public static final String SHADER_VANILLA_RENDERTYPE_WATER_MASK = "rendertype_water_mask";
	public static final String SHADER_VANILLA_RENDERTYPE_OUTLINE = "rendertype_outline";
	public static final String SHADER_VANILLA_RENDERTYPE_ARMOR_GLINT = "rendertype_armor_glint";
	public static final String SHADER_VANILLA_RENDERTYPE_ARMOR_ENTITY_GLINT = "rendertype_armor_entity_glint";
	public static final String SHADER_VANILLA_RENDERTYPE_GLINT_TRANSLUCENT = "rendertype_glint_translucent";
	public static final String SHADER_VANILLA_RENDERTYPE_GLINT = "rendertype_glint";
	public static final String SHADER_VANILLA_RENDERTYPE_GLINT_DIRECT = "rendertype_glint_direct";
	public static final String SHADER_VANILLA_RENDERTYPE_ENTITY_GLINT = "rendertype_entity_glint";
	public static final String SHADER_VANILLA_RENDERTYPE_ENTITY_GLINT_DIRECT = "rendertype_entity_glint_direct";
	public static final String SHADER_VANILLA_RENDERTYPE_TEXT = "rendertype_text";
	public static final String SHADER_VANILLA_RENDERTYPE_TEXT_INTENSITY = "rendertype_text_intensity";
	public static final String SHADER_VANILLA_RENDERTYPE_TEXT_SEE_THROUGH = "rendertype_text_see_through";
	public static final String SHADER_VANILLA_RENDERTYPE_TEXT_INTENSITY_SEE_THROUGH = "rendertype_text_intensity_see_through";
	public static final String SHADER_VANILLA_RENDERTYPE_LIGHTNING = "rendertype_lightning";
	public static final String SHADER_VANILLA_RENDERTYPE_TRIPWIRE = "rendertype_tripwire";
	public static final String SHADER_VANILLA_RENDERTYPE_END_PORTAL = "rendertype_end_portal";
	public static final String SHADER_VANILLA_RENDERTYPE_END_GATEWAY = "rendertype_end_gateway";
	public static final String SHADER_VANILLA_RENDERTYPE_LINES = "rendertype_lines";
	public static final String SHADER_VANILLA_RENDERTYPE_CRUMBLING = "rendertype_crumbling";
	// @formatter:on

	private static final Minecraft CLIENT = Minecraft.getInstance();

	public static ShaderProgram getShader(ResourceLocation id) {
		final var shader = GameRendererAccessor.getShader(CLIENT.gameRenderer, id);
		if (shader == null)
			throw new IllegalArgumentException(String.format("The requested shader '%s' could not be found.", id));
		return shader;
	}

	public static ShaderProgram getVanillaShader(String id) {
		final var shader = GameRendererAccessor.getVanillaShader(CLIENT.gameRenderer, id);
		if (shader == null)
			throw new IllegalArgumentException(
					String.format("The requested vanilla shader '%s' could not be found.", id));
		return shader;
	}

}
