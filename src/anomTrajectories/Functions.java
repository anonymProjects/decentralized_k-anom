package anomTrajectories;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.TimeZone;

public class Functions {
	static ArrayList<GridGenerator2> grids;
	static ArrayList<Topics>gridTopics = new ArrayList<Topics>();
	static double marginInTime;
	//Maybe more precise calculating the max distance between all coordinates in the dataset
	static double maxDist = Math.sqrt((19100*19100)+(23300*23300));	//diagonal of the grid
	static double minDist = 0.0;
	//Maybe more precise calculating the max time diference between all coordinates in the dataset
	static int maxTime = 24*60*60;	//24 hours in secs
	static int minTime = 0;
	static double meanVelocity;
	static double compensation;
	static HashMap<Peer, Double>distsFar;
	
	public static long anonymizeSendingTrajectory(ArrayList<Peer> peers, int k, int minGridSize, int timeMargin,
												  boolean interpolated, boolean aggregating, int timeWaiting) {
		//To accelerate, using hash with the quadrants as keys and matching of quagdrants in a step O(n)
		//Searches matchings reducing the granularity (bigger quadrant) until find k matchings
		//Topic is formed by quadrants where the trajectory traverses + (mean time / timeMargin)
		long iniTime, runTime;
		boolean sendingTrajectory = true;
		
		System.out.println("");
		System.out.println("Peers: " + peers.size());
		System.out.println("k: " + k);
		System.out.println("Min grid size: " + minGridSize);
		System.out.println("Time margin: " + timeMargin);
		
		iniTime = System.currentTimeMillis();
		assignMatchignsToPeers(peers, k, timeMargin, interpolated, sendingTrajectory, timeWaiting);
		
		//Calculates centroids and anonymizes trajectories
		Progress.createProgress(null, peers.size(), true);
		for(Peer peer:peers) {
			if(peer.anonymizator) {	//Only the peer who has the matchings
				peer.anonymizeTrajectory1(grids, interpolated, aggregating);	//sending trajectory
			}
			Progress.update();
			peer.initialize(grids);
		}
		
		runTime = System.currentTimeMillis() - iniTime;
		runTime /= 1000;
		System.out.println("Runtime (secs): " + runTime);
		
		return runTime;
	}
	
	public static long anonymizeSendingCentroids(ArrayList<Peer> peers, int k, int minGridSize, int timeMargin, 
												 boolean interpolated, boolean aggregating, int timeWaiting) {
		//To accelerate, using hash with the quadrants as keys and matching of quagdrants in a step O(n)
		//Searches matchings reducing the granularity (bigger quadrant) until find k matchings
		//Topic is formed by quadrants where the trajectory traverses + (mean time / timeMargin)
		long iniTime, runTime;
		boolean sendingTrajectory = false;
		
		System.out.println("");
		System.out.println("Peers: " + peers.size());
		System.out.println("k: " + k);
		System.out.println("Min grid size: " + minGridSize);
		System.out.println("Time margin: " + timeMargin);
		
		iniTime = System.currentTimeMillis();
		assignMatchignsToPeers(peers, k, timeMargin, interpolated, sendingTrajectory, timeWaiting);
		
		//Calculates centroids and anonymizes trajectories
		Progress.createProgress(null, peers.size(), true);
		for(Peer peer:peers) {
			if(peer.anonymizator) {	//Only the peer who has the matchings
				peer.anonymizeTrajectory2(grids, interpolated, aggregating);	//sending quadrants centroids
			}
			Progress.update();
			peer.initialize(grids);
		}
		
		runTime = System.currentTimeMillis() - iniTime;
		runTime /= 1000;
		System.out.println("Runtime (secs): " + runTime);
		
		return runTime;
	}
	
