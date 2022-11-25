package it.unibo.comm22.bluetooth

import it.unibo.kactor.ApplMessage
import unibo.comm22.interfaces.IApplMsgHandler
import unibo.comm22.interfaces.Interaction2021
import unibo.comm22.utils.ColorsOut
import java.io.IOException
import kotlin.concurrent.thread
import it.unibo.kactor.IApplMessage;


/**
 * @param name
 * @param address Address for the adapter, as a given PC might have more than one.
 * @param port Defaults to 5 (arbitrary value)
 */
class BluetoothServer(val name: String, val address: String, val msgHandler: IApplMsgHandler, val port: Int = 5) {
    private var stopped = true
    private var lastConn: BluetoothConnectionPython? = null // One at a time due to BT

    fun activate() {
        if (stopped) {
            stopped = false
            ColorsOut.out("$name | BT ACTIVATE ADDR=$address PORT=$port", ColorsOut.BLUE)
            thread(block = this::run, start = true)
        }
    }

    fun stop() {
        if (!stopped) {
            stopped = true
            lastConn?.close()
            ColorsOut.out("$name | MAUNALLY STOPPED", ColorsOut.BLUE)
        }
    }

    private fun run() {
        while (!stopped) {
            val conn = try {
                BluetoothConnectionPython.createServer(address, port)
            } catch (e: IOException) {
                e.printStackTrace()
                stopped = true
                return
            }
            BluetoothApplMessageHandler(msgHandler, conn)
            lastConn = conn as BluetoothConnectionPython

            // Can handle one connection at a time
            lastConn!!.process.waitFor()
        }
    }
}

class BluetoothApplMessageHandler(private val handler: IApplMsgHandler, private val conn: Interaction2021) : Thread() {
    init {
        start()
    }

    override fun run() {
        val name = handler.name
        try {
            ColorsOut.out("BluetoothApplMessageHandler | STARTS with handler=$name conn=$conn", ColorsOut.BLUE)
            while (true) {
                val msg = conn.receiveMsg()
                ColorsOut.out("$name  | BluetoothApplMessageHandler received: $msg", ColorsOut.YELLOW)
                if (msg == null) {
                    conn.close()
                    break
                } else {
                    val m: IApplMessage = try {
                        ApplMessage(msg)
                    } catch(e: Exception) {
                        e.printStackTrace()
                        System.err.println("Invalid message: ${e.message}")
                        continue
                    }

                    handler.elaborate(m, conn)
                }
            }
            ColorsOut.out("BluetoothApplMessageHandler  |  BYE", ColorsOut.BLUE)
        } catch (e: Exception) {
            ColorsOut.outerr("BluetoothApplMessageHandler | ERROR:" + e.message)
        }
    }
}

abstract class BluetoothMsgHandler(private val name: String) : IApplMsgHandler {
    override fun getName() = name

    override fun sendMsgToClient(msg: String, conn: Interaction2021) {
        conn.forward(msg)
    }

    override fun sendAnswerToClient(msg: String, conn: Interaction2021) {
        conn.reply(msg)
    }
}
