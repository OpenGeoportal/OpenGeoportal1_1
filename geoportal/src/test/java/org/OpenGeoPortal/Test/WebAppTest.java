package org.OpenGeoPortal.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.Parameterized.Parameters;
import static org.junit.Assert.* ;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * Test code for OpenGeoPortal
 * exercise the portal web application using the Selenium 2 API and
 * retrieves elements from the DOM by their id, name and row count in a table
 * @author smcdon08
 *
 */ 

@RunWith(value = org.junit.runners.Parameterized.class) // Parameterized.class)
public class WebAppTest 
{
	
	public enum BrowserType {Firefox, Chrome}
	
	public String url = "http://localhost:8080/geoportal/openGeoPortalHome.jsp";
	//public String url = "http://geoportal-dev.atech.tufts.edu/openGeoPortalHome.jsp";
	//public String url = "http://geoportal-demo.atech.tufts.edu";
	//public String url = "http://geodata.tufts.edu";

	public static Logger logger = null;
	
	
	/** a list of browsers we want to test against */
	 @Parameters
	 public static Collection<Object[]> data() {
	   //Object[][] data = new Object[][] { {BrowserType.Firefox}, {BrowserType.Chrome} };
	   Object[][] data = new Object[][] { {BrowserType.Firefox}};
	   return Arrays.asList(data);
	 }

	
	private static WebDriver webDriver = null;
	private static BrowserType browserType = null;
	
	public WebAppTest(BrowserType passedBrowserType)
	{
		String commandLineUrl = System.getProperty("url");
		if (commandLineUrl != null)
			url = commandLineUrl;
		if ((webDriver == null) || (passedBrowserType != browserType))
		{
			// create the browser for a given type only once, then reuse
			browserType = passedBrowserType;
			webDriver = createWebDriver(browserType);
			webDriver.get(url);
		}
		
		if (logger == null)
		{
			logger = LoggerFactory.getLogger(WebAppTest.class);
		}
	}
	
	
	/**
	 * note: some tests don't work with the HtmlUnitDriver
	 * @return
	 */
	public static WebDriver createWebDriver(BrowserType browserType)
	{
		// first check to see if we have to run headless
		String headless = System.getProperty("headless");
		if (headless != null)
			if (headless.equals("true"))
			{
				
				HtmlUnitDriver htmlDriver = new HtmlUnitDriver();
				htmlDriver.setJavascriptEnabled(true);
				return htmlDriver;
			}
		// here if we are making a real browser
		WebDriver webDriver = null;
		if (browserType == BrowserType.Firefox)
			webDriver =  new FirefoxDriver();
		else if (browserType == BrowserType.Chrome)
			webDriver =  new ChromeDriver(); 
		return webDriver;
	}
	
	/**
	 * see if any search results are displayed
	 * actually, verify the displayed number of search results is not 0
	 * @param webDriver
	 */
	@Test
	public void searchResultsExistTest()
	{
		logger.info("  in searchResultsExistTest");		
		int initialLayerCount = getSearchResultsCount(webDriver);
		if (initialLayerCount != 0)
			fail("search should not run on page load.");
		zoomToWorld(webDriver);
		pause();
		initialLayerCount = getSearchResultsCount(webDriver);
		logger.info(initialLayerCount + " results found");
		if (initialLayerCount == 0)
			fail("no search results found.");
		
	}
	
