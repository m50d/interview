package users.main

import cats.data._
import org.http4s.RhoDsl._
import users.config._
import org.http4s.rho.RhoRoutes
import cats.Monad
import cats.effect.IO
import org.http4s.rho.swagger.syntax.io
import org.http4s.EntityEncoder
import users.domain.User
import org.http4s.Charset
import org.http4s.DefaultCharset
import org.http4s.MediaType
import org.http4s.headers.`Content-Type`
import fs2.Chunk
import org.http4s.rho.bits.StringParser
import org.http4s.rho.bits.ResultResponse
import org.http4s.rho.bits.SuccessResponse
import org.http4s.circe._
import _root_.io.circe.generic.auto._
import _root_.io.circe.syntax._
import scala.reflect.runtime.universe.TypeTag
import scala.concurrent.Future
import users.services.usermanagement.Error
import org.http4s.EntityDecoder
import _root_.io.circe.Json
import users.domain.UserName
import users.domain.EmailAddress
import users.domain.Password
import org.http4s.rho.bits.ResponseGeneratorInstances
import _root_.io.circe.Encoder

object Routes {
  val reader: Reader[Services, Routes] =
    Reader(Routes.apply)

  val fromApplicationConfig: Reader[ApplicationConfig, Routes] =
    Services.fromApplicationConfig andThen reader
}

case class SignupRequest(
  userName: UserName,
  emailAddress: EmailAddress,
  password: Option[Password])

trait UserResponseGenerator extends ResponseGeneratorInstances[IO] {
  def toResponse[A: Encoder](er: Either[Error, A]) = er match {
    case Left(Error.Exists) => Conflict("Exists")
    case Left(Error.NotFound) => NotFound("Not found")
    case Left(Error.Active) => Locked("Active")
    case Left(Error.Deleted) => Gone("Deleted")
    case Left(Error.Blocked) => Forbidden("Blocked")
    case Left(Error.System(t)) => InternalServerError(t.getStackTrace.mkString("\n"))
    case Right(a) => Ok(a.asJson)
  }
}

case class Routes(services: Services) {
  implicit def idEncoder[F[_]](implicit charset: Charset = DefaultCharset): EntityEncoder[F, User.Id] = {
    val hdr = `Content-Type`(MediaType.text.plain).withCharset(charset)
    EntityEncoder.simple(hdr)(id => Chunk.bytes(id.value.getBytes(charset.nioCharset)))
  }
  implicit def idParser[F[_]]: StringParser[F, User.Id] = new StringParser[F, User.Id] {
    override def parse(s: String)(implicit F: Monad[F]) = SuccessResponse(User.Id(s))
    override def typeTag = Some(implicitly[TypeTag[User.Id]])
  }

  def f[A](fa: => Future[A]): IO[A] = IO.fromFuture(IO(fa))

  final val rhoRoutes = new RhoRoutes[IO] with UserResponseGenerator {
    GET / "generateId" |>>
      { f(services.userManagement.generateId()) map { Ok(_) } }
    POST / "signUp" ^ jsonOf[IO, SignupRequest] |>> {
      body: SignupRequest =>
        f(services.userManagement.signUp(body.userName, body.emailAddress, body.password)) map { toResponse(_) }
    }
    GET / "users" / pathVar[User.Id] |>> { id: User.Id =>
      f(services.userManagement.get(id)) map { toResponse(_) }
    }
    POST / "users" / pathVar[User.Id] / "updateEmail" ^ EntityDecoder.text[IO] |>> { (id: User.Id, email: String) => 
      f(services.userManagement.updateEmail(id, EmailAddress(email)))  map { toResponse(_) }
    }
    GET |>> Ok("Hello world")
  }
  final val middleware = io.createRhoMiddleware()
  final val routes = rhoRoutes.toRoutes(middleware)
}
