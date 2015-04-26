package twitter;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.node.actions.Parameter;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.vertx.java.core.Handler;
import twitter4j.*;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;

import java.io.*;
import java.util.HashSet;

public class TwitterThing {
	
	private Node node;
	private Node err;
	private Twitter twitter;
	private AccessToken accessToken;
	private String userPath;
	private HashSet<TwitterStream> openStreams = new HashSet<TwitterStream>();
	private static final String consumerKey = "ggZzj1uh8Y6zRuvHp0QZdV1H2";
	private static final String consumerSecret = "MxRQ5zROUgyMRRJXBGQkeMtubOZqimbSsxT5GyhxXOmDtzQ6ox";
	
	private TwitterThing(Node node) {
		this.node = node;
	}
	
	public static void start(Node parent) {
		Node node = parent.createChild("twitter").build();
		final TwitterThing twit = new TwitterThing(node);
		twit.init();
	}
	
	private void init() {
		err = node.createChild("errors").build();
		Action act = new Action(Permission.READ, new ConnectHandler());
		act.addParameter(new Parameter("username", ValueType.STRING));
		node.createChild("connect").setAction(act).build();
			
	}
	
	private void saveAccessToken(File file) {
		if (accessToken == null) {
            return;
        }
        try {
            FileOutputStream fileOut = new FileOutputStream(file);
            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(accessToken);
            objectOut.close();
        } catch (IOException e) {
            String msg = "IOException while saving accessToken.";
            System.out.println(msg);
        }
	}
	
	private void loadAccessToken(File file) {
        if (file.exists()) {
            try {
                FileInputStream fileIn = new FileInputStream(file);
                ObjectInputStream objectIn = new ObjectInputStream(fileIn);
                accessToken = (AccessToken) objectIn.readObject();
                objectIn.close();
            } catch (IOException e) {
                String msg = "IOException while loading accessToken.";
                System.out.println(msg);
            } catch (ClassNotFoundException e) {
                String msg = "ClassNotFoundException while loading accessToken.";
                System.out.println(msg);
            }
        }
	}
	
	
	private void connect() {
	
		NodeBuilder builder = node.createChild("startSampleStream");
		builder.setAction(new Action(Permission.READ, new SampleHandler()));
		builder.build();
		
		builder = node.createChild("startFilteredStream");
		Action act = new Action(Permission.READ, new FilterHandler());
		act.addParameter(new Parameter("streamName", ValueType.STRING));
		act.addParameter(new Parameter("locations", ValueType.STRING));
		act.addParameter(new Parameter("track", ValueType.STRING));
		act.addParameter(new Parameter("follow", ValueType.STRING));
		act.addParameter(new Parameter("count", ValueType.NUMBER));
		act.addParameter(new Parameter("language", ValueType.STRING));
		builder.setAction(act);
		builder.build();
		
		builder = node.createChild("updateStatus");
		act = new Action(Permission.READ, new PostHandler());
		act.addParameter(new Parameter("status", ValueType.STRING));
		builder.setAction(act);
		builder.build();
		
		
		
	}
	
	private class ConnectHandler implements Handler<ActionResult> {
		public void handle(ActionResult event){
			
			String username = event.getParameter("username", ValueType.STRING).getString();
			userPath = "C:/dgtwitbot/userdata/"+username;
			File userFile = new File(userPath);
			
			clearErrorMsgs();
			
			if (!userFile.exists()) {
				userFile.setWritable(true);
				if (userFile.mkdirs()) {
					System.out.println("made a user dir");
				}
			} else {
				File tokenFile = new File(userPath+"/accessToken.ser");
				loadAccessToken(tokenFile);
			}		
			
			NodeBuilder builder = node.createChild("logout");
			builder.setAction(new Action(Permission.READ, new LogoutHandler()));
			builder.build();
			
			builder = node.createChild("deleteUserAccount");
			builder.setAction(new Action(Permission.READ, new AccountDeleteHandler()));
			builder.build();
			
			node.removeChild("connect");
			
			if (accessToken != null) {
				ConfigurationBuilder cb = new ConfigurationBuilder();
				cb.setDebugEnabled(true)
	  			.setOAuthConsumerKey(consumerKey)
	  			.setOAuthConsumerSecret(consumerSecret)
	  			.setOAuthAccessToken(accessToken.getToken())
	  			.setOAuthAccessTokenSecret(accessToken.getTokenSecret())
	  			.setJSONStoreEnabled(true);		
				TwitterFactory tf = new TwitterFactory(cb.build());
				twitter = tf.getInstance();
				connect();
				return;
			}
			
			builder = node.createChild("Authorization URL");
			builder.setValue(new Value(""));
			Node auth = builder.build();
			
			ConfigurationBuilder cb = new ConfigurationBuilder();
			cb.setDebugEnabled(true)
  			.setOAuthConsumerKey(consumerKey)
  			.setOAuthConsumerSecret(consumerSecret)
  			.setJSONStoreEnabled(true);		
			TwitterFactory tf = new TwitterFactory(cb.build());
			twitter = tf.getInstance();
			try {
				RequestToken requestToken = twitter.getOAuthRequestToken();
				auth.setValue(new Value(requestToken.getAuthorizationURL()));
				
				Action act = new Action(Permission.READ, new AuthHandler(requestToken));
				act.addParameter(new Parameter("pin", ValueType.STRING));
				node.createChild("authorize").setAction(act).build();
				
			} catch (TwitterException e) {
				e.printStackTrace();
			}
			
		}	
	}
	
