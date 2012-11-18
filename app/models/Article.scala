package models
import scala.collection.JavaConversions._
import scalaj.http.{Http,HttpOptions}
import twitter4j._

case class Article(url:String, tweets:List[Status]) {

  val html = gethtml
  def gethtml = {
    try {
      println(url)
      Http(url).option(HttpOptions.connTimeout(1000)).option(HttpOptions.readTimeout(5000)).responseCode
    } catch {
      case e:Exception => e.toString
    }
  }
}
object Article {
  
  val twitter = (new TwitterFactory).getInstance

  def findAll = { 
    val tweets = twitter.getHomeTimeline(new Paging(1,100)).iterator.toList
    tweets.filterNot { _.getURLEntities.isEmpty }.foldLeft(Map[String, List[Status]]() withDefaultValue List[Status]()){
      (m,s) => m + (s.getURLEntities.head.getExpandedURL.toString -> (m(s.getURLEntities.head.getExpandedURL.toString) ++ List(s)) )
    }.map{ case (k,v) => Article(k,v) }.toList sortBy { a => (-a.tweets.size, a.url) }
  }
}

