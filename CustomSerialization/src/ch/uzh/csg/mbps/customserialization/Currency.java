package ch.uzh.csg.mbps.customserialization;

import java.util.HashMap;
import java.util.Map;

import ch.uzh.csg.mbps.customserialization.exceptions.UnknownCurrencyException;

/**
 * This class contains the supported currencies.
 * 
 * @author Jeton Memeti
 * 
 */
public enum Currency {
	BTC((byte) 0x01);
	
	private byte code;
	
	private Currency(byte code) {
		this.code = code;
	}

	/**
	 * Returns the code of this Currency.
	 */
	public byte getCode() {
		return code;
	}
	
	private static Map<Byte, Currency> codeCurrencyMap = null;
	
	/**
	 * Returns the Currency based on the code.
	 * 
	 * @param b
	 *            the code
	 * @throws UnknownCurrencyException
	 *             if the given code is not known
	 */
	public static Currency getCurrency(byte b) throws UnknownCurrencyException {
		if (codeCurrencyMap == null)
			initMap();
		
		Currency currency = codeCurrencyMap.get(b);
		if (currency == null)
			throw new UnknownCurrencyException();
		else
			return currency;
	}

	private static void initMap() {
		codeCurrencyMap = new HashMap<Byte, Currency>();
		for (Currency c : values()) {
			codeCurrencyMap.put(c.getCode(), c);
		}
	}
	
}
