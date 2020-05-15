package com.mctait.bulkmp3downloader;

public class LinkAndFilename {
	
	String link;
	String filename;
	
	public LinkAndFilename(String link, String filename){
		this.link = link;
		this.filename = filename;
	}

	protected String getLink() {
		return link;
	}

	protected String getFilename() {
		return filename;
	}

}
