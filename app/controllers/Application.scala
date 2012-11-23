package controllers

import play.api._
import play.api.cache.Cache
import play.api.mvc._
import play.api.Play.current
import twitter4j._
import twitter4j.auth.RequestToken

object Application extends Controller {
  
  def index = Action { request =>
    Cache.get("twitter").asInstanceOf[Option[Twitter]] match {
      case Some(twitter:Twitter) => Ok(views.html.index(twitter.verifyCredentials.getScreenName)) 
      case None => {
        val callback_url = "http://"+request.host+routes.Application.callback()
        println("setting callback_url to "+callback_url)
        val request_token = (new TwitterFactory).getInstance.getOAuthRequestToken(callback_url)
        Cache.set("request_token",request_token, 60)
        Redirect(request_token.getAuthenticationURL)
      }
    }
  }
  
  def callback = Action { request =>
    val twitter = (new TwitterFactory).getInstance
    val rt = Cache.get("request_token").asInstanceOf[Option[RequestToken]] match { case Some(r:RequestToken) => r }
    val ov = request.queryString.get("oauth_verifier").get.mkString
    val token = twitter.getOAuthAccessToken(rt, ov)
    println("in callback: screename:"+twitter.verifyCredentials.getScreenName+" token:"+token.getToken+" secret:"+token.getTokenSecret)
    Cache.set("twitter", twitter, 60) 
    Redirect(routes.Application.index())
  }
  
}