	/**
	 * return the number of results from the search results count label
	 * @param webDriver
	 * @return
	 */
	public static int getSearchResultsCount(WebDriver webDriver)
	{
		WebElement spanElement = webDriver.findElement(By.id("resultsNumber"));
		String text = spanElement.getText(); 
		String temp = text.replace("(", "");
		temp = temp.replace(")", "");
		int layerCount = Integer.parseInt(temp);
		return layerCount;
	}
	
	
	/**
	 * click on the sort button specified by the passed buttonClass and verify the first result changes
	 * note that changing the sort order isn't theoretically guaranteed to change the first item
	 *   in the list, but practically speaking it is
	 * @param webDriver
	 * @param buttonClass
	 * @param errorMessage
	 */
	@Test
	public void sortByTest()
	{
		sortByTest(webDriver, "colTitle", "Re-sort by name failed");
		sortByTest(webDriver, "colOriginator", "Re-sort by originator failed");
	}
	
	
	private void sortByTest(WebDriver webDriver, String buttonClass, String errorMessage)
	{
		logger.info("  in sortByTest with " + buttonClass);
		String firstTitle = getFirstLayerName(webDriver);
		clickSortByButton(webDriver, buttonClass);
		pause();
		String resortedFirstTitle = getFirstLayerName(webDriver);
		boolean stringsSame = firstTitle.equals(resortedFirstTitle);
		assertFalse(errorMessage, stringsSame);
	}
	
	
	/**
	 * sleep for a brief period so the page has time to update
	 * this is important when the map is changed and both the map and search results
	 *   must be updated
	 */
	private static void pause(int milliseconds)
	{
		try 
		{
			Thread.sleep(milliseconds);
		} 
		catch (InterruptedException e) 
		{
			e.printStackTrace();
		}
	}
	
	private static void pause()
	{
		pause(500);
	}
	
	/**
	 * returns the WebElement for the first layer in the search results table
	 * @param webDriver
	 * @return
	 */
	private static WebElement getFirstResult(WebDriver webDriver)
	{
		WebElement searchResults = webDriver.findElement(By.id("searchResults"));
		List<WebElement> layerRows = searchResults.findElements(By.tagName("tr"));
		if (layerRows.size() > 1)
		{
			WebElement firstRow = layerRows.get(1);
			return firstRow;
		}
		fail("did not find first result");
		return null;
	}
	
	
	/**
	 * returns the name of the first layer in the search results table
	 * @param webDriver
	 * @return
	 */
	private static String getFirstLayerName(WebDriver webDriver)
	{
		WebElement firstRow = getFirstResult(webDriver);
		WebElement title = firstRow.findElement(By.className("colTitle"));
		String title$ = title.getText();
		return title$;
	}
	
	/**
	 * save the first layer and verifies the saved layer count increments by one
	 * this test assumes the first layer is not saved
	 * @param webDriver
	 */
	@Test
	public void saveLayerTest()
	{
		logger.info("  in saveLayerTest");
		int startLayerCount = getSavedLayersCount(webDriver);
		saveFirstLayer(webDriver);
		pause();
		int endLayerCount = getSavedLayersCount(webDriver);
		assertEquals("Save layer test failed", startLayerCount + 1, endLayerCount);
	}
	
	/**
	 * click the save button on the first search results
	 * this will have to change if the columns get re-arranged
	 * @param webDriver
	 */
	private static void saveFirstLayer(WebDriver webDriver)
	{
		WebElement firstRow = getFirstResult(webDriver);
		List<WebElement> buttonElements = firstRow.findElements(By.className("saveControl"));
		WebElement saveButton = buttonElements.get(0);
		saveButton.click();
	}
	
	/**
	 * return the saved layers count from the title on the saved layers tab
	 * @param webDriver
	 * @return
	 */
	private static int getSavedLayersCount(WebDriver webDriver)
	{
		WebElement element = webDriver.findElement(By.id("savedLayersNumberTab"));
		if (element == null){
			List<WebElement> elements = webDriver.findElements(By.className("savedLayersNumber"));
			element = elements.get(0);
		}
		String text = element.getText();
		text = text.replace("(", "");
		text = text.replace(")", "");
		int count = Integer.parseInt(text);
		return count;
	}
	
