package com.vesodo.khaosat.model

import kotlinx.serialization.Serializable

@Serializable
data class Obj_MAU_CANVAS(
    var MA_DVIQLY: String = "",
    var id_MAU: Int = 0,
    var TEN_MAU: String = "",
    var LENH: Int = 0,
    var X1: Float = 0f,
    var Y1: Float = 0f,
    var X2: Float = 0f,
    var Y2: Float = 0f,
    var X3: Float = 0f,
    var Y3: Float = 0f,
    var CHU: String = "",
    var HINH: Int = 0,
    var THUOC_TINH: Int = 0,
    var DONG_BO: Int = 0
)
