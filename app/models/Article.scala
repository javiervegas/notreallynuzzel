package models
import scala.collection.JavaConversions._
import twitter4j._

case class Article(url:String, tweets:List[Status])

object Article {
  
  val twitter = (new TwitterFactory).getInstance

  def findAll = { 
    val tweets = twitter.getHomeTimeline(new Paging(1,100)).iterator.toList.filterNot { _.getURLEntities.isEmpty }
    tweets.foldLeft(Map[String, List[Status]]() withDefaultValue List[Status]()){
      (m,s) => m + (s.getURLEntities.head.getDisplayURL -> (m(s.getURLEntities.head.getDisplayURL) ++ List(s)) )
    }.map{ case (k,v) => Article(k,v) }.toList sortBy { a => (-a.tweets.size, a.url) }
  }
}

