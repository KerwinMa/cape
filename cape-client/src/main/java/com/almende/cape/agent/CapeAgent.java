package com.almende.cape.agent;

import java.net.URI;
import java.util.logging.Logger;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentHost;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.annotation.Required;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.eve.state.State;
import com.almende.eve.transport.xmpp.XmppService;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The abstract class CapeServiceAgent contains methods to set an xmpp account
 * and to register itself with the merlin agent.
 */
public abstract class CapeAgent extends Agent {
	// TODO: do not hard-code the merlin agent url.
	protected static URI MERLIN_URL = URI.create("xmpp:merlin@openid.almende.org");

	/**
	 * Set xmpp account for the agent. Only applicable when no account has been
	 * set yet (in that case, use changeAccount instead).
	 * 
	 * @param username
	 * @param password
	 * @param resource
	 * @throws Exception
	 */
	public void setAccount(@Name("username") String username,
			@Name("password") String password, @Name("resource") String resource)
			throws Exception {
		changeAccount(null, null, username, password, resource);
	}

	// TODO: remove this method getEverything!!!
	public Object getEverything() {
		return getState();
	}

	/**
	 * Update the current xmpp account
	 * 
	 * @param oldUsername
	 * @param newUsername
	 * @param oldPassword
	 * @param newPassword
	 * @throws Exception
	 */
	public void changeAccount(
			@Name("oldUsername") @Required(false) String oldUsername,
			@Name("oldPassword") @Required(false) String oldPassword,
			@Name("newUsername") String newUsername,
			@Name("newPassword") String newPassword,
			@Name("newResource") String newResource) throws Exception {
		// TODO: do not store password! (at least not plain text)

		if (verifyAccount(newUsername, newPassword)) {
			if (oldUsername != null && oldPassword != null) {
				disconnect();
			}

			State context = getState();
			context.put("xmppUsername", newUsername);
			context.put("xmppPassword", newPassword);
			if (newResource != null) {
				context.put("xmppResource", newResource);
			} else {
				context.remove("xmppResource");
			}
			connect();
		} else {
			throw new Exception("Cannot change account: "
					+ "old username or password does not match");
		}
	}

	/**
	 * Remove current xmpp account
	 * 
	 * @param username
	 * @param password
	 * @throws Exception
	 */
	public void removeAccount(@Name("username") String username,
			@Name("password") String password) {
		if (verifyAccount(username, password)) {
			try {
				disconnect();
			} catch (Exception e) {
			}

			State context = getState();
			context.remove("xmppUsername");
			context.remove("xmppPassword");
			context.remove("xmppResource");
		}
	}

	/**
	 * Check whether given username and password match the stored username and
	 * password. Returns true when no username and password have been saved.
	 * 
	 * @param username
	 * @param password
	 */
	protected boolean verifyAccount(String username, String password) {
		State context = getState();
		String currentUsername = context.get("xmppUsername", String.class);
		String currentPassword = context.get("xmppPassword", String.class);
		if (currentUsername == null)
			System.err.println("CurrentUsername is null");
		if (currentPassword == null)
			System.err.println("currentPassword is null");
		System.err.println("Compare:'" + currentUsername + "'/'"
				+ currentPassword + "' to '" + username + "'/'" + password
				+ "'");
		return (currentUsername == null || currentUsername.equals(username))
				&& (currentPassword == null || currentPassword.equals(password));
	}

	/**
	 * Connect the agent to the xmpp server
	 * 
	 * @throws Exception
	 */
	public void connect() throws Exception {
		AgentHost factory = getAgentHost();
		XmppService service = (XmppService) factory.getTransportService("xmpp");
		if (service != null) {
			State context = getState();
			String username = context.get("xmppUsername", String.class);
			String password = context.get("xmppPassword", String.class);
			String resource = context.get("xmppResource", String.class);
			if (username == null) {
				throw new Exception(
						"Cannot connect: no username set. "
								+ "Please set a username and password using the method setAccount first.");
			}
			if (password == null) {
				throw new Exception(
						"Cannot connect: no password set for user '"
								+ username
								+ "'. "
								+ "Please set a username and password using the method setAccount first.");
			}
			try {
				disconnect();
			} catch (Exception e) {
			}

			service.connect(getId(), username, password, resource);
		} else {
			throw new Exception("No XMPP service registered");
		}
	}

	/**
	 * Disconnect the agent from the xmpp server
	 * 
	 * @throws Exception
	 */
	public void disconnect() throws Exception {
		AgentHost factory = getAgentHost();
		XmppService service = (XmppService) factory.getTransportService("xmpp");
		if (service != null) {
			service.disconnect(getId());
		} else {
			throw new Exception("No XMPP service registered");
		}
	}

