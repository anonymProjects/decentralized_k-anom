package anomTrajectories;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.TimeZone;

public class Peer {
	
	int id;
	Trajectory trajectory;
	ArrayList<ArrayList<Peer>>gridMatchings;
	HashSet<Peer>controlPeersMatched;
	int currentK;
	double distance;
	boolean assigned;
	boolean anonymizator;
	int numMessages;
	HashSet<Integer>blackList;
	static ArrayList<Peer>peers;
	static double p;
	static Random rnd = new Random();
	boolean badPeer;
	int indexRandomPeer;
	final static int delivered = 1;
	final static int rejectedByIntermediate = 2;
	final static int rejectedByDestination = 3;
	
	public Peer(int id, Trajectory trayecto){
		this.id = id;
		this.trajectory = trayecto;
		this.initialize();
		this.assigned = false;
		this.anonymizator = false;
		numMessages = 0;
		this.badPeer = false;
		this.blackList = new HashSet<Integer>();
		indexRandomPeer = 0;
	}
	
	public void initialize() {
		this.gridMatchings = new ArrayList<ArrayList<Peer>>();
		this.controlPeersMatched = new HashSet<Peer>();
		this.currentK = 1;
		this.assigned = false;
		this.anonymizator = false;
	}
	
	public void initialize(ArrayList<GridGenerator2> grids) {
		this.initialize();
		for(int i=0; i<grids.size(); i++) {
			this.gridMatchings.add(new ArrayList<Peer>());
		}
	}
	
	public void anonymizeTrajectory1(ArrayList<GridGenerator2>grids, boolean interpolado, boolean aggregating) {
		//Con precalculo de puntos por cuadrante
		//se anonimiza: punto -> cuadrante; punto + puntos (envia toda la trayectoria de una vez)
		if(interpolado) {
			anonymizeTrajectory1Interpolated(grids, aggregating);
		}
		else {
			anonymizeTrajectory1NotInterpolated(grids, aggregating);
		}
	}
	
	public void anonymizeTrajectory2(ArrayList<GridGenerator2>grids, boolean interpolado, boolean aggregating) {
		//Con precalculo de puntos por cuadrante
		//se anonimiza: punto -> cuadrante; punto + centroid quadrante (envia centroid de cada quadrante)
		if(interpolado) {
			anonymizeTrajectory2Interpolated(grids, aggregating);
		}
		else {
			anonymizeTrajectory2NotInterpolated(grids, aggregating);
		}
	}
	
	private void anonymizeTrajectory1Interpolated(ArrayList<GridGenerator2> grids, boolean aggregating) {
		if(aggregating) {
			anonymizeTrajectory1InterpolatedAggregating(grids);
		}
		else {
			anonymizeTrajectory1Interpolated(grids);
		}
		
	}
	
	private void anonymizeTrajectory1NotInterpolated(ArrayList<GridGenerator2> grids, boolean aggregating) {
		if(aggregating) {
			anonymizeTrajectory1NotInterpolatedAggregating(grids);
		}
		else {
			anonymizeTrajectory1NotInterpolated(grids);
		}
		
	}
	
	private void anonymizeTrajectory2Interpolated(ArrayList<GridGenerator2> grids, boolean aggregating) {
		if(aggregating) {
			anonymizeTrajectory2InterpolatedAggregating(grids);
		}
		else {
			anonymizeTrajectory2Interpolated(grids);
		}
		
	}
	
	private void anonymizeTrajectory2NotInterpolated(ArrayList<GridGenerator2> grids, boolean aggregating) {
		if(aggregating) {
			anonymizeTrajectory2NotInterpolatedAggregating(grids);
		}
		else {
			anonymizeTrajectory2NotInterpolated(grids);
		}
		
	}
	
	public void anonymizeTrajectory1NotInterpolated(ArrayList<GridGenerator2>grids) {
		//Con precalculo de puntos por cuadrante
		//se anonimiza: punto -> cuadrante; punto + puntos (envia toda la trayectoria de una vez)
		Trajectory trayectoPeerMatched;
		GridGenerator2 grid;
		Coordinate2 coordenadaMatching, coordenadaAnom;
		ArrayList<Coordinate2>coordenadasInQuadrant;
		ArrayList<Coordinate2>coordenadasForCentroid;
		ArrayList<Integer>gridsWithMatching;
		GridCoordinate quadrantCoordenada, quadrantCuadricula, quadrant;
		Coordinate2 coord;
		int indexCoordenada, indexQuadrant;

		gridsWithMatching = new ArrayList<Integer>();
		for(int indexGrid=0; indexGrid<gridMatchings.size(); indexGrid++) {
			grid = grids.get(indexGrid);
			if(this.gridMatchings.get(indexGrid).size() > 0) {
				gridsWithMatching.add(indexGrid);
			}
			else {	//In this grid there are not matchings
				continue;
			}
			//For each peer matching, put each coordinate in its quadrant of this grid
			for(Peer peerMatched:this.gridMatchings.get(indexGrid)) {
				trayectoPeerMatched = peerMatched.getTrayecto();
				indexCoordenada = 0;
				coord = trayectoPeerMatched.coordinate.get(indexCoordenada);
				quadrantCoordenada = grid.getGridPosition(coord);
				for(int i=0; i<trayectoPeerMatched.quadrants.get(indexGrid).size(); i++) {
					quadrantCuadricula = trayectoPeerMatched.quadrants.get(indexGrid).get(i);
					coordenadasInQuadrant = new ArrayList<Coordinate2>();
					while(quadrantCoordenada.equals(quadrantCuadricula)) {
						coordenadasInQuadrant.add(coord);
						indexCoordenada++;
						if(indexCoordenada >= trayectoPeerMatched.coordinate.size()) {
							break;
						}
						coord = trayectoPeerMatched.coordinate.get(indexCoordenada);
						quadrantCoordenada = grid.getGridPosition(coord);
					}
					quadrantCuadricula.coordinates.addAll(coordenadasInQuadrant);
					quadrantCuadricula.calculateCentroid();
				}
			}
			//For this peer, determine the quadrant of this grid where each coordinate falls
			indexCoordenada = 0;
			coord = this.trajectory.coordinate.get(indexCoordenada);
			quadrantCoordenada = grid.getGridPosition(coord);
			for(int i=0; i<this.trajectory.quadrants.get(indexGrid).size(); i++) {
				quadrantCuadricula = this.trajectory.quadrants.get(indexGrid).get(i);
				while(quadrantCoordenada.equals(quadrantCuadricula)) {
					coord.indexQuadrant.put(indexGrid, i);
					indexCoordenada++;
					if(indexCoordenada >= this.trajectory.coordinate.size()) {
						break;
					}
					coord = this.trajectory.coordinate.get(indexCoordenada);
					quadrantCoordenada = grid.getGridPosition(coord);
				}
			}
		}
		
		//Anonymize
		this.trajectory.coordinatesAnom = new ArrayList<Coordinate2>();
		coordenadasForCentroid = new ArrayList<Coordinate2>();
		for(int i=0; i<this.trajectory.coordinate.size(); i++) {
			coordenadaMatching = this.trajectory.coordinate.get(i);
			coordenadasForCentroid.clear();
			coordenadasForCentroid.add(coordenadaMatching);
			for(int index_grid:gridsWithMatching) {
				indexQuadrant = coordenadaMatching.indexQuadrant.get(index_grid);
				for(Peer peerMatched:this.gridMatchings.get(index_grid)) {
					quadrant = peerMatched.trajectory.quadrants.get(index_grid).get(indexQuadrant);
					coordenadasForCentroid.addAll(quadrant.coordinates);
				}
			}
			coordenadaAnom = Coordinate2.calculateCentroid(coordenadasForCentroid);
			this.trajectory.coordinatesAnom.add(coordenadaAnom);
		}
		
		//Comunicate to the matched peers the resulting anonymization (to can calculate the error later)
		//This is not necessary in a real scenario
		for(int index_grid:gridsWithMatching) {
			for(Peer peerMatched:this.gridMatchings.get(index_grid)) {
				peerMatched.trajectory.coordinatesAnom = new ArrayList<Coordinate2>();
				peerMatched.trajectory.coordinatesAnom.addAll(this.trajectory.coordinatesAnom);
			}
		}

		//free up memory
		this.trajectory.coordinatesInterpolated.clear();
		for(int index_grid:gridsWithMatching) {
			for(GridCoordinate quad:this.trajectory.quadrants.get(index_grid)) {
				quad.coordinates.clear();
			}
		}
		for(Coordinate2 coordenada:this.trajectory.coordinate) {
			coordenada.indexQuadrant.clear();
		}
		for(int index_grid:gridsWithMatching) {
			for(Peer peerMatched:this.gridMatchings.get(index_grid)) {
				for(GridCoordinate quad:peerMatched.trajectory.quadrants.get(index_grid)) {
					quad.coordinates.clear();
				}
			}
		}
	}
	
