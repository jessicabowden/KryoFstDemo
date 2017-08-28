name := "KryoFstDemo"

version := "1.0"

scalaVersion := "2.11.8"

lazy val versions = new {
  val lucene = "6.3.0"
}

libraryDependencies ++= Seq(
  "org.apache.lucene" % "lucene-core" % versions.lucene,
  "org.apache.lucene" % "lucene-misc" % versions.lucene,
  "org.apache.lucene" % "lucene-analyzers-common" % versions.lucene,

  "com.twitter" % "chill_2.11" % "0.9.2",

  "com.amazonaws" % "aws-java-sdk" % "1.7.4",

  "org.apache.commons" % "commons-io" % "1.3.2"
)
