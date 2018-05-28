package ctx

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

object ImplicitContext {
  implicit val system = ActorSystem()
  implicit val dispatcher = system.dispatcher
  implicit val materializer = ActorMaterializer()
}
