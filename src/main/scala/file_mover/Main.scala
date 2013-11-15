package main

import file_mover.pathutil.RichPath
import java.io.FileReader
import java.nio.file.{Files, Paths}

import scala.concurrent.{Await, future, ExecutionContext}
import ExecutionContext.Implicits.global

import scala.concurrent.duration.Duration
import watch.Watcher._
import move.FileMover
import config.ConfigFileParser._
import RichPath._

object Main extends App {

  val configFilePath = Paths.get("config.txt")

  if (!Files.exists(configFilePath.getParent))
    Files.createDirectory(configFilePath.getParent)

  if (!Files.exists(configFilePath))
    Files.createFile(configFilePath)

  val r = new FileReader(configFilePath.toFile)
  val watchList = parseAll(file, r).get

  val watchFutures = watchList map { case (watchPath, moveList) => {
    future { watch(watchPath) {
      case Created(eventPath) => {

        moveList foreach {
          case (moveParams, movePath) => {

            if (moveParams contains (eventPath.extension)) {
              val mover = new FileMover(movePath)

              eventPath.downloadFinish.onSuccess {
                case p => mover.move(p)
              }

            }
          }
        }
      }
    }}
  }}

  watchFutures foreach (Await.ready(_, Duration.Inf))
}
