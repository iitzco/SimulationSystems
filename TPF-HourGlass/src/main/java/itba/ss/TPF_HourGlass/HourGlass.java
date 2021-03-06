package itba.ss.TPF_HourGlass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HourGlass {

	static final double EPSILON = 0.000001;

	static final double MASS = 0.01;
	static final double MAX_TRIES = 10000;

	static int ITERATIONS_TO_REVERT_GRAVITY = 10000;

	double R;
	double D;

	double MARGIN;

	double TOP;
	double BOTTOM;

	double m = 0.01;

	double dt;
	double dt2;
	double maxTime;

	int flips;

	int maxParticles;

	List<Particle> particles;

	int N;

	long caudal = 0;

	double maxSpeed;

	IntegralMethod integralMethod;

	List<Particle> bounds;

	List<Double> measuredTimes;

	public HourGlass(double r, double d, double s, double dT, double dT2, int flips, double maxTime, int maxParticles,
			IntegralMethod integralMethod) {
		super();
		R = r;
		D = d;

		TOP = Math.sqrt(Math.pow(R, 2) - Math.pow(s, 2));

		BOTTOM = -TOP;

		MARGIN = TOP / R;

		ITERATIONS_TO_REVERT_GRAVITY = (int) (1 / dT);

		this.flips = flips;

		this.maxTime = maxTime;
		this.dt = dT;
		this.dt2 = dT2;

		this.maxParticles = maxParticles;

		this.integralMethod = integralMethod;

		this.measuredTimes = new ArrayList<>();

		this.maxSpeed = -1;

		locateParticles(maxParticles);
	}

	private void locateParticles(int size) {
		this.particles = new ArrayList<Particle>();

		boolean flag = true;
		int id = 0;

		while (flag && particles.size() < size) {
			double diameter = Math.random() * (this.D / 5 - this.D / 7) + (this.D / 7);
			double r = diameter / 2;
			double x = 0, y = 0, z = 0;
			int tries = 0;
			do {
				x = Math.random() * (this.R * 2 - 2 * r) - R + r;
				y = Math.random() * (this.R * 2 - 2 * r) - R + r;
				z = (Math.random() * (this.TOP - 2 * r) + r);

				tries++;
				if (tries == MAX_TRIES) {
					flag = false;
					break;
				}
			} while (overlap(x, y, z, r) || !insideGlass(x, y, z, r));
			if (flag) {
				Particle p = new Particle(id++, diameter / 2, x, y, z, 0, 0, 0, MASS);
				this.particles.add(p);
			}
		}
		this.N = this.particles.size();
	}

	private boolean overlap(double x, double y, double z, double r) {
		for (Particle p : particles) {
			if (getDistance(p.x, p.y, p.z, x, y, z) < (p.r + r))
				return true;
		}
		return false;
	}

	private boolean insideGlass(double x, double y, double z, double r) {
		return distanceToCenterTop(x, y, z) < R - r;
	}

	private double distanceToCenterTop(double x, double y, double z) {
		return Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z - TOP, 2));
	}

	private double distanceToCenterBottom(double x, double y, double z) {
		return Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z - BOTTOM, 2));
	}

	private double distanceToCenterTop(Particle p) {
		return distanceToCenterTop(p.x, p.y, p.z);
	}

	private double distanceToCenterBottom(Particle p) {
		return distanceToCenterBottom(p.x, p.y, p.z);
	}

	private double getDistance(double x0, double y0, double z0, double x1, double y1, double z1) {
		return Math.sqrt(Math.pow(x0 - x1, 2) + Math.pow(y0 - y1, 2) + Math.pow(z0 - z1, 2));
	}

	private boolean inContact(Particle p1, Particle p2) {
		return getDistance(p1.x, p1.y, p1.z, p2.x, p2.y, p2.z) <= (p1.r + p2.r);
	}

	private double getMagnitude(double x, double y, double z) {
		return Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
	}

	public void run() {
		double currentTime = 0;

		int iteration = 0;
		int iterationToRevertGravity = 0;
		int currFlips = 0;

		boolean allPassedHalf = true;
		boolean existsFreeParticle = false;

		double measuredTime = 0;
		System.err.println("PARTICLES " + particles.size());
		System.err.println("FLIP 0:");

		Set<Particle> passedHalfParticles = new HashSet<>();

		// CellIndexMethod method = new CellIndexMethod(3 * this.R, 2 * D / 4);

		while (maxTime == 0 || currentTime < maxTime) {

			// try {
			// method.load(new HashSet<>(this.particles), R, BOTTOM);
			// } catch (IndexOutOfBoundsException e) {
			// System.err.println("Delta T was too big. Try with smaller");
			// System.exit(1);
			// }
			Map<Particle, Set<Particle>> neighbors = findNeighbors();

			List<Particle> nextGen = new ArrayList<>();

			addWallParticles(neighbors);

			allPassedHalf = true;
			existsFreeParticle = false;

			for (Particle particle : particles) {
				Set<Particle> n = neighbors.get(particle);
				Particle p = integralMethod.moveParticle(particle, n);
				if (!passedHalf(p)) {
					allPassedHalf = false;
				} else {
					if (!passedHalfParticles.contains(p)) {
						System.err.println(measuredTime);
						passedHalfParticles.add(p);
					}
				}
				if (n.size() == 0)
					existsFreeParticle = true;
				nextGen.add(p);
			}

			if (Math.abs(currentTime / dt2 - Math.round(currentTime / dt2)) < EPSILON)
				printOvitoState(iteration, neighbors);

			this.particles = nextGen;
			currentTime += dt;
			measuredTime += dt;
			iteration++;

			if (allPassedHalf && measuredTimes.size() == currFlips) {
				System.err.println("AVG FLIP " + currFlips + ": " + measuredTime);
				measuredTimes.add(measuredTime);
			}

			if (allPassedHalf && !existsFreeParticle)
				iterationToRevertGravity++;
			else
				iterationToRevertGravity = 0;

			if (iterationToRevertGravity == ITERATIONS_TO_REVERT_GRAVITY) {
				integralMethod.getAccelerator().reverseGravity();
				currFlips++;
				passedHalfParticles = new HashSet<>();
				iterationToRevertGravity = 0;
				measuredTime = 0;

				if (currFlips > flips)
					return;
				else
					System.err.println("FLIP " + currFlips + ":");
			}
		}
		System.err.println("Time run out at flip " + currFlips);
	}

	public Map<Particle, Set<Particle>> filterNeighbors(Map<Particle, Set<Particle>> original) {
		Map<Particle, Set<Particle>> ret = new HashMap<>();
		for (Particle particle : original.keySet()) {
			HashSet<Particle> set = new HashSet<>();
			for (Particle neighbor : original.get(particle)) {
				if (getDistance(particle.x, particle.y, particle.z, neighbor.x, neighbor.y,
						neighbor.z) <= (particle.r + neighbor.r)) {
					set.add(neighbor);
				}
			}
			ret.put(particle, set);
		}
		return ret;
	}

	private boolean passedHalf(Particle p) {
		return Math.signum(p.z) == Math.signum(integralMethod.getAccelerator().getGravity());
	}

	private void addWallParticles(Map<Particle, Set<Particle>> neighbors) {
		for (Particle particle : particles) {
			List<Particle> newParticles = getContactParticle(particle);
			neighbors.get(particle).addAll(newParticles);
		}
	}

	private List<Particle> getContactParticle(Particle particle) {
		List<Particle> ret = new ArrayList<>();
		Particle p = null;

		int base = particles.size();

		int sign = particle.z >= 0 ? 1 : -1;
		double bound = particle.z >= 0 ? TOP : BOTTOM;

		if (sign * (particle.z + sign * particle.r) > sign * bound) {
			p = new Particle(base++, particle.r, particle.mass);
			p.isWall = true;
			p.x = particle.x;
			p.y = particle.y;
			p.z = bound + sign * particle.r;
			p.speedX = 0;
			p.speedY = 0;
			p.speedZ = 0;

			ret.add(p);
		}
		double levelZeroRadius = Math.sqrt(Math.pow(R, 2) - Math.pow(bound, 2));
		double distanceToVerticalLine = Math.sqrt(Math.pow(particle.x, 2) + Math.pow(particle.y, 2));
		double d = sign == 1 ? distanceToCenterTop(particle) : distanceToCenterBottom(particle);
		if (distanceToVerticalLine > levelZeroRadius) {
			if (d > (R - particle.r) && d < (R + particle.r)) {
				double vecX = particle.x;
				double vecY = particle.y;
				double vecZ = particle.z - bound;

				double magnitude = getMagnitude(vecX, vecY, vecZ);

				double normVecX = vecX / magnitude;
				double normVecY = vecY / magnitude;
				double normVecZ = vecZ / magnitude;

				double x = normVecX * (R + particle.r);
				double y = normVecY * (R + particle.r);
				double z = normVecZ * (R + particle.r);

				p = new Particle(base++, particle.r, particle.mass);
				p.isWall = true;
				p.x = x;
				p.y = y;
				p.z = z + bound;
				p.speedX = 0;
				p.speedY = 0;
				p.speedZ = 0;
				ret.add(p);
			}
		} else if (distanceToVerticalLine + particle.r > levelZeroRadius) {
			double vecX = particle.x;
			double vecY = particle.y;
			double vecZ = 0;

			double magnitude = getMagnitude(vecX, vecY, vecZ);

			double normVecX = vecX / magnitude;
			double normVecY = vecY / magnitude;

			double x = normVecX * levelZeroRadius;
			double y = normVecY * levelZeroRadius;

			p = new Particle(base++, 0, Double.MAX_VALUE);
			p.isWall = true;
			p.x = x;
			p.y = y;
			p.z = 0;
			p.speedX = 0;
			p.speedY = 0;
			p.speedZ = 0;

			// Check if point inside particle
			if (Math.sqrt(Math.pow(p.x - particle.x, 2) + Math.pow(p.y - particle.y, 2)
					+ Math.pow(p.z - particle.z, 2)) <= particle.r)
				ret.add(p);

		}
		return ret;

	}

	private Map<Particle, Set<Particle>> findNeighbors() {
		Map<Particle, Set<Particle>> ret = new HashMap<>();
		for (Particle p1 : particles) {
			if (!ret.containsKey(p1))
				ret.put(p1, new HashSet<>());
			for (Particle p2 : particles) {
				if (p1.id != p2.id && inContact(p1, p2)) {
					if (!ret.get(p1).contains(p2)) {
						ret.get(p1).add(p2);
						if (!ret.containsKey(p2))
							ret.put(p2, new HashSet<>());
						ret.get(p2).add(p1);
					}
				}
			}
		}
		return ret;
	}

	public void printOvitoState(int iteration, Map<Particle, Set<Particle>> otherp) {

		createBounds(particles.size());
		System.out.println(particles.size() + bounds.size());
		System.out.println("t " + iteration);

		calculateMaxSpeed();

		for (Particle p : particles) {
			double relative_speed = 1;
			if (maxSpeed > 0) {
				relative_speed = p.getVelocity() / maxSpeed;
			}
			System.out.println(p.id + "\t" + p.x + "\t" + p.y + "\t" + p.z + "\t" + p.r + "\t" + p.mass + "\t"
					+ relative_speed + "\t" + ((relative_speed / 2) + 0.5) + "\t"
					+ ((Math.abs(1 - relative_speed) / 2) + 0.5) + "\t0");
		}

		for (Particle p : bounds) {
			System.out.println(
					p.id + "\t" + p.x + "\t" + p.y + "\t" + p.z + "\t" + p.r + "\t" + p.mass + "\t1\t1\t1\t0.5");
		}
	}

	private void calculateMaxSpeed() {
		for (Particle particle : particles) {
			double auxSpeed = particle.getVelocity();
			if (auxSpeed > maxSpeed)
				maxSpeed = auxSpeed;
		}
	}

	private void createBounds(int size) {
		if (bounds == null) {
			bounds = new ArrayList<>();
			int base = size;
			double r = 0.01;
			double percentage = 50.0;
			Particle p = null;
			for (double i = Math.sqrt(Math.pow(R, 2) - Math.pow(TOP, 2)); i < R; i += (R / percentage)) {
				double z1 = -1 * (Math.sqrt(Math.pow(R, 2) - Math.pow(i, 2))) + TOP;
				double z2 = -1 * z1;
				for (double j = -i; j <= i; j += (i / percentage)) {
					double y = Math.sqrt(Math.pow(i, 2) - Math.pow(j, 2));
					p = new Particle(base++, r, j, y, z1, 0, 0, 0, MASS);
					bounds.add(p);
					p = new Particle(base++, r, j, y, z2, 0, 0, 0, MASS);
					bounds.add(p);
					p = new Particle(base++, r, j, -y, z1, 0, 0, 0, MASS);
					bounds.add(p);
					p = new Particle(base++, r, j, -y, z2, 0, 0, 0, MASS);
					bounds.add(p);
				}
			}
			for (double j = -R; j <= R; j += (R / percentage)) {
				double y = Math.sqrt(Math.pow(R, 2) - Math.pow(j, 2));
				p = new Particle(base++, r, j, y, TOP, 0, 0, 0, MASS);
				bounds.add(p);
				p = new Particle(base++, r, j, y, BOTTOM, 0, 0, 0, MASS);
				bounds.add(p);
				p = new Particle(base++, r, j, -y, TOP, 0, 0, 0, MASS);
				bounds.add(p);
				p = new Particle(base++, r, j, -y, BOTTOM, 0, 0, 0, MASS);
				bounds.add(p);
			}
		}
	}

	public void printAvgTime() {
		if (measuredTimes.size() > 0) {
			double aux = 0;
			for (Double each : measuredTimes) {
				aux += each;
			}
			System.err.println("AVG: " + (aux / measuredTimes.size()));
		}
	}

	public static void main(String[] args) {
		double R = 1;
		double D = 1;
		double S = 0.25;

		int flips = 0;
		double maxTime = 0;
		double deltaT = 0.000001;
		double deltaT2 = 0.02;
		double kn = 1E4;
		double gamma = 100;

		int maxParticles = Integer.MAX_VALUE;

		try {
			R = Double.valueOf(args[0]);
			D = Double.valueOf(args[1]);
			S = Double.valueOf(args[2]);
			deltaT = Double.valueOf(args[3]);
			deltaT2 = Double.valueOf(args[4]);
			kn = Double.valueOf(args[5]);
			gamma = Double.valueOf(args[6]);
			flips = Integer.valueOf(args[7]);
			maxTime = Double.valueOf(args[8]);
			if (args.length == 10) {
				maxParticles = Integer.valueOf(args[9]);
			}
		} catch (Exception e) {
			System.err.println("Wrong Parameters. Expect R D S deltaT deltaT2 kn kt flips maxTime (maxParticles)");
			return;
		}

		Accelerator accelerator = new GranularAccelerator(kn, gamma);
		IntegralMethod integralMethod = new Beeman(deltaT, accelerator);

		HourGlass g = new HourGlass(R, D, S, deltaT, deltaT2, flips, maxTime, maxParticles, integralMethod);
		g.run();
		g.printAvgTime();

	}

}
