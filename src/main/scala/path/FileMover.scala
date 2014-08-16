package path

import java.nio.file.Path
import java.util.concurrent.TimeUnit

import com.twitter.logging.Logger
import path.RichPath.path2RichPath

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

class FileMover(logger: Logger, moveExtentions: List[String], destPath: Path) {
  /* Paths that are currently downloading and that will be moved when the download completes. */
  private var _pathsPendingMove = Set.empty[Path]

  def onEvent(eventPath: Path): Unit = {
    if (moveExtentions.contains(eventPath.extension)) {
      logger.info(s"Event path matching move rule detected: s$eventPath")

      if (!pathsPendingMove.contains(eventPath)) {
        _pathsPendingMove = _pathsPendingMove + eventPath

        logger.info(s"Waiting for download of $eventPath to finish (if it is downloading).")
        eventPath.downloadFinish(Duration.create(1, TimeUnit.HOURS)).onComplete {
          case Success(_) => {
            logger.info(s"Moving $eventPath")
            val eventPathAfterMove = FileUtil.move(eventPath, destPath)
            logger.info(s"$eventPath moved to $eventPathAfterMove.")
            _pathsPendingMove = _pathsPendingMove - eventPath
          }
          case Failure(_) => {
            logger.error(s"$eventPath has not been moved. It took too long to download.")
            _pathsPendingMove = _pathsPendingMove - eventPath
          }
        }
      }
      else
        logger.info(s"Ignoring duplicate event involving $eventPath.")
    }
  }

  def pathsPendingMove = _pathsPendingMove
}
