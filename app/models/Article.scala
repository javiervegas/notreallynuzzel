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
          case s:String if (s.length < 230) => s
          case s:String => s.substring(0,280)
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
}

case class ArticleWithTweets(article:Article, tweets:List[Status])

object Article {
  
  val twitter = (new TwitterFactory).getInstance

  def findAll = { 
    val tweets = twitter.getHomeTimeline(new Paging(1,40)).iterator.toList
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

