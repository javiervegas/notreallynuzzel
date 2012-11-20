package models
import akka.dispatch.{ Await, ExecutionContext, Future }
import akka.util.duration._
import java.util.concurrent.Executors
import org.jsoup._
import play.api.cache.Cache
import play.api.Play.current
import scala.collection.JavaConversions._
import scalaj.http.{Http,HttpOptions}
import twitter4j._

case class Article(url:String) {

  val pool = Executors.newCachedThreadPool()
  implicit val ec = ExecutionContext.fromExecutorService(pool)
  val content = Future {
    try {
      println("fut: before downloading "+url)
      val (responseCode, headers, content) = Http(url).option(HttpOptions.connTimeout(1000)).option(HttpOptions.readTimeout(5000)).asHeadersAndParse(Http.readString)
      println("fut: after downloading "+url)
      if (responseCode==200 && headers.getOrElse("Content-Type",headers.getOrElse("Content-type","UNKNOWN")).startsWith("text")) {
        val parsed = Jsoup.parse(content)
        val h1 = parsed.getElementsByTag("h1").text
        val title = if (h1.isEmpty) {
          parsed.title
        } else {
          h1
        } 
        val summary = try {
          parsed.getElementsByTag("p").text.substring(0,280)
        } catch {
          case e:Exception => e.toString
        } 
        Some(title, summary)
      } else {
        None
      }
    } catch {
      case e:Exception => { 
        println(e.toString)
        None
      }
    }
  }

  lazy val info = Await.result(content, 10 minute).asInstanceOf[Option[(String,String)]]
  val domain = url.replaceAll(""".+//""","").replaceAll("""/.*""","")
}

case class ArticleWithTweets(article:Article, tweets:List[Status])

object Article {
  
  val twitter = (new TwitterFactory).getInstance

  def findAll = { 
    val tweets = twitter.getHomeTimeline(new Paging(1,100)).iterator.toList
    tweets.filterNot { _.getURLEntities.isEmpty }.foldLeft(Map[String, List[Status]]() withDefaultValue List[Status]()){
      (m,s) => m + (s.getURLEntities.head.getExpandedURL.toString -> (m(s.getURLEntities.head.getExpandedURL.toString) ++ List(s)) )
    }.map{ case (k,v) => ArticleWithTweets(Article.find(k),v) }.filter { _.article.info.isDefined }.toList sortBy { a => (-a.tweets.size, -a.tweets.head.getCreatedAt.getTime) }
  } 
  def find(url:String) = {
    Cache.get("article-" + url) match { 
      case Some(article:Article) => article 
      case None => {
        val article = Article(url)
        Cache.set("article-" + url, article)
        article 
      }
    }
  }

}