	public void anonymizeTrajectory1NotInterpolatedAggregating(ArrayList<GridGenerator2>grids) {
		//Con precalculo de puntos por cuadrante
		//se anonimiza: punto -> cuadrante; punto + puntos (envia toda la trayectoria de una vez)
		ArrayList<Peer>peersInCluster;
		Peer centroid;
		Coordinate2 coordenada;
		
		peersInCluster = new ArrayList<Peer>();
		peersInCluster.add(this);
		for(int indexGrid=0; indexGrid<gridMatchings.size(); indexGrid++) {
			for(Peer peerMatched:this.gridMatchings.get(indexGrid)) {
				peersInCluster.add(peerMatched);
			}
		}
		
		//Anonymize
		centroid = Peer.calculateCentroidAggregatingCoordinates(peersInCluster);
		for(Peer peer:peersInCluster) {
			peer.trajectory.coordinatesAnom = new ArrayList<Coordinate2>();
			peer.trajectory.velocityAnom = centroid.trajectory.velocityAnom;
			for(int i=0; i<centroid.trajectory.coordinate.size(); i++) {
				coordenada = centroid.trajectory.coordinate.get(i);
				peer.trajectory.coordinatesAnom.add(coordenada);
			}
		}
		
	}
	
	public void anonymizeTrajectory1Interpolated(ArrayList<GridGenerator2>grids) {
		//Con precalculo de puntos por cuadrante
		//se anonimiza: punto -> cuadrante; punto + puntos (envia toda la trayectoria de una vez)
		Trajectory trayectoPeerMatched;
		GridGenerator2 grid;
		Coordinate2 coordenadaMatching, coordenadaAnom;
		ArrayList<Coordinate2>coordenadasInQuadrant;
		ArrayList<Coordinate2>coordenadasForCentroid;
		ArrayList<Integer>gridsWithMatching;
		GridCoordinate quadrantCoordenada, quadrantCuadricula, quadrant;
		Coordinate2 coord;
		int indexCoordenada, indexQuadrant;
		
		this.trajectory.calculateCoordinatesInterpolated();
		gridsWithMatching = new ArrayList<Integer>();
		for(int indexGrid=0; indexGrid<gridMatchings.size(); indexGrid++) {
			grid = grids.get(indexGrid);
			if(this.gridMatchings.get(indexGrid).size() > 0) {
				gridsWithMatching.add(indexGrid);
			}
			else {	//In this grid there are not matchings
				continue;
			}
			//For each peer matched, put each coordinate in its quadrant of this grid
			for(Peer peerMatched:this.gridMatchings.get(indexGrid)) {
				trayectoPeerMatched = peerMatched.getTrayecto();
				trayectoPeerMatched.calculateCoordinatesInterpolated();
				indexCoordenada = 0;
				coord = trayectoPeerMatched.coordinatesInterpolated.get(indexCoordenada);
				quadrantCoordenada = grid.getGridPosition(coord);
				for(int i=0; i<trayectoPeerMatched.interpolatedQuadrants.get(indexGrid).size(); i++) {
					quadrantCuadricula = trayectoPeerMatched.interpolatedQuadrants.get(indexGrid).get(i);
					coordenadasInQuadrant = new ArrayList<Coordinate2>();
					while(quadrantCoordenada.equals(quadrantCuadricula)) {
						coordenadasInQuadrant.add(coord);
						indexCoordenada++;
						if(indexCoordenada >= trayectoPeerMatched.coordinatesInterpolated.size()) {
							break;
						}
						coord = trayectoPeerMatched.coordinatesInterpolated.get(indexCoordenada);
						quadrantCoordenada = grid.getGridPosition(coord);
					}
					quadrantCuadricula.coordinates.addAll(coordenadasInQuadrant);
					quadrantCuadricula.calculateCentroid();
				}
			}
			//For this peer, determine the quadrant of this grid where each coordinate falls
			indexCoordenada = 0;
			coord = this.trajectory.coordinatesInterpolated.get(indexCoordenada);
			quadrantCoordenada = grid.getGridPosition(coord);
			for(int i=0; i<this.trajectory.interpolatedQuadrants.get(indexGrid).size(); i++) {
				quadrantCuadricula = this.trajectory.interpolatedQuadrants.get(indexGrid).get(i);
				while(quadrantCoordenada.equals(quadrantCuadricula)) {
					coord.indexQuadrant.put(indexGrid, i);
					indexCoordenada++;
					if(indexCoordenada >= this.trajectory.coordinatesInterpolated.size()) {
						break;
					}
					coord = this.trajectory.coordinatesInterpolated.get(indexCoordenada);
					quadrantCoordenada = grid.getGridPosition(coord);
				}
			}
		}
		
		//Anonymize
		this.trajectory.coordinatesAnom = new ArrayList<Coordinate2>();
		coordenadasForCentroid = new ArrayList<Coordinate2>();
		for(int i=0; i<this.trajectory.coordinatesInterpolated.size(); i++) {
			coordenadaMatching = this.trajectory.coordinatesInterpolated.get(i);
			coordenadasForCentroid.clear();
			coordenadasForCentroid.add(coordenadaMatching);
			for(int index_grid:gridsWithMatching) {
				indexQuadrant = coordenadaMatching.indexQuadrant.get(index_grid);
				for(Peer peerMatched:this.gridMatchings.get(index_grid)) {
					quadrant = peerMatched.trajectory.interpolatedQuadrants.get(index_grid).get(indexQuadrant);
					coordenadasForCentroid.addAll(quadrant.coordinates);
				}
			}
			coordenadaAnom = Coordinate2.calculateCentroid(coordenadasForCentroid);
			this.trajectory.coordinatesAnom.add(coordenadaAnom);
		}
		
		//Comunicate to the matched peers the resulting anonymization (to can calculate the error later)
		//This is not necessary in a real scenario
		for(int index_grid:gridsWithMatching) {
			for(Peer peerMatched:this.gridMatchings.get(index_grid)) {
				peerMatched.trajectory.coordinatesAnom = new ArrayList<Coordinate2>();
				peerMatched.trajectory.coordinatesAnom.addAll(this.trajectory.coordinatesAnom);
			}
		}
		
		//free up memory
		for(int index_grid:gridsWithMatching) {
			for(GridCoordinate quad:this.trajectory.interpolatedQuadrants.get(index_grid)) {
				quad.coordinates.clear();
			}
		}
		for(Coordinate2 coordenada:this.trajectory.coordinatesInterpolated) {
			coordenada.indexQuadrant.clear();
		}
		this.trajectory.coordinatesInterpolated.clear();
		for(int index_grid:gridsWithMatching) {
			for(Peer peerMatched:this.gridMatchings.get(index_grid)) {
				for(GridCoordinate quad:peerMatched.trajectory.interpolatedQuadrants.get(index_grid)) {
					quad.coordinates.clear();
				}
				peerMatched.trajectory.coordinatesInterpolated.clear();
			}
		}
	}
	
