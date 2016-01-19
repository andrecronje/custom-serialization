package com.coinblesk.customserialization;

import com.coinblesk.customserialization.Currency;
import com.coinblesk.customserialization.PaymentRequest;
import com.coinblesk.customserialization.DecoderFactory;
import com.coinblesk.customserialization.PKIAlgorithm;
import com.coinblesk.customserialization.PrimitiveTypeSerializer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.coinblesk.customserialization.exceptions.IllegalArgumentException;
import com.coinblesk.customserialization.exceptions.NotSignedException;
import com.coinblesk.customserialization.exceptions.SerializationException;
import com.coinblesk.customserialization.exceptions.UnknownPKIAlgorithmException;
import com.coinblesk.customserialization.testutils.TestUtils;

public class PaymentRequestTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testConstructor_IllegalArgumentException() {
		boolean exceptionThrown = false;

		try {
			new PaymentRequest(null, 1, "buyer", "seller", Currency.BTC, 12, System.currentTimeMillis());
		} catch (IllegalArgumentException e) {
			exceptionThrown = true;
		}
		assertTrue(exceptionThrown);
		exceptionThrown = false;
		
		try {
			new PaymentRequest(PKIAlgorithm.DEFAULT, 0, "buyer", "seller", Currency.BTC, 12, System.currentTimeMillis());
		} catch (IllegalArgumentException e) {
			exceptionThrown = true;
		}
		assertTrue(exceptionThrown);
		exceptionThrown = false;
		
		try {
			new PaymentRequest(PKIAlgorithm.DEFAULT, 256, "buyer", "seller", Currency.BTC, 1, 1);
		} catch (IllegalArgumentException e) {
			exceptionThrown = true;
		}
		assertTrue(exceptionThrown);
		exceptionThrown = false;
		
		try {
			new PaymentRequest(PKIAlgorithm.DEFAULT, 1, null, "seller", Currency.BTC, 12, System.currentTimeMillis());
		} catch (IllegalArgumentException e) {
			exceptionThrown = true;
		}
		assertTrue(exceptionThrown);
		exceptionThrown = false;
		
		try {
			new PaymentRequest(PKIAlgorithm.DEFAULT, 1, "buyer", "", Currency.BTC, 12, System.currentTimeMillis());
		} catch (IllegalArgumentException e) {
			exceptionThrown = true;
		}
		assertTrue(exceptionThrown);
		exceptionThrown = false;
		
		try {
			new PaymentRequest(PKIAlgorithm.DEFAULT, 1, "buyer", "seller", null, 12, System.currentTimeMillis());
		} catch (IllegalArgumentException e) {
			exceptionThrown = true;
		}
		assertTrue(exceptionThrown);
		exceptionThrown = false;
		
		try {
			new PaymentRequest(PKIAlgorithm.DEFAULT, 1, "buyer", "seller", Currency.BTC, 0, System.currentTimeMillis());
		} catch (IllegalArgumentException e) {
			exceptionThrown = true;
		}
		assertTrue(exceptionThrown);
		exceptionThrown = false;
		
		try {
			new PaymentRequest(PKIAlgorithm.DEFAULT, 1, "buyer", "seller", Currency.BTC, 1, 0);
		} catch (IllegalArgumentException e) {
			exceptionThrown = true;
		}
		assertTrue(exceptionThrown);
		exceptionThrown = false;
		
