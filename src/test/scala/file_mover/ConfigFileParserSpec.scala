package file_mover

import org.specs2.mutable.Specification

import _root_.config.ConfigFileParser._
import java.nio.file.Paths
import scala.util.parsing.input.CharSequenceReader

class ConfigFileParserSpec extends Specification {

  "Parser" should {

    "watchPath" in {
      implicit val parserToTest = watchPath
      parsing ("watch C:\\dir") mustEqual(Paths.get("C:\\dir"))
    }

    "moveDef" in {
      implicit val parserToTest = moveDef
      parsing ("txt, png => C:\\move_dir") mustEqual((List("txt", "png"), Paths.get("C:\\move_dir")))
    }

    "watchDef" in {
      implicit val parserToTest = watchDef
      parsing (
        """watch C:\\dir {
          |  txt, png => C:\\move_dir
          |  gif => C:\\gif_dir
          |}""".stripMargin) mustEqual((Paths.get("C:\\dir"),List(
                                                               (List("txt", "png"), Paths.get("C:\\move_dir")),
                                                               (List("gif"), Paths.get("C:\\gif_dir"))
                                                             )

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