	public void anonymizeTrajectory1InterpolatedAggregating(ArrayList<GridGenerator2>grids) {
		//Con precalculo de puntos por cuadrante
		//se anonimiza: punto -> cuadrante; punto + puntos (envia toda la trayectoria de una vez)
		ArrayList<Peer>peersInCluster;
		Peer centroid;
		Coordinate2 coordenada;
		
		peersInCluster = new ArrayList<Peer>();
		peersInCluster.add(this);
		for(int indexGrid=0; indexGrid<gridMatchings.size(); indexGrid++) {
			for(Peer peerMatched:this.gridMatchings.get(indexGrid)) {
				peersInCluster.add(peerMatched);
			}
		}
		
//		for(Peer peer:peersInCluster) {
//			peer.trayecto.calculaCoordenadasInterpoladas();
//		}
		
		//Anonymize
		centroid = Peer.calculateCentroidAggregatingCoordinates(peersInCluster);
		for(Peer peer:peersInCluster) {
			peer.trajectory.coordinatesAnom = new ArrayList<Coordinate2>();
			peer.trajectory.velocityAnom = centroid.trajectory.velocityAnom;
			for(int i=0; i<centroid.trajectory.coordinate.size(); i++) {
				coordenada = centroid.trajectory.coordinate.get(i);
				peer.trajectory.coordinatesAnom.add(coordenada);
			}
		}
		
//		//Free up memory
//		for(Peer peer:peersInCluster) {
//			peer.trayecto.coordenadasInterpoladas.clear();
//		}
		
	}
	
