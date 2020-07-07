package com.web.pushnotification;

import com.web.servercontroller.Server;
import com.web.serverdao.ServerDao;

//thread class to send email for agent to come online
public class PushNotification extends Thread {
		
	String user;
	public PushNotification(String username) {
		this.user = username;
	}
	
	@SuppressWarnings("deprecation")
	public void run() {
		ServerDao dao = new ServerDao();
		String[] agent = dao.getAgentName();
		Server s = new Server();
		if(agent[0] == null || agent[1] == null) {
			s.sendNoAgent(user);
			this.stop();
		} else {
			System.out.println(agent[0]);
			System.out.println(agent[1]);
			ServerDao d = new ServerDao();
			d.updateIsAsked(agent[0]);
			Mailer.sendMail(agent[1],agent[0],user);
			try {
				Thread.sleep(60000);
				s.callToConnect(user);
				this.stop();
			} catch (InterruptedException e) {
				System.out.println(e.getMessage());
			}
		}
	}
	
}
