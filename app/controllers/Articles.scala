package controllers 
import models.Article
import play.api.cache.Cache
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller} 
import play.api.Play.current
import twitter4j._

object Articles extends Controller { 

  def list = Action { implicit request => 
    //val uuid = request.session.get("uuid").get
    //val twitter = Cache.getAs[Twitter](uuid+"_twitter").get
    val twitter = Cache.getAs[Map[String,Twitter]]("users").get.get(request.session.get("name").get) match {
      case Some(t:Twitter) => t
      case None => Cache.getAs[Twitter](request.session.get("uuid").get+"_twitter").get
    }
    val articles = Article.findAll(twitter)
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

