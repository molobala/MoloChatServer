package com.molo;


import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.security.spec.KeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.sql.SQLException;
import java.util.Base64;

import com.molo.net.SecondServeur;
import com.molo.security.CSAREncryption;
import com.molo.security.RSAEncryption;


public class Main {
	public static final String TEMP_DIR="temp";
	public static final String USERS_DATA_DIR="data/users";
	public static final String USERS_FILE_DATA_DIR="data/users/files";
	public static void main(String[] args) throws IOException, SQLException {
		config();
		SecondServeur serveur=new SecondServeur(7081);
		serveur.run();
//		String text="Molobala est un village";
//		KeyPair keys=RSAEncryption.KeyManager.genKey(1024);
//		RSAPrivateKeySpec pSpec=RSAEncryption.KeyManager.getPrivateKeySpecification(keys.getPrivate());
//		RSAPublicKeySpec pubSpec= RSAEncryption.KeyManager.getPublicKeySpecification(keys.getPublic());
//		System.out.println("La cle privée : "+pSpec.getModulus()+"\n"+pSpec.getPrivateExponent());
//		System.out.println("La cle public : "+pubSpec.getModulus()+"\n"+pubSpec.getPublicExponent());
//		byte[] textByte=RSAEncryption.encrypt(text.getBytes(), keys.getPrivate());
//		String retText=new String(RSAEncryption.decrypt(textByte, keys.getPublic()));
//		System.out.println("Le text d'origine est "+text);
//		System.out.println("Le text crypte "+Base64.getEncoder().encodeToString(textByte));
//		System.out.println("Le text decrypté "+retText);
//		String csarKey=CSAREncryption.genKey(CSAREncryption.ALPHABET);
//		System.out.println("La clé CSAR : "+csarKey);
//		String cryptedSTr=CSAREncryption.encrypt(text, CSAREncryption.ALPHABET, csarKey);
//		System.out.println("Crypté : "+cryptedSTr);
//		System.out.println("Decrypté "+CSAREncryption.decrypt(cryptedSTr, CSAREncryption.ALPHABET, csarKey));
		
	}
	private static void config() {
		// TODO Auto-generated method stub
		File tempDir=new File(TEMP_DIR);
		if(!tempDir.isDirectory()){
			tempDir.mkdir();
		}
		tempDir=new File(USERS_DATA_DIR);
		if(!tempDir.isDirectory()){
			tempDir.mkdirs();
		}
		tempDir=new File(USERS_FILE_DATA_DIR);
		if(!tempDir.isDirectory()){
			tempDir.mkdirs();
		}
	}
}