	/**
	 * click the passed sort button on the search results table column name row
	 * note that we get the actual element from the first row of the table by its class name
	 * @param webDriver
	 * @param buttonClass
	 */
	private static void clickSortByButton(WebDriver webDriver, String buttonClass)
	{
		WebElement searchResults = webDriver.findElement(By.id("searchResults"));		
		List<WebElement> layerRows = searchResults.findElements(By.tagName("tr"));	
		WebElement titlesRow = layerRows.get(0);	
		WebElement nameColumn = titlesRow.findElement(By.className(buttonClass));
		if (nameColumn != null)
			nameColumn.click();
		else
			fail("Did not find button to sort results by name");
	}
	
	/**
	 * enters text into the geocoding search box and verifies the number of search results decreases
	 * initially, it zooms the map all the way out
	 * @param webDriver
	 */
	@Test
	public void geoCodingTest()
	{
		logger.info("  in geoCodingTest");
		zoomToWorld(webDriver);
		pause();
		int startResultsCount = getSearchResultsCount(webDriver);
		setLocation(webDriver, "Boston, MA");
		pause();
		int endResultsCount = getSearchResultsCount(webDriver);
		assertTrue("geoCoding test failed", startResultsCount > endResultsCount);
		
	}
	
	/**
	 * geocoding to an address returns a slightly different kind of object, must test
	 * @param webDriver
	 */
	@Test
	public void geoCodingAddressTest()
	{
		logger.info("  in geoCodingAddressTest");
		zoomToWorld(webDriver);
		pause();
		int startResultsCount = getSearchResultsCount(webDriver);
		setLocation(webDriver, "1600 Pennsylvania ave, washington dc");
		pause();
		int endResultsCount = getSearchResultsCount(webDriver);
		assertTrue("geoCoding test failed", startResultsCount > endResultsCount);
		
	}
	
	/**
	 * verify when restricted layer button is clear the number of search results decreases
	 * this assumes there is at least one restricted layer in Solr
	 *   and the restricted box starts out checked, meaning include all restricted layers
	 */
	/*
	@Test
	public void restrictedTest()
	{
		logger.info("  in restrictedTest");
		zoomToWorld(webDriver);
		advancedSearch(webDriver);
		clickButton(webDriver, "advancedSearchSubmit");
		pause();
		int startResultsCount = getSearchResultsCount(webDriver);
		clickButton(webDriver, "restrictedCheck");
		clickButton(webDriver, "advancedSearchSubmit");
		pause();
		int endResultsCount = getSearchResultsCount(webDriver);
		assertTrue("restriected test fail, " + startResultsCount + ", " + endResultsCount, startResultsCount > endResultsCount);
		clickButton(webDriver, "restrictedCheck");  // put it back to default value
		clickButton(webDriver, "lessSearchOptions");  // reset to basic
	}
	*/
	/**
	 * click on the zoom to world button
	 * @param webDriver
	 */
	private static void zoomToWorld(WebDriver webDriver)
	{
		clickButtonByClass(webDriver, "olControlZoomToMaxExtentItemInactive");
	}
	
	/**
	 * click on the advanced search link
	 * @param webDriver
	 */
	private static void advancedSearch(WebDriver webDriver)
	{
		WebElement advancedSearch = webDriver.findElement(By.id("moreSearchOptions"));
		advancedSearch.click();
	}
	
	/**
	 * enter the passed text into the html element with the passed element id
	 * @param webDriver
	 * @param fieldId
	 * @param value
	 */
	private static void setField(WebDriver webDriver, String fieldId, String value)
	{
		WebElement field = webDriver.findElement(By.id(fieldId));
		field.clear();
		field.sendKeys(value);
	}
	
	/**
	 * click the button corresponding to the passed element id
	 * @param webDriver
	 * @param buttonId
	 */
	private static void clickButton(WebDriver webDriver, String buttonId)
	{
		WebElement button = webDriver.findElement(By.id(buttonId));
		button.click();
	}
	
	private static void clickPulldownOption(WebDriver webDriver, String pulldownId, String buttonId)
	{
		clickButton(webDriver, pulldownId);
		clickButton(webDriver, buttonId);
	}
	
