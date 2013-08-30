package com.almende.cape.entity;

import java.io.Serializable;

public class DataSource implements Serializable {
	private static final long serialVersionUID = 1L;
	public DataSource() {}
	
	public DataSource(String userId, String agentUrl, String dataType) {
		this.userId = userId;
		this.agentUrl = agentUrl;
		this.dataType = dataType;
	}
	
	public String getAgentUrl() {
		return agentUrl;
	}

	public void setAgentUrl(String agentUrl) {
		this.agentUrl = agentUrl;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getDataType() {
		return dataType;
	}

	public void setDataType(String dataType) {
		this.dataType = dataType;
	}
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj.getClass() != getClass()) {
			return false;
		}

		DataSource other = (DataSource) obj;
		return userId.equals(other.userId) && 
				agentUrl.equals(other.agentUrl) &&
				dataType.equals(other.dataType);
	}

	private String userId = null;
	private String agentUrl = null;
	private String dataType = null;
}