	public void anonymizeTrajectory2NotInterpolated(ArrayList<GridGenerator2>grids) {
		//Con precalculo de puntos por cuadrante
		//se anonimiza: punto -> cuadrante; punto + centroid quadrante (envia centroid de cada quadrante)
		Trajectory trayectoPeerMatched;
		GridGenerator2 grid;
		Coordinate2 coordenadaMatching, coordenadaAnom;
		ArrayList<Coordinate2>coordenadasInQuadrant;
		ArrayList<Coordinate2>coordenadasForCentroid;
		ArrayList<Integer>gridsWithMatching;
		GridCoordinate quadrantCoordenada, quadrantCuadricula, quadrant;
		Coordinate2 coord;
		int indexCoordenada, indexQuadrant;

		gridsWithMatching = new ArrayList<Integer>();
		for(int indexGrid=0; indexGrid<gridMatchings.size(); indexGrid++) {
			grid = grids.get(indexGrid);
			if(this.gridMatchings.get(indexGrid).size() > 0) {
				gridsWithMatching.add(indexGrid);
			}
			else {	//In this grid there are not matchings
				continue;
			}
			//For each peer matching, put each coordinate in its quadrant of this grid
			for(Peer peerMatched:this.gridMatchings.get(indexGrid)) {
				trayectoPeerMatched = peerMatched.getTrayecto();
				indexCoordenada = 0;
				coord = trayectoPeerMatched.coordinate.get(indexCoordenada);
				quadrantCoordenada = grid.getGridPosition(coord);
				for(int i=0; i<trayectoPeerMatched.quadrants.get(indexGrid).size(); i++) {
					quadrantCuadricula = trayectoPeerMatched.quadrants.get(indexGrid).get(i);
					coordenadasInQuadrant = new ArrayList<Coordinate2>();
					while(quadrantCoordenada.equals(quadrantCuadricula)) {
						coordenadasInQuadrant.add(coord);
						indexCoordenada++;
						if(indexCoordenada >= trayectoPeerMatched.coordinate.size()) {
							break;
						}
						coord = trayectoPeerMatched.coordinate.get(indexCoordenada);
						quadrantCoordenada = grid.getGridPosition(coord);
					}
					quadrantCuadricula.coordinates.addAll(coordenadasInQuadrant);
					quadrantCuadricula.calculateCentroid();
				}
			}
			//For this peer, determine the quadrant of this grid where each coordinate falls
			indexCoordenada = 0;
			coord = this.trajectory.coordinate.get(indexCoordenada);
			quadrantCoordenada = grid.getGridPosition(coord);
			for(int i=0; i<this.trajectory.quadrants.get(indexGrid).size(); i++) {
				quadrantCuadricula = this.trajectory.quadrants.get(indexGrid).get(i);
				while(quadrantCoordenada.equals(quadrantCuadricula)) {
					coord.indexQuadrant.put(indexGrid, i);
					indexCoordenada++;
					if(indexCoordenada >= this.trajectory.coordinate.size()) {
						break;
					}
					coord = this.trajectory.coordinate.get(indexCoordenada);
					quadrantCoordenada = grid.getGridPosition(coord);
				}
			}
		}
		
		//Anonymize
		this.trajectory.coordinatesAnom = new ArrayList<Coordinate2>();
		coordenadasForCentroid = new ArrayList<Coordinate2>();
		for(int i=0; i<this.trajectory.coordinate.size(); i++) {
			coordenadaMatching = this.trajectory.coordinate.get(i);
			coordenadasForCentroid.clear();
			coordenadasForCentroid.add(coordenadaMatching);
			for(int index_grid:gridsWithMatching) {
				indexQuadrant = coordenadaMatching.indexQuadrant.get(index_grid);
				for(Peer peerMatched:this.gridMatchings.get(index_grid)) {
					quadrant = peerMatched.trajectory.quadrants.get(index_grid).get(indexQuadrant);
					coordenadasForCentroid.add(quadrant.centroid);
				}
			}
			coordenadaAnom = Coordinate2.calculateCentroid(coordenadasForCentroid);
			this.trajectory.coordinatesAnom.add(coordenadaAnom);
		}
		
		//Comunicate to the matched peers the resulting anonymization (to can calculate the error later)
		//This is not necessary in a real scenario
		for(int index_grid:gridsWithMatching) {
			for(Peer peerMatched:this.gridMatchings.get(index_grid)) {
				peerMatched.trajectory.coordinatesAnom = new ArrayList<Coordinate2>();
				peerMatched.trajectory.coordinatesAnom.addAll(this.trajectory.coordinatesAnom);
			}
		}
		
		//free up memory
		this.trajectory.coordinatesInterpolated.clear();
		for(int index_grid:gridsWithMatching) {
			for(GridCoordinate quad:this.trajectory.quadrants.get(index_grid)) {
				quad.coordinates.clear();
			}
		}
		for(Coordinate2 coordenada:this.trajectory.coordinate) {
			coordenada.indexQuadrant.clear();
		}
		for(int index_grid:gridsWithMatching) {
			for(Peer peerMatched:this.gridMatchings.get(index_grid)) {
				for(GridCoordinate quad:peerMatched.trajectory.quadrants.get(index_grid)) {
					quad.coordinates.clear();
				}
			}
		}
	}
	
	public void anonymizeTrajectory2NotInterpolatedAggregating(ArrayList<GridGenerator2>grids) {
		//Con precalculo de puntos por cuadrante
		//se anonimiza: punto -> cuadrante; punto + centroid quadrante (envia centroid de cada quadrante)
		ArrayList<Peer>peersInCluster;
		Peer centroid;
		Coordinate2 coordenada;
		Trajectory trayectoPeerMatched;
		GridGenerator2 grid;
		ArrayList<Coordinate2>coordenadasInQuadrant;
		GridCoordinate quadrantCoordenada, quadrantCuadricula;
		Coordinate2 coord;
		int indexCoordenada;

		for(int indexGrid=0; indexGrid<gridMatchings.size(); indexGrid++) {
			grid = grids.get(indexGrid);
			//For each peer matching, put each coordinate in its quadrant of this grid
			//Calculate the quadrant-grid centroid and save in centroidsCuadriculas (this will be the new trajectory)
			//Then add peer to cluster 
			for(Peer peerMatched:this.gridMatchings.get(indexGrid)) {
				peerMatched.trajectory.centroidsQuadrants = new ArrayList<Coordinate2>();
				trayectoPeerMatched = peerMatched.getTrayecto();
				indexCoordenada = 0;
				coord = trayectoPeerMatched.coordinate.get(indexCoordenada);
				quadrantCoordenada = grid.getGridPosition(coord);
				for(int i=0; i<trayectoPeerMatched.quadrants.get(indexGrid).size(); i++) {
					quadrantCuadricula = trayectoPeerMatched.quadrants.get(indexGrid).get(i);
					coordenadasInQuadrant = new ArrayList<Coordinate2>();
					while(quadrantCoordenada.equals(quadrantCuadricula)) {
						coordenadasInQuadrant.add(coord);
						indexCoordenada++;
						if(indexCoordenada >= trayectoPeerMatched.coordinate.size()) {
							break;
						}
						coord = trayectoPeerMatched.coordinate.get(indexCoordenada);
						quadrantCoordenada = grid.getGridPosition(coord);
					}
					quadrantCuadricula.coordinates.addAll(coordenadasInQuadrant);
					quadrantCuadricula.calculateCentroid();
					peerMatched.trajectory.centroidsQuadrants.add(quadrantCuadricula.centroid);
				}
			}
		}
		
		//this peer uses his trajectory to aggregate
		this.trajectory.centroidsQuadrants = new ArrayList<Coordinate2>();
		for(Coordinate2 c:this.trajectory.coordinate) {
			this.trajectory.centroidsQuadrants.add(c);
		}
		
		//Create the cluster
		peersInCluster = new ArrayList<Peer>();
		peersInCluster.add(this);
		for(int indexGrid=0; indexGrid<gridMatchings.size(); indexGrid++) {
			for(Peer peerMatched:this.gridMatchings.get(indexGrid)) {
				peersInCluster.add(peerMatched);
			}
		}
		
		//Anonymize
		centroid = Peer.calculateCentroidAggregatingCentroids(peersInCluster);
		for(Peer peer:peersInCluster) {
			peer.trajectory.coordinatesAnom = new ArrayList<Coordinate2>();
			peer.trajectory.velocityAnom = centroid.trajectory.velocityAnom;
			for(int i=0; i<centroid.trajectory.coordinate.size(); i++) {
				coordenada = centroid.trajectory.coordinate.get(i);
				peer.trajectory.coordinatesAnom.add(coordenada);
			}
		}
		
		//Free up memory
		for(Peer peer:peersInCluster) {
			peer.trajectory.centroidsQuadrants.clear();
		}
		
		
	}
	
