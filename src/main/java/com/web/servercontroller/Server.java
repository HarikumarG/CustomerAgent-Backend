package com.web.servercontroller;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
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
	static HashMap<String,Session> users = new HashMap<String,Session>();
	static LinkedHashMap<String,Session> agents = new LinkedHashMap<String, Session>();
	static HashMap<String,Session> busy = new HashMap<String, Session>();
	static HashMap<String,HashSet<String>> askedlist = new HashMap<String,HashSet<String>>();
	static HashMap<String,HashSet<String>> agentConnectlist = new HashMap<String,HashSet<String>>();
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
		if(sessionisAgent.equals("true")) {
			handleClose(sessionName,"null",sessionisAgent);
		} else {
			handleClose(sessionName,sessionTo,sessionisAgent);
		}
	}
	@OnError
	public void onError(Session session,Throwable throwable) {
		System.out.println("On error called "+session.getUserProperties().get("name")+"-"+throwable.getMessage());
	}
	public void handler(Session session,ServerModel data) {
		switch(data.getType()) {
			case "login": {
				if(data.getName().equals("null") || users.containsKey(data.getName()) || agents.containsKey(data.getName())) {
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
		session.getUserProperties().put("isAgent",data.getIsAgent());
		session.getUserProperties().put("name",data.getName());
		if(data.getIsAgent().equals("false")) {
			session.getUserProperties().put("to","null");
			users.put(data.getName(),session);
			session.getUserProperties().put("agentexist",false);
			if(agents.size() > 0) { 
				System.out.println("RR algo called");
				RRalgo(data.getName(),session);
			} else if(busy.size() == 0){
				ServerModel packet = new ServerModel("noagent",data.getIsAgent(),data.getName(),"null","null");
				sendTo(session,packet);
			} else {
				RRbusyalgo(data.getName(),session);
			}
		} else if(data.getIsAgent().equals("true")){
			agents.put(data.getName(),session);
			askedlist.put(data.getName(),new HashSet<String>());
			agentConnectlist.put(data.getName(),new HashSet<String>());
			System.out.println("Number of agents "+agents.size());
		}
	}
	public void handleLeave(Session session,ServerModel data) {
		System.out.println(data.getName()+" is disconnecting from "+data.getTo());
		//session.getUserProperties().replace("to","null");
		if(agentConnectlist.get(data.getName()).contains(data.getTo())) {
			agentConnectlist.get(data.getName()).remove(data.getTo());
		}
		if(agentConnectlist.get(data.getName()).size() == 0) {
			movefrombusytoavailable(data.getName());
		}
		ServerModel packet = new ServerModel("leave",data.getIsAgent(),data.getName(),data.getTo(),"null");
		sendTo(session,packet);
		if(users.containsKey(data.getTo())) {
			Session conn = users.get(data.getTo());
			conn.getUserProperties().replace("to","null");
			ServerModel temppacket = new ServerModel("leave",(String)conn.getUserProperties().get("isAgent"),
					(String)conn.getUserProperties().get("name"),data.getTo(),"null");			
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
			if(users.containsKey(data.getTo())) {
				Session conn = users.get(data.getTo());
				movefromavailabletobusy(data.getName());
				conn.getUserProperties().replace("to",data.getName());
				//session.getUserProperties().replace("to",data.getTo());
				agentConnectlist.get(data.getName()).add(data.getTo());
				ServerModel agentpacket = new ServerModel("connected",(String)session.getUserProperties().get("isAgent"),
						(String)session.getUserProperties().get("name"),data.getTo(),"null");
				sendTo(session,agentpacket);
				ServerModel userpacket = new ServerModel("connected",(String)conn.getUserProperties().get("isAgent"),
						(String)conn.getUserProperties().get("name"),(String)conn.getUserProperties().get("to"),"null");
				sendTo(conn,userpacket);
			} else {
				System.out.println(data.getTo()+": User is not online");
				ServerModel packet = new ServerModel("leave",data.getIsAgent(),data.getName(),data.getTo(),"null");
				sendTo(session,packet);
				if(fifo.containsKey(data.getName())) {
					Session conns = fifo.get(data.getName());
					if(agentConnectlist.get(data.getName()).size() == 0) {
						agents.put(data.getName(),conns);
						System.out.println(data.getName()+" :This agent is pushed back from fifo to available");
					} else {
						busy.put(data.getName(),conns);
						System.out.println(data.getName()+" :This agent is pushed back from fifo to busy");
					}
					fifo.remove(data.getName());
				}
			}
		} else if(!data.getName().equals("null") && !data.getTo().equals("null") && data.getIsAgent().equals("true") && 
				data.getMessage().equals("no")) {
			System.out.println("Agent rejected the request..Now call for next agent");
			if(users.containsKey(data.getTo())) {
				Session conn = users.get(data.getTo());
				String username = (String)conn.getUserProperties().get("name");
				System.out.println("RR algo again called");
				conn.getUserProperties().replace("agentexist",false);
				if(agents.size() > 0) {
					RRalgo(username,conn);
				} else {
					RRbusyalgo(username,conn);
				}
			} else {
				System.out.println(data.getTo()+": User is not online");
				ServerModel packet = new ServerModel("leave",data.getIsAgent(),data.getName(),data.getTo(),"null");
				sendTo(session,packet);
			}
			if(fifo.containsKey(data.getName())) {
				Session conns = fifo.get(data.getName());
				if(agentConnectlist.get(data.getName()).size() == 0) {
					agents.put(data.getName(),conns);
					System.out.println(data.getName()+" :This agent is pushed back from fifo to available");
				} else {
					busy.put(data.getName(),conns);
					System.out.println(data.getName()+" :This agent is pushed back from fifo to busy");
				}
				fifo.remove(data.getName());
			}
		}
	}
	public void handleClose(String sessionName,String sessionTo,String sessionisAgent) {
		
		if(sessionisAgent.equals("false") && !sessionName.equals("null")) {
			System.out.println(sessionName+" : This Customer is left");
			if(!sessionTo.equals("null")) {
				System.out.println(sessionName+" is disconnecting from "+sessionTo);
				if(busy.containsKey(sessionTo)) {
					Session conn = busy.get(sessionTo);
					String connName = (String)conn.getUserProperties().get("name");
					String connisAgent = (String)conn.getUserProperties().get("isAgent");
					//conn.getUserProperties().replace("to","null");
					if(agentConnectlist.get(connName).contains(sessionName)) {
						agentConnectlist.get(connName).remove(sessionName);
					}
					if(agentConnectlist.get(connName).size() == 0) {
						movefrombusytoavailable(connName);
					}
					ServerModel packet = new ServerModel("leave",connisAgent,connName,sessionName,"null");
					sendTo(conn,packet);
				}
			}
			users.remove(sessionName);
			for(Map.Entry<String,HashSet<String>> agent: askedlist.entrySet()) {
				agent.getValue().remove(sessionName);
			}
		} else if(sessionisAgent.equals("true") && !sessionName.equals("null")) {
			//numberofagents--;
			System.out.println(sessionName+" :This agent is left");
			if(sessionTo.equals("null")) {
				HashSet<String> temp = agentConnectlist.get(sessionName);
				for(String user: temp) {
					System.out.println(sessionName+" is disconnecting from "+user);
					if(users.containsKey(user)) {
						Session conn = users.get(user);
						String connName = (String)conn.getUserProperties().get("name");
						String connisAgent = (String)conn.getUserProperties().get("isAgent");
						conn.getUserProperties().replace("to","null");
						ServerModel packet = new ServerModel("leave",connisAgent,connName,sessionName,"null");
						sendTo(conn,packet);
					}
				}
			}
			if(busy.containsKey(sessionName)) {
				busy.remove(sessionName);
			}
			if(agents.containsKey(sessionName)) {
				agents.remove(sessionName);
			}
			if(fifo.containsKey(sessionName)) {
				fifo.remove(sessionName);
			}
			if(askedlist.containsKey(sessionName)) {
				askedlist.remove(sessionName);
			}
			if(agentConnectlist.containsKey(sessionName)) {
				agentConnectlist.remove(sessionName);
			}
			System.out.println("Number of agents still online "+agents.size());
		}
	}
	
	public void RRalgo(String username,Session session) {
		for(Map.Entry<String,Session> agent: agents.entrySet()) {
			String agentname = agent.getKey();
			Session conn = agent.getValue();
			String connisAgent = (String)conn.getUserProperties().get("isAgent");
			if(connisAgent.equals("true") && askedlist.containsKey(agentname) && !askedlist.get(agentname).contains(username)) {
				session.getUserProperties().replace("agentexist",true);
				askedlist.get(agentname).add(username);
				ServerModel packet = new ServerModel("ask",connisAgent,agentname,username,"null");
				sendTo(conn,packet);
				agents.remove(agentname);
				//numberofagents--;
				fifo.put(agentname,conn);
				break;
			}
		}
		if(!(Boolean)session.getUserProperties().get("agentexist")) {
			ServerModel packet = new ServerModel("busy",(String)session.getUserProperties().get("isAgent")
					,username,"null","null");
			sendTo(session,packet);
		}
	}
	public void RRbusyalgo(String username,Session session) {
		for(Map.Entry<String,Session> agent: busy.entrySet()) {
			String agentname = agent.getKey();
			Session conn = agent.getValue();
			String connisAgent = (String)conn.getUserProperties().get("isAgent");
			if(connisAgent.equals("true") && askedlist.containsKey(agentname) && !askedlist.get(agentname).contains(username)) {
				session.getUserProperties().replace("agentexist",true);
				askedlist.get(agentname).add(username);
				ServerModel packet = new ServerModel("ask",connisAgent,agentname,username,"null");
				sendTo(conn,packet);
				busy.remove(agentname);
				fifo.put(agentname,conn);
				break;
			}
		}
		if(!(Boolean)session.getUserProperties().get("agentexist")) {
			ServerModel packet = new ServerModel("busy",(String)session.getUserProperties().get("isAgent")
					,username,"null","null");
			sendTo(session,packet);
		}
	}
	public void movefromavailabletobusy(String agentname) {
		if(!agentname.equals("null") && !busy.containsKey(agentname) && fifo.containsKey(agentname)) {			
			Session conn = fifo.get(agentname);
			fifo.remove(agentname);
			busy.put(agentname,conn);
			System.out.println("Move successful from available to busy "+agentname);
		} else if(!agentname.equals("null") && busy.containsKey(agentname) && fifo.containsKey(agentname)){
			fifo.remove(agentname);
		} else {
			System.out.println("Move unsuccessful from available to busy");
		}
	}
	public void movefrombusytoavailable(String agentname) {
		if(!agentname.equals("null") && busy.containsKey(agentname) && !agents.containsKey(agentname)) {			
			Session conn = busy.get(agentname);
			busy.remove(agentname);
			agents.put(agentname,conn);
			//numberofagents++;
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