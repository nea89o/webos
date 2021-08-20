package io

sealed class Path {
	abstract val parts: List<String>
	fun toAbsolutePath(relativeTo: Absolute): Absolute {
		return relativeTo.resolve(this)
	}

	abstract fun resolve(path: Path): Path

	abstract val stringPath: String

	companion object {
		val root = Absolute(listOf())

		fun ofShell(string: String, userHome: Absolute): Path =
			ofShell(string.split("/"), userHome)

		fun ofShell(vararg parts: String, userHome: Absolute): Path =
			ofShell(parts.toList(), userHome)

		fun of(vararg parts: String): Path =
			of(parts.toList())

		fun of(string: String): Path =
			of(string.split("/"))

		fun ofShell(parts: List<String>, userHome: Absolute): Path {
			if (parts.firstOrNull() == "~")
				return userHome.resolve(Relative(parts.subList(1, parts.size).filter { it.isNotEmpty() }))
			return of(parts)
		}

		fun of(parts: List<String>): Path {
			if (parts.isEmpty())
				return root
			if (parts[0] == "") // Starts with a /
				return Absolute(parts.subList(1, parts.size).filter { it.isNotEmpty() })
			return Relative(parts.filter { it.isNotEmpty() })
		}
	}

	data class Relative internal constructor(override val parts: List<String>) : Path() {
		override fun resolve(path: Path): Path {
			if (path is Absolute) return path
			return Relative(this.parts + path.parts)
		}

		override val stringPath: String get() = parts.joinToString("/")
	}

	data class Absolute internal constructor(override val parts: List<String>) : Path() {
		override fun resolve(path: Path): Absolute {
			if (path is Absolute) return path
			return Absolute(this.parts + path.parts)
		}

		override val stringPath: String get() = "/" + parts.joinToString("/")

		fun relativize(path: Path): Relative = when (path) {
			is Relative -> path
			is Absolute -> {
				var idx = 0
				while (idx < path.parts.size && idx < parts.size && path.parts[idx] == parts[idx]) {
					idx++
				}
				val returns = if (idx < parts.size) {
					parts.size - idx
				} else {
					0
				}
				Relative(List(returns) { ".." } + path.parts.subList(idx, path.parts.size))
			}
		}
	}

	override fun toString(): String = "Path($stringPath)"
}
