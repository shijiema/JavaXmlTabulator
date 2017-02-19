package person.developer.shijiema.XMLUtils;

import java.util.Objects;

/**
 * a simple key-value pair of string key and value
 */
public class StringKeyValuePair implements Comparable<StringKeyValuePair>{
	private String key;
	private String value;

	private StringKeyValuePair(String key, String value) {
		this.key = key;
		this.value = value;
	}

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}
	public String toString(){
		return key+"="+value;
	}
	public static StringKeyValuePair newStringKeyValuePair(String key, String value) {
		return new StringKeyValuePair(key, value);
	}

	/**
	 * assuming keyEqValue i sin format k=v. if there's no "=", then value is null
	 * @param keyEqValue
	 * @return
	 */
	public static StringKeyValuePair newStringKeyValuePair(String keyEqValue) {
		Objects.requireNonNull(keyEqValue);
		String key = keyEqValue.substring(0,keyEqValue.indexOf("="));
		String value = keyEqValue.substring(keyEqValue.indexOf("=")+1,keyEqValue.length());
		return new StringKeyValuePair(key, value);
	}

	@Override
	public int compareTo(StringKeyValuePair o) {
		Objects.requireNonNull(o);
		return o.toString().compareTo(this.toString());
	}
}