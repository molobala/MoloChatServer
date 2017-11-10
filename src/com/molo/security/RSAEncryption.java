package com.molo.security;


import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;


public class RSAEncryption {
	public static byte[]  encrypt(byte[] text,Key key){
		// Recuperation de la cle publique
		//PublicKey clePublique = KeyManager.readPublicKeyFromFile(publicKeyFileName);
		// Chiffrement du message
		byte[] bytes = null;
		try {
		    Cipher chiffreur = Cipher.getInstance("RSA");
		    chiffreur.init(Cipher.ENCRYPT_MODE, key);
		    bytes = chiffreur.doFinal(text);
		} catch(NoSuchAlgorithmException e) {
		    System.err.println("Erreur lors du chiffrement : " + e);
		    e.printStackTrace();
		    System.exit(-1);
		} catch(NoSuchPaddingException e) {
		    System.err.println("Erreur lors du chiffrement : " + e);
		    e.printStackTrace();
		    System.exit(-1);
		} catch(InvalidKeyException e) {
		    System.err.println("Erreur lors du chiffrement : " + e);
		    e.printStackTrace();
		    System.exit(-1);
		} catch(IllegalBlockSizeException e) {
		    System.err.println("Erreur lors du chiffrement : " + e);
		    e.printStackTrace();
		    System.exit(-1);
		} catch(BadPaddingException e) {
		    System.err.println("Erreur lors du chiffrement : " + e);
		    e.printStackTrace();
		    System.exit(-1);
		} 
		//messageEncrypted=Base64.getEncoder().encodeToString(bytes);
		return bytes;
	}
	public static byte[] decrypt(byte[] text,Key key){
		// Récupération de la clé privée
		//PrivateKey clePrivee = KeyManager.readPrivateKeyFromFile(privateKeyFileName);
		// Déchiffrement du message
		byte[] bytes = null;
		try {
		    Cipher dechiffreur = Cipher.getInstance("RSA");
		    dechiffreur.init(Cipher.DECRYPT_MODE, key);
		    bytes = dechiffreur.doFinal(text);
		} catch(NoSuchAlgorithmException e) {
			e.printStackTrace();
		   // System.err.println("Erreur lors du dechiffrement : " + e);
		    System.exit(-1);
		} catch(NoSuchPaddingException e) {
			e.printStackTrace();
		    //System.err.println("Erreur lors du dechiffrement : " + e);
		    System.exit(-1);
		} catch(InvalidKeyException e) {
			e.printStackTrace();
		    //System.err.println("Erreur lors du dechiffrement : " + e);
		    System.exit(-1);
		} catch(IllegalBlockSizeException e) {
			e.printStackTrace();
		    //System.err.println("Erreur lors du dechiffrement : " + e);
		    System.exit(-1);
		} catch(BadPaddingException e) {
			e.printStackTrace();
		    //System.err.println("Erreur lors du dechiffrement : " + e);
		    System.exit(-1);
		}
		//String messageEncrypted=Base64.getEncoder().encodeToString(bytes);
		return bytes;
	}
	
	
	public static class KeyManager{
		public static PrivateKey readPrivateKey(BigInteger modulo,BigInteger exposant){
			PrivateKey clePrivee = null;
			try {
			    RSAPrivateKeySpec specification = new RSAPrivateKeySpec(modulo, exposant);
			    KeyFactory usine = KeyFactory.getInstance("RSA");
			    clePrivee = usine.generatePrivate(specification);
			} catch(NoSuchAlgorithmException e) {
			    System.err.println("Algorithme RSA inconnu : " + e);
			    System.exit(-1);
			} catch(InvalidKeySpecException e) {
			    System.err.println("Spécification incorrecte : " + e);
			    System.exit(-1);
			}
			return clePrivee;
		}
		public static PublicKey readPublicKey(BigInteger modulo,BigInteger exposant){
			PublicKey clePublique = null;
			try {
				RSAPublicKeySpec specification = new RSAPublicKeySpec(modulo, exposant);
				KeyFactory usine = KeyFactory.getInstance("RSA");
				clePublique = usine.generatePublic(specification);
			} catch(NoSuchAlgorithmException e) {
				System.err.println("Algorithme RSA inconnu : " + e);
				System.exit(-1);
			} catch(InvalidKeySpecException e) {
				System.err.println("Spécification incorrecte : " + e);
				System.exit(-1);
			}
			return clePublique;
		}
		public static KeySpec getKeySpec(Key key){
			KeySpec specification = null;
			try {
			    KeyFactory usine = KeyFactory.getInstance("RSA");
			    specification = usine.getKeySpec(key, KeySpec.class);
			} catch(NoSuchAlgorithmException e) {
			    System.err.println("RSA inconnu : " + e);
			    System.exit(-1);
			} catch(InvalidKeySpecException e) {
			    System.err.println("Cle incorrecte : " + e);
			    System.exit(-1);  
			}
			return specification;
		}
		public static RSAPublicKeySpec getPublicKeySpecification(PublicKey key){
			RSAPublicKeySpec specification = null;
			try {
			    KeyFactory usine = KeyFactory.getInstance("RSA");
			    specification = usine.getKeySpec(key, RSAPublicKeySpec.class);
			} catch(NoSuchAlgorithmException e) {
			    System.err.println("RSA inconnu : " + e);
			    System.exit(-1);
			} catch(InvalidKeySpecException e) {
			    System.err.println("Cle incorrecte : " + e);
			    System.exit(-1);  
			}
			
			return specification;
		}
		public static RSAPrivateKeySpec getPrivateKeySpecification(PrivateKey key){
			RSAPrivateKeySpec specification = null;
			try {
			    KeyFactory usine = KeyFactory.getInstance("RSA");
			    specification = usine.getKeySpec(key, RSAPrivateKeySpec.class);
			} catch(NoSuchAlgorithmException e) {
			    System.err.println("Algorithme RSA inconnu : " + e);
			    System.exit(-1);
			} catch(InvalidKeySpecException e) {
			    System.err.println("Clé incorrecte : " + e);
			    System.exit(-1);  
			}
			return specification;
		}
		public static KeyPair genKey(int length){
			KeyPairGenerator generateurCles = null;
			try {
			    generateurCles = KeyPairGenerator.getInstance("RSA");
			    generateurCles.initialize(length);
			} catch(NoSuchAlgorithmException e) {
			    System.err.println("Erreur lors de l'initialisation du générateur de clés : " + e);
			    System.exit(-1);
			}

			// GÃ©nÃ©ration de la paire de clÃ©s
			KeyPair paireCles = generateurCles.generateKeyPair();
			return paireCles;
		}
	}
}
