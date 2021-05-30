package anomTrajectories;

import java.util.ArrayList;
import java.util.HashMap;

public class Trajectory {
	ArrayList<Coordinate2>coordinate;	
	ArrayList<Coordinate2>coordinateNormalized;	
	ArrayList<Coordinate2>coordinatesInterpolated;	
	ArrayList<Coordinate2>coordinatesAnom;	
	ArrayList<Coordinate2>centroidsQuadrants;	
	ArrayList<ArrayList<GridCoordinate>>quadrants;	
	ArrayList<ArrayList<GridCoordinate>>interpolatedQuadrants;	
	ArrayList<String>topicsString;	
	ArrayList<String>topicsInterpolatedString;	
	ArrayList<Section>sections;
	ArrayList<Section>sectionsQuadrants;
	boolean occupied;
	String cab;
	int idCab;
	int idTrajectory;
	boolean driver;
	double distance;
	double distanceAcum;
	double distanceAcumCentroidsQuadrants;
	double acumulatedTime;
	double velocity;
	double velocityAnom;
	int time;
	boolean matching;
	boolean sendInfo;
	int index;
	static double metersbetweenPoints;
	static boolean interpolated = false;
	
	public Trajectory(ArrayList<Coordinate2> coordinates, boolean occupied, String cab, int idCab){
		this.coordinate = coordinates;
		this.occupied = occupied;
		this.cab = cab;
		this.idCab = idCab;
		this.driver = false;
		this.matching = false;
		this.sendInfo = false;
	}
	
	public Trajectory(ArrayList<Coordinate2> coordinates){
		this.coordinate = coordinates;
	}
	
	public void calculateGeneralizedTrajectories(ArrayList<GridGenerator2> grids, int timeMargin) {
		GridCoordinate quadrant, quadrantAnt;
		ArrayList<GridCoordinate>quadrantTemp;
		String topic;
		
		this.calculateCoordinatesInterpolated();
		
		quadrants = new ArrayList<ArrayList<GridCoordinate>>();
		for(GridGenerator2 grid:grids) {	// Grids are sorted as gridSize from small to big (more to less granularity)
			quadrantTemp = new ArrayList<GridCoordinate>();
			for(Coordinate2 coordinate:this.coordinate) {
				quadrant = grid.generalizeCoordinate(coordinate);
				if(quadrantTemp.size() == 0) {
					quadrantTemp.add(quadrant);
				}
				else {
					quadrantAnt = quadrantTemp.get(quadrantTemp.size()-1);
					if(!quadrant.equals(quadrantAnt)) {
						quadrantTemp.add(quadrant);
					}
				}
			}
			quadrants.add(quadrantTemp);
		}
		
		this.topicsString = new ArrayList<String>();
		for(int indexGrid=0; indexGrid<grids.size(); indexGrid++) {
			topic = generateTopicString(indexGrid, timeMargin, false);
			topicsString.add(topic);
		}
		
		interpolatedQuadrants = new ArrayList<ArrayList<GridCoordinate>>();
		for(GridGenerator2 grid:grids) {	// Grids are sorted as gridSize from small to big (more to less granularity)
			quadrantTemp = new ArrayList<GridCoordinate>();
			for(Coordinate2 coordinate:this.coordinatesInterpolated) {
				quadrant = grid.generalizeCoordinate(coordinate);
				if(quadrantTemp.size() == 0) {
					quadrantTemp.add(quadrant);
				}
				else {
					quadrantAnt = quadrantTemp.get(quadrantTemp.size()-1);
					if(!quadrant.equals(quadrantAnt)) {
						quadrantTemp.add(quadrant);
					}
				}
			}
			interpolatedQuadrants.add(quadrantTemp);
		}
		this.coordinatesInterpolated.clear();	//to free up memory
		
		this.topicsInterpolatedString = new ArrayList<String>();
		for(int indexGrid=0; indexGrid<grids.size(); indexGrid++) {
			topic = generateTopicString(indexGrid, timeMargin, true);
			topicsInterpolatedString.add(topic);
		}
		
	}
	
	public String generateTopicString(int gridIndex, int timeMargin, boolean interpolated) {
		long tMean;
		String quadrantsString, topicString;
		
		tMean = this.timeMean();
		tMean /= timeMargin;
			if(interpolated) {
				quadrantsString = this.getQuadrantsInterpolatedString(gridIndex);
			}
			else {
				quadrantsString = this.getQuadrantsString(gridIndex);
			}
			topicString = quadrantsString + String.valueOf(tMean);
			
		return topicString;
	}
	
