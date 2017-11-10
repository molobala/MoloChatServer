package com.molo.ftp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class MFtp {
	public static boolean ftpPut(File file,DataOutputStream out){
		//File file=new File(fname);
		if(!file.exists())
			return false;
		{
			System.out.println("Begin sending file");
			BufferedInputStream fin=null;
			try{
				fin= new BufferedInputStream(new FileInputStream(file));
				String fname=file.getName();
				long fileSize=file.length();
				//send file name;
				out.writeUTF(fname);
				out.flush();//send completely those informations
				//send file size
				out.writeLong(fileSize);
				out.flush();//send completely those informations
				int byteRead=0;
				byte[] buffer=new byte[(int) Math.min(4096, fileSize)];
				System.out.println("Buffer size: "+buffer.length);
				while((byteRead=fin.read(buffer))>0){
					out.write(buffer,0,byteRead);
					out.flush();
					System.out.println("BYTE READ AND WRITE TO SERVER :"+byteRead);
				}
				System.out.println("File totaly sent");
				out.flush();
				fin.close();
			}catch(NumberFormatException e){
				return false;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}
	 public static boolean ftpPut(String fname,DataOutputStream out){
		File file=new File(fname);
		if(!file.exists())
			return false;
		 {
			System.out.println("Begin sending file");
			BufferedInputStream fin=null;
			try{
				fin= new BufferedInputStream(new FileInputStream(file));
				long fileSize=file.length();
				fname=file.getName();
				//send file name;
				out.writeUTF(fname);
				out.flush();//send completely those informations
				//send file size
				out.writeLong(fileSize);
				out.flush();//send completely those informations
				int byteRead=0;
				byte[] buffer=new byte[(int) Math.min(4096, fileSize)];
				System.out.println("Buffer size: "+buffer.length);
				while((byteRead=fin.read(buffer))>0){
					out.write(buffer,0,byteRead);
					out.flush();
					System.out.println("BYTE READ AND WRITE TO SERVER :"+byteRead);
				}
				System.out.println("File totaly sent");
				out.flush();
				fin.close();
			}catch(NumberFormatException e){
				return false;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}
	public static File ftpGetFile(DataInputStream din,String dir){
		
		//read file size from the client
		try{
			//read file name
			String fname=din.readUTF();
			//read filename
			long fileSize=din.readLong();
			
			File outPut=new File(dir+"/"+fname);
			BufferedOutputStream fout=null;
			fout= new BufferedOutputStream(new FileOutputStream(outPut));
			long byteRestants=fileSize;
			byte[] buffer=new byte[(int) Math.min(4096, fileSize)];
			System.out.println("Start receiving file: "+fname+" / "+fileSize);
			int byteToRead=0;
			while(byteRestants>0){
				byteToRead=din.read(buffer, 0,(int)Math.min(buffer.length, byteRestants));
				byteRestants-=byteToRead;
				fout.write(buffer,0,byteToRead);
				System.out.println("Byte restant: "+byteRestants);
			}
			fout.close();
			return outPut;
		}catch(NumberFormatException e){
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}	
	}
}
