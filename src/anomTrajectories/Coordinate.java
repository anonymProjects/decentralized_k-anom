package anomTrajectories;
/**
 * Created by manel on 29/08/2016.
 */
public class Coordinate {
    double latitude;
    double longitude;

    public Coordinate(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void setLocation(double lat, double lon){
        this.latitude = lat;
        this.longitude = lon;
    }

    public String toString() {
        return "latitude = "+latitude+", longitude = "+longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
