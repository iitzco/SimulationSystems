package itba.ss.TPF_HourGlass;

import java.util.Set;

public class GranularAccelerator implements Accelerator {

	private double kn;
	private double kt;
	private static final double G = -10;

	public GranularAccelerator(double kn, double kt) {
		super();
		this.kn = kn;
		this.kt = kt;
	}

	public double getForceX(Particle p, Set<Particle> set) {
		if (p.fX == null) {
			setForces(p, set);
		}
		return p.fX;
	}

	public double getForceY(Particle p, Set<Particle> set) {
		if (p.fY == null) {
			setForces(p, set);
		}
		return p.fY;
	}

	public double getForceZ(Particle p, Set<Particle> set) {
		if (p.fZ == null) {
			setForces(p, set);
		}
		return p.fZ;
	}

	private void setForces(Particle p, Set<Particle> set) {
		double fX = 0;
		double fY = 0;
		double fZ = p.mass * G;
		for (Particle particle : set) {
			fX += getFN(p, particle) * getENX(p, particle);
			fY += getFN(p, particle) * getENY(p, particle);
			fZ += getFN(p, particle) * getENZ(p, particle);
		}
		p.fX = fX;
		p.fY = fY;
		p.fZ = fZ;
	}

	private double getENZ(Particle p, Particle particle) {
		return (particle.z - p.z) / getDistance(particle.x, particle.y, particle.z, p.x, p.y, p.z);
	}

	private double getENY(Particle p, Particle particle) {
		return (particle.y - p.y) / getDistance(particle.x, particle.y, particle.z, p.x, p.y, p.z);
	}

	private double getENX(Particle p, Particle particle) {
		return (particle.x - p.x) / getDistance(particle.x, particle.y, particle.z, p.x, p.y, p.z);
	}

	private double getFN(Particle p, Particle other) {
		return -kn * getEpsilon(p, other);
	}

	private double getRelativeSpeed(Particle p, Particle other) {
		return (Math.sqrt(Math.pow(p.speedX, 2) + Math.pow(p.speedY, 2) + Math.pow(p.speedZ, 2)))
				- (Math.sqrt(Math.pow(other.speedX, 2) + Math.pow(other.speedY, 2) + Math.pow(other.speedZ, 2)));
	}

	private double getEpsilon(Particle p, Particle other) {
		return p.r + other.r - (getDistance(p.x, p.y, p.z, other.x, other.y, other.z));
	}

	private double getDistance(double x0, double y0, double z0, double x1, double y1, double z1) {
		return Math.sqrt(Math.pow(x0 - x1, 2) + Math.pow(y0 - y1, 2) + Math.pow(z0 - z1, 2));
	}

}
