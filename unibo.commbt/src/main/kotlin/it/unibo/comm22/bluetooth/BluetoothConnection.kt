package it.unibo.comm22.bluetooth

import unibo.comm22.interfaces.Interaction2021
import unibo.comm22.utils.ColorsOut
import java.io.BufferedReader
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.PrintStream
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.concurrent.thread
import kotlin.io.path.outputStream

/**
 * Handles a bluetooth connection based on Python scripts
 * @param process Process of the python script that implements the connection,
 *  its stdout should be the connection received data while its stdin should be
 *  data sent to the connection
 */
class BluetoothConnectionPython(val address: String, val port: Int, val listen: Boolean = false, params: String = "")
        : Interaction2021 {

    val process: Process
    private val procStdOut: BufferedReader
    private val procStdIn: OutputStreamWriter

    val open get() = process.isAlive

    init {
        val scriptPath = Path.of(
            BluetoothConfig.scriptFolder,
            if (listen) BluetoothConfig.serverScript else BluetoothConfig.clientScript,
        ).toAbsolutePath()
        val executable = "${BluetoothConfig.pythonCmd} $scriptPath"
        val verbosePart = if (BluetoothConfig.verbose) " -v" else ""
        val listenPart = if (listen) " -l" else ""
        val cmd = "$executable -a $address -p $port $listenPart$params$verbosePart"

        process = Runtime.getRuntime().exec(cmd)

        ColorsOut.out("BtConnPython | Started process with command '$cmd', " +
                "status: ${if (process.isAlive) "running" else "dead"}" +
                "/${if (!process.isAlive) process.exitValue() else "-"}" +
                " listen: $listen"
        )

        procStdOut = process.inputStream.bufferedReader()
        procStdIn = OutputStreamWriter(process.outputStream)

        tryRedirectProcessStderr(process)

        Runtime.getRuntime().addShutdownHook(thread(start=false, block={
            if (process.isAlive) {
                process.destroy()
            }
        }))

        waitStart(this)
    }

    companion object {
        fun createClient(address: String, port: Int = 5, nattempts: Int = 5): Interaction2021 {
            val params = "-r $nattempts"

            return BluetoothConnectionPython(address, port, false, params)
        }

        fun createServer(address: String, port: Int = 5): Interaction2021 {
            return BluetoothConnectionPython(address, port, true)
        }

        private fun waitStart(conn: BluetoothConnectionPython) {
            val checkLine = "CONNECTED"
            val startLine = conn.receiveMsg()

            if (startLine == checkLine) {
                ColorsOut.out("BT conn setup: received $checkLine, returning", ColorsOut.BLUE)
            } else {
                conn.close()
                if (startLine != null)
                    throw IOException("BT conn setup: Didn't start BT script properly, start line is <$startLine>; check log if logging")
                else
                    throw IOException("BT conn setup: Script error!")
            }
        }

        private fun tryRedirectProcessStderr(process: Process) {
            if (BluetoothConfig.logScripts || BluetoothConfig.printStdErr) {
                thread {
                    val streams = ArrayList<PrintStream>()
                    if (BluetoothConfig.logScripts) {
                        val logFolderPath = Path.of(BluetoothConfig.scriptFolder, BluetoothConfig.logScriptsFolder)
                        if (!logFolderPath.toFile().isDirectory) {
                            logFolderPath.toFile().mkdir()
                        }
                        val timestamp = DateTimeFormatter.ofPattern("MM-dd_HH-mm").format(LocalDateTime.now())
                        val filename = "btscript_${timestamp}_${process.pid()}.log"
                        val filepath = logFolderPath.resolve(filename)
                        val logStream = PrintStream(filepath.outputStream())
                        streams.add(logStream)
                    }
                    if (BluetoothConfig.printStdErr) {
                        streams.add(System.err)
                    }

                    val reader = process.errorStream.bufferedReader()
                    try {
                        while (process.isAlive || reader.ready()) {
                            reader.readLine()?.let { line ->
                                // Python non installato
                                if (line.lowercase().matches(Regex(""".*python.*(non trovato|not found).*"""))) {
                                    System.err.println(
                                        "Python command '${BluetoothConfig.pythonCmd}' not found; maybe not installed " +
                                                "or should use change command in config?\nOS error is: $line"
                                    )
                                }

                                streams.forEach {
                                    it.println(line)
                                    it.flush()
                                }
                            }
                        }
                    } finally {
                        streams.forEach{
                            it.flush()
                        }
                    }
                }
            }
        }
    }

    override fun forward(msg: String) {
        try {
            procStdIn.write("$msg\n")
            procStdIn.flush()
        } catch (e : IOException) {
            e.printStackTrace()
        }
    }

    override fun request(msg: String): String? {
        forward(msg)
        return receiveMsg()
    }

    override fun reply(msg: String) {
        forward(msg)
    }

    override fun receiveMsg(): String? {
        return try {
            procStdOut.readLine()
        } catch (e : IOException) {
            e.printStackTrace()
            null
        }
    }

    override fun close() {
        process.destroy()
    }
}