	public void calculateCoordinatesInterpolated() {
		Coordinate2 coordinateIni, coordinateEnd, coordinateNew;
		double timeInterpolatedTemp, gapTime;
		long timeInterpolated;
		double dist;
		int numPoints;
		double x, y, gap, x1, x2, y1, y2;
		boolean interpolatedLatitude;
		
		coordinatesInterpolated = new ArrayList<Coordinate2>();
		for(int i=0; i<this.coordinate.size()-1; i++) {
			coordinateIni = this.coordinate.get(i);
			coordinateEnd = this.coordinate.get(i+1);
			if(coordinateIni.latitude == coordinateEnd.latitude && coordinateIni.longitude == coordinateEnd.longitude) {
				coordinatesInterpolated.add(coordinateIni);
				coordinatesInterpolated.add(coordinateEnd);
				continue;
			}
			dist = coordinateIni.distanceFast(coordinateEnd);
			numPoints = (int)(dist / metersbetweenPoints);
			if(numPoints <= 1) {
				coordinatesInterpolated.add(coordinateIni);
				coordinatesInterpolated.add(coordinateEnd);
				continue;
			}
			if(coordinateEnd.latitude == coordinateIni.latitude) {
				interpolatedLatitude = false;
				gap = coordinateEnd.longitude - coordinateIni.longitude;
				x = coordinateIni.longitude; 
				x1 = coordinateIni.longitude;
				x2 = coordinateEnd.longitude;
				y1 = coordinateIni.latitude;
				y2 = coordinateEnd.latitude;
			}
			else {
				interpolatedLatitude = true;
				gap = coordinateEnd.latitude - coordinateIni.latitude;
				x = coordinateIni.latitude;
				x1 = coordinateIni.latitude;
				x2 = coordinateEnd.latitude;
				y1 = coordinateIni.longitude;
				y2 = coordinateEnd.longitude;
			}
			gap /= numPoints;
			gapTime = (float)(coordinateEnd.time - coordinateIni.time) / (float)numPoints;
			timeInterpolatedTemp = coordinateIni.time;
			coordinatesInterpolated.add(coordinateIni);
			for(int j=0; j<numPoints-1; j++) {
				x += gap;
				y = interpolate(x, x1, y1, x2, y2);
				timeInterpolatedTemp += gapTime;
				timeInterpolated = Math.round(timeInterpolatedTemp);
				if(interpolatedLatitude) {
					coordinateNew = new Coordinate2(x, y, timeInterpolated);
				}
				else {
					coordinateNew = new Coordinate2(y, x, timeInterpolated);
				}
				this.coordinatesInterpolated.add(coordinateNew);
			}
			coordinatesInterpolated.add(coordinateEnd);
		}
	}
	
	public static double interpolate(double x, double x1, double y1, double x2, double y2) {
		double y;
		
		y = y1 + ((y2 - y1) / (x2 - x1)) * (x - x1);
		
		return y;
	}
	
	public boolean matchQuadrants(Trajectory other, int indexQuadrant) {
		GridCoordinate quadrant, quadrantOther;
		
		if(this.quadrants.size() != other.quadrants.size()) {
			return false;
		}
		for(int i=0; i<this.quadrants.size(); i++) {
			quadrant = this.quadrants.get(indexQuadrant).get(i);
			quadrantOther = other.quadrants.get(indexQuadrant).get(i);
			if(!quadrant.equals(quadrantOther)) {
				return false;
			}
		}
		
		return true;
	}
	
	public boolean matchQuadrantsInterpolated(Trajectory otro, int indexCuadricula) {
		GridCoordinate quadrant, quadrantOther;
		
		if(this.interpolatedQuadrants.size() != otro.interpolatedQuadrants.size()) {
			return false;
		}
		for(int i=0; i<this.interpolatedQuadrants.size(); i++) {
			quadrant = this.interpolatedQuadrants.get(indexCuadricula).get(i);
			quadrantOther = otro.interpolatedQuadrants.get(indexCuadricula).get(i);
			if(!quadrant.equals(quadrantOther)) {
				return false;
			}
		}
		
		return true;
	}
	
	public String getQuadrantsString(int indexQuadrant) {
		String s = "";
		
		for(GridCoordinate cuadricula:this.quadrants.get(indexQuadrant)) {
			s += cuadricula;
		}
		
		return s;
	}
	
	public String getQuadrantsInterpolatedString(int gridIndex) {
		String s = "";
		
		for(GridCoordinate quadrant:this.interpolatedQuadrants.get(gridIndex)) {
			s += quadrant;
		}
		
		return s;
	}
	
	public String getQuadrantsInterpoladasIniFinString(int indexQuadrant) {
		String s = "";
		
		s += this.interpolatedQuadrants.get(indexQuadrant).get(0);
		s += this.interpolatedQuadrants.get(indexQuadrant).get(this.interpolatedQuadrants.size()-1);
		
		return s;
	}
	
	public ArrayList<Coordinate2> getCoordinates() {
		return coordinate;
	}
	
	public int getNumCoordinates(){
		return this.coordinate.size();
	}

	public Coordinate2 getIni(){
		return coordinate.get(0);
	}
	
	public Coordinate2 getEnd(){
		return coordinate.get(coordinate.size()-1);
	}
	
	public Coordinate2 getCoordenada(int index){
		return coordinate.get(index);
	}
	
	public int getIdcab(){
		return this.idCab;
	}
	
	public double calculateDistanceStartEnd(){	//meters
		double dist;
		
		dist = getIni().distancia(getEnd(), Coordinate2.distanceFast);
		//dist = getInicio().distancia(getFinal(), Coordenada.distanciaHaversine);
		//dist = getInicio().distanciaVincenty(getFinal());
		//dist = VincentyDistanceCalculator.getDistance(
		//		getInicio().latitud, getInicio().longitud, getFinal().latitud, getFinal().longitud);
		
		return dist;
	}
	
