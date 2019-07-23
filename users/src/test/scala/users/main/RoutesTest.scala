package users.main

import org.junit.Test
import users.config.ApplicationConfig
import users.config.ExecutorsConfig
import users.config.ServicesConfig
import users.config.ServerConfig
import org.http4s.syntax.all._
import org.http4s.Request
import org.http4s.Method
import org.http4s.Uri
import users.domain.EmailAddress
import users.domain.UserName
import org.http4s.circe._
import _root_.io.circe.generic.auto._
import _root_.io.circe.syntax._
import cats.effect.IO
import org.http4s.Status
import users.domain.User

class RoutesTest {
  @Test def basicFunctionality(): Unit = {
    implicit val userDecoder = jsonOf[IO, User]
    val config: ApplicationConfig = ApplicationConfig(
      executors = ExecutorsConfig(services = ExecutorsConfig.ServicesConfig(parallellism = 4)),
      services = ServicesConfig(users = ServicesConfig.UsersConfig(failureProbability = 0, timeoutProbability = 0)),
      server = ServerConfig(port = 80))
    val routes = Routes.fromApplicationConfig.run(config)
    val routeFunction = routes.routes.orNotFound
    
    val getUnknownUser = Request[IO](method = Method.GET, uri = Uri.uri("/users/bogus") )
    val unknownUserResponse = routeFunction.run(getUnknownUser).unsafeRunSync()
    require(unknownUserResponse.status == Status.NotFound)
    
    val signupRequestContent = SignupRequest(UserName("testUser"), EmailAddress("testEmail"), None)
    println(signupRequestContent.asJson)
    val signupRequest = Request[IO](method = Method.POST, uri = Uri.uri("/signUp") ).withEntity(signupRequestContent.asJson)
    val signupResponse = routeFunction.run(signupRequest).unsafeRunSync()
    require(signupResponse.status == Status.Ok)
    
    val id = signupResponse.as[User].unsafeRunSync().id.value
    val getUser = Request[IO](method = Method.GET, uri = Uri.fromString(s"/users/$id").right.get)
    val getUserResponse = routeFunction.run(getUser).unsafeRunSync()
    require(getUserResponse.status == Status.Ok, getUserResponse.as[String].unsafeRunSync())
  }
}