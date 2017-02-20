/*
MIT License

Copyright (c) 2017 Andrew Ma

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

 */
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
		int index = keyEqValue.indexOf("=");
		String key = keyEqValue.substring(0,index==-1?keyEqValue.length():index);
		String value = index==-1?"":keyEqValue.substring(index+1,keyEqValue.length());
		if('"'==value.charAt(0))value=value.substring(1);
		if('"'==value.charAt(value.length()-1))value = value.substring(0,value.length()-1);
		return new StringKeyValuePair(key, value);
	}

	@Override
	public int compareTo(StringKeyValuePair o) {
		Objects.requireNonNull(o);
		return o.toString().compareTo(this.toString());
	}
}