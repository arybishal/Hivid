package com.hivid.app.domain.model

enum class PipelineStage(val displayName: String) {
    INITIALIZATION("Pipeline Initialization"),
    SOURCE_ANALYSIS("Source Video Analysis"),
    CAPABILITY_PROBE("Hardware Capability Detection"),
    DECODER_INIT("Video Decoder Initialization"),
    EGL_INIT("OpenGL ES & EGL Initialization"),
    SHADER_COMPILE("HDR Shader Compilation"),
    ENCODER_CONFIG("HEVC Main10 Encoder Configuration"),
    SURFACE_BINDING("Surface Binding & Texture Setup"),
    FRAME_PROCESSING("GPU Frame Processing & PQ Encoding"),
    AUDIO_PASSTHROUGH("Audio Track Passthrough"),
    MUXER_INIT("MediaMuxer Track Configuration"),
    MUXER_WRITE("MediaMuxer Sample Writing"),
    MUXER_FINALIZE("MediaMuxer Stop & Close"),
    MEDIASTORE_SAVE("MediaStore Gallery Publishing"),
    CONTAINER_REMUX("Lossless Container Remuxing")
}
