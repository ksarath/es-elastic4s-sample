# Elastic Search - Nested queries, Nested aggregations sample using elastic4s and Scala

## Usage

You can create nested structure documents in Elastic Search.

See SampleMapping in tests. Here we have an Article which have its own properties like name and tags.
An article is also authored by one or more authors.
Each author may have published zero or more books.
Each book is published by one or more publishers.

### NestedQuery Example

To get articles authored by either author 1 or author 2
```
object SM extends NestedQuery
val queryDef = search("indexName" / "mappingName") query {
  boolQuery() must SM.generateFilterQueries(Seq(
    SearchFilter("authors.name.keyword", "author 1", "nestedText", "term"),
    SearchFilter("authors.name.keyword", "author 2", "nestedText", "term")
  ))
}

val result = Await.result(client execute queryDef, 20 seconds)
val articles = result.hits.toList map (_.to[Article])
```

### NestedAggregation Example

To get all authors linked to either Publisher 1 or Publisher 2
```
object SM extends NestedQuery with NestedAggregation
val queryDef = search("indexName" / "mappingName") aggregations {
  SM.getNestedAggregationQuery(
    NestedAggregationField("authors.name.keyword", "nestedText"),
    Seq(
      SearchFilter("authors.books.publishers.name.keyword", "Publisher 1", "nestedText", "term"),
      SearchFilter("authors.books.publishers.name.keyword", "Publisher 2", "nestedText", "term")
    )
  )
}

val result = Await.result(client execute queryDef, 20 seconds)
val authors = SM.getAggregationResponse(result) map (_.to[Author])
```

# License

This software is licensed under the Apache 2.0 license, see [LICENSE](https://github.com/ksarath/es-elastic4s-sample/blob/master/LICENSE).
