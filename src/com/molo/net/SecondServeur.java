package com.molo.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.spec.RSAPublicKeySpec;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import com.molo.DateUtils;
import com.molo.Main;
import com.molo.Utils;
import com.molo.dao.AttachmentDao;
import com.molo.dao.ChatMessageDao;
import com.molo.dao.ChatThreadDao;
import com.molo.dao.MembreDao;
import com.molo.dbconnection.DBHandler;
import com.molo.entity.Attachment;
import com.molo.entity.Membre;
import com.molo.ftp.MFtp;
import com.molo.message.Message;
import com.molo.message.MyStandarMessageType;
import com.molo.message.PushMessage;
import com.molo.net.SecondServeur.CallInfo.Status;
import com.molo.security.CSAREncryption;
import com.molo.security.Encrypt;
import com.molo.security.RSAEncryption;


public class SecondServeur {
	final static List<Pair<Membre,Socket>> LOGS=new ArrayList<SecondServeur.Pair<Membre,Socket>>();
	final static List<LogKey> LOGKEYS=new ArrayList<>();//fonctinnalité à implementer
	private static List<VoipProcess> voipProcessList=new ArrayList<>();
	//public final static String PASSWORD="molobala",LOGIN="molo";
	final static List<Traitement> mTraitements=new ArrayList<>();
	//Map<ChatThread, List<ChatMessage>> messagesTables=new ConcurrentHashMap<SecondServeur.ChatThread, List<ChatMessage>>();
	private static List<CallThread> calls=new ArrayList<>();
	ServerSocket mServeurSock=null;
	ServerSocket mVoipSock=null;
	Thread voipThread;
	//private static Map<String,>
	private boolean go=true;
	private DBHandler dbh=null;
	public SecondServeur(int p) throws IOException {
		// TODO Auto-generated constructor stub
		try {
			dbh=new DBHandler();
		} catch (SQLException e) {
			// TODO: handle exception
			return;
		}
		//System.out.println("oui");
		mServeurSock=new ServerSocket(p);
		mServeurSock.setSoTimeout(30*60000);
		mVoipSock=new ServerSocket(p+1);
		mVoipSock.setSoTimeout(30*60000);
		voipThread=new Thread(()->{
			System.out.println("Listen to voip client...");
			while(go && !mVoipSock.isClosed()){
				try {
					Socket s= mVoipSock.accept();
					VoipProcess call=new VoipProcess(s);
					System.out.println("New voip client connected");
					pushVoipProcess(call);
					call.start();
					Thread.sleep(100);
				}catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					break;
				} 
				catch (Exception e) {
					// TODO Auto-generated catch block
					break;
				}
			}
		});
		//run();
		
	}
	public void stop() {
		this.go=false;
	}
	public void run(){
		System.out.println("En ecoute des clients");
		if(voipThread.getState().equals(Thread.State.NEW))
			voipThread.start();
		while(go){
			try {
				Socket s= mServeurSock.accept();
				Traitement t=new Traitement(s);
				pushTraitement(t);
				System.out.println("Connexion d'un client:"+s.getInetAddress());
				t.start();
				Thread.sleep(100);
			}catch (SocketTimeoutException e) {
				//mMainThread.interrupt();
				break;
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				break;
			} catch (IOException e) {
				//mMainThread.interrupt();
				break;
			}
			
		}
		try {
			mServeurSock.close();
			mVoipSock.close();
			voipThread.interrupt();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Arret du serveur");
		synchronized (mTraitements) {
			for(Traitement t:mTraitements){
				t.onStopServer();
				//mTraitements.remove(t);
			}
			
		}
		for(VoipProcess p:voipProcessList){
			p.onStopServer();
		}
		mTraitements.clear();
		voipProcessList.clear();
	}
	private class Traitement extends Thread{
		Socket mSocket=null;
		private final ObjectMapper mapper;
		private boolean isAuthentificated=false;
		private DataInputStream mIn=null;
		private DataOutputStream mOut=null;
		private ChatMessageDao messageManager;
		private ChatThreadDao threadManager;
		private MembreDao memberManager;
		private AttachmentDao attachmentManager;
		private Membre mMember;
		private LinkedList<PushMessage> pushMessages=new LinkedList<PushMessage>();
		private Thread pushDeliverer;
		private boolean canPush=true;
		private CallInfo callInfo=new CallInfo();
		private boolean isCalling=false;
		private KeyPair rsaKeys=null;
		private String csarKey=null;
		public Traitement(Socket s) {
			// TODO Auto-generated constructor stub
			this.mSocket=s;
			mapper=new ObjectMapper();
			mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			messageManager=new ChatMessageDao(dbh);
			memberManager=new MembreDao(dbh);
			threadManager=new ChatThreadDao(dbh);
			attachmentManager=new AttachmentDao(dbh);
			pushDeliverer=new Thread(()->{
				boolean exit=false;
				while(this.mSocket.isConnected()){
					while(!canPush || pushMessages.isEmpty()){
						try {
							Thread.sleep(200);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							//e.printStackTrace();
							exit=true;
							break;
						}
					}
					if(!exit){
						System.out.println(getLogin()+": PushMessage size: "+pushMessages.size());
						PushMessage notification=pushMessages.pop();
						println(createResponse(0, notification.status, notification.data));						
					}
				}
			});
		}
		public String getLogin(){
			return (mMember!=null)?mMember.getLogin():"";
		}
		public int getMemberId(){
			return (mMember!=null)?mMember.getId():-1;
		}
		public void onStopServer() {
			// TODO Auto-generated method stub
			Message msg=new Message(0,"Serveur Stoped", MyStandarMessageType.EXIT);
			println(createResponse(0, MyStandarMessageType.SERVER_CLOSED, msg));
			try {
				this.mSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		private void close(){
			try{
				if(isAuthentificated && mMember!=null)
				{
					synchronized (LOGS) {
						for(Iterator<Pair<Membre, Socket>> it=LOGS.iterator();it.hasNext();){
							Pair<Membre, Socket> p=it.next();
							if(p.second==this.mSocket)
								it.remove();
						}	
					}
				}
				mSocket.close();
			}catch(IOException e){
			}
			this.interrupt();
			if(pushDeliverer.isAlive())
				pushDeliverer.interrupt();
			removeFromTraitement(this);
			if(mMember!=null) System.out.println("Disconnexion of "+mMember.getLogin());
		}
		private void genKey(){
			csarKey=CSAREncryption.genKey(CSAREncryption.ALPHABET);
			rsaKeys=RSAEncryption.KeyManager.genKey(1024);
		}
		private String encryptCSAR(final String text){
			if(this.csarKey!=null){
				return CSAREncryption.encrypt(text, CSAREncryption.ALPHABET, this.csarKey);
			}
			return new String(text);
		}
		private String decryptCSAR(final String text){
			if(csarKey!=null){
				return CSAREncryption.decrypt(text, CSAREncryption.ALPHABET, this.csarKey);
			}
			return new String(text);
		}
		
		@Override
		public void run() {
			if(pushDeliverer.getState()==Thread.State.NEW)
				pushDeliverer.start();
			try{
				System.out.println("Commencement du traitement");
				mIn=new DataInputStream(new BufferedInputStream((mSocket.getInputStream())));
				//mOut=new PrintWriter(new OutputStreamWriter(mSocket.getOutputStream(),"utf-8"));
				mOut=new DataOutputStream(new BufferedOutputStream(mSocket.getOutputStream()));
				String line="";
				//authentification
				int authentificatCounter=0;
				Message message=new Message();
				message.setContent("Bienvenu");
				message.setStatus(MyStandarMessageType.OK);
				println(createResponse(0,MyStandarMessageType.OK, message));
				//println(out,  message.toString());
				while(mSocket.isConnected()){
					if(authentificatCounter>=6)
						break;
					try{
						line=readLine();
						if(line==null)
							break;
						//on crée un objet JSonNode
						JsonNode request=mapper.readTree(line);
						long comId=0;
						if(!request.has("hash") || !request.has("command")){
							message.setContent("Syntaxe error");
							message.setStatus(MyStandarMessageType.SYNTAXE_ERROR);
							message.setId(0);
							println(createResponse(0,MyStandarMessageType.SYNTAXE_ERROR, message));
							continue;
						}
						String command=request.get("command").asText();
						comId=request.get("hash").asLong();
						System.out.println(line);
						message.setId(comId);
						if(!isAuthentificated){
							if(command.equalsIgnoreCase("authentificate")){
								//demande d'authentification
								if(request.has("login") && request.has("password")){
									String log=request.get("login").asText(),pwd=request.get("password").asText();
									//authentification
									Membre m=memberManager.getOne(log,pwd);
									if(m!=null){
										//success
										isAuthentificated=true;
										mMember=m;
										//LOGS.add(new Pair<>(m, mSocket));
										pushToLOGS(new Pair<>(m, mSocket));
										//on ouvre une session
										LogKey k=new LogKey();
										genKey();
										if(request.has("remember")){
											boolean r=request.get("remember").asBoolean();
											if(r){
												//we create a new key 
												k.login=new String(mMember.getLogin());
												k.key=LogKey.generateKey(mMember.getLogin());
												k.date=new Date();
												LOGKEYS.add(k);
											}
										}
										MembreWrapper mw=new MembreWrapper(mMember);
										mw.key=k.key;
										mw.isOnline=true;
//										message.setStatus(MyStandarMessageType.AUTHENTIFICATED_SUCCESSFULLY);
//										message.setContent("Connected successfully! Your login is: "+mMember.getLogin());
										//message.setId(comId);
										//System.out.println(message.toString());
										println( createResponse(comId,MyStandarMessageType.AUTHENTIFICATED_SUCCESSFULLY,mw));
										MembreWrapper wrapper=new MembreWrapper(mMember);
										wrapper.isOnline=true;
										synchronized (mTraitements) {
											for(Traitement t:mTraitements){
												if(t.isAuthentificated() && t!=this && !t.mMember.getLogin().equals(getLogin())){
													//t.println(t.mOut, t.createResponse(0,MyStandarMessageType.ON_NEW_MEMBER_CONNECTION,mMember));
													t.pushNotification(0, MyStandarMessageType.ON_NEW_MEMBER_CONNECTION, wrapper);
												}
											}	
										}
										
									}else{
										authentificatCounter++;
										message.setStatus(MyStandarMessageType.AUTHENTIFICATION_ERROR);
										message.setContent("Login and password incorrect");
										println( createResponse(comId,MyStandarMessageType.AUTHENTIFICATION_ERROR,message));
									}
								}else{
									message.setStatus(MyStandarMessageType.SYNTAXE_ERROR);
									message.setContent("Syntaxe Error:you should specify the login and the password");
									println( createResponse(comId,MyStandarMessageType.SYNTAXE_ERROR,message));
								}
							}else if(command.equalsIgnoreCase("register")){
								//demande d'enregistrement
								int status=0;
								if(request.has("login") && request.has("password")){
									//les champ obligatoires present
									String log=request.get("login").asText();
									String pwd=request.get("password").asText();
									if(!log.isEmpty() && log.matches("^[a-zA-Z0-9_]+([a-zA-Z0-9_]|[\\s])*")
											&& !pwd.isEmpty() && pwd.length()>6){
										String nom=null,prenom=null;
										if(request.has("name"))
											nom=request.get("name").asText();
										if(request.has("fname"))
											prenom=request.get("fname").asText();
										memberManager.insert(new Membre(0, nom, prenom, log, pwd));
										message.setStatus(MyStandarMessageType.OK);
										status=MyStandarMessageType.OK;
										message.setContent("You have been registered with succes");
									}else{
										message.setStatus(MyStandarMessageType.REGISTER_ERROR);
										message.setContent("Registration failled:Login format invalid or password format invalid");
										status=MyStandarMessageType.REGISTER_ERROR;
									}
								}else{
									message.setStatus(MyStandarMessageType.SYNTAXE_ERROR);
									message.setContent("Syntaxe error!Must specify login and password");
									status=MyStandarMessageType.SYNTAXE_ERROR;
								}
								println( createResponse(comId,status, message));
							}else if(command.equalsIgnoreCase("authentificateWithKey")){
								if(request.has("key")){
									String key=request.get("key").asText();
									for(LogKey k:LOGKEYS){
										if(k.equals(key)){
											//success
											String log=k.login;
											//authentification
											Membre m=memberManager.getOne(log);
											if(m!=null){
												//success
												isAuthentificated=true;
												mMember=m;
												genKey();
												//LOGS.add(new Pair<>(m, mSocket));
												pushToLOGS(new Pair<>(m, mSocket));
												//on ouvre une session
												MembreWrapper wrapper=new MembreWrapper(mMember);
												wrapper.isOnline=true;
												synchronized (mTraitements) {
													for(Traitement t:mTraitements){
														if(t.isAuthentificated() && t!=this && !t.mMember.getLogin().equals(getLogin())){
															//t.println(t.mOut, t.createResponse(0,MyStandarMessageType.ON_NEW_MEMBER_CONNECTION,mMember));
															t.pushNotification(0, MyStandarMessageType.ON_NEW_MEMBER_CONNECTION, wrapper);
														}
													}	
												}
											}else{
												authentificatCounter++;
												message.setStatus(MyStandarMessageType.AUTHENTIFICATION_ERROR);
												message.setContent("Login and password incorrect");
												println( createResponse(comId,MyStandarMessageType.AUTHENTIFICATION_ERROR,message));
											}
										}else{
											authentificatCounter++;
											message.setStatus(MyStandarMessageType.AUTHENTIFICATION_ERROR);
											message.setContent("No saved key found");
											println( createResponse(comId,MyStandarMessageType.AUTHENTIFICATION_ERROR,message));
										}
									}
								}
							}
						}else if(isAuthentificated){
							//operation after authentificaion
							MediaFile[] medias=null;
							if(request.has("files")){
								System.out.println("Has Media");
								medias= mapper.readValue(request.get("files"),MediaFile[].class);
								if(medias!=null){
									
//									for(MediaFile m:medias)
//										System.err.println("MEDIASSS");
									for(MediaFile m:medias){
										System.out.println("MEDIA: "+m.name);
										m.file=MFtp.ftpGetFile(mIn, Main.TEMP_DIR);
									}
									
								}
							}
							if(command.matches("updateProfil")){
								if(medias!=null && medias.length>0){
									MediaFile prof=medias[0];
									if(mMember.getLogin().isEmpty()){
										File ancienFile=new File(mMember.getProfil());
										Files.deleteIfExists(ancienFile.toPath());
									}
									String newProf=Main.USERS_FILE_DATA_DIR+"/prof_"+mMember.getLogin()+"_"+new Date().getTime()+"."+Utils.getFileExtension(prof.name);
									Files.move(prof.file.toPath(), new File(newProf).toPath(), StandardCopyOption.REPLACE_EXISTING);
									memberManager.updateProfil(mMember.getId(),newProf);
									mMember.setProfil(newProf);
									//dispacthing the update profil to all clients
									ObjectNode n=mapper.createObjectNode();
									n.put("profil", newProf);
									n.put("login", mMember.getLogin());
									println( createResponse(comId, MyStandarMessageType.OK, n));
									this.dispatchMessageToAllFriends(n,MyStandarMessageType.PROFIL_UPDATED);
								}else{
									message.setContent("No file sent");
									message.setId(comId);
									message.setStatus(MyStandarMessageType.SYNTAXE_ERROR);
									println( createResponse(comId, MyStandarMessageType.SYNTAXE_ERROR, message));
								}
							}else if(command.matches("getUserProfil")){
								if(request.has("user")){
									String userLog=request.get("user").asText();
									Membre m=memberManager.getOne(userLog);
									MediaFile f=new MediaFile();
									f.file=new File(m.getProfil());
									f.name=f.file.getName();
									f.size=f.file.length();
									f.type="image/png";
									ObjectNode n=mapper.createObjectNode();
									n.put("cache", true);
									System.out.println("SIZE: "+f.size+" ;; "+f.size);
									println( createResponse(comId, MyStandarMessageType.OK, n,f));
									mOut.flush();
									sendFile(f);
								}else{
									message.setContent("Yous should specify user field");
									message.setId(comId);
									message.setStatus(MyStandarMessageType.SYNTAXE_ERROR);
									println( createResponse(comId, MyStandarMessageType.SYNTAXE_ERROR, message));
								}
							}else if(command.matches("getPublicKey")){
								if(rsaKeys!=null){
									ObjectNode n=mapper.createObjectNode();
									RSAPublicKeySpec spec=RSAEncryption.KeyManager.getPublicKeySpecification(rsaKeys.getPublic());
									n.put("mod",spec.getModulus().toString());
									n.put("exp",spec.getPublicExponent().toString());
									println(createResponse(comId, MyStandarMessageType.OK, n));									
								}else{
									message.setContent("No key is genereted");
									message.setId(comId);
									message.setStatus(MyStandarMessageType.ERROR);
									println( createResponse(comId, MyStandarMessageType.ERROR, message));
								}
							}else if(command.matches("getCSARKey")){
								if(csarKey!=null){
									ObjectNode n=mapper.createObjectNode();
									String csarKeyEnc=Base64.getEncoder().encodeToString(RSAEncryption.encrypt(csarKey.getBytes(StandardCharsets.UTF_8), rsaKeys.getPrivate()));
									n.put("key", csarKeyEnc);
									println(createResponse(comId, MyStandarMessageType.OK, n));									
								}else{
									message.setContent("No key is genereted");
									message.setId(comId);
									message.setStatus(MyStandarMessageType.ERROR);
									println( createResponse(comId, MyStandarMessageType.ERROR, message));
								}
							}
							else if(command.matches("call")){
								if(request.has("other")){
									String other=request.get("other").asText();
									if(call(other)){
										println(createResponse(comId, MyStandarMessageType.OK,other));
									}else{
										println(createResponse(comId, MyStandarMessageType.UNREACHABLE,other+" is unreachable" ));
									}
								}else{
									message.setContent("other field not specified");
									message.setId(comId);
									message.setStatus(MyStandarMessageType.SYNTAXE_ERROR);
									println( createResponse(comId, MyStandarMessageType.SYNTAXE_ERROR, message));
								}
							}else if(command.matches("answerCall")){
								if(request.has("other")){
									if(answerCall(request.get("other").asText())){
										ObjectNode n=mapper.createObjectNode();
										n.put("other", request.get("other").asText());
										n.put("label", callInfo.currentLabel);
										println(createResponse(comId, MyStandarMessageType.OK, n));
									}
								}else{
									message.setContent("other field not specified");
									message.setId(comId);
									message.setStatus(MyStandarMessageType.SYNTAXE_ERROR);
									println( createResponse(comId, MyStandarMessageType.SYNTAXE_ERROR, message));
								}
							}else if(command.matches("cancelCall")){
								if(request.has("other")){
									String other=request.get("other").asText();
									System.out.println("Cancelling call "+other);
									cancelCall(request.get("other").asText());
									println(createResponse(comId, MyStandarMessageType.OK, "call canceled"));
									
								}else{
									message.setContent("other field not specified");
									message.setId(comId);
									message.setStatus(MyStandarMessageType.SYNTAXE_ERROR);
									println( createResponse(comId, MyStandarMessageType.SYNTAXE_ERROR, message));
								}
							}
							else if(command.matches("updateUnread")){
								if(request.has("thread")){
									long tid=request.get("thread").asLong();
									com.molo.entity.ChatThread th=threadManager.getOne(tid);
									threadManager.setUnread(tid,0);
									message.setStatus(MyStandarMessageType.OK);
									message.setContent("updated");
									println(createResponse(comId, message.getStatus(), message));
									//send read notification to all concerned clients
									if(th!=null){
										String other=th.getMember1().equals(mMember.getLogin())?th.getMember2():th.getMember1();
										synchronized (mTraitements) {
											ObjectNode n=mapper.createObjectNode();
											n.put("thread", tid);
											for(Traitement t:mTraitements){
												if((t.isAuthentificated() && t.mMember.equals(this.mMember) && t!=this) || 
														t.mMember.getLogin().equals(other)){
													//t.println(t.mOut, t.createResponse(0, MyStandarMessageType.ON_MESSAGE_SEEN, n));
													t.pushNotification(0, MyStandarMessageType.ON_MESSAGE_SEEN, n);
												}
											}
										}
									}
								}else{
									message.setStatus(MyStandarMessageType.SYNTAXE_ERROR);
									message.setContent("Syntaxe error!Must specify the thread id");
									println(createResponse(comId, message.getStatus(), message));
								}
							}else if(command.matches("fput")){
								//we retrieve file informations from the commande arguments
								if(request.has("fname") && request.has("fsize") && request.has("ftype")){
									String fname=request.get("fname").asText(),
											ftype=request.get("ftype").asText();
									long fsize=request.get("fsize").asLong();
									fname=Main.USERS_FILE_DATA_DIR+"/"+mMember.getLogin()+(new Date()).getTime()+"."+Utils.getFileExtension(fname);
									message.setContent(fname);
									message.setStatus(MyStandarMessageType.OK);
									println(createResponse(comId, MyStandarMessageType.OK, message));
									MFtp.ftpGetFile(mIn, Main.TEMP_DIR);
								}else{
									message.setStatus(MyStandarMessageType.SYNTAXE_ERROR);
									message.setContent("Syntaxe error!fname fsize ftype should be specified");
									println(createResponse(comId, MyStandarMessageType.SYNTAXE_ERROR, message));
								}
							}else if(command.matches("fget")){
								if(request.has("name")){
									String fname=request.get("name").asText();
									message.setContent(fname);
									message.setStatus(MyStandarMessageType.OK);
									MediaFile m=new MediaFile();
									m.file=new File(fname);
									if(m.file.exists()){
										m.name=m.file.getName();
										m.size=m.file.length();
										m.type=Utils.getFileType(fname);
										println(createResponse(comId, MyStandarMessageType.OK, message,m));
										canPush=false;
										MFtp.ftpPut(m.file, mOut);
										canPush=true;
									}else{
										message.setStatus(MyStandarMessageType.NOT_FOUND);
										message.setContent("No file found");
										println(createResponse(comId, MyStandarMessageType.NOT_FOUND, message));
									}
								}else{
									message.setStatus(MyStandarMessageType.SYNTAXE_ERROR);
									message.setContent("Syntaxe error!fname should be specified");
									println(createResponse(comId, MyStandarMessageType.SYNTAXE_ERROR, message));
								}
							}

							else if(command.equalsIgnoreCase("postmessage")){
								//retrive receiver and msg content
								if(request.has("receiver") && request.has("content")){
									String receiver=request.get("receiver").asText();
									if(!receiver.equals(getLogin())){
										String content=request.get("content").asText();//content is encrypted with csar if encrypt field is true
										System.out.println("MESSAGE ENC: "+content);										
										if(request.has("encrypt") && request.get("encrypt").asBoolean()){
											content=decryptCSAR(content);
										}
										System.out.println("MESSAGE: "+content);
										boolean isVoiceMessage=false;
										if(medias!=null && medias.length>0){
											if(medias[0].type.equalsIgnoreCase("audio/wav")){
												isVoiceMessage=true;
											}
										}
										ChatMessageWrapper w=writeTo(receiver, content,isVoiceMessage,medias);
										if(w!=null)
										{
											println( createResponse(comId,MyStandarMessageType.OK, w));
											//dispatch the send event to all connected member with the same login
											synchronized (mTraitements) {
												for(Traitement t:mTraitements){
													if(t!=this && t.getLogin().equals(getLogin())){
														//t.println(t.mOut, t.createResponse(0, MyStandarMessageType.MESSAGE_SENT, w));
														t.pushNotification(0, MyStandarMessageType.MESSAGE_SENT, w);
													}
												}	
											}
										}else{
											message.setContent("Error while sending message");
											message.setStatus(MyStandarMessageType.SEND_MESSAGE_ERROR);
											println( createResponse(comId, message.getStatus(), message));
										}
									}
									else{
										message.setContent("You can't message yourself");
										message.setStatus(MyStandarMessageType.SEND_MESSAGE_ERROR);
										println( createResponse(comId, message.getStatus(), message));
									}
								}else{
									message.setContent("Syntaxe Error");
									message.setStatus(MyStandarMessageType.SYNTAXE_ERROR);
									println( createResponse(comId,MyStandarMessageType.SYNTAXE_ERROR,message));
								}
							}else if(command.equalsIgnoreCase("getAllThreads")){
								//retrieve all thread for that member
								if(mMember!=null){
									List<CThreadWrapper> threads=new ArrayList<CThreadWrapper>();
									List<com.molo.entity.ChatThread> tmps=threadManager.getAllForAMember(mMember.getLogin());;										
									if(!tmps.isEmpty()){
										for(com.molo.entity.ChatThread c:tmps)
										{
											CThreadWrapper w=new CThreadWrapper();
											w.id=c.getId();
											com.molo.entity.ChatMessage lM=messageManager.getLastMessage(c.getId());
											if(lM!=null){
												w.lastMessage=lM.getContent();
												w.lastSender=lM.getSender();
												w.lastActiveTime=lM.getDate();
											}
											else{
												w.lastMessage="";
												w.lastSender="";
												w.lastActiveTime="";
											}
											w.self=mMember.getLogin();
											w.other=(c.getMember1().equals(mMember.getLogin())?c.getMember2():c.getMember1());
											w.profil=memberManager.getOneProfilPath(w.other);
											w.date=c.getDate();
											w.unread=c.getUnread();
											
											w.messageCount=messageManager.getCountFor(c.getId());
											synchronized (mTraitements) {
												for(Traitement t:mTraitements){
													if(t.mMember!=null && w.other.equals(t.mMember.getLogin())){
														w.isOnline=true;
														break;
													}
												}
											}
											threads.add(w);
										}
									}
									println( createResponse(comId, MyStandarMessageType.OK, threads));
								}else{
									message.setContent("You're not authentificate yet");
									message.setStatus(MyStandarMessageType.ERROR);
									println( createResponse(comId, MyStandarMessageType.ERROR, message));
								}
							}
							else if(command.equalsIgnoreCase("getThreads")){
								//retrieve all thread for that member
								if(request.has("page") && request.has("size")){
									if(mMember!=null){
										int page=request.get("page").asInt(),size=request.get("size").asInt();
										List<CThreadWrapper> threads=new ArrayList<CThreadWrapper>();
										List<com.molo.entity.ChatThread> tmps=threadManager.getAllForAMember(mMember.getLogin());
										if(!tmps.isEmpty()){
											for(int d=page*size,i=d;i<d+size && i<tmps.size();i++)
											{
												com.molo.entity.ChatThread c=tmps.get(i);
												CThreadWrapper w=new CThreadWrapper();
												w.id=c.getId();
												com.molo.entity.ChatMessage lM=messageManager.getLastMessage(c.getId());
												if(lM!=null){
													w.lastMessage=lM.getContent();
													w.lastSender=lM.getSender();
													w.lastActiveTime=lM.getDate();
												}
												else{
													w.lastMessage="";
													w.lastSender="";
													w.lastActiveTime="";
												}
												w.self=mMember.getLogin();
												w.other=(c.getMember1().equals(mMember.getLogin())?c.getMember2():c.getMember1());
												w.profil=memberManager.getOneProfilPath(w.other);
												w.date=c.getDate();
												w.unread=c.getUnread();
												w.messageCount=messageManager.getCountFor(c.getId());
												synchronized (mTraitements) {
													for(Traitement t:mTraitements){
														if(t.isAuthentificated && w.other.equals(t.mMember.getLogin())){
															w.isOnline=true;
															break;
														}
													}
												}
												threads.add(w);
											}
										}
										println( createResponse(comId, MyStandarMessageType.OK, threads));
									}else{
										message.setContent("You're not authentificate yet");
										message.setStatus(MyStandarMessageType.ERROR);
										println( createResponse(comId, MyStandarMessageType.ERROR, message));
									}	
								}else{
									message.setContent("Syntaxe Error");
									message.setStatus(MyStandarMessageType.SYNTAXE_ERROR);
									println( createResponse(comId,MyStandarMessageType.SYNTAXE_ERROR,message));
								}
							}
							else if(command.equalsIgnoreCase("getAllMessages")){
								//retrieve all message for a thread
								if(request.has("other")){
									String login=request.get("other").asText();
									Membre m=memberManager.getOne(login);
									if(m!=null){
										com.molo.entity.ChatThread cth=threadManager.getFor(getLogin(), m.getLogin());
										List<ChatMessageWrapper> messages=new ArrayList<ChatMessageWrapper>();
										if(cth!=null){											
											List<com.molo.entity.ChatMessage> tmps=messageManager.getAllForAThread(cth.getId());
											for(com.molo.entity.ChatMessage msg:tmps){
												ChatMessageWrapper w=new ChatMessageWrapper(msg);
												List<Attachment> atts=attachmentManager.getAllForAMessage(msg.getId());
												if(atts.size()==1 && atts.get(0).getType().equalsIgnoreCase("audio/wav")){
													w.isVoiceMessage=true;
												}else{
													w.isVoiceMessage=false;
												}
												w.attachments=atts;
												w.content=encryptCSAR(w.content);
												messages.add(w);
											}
										}
										println( createResponse(comId, MyStandarMessageType.OK, messages));
									}else{
										message.setContent("No Member found");
										message.setStatus(MyStandarMessageType.ERROR);
										println( createResponse(comId, MyStandarMessageType.ERROR, message));
									}
								}else{
									message.setContent("Syntaxe Error");
									message.setStatus(MyStandarMessageType.SYNTAXE_ERROR);
									println( createResponse(comId,MyStandarMessageType.SYNTAXE_ERROR,message));
								}
							}
							else if(command.equalsIgnoreCase("getMessagesOffset")){
								//retrieve all message for a thread
								if(request.has("other") && request.has("offset") && request.has("number")){
									String login=request.get("other").asText();
									Membre m=memberManager.getOne(login);
									if(m!=null){
										com.molo.entity.ChatThread cth=threadManager.getFor(getLogin(), m.getLogin());
										List<ChatMessageWrapper> messages=new ArrayList<ChatMessageWrapper>();
										if(cth!=null){
											int offset=request.get("offset").asInt(),size=request.get("number").asInt();
											List<com.molo.entity.ChatMessage> tmps=messageManager.getAllDescForAThread(cth.getId());
											if(!tmps.isEmpty()){
												for(int d=offset,i=d;i<d+size && i<tmps.size();i++)
												{
													com.molo.entity.ChatMessage msg=tmps.get(i);
													ChatMessageWrapper w=new ChatMessageWrapper(msg);
													List<Attachment> atts=attachmentManager.getAllForAMessage(msg.getId());
													if(atts!=null)
														w.attachments=atts;
													else w.attachments=new ArrayList<>();
													if(atts.size()==1 && atts.get(0).getType().equalsIgnoreCase("audio/wav")){
														w.isVoiceMessage=true;
													}else{
														w.isVoiceMessage=false;
													}
													w.content=encryptCSAR(w.content);
													messages.add(w);
												}
											}
										}
										println( createResponse(comId, MyStandarMessageType.OK, messages));
									}else{
										message.setContent("No Member found");
										message.setStatus(MyStandarMessageType.ERROR);
										println( createResponse(comId, MyStandarMessageType.ERROR, message));
									}
								}else{
									message.setContent("Syntaxe Error");
									message.setStatus(MyStandarMessageType.SYNTAXE_ERROR);
									println( createResponse(comId,MyStandarMessageType.SYNTAXE_ERROR,message));
								}
							}
							else if(command.equalsIgnoreCase("getMessages")){
								//retrieve all message for a thread
								if(request.has("other") && request.has("page") && request.has("size")){
									String login=request.get("other").asText();
									Membre m=memberManager.getOne(login);
									if(m!=null){
										com.molo.entity.ChatThread cth=threadManager.getFor(getLogin(), m.getLogin());
										List<ChatMessageWrapper> messages=new ArrayList<ChatMessageWrapper>();
										if(cth!=null){
											int page=request.get("page").asInt(),size=request.get("size").asInt();
											List<com.molo.entity.ChatMessage> tmps=messageManager.getAllDescForAThread(cth.getId());
											if(!tmps.isEmpty()){
												for(int d=page*size,i=d;i<d+size && i<tmps.size();i++)
												{
													com.molo.entity.ChatMessage msg=tmps.get(i);
													ChatMessageWrapper w=new ChatMessageWrapper(msg);
													List<Attachment> atts=attachmentManager.getAllForAMessage(msg.getId());
													if(atts!=null)
														w.attachments=atts;
													else w.attachments=new ArrayList<>();
													if(atts.size()==1 && atts.get(0).getType().equalsIgnoreCase("audio/wav")){
														w.isVoiceMessage=true;
													}else{
														w.isVoiceMessage=false;
													}
													w.content=encryptCSAR(w.content);
													messages.add(w);
												}
											}
										}
										println( createResponse(comId, MyStandarMessageType.OK, messages));
									}else{
										message.setContent("No Member found");
										message.setStatus(MyStandarMessageType.ERROR);
										println( createResponse(comId, MyStandarMessageType.ERROR, message));
									}
								}else{
									message.setContent("Syntaxe Error");
									message.setStatus(MyStandarMessageType.SYNTAXE_ERROR);
									println( createResponse(comId,MyStandarMessageType.SYNTAXE_ERROR,message));
								}
							}
							else if(command.equalsIgnoreCase("getLogin")){
								Membre m=new Membre(getMemberId(), this.mMember.getNom(), this.mMember.getPrenom(), this.mMember.getLogin(), "");
								println( createResponse(comId,MyStandarMessageType.OK,m));
							}else if(command.equalsIgnoreCase("getMessagePage")){
								
							}else if(command.equalsIgnoreCase("getAllConnectedMembers")){
								//String members="";
								List<Membre> members=new ArrayList<>();
								synchronized (LOGS) {
									System.out.println("LOGS : "+LOGS.size());
									for(Pair<Membre, Socket> s:LOGS)
									{
										if(!s.first.getLogin().equals(mMember.getLogin())){
											//members+=s+";";
											if(!members.contains(s.first))
												members.add(s.first);
										}
									}									
								}
								println( createResponse(comId,MyStandarMessageType.OK,members));
							}else if(command.equalsIgnoreCase("getAllMembers")){
								List<MembreWrapper> members=new ArrayList<SecondServeur.MembreWrapper>();
								List<Membre> tmps=memberManager.getAll();
								for(Membre m:tmps){
									if(!m.equals(mMember)){
										MembreWrapper w=new MembreWrapper(m);
										w.isOnline=isOnLine(m);
										w.key="";
										members.add(w);										
									}
								}
								println( createResponse(comId,MyStandarMessageType.OK,members));
							}else if(command.matches("exit|quit")){
								message.setStatus(MyStandarMessageType.EXIT);
								message.setContent("Good By");
								int occ=0;
								synchronized (LOGS) {
									for(Iterator<Pair<Membre, Socket>> it=LOGS.iterator();it.hasNext();){
										Pair<Membre, Socket> p=it.next();
										if(p.second==mSocket)
										{
											it.remove();
											continue;
										}
										if(p.first.equals(mMember))
										{
											occ++;
											System.err.println("P.FIRS : "+p.first.getLogin());
										}
									}	
								}
								System.err.println("OCC: "+occ);
								if(occ==0){
									MembreWrapper wrapper=new MembreWrapper(mMember);
									wrapper.isOnline=false;
									synchronized (mTraitements) {
										for(Traitement t:mTraitements){
											if(t.isAuthentificated() && this!=t && !t.mMember.equals(mMember))
											{
												//t.println(t.mOut,t.createResponse(0, MyStandarMessageType.ON_MEMBER_DISCONNECTION,mMember));
												t.pushNotification(0, MyStandarMessageType.ON_MEMBER_DISCONNECTION, wrapper);
											}
										}									
									}
								}
								isAuthentificated=false;
								println( createResponse(comId,MyStandarMessageType.EXIT,message));
								close();
								//break;
							}
							else{
								message.setContent("Syntaxe Error");
								message.setStatus(MyStandarMessageType.SYNTAXE_ERROR);
								println( createResponse(comId,MyStandarMessageType.SYNTAXE_ERROR,message));
							}
							if(medias!=null){
								for(MediaFile f:medias){
									Files.deleteIfExists(f.file.toPath());
								}
							}
						}
						else if(command.matches("exit|quit")){
							message.setStatus(MyStandarMessageType.EXIT);
							message.setContent("Good By");
							println(createResponse(comId,MyStandarMessageType.EXIT,message));
							close();
							break;
						}else{
							message.setContent("Syntaxe Error");
							message.setStatus(MyStandarMessageType.SYNTAXE_ERROR);
							println(createResponse(comId,MyStandarMessageType.SYNTAXE_ERROR,message));
						}
						
					}catch(EOFException e){
						close();
						break;
					}
					catch(SocketException e){
						//e.printStackTrace();
						close();
						break;
					}catch(SocketTimeoutException e){
						//e.printStackTrace();
						close();
						break;
					}
				}
			}catch(IOException e){
				e.printStackTrace();
				close();
			}
		}
		private void cancelCall(String other) {
			// TODO Auto-generated method stub
			if(isCalling())
			{
				System.out.println("Cancelling self call to "+other);
				setCallStatus(CallInfo.Status.NONE);
				synchronized (mTraitements) {
					for(Traitement t:mTraitements){
						if(t!=this && t.isAuthentificated() && t.getLogin().equals(other)){
							t.pushNotification(0, MyStandarMessageType.CALL_CANCELED, getLogin());
							t.setCallStatus(CallInfo.Status.NONE);
						}
					}	
				}
			}else{
				System.out.println("Canceling other call from "+other);
				Traitement oth=null;
				synchronized (mTraitements) {
					for(Traitement t:mTraitements){
						if(t!=this && t.isAuthentificated() && t.getLogin().equals(getLogin())){
							t.pushNotification(0, MyStandarMessageType.CALL_CANCELED, other);
						}
						if(oth==null && t.isAuthentificated()  && t.getLogin().equals(other) && t.isCalling())
							oth=t;
					}
				}
				if(oth!=null){
					oth.pushNotification(0, MyStandarMessageType.CALL_CANCELED, getLogin());
				}
			}
			callInfo.other="";
			callInfo.currentLabel="";
			//currentCall=null;
		}
		synchronized private void setCallStatus(Status st) {
			// TODO Auto-generated method stub
			this.callInfo.status=st;
		}
		
		private boolean  answerCall(String other) {
			//dispacth call to all appropriate socket
			if(isCalling())
				return false;//can answer his own call
			Traitement oth=null;
			//dispatch answer event to all connected member whith login self login
			synchronized (mTraitements) {
				for(Traitement t:mTraitements){
					if(t!=this && t.isAuthentificated() && t.getLogin().equals(getLogin())){
						t.pushNotification(0, MyStandarMessageType.CALL_ACCEPTED, other);
					}
					if(oth==null && t.isAuthentificated()  && t.getLogin().equals(other) && t.isCalling())
						oth=t;
				}
			}
			if(oth!=null){
				setCallStatus(Status.CALLING);
				CallThread callThread=new CallThread(this,oth);
				callThread.caller=other;
				callThread.receiver=getLogin();
				callInfo.other=other;
				//callThread.label
				//currentCall=new CallThread(getLogin(), other);
				String label=getLogin()+"_"+other+new Date().getTime();
				try {
					label=Encrypt.encrypt(label,"sha-1");
					callThread.label=label;
					callInfo.currentLabel=label;
					pushCall(callThread);
					ObjectNode n=mapper.createObjectNode();
					n.put("label", label);
					n.put("other",getLogin());
					oth.pushNotification(0, MyStandarMessageType.CALL_ACCEPTED, n);
				} catch (UnsupportedEncodingException
						| NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					//currentCall=null;
					//finishCall();
					return false;
				}
				return true;
			}
			return false;
			//if(currentCall.)
		}
		synchronized public boolean isCalling() {
			// TODO Auto-generated method stub
			return callInfo.status==Status.CALLING;
		}
		private boolean call(String other) {
			// TODO Auto-generated method stub
			//we should save the call as a message and then try to find all socket for 'other' login
			//dispacth call to all appropriate socket
			//currentCall=new CallThread(mMember.getLogin(), other);
			callInfo.status=Status.CALLING;
			callInfo.other=other;
			int occ=0;
			ObjectNode n=mapper.createObjectNode();
			n.put("other", getLogin());
			//dispatch call info to all connected members with login== other
			synchronized (mTraitements) {
				for(Traitement t:mTraitements){
					if(t!=this && t.isAuthentificated() && t.getLogin().equals(other)){
						t.pushNotification(0, MyStandarMessageType.CALL_FROM, n);
						occ++;
					}
				}
			}
			if(occ==0){
				//if no connected member found
				//currentCall=null;
				isCalling=false;
				return false;
			}else{
				isCalling=true;
				return true;
			}
		}
		synchronized public void finishCall() {
			// TODO Auto-generated method stub
			if(!isCalling)
				return;
			pushNotification(0, MyStandarMessageType.CALL_FINISHED, callInfo.other);
			setCallStatus(CallInfo.Status.NONE);
			callInfo.currentLabel="";
			callInfo.other="";
//			if(currentCall!=null){
//				currentCall.endTime=System.currentTimeMillis();
//				currentCall.label="";
//				//save the message
//				//dispacth the cancel call notification
//				for(Traitement t:mTraitements){
//					if(t.isAuthentificated() 
//							&& (t.getLogin().equals(currentCall.self))){
//						t.pushNotification(0, MyStandarMessageType.CALL_CANCELED, mMember);
//					}
//				}
//			}
		}
		synchronized private boolean isAuthentificated() {
			// TODO Auto-generated method stub
			return isAuthentificated;
		}
		synchronized private void sendFile(MediaFile... files) throws IOException {
			// TODO Auto-generated method stub
			canPush=false;
			if(files!=null && files.length >0){
				System.out.println("SendFile");
				for(MediaFile f:files){
					System.out.println("Start sending file");
					MFtp.ftpPut(f.file,mOut);
				}	
			}
			canPush=true;
		}
		private void dispatchMessageToAllFriends(Object n, int status) {
			// TODO Auto-generated method stub
			synchronized (mTraitements) {
				for(Traitement t:mTraitements){
					//if(t!=this) t.println(t.mOut, t.createResponse(0, status, n));
					if(t!=this) t.pushNotification(0, status, n);
				}
			}
		}
		private void pushNotification(long hash, int status, Object data) {
			// TODO Auto-generated method stub
			synchronized (pushMessages) {
				pushMessages.add(new PushMessage(hash, status, data));
			}
		}
		private String createResponse(long hash,int status,Object obj, MediaFile... f){
			ObjectNode n=mapper.createObjectNode();
			n.put("hash", hash);
			n.put("status", status);
			
			try {
				n.put("data", mapper.readTree(mapper.writeValueAsString(obj)));
				if(f!=null && f.length>0) n.put("files", mapper.readTree(mapper.writeValueAsString(f)));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return n.toString();
		}
		
		private ChatMessageWrapper writeTo(String other,String msg,boolean isMessageVocal,MediaFile...attchs) throws IOException{
			//verify if a thread exist between the too member
			Membre m=memberManager.getOne(other);
			if(m==null){
				//aucun membre trouvé
				return null;
			}
			com.molo.entity.ChatThread cth=threadManager.getFor(getLogin(),m.getLogin());
			System.out.println("Mid: "+getMemberId()+"; OID : "+m.getId());
			if(cth==null){
				//we creat a new chat thread
				cth=new com.molo.entity.ChatThread(0, getLogin(), m.getLogin(),DateUtils.getCurrentInString());
				cth=threadManager.insert(cth);//insertion of chatThread
			}
			//we write the new message in the database 
			//we create a new ChatMessage for store that message
			com.molo.entity.ChatMessage cmessage=messageManager.insert(new com.molo.entity.ChatMessage(0, msg,cth.getId(), m.getLogin(), getLogin(),DateUtils.getCurrentInString()));
			if(cmessage==null){
				return null;
			}
			//saving attachment if exist
			ChatMessageWrapper w=new ChatMessageWrapper(cmessage);
			w.isVoiceMessage=isMessageVocal;
			List<Attachment> attachments=new ArrayList<Attachment>();
			if(attchs!=null && attchs.length>0){
				for(MediaFile f:attchs){
					String destFname=Main.USERS_FILE_DATA_DIR+"/"+"mess_"+getLogin()+"_"+new Date().getTime()+"."+Utils.getFileExtension(f.name);
					File destF=new File(destFname);
					Files.move(f.file.toPath(),destF.toPath(), StandardCopyOption.REPLACE_EXISTING);
					Attachment at=new Attachment(0,destFname, f.type, f.file.length(), cmessage.getId());
					at=attachmentManager.insert(at);
					attachments.add(at);
				}
			}
			w.attachments=attachments;
			System.out.println("CMESS: "+w.content);
			//We try to transfert that message to the client if connected already
			synchronized(mTraitements){
				for(Traitement t:mTraitements){
					if(t.getLogin().equals(other)){
						ChatMessageWrapper tmpW=new ChatMessageWrapper(cmessage);
						tmpW.isVoiceMessage=isMessageVocal;
						//receiver is on line we send so the new message
						//t.println(t.mOut, t.createResponse(0, MyStandarMessageType.MESSAGE, cmessage));
						tmpW.content=t.encryptCSAR(msg);
						t.pushNotification(0, MyStandarMessageType.MESSAGE, tmpW);
					}
				}
			}
			w.content=(this.encryptCSAR(msg));
			//member  not connected , we only update unread fiel in thread table
			threadManager.updateUnreadNumber(cth.getId(), 1);
			return w;
		}
		private String readLine() throws SocketTimeoutException,IOException,SocketException{
			String l="";
			synchronized (mIn) {
				try {
					l=mIn.readUTF();
					
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
			}
			return l;
		}
		private void println(String str){
			synchronized (mOut) {
				try {
					mOut.writeUTF(str+"\n");
					//mOut2.wr
					mOut.flush();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
				}
			}
		}
		
		
	}
	private static class ChatThread{
		String log1,log2;
		private ChatThread(String l1,String l2){
			log1=l1;
			log2=l2;
		}
		
	}
	private class Pair<T,V>{
		public T first;
		public V second;
		public Pair(T first, V second) {
			this.first = first;
			this.second = second;
		}
		
	}
	private class CThreadWrapper{
		public String lastActiveTime;
		public String date;
		public String lastMessage;
		public String self;
		public String other;
		public String lastSender;
		public String profil;
		public long id;
		public int unread;
		public long messageCount;
		public boolean isOnline;
		public CThreadWrapper() {
			// TODO Auto-generated constructor stub
		}
	}
	
	private static class LogKey{
		public String login;
		public String key;
		public Date date;
		public LogKey() {
			// TODO Auto-generated constructor stub
		}
		public static String generateKey(String log) {
			// TODO Auto-generated method stub
			String k=new Date().getTime()+"_"+log;
			try {
				k=Encrypt.encrypt(k, "SHA-1");
			} catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return k;
		}
		public LogKey(String key) {
			// TODO Auto-generated constructor stub
			this.key=key;
		}
		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return key;
		}
		@Override
		public boolean equals(Object o) {
			// TODO Auto-generated method stub
			if(o==this)
				return true;
			if(o instanceof String){
				if(o.equals(key))
					return true;
			}
			return false;
		}
	}
	class MembreWrapper{
		public MembreWrapper() {
			// TODO Auto-generated constructor stub
		}
		public MembreWrapper(Membre m){
			this.id=m.getId();
			this.key="";
			this.login=m.getLogin();
			this.nom=m.getNom();
			this.prenom=m.getPrenom();
			this.profil=m.getProfil();
		}
		public boolean isOnline;
		public int id;
		public String nom="";
		public String prenom="";
		public String login="";
		public String key="";
		public String profil="";
	}
	class ChatMessageWrapper{
		public ChatMessageWrapper(com.molo.entity.ChatMessage cmessage) {
			// TODO Auto-generated constructor stub
			content=cmessage.getContent();
			date=cmessage.getDate();
			receiver=cmessage.getReceiver();
			sender=cmessage.getSender();
			thread=cmessage.getThread();
			id=cmessage.getId();
			isVoiceMessage=false;
			attachments=new ArrayList<Attachment>();
		}
		public ChatMessageWrapper() {
			// TODO Auto-generated constructor stub
		}
		public boolean isVoiceMessage;
		public String content;
		public String sender;
		public String receiver;
		public String date;
		public long thread;
		public long id;
		public List<Attachment> attachments;
	}
	static class  MediaFile{
		protected File file;
		public String type;
		public long size;
		public String name;
	}
	static class CallThread{
		public Traitement other;
		public Traitement self;
		public long startTime;
		public long endTime;
		public String label;
		public String caller;
		public String receiver;
		public CallThread() {
			// TODO Auto-generated constructor stub
		}
		public CallThread(Traitement self,Traitement other){
			this.self=self;
			this.other=other;
			this.startTime=System.currentTimeMillis();
			this.endTime=System.currentTimeMillis();
		}
	}
	static class CallInfo{
		public String other;
		public static enum Status{
			CALLING,
			RECEIVING_CALL,
			NONE,
			IN_CALL,
		}
		public String currentLabel;
		public Status status=Status.NONE;
	}
	synchronized public static void removeFromVoipProcessList(VoipProcess voipProcess) {
		// TODO Auto-generated method stub
		voipProcessList.remove(voipProcess);
		//String label=voipProcess.getLabel();
//		for(Traitement t:mTraitements){
//			if(t.currentCall!=null && t.currentCall.label!=null && t.currentCall.label.equals(label))
//				t.finishCall();
//		}
	}

	synchronized public boolean isOnLine(Membre m) {
		// TODO Auto-generated method stub
		for(Traitement t:mTraitements){
			if(t.isAuthentificated && t.mMember!=null && t.getLogin().equals(m.getLogin()))
				return true;
		}
		return false;
	}
	synchronized public void pushToLOGS(Pair<Membre,Socket> pair) {
		// TODO Auto-generated method stub
		LOGS.add(pair);
	}
	synchronized public  void removeFromTraitement(Traitement traitement) {
		// TODO Auto-generated method stub
		mTraitements.remove(traitement);
		
	}
	synchronized public void pushTraitement(Traitement t){
		mTraitements.add(t);
	}
	synchronized public static void pushVoipProcess(VoipProcess p){
		voipProcessList.add(p);
//
	}
	synchronized public static VoipProcess findProcessByLabel(String la, VoipProcess self){
		for(VoipProcess p:voipProcessList){
			if(p!=self && p.getLabel().equals(la))
				return p;
		}
		return null;
	}
	public static void pushCall(CallThread cth){
		synchronized (calls) {
			calls.add(cth);
		}
	}
	public static void removeCall(CallThread cth){
		synchronized (calls) {
			calls.remove(cth);
		}
	}
	public static CallThread getCallByLabel(String label){
		synchronized (calls) {
			for(CallThread c:calls){
				if(c.label.equals(label))
					return c;
			}
			return null;
		}
	}
	public static void finishCallFor( String label) {
		// TODO Auto-generated method stub
		CallThread cth=getCallByLabel(label);
//		if(cth!=null){
//			if(cth.self.getLogin().equals(login)){
//				cth.self.finishCall();
//			}else{
//				cth.other.finishCall();
//			}
//		}
		if(cth!=null){
			cth.self.finishCall();
			cth.other.finishCall();
			removeCall(cth);			
		}
	}
}
