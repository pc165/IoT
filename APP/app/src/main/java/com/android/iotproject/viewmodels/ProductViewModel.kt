package com.android.iotproject.viewmodels

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.android.iotproject.adapter.DiscoveredBluetoothDevice
import com.android.iotproject.profile.ProductManager
import no.nordicsemi.android.ble.ConnectRequest
import no.nordicsemi.android.ble.livedata.state.ConnectionState

class ProductViewModel(application: Application) : AndroidViewModel(application) {
    private val productManager: ProductManager = ProductManager(getApplication())
    private var device: BluetoothDevice? = null
    private var connectRequest: ConnectRequest? = null
    val connectionState: LiveData<ConnectionState>
        get() = productManager.state

    fun getImage() = productManager.getImage()
    fun getPercent() = productManager.getPercent()

    fun connect(target: DiscoveredBluetoothDevice) {
        // Prevent from calling again when called again (screen orientation changed).
        if (device == null) {
            device = target.device
            reconnect()
        }
    }

    fun reconnect() {
        if (device != null) {
            connectRequest = productManager.connect(device!!)
                .retry(3, 100)
                .useAutoConnect(false)
                .then { connectRequest = null }
            connectRequest!!.enqueue()
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }

    private fun disconnect() {
        device = null
        if (connectRequest != null) {
            connectRequest!!.cancelPendingConnection()
        } else if (productManager.isConnected) {
            productManager.disconnect().enqueue()
        }
    }

    fun getPicture() {
        productManager.getPicture()
    }
}