package person.developer.shijiema.XMLUtils;

import org.w3c.dom.Node;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;


public class MyNode implements Comparable<MyNode>{
	private String key;
	private String value;
	private Map<String,String> properties = null;

	private MyNode(String key, String value) {
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

	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append(key+"="+value);
		if(this.properties!=null) {
			for (Map.Entry<String, String> p : this.properties.entrySet()) {
				sb.append(p.getKey() + "=" + p.getValue()).append(" ");
			}
		}
		return sb.toString();
	}
	public int hashCode(){
		return getKey().hashCode();
	}

	public static MyNode newStringKeyValuePair(String key, String value) {
		return new MyNode(key, value);
	}
	
	@Override
	public int compareTo(MyNode o) {
		Objects.requireNonNull(o);
		return o.toString().compareTo(this.toString());
	}
}