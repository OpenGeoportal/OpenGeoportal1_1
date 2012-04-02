package org.OpenGeoPortal.Proxy;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

public class SimpleGenericProxy implements GenericProxy {
	private HttpEntity responseEntity;

	//break this down into pieces so I can reuse better
	//now that these are pieces, I can separate them into other classes and inject for a modular proxy
	//do I need the request object?
	
	public void proxyRequest(HttpServletRequest request,
			HttpServletResponse response, String remoteUrl){
		this.abstractRequest(request, response, remoteUrl, "copy");
	}
	
	public void abstractRequest(HttpServletRequest request, HttpServletResponse response, String remoteUrl, String action){
		DefaultHttpClient httpclient = new DefaultHttpClient();
		try {

			HttpGet internalRequest = new HttpGet(remoteUrl);
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
		this.abstractRequest(request, response, remoteUrl, "stream");
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
