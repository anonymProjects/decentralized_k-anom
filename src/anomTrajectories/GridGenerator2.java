package anomTrajectories;
import java.util.ArrayList;

public class GridGenerator2 {
    public static Coordinate topLeftCoordinate = new Coordinate(37.810791, -122.515053);
    public static Coordinate botRightCoordinate = new Coordinate(37.601877, -122.343929);
    //public static Coordinate botRightCoordinate = new Coordinate(37.707358, -122.355784);
    private int latitudeNumberOfSquares = 0;
    private int longitudeNumberOfSquares = 0;
    public static final double degreesPerKilometer = 0.009; //TODO: this should be in a constants class.
    private double gridDegrees = 0;
    int gridSize;
    
    public GridGenerator2(int gridDistance) {
    	//Take in account that first grid position is 0,0 (x,y)
        //Calculate the grid distance in decimal degrees.
    	this.gridSize = gridDistance;
        gridDegrees = (gridDistance * degreesPerKilometer)/1000;

        //Calculate the total number of grid positions. We ceil the latitude as it is a positive number, and we floor the longitude as it is negative.
        //TODO: We ceil the latitude as it is a positive number, and we floor the longitude as it is negative. AUTOMATE IT
        latitudeNumberOfSquares = (int) Math.ceil((topLeftCoordinate.getLatitude() - botRightCoordinate.getLatitude()) / gridDegrees);
        longitudeNumberOfSquares = (int) Math.floor((topLeftCoordinate.getLongitude() - botRightCoordinate.getLongitude()) / gridDegrees);

        //Check and correct negative numbers.
        latitudeNumberOfSquares = (latitudeNumberOfSquares < 0) ? latitudeNumberOfSquares * -1 : latitudeNumberOfSquares;
        longitudeNumberOfSquares = (longitudeNumberOfSquares < 0) ? longitudeNumberOfSquares * -1 : longitudeNumberOfSquares;
//        System.out.println("Grid - x: "+longitudeNumberOfSquares+" y: "+latitudeNumberOfSquares + " (" 
//                + longitudeNumberOfSquares*latitudeNumberOfSquares + ")");
    }
    
    public int getNumSquares() {
    	return longitudeNumberOfSquares*latitudeNumberOfSquares;
    }
    
    public GridCoordinate getGridPosition(Coordinate coord){
        GridCoordinate result = new GridCoordinate();
        double latitude;
        double longitude;

        //First we check if the passed coordinate is located inside the grid's bounds.
        if(coord.getLatitude() > topLeftCoordinate.getLatitude() && coord.getLatitude() < botRightCoordinate.getLatitude())
        {
            System.out.println("Coordinate out of bounds");
            return new GridCoordinate(-1,-1);
        }
        else if(coord.getLongitude() < topLeftCoordinate.getLongitude() && coord.getLongitude() > botRightCoordinate.getLongitude())
        {
            System.out.println("Coordinate out of bounds");
            return new GridCoordinate(-1,-1);
        }

        //Next, we need to know the distance from left to point, and then calculate the grid position.
        latitude = topLeftCoordinate.getLatitude() - coord.getLatitude();
        longitude = topLeftCoordinate.getLongitude() - coord.getLongitude();
        result.y = (int)(latitude / gridDegrees);
        result.x = (int)(longitude / gridDegrees);
        //Negative to positive
        result.y = (result.y < 0) ? result.y * -1 : result.y;
        result.x = (result.x < 0) ? result.x * -1 : result.x;

        return result;
    }
    
    public GridCoordinate getGridPosition(Coordinate2 coord){
        GridCoordinate result = new GridCoordinate();
        double latitude;
        double longitude;

        //First we check if the passed coordinate is located inside the grid's bounds.
        if(coord.latitude > topLeftCoordinate.getLatitude() || coord.latitude < botRightCoordinate.getLatitude())
        {
            System.out.println("Coordinate out of bounds");
            return new GridCoordinate(-1,-1);
        }
        else if(coord.longitude < topLeftCoordinate.getLongitude() || coord.longitude > botRightCoordinate.getLongitude())
        {
            System.out.println("Coordinate out of bounds");
            return new GridCoordinate(-1,-1);
        }

        //Next, we need to know the distance from left to point, and then calculate the grid position.
        latitude = topLeftCoordinate.getLatitude() - coord.latitude;
        longitude = topLeftCoordinate.getLongitude() - coord.longitude;
        result.y = (int)(latitude / gridDegrees);
        result.x = (int)(longitude / gridDegrees);
        //Negative to positive
        result.y = (result.y < 0) ? result.y * -1 : result.y;
        result.x = (result.x < 0) ? result.x * -1 : result.x;

        return result;
    }
    
