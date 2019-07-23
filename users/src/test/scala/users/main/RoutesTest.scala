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

class RoutesTest {
  @Test def basicFunctionality(): Unit = {
    val config: ApplicationConfig = ApplicationConfig(
      executors = ExecutorsConfig(services = ExecutorsConfig.ServicesConfig(parallellism = 4)),
      services = ServicesConfig(users = ServicesConfig.UsersConfig(failureProbability = 0, timeoutProbability = 0)),
      server = ServerConfig(port = 80))
    val routes = Routes.fromApplicationConfig.run(config)
    val routeFunction = routes.routes.orNotFound
    val requestContent = SignupRequest(UserName("testUser"), EmailAddress("testEmail"), None)
    val request = Request[IO](method = Method.POST, uri = Uri.uri("/signUp") ).withEntity(requestContent.asJson)
    val response = routeFunction.run(request).unsafeRunSync()
    
    require(response.status == Status.Ok)
  }
}