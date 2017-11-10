package com.molo.message;

public class ChatMessage extends Message {
	private String sender,receiver;
	public ChatMessage(long id,String sender,String rec,String content) {
		// TODO Auto-generated constructor stub
		super(id,content,MyStandarMessageType.OK);
		this.sender=sender;
		this.receiver=rec;
		
	}
	
	public ChatMessage() {
		// TODO Auto-generated constructor stub
	}

	public String getSender() {
		return sender;
	}

	public void setSender(String sender) {
		this.sender = sender;
	}

	public String getReceiver() {
		return receiver;
	}

	public void setReceiver(String receiver) {
		this.receiver = receiver;
	}
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "{id:"+getId()+"#status:"+getStatus()+"#content:"+getContent()+"#sender:"+sender+"#receiver:"+receiver+"}";
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
					}else if(tmps[0].equalsIgnoreCase("receiver")){
						this.setReceiver(tmps[1]);
					}else if(tmps[0].equalsIgnoreCase("sender")){
						this.setSender(tmps[1]);
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
