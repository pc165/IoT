package com.android.iotproject.profile

import no.nordicsemi.android.ble.data.Data

const val GET_PICTURE: Byte = 0x01

class OperationCode {
    companion object {
        fun getPicture(): Data {
            return Data.opCode(GET_PICTURE)
        }
    }
}