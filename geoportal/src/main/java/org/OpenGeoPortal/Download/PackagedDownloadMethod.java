package org.OpenGeoPortal.Download;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface PackagedDownloadMethod extends DownloadMethod{
	InputStream getResponseStream(List<RequestedLayer> layerList)
	throws IOException;
}
