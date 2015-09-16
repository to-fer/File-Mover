package main

import java.io.{FileNotFoundException, FileReader}
import java.nio.file.{Path, Files, Paths}

import _root_.path.FileMover
import com.twitter.logging.Logger
import com.twitter.logging.config.{FileHandlerConfig, LoggerConfig}
import config.ConfigFileParser._
import config.error.NoMoveRulesException
import watch.Watcher._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, future}
import scala.util.{Try, Success, Failure}

object Main extends App {
  def getConfigDirectoryPath(): Try[Path] = Try({
    val home = sys.env("HOME")
    val osName = sys.props("os.name")
    if (osName.contains("Windows"))
      Paths.get(home, ".file_mover")
    else
      Paths.get(home, ".config", "file_mover")
  })

  def createFileIfNotExists(p: Path): Unit = {
    if (!Files.exists(p)) {
      Files.createDirectories(p.getParent)
      Files.createFile(p)
    }
  }

  val configDirectory = getConfigDirectoryPath()
  val logFile = configDirectory.flatMap(p => Try(p.resolve("mover_log.log")))
  logFile.foreach(createFileIfNotExists)
  val logger = Logger.get(getClass)
  val config = logFile.flatMap(log => Try(new LoggerConfig {
    handlers = new FileHandlerConfig {
      filename = log.toString
    }
  }))
  config.foreach(_())

  val configFile = configDirectory.flatMap(p => Try(p.resolve("move.txt")))
  configFile.foreach(createFileIfNotExists)

  // TODO detect when move rules conflict.
  val watchList = configFile.flatMap(p => Try({
    val configFileReader = new FileReader(p.toFile)
    val watches = parseAll(file, configFileReader).get
    configFileReader.close()
    if (watches.isEmpty)
      throw new NoMoveRulesException(s"$p contains no move rules!")
    watches
  }))

  val watchFutures = watchList.map(_.map {
    case (watchPath, moveList) => {
      val fileMovers = moveList map {
        case (moveParams, movePath) =>
          new FileMover(logger, moveParams, movePath)
      }

      logger.info(s"Performing initial move of files in $watchPath.")
      // Perform initial move on startup.
      val dirStream = Files newDirectoryStream watchPath
      val dirIt = dirStream.iterator
      while(dirIt.hasNext) {
        val nextDir = dirIt.next
        fileMovers.foreach(_.onEvent(nextDir))
      }

      dirStream.close()

      logger.info(s"Watching $watchPath")
      future {
        watch(watchPath) {
          case Created(eventPath) => {
            fileMovers.foreach(_.onEvent(eventPath))
          }
        }
      }
    }
  })

  watchFutures match {
    case Success(futures) => futures.foreach(Await.ready(_, Duration.Inf))
    case Failure(exception) => exception match {
      case ex: NoMoveRulesException => logger.error("Supplied move file contains no move rules.")
      case ex: NoSuchElementException => logger.error("HOME environment variable does not exist; you must define it.")
      case ex: FileNotFoundException => logger.error("Move file not found.")
      case ex => {
        logger.error("Unknown failure!")
        ex.printStackTrace()
      }
    }
  }
}