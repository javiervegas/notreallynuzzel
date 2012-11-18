package models
import scala.collection.JavaConversions._
import twitter4j.Paging
import twitter4j.Twitter
import twitter4j.TwitterFactory

case class Article(url:String, author:String, content:String)

object Article {
  
  val twitter = (new TwitterFactory).getInstance

  def findAll = { 
    twitter.getHomeTimeline(new Paging(1,100)).iterator.toList.filterNot {_.getURLEntities.isEmpty }.map { s => Article(s.getURLEntities.head.getDisplayURL, s.getUser.getName, s.getText) } 
  }
}

