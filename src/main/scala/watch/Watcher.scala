package watch
import java.nio.file._
import java.nio.file.StandardWatchEventKinds._
import scala.collection.JavaConversions._

object Watcher {

  abstract class Event(val eventPath: Path)
  case class Created(override val eventPath: Path) extends Event(eventPath)
  case class Deleted(override val eventPath: Path) extends Event(eventPath)
  case class Modified(override val eventPath: Path) extends Event(eventPath)

  def watch(watchPath: Path, stopCondition: => Boolean = false)(f: PartialFunction[Event, Unit]): Unit = {

    /**
     * This is so we only end up watching for events we can actually handle. Things get a little messier if we don't
     * do this.
     */
    def extractEventKinds: List[WatchEvent.Kind[Path]] = {
      val dummyPath = Paths.get(".")
      val allFileEvents = Created(dummyPath) :: Deleted(dummyPath) :: Modified(dummyPath) :: Nil
      val fileEventsToWatch = allFileEvents.filter(f.isDefinedAt)
      val eventKindsToWatch = fileEventsToWatch map {
        case Created(x) => ENTRY_CREATE
        case Deleted(x) => ENTRY_DELETE
        case Modified(x) => ENTRY_MODIFY
      }
      eventKindsToWatch
    }
    val eventKinds = extractEventKinds
    val service = FileSystems.getDefault.newWatchService
    watchPath.register(service, eventKinds: _*)
    var watchKeyIsValid = false
    do {
      val watchKey = service.take()
      val eventList = watchKey.pollEvents map (_.asInstanceOf[WatchEvent[Path]])
      val wrappedEvents =
        eventList map { e => {
          val absEventPath = watchPath resolve e.context
          e.kind match {
            case ENTRY_CREATE => Created(absEventPath)
            case ENTRY_DELETE => Deleted(absEventPath)
            case ENTRY_MODIFY => Modified(absEventPath)
          }
        }}
      wrappedEvents foreach f

      watchKey.reset()
      watchKeyIsValid = watchKey.isValid
    } while (!stopCondition && watchKeyIsValid)
  }
}