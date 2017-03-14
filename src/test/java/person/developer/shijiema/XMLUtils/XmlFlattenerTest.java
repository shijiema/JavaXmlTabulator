package person.developer.shijiema.XMLUtils;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;

/**
 * Unit test for simple App.
 */
public class XmlFlattenerTest
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public XmlFlattenerTest(String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( XmlFlattenerTest.class );
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
        Assert.assertNotNull(o.getBody());
        Assert.assertEquals(o.getBody().size(),2);
        Assert.assertEquals(o.getBody().get(0).get("Relations_Relationship_bala"),"ddd");
        Assert.assertNull(o.getBody().get(1).get("Relations_Relationship_bala"));
        Assert.assertEquals(o.getBody().get(1).get("Relations_Relationship_Score"),"90.0");
    }
    public void testXmlFromString() throws Exception {
        String xml2 = "<Relations><Relationship bala=\"ddd\">        <id>1</id>        <Type>match</Type>        <Weight>1.0</Weight>        <Score>100.0</Score>    </Relationship>        <Relationship>        <id>2</id>        <Type>match</Type>        <Weight>1.0</Weight>        <Score>90.0</Score>    </Relationship></Relations>";

        XmlFlattener o = new XmlFlattener(xml2);
//        System.out.println(o.getHeaders());
//        System.out.println(o.getBody());
        Assert.assertEquals(o.getHeaders().size(),5);
        Assert.assertNotNull(o.getBody());
        Assert.assertEquals(o.getBody().size(),2);
        for(Map<String,String> m:o.getBody()) {
            if(m.get("Relations_Relationship_id").equals("1")) {
                Assert.assertEquals(m.get("Relations_Relationship_bala"), "ddd");
            }
            if(m.get("Relations_Relationship_id").equals("2")) {
                Assert.assertNull(m.get("Relations_Relationship_bala"));
                Assert.assertEquals(m.get("Relations_Relationship_Score"), "90.0");
            }

        }
    }
    public void testXmlFromFile() throws Exception{
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("Relations.xml").getFile());
        XmlFlattener o = new XmlFlattener(file);
        Assert.assertEquals(o.getHeaders().size(),5);
        Assert.assertNotNull(o.getBody());
        Assert.assertEquals(o.getBody().size(),2);
        for(Map<String,String> m:o.getBody()) {
            if(m.get("Relations_Relationship_id").equals("1")) {
                Assert.assertEquals(m.get("Relations_Relationship_bala"), "ddd");
            }
            if(m.get("Relations_Relationship_id").equals("2")) {
                Assert.assertNull(m.get("Relations_Relationship_bala"));
                Assert.assertEquals(m.get("Relations_Relationship_Score"), "90.0");
            }

        }
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
    public void testBreakfactMenu() throws Exception{
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("BreakfastMenu.xml").getFile());
        XmlFlattener o = new XmlFlattener(file);
//        Iterator itor = o.iterator();
//        while(itor.hasNext()){
//            System.out.println(itor.next());
//        }
        Assert.assertEquals(o.getHeaders().size(),4);
        Assert.assertNotNull(o.getBody());
        Assert.assertEquals(o.getBody().size(),5);
        for(Map<String,String> m:o.getBody()) {
            if("Strawberry Belgian Waffles".equals(m.get("breakfast_menu_food_name"))) {
                Assert.assertEquals(o.getBody().get(1).get("breakfast_menu_food_price"), "$7.95");
            }
            if("Homestyle Breakfast".equals(m.get("breakfast_menu_food_name"))) {
                Assert.assertEquals(o.getBody().get(4).get("breakfast_menu_food_calories"), "950");
            }
        }
    }
    public void testEtest() throws Exception{
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("ETest.xml").getFile());
        XmlFlattener o = new XmlFlattener(file);
        Iterator itor = o.iterator();
//        while(itor.hasNext()){
//            System.out.println(itor.next());
//        }
        Assert.assertEquals(o.getHeaders().size(),13);
        Assert.assertNotNull(o.getBody());
        Assert.assertEquals(o.getBody().size(),8);
        Assert.assertEquals(o.getBody().get(0).get("E1"),"TextE1");
        Assert.assertEquals(o.getBody().get(0).get("E1_E3"),"TextE3");
        Assert.assertEquals(o.getBody().get(0).get("E1_E2"),"TextE2");
        Assert.assertEquals(o.getBody().get(0).get("E1_E2_p1"),"v1");
        Assert.assertEquals(o.getBody().get(0).get("E1_E4_E6_E10"),"TextE10");
        Assert.assertEquals(o.getBody().get(1).get("E1_E4_E6_E10"),"TextE10");
        Assert.assertNotNull(o.getBody().get(2).get("E1_E4_E6_E10"));
        Assert.assertEquals(o.getBody().get(7).get("E1_E2_p1"),"v3");
        Assert.assertEquals(o.getBody().get(7).get("E1_E2_E15"),"TextE15_3");
    }
    public void testTranslinkBusesBadPerf() throws Exception{
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("TranslinkBuses.xml").getFile());
        //System.out.println(Calendar.getInstance().getTime());
        XmlFlattener o = new XmlFlattener(file);
        //System.out.println(Calendar.getInstance().getTime());
        Iterator itor = o.iterator();
//        while(itor.hasNext()){
//            System.out.println(itor.next());
//        }

    }

}
