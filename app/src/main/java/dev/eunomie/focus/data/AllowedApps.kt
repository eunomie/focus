package dev.eunomie.focus.data

/**
 * The list rules, kept as pure functions rather than inline in the DataStore edit
 * lambdas — index arithmetic and a cap are exactly the sort of thing that is easy to get
 * subtly wrong and impossible to test through a coroutine and a file.
 */
object AllowedApps {

    fun toggle(current: List<String>, packageName: String, max: Int): List<String> = when {
        packageName in current -> current - packageName
        current.size >= max -> current
        else -> current + packageName
    }

    fun move(current: List<String>, packageName: String, delta: Int): List<String> {
        val from = current.indexOf(packageName)
        val to = from + delta
        if (from < 0 || to !in current.indices) return current
        return current.toMutableList().apply {
            this[from] = current[to]
            this[to] = current[from]
        }
    }
}
