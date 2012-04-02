package org.OpenGeoPortal.Download;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.OpenGeoPortal.Authentication.OgpAuthenticator;
import org.springframework.web.HttpRequestHandler;

public class DownloadServlet implements HttpRequestHandler {
	private DownloadHandler downloadHandler;
	private OgpAuthenticator ogpAuthenticator;



	public OgpAuthenticator getOgpAuthenticator() {
		return ogpAuthenticator;
	}



	public void setOgpAuthenticator(OgpAuthenticator ogpAuthenticator) {
		this.ogpAuthenticator = ogpAuthenticator;
	}



	public DownloadHandler getDownloadHandler() {
		return downloadHandler;
	}



	public void setDownloadHandler(DownloadHandler downloadHandler) {
		this.downloadHandler = downloadHandler;
	}



	@Override
	public void handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		// do data validation here
		 /**
		 * This servlet should receive a POST request with an object containing 
		 * all the info needed for each layer to be downloaded.  The servlet calls a class 
		 * that handles all of the download logic.  Additionally, it checks the session
		 * variable "username" to see if the user has authenticated.  A boolean is passed
		 * to the download code.  If one has been provided, an email address is passed to the 
		 * download code to accomodate systems that email a link to the user for their layers
		 *
		 * @author Chris Barnett
		 */

		if (request.getMethod() != "POST"){
			//only allow POST requests
			response.setStatus(403);
		}

		  //do data validation here
		String layerIds = request.getParameter("layers");
		String emailAddress = request.getParameter("email");
		String bboxJoined = request.getParameter("bbox");
		String[] bbox = bboxJoined.split(",");
		String[] layers = layerIds.split("&");
		Map<String,String> layerMap = new HashMap<String,String>();
		for (int i = 0; i < layers.length; i++){
			String[] layerInfo = layers[i].split("=");
			layerMap.put(layerInfo[0], layerInfo[1]);
		}

		//a better, more general way to handle this might be to pass the request object to downloadHandler
		//also, the authenticator should be able to tell us what institution(s) the user is authenticated for
		downloadHandler.setLocallyAuthenticated(ogpAuthenticator.isAuthenticated(request));
		downloadHandler.setReplyEmail(emailAddress);
		downloadHandler.requestLayers(layerMap, bbox);
		
		response.setContentType("application/json");
		//return a link to the zip file, or info to create link  
		response.getWriter().write(downloadHandler.getJsonResponse());
	}
}
