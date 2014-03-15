package file_mover

import org.specs2.mutable.Specification

import _root_.config.ConfigFileParser._
import java.nio.file.Paths
import scala.util.parsing.input.CharSequenceReader

class ConfigFileParserSpec extends Specification {

  "Parser" should {
    val watchDir = Paths.get("C:", "dir")
    val watchDefString = s"watch $watchDir"

    val textFileMoveDir = Paths.get("C:", "move_dir")
    val textFileMoveDef = List("txt", "doc")
    val textFileMoveDefString = textFileMoveDef.mkString(", ") + s" => $textFileMoveDir"

    val gifFileMoveDir = Paths.get("C:", "gif_dir")
    val gifFileMoveDef = List("gif")
    val gifFileMoveDefString = gifFileMoveDef(0) + s" => $gifFileMoveDir"

    "watchPath" in {
      implicit val parserToTest = watchPath
      parsing (watchDefString) mustEqual(watchDir)
    }

    "moveDef" in {
      implicit val parserToTest = moveDef
      parsing (textFileMoveDefString) mustEqual((textFileMoveDef, textFileMoveDir))
    }

    "watchDef" in {
      implicit val parserToTest = watchDef
      parsing (
        s"""$watchDefString {
          |  $textFileMoveDefString
          |  $gifFileMoveDefString
          |}""".stripMargin) mustEqual(watchDir , List(
                                                        (textFileMoveDef, textFileMoveDir),
                                                        (gifFileMoveDef, gifFileMoveDir)
                                                      )
                                      )
    }
  }

  private def parsing[T] (s: String)(implicit p: Parser[T]): T = {
    val phraseParser = phrase(p)
    val input = new CharSequenceReader(s)
    phraseParser(input) match {
      case Success(t: T,_)     => t
      case Failure(msg,_) => throw new IllegalArgumentException(
        "Could not parse '" + s + "': " + msg)
    }
  }

}
