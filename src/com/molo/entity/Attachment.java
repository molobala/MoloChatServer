package com.molo.entity;

public class Attachment {
	private long id=0;
	private String path="",
				   type="";
	private long size=0;
	private long message=0;
	public Attachment() {
		// TODO Auto-generated constructor stub
	}
	public Attachment(long id, String path, String type, long size,
			long message) {
		super();
		this.id = id;
		this.path = path;
		this.type = type;
		this.size = size;
		this.message = message;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public long getSize() {
		return size;
	}
	public void setSize(long size) {
		this.size = size;
	}
	public long getMessage() {
		return message;
	}
	public void setMessage(long message) {
		this.message = message;
	}
	
}
