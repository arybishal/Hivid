package com.hivid.app.media.gl

import android.opengl.*
import android.util.Log
import android.view.Surface

class EglCore {

    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglConfig: EGLConfig? = null

    val display: EGLDisplay get() = eglDisplay
    val context: EGLContext get() = eglContext

    init {
        initEGL()
    }

    private fun initEGL() {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
            throw RuntimeException("unable to get EGL14 display")
        }

        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            throw RuntimeException("unable to initialize EGL14")
        }

        // Try 10-bit RGBA 1010102 config first
        val attribList10Bit = intArrayOf(
            EGL14.EGL_RED_SIZE, 10,
            EGL14.EGL_GREEN_SIZE, 10,
            EGL14.EGL_BLUE_SIZE, 10,
            EGL14.EGL_ALPHA_SIZE, 2,
            EGL14.EGL_RENDERABLE_TYPE, EGLExt.EGL_OPENGL_ES3_BIT_KHR,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
            EGL14.EGL_NONE
        )

        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)

        val success10 = EGL14.eglChooseConfig(
            eglDisplay, attribList10Bit, 0,
            configs, 0, configs.size, numConfigs, 0
        )

        if (success10 && numConfigs[0] > 0 && configs[0] != null) {
            eglConfig = configs[0]
            Log.d(TAG, "Selected 10-bit EGLConfig (10:10:10:2)")
        } else {
            // Fallback to standard 8-bit RGBA8888 config
            val attribList8Bit = intArrayOf(
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGLExt.EGL_OPENGL_ES3_BIT_KHR,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                EGL14.EGL_NONE
            )
            val success8 = EGL14.eglChooseConfig(
                eglDisplay, attribList8Bit, 0,
                configs, 0, configs.size, numConfigs, 0
            )
            if (!success8 || numConfigs[0] == 0) {
                throw RuntimeException("Unable to find suitable EGLConfig for GLES3")
            }
            eglConfig = configs[0]
            Log.d(TAG, "Selected fallback 8-bit EGLConfig")
        }

        // Create GLES 3.0 context
        val contextAttribs = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 3,
            EGL14.EGL_NONE
        )

        eglContext = EGL14.eglCreateContext(
            eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT,
            contextAttribs, 0
        )
        checkEglError("eglCreateContext GLES3")
    }

    fun createWindowSurface(surface: Surface): EGLSurface {
        val extensions = EGL14.eglQueryString(eglDisplay, EGL14.EGL_EXTENSIONS) ?: ""
        val hasBt2020Pq = extensions.contains("EGL_EXT_gl_colorspace_bt2020_pq")

        val surfaceAttribs = if (hasBt2020Pq) {
            Log.d(TAG, "Enabling EGL_EXT_gl_colorspace_bt2020_pq window colorspace")
            intArrayOf(
                EGL_GL_COLORSPACE_KHR, EGL_GL_COLORSPACE_BT2020_PQ_EXT,
                EGL14.EGL_NONE
            )
        } else {
            intArrayOf(EGL14.EGL_NONE)
        }

        val eglSurface = EGL14.eglCreateWindowSurface(
            eglDisplay, eglConfig, surface,
            surfaceAttribs, 0
        )
        checkEglError("eglCreateWindowSurface")
        if (eglSurface == EGL14.EGL_NO_SURFACE) {
            throw RuntimeException("surface was null")
        }
        return eglSurface
    }

    fun makeCurrent(eglSurface: EGLSurface) {
        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            throw RuntimeException("eglMakeCurrent failed")
        }
    }

    fun makeNothingCurrent() {
        EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
    }

    fun swapBuffers(eglSurface: EGLSurface): Boolean {
        return EGL14.eglSwapBuffers(eglDisplay, eglSurface)
    }

    fun setPresentationTime(eglSurface: EGLSurface, nsecs: Long) {
        EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, nsecs)
    }

    fun releaseSurface(eglSurface: EGLSurface) {
        if (eglSurface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglDestroySurface(eglDisplay, eglSurface)
        }
    }

    fun release() {
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(eglDisplay)
        }
        eglDisplay = EGL14.EGL_NO_DISPLAY
        eglContext = EGL14.EGL_NO_CONTEXT
        eglConfig = null
    }

    private fun checkEglError(msg: String) {
        val error = EGL14.eglGetError()
        if (error != EGL14.EGL_SUCCESS) {
            Log.e(TAG, "$msg: EGL error: 0x" + Integer.toHexString(error))
        }
    }

    companion object {
        private const val TAG = "EglCore"
        private const val EGL_GL_COLORSPACE_KHR = 0x309D
        private const val EGL_GL_COLORSPACE_BT2020_PQ_EXT = 0x3340
    }
}
