package com.coinblesk.customserialization;

import java.nio.charset.Charset;

import com.coinblesk.customserialization.exceptions.IllegalArgumentException;
import com.coinblesk.customserialization.exceptions.NotSignedException;
import com.coinblesk.customserialization.exceptions.UnknownCurrencyException;
import com.coinblesk.customserialization.exceptions.UnknownPKIAlgorithmException;
import com.coinblesk.customserialization.exceptions.UnknownServerResponseStatusException;

/**
 * This class represents a payment response, which is transferred via NFC
 * between two clients. The byte array serialization allows keeping the payload
 * and signature as as small as possible, which is important especially for the
 * NFC.
 * 
 * @author Jeton Memeti
 * 
 */
public class PaymentResponse extends SignedSerializableObject {
	
	private ServerResponseStatus status;
	private String reason;
	
	private String usernamePayer;
	private String usernamePayee;
	
	private Currency currency;
	private long amount;
	private long timestamp;
	
	//this constructor is needed for the DecoderFactory
	protected PaymentResponse() {
	}
	
	/**
	 * This constructor generates a new object.
	 * 
	 * @param pkiAlgorithm
	 *            the server's {@link PKIAlgorithm} to be used for
	 *            {@link SignedSerializableObject} super class
	 * @param keyNumber
	 *            the server's key number to be used for the
	 *            {@link SignedSerializableObject} super class
	 * @param status
	 *            the {@link ServerResponseStatus} indicating if the transaction
	 *            was granted or not
	 * @param reason
	 *            the reason if the transaction was not granted
	 * @param usernamePayer
	 *            he payer's username
	 * @param usernamePayee
	 *            the payee's username
	 * @param currency
	 *            the {@link Currency} of the payment same as in the
	 *            {@link PaymentRequest}
	 * @param amount
	 *            the amount same as in the {@link PaymentRequest}
	 * @param timestamp
	 *            the payee's timestamp
	 * @throws IllegalArgumentException
	 *             if any argument is null or does not fit into the foreseen
	 *             primitive type
	 */
	public PaymentResponse(PKIAlgorithm pkiAlgorithm, int keyNumber, ServerResponseStatus status, String reason, String usernamePayer, String usernamePayee, Currency currency, long amount, long timestamp) throws IllegalArgumentException {
		this(1, pkiAlgorithm, keyNumber, status, reason, usernamePayer, usernamePayee, currency, amount, timestamp);
	}
	
	private PaymentResponse(int version, PKIAlgorithm pkiAlgorithm, int keyNumber, ServerResponseStatus status, String reason, String usernamePayer, String usernamePayee, Currency currency, long amount, long timestamp) throws IllegalArgumentException {
		super(version, pkiAlgorithm, keyNumber);
		
		checkParameters(status, reason, usernamePayer, usernamePayee, currency, amount, timestamp);
		
		this.status = status;
		this.reason = reason;
		
		this.usernamePayer = usernamePayer;
		this.usernamePayee = usernamePayee;
		this.currency = currency;
		this.amount = amount;
		this.timestamp = timestamp;
		
		setPayload();
	}
	
	private void checkParameters(ServerResponseStatus status, String reason, String usernamePayer, String usernamePayee, Currency currency, long amount, long timestamp) throws IllegalArgumentException {
		if (status == null)
			throw new IllegalArgumentException("The status cannot be null.");
		
		if (status.getCode() == 2) {
			if (reason == null)
				throw new IllegalArgumentException("The reason cannot be null if the status is set to FAILURE.");
			if (reason.length() > 255)
				throw new IllegalArgumentException("The reason cannot be longer than 255 characters.");
		}
		
		if (usernamePayer == null || usernamePayer.length() == 0 || usernamePayer.length() > 255)
			throw new IllegalArgumentException("The payers's username cannot be null, empty, or longer than 255 characters.");
		
		if (usernamePayee == null || usernamePayee.length() == 0 || usernamePayee.length() > 255)
			throw new IllegalArgumentException("The payee's username cannot be null, empty, or longer than 255 characters.");
		
		if (usernamePayee.equalsIgnoreCase(usernamePayer))
			throw new IllegalArgumentException("The payee's username can't be equals to the payer's username.");
		
		if (currency == null)
			throw new IllegalArgumentException("The currency cannot be null.");
		
		if (amount <= 0)
			throw new IllegalArgumentException("The amount must be greater than 0.");
		
		if (timestamp <= 0)
			throw new IllegalArgumentException("The timestamp must be greater than 0.");
	}
	
