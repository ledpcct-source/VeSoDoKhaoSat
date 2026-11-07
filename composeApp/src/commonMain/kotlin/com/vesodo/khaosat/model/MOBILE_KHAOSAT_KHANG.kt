package com.vesodo.khaosat.model

import kotlinx.serialization.Serializable

@Serializable
data class MOBILE_KHAOSAT_KHANG(
    var MA_YCAU_KNAI: String = "",
    var MA_DVIQLY: String = "",
    var TEN_KHANG_KS: String = "",
    var DIA_CHI: String = "",
    var DIEN_THOAI_KS: String = "",
    var MA_TRAM: String = "",
    var TENTRAM_BIENAP: String = "",
    var CONG_SUAT_TRAM: String = "",
    var GHI_CHU: String = ""
)
