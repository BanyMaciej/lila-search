package lila.search
package client

import akka.util.ByteString
import com.github.plokhotnyuk.jsoniter_scala.core._
import lila.search.spec.*
import play.api.libs.ws.BodyWritable
import play.api.libs.ws.InMemoryBody
import play.api.libs.ws.StandaloneWSClient
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import smithy4s.json.Json.given
import smithy4s.schema.Schema
import play.api.libs.ws.BodyReadable

object implicits:

  given [A](using JsonCodec[A]): BodyWritable[A] =
    BodyWritable(a => InMemoryBody(ByteString.fromArrayUnsafe(writeToArray(a))), "application/json")

  given [A](using JsonCodec[A]): BodyReadable[A] =
    BodyReadable(res => readFromArray(res.bodyAsBytes.toArray))

  def apply(client: StandaloneWSClient, url: String)(using ExecutionContext): SearchService[Future] =
    PlayClient(client, url)

class PlayClient(client: StandaloneWSClient, baseUrl: String)(using ExecutionContext)
    extends SearchService[Future]:

  import implicits.given

  override def countForum(text: String, troll: Boolean): Future[CountResponse] =
    request(s"$baseUrl/count/forum", ForumInputBody(text, troll))

  override def searchForum(body: ForumInputBody, from: Int, size: Int): Future[SearchResponse] =
    request(s"$baseUrl/search/forum/{from}/{int}", body)

  private def request[D: Schema, R: Schema](url: String, data: D): Future[R] =
    client.url(url).post(data).flatMap {
      case res if res.status == 200 => Future(res.body[R])
      case res                      => Future.failed(Exception(s"$url ${res.status}"))
    }
