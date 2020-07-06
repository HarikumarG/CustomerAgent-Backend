package com.web.pushnotification;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.PasswordAuthentication;

public class Mailer {
	
	public static void sendMail(String to,String agentname,String username) {
		
		Properties props = new Properties();
		props.put("mail.smtp.auth","true");
		props.put("mail.smtp.starttls.enable","true");
		props.put("mail.smtp.host",MailerConstants.host);
		props.put("mail.smtp.port",MailerConstants.port);
		
		Session session = Session.getInstance(props,new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(MailerConstants.fromUser,MailerConstants.password);
			}
		});
		
		try {
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(MailerConstants.mailid));
			message.setRecipients(Message.RecipientType.TO,InternetAddress.parse(to));
			message.setSubject("Waiting Customer");
			message.setText("Hi "+agentname+",\nThis customer "+username+" is waiting for you to connect."
					+ "\nPlease try to connect with in 5 min.\n\n\nRegards,\nAgent Team");
			Transport.send(message);
			System.out.println("Mail sent to "+agentname+" successfully");
			
		} catch(MessagingException e) {
			System.out.println(e.getMessage());
		}
	}
}
