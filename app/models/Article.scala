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
      val (responseCode, headers, content) = Http(url).option(HttpOptions.connTimeout(3000)).option(HttpOptions.readTimeout(15000)).asHeadersAndParse(Http.readString)
      println("downloaded "+url)
      if (responseCode==200 && headers.getOrElse("Content-Type",headers.getOrElse("Content-type","UNKNOWN")).startsWith("text")) {
        val parsed = Jsoup.parse(content)
        val (title, domain) = parsed.title.split("""\|""") match {
          case Array(title,domain) => (title, domain)
          case Array(title) => (title, Article.getDomain(url))
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
  def title= info.get._1
  def summary = info.get._2
  def domain = info.get._3
}

object Article {
  

  def findAll = { 
    val twitter = (new TwitterFactory).getInstance
    val rt = Cache.get("request_token").asInstanceOf[Option[RequestToken]] match { case Some(r:RequestToken) => r }
    val ov = Cache.get("oauth_verifier").asInstanceOf[Option[String]] match { case Some(r:String) => r }
    val token = twitter.getOAuthAccessToken(rt, ov)
    println("from callback: id:"+twitter.verifyCredentials.getId+" token:"+token.getToken+" secret:"+token.getTokenSecret)
    val tweets = twitter.getHomeTimeline(new Paging(1, 500)).iterator.toList
    println("got tweets:"+tweets.size)
    val aggregatedAndSorted = tweets.filterNot { _.getURLEntities.isEmpty }.foldLeft(Map[String, List[Status]]() withDefaultValue List[Status]()){
      (m,s) => m + (s.getURLEntities.head.getExpandedURL.toString -> (m(s.getURLEntities.head.getExpandedURL.toString) ++ List(s)) )
    }.toList.sortBy{ case (k,v) => (-v.size, -v.head.getCreatedAt.getTime) }
    val top_articles = aggregatedAndSorted.filterNot{ case (url, tw) => not_show.contains(getDomain(url)) }//.take(20)
    top_articles.map { case (url,tw) => Article.findByURL(url) }
    URLWithTweetsCollection(top_articles)
  } 

  val not_show = List("www.flickr.com","youtu.be","instagram.com","www.youtube.com","twitpic.com","4sq.com")

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
  
  def getDomain(url:String) = url.replaceAll(""".+//""","").replaceAll("""/.*""","")

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

case class URLWithTweetsCollection(uwtl:List[(String, List[twitter4j.Status])]) {

  def size = uwtl.size

}

object URLWithTweetsCollection {

  def json_url(url:String) = "/articles/"+URLEncoder.encode(url,"UTF-8") //TODO: use routes.Articles/details

  implicit object URLWithTweetsCollectionWrites extends Writes[URLWithTweetsCollection] { 
    def writes(uwtc:URLWithTweetsCollection) = Json.toJson( 
      uwtc.uwtl.map { case(url, tweets) =>
        Map( 
          "json_url" -> Json.toJson(json_url(url)),
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
