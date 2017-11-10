package com.molo.security;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Encrypt {
	public static String encrypt(String str,String alg) throws UnsupportedEncodingException, NoSuchAlgorithmException{
		 MessageDigest crypt = MessageDigest.getInstance(alg);
		 crypt.reset();
		 crypt.update(str.getBytes("UTF-8"));
		 return byteArrayToHexString(crypt.digest());
	}
	public static String byteArrayToHexString(byte[] b) {
		  String result = "";
		  for (int i=0; i < b.length; i++) {
		    result +=
		          Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
		  }
		  return result;
	}
}
