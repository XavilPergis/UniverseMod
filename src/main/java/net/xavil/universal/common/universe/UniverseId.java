package net.xavil.universal.common.universe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Vec3i;
import net.xavil.universal.common.universe.system.StarSystemNode;

public record UniverseId(
		SectorId galaxySector,
		SectorId systemSector,
		/**
		 * The ID used in {@link StarSystemNode#lookup(int)}
		 */
		int systemNodeId) {

	public SystemId systemId() {
		return new SystemId(galaxySector, systemSector);
	}

	private static String escapeMinus(int n) {
		return n >= 0 ? "" + n : "m" + -n;
	}

	public String uniqueName() {
		return "id"
				+ escapeMinus(this.galaxySector.sectorPos.getX()) + "_"
				+ escapeMinus(this.galaxySector.sectorPos.getY()) + "_"
				+ escapeMinus(this.galaxySector.sectorPos.getZ()) + "_"
				+ this.galaxySector.sectorId + "_"
				+ escapeMinus(this.systemSector.sectorPos.getX()) + "_"
				+ escapeMinus(this.systemSector.sectorPos.getY()) + "_"
				+ escapeMinus(this.systemSector.sectorPos.getZ()) + "_"
				+ this.systemSector.sectorId + "_"
				+ this.systemNodeId;
	}

	public record SectorId(Vec3i sectorPos, int sectorId) {
		public static final Codec<SectorId> CODEC = RecordCodecBuilder.create(inst -> inst.group(
				Vec3i.CODEC.fieldOf("pos").forGetter(SectorId::sectorPos),
				Codec.INT.fieldOf("id").forGetter(SectorId::sectorId))
				.apply(inst, SectorId::new));
	}

	public record SystemId(SectorId galaxySector, SectorId systemSector) {
		public static final Codec<SystemId> CODEC = RecordCodecBuilder.create(inst -> inst.group(
				SectorId.CODEC.fieldOf("galaxy").forGetter(SystemId::galaxySector),
				SectorId.CODEC.fieldOf("system").forGetter(SystemId::systemSector))
				.apply(inst, SystemId::new));
	}

	public static final Codec<UniverseId> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			SectorId.CODEC.fieldOf("galaxy").forGetter(UniverseId::galaxySector),
			SectorId.CODEC.fieldOf("system").forGetter(UniverseId::systemSector),
			Codec.INT.fieldOf("id").forGetter(UniverseId::systemNodeId))
			.apply(inst, UniverseId::new));

}
