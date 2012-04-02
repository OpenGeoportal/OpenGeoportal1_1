package org.OpenGeoPortal.Download;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.web.HttpRequestHandler; 


/**
 * Servlet implementation class OGPRemoteDownload
 */
public class OGPRemoteDownload implements HttpRequestHandler {
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		// TODO Auto-generated method stub
	}

	/**
	 * @throws Exception 
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doWork(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String layerNames = request.getParameter("layers");
		String emailAddress = request.getParameter("email");
		String[] bbox = request.getParameterValues("bbox");
		String[] layers = layerNames.split("&");
		Map<String,String> layerMap = new HashMap<String,String>();
		for (int i = 0; i < layers.length; i++){
			String[] layerInfo = layers[i].split("=");
			layerMap.put(layerInfo[0], layerInfo[1]);
		}

		/*String username;
		String password;
		Boolean authenticated = LDAP.authenticate(username, password);
		*/
		//no way to authenticate to a remote system for now;
		Boolean authenticated = false;

		BeanFactory injector = new FileSystemXmlApplicationContext("download.xml");
		DownloadHandler downloadHandler = (DownloadHandler) injector.getBean("downloadPackager.ogp.remote");
		downloadHandler.setLocallyAuthenticated(authenticated);
		downloadHandler.setReplyEmail(emailAddress);
		downloadHandler.requestLayers(layerMap, bbox);
		//response.
	}

	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		try {
			this.doWork(request, response);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
