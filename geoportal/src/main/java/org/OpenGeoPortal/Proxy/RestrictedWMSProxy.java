package org.OpenGeoPortal.Proxy;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.OpenGeoPortal.Authentication.OgpAuthenticator;

import org.springframework.web.HttpRequestHandler;

public class RestrictedWMSProxy implements HttpRequestHandler {
	private GenericProxy genericProxy;
	private OgpAuthenticator ogpAuthenticator;
	private String proxyTo;

	//this needs to handle authentication if supplied a username and password

	public String getProxyTo() {
		return proxyTo;
	}

	public void setProxyTo(String proxyTo) {
		this.proxyTo = proxyTo;
	}

	public void setGenericProxy(GenericProxy genericProxy) {
		this.genericProxy = genericProxy;
	}

	public GenericProxy getGenericProxy() {
		return this.genericProxy;
	}

	public void setOgpAuthenticator(OgpAuthenticator ogpAuthenticator) {
		this.ogpAuthenticator = ogpAuthenticator;
	}

	public OgpAuthenticator getOgpAuthenticator() {
		return this.ogpAuthenticator;
	}

	@Override
	public void handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		// check authentication
		if (this.ogpAuthenticator.isAuthenticated(request)){
			// forward the request to the protected GeoServer instance
			System.out.println("executing WMS request to protected GeoServer: "
					+ request.getQueryString());
			String remoteUrl = this.proxyTo + "?"
					+ request.getQueryString();
			
			this.genericProxy.proxyRequest(request, response, remoteUrl);
		} else {
			response.sendError(401);
		}

	}
}
