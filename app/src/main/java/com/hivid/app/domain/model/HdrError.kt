package com.hivid.app.domain.model

sealed class HdrError(override val message: String) : Exception(message) {
    class UnsupportedDevice(reason: String) : HdrError("Device unsupported: $reason")
    class LowStorage(neededMb: Long, availableMb: Long) : HdrError("Insufficient storage space: $neededMb MB needed, $availableMb MB available")
    class DecoderFailed(reason: String) : HdrError("Video decoder failed: $reason")
    class EncoderFailed(reason: String) : HdrError("Hardware HDR encoder failed: $reason")
    class MuxerFailed(reason: String) : HdrError("Media muxer failed: $reason")
    object Cancelled : HdrError("Conversion cancelled by user")
}
