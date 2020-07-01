package com.web.servercontroller;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.websocket.EncodeException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import com.web.servermodel.ServerModel;

@ServerEndpoint(value = "/chat", encoders = ServerModelEncoder.class,decoders = ServerModelDecoder.class)
public class Server {
	static LinkedHashMap<String,Session> users = new LinkedHashMap<String,Session>();
	static HashMap<String,Session> busy = new HashMap<String, Session>();
	static HashMap<String,Integer> rejectcount = new HashMap<String,Integer>();
	static HashMap<String,Session> fifo = new HashMap<String,Session>();
	static int numberofagents = 0;
	@OnOpen
	public void open(Session s) throws IOException, EncodeException{
		System.out.println("One connection is connected");
	}
	@OnMessage
	public void message(ServerModel m,Session session) throws EncodeException, IOException{
		handler(session,m);
	}
	@OnClose
	public void close(Session session) throws IOException, EncodeException{
		String sessionName = (String)session.getUserProperties().get("name");
		String sessionTo = (String)session.getUserProperties().get("to");
		String sessionisAgent = (String)session.getUserProperties().get("isAgent");		
		handleClose(sessionName,sessionTo,sessionisAgent);	
	}
	@OnError
	public void onError(Session session,Throwable throwable) {
		System.out.println("On error called");
	}
	public void handler(Session session,ServerModel data) {
		switch(data.getType()) {
			case "login": {
				if(data.getName().equals("null") || users.containsKey(data.getName())) {
					System.out.println(data.getName()+": This name already exist");
				} else {
					handleLogin(session,data);
				}
				break;
			}
			case "leave": {
				if(!data.getName().equals("null") && data.getIsAgent().equals("true")) {
					handleLeave(session,data);
				} else {
					System.out.println("Case leave is not called by an agent");
				}
				break;
			}
			case "message": {
				handleMessage(data);
				break;
			}
			case "askresponse": {
				handleAskResponse(session,data);
				break;
			}
			default: {
				System.out.println("No such command exist");
				break;
			}
		}
	}
	public void handleLogin(Session session,ServerModel data) {
		System.out.println("User logged in as "+data.getName()+" as "+data.getIsAgent());
		session.getUserProperties().put("type","null");
		session.getUserProperties().put("isAgent",data.getIsAgent());
		session.getUserProperties().put("name",data.getName());
		session.getUserProperties().put("to","null");
		session.getUserProperties().put("message","null");
		users.put(data.getName(),session);
		if(data.getIsAgent().equals("false")) {
			session.getUserProperties().put("agentexist",false);
			rejectcount.put(data.getName(),0);
			System.out.println("RR algo called");
			RRalgo(data.getName(),session);
		} else {
			numberofagents++;
			System.out.println("Number of agents "+numberofagents);
		}
	}
	public void handleLeave(Session session,ServerModel data) {
		System.out.println(data.getName()+" is disconnecting from "+data.getTo());
		session.getUserProperties().replace("to","null");
		movefrombusytoavailable(data.getName());
		ServerModel packet = new ServerModel("leave",data.getIsAgent(),data.getName(),"null","null");
		sendTo(session,packet);
		if(users.containsKey(data.getTo())) {
			Session conn = users.get(data.getTo());
			conn.getUserProperties().replace("to","null");
			ServerModel temppacket = new ServerModel("leave",(String)conn.getUserProperties().get("isAgent"),
					(String)conn.getUserProperties().get("name"),"null","null");			
			sendTo(conn,temppacket);
		}
	}
	public void handleMessage(ServerModel data) {
		if(data.getIsAgent().equals("true") && !data.getTo().equals("null") && users.containsKey(data.getTo())) {
			System.out.println("Message sent from agent to user");
			Session conn = users.get(data.getTo());
			String username = (String)conn.getUserProperties().get("name");
			if(data.getTo().equals(username)){
				System.out.println("Message sent from "+data.getName()+" to "+username);					
				sendTo(conn,data);
			} else {
				System.out.println("The user is connected with someone else");
			}
		} else if(data.getIsAgent().equals("false") && !data.getTo().equals("null") && busy.containsKey(data.getTo())) {
			System.out.println("Message sent from user to agent");
			Session conn = busy.get(data.getTo());
			String agentname = (String)conn.getUserProperties().get("name");
			if(data.getTo().equals(agentname)) {
				System.out.println("Message sent from "+data.getName()+" to "+agentname);
				sendTo(conn,data);
			} else {
				System.out.println("The agent is connected with someone else");
			}
		} else {
			System.out.println("Send To is not available and isAgent is "+data.getIsAgent());
		}
	}
	public void handleAskResponse(Session session,ServerModel data) {
		if(!data.getName().equals("null") && !data.getTo().equals("null") && data.getIsAgent().equals("true") && 
				data.getMessage().equals("yes")) {
			Session conn = users.get(data.getTo());
			movefromavailabletobusy(data.getName());
			conn.getUserProperties().replace("to",data.getName());
			session.getUserProperties().replace("to",data.getTo());
			ServerModel agentpacket = new ServerModel("connected",(String)session.getUserProperties().get("isAgent"),
					(String)session.getUserProperties().get("name"),(String)session.getUserProperties().get("to"),"null");
			sendTo(session,agentpacket);
			ServerModel userpacket = new ServerModel("connected",(String)conn.getUserProperties().get("isAgent"),
					(String)conn.getUserProperties().get("name"),(String)conn.getUserProperties().get("to"),"null");
			sendTo(conn,userpacket);
		} else if(!data.getName().equals("null") && !data.getTo().equals("null") && data.getIsAgent().equals("true") && 
				data.getMessage().equals("no")) {
			System.out.println("Agent rejected the request..Now call for next agent");
		
			Session conn = users.get(data.getTo());
			String username = (String)conn.getUserProperties().get("name");
			if(rejectcount.containsKey(username)) {
				int count = rejectcount.get(username);
				count++;
				rejectcount.replace(username,count);
				if(count > numberofagents) {
					System.out.println("Count exceeded");
					ServerModel packet = new ServerModel("busy",(String)conn.getUserProperties().get("isAgent")
							,username,"null","null");
					sendTo(conn,packet);
				} else {
					System.out.println("RR algo again called");
					RRalgo(username,conn);
				}
			} else {
				System.out.println("No count exist");
			}
			
			if(fifo.containsKey(data.getName())) {
				System.out.println(data.getName()+" :This agent is pushed back from fifo to available");
				Session conns = fifo.get(data.getName());
				users.put(data.getName(),conns);
				numberofagents++;
				fifo.remove(data.getName());
			}
		}
	}
	public void handleClose(String sessionName,String sessionTo,String sessionisAgent) {
		
		if(sessionisAgent.equals("false") && !sessionName.equals("null")) {
			System.out.println(sessionName+" : This Customer is left");
			if(rejectcount.containsKey(sessionName)) {
				rejectcount.remove(sessionName);
			}
			if(!sessionTo.equals("null")) {
				System.out.println(sessionName+" is disconnecting from "+sessionTo);
				if(busy.containsKey(sessionTo)) {
					Session conn = busy.get(sessionTo);
					String connName = (String)conn.getUserProperties().get("name");
					String connisAgent = (String)conn.getUserProperties().get("isAgent");
					conn.getUserProperties().replace("to","null");
					movefrombusytoavailable(connName);
					ServerModel packet = new ServerModel("leave",connisAgent,connName,sessionName,"null");
					sendTo(conn,packet);
				}
			}
			users.remove(sessionName);
		} else if(sessionisAgent.equals("true") && !sessionName.equals("null")) {
			numberofagents--;
			System.out.println(sessionName+" :This agent is left");
			System.out.println("Number of agents still online "+numberofagents);
			if(!sessionTo.equals("null")) {
				System.out.println(sessionName+" is disconnecting from "+sessionTo);
				if(users.containsKey(sessionTo)) {
					Session conn = users.get(sessionTo);
					String connName = (String)conn.getUserProperties().get("name");
					String connisAgent = (String)conn.getUserProperties().get("isAgent");
					conn.getUserProperties().replace("to","null");
					ServerModel packet = new ServerModel("leave",connisAgent,connName,sessionName,"null");
					sendTo(conn,packet);
				}
			}
			if(busy.containsKey(sessionName)) {
				busy.remove(sessionName);
			}
			if(users.containsKey(sessionName)) {
				users.remove(sessionName);
			}
			if(fifo.containsKey(sessionName)) {
				fifo.remove(sessionName);
			}
		}
	}
	
