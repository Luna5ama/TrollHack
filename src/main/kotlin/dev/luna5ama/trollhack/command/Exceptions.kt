package dev.luna5ama.trollhack.command

/**
 * Exception throws when no command is found in a [AbstractCommandManager]
 *
 * @see AbstractCommandManager.getCommand
 */
class CommandNotFoundException(val command: String?) :
    Exception("Command not found: '$command'.")

/**
 * Exception throws when no subcommand is found for a [Command]
 *
 * @see Command.invoke
 */
class SubCommandNotFoundException(args: Array<String>, val command: Command<*>) :
    Exception("No matching sub command found for args: \"${args.sliceArray(1 until args.size).joinToString(" ")}\".")
