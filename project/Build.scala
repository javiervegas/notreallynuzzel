import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "splay"
    val appVersion      = "1.0"

    val appDependencies = Seq(
      // Add your project dependencies here,
      "org.twitter4j" % "twitter4j-core" % "[2.2,)",
      "org.scalaj" %% "scalaj-http" % "0.3.2"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      // Add your own project settings here      
    )

}