	public static void assignMatchignsToPeers(ArrayList<Peer> peers, int k, int timeMargin,
											  boolean interpolated, boolean sendingTrajectory, int timeWaiting) {
		ArrayList<Peer>topicSubscriptors;
		ArrayList<Peer>peersToAssign;
		String topic;
		int numPeersNotAssign;
		int newTimeMargin;
		Peer pMatching, pMatched;
		
		for(Peer peer:peers) {
			peer.numMessages = 0;
			peer.blackList.clear();
		}
		
		peersToAssign = new ArrayList<Peer>();
		newTimeMargin = timeMargin;
		numPeersNotAssign = peers.size();
		while(numPeersNotAssign > 2*k && newTimeMargin < maxTime) {
			peersToAssign.clear();
			for(Peer p:peers) {
				if(!p.assigned) {
					peersToAssign.add(p);
				}
			}
			generateTopicSubscriptions(peersToAssign, newTimeMargin, interpolated);
			for(Peer peerMatching:peersToAssign) {
				if(!peerMatching.assigned) {
					for(int gridIndex=0; gridIndex<grids.size(); gridIndex++) {
						if(interpolated) {
							topic = peerMatching.trajectory.topicsInterpolatedString.get(gridIndex);
						}
						else {
							topic = peerMatching.trajectory.topicsString.get(gridIndex);
						}
						topicSubscriptors = gridTopics.get(gridIndex).getTopicSubscriptors(topic);
						if(topicSubscriptors.size() > 1) {
							for(Peer peerMatched:topicSubscriptors){
								if(peerMatching.equals(peerMatched)) {
									continue;
								}
								if(peerMatching.currentK < k && !peerMatched.assigned) {
									if(sendingTrajectory) {
										simulateSendTrajectory(peers, peerMatching, peerMatched, gridIndex,
															   timeWaiting);
									}
									else {
										simulateSendCentroids(peers, peerMatching, peerMatched, gridIndex, interpolated,
															  timeWaiting);
									}
								}
								if(peerMatching.currentK >= k) {
									break;
								}
							}
							if(peerMatching.currentK >= k) {
								break;
							}
						}
					}
					if(peerMatching.currentK < k) {	//This peer has not got the k-1 peers
						for(Peer p:peerMatching.controlPeersMatched) {
							p.assigned = false;
						}
						peerMatching.initialize(grids);
					}
					else {	//This peer has got the k-1 peers
						peerMatching.anonymizator = true;
					}
				}
			}
			numPeersNotAssign = 0;
			for(Peer peer:peers) {
				if(!peer.assigned) {
					numPeersNotAssign++;
				}
			}
			System.out.println("Peers not assigned: " + numPeersNotAssign);
			newTimeMargin += timeMargin;
		}
		//Rest of peers joined
		peersToAssign.clear();
		for(Peer p:peers) {
			if(!p.assigned) {
				peersToAssign.add(p);
			}
		}
		System.out.println("Peers not assigned: " + peersToAssign.size());
		if(peersToAssign.size() > 0) {
			pMatching = peersToAssign.get(0);
			pMatching.anonymizator = true;
			for(int i=1; i<peersToAssign.size(); i++) {
				pMatched = peersToAssign.get(i);
				pMatching.addGridMatching(grids.size()-1, pMatched);
			}
		}
		
		numPeersNotAssign = 0;
		for(Peer peer:peers) {
			if(!peer.assigned) {
				numPeersNotAssign++;
			}
		}
		System.out.println("Peers not assigned: " + numPeersNotAssign);
		
	}

	private static void simulateSendTrajectory(ArrayList<Peer> peers, Peer peerMatching, Peer peerMatched, int gridIndex,
											   int timeWaiting) {
		boolean receipt;
		String message = "trajectory";
		int time;
		
		if(!peerMatched.blackList.contains(peerMatching.id)) {
			receipt = peerMatched.send_tit_for_tat(message, peerMatching, true);
			time = 0;
			while(!receipt && time < timeWaiting) {
				receipt = peerMatched.send_tit_for_tat(message, peerMatching, true);
				time++;
			}
			if(receipt) {
				peerMatching.addGridMatching(gridIndex, peerMatched);
			}
		}
		
	}
	
	private static void simulateSendCentroids(ArrayList<Peer> peers, Peer peerMatching, Peer peerMatched, 
											  int gridIndex, boolean interpolated, int timeWaiting) {
		boolean receipt = false;
		String message = "centroid";
		int time;
		
		if(!peerMatched.blackList.contains(peerMatching.id)) {
			if(interpolated) {
				for(int i=0; i<peerMatched.trajectory.interpolatedQuadrants.get(gridIndex).size(); i++) {
					receipt = peerMatched.send_tit_for_tat(message, peerMatching, true);
					time = 0;
					while(!receipt && time < timeWaiting) {
						receipt = peerMatched.send_tit_for_tat(message, peerMatching, true);
						time++;
					}
					if(!receipt) {
						break;
					}
				}
			}
			else {
				for(int i=0; i<peerMatched.trajectory.quadrants.get(gridIndex).size(); i++) {
					receipt = peerMatched.send_tit_for_tat(message, peerMatching, true);
					time = 0;
					while(!receipt && time < timeWaiting) {
						receipt = peerMatched.send_tit_for_tat(message, peerMatching, true);
						time++;
					}
					if(!receipt) {
						break;
					}
				}
			}
			if(receipt) {
				peerMatching.addGridMatching(gridIndex, peerMatched);
			}
		}
		
	}

