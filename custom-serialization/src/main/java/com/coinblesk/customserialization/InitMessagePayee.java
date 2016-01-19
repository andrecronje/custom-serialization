package com.coinblesk.customserialization;

import java.nio.charset.Charset;

import com.coinblesk.customserialization.exceptions.IllegalArgumentException;
import com.coinblesk.customserialization.exceptions.NotSignedException;
import com.coinblesk.customserialization.exceptions.UnknownCurrencyException;

public class InitMessagePayee extends SerializableObject {
	
	private String username;
	private Currency currency;
	private long amount;
	
	//this constructor is needed for the DecoderFactory
	protected InitMessagePayee() {
	}
	
	public InitMessagePayee(String username, Currency currency, long amount) throws IllegalArgumentException {
		this(1, username, currency, amount);
	}
	
	private InitMessagePayee(int version, String username, Currency currency, long amount) throws IllegalArgumentException {
		super(version);
		checkParameters(username, currency, amount);
		
		this.username = username;
		this.currency = currency;
		this.amount = amount;
	}
	
	private void checkParameters(String username, Currency currency, long amount) throws IllegalArgumentException {
		if (username == null || username.isEmpty() || username.length() > 255)
			throw new IllegalArgumentException("The username cannot be null, empty, or longer than 255 characters.");
		
		if (currency == null)
			throw new IllegalArgumentException("The currency cannot be null.");
		
		if (amount < 0)
			throw new IllegalArgumentException("The amount must be greater than 0.");
	}

	public String getUsername() {
		return username;
	}

	public Currency getCurrency() {
		return currency;
	}

	public long getAmount() {
		return amount;
	}

	@Override
	public byte[] encode() throws NotSignedException {
		byte[] usernameBytes = username.getBytes(Charset.forName("UTF-8"));
		
		/*
		 * version
		 * + username.length
		 * + username
		 * + currency.getcode
		 * + amount
		 */
		int index = 0;
		byte[] result = new byte[1+1+usernameBytes.length+1+8];
		result[index++] = (byte) getVersion();
		result[index++] = (byte) usernameBytes.length;
		for (byte b : usernameBytes) {
			result[index++] = b;
		}
		result[index++] = currency.getCode();
		byte[] amountBytes = PrimitiveTypeSerializer.getLongAsBytes(amount);
		for (byte b : amountBytes) {
			result[index++] = b;
		}
		
		return result;
	}

	@Override
	public InitMessagePayee decode(byte[] bytes) throws IllegalArgumentException, NotSignedException, UnknownCurrencyException {
		try {
			int index = 0;
			
			int version = bytes[index++] & 0xFF;
			int usernameLength = bytes[index++] & 0xFF;
			byte[] usernameBytes = new byte[usernameLength];
			for (int i=0; i<usernameLength; i++) {
				usernameBytes[i] = bytes[index++];
			}
			String username = new String(usernameBytes);
			Currency currency = Currency.getCurrency(bytes[index++]);
			
			byte[] amountBytes = new byte[Long.SIZE / Byte.SIZE]; //8 bytes (long)
			for (int i=0; i<amountBytes.length; i++) {
				amountBytes[i] = bytes[index++];
			}
			long amount = PrimitiveTypeSerializer.getBytesAsLong(amountBytes);
			
			return new InitMessagePayee(version, username, currency, amount);
		} catch (IndexOutOfBoundsException e) {
			throw new IllegalArgumentException("The given byte array is corrupt (not long enough).");
		}
	}

}
