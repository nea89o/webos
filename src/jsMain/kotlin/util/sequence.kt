package util

fun <T> Iterable<T>.expandWith(t: T): Sequence<T> =
	this.asSequence() + generateSequence { t }.asSequence()

