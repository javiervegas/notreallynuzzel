package controllers

import play.api._
import play.api.cache.Cache
import play.api.mvc._
import play.api.Play.current
import twitter4j._
import twitter4j.auth.RequestToken
import twitter4j.conf.ConfigurationBuilder

object Application extends Controller {
  
  def index = show(None)

  def profile(name:String) = show(Some(name))

  def show(name:Option[String]) = Action { request =>
    val uuid = request.session.get("uuid") match {
      case Some(u:String) => u
      case None => java.util.UUID.randomUUID.toString
    }
    play.api.Logger.info(uuid)
    Cache.getAs[Twitter](uuid+"_twitter") match {
      case Some(twitter:Twitter) => Ok(views.html.index(name match {
        case Some(name:String) => "@"+name+"'s"
        case None => "your"
      }, twitter.verifyCredentials.getScreenName, Cache.getAs[Map[String,Twitter]]("users").get.keySet-"test")).withSession( 
        request.session + ("name" -> (name match {
          case Some(name:String) => name
          case None => twitter.verifyCredentials.getScreenName
        }) 
      ))  
      case None => {
        val callback_url = "http://"+request.host+routes.Application.callback()
        play.api.Logger.info("setting callback_url to "+callback_url)
        val request_token = (new TwitterFactory).getInstance.getOAuthRequestToken(callback_url)
        Cache.set(uuid+"_request_token",request_token, 60*60*24)
        Redirect(request_token.getAuthenticationURL).withSession( 
          request.session + ("uuid" -> uuid )
        ) 
      }
    }
  }
  
  def callback = Action { request =>
    val uuid = request.session.get("uuid").get
    val twitter = (new TwitterFactory).getInstance
    val rt = Cache.getAs[RequestToken](uuid+"_request_token").get
    val ov = request.queryString.get("oauth_verifier").get.mkString
    val token = twitter.getOAuthAccessToken(rt, ov)
    play.api.Logger.info("in callback: screename:"+twitter.verifyCredentials.getScreenName+" token:"+token.getToken+" secret:"+token.getTokenSecret)
    Cache.set(uuid+"_twitter", twitter) 
    Cache.set("users", (Cache.getAs[Map[String,Twitter]]("users") match { 
      case Some(m:Map[String,Twitter]) => m
      case None => {
        val cb = new ConfigurationBuilder()
          .setOAuthAccessToken("6204-EnlKWhDit3RT3RCvvnDqBsO62VeRju7gBbiHhenvo")
          .setOAuthAccessTokenSecret("LfGl7RpmiwmjBwOw3yuPe94FA5yTHLnvqgUEpgrUQY")
        val tf = new TwitterFactory(cb.build())
        Map[String,Twitter]("test" -> tf.getInstance() )
      }
    }) + (twitter.verifyCredentials.getScreenName -> twitter ) )
    Redirect(routes.Application.index())
  }
  
}
