package com.vesodo.khaosat.model

import kotlinx.serialization.Serializable

@Serializable
data class Obj_CANVAS(
    var MA_DVIQLY: String = "",
    var MA_YCAU_KNAI: String = "",
    var STT: Long = 0L,
    var TEN: String = "",
    var LENH: Int = 0,
    var X1: Float = 0f,
    var Y1: Float = 0f,
    var X2: Float = 0f,
    var Y2: Float = 0f,
    var X3: Float = 0f,
    var Y3: Float = 0f,
    var HINH: Int = 0,
    var THUOC_TINH: Int = 0,
    var CHON: Int = 0,
    var ROTATION: Float = 0f,
    var SCALE: Float = 1f
)
