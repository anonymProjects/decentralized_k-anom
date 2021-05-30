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
		int timeMargin, dayWeek;
		boolean interpolated;
		boolean aggregating;
		long runTime;
		ArrayList<ArrayList<Result>>results = new ArrayList<ArrayList<Result>>();
		Result resultPartial;
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
		trajectories = Functions.filterTrajectoriesByNumCoordinates(trajectories, numMinCoordinates);
		trajectories = Functions.filterTrajectoriesByDistance(trajectories, minDistance);
		trajectories = Functions.filterTrajectoriesDayWeek(trajectories, dayWeek);
		Functions.calculateUnifiedTime(trajectories);
		Functions.calculateMeanNumTypicalDeviationCoordinatesByTrajectory(trajectories);
		peers = Functions.createListPeers(trajectories, 0);
		
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
			Collections.shuffle(peers);
			for(int k=5; k<=100; k+=5) {
				Functions.assignBadPeers(peers, percenBadPeers);
				runTime = Functions.anonymizeSendingTrajectory(peers, k, minGridSize, timeMargin, interpolated, aggregating, timeWaiting);
				resultPartial = Functions.computeStatistics(peers);
				resultPartial.item = k;
				resultPartial.runTime = runTime;
				results.get(i).add(resultPartial);
				System.out.println("Partial results (" + (i+1) + "):");
				Functions.printResultsPartial(results.get(i));
			}
		}
		System.out.println("\nTotal results (" + numRunnings + "):" );
		Functions.printResults(results);
		
	}	

}
