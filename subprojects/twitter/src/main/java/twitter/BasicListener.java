package twitter;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.value.Value;
import org.vertx.java.core.json.JsonObject;

import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterObjectFactory;


public class BasicListener implements StatusListener{
	
	private final Node tweetNode;
	
	public BasicListener(Node tweetNode) {
		this.tweetNode = tweetNode;
	}

	public void onException(Exception arg0) {
		// TODO Auto-generated method stub
		
	}

	public void onDeletionNotice(StatusDeletionNotice arg0) {
		// TODO Auto-generated method stub
		
	}

	public void onScrubGeo(long arg0, long arg1) {
		// TODO Auto-generated method stub
		
	}

	public void onStallWarning(StallWarning arg0) {
		// TODO Auto-generated method stub
		
	}

	public void onStatus(Status status) {
		
//		Node nameNode = tweetNode.getChild("name");
//		Node textNode = tweetNode.getChild("text");
		Node jsonNode = tweetNode.getChild("rawJSON");
		
//		nameNode.setValue(new Value(status.getUser().getName()));
//		textNode.setValue(new Value(status.getText()));

		String jsonStr = TwitterObjectFactory.getRawJSON(status);
		JsonObject jsonObj = new JsonObject(jsonStr);
		jsonNode.setValue(new Value(jsonObj));
	}

	public void onTrackLimitationNotice(int arg0) {
		// TODO Auto-generated method stub
		
	}


}
