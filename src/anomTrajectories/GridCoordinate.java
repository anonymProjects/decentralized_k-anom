package anomTrajectories;

import java.util.ArrayList;

/**
 * Created by manel on 30/08/2016.
 */
public class GridCoordinate {
    int x;
    int y;
    ArrayList<Coordinate2>coordinates;
    Coordinate2 centroid;

    public GridCoordinate(){
        x = 0;
        y = 0;
        this.coordinates = new ArrayList<Coordinate2>();
    }
    public GridCoordinate(int x, int y){
        this.x = x;
        this.y = y;
        this.coordinates = new ArrayList<Coordinate2>();
    }
    
    public void calculateCentroid() {
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
		
		centroid = new Coordinate2(lat, lon, t); 
	}

    public int getX(){
        return x;
    }

    public int getY(){
        return y;
    }
    
    @Override
    public GridCoordinate clone(){
    	return new GridCoordinate(this.x, this.y);
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }
    
    @Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
    public boolean equals(Object other){
        boolean result = false;
        GridCoordinate otherCoord;

        //Quick checks
        if(other == null) return result;
        if(!(other instanceof GridCoordinate)) return result;
        if(other == this) return true;

        //Check if same grid position
        otherCoord = (GridCoordinate) other;
        if(this.x == otherCoord.x && this.y == otherCoord.y) result = true;

        return result;
    }
}
