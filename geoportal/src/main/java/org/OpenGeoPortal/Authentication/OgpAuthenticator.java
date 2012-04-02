package org.OpenGeoPortal.Authentication;

import javax.servlet.http.HttpServletRequest;

public interface OgpAuthenticator {
	boolean authenticate(String username, String password);
	boolean isAuthenticated(HttpServletRequest request);
}
