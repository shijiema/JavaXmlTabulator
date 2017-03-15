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

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.io.*;
import java.util.*;

import static org.w3c.dom.Node.TEXT_NODE;

/**
 * Utility that converts XML to tabular data.
 * @link https://docs.oracle.com/javase/7/docs/api/org/w3c/dom/Node.html#getNodeValue()
 * @author Andrew Ma
 * @version 1.0, 02/14/2017
 */
public class JavaXmlTabulator implements Iterable<List<String>>{
    final String NAME_SEPARATOR = "_";
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
        List<KeyValueWithProperties> path = new ArrayList<KeyValueWithProperties>();
        this.xml = builder.parse(new InputSource(reader));
        //pre-process, merge non-repeat leaf elements as part of parent node
        bottomUpMerge(this.xml.getDocumentElement());
       //printing out
        //printXml(this.xml);
        //reform to get a more compact xml
        this.xml.normalizeDocument();
        //make production of nodes from bottom to top
        List<List<Node>> productionOfNodes = bottomUpProduction(this.xml);
        convertNodeToKVP(productionOfNodes);//TODO: try to use node directly
        headerSet.clear();
        convertDuplicatedColsToRows();
    }

    private void convertNodeToKVP(List<List<Node>> productionOfNodes) {
        Objects.requireNonNull(productionOfNodes);
        for(List<Node> lon : productionOfNodes){
            List<KeyValueWithProperties> kvpl = new ArrayList<>();
            for(Node n:lon){
                kvpl.add(nodeToKvp(n));
            }
            result.add(kvpl);
        }
    }

    private KeyValueWithProperties nodeToKvp(Node n) {
        Objects.requireNonNull(n);
        XPathExpression expr = null;
        String header = null;
        KeyValueWithProperties curNode = KeyValueWithProperties.newStringKeyValuePair(parseNodeName(n), null);
        //text
        NodeList children = n.getChildNodes();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < children.getLength(); i++) {
            if(Node.TEXT_NODE == children.item(i).getNodeType()) {
                sb.append(children.item(i).getNodeValue().trim()).append(" ");
            }
        }
        if (sb.length() > 0) {
            curNode.setValue(sb.toString().trim());
        }
        //attributes, included the elements wrapped in
        NamedNodeMap properties = n.getAttributes();
        if (properties != null && properties.getLength() > 0) {
            for (int i = 0; i < properties.getLength(); i++) {
                curNode.addProperty(properties.item(i).getNodeName(), properties.item(i).getNodeValue());
            }
        }
        return curNode;
    }

    private void printXml(Document document) {
        try {
            DOMSource domSource = new DOMSource(document);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.transform(domSource, result);
            System.out.println("XML IN String format is: \n" + writer.toString());
        }catch (Exception e){}
    }

    public JavaXmlTabulator(File xmlFile) throws Exception {
        initDocumentAndXpath();
        Reader reader = new FileReader(xmlFile);

        parse(reader);
    }
    public JavaXmlTabulator(String xmlStr) throws Exception {
        initDocumentAndXpath();
        Reader reader = new CharArrayReader(xmlStr.toCharArray());

        parse(reader);
    }
    private void bottomUpMerge(Node root) throws XPathExpressionException {
        if(root == null)return;
        //get all leaf nodes
        XPathExpression expr = xpath.compile("//*[count(child::*) < 1]");
        NodeList leafElements = (NodeList) expr.evaluate(root, XPathConstants.NODESET);
        boolean isToContinue = true;
        while(isToContinue){
            isToContinue=false;
            boolean changed = false;
            for(int i=0;i<leafElements.getLength();i++){
                Node n = leafElements.item(i);
                changed = mergeToParent(n);
                if(changed)isToContinue=true;
            }
            leafElements = (NodeList) expr.evaluate(root, XPathConstants.NODESET);
        }
    }

    private boolean mergeToParent(Node n) {
        //current node and parent must be both Elements
        if(!(n instanceof Element) || n.getParentNode()==null || !(n.getParentNode() instanceof Element))return false;
        //current node must be unique children element
        NodeList elementsByTagName = ((Element) n.getParentNode()).getElementsByTagName(n.getNodeName());
        if(elementsByTagName.getLength()>1)return false;
        //otherwise, merge to parent
        Element parent = (Element)(n.getParentNode());
        //set all its attributes to be parent node's attributes, prefixed with node name
        NamedNodeMap attributes = n.getAttributes();
        for(int i=0;i<attributes.getLength();i++){
            parent.setAttribute(n.getNodeName()+NAME_SEPARATOR+attributes.item(i).getNodeName(),attributes.item(i).getNodeValue());
        }
        //set its text also to be parent attribute, using node name as attribute directly. hopefully there's no conflict with existing parent attribute
        String text = n.getTextContent().trim();
        if(!"".equals(text) ){
            //get text content
            parent.setAttribute(n.getNodeName(),text);//do not keep white spaces
        }
        //remove node since its content has been merge into parent Element
        parent.removeChild(n);
        return true;//true means there is change
    }
    private boolean isUniqueElement(Node n){
        Objects.requireNonNull(n);
        Node parent = n.getParentNode();
        if(parent==null||parent == n.getOwnerDocument())return true;

        if(((Element)parent).getElementsByTagName(n.getNodeName()).getLength()>1)return false;
        return true;
    }
    /**
     * Recursively going through layers of XML, building up rows and columns as it goes.
     */
    private List<List<Node>> bottomUpProduction(Document doc) throws XPathExpressionException {
        Objects.requireNonNull(doc);
        //get leaf nodes
        XPathExpression expr = xpath.compile("//*[count(child::*) < 1]");
        NodeList leafElements = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
        //figure out parents nodes from children nodes
        Set<Node> parents = new LinkedHashSet<Node>();
        Set<Node> grandParents = new LinkedHashSet<Node>();
        for (int i = 0; i < leafElements.getLength(); i++) {
            Node p = leafElements.item(i).getParentNode();
            if(p!=null && p!= doc && p!=doc.getDocumentElement()) parents.add(p);
        }
        Map<Node, List<List<Node>>> nodeProductionMap = new LinkedHashMap<Node, List<List<Node>>>();


        while (parents.size() > 0) {
            Iterator<Node> itor = parents.iterator();
            while (itor.hasNext()) {
                Node curNode = itor.next();
                //some nodes goes back to document element earlier, stop them there
                if (curNode.getParentNode() != null && curNode.getParentNode() != doc.getDocumentElement() && doc.getParentNode()!= doc) {
                    grandParents.add(curNode.getParentNode());
                }
                collapseAParentNodeAndMakeDescendantProduction(curNode, nodeProductionMap);
                itor.remove();
            }

            parents.addAll(grandParents);
            grandParents.clear();
        }

        //now for the document element
        return collapseAParentNodeAndMakeDescendantProduction(doc.getDocumentElement(), nodeProductionMap);
    }

    private String parseNodeName(Node node){
        if(node==null)return null;
        StringBuffer sb = new StringBuffer();
        Document docNode = node.getOwnerDocument();
        while(node!=docNode && node!=null){
            sb.insert(0,"_"+node.getNodeName());
            node = node.getParentNode();
        }
        sb.delete(0,1);
        return sb.toString();
    }
    private KeyValueWithProperties getKVPFromNode(Node node) throws XPathExpressionException {

        if(node==null){
            return null;
        }

        XPathExpression expr = null;
        String header = null;

        //node and its properties
        //text
        expr = xpath.compile("./child::text()");
        NodeList textInCurrentNode = (NodeList) expr.evaluate(node, XPathConstants.NODESET);
        KeyValueWithProperties curNode = KeyValueWithProperties.newStringKeyValuePair(parseNodeName(node), null);
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

        //cur node its attribute nodes.
        NamedNodeMap properties = node.getAttributes();
        if(properties!=null && properties.getLength()>0){
            for(int i=0;i<properties.getLength();i++){
                curNode.addProperty(properties.item(i).getNodeName(),properties.item(i).getNodeValue());
            }
        }

        properties = null;
        return curNode;
    }

    private List<List<Node>> collapseAParentNodeAndMakeDescendantProduction(Node curNode, Map<Node,List<List<Node>>> nodeProductionMap) throws XPathExpressionException {
        NodeList childNodes = curNode.getChildNodes();
        //product the node
        //get repeat statistics
        Map<String,Set<Node>> stats = new LinkedHashMap<>();
        //List<Node> childrenToRemove = new ArrayList<>();
        for(int i=0;i<childNodes.getLength();i++){
            Node cn = childNodes.item(i);
            //for non-text nodes
            if(TEXT_NODE != cn.getNodeType() ) {
                String nodeName = parseNodeName(cn);
                if (stats.containsKey(nodeName)) {
                    stats.get(nodeName).add(cn);
                } else {
                    Set<Node> ns = new LinkedHashSet<Node>();
                    ns.add(cn);
                    stats.put(nodeName, ns);
                }
                //childrenToRemove.add(cn);
            }
        }
        List<List<Node>> firstSet = new ArrayList<List<Node>>();
        List<List<Node>> secondSet = null;
        //production of children nodes
        for (Map.Entry<String,Set<Node>> e : stats.entrySet()){
            //merge same type of element to product set
            for(Node n:e.getValue()){
                if(nodeProductionMap.containsKey(n)){
                    for(List<Node> l:nodeProductionMap.get(n)) {
                        firstSet.add(l);
                    }
                }else {
                    //not yet having a production set, making one
                    List<Node> nl = new ArrayList<>();
                    nl.add(n);
                    firstSet.add(nl);
                }
            }
            secondSet = makeNodeProduction(firstSet,secondSet);
            firstSet = new ArrayList<List<Node>>();
        }
        if(secondSet!=null){
            //appending current node
            for(List<Node> ln:secondSet){
                ln.add(0,curNode);
            }
        }else{
            //only a document element
            List<Node> list = new ArrayList<>();
            list.add(curNode);
            secondSet = new ArrayList<List<Node>>();
            secondSet.add(list);
        }
        //after this the secondSet is current node's corresponding descendant node' production, including itself
        nodeProductionMap.put(curNode,secondSet);
        //remove children
        //not necessary
//        for(Node n:childrenToRemove) {
//            curNode.removeChild(n);
//        }
        return secondSet;
    }

    private List<List<Node>> makeNodeProduction(List<List<Node>> firstSet,List<List<Node>> secondSet) {
        Objects.requireNonNull(firstSet);
        List<List<Node>> newNodeProd = new ArrayList<List<Node>>();
        if(secondSet==null || secondSet.size()==0)
            return firstSet;
        for(List<Node> f:firstSet){
            for (List<Node> ln : secondSet) {
                    List<Node> aNewList = new ArrayList<>(ln);
                    aNewList.addAll(0, f);
                    newNodeProd.add(aNewList);
            }
        }
        return newNodeProd;
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
        Map<String,String> mp = new TreeMap<String,String>();
        for(KeyValueWithProperties kv:l){
            if(kv.getValue()!=null&&!"".equals(kv.getValue())) {
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
            if(kv.getProperties()!=null ||(kv.getValue()!=null && !"".equals(kv.getValue()))) {
                if (list.size() == 0) {
                    List<KeyValueWithProperties> newList = new ArrayList<>();
                    newList.add(kv);
                    production.add(newList);
                } else {
                    for (List<KeyValueWithProperties> ol : list) {
                        //for each original list, create new list to add new value
                        List<KeyValueWithProperties> newList = new ArrayList<>(ol);
                        newList.add(kv);
                        production.add(newList);
                    }
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
		//String xml2 = "<Relations><Relationship bala=\"ddd\">noise<id>1</id><Type>match</Type>noise 2<Weight>1.0</Weight>        <Score>100.0</Score>    </Relationship>        <Relationship>        <id>2</id>   noise 3     <Type>match</Type>        <Weight>1.0</Weight>        <Score>90.0</Score>    </Relationship></Relations>";
        //String xml2 = "<E1>TextE1<E2 p1=\"v1\">TextE2</E2><E2 p1=\"v3\">E2</E2><E2 p1=\"v4\">E22<E15>TextE15</E15></E2><E3>TextE3</E3> <E4><E5>TextE5</E5><E6>TextE6<E9>textE9</E9><E10>TextE10</E10></E6></E4><E7 p2=\"v2\">TextE7<E8>TextE8</E8></E7><E11><E12><E13><E14>TextE14</E14></E13></E12></E11></E1>";
        String xml2 = "<Relations><first>hellow</first>noise<second>Andrew</second></Relations>";
        JavaXmlTabulator o = new JavaXmlTabulator(xml2);
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
        JavaXmlTabulator flatter= null;
        public ListIterator(JavaXmlTabulator javaXmlTabulator) {
            index = 0;
            flatter = javaXmlTabulator;
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
