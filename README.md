# XmlFlatter
Converting XML to tabular form of data in pure Java implementation. No third party library dependency.

For am Xml such as

<Relations>
	<Relationship bala="ddd">
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

It will convert it to a flatten version of data that if iterating, looks like this:

[Relations_Relationship_bala, Relations_Relationship_id, Relations_Relationship_Type, Relations_Relationship_Weight, Relations_Relationship_Score]
[ddd, 1, match, 1.0, 100.0]
[null, 2, match, 1.0, 90.0]

It has two ways to access transformed data.

First method is through its iterator() method, which will return each row as List<String>. First row is header and subsequent rows are body content. Headers and body content have been aligned.

Second method is through its getHeaders() and getBody() if partial data access is what is wanted.