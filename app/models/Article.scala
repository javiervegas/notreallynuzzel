package models
import scala.collection.JavaConversions._
import twitter4j.Paging
import twitter4j.Twitter
import twitter4j.TwitterFactory

case class Article(url:String, tweets:List[Tweet])
case class Tweet(author:String, content:String)

object Article {
  
  val twitter = (new TwitterFactory).getInstance

  def findAll = { 
    val tweets = twitter.getHomeTimeline(new Paging(1,100)).iterator.toList.filterNot { _.getURLEntities.isEmpty }
    tweets.foldLeft(Map[String, List[Tweet]]() withDefaultValue List[Tweet]()){
      (m,s) => m + (s.getURLEntities.head.getDisplayURL -> (m(s.getURLEntities.head.getDisplayURL) ++ List(Tweet(s.getUser.getName, s.getText)) ) )
    }.map{ case (k,v) => Article(k,v) }.toList sortBy { a => (-a.tweets.size, a.url) }
  }
}

