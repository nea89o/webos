package io

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.types.shouldBeTypeOf

class PathTest : FunSpec({
	val homeDir = Path.of("/home") as Path.Absolute
	test("recognize relative paths as such") {
		listOf(
			Path.of("a/b"),
			Path.of("."),
			Path.of("a", "b"),
			Path.ofShell("a/b", userHome = homeDir),
			Path.ofShell(".", userHome = homeDir),
			Path.ofShell("a", "b", userHome = homeDir),
			Path.ofShell(listOf("a", "b"), userHome = homeDir),
		).forEach {
			assertSoftly(it) { shouldBeTypeOf<Path.Relative>() }
		}
	}
	test("recognize absolute paths as such") {
		listOf(
			Path.of("/a/b"),
			Path.of("/"),
			Path.ofShell("/b/c", userHome = homeDir),
		).forEach {
			assertSoftly(it) { shouldBeTypeOf<Path.Absolute>() }
		}
	}
})