	public void anonymizeTrajectory2Interpolated(ArrayList<GridGenerator2>grids) {
		//Con precalculo de puntos por cuadrante
		//se anonimiza: punto -> cuadrante; punto + centroid quadrante (envia centroid de cada quadrante)
		Trajectory trayectoPeerMatched;
		GridGenerator2 grid;
		Coordinate2 coordenadaMatching, coordenadaAnom;
		ArrayList<Coordinate2>coordenadasInQuadrant;
		ArrayList<Coordinate2>coordenadasForCentroid;
		ArrayList<Integer>gridsWithMatching;
		GridCoordinate quadrantCoordenada, quadrantCuadricula, quadrant;
		Coordinate2 coord;
		int indexCoordenada, indexQuadrant;

		this.trajectory.calculateCoordinatesInterpolated();
		gridsWithMatching = new ArrayList<Integer>();
		for(int indexGrid=0; indexGrid<gridMatchings.size(); indexGrid++) {
			grid = grids.get(indexGrid);
			if(this.gridMatchings.get(indexGrid).size() > 0) {
				gridsWithMatching.add(indexGrid);
			}
			else {	//In this grid there are not matchings
				continue;
			}
			//For each peer matching, put each coordinate in its quadrant of this grid
			for(Peer peerMatched:this.gridMatchings.get(indexGrid)) {
				trayectoPeerMatched = peerMatched.getTrayecto();
				trayectoPeerMatched.calculateCoordinatesInterpolated();
				indexCoordenada = 0;
				coord = trayectoPeerMatched.coordinatesInterpolated.get(indexCoordenada);
				quadrantCoordenada = grid.getGridPosition(coord);
				for(int i=0; i<trayectoPeerMatched.interpolatedQuadrants.get(indexGrid).size(); i++) {
					quadrantCuadricula = trayectoPeerMatched.interpolatedQuadrants.get(indexGrid).get(i);
					coordenadasInQuadrant = new ArrayList<Coordinate2>();
					while(quadrantCoordenada.equals(quadrantCuadricula)) {
						coordenadasInQuadrant.add(coord);
						indexCoordenada++;
						if(indexCoordenada >= trayectoPeerMatched.coordinatesInterpolated.size()) {
							break;
						}
						coord = trayectoPeerMatched.coordinatesInterpolated.get(indexCoordenada);
						quadrantCoordenada = grid.getGridPosition(coord);
					}
					quadrantCuadricula.coordinates.addAll(coordenadasInQuadrant);
					quadrantCuadricula.calculateCentroid();
				}
			}
			//For this peer, determine the quadrant of this grid where each coordinate falls
			indexCoordenada = 0;
			coord = this.trajectory.coordinatesInterpolated.get(indexCoordenada);
			quadrantCoordenada = grid.getGridPosition(coord);
			for(int i=0; i<this.trajectory.interpolatedQuadrants.get(indexGrid).size(); i++) {
				quadrantCuadricula = this.trajectory.interpolatedQuadrants.get(indexGrid).get(i);
				while(quadrantCoordenada.equals(quadrantCuadricula)) {
					coord.indexQuadrant.put(indexGrid, i);
					indexCoordenada++;
					if(indexCoordenada >= this.trajectory.coordinatesInterpolated.size()) {
						break;
					}
					coord = this.trajectory.coordinatesInterpolated.get(indexCoordenada);
					quadrantCoordenada = grid.getGridPosition(coord);
				}
			}
		}
		
		//Anonymize
		this.trajectory.coordinatesAnom = new ArrayList<Coordinate2>();
		coordenadasForCentroid = new ArrayList<Coordinate2>();
		for(int i=0; i<this.trajectory.coordinatesInterpolated.size(); i++) {
			coordenadaMatching = this.trajectory.coordinatesInterpolated.get(i);
			coordenadasForCentroid.clear();
			coordenadasForCentroid.add(coordenadaMatching);
			for(int index_grid:gridsWithMatching) {
				indexQuadrant = coordenadaMatching.indexQuadrant.get(index_grid);
				for(Peer peerMatched:this.gridMatchings.get(index_grid)) {
					quadrant = peerMatched.trajectory.interpolatedQuadrants.get(index_grid).get(indexQuadrant);
					coordenadasForCentroid.add(quadrant.centroid);
				}
			}
			coordenadaAnom = Coordinate2.calculateCentroid(coordenadasForCentroid);
			this.trajectory.coordinatesAnom.add(coordenadaAnom);
		}
		
		//Comunicate to the matched peers the resulting anonymization (to can calculate the error later)
		//This is not necessary in a real scenario
		for(int index_grid:gridsWithMatching) {
			for(Peer peerMatched:this.gridMatchings.get(index_grid)) {
				peerMatched.trajectory.coordinatesAnom = new ArrayList<Coordinate2>();
				peerMatched.trajectory.coordinatesAnom.addAll(this.trajectory.coordinatesAnom);
			}
		}
		
		//free up memory
		this.trajectory.coordinatesInterpolated.clear();
		for(int index_grid:gridsWithMatching) {
			for(GridCoordinate quad:this.trajectory.interpolatedQuadrants.get(index_grid)) {
				quad.coordinates.clear();
			}
		}
		for(Coordinate2 coordenada:this.trajectory.coordinatesInterpolated) {
			coordenada.indexQuadrant.clear();
		}
		for(int index_grid:gridsWithMatching) {
			for(Peer peerMatched:this.gridMatchings.get(index_grid)) {
				for(GridCoordinate quad:peerMatched.trajectory.interpolatedQuadrants.get(index_grid)) {
					quad.coordinates.clear();
				}
			}
		}
	}
	