	/**
	 * this function isn't used since I can't get mouse over stuff to work
	 * @param webDriver
	 * @param buttonId
	 */
	private static void clickDataSource(WebDriver webDriver, String buttonId)
	{

		WebElement sourceMenu = webDriver.findElement(By.id("sourceDropdownMenu"));
		sourceMenu.click();
		Actions builder = new Actions(webDriver);   
		builder.moveToElement(sourceMenu).build().perform();
		pause(300);
		WebElement button = webDriver.findElement(By.id(buttonId));
		button.click();

		// move mouse to get menu to close 
		WebElement tempElement = webDriver.findElement(By.id("advancedKeywordText"));
		tempElement.click();

		//Actions builder = new Actions(webDriver);   
		//builder.moveToElement(webDriver.findElement(By.id("sourceDropbox"))).click(webDriver.findElement(By.id(buttonId))).perform();
	}
	
	/**
	 * click the button corresponding to the passed class name
	 * @param webDriver
	 * @param className
	 */
	private static void clickButtonByClass(WebDriver webDriver, String className)
	{
		WebElement button = webDriver.findElement(By.className(className));
		button.click();
	}
	
	private static void setLocation(WebDriver webDriver, String location)
	{
		WebElement geoSearch = webDriver.findElement(By.id("geosearch"));
		geoSearch.click();  //clears field
		geoSearch.sendKeys(location);
		clickButton(webDriver, "goButton");
	}
	
	/**
	 * verify number of search results increases when we zoom out
	 * @param webDriver
	 */
	@Test
	public void zoomToWorldTest()
	{
		logger.info("  in zoomToWorldTest");
		setLocation(webDriver, "Boston, MA");
		pause();
		int bostonLayerCount = getSearchResultsCount(webDriver);
		zoomToWorld(webDriver);
		pause();
		int worldLayerCount = getSearchResultsCount(webDriver);
		assertTrue("zoom to world button test failed.", worldLayerCount > bostonLayerCount);
	}
	