	public double calculateAcumuatedDistanceStartEnd(){	//meters
		double dist, distTotal;
		Coordinate2 c1, c2;
		
		distTotal = 0;
		for(int i=0; i<coordinate.size()-1; i++){
			c1 = coordinate.get(i);
			c2 = coordinate.get(i+1);
			//dist = c1.distancia(c2, Coordenada.distanciaHaversine);
			//System.out.println(dist);
			dist = c1.distancia(c2, Coordinate2.distanceFast);
			//System.out.println(dist);
			//dist = c1.distanciaVincenty(c2);
			//dist = VincentyDistanceCalculator.getDistance(
			//		c1.latitud, c1.longitud, c2.latitud, c2.longitud);
			distTotal += dist; 
		}
		
		//dist = getInicio().distanciaVincenty(getFinal());
		//dist = VincentyDistanceCalculator.getDistance(
		//		getInicio().latitud, getInicio().longitud, getFinal().latitud, getFinal().longitud);
		
		return distTotal;
	}
	
	public void calculateAcumulatedDistance() {
		double dist, totalDist;
		
		totalDist = 0;
		for(int i=0; i<this.coordinate.size()-1; i++) {
//			dist = this.coordenadas.get(i).distanceHaversine(this.coordenadas.get(i+1));
			dist = this.coordinate.get(i).distanceFast(this.coordinate.get(i+1));
			totalDist += dist;
		}
		
		this.distanceAcum = totalDist;
	}
	
	public void calculateAcumulatedDistanceInCentroidsQuadrants() {
		double dist, totalDist;
		
		totalDist = 0;
		for(int i=0; i<this.centroidsQuadrants.size()-1; i++) {
//			dist = this.centroidsCuadriculas.get(i).distanceHaversine(this.centroidsCuadriculas.get(i+1));
			dist = this.centroidsQuadrants.get(i).distanceFast(this.centroidsQuadrants.get(i+1));
			totalDist += dist;
		}
		
		this.distanceAcumCentroidsQuadrants = totalDist;
	}
	
	public void calculateAcumulatedTime() {
		long time, totalTime;
		
		totalTime = 0;
		for(int i=0; i<this.coordinate.size()-1; i++) {
			time = Math.abs(this.coordinate.get(i).time - this.coordinate.get(i+1).time);
			totalTime += time;
		}
		
		this.acumulatedTime = totalTime;
	}
	
	public void calculateVelocity() {
		this.velocity = this.distanceAcum / this.acumulatedTime;
	}
	
	public int calculateTime(){	//seconds
		int tiempo;
		
		tiempo = (int)(getEnd().time - getIni().time);
		
		return tiempo;
	}
	
	public int getAcumulatedTime() {	//In secs
		long time1, time2;
		int acumTime;
		
		acumTime = 0;
		for(int i=0; i<this.coordinate.size()-1; i++) {
			time1 = this.coordinate.get(i).time;
			time2 = this.coordinate.get(i+1).time;
			acumTime += (int)Math.abs(time2 - time1);
		}
		
		return acumTime;
	}
	
	public int getAcumulatedTimeInCentroidsQuadrants() {	//In secs
		long time1, time2;
		int acumTime;
		
		acumTime = 0;
		for(int i=0; i<this.centroidsQuadrants.size()-1; i++) {
			time1 = this.centroidsQuadrants.get(i).time;
			time2 = this.centroidsQuadrants.get(i+1).time;
			acumTime += (int)Math.abs(time2 - time1);
		}
		
		return acumTime;
	}
	
	public long timeMean() {
		long time;
		
		time = getEnd().time + getIni().time;
		time /= 2;
		
		return time;
	}

	public double getDistance() {
		return distance;
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}

	public double getDistanceAcum() {
		return distanceAcum;
	}

	public void setAcumulatedDistance(double distanceAcum) {
		this.distanceAcum = distanceAcum;
	}

	public int getTime() {
		return time;
	}

	public void setTime(int time) {
		this.time = time;
	}

	public boolean isDriver() {
		return driver;
	}

	public void setDriver(boolean driver) {
		this.driver = driver;
	}
	
	public long getTimeInitial(){
		return coordinate.get(0).time;
	}
	
	public int[]searchMatchingIntermediate(Trajectory trajectory){
		Coordinate2 ini, end;
		int indexMatching[] = new int[2];
		
		ini = trajectory.getIni();
		end = trajectory.getEnd();
		indexMatching[0] = searchCLosest(ini);
		indexMatching[1] = searchClosest(indexMatching[0], end);
		
		if(indexMatching[0] >= indexMatching[1]){
			return null;
		}
		
		return indexMatching;
	}
	
	public int[]fillIndexMatching(){
		int indicesMatching[] = new int[2];
		
		indicesMatching[0] = 0;
		indicesMatching[1] = this.coordinate.size()-1;
		
		return indicesMatching;
	}
	
