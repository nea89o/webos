package io

sealed interface Path {
	val parts: List<String>
	fun toAbsolutePath(relativeTo: Absolute): Absolute {
		return relativeTo.resolve(this)
	}

	fun resolve(path: Path): Path

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

	data class Relative internal constructor(override val parts: List<String>) : Path {
		override fun resolve(path: Path): Path {
			if (path is Absolute) return path
			return Relative(this.parts + path.parts)
		}
	}

	data class Absolute internal constructor(override val parts: List<String>) : Path {
		override fun resolve(path: Path): Absolute {
			if (path is Absolute) return path
			return Absolute(this.parts + path.parts)
		}

		fun relativize(path: Path): Relative = when (path) {
			is Relative -> path
			is Absolute -> {
				var commonPrefix = true
				val partList = mutableListOf<String>()
				var returns = 0
				for ((idx, part) in path.parts.withIndex()) {
					if (idx < this.parts.size) {
						if (this.parts[idx] == part && commonPrefix) {
							continue
						} else {
							commonPrefix = false
							returns++
						}
					}
					partList.add(part)
				}
				Relative(List(returns) { "" } + partList)
			}
		}
	}
}
