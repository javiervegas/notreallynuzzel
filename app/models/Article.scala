package models
import akka.dispatch.{ Await, ExecutionContext, Future }
import akka.util.duration._
import java.util.concurrent.Executors
import org.jsoup._
import scala.collection.JavaConversions._
import scalaj.http.{Http,HttpOptions}
import twitter4j._

case class Article(url:String, tweets:List[Status]) {

  val pool = Executors.newCachedThreadPool()
  implicit val ec = ExecutionContext.fromExecutorService(pool)
  val content = Future {
    try {
      println("fut: before downloading "+url)
      val (responseCode, headers, content) = Http(url).option(HttpOptions.connTimeout(1000)).option(HttpOptions.readTimeout(5000)).asHeadersAndParse(Http.readString)
      println("fut: after downloading "+url)
      if (headers.getOrElse("Content-Type",headers.getOrElse("Content-type","UNKNOWN")).startsWith("text")) {
        val parsed = Jsoup.parse(content)
        val h1 = parsed.getElementsByTag("h1").text
        if (h1.isEmpty) {
          Some(parsed.title)
        } else {
          Some(h1)
        }
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

  lazy val title = Await.result(content, 10 minute).asInstanceOf[Option[String]]
}
object Article {
  
  val twitter = (new TwitterFactory).getInstance

  def findAll = { 
    val tweets = twitter.getHomeTimeline(new Paging(1,100)).iterator.toList
    tweets.filterNot { _.getURLEntities.isEmpty }.foldLeft(Map[String, List[Status]]() withDefaultValue List[Status]()){
      (m,s) => m + (s.getURLEntities.head.getExpandedURL.toString -> (m(s.getURLEntities.head.getExpandedURL.toString) ++ List(s)) )
    }.map{ case (k,v) => Article(k,v) }.filter { _.title.isDefined }.toList sortBy { a => (-a.tweets.size, a.url) }
  }
}

