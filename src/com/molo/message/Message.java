package com.molo.message;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Message {
	private int status=0;
	private String content="";
	private long id;
	public Message(long id,String content,int st) {
		// TODO Auto-generated constructor stub
		this.status=st;
		this.content=content;
		this.id=id;
	}
	public Message() {
		// TODO Auto-generated constructor stub
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	@Override
	public String toString() {
		// TODO Auto-generated method stub
	
		return "{id:"+id+"#status:"+status+"#content:"+content+"}";
	}
	public void fromJsonString(String msg){
		if(msg==null)
			return;
		msg=msg.replaceAll("\\\\#", "&\\[esch\\]&").replaceAll("\\\\:", "&\\[escdp\\]&");
		String[] elements=msg.trim().substring(1, msg.length()-1).split("[#]");
		for(String s:elements){
			if(!s.isEmpty()){
				String[] tmps=s.split("[:]");
				if(tmps.length>=2){
					if(tmps[0].equalsIgnoreCase("status")){
						try {
							this.setStatus(Integer.valueOf(tmps[1]));
						} catch (NumberFormatException e) {
							// TODO: handle exception
							this.setStatus(0);
						}
					}else if(tmps[0].equalsIgnoreCase("content")){
						this.setContent(tmps[1].replaceAll("&\\[esch\\]&", "#").replaceAll("&\\[escdp\\]&", ":"));
					}else if(tmps[0].equalsIgnoreCase("id")){
						try {
							this.setId(Long.valueOf(tmps[1]));
						} catch (NumberFormatException e) {
							// TODO: handle exception
							this.setId(0);
						}
					}
				}
			}
		}
	}
}