	private class AuthHandler implements Handler<ActionResult> {
		
		private RequestToken reqToken;
		
		public AuthHandler(RequestToken requestToken) {
			this.reqToken = requestToken;
		}
		public void handle(ActionResult event) {
			String authpin = event.getParameter("pin", ValueType.STRING).getString();
			try{
				if(authpin.length() > 0){
					accessToken = twitter.getOAuthAccessToken(reqToken, authpin);
		        }else{
		        	accessToken = twitter.getOAuthAccessToken();
		        }
				saveAccessToken(new File(userPath+"/accessToken.ser"));

				clearErrorMsgs();
				twitter.setOAuthAccessToken(accessToken);
				node.removeChild("authorize");
				connect();
		    } catch (TwitterException te) {
		    	if(401 == te.getStatusCode()){
		    		NodeBuilder builder = err.createChild("authorization error message");
					builder.setValue(new Value("401: Unable to get the access token."));
					builder.build();
		    		System.out.println("Unable to get the access token.");
		        }else{
		        	NodeBuilder builder = err.createChild("authorization error message");
					builder.setValue(new Value("Unable to get the access token."));
					builder.build();
		        	te.printStackTrace();
		        }
		    }
			
		}
		
	}
	
	private class FilterHandler implements Handler<ActionResult> {
		public void handle(ActionResult event) {
			ValueType type = ValueType.STRING;
			String name = event.getParameter("streamName", type).getString();
			String alllocstrings = event.getParameter("locations", type).getString();
			String alltrack = event.getParameter("track", type).getString();
			String allfollowstrings = event.getParameter("follow", type).getString();
			Number count = event.getParameter("count", ValueType.NUMBER).getNumber();
			String alllang = event.getParameter("language", type).getString();
			
			Node stream = node.createChild(name).build();
			Node Atweet = stream.createChild("someTweet").build();
			NodeBuilder builder = Atweet.createChild("rawJSON");
			builder.setValue(new Value(""));
			builder.build();
			ConfigurationBuilder cb = new ConfigurationBuilder();
			cb.setDebugEnabled(true)
	  			.setOAuthConsumerKey(consumerKey)
	  			.setOAuthConsumerSecret(consumerSecret)
	  			.setOAuthAccessToken(accessToken.getToken())
	  			.setOAuthAccessTokenSecret(accessToken.getTokenSecret())
	  			.setJSONStoreEnabled(true);
			TwitterStreamFactory tsf = new TwitterStreamFactory(cb.build());
			TwitterStream twitterStream = tsf.getInstance();
			BasicListener listener = new BasicListener(Atweet);
			twitterStream.addListener(listener);
			openStreams.add(twitterStream);
			
			stream.createChild("endStream").setAction(new Action(Permission.READ, new EndStreamHandler(stream, twitterStream))).build();
			
			FilterQuery fq = new FilterQuery();
			boolean locsSet = (alllocstrings != null && alllocstrings.length() > 0);
			boolean trackSet = (alltrack != null && alltrack.length() > 0);
			boolean followSet = (allfollowstrings != null && allfollowstrings.length() > 0);
			if ((!locsSet) && (!trackSet) && (!followSet)) {
				builder = err.createChild("filter error message");
				builder.setValue(new Value("Must specify at least one of locations, track, and follow"));
				builder.build();
			}
			if (locsSet) {
				String[] locstrings = alllocstrings.split(";");
				int loclength = locstrings.length;
				double[][] locs = new double[loclength][];
				for (int i=0; i<loclength; i++) {
					String[] innerLocstrings = locstrings[i].split(",");
					int innerlength = innerLocstrings.length;
					double[] inner = new double[innerlength];
					for (int j=0; j<innerlength; j++) {
						inner[j] = Double.parseDouble(innerLocstrings[j]);
					}
					locs[i] = inner;
				}
				fq.locations(locs);
			}
			if (trackSet) {
				String[] track = alltrack.split(",");
				fq.track(track);
			}
			if (followSet) {
				String[] followstrings = allfollowstrings.split(",");
				int followlength = followstrings.length;
				long[] follow = new long[followlength];
				for (int i=0; i<followlength; i++) {
					follow[i] = Long.parseLong(followstrings[i]);
				}
				fq.follow(follow);
			}
			if (count != null) {
				fq.count(count.intValue());
			}
			if (alllang != null && alllang.length() > 0) {
				String[] lang = alllang.split(",");
				fq.language(lang);
			}
			twitterStream.filter(fq);
		}
	}
	
