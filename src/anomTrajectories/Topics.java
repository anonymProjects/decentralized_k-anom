package anomTrajectories;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class Topics {
	HashMap<String, ArrayList<Peer>>topics;
	
	public Topics() {
		this.topics = new HashMap<String, ArrayList<Peer>>();
	}
	
	public boolean existsTopic(String topic) {
		ArrayList<Peer> subscriptors;
		
		subscriptors = this.topics.get(topic);
		if(subscriptors == null) {
			return false;
		}
		return true;
	}
	
	public void addTopicSubscriptor(String topic, Peer subscriptor) {
		if(!this.existsTopic(topic)) {
			this.topics.put(topic, new ArrayList<Peer>());
		}
		this.topics.get(topic).add(subscriptor);
	}
	
	public ArrayList<Peer> getTopicSubscriptors(String topic){
		return this.topics.get(topic);
	}
	
	public Set<String> getTopics() {
		return this.topics.keySet();
	}

}

