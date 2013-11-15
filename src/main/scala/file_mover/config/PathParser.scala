package config

import java.nio.file.{Paths, Path}
import scala.util.parsing.combinator.RegexParsers

trait PathParser extends RegexParsers {
  def path: Parser[Path] = """[\S]+""".r ^^ (Paths.get(_))
}
