package net.xavil.ultraviolet.common.universe.system.gen2;

import java.util.ArrayList;
import java.util.List;

import net.xavil.hawklib.Units;
import net.xavil.hawklib.math.matrices.Vec3;

public class GravityVolume {

	public static abstract sealed class Node {
		public abstract Vec3 computeCenterOfMass();

		public abstract Vec3 computeForce(double theta, int id, Vec3 pos, double mass);
	}

	public static Vec3 forceFromGravity_N(Vec3 posA, double massA, Vec3 posB, double massB) {
		final Vec3 posA_m = posA.mul(Units.TERA), posB_m = posB.mul(Units.TERA);
		final double massA_kg = massA * Units.YOTTA, massB_kg = massB * Units.YOTTA;
		// F = G * m1*m2 / r^2
		final var toB_m = posB_m.sub(posA_m);
		final var numerator = Units.GRAVITATIONAL_CONSTANT_m3_PER_kg_s2 * massA_kg * massB_kg;
		return toB_m.normalize().mul(numerator / toB_m.lengthSquared());
	}

	public static final class Leaf extends Node {
		public final int id;
		public Vec3 position;
		public double mass;
		public Vec3 acceleration = Vec3.ZERO;

		public Leaf(int id, Vec3 position, double mass) {
			this.id = id;
			this.position = position;
			this.mass = mass;
		}

		@Override
		public Vec3 computeCenterOfMass() {
			return this.position;
		}

		@Override
		public Vec3 computeForce(double theta, int id, Vec3 pos, double mass) {
			return this.id == id ? Vec3.ZERO : forceFromGravity_N(pos, mass, this.position, this.mass);
		}
	}

	public static final class Branch extends Node {
		public Node nnn = null;
		public Node nnp = null;
		public Node npn = null;
		public Node npp = null;
		public Node pnn = null;
		public Node pnp = null;
		public Node ppn = null;
		public Node ppp = null;

		public Vec3 lower, center, upper;
		public Vec3 centerOfMass = Vec3.ZERO;
		public double nodeWidth;
		public double totalMass = 0.0;

		public Branch(Vec3 lower, Vec3 upper) {
			this.lower = lower;
			this.upper = upper;
			this.center = lower.add(upper).mul(0.5);
			this.nodeWidth = this.upper.y - this.lower.y;
		}

		@Override
		public Vec3 computeCenterOfMass() {
			var com = Vec3.ZERO;
			// @formatter:off
			if (this.nnn != null) com = com.add(this.nnn.computeCenterOfMass());
			if (this.nnp != null) com = com.add(this.nnp.computeCenterOfMass());
			if (this.npn != null) com = com.add(this.npn.computeCenterOfMass());
			if (this.npp != null) com = com.add(this.npp.computeCenterOfMass());
			if (this.pnn != null) com = com.add(this.pnn.computeCenterOfMass());
			if (this.pnp != null) com = com.add(this.pnp.computeCenterOfMass());
			if (this.ppn != null) com = com.add(this.ppn.computeCenterOfMass());
			if (this.ppp != null) com = com.add(this.ppp.computeCenterOfMass());
			// @formatter:on
			this.centerOfMass = com.div(this.totalMass);
			return this.centerOfMass;
		}

		@Override
		public Vec3 computeForce(double theta, int id, Vec3 pos, double mass) {
			if (this.nodeWidth / pos.distanceTo(this.centerOfMass) > theta) {
				return forceFromGravity_N(pos, mass, this.centerOfMass, this.totalMass);
			}

			var force = Vec3.ZERO;
			// @formatter:off
			if (this.nnn != null) force = force.add(this.nnn.computeForce(theta, id, pos, mass));
			if (this.nnp != null) force = force.add(this.nnp.computeForce(theta, id, pos, mass));
			if (this.npn != null) force = force.add(this.npn.computeForce(theta, id, pos, mass));
			if (this.npp != null) force = force.add(this.npp.computeForce(theta, id, pos, mass));
			if (this.pnn != null) force = force.add(this.pnn.computeForce(theta, id, pos, mass));
			if (this.pnp != null) force = force.add(this.pnp.computeForce(theta, id, pos, mass));
			if (this.ppn != null) force = force.add(this.ppn.computeForce(theta, id, pos, mass));
			if (this.ppp != null) force = force.add(this.ppp.computeForce(theta, id, pos, mass));
			// @formatter:on
			return force;
		}

