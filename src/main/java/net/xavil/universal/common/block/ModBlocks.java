package net.xavil.universal.common.block;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.core.Registry;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.xavil.universal.Mod;

public final class ModBlocks {

	public static final Block SILICATE_FINE_REGOLITH = new Block(
			FabricBlockSettings.of(Material.STONE).strength(0.4f).sound(SoundType.WOOL));
	public static final Block SILICATE_LOOSE_REGOLITH = new Block(
			FabricBlockSettings.of(Material.STONE).strength(1f, 4f).sound(SoundType.GRAVEL));
	public static final Block SILICATE_ROCK = new Block(
			FabricBlockSettings.of(Material.STONE).strength(1.5f, 6f).sound(SoundType.TUFF));

	public static final Block METAL_RICH_FINE_REGOLITH = new Block(
			FabricBlockSettings.of(Material.STONE).strength(0.4f).sound(SoundType.WOOL));
	public static final Block METAL_RICH_LOOSE_REGOLITH = new Block(
			FabricBlockSettings.of(Material.STONE).strength(1f, 4f).sound(SoundType.GRAVEL));
	public static final Block METAL_RICH_ROCK = new Block(
			FabricBlockSettings.of(Material.STONE).strength(1.5f, 6f).sound(SoundType.TUFF));

	public static final Block ICY_ROCK = new Block(
			FabricBlockSettings.of(Material.ICE_SOLID).strength(1.5f, 6.0f).sound(SoundType.DEEPSLATE));
	public static final Block ORGANIC_ICY_ROCK = new Block(
			FabricBlockSettings.of(Material.ICE_SOLID).strength(1.5f, 6.0f).sound(SoundType.DEEPSLATE));

	// different rock types should have different properties, maybe store and ID of
	// which planet the rock came from in the item stack nbt?

	private static void registerBasicBlock(String name, Block block) {
		final var settings = new FabricItemSettings().tab(CreativeModeTab.TAB_MISC);
		Registry.register(Registry.BLOCK, Mod.namespaced(name), block);
		Registry.register(Registry.ITEM, Mod.namespaced(name), new BlockItem(block, settings));
	}

	public static void register() {
		registerBasicBlock("silicate_fine_regolith", SILICATE_FINE_REGOLITH);
		registerBasicBlock("silicate_loose_regolith", SILICATE_LOOSE_REGOLITH);
		registerBasicBlock("silicate_rock", SILICATE_ROCK);
		registerBasicBlock("metal_rich_fine_regolith", METAL_RICH_FINE_REGOLITH);
		registerBasicBlock("metal_rich_loose_regolith", METAL_RICH_LOOSE_REGOLITH);
		registerBasicBlock("metal_rich_rock", METAL_RICH_ROCK);
		registerBasicBlock("icy_rock", ICY_ROCK);
		registerBasicBlock("organic_icy_rock", ORGANIC_ICY_ROCK);
	}

}
