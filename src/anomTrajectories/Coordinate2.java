package anomTrajectories;

import java.util.ArrayList;
import java.util.HashMap;

public class Coordinate2 {
	double latitude;
	double longitude;
	long time;
	long timeUnified;
	public static final int distanceHaversine = 0;
	public static final int distanceVincenty = 1;
	public static final int distanceFast = 2;
	HashMap<Integer, Integer>indexQuadrant;
	static double radius = 6371; // earth's mean radius in km
	
	public Coordinate2(double latitude, double longitude, long time) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.time = time;
		indexQuadrant = new HashMap<Integer, Integer>();
	}
	
	public Coordinate2(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}
	
	public void setTimeUnified(long timeUnified) {
		this.timeUnified = timeUnified;
	}
	
	public static Coordinate2 calculateCentroid(ArrayList<Coordinate2>coordinates) {
		double lat, lon;
		long t;
		
		lat = lon = 0.0;
		t = 0L;
		for(Coordinate2 coordinate:coordinates) {
			lat += coordinate.latitude;
			lon += coordinate.longitude;
			t += coordinate.time;
		}
		lat /= coordinates.size();
		lon /= coordinates.size();
		t /= coordinates.size();
		
		return new Coordinate2(lat, lon, t); 
	}
	
	public Coordinate2 aggregate(Coordinate2 other) {
		double lat, lon;
		long t;
		
		lat = (this.latitude + other.latitude) / 2;
		lon = (this.longitude + other.longitude) / 2;
		t = (this.time + other.time) / 2;
		
		return new Coordinate2(lat, lon, t);
	}

	public double distancia(Coordinate2 coordinate, int typeDistance){
		if(typeDistance == distanceHaversine){
			return distanciaHaversine(coordinate);
		}
		if(typeDistance == distanceVincenty){
			return distanceVincenty(coordinate);
		}
		if(typeDistance == distanceFast){
			return distanceFast(coordinate);
		}
		return distanciaHaversine(coordinate);
	}
	
	public double distance(Coordinate2 coordinate, double velocity) {
		double dist;
		
//		dist = this.distanceNormal(coordenada, velocity);
		dist = this.distanceWithCompensation(coordinate, velocity);
//		dist = this.distanceWithNormalization(coordenada);
//		dist = this.distanceWithoutTime(coordenada);
		
		return dist;
	}
	
	public double distanciaHaversine(Coordinate2 coordinate){
		 final double R = 6372800.0; // In meters
		 double lat1, lat2;
		 double dLat = Math.toRadians(this.latitude - coordinate.latitude);
		 double dLon = Math.toRadians(this.longitude - coordinate.longitude);
		 
		 lat1 = Math.toRadians(this.latitude);
		 lat2 = Math.toRadians(coordinate.latitude);
		 
		 double a = Math.pow(Math.sin(dLat / 2),2) + Math.pow(Math.sin(dLon / 2),2) * Math.cos(lat1) * Math.cos(lat2);
		 double c = 2 * Math.asin(Math.sqrt(a));
		 return R * c;
	}
	
	public double distanceFast(Coordinate2 coordinate){
		double lat2, lon2, dist, deglen, x, y;
		
		lat2 = coordinate.latitude;
		lon2 = coordinate.longitude;
		deglen = 110.25;
		x = this.latitude - lat2;
		y = (this.longitude - lon2) * Math.cos(lat2);
		dist = deglen * Math.sqrt(x*x + y*y);
		dist *= 1000;	//meters
		return dist;
	}
	
	public double distanceWithTimeNormalizedAnt(Coordinate2 coordinate) {
		double dist, normDist, normDifTime;
		long difTime;
		
		dist = this.distanceFast(coordinate);
		difTime = Math.abs(this.time - coordinate.time);
		normDist = (dist - Functions.minDist) / (Functions.maxDist - Functions.minDist);
		normDifTime = (double)(difTime - Functions.minTime) / (double)(Functions.maxTime - Functions.minTime);
		dist = (normDist * 0.5) + (normDifTime * 0.5);
		
		return  dist;
	}
	
	public double distanceWithTimeNormalized(Coordinate2 coordinate) {
		double dist, normDifTime;
		long difTime;
		
		dist = this.distanceFast(coordinate);
		difTime = Math.abs(this.time - coordinate.time);
		normDifTime = difTime * Functions.meanVelocity;
		dist = dist + normDifTime;
		
		return  dist;
	}
	
	public double distanceWithTimeNormalized2(Coordinate2 coordinate) {
		double dist, normDifTime;
		long difTime;
		
		dist = this.distanceFast(coordinate);
		difTime = Math.abs(this.time - coordinate.time);
		normDifTime = difTime * Functions.meanVelocity;
		dist = (dist * 0.5) + (normDifTime * 0.5);
		
		return  dist;
	}
	
	public double distanceNormal(Coordinate2 coordinate, double velocity) {
		double dist, normDifTime;
		long difTime;
		
		dist = this.distanceFast(coordinate);
		difTime = Math.abs(this.time - coordinate.time);
		normDifTime = difTime * velocity;
		dist = dist + normDifTime;
		
		return  dist;
	}
	
	public double distanceWithCompensation(Coordinate2 coordinate, double velocity) {
		double dist, normDifTime;
		long difTime;
		
		dist = this.distanceFast(coordinate);
		difTime = Math.abs(this.time - coordinate.time);
		velocity = 0;
		normDifTime = difTime * velocity;
		normDifTime /= Functions.compensation;
		dist = dist + normDifTime;
		
		return  dist;
	}
	
	public double distanceWithNormalization(Coordinate2 coordinate) {
		double dist, normDist, normDifTime;
		long difTime;
		
		dist = this.distanceFast(coordinate);
		difTime = Math.abs(this.time - coordinate.time);
		normDist = (dist - Functions.minDist) / (Functions.maxDist - Functions.minDist);
		normDifTime = (double)(difTime - Functions.minTime) / (double)(Functions.maxTime - Functions.minTime);
		dist = (normDist * 0.5) + (normDifTime * 0.5);
		dist = dist * (Functions.maxDist - Functions.minDist);
		
		return  dist;
	}
	
	public double distanceWithoutTime(Coordinate2 coordinate) {
		double dist;
		
		dist = this.distanceFast(coordinate);
		
		return  dist;
	}
	
	// Calculate the distance between two points in meters
	double distanceHaversine(Coordinate2 other) {

		double lat1 = DegToRad(this.latitude);
		double lon1 = DegToRad(this.longitude);

		double lat2 = DegToRad(other.latitude);
		double lon2 = DegToRad(other.longitude);

		double deltaLat = lat2 - lat1;
		double deltaLon = lon2 - lon1;

		double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

		return ((radius * c) * 1000);
	}
	
	// Helper function to convert degrees to radians
	public static double DegToRad(double deg) {
		return (deg * Math.PI / 180);
	}

	// Helper function to convert radians to degrees
	public static double RadToDeg(double rad) {
		return (rad * 180 / Math.PI);
	}
	
	// Calculate the (initial) bearing between two points, in degrees
	public double CalculateBearing(Coordinate2 endPoint) {
		double lat1 = DegToRad(this.latitude);
		double lat2 = DegToRad(endPoint.latitude);
		double deltaLon = DegToRad(endPoint.longitude - this.longitude);

		double y = Math.sin(deltaLon) * Math.cos(lat2);
		double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(deltaLon);
		double bearing = Math.atan2(y, x);

		// since atan2 returns a value between -180 and +180, we need to convert it to 0 - 360 degrees
		return (RadToDeg(bearing) + 360) % 360;
	}
	
	// Calculate the destination point from given point having travelled the given distance (in meters),
	// on the given initial bearing (bearing may vary before destination is reached)
	public static Coordinate2 CalculateDestinationLocation(Coordinate2 point, double bearing, double distance) {

		distance = (distance / 1000) / radius; // convert to angular distance in radians
		bearing = DegToRad(bearing); // convert bearing in degrees to radians

		double lat1 = DegToRad(point.latitude);
		double lon1 = DegToRad(point.longitude);

		double lat2 = Math.asin(Math.sin(lat1) * Math.cos(distance) + Math.cos(lat1) * Math.sin(distance) * Math.cos(bearing));
		double lon2 = lon1 + Math.atan2(Math.sin(bearing) * Math.sin(distance) * Math.cos(lat1), Math.cos(distance) - Math.sin(lat1) * Math.sin(lat2));
		lon2 = (lon2 + 3 * Math.PI) % (2 * Math.PI) - Math.PI; // normalize to -180 - + 180 degrees

		return new Coordinate2(RadToDeg(lat2), RadToDeg(lon2));
	}
	
	public double distanceTime2(Coordinate2 coordinate) {
		double dist;
		double difTime;
		int penalty;
		
		dist = this.distanceFast(coordinate);
		difTime = (double)(this.time - coordinate.time) / Functions.marginInTime;
		penalty = ((int)difTime) + 1;
		dist *= penalty;
		
		return  dist;
	}

	public double distanceVincenty(Coordinate2 coordinate) {
		return distVincenty(this.latitude, this.longitude, coordinate.latitude, coordinate.longitude);
	}
	
	/**
	 * Calculates geodetic distance between two points specified by latitude/longitude using Vincenty inverse formula
	 * for ellipsoids
	 * 
	 * @param lat1
	 *            first point latitude in decimal degrees
	 * @param lon1
	 *            first point longitude in decimal degrees
	 * @param lat2
	 *            second point latitude in decimal degrees
	 * @param lon2
	 *            second point longitude in decimal degrees
	 * @returns distance in meters between points with 5.10<sup>-4</sup> precision
	 * @see <a href="http://www.movable-type.co.uk/scripts/latlong-vincenty.html">Originally posted here</a>
	 */
	private static double distVincenty(double lat1, double lon1, double lat2, double lon2) {
	    double a = 6378137, b = 6356752.314245, f = 1 / 298.257223563; // WGS-84 ellipsoid params
	    double L = Math.toRadians(lon2 - lon1);
	    double U1 = Math.atan((1 - f) * Math.tan(Math.toRadians(lat1)));
	    double U2 = Math.atan((1 - f) * Math.tan(Math.toRadians(lat2)));
	    double sinU1 = Math.sin(U1), cosU1 = Math.cos(U1);
	    double sinU2 = Math.sin(U2), cosU2 = Math.cos(U2);

	    double sinLambda, cosLambda, sinSigma, cosSigma, sigma, sinAlpha, cosSqAlpha, cos2SigmaM;
	    double lambda = L, lambdaP, iterLimit = 100;
	    do {
	        sinLambda = Math.sin(lambda);
	        cosLambda = Math.cos(lambda);
	        sinSigma = Math.sqrt((cosU2 * sinLambda) * (cosU2 * sinLambda)
	                + (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda) * (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda));
	        if (sinSigma == 0)
	            return 0; // co-incident points
	        cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * cosLambda;
	        sigma = Math.atan2(sinSigma, cosSigma);
	        sinAlpha = cosU1 * cosU2 * sinLambda / sinSigma;
	        cosSqAlpha = 1 - sinAlpha * sinAlpha;
	        cos2SigmaM = cosSigma - 2 * sinU1 * sinU2 / cosSqAlpha;
	        if (Double.isNaN(cos2SigmaM))
	            cos2SigmaM = 0; // equatorial line: cosSqAlpha=0 (§6)
	        double C = f / 16 * cosSqAlpha * (4 + f * (4 - 3 * cosSqAlpha));
	        lambdaP = lambda;
	        lambda = L + (1 - C) * f * sinAlpha
	                * (sigma + C * sinSigma * (cos2SigmaM + C * cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM)));
	    } while (Math.abs(lambda - lambdaP) > 1e-12 && --iterLimit > 0);

	    if (iterLimit == 0)
	        return Double.NaN; // formula failed to converge

	    double uSq = cosSqAlpha * (a * a - b * b) / (b * b);
	    double A = 1 + uSq / 16384 * (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)));
	    double B = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)));
	    double deltaSigma = B
	            * sinSigma
	            * (cos2SigmaM + B
	                    / 4
	                    * (cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM) - B / 6 * cos2SigmaM
	                            * (-3 + 4 * sinSigma * sinSigma) * (-3 + 4 * cos2SigmaM * cos2SigmaM)));
	    double dist = b * A * (sigma - deltaSigma);

	    return dist;
	}
	
	@Override
	public String toString() {
		return "lat=" + latitude + ",lon=" + longitude + ",t=" + time;
	}
	
	
