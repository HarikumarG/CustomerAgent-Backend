package com.web.servermodel;

public class ServerModel {
	
	String type;
	String isAgent;
	String name;
	String to;
	String message;
	
	public ServerModel() {}
	
	public ServerModel(String Type,String IsAgent,String Name,String To,String Message) {
		this.type = Type;
		this.isAgent = IsAgent;
		this.name = Name;
		this.to = To;
		this.message = Message;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getIsAgent() {
		return isAgent;
	}
	public void setIsAgent(String isAgent) {
		this.isAgent = isAgent;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getTo() {
		return to;
	}
	public void setTo(String to) {
		this.to = to;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
}
