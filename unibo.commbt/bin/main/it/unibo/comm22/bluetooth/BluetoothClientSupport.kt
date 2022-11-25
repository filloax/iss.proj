package it.unibo.comm22.bluetooth

import unibo.comm22.interfaces.Interaction2021


object BluetoothClientSupport {
    fun connect(address: String, port: Int = 5, nattempts: Int = 5): Interaction2021 {
        return BluetoothConnectionPython.createClient(address, port)
    }
}