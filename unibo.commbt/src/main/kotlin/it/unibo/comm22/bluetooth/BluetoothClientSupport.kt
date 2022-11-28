package it.unibo.comm22.bluetooth

import unibo.comm22.interfaces.Interaction2021
import unibo.comm22.utils.ColorsOut


object BluetoothClientSupport {
    fun connect(address: String, port: Int = 5, nattempts: Int = 5): Interaction2021 {
        ColorsOut.out("BluetoothClientSupport | connecting...", ColorsOut.ANSI_PURPLE)
        return BluetoothConnectionPython.createClient(address, port, nattempts)
    }
}