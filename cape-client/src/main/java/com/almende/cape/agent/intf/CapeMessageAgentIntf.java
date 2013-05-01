package com.almende.cape.agent.intf;

import java.util.List;
import java.util.Set;

import com.almende.eve.agent.AgentInterface;
import com.almende.eve.agent.annotation.Name;
import com.almende.eve.agent.annotation.Required;
import com.almende.cape.entity.Message;

public interface CapeMessageAgentIntf extends AgentInterface {

	public void onMessage(@Name("message") Message message) throws Exception;	
	public Message sendMessage(@Name("message") String message, 
								@Name("subject") @Required(false) String subject, 
								@Name("receivers") Set<String> receivers) throws Exception;
	public void dispatch (@Name("userId") String userId, @Name("message") Message message);
	public List<Message> getInbox (
			@Name("since") @Required(false) String since,
			@Name("status") @Required(false) String status,
			@Name("limit") @Required(false) Integer limit) throws Exception;
	public List<Message> getOutbox (
			@Name("since") @Required(false) String since,
			@Name("status") @Required(false) String status,
			@Name("limit") @Required(false) Integer limit) throws Exception;
	public List<Message> getAllMessages (
			@Name("since") @Required(false) String since,
			@Name("status") @Required(false) String status,
			@Name("limit") @Required(false) Integer limit) throws Exception;

	public void deleteMessages() throws Exception;
}