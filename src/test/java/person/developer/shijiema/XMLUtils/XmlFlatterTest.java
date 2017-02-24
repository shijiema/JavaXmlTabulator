package person.developer.shijiema.XMLUtils;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.util.Iterator;

/**
 * Unit test for simple App.
 */
public class XmlFlatterTest
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public XmlFlatterTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( XmlFlatterTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testText() throws Exception {
        String xml2 = "<Relations><Relationship bala=\"ddd\">        <id>1</id>  noise      <Type>match</Type>        <Weight>1.0</Weight>        <Score>100.0</Score>    </Relationship>        <Relationship>        <id>2</id>        <Type>match</Type>        <Weight>1.0</Weight>        <Score>90.0</Score>    </Relationship></Relations>";

        XmlFlattener o = new XmlFlattener(xml2);
//        Iterator itor = o.iterator();
//        while(itor.hasNext()){
//            System.out.println(itor.next());
//        }
//        System.out.println(o.getHeaders());
//        System.out.println(o.getBody());
        Assert.assertEquals(o.getHeaders().size(),6);
        Assert.assertEquals(o.getHeaders().get(0),"Relations_Relationship");
        Assert.assertEquals(o.getHeaders().get(1),"Relations_Relationship_bala");
        Assert.assertEquals(o.getHeaders().get(5),"Relations_Relationship_Score");
        Assert.assertNotNull(o.getBody());
        Assert.assertEquals(o.getBody().size(),2);
        Assert.assertEquals(o.getBody().get(0).get("Relations_Relationship_bala"),"ddd");
        Assert.assertNull(o.getBody().get(1).get("Relations_Relationship_bala"));
        Assert.assertEquals(o.getBody().get(1).get("Relations_Relationship_Score"),"90.0");
    }
    public void testXmlFromString() throws Exception {
        String xml2 = "<Relations><Relationship bala=\"ddd\">        <id>1</id>        <Type>match</Type>        <Weight>1.0</Weight>        <Score>100.0</Score>    </Relationship>        <Relationship>        <id>2</id>        <Type>match</Type>        <Weight>1.0</Weight>        <Score>90.0</Score>    </Relationship></Relations>";

        XmlFlattener o = new XmlFlattener(xml2);
        //System.out.println(o.getHeaders());
        //System.out.println(o.getBody());
        Assert.assertEquals(o.getHeaders().size(),5);
        Assert.assertEquals(o.getHeaders().get(0),"Relations_Relationship_bala");
        Assert.assertEquals(o.getHeaders().get(4),"Relations_Relationship_Score");
        Assert.assertNotNull(o.getBody());
        Assert.assertEquals(o.getBody().size(),2);
        Assert.assertEquals(o.getBody().get(0).get("Relations_Relationship_bala"),"ddd");
        Assert.assertNull(o.getBody().get(1).get("Relations_Relationship_bala"));
        Assert.assertEquals(o.getBody().get(1).get("Relations_Relationship_Score"),"90.0");
    }
    public void testXmlFromFile() throws Exception{
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("Relations.xml").getFile());
        XmlFlattener o = new XmlFlattener(file);
        Assert.assertEquals(o.getHeaders().size(),5);
        Assert.assertEquals(o.getHeaders().get(0),"Relations_Relationship_bala");
        Assert.assertEquals(o.getHeaders().get(4),"Relations_Relationship_Score");
        Assert.assertNotNull(o.getBody());
        Assert.assertEquals(o.getBody().size(),2);
        Assert.assertEquals(o.getBody().get(0).get("Relations_Relationship_bala"),"ddd");
        Assert.assertNull(o.getBody().get(1).get("Relations_Relationship_bala"));
        Assert.assertEquals(o.getBody().get(1).get("Relations_Relationship_Score"),"90.0");
    }
    public void testPomXml() throws Exception{
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("testXmlPom.xml").getFile());
        XmlFlattener o = new XmlFlattener(file);
        //there is no repeated element, the entire xml is converted to one line
        Assert.assertEquals(o.getBody().size(),1);
//        Iterator itor = o.iterator();
//        while(itor.hasNext()){
//            System.out.println(itor.next());
//        }

    }
}
