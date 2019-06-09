package controllers

import engines.UriEngine
import javax.inject._
import play.api.Configuration
import play.api.mvc._

@Singleton
class HomeController @Inject()(config: Configuration) extends Controller {

  val uriEngine = new UriEngine(config)

  def index() = Action { implicit request: Request[AnyContent] =>
    val maybeUriObj = uriEngine.getRequestUri[AnyContent](request)
    if (maybeUriObj.isDefined) {
      val newUri = uriEngine.removeRefFromUri(maybeUriObj.get)
      Redirect(newUri)
    } else {
      val summary = uriEngine.getSummary()
      Ok(views.html.index(summary))
    }
  }

  def displayNoRefUri() = Action { implicit request: Request[AnyContent] =>
    val formData = request.body.asFormUrlEncoded
    val requestUri = formData.get("requestLink").headOption
    val responseUri = requestUri.map(uriEngine.removeRefFromUri(_))
    val summary = uriEngine.getSummary().map(s => s.copy(totalCalls = s.totalCalls + 1))
    Ok(views.html.index(summary, responseUri))
  }

}
