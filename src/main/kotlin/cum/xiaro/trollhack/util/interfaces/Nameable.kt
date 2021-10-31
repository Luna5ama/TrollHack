package cum.xiaro.trollhack.util.interfaces

interface Nameable {
    val name: CharSequence
    val nameAsString: String
        get() = name.toString()
}