	public static long anonymizeViaMicroaggregation(ArrayList<Peer> peers, Peer centroid, int k) {
		ArrayList<Peer>peersToAssign;
		Peer farthestPeer, closestPeer;
		ArrayList<ArrayList<Peer>>clusters;
		int indexCluster;
		Coordinate2 coordinate;
		long iniTime, runTime;
		
		clusters = new ArrayList<ArrayList<Peer>>();
		
		System.out.println("Calculating clusters...");
		Progress.createProgress(null, peers.size(), true);
		iniTime = System.currentTimeMillis();
		peersToAssign = new ArrayList<Peer>();
		peersToAssign.addAll(peers);
		indexCluster = 0;
		while(peersToAssign.size() >= (2*k)) {
			clusters.add(new ArrayList<Peer>());
			farthestPeer = searchFarthest(centroid, peersToAssign);
			clusters.get(indexCluster).add(farthestPeer);
			peersToAssign.remove(farthestPeer);
			Progress.update();
			for(int i=0; i<k-1; i++) {
				closestPeer = searchClosest(farthestPeer, peersToAssign);
				clusters.get(indexCluster).add(closestPeer);
				peersToAssign.remove(closestPeer);
				Progress.update();
			}
			indexCluster++;
		}
		//Rest of peers joined in a cluster
		if(peersToAssign.size() > 0) {
			clusters.add(new ArrayList<Peer>());
			for(Peer peer:peersToAssign) {
				clusters.get(indexCluster).add(peer);
				Progress.update();
			}
			peersToAssign.clear();
		}
		//Anonymizing
		for(ArrayList<Peer>cluster:clusters) {
			centroid = Peer.calculateCentroidAggregatingCoordinates(cluster);
			for(Peer peer:cluster) {
				peer.trajectory.coordinatesAnom = new ArrayList<Coordinate2>();
				peer.trajectory.velocityAnom = centroid.trajectory.velocityAnom;
				for(int i=0; i<centroid.trajectory.coordinate.size(); i++) {
					coordinate = centroid.trajectory.coordinate.get(i);
					peer.trajectory.coordinatesAnom.add(coordinate);
				}
			}
		}
		
		runTime = System.currentTimeMillis() - iniTime;
		runTime /= 1000;
		System.out.println("Runtime (secs): " + runTime);
		
		return runTime;
	}
	
	private static Peer searchFarthest(Peer peer, ArrayList<Peer>peers) {
		Peer farthestPeer = null;
		double dist, maxDist;
		
		maxDist = 0;
		for(Peer p:peers) {
//			dist = p.trayecto.normalizedTrajectoryDistance(peer.trayecto);
//			dist = p.trayecto.distanceAnt(peer.trayecto);
			dist = Functions.distsFar.get(p);
			if(dist > maxDist) {
				maxDist = dist;
				farthestPeer = p;
			}
		}
		
		return farthestPeer;
	}
	
	private static Peer searchClosest(Peer peer, ArrayList<Peer>peers) {
		Peer closestPeer = null;
		double dist, minDist;
		
		minDist = Double.MAX_VALUE;
		for(Peer p:peers) {
			dist = p.trajectory.distanceAnt(peer.trajectory);
			if(dist < minDist) {
				minDist = dist;
				closestPeer = p;
			}
		}
		
		return closestPeer;
	}
	
	public static boolean testMatchings(Peer peer, int k) {
		int currentK;
		
		if(peer.assigned && peer.controlPeersMatched.size() == 0) {
			return true;
		}
		currentK = 0;
		for(ArrayList<Peer>peers:peer.gridMatchings) {
			currentK += peers.size();
		}
		if(currentK >= (k-1)) {
			return true;
		}
		
		return false;
	}
	
	public static void generateTopicSubscriptions(ArrayList<Peer> peers, int timeMargin, boolean interpolated) {
		String topicString;
		
		calculateGeneralizedTrajectories(peers, grids, timeMargin);
		gridTopics.clear();
		for(int i=0; i<grids.size(); i++) {
			gridTopics.add(new Topics());
		}
		for(Peer peer:peers) {
			peer.initialize();
			for(int i=0; i<grids.size(); i++) {
				peer.gridMatchings.add(new ArrayList<Peer>());
			}
		}

		for(Peer peer:peers) {
			for(int gridIndex=0; gridIndex<grids.size(); gridIndex++) {
				if(interpolated) {
					topicString = peer.trajectory.topicsInterpolatedString.get(gridIndex);
				}
				else {
					topicString = peer.trajectory.topicsString.get(gridIndex);
				}
				
				gridTopics.get(gridIndex).addTopicSubscriptor(topicString, peer);
//				peer.numMessages++;	//topic subscription
			}
		}
	}
	
	public static ArrayList<GridGenerator2> generateGrids(int minGridSize) {
		GridGenerator2 grid, gridAnt;
		int gridSize;
		
		grids = new ArrayList<GridGenerator2>();
		
		gridSize = minGridSize;
		grid = new GridGenerator2(gridSize);
		grids.add(grid);
		while(!grid.isMaximum()) {
			gridAnt = grid;
			gridSize += minGridSize;
			grid = new GridGenerator2(gridSize);
			if(!grid.equals(gridAnt)) {
				grids.add(grid);
				gridAnt = grid;
			}
		}
		Trajectory.metersbetweenPoints = grids.get(0).gridSize / 2;
		
		return grids;
	}

	public static double computeOverlap(long i_ini, long i_fin, long g_ini, long g_fin) {
		long I;
		double overlap;
		
		I = Math.max(Math.min(i_fin, g_fin) - Math.max(i_ini, g_ini), 0);
		overlap = 100 * Math.min(((double)I/(i_fin-i_ini)), ((double)I/(g_fin-g_ini)));
		
		return overlap;
	}
	
	public static boolean computeOverlap2(long i_ini, long i_fin, long g_ini, long g_fin) {
		
		if(i_ini >= g_ini && i_ini <= g_fin) {
			return true;
		}
		if(i_fin >= g_ini && i_fin <= g_fin) {
			return true;
		}
		
		return false;
	}
	
