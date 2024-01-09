package net.xavil.ultraviolet.common.universe.galaxy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.annotation.Nullable;

import com.google.common.base.Charsets;

import net.xavil.hawklib.SplittableRng;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.collections.interfaces.MutableMap;
import net.xavil.hawklib.math.matrices.Mat4;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.common.universe.galaxy.GalaxySector.ElementHolder;
import net.xavil.ultraviolet.common.universe.galaxy.GalaxySector.PackedElements;
import net.xavil.ultraviolet.common.universe.id.GalaxySectorId;
import net.xavil.ultraviolet.common.universe.system.StarSystem;
import net.xavil.universegen.system.StellarCelestialNode;

public class StarCatalogGalaxyGenerationLayer extends GalaxyGenerationLayer {

	private final StartingSystemGalaxyGenerationLayer startingGenerator;
	private final MutableMap<SectorPos, PackedElements> sectorMap = MutableMap.hashMap();

	public StarCatalogGalaxyGenerationLayer(Galaxy parentGalaxy,
			StartingSystemGalaxyGenerationLayer startingGenerator) {
		super(parentGalaxy);
		this.startingGenerator = startingGenerator;

		loadSectorMap();
	}

	// FK5 system - not the most modern but i already input all the values by hand.
	// this is just to get the data from the catalog to line up with the galactic
	// plane in the mod.
	// https://www.aanda.org/articles/aa/full_html/2011/02/aa14961-10/aa14961-10.html
	private static final Mat4 EQUATORIAL_TO_GALACTIC = new Mat4(
			-0.054875539390, -0.873437104725, -0.483834991775, 0,
			+0.494109453633, -0.444829594298, +0.746982248696, 0,
			-0.867666135681, -0.198076389622, +0.455983794523, 0,
			0, 0, 0, 1);

	private static String readString(ByteBuffer buf) {
		final int start = buf.position();
		while (buf.get() != 0) {
		}
		final int end = buf.position();
		// subtract 1 so we dont include the null terminator
		return new String(buf.array(), start, end - start - 1, Charsets.UTF_8);
	}

	@Nullable
	private static ByteBuffer loadStarCatalog() {
		try {
			final var stream = StarCatalogGalaxyGenerationLayer.class.getResourceAsStream("/star_catalog.bin");
			return ByteBuffer.wrap(stream.readAllBytes()).order(ByteOrder.BIG_ENDIAN);
		} catch (IOException ex) {
			Mod.LOGGER.error("Failed to load star catalog");
			ex.printStackTrace();
			return null;
		}
	}

	private void loadSectorMap() {
		this.sectorMap.clear();

		final var buf = loadStarCatalog();
		if (buf == null)
			return;

		final var startingSystem = new GalaxySector.ElementHolder();
		this.startingGenerator.copyStartingSystemInfo(startingSystem);

		int loadedCount = 0;
		final var rng = new SplittableRng(1234);
		final var temp = new GalaxySector.ElementHolder();
		temp.generationLayer = this.layerId;

		while (buf.hasRemaining()) {
			rng.advance();
			temp.systemSeed = rng.uniformLong("system_seed");

			temp.systemPosTm.x = Units.Tm_PER_pc * buf.getFloat();
			temp.systemPosTm.y = Units.Tm_PER_pc * buf.getFloat();
			temp.systemPosTm.z = Units.Tm_PER_pc * buf.getFloat();
			// positions from the star catalog are in J2000 equatorial coordinate system (i
			// think)
			Mat4.mul(temp.systemPosTm, EQUATORIAL_TO_GALACTIC, temp.systemPosTm, 0);
			// coordinates are right-handed Z up, we need them in right handed Y up...
			final var tmpY = temp.systemPosTm.y;
			temp.systemPosTm.y = temp.systemPosTm.z;
			temp.systemPosTm.z = -tmpY;
			// offset position to be relative to the starting system
			Vec3.add(temp.systemPosTm, temp.systemPosTm, startingSystem.systemPosTm);
			final var sectorPos = SectorPos.fromPos(GalaxySector.ROOT_LEVEL, temp.systemPosTm);

			temp.luminosityLsol = buf.getFloat();
			temp.temperatureK = buf.getFloat();

			// TODO: guess these values since we don't know these values natively
			temp.massYg = 1 * Units.Yg_PER_Msol;
			temp.systemAgeMyr = 1;

			temp.name = readString(buf);
			// spectral classification - unused for now
			readString(buf);

			this.sectorMap.entry(sectorPos)
					.orInsertWith(() -> new GalaxySector.PackedElements(sectorPos.minBound(), true))
					.push(temp);

			loadedCount += 1;
		}

		this.sectorMap.values().forEach(GalaxySector.PackedElements::shrinkToFit);
		Mod.LOGGER.info("Loaded {} stars from catalogue into {} sectors", loadedCount, this.sectorMap.size());
	}

	@Override
	public void generateInto(Context ctx, PackedElements elements) {
		final var sector = this.sectorMap.getOrNull(ctx.pos);
		if (sector == null)
			return;
		final int startIndex = elements.size();
		elements.reserve(sector.size());
		elements.storeSequence(sector, startIndex);
		elements.markWritten(sector.size());
	}

	@Override
	public StarSystem generateFullSystem(GalaxySector sector, GalaxySectorId id, ElementHolder elem) {
		final var node = new StellarCelestialNode();

		node.luminosityLsol = elem.luminosityLsol;
		node.massYg = elem.massYg;
		node.temperature = elem.temperatureK;
		node.radius = StellarCelestialNode.radiusFromLuminosityAndTemperature(elem.luminosityLsol, elem.temperatureK);
		// TODO: guess this value too
		node.type = StellarCelestialNode.Type.MAIN_SEQUENCE;

		node.build();
		node.assignSeeds(elem.systemSeed);

		// TODO: assign system name
		return new StarSystem("idk", this.parentGalaxy, elem, node);
	}

}
