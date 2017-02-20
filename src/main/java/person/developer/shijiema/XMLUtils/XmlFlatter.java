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

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;
import java.io.CharArrayReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.*;

/**
 * Utility that converts XML to tabular data.
 * @link https://docs.oracle.com/javase/7/docs/api/org/w3c/dom/Node.html#getNodeValue()
 * @author Andrew Ma
 * @version 1.0, 02/14/2017
 */
public class XmlFlatter implements Iterable<List<String>>{
    int stopLevel = -1;
    List<Map<String,String>> mapResult = new ArrayList<Map<String,String>>();
    List<List<StringKeyValuePair>> result = new ArrayList<List<StringKeyValuePair>>();
    Set<String> headerSet = new LinkedHashSet<String>();
    List<String> headers = new LinkedList<String>();
    DocumentBuilder builder = null;
    Document xml = null;
    XPath xpath = null;
    private void initDocumentAndXpath() throws Exception{
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        //factory.setNamespaceAware(true);
        //factory.setValidating(true);
        builder = factory.newDocumentBuilder();
        XPathFactory xPathfactory = XPathFactory.newInstance();
        xpath = xPathfactory.newXPath();
    }
    private void parse(Reader reader) throws Exception{
        List<StringKeyValuePair> firstRow = new ArrayList<StringKeyValuePair>();
        this.xml = builder.parse(new InputSource(reader));
        //figure out stop level
        stopLevel = maxAncestorCnt(this.xml);

        buildResult("/", firstRow);
        convertDuplicatedColsToRows();
    }
    public XmlFlatter(File xmlFile) throws Exception {
        initDocumentAndXpath();
        Reader reader = new FileReader(xmlFile);

        parse(reader);
    }
    public XmlFlatter(String xmlStr) throws Exception {
        initDocumentAndXpath();
        Reader reader = new CharArrayReader(xmlStr.toCharArray());

        parse(reader);
    }
    /**
     * Recursively going through layers of XML, building up rows and columns as it goes.
     * @param path xPath of current selections
     * @param row	a row to build
     */
    private void buildResult(String path, List<StringKeyValuePair> row) throws XPathExpressionException {
        //no more child, parent is leaf
        if(xml==null){
            return;
        }

        //get all children.
        //leaf level Elements
        XPathExpression expr = xpath.compile(path+"*[count(child::*) < 1]");
        NodeList leafChildren = (NodeList) expr.evaluate(xml, XPathConstants.NODESET);
        //String[] leafChildren = xml..copyOf(path+"*[count(child::*) < 1]");//leaf element children
        //Elements having child elements
        expr = xpath.compile(path+"*[count(child::*) >= 1]");
        NodeList childrenAsParent = (NodeList) expr.evaluate(xml, XPathConstants.NODESET);
        //String[] childrenAsParent = xml.copyOf(path+"*[count(child::*) >= 1]");//element with element children
        //all attribute nodes
        expr = xpath.compile(path+"attribute::*");
        NodeList properties = (NodeList) expr.evaluate(xml, XPathConstants.NODESET);
        //String[] properties = xml.copyOf(path+"attribute::*");//attributes

        //attributes belongs to current row
        if(properties!=null && properties.getLength()>0){

            for(int i=0;i<properties.getLength();i++){
                StringKeyValuePair kvp = StringKeyValuePair.newStringKeyValuePair(path+"@"+properties.item(i).getNodeName()+"="+properties.item(i).getNodeValue());
                String header = normalizeAKey(kvp.getKey());
                row.add(StringKeyValuePair.newStringKeyValuePair(header,kvp.getValue()));
            }
        }
        //leaf children belongs to current row. this method append duplicate element as columns
        //mathod 1, spread same keys as columns
//		if(leafChildren!=null && leafChildren.length>0){
//			Map<String,Integer> stats = new HashMap<String,Integer>();//xml contains duplicate elements as siblings
//			for(String lc:leafChildren){
//				Xml t = new Xml(lc);
//				String localName = t.getQName().getLocalNameString();
//				if(!stats.containsKey(localName)){
//					stats.put(localName, 0);
//				}else{
//					int seq = stats.get(localName);
//					stats.put(localName,seq+1);
//					localName = localName+"_"+seq;
//				}
//				String header = path+localName;
//				header = normalizeAKey(header);
//				row.add(StringKeyValuePair.newStringKeyValuePair(header,t.valueOf()));
//			}
//		}
        //method two, leave them as same column name, later convert to rows.
        //this is more work, but more reasonable
        if(leafChildren!=null && leafChildren.getLength()>0){

            for(int i=0;i<leafChildren.getLength();i++){
                Node t = leafChildren.item(i);
                String nodeName = t.getNodeName();
                //Element's node value is null, trying to get its text node's value
                String nodeValue= t.getFirstChild()==null?null:t.getFirstChild().getNodeValue();
                String header = path+nodeName;
                header = normalizeAKey(header);
                row.add(StringKeyValuePair.newStringKeyValuePair(header,nodeValue));
            }
        }
        //for element having child elements
        if(childrenAsParent!=null&&childrenAsParent.getLength()>0){
            //sort them the first
            Node[] nodes = new Node[childrenAsParent.getLength()];
            for(int i=0;i<childrenAsParent.getLength();i++){
                nodes[i] = childrenAsParent.item(i);
            }
            Arrays.sort(nodes, new Comparator<Node>() {
                @Override
                public int compare(Node o1, Node o2) {
                    return o1.getNodeName().compareToIgnoreCase(o2.getNodeName());
                }
            });
            int index = 1;
            String prev = "";
            //spawn a new row for each children
            for(int i=0;i<nodes.length;i++){
                Node t = nodes[i];
                List<StringKeyValuePair> addRow = new ArrayList<StringKeyValuePair>();
                addRow.addAll(row);
                String curElement = t.getNodeName();
                if(!curElement.equalsIgnoreCase(prev)){
                    index=1;//reset index for new element
                }
                //go to next layer
                buildResult(path+curElement+"["+index+"]/",addRow);
                index++;
                prev = curElement;
            }
        }
        if(ancestorCnt(path)==stopLevel){
            result.add(row);
        }
    }
    /**
     * return parsed headers
     * @return
     */
    public List<String> getHeaders(){
        return headers;
    }
    /**
     * Return result as list of maps.
     * The reason to have map as a row is to facilitate retrieval of values by headers.
     * It also makes it easy by not having to correlate positions between headers and values
     * @return
     */
    public List<Map<String,String>> getBody(){
        return mapResult;
    }
    /**
     * This utility uses List<List<KV>> to facilitate production operation, but it should
     * be converted to List<Map<k,V>> for easier content consumption.
     *
     * This function convert List<KV> to Map<K,V>
     * @param l
     * @return
     */
    private Map<String,String> convertListOfKVPToMap(List<StringKeyValuePair> l){
        Objects.requireNonNull(l);
        Map<String,String> mp = new HashMap<String,String>();
        for(StringKeyValuePair kv:l){
            mp.put(kv.getKey(), kv.getValue());
            headerSet.add(kv.getKey());
        }
        return mp;
    }
    /**
     * For duplicate columns within a row, convert them to be in different rows.
     */
    private void convertDuplicatedColsToRows(){
        List<List<StringKeyValuePair>> newResult = new ArrayList<List<StringKeyValuePair>>();

        Iterator<List<StringKeyValuePair>> rows = result.iterator();
        //revisit each row
        while(rows.hasNext()){
            Map<String, Set<StringKeyValuePair>> stats = new LinkedHashMap<String,Set<StringKeyValuePair>>();
            List<StringKeyValuePair> l = rows.next();
            for(StringKeyValuePair kv : l){
                if(stats.containsKey(kv.getKey())){
                    stats.get(kv.getKey()).add(kv);
                }else{
                    Set<StringKeyValuePair> s = new TreeSet<StringKeyValuePair>();
                    s.add(kv);
                    stats.put(kv.getKey(), s);
                }
            }
            List<List<StringKeyValuePair>> tmpRows = new ArrayList<List<StringKeyValuePair>>();
            for(Map.Entry<String, Set<StringKeyValuePair>> e:stats.entrySet()){
                tmpRows = makeProduction(tmpRows, e.getValue());
            }
            newResult.addAll(tmpRows);
            rows.remove();
        }
        result.addAll(newResult);
        //following operation will also generates headers
        rows = result.iterator();
        while(rows.hasNext()){
            mapResult.add(convertListOfKVPToMap(rows.next()));
            rows.remove();
        }
        headers.addAll(headerSet);
        headerSet.clear();
    }
    /**
     * make production between two collections.
     * @param list
     * @param setToAdd
     * @return
     */
    private List<List<StringKeyValuePair>> makeProduction(List<List<StringKeyValuePair>> list,Set<StringKeyValuePair> setToAdd){
        List<List<StringKeyValuePair>> production = new ArrayList<List<StringKeyValuePair>>();
        if(list==null){
            list = new ArrayList<List<StringKeyValuePair>>();
        }

        for(StringKeyValuePair kv:setToAdd){
            if(list.size()==0){
                List<StringKeyValuePair> newList = new ArrayList<>();
                newList.add(kv);
                production.add(newList);
            }else{
                for(List<StringKeyValuePair> ol:list){
                    //for each original list, create new list to add new value
                    List<StringKeyValuePair> newList = new ArrayList<>(ol);
                    newList.add(kv);
                    production.add(newList);
                }
            }
        }
        return production;
    }
    /**
     * count number of ancenstors of lowest element within path
     * @param path
     * @return
     */
    private int ancestorCnt(String path){
        int i=-1,cnt=0;
        i = path.indexOf("/");
        while(i!=-1){
            cnt++;
            i=path.indexOf("/",i+1);
        }
        //remove the tailing count
        return cnt>0?cnt-1:cnt;
    }
    /**
     * Return maximum number of ancestors of an element in given xml.
     * @param xml
     * @return
     */
    private int maxAncestorCnt(Document xml) throws XPathExpressionException {
        //xml.select("//*[count(child::*)>0]");//find elements that has children
        XPathExpression expr = xpath.compile("//*[count(child::*)>0]");
        NodeList parents = (NodeList) expr.evaluate(xml, XPathConstants.NODESET);
        int maxCnt = 0;
        if(parents!=null && parents.getLength()>0) {
            for (int i = 0; i < parents.getLength(); i++) {
                Node si = parents.item(i);
                //reset for each node
                int cnt = 0;
                while (si.getParentNode() != null) {
                    si = si.getParentNode();
                    cnt++;
                }

                maxCnt = Math.max(maxCnt, cnt);
            }
        }

        return maxCnt;//with / as one parent level
    }
    /**
     * Normalize a key by replacing indices and special characters,
     * and using "_" as separator between original element names.
     * @param key
     * @return
     */
    private String normalizeAKey(String key){
        //[d+] as separator
        String tmp[] = key.split("\\Q[\\E[\\d]+\\Q]\\E");
        StringBuilder sb = new StringBuilder();
        for(String s: tmp){
            s = s.replaceAll("/@", "");
            s = s.replaceAll("/", "");
            sb.append(s).append("_");
        }
        sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }

	public static void main(String[] args) throws Exception {
		//System.out.println(Xml.isXml("<Relationship bala=\"ddd\">        <id>1</id>        <Type>match</Type>        <Weight>1.0</Weight>        <Score>100.0</Score>    </Relationship>        <Relationship>        <id>2</id>        <Type>match</Type>        <Weight>1.0</Weight>        <Score>90.0</Score>    </Relationship>"));
		String xml2 = "<Relations><Relationship bala=\"ddd\">        <id>1</id>        <Type>match</Type>        <Weight>1.0</Weight>        <Score>100.0</Score>    </Relationship>        <Relationship>        <id>2</id>        <Type>match</Type>        <Weight>1.0</Weight>        <Score>90.0</Score>    </Relationship></Relations>";

		XmlFlatter o = new XmlFlatter(xml2);
		System.out.println(o.getBody());
		Iterator<List<String>> it = o.iterator();
		while(it.hasNext()){
			System.out.println(it.next());
		}
	}


    @Override
    public Iterator<List<String>> iterator() {

        return new ListIterator<List<String>>(this);
    }

    private class ListIterator<Item> implements Iterator<Item> {
        private int index;
        XmlFlatter flatter= null;
        public ListIterator(XmlFlatter xmlFlatter) {
            index = 0;
            flatter = xmlFlatter;
        }

        public boolean hasNext()  {
            return (flatter.getHeaders().size()==0?false:(index<=flatter.getBody().size()));
        }
        public void remove()      { throw new UnsupportedOperationException();  }
        @SuppressWarnings("unchecked")
        public Item next() {
            if (!hasNext()) throw new RuntimeException("No more content.");
            Item tmp=null;
            if(index==0){

                tmp = (Item)flatter.getHeaders();
            }else{
                List<String> aRow = new ArrayList<String>();
                for(String h:flatter.getHeaders()){
                    aRow.add(flatter.getBody().get(index-1).get(h));

                }
                tmp = (Item)aRow;
            }
            index++;

            return tmp;
        }
    }
}