		public void insert(Leaf toInsert) {
			final var x = toInsert.position.x >= this.center.x ? 1 : 0;
			final var y = toInsert.position.y >= this.center.y ? 2 : 0;
			final var z = toInsert.position.z >= this.center.z ? 4 : 0;
			this.totalMass += toInsert.mass;
			switch (x | y | z) {
				case 0:
					if (this.nnn == null)
						this.nnn = toInsert;
					else if (this.nnn instanceof Branch node)
						node.insert(toInsert);
					else if (this.nnn instanceof Leaf node) {
						double lx = this.lower.x, ly = this.lower.y, lz = this.lower.z;
						double hx = this.center.x, hy = this.center.y, hz = this.center.z;
						var newBranch = new Branch(Vec3.from(lx, ly, lz), Vec3.from(hx, hy, hz));
						newBranch.insert(node);
						newBranch.insert(toInsert);
						this.nnn = newBranch;
					}
					break;
				case 1:
					if (this.nnp == null)
						this.nnp = toInsert;
					else if (this.nnp instanceof Branch node)
						node.insert(toInsert);
					else if (this.nnp instanceof Leaf node) {
						double lx = this.lower.x, ly = this.lower.y, lz = this.center.z;
						double hx = this.center.x, hy = this.center.y, hz = this.upper.z;
						var newBranch = new Branch(Vec3.from(lx, ly, lz), Vec3.from(hx, hy, hz));
						newBranch.insert(node);
						newBranch.insert(toInsert);
						this.nnp = newBranch;
					}
					break;
				case 2:
					if (this.npn == null)
						this.npn = toInsert;
					else if (this.npn instanceof Branch node)
						node.insert(toInsert);
					else if (this.npn instanceof Leaf node) {
						double lx = this.lower.x, ly = this.center.y, lz = this.lower.z;
						double hx = this.center.x, hy = this.upper.y, hz = this.center.z;
						var newBranch = new Branch(Vec3.from(lx, ly, lz), Vec3.from(hx, hy, hz));
						newBranch.insert(node);
						newBranch.insert(toInsert);
						this.npn = newBranch;
					}
					break;
				case 3:
					if (this.npp == null)
						this.npp = toInsert;
					else if (this.npp instanceof Branch node)
						node.insert(toInsert);
					else if (this.npp instanceof Leaf node) {
						double lx = this.lower.x, ly = this.center.y, lz = this.center.z;
						double hx = this.center.x, hy = this.upper.y, hz = this.upper.z;
						var newBranch = new Branch(Vec3.from(lx, ly, lz), Vec3.from(hx, hy, hz));
						newBranch.insert(node);
						newBranch.insert(toInsert);
						this.npp = newBranch;
					}
					break;
				case 4:
					if (this.pnn == null)
						this.pnn = toInsert;
					else if (this.pnn instanceof Branch node)
						node.insert(toInsert);
					else if (this.pnn instanceof Leaf node) {
						double lx = this.center.x, ly = this.lower.y, lz = this.lower.z;
						double hx = this.upper.x, hy = this.center.y, hz = this.center.z;
						var newBranch = new Branch(Vec3.from(lx, ly, lz), Vec3.from(hx, hy, hz));
						newBranch.insert(node);
						newBranch.insert(toInsert);
						this.pnn = newBranch;
					}
					break;
				case 5:
					if (this.pnp == null)
						this.pnp = toInsert;
					else if (this.pnp instanceof Branch node)
						node.insert(toInsert);
					else if (this.pnp instanceof Leaf node) {
						double lx = this.center.x, ly = this.lower.y, lz = this.center.z;
						double hx = this.upper.x, hy = this.center.y, hz = this.upper.z;
						var newBranch = new Branch(Vec3.from(lx, ly, lz), Vec3.from(hx, hy, hz));
						newBranch.insert(node);
						newBranch.insert(toInsert);
						this.pnp = newBranch;
					}
					break;
				case 6:
					if (this.ppn == null)
						this.ppn = toInsert;
					else if (this.ppn instanceof Branch node)
						node.insert(toInsert);
					else if (this.ppn instanceof Leaf node) {
						double lx = this.center.x, ly = this.center.y, lz = this.lower.z;
						double hx = this.upper.x, hy = this.upper.y, hz = this.center.z;
						var newBranch = new Branch(Vec3.from(lx, ly, lz), Vec3.from(hx, hy, hz));
						newBranch.insert(node);
						newBranch.insert(toInsert);
						this.ppn = newBranch;
					}
					break;
				case 7:
					if (this.ppp == null)
						this.ppp = toInsert;
					else if (this.ppp instanceof Branch node)
						node.insert(toInsert);
					else if (this.ppp instanceof Leaf node) {
						double lx = this.center.x, ly = this.center.y, lz = this.center.z;
						double hx = this.upper.x, hy = this.upper.y, hz = this.upper.z;
						var newBranch = new Branch(Vec3.from(lx, ly, lz), Vec3.from(hx, hy, hz));
						newBranch.insert(node);
						newBranch.insert(toInsert);
						this.ppp = newBranch;
					}
					break;
				default:
			}
		}
	}

	public static class Particle {
		public Vec3 position;
		public Vec3 velocity;
		public Vec3 acceleration;
		public Vec3 angularMomentum;
		public double mass;
		public double radius;
	}

	public final List<Particle> particles = new ArrayList<>();

	void update(double dt) {
		if (this.particles.isEmpty())
			return;

		final var root = new Branch(Vec3.broadcast(-Units.fromLy(1)), Vec3.broadcast(Units.fromLy(1)));
		for (int i = 1; i < this.particles.size(); ++i) {
			final var particle = this.particles.get(i);
			root.insert(new Leaf(i, particle.position, particle.mass));
		}

		root.computeCenterOfMass();

		for (int i = 1; i < this.particles.size(); ++i) {
			final var particle = this.particles.get(i);

			// leapfrog integration i think

			// v_(i+0.5) = v_i + a_i*dt/2;
			particle.velocity = particle.velocity.add(particle.acceleration.mul(dt / 2.0));

			// x_(i+1) = x_i + v_(i+0.5)*dt;
			particle.position = particle.position.add(particle.velocity.mul(dt));

			// v_(i+1) = v_(i+0.5) + a_(i+1)*dt/2;
			final var force_N = root.computeForce(1.5, i, particle.position, particle.mass);
			particle.acceleration = force_N.div(particle.mass);
			particle.velocity = particle.velocity.add(particle.acceleration.mul(dt / 2.0));
		}
	}

}
