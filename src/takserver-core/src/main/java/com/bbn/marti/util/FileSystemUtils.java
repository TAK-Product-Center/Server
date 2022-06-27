

package com.bbn.marti.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.log4j.Logger;

public class FileSystemUtils { 

	private static final Logger log = Logger.getLogger(FileSystemUtils.class
			.getSimpleName());

	public static String md5sumFile(String path){
		if(path == null || path.length() < 1)
			return null;

		return md5sumFile(new File(path));
	}

	public static String md5sumFile(File file){ 
		if(file == null || !file.exists())
			return null;

		try {
			return md5sum(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		return null;
	}

	public static String md5sumContent(String content){
		if(content == null || content.length() < 1)
			return null;
		try {
			return md5sum(new ByteArrayInputStream(content.getBytes("UTF-8")));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	/*
	 * Calculate checksum of a File using MD5 algorithm
	 */
	public static String md5sum(InputStream input){
		String checksum = null;

		if(input == null)
			return checksum;

		try {
			MessageDigest md = MessageDigest.getInstance("MD5");

			//Using MessageDigest update() method to provide input
			byte[] buffer = new byte[8192];
			int numOfBytesRead;
			while( (numOfBytesRead = input.read(buffer)) > 0){
				md.update(buffer, 0, numOfBytesRead);
			}
			byte[] hash = md.digest();
			md.reset();
			checksum = new BigInteger(1, hash).toString(16); //don't use this, truncates leading zero
		} catch (IOException ex) {
			log.warn("Error computing md5sum", ex);
		} catch (NoSuchAlgorithmException ex) {
			log.warn("Error computing md5sum", ex);
		} finally{
			if(input != null)
			{
				try {
					input.close();
				} catch (IOException e) {
					log.warn("Error computing md5sum", e);
				}
			}
		}

		return checksum;
	}

}

