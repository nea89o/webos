package io

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.be
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beOfType
import io.kotest.matchers.types.shouldBeTypeOf

class PathTest : FunSpec({
	val homeDir = Path.of("/home") as Path.Absolute
	test("recognize relative paths as such") {
		forAll(
			row(Path.of("a/b")),
			row(Path.of(".")),
			row(Path.of("a", "b")),
			row(Path.ofShell("a/b", userHome = homeDir)),
			row(Path.ofShell(".", userHome = homeDir)),
			row(Path.ofShell("a", "b", userHome = homeDir)),
			row(Path.ofShell(listOf("a", "b"), userHome = homeDir)),
		) {
			assertSoftly(it) { shouldBeTypeOf<Path.Relative>() }
		}
	}
	test("recognize absolute paths as such") {
		forAll(
			row(Path.of("/a/b")),
			row(Path.of("/")),
			row(Path.ofShell("/b/c", userHome = homeDir)),
		) {
			assertSoftly(it) { shouldBeTypeOf<Path.Absolute>() }
		}
	}
	test("Path.of(x).stringPath == x") {
		forAll(
			row("/"),
			row("/a/b"),
			row("a/b"),
			row("."),
		) { name ->
			assertSoftly(Path.of(name).stringPath) {
				shouldBe(name)
			}
			assertSoftly(Path.ofShell(name, homeDir).stringPath) {
				shouldBe(name)
			}
		}
	}
	test("Shell resolution of home directory") {
		forAll(
			row("~/a", "/home/a"),
			row("~", "/home"),
			row("~/.", "/home/."),
			row("/a", "/a"),
			row("a", "a"),
		) { a, b ->
			assertSoftly(Path.ofShell(a, homeDir).stringPath) {
				shouldBe(b)
			}
		}
	}
	test("Relative path resolution works") {
		forAll(
			row("/a/b", "c/d", "/a/b/c/d"),
			row("/a/b", "/c/d", "/c/d"),
			row("/a/", "c", "/a/c"),
		) { a, b, c ->
			val x = Path.of(a)
			val y = Path.of(b)
			val z = x.resolve(y)
			assertSoftly {
				x should beOfType<Path.Absolute>()
				z should beOfType<Path.Absolute>()
				z.stringPath should be(c)
			}
		}
	}
	test("Equality checks should work") {
		forAll(
			row("a"),
			row("a/b"),
			row("/a/b"),
			row("c//d")
		) {
			assertSoftly {
				Path.of(it) should be(Path.of(it))
			}
		}
	}
	test("relaitivization works") {
		forAll(
			row("/a/b", "/a", ".."),
			row("/a", "/a/b", "b"),
			row("/a/b", "/a/c", "../c"),
		) { a, b, c ->
			assertSoftly {
				Path.of(a).shouldBeTypeOf<Path.Absolute>().relativize(Path.of(b)) shouldBe Path.of(c)
			}
		}
	}
})
