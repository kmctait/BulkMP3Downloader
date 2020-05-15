package com.mctait.bulkmp3downloader;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.util.Log;

public abstract class Search {

	private int numResultsObtained = 0;
	private Set<String> resultLinks = new HashSet<String>();
	private String className = this.getClass().getSimpleName();
	
	public Search(String queryTerm, int numSearchResults) {
		
		String parsedQueryTerm = parseQueryTerm(queryTerm);
		
		String searchQuery = "-inurl:htm+-inurl:html+intitle:%22index+of%22+mp3+%22" + parsedQueryTerm + "%22" + "&num=" + numSearchResults;
		Log.d(className, "query: " + searchQuery);
		
		setResultLinks(getDataFromSearchEngine(searchQuery));
		setNumResultsObtained(getResultLinks().size());
	}
	
	private String parseQueryTerm(String queryTerm)
	{
		String parsedTerms = new String();
		parsedTerms = queryTerm.replace(' ', '+');
		
		return parsedTerms;
	}
	
	protected Set<String> getDataFromSearchEngine(String query) {
		
		Set<String> result = new HashSet<String>();	
		String request = "http://www.google.com/search?q=" + query;
		Log.d(className, "Sending request..." + request);

		try {
			// need http protocol, set this as a Google bot agent :)
			Document doc = Jsoup
					.connect(request)
					.userAgent("Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)")
					.timeout(5000).get();

			// get all links
			Elements titles = doc.select("h3[class=r]");
			for (Element title : titles) {
				
				Elements link = title.select("a[href]");
				String alink = link.attr("href");
				if(alink.startsWith("/url?q=")){
					result.add(getLinkURL(alink));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	protected String getLinkURL(String line) {
		String link = new String();
		if((line.indexOf("http") >= 0) && (line.indexOf("&") >= 0)) {
			link = line.substring(line.indexOf("http"), line.indexOf("&")); 
		}
		return link;
	}
	
	protected void printSearchResults() {
		
		Log.d(className, "Num of search result links: " + getNumResultsObtained());
		
		for(String link : getResultLinks()){
			Log.d(className, link);
		}	
	}

	protected int getNumResultsObtained() {
		return numResultsObtained;
	}

	protected Set<String> getResultLinks() {
		return resultLinks;
	}
	
	protected void setNumResultsObtained(int numResultsObtained) {
		this.numResultsObtained = numResultsObtained;
	}

	protected void setResultLinks(Set<String> resultLinks) {
		this.resultLinks = resultLinks;
	}
	
}