		try {
			new PaymentRequest(PKIAlgorithm.DEFAULT, 1, "buyer", "BuyeR", Currency.BTC, 12, System.currentTimeMillis());
		} catch (IllegalArgumentException e) {
			exceptionThrown = true;
		}
		assertTrue(exceptionThrown);
		exceptionThrown = false;
	}
	
	@Test
	public void testEncode_fail() throws IllegalArgumentException {
		//this will fail because the PaymentRequest is not signed
		PaymentRequest pr = new PaymentRequest(PKIAlgorithm.DEFAULT, 1, "buyer", "seller", Currency.BTC, 12, System.currentTimeMillis());
		boolean exceptionThrown = false;
		
		try {
			pr.encode();
		} catch (NotSignedException e) {
			exceptionThrown = true;
		}
		
		assertTrue(exceptionThrown);
	}
	
	@Test
	public void testSignEncode() throws IllegalArgumentException, InvalidKeyException, NoSuchAlgorithmException, SignatureException, NotSignedException, NoSuchProviderException, InvalidAlgorithmParameterException, UnknownPKIAlgorithmException {
		KeyPair keyPair = TestUtils.generateKeyPair();
		long timestamp = System.currentTimeMillis();
		
		PaymentRequest pr = new PaymentRequest(PKIAlgorithm.DEFAULT, 1, "buyer", "seller", Currency.BTC, 12, timestamp);
		pr.sign(keyPair.getPrivate());
		
		byte[] encode = pr.encode();
		
		int index = 0;
		
		assertEquals(1, encode[index++]); //version
		assertEquals(1, encode[index++]); //SignatureAlgorithm
		assertEquals(1, encode[index++]); //keynumber
		
		assertEquals(5, encode[index++]); //"buyer".length
		
		String s = "buyer";
		byte[] bytesB = s.getBytes(Charset.forName("UTF-8"));
		assertEquals(5, bytesB.length);
		for (byte b : bytesB) {
			assertEquals(b, encode[index++]); //username payer
		}
		
		assertEquals(6,  encode[index++]); //"seller".length
		
		String t = "seller";
		byte[] bytesS = t.getBytes(Charset.forName("UTF-8"));
		assertEquals(6, bytesS.length);
		for (byte b : bytesS) {
			assertEquals(b, encode[index++]); //username payee
		}
		
		assertEquals(1, encode[index++]); //nofCurrencies
		
		assertEquals(1, encode[index++]); //currency
		
		byte[] longAsBytes = PrimitiveTypeSerializer.getLongAsBytes(12);
		assertEquals(8, longAsBytes.length);
		for (byte b : longAsBytes) {
			assertEquals(b, encode[index++]);  //amount
		}
		
		byte[] longAsBytes2 = PrimitiveTypeSerializer.getLongAsBytes(timestamp);
		assertEquals(8, longAsBytes2.length);
		for (byte b : longAsBytes2) {
			assertEquals(b, encode[index++]); //timestamp
		}
	}
	
	@Test
	public void testDecode() throws IllegalArgumentException, InvalidKeyException, NoSuchAlgorithmException, SignatureException, NoSuchProviderException, InvalidAlgorithmParameterException, SerializationException {
		KeyPair keyPair = TestUtils.generateKeyPair();
		long timestamp = System.currentTimeMillis();
		
		PaymentRequest pr = new PaymentRequest(PKIAlgorithm.DEFAULT, 1, "buyer", "seller", Currency.BTC, 12, timestamp);
		pr.sign(keyPair.getPrivate());
		
		byte[] encode = pr.encode();
		
		PaymentRequest decoded = DecoderFactory.decode(PaymentRequest.class, encode);
		boolean verify = decoded.verify(keyPair.getPublic());
		
		assertTrue(verify);
		assertTrue(pr.equals(decoded));
	}
	
	@Test
	public void testEncodeDecode_withInputCurrency() throws Exception {
		KeyPair keyPair = TestUtils.generateKeyPair();
		long timestamp = System.currentTimeMillis();
		
		PaymentRequest pr = new PaymentRequest(PKIAlgorithm.DEFAULT, 1, "buyer", "seller", Currency.BTC, 12, Currency.CHF, 540, timestamp);
		pr.sign(keyPair.getPrivate());
		
		byte[] encode = pr.encode();
		
		PaymentRequest decoded = DecoderFactory.decode(PaymentRequest.class, encode);
		boolean verify = decoded.verify(keyPair.getPublic());
		
		assertTrue(verify);
		assertTrue(pr.equals(decoded));
	}

}
