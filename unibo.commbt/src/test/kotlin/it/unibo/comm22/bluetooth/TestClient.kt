package it.unibo.comm22.bluetooth

import it.unibo.kactor.ApplMessage
import it.unibo.kactor.ApplMessageType
import it.unibo.kactor.IApplMessage
import org.junit.Assert.assertEquals
import org.junit.Test
import unibo.comm22.interfaces.Interaction2021
import unibo.comm22.utils.ColorsOut
import unibo.comm22.utils.CommSystemConfig
import java.io.BufferedReader
import java.io.InputStreamReader


val serverAddress = "30:89:4a:64:d0:b5"

fun main() {
    BluetoothConfig.disableWrite()
    BluetoothConfig.setConfiguration()

    CommSystemConfig.tracing = true

    val clientConn = BluetoothClientSupport.connect(serverAddress, nattempts = 1)

    val input = System.`in`
    val reader = BufferedReader(InputStreamReader(input))
    var line = reader.readLine()

    while (line != null) {
        val applMessage = ApplMessage("testMsg", ApplMessageType.dispatch.toString(), "client", "server", line, "1")
        clientConn.forward(applMessage.toString())

        line = reader.readLine()
    }

    clientConn.close()
}