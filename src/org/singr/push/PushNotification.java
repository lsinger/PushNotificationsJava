package org.singr.push;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

/**
 * The <code>PushNotification</code> class represents push notifications you want to send to 
 * an Apple mobile device via the Push Notifications application 
 * (<a href="http://www.appnotifications.com/">http://www.appnotifications.com/</a>). The class 
 * uses the application's public HTTP API. 
 * 
 * Construct a message using one of the constructors and add properties via the provided setters. 
 * For sounds and a message's urgency, you must use the provided enums, <code>PushNotification.Sound</code> 
 * and <code>PushNotification.Urgency</code>, respectively. 
 * 
 * To send a message, call one of the <code>send</code> methods. One takes an API key, the other one takes 
 * a user's email address and password to resolve the API key itself before sending. Resolved API 
 * keys are cached. The cache is local to a JVM instance. 
 * 
 * There is no limit on sending a single <code>PushNotification</code> instance. You could, e.g., 
 * construct a notification and loop over a list of API keys to send the same notification to a list 
 * of users. 
 * 
 * To just retrieve the API key for a user, use the <code>resolveApiKey</code> method. The API key 
 * for a user can also be found in the 
 * <a href="http://www.appnotifications.com/account/rest_api">API section of the Push Notifications website</a>. 
 * 
 * Receivers of push notifications must have the Push Notifications application installed. 
 * 
 * @author Leif Singer
 * @version 1.0, 20100604
 *
 */

public class PushNotification {

	private static final String URL_NOTIFICATIONS = "https://www.appnotifications.com/account/notifications.xml";
	private static final String URL_USER_SESSION = "https://www.appnotifications.com/user_session.xml";
	private String title;
	private String message;
	private String apiKey;
	private String actionLabel;
	private String actionUrl;
	private Urgency urgency;
	private Sound sound;
	private boolean isSilent;
	private static Map<String, String> cachedKeys = new HashMap<String, String>();
	
	/**
	 * Construct a new push notification. 
	 * 
	 * @param title The title of the notification. 
	 * @param message The message to display. 
	 */
	public PushNotification(String title, String message) {
		this.title = title;
		this.message = message;
	}
	
	/**
	 * Construct a new push notification. 
	 * 
	 * @param title The title of the notification. 
	 * @param message The message to display.
	 * @param actionLabel The action label to display ("slide to ...").  
	 */
	public PushNotification(String title, String message, String actionLabel) {
		this.title = title;
		this.message = message;
		this.actionLabel = actionLabel;
	}
	
	/**
	 * Resolve the API key for a given user. 
	 * @param emailAddress The user's email address as registered with http://appnotifications.com/
	 * @param password The user's password. 
	 * @return The API key for the given user. 
	 * @throws PushNotificationException
	 * 		   If connecting wasn't possible or the user's credentials were wrong. 
	 */
	public String resolveApiKey(String emailAddress, String password) throws PushNotificationException {
		if ( PushNotification.cachedKeys.containsKey(emailAddress) ) {
			return PushNotification.cachedKeys.get(emailAddress);
		}
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("user_session[email]", emailAddress);
		params.put("user_session[password]", password);
		String response = this.postToUrl(params, URL_USER_SESSION);
		String apiKey = this.textBetweenTags(response, "<single-access-token>", "</single-access-token>");
		
		if ( apiKey.length() == 0 ) {
			String error = this.textBetweenTags(response, "<ERROR>", "</ERROR>");
			if ( error.length() > 0 ) {
				throw new PushNotificationException("Error resolving API key for user " + emailAddress + " (" + error + ").");
			}
			throw new PushNotificationException("Error resolving API key for user " + emailAddress + ".");
		}
		
		PushNotification.cachedKeys.put(emailAddress, apiKey);
		return apiKey;
	}
	
	private String textBetweenTags(String str, String tagOpen, String tagClose) {
		if ( str == null || tagOpen == null || tagClose == null ) {
			return "";
		}
		if ( !str.contains(tagOpen) || !str.contains(tagClose) ) {
			return "";
		}
		return str.substring(str.indexOf(tagOpen)+tagOpen.length(), str.indexOf(tagClose));
	}

	/**
	 * Send this push notification. 
	 * @param emailAddress The user's email address as registered with http://appnotifications.com/
	 * @param password The user's password. 
	 * @return The ID of the notification that got sent. 
	 * @throws PushNotificationException
	 * 		   If connecting wasn't possible or the user's credentials were wrong. 
	 */
	public int send(String emailAddress, String password) throws PushNotificationException {
		String apiKey = this.resolveApiKey(emailAddress, password);
		return this.send(apiKey);
	}
	
	/**
	 * Sets this notification's action URL, i.e., the URL to open when the user chooses to view 
	 * the notification upon receipt. Per default, this just opens the Push Notifications 
	 * application. You may use regular http / https URLs, but also device specific URLs are 
	 * allowed. E.g. using "tel://", followed by a number, will make a phone device call that 
	 * number when the user chooses to view the notification via the dialog that appears when the 
	 * notification arrives (@see #setActionLabel(String)). 
	 * 
	 * @param actionUrl The action URL. 
	 */
	public void setActionUrl(String actionUrl) {
		this.actionUrl = actionUrl;
	}
	
