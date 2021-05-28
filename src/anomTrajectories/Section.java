package anomTrajectories;

public class Section {
	Coordinate2 ini;
	Coordinate2 fin;
	double velocity;
	double bearing;
	double meters;
	int seconds;
	double restMeters;
	double timeProportion;
	
	public Section(Coordinate2 ini, Coordinate2 fin) {
		this.ini = ini;
		this.fin = fin;
		this.bearing = this.calculateBearing();
		this.meters = this.calculateDistance();
		this.seconds = this.calculateTimeSeconds();
		this.velocity = this.calculateVelocity();
	}
	
	private double calculateBearing() {
		return this.ini.CalculateBearing(fin);
	}

	public double calculateDistance() {
//		return this.ini.distanceHaversine(fin);
		return this.ini.distanceFast(fin);
	}
	
	public int calculateTimeSeconds() {
		return (int)Math.abs(ini.time - fin.time);
	}
	
	public double calculateVelocity() {
		return this.meters / (double)seconds;
	}

}
