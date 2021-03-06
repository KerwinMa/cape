package com.almende.cape;

import com.almende.cape.agent.CapeClientAgent;
import com.almende.cape.handler.NotificationHandler;
import com.almende.cape.handler.StateChangeHandler;
import com.almende.eve.agent.AgentHost;
import com.almende.eve.agent.AgentSignal;
import com.almende.eve.scheduler.RunnableSchedulerFactory;
import com.almende.eve.state.MemoryStateFactory;
import com.almende.eve.transport.xmpp.XmppService;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * CAPE Client interface
 */
public class CapeClient {
	private CapeClientAgent agent = null;
	private AgentHost factory = null;
	
	/**
	 * Constructor
	 * @throws Exception
	 */
	public CapeClient() {
		this(AgentHost.getInstance());
	}
	public CapeClient(AgentHost factory){
		if (factory == null) {
			factory = AgentHost.getInstance();
		}
		
		factory.setStateFactory(new MemoryStateFactory());
		factory.setSchedulerFactory(new RunnableSchedulerFactory(factory, ".runnablescheduler"));

		String host = "openid.almende.org";
		Integer port = 5222;
		String service = host;
		XmppService xmppService = new XmppService(factory,host, port, service);
		factory.addTransportService(xmppService);
		this.factory=factory;
	}
	/**
	 * Login to CAPE
	 * @param username
	 * @param password
	 * @throws Exception 
	 */
	public void login(String username, String password) throws Exception {
		// ensure we are logged out
		logout();
		
		// create a user agent if not existing
		CapeClientAgent agent = (CapeClientAgent) factory.getAgent(username);
		if (agent == null) {
			agent = (CapeClientAgent) factory.createAgent(
					CapeClientAgent.class, username);
			
			String resource = "client";
			agent.setAccount(username, password, resource);
		}
		
		// connect to xmpp service
		agent.connect();
		
		// no exceptions thrown
		this.agent = agent;
	}
	
	/**
	 * Logout from CAPE
	 * @throws Exception 
	 */
	public void logout() throws Exception {
		if (agent != null) {
			// remove any handlers
			agent.removeNotificationHandler();
			agent.removeStateChangeHandlers();
			
			// disconnect from xmpp service
			agent.disconnect();

			agent.signalAgent(new AgentSignal<Void>("destroy",null));
			agent = null;
		}
	}
	
	/**
	 * Send a notification to the logged in user
	 * @param message
	 * @throws Exception 
	 */
	public void sendNotification(String message) throws Exception {
		agent.sendNotification(null, message);
	}

	/**
	 * Send a notification to a user
	 * @param userId
	 * @param message
	 * @throws Exception 
	 */
	public void sendNotification(String userId, String message) 
			throws Exception {
		if (agent == null) {
			throw new Exception("Not logged in");
		}
		agent.sendNotification(userId, message);
	}

	/**
	 * Set a handler for incoming notifications
	 * @param handler
	 * @throws Exception 
	 */
	public void onNotification(NotificationHandler handler) 
			throws Exception {
		if (agent == null) {
			throw new Exception("Not logged in");
		}
		agent.setNotificationHandler(handler);
	}
	/**
	 * Set a handler to be triggered when a users state changes
	 * @param notificationHandler
	 * @throws Exception 
	 */
	public void onStateChange(String state, StateChangeHandler handler) 
			throws Exception {
		String userId = null;
		onStateChange(userId, state, handler);
	}
	
	/**
	 * Set a handler to be triggered when a users state changes
	 * @param notificationHandler
	 * @throws Exception 
	 */
	public void onStateChange(String userId, String state, 
			StateChangeHandler handler) throws Exception {
		if (agent == null) {
			throw new Exception("Not logged in");
		}
		agent.addStateChangeHandler(userId, state, handler);
	}
	
	/**
	 * Retrieve contacts
	 * @param filter
	 * @return contacts
	 * @throws Exception 
	 */
	public ArrayNode getContacts(ObjectNode filter) throws Exception {
		if (agent == null) {
			throw new Exception("Not logged in");
		}
		return agent.getContacts(filter);
	}
	
	@Override
	public void finalize () {
		try {
			logout();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

}