//	While it seems OK to use linear interpolation for shorter distances, it can in fact be quite off, especially as you get closer to the poles. Seeing from the example that you are in Hamburg, this will already have an effect that's noticable over a few hundred meters. See this answer for a good explanation.
//
//	The Problem: The distance between 1 degree in longitude varies greatly depending on your latitude.
//
//	This is because the earth is NOT flat, but a sphere - actually an ellipsoid. Therefore a straight line on a two dimensional map is NOT a straight line on the globe - and vice versa.
//
//	To get around this problem one can use the following approach:
//
//	Get the bearing from the start coordinate (L1) to the end coordinate (L2)
//	Calculate a new coordinate from the start coordinate (L1) along a great circle path, given the calculated bearing and a specified distance
//	Repeat this process, but using the newly calculated coordinate as the starting coordinate
//	We can create a few simple functions that will do the trick for us:
//	
//	This uses a mean earth radius of 6371 km. See Wikipedia for an explanation of this number and its accuracy.
//
//	One can now calculate a new intermediary location between the two points, given a distance travelled (in km):
//	
//	double bearing = CalculateBearing(startLocation, endLocation);
//
//	Location intermediaryLocation = CalculateDestinationLocation(startLocation, bearing, distanceTravelled);
//	
//	Assuming a speed of v (e.g. 1.39) meters per second, one can now use a simple for loop to get points 1 second apart:
//	
//	List<Location> locations = new ArrayList<Location>();
//
//	// assuming duration in full seconds
//	for (int i = 0; i < duration; i++){
//	    double bearing = CalculateBearing(startLocation, endLocation);
//	    double distanceInKm = v / 1000;
//	    Location intermediaryLocation = CalculateDestinationLocation(startLocation, bearing, distanceInKm);
//
//	    // add intermediary location to list
//	    locations.add(intermediaryLocation);
//
//	    // set intermediary location as new starting location
//	    startLocation = intermediaryLocation;
//	}
//	
//	As an added bonus, you can even determin the time required to travel between any two points:
//	
//	double distanceBetweenPoints = CalculateDistanceBetweenLocations(startPoint, endPoint) * 1000; // multiply by 1000 to get meters instead of km
//
//	double timeRequired = distanceBetweenPoints / v;
	
	

}
