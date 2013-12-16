package main

import file_mover.pathutil.RichPath
import java.io.FileReader
import java.nio.file.{Path, Files, Paths}

import scala.concurrent.{Await, future, ExecutionContext}
import ExecutionContext.Implicits.global

import scala.concurrent.duration.Duration
import watch.Watcher._
import move.FileMover
import config.ConfigFileParser._
import RichPath._

object Main extends App {

  val configFilePath = Paths.get("config.txt")

  if (!Files.exists(configFilePath))
    Files.createFile(configFilePath)

  val r = new FileReader(configFilePath.toFile)
  val watchList = parseAll(file, r).get
  r.close()

  val watchFutures = watchList map { case (watchPath, moveList) => {

    def performMove(ePath: Path) = {
      moveList foreach {
        case (moveParams, movePath) => {

          if (moveParams contains (ePath.extension)) {
            val mover = new FileMover(movePath)

            ePath.downloadFinish.onSuccess {
              case p => mover.move(p)
            }

          }
        }
      }
    }

    // Perform initial move on startup.
    val dirStream = Files newDirectoryStream watchPath
    val dirIt = dirStream.iterator
    while(dirIt.hasNext)
      performMove(dirIt.next)
    dirStream.close()

    future { watch(watchPath) {
      case Created(eventPath) => {
        println(eventPath)
        performMove(eventPath)
      }
    }}
  }}

  watchFutures foreach (Await.ready(_, Duration.Inf))
}