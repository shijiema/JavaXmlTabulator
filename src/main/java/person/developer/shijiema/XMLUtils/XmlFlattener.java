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
import org.w3c.dom.NamedNodeMap;
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
public class XmlFlattener implements Iterable<List<String>>{
    List<Map<String,String>> mapResult = new ArrayList<Map<String,String>>();
    List<List<KeyValueWithProperties>> result = new ArrayList<List<KeyValueWithProperties>>();
    Set<String> headerSet = new LinkedHashSet<String>();
    List<String> headers = new LinkedList<String>();
    DocumentBuilder builder = null;
    Document xml = null;
    XPath xpath = null;

    private void initDocumentAndXpath() throws Exception{
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        //factory.setNamespaceAware(true);
        builder = factory.newDocumentBuilder();
        XPathFactory xPathfactory = XPathFactory.newInstance();
        xpath = xPathfactory.newXPath();
    }
    private void parse(Reader reader) throws Exception{
        List<KeyValueWithProperties> firstRow = new ArrayList<KeyValueWithProperties>();
        this.xml = builder.parse(new InputSource(reader));
        //figure out stop level
        buildResult(this.xml,"/", firstRow);
        headerSet.clear();
        convertDuplicatedColsToRows();
    }
    public XmlFlattener(File xmlFile) throws Exception {
        initDocumentAndXpath();
        Reader reader = new FileReader(xmlFile);

        parse(reader);
    }
    public XmlFlattener(String xmlStr) throws Exception {
        initDocumentAndXpath();
        Reader reader = new CharArrayReader(xmlStr.toCharArray());

        parse(reader);
    }
    /**
     * Recursively going through layers of XML, building up rows and columns as it goes.
     * @param path xPath of current selections
     * @param row	a row to build
     */
    private void buildResult(Node node, String path, List<KeyValueWithProperties> row) throws XPathExpressionException {
        //no xml is to process
        if(node==null){
            return;
        }
//TODO: use Node to replace KeyValueWithProperties
        //process children in interests.
        XPathExpression expr = null;
        String header = null;
        if("/".equals(path)&&result.size()==0){result.add(row);}

        //current node and its properties
        //text
        expr = xpath.compile("child::text()");
        NodeList textInCurrentNode = (NodeList) expr.evaluate(node, XPathConstants.NODESET);
        KeyValueWithProperties curNode = KeyValueWithProperties.newStringKeyValuePair(normalizeAKey(path), null);
        if(textInCurrentNode!=null &&textInCurrentNode.getLength()>0){
            StringBuilder sb = new StringBuilder();
            for(int i=0;i<textInCurrentNode.getLength();i++){
                sb.append(textInCurrentNode.item(i).getNodeValue().trim()).append(" ");
            }
            if(sb.length()>0) {
                curNode.setValue(sb.toString().trim());
            }
        }
        textInCurrentNode = null;

        //cur node its attribute nodes. hide them within node for node centric operations, expend them out later
        expr = xpath.compile("attribute::*");
        NodeList properties = (NodeList) expr.evaluate(node, XPathConstants.NODESET);
        if(properties!=null && properties.getLength()>0){
            for(int i=0;i<properties.getLength();i++){
                curNode.addProperty(properties.item(i).getNodeName(),properties.item(i).getNodeValue());
            }
        }
        //adding it as a column if it has some values
        if((curNode.getValue()!=null && !"".equals(curNode.getValue())) || curNode.getProperties()!=null){
            row.add(curNode);
        }
        properties = null;
        //making a common parent candidate
        List<KeyValueWithProperties> commonParents = new ArrayList<KeyValueWithProperties>();
        commonParents.addAll(row);
        //leaf children of current node. including into same row. repeat element becomes new row
        expr = xpath.compile("*[count(child::*) < 1]");
        List<KeyValueWithProperties> repeatElements =new LinkedList<KeyValueWithProperties>();
        NodeList leafChildren = (NodeList) expr.evaluate(node, XPathConstants.NODESET);
        if(leafChildren!=null && leafChildren.getLength()>0){

            for(int i=0;i<leafChildren.getLength();i++){
                Node t = leafChildren.item(i);
                String nodeName = t.getNodeName();
                //for leaf Element, its node value is null, trying to get its text node's value
                String nodeValue= t.getFirstChild()==null?null:t.getFirstChild().getNodeValue();
                header = path+nodeName;
                header = normalizeAKey(header);
                KeyValueWithProperties tmpNode = KeyValueWithProperties.newStringKeyValuePair(header,nodeValue);
                //process its properties
                NamedNodeMap nodeProps = t.getAttributes();
                if(nodeProps!=null && nodeProps.getLength()>0){
                    for(int j=0;j<nodeProps.getLength();j++){
                        tmpNode.addProperty(nodeProps.item(j).getNodeName(),nodeProps.item(j).getNodeValue());
                    }
                }
                if((tmpNode.getValue()!=null && !"".equals(tmpNode.getValue().trim())) || tmpNode.getProperties()!=null){
                    expr = xpath.compile(nodeName+"[count(following-sibling::"+nodeName+") > 0]");
                    NodeList siblingOfSameElement = (NodeList) expr.evaluate(node, XPathConstants.NODESET);
                    if(siblingOfSameElement==null || siblingOfSameElement.getLength()==0){
                        row.add(tmpNode);
                    }else{
                        repeatElements.add(tmpNode);
                    }
                }
            }
            //possibly adding more content to common parent: all unique leaf nodes under cur node are treated as part of cur node,
            // they stay on same row
            commonParents.clear();
            commonParents.addAll(row);
            //adding rest of leaf child into row
            //repeat leaf node, adding to row directly and leave for unpivot function to straighten them
            for(KeyValueWithProperties n:repeatElements){
                row.add(n);
            }
        }
        leafChildren = null;
        //for element having child elements
        expr = xpath.compile("*[count(child::*) >= 1]");
        NodeList childrenAsParent = (NodeList) expr.evaluate(node, XPathConstants.NODESET);
        if(childrenAsParent!=null&&childrenAsParent.getLength()>0){
            //spawn a new row for repeating children
            for(int i=0;i<childrenAsParent.getLength();i++){
                Node cn = childrenAsParent.item(i);
                String curElement = cn.getNodeName();
                String curPath = path+curElement+"/";
                String curPathNorm = normalizeAKey(curPath);
                expr = xpath.compile(curElement+"[count(following-sibling::"+curElement+") > 0]");
                NodeList siblingWithSameElement = (NodeList) expr.evaluate(node, XPathConstants.NODESET);
                //it element is repeating

                if(siblingWithSameElement!=null && siblingWithSameElement.getLength()>0) {
                    expr = xpath.compile(curElement + "[1]");
                    NodeList firstElement = (NodeList) expr.evaluate(node, XPathConstants.NODESET);
                    Node firstNode = firstElement.item(0);
                    //if it is first one
                    if (cn == firstNode) {
                        buildResult(cn, curPath, row);
                    } else {
                        //second one and on, creating new rows
                        List<KeyValueWithProperties> aNewRow = new ArrayList<KeyValueWithProperties>();
                        aNewRow.addAll(commonParents);
                        result.add(aNewRow);
                        buildResult(cn, curPath, aNewRow);
                    }
                }else{
                    buildResult(cn, curPath, row);
                }
            }
        }
            childrenAsParent = null;
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
    private Map<String,String> convertListOfKVPToMap(List<KeyValueWithProperties> l){
        Objects.requireNonNull(l);
        Map<String,String> mp = new LinkedHashMap<String,String>();
        for(KeyValueWithProperties kv:l){
            if(kv.getValue()!=null) {
                mp.put(kv.getKey(), kv.getValue());
                headerSet.add(kv.getKey());
            }
            if(kv.getProperties()!=null){
                for(Map.Entry<String, String> e:kv.getProperties().entrySet()){
                    String propKey = kv.getKey()+"_"+e.getKey();
                    mp.put(propKey, e.getValue());
                    headerSet.add(propKey);
                }
            }
        }
        return mp;
    }
    /**
     * For duplicate columns within a row, convert them to be in different rows.
     */
    private void convertDuplicatedColsToRows(){
        List<List<KeyValueWithProperties>> newResult = new ArrayList<List<KeyValueWithProperties>>();

        Iterator<List<KeyValueWithProperties>> rows = result.iterator();
        //revisit each row
        while(rows.hasNext()){
            Map<String, Set<KeyValueWithProperties>> stats = new LinkedHashMap<String,Set<KeyValueWithProperties>>();
            List<KeyValueWithProperties> l = rows.next();
            for(KeyValueWithProperties kv : l){
                if(stats.containsKey(kv.getKey())){
                    stats.get(kv.getKey()).add(kv);
                }else{
                    Set<KeyValueWithProperties> s = new TreeSet<KeyValueWithProperties>();
                    s.add(kv);
                    stats.put(kv.getKey(), s);
                }
            }
            List<List<KeyValueWithProperties>> tmpRows = new ArrayList<List<KeyValueWithProperties>>();
            for(Map.Entry<String, Set<KeyValueWithProperties>> e:stats.entrySet()){
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
    private List<List<KeyValueWithProperties>> makeProduction(List<List<KeyValueWithProperties>> list, Set<KeyValueWithProperties> setToAdd){
        List<List<KeyValueWithProperties>> production = new ArrayList<List<KeyValueWithProperties>>();
        if(list==null){
            list = new ArrayList<List<KeyValueWithProperties>>();
        }

        for(KeyValueWithProperties kv:setToAdd){
            if(list.size()==0){
                List<KeyValueWithProperties> newList = new ArrayList<>();
                newList.add(kv);
                production.add(newList);
            }else{
                for(List<KeyValueWithProperties> ol:list){
                    //for each original list, create new list to add new value
                    List<KeyValueWithProperties> newList = new ArrayList<>(ol);
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
        // "/" as separator
        if('/'==key.charAt(0)){
            key = key.substring(1);
        }
        if(key.length()>0 && '/'==key.charAt(key.length()-1)){
            key = key.substring(0,key.length()-1);
        }
        key = key.replaceAll("/@","_");
        key = key.replaceAll("/","_");

        return key;
    }

	public static void main(String[] args) throws Exception {
		//System.out.println(Xml.isXml("<Relationship bala=\"ddd\">        <id>1</id>        <Type>match</Type>        <Weight>1.0</Weight>        <Score>100.0</Score>    </Relationship>        <Relationship>        <id>2</id>        <Type>match</Type>        <Weight>1.0</Weight>        <Score>90.0</Score>    </Relationship>"));
		String xml2 = "<Relations><Relationship bala=\"ddd\">        <id>1</id>        <Type>match</Type>        <Weight>1.0</Weight>        <Score>100.0</Score>    </Relationship>        <Relationship>        <id>2</id>        <Type>match</Type>        <Weight>1.0</Weight>        <Score>90.0</Score>    </Relationship></Relations>";
        //String xml2 = "<E1>TextE1<E2 p1=\"v1\">TextE2</E2><E2 p1=\"v3\">E2</E2><E2 p1=\"v4\">E22<E15>TextE15</E15></E2><E3>TextE3</E3> <E4><E5>TextE5</E5><E6>TextE6<E9>textE9</E9><E10>TextE10</E10></E6></E4><E7 p2=\"v2\">TextE7<E8>TextE8</E8></E7><E11><E12><E13><E14>TextE14</E14></E13></E12></E11></E1>";
		XmlFlattener o = new XmlFlattener(xml2);
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
        XmlFlattener flatter= null;
        public ListIterator(XmlFlattener xmlFlattener) {
            index = 0;
            flatter = xmlFlattener;
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
