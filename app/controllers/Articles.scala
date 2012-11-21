package controllers 
import models.{Article,ArticleWithTweetsCollection} 
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
    Article.findInCacheByURL(url).map { article =>  Ok(Json.toJson(article)) }.getOrElse(NotFound) 
  }
}

