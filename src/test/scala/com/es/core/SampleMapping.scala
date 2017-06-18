package com.es.core

import com.sksamuel.elastic4s.ElasticDsl.{nestedField, _}
import com.sksamuel.elastic4s.bulk.RichBulkResponse
import com.sksamuel.elastic4s.{Hit, HitReader, Indexable, TcpClient}
import com.sksamuel.elastic4s.mappings.PutMappingDefinition
import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.concurrent.Future

private[core] case class Publisher(id: String, name: String)
private[core] case class Book(id:String, name: String, publishers: Seq[Publisher] = Nil)
private[core] case class Author(id: String, name: String, books: Seq[Book] = Nil)
private[core] case class Article(id: String, title: String, tags: Option[String] = None, authors: Seq[Author] = Nil)

trait SampleMapping extends ESMapping {
  private implicit val publisherFmt = jsonFormat2(Publisher)
  private implicit val bookFmt = jsonFormat3(Book)
  private implicit val authorFmt = jsonFormat3(Author)
  private implicit val articleFmt = jsonFormat4(Article)

  private implicit object ArticleJson extends Indexable[Article] {
    override def json(t: Article): String = t.toJson.compactPrint
  }
  implicit object ArticleHitAs extends HitReader[Article] {
    override def read(hit: Hit): Either[Throwable, Article] = {
      Right(hit.sourceAsString.parseJson.convertTo[Article])
    }
  }

  override val indexName: String = "myindex"

  override val mappingName: String = "articles"

  protected override val mappingDefinition: PutMappingDefinition = {
    putMapping(indexName / mappingName) fields (
      keywordField("id") index true,
      textField("title") index false fields (keywordField("keyword") normalizer ("lowerCaseNormalizer")),
      keywordField("tags") index true,
      nestedField("authors") fields (
        keywordField("id") index true,
        textField("name") index false fields (keywordField("keyword") normalizer ("lowerCaseNormalizer")),
        nestedField("books") fields (
          keywordField("id") index true,
          textField("name") index false fields (keywordField("keyword") normalizer ("lowerCaseNormalizer")),
          nestedField("publishers") fields (
            keywordField("id") index true,
            textField("name") index false fields (keywordField("keyword") normalizer ("lowerCaseNormalizer"))
          )
        )
      )
    )
  }

  private[core] def insertSampleData()(implicit elasticClient: TcpClient): Future[RichBulkResponse] = {
    elasticClient execute bulk(
      indexInto(indexName / mappingName) source Article("id 1", "article 1", Option("scala, es4s, es"), Seq(
        Author("aid 1", "author 1", Seq(Book("bid 1", "Book 1", Seq(Publisher("pid 1", "Publisher 1")))))
      )),
      indexInto(indexName / mappingName) source Article("id 2", "article 2", Option("scala, es4s, es"), Seq(
        Author("aid 2", "author 2", Seq(Book("bid 2", "Book 2", Seq(Publisher("pid 2", "Publisher 2")))))
      )),
      indexInto(indexName / mappingName) source Article("id 3", "article 3", Option("scala, es4s, es"), Seq(
        Author("aid 3", "author 3", Seq(Book("bid 3", "Book 3", Seq(Publisher("pid 1", "Publisher 1")))))
      )),
      indexInto(indexName / mappingName) source Article("id 4", "article 4", Option("scala, es4s, es"), Seq(
        Author("aid 4", "author 4", Seq(Book("bid 4", "Book 4", Seq(Publisher("pid 2", "Publisher 2")))))
      ))
    )
  }
}
