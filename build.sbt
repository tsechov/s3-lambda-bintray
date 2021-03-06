
import com.typesafe.sbt.GitPlugin.autoImport._
import sbt.Keys._
import sbtassembly.AssemblyPlugin.autoImport._
import sbtbuildinfo.BuildInfoPlugin.autoImport._


val requiredJavaVersion: String = "1.8"
initialize := {
  val required = requiredJavaVersion
  val current = sys.props("java.specification.version")
  assert(current == required, s"Unsupported JDK: java.specification.version $current != $required")

  import java.util.Properties

  import scala.collection.JavaConverters._


  val prop = new Properties()
  IO.load(prop, new File("env.properties"))
  prop.entrySet().asScala.foreach {
    (entry) => {
      println(s"loading envprop ${entry.getKey}")
      sys.props += ((entry.getKey.asInstanceOf[String], entry.getValue.asInstanceOf[String]))
    }
  }

}

resolvers += Resolver.bintrayRepo("jfrog", "bintray-tools")


scalacOptions += "-target:jvm-" + requiredJavaVersion
javacOptions ++= Seq("-source", requiredJavaVersion, "-target", requiredJavaVersion, "-Xlint")


val bintrayClientVersion: String = "0.8.3"
val awsSdkVersion: String = "1.1.0"

val mavenVersion: String = "3.3.9"
val VersionRegex = "v([0-9]+.[0-9]+.[0-9]+)-?(.*)?".r
lazy val root = (project in file(".")).
  enablePlugins(BuildInfoPlugin, GitVersioning, GitBranchPrompt).
  settings(
    name := "s3-bintray-deploy",
    scalaVersion := "2.11.7",
    retrieveManaged := true,
    libraryDependencies ++= Seq(
      "com.amazonaws" % "aws-lambda-java-core" % awsSdkVersion,
      "com.amazonaws" % "aws-lambda-java-events" % awsSdkVersion,
      "com.github.seratch" %% "awscala" % "0.5.+",
      "org.slf4j" % "log4j-over-slf4j" % "1.7.11",
      "com.typesafe.scala-logging" % "scala-logging_2.11" % "3.1.0",
      "ch.qos.logback" % "logback-classic" % "1.1.2",
      "org.rauschig" % "jarchivelib" % "0.7.1",
      "com.jfrog.bintray.client" % "bintray-client-java-api" % bintrayClientVersion,
      "com.jfrog.bintray.client" % "bintray-client-java-service" % bintrayClientVersion,
      "io.reactivex" % "rxjava" % "1.1.3",
      "org.apache.maven" % "maven-model" % mavenVersion,
      "org.apache.maven" % "maven-core" % mavenVersion,
      "org.apache.maven" % "maven-artifact" % mavenVersion,
      "com.google.jimfs" % "jimfs" % "1.1",
      "org.scalatest" % "scalatest_2.11" % "2.2.6" % "test"

    ).map(_.exclude("commons-logging", "commons-logging")),


    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "hu.blackbelt.cd.bintray.deploy",

    assemblyMergeStrategy in assembly := {

      case PathList("META-INF", xs@_*) =>
        (xs map {_.toLowerCase}) match {
          case "services" :: xs => MergeStrategy.filterDistinctLines
          case _ => MergeStrategy.discard
        }
      case x => MergeStrategy.first
    },

    artifact in(Compile, assembly) := {
      val art = (artifact in(Compile, assembly)).value
      art.copy(`classifier` = Some("assembly"))
    },
    addArtifact(artifact in(Compile, assembly), assembly),


    git.useGitDescribe := true,
    git.baseVersion := "0.0.0",

    publishTo := Some("Blackbelt lambdas" atS3 "s3://bb-lambdas"),
    //    publishTo := Some(Resolver.file("file", new File(target.value.absolutePath + "/publish"))),
    publishConfiguration ~= { config =>
      val newArts = config.artifacts.filterKeys(_.`classifier` == Some("assembly"))
      new PublishConfiguration(config.ivyFile, config.resolverName, newArts, config.checksums, config.logging)
    }

  )



com.updateimpact.Plugin.openBrowser in ThisBuild := true







