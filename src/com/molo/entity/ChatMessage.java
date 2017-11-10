package com.molo.entity;



public class ChatMessage {
	private  long id;//l'idenetifiant unique
	private String content;
	private String receiver;
	private String sender;
	private long thread;
	private String date;
	public long getThread() {
		return thread;
	}
	public void setThread(long thread) {
		this.thread = thread;
	}
	
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	public ChatMessage(long id, String content,long thread, String receiver,
			String sender,String d) {
		super();
		this.id = id;
		this.date=d;
		this.content = content;
		this.receiver = receiver;
		this.sender = sender;
		this.thread=thread;
	}
	public ChatMessage() {
		// TODO Auto-generated constructor stub
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public String getReceiver() {
		return receiver;
	}
	public void setReceiver(String receiver) {
		this.receiver = receiver;
	}
	public String getSender() {
		return sender;
	}
	public void setSender(String sender) {
		this.sender = sender;
	}
	
}
