# XmlFlatter
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