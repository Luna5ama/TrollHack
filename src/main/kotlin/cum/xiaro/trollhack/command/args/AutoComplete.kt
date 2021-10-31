package cum.xiaro.trollhack.util.command.args

interface AutoComplete {
    fun completeForInput(string: String): String?
}

class DynamicPrefixMatch(
    private val matchList: () -> Collection<String>?
) : AutoComplete {
    override fun completeForInput(string: String): String? {
        if (string.isBlank()) return null
        val list = matchList() ?: return null

        return list.find { it.startsWith(string, true) }
    }
}

class StaticPrefixMatch(
    private val matchList: Collection<String>
) : AutoComplete {
    override fun completeForInput(string: String): String? {
        if (string.isBlank()) return null

        return matchList.find { it.startsWith(string, true) }
    }
}
