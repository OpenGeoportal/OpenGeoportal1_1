package org.OpenGeoPortal.Download;

import java.io.IOException;
import java.net.URLConnection;

public interface HttpRequester {
	URLConnection sendRequest(String serviceURL, String requestString,
			String requestMethod) throws IOException;
	URLConnection sendRequest(String serviceURL, String requestString,
			String requestMethod, String contentType) throws IOException;
}