	public static Result computeStatistics(ArrayList<Peer> peers) {
		double SSE, TMD, meanSSE, RMSE, meanRMSE, meanRMSEBadPeers, meanRMSENormalPeers;
		int numMessages, numBadPeers, numNormalPeers;
		Double dist;
		Result result;
		Trajectory trayectoOri, trayectoAnom;
		
		System.out.println("Calculating statistics...");
		
		trayectoOri = new Trajectory(null);
		trayectoAnom = new Trajectory(null);
		SSE = 0;
		for(Peer peer:peers) {
//			System.out.println("Peer: " + peer);
			trayectoOri.coordinate = peer.trajectory.coordinate;
			trayectoOri.velocity = peer.trajectory.velocity;
			trayectoAnom.coordinate = peer.trajectory.coordinatesAnom;
			trayectoAnom.velocity = peer.trajectory.velocityAnom;
//			if(peer.trajectory.velocity < 0.0001 || peer.trajectory.velocityAnom < 0.0001) {
//				System.out.println("Peer con velocidad 0.0:" + peer + " Vel: " + 
//									peer.trajectory.velocity + " VelAnom: " + peer.trajectory.velocityAnom);
//			}
//			dist = trayectoOri.HaservineTrajectoryDistance(trayectoAnom);
			dist = trayectoOri.distanceAnt(trayectoAnom);
			SSE += (dist*dist);
		}
		meanSSE = SSE / peers.size();
		
		System.out.println("SSE: " + convert(SSE, 2));
		System.out.println("Mean SSE: " + convert(meanSSE, 2));
		
		RMSE = meanRMSEBadPeers = meanRMSENormalPeers = 0;
		numBadPeers = numNormalPeers = 0;
		for(Peer peer:peers) {
			trayectoOri.coordinate = peer.trajectory.coordinate;
			trayectoOri.velocity = peer.trajectory.velocity;
			trayectoAnom.coordinate = peer.trajectory.coordinatesAnom;
			trayectoAnom.velocity = peer.trajectory.velocityAnom;
//			dist = trayectoOri.HaservineTrajectoryDistance(trayectoAnom);
			dist = trayectoOri.distanceAnt(trayectoAnom);
			RMSE += dist;
			if(peer.badPeer) {
				meanRMSEBadPeers += dist;
				numBadPeers++;
			}
			else {
				meanRMSENormalPeers += dist;
				numNormalPeers++;
			}
		}
		meanRMSE = RMSE / peers.size();
		meanRMSEBadPeers /= numBadPeers;
		meanRMSENormalPeers /= numNormalPeers;
		
		TMD = 0;
		for(Peer peer:peers) {
//			System.out.println("Peer: " + peer);
			trayectoOri.coordinate = peer.trajectory.coordinate;
			trayectoOri.velocity = peer.trajectory.velocity;
			trayectoAnom.coordinate = peer.trajectory.coordinatesAnom;
			trayectoAnom.velocity = peer.trajectory.velocityAnom;
//			dist = trayectoOri.HaservineTrajectoryDistance(trayectoAnom);
			dist = trayectoOri.distanceTMD(trayectoAnom);
			TMD += dist;
		}
		TMD = TMD / peers.size();
		
		System.out.println("SSE: " + convert(SSE, 2));
		System.out.println("Mean SSE: " + convert(meanSSE, 2));
		
		System.out.println("RMSE: " + convert(RMSE, 2));
		System.out.println("meanRMSE: " + convert(meanRMSE, 2));
		
		result = new Result(meanRMSE, SSE);
		result.meanRmseBadPeers = meanRMSEBadPeers;
		result.meanRmseNormalPeers = meanRMSENormalPeers;
		result.TMD = TMD;
		
		numMessages = 0;
		for(Peer peer:peers) {
			numMessages += peer.numMessages;
		}
		result.numMessages = numMessages;
		
		return result;
		
	}
	
	public static void calculateGeneralizedTrajectories(ArrayList<Peer> peers, ArrayList<GridGenerator2> grids, int timeMargin) {
		Trajectory trayecto;
		
		for(Peer peer:peers) {
			trayecto = peer.getTrayecto();
			trayecto.calculateGeneralizedTrajectories(grids, timeMargin);
		}
		
	}
	
	public static void filterCoordinatesInsideQuadrant(ArrayList<Trajectory>trajectories){
		ArrayList<Coordinate2>coordinatesTemp;
		double latitudTop, latitudBot, longitudTop, longitudBot;
		boolean ok;
		
		latitudTop = GridGenerator.topLeftCoordinate.latitude;
		latitudBot = GridGenerator.botRightCoordinate.latitude;
		longitudTop = GridGenerator.topLeftCoordinate.longitude;
		longitudBot = GridGenerator.botRightCoordinate.longitude;
		
		for(Trajectory trajectory:trajectories){
			coordinatesTemp = new ArrayList<Coordinate2>();
			for(Coordinate2 coor:trajectory.getCoordinates()){
				ok = true;
				if(coor.latitude > latitudTop || coor.longitude < longitudTop){
					ok = false;
				}
				if(coor.latitude < latitudBot || coor.longitude > longitudBot){
					ok = false;
				}
				if(ok){
					coordinatesTemp.add(coor);
				}
			}
			trajectory.coordinate = coordinatesTemp;
		}
		System.out.println("Filtered coordinates inside quadrant");
	}
	
