package com.molo.extra;

public class ColumnNameFormatter {
	public static String format(String cn){
		StringBuilder sb=new StringBuilder();
		for(int i=0;i<cn.length();i++){
			char c=cn.charAt(i);
			if(c=='_'){
				char tmp=cn.charAt(i+1);
				if(Character.isAlphabetic(tmp)){
					sb.append(Character.toUpperCase(tmp));
				}else{
					sb.append(tmp);
				}
				i+=1;
				continue;
			}else{
				sb.append(c);
			}
		}
		return sb.toString();
	}
}
