package it.unibo.comm22.bluetooth

import it.unibo.kactor.ApplMessage
import it.unibo.kactor.ApplMessageType
import it.unibo.kactor.IApplMessage
import org.junit.Assert.assertEquals
import org.junit.Test
import unibo.comm22.interfaces.Interaction2021
import unibo.comm22.utils.ColorsOut
import unibo.comm22.utils.CommSystemConfig

val localAddress = "30:89:4a:64:d0:b5"

fun main() {
    BluetoothConfig.disableWrite()
    BluetoothConfig.setConfiguration()

    CommSystemConfig.tracing = true

    val server = BluetoothServer("TestServer", localAddress, object : BluetoothMsgHandler("TestHandler") {
        override fun elaborate(msg: IApplMessage, conn: Interaction2021) {
            ColorsOut.outappl("Server received message: '$msg'", ColorsOut.CYAN)
        }
    })

    server.activate()
}