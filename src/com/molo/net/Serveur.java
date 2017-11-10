package com.molo.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.molo.message.ChatMessage;
import com.molo.message.Message;
import com.molo.message.MyStandarMessageType;


public class Serveur {
	final static Map<String,Socket> LOGS=new ConcurrentHashMap<String,Socket>();
	public final static String PASSWORD="molobala",LOGIN="molo";
	List<Traitement> mTraitements=new ArrayList<>();
	Map<ChatThread, List<ChatMessage>> messagesTables=new ConcurrentHashMap<Serveur.ChatThread, List<ChatMessage>>();
	ServerSocket mSock=null;
	private boolean go=true;
	public Serveur(int p) throws IOException {
		// TODO Auto-generated constructor stub
		mSock=new ServerSocket(p);
		mSock.setSoTimeout(30*60000);
		run();
		
	}
	public void stop() {
		this.go=false;
	}
	private void run(){
		System.out.println("En ecoute des clients");
		while(go){
			Socket s;
			try {
				s = mSock.accept();
				Traitement t=new Traitement(s);
				mTraitements.add(t);
				System.out.println("Connexion d'un client:"+s.getInetAddress());
				t.start();
			}catch (SocketTimeoutException e) {
				//mMainThread.interrupt();
				break;
				
			} catch (IOException e) {
				//mMainThread.interrupt();
				break;
			}
			
		}
		try {
			mSock.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Arret du serveur");
		for(Traitement t:mTraitements){
			t.onStopServer();
			//mTraitements.remove(t);
		}
	}
	private class Traitement extends Thread{
		Socket mSocket=null;
		private String mLog;
		private boolean isAuthentificated=false;
		BufferedReader in=null;
		PrintWriter out=null;
		public Traitement(Socket s) {
			// TODO Auto-generated constructor stub
			this.mSocket=s;
//			try {
//				this.mSocket.setSoTimeout(3*60000);
//			} catch (SocketException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		}
		public void onStopServer() {
			// TODO Auto-generated method stub
			Message msg=new Message(0,"Serveur Stoped", MyStandarMessageType.EXIT);
			println(out, msg.toString());
			this.interrupt();
		}
		private void close(){
			try{
				if(isAuthentificated && mLog!=null)
				{
					LOGS.remove(mLog);
					for(ChatThread t:messagesTables.keySet()){
						if(t.log1.equals(mLog) || t.log2.equals(mLog)){
							messagesTables.remove(t);
						}
					}
				}
				mSocket.close();
			}catch(IOException e){
			}
			this.interrupt();
			mTraitements.remove(this);
			if(isAuthentificated){				
				Message msg=new Message(0,mLog, MyStandarMessageType.ON_MEMBER_DISCONNECTION);
				for(Traitement t:mTraitements){
					if(t.isAuthentificated && this!=t)
						t.println(t.out, msg.toString());
				}
			}
			System.out.println("Disconnexion of "+mLog);
		}
		@Override
		public void run() {
			try{
				System.out.println("Commencement du traitement");
				in=new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
				out=new PrintWriter(mSocket.getOutputStream());
				String commande="";
				//authentification
				int authentificatCounter=0;
				Message message=new Message();
				message.setContent("Bienvenu");
				message.setStatus(MyStandarMessageType.OK);
				println(out, message.toString());
				while(mSocket.isConnected()){
					if(authentificatCounter>=6)
						break;
					try{
						commande=readLine(in);
						if(commande==null)
							break;
						long comId=0;
						boolean baseSyntaxeCorrect=false;
						//System.out.println(commande);
						String[] ls=commande.trim().split("[\\s]");
						if(ls.length<=1)
							continue;
						try{
							comId=Long.valueOf(ls[0]);
							baseSyntaxeCorrect=true;
						}catch(NumberFormatException e){
							baseSyntaxeCorrect=false;
						}
						if(!baseSyntaxeCorrect){
							message.setContent("Syntaxe error");
							message.setStatus(MyStandarMessageType.SYNTAXE_ERROR);
							message.setId(0);
							println(out, message.toString());
							continue;
						}
						message.setId(comId);
						System.out.println("Two parts");
						if(!isAuthentificated && ls[1].equalsIgnoreCase("authentificate")){
							String subCommand=commande.replaceFirst("[\\s]*[0-9]+[\\s]+[a-zA-Z0-9_]+[\\s]+", "").trim();
							message.setId(comId);
							System.out.println("SubComm: "+subCommand);
							if(subCommand.matches("[\\s]*[a-zA-Z0-9_]+/[a-zA-Z0-9_]+[\\s]*")){
								String[] tmps=subCommand.split("[/]");
								if(tmps[0].equals(LOGIN) && tmps[1].equals(PASSWORD)){
									//success
									isAuthentificated=true;
									mLog="client_"+(LOGS.size()+1);
									LOGS.put(mLog, mSocket);
									//on ouvre une session
									message.setStatus(MyStandarMessageType.AUTHENTIFICATED_SUCCESSFULLY);
									message.setContent("Connected successfully! Your login is\\: "+mLog);
									//message.setId(comId);
									//System.out.println(message.toString());
									println(out, message.toString());
									message.setId(0);
									message.setContent(mLog);
									message.setStatus(MyStandarMessageType.ON_NEW_MEMBER_CONNECTION);
									for(Traitement t:mTraitements){
										if(t.isAuthentificated && t!=this) t.println(t.out, message.toString());
									}
								}else{
									authentificatCounter++;
									message.setStatus(MyStandarMessageType.AUTHENTIFICATION_ERROR);
									message.setContent("Authentification Error");
									println(out, message.toString());
								}
							}else{
								message.setStatus(MyStandarMessageType.SYNTAXE_ERROR);
								message.setContent("Syntaxe Error\\:Should be Log/Password");
								this.println(out, message.toString());
							}
							
						}else if(isAuthentificated){
							//operation after authentificaion
							if(ls[1].matches("talkto")){
								String subCommande=commande.replaceFirst("[\\s]*[0-9]+[\\s]+[a-zA-Z0-9_]+[\\s]+", "");
								openChannelTo(subCommande.trim());
							}
							else if(ls[1].equalsIgnoreCase("setLogin")){
								String subCommande=commande.replaceFirst("[\\s]*[0-9]+[\\s]+setLogin[\\s]+", "");
								String old=this.mLog;
								boolean found=false;
								for(String l:LOGS.keySet()){
									if(l!=mLog && subCommande.equals(l)){
										//error and break
										found=true;
										break;
									}
								}
								if(found){
									message.setContent("Login already exits! Please choose an other");
									message.setStatus(MyStandarMessageType.SET_LOGIN_ERROR);
									continue;
								}
								this.mLog=subCommande;
								LOGS.put(mLog, LOGS.remove(old));
								for(ChatThread th:messagesTables.keySet()){
									if(th.log1.equals(old))
										th.log1=mLog;
									else if(th.log2.equals(old))
										th.log2=mLog;
									
								}
								//response to the client
								message.setContent(mLog);
								message.setStatus(MyStandarMessageType.OK);
								println(out, message.toString());
								//notify all connected members of the changement of the login
								message.setId(0);
								message.setContent(old+"="+mLog);
								message.setStatus(MyStandarMessageType.ON_ONE_LOGIN_CHANGED);
								for(Traitement t:mTraitements){
									if(t.isAuthentificated && t!=this) t.println(t.out, message.toString());
								}
								
							}
							else if(ls[1].equalsIgnoreCase("postmessage")){
								//retrive receiver and msg content
								if(ls.length>=3){
									String receiver=ls[2];
									String content=commande.replaceFirst("[\\s]*[0-9]+[\\s]+[a-zA-Z0-9_]+[\\s]+[a-zA-Z0-9_]+[\\s]+", "");
									if(!writeTo(receiver, content,message))
									{
	//										message.setStatus(MyStandarMessageType.SEND_MESSAGE_ERROR);
	//										message.setContent("Enable to send message to other");
									}else{
	//										message.setStatus(MyStandarMessageType.OK);
	//										message.setContent(content+"(sent)");
									}
									println(out, new ChatMessage(message.getId(), mLog, receiver, content).toString());
								}else{
									message.setContent("Syntaxe Error");
									message.setStatus(MyStandarMessageType.SYNTAXE_ERROR);
									println(out, message.toString());
								}								
							}else if(ls[1].equalsIgnoreCase("getAllMessages")){
							}else if(ls[1].equalsIgnoreCase("getLogin")){
								message.setContent(mLog);
								message.setStatus(MyStandarMessageType.OK);
								println(out, message.toString());
							}else if(ls[1].equalsIgnoreCase("getAllMessages")){
								
							}else if(ls[1].equalsIgnoreCase("getAllConnectedMembers")){
								String members="";
								for(String s:LOGS.keySet())
								{
									if(!s.equals(mLog)) members+=s+";";
								}
								if(!members.isEmpty())
									members=members.substring(0,members.length()-1);
								message.setContent(members);
								message.setStatus(MyStandarMessageType.OK);
								println(out, message.toString());
							}else if(ls[1].matches("exit|quit")){
								message.setStatus(MyStandarMessageType.EXIT);
								message.setContent("Good By");
								println(out, message.toString());
								Message msg=new Message(0,mLog, MyStandarMessageType.ON_MEMBER_DISCONNECTION);
								LOGS.remove(mLog);
								for(Traitement t:mTraitements){
									if(t.isAuthentificated && this!=t)
										t.println(t.out, msg.toString());
								}
								isAuthentificated=false;
								close();
								break;
							}
							else{
								message.setContent("Syntaxe Error");
								message.setStatus(MyStandarMessageType.SYNTAXE_ERROR);
								println(out, message.toString());
							}
						}
						else if(ls[1].matches("exit|quit")){
							message.setStatus(MyStandarMessageType.EXIT);
							message.setContent("Good By");
							println(out, message.toString());
							close();
							break;
						}else{
							message.setContent("Syntaxe Error");
							message.setStatus(MyStandarMessageType.SYNTAXE_ERROR);
							println(out, message.toString());
						}
						
					}catch(SocketException e){
						close();
						break;
					}catch(SocketTimeoutException e){
						close();
						break;
					}
				}
			}catch(IOException e){
				close();
			}
		}
		private ChatThread openChannelTo(String other) {
			if(LOGS.containsKey(other)){				
				ChatThread thread=new ChatThread(mLog,other);
				messagesTables.put(thread, new ArrayList<ChatMessage>());
				return thread;
			}
			return null;
		}
		private boolean writeTo(String other,String msg, Message message) throws IOException{
			if(LOGS.containsKey(other)){
				//ok
				ChatThread th=null;
				for(ChatThread t:messagesTables.keySet()){
					if((t.log1.equals(other) && t.log2.equals(mLog)) || (t.log1.equals(mLog) && t.log2.equals(other))){
						//il y a un thread entre les deux
						th=t;
						break;
					}
				}
				th=openChannelTo(other);
				if(th!=null){
					ChatMessage cmsg=new ChatMessage(0,mLog, other, msg);
					cmsg.setStatus(MyStandarMessageType.MESSAGE);
					messagesTables.get(th).add(cmsg);
					LOGS.get(other).getOutputStream().write((cmsg.toString()+"\n").getBytes());
					message.setStatus(MyStandarMessageType.MESSAGE);
					message.setContent(msg+"(sent)");
				}else{
					message.setStatus(MyStandarMessageType.SEND_MESSAGE_ERROR);
					message.setContent("Enable to establish channel with other user");
				}
				return true;
			}else{
				message.setStatus(MyStandarMessageType.SEND_MESSAGE_ERROR);
				message.setContent("No connected client found");
				return false;
			}
		}
		private String readLine(BufferedReader r) throws SocketTimeoutException,IOException,SocketException{
			String l="";
			try {
				l=r.readLine();
			}
			catch(SocketException e){
				throw e;
			}
			catch(SocketTimeoutException e){
				throw e;
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				throw e;
			}
			return l;
		}
		private void println(PrintWriter out,String str){
			out.println(str);
			out.flush();
		}
		private void print(PrintWriter out,String str){
			out.print(str);
			out.flush();
		}
	}
	private static class ChatThread{
		String log1,log2;
		private ChatThread(String l1,String l2){
			log1=l1;
			log2=l2;
		}
		
	}
}
