package anomTrajectories;

import java.util.ArrayList;

public class Principal {

	public static void main(String[] args) {
		ArrayList<Trajectory>trayectos;
		ArrayList<Peer>peers;
		int numCoordenadasMinimo;
		double distanciaMinima;
		int gridSize, k;
		double p, espera;
		boolean interpolado;
		
		trayectos = Functions.loadTrajectories();
		Functions.calculateAcumulatedDistanceTrajectory(trayectos);
		Functions.calculateDistanceTrajectory(trayectos);
		Functions.calculateTimeTrajectory(trayectos);
		
		
		//este filtro hace que los trayectos esten dentro de una porcion de san francisco
		Functions.filterCoordinatesInsideQuadrant(trayectos);
		
		numCoordenadasMinimo = 4;
		distanciaMinima = 500.0;
//		distanciaMinima = 0.0;
		trayectos = Functions.filterTrajectoriesByNumCoordinates(trayectos, numCoordenadasMinimo);
		trayectos = Functions.filterTrajectoriesByDistance(trayectos, distanciaMinima);
		trayectos = Functions.filterTrajectoriesMondayToFriday(trayectos);
		Functions.calculateUnifiedTime(trayectos);
//		trayectos = Funciones.filtraUnaSemana(trayectos);
		Functions.calculateMeanNumTypicalDeviationCoordinatesByTrajectory(trayectos);
		peers = Functions.createListPeers(trayectos, 0);
//		Funciones.imprimeEstadisticaInicial(peers);
		
		k = 5;
		interpolado = true;
		gridSize = 1000;	//en metros
		p = 0.0;	//en porcentaje
		espera = 60*60; // en segundos
//		Funciones.anonymiza2(peers,k, gridSize, p, interpolado);
//		Funciones.anonymiza3(peers,k, gridSize, espera, interpolado);
//		Funciones.anonymiza4(peers,k, gridSize, p);
//		Funciones.imprimeEstadisticasAnom(peers);
		
	}	

}