	private int searchCLosest(Coordinate2 coord){
		int indexClosest;
		double dist, minDist;
		
		indexClosest = 0;
		minDist = Double.MAX_VALUE;
		for(int i=0; i<coordinate.size(); i++){
			dist = coordinate.get(i).distancia(coord, Coordinate2.distanceFast);
			//dist = coordenadas.get(i).distancia(coordenada, Coordenada.distanciaHaversine);
			if(dist < minDist){
				indexClosest = i;
				minDist = dist;
			}
		}
		
		return indexClosest;
	}
	
	private int searchClosest(int from, Coordinate2 coord){
		int indexClosest;
		double dist, minDist;
		
		indexClosest = 0;
		minDist = Double.MAX_VALUE;
		for(int i=from; i<coordinate.size(); i++){
			dist = coordinate.get(i).distancia(coord, Coordinate2.distanceFast);
			//dist = coordenadas.get(i).distancia(coordenada, Coordenada.distanciaHaversine);
			if(dist < minDist){
				indexClosest = i;
				minDist = dist;
			}
		}
		
		return indexClosest;
	}
	
	public double distanceAnt(Trajectory other) {
		double dist, RMSE, indexThis, indexOther;
		int numCoordinatesNew, indexInt;
		double gapThis, gapOther, partial;
		Coordinate2 coordinateThis, coordinateOther;
		double velocity;
		
		numCoordinatesNew = (int)Math.round(((double)this.coordinate.size() + (double)other.coordinate.size()) / 2);
		gapThis = (double)this.coordinate.size() / (double)numCoordinatesNew;
		gapOther = (double)other.coordinate.size() / (double)numCoordinatesNew;
		
		partial = 0.0;
		indexThis = indexOther = 0;
		for(int i=0; i<numCoordinatesNew; i++) {
			indexInt = (int) Math.round(indexThis);
			if(indexInt >= this.coordinate.size()) {
				indexInt = this.coordinate.size()-1;
			}
			coordinateThis = this.coordinate.get(indexInt);
			indexInt = (int) Math.round(indexOther);
			if(indexInt >= other.coordinate.size()) {
				indexInt = other.coordinate.size()-1;
			}
			coordinateOther = other.coordinate.get(indexInt);
			velocity = (this.velocity + other.velocity) / 2;
			dist = coordinateThis.distance(coordinateOther, velocity);
			partial += (dist*dist);
			indexThis += gapThis;
			indexOther += gapOther;
		}
		RMSE = partial / numCoordinatesNew;
		RMSE = Math.sqrt(RMSE);
		
		return RMSE;
	}
	
	public double distanceTMD(Trajectory other) {
		double dist, TMD, indexThis, indexOther;
		int numCoordinatesNew, indexInt;
		double gapThis, gapOther, partial;
		Coordinate2 coordinateThis, coordinateOther;
		double velocity;
		
		numCoordinatesNew = (int)Math.round(((double)this.coordinate.size() + (double)other.coordinate.size()) / 2);
		gapThis = (double)this.coordinate.size() / (double)numCoordinatesNew;
		gapOther = (double)other.coordinate.size() / (double)numCoordinatesNew;
		
		partial = 0.0;
		indexThis = indexOther = 0;
		for(int i=0; i<numCoordinatesNew; i++) {
			indexInt = (int) Math.round(indexThis);
			if(indexInt >= this.coordinate.size()) {
				indexInt = this.coordinate.size()-1;
			}
			coordinateThis = this.coordinate.get(indexInt);
			indexInt = (int) Math.round(indexOther);
			if(indexInt >= other.coordinate.size()) {
				indexInt = other.coordinate.size()-1;
			}
			coordinateOther = other.coordinate.get(indexInt);
			velocity = (this.velocity + other.velocity) / 2;
			dist = coordinateThis.distance(coordinateOther, velocity);
			partial += dist;
			indexThis += gapThis;
			indexOther += gapOther;
		}
		TMD = partial / numCoordinatesNew;
		
		return TMD;
	}
	
	public double normalizedTrajectoryDistance(Trajectory other) {
		double dist, partial, RMSE;
		Trajectory newTrajectory, baseTrajectory;
		
		//First convert the smallest trajectory
		//in order to have the same number of coordinates than the greatest trajectory 
		if(this.coordinate.size() > other.coordinate.size()) {
			newTrajectory = this.convert(other);
			baseTrajectory = this;
		}
		else {
			newTrajectory = other.convert(this);
			baseTrajectory = other;
		}
		
		partial = 0.0;
		for(int i=0; i<baseTrajectory.coordinate.size(); i++) {
			dist = baseTrajectory.coordinate.get(i).distanceFast(newTrajectory.getCoordenada(i));
//			dist = baseTrayecto.coordenadas.get(i).distanceHaversine(nuevoTrayecto.getCoordenada(i));
			partial += (dist*dist);
		}
		RMSE = partial / baseTrajectory.coordinate.size();
		RMSE = Math.sqrt(RMSE);
		
		return RMSE;
	}
	
	public double HaservineTrajectoryDistance(Trajectory other) {
		double dist, partial, RMSE;
		
		partial = 0.0;
		for(int i=0; i<this.coordinate.size(); i++) {
//			dist = this.coordenadas.get(i).distanceHaversine(other.getCoordenada(i));
			dist = this.coordinate.get(i).distanceFast(other.getCoordenada(i));
			partial += (dist*dist);
		}
		RMSE = partial / this.coordinate.size();
		RMSE = Math.sqrt(RMSE);
		
		return RMSE;
	}
	