	public void anonymizeTrajectory2InterpolatedAggregating(ArrayList<GridGenerator2>grids) {
		//Con precalculo de puntos por cuadrante
		//se anonimiza: punto -> cuadrante; punto + centroid quadrante (envia centroid de cada quadrante)
		ArrayList<Peer>peersInCluster;
		Peer centroid;
		Coordinate2 coordenada;
		Trajectory trayectoPeerMatched;
		GridGenerator2 grid;
		ArrayList<Coordinate2>coordenadasInQuadrant;
		GridCoordinate quadrantCoordenada, quadrantCuadricula;
		Coordinate2 coord;
		int indexCoordenada;

		this.trajectory.calculateCoordinatesInterpolated();
		for(int indexGrid=0; indexGrid<gridMatchings.size(); indexGrid++) {
			grid = grids.get(indexGrid);
			//For each peer matching, put each coordinate in its quadrant of this grid
			//Calculate the quadrant-grid centroid and save in centroidsCuadriculas (this will be the new trajectory)
			//Then add peer to cluster 
			for(Peer peerMatched:this.gridMatchings.get(indexGrid)) {
				peerMatched.trajectory.centroidsQuadrants = new ArrayList<Coordinate2>();
				trayectoPeerMatched = peerMatched.getTrayecto();
				trayectoPeerMatched.calculateCoordinatesInterpolated();
				indexCoordenada = 0;
				coord = trayectoPeerMatched.coordinatesInterpolated.get(indexCoordenada);
				quadrantCoordenada = grid.getGridPosition(coord);
				for(int i=0; i<trayectoPeerMatched.interpolatedQuadrants.get(indexGrid).size(); i++) {
					quadrantCuadricula = trayectoPeerMatched.interpolatedQuadrants.get(indexGrid).get(i);
					coordenadasInQuadrant = new ArrayList<Coordinate2>();
					while(quadrantCoordenada.equals(quadrantCuadricula)) {
						coordenadasInQuadrant.add(coord);
						indexCoordenada++;
						if(indexCoordenada >= trayectoPeerMatched.coordinatesInterpolated.size()) {
							break;
						}
						coord = trayectoPeerMatched.coordinatesInterpolated.get(indexCoordenada);
						quadrantCoordenada = grid.getGridPosition(coord);
					}
					quadrantCuadricula.coordinates.addAll(coordenadasInQuadrant);
					quadrantCuadricula.calculateCentroid();
					peerMatched.trajectory.centroidsQuadrants.add(quadrantCuadricula.centroid);
				}
			}
		}
		
		//this peer uses his trajectory to aggregate
		this.trajectory.centroidsQuadrants = new ArrayList<Coordinate2>();
		for(Coordinate2 c:this.trajectory.coordinatesInterpolated) {
			this.trajectory.centroidsQuadrants.add(c);
		}
		
		//Create the cluster
		peersInCluster = new ArrayList<Peer>();
		peersInCluster.add(this);
		for(int indexGrid=0; indexGrid<gridMatchings.size(); indexGrid++) {
			for(Peer peerMatched:this.gridMatchings.get(indexGrid)) {
				peersInCluster.add(peerMatched);
			}
		}
		
		//Anonymize
		centroid = Peer.calculateCentroidAggregatingCentroids(peersInCluster);
		for(Peer peer:peersInCluster) {
			peer.trajectory.coordinatesAnom = new ArrayList<Coordinate2>();
			peer.trajectory.velocityAnom = centroid.trajectory.velocityAnom;
			for(int i=0; i<centroid.trajectory.coordinate.size(); i++) {
				coordenada = centroid.trajectory.coordinate.get(i);
				peer.trajectory.coordinatesAnom.add(coordenada);
			}
		}
		
		//Free up memory
		for(Peer peer:peersInCluster) {
			peer.trajectory.coordinatesInterpolated.clear();
		}
		
	}
	
	public void anonymizeTrajectory1Anterior(ArrayList<GridGenerator2>grids, boolean interpolado) {
		//Con precalculo de puntos por cuadrante
		//se anonimiza: punto -> cuadrante; punto + puntos (envia toda la trayectoria de una vez)
		Trajectory trayectoPeerMatched;
		GridGenerator2 grid;
		Coordinate2 coordenadaMatching, coordenadaMatched, coordenadaAnom;
		ArrayList<Coordinate2>coordenadasInQuadrant;
		ArrayList<Coordinate2>coordenadasForCentroid;
		ArrayList<Coordinate2>coordenadasSource;
		ArrayList<Integer>gridsWithMatching;
		ArrayList<HashMap<GridCoordinate, ArrayList<Coordinate2>>>coordenadasPorCuadrante;
		GridCoordinate quadrantMatching, quadrantMatched;

		coordenadasPorCuadrante = new ArrayList<HashMap<GridCoordinate, ArrayList<Coordinate2>>>();
		gridsWithMatching = new ArrayList<Integer>();
		for(int indexGrid=0; indexGrid<gridMatchings.size(); indexGrid++) {
			grid = grids.get(indexGrid);
			if(this.gridMatchings.get(indexGrid).size() > 0) {
				gridsWithMatching.add(indexGrid);
			}
			coordenadasPorCuadrante.add(new HashMap<GridCoordinate, ArrayList<Coordinate2>>());
			for(Peer peerMatched:this.gridMatchings.get(indexGrid)) {
				trayectoPeerMatched = peerMatched.getTrayecto();
				if(interpolado) {
					trayectoPeerMatched.calculateCoordinatesInterpolated();
					coordenadasSource = trayectoPeerMatched.coordinatesInterpolated;
				}
				else {
					coordenadasSource = trayectoPeerMatched.coordinate;
				}
				for(int i=0; i<coordenadasSource.size(); i++) {
					coordenadaMatched = coordenadasSource.get(i);
					quadrantMatched = grid.getGridPosition(coordenadaMatched);
					coordenadasInQuadrant = coordenadasPorCuadrante.get(indexGrid).get(quadrantMatched);
					if(coordenadasInQuadrant == null) {
						coordenadasPorCuadrante.get(indexGrid).put(quadrantMatched, new ArrayList<Coordinate2>());
					}
					coordenadasPorCuadrante.get(indexGrid).get(quadrantMatched).add(coordenadaMatched);
				}
				trayectoPeerMatched.coordinatesInterpolated.clear();	//free up memory
			}
		}
		
		this.trajectory.coordinatesAnom = new ArrayList<Coordinate2>();
		coordenadasForCentroid = new ArrayList<Coordinate2>();
		coordenadasInQuadrant = new ArrayList<Coordinate2>();
		if(interpolado) {
			this.trajectory.calculateCoordinatesInterpolated();
			coordenadasSource = this.trajectory.coordinatesInterpolated;
		}
		else {
			coordenadasSource = this.trajectory.coordinate;
		}
		for(int i=0; i<coordenadasSource.size(); i++) {
			coordenadaMatching = coordenadasSource.get(i);
			coordenadasForCentroid.clear();
			coordenadasForCentroid.add(coordenadaMatching);
			for(int index_grid:gridsWithMatching) {
				grid = grids.get(index_grid);
				quadrantMatching = grid.getGridPosition(coordenadaMatching);
				coordenadasInQuadrant = coordenadasPorCuadrante.get(index_grid).get(quadrantMatching);
				coordenadasForCentroid.addAll(coordenadasInQuadrant);
			}
			coordenadaAnom = Coordinate2.calculateCentroid(coordenadasForCentroid);
			this.trajectory.coordinatesAnom.add(coordenadaAnom);
		}
		this.trajectory.coordinatesInterpolated.clear();	//free up memory
	}
	
