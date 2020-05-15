package com.mctait.bulkmp3downloader;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.util.Log;

public class MP3Downloader {
	
	private static final String MP3_PATTERN = "([^\\s]+(\\.(?i)(mp3|m4a))$)";
	private String 	className = this.getClass().getSimpleName();
	private Pattern pattern;
	private Matcher matcher;
	
	public MP3Downloader() {
		pattern = Pattern.compile(MP3_PATTERN);
	}
	
	protected void downLoadMP3sFromResults(Set<String> results) {
		for(String link : results){
			Log.d(className, link);
			if(isURLHealthy(link))
				getMP3LinksFromPage(link);
		}	
	}

	protected Set<String> getMP3LinksFromPage(String link) {
		Set<String> links = new HashSet<String>();
		Log.d(className, "searching link: " + link);
		
		try {
			Document doc = Jsoup
					.connect(link)
					.timeout(5000).get();
			
			// get all links
			Elements alinks = doc.select("a[href]");
			for (Element alink : alinks) {
				
				String text = alink.text();
				String mp3link = alink.attr("href");
				
				matcher = pattern.matcher(mp3link);
				if(matcher.matches()) {
					Log.d(className, "alink: " + mp3link);
					Log.d(className, "text: " + text);
					// TODO
					//downloadFromUrl(link+mp3link, downloadFolder, text);
					downloadFromUrl(link+mp3link, text);
				}
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
		return links;
	}
	
	
	// TODO needs to go into MainActivity....
	private void downloadFromUrl(String link, String localFilename) throws IOException {
	    InputStream is = null;
	    FileOutputStream fos = null;
	    
	    URL url = new URL(link);

	    try {
	        URLConnection urlConn = url.openConnection();//connect
	        
	        is = urlConn.getInputStream();               //get connection inputstream
	        Log.d(className, "localFilename: " + localFilename);

	        fos = new FileOutputStream(localFilename);   //open outputstream to local file

	        byte[] buffer = new byte[4096];              //declare 4KB buffer
	        int len;

	        //while we have available data, continue downloading and storing to local file
	        while ((len = is.read(buffer)) > 0) {  
	            fos.write(buffer, 0, len);
	        }
	    } finally {
	        try {
	            if (is != null) {
	                is.close();
	            }
	        } finally {
	            if (fos != null) {
	                fos.close();
	            }
	        }
	    }
	}
	

	private boolean isURLHealthy(String urlString) {
		
		boolean urlStringOK = false;
		
		try {
			URL url = new URL(urlString);
			HttpURLConnection huc = (HttpURLConnection)url.openConnection();
			huc.setRequestMethod("HEAD");
			huc.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.0; en-US; rv:1.9.1.2) Gecko/20090729 Firefox/3.5.2 (.NET CLR 3.5.30729)");
			huc.connect();
			int code = huc.getResponseCode();
			Log.d(className, "code: " + code);
			if(huc.getResponseCode() == HttpURLConnection.HTTP_OK)
				urlStringOK = true;
		} catch(MalformedURLException mue) {
			Log.e(className, "Malformed URL");
		} catch(IOException ioe) {
			Log.e(className, "IOException");
		}
		return urlStringOK;
	}
}
