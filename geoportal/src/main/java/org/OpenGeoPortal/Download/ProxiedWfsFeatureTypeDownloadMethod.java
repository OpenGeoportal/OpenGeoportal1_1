package org.OpenGeoPortal.Download;

public class ProxiedWfsFeatureTypeDownloadMethod extends
		WfsFeatureTypeDownloadMethod implements UnpackagedDownloadMethod {
	private String proxyTo;
	@Override
	public String getUrl(){
		return this.proxyTo;
	}
	public String getProxyTo() {
		return proxyTo;
	}
	public void setProxyTo(String proxyTo) {
		this.proxyTo = proxyTo;
	};
}