	/**
	 * verify that the data types buttons do something
	 * turn all data types off and test them one at a time
	 * @param webDriver
	 */
	@Test
	public void dataTypeTest()
	{
		logger.info("  in dataTypeTest");
		advancedSearch(webDriver);
		zoomToWorld(webDriver);
		pause(300);
		int initialLayerCount = getSearchResultsCount(webDriver);
		// clear all data types
		clickPulldownOption(webDriver, "dataTypeDropdown", "dataTypeCheckPoint");  // clear
		clickPulldownOption(webDriver, "dataTypeDropdown", "dataTypeCheckLine");   // clear
		clickPulldownOption(webDriver, "dataTypeDropdown", "dataTypeCheckPolygon");   // clear
		//clickPulldownOption(webDriver, "dataTypeDropdown", "dataTypeCheckRaster");   // clear
		clickPulldownOption(webDriver, "dataTypeDropdown", "dataTypeCheckPaperMap");   // clear
		clickButton(webDriver, "advancedSearchSubmit");
		//clickPulldownOption(webDriver, "dataTypeDropdown", "dataTypeCheckRaster");
		
		pause();
		int rasterLayerCount = getSearchResultsCount(webDriver);
		clickPulldownOption(webDriver, "dataTypeDropdown", "dataTypeCheckRaster"); // clear
		clickPulldownOption(webDriver, "dataTypeDropdown", "dataTypeCheckPoint"); 
		clickButton(webDriver, "advancedSearchSubmit");
		pause();
		int pointLayerCount = getSearchResultsCount(webDriver);
		
		clickPulldownOption(webDriver, "dataTypeDropdown", "dataTypeCheckPoint"); // clear
		clickPulldownOption(webDriver, "dataTypeDropdown", "dataTypeCheckLine"); 
		clickButton(webDriver, "advancedSearchSubmit");
		pause();
		int lineLayerCount = getSearchResultsCount(webDriver);
		

		clickPulldownOption(webDriver, "dataTypeDropdown", "dataTypeCheckLine");  // clear
		clickPulldownOption(webDriver, "dataTypeDropdown", "dataTypeCheckPolygon");  
		clickButton(webDriver, "advancedSearchSubmit");
		pause();
		int polygonLayerCount = getSearchResultsCount(webDriver);
		
		clickPulldownOption(webDriver, "dataTypeDropdown", "dataTypeCheckPolygon");    // clear
		clickPulldownOption(webDriver, "dataTypeDropdown", "dataTypeCheckPaperMap");
		clickButton(webDriver, "advancedSearchSubmit");
		pause();
		int mapLayerCount = getSearchResultsCount(webDriver);
				
		clickPulldownOption(webDriver, "dataTypeDropdown", "dataTypeCheckRaster");
		clickPulldownOption(webDriver, "dataTypeDropdown", "dataTypeCheckPoint");
		clickPulldownOption(webDriver, "dataTypeDropdown", "dataTypeCheckLine");
		clickPulldownOption(webDriver, "dataTypeDropdown", "dataTypeCheckPolygon");

		clickButton(webDriver, "lessSearchOptions");  // reset to basic
		int vectorLayerCount = pointLayerCount + lineLayerCount + polygonLayerCount;
		assertTrue("raster test failed", ((initialLayerCount >= rasterLayerCount) && (rasterLayerCount > 0)));
		assertTrue("vector test failed", ((initialLayerCount >= vectorLayerCount) && (vectorLayerCount > 0)));
		assertTrue("map test failed", ((initialLayerCount >= mapLayerCount) && (mapLayerCount > 0)));
		
		int dataTypeTotal = rasterLayerCount + vectorLayerCount + mapLayerCount;
		assertTrue("data type test failed, initial layer count = " + initialLayerCount + " data type total = " + dataTypeTotal
				+ "(point = " + pointLayerCount + ", line = " + lineLayerCount + ", polygon = " + polygonLayerCount
				+ ", [vector = " + vectorLayerCount
				+ "], raster = " + rasterLayerCount + ", map = " + mapLayerCount + ")", 
				initialLayerCount >= dataTypeTotal);
		
	}
	
	/**
	 * return true if a checkbox is checked, false otherwise
	 * @param webDriver, String id
	 * @return Boolean 
	 */
	public static Boolean isChecked(WebDriver webDriver, String id)
	{
		WebElement inputElement = webDriver.findElement(By.id(id));
		String checked = inputElement.getAttribute("checked");
		return Boolean.parseBoolean(checked);
	}
	
	/**
	 * turn off all sources
	 * @param webDriver
	 */
	public void clearDataSources(WebDriver webDriver){
		List<String> dataSourceArray = getDataSourceIds(webDriver);
		for (String dataSource: dataSourceArray){
			if (isChecked(webDriver, dataSource)){
				clickPulldownOption(webDriver, "sourceDropdown", dataSource);  // clear
			}
		}
	}
	
	/**
	 * turn on all sources
	 * @param webDriver
	 */
	public void selectAllDataSources(WebDriver webDriver){
		List<String> dataSourceArray = getDataSourceIds(webDriver);
		for (String dataSource: dataSourceArray){
			if (!isChecked(webDriver, dataSource)){
				clickPulldownOption(webDriver, "sourceDropdown", dataSource);  // select unchecked
			}
		}
	}
	
	/**
	 * Get datasource checkbox id's (verifies that element is a checkbox)
	 * @param webDriver
	 * @return List<String> datasource id's
	 */
	public List<String> getDataSourceIds(WebDriver webDriver){
		List<WebElement> elements = webDriver.findElements(By.className("sourceCheck"));
		List<String> dataSources = new ArrayList<String>();
		for (WebElement checkbox : elements){
			if (checkbox.getTagName().equalsIgnoreCase("input")&&checkbox.getAttribute("type").equalsIgnoreCase("checkbox")){
				dataSources.add(checkbox.getAttribute("id"));
			}
		}
		return dataSources;
	}
	