	private void setPayload() {
		byte[] reasonBytes = null;
		byte[] usernamePayerBytes = usernamePayer.getBytes(Charset.forName("UTF-8"));
		byte[] usernamePayeeBytes = usernamePayee.getBytes(Charset.forName("UTF-8"));
		byte[] amountBytes = PrimitiveTypeSerializer.getLongAsBytes(amount);
		byte[] timestampBytes = PrimitiveTypeSerializer.getLongAsBytes(timestamp);
		
		int length;
		if (status == ServerResponseStatus.FAILURE) {
			/*
			 * version
			 * + signatureAlgorithm.getCode()
			 * + keyNumber
			 * + status
			 * + reason.length
			 * + reason
			 * + usernamePayer.length
			 * + usernamePayer
			 * + usernamePayee.length
			 * + usernamePayee
			 * + currency.getCode()
			 * + amount
			 * + timestamp
			 */
			reasonBytes = reason.getBytes(Charset.forName("UTF-8"));
			length = 1+1+1+1+1+reasonBytes.length+1+usernamePayerBytes.length+1+usernamePayeeBytes.length+1+8+8;
		} else {
			/*
			 * version
			 * + signatureAlgorithm.getCode()
			 * + keyNumber
			 * + status
			 * + usernamePayer.length
			 * + usernamePayer
			 * + usernamePayee.length
			 * + usernamePayee
			 * + currency.getCode()
			 * + amount
			 * + timestamp
			 */
			length = 1+1+1+1+1+usernamePayerBytes.length+1+usernamePayeeBytes.length+1+8+8;
		}
		byte[] payload = new byte[length];
		
		int index = 0;
		
		payload[index++] = (byte) getVersion();
		payload[index++] = getPKIAlgorithm().getCode();
		payload[index++] = (byte) getKeyNumber();
		payload[index++] = status.getCode();
		
		if (status == ServerResponseStatus.FAILURE) {
			payload[index++] = (byte) reason.length();
			for (byte b : reasonBytes) {
				payload[index++] = b;
			}
		}
		
		payload[index++] = (byte) usernamePayerBytes.length;
		for (byte b : usernamePayerBytes) {
			payload[index++] = b;
		}
		payload[index++] = (byte) usernamePayeeBytes.length;
		for (byte b : usernamePayeeBytes) {
			payload[index++] = b;
		}
		payload[index++] = currency.getCode();
		for (byte b : amountBytes) {
			payload[index++] = b;
		}
		for (byte b : timestampBytes) {
			payload[index++] = b;
		}
		
		this.payload = payload;
	}
	
	public ServerResponseStatus getStatus() {
		return status;
	}

	public String getReason() {
		return reason;
	}

	public String getUsernamePayer() {
		return usernamePayer;
	}

	public String getUsernamePayee() {
		return usernamePayee;
	}

	public Currency getCurrency() {
		return currency;
	}

	public long getAmount() {
		return amount;
	}

	public long getTimestamp() {
		return timestamp;
	}

	@Override
	public PaymentResponse decode(byte[] bytes) throws IllegalArgumentException, UnknownPKIAlgorithmException, UnknownServerResponseStatusException, UnknownCurrencyException, NotSignedException {
		if (bytes == null)
			throw new IllegalArgumentException("The argument can't be null.");
		
		try {
			int index = 0;
			
			int version = bytes[index++] & 0xFF;
			PKIAlgorithm pkiAlgorithm = PKIAlgorithm.getPKIAlgorithm(bytes[index++]);
			int keyNumber = bytes[index++] & 0xFF;
			ServerResponseStatus status = ServerResponseStatus.getStatus(bytes[index++]);
			
			String reason;
			if (status == ServerResponseStatus.FAILURE) {
				int reasonLength = bytes[index++] & 0xFF;
				byte[] reasonBytes = new byte[reasonLength];
				for (int i=0; i<reasonLength; i++) {
					reasonBytes[i] = bytes[index++];
				}
				reason = new String(reasonBytes);
			} else {
				reason = null;
			}
			
			int usernamePayerLength = bytes[index++] & 0xFF;
			byte[] usernamePayerBytes = new byte[usernamePayerLength];
			for (int i=0; i<usernamePayerLength; i++) {
				usernamePayerBytes[i] = bytes[index++];
			}
			String usernamePayer = new String(usernamePayerBytes);
			
			int usernamePayeeLength = bytes[index++] & 0xFF;
			byte[] usernamePayeeBytes = new byte[usernamePayeeLength];
			for (int i=0; i<usernamePayeeLength; i++) {
				usernamePayeeBytes[i] = bytes[index++];
			}
			String usernamePayee = new String(usernamePayeeBytes);
			
			Currency currency = Currency.getCurrency(bytes[index++]);
			
			byte[] amountBytes = new byte[Long.SIZE / Byte.SIZE]; //8 bytes (long)
			for (int i=0; i<amountBytes.length; i++) {
				amountBytes[i] = bytes[index++];
			}
			long amount = PrimitiveTypeSerializer.getBytesAsLong(amountBytes);
			
			byte[] timestampBytes = new byte[Long.SIZE / Byte.SIZE]; //8 bytes (long)
			for (int i=0; i<timestampBytes.length; i++) {
				timestampBytes[i] = bytes[index++];
			}
			long timestamp = PrimitiveTypeSerializer.getBytesAsLong(timestampBytes);
			
			PaymentResponse pr = new PaymentResponse(version, pkiAlgorithm, keyNumber, status, reason, usernamePayer, usernamePayee, currency, amount, timestamp);
			
			int signatureLength = bytes.length - index;
			if (signatureLength == 0) {
				throw new NotSignedException();
			} else {
				byte[] signature = new byte[signatureLength];
				for (int i=0; i<signature.length; i++) {
					signature[i] = bytes[index++];
				}
				pr.signature = signature;
			}
			
			return pr;
		} catch (IndexOutOfBoundsException e) {
			throw new IllegalArgumentException("The given byte array is corrupt (not long enough).");
		}
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (!(o instanceof PaymentResponse))
			return false;
		
		PaymentResponse pr = (PaymentResponse) o;
		if (getVersion() != pr.getVersion())
			return false;
		if (getPKIAlgorithm().getCode() != pr.getPKIAlgorithm().getCode())
			return false;
		if (getKeyNumber() != pr.getKeyNumber())
			return false;
		if (!this.usernamePayer.equals(pr.usernamePayer))
			return false;
		if (!this.usernamePayee.equals(pr.usernamePayee))
			return false;
		if (this.currency.getCode() != pr.currency.getCode())
			return false;
		if (this.amount != pr.amount)
			return false;
		if (this.timestamp != pr.timestamp)
			return false;
		
		return true;
	}
	
}
