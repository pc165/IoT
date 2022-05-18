package com.android.iotproject.profile.callback

import android.bluetooth.BluetoothDevice
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.MutableLiveData
import no.nordicsemi.android.ble.callback.DataReceivedCallback
import no.nordicsemi.android.ble.callback.DataSentCallback
import no.nordicsemi.android.ble.data.Data
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32


class DataCallback : DataReceivedCallback, DataSentCallback {
    private var TAG: String = javaClass.simpleName
    private var mBytesTransfered: Int = 0
    private var mBytesTotal: Int = 0
    private var mDataBuffer: ByteArray = ByteArray(0)
    private var mChecksum: Long = 0
    var percent: MutableLiveData<Int> = MutableLiveData<Int>(0)
    var image: MutableLiveData<Bitmap> =
        MutableLiveData<Bitmap>(Bitmap.createBitmap(10, 10, Bitmap.Config.RGB_565))

    fun isFinished() = mBytesTotal == mBytesTransfered

    override fun onDataReceived(device: BluetoothDevice, data: Data) {
        try {
            if (mBytesTransfered == mBytesTotal) {
                val received: ByteArray = data.value!!
                if (received[0] == 0xFF.toByte()) { // First byte should always be 0XFF
                    var byteBuffer = ByteBuffer.wrap(received.copyOfRange(1, 5))
                    var check = ByteBuffer.wrap(received.copyOfRange(5, received.size))
                    check = check.order(ByteOrder.LITTLE_ENDIAN)
                    byteBuffer = byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
                    mBytesTotal = byteBuffer.int
                    mChecksum = (check.int.toLong()) and 0xffffffffL
                    mBytesTransfered = 0
                    mDataBuffer = ByteArray(mBytesTotal)
                    percent.value = 0
                } else {
                    Log.w(TAG, "Picture info invalid $data")
                }
            } else {
                val txValue: ByteArray = data.value!!
                System.arraycopy(
                    txValue,
                    0,
                    mDataBuffer,
                    mBytesTransfered,
                    txValue.size
                )
                mBytesTransfered += txValue.size
                val p = (mBytesTransfered / (mBytesTotal.toFloat())) * 100
                percent.value = p.toInt()
                if (mBytesTransfered >= mBytesTotal) {
                    val c = CRC32()
                    c.update(mDataBuffer)
                    Log.i(TAG, "Transfer Completed")
                    if (mChecksum != c.value) {
                        Log.i(TAG, "Invalid Checksum $mChecksum != ${c.value.toInt()}")
                    }
                    val jpgHeader = byteArrayOf(-1, -40, -1, -32)
                    if (jpgHeader.contentEquals(mDataBuffer.copyOfRange(0, 4))) {
                        image.value =
                            BitmapFactory.decodeByteArray(mDataBuffer, 0, mDataBuffer.size)
                    } else {
                        Log.w(TAG, "JPG header missing!! Image data corrupt.")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during transfer $e")
            mBytesTotal = 0
            mBytesTransfered = 0
        }
    }

    override fun onDataSent(device: BluetoothDevice, data: Data) {
        Log.d(TAG, "Send $data")

    }
}