	/**
	 * turn off all sources then turn them on one at a time
	 * @param webDriver
	 */
	@Test
	public void dataSourceTest()
	{
		logger.info("  in dataSourceTest");
		clickButton(webDriver, "moreSearchOptions");
		pause(100);
		zoomToWorld(webDriver);
		pause();
		selectAllDataSources(webDriver);
		pause();
		
		clickButton(webDriver, "advancedSearchSubmit");
		pause();
		int initialLayerCount = getSearchResultsCount(webDriver);
		
		List<String> dataSourceIds = getDataSourceIds(webDriver);
		int compositeLayerCount = 0;
		for (String dataSource : dataSourceIds){
			clearDataSources(webDriver);
			pause();
			
			clickPulldownOption(webDriver, "sourceDropdown", dataSource);
		
			clickButton(webDriver, "advancedSearchSubmit");
			pause();
			
			int currentLayerCount = getSearchResultsCount(webDriver);
			compositeLayerCount += currentLayerCount;
			String dataSourceValue = webDriver.findElement(By.id(dataSource)).getAttribute("value");
			
			assertTrue(dataSourceValue + " test failed (initialLayerCount = " + initialLayerCount + ", currentLayerCount = " + currentLayerCount,
				((initialLayerCount >= currentLayerCount) && (currentLayerCount > 0)));
		}
		
		assertTrue("total layer count by source too large", initialLayerCount >= compositeLayerCount);

		// turn back on 
		selectAllDataSources(webDriver);
		clickButton(webDriver, "lessSearchOptions");  // reset
	}
	
	/**
	 * perform multiple searches with different dates 
	 * @param webDriver
	 */
	@Test
	public void dateSearchTest()
	{
		logger.info("  in dateSearchTest");
		clickButton(webDriver, "moreSearchOptions");
		pause();
		zoomToWorld(webDriver);
		pause();
		clickButton(webDriver, "advancedSearchSubmit");
		pause();
		int totalLayerCount = getSearchResultsCount(webDriver);
		setField(webDriver, "advancedDateFromText", "0");
		setField(webDriver, "advancedDateToText", "1000");
		clickButton(webDriver, "advancedSearchSubmit");
		pause();
		int veryOldLayers = getSearchResultsCount(webDriver);
		pause();
		setField(webDriver, "advancedDateFromText", "1000");      
		setField(webDriver, "advancedDateToText", "2020");
		clickButton(webDriver, "advancedSearchSubmit");
		pause();
		int newerLayers = getSearchResultsCount(webDriver);
		
		// clean-up
		setField(webDriver, "advancedDateFromText", "");
		setField(webDriver, "advancedDateToText", "");
		clickButton(webDriver, "advancedSearchSubmit");
		clickButton(webDriver, "lessSearchOptions");
		
		// did the searches work?
		assertTrue("date search failed, with number of total layers = " + totalLayerCount
					+ " and number of layers between years 0 and 1000 = " + veryOldLayers 
					+ ".  Total number of layers should have been larger then layers between 0 and 1000",
					veryOldLayers < totalLayerCount); 
		assertTrue("data search failed with number of layers between years 0 and 1000 = " + veryOldLayers
					+ " and number of layers between years 1000 and 2020 = " + newerLayers
					+ ".  The number of layers from more recent window should have been greater " 
					+ "then number of ancient layers.", veryOldLayers < newerLayers);
		
	}
	
	/**
	 * close the browser, since tests are done. 
	 * @param webDriver
	 */
	@Test
	public void quitBrowser(){
		logger.info("Closing Brower");
		webDriver.quit();
	}
	
	
}
