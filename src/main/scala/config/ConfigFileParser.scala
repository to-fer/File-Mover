package config

import scala.util.parsing.combinator.JavaTokenParsers
import java.nio.file.Path

/*
 * Configuration file example:
 *
 * watch /dir/to/watch/goes/here {
 *   ext1, ext2, ext3 => /path/to/move/files/with/these/extensions
 *   ext4 => /another/move/path
 * }
 */
object ConfigFileParser extends JavaTokenParsers with PathParser {
  def file: Parser[List[(Path, List[(List[String], Path)])]] = rep(watchDef) ^^ (List() ++ _)

  def watchDef: Parser[(Path, List[(List[String], Path)])] = watchPath~"{"~rep(moveDef)~"}" ^^ {case (wp~"{"~mds~"}") => (wp, mds)}
  def watchPath: Parser[Path] = "watch "~>path

  def moveDef: Parser[(List[String], Path)] = repsep("""[^, ]+""".r, ",")~"=>"~path ^^ {case (mps~"=>"~mp) => (mps, mp)}

}
