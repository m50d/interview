package users.main

import cats.data._
import org.http4s.RhoDsl._
import users.config._
import org.http4s.rho.RhoRoutes
import cats.Monad


object Application {
  val reader: Reader[Services, Application] =
    Reader(Application.apply)

  val fromApplicationConfig: Reader[ApplicationConfig, Application] =
    Services.fromApplicationConfig andThen reader
}

case class Application(
    services: Services
) {
  final def restRoutes[F[_]: Monad] = new RhoRoutes[F] {
    GET / "somePath" / pathVar[Int]("someInt", "parameter description") +? paramD[String]("name", "parameter description") |>> {
      (someInt: Int, name: String) => Ok("result")
    }
} 
}
