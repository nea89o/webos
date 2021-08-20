import kotlin.coroutines.*

class IORunner : Continuation<Unit> {
	var nextStep: Continuation<Unit>? = null
	val eventQueue = ArrayDeque<String>()
	fun _CALLED_BY_JS_onInput(str: String) {
		eventQueue.addLast(str)
		awake()
	}

	fun _CALLED_BY_JS_onWhatever() {
		awake()
	}

	private fun awake() {
		val step = nextStep
		if (step != null) {
			nextStep = null
			step.resume(Unit)
		}
	}

	suspend fun wait() {
		return suspendCoroutine { c ->
			console.log("SUSPENDING COROUTINE")
			nextStep = c
		}
	}

	override val context: CoroutineContext
		get() = EmptyCoroutineContext

	override fun resumeWith(result: Result<Unit>) {
		if (result.isFailure) {
			result.exceptionOrNull()?.printStackTrace()
		} else {
			println("IORunner exited successfully")
		}
	}

	companion object {
		fun runOnIO(block: suspend IORunner.() -> Unit): IORunner {
			val r = IORunner()
			block.startCoroutine(r, r)
			return r
		}
	}
}


suspend fun IORunner.getOneKey(): String {
	while (true) {
		val x = eventQueue.removeFirstOrNull()
		if (x == null)
			wait()
		else {
			return x
		}
	}
}