	public static void printInitialStatistics(ArrayList<Peer> peers) {
		Trajectory trajectory;
		int hoursTrajectory[] = new int[24];
		int dayMonthTrajectory[] = new int[31];
		int dayWeekTrajectory[] = new int[7];
		int hoursPoint[] = new int[24];
		int dayMonthPoint[] = new int[31];
		int dayWeekPoint[] = new int[7];
		HashMap<Integer, Integer>numberPointsRank = new HashMap<>();
		ArrayList<Integer>temp = new ArrayList<>();
		Timestamp timestamp;
		Calendar calendar = Calendar.getInstance();
		long time;
		int day, hour, dayWeek, totalPoints, numPuntosRango;
		Integer numPointsTemp;
		
		calendar.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
		totalPoints = 0;
		for(Peer peer:peers) {
			trajectory = peer.getTrayecto();
			time = trajectory.getTimeInitial();
			timestamp = new Timestamp(time*1000);
			calendar.setTimeInMillis(timestamp.getTime());
			day = calendar.get(Calendar.DAY_OF_MONTH);
			day--;
			hour = calendar.get(Calendar.HOUR_OF_DAY);
			dayWeek = calendar.get(Calendar.DAY_OF_WEEK);
			dayWeek--;
			
			hoursTrajectory[hour]++;
			dayMonthTrajectory[day]++;
			dayWeekTrajectory[dayWeek]++;
			numPuntosRango = (int)(trajectory.getCoordinates().size());
			numPointsTemp = numberPointsRank.get(numPuntosRango);
			if(numPointsTemp == null){
				numberPointsRank.put(numPuntosRango, 1);
			}
			else{
				numPointsTemp++;
				numberPointsRank.put(numPuntosRango,  numPointsTemp);
			}
			
			calendar.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
			for(Coordinate2 coordenada:trajectory.getCoordinates()) {
				time = coordenada.time;
				timestamp = new Timestamp(time*1000);
				calendar.setTimeInMillis(timestamp.getTime());
				day = calendar.get(Calendar.DAY_OF_MONTH);
				day--;
				hour = calendar.get(Calendar.HOUR_OF_DAY);
				dayWeek = calendar.get(Calendar.DAY_OF_WEEK);
				dayWeek--;
				
				hoursPoint[hour]++;
				dayMonthPoint[day]++;
				dayWeekPoint[dayWeek]++;
				totalPoints++;
			}
		}
		
		System.out.println("Number of peers (trajectories): " + peers.size());
		System.out.println("Number of trajectories by hours: ");
		for(int i=0; i<24; i++){
			System.out.println(i + "\t" + hoursTrajectory[i]);
		}
		System.out.println("Number of trajectories by day of month: ");
		for(int i=0; i<31; i++){
			System.out.println(i + "\t" + dayMonthTrajectory[i]);
		}
		System.out.println("Number of trajectories by day of week: ");
		for(int i=0; i<7; i++){
			System.out.println(i + "\t" + dayWeekTrajectory[i]);
		}
		
		System.out.println("Number of points: " + totalPoints);
		System.out.println("Number of points by hours: ");
		for(int i=0; i<24; i++){
			System.out.println(i + "\t" + hoursPoint[i]);
		}
		System.out.println("Number of points by day of month: ");
		for(int i=0; i<31; i++){
			System.out.println(i + "\t" + dayMonthPoint[i]);
		}
		System.out.println("Number of points by day of week: ");
		for(int i=0; i<7; i++){
			System.out.println(i + "\t" + dayWeekPoint[i]);
		}
		
		for(Integer i:numberPointsRank.keySet()){
			temp.add(i);
		}
		Collections.sort(temp);
		System.out.println("Number of trajectories by number of points: ");
		for(Integer i:temp){
			if(i > 40){
				break;
			}
			numPointsTemp = numberPointsRank.get(i);
			if(numPointsTemp == null){
				numPointsTemp = 0;
			}
			System.out.println(i + "\t" + numPointsTemp);
		}
		
	}
	
	public static ArrayList<Trajectory> filterTrajectoriesMondayToFriday(ArrayList<Trajectory>trajectories){
		ArrayList<Trajectory>trayectosFiltrado = new ArrayList<>();
		Calendar calendar = Calendar.getInstance();
		long time;
		Timestamp timestamp;
		int diaSem;
		
		//Treu dissabte(6) i diumenge(0), es queda amb els trayectes de dilluns a divendres
		calendar.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
		for(Trajectory trayecto:trajectories) {
			time = trayecto.getTimeInitial();
			timestamp = new Timestamp(time*1000);
			calendar.setTimeInMillis(timestamp.getTime());
			diaSem = calendar.get(Calendar.DAY_OF_WEEK);
			diaSem--;
			if(diaSem > 0 && diaSem < 6) {
				trayectosFiltrado.add(trayecto);
			}
		}
		System.out.println("Trajectories Monday to Friday: " + trayectosFiltrado.size());
		
		return trayectosFiltrado;
	} 
	