	public Trajectory convertAnt(Trajectory other) {
		ArrayList<Coordinate2>coordinatesNew;
		Coordinate2 coordinate;
		double meters, bearing, vel;
		int indexSection;
		Section section;
		long time1, time2;
		int secs;
		boolean ok;
		
		coordinatesNew = new ArrayList<Coordinate2>();
		//First coordinate
		time1 = this.getTimeInitial();
		time2 = other.getTimeInitial();
		secs = (int)Math.abs(time1 - time2);
		vel = other.sections.get(0).velocity;
		meters = vel * secs;
		bearing = this.getIni().CalculateBearing(other.getIni());
		coordinate = Coordinate2.CalculateDestinationLocation(other.getIni(), bearing, meters);
		coordinatesNew.add(coordinate);
		Trajectory trajectory;
		
		//rest of coordinates
		indexSection = 0;
		section = other.sections.get(indexSection);
		section.restMeters = section.meters;
		for(int i=0; i<this.coordinate.size()-1; i++) {
			time1 = this.coordinate.get(i).time;
			time2 = this.coordinate.get(i+1).time;
			meters = section.velocity * Math.abs(time1 - time2);
			ok = false;
			while(!ok) {
				if(meters <= section.restMeters) {
					coordinate = Coordinate2.CalculateDestinationLocation(coordinate, section.bearing, meters);
					coordinatesNew.add(coordinate);
					section.restMeters -= meters;
					ok = true;
				}
				else {
					meters -= section.restMeters;
					indexSection++;
					section = other.sections.get(indexSection);
					section.restMeters = section.meters;
				}
			}
		}
		
		trajectory = new Trajectory(coordinatesNew);
		
		return trajectory;
	}
	
	public Trajectory convert(Trajectory other) {
		ArrayList<Coordinate2>coordinateNew;
		Coordinate2 coordinate;
		double meters, bearing, vel1, vel2;
		int indexSection;
		Section sectionBase, sectionOther;
		double time1, time2;
		int secs;
		boolean ok;
		Trajectory trayecto;
		
		coordinateNew = new ArrayList<Coordinate2>();
		this.calculateSections();
		other.calculateSections();
		//First coordinate
		time1 = this.getTimeInitial();
		time2 = other.getTimeInitial();
		secs = (int)Math.abs(time1 - time2);
		vel1 = other.sections.get(0).velocity;
		vel2 = this.sections.get(0).velocity;
		meters = ((vel1 + vel2) / 2) * secs;
		bearing = this.getIni().CalculateBearing(other.getIni());
		coordinate = Coordinate2.CalculateDestinationLocation(other.getIni(), bearing, meters);
		coordinateNew.add(coordinate);
		
		//rest of coordinates
		indexSection = 0;
		sectionOther = other.sections.get(indexSection);
		sectionOther.restMeters = sectionOther.meters;
		for(int i=0; i< this.sections.size(); i++) {
			sectionBase = this.sections.get(i);
			meters = other.distanceAcum * sectionBase.timeProportion;
			ok = false;
			while(!ok) {
				if(meters <= sectionOther.restMeters || i == this.sections.size()-1) {
					coordinate = Coordinate2.CalculateDestinationLocation(coordinate, sectionOther.bearing, meters);
					coordinateNew.add(coordinate);
					sectionOther.restMeters -= meters;
					ok = true;
				}
				else {
					meters -= sectionOther.restMeters;
					indexSection++;
					sectionOther = other.sections.get(indexSection);
					sectionOther.restMeters = sectionOther.meters;
				}
			}
		}
		
		trayecto = new Trajectory(coordinateNew);
		
		return trayecto;
	}
	
	public Trajectory convertCentroidsQuadrants(Trajectory other) {
		ArrayList<Coordinate2>coordinatesNew;
		Coordinate2 coordinate;
		double meters, bearing, vel1, vel2;
		int indexSection;
		Section sectionBase, sectionOther;
		double time1, time2;
		int secs;
		boolean ok;
		Trajectory trayecto;
		
		if(other.centroidsQuadrants.size() == 1) {
			return this.convertCentroidsCuadriculasNTo1(other);
		}
		
		coordinatesNew = new ArrayList<Coordinate2>();
		//First coordinate
		this.calculateSectionsCentroidsQuadrants();
		other.calculateSectionsCentroidsQuadrants();
		time1 = this.sectionsQuadrants.get(0).ini.time;
		time2 = other.sectionsQuadrants.get(0).ini.time;
		secs = (int)Math.abs(time1 - time2);
		vel1 = other.sectionsQuadrants.get(0).velocity;
		vel2 = this.sectionsQuadrants.get(0).velocity;
		meters = ((vel1 + vel2) / 2) * secs;
		bearing = this.sectionsQuadrants.get(0).ini.CalculateBearing(other.sectionsQuadrants.get(0).ini);
		coordinate = Coordinate2.CalculateDestinationLocation(other.sectionsQuadrants.get(0).ini, bearing, meters);
		coordinatesNew.add(coordinate);
		
		//rest of coordinates
		indexSection = 0;
		sectionOther = other.sectionsQuadrants.get(indexSection);
		sectionOther.restMeters = sectionOther.meters;
		for(int i=0; i< this.sectionsQuadrants.size(); i++) {
			sectionBase = this.sectionsQuadrants.get(i);
			meters = other.distanceAcumCentroidsQuadrants * sectionBase.timeProportion;
			ok = false;
			while(!ok) {
				if(meters <= sectionOther.restMeters || i == this.sectionsQuadrants.size()-1) {
					coordinate = Coordinate2.CalculateDestinationLocation(coordinate, sectionOther.bearing, meters);
					coordinatesNew.add(coordinate);
					sectionOther.restMeters -= meters;
					ok = true;
				}
				else {
					meters -= sectionOther.restMeters;
					indexSection++;
					if(indexSection < other.sectionsQuadrants.size()) {
						sectionOther = other.sectionsQuadrants.get(indexSection);
					}
					sectionOther.restMeters = sectionOther.meters;
				}
			}
		}
		
		trayecto = new Trajectory(coordinatesNew);
		
		return trayecto;
	}
	
