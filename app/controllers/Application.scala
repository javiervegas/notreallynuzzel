package controllers

import play.api._
import play.api.cache.Cache
import play.api.mvc._
import play.api.Play.current
import twitter4j._
import twitter4j.auth.RequestToken

object Application extends Controller {
  
  def index = show(None)
  def profile(name:String) = show(Some(name))
  def show(name:Option[String]) = Action { request =>
    val uuid = request.session.get("uuid") match {
      case Some(u:String) => u
      case None => java.util.UUID.randomUUID.toString
    }
    println(uuid)
    Cache.get(uuid+"_twitter").asInstanceOf[Option[Twitter]] match {
      case Some(twitter:Twitter) => Ok(views.html.index(name match {
        case None => "your"
        case Some(name:String) => "@"+name+"'s"
      }, twitter.verifyCredentials.getScreenName)) 
      case None => {
        val callback_url = "http://"+request.host+routes.Application.callback()
        println("setting callback_url to "+callback_url)
        val request_token = (new TwitterFactory).getInstance.getOAuthRequestToken(callback_url)
        Cache.set(uuid+"_request_token",request_token, 600)
        Redirect(request_token.getAuthenticationURL).withSession( 
          request.session + ("uuid" -> uuid )
        ) 
      }
    }
  }
  
  def callback = Action { request =>
    val uuid = request.session.get("uuid").get
    val twitter = (new TwitterFactory).getInstance
    val rt = Cache.get(uuid+"_request_token").asInstanceOf[Option[RequestToken]] match { case Some(r:RequestToken) => r }
    val ov = request.queryString.get("oauth_verifier").get.mkString
    val token = twitter.getOAuthAccessToken(rt, ov)
    println("in callback: screename:"+twitter.verifyCredentials.getScreenName+" token:"+token.getToken+" secret:"+token.getTokenSecret)
    Cache.set(uuid+"_twitter", twitter, 600) 
    Redirect(routes.Application.index())
  }
  
}
