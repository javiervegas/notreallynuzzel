package controllers 
import play.api.mvc.{Action, Controller} 
import models.Article 
object Articles extends Controller { 
  def list = Action { implicit request => 
    val articles = Article.findAll  
    Ok(views.html.articles.list(articles)) 
  } 
} 

