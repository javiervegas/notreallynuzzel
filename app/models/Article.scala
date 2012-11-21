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
  def title = info.get._1
  def summary = info.get._2
  def domain = info.get._3
}

case class ArticleWithTweets(article:Article, tweets:List[Status]) {
  def title = article.title
  def summary = article.title
  def domain = article.title
  def url = article.url
  def json_url = "/articles/"+URLEncoder.encode(url,"UTF-8") //TODO: use routes.Articles/details
}

object Article {
  
  val twitter = (new TwitterFactory).getInstance

  def findAll = { 
    val tweets = twitter.getHomeTimeline(new Paging(1,40)).iterator.toList
    val aggregatedAndSorted = tweets.filterNot { _.getURLEntities.isEmpty }.foldLeft(Map[String, List[Status]]() withDefaultValue List[Status]()){
      (m,s) => m + (s.getURLEntities.head.getExpandedURL.toString -> (m(s.getURLEntities.head.getExpandedURL.toString) ++ List(s)) )
    }.map{ case (k,v) => ArticleWithTweets(Article.findByURL(k),v) }.toList sortBy { a => (-a.tweets.size, -a.tweets.head.getCreatedAt.getTime) }
    ArticleWithTweetsCollection(aggregatedAndSorted)
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
        val tweet = awt.tweets.head
        val user = tweet.getUser
        Map( 
          "json_url" -> Json.toJson(awt.json_url),
          "tweet" -> Json.toJson(tweet.getText),
          "created_at" ->  Json.toJson(tweet.getCreatedAt.toString),
          "profile_image" ->  Json.toJson(user.getProfileImageURL.toString),
          "user_name" ->  Json.toJson(user.getName),
          //"user_url" ->  Json.toJson(user.getURL.toString),
          "user_screenname" ->  Json.toJson(user.getScreenName)
        ) 
      }
    ) 
  } 
}
