package controllers

import javax.inject._

import play.api.mvc._

import scala.io.Source
import scala.util.control.NonFatal

@Singleton
class HomeController @Inject()(cc: ControllerComponents, env: play.api.Environment)
  extends AbstractController(cc) {

  def index: Action[AnyContent] = Action {
    val file = env.getFile("public/index.html")
    if (!file.exists()) {
      NotFound("public/index.html not found")
    } else {
      val source = Source.fromFile(file)(scala.io.Codec.UTF8)
      try {
        Ok(source.mkString).as("text/html")
      } catch {
        case NonFatal(e) => InternalServerError(e.getMessage)
      } finally {
        source.close()
      }
    }
  }
}
