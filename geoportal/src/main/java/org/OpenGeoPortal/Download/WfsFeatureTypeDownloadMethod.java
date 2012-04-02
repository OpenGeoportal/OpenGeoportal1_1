package org.OpenGeoPortal.Download;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class WfsFeatureTypeDownloadMethod implements UnpackagedDownloadMethod {
	final Boolean metadata = false;
	HttpRequester httpRequester;
	RequestedLayer currentLayer;
	
	public void setCurrentLayer(RequestedLayer currentLayer){
		this.currentLayer = currentLayer;
	}
	
	public void setHttpRequester(HttpRequester httpRequester){
		this.httpRequester = httpRequester;
	}
	
	@Override
	public Boolean includesMetadata() {
		return this.metadata;
	}
	
	@Override
	public void validate(RequestedLayer currentLayer) {
		// TODO Auto-generated method stub
	}

	@Override
	public String createDownloadRequest() throws Exception {
		//--generate POST message
		//info needed: geometry column, bbox coords, epsg code, workspace & layername
	 	//all client bboxes should be passed as lat-lon coords.  we will need to get the appropriate epsg code for the layer
	 	//in order to return the file in original projection to the user (will also need to transform the bbox)
		BoundingBox bounds = this.getClipBounds();
		String layerName = this.currentLayer.getLayerNameNS();
		String workSpace = this.currentLayer.workSpace;
		Map<String, String> describeLayerInfo = getWfsDescribeLayerInfo();
		String geometryColumn = describeLayerInfo.get("geometryColumn");
		String nameSpace = describeLayerInfo.get("nameSpace");
		int epsgCode = 4326;
		String bboxFilter = "";
		if (!this.currentLayer.nativeBounds.isEquivalent(bounds)){
			/*
			 *       <ogc:BBOX>
        <ogc:PropertyName>the_geom</ogc:PropertyName>
        <gml:Box srsName="http://www.opengis.net/gml/srs/epsg.xml#4326">
           <gml:coordinates>-75.102613,40.212597 -72.361859,41.512517</gml:coordinates>
        </gml:Box>
      </ogc:BBOX>
			 */
  			bboxFilter += "<ogc:Filter>"
      		+		"<ogc:BBOX>"
        	+			"<ogc:PropertyName>" + geometryColumn + "</ogc:PropertyName>"
        	+			bounds.generateGMLBox(epsgCode)
        	+		"</ogc:BBOX>"
      		+	"</ogc:Filter>";
		}
		// TODO should be xml
		/*<wfs:GetFeature service="WFS" version="1.0.0"
  outputFormat="GML2"
  xmlns:topp="http://www.openplans.org/topp"
  xmlns:wfs="http://www.opengis.net/wfs"
  xmlns:ogc="http://www.opengis.net/ogc"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://www.opengis.net/wfs
                      http://schemas.opengis.net/wfs/1.0.0/WFS-basic.xsd">
  <wfs:Query typeName="topp:states">
    <ogc:Filter>
       <ogc:FeatureId fid="states.3"/>
    </ogc:Filter>
    </wfs:Query>
</wfs:GetFeature>
*/
		String getFeatureRequest = "<wfs:GetFeature service=\"WFS\" version=\"1.0.0\""
			+ " outputFormat=\"shape-zip\""
			+ " xmlns:" + workSpace + "=\"" + nameSpace + "\""
  			+ " xmlns:wfs=\"http://www.opengis.net/wfs\""
  			+ " xmlns:ogc=\"http://www.opengis.net/ogc\""
  			+ " xmlns:gml=\"http://www.opengis.net/gml\""
  			+ " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
  			+ " xsi:schemaLocation=\"http://www.opengis.net/wfs"
            + " http://schemas.opengis.net/wfs/1.0.0/WFS-basic.xsd\">"
  			+ "<wfs:Query typeName=\"" + layerName + "\">"
  			+ bboxFilter
  			+ "</wfs:Query>"
			+ "</wfs:GetFeature>";

    	return getFeatureRequest;
	}

	BoundingBox getClipBounds() {
			return this.currentLayer.nativeBounds.getIntersection(this.currentLayer.requestedBounds);
	}
	
	public String getUrl(){
		return this.currentLayer.getWfsUrl();
	};
	
	 Map<String, String> getWfsDescribeLayerInfo()
	 	throws Exception
	 {
		// TODO should be xml
		/*DocumentFragment requestXML = createDocumentFragment();
		// Insert the root element node
		Element rootElement = requestXML.createElement("DescribeFeatureType");
		requestXML.appendChild(rootElement);*/
		String layerName = this.currentLayer.getLayerNameNS();
	 	String describeFeatureRequest = "<DescribeFeatureType"
	            + " version=\"1.0.0\""
	            + " service=\"WFS\""
	            + " xmlns=\"http://www.opengis.net/wfs\""
	            + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
	            + " xsi:schemaLocation=\"http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.0.0/wfs.xsd\">"
	            + 	"<TypeName>" + layerName + "</TypeName>"
	            + "</DescribeFeatureType>";

		URLConnection responseConnection = this.httpRequester.sendRequest(this.getUrl(), describeFeatureRequest, "POST");
		System.out.println(responseConnection.getContentType());//check content type before doing any parsing of xml?
		InputStream inputStream = responseConnection.getInputStream();

		//parse the returned XML and return needed info as a map
		// Create a factory
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		// Use document builder factory
		DocumentBuilder builder = factory.newDocumentBuilder();
		//Parse the document
		Document document = builder.parse(inputStream);
		//initialize return variable
		Map<String, String> describeLayerInfo = new HashMap<String, String>();

		//get the namespace info
		Node schemaNode = document.getFirstChild();
		if (schemaNode.getNodeName().equals("ServiceExceptionReport")){
			this.handleServiceException(schemaNode);
		}
		try {
			NamedNodeMap schemaAttributes = schemaNode.getAttributes();
			describeLayerInfo.put("nameSpace", schemaAttributes.getNamedItem("targetNamespace").getNodeValue());

			//we can get the geometry column name from here
			NodeList elementNodes = document.getElementsByTagName("xsd:element");
			for (int i = 0; i < elementNodes.getLength(); i++){
				Node currentNode = elementNodes.item(i);
				NamedNodeMap currentAttributeMap = currentNode.getAttributes();
				String attributeValue = null;
				for (int j = 0; j < currentAttributeMap.getLength(); j++){
					Node currentAttribute = currentAttributeMap.item(j);
					String currentAttributeName = currentAttribute.getNodeName();
					if (currentAttributeName.equals("name")){
						attributeValue = currentAttribute.getNodeValue();
					} else if (currentAttributeName.equals("type")){
						if (currentAttribute.getNodeValue().startsWith("gml:")){
							describeLayerInfo.put("geometryColumn", attributeValue);
							break;
						}
					}
				}
			}
			
		} catch (Exception e){
			throw new Exception("error getting layer info: "+ e.getMessage());
		}
		
		return describeLayerInfo;
	 }

	 void handleServiceException(Node schemaNode) throws Exception{
			String errorMessage = "";
			for (int i = 0; i < schemaNode.getChildNodes().getLength(); i++){
				String nodeName = schemaNode.getChildNodes().item(i).getNodeName();
				if (nodeName.equals("ServiceException")){
					errorMessage += schemaNode.getChildNodes().item(i).getTextContent().trim();
				}
			}
			throw new Exception(errorMessage);
	 }
	 
		@Override
		public InputStream getResponseStream(RequestedLayer currentLayer) throws IOException {
			this.currentLayer = currentLayer;
			this.currentLayer.metadata = this.includesMetadata();
			URLConnection responseConnection = null;
			try {
				responseConnection = this.httpRequester.sendRequest(this.getUrl(), this.createDownloadRequest(), "POST");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			this.currentLayer.responseMIMEType = responseConnection.getContentType();
			return responseConnection.getInputStream();
		}
}
