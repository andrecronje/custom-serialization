package ch.uzh.csg.mbps.customserialization.security;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.jce.spec.ECParameterSpec;

import ch.uzh.csg.mbps.customserialization.PKIAlgorithm;
import ch.uzh.csg.mbps.customserialization.UnknownPKIAlgorithmException;

//TODO: javadoc
public class KeyHandler {
	
	static {
		Security.addProvider(new BouncyCastleProvider());
	}
	
	//TODO javadoc: uses the default algorithm
	public static KeyPair generateKeyPair() throws UnknownPKIAlgorithmException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
		return generateKeyPair(PKIAlgorithm.DEFAULT);
	}
	
	public static KeyPair generateKeyPair(PKIAlgorithm algorithm) throws UnknownPKIAlgorithmException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
		if (algorithm.getCode() != PKIAlgorithm.DEFAULT.getCode())
			throw new UnknownPKIAlgorithmException();
		
		ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec(algorithm.getKeyPairSpecification());
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algorithm.getKeyPairAlgorithm(), algorithm.getSecurityProvider());
		keyGen.initialize(ecSpec, new SecureRandom());
		return keyGen.generateKeyPair();
		
	}
	
	public static String encodePrivateKey(PrivateKey privateKey) {
		byte[] privateEncoded = Base64.encodeBase64(privateKey.getEncoded());
		return new String(privateEncoded);
	}
	
	public static String encodePublicKey(PublicKey publicKey) {
		byte[] publicEncoded = Base64.encodeBase64(publicKey.getEncoded());
		return new String(publicEncoded);
	}
	
	public static PublicKey decodePublicKey(String publicKeyEncoded) throws UnknownPKIAlgorithmException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
		return decodePublicKey(publicKeyEncoded, PKIAlgorithm.DEFAULT);
	}

	public static PublicKey decodePublicKey(String publicKeyEncoded, PKIAlgorithm algorithm) throws UnknownPKIAlgorithmException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
		if (algorithm.getCode() != PKIAlgorithm.DEFAULT.getCode())
			throw new UnknownPKIAlgorithmException();
		
		byte[] decoded = Base64.decodeBase64(publicKeyEncoded.getBytes());
		EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(decoded);
		
		KeyFactory keyFactory = KeyFactory.getInstance(algorithm.getKeyPairAlgorithm(), algorithm.getSecurityProvider());
		return keyFactory.generatePublic(publicKeySpec);
	}
	
	public static PrivateKey decodePrivateKey(String privateKeyEncoded) throws UnknownPKIAlgorithmException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
		return decodePrivateKey(privateKeyEncoded, PKIAlgorithm.DEFAULT);
	}
	
	public static PrivateKey decodePrivateKey(String privateKeyEncoded, PKIAlgorithm algorithm) throws UnknownPKIAlgorithmException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
		if (algorithm.getCode() != PKIAlgorithm.DEFAULT.getCode())
			throw new UnknownPKIAlgorithmException();
		
		byte[] decoded = Base64.decodeBase64(privateKeyEncoded.getBytes());
		EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(decoded);
		
		KeyFactory keyFactory = KeyFactory.getInstance(algorithm.getKeyPairAlgorithm(), algorithm.getSecurityProvider());
		return keyFactory.generatePrivate(privateKeySpec);
	}

}
