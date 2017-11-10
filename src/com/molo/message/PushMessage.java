package com.molo.message;

public class PushMessage {
	public Object data;
	public int status;
	public long hash;
	public PushMessage() {
		// TODO Auto-generated constructor stub
	}
	public PushMessage(long hash,int status,Object o){
		this.data=o;
		this.hash=hash;
		this.status=status;
	}
}
