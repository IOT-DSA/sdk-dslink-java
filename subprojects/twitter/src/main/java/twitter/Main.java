package twitter;

import java.io.File;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkFactory;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;

public class Main extends DSLinkHandler{

	public static void main(String[] args) {
		File nodesfile = new File("nodes.json");
		if (nodesfile.exists()) {
			if (nodesfile.delete()){
				System.out.println(".nodes deleted");
			}
		}
		args = new String[] { "-b", "http://localhost:8080/conn" };
		DSLinkFactory.startResponder("twitResponder", args, new Main());
	}
	
	@Override
	public void onResponderConnected(DSLink link){
		NodeManager manager = link.getNodeManager();
        Node superRoot = manager.getNode("/").getNode();
        TwitterThing.start(superRoot);
        
	}

}
