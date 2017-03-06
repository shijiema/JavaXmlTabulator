package person.developer.shijiema.XMLUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;


public class KeyValueWithProperties implements Comparable<KeyValueWithProperties>{
	private String key;
	private String value;
	private Map<String,String> properties = null;
	
	private KeyValueWithProperties(String key, String value) {
		this.key = key;
		this.value = value;
	}
	public void setValue(String v){
		this.value = "".equals(v)?null:v;
	}
	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}
	public Map<String,String> getProperties(){
		return properties;
	}
	public void addProperty(String k,String v){
		Objects.requireNonNull(k);
		Objects.requireNonNull(v);
		if(properties==null){
			properties = new LinkedHashMap<>();
		}
		properties.put(k, v);
	}
	public void addProperty(String keyEqValue) {
		int index = keyEqValue.indexOf("=");
		String key = keyEqValue.substring(0,index==-1?keyEqValue.length():index);
		String value = index==-1?"":keyEqValue.substring(index+1,keyEqValue.length());
		if('"'==value.charAt(0))value = value.substring(1);
		if('"'==value.charAt(value.length()-1))value=value.substring(0,value.length()-1);
		addProperty(key,value);
	}
	public String toString(){
		return key+"="+value;
	}
	public int hashCode(){
		return getKey().hashCode();
	}
	public boolean equals(Object o){
		if(o!=null && o instanceof KeyValueWithProperties && ((KeyValueWithProperties) o).getKey().equals(getKey())){
			return true;
		}
		return false;
	}
	public static KeyValueWithProperties newStringKeyValuePair(String key, String value) {
		return new KeyValueWithProperties(key, value);
	}
	
	public static KeyValueWithProperties newStringKeyValuePair(String keyEqValue) {
		int index = keyEqValue.indexOf("=");
		String key = keyEqValue.substring(0,index==-1?keyEqValue.length():index);
		String value = index==-1?"":keyEqValue.substring(index+1,keyEqValue.length());
		if('"'==value.charAt(0))value = value.substring(1);
		if('"'==value.charAt(value.length()-1))value=value.substring(0,value.length()-1);
		return new KeyValueWithProperties(key, value);
	}

	@Override
	public int compareTo(KeyValueWithProperties o) {
		Objects.requireNonNull(o);
		return o.toString().compareTo(this.toString());
	}
}