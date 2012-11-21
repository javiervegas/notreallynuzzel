package controllers

import play.api._
import play.api.cache.Cache
import play.api.mvc._
import play.api.Play.current
import twitter4j._
import twitter4j.auth.RequestToken

object Application extends Controller {
  
  def index = Action { request =>
    Cache.get("request_token").asInstanceOf[Option[RequestToken]] match {
      case Some(rt:RequestToken) => Ok(views.html.index()) 
      case None => {
        val request_token = (new TwitterFactory).getInstance.getOAuthRequestToken("http://"+request.host+routes.Application.callback())
        Cache.set("request_token",request_token, 60)
        Redirect(request_token.getAuthenticationURL)
      }
    }
  }
  
  def callback = Action { request =>
    Cache.set("oauth_verifier", request.queryString.get("oauth_verifier").get.mkString) 
    Redirect(routes.Application.index())
  }
  
}
