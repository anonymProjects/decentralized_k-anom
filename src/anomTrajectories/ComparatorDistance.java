package anomTrajectories;
import java.util.Comparator;

public class ComparatorDistance implements Comparator<Peer>{

	public int compare(Peer o1, Peer o2) {
		if(o1.distance < o2.distance) {
			return -1;
		}
		if(o1.distance > o2.distance) {
			return 1;
		}
		return 0;
	}
}