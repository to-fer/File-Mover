package main

import java.io.FileReader
import java.nio.file.{Files, Paths}

import _root_.path.FileMover
import com.twitter.logging.Logger
import com.twitter.logging.config.{FileHandlerConfig, LoggerConfig}
import config.ConfigFileParser._
import watch.Watcher._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, future}

object Main extends App {

  val osName = sys props "os.name"
  val homeDir = sys env "HOME"
  val configFilePath =
    if (osName.contains("Linux"))
      Paths.get(homeDir, ".config", "file_mover", "move")
    else if (osName.contains("Windows"))
      Paths.get(homeDir, ".file_mover", "move.txt")
    else
      Paths.get("move.txt")
  if (!Files.exists(configFilePath)) {
    Files.createDirectories(configFilePath.getParent)
    Files.createFile(configFilePath)
  }

  // TODO detect when move rule conflict.
  val configFileReader = new FileReader(configFilePath.toFile)
  val watchList = parseAll(file, configFileReader).get
  configFileReader.close()

  val logFile = configFilePath.getParent resolve "mover_log.log"
  val logger = Logger.get(getClass)
  val config = new LoggerConfig {
    handlers = new FileHandlerConfig {
      filename = logFile.toString
    }
  }
  config()

  if (watchList.isEmpty)
    logger.warning(s"There are no move rules specified in $configFilePath.")

  val watchFutures = watchList map {
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
  }

  watchFutures foreach (Await.ready(_, Duration.Inf))
}