	public static ArrayList<Trajectory> filterTrajectoriesDayWeek(ArrayList<Trajectory>trajectories, int diaSemToKeep){
		ArrayList<Trajectory>trayectosFiltrado = new ArrayList<>();
		Calendar calendar = Calendar.getInstance();
		long time;
		Timestamp timestamp;
		int diaSem;
		
		//0-Sunday, 1-Monday, 2-Tuesday, 3-Wednesday, 4-Thursday, 5-Friday, 6-Saturday 
		calendar.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
		for(Trajectory trayecto:trajectories) {
			time = trayecto.getTimeInitial();
			timestamp = new Timestamp(time*1000);
			calendar.setTimeInMillis(timestamp.getTime());
			diaSem = calendar.get(Calendar.DAY_OF_WEEK);
			diaSem--;
			if(diaSem == diaSemToKeep) {
				trayectosFiltrado.add(trayecto);
			}
		}
		System.out.println("Trajectories day " + diaSemToKeep + ": "  + trayectosFiltrado.size());
		
		return trayectosFiltrado;
	} 
	
	public static void calculateUnifiedTime(ArrayList<Trajectory>trayectos){
		Calendar calendar = Calendar.getInstance();
		long time, time1, timeAnt, dif;
		Timestamp timestamp;
		
		calendar.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
		for(Trajectory trayecto:trayectos){
			time1 = trayecto.getTimeInitial();
			for(Coordinate2 coordenada:trayecto.coordinate){
				time = coordenada.time;
				timestamp = new Timestamp(time*1000);
				calendar.setTimeInMillis(timestamp.getTime());
				calendar.set(Calendar.DAY_OF_MONTH, 4);
				calendar.set(Calendar.MONTH, 1);
				time = calendar.getTimeInMillis() / 1000;
				coordenada.setTimeUnified(time);
				coordenada.time = time;
				timestamp = new Timestamp(time*1000);
				coordenada.time = time;
			}
		}
		
	}

	public static ArrayList<Peer> createListPeers(ArrayList<Trajectory> trajectories, int numPeers) {
		ArrayList<Peer>peers = new ArrayList<Peer>();
		Peer peer;
		int peerId, peersToCreate;
		Trajectory trajectory;
		double meanVelocity;
		double maxDistTime;
		
		if(numPeers == 0) {
			peersToCreate = trajectories.size();
		}
		else {
			peersToCreate = numPeers;
		}
		
		meanVelocity = 0.0;
		peerId = 0;
		for(int i=0; i<peersToCreate; i++) {
			trajectory = trajectories.get(i);
			trajectory.calculateAcumulatedDistance();
			trajectory.calculateAcumulatedTime();
			trajectory.calculateVelocity();
			meanVelocity += trajectory.velocity;
			peer = new Peer(peerId, trajectory);
			peers.add(peer);
			peerId++;
		}
		Functions.meanVelocity = meanVelocity / peers.size();
		System.out.println("Mean velocity: " + convert(Functions.meanVelocity, 2) + " m/sec");
		System.out.println("Max distance: " + convert(Functions.maxDist, 2) + " m");
		System.out.println("Max time: " + convert(Functions.maxTime, 2) + " sec");
		maxDistTime = Functions.maxTime * Functions.meanVelocity;
		System.out.println("Max distance in time: " + convert(maxDistTime, 2) + " m");
		Functions.compensation = maxDistTime / Functions.maxDist;
		System.out.println("Compensation time / distance: " + convert(Functions.compensation, 2));
		
		System.out.println("Peers created: " + peers.size());
		
		return peers;
	}
	
	public static void assignBadPeers(ArrayList<Peer>peers, double percenBadPeers) {
		int numBadPeers;
		ArrayList<Integer>indexPeers;
		
		indexPeers = new ArrayList<Integer>();
		for(Peer peer:peers) {
			indexPeers.add(peer.id);
			peer.badPeer = false;
		}
		
		Peer.peers = peers;
		numBadPeers = (int)Math.round(Peer.peers.size() * percenBadPeers / 100);
		Collections.shuffle(indexPeers, Peer.rnd);
		for(int i=0; i<numBadPeers; i++) {
			peers.get(indexPeers.get(i)).badPeer = true;
		}
	}
	
	public static ArrayList<Trajectory> filterOneWeek(ArrayList<Trajectory>trajectories){
		ArrayList<Trajectory>trayectosFiltrado = new ArrayList<>();
		Timestamp timestamp;
		Calendar calendar = Calendar.getInstance();
		long time;
		int dia;
		
		calendar.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
		for(Trajectory trayecto:trajectories){
			time = trayecto.getTimeInitial();
			timestamp = new Timestamp(time*1000);
			calendar.setTimeInMillis(timestamp.getTime());
			dia = calendar.get(Calendar.DAY_OF_MONTH);
			dia--;
			if(dia >= 1 && dia <= 7){
				trayectosFiltrado.add(trayecto);
			}
		}
		System.out.println("Trajectories one weeek: " + trayectosFiltrado.size());
		
		return trayectosFiltrado;
	}
	
	public static void calculateMeanNumTypicalDeviationCoordinatesByTrajectory(ArrayList<Trajectory> trajectory) {
		int totalCoordinates;
		double mean, partial, deviation;
		
		totalCoordinates = 0;
		for(Trajectory trayecto:trajectory){
			totalCoordinates += trayecto.getNumCoordinates();
		}
		mean = (double)totalCoordinates / (double)trajectory.size();
		System.out.println("Mean coordinates by trajectory: " + truncate(mean, 2));
		
		totalCoordinates = 0;
		for(Trajectory trayecto:trajectory){
			partial = (trayecto.getNumCoordinates() - mean);
			totalCoordinates += partial * partial;
		}
		deviation = Math.sqrt(totalCoordinates / trajectory.size());
		System.out.println("Typical deviation of coordinates by trajectory: " + truncate(deviation, 2));
	}
	
	public static void calculateAcumulatedDistanceTrajectory(ArrayList<Trajectory>trajectories){
		double dist;
		
		for(Trajectory trayecto:trajectories){
			dist = trayecto.calculateAcumuatedDistanceStartEnd();
			trayecto.setAcumulatedDistance(dist);
		}
	}
	
	public static void calculateDistanceTrajectory(ArrayList<Trajectory>trajectories){
		double dist;
		
		for(Trajectory trayecto:trajectories){
			dist = trayecto.calculateDistanceStartEnd();
			trayecto.setDistance(dist);
		}
	}
	
	public static void calculateTimeTrajectory(ArrayList<Trajectory>trajectories){
		int tiempo;
		
		for(Trajectory trayecto:trajectories){
			tiempo = trayecto.calculateTime();
			trayecto.setTime(tiempo);
		}
	}
	
	public static ArrayList<Trajectory> filterTrajectoriesByNumCoordinates(ArrayList<Trajectory>trajectories, int numCoordinates){
		ArrayList<Trajectory>trayectosFiltrado = new ArrayList<>();
		
		for(Trajectory trayecto:trajectories){
			if(trayecto.getNumCoordinates() >= numCoordinates){
				trayectosFiltrado.add(trayecto);
			}
		}
		System.out.println("Trajectories with " + numCoordinates + " or more coordinates: " + trayectosFiltrado.size());
		
		return trayectosFiltrado;
	}
	
	public static ArrayList<Trajectory> filterTrajectoriesByDistance(ArrayList<Trajectory>trajectories, double distance){
		ArrayList<Trajectory>trayectosFiltrado = new ArrayList<>();
		double dist;
		
		for(Trajectory trayecto:trajectories){
			dist = trayecto.getDistanceAcum();
			if(dist >= distance){
				trayectosFiltrado.add(trayecto);
			}
		}
		System.out.println("Trajectories with " + distance + " or more distance: " + trayectosFiltrado.size());
		
		return trayectosFiltrado;
	}
	
