package models
import akka.dispatch.{ Await, ExecutionContext, Future }
import akka.util.duration._
import java.net.URLEncoder
import java.util.concurrent.Executors
import org.jsoup._
import play.api.cache.Cache
import play.api.libs.json.{Json,Writes} 
import play.api.Play.current
import scala.collection.JavaConversions._
import scalaj.http.{Http,HttpOptions}
import twitter4j._
import twitter4j.auth.RequestToken

case class Article(url:String) {

  val pool = Executors.newCachedThreadPool()
  implicit val ec = ExecutionContext.fromExecutorService(pool)
  val content = Future {
    try {
      val (responseCode, headers, content) = Http(url).option(HttpOptions.connTimeout(1000)).option(HttpOptions.readTimeout(5000)).asHeadersAndParse(Http.readString)
      println("downloaded "+url)
      if (responseCode==200 && headers.getOrElse("Content-Type",headers.getOrElse("Content-type","UNKNOWN")).startsWith("text")) {
        val parsed = Jsoup.parse(content)
        val (title, domain) = parsed.title.split("""\|""") match {
          case Array(title,domain) => (title, domain)
          case Array(title) => (title, url.replaceAll(""".+//""","").replaceAll("""/.*""",""))
          case a:Array[String] => (a.head, a.last)
        }
        val summary = parsed.getElementsByTag("p").text match {
          case s:String if s.isEmpty => title
          case s:String if (s.length < 250) => s
          case s:String => s.substring(0,250)
        } 
        Some(title, summary, domain)
      } else {
        None
      }
    } catch {
      case e:Exception => { 
        println(url+" -> "+e.toString)
        None
      }
    }
  }

  lazy val info = Await.result(content, 10 minute).asInstanceOf[Option[(String,String,String)]]
  val (title, summary, domain) = info.get
}

case class ArticleWithTweets(article:Article, tweets:List[Status]) {
  def title = article.title
  def summary = article.title
  def domain = article.title
  def url = article.url
  def json_url = "/articles/"+URLEncoder.encode(url,"UTF-8") //TODO: use routes.Articles/details
}

object Article {
  

  def findAll = { 
    val twitter = (new TwitterFactory).getInstance
    val rt = Cache.get("request_token").asInstanceOf[Option[RequestToken]] match { case Some(r:RequestToken) => r }
    val ov = Cache.get("oauth_verifier").asInstanceOf[Option[String]] match { case Some(r:String) => r }
    val token = twitter.getOAuthAccessToken(rt, ov)
    println("from callback: id:"+twitter.verifyCredentials().getId()+" token:"+token.getToken+" secret:"+token.getTokenSecret)
    val tweets = twitter.getHomeTimeline(new Paging(1, 500)).iterator.toList
    println("got tweets:"+tweets.size)
    val aggregatedAndSorted = tweets.filterNot { _.getURLEntities.isEmpty }.foldLeft(Map[String, List[Status]]() withDefaultValue List[Status]()){
      (m,s) => m + (s.getURLEntities.head.getExpandedURL.toString -> (m(s.getURLEntities.head.getExpandedURL.toString) ++ List(s)) )
    }.toList.sortBy{ case (k,v) => (-v.size, -v.head.getCreatedAt.getTime) }
    val top_articles = aggregatedAndSorted.take(20).map{ case (k,v) => ArticleWithTweets(Article.findByURL(k),v) }
    ArticleWithTweetsCollection(top_articles)
  } 

  def findByURL(url:String) = {
    findInCacheByURL(url) match { 
      case Some(article:Article) => article 
      case None => {
        val article = Article(url)
        Cache.set("article-" + url, article, 60)
        article 
      }
    }
  }

  def findInCacheByURL(url:String) = Cache.get("article-" + url).asInstanceOf[Option[Article]]

  implicit object ArticleWrites extends Writes[Article] { 
    def writes(a: Article) = Json.toJson( 
      Map( 
        "url" -> Json.toJson(a.url),
        "title" -> Json.toJson(a.title),
        "domain" -> Json.toJson(a.domain),
        "summary" -> Json.toJson(a.summary)
      ) 
    ) 
  } 

}

case class ArticleWithTweetsCollection(awtl:List[ArticleWithTweets]) {

  def size = awtl.size

}

object ArticleWithTweetsCollection {

  implicit object ArticleWithTweetsCollectionWrites extends Writes[ArticleWithTweetsCollection] { 
    def writes(awtc:ArticleWithTweetsCollection) = Json.toJson( 
      awtc.awtl.map { awt =>
        val tweets = awt.tweets
        Map( 
          "json_url" -> Json.toJson(awt.json_url),
          "tweets" -> Json.toJson(tweets.map { tweet => Map(
            "tweet" -> Json.toJson(tweet.getText),
            "created_at" ->  Json.toJson(tweet.getCreatedAt.toString),
            "profile_image" ->  Json.toJson(tweet.getUser.getProfileImageURL.toString),
            "user_name" ->  Json.toJson(tweet.getUser.getName),
            //"user_url" ->  Json.toJson(tweet.getUser.getURL.toString),
            "user_screenname" ->  Json.toJson(tweet.getUser.getScreenName)
          ) })
        ) 
      }
    ) 
  } 
}
