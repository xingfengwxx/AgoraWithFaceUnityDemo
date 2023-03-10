package com.wangxingxing.agorawithfaceunitydemo

import android.app.Application
import android.util.Log
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.Utils
import com.faceunity.nama.FURenderer
import io.agora.rtc2.Constants
import io.agora.rtc2.RtcEngine

/**
 * author : 王星星
 * date : 2023/2/22 17:02
 * email : 1099420259@qq.com
 * description :
 */
class App : Application() {

    private lateinit var mRtcEngine: RtcEngine
    private lateinit var mRtcEventHandler: RtcEngineEventHandlerProxy

    override fun onCreate() {
        super.onCreate()

        initLog()
        Utils.init(this)

        initRtcEngine()
        initVideoCaptureAsync()
    }

    private fun initRtcEngine() {
        mRtcEventHandler = RtcEngineEventHandlerProxy()
        try {
            mRtcEngine = RtcEngine.create(this, AGORA_APP_ID, mRtcEventHandler)
            mRtcEngine.enableVideo()
            mRtcEngine.registerVideoFrameObserver(PreprocessorFaceUnity.getInstance())
            mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
        } catch (e: Exception) {
            throw RuntimeException(
                """
                NEED TO check rtc sdk init fatal error
                ${Log.getStackTraceString(e)}
                """.trimIndent()
            )
        }
    }

    private fun initVideoCaptureAsync() {
        Thread {
            val application = applicationContext
            FURenderer.getInstance().setup(application)
        }.start()
    }

    fun rtcEngine(): RtcEngine? {
        return mRtcEngine
    }

    fun addRtcHandler(handler: RtcEngineEventHandler?) {
        mRtcEventHandler.addEventHandler(handler)
    }

    fun removeRtcHandler(handler: RtcEngineEventHandler?) {
        mRtcEventHandler.removeEventHandler(handler)
    }

    private fun initLog() {
        LogUtils.getConfig()
            .setLogSwitch(BuildConfig.DEBUG)
            .setGlobalTag("wxx")
            .setBorderSwitch(true)
    }

    companion object {
        const val AGORA_APP_ID = "a0c47ef0486940acabbdf813b417cbdf"
    }
}