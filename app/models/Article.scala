package models
import org.jsoup._
import scala.collection.JavaConversions._
import scalaj.http.{Http,HttpOptions}
import twitter4j._

case class Article(url:String, tweets:List[Status]) {

  val (responseCode, headersMap, content) = gethtml

  def gethtml = {
    try {
      println(url)
      Http(url).option(HttpOptions.connTimeout(1000)).option(HttpOptions.readTimeout(5000)).asHeadersAndParse(Http.readString)
    } catch {
      case e:Exception => { 
        println(e.toString)
        ("400", Map[String,String](), "")
      }
    }
  }
  def is_html = headersMap.getOrElse("Content-Type",headersMap.getOrElse("Content-type","UNKNOWN")).startsWith("text")
  def title = {
    println("TITLE FOR "+url)
    try {
      val doc = Jsoup.parse(content)
      doc.title
    } catch {
      case e:Exception => "FAILED TITLE"
    }
  }
}
object Article {
  
  val twitter = (new TwitterFactory).getInstance

  def findAll = { 
    val tweets = twitter.getHomeTimeline(new Paging(1,100)).iterator.toList
    tweets.filterNot { _.getURLEntities.isEmpty }.foldLeft(Map[String, List[Status]]() withDefaultValue List[Status]()){
      (m,s) => m + (s.getURLEntities.head.getExpandedURL.toString -> (m(s.getURLEntities.head.getExpandedURL.toString) ++ List(s)) )
    }.map{ case (k,v) => Article(k,v) }.filter {_.is_html}.toList sortBy { a => (-a.tweets.size, a.url) }
  }
}

