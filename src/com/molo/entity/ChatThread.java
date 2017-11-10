package com.molo.entity;



public class ChatThread {
	private long id;
	private String member1,member2;
	private int unread;
	private String date;
	public ChatThread() {
		// TODO Auto-generated constructor stub
	}
	public ChatThread(long id, String member1, String member2,String d) {
		super();
		this.id = id;
		this.date=d;
		this.member1 = member1;
		this.member2 = member2;
	}
	
	
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	public int getUnread() {
		return unread;
	}
	public void setUnread(int unread) {
		this.unread = unread;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getMember1() {
		return member1;
	}
	public void setMember1(String member1) {
		this.member1 = member1;
	}
	public String getMember2() {
		return member2;
	}
	public void setMember2(String member2) {
		this.member2 = member2;
	}

	
}
