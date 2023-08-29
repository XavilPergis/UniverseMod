package net.xavil.hawklib.client;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.xavil.hawklib.client.gl.shader.ShaderProgram;
import net.xavil.ultraviolet.mixin.accessor.GameRendererAccessor;

public class HawkShaders {
	
	protected HawkShaders() {
	}

	public static final ResourceLocation SHADER_BLIT_LOCATION = new ResourceLocation("hawk", "blit.glsl");

	public static final Supplier<ShaderProgram> SHADER_BLIT = () -> getShader(SHADER_BLIT_LOCATION);

	// @formatter:off
	public static final String SHADER_VANILLA_BLOCK_LOCATION = "block";
	public static final String SHADER_VANILLA_NEW_ENTITY_LOCATION = "new_entity";
	public static final String SHADER_VANILLA_PARTICLE_LOCATION = "particle";
	public static final String SHADER_VANILLA_POSITION_LOCATION = "position";
	public static final String SHADER_VANILLA_POSITION_COLOR_LOCATION = "position_color";
	public static final String SHADER_VANILLA_POSITION_COLOR_LIGHTMAP_LOCATION = "position_color_lightmap";
	public static final String SHADER_VANILLA_POSITION_COLOR_TEX_LOCATION = "position_color_tex";
	public static final String SHADER_VANILLA_POSITION_COLOR_TEX_LIGHTMAP_LOCATION = "position_color_tex_lightmap";
	public static final String SHADER_VANILLA_POSITION_TEX_LOCATION = "position_tex";
	public static final String SHADER_VANILLA_POSITION_TEX_COLOR_LOCATION = "position_tex_color";
	public static final String SHADER_VANILLA_POSITION_TEX_COLOR_NORMAL_LOCATION = "position_tex_color_normal";
	public static final String SHADER_VANILLA_POSITION_TEX_LIGHTMAP_COLOR_LOCATION = "position_tex_lightmap_color";
	public static final String SHADER_VANILLA_RENDERTYPE_SOLID_LOCATION = "rendertype_solid";
	public static final String SHADER_VANILLA_RENDERTYPE_CUTOUT_MIPPED_LOCATION = "rendertype_cutout_mipped";
	public static final String SHADER_VANILLA_RENDERTYPE_CUTOUT_LOCATION = "rendertype_cutout";
	public static final String SHADER_VANILLA_RENDERTYPE_TRANSLUCENT_LOCATION = "rendertype_translucent";
	public static final String SHADER_VANILLA_RENDERTYPE_TRANSLUCENT_MOVING_BLOCK_LOCATION = "rendertype_translucent_moving_block";
	public static final String SHADER_VANILLA_RENDERTYPE_TRANSLUCENT_NO_CRUMBLING_LOCATION = "rendertype_translucent_no_crumbling";
	public static final String SHADER_VANILLA_RENDERTYPE_ARMOR_CUTOUT_NO_CULL_LOCATION = "rendertype_armor_cutout_no_cull";
	public static final String SHADER_VANILLA_RENDERTYPE_ENTITY_SOLID_LOCATION = "rendertype_entity_solid";
	public static final String SHADER_VANILLA_RENDERTYPE_ENTITY_CUTOUT_LOCATION = "rendertype_entity_cutout";
	public static final String SHADER_VANILLA_RENDERTYPE_ENTITY_CUTOUT_NO_CULL_LOCATION = "rendertype_entity_cutout_no_cull";
	public static final String SHADER_VANILLA_RENDERTYPE_ENTITY_CUTOUT_NO_CULL_Z_OFFSET_LOCATION = "rendertype_entity_cutout_no_cull_z_offset";
	public static final String SHADER_VANILLA_RENDERTYPE_ITEM_ENTITY_TRANSLUCENT_CULL_LOCATION = "rendertype_item_entity_translucent_cull";
	public static final String SHADER_VANILLA_RENDERTYPE_ENTITY_TRANSLUCENT_CULL_LOCATION = "rendertype_entity_translucent_cull";
	public static final String SHADER_VANILLA_RENDERTYPE_ENTITY_TRANSLUCENT_LOCATION = "rendertype_entity_translucent";
	public static final String SHADER_VANILLA_RENDERTYPE_ENTITY_SMOOTH_CUTOUT_LOCATION = "rendertype_entity_smooth_cutout";
	public static final String SHADER_VANILLA_RENDERTYPE_BEACON_BEAM_LOCATION = "rendertype_beacon_beam";
	public static final String SHADER_VANILLA_RENDERTYPE_ENTITY_DECAL_LOCATION = "rendertype_entity_decal";
	public static final String SHADER_VANILLA_RENDERTYPE_ENTITY_NO_OUTLINE_LOCATION = "rendertype_entity_no_outline";
	public static final String SHADER_VANILLA_RENDERTYPE_ENTITY_SHADOW_LOCATION = "rendertype_entity_shadow";
	public static final String SHADER_VANILLA_RENDERTYPE_ENTITY_ALPHA_LOCATION = "rendertype_entity_alpha";
	public static final String SHADER_VANILLA_RENDERTYPE_EYES_LOCATION = "rendertype_eyes";
	public static final String SHADER_VANILLA_RENDERTYPE_ENERGY_SWIRL_LOCATION = "rendertype_energy_swirl";
	public static final String SHADER_VANILLA_RENDERTYPE_LEASH_LOCATION = "rendertype_leash";
	public static final String SHADER_VANILLA_RENDERTYPE_WATER_MASK_LOCATION = "rendertype_water_mask";
	public static final String SHADER_VANILLA_RENDERTYPE_OUTLINE_LOCATION = "rendertype_outline";
	public static final String SHADER_VANILLA_RENDERTYPE_ARMOR_GLINT_LOCATION = "rendertype_armor_glint";
	public static final String SHADER_VANILLA_RENDERTYPE_ARMOR_ENTITY_GLINT_LOCATION = "rendertype_armor_entity_glint";
	public static final String SHADER_VANILLA_RENDERTYPE_GLINT_TRANSLUCENT_LOCATION = "rendertype_glint_translucent";
	public static final String SHADER_VANILLA_RENDERTYPE_GLINT_LOCATION = "rendertype_glint";
	public static final String SHADER_VANILLA_RENDERTYPE_GLINT_DIRECT_LOCATION = "rendertype_glint_direct";
	public static final String SHADER_VANILLA_RENDERTYPE_ENTITY_GLINT_LOCATION = "rendertype_entity_glint";
	public static final String SHADER_VANILLA_RENDERTYPE_ENTITY_GLINT_DIRECT_LOCATION = "rendertype_entity_glint_direct";
	public static final String SHADER_VANILLA_RENDERTYPE_TEXT_LOCATION = "rendertype_text";
	public static final String SHADER_VANILLA_RENDERTYPE_TEXT_INTENSITY_LOCATION = "rendertype_text_intensity";
	public static final String SHADER_VANILLA_RENDERTYPE_TEXT_SEE_THROUGH_LOCATION = "rendertype_text_see_through";
	public static final String SHADER_VANILLA_RENDERTYPE_TEXT_INTENSITY_SEE_THROUGH_LOCATION = "rendertype_text_intensity_see_through";
	public static final String SHADER_VANILLA_RENDERTYPE_LIGHTNING_LOCATION = "rendertype_lightning";
	public static final String SHADER_VANILLA_RENDERTYPE_TRIPWIRE_LOCATION = "rendertype_tripwire";
	public static final String SHADER_VANILLA_RENDERTYPE_END_PORTAL_LOCATION = "rendertype_end_portal";
	public static final String SHADER_VANILLA_RENDERTYPE_END_GATEWAY_LOCATION = "rendertype_end_gateway";
	public static final String SHADER_VANILLA_RENDERTYPE_LINES_LOCATION = "rendertype_lines";
	public static final String SHADER_VANILLA_RENDERTYPE_CRUMBLING_LOCATION = "rendertype_crumbling";

	public static final Supplier<ShaderProgram> SHADER_VANILLA_BLOCK = () -> getVanillaShader(SHADER_VANILLA_BLOCK_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_NEW_ENTITY = () -> getVanillaShader(SHADER_VANILLA_NEW_ENTITY_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_PARTICLE = () -> getVanillaShader(SHADER_VANILLA_PARTICLE_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_POSITION = () -> getVanillaShader(SHADER_VANILLA_POSITION_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_POSITION_COLOR = () -> getVanillaShader(SHADER_VANILLA_POSITION_COLOR_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_POSITION_COLOR_LIGHTMAP = () -> getVanillaShader(SHADER_VANILLA_POSITION_COLOR_LIGHTMAP_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_POSITION_COLOR_TEX = () -> getVanillaShader(SHADER_VANILLA_POSITION_COLOR_TEX_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_POSITION_COLOR_TEX_LIGHTMAP = () -> getVanillaShader(SHADER_VANILLA_POSITION_COLOR_TEX_LIGHTMAP_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_POSITION_TEX = () -> getVanillaShader(SHADER_VANILLA_POSITION_TEX_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_POSITION_TEX_COLOR = () -> getVanillaShader(SHADER_VANILLA_POSITION_TEX_COLOR_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_POSITION_TEX_COLOR_NORMAL = () -> getVanillaShader(SHADER_VANILLA_POSITION_TEX_COLOR_NORMAL_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_POSITION_TEX_LIGHTMAP_COLOR = () -> getVanillaShader(SHADER_VANILLA_POSITION_TEX_LIGHTMAP_COLOR_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_SOLID = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_SOLID_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_CUTOUT_MIPPED = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_CUTOUT_MIPPED_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_CUTOUT = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_CUTOUT_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_TRANSLUCENT = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_TRANSLUCENT_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_TRANSLUCENT_MOVING_BLOCK = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_TRANSLUCENT_MOVING_BLOCK_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_TRANSLUCENT_NO_CRUMBLING = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_TRANSLUCENT_NO_CRUMBLING_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_ARMOR_CUTOUT_NO_CULL = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_ARMOR_CUTOUT_NO_CULL_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_ENTITY_SOLID = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_ENTITY_SOLID_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_ENTITY_CUTOUT = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_ENTITY_CUTOUT_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_ENTITY_CUTOUT_NO_CULL = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_ENTITY_CUTOUT_NO_CULL_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_ENTITY_CUTOUT_NO_CULL_Z_OFFSET = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_ENTITY_CUTOUT_NO_CULL_Z_OFFSET_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_ITEM_ENTITY_TRANSLUCENT_CULL = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_ITEM_ENTITY_TRANSLUCENT_CULL_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_ENTITY_TRANSLUCENT_CULL = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_ENTITY_TRANSLUCENT_CULL_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_ENTITY_TRANSLUCENT = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_ENTITY_TRANSLUCENT_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_ENTITY_SMOOTH_CUTOUT = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_ENTITY_SMOOTH_CUTOUT_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_BEACON_BEAM = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_BEACON_BEAM_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_ENTITY_DECAL = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_ENTITY_DECAL_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_ENTITY_NO_OUTLINE = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_ENTITY_NO_OUTLINE_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_ENTITY_SHADOW = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_ENTITY_SHADOW_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_ENTITY_ALPHA = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_ENTITY_ALPHA_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_EYES = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_EYES_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_ENERGY_SWIRL = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_ENERGY_SWIRL_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_LEASH = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_LEASH_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_WATER_MASK = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_WATER_MASK_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_OUTLINE = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_OUTLINE_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_ARMOR_GLINT = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_ARMOR_GLINT_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_ARMOR_ENTITY_GLINT = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_ARMOR_ENTITY_GLINT_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_GLINT_TRANSLUCENT = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_GLINT_TRANSLUCENT_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_GLINT = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_GLINT_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_GLINT_DIRECT = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_GLINT_DIRECT_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_ENTITY_GLINT = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_ENTITY_GLINT_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_ENTITY_GLINT_DIRECT = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_ENTITY_GLINT_DIRECT_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_TEXT = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_TEXT_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_TEXT_INTENSITY = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_TEXT_INTENSITY_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_TEXT_SEE_THROUGH = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_TEXT_SEE_THROUGH_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_TEXT_INTENSITY_SEE_THROUGH = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_TEXT_INTENSITY_SEE_THROUGH_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_LIGHTNING = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_LIGHTNING_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_TRIPWIRE = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_TRIPWIRE_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_END_PORTAL = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_END_PORTAL_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_END_GATEWAY = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_END_GATEWAY_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_LINES = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_LINES_LOCATION);
	public static final Supplier<ShaderProgram> SHADER_VANILLA_RENDERTYPE_CRUMBLING = () -> getVanillaShader(SHADER_VANILLA_RENDERTYPE_CRUMBLING_LOCATION);
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
