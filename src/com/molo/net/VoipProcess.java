package com.molo.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import javax.sound.sampled.AudioFormat;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import com.molo.Utils;
import com.molo.message.MyStandarMessageType;

public class VoipProcess extends Thread{
	//private AudioFormat audioFormat=Utils.getAudioFormat(16000);
	private final int BUFF_SIZE = 4096*10;
	private Socket mSocket;
	private String label="";
	private DataInputStream reader;
	private DataOutputStream writer;
	private ObjectMapper mapper;
	private VoipProcess other;
	private boolean isConnected=false;
	public VoipProcess(Socket s) {
		// TODO Auto-generated constructor stub
		this.mSocket=s;
		mapper=new ObjectMapper();
		try {
			reader=new DataInputStream(new BufferedInputStream(mSocket.getInputStream()));
			writer=new DataOutputStream(new BufferedOutputStream(mSocket.getOutputStream()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	synchronized public boolean isConnected(){
		return isConnected;
	}
	@Override
	public void run() {
		try {
			String line=reader.readUTF();
			System.out.println("VoipProcess:Line "+line);
			JsonNode request=mapper.readTree(line);
			if(request.has("label")){
				this.label=request.get("label").asText();
				long timer=System.currentTimeMillis();
				VoipProcess o=null;
				while(System.currentTimeMillis()-timer<1000*60 && o==null){
					o=SecondServeur.findProcessByLabel(label,this);
					Thread.sleep(100);
					//System.out.println(""+o);
				}
				System.out.println("VoipProcess:Other "+o);
				if(o!=null){
					this.setOther(o);
					o.setOther(this);
					ObjectNode n=mapper.createObjectNode();
					n.put("label",this.label);
					n.put("status", MyStandarMessageType.OK);
					writer.writeUTF(mapper.writeValueAsString(n));
					writer.flush();
					byte[] buffer=new byte[BUFF_SIZE];
					while(mSocket.isConnected() && this.other.isConnected() && this.isConnected() ){
						//read byte from client and rewrite them to other socket
						int number=this.readBytes(buffer);
						//System.out.println("number "+number);
						if(number>0)
							other.transfertBytes(buffer,number);
						else break;
					}
				}else{
					ObjectNode n=mapper.createObjectNode();
					n.put("label",this.label);
					n.put("status", MyStandarMessageType.ERROR);
					writer.writeUTF(mapper.writeValueAsString(n));
					writer.flush();
				}
			}
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		close();
	}
	synchronized private void transfertBytes(byte[] readBytes, int number) throws IOException {
		// TODO Auto-generated method stub
		this.writer.write(readBytes, 0, number);
	}
	private int readBytes(byte[] buff) throws IOException {
		// TODO Auto-generated method stub
		return  this.reader.read(buff);
	}
	synchronized private void setOther(VoipProcess p) {
		// TODO Auto-generated method stub
		isConnected=true;
		this.other=p;
	}
	private void close() {
		// TODO Auto-generated method stub
		if(mSocket.isConnected()){
			try {
				mSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		SecondServeur.finishCallFor(this.label);
		SecondServeur.removeFromVoipProcessList(this);
		if(this.isAlive())
			this.interrupt();
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public void onStopServer() {
		// TODO Auto-generated method stub
		close();
	}
	
}
