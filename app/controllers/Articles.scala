package controllers 
import models.Article
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller} 

object Articles extends Controller { 

  def list = Action { implicit request => 
    val articles = Article.findAll  
    println(articles.size.toString+" articles found")
    //Ok(views.html.articles.list(articles)) 
    Ok(Json.toJson(articles))
  } 

  def details(url: String) = Action { 
    Article.findInCacheByURL(url) match {
      case Some(article:Article) => { 
        try {
          Ok(Json.toJson(article)) 
        } catch {
          case e:Exception => {
            println("detaild for "+url)
            println(article)
            UnsupportedMediaType
          }
        }
      }
      case None => NotFound 
    }
  }
}

