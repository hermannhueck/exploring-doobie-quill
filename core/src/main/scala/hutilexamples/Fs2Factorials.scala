package hutilexamples

import cats.effect.{ExitCode, IO}

import fs2.Stream

object Fs2Factorials extends hutil.IOApp {

  val ints: Stream[IO, Int]          = Stream.range(1, 31).covary[IO]
  val factorials: Stream[IO, BigInt] =
    ints.scan(BigInt(1))((acc, next) => acc * next)

  val stream: Stream[IO, Unit] =
    factorials
      .zipWithIndex
      .map { case num -> index => s"$index = $num" }
      .evalMap { str => IO(println(str)) }

  def ioRun(args: List[String]): IO[ExitCode] =
    stream.compile.drain.as(ExitCode.Success)
}