	/**
	 * Sets this notification's action label, i.e., what is displayed as button when the notification
	 * is received. If the device is locked, this will be preceded by "slide to" -- e.g., 
	 * "slide to chat" for an action label of "chat". 
	 * 
	 * @param actionLabel The action label. 
	 */
	public void setActionLabel(String actionLabel) {
		this.actionLabel = actionLabel;
	}
	
	/**
	 * Sets this notification's urgency, from the <code>PushNotification.Urgency</code> enum.
	 * 
	 * @param urgency The urgency, e.g., Urgency.LOW. 
	 */
	public void setUrgency(Urgency urgency) {
		this.urgency = urgency;
	}
	
	/**
	 * Sets this notification's sound, from the <code>PushNotification.Sound</code> enum. 
	 * 
	 * @param sound The sound, e.g., Sound.Sound1. 
	 */
	public void setSound(Sound sound) {
		this.sound = sound;
	}
	
	/**
	 * Sets whether this notification is silent. 
	 * 
	 * @param isSilent A boolean indicating whether this notification is silent. 
	 */
	public void setIsSilent(boolean isSilent) {
		this.isSilent = isSilent;
	}
	
	/**
	 * Send this push notification. 
	 * @param emailAddress The user's email address as registered with http://appnotifications.com/
	 * @param password The user's password. 
	 * @return The ID of the notification that got sent. 
	 * @throws PushNotificationException
	 * 		   If connecting wasn't possible or the user's credentials were wrong. 
	 */
	public int send(String apiKey) throws PushNotificationException {
		this.apiKey = apiKey;
		int code = -1;
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("notification[message]", this.message);
		params.put("notification[title]", this.title);
		if (!"".equals(this.actionUrl) && null != this.actionUrl) {
			params.put("notification[run_command]", this.actionUrl);
		}
		if (!"".equals(this.actionLabel) && null != this.actionLabel) {
			params.put("notification[action_loc_key]", this.actionLabel);
		}
		if (!"".equals(this.urgency) && null != this.urgency) {
			params.put("notification[message_level]", this.urgency.urgency);
		}
		if (!"".equals(this.sound) && null != this.sound) {
			params.put("notification[sound]", this.sound.filename);
		}
		params.put("notification[silent]", this.isSilent ? "1" : "0");
		params.put("user_credentials", this.apiKey);
		
		String response = this.postToUrl(params, URL_NOTIFICATIONS);
		String codeStr = this.textBetweenTags(response, "<id type=\"integer\">", "</id>");
		if ( codeStr.length() > 0 ) {
			code = Integer.parseInt(codeStr);
		}
		return code;
	}
	
	private String postToUrl(Map<String, String> postParameters, String url) throws PushNotificationException {
		String params = this.parameterMapToString(postParameters);

		DataOutputStream wr;
		StringBuffer response = new StringBuffer();
		try {
			HttpsURLConnection connection = buildConnection(params.getBytes().length, new URL(url));
			wr = new DataOutputStream (connection.getOutputStream());
			wr.writeBytes(params);
			wr.flush();
			wr.close();
			
			InputStream is = connection.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			String line;
			while((line = rd.readLine()) != null) {
				response.append(line);
				response.append('\r');
			}
			rd.close();
			connection.disconnect();
		} catch (IOException e) {
			throw new PushNotificationException("Could not read from URL " + url, e);
		}
		
		return response.toString();
	}

	private String parameterMapToString(Map<String, String> postParameters) throws PushNotificationException {
		StringBuilder sb = new StringBuilder();
		for (String key : postParameters.keySet()) {
			try {
				sb.append("&" + URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(postParameters.get(key), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new PushNotificationException("Could not create post parameter string. ", e);
			}
		}
		sb.deleteCharAt(0);
		return sb.toString();
	}

	private HttpsURLConnection buildConnection(int contentLength, URL url) throws PushNotificationException {
		HttpsURLConnection connection = null;
		try {
			connection = (HttpsURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			connection.setRequestProperty("Content-Length", "" + contentLength);
			connection.setUseCaches (false);
			connection.setDoInput(true);
			connection.setDoOutput(true);
		} catch (IOException e) {
			throw new PushNotificationException("Could not create connection for URL " + url.toExternalForm(), e);
		}
		return connection;
	}

	public enum Urgency {
		LOW("-2"), MODERATE("-1"), NORMAL("0"), HIGH("1"), EMERGENCY("2");

		public String urgency = "0";

		Urgency(String urgency) {
			this.urgency = urgency;
		}

	}

	public enum Sound {
		Sound1("1.caf"), Sound2("2.caf"), Sound3("3.caf"), Sound4("4.caf"), Sound5("5.caf");

		public final String filename;

		Sound(String filename) {
			this.filename = filename;
		}
	}

	public class PushNotificationException extends Exception {

		private static final long serialVersionUID = 1225300822950177308L;

		public PushNotificationException(String message) {
			super(message);
		}
		
		public PushNotificationException(String message, Throwable cause) {
			super(message, cause);
		}
		
	}
}
