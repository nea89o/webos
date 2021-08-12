package io

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.types.shouldBeTypeOf

class PathTest : DescribeSpec({
	describe("Path") {
		val homeDir = Path.of("/home") as Path.Absolute
		it("recognize relative paths as such") {
			listOf(
				Path.of("a/b"),
				Path.of("."),
				Path.of("a", "b"),
				Path.ofShell("a/b", userHome = homeDir),
				Path.ofShell(".", userHome = homeDir),
				Path.ofShell("a", "b", userHome = homeDir),
				Path.ofShell(listOf("a", "b"), userHome = homeDir),
			).forEach {
				it.shouldBeTypeOf<Path.Relative>()
			}
		}
		it("recognize absolute paths as such") {
			listOf(
				Path.of("/a/b"),
				Path.of("/"),
				Path.ofShell("/b/c", userHome = homeDir),
			).forEach {
				it.shouldBeTypeOf<Path.Absolute>()
			}
		}
	}
})