	/**
	 * Register the agent as providing data information of a specific type
	 * 
	 * @param dataType
	 * @throws Exception
	 */
	protected void register(@Name("dataType") String dataType) throws Exception {
		String username = getUsername();
		if (username == null) {
			throw new IllegalArgumentException("No username set. "
					+ "Set username and password first using setAccount().");
		}

		String method = "register";
		ObjectNode params = JOM.createObjectNode();
		ObjectNode dataSource = JOM.createObjectNode();
		dataSource.put("userId", username);
		dataSource.put("agentUrl", getXmppUrl());
		dataSource.put("dataType", dataType);
		params.put("dataSource", dataSource);
		// TODO: replace ObjectNode with DataSource
		send(MERLIN_URL, method, params);
	}

	protected String getUsername() {
		return getState().get("xmppUsername", String.class);
	}

	/**
	 * Unregister the agent as providing data information of a specific type
	 * 
	 * @param dataType
	 * @throws Exception
	 */
	protected void unregister(@Name("dataType") String dataType)
			throws Exception {
		String username = getUsername();

		if (username != null) {
			String method = "unregister";
			ObjectNode params = JOM.createObjectNode();
			ObjectNode dataSource = JOM.createObjectNode();
			dataSource.put("userId", username);
			dataSource.put("agentUrl", getXmppUrl());
			dataSource.put("dataType", dataType);
			params.put("dataSource", dataSource);
			// TODO: replace ObjectNode with DataSource
			send(MERLIN_URL, method, params);
		}
	}

	/**
	 * Find a datasource for a specific userid and datatype
	 * 
	 * @param userId
	 * @param dataType
	 * @return contactAgentUrl
	 * @throws Exception
	 */
	@Access(AccessType.UNAVAILABLE)
	public URI findDataSource(String userId, String dataType)
			throws Exception {
		String method = "find";
		ObjectNode params = JOM.createObjectNode();
		params.put("userId", userId);
		params.put("dataType", dataType);

		logger.info("Requesting the MerlinAgent for a dataSource with userId="
				+ userId + " and dataType=" + dataType);

		ArrayNode contactSources = send(MERLIN_URL, method, params,JOM.getSimpleType(ArrayNode.class));
		if (contactSources.size() > 0) {
			ObjectNode contactSource = (ObjectNode) contactSources.get(0);

			logger.info("Retrieved dataSource from MerlinAgent: "
					+ contactSource);

			return URI.create(contactSource.get("agentUrl").asText());
		}
		return null;
	}

	/**
	 * Send a notification to any user
	 * 
	 * @param userId
	 *            can be null
	 * @param message
	 * @throws Exception
	 */
	@Access(AccessType.UNAVAILABLE)
	public void sendNotification(String userId, String message)
			throws Exception {
		if (userId == null) {
			userId = getId();
		}
		String dataType = "dialog";

		// find an agent which can handle a dialog with the user
		URI notificationAgentUrl = findDataSource(userId, dataType);
		if (notificationAgentUrl == null) {
			throw new Exception(
					"No data source found supporting a dialog with user "
							+ userId);
		}

		// send the notification
		String method = "onNotification";
		ObjectNode params = JOM.createObjectNode();
		params.put("message", message);
		send(notificationAgentUrl, method, params);
	}

	/**
	 * Retrieve contacts
	 * 
	 * @param contactFilter
	 * @return contacts
	 * @throws Exception
	 */
	// TODO: replace arrayNode for List<Contact> and ObjectNode with java class
	// Contact?
	@Access(AccessType.UNAVAILABLE)
	public ArrayNode getContacts(ObjectNode filter) throws Exception {
		String userId = getId();
		String dataType = "contacts";
		URI contactAgentUrl = findDataSource(userId, dataType);
		if (contactAgentUrl == null) {
			throw new Exception(
					"No data source found containing contacts of user "
							+ getId());
		}
		// TODO: cache the retrieved data source for some time

		String method = "getContacts";
		ObjectNode params = JOM.createObjectNode();
		String filterStr = (filter != null) ? JOM.getInstance()
				.writeValueAsString(filter) : "";
		params.put("filter", filterStr);
		String contacts = send(contactAgentUrl, method, params,JOM.getSimpleType(String.class));
		ArrayNode array = JOM.getInstance()
				.readValue(contacts, ArrayNode.class);

		return array;
	}

	/**
	 * Get the xmpp url of this agent. Will return null if there is no xmpp url.
	 * 
	 * @return url
	 */
	protected String getXmppUrl() {
		for (String url : getUrls()) {
			if (url.startsWith("xmpp:")) {
				return url;
			}
		}
		return null;
	}

	private Logger logger = Logger.getLogger(this.getClass().getSimpleName());
}
