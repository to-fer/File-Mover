package main

import java.io.FileReader
import java.nio.file.{Path, Files, Paths}

import scala.concurrent.{Await, future, ExecutionContext}
import ExecutionContext.Implicits.global

import scala.concurrent.duration.Duration
import watch.Watcher._
import move.FileMover
import config.ConfigFileParser._
import pathutil.RichPath._
import com.twitter.logging.Logger
import com.twitter.logging.config.{FileHandlerConfig, LoggerConfig}
import java.util.concurrent.TimeUnit
import scala.util.{Failure, Success}

object Main extends App {

  val configFilePath = Paths.get("config.txt")
  if (!Files.exists(configFilePath))
    Files.createFile(configFilePath)
  val r = new FileReader(configFilePath.toFile)
  val watchList = parseAll(file, r).get
  r.close()

  val logger = Logger.get(getClass)
  val config = new LoggerConfig {
    handlers = new FileHandlerConfig {
      filename = "file-mover.log"
    }
  }
  config()

  val watchFutures = watchList map {
    case (watchPath, moveList) => {
      def performMove(eventPath: Path) = {
        moveList foreach {
          case (moveParams, movePath) => {
            if (moveParams contains (eventPath.extension)) {
              val mover = new FileMover(movePath)

              logger.info(s"About to move $eventPath. If you don't see a move-conformation message, then there was a " +
                           "problem, probably with detecting when the file to be moved finished downloading.")

              eventPath.downloadFinish(Duration.create(1, TimeUnit.HOURS)).onComplete {
                case Success(_) => {
                  logger.info(s"Moving $eventPath")
                  val eventPathAfterMove = mover.move(eventPath)
                  logger.info(s"$eventPath moved to $eventPathAfterMove.")
                }
                case Failure(_) =>
                  logger.error(s"$eventPath has not been moved. It took too long to download.")
              }
            }
          }
        }
      }

      logger.info(s"Performing initial move of files in $watchPath.")
      // Perform initial move on startup.
      val dirStream = Files newDirectoryStream watchPath
      val dirIt = dirStream.iterator
      while(dirIt.hasNext)
        performMove(dirIt.next)
      dirStream.close()

      logger.info(s"Watching $watchPath")
      future {
        watch(watchPath) {
          case Created(eventPath) => {
            logger.info(s"File creation detected: $eventPath")

            performMove(eventPath)
          }
        }
      }
    }
  }

  watchFutures foreach (Await.ready(_, Duration.Inf))
}