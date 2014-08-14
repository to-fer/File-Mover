package main

import java.io.FileReader
import java.nio.file.{Files, Path, Paths}

import com.twitter.logging.Logger
import com.twitter.logging.config.{FileHandlerConfig, LoggerConfig}
import config.ConfigFileParser._
import _root_.path.FileMover
import watch.Watcher._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, future}

object Main extends App {

  val osName = sys props "os.name"
  val homeDir = sys env "HOME"
  val configFilePath =
    if (osName.contains("Linux"))
      Paths.get(homeDir, ".config", "file_mover", "path")
    else if (osName.contains("Windows"))
      Paths.get(homeDir, ".file_mover", "move.txt")
    else
      Paths.get("move.txt")
  if (!Files.exists(configFilePath)) {
    Files.createDirectories(configFilePath.getParent)
    Files.createFile(configFilePath)
  }

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

  var downloadSet = Set[Path]()
  sys addShutdownHook {
    if (!downloadSet.isEmpty) {
      downloadSet foreach { filePath => {
        logger.warning(s"$filePath's download was interrupted and was not moved.")
      }}
    }
  }

  if (watchList.isEmpty)
    logger.warning(s"There are no move rules specified in $configFilePath.")

  val watchFutures = watchList map {
    case (watchPath, moveList) => {
      val fileMovers = moveList map {
        case (moveParams, movePath) =>
          new FileMover(moveParams, movePath)
      }

      logger.info(s"Performing initial move of files in $watchPath.")
      // Perform initial move on startup.
      val dirStream = Files newDirectoryStream watchPath
      val dirIt = dirStream.iterator
      while(dirIt.hasNext)
        fileMovers.foreach(_.onEvent(dirIt.next))
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