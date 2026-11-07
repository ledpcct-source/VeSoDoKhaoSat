package com.vesodo.khaosat.map

expect object PlatformTileFetcher {
    suspend fun fetchTileBytes(url: String): ByteArray?
    suspend fun fetchText(url: String, postBody: String? = null): String?
}
