

package com.bbn.marti;

import java.util.*;

public class FileDownloadCounter {

	private static FileDownloadCounter instance = null;

	public static FileDownloadCounter instance() {
		if(instance == null)
			instance = new FileDownloadCounter();

		return instance;
	}

	private Map<String, List<String>> filesDownloadedByWho = new HashMap<String, List<String>>();

	public void putDownloader(String filename, String downloader) {
		List<String> downloaders =
			filesDownloadedByWho.get(filename);

		if(downloaders == null) {
			downloaders = new LinkedList<String>();
			filesDownloadedByWho.put(filename, downloaders);
		} 

		downloaders.add(downloader);
	}

	public List<String> getDownloaders(String filename) {
		return filesDownloadedByWho.get(filename);
	}
}
