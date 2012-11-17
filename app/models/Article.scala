package models
import scala.collection.JavaConversions._
import twitter4j.TwitterFactory
import twitter4j.Twitter
import twitter4j.conf.ConfigurationBuilder

case class Article(id:Long, title:String, content:String)

object Article {
  
  val cb = new ConfigurationBuilder()
  cb.setDebugEnabled(true)
    .setOAuthConsumerKey("1mQSCHfh4duLVSzmLsBQ")
    .setOAuthConsumerSecret("xwkn0bjerCIzY1hmcXlUnpSl6tdTaJkFADanJo1Kwh8")
    .setOAuthAccessToken("14998341-m8nDySA6O8ByHin9JyVJA5cXMuD1JlTJt2XpFJhlI")
    .setOAuthAccessTokenSecret("LhYQQ0IihaajHlRLlV5xqtMjLLUlUXPR4dwMRIA")
  val tf = new TwitterFactory(cb.build())
  val twitter = tf.getInstance()

  def findAll = { 
    var articles = List[Article]()
    val it = twitter.getHomeTimeline.iterator
    while (it.hasNext) {
      val status = it.next
      articles  = Article(1, status.getUser.getName, status.getText) :: articles
    }
    articles
  }
}

