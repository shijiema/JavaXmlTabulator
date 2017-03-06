# XmlFlattener
Converting XML to tabular form of data in pure Java implementation. No third party library required.

For am Xml such as

&lt;Relations&gt;
	&lt;Relationship bala=&quot;ddd&quot;&gt;
		&lt;id&gt;1&lt;/id&gt;
		&lt;Type&gt;match&lt;/Type&gt;
		&lt;Weight&gt;1.0&lt;/Weight&gt;
		&lt;Score&gt;100.0&lt;/Score&gt;
	&lt;/Relationship&gt;
	&lt;Relationship&gt;
		&lt;id&gt;2&lt;/id&gt;
		&lt;Type&gt;match&lt;/Type&gt;
		&lt;Weight&gt;1.0&lt;/Weight&gt;
		&lt;Score&gt;90.0&lt;/Score&gt;
	&lt;/Relationship&gt;
&lt;/Relations&gt;

It will convert it to a flatten version of data that if iterating, looks like this:

[Relations_Relationship_bala, Relations_Relationship_id, Relations_Relationship_Type, Relations_Relationship_Weight, Relations_Relationship_Score]
[ddd, 1, match, 1.0, 100.0]
[null, 2, match, 1.0, 90.0]

It has two ways to access transformed data.

First method is through its iterator() method, which will return each row as List<String>. First row is header and subsequent rows are body content. Headers and body content have been aligned.

Second method is through its getHeaders() and getBody() if partial data access is what is wanted.

Algorithm

thoughts,
-leaf elements are on same row as current element;
--non-repeat leaf elements are treated attributes of current element;
--repeat leaf elements are treated as attributes of current element as well, but they need to be put on different rows.
(not absolutely sure)
-for non-leaf elements,
--merge non-repeat ones into same row as current element,
--create new row for the repeat ones.

basing the current node:
1. parse out its text and properties, they should stay together on same row.

2. leave nodes (usually columns in a row) are treated as current node's attributes, so they stay on same row.
2.1 trying to include non-repeat leaf nodes (static content) in same row(this becomes part of common parent)
2.2 trying to include repeat leaf node in same row(the repeated nodes will be un-pivoted to multiple rows)
(not absolutely sure)

3. then process non-leaf nodes.
each of them become current element
3.1 for the repeated non-leaf elements , for first one, join it with old row;
    for second and on, create new row for them based on common parents (this might be different from common parent for first one)
    determined at 1.1
3.2 for the single non-leaf elements,join it with old row
3.3 repeat the whole process for current element.

