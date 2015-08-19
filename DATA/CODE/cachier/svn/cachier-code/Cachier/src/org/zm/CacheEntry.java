package org.zm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class CacheEntry {

    /** name of the cache entry, IE "cachier load cacheName" */
    String cacheName;

    /** folder being cached */
    File sourceFolder;

    /** place to store cached folder */
    File cacheFolder;

    /** location of stored folder */
    File dataStoreFolder;

    public File getDataStoreFolder() {
	return dataStoreFolder;
    }

    public void setDataStoreFolder(File dataStoreFolder) {
	this.dataStoreFolder = dataStoreFolder;
    }

    /** true if cache is stored in place */
    boolean inPlace;

    public String getCacheName() {
	return cacheName;
    }

    public void setCacheName(String cacheName) {
	this.cacheName = cacheName;
    }

    public File getSourceFolder() {
	return sourceFolder;
    }

    public void setSourceFolder(File sourceFolder) {
	this.sourceFolder = sourceFolder;
    }

    public File getCacheFolder() {
	return cacheFolder;
    }

    public void setCacheFolder(File cacheFolder) {
	this.cacheFolder = cacheFolder;
    }

    public boolean isInPlace() {
	return inPlace;
    }

    public void setInPlace(boolean inPlace) {
	this.inPlace = inPlace;
    }

    public static final String CACHE_NAME_PREFIX = "Name=";
    public static final String CACHE_LOCATION_PREFIX = "CachePath=";
    public static final String SOURCE_LOCATION_PREFIX = "SourcePath=";
    public static final String IN_PLACE_PREFIX = "InPlace=";

    /**
     * Creates a CacheEntry from a stored configfile
     * 
     * If file is invalid, returns null;
     * 
     * @param config
     * @return
     */
    public static CacheEntry createFromCfgFile(File config) {

	CacheEntry ret = null;
	String cacheName = null;
	String cacheLocation = null;
	String sourceLocation = null;
	String isInPlace = null;

	BufferedReader in;
	try {
	    in = new BufferedReader(new FileReader(config));

	    String read;
	    while (!in.ready())
		;

	    while ((read = in.readLine()) != null) {

		if (read.startsWith(CACHE_NAME_PREFIX))
		    cacheName = read.substring(CACHE_NAME_PREFIX.length());
		else if (read.startsWith(CACHE_LOCATION_PREFIX))
		    cacheLocation = read.substring(CACHE_LOCATION_PREFIX.length());
		else if (read.startsWith(SOURCE_LOCATION_PREFIX))
		    sourceLocation = read.substring(SOURCE_LOCATION_PREFIX.length());
		else if (read.startsWith(IN_PLACE_PREFIX))
		    isInPlace = read.substring(IN_PLACE_PREFIX.length());

	    }

	    if (cacheName != null && cacheLocation != null && sourceLocation != null && isInPlace != null) {
		ret = new CacheEntry(cacheName, new File(sourceLocation), new File(cacheLocation), (isInPlace.toLowerCase().equals("true")));
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
	return ret;
    }

    public CacheEntry(String cacheName, File sourceFolder, File cacheFolder, boolean inPlace) {
	super();
	this.cacheName = cacheName;
	this.sourceFolder = sourceFolder;
	this.cacheFolder = cacheFolder;
	this.inPlace = inPlace;
    }

}
