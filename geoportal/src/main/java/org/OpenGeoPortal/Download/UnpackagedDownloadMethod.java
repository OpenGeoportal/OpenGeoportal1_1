package org.OpenGeoPortal.Download;

import java.io.IOException;
import java.io.InputStream;

public interface UnpackagedDownloadMethod extends DownloadMethod{
	InputStream getResponseStream(RequestedLayer layer)
	throws IOException;
}