	private class SampleHandler implements Handler<ActionResult> {
		public void handle(ActionResult event) {
			Node stream = node.createChild("sampleStream").build();
			Node Atweet = stream.createChild("someTweet").build();
			NodeBuilder builder = Atweet.createChild("rawJSON");
			builder.setValue(new Value(""));
			builder.build();
						
			ConfigurationBuilder cb = new ConfigurationBuilder();
			cb.setDebugEnabled(true)
	  			.setOAuthConsumerKey(consumerKey)
	  			.setOAuthConsumerSecret(consumerSecret)
	  			.setOAuthAccessToken(accessToken.getToken())
	  			.setOAuthAccessTokenSecret(accessToken.getTokenSecret())
	  			.setJSONStoreEnabled(true);
			TwitterStreamFactory tsf = new TwitterStreamFactory(cb.build());
			TwitterStream twitterStream = tsf.getInstance();
			BasicListener listener = new BasicListener(Atweet);
			twitterStream.addListener(listener);
			openStreams.add(twitterStream);
			
			stream.createChild("endStream").setAction(new Action(Permission.READ, new EndStreamHandler(stream, twitterStream))).build();
			
			twitterStream.sample();
		}
	}
	
	private class LogoutHandler implements Handler<ActionResult> {
		public void handle(ActionResult event) {
			logout();
		}
	}
	
	private void logout() {
		twitter = null;
		accessToken = null;
		userPath = null;
		for (TwitterStream ts: openStreams) {
			ts.cleanUp();
			ts.shutdown();
			openStreams.remove(ts);
		}
		for (Node child: node.getChildren().values()) {
			node.removeChild(child);
		}
		init();
	}
	
	private class AccountDeleteHandler implements Handler<ActionResult> {
		public void handle (ActionResult event) {
			File toDelete = new File(userPath);
			boolean failed = false;
			for (File f: toDelete.listFiles()) {
				if (!f.delete()) {
					failed = true;
				}
			}
			if (failed || !toDelete.delete()) {
				logout();
				NodeBuilder builder = err.createChild("deletion error message");
				builder.setValue(new Value("Account deletion failed. logging out."));
				builder.build();
			} else {
				logout();
			}
		}
	}
	
	private void clearErrorMsgs() {
		if (err.getChildren() == null) return;
		for (Node child: err.getChildren().values()) {
			err.removeChild(child);
		}
	}
	
	private class PostHandler implements Handler<ActionResult> {
		public void handle (ActionResult event) {
			String status = event.getParameter("status", ValueType.STRING).getString();
			try {
				twitter.updateStatus(status);
			} catch (TwitterException e) {
				NodeBuilder builder = err.createChild("post error message");
				builder.setValue(new Value("Error updating status"));
				builder.build();
				e.printStackTrace();
			}
		}
	}
	
	private class EndStreamHandler implements Handler<ActionResult> {
		
		private Node snode;
		private TwitterStream ts;
		
		public EndStreamHandler(Node streamNode, TwitterStream stream) {
			snode = streamNode;
			ts = stream;
		}
		
		public void handle (ActionResult event) {
			ts.cleanUp();
			ts.shutdown();
			openStreams.remove(ts);
			node.removeChild(snode);
		}
	}

}
