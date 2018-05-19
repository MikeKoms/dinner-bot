package places.foursquare

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, ResponseEntity}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.Materializer

import scala.concurrent.{ExecutionContext, Future}

trait HttpClient {
  def request[T](req: HttpRequest)
                (implicit um: Unmarshaller[ResponseEntity, T]
                 , ac: ActorSystem
                 , mat: Materializer
                 , executionContext: ExecutionContext): Future[T]
}

class JsonHttpClient extends HttpClient {
    override def request[T](req: HttpRequest)
                           (implicit um: Unmarshaller[ResponseEntity, T]
                           , ac: ActorSystem
                           , mat: Materializer
                           , ec: ExecutionContext): Future[T] = {
      val resp = Http().singleRequest(req)
      resp.flatMap(resp => Unmarshal(resp.entity).to[T])
    }
}


