package itba.ss.TP2_CellularAutomata;

public class Particle {

	int id;
	double x;
	double y;
	double r;

	public Particle(int id, double x, double y, double r) {
		this.id = id;
		this.x = x;
		this.y = y;
		this.r = r;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Particle other = (Particle) obj;
		if (id != other.id)
			return false;
		return true;
	}

}