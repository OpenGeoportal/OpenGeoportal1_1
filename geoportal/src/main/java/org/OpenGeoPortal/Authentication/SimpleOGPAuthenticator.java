package org.OpenGeoPortal.Authentication;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class SimpleOGPAuthenticator implements OgpAuthenticator {
	private AuthenticationMethod authenticationMethod;
	
	public AuthenticationMethod getAuthenticationMethod() {
		return authenticationMethod;
	}

	public void setAuthenticationMethod(AuthenticationMethod authenticationMethod) {
		this.authenticationMethod = authenticationMethod;
	}
	
	@Override
	public boolean isAuthenticated(HttpServletRequest request) {
		boolean authenticated = false;
		HttpSession session = request.getSession(false);
		if (session != null) {
			while (session.getAttributeNames().hasMoreElements()) {
				Object attribute = session.getAttributeNames().nextElement();
				//System.out.println(attribute);
				if (attribute.equals("username")) {
					authenticated = true;
					break;
				}
			}
		}
		return authenticated;
	}

	@Override
	public boolean authenticate(String username, String password) {
		// TODO Auto-generated method stub
		return authenticationMethod.authenticate(username, password);
	}


}