	public Trajectory convertCentroidsCuadriculasNTo1(Trajectory other) {
		ArrayList<Coordinate2>coordinatesNew;
		Coordinate2 coordinate, coordOther;
		double meters, bearing, vel1, vel2;
		double time1, time2;
		int secs;
		boolean ok;
		Trajectory trajectory;
		
		coordOther = other.centroidsQuadrants.get(0);	//there is only one coordinate
		coordinatesNew = new ArrayList<Coordinate2>();
		for(Coordinate2 coordThis:this.centroidsQuadrants) {
			time1 = coordThis.time;
			time2 = coordOther.time;
			secs = (int)Math.abs(time1 - time2);
			vel1 = this.velocity;
			vel2 = other.velocity;
			meters = ((vel1 + vel2) / 2) * secs;
			bearing = coordThis.CalculateBearing(coordOther);
			coordinate = Coordinate2.CalculateDestinationLocation(coordOther, bearing, meters);
			coordinatesNew.add(coordinate);
		}
		
		trajectory = new Trajectory(coordinatesNew);
		
		return trajectory;
	}
	
	public void calculateSections() {
		Section section;
		double totalTime;
		
		this.sections = new ArrayList<Section>();
		for(int i=0; i<this.coordinate.size()-1; i++) {
			section = new Section(this.coordinate.get(i), this.coordinate.get(i+1));
			this.sections.add(section);
		}
		totalTime = this.getAcumulatedTime();
		for(Section sec:sections) {
			sec.timeProportion = (double)sec.seconds / totalTime;
		}
	}
	
	public void calculateSectionsCentroidsQuadrants() {
		Section section;
		double totalTime;
		
		this.sectionsQuadrants = new ArrayList<Section>();
		for(int i=0; i<this.centroidsQuadrants.size()-1; i++) {
			section = new Section(this.centroidsQuadrants.get(i), this.centroidsQuadrants.get(i+1));
			this.sectionsQuadrants.add(section);
		}
		totalTime = this.getAcumulatedTimeInCentroidsQuadrants();
		for(Section sec:sectionsQuadrants) {
			sec.timeProportion = (double)sec.seconds / totalTime;
		}
	}
	
	public static Trajectory aggregateCoordinates(ArrayList<Trajectory>trajectories) {
		Trajectory trajectory, newTrajectory, baseTrajectory;
		int maxNumCoord, indexTrajectoryBase;
		ArrayList<Trajectory> newTrajectories;
		ArrayList<Coordinate2>aggregatedCoords;
		ArrayList<Coordinate2>coordinates;
		
		//First convert the smallest trajectories
		//in order to have the same number of coordinates than the greatest trajectory
		//In the conversion the distance is normalized by the time
		baseTrajectory = trajectories.get(0);
		maxNumCoord = indexTrajectoryBase = 0;
		for(int i=0; i<trajectories.size(); i++) {
			trajectory = trajectories.get(i);
			if(trajectory.coordinate.size() > maxNumCoord) {
				maxNumCoord = trajectory.coordinate.size();
				baseTrajectory = trajectory;
				indexTrajectoryBase = i;
			}
		}
		
		newTrajectories = new ArrayList<Trajectory>();
		baseTrajectory.coordinateNormalized = baseTrajectory.coordinate;
		newTrajectories.add(baseTrajectory);
		for(int i=0; i<trajectories.size(); i++) {
			if(indexTrajectoryBase != i) {
				trajectory = trajectories.get(i);
				newTrajectory = baseTrajectory.convert(trajectory);
				trajectory.coordinateNormalized = newTrajectory.coordinate;
				newTrajectories.add(newTrajectory);
			}
		}
		
		coordinates = new ArrayList<Coordinate2>();
		aggregatedCoords = new ArrayList<Coordinate2>();
		for(int i=0; i<baseTrajectory.coordinate.size(); i++) {
			for(int j=0; j<newTrajectories.size(); j++) {
				trajectory = newTrajectories.get(j);
				coordinates.add(trajectory.coordinate.get(i));
			}
			aggregatedCoords.add(Coordinate2.calculateCentroid(coordinates));
			coordinates.clear();
		}
		
		return new Trajectory(aggregatedCoords, true, "", 0);
	}
	