	public double calculateMatchedDistance1(Peer other, GridGenerator2 grid, boolean interpolado) {
		//punto + puntos
		//the trajectories has to have the same generalized trajectory (the trajectories match)
		//Used for take the best trajectory among the matching
		Double RMSE;
		Double dist, partial;
		ArrayList<Coordinate2>coordenadasSource;
		ArrayList<Coordinate2>coordenadasInQuadrant;
		HashMap<GridCoordinate, ArrayList<Coordinate2>>coordenadasPorCuadrante;
		GridCoordinate quadrant;
		ArrayList<Coordinate2>coordenadasForCentroid;
		Coordinate2 coordenada, coordenadaAnom, coordenadaOri;
		String key;
		
		coordenadasPorCuadrante = new HashMap<GridCoordinate, ArrayList<Coordinate2>>();
		if(interpolado) {
			this.trajectory.calculateCoordinatesInterpolated();
			other.trajectory.calculateCoordinatesInterpolated();
			coordenadasSource = other.trajectory.coordinatesInterpolated;
		}
		else {
			coordenadasSource = other.trajectory.coordinate;
		}
		for(Coordinate2 coordenadaOther:coordenadasSource) {
			quadrant = grid.getGridPosition(coordenadaOther);
			coordenadasInQuadrant = coordenadasPorCuadrante.get(quadrant);
			if(coordenadasInQuadrant == null) {
				coordenadasPorCuadrante.put(quadrant, new ArrayList<Coordinate2>());
			}
			coordenadasPorCuadrante.get(quadrant).add(coordenadaOther);
		}
		
		//The quadrants are the same for both trajectories
		this.trajectory.coordinatesAnom = new ArrayList<Coordinate2>();
		coordenadasForCentroid = new ArrayList<Coordinate2>();
		coordenadasInQuadrant = new ArrayList<Coordinate2>();
		if(interpolado) {
			this.trajectory.calculateCoordinatesInterpolated();
			coordenadasSource = this.trajectory.coordinatesInterpolated;
		}
		else {
			coordenadasSource = this.trajectory.coordinate;
		}
		for(int i=0; i<coordenadasSource.size(); i++) {
			coordenada = coordenadasSource.get(i);
			quadrant = grid.getGridPosition(coordenada);
			coordenadasForCentroid.clear();
			coordenadasForCentroid.add(coordenada);
			coordenadasInQuadrant = coordenadasPorCuadrante.get(quadrant);
			coordenadasForCentroid.addAll(coordenadasInQuadrant);
			coordenadaAnom = Coordinate2.calculateCentroid(coordenadasForCentroid);
			this.trajectory.coordinatesAnom.add(coordenadaAnom);
		}
		
		partial = 0.0;
		for(int i=0; i<coordenadasSource.size(); i++) {
			coordenadaOri = coordenadasSource.get(i);
			coordenadaAnom = this.trajectory.coordinatesAnom.get(i);
			dist = coordenadaOri.distanceFast(coordenadaAnom);
			partial += (dist*dist);
		}
		partial /= coordenadasSource.size();
		RMSE = Math.sqrt(partial);
		this.trajectory.coordinatesInterpolated.clear();
		other.trajectory.coordinatesInterpolated.clear();
		this.trajectory.coordinatesAnom.clear();
		
		return RMSE;
	}
	
	public double calculateMatchedDistance2(Peer other, GridGenerator2 grid, boolean interpolado) {
		//centroid + centroid (faster)
		//the trajectories has to have the same generalized trajectory (the trajectories match)
		//Used for take the best trajectory among the matching
		Double RMSE;
		Double dist, partial;
		ArrayList<Coordinate2>coordenadasSource;
		ArrayList<Coordinate2>coordenadasInQuadrant;
		HashMap<GridCoordinate, ArrayList<Coordinate2>>coordenadasPorCuadranteThis;
		HashMap<GridCoordinate, ArrayList<Coordinate2>>coordenadasPorCuadranteOther;
		HashMap<GridCoordinate, Coordinate2>centroidePorCuadranteThis;
		HashMap<GridCoordinate, Coordinate2>centroidePorCuadranteOther;
		GridCoordinate quadrant;
		ArrayList<Coordinate2>coordenadasForCentroid;
		Coordinate2 centroid, centroidThis, centroidOther;
		
		coordenadasPorCuadranteThis = new HashMap<GridCoordinate, ArrayList<Coordinate2>>();
		if(interpolado) {
			this.trajectory.calculateCoordinatesInterpolated();
			coordenadasSource = this.trajectory.coordinatesInterpolated;
		}
		else {
			coordenadasSource = this.trajectory.coordinate;
		}
		for(Coordinate2 coordenadaThis:coordenadasSource) {
			quadrant = grid.getGridPosition(coordenadaThis);
			coordenadasInQuadrant = coordenadasPorCuadranteThis.get(quadrant);
			if(coordenadasInQuadrant == null) {
				coordenadasPorCuadranteThis.put(quadrant, new ArrayList<Coordinate2>());
			}
			coordenadasPorCuadranteThis.get(quadrant).add(coordenadaThis);
		}
		centroidePorCuadranteThis = new HashMap<GridCoordinate, Coordinate2>();
		for(GridCoordinate quad:coordenadasPorCuadranteThis.keySet()) {
			coordenadasForCentroid = coordenadasPorCuadranteThis.get(quad);
			centroid = Coordinate2.calculateCentroid(coordenadasForCentroid);
			centroidePorCuadranteThis.put(quad, centroid);
		}
		
		coordenadasPorCuadranteOther = new HashMap<GridCoordinate, ArrayList<Coordinate2>>();
		if(interpolado) {
			other.trajectory.calculateCoordinatesInterpolated();
			coordenadasSource = other.trajectory.coordinatesInterpolated;
		}
		else {
			coordenadasSource = other.trajectory.coordinate;
		}
		for(Coordinate2 coordenadaOther:coordenadasSource) {
			quadrant = grid.getGridPosition(coordenadaOther);
			coordenadasInQuadrant = coordenadasPorCuadranteOther.get(quadrant);
			if(coordenadasInQuadrant == null) {
				coordenadasPorCuadranteOther.put(quadrant, new ArrayList<Coordinate2>());
			}
			coordenadasPorCuadranteOther.get(quadrant).add(coordenadaOther);
		}
		centroidePorCuadranteOther = new HashMap<GridCoordinate, Coordinate2>();
		for(GridCoordinate quad:coordenadasPorCuadranteOther.keySet()) {
			coordenadasForCentroid = coordenadasPorCuadranteOther.get(quad);
			centroid = Coordinate2.calculateCentroid(coordenadasForCentroid);
			centroidePorCuadranteOther.put(quad, centroid);
		}
		
		//The quadrants are the same for both trajectories
		partial = 0.0;
		for(GridCoordinate quad:centroidePorCuadranteOther.keySet()) {
			centroidThis = centroidePorCuadranteThis.get(quad);
			centroidOther = centroidePorCuadranteOther.get(quad);
			dist = centroidThis.distanceFast(centroidOther);
			partial += (dist*dist);
		}
		partial /= centroidePorCuadranteOther.size();
		RMSE = Math.sqrt(partial);
		this.trajectory.coordinatesInterpolated.clear();
		other.trajectory.coordinatesInterpolated.clear();
		
		return RMSE;
	}
	