	public static ArrayList<Trajectory> loadTrajectories(){
		ArrayList<Trajectory>trajectoriesFree = new ArrayList<>();
		ArrayList<Trajectory>trajectoriesOccupied = new ArrayList<>();
		ArrayList<Trajectory>trajectoriesAll = new ArrayList<>();
		ArrayList<String>cabs = new ArrayList<>();
		ArrayList<Coordinate2>coordinates = new ArrayList<>();
		File file;
		Coordinate2 coordinate;
		Trajectory trajectory;
		FileReader fr;
		BufferedReader br;
		String line;
		double lat, lon;
		int ocu, ocuAnt;
		long time;
		String strTemp[];
		int indexIni, indexFin;
		boolean first;
		int idCab;
		
		file = new File("./cabspottingdata/_cabs.txt");
		try {
			fr = new FileReader (file);
			br = new BufferedReader(fr);
			while((line=br.readLine())!=null){
				indexIni = line.indexOf("\"");
				indexIni++;
				indexFin = line.indexOf("\"", indexIni);
				line = line.substring(indexIni, indexFin);
				cabs.add(line);
			}
			br.close();
			fr.close();
			
			idCab = 1;
			for(String cab:cabs){
				file = new File("./cabspottingdata/new_" + cab + ".txt");
				fr = new FileReader (file);
				br = new BufferedReader(fr);
				first = true;
				ocuAnt = 0;
				coordinates = new ArrayList<>();
				while((line=br.readLine())!=null){
					strTemp = line.split(" ");
					lat = Double.parseDouble(strTemp[0]);
					lon = Double.parseDouble(strTemp[1]);
					ocu = Integer.parseInt(strTemp[2]);
					time = Long.parseLong(strTemp[3]);
					coordinate = new Coordinate2(lat, lon, time);
					if(first){
						ocuAnt = ocu;
						first = false;
					}
					if(ocu != ocuAnt){
						if(ocu == 0){
							trajectory = new Trajectory(coordinates, false, cab, idCab);
							trajectoriesFree.add(trajectory);
						}
						else{
							trajectory = new Trajectory(coordinates, true, cab, idCab);
							trajectoriesOccupied.add(trajectory);
						}
						trajectoriesAll.add(trajectory);
						coordinates = new ArrayList<>();
					}
					coordinates.add(0, coordinate);
					ocuAnt = ocu;
				}
				br.close();
				fr.close();
				idCab++;
			}
			System.out.println("Trajectories free loaded: " + trajectoriesFree.size());
			System.out.println("Trajectories occupied loaded: " + trajectoriesOccupied.size());
			System.out.println("Trajectories all loaded: " + trajectoriesAll.size());
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("Trajectories loaded: " + trajectoriesOccupied.size());
		
		return trajectoriesOccupied;
	}
	
	public static void printResults(ArrayList<ArrayList<Result>> results) {
		String s;
		ArrayList<Result>resultsTotal = new ArrayList<Result>();
		Result resultPartial;
		
		for(int i=0; i<results.get(0).size(); i++) {
			resultsTotal.add(new Result());
		}
		
		for(ArrayList<Result>resultsPartial:results) {
			for(int i=0; i<resultsPartial.size(); i++) {
				resultPartial = resultsPartial.get(i);
				resultsTotal.get(i).item = resultPartial.item;
				resultsTotal.get(i).meanRmse += resultPartial.meanRmse;
				resultsTotal.get(i).SSE += resultPartial.SSE;
				resultsTotal.get(i).TMD += resultPartial.TMD;
				resultsTotal.get(i).runTime += resultPartial.runTime;
				resultsTotal.get(i).numMessages += resultPartial.numMessages;
			}
		}
		
		for(Result result:resultsTotal) {
			result.meanRmse /= results.size();
			result.SSE /= results.size();
			result.TMD /= results.size();
			result.runTime /= results.size();
			result.numMessages /= results.size();
		}
		
		for(Result result:resultsTotal) {
			s = "";
			s += convert(result.item, 1);
			s += "\t";
			s += convert(result.meanRmse);
			s += "\t";
			s += convert(result.SSE);
			s += "\t";
			s += convert(result.TMD);
			s += "\t";
			s += result.runTime;
			s += "\t";
			s += result.numMessages;
			
			System.out.println(s);
		}
		
	}
	
	public static void printResultsBadPeers(ArrayList<ArrayList<Result>> results) {
		String s;
		ArrayList<Result>resultsTotal = new ArrayList<Result>();
		Result resultPartial;
		
		for(int i=0; i<results.get(0).size(); i++) {
			resultsTotal.add(new Result());
		}
		
		for(ArrayList<Result>resultsPartial:results) {
			for(int i=0; i<resultsPartial.size(); i++) {
				resultPartial = resultsPartial.get(i);
				resultsTotal.get(i).item = resultPartial.item;
				resultsTotal.get(i).meanRmse += resultPartial.meanRmse;
				resultsTotal.get(i).SSE += resultPartial.SSE;
				resultsTotal.get(i).TMD += resultPartial.TMD;
				resultsTotal.get(i).runTime += resultPartial.runTime;
				resultsTotal.get(i).numMessages += resultPartial.numMessages;
				resultsTotal.get(i).meanRmseBadPeers += resultPartial.meanRmseBadPeers;
				resultsTotal.get(i).meanRmseNormalPeers += resultPartial.meanRmseNormalPeers;
			}
		}
		
		for(Result result:resultsTotal) {
			result.meanRmse /= results.size();
			result.SSE /= results.size();
			result.TMD /= results.size();
			result.runTime /= results.size();
			result.numMessages /= results.size();
			result.meanRmseBadPeers /= results.size();
			result.meanRmseNormalPeers /= results.size();
		}
		
		for(Result result:resultsTotal) {
			s = "";
			s += convert(result.item, 1);
			s += "\t";
			s += convert(result.meanRmse);
			s += "\t";
			s += convert(result.SSE);
			s += "\t";
			s += convert(result.TMD);
			s += "\t";
			s += convert(result.meanRmse);
			s += "\t";
			s += convert(result.meanRmseBadPeers);
			s += "\t";
			s += convert(result.meanRmseNormalPeers);
			s += "\t";
			s += result.runTime;
			s += "\t";
			s += result.numMessages;
			
			System.out.println(s);
		}
		
	}
	
	public static void printResultsPartial(ArrayList<Result> results) {
		String s;
		
		for(Result result:results) {
			s = "";
			s += convert(result.item, 1);
			s += "\t";
			s += convert(result.meanRmse);
			s += "\t";
			s += convert(result.SSE);
			s += "\t";
			s += convert(result.TMD);
			s += "\t";
			s += result.runTime;
			s += "\t";
			s += result.numMessages;
			
			System.out.println(s);
		}
		
	}

	public static String convert(double numero){
		String s;
		
		s = "";
		s = String.valueOf(numero);
		s = s.replace(".", ",");
		
		return s;
	}
	
	public static String convert(double numero, int decimales){
		String s;
		double mult;
		
		mult = Math.pow(10, decimales);
		numero *= mult;
		numero = Math.round(numero);
		numero /= mult;
		
		s = "";
		s = String.valueOf(numero);
		s = s.replace(".", ",");
		
		return s;
	}
	
	public static String truncate(double numero, int decimales){
		String s;
		double mult;
		
		mult = Math.pow(10, decimales);
		numero *= mult;
		numero = Math.round(numero);
		numero /= mult;
		
		return String.valueOf(numero);
	}

}