	public static Trajectory aggregateCentroids(ArrayList<Trajectory>trajectories) {
		Trajectory trajectory, newTrajectory, baseTrajectory;
		int maxNumCoord, indexTrajectoryBase;
		ArrayList<Trajectory> newTrajectories;
		ArrayList<Coordinate2>aggregatedCoords;
		ArrayList<Coordinate2>coordinates;
		
		//Calculate accumulated distance based on centroids
		for(Trajectory t:trajectories) {
			t.calculateAcumulatedDistanceInCentroidsQuadrants();
		}
		
		//First convert the smallest trajectories
		//in order to have the same number of coordinates than the greatest trajectory
		baseTrajectory = trajectories.get(0);
		maxNumCoord = indexTrajectoryBase = 0;
		for(int i=0; i<trajectories.size(); i++) {
			trajectory = trajectories.get(i);
			if(trajectory.centroidsQuadrants.size() > maxNumCoord) {
				maxNumCoord = trajectory.coordinate.size();
				baseTrajectory = trajectory;
				indexTrajectoryBase = i;
			}
		}

		newTrajectories = new ArrayList<Trajectory>();
		//The normalized coordinates of the base trajectory do not change
		baseTrajectory.coordinateNormalized = baseTrajectory.centroidsQuadrants;
		newTrajectories.add(baseTrajectory);
		for(int i=0; i<trajectories.size(); i++) {
			if(indexTrajectoryBase != i) {
				trajectory = trajectories.get(i);
				newTrajectory = baseTrajectory.convertCentroidsQuadrants(trajectory);
				trajectory.coordinateNormalized = newTrajectory.coordinate;
				newTrajectory.centroidsQuadrants = newTrajectory.coordinate;
				newTrajectories.add(newTrajectory);
			}
		}

		coordinates = new ArrayList<Coordinate2>();
		aggregatedCoords = new ArrayList<Coordinate2>();
		for(int i=0; i<baseTrajectory.centroidsQuadrants.size(); i++) {
			for(int j=0; j<newTrajectories.size(); j++) {
				trajectory = newTrajectories.get(j);
				coordinates.add(trajectory.centroidsQuadrants.get(j));
			}
			aggregatedCoords.add(Coordinate2.calculateCentroid(coordinates));
			coordinates.clear();
		}
		
		return new Trajectory(aggregatedCoords, true, "", 0);
	}
	
	public static Trajectory aggregateCoordinatesAnt(ArrayList<Trajectory>trajectories) {
		int numCoordinatesNew;
		double gap[];
		double index[];
		ArrayList<Coordinate2>aggregatedCoords;
		ArrayList<Coordinate2>coordinates;
		Trajectory trajectory;
		int indexInt;
		double velocity;
		
		numCoordinatesNew = 0;
		velocity = 0.0;
		for(Trajectory t:trajectories) {
			numCoordinatesNew += t.coordinate.size();
			velocity += t.velocity;
		}
		velocity /= trajectories.size();
		numCoordinatesNew = (int) Math.round((double)numCoordinatesNew / (double)trajectories.size());
		gap = new double[trajectories.size()];
		for(int i=0; i<trajectories.size(); i++) {
			trajectory = trajectories.get(i);
			gap[i] = (double)trajectory.coordinate.size() / (double)numCoordinatesNew;
		}
		
		coordinates = new ArrayList<Coordinate2>();
		aggregatedCoords = new ArrayList<Coordinate2>();
		index = new double[trajectories.size()];
		for(int i=0; i<numCoordinatesNew; i++) {
			for(int j=0; j<trajectories.size(); j++) {
				trajectory = trajectories.get(j);
				indexInt = (int) Math.round(index[j]);
				if(indexInt >= trajectory.coordinate.size()) {
					indexInt = trajectory.coordinate.size()-1;
				}
				coordinates.add(trajectory.coordinate.get(indexInt));
			}
			aggregatedCoords.add(Coordinate2.calculateCentroid(coordinates));
			coordinates.clear();
			for(int j=0; j<trajectories.size(); j++) {
				index[j] += gap[j];
			}
		}
		
		trajectory = new Trajectory(aggregatedCoords, true, "", 0);
		trajectory.velocityAnom = velocity;
		
		return trajectory;
	}
	
