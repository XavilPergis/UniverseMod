package net.xavil.universal.client.screen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class SpatialPartition<T> {

	private static final double RADIUS_LIMIT = 4;
	private final double cellSize;
	private final Map<Vec3i, List<SpatialCell<T>>> cells = new HashMap<>();

	private record SpatialCell<T>(Vec3 cellOffset, double boundingRadius, T value) {
	}

	public SpatialPartition(double cellSize) {
		this.cellSize = cellSize;
	}

	public void add(Vec3 pos, double radius, T value) {
		var r = radius / this.cellSize;
		// if (r > RADIUS_LIMIT)
		r = Math.min(r, RADIUS_LIMIT);
		final var cellPos = pos.scale(1 / this.cellSize);
		final int minX = Mth.floor(cellPos.x - r), minY = Mth.floor(cellPos.y - r), minZ = Mth.floor(cellPos.z - r);
		final int maxX = Mth.floor(cellPos.x + r), maxY = Mth.floor(cellPos.y + r), maxZ = Mth.floor(cellPos.z + r);

		for (int x = minX; x <= maxX; ++x) {
			for (int y = minY; x <= maxY; ++y) {
				for (int z = minZ; z <= maxZ; ++z) {
					var closestX = Mth.clamp(cellPos.x, x, x + 1);
					var closestY = Mth.clamp(cellPos.y, y, y + 1);
					var closestZ = Mth.clamp(cellPos.z, z, z + 1);
					var diff = cellPos.subtract(closestX, closestY, closestZ);
					if (diff.lengthSqr() < r * r) {
						var cellIndex = new Vec3i(x, y, z);
						if (!this.cells.containsKey(cellIndex)) {
							this.cells.put(cellIndex, new ArrayList<>());
						}
						var cellOffsetX = cellPos.x - Math.floor(cellPos.x);
						var cellOffsetY = cellPos.y - Math.floor(cellPos.y);
						var cellOffsetZ = cellPos.z - Math.floor(cellPos.z);
						var cellOffset = new Vec3(cellOffsetX, cellOffsetY, cellOffsetZ);
						this.cells.get(cellIndex).add(new SpatialCell<>(cellOffset, radius, value));
					}
				}
			}
		}
	}

	public interface RaycastConsumer {
		// return true to stop traversal, or false to continue.
		boolean handleRaycast();
	}

	private static boolean raySphere(Vec3 origin, Vec3 dir, Vec3 sphereCenter, double radius) {
		var p = origin.subtract(sphereCenter);
		if (p.dot(dir) > 0 || radius * radius > p.lengthSqr())
			return false;
		var a = p.subtract(dir.scale(p.dot(dir)));
		if (a.lengthSqr() > radius * radius)
			return false;
		return true;
	}

	public @Nullable T rayCast(Vec3 start, Vec3 end, RaycastConsumer consumer) {
		final var dir = end.subtract(start).normalize();

		final var cellStart = start.scale(1 / cellSize);

		// which direction we have to move the cell pos towards when we step that axis.
		final int gridStepX = dir.x > 0 ? 1 : dir.x < 0 ? -1 : 0;
		final int gridStepY = dir.y > 0 ? 1 : dir.y < 0 ? -1 : 0;
		final int gridStepZ = dir.z > 0 ? 1 : dir.z < 0 ? -1 : 0;

		// the tti (time-to-impact) is multiplied with gridStep* because we always want
		// tti to be positive, as its essentially the length of a line segment between
		// the current point and where the current point plus the direction next
		// intersects with the plane of the axis we're looking at.
		final double lenStepX = gridStepX / dir.x;
		final double lenStepY = gridStepY / dir.y;
		final double lenStepZ = gridStepZ / dir.z;

		final double offsetX = cellStart.x - Math.floor(cellStart.x);
		final double offsetY = cellStart.y - Math.floor(cellStart.y);
		final double offsetZ = cellStart.z - Math.floor(cellStart.z);

		double lenX = lenStepX, lenY = lenStepY, lenZ = lenStepZ;
		double curX = start.x, curY = start.y, curZ = start.z;

		int iterationCount = 0;
		while (iterationCount++ < 1000) {
			// find axis with the smallest amount left to go
			if (lenX <= lenY && lenX <= lenZ) {
				curX += gridStepX;
				lenX += lenStepX;
			} else if (lenY <= lenX && lenY <= lenZ) {
				curY += gridStepY;
				lenY += lenStepY;
			} else if (lenZ <= lenX && lenZ <= lenY) {
				curZ += gridStepZ;
				lenZ += lenStepZ;
			}

			var curPos = new Vec3i(curX + offsetX, curY + offsetY, curZ + offsetZ);
			var itemsInCell = this.cells.get(curPos);
			if (itemsInCell == null)
				continue;

			SpatialCell<T> closestCell = null;
			double closestCellDistance = Double.MAX_VALUE;
			for (var cell : itemsInCell) {
				var center = Vec3.atLowerCornerOf(curPos);
				if (raySphere(cellStart, dir, center, cell.boundingRadius)) {
					var distance = cellStart.distanceTo(center);
					if (distance < closestCellDistance) {
						closestCellDistance = distance;
						closestCell = cell;
					}
				}
			}

			if (closestCell != null)
				return closestCell.value;
		}
		return null;
	}

}
