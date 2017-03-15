# Java Xml Tabulator
Converting XML to tabular form of data in pure Java implementation. No third party library required.

For am Xml such as
```
<Relations>
	<Relationship bala=&quot;ddd&quot;>
		<id>1</id>
		<Type>match</Type>
		<Weight>1.0</Weight>
		<Score>100.0</Score>
	</Relationship>
	<Relationship>
		<id>2</id>
		<Type>match</Type>
		<Weight>1.0</Weight>
		<Score>90.0</Score>
	</Relationship>
</Relations>
```
It will convert it to a flatten version of data that if iterating, looks like this:
```
[Relations_Relationship_bala, Relations_Relationship_id, Relations_Relationship_Type, Relations_Relationship_Weight, Relations_Relationship_Score]
[ddd, 1, match, 1.0, 100.0]
[null, 2, match, 1.0, 90.0]
```
It has two ways to access transformed data.

First method is through its iterator() method, which will return each row as List<String>. First row is header and subsequent rows are body content. Headers and body content have been aligned.

Second method is through its getHeaders() and getBody() if partial data access is what is wanted.

## Algorithm

### Observations:
1. non-repeat elements in XML could be treated as parent node's attributes
2. repeat element in XML usually means multiple rows after being flattened
3. path from root to node makes the columns in tabular format
```
tablify(){
    with XML tree,
    1. merge non-repeat elements to their parents
        1.1 from leaf to root, merge non-repeat children element to its parent as its parent's attributes. this includes both text and its attributes
            attribute name for child node is child node element name; attribute value for child node element is child node's text value
            attribute name for child node attributes are child node element name + child node attribute name; value is child element attribute value
        1.2 remove these children from their parents
        1.3 repeat 1.1 and 1.2 until no more such children exists
    2. make node production from leaf to root
        2.1 for a node, make its equivalent node production
                2.1.1 leaf node's children node is null
                2.1.2 rows of repeat children element 1 * rows repeat children element 2 * rows repeat children element n
                2.1.3 insert this parent node to head of each produced row from 2.1.1

        2.2 do 2.1 for all parent nodes(null node's parent is the leaf node), but stop at root element
        2.3 do 2.1 for root element(this is because different path has different depth, they have to wait to do final production)
    3. in each row in final node production, convert node to columns
        3.1 node column name = path to node; node value = text content in node
        3.2 node attribute column name = path to node + attribute name; node attribute column value = attribute value
        (this works well for non-repeat node wrapped as parent node's attribute)
    4.return key-value paired node production
}

```
* **Shi Jie Ma** - *Initial work*