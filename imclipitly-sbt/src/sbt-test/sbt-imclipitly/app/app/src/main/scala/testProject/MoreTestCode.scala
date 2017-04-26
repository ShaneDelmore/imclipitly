package demo

object OtherEnrichments {
  implicit class OptionImprovements[A](opt: Option[A]) {
    def valueOr(default: A) = opt.getOrElse(default)
  }
}
