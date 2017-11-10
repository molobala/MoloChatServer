package com.molo.security;

import com.molo.Utils;

import java.util.Random;

public class CSAREncryption {
	private static String ALPHAT_HEX="4142434445464748494a4b4c4d4e4f505152535455565758595a6162636465666768696a6b6c6d6e6f707172737475767778797a303132333435363738395f2d27222e2c3b3a3f2a24255ea83d28295b5d237ee9e85c2f21a73c3e407b7d";
	//public static String ALPHABET="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-'\".,;:?*$%^¨=()[]#~ιθ\\/!§<>@{}";
	public static String ALPHABET= Utils.convertHexToString(ALPHAT_HEX);
	public static String genKey(String ref){
		String tmp=new String(ref);
		StringBuilder builder=new StringBuilder();
	    int len=ref.length(),i,l=tmp.length();
	    Random rand=new Random();
	    for(i=0;i<len;i++)
	    {
	        int k=rand.nextInt(l);
	        char c=tmp.charAt(k);
	        builder.append(c);
	        tmp=tmp.replace(""+c,"");
	        l=tmp.length();
	    }
	    return builder.toString();
	}
	public static String encrypt(String text,String ref,String key){
		StringBuilder phC=new StringBuilder();
		char[] charArr=text.toCharArray();
	    for(char c:charArr)
	    {
	    	//System.out.println("III",""+i+";("+(char)c+"");
	        int n=ref.indexOf(c);
	        if(n>=0){
	        	 phC.append(key.charAt(n));
	        }
	        else{
	        	phC.append(c);
	        }
	    }
	    return phC.toString();
	}
	public static String decrypt(String text,String ref,String key){
		StringBuilder phD=new StringBuilder();
		char[] charArr=text.toCharArray();
	    for(char c:charArr)
	    {
	        int n=key.indexOf(c);
	        if(n>=0){
	        	 phD.append(ref.charAt(n));
	        }
	        else{
	        	phD.append(c);
	        }
	    }
	    return phD.toString();
	}
}