	public static Trajectory aggregateInterpolatedAnt(ArrayList<Trajectory>trajectories) {
		int numCoordinatesNew;
		double gap[];
		double index[];
		ArrayList<Coordinate2>aggregatedCoords;
		ArrayList<Coordinate2>coordinates;
		Trajectory trajectory;
		int indexInt;
		double velocity;
		
		numCoordinatesNew = 0;
		velocity = 0.0;
		for(Trajectory t:trajectories) {
			numCoordinatesNew += t.coordinatesInterpolated.size();
			velocity += t.velocity;
		}
		velocity /= trajectories.size();
		numCoordinatesNew = (int) Math.round((double)numCoordinatesNew / (double)trajectories.size());
		gap = new double[trajectories.size()];
		for(int i=0; i<trajectories.size(); i++) {
			trajectory = trajectories.get(i);
			gap[i] = (double)trajectory.coordinatesInterpolated.size() / (double)numCoordinatesNew;
		}
		
		coordinates = new ArrayList<Coordinate2>();
		aggregatedCoords = new ArrayList<Coordinate2>();
		index = new double[trajectories.size()];
		for(int i=0; i<numCoordinatesNew; i++) {
			for(int j=0; j<trajectories.size(); j++) {
				trajectory = trajectories.get(j);
				indexInt = (int) Math.round(index[j]);
				if(indexInt >= trajectory.coordinatesInterpolated.size()) {
					indexInt = trajectory.coordinatesInterpolated.size()-1;
				}
				coordinates.add(trajectory.coordinatesInterpolated.get(indexInt));
			}
			aggregatedCoords.add(Coordinate2.calculateCentroid(coordinates));
			coordinates.clear();
			for(int j=0; j<trajectories.size(); j++) {
				index[j] += gap[j];
			}
		}
		
		trajectory = new Trajectory(aggregatedCoords, true, "", 0);
		trajectory.velocityAnom = velocity;
		
		return trajectory;
	}
	
	public static Trajectory aggregateCentroidsAnt(ArrayList<Trajectory>trajectories) {
		int numCoordinatesNew;
		double gap[];
		double index[];
		ArrayList<Coordinate2>aggregatedCoords;
		ArrayList<Coordinate2>coordinates;
		Trajectory trajectory;
		int indexInt;
		double velocity;
		
		numCoordinatesNew = 0;
		velocity = 0.0;
		for(Trajectory t:trajectories) {
			numCoordinatesNew += t.centroidsQuadrants.size();
			velocity += t.velocity;
		}
		velocity /= trajectories.size();
		numCoordinatesNew = (int) Math.round((double)numCoordinatesNew / (double)trajectories.size());
		gap = new double[trajectories.size()];
		for(int i=0; i<trajectories.size(); i++) {
			trajectory = trajectories.get(i);
			gap[i] = (double)trajectory.centroidsQuadrants.size() / (double)numCoordinatesNew;
		}
		
		coordinates = new ArrayList<Coordinate2>();
		aggregatedCoords = new ArrayList<Coordinate2>();
		index = new double[trajectories.size()];
		for(int i=0; i<numCoordinatesNew; i++) {
			for(int j=0; j<trajectories.size(); j++) {
				trajectory = trajectories.get(j);
				indexInt = (int) Math.round(index[j]);
				if(indexInt >= trajectory.centroidsQuadrants.size()) {
					indexInt = trajectory.centroidsQuadrants.size()-1;
				}
				coordinates.add(trajectory.centroidsQuadrants.get(indexInt));
			}
			aggregatedCoords.add(Coordinate2.calculateCentroid(coordinates));
			coordinates.clear();
			for(int j=0; j<trajectories.size(); j++) {
				index[j] += gap[j];
			}
		}
		
		trajectory = new Trajectory(aggregatedCoords, true, "", 0);
		trajectory.velocityAnom = velocity;
		
		return trajectory;
	}
	
	public Trajectory aggregate(Trajectory other) {
		int numCoordinatesNew, indexInt;
		double gapThis, gapOther, indexThis, indexOther;
		Coordinate2 coordinateThis, coordinateOther, coordinateAggregated;
		ArrayList<Coordinate2>aggregatedCoords;
		Trajectory aggregated;
		double velocity;
		
		numCoordinatesNew = (int) Math.round(((double)this.coordinate.size() + (double)other.coordinate.size()) / 2.0);
		velocity = (this.velocity + other.velocity) / 2;
		gapThis = (double)this.coordinate.size() / (double)numCoordinatesNew;
		gapOther = (double)other.coordinate.size() / (double)numCoordinatesNew;
		
		aggregatedCoords = new ArrayList<Coordinate2>();
		indexThis = indexOther = 0;
		for(int i=0; i<numCoordinatesNew; i++) {
			indexInt = (int) Math.round(indexThis);
			if(indexInt >= this.coordinate.size()) {
				indexInt = this.coordinate.size()-1;
			}
			coordinateThis = this.coordinate.get(indexInt);
			indexInt = (int) Math.round(indexOther);
			if(indexInt >= other.coordinate.size()) {
				indexInt = other.coordinate.size()-1;
			}
			coordinateOther = other.coordinate.get(indexInt);
			coordinateAggregated = coordinateThis.aggregate(coordinateOther);
			aggregatedCoords.add(coordinateAggregated);
			indexThis += gapThis;
			indexOther += gapOther;
		}
		aggregated = new Trajectory(aggregatedCoords, true, "", 0);
		aggregated.velocityAnom = velocity;
		
		return aggregated;
	}

	@Override
	public String toString() {
		return coordinate + ",cab=" + cab + ",idCab=" + idCab + ",dist=" + distance
				+ ",distAcum=" + distanceAcum;
		
//		return cuadriculas;
	}
	
	

}
