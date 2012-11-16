package models

case class Article(id:Long, title:String, content:String)

object Article {
  
  var articles = (1L to 4L).map { i => Article(i, "tit", "cont1") }

  def findAll = this.articles.toList.sortBy(_.id) 

}

