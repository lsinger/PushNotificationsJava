package org.singr.push;

import org.singr.push.PushNotification.PushNotificationException;
import org.singr.push.PushNotification.Sound;
import org.singr.push.PushNotification.Urgency;


public class ExampleClient {
	public static void main(String[] args) {
		String apiKey = "put your API key here";
		
		System.out.println("Sending Notification ...");
		PushNotification pn = new PushNotification("Important Notice", "Please wash the dishes!");
		pn.setActionLabel("view");
		pn.setActionUrl("http://www.youtube.com/watch?v=c9BA5e2Of_U");
		//pn.setIsSilent(true);
		pn.setUrgency(Urgency.HIGH);
		pn.setSound(Sound.Sound3);
		try {
			System.out.println("Sent notification with ID: " + pn.send(apiKey));
		} catch (PushNotificationException e) {
			e.printStackTrace();
		}
		System.out.println("Done.");
	}
}