	public void RRalgo(String username,Session session) {
		String sessionisAgent = (String)session.getUserProperties().get("isAgent");
		for(Map.Entry<String,Session> agent: users.entrySet()) {
			String agentname = agent.getKey();
			Session conn = agent.getValue();
			String connisAgent = (String)conn.getUserProperties().get("isAgent");
			if(connisAgent.equals("true")) {
				session.getUserProperties().replace("agentexist",true);
				ServerModel packet = new ServerModel("ask",connisAgent,agentname,username,"null");
				sendTo(conn,packet);
				users.remove(agentname);
				numberofagents--;
				fifo.put(agentname,conn);
				break;
			}
		}
		if(!(boolean)session.getUserProperties().get("agentexist")) {
			ServerModel packet = new ServerModel("noagent",sessionisAgent,username,"null","null");
			sendTo(session,packet);
		}
	}
	public void movefromavailabletobusy(String agentname) {
		if(!agentname.equals("null") && !busy.containsKey(agentname) && fifo.containsKey(agentname)) {			
			Session conn = fifo.get(agentname);
			fifo.remove(agentname);
			busy.put(agentname,conn);
			System.out.println("Move successful from available to busy "+agentname);
		} else {
			System.out.println("Move unsuccessful from available to busy");
		}
	}
	public void movefrombusytoavailable(String agentname) {
		if(!agentname.equals("null") && busy.containsKey(agentname) && !users.containsKey(agentname)) {			
			Session conn = busy.get(agentname);
			busy.remove(agentname);
			users.put(agentname,conn);
			numberofagents++;
			System.out.println("Move successful from busy to available "+agentname);
		} else {
			System.out.println("Move unsuccessful from busy to available");
		}
	}
	public void sendTo(Session session,ServerModel packet) {
		try {
			session.getBasicRemote().sendObject(packet);
		} catch (IOException e) {
			System.out.println("IOException Error in sendTo :"+e.getMessage());
		} catch (EncodeException e) {
			System.out.println("EncodeException Error in sendTo :"+e.getMessage());
		}
	}
}