	public static Peer calculateCentroidAggregatingCoordinates(ArrayList<Peer> peers) {
		Peer centroid;
		Trajectory trayectoCentroid;
		ArrayList<Trajectory>trayectos;
		
		trayectos = new ArrayList<Trajectory>();
		for(Peer peer:peers) {
			trayectos.add(peer.trajectory);
		}
		
//		trayectoCentroid = Trayecto.aggregateCoordinates(trayectos);
		trayectoCentroid = Trajectory.aggregateCoordinatesAnt(trayectos);
		
		centroid = new Peer(0, trayectoCentroid);
		
		return centroid;
	}
	
	public static Peer calculateCentroidAggregatingCentroids(ArrayList<Peer> peers) {
		Peer centroid;
		Trajectory trayectoCentroid;
		ArrayList<Trajectory>trayectos;
		
		trayectos = new ArrayList<Trajectory>();
		for(Peer peer:peers) {
			trayectos.add(peer.trajectory);
		}
		
		trayectoCentroid = Trajectory.aggregateCentroidsAnt(trayectos);
		centroid = new Peer(0, trayectoCentroid);
		
		return centroid;
	}
	
	public static Peer calculateCentroidMinimizingDistance(ArrayList<Peer> peers) {
		double minDist, dist;
		Peer minPeer = null;
		
		minDist = Double.MAX_VALUE;
		System.out.println("Calculating centroid minimizing distance...");
		Progress.createProgress(null, peers.size(), true);
		for(Peer peer1:peers) {
			dist = 0.0;
			for(Peer peer2:peers) {
//				dist += peer1.trayecto.normalizedTrajectoryDistance(peer2.trayecto);
				dist += peer1.trajectory.distanceAnt(peer2.trajectory);
			}
			if(dist < minDist) {
				minDist = dist;
				minPeer = peer1;
			}
			Progress.update();
		}
		
		System.out.println("Pre-calculating centroid distances...");
		Functions.distsFar = new HashMap<Peer, Double>();
		for(Peer peer:peers) {
			dist = peer.trajectory.distanceAnt(minPeer.trajectory);
			Functions.distsFar.put(peer, dist);
		}
		
		return minPeer;
	}
	
	public static Peer calculaCentroidClosestToCenterPoint(ArrayList<Peer> peers){
		Calendar calendar = Calendar.getInstance();
		long time;
		Timestamp timestamp;
		Coordinate2 centroidCoordinate;
		double lat, lon;
		double dist, minDist;
		Peer minPeer = null;
		
		lat = (GridGenerator2.topLeftCoordinate.latitude + GridGenerator2.botRightCoordinate.latitude) / 2;
		lon = (GridGenerator2.topLeftCoordinate.longitude + GridGenerator2.botRightCoordinate.longitude) / 2;
		time = peers.get(0).trajectory.getTiempoInicio();
		calendar.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
		timestamp = new Timestamp(time*1000);
		calendar.setTimeInMillis(timestamp.getTime());
		calendar.set(Calendar.DAY_OF_MONTH, 4);
		calendar.set(Calendar.MONTH, 1);
		calendar.set(Calendar.HOUR_OF_DAY, 12);
		time = calendar.getTimeInMillis() / 1000;
		
		centroidCoordinate = new Coordinate2(lat, lon, time);
		
		minDist = Double.MAX_VALUE;
		for(Peer peer:peers) {
			dist = 0;
			for(Coordinate2 coordenada:peer.trajectory.coordinate) {
				dist += coordenada.distanceWithTimeNormalized(centroidCoordinate);
			}
			if(dist < minDist) {
				minDist = dist;
				minPeer = peer;
			}
		}
		
		return minPeer;
		
	}
	
	public Trajectory getTrayecto() {
		return this.trajectory;
	}
	
	public boolean gridMatchingContains(Peer peer) {
		if(controlPeersMatched.contains(peer)) {
			return true;
		}
		return false;
	}
	
	public void addGridMatching(Integer grid, Peer peer) {
		this.gridMatchings.get(grid).add(peer);
		this.currentK++;
		this.controlPeersMatched.add(peer);
		peer.assigned = true;
		this.assigned = true;
	}
	
	public boolean send_tit_for_tat(String message, Peer pJota, boolean firstHop) {
		double pForward;
		Peer pPrima;
		boolean receipt;
		
		if(firstHop) {
			pForward = 1.0;
		}
		else {
			pForward = Peer.p;
		}
		if(bernoulli(pForward) == 1) {
			pPrima = getRandomPeer();
			while(this.blackList.contains(pPrima.id) || this.equals(pPrima) || pPrima.equals(pJota)) {
				pPrima = getRandomPeer();
			}
			receipt = this.send(message, pJota, pPrima);
			if(receipt) {
				return true;
			}
			else {
				this.blackList.add(pPrima.id);
			}
		}
		else {
			receipt = this.send(message, pJota, pJota);
			if(receipt) {
				return true;
			}
			else {
				this.blackList.add(pJota.id);
			}
		}
		
		return false;
	}
	
	public boolean send(String message, Peer pJota, Peer pPrima) {
		boolean receipt;
		
		this.numMessages++;
		receipt = pPrima.receive(message, pJota);
		
		return receipt;
	}
	
	public boolean receive(String message, Peer pJota) {
		boolean receipt = false;
		
		if(!this.badPeer) {
			if(!this.equals(pJota)) {
				receipt = this.send_tit_for_tat(message, pJota, false);
				if(receipt) {
					this.numMessages++;
					return true;
				}
				else {
					return false;
				}
			}
			else {
				this.numMessages++;
				return sign(message);
			}
		}
		else {
			return false;
		}
		
	}
	
	private Peer getRandomPeer() {
		int randomPeerIndex;
		Peer randomPeer;
		
		randomPeerIndex = Peer.rnd.nextInt(Peer.peers.size());
		randomPeer = Peer.peers.get(randomPeerIndex);
		
		return randomPeer;
	}
	
	public boolean sign(String message) {
		return true;
	}
	
	public static int bernoulli(double p) {
		if(Math.random() < p) {
			return 1;
		}
		return 0;
	}

	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}

	@Override
	public boolean equals(Object other) {
		boolean result = false;
		Peer otherUser;
		
		//Quick checks
        if(other == null) return result;
        if(!(other instanceof Peer)) return result;
        if(other == this) return true;
		
        otherUser = (Peer)other;
        if(this.id == otherUser.id) {
        	return true;
        }
        return false;
	}

	@Override
	public String toString() {
		return String.valueOf(id);
	}
	
}