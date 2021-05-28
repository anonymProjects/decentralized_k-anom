package anomTrajectories;

import java.util.ArrayList;
import java.util.Collections;

public class Main2 {

	public static void main(String[] args) {
		ArrayList<Trajectory>trajectories;
		ArrayList<Peer>peers;
		int numMinCoordinates;
		double minDistance;
		int minGridSize;
		double p;
		int timeMargin, dayWeek;
		boolean interpolated;
		boolean aggregating;
		long runTime;
		ArrayList<ArrayList<Result>>results = new ArrayList<ArrayList<Result>>();
		Result resultPartial;
		Peer centroid;
		double percenBadPeers;
		int timeWaiting;
		int numRunnings;
		
		trajectories = Functions.loadTrajectories();
		Functions.calculateAcumulatedDistanceTrajectory(trajectories);
		Functions.calculateDistanceTrajectory(trajectories);
		Functions.calculateTimeTrajectory(trajectories);
		
		
		//Filter trajectories inside a zone of San Francisco
		Functions.filterCoordinatesInsideQuadrant(trajectories);
		
		numMinCoordinates = 4;
		minDistance = 500.0;
		dayWeek = 1;
//		distanciaMinima = 0.0;
		trajectories = Functions.filterTrajectoriesByNumCoordinates(trajectories, numMinCoordinates);
		trajectories = Functions.filterTrajectoriesByDistance(trajectories, minDistance);
//		trayectos = Funciones.filtraTrayectosDeLunesaViernes(trayectos);
		trajectories = Functions.filterTrajectoriesDayWeek(trajectories, dayWeek);
		Functions.calculateUnifiedTime(trajectories);
		Functions.calculateMeanNumTypicalDeviationCoordinatesByTrajectory(trajectories);
		peers = Functions.createListPeers(trajectories, 0);
//		Funciones.imprimeEstadisticaInicial(peers);
//		System.exit(0);
		
//		int k = 3;
//		interpolado = true;
//		minGridSize = 100;	//en metros
//		timeMargin = 60*60; // en segundos
////		Funciones.anonymiza2(peers,k, gridSize, p_overlap, interpolado);
////		Funciones.anonymiza3(peers,k, gridSize, timeMargin, interpolado);
////		Funciones.anonymiza4(peers,k, gridSize, p_overlap);
//		Funciones.anonymiza5(peers,k, minGridSize, timeMargin, interpolado);
//		Funciones.imprimeEstadisticasAnom2(peers, interpolado);
		
		numRunnings = 5;
		for(int i=0; i<numRunnings; i++) {
			results.add(new ArrayList<Result>());
			interpolated = false;
			aggregating = true;
			minGridSize = 100;	//meters
			Peer.p = 0.5;	//Bernoulli probability
			timeMargin = 60*60; // seconds
			percenBadPeers = 0.0;
			timeWaiting = 10;
			Functions.assignBadPeers(peers, percenBadPeers);
			Functions.generateGrids(minGridSize);
//			centroid = Peer.calculateCentroidMinimizingDistance(peers);
//			centroid = Peer.calculateCentroidAggregatingCoordinates(peers);
//			centroid = Peer.calculaCentroidClosestToCenterPoint(peers);
			Collections.shuffle(peers);
			int k = 10;
//			for(int k=5; k<=100; k+=5) {
//			for(double pBernoulli=0.0; pBernoulli<=0.9; pBernoulli+=0.1) {
			for(double badPeers=0.0; badPeers<=10.0; badPeers+=1.0) {
//				Peer.p = pBernoulli;	//Bernoulli probability
				percenBadPeers = badPeers;
				Functions.assignBadPeers(peers, percenBadPeers);
				runTime = Functions.anonymizeSendingTrajectory(peers, k, minGridSize, timeMargin, interpolated, aggregating, timeWaiting);
//				runTime = Funciones.anonymizeSendingCentroids(peers, k, minGridSize, timeMargin, interpolado, aggregating, timeWaiting);
//				runTime = Funciones.anonymizeViaMicroaggregation(peers, centroid, k);
				resultPartial = Functions.computeStatistics(peers);
//				resultPartial.item = k;
//				resultPartial.item = pBernoulli;
				resultPartial.item = badPeers;
				resultPartial.runTime = runTime;
				results.get(i).add(resultPartial);
				System.out.println("Partial results (" + (i+1) + "):");
				Functions.printResultsPartial(results.get(i));
			}
		}
		System.out.println("\nTotal results (" + numRunnings + "):" );
//		Funciones.printResults(results);
		Functions.printResultsBadPeers(results);
		
	}	

}