    public GridCoordinate generalizeCoordinate(Coordinate2 coord) {
    	return getGridPosition(coord);
    }

    public boolean checkSameGridPosition(Coordinate coord1, Coordinate coord2) {
        GridCoordinate c1 = getGridPosition(coord1);
        GridCoordinate c2 = getGridPosition(coord2);
        return c1.equals(c2);
    }

    public ArrayList<GridCoordinate> findElegibleGridPositions(Coordinate coord, int totalDistance) {
        ArrayList<GridCoordinate> result = new ArrayList<>();
        Coordinate moved;
        GridCoordinate temp;
        boolean addGridCoordinate = true;

        result.add(getGridPosition(coord)); //Adds the actual grid position to the result, so we can skil distance = 0
        for(int distance = (totalDistance/2); distance <= totalDistance; distance += (totalDistance/2)) {
            for (double angle = 0; angle <= 360; angle += 11.25) {
                moved = moveCoordinate(coord, angle, distance);
                temp = getGridPosition(moved);
                for(GridCoordinate gc : result)
                {
                    if(gc.equals(temp)) addGridCoordinate = false;
                }
                if(addGridCoordinate) result.add(temp);
                addGridCoordinate = true;
            }
        }
        return result;
    }
    
    public ArrayList<GridCoordinate> findElegibleGridPositions(Coordinate2 coord, int totalDistance) {
        ArrayList<GridCoordinate> result = new ArrayList<>();
        Coordinate moved;
        GridCoordinate temp;
        boolean addGridCoordinate = true;

        result.add(getGridPosition(coord)); //Adds the actual grid position to the result, so we can skil distance = 0
        if(totalDistance == 0){
        	return result;
        }
        for(int distance = (totalDistance/2); distance <= totalDistance; distance += (totalDistance/2)) {
            for (double angle = 0; angle <= 360; angle += 11.25) {
                moved = moveCoordinate(coord, angle, distance);
                temp = getGridPosition(moved);
                for(GridCoordinate gc : result)
                {
                    if(gc.equals(temp)) addGridCoordinate = false;
                }
                if(addGridCoordinate) result.add(temp);
                addGridCoordinate = true;
            }
        }
        return result;
    }

    public boolean checkIsElegibleForRide(Coordinate driverPosition, Coordinate riderPosition, int range) {
        if(checkSameGridPosition(driverPosition,riderPosition)) return true;
        else if (distanciaHaversine(driverPosition,riderPosition) <= range) return true;
        else return false;
    }

    //TODO: Move function below to a suitable class
    public static double distanciaHaversine(Coordinate coord1, Coordinate coord2){
        final double R = 6372800.0; // in meters
        double lat1, lat2;
        double dLat = Math.toRadians(coord2.latitude - coord1.latitude);
        double dLon = Math.toRadians(coord2.longitude - coord1.longitude);

        lat1 = Math.toRadians(coord2.latitude);
        lat2 = Math.toRadians(coord1.latitude);

        double a = Math.pow(Math.sin(dLat / 2),2) + Math.pow(Math.sin(dLon / 2),2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return R * c;
    }

    public static Coordinate moveCoordinate(Coordinate coord, double angle, double distMeters) {
        double theta = angle;
        double distance = (distMeters/1000)*degreesPerKilometer;
        double x2 = coord.latitude + (Math.cos(theta)*(distance));
        double y2 = coord.longitude + (Math.sin(theta)*(distance));

        return new Coordinate(x2,y2);
    }
    
    public static Coordinate moveCoordinate(Coordinate2 coord, double angle, double distMeters) {
        double theta = angle;
        double distance = (distMeters/1000)*degreesPerKilometer;
        double x2 = coord.latitude + (Math.cos(theta)*(distance));
        double y2 = coord.longitude + (Math.sin(theta)*(distance));

        return new Coordinate(x2,y2);
    }
    
    public boolean isMaximum() {
    	if(this.latitudeNumberOfSquares >=2 && this.longitudeNumberOfSquares >=1) {
    		return false;
    	}
    	if(this.latitudeNumberOfSquares >=1 && this.longitudeNumberOfSquares >=2) {
    		return false;
    	}
    	return true;
    }
    
    @Override
    public boolean equals(Object o) {
    	GridGenerator2 other;
    	
    	other = (GridGenerator2)o;
    	if(this.latitudeNumberOfSquares != other.latitudeNumberOfSquares) {
    		return false;
    	}
    	if(this.longitudeNumberOfSquares != other.longitudeNumberOfSquares) {
    		return false;
    	}
    	return true;
    }
    
    @Override
    public String toString() {
    	return ("x: "+longitudeNumberOfSquares+" y: "+latitudeNumberOfSquares + " (" 
    	        + longitudeNumberOfSquares*latitudeNumberOfSquares + ")");
    }
}
