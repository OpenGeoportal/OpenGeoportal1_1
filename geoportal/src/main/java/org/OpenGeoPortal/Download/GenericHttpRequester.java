package org.OpenGeoPortal.Download;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * contains boilerplate code for sending and receiving http GET and POST requests
 * @author chris
 *
 */
public class GenericHttpRequester implements HttpRequester {
	private String requestMethod;
	private String serviceURL;
	private String requestBody;
	//private Map<String, List<String>> responseHeaders;
	
	public String getRequestMethod(){
		return this.requestMethod;
	}

	public String getServiceURL(){
		return this.serviceURL;
	}
	
	/*public String getResponseMIMEType(){
		System.out.println("ResponseHeaders: " + responseHeaders);
		return "application/zip";
	}*/
	
	@Override
	public URLConnection sendRequest(String serviceURL, String requestBody, String requestMethod) throws IOException
	 {
		this.requestMethod = requestMethod;
		this.serviceURL = serviceURL;
		this.setRequestBody(requestBody);
		if (requestMethod.equals("POST")){
			return sendPostRequest("xml");
		} else if (requestMethod.equals("GET")){
			return sendGetRequest();
		} else {
			//throw new Exception("The method " + requestMethod + " is not supported.");
			return null;
		}
	 }
	
	public URLConnection sendRequest(String serviceURL, String requestBody, String requestMethod, String contentType) throws IOException
	 {
		this.requestMethod = requestMethod;
		this.serviceURL = serviceURL;
		this.setRequestBody(requestBody);
		if (requestMethod.equals("POST")){
			if (contentType.equals("xml")){
				return sendPostRequest("xml");
			} else if (contentType.equals("json")){
				return sendPostRequest("json");
			} else {
				return null;
			}
		} else if (requestMethod.equals("GET")){
			return sendGetRequest();
		} else {
			//throw new Exception("The method " + requestMethod + " is not supported.");
			return null;
		}
	 }
	
	private void setRequestBody(String requestBody){
		this.requestBody = requestBody;
	}

	private URLConnection sendPostRequest(String contentType) throws IOException
		{
			String url$ = this.getServiceURL();
			URL url = new URL(url$);
			System.out.println("Sending POST request to " + url$ + ": " + this.requestBody);
			URLConnection urlConnection = url.openConnection();
			urlConnection.setDoOutput(true);
			urlConnection.setDoInput(true);
			if (contentType.equals("xml")){
				urlConnection.setRequestProperty("Content-Type", "text/xml");
			} else if (contentType.equals("json")){
				urlConnection.setRequestProperty("Content-Type", "text/json");
			}

			//--write POST message to the network stream

			DataOutputStream outputStream = new DataOutputStream(urlConnection.getOutputStream());
			outputStream.writeBytes(this.requestBody);
			outputStream.close();
			//this.responseHeaders = urlConnection.getHeaderFields();
			if (urlConnection.getHeaderField(0).indexOf("200") < 0){
				//is there a better way to get the response code?
				throw new IOException("connection error:" + urlConnection.getHeaderField(0).trim());
			}
			return urlConnection;
	}
	
	private URLConnection sendGetRequest()
	 throws IOException, MalformedURLException
		{
			String url$ = this.getServiceURL() + "?" + this.requestBody;
			URL url = new URL(url$);
			System.out.println("Sending GET request to " + url$);
			URLConnection urlConnection = url.openConnection();
			//this.responseHeaders = urlConnection.getHeaderFields();
			if (urlConnection.getHeaderField(0).indexOf("200") < 0){
				//is there a better way to get the response code?
				throw new IOException("connection error:" + urlConnection.getHeaderField(0).trim());
			}	
			return urlConnection;
	}

}
