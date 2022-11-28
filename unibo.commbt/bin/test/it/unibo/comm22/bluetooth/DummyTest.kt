package it.unibo.comm22.bluetooth

import it.unibo.kactor.ApplMessage
import it.unibo.kactor.ApplMessageType
import it.unibo.kactor.IApplMessage
import org.junit.Assert.assertEquals
import org.junit.Test
import unibo.comm22.interfaces.Interaction2021
import unibo.comm22.utils.ColorsOut
import unibo.comm22.utils.CommSystemConfig
import unibo.comm22.utils.CommUtils


class DummyTest {
    val address = "00:10:60:d1:4b:88"

    /**
     * Nota: non funziona con BT <4.1 (come controllare:
     * https://support.microsoft.com/it-it/windows/quale-versione-bluetooth-%C3%A8-presente-nel-pc-f5d4cff7-c00d-337b-a642-d2d23b082793 )
     * Richiede un client e server dalla stessa porta, funzionalità aggiunta
     * nella versione 4.1 del protocollo.python get
     */
    @Test
    fun dummyTest() {
        BluetoothConfig.disableWrite()
        BluetoothConfig.setConfiguration()

        BluetoothConfig.scriptFolder = "../scripts/test"
        BluetoothConfig.serverScript = "dummy_server.py"

        CommSystemConfig.tracing = true

        val msg = "ananas"
        var received : String? = null

        val server = BluetoothServer("TestServer", address, object : BluetoothMsgHandler("TestHandler") {
            override fun elaborate(msg: IApplMessage, conn: Interaction2021) {
                ColorsOut.outappl("Server received message: '$msg'", ColorsOut.CYAN)
                received = msg.msgContent()
                conn.close()
            }
        })

        server.activate()

        ColorsOut.outappl("Waiting for server received...", ColorsOut.YELLOW)

        while (received == null) {
            Thread.sleep(500)
        }

        server.stop()

        assertEquals(msg, received)
    }
}