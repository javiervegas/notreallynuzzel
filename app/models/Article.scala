package models
import scala.collection.JavaConversions._
import twitter4j.TwitterFactory
import twitter4j.Twitter

case class Article(id:Long, title:String, content:String)

object Article {
  
  val twitter = (new TwitterFactory).getInstance

  def findAll = { 
    twitter.getHomeTimeline.iterator.toList.map { s => Article(s.getId, s.getUser.getName, s.getText) } 
  }
}

