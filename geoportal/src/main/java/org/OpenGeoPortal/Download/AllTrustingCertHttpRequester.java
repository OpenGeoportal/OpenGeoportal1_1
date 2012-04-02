package org.OpenGeoPortal.Download;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * creates a https connection where the certificate is always trusted.  This is needed for MIT's
 * system, which requests, but does not require a certificate.  Of course, don't use this unless you
 * trust the remote system.
 * 
 * @author chris
 *
 */
public class AllTrustingCertHttpRequester extends GenericHttpRequester
		implements HttpRequester {
	public AllTrustingCertHttpRequester(){
		this.handleCertificates();
	}
	
	private void handleCertificates() {
		// MIT's server requests but does not require certificates for public layers
		//
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[]{
				new X509TrustManager() {
					public java.security.cert.X509Certificate[] getAcceptedIssuers() {
						return null;
					}
					public void checkClientTrusted(
							java.security.cert.X509Certificate[] certs, String authType) {
					}
					public void checkServerTrusted(
							java.security.cert.X509Certificate[] certs, String authType) {
					}
				}
		};

		// Install the all-trusting trust manager
		try {
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception e) {
			//System.out.println("handleCertificates threw an exception");
		}
		// Now you can access an https URL without having the certificate in the truststore
	}
}
