package com.hivid.app.media.gl

import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class HdrRenderProgram {

    private var programId: Int = 0
    private var uMVPMatrixLoc: Int = -1
    private var uTexMatrixLoc: Int = -1
    private var uExpansionLoc: Int = -1
    private var aPositionLoc: Int = -1
    private var aTextureCoordLoc: Int = -1

    private val vertexBuffer: FloatBuffer
    private val texCoordBuffer: FloatBuffer

    init {
        val quadCoords = floatArrayOf(
            -1.0f, -1.0f,
             1.0f, -1.0f,
            -1.0f,  1.0f,
             1.0f,  1.0f
        )
        val texCoords = floatArrayOf(
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f
        )

        vertexBuffer = ByteBuffer.allocateDirect(quadCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(quadCoords)
        vertexBuffer.position(0)

        texCoordBuffer = ByteBuffer.allocateDirect(texCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(texCoords)
        texCoordBuffer.position(0)

        compileShaders()
    }

    private fun compileShaders() {
        val vertexShaderSource = """
            #version 300 es
            layout(location = 0) in vec4 aPosition;
            layout(location = 1) in vec4 aTextureCoord;
            uniform mat4 uMVPMatrix;
            uniform mat4 uTexMatrix;
            out vec2 vTextureCoord;

            void main() {
                gl_Position = uMVPMatrix * aPosition;
                vTextureCoord = (uTexMatrix * aTextureCoord).xy;
            }
        """.trimIndent()

        val fragmentShaderSource = """
            #version 300 es
            #extension GL_OES_EGL_image_external_essl3 : require
            precision highp float;

            uniform samplerExternalOES uTexture;
            uniform float uExpansion; // 0.0 to 1.0
            in vec2 vTextureCoord;
            out vec4 fragColor;

            // Step A: Convert Rec.709 / sRGB transfer to linear light (0.0 to 1.0)
            vec3 rec709ToLinear(vec3 c) {
                // Piecewise sRGB / Rec.709 EOTF
                bvec3 cutoff = lessThan(c, vec3(0.04045));
                vec3 low = c / 12.92;
                vec3 high = pow((c + vec3(0.055)) / 1.055, vec3(2.4));
                return mix(high, low, cutoff);
            }

            // Step B: Inverse tone mapping with expansion parameter
            vec3 expandHighlights(vec3 linearColor, float expansion) {
                // Perceptual Rec.709 luminance
                float Y = dot(linearColor, vec3(0.2126, 0.7152, 0.0722));
                
                // Soft shoulder expansion: lower luminance (shadows and skin tones below 0.45)
                // remain untouched, preserving natural contrast without clipping.
                float shoulder = smoothstep(0.40, 1.0, Y);
                
                // Expansion multiplier: baseline 1.0 up to 10.0x for 1000 nits specular peak
                float boost = 1.0 + (expansion * 8.5) * (shoulder * shoulder);
                return linearColor * boost;
            }

            // Step C: Color gamut transformation matrix from Rec.709 to BT.2020
            const mat3 M_709_TO_2020 = mat3(
                0.6274040, 0.0690970, 0.0163916,
                0.3292820, 0.9195400, 0.0880132,
                0.0433136, 0.0113615, 0.8955952
            );

            // Step D: SMPTE ST 2084 (Perceptual Quantizer / PQ) OETF
            // L is absolute normalized luminance where 1.0 = 10,000 nits.
            // For SDR baseline (100 nits), L = 100 / 10000 = 0.01.
            vec3 linearToPQ(vec3 L) {
                L = max(L, vec3(0.0));
                const float c1 = 0.8359375;        // 107/128
                const float c2 = 18.8515625;      // 2413/128
                const float c3 = 18.6875;         // 2392/128
                const float m1 = 0.1593017578125; // 2610/16384
                const float m2 = 78.84375;        // 2523/32

                vec3 L_m1 = pow(L, vec3(m1));
                vec3 num = c1 + c2 * L_m1;
                vec3 den = vec3(1.0) + c3 * L_m1;
                return pow(num / den, vec3(m2));
            }

            void main() {
                // Sample external OES texture
                vec4 srcColor = texture(uTexture, vTextureCoord);
                
                // 1. Rec.709 SDR to Linear
                vec3 linearCol = rec709ToLinear(clamp(srcColor.rgb, 0.0, 1.0));
                
                // 2. Inverse Tone Mapping (expand highlights)
                vec3 expandedCol = expandHighlights(linearCol, uExpansion);
                
                // 3. Primaries conversion: Rec.709 -> BT.2020
                vec3 bt2020Col = max(M_709_TO_2020 * expandedCol, vec3(0.0));
                
                // 4. Scale to PQ absolute luminance (100 nits SDR = 0.01 in PQ)
                vec3 pqLuminance = bt2020Col * 0.01;
                
                // 5. Apply PQ transfer function
                vec3 pqEncoded = linearToPQ(pqLuminance);
                
                fragColor = vec4(clamp(pqEncoded, 0.0, 1.0), 1.0);
            }
        """.trimIndent()

        val vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderSource)
        val fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderSource)

        programId = GLES30.glCreateProgram()
        GLES30.glAttachShader(programId, vertexShader)
        GLES30.glAttachShader(programId, fragmentShader)
        GLES30.glLinkProgram(programId)

        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES30.GL_TRUE) {
            val error = GLES30.glGetProgramInfoLog(programId)
            GLES30.glDeleteProgram(programId)
            throw RuntimeException("Could not link HDR program: $error")
        }

        uMVPMatrixLoc = GLES30.glGetUniformLocation(programId, "uMVPMatrix")
        uTexMatrixLoc = GLES30.glGetUniformLocation(programId, "uTexMatrix")
        uExpansionLoc = GLES30.glGetUniformLocation(programId, "uExpansion")
        aPositionLoc = 0
        aTextureCoordLoc = 1
    }

    fun draw(
        textureId: Int,
        mvpMatrix: FloatArray,
        texMatrix: FloatArray,
        expansion: Float // 0.0 to 1.0
    ) {
        GLES30.glUseProgram(programId)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)

        GLES30.glUniformMatrix4fv(uMVPMatrixLoc, 1, false, mvpMatrix, 0)
        GLES30.glUniformMatrix4fv(uTexMatrixLoc, 1, false, texMatrix, 0)
        GLES30.glUniform1f(uExpansionLoc, expansion.coerceIn(0f, 1f))

        GLES30.glEnableVertexAttribArray(aPositionLoc)
        GLES30.glVertexAttribPointer(aPositionLoc, 2, GLES30.GL_FLOAT, false, 8, vertexBuffer)

        GLES30.glEnableVertexAttribArray(aTextureCoordLoc)
        GLES30.glVertexAttribPointer(aTextureCoordLoc, 2, GLES30.GL_FLOAT, false, 8, texCoordBuffer)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

        GLES30.glDisableVertexAttribArray(aPositionLoc)
        GLES30.glDisableVertexAttribArray(aTextureCoordLoc)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        GLES30.glUseProgram(0)
    }

    fun release() {
        if (programId != 0) {
            GLES30.glDeleteProgram(programId)
            programId = 0
        }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, shaderCode)
        GLES30.glCompileShader(shader)

        val compiled = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            val error = GLES30.glGetShaderInfoLog(shader)
            GLES30.glDeleteShader(shader)
            throw RuntimeException("Could not compile shader $type: $error")
        }
        return shader
    }
}
