package org.OpenGeoPortal.Proxy;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;

public class AuthenticatingGenericProxy implements GenericProxy {
	private HttpEntity responseEntity;
	private UsernamePasswordCredentials credentials;
	private Boolean useAuthentication;
	private String username;
	private String password;

	//break this down into pieces so I can reuse better
	//now that these are pieces, I can separate them into other classes and inject for a modular proxy
	//do I need the request object?

	public void setUsername(String username) {
		this.setUseAuthentication();
		this.username = username;
	}

	public void setPassword(String password) {
		this.setUseAuthentication();
		this.password = password;
	}

	public void setUseAuthentication(){
		this.useAuthentication = false;
		if ((this.username != null)&&(!this.username.isEmpty())){
			if ((this.password != null)&&(!this.password.isEmpty())){
				//use authentication if a username and password are provided
				this.useAuthentication = true;
			}
		}
	}
	
	public void proxyRequest(HttpServletRequest request,
			HttpServletResponse response, String remoteAddress){
		try {
			this.abstractRequest(request, response, remoteAddress, "copy");
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void abstractRequest(HttpServletRequest request, HttpServletResponse response, String remoteAddress, String action) throws MalformedURLException{
		HttpHost targetHost = new HttpHost(remoteAddress);
		DefaultHttpClient httpclient = new DefaultHttpClient();
		
		if (useAuthentication){
			int port = targetHost.getPort();
			String hostName = targetHost.getHostName();
		
			this.credentials = new UsernamePasswordCredentials(this.username, this.password);

			httpclient.getCredentialsProvider().setCredentials(
    	        new AuthScope(hostName, port), 
    	        this.credentials);
    	
			// Create AuthCache instance
			AuthCache authCache = new BasicAuthCache();
			// Generate BASIC scheme object and add it to the local auth cache
			BasicScheme basicAuth = new BasicScheme();
		
			authCache.put(targetHost, basicAuth);

			// Add AuthCache to the execution context
			BasicHttpContext localcontext = new BasicHttpContext();
			localcontext.setAttribute(ClientContext.AUTH_CACHE, authCache);
		}
		
		try {

			HttpGet internalRequest = new HttpGet(remoteAddress);
			HttpResponse internalResponse = httpclient.execute(internalRequest);
			//internalResponse.getEntity().getContent()
			// copy headers
			//this.copyHeaders(internalResponse, response);

			this.checkStatus(internalResponse, response);

			if (action.equalsIgnoreCase("copy")){
				this.copyResponse(internalResponse, response);
			} else if (action.equalsIgnoreCase("stream")){
				responseEntity = internalResponse.getEntity();
			}
		
			
		} catch (Exception e){
			System.out.println("generic proxy failed");
			System.out.println(e.getMessage());
			try {
				response.getOutputStream().print(e.getMessage());
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			e.getStackTrace();
		} finally {
			// When HttpClient instance is no longer needed,
			// shut down the connection manager to ensure
			// immediate deallocation of all system resources
			httpclient.getConnectionManager().shutdown();
		}
	}
	
	public InputStream getContentStream(HttpServletRequest request, HttpServletResponse response, String remoteUrl){
		try {
			this.abstractRequest(request, response, remoteUrl, "stream");
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			return this.responseEntity.getContent();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public void checkStatus (HttpResponse internalResponse, HttpServletResponse externalResponse) throws IOException{
		if (internalResponse.getStatusLine().getStatusCode() != 200){
			externalResponse.sendError(internalResponse.getStatusLine().getStatusCode());
			System.out.println(internalResponse.getStatusLine());
		}
	}
	
	public void copyHeaders (HttpResponse internalResponse, HttpServletResponse externalResponse){
		Header[] headers = internalResponse.getAllHeaders();
		for (int i = 0; i < headers.length; i++) {
			 //System.out.println(headers[i]);
			if (!headers[i].getName().equals("Content-Disposition")){
				externalResponse.setHeader(headers[i].getName(), headers[i].getValue());
			}
		}
	}
	
	public void copyResponse(HttpResponse internalResponse, HttpServletResponse externalResponse) throws IOException{
		HttpEntity entity = internalResponse.getEntity();
		//System.out.println(entity.getContentType());
		
		OutputStream outputStream = externalResponse.getOutputStream();
		BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
		BufferedInputStream bufferedInputStream = new BufferedInputStream(entity.getContent());

		try {
			int currentBytes;
			while ((currentBytes = bufferedInputStream.read()) != -1) {
				 //System.out.println("Receiving " + currentBytes + " bytes");
				bufferedOutputStream.write(currentBytes);
			} 
		} catch (Exception e){
			e.getStackTrace();
			}
		finally {
			try {
				bufferedInputStream.close();
			} finally {
				bufferedOutputStream.close();
			}
		}

		EntityUtils.consume(entity);
	}
}
