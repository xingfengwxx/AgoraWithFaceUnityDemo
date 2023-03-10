package com.wangxingxing.agorawithfaceunitydemo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.LogUtils
import com.faceunity.core.enumeration.FUAIProcessorEnum
import com.faceunity.nama.FURenderer
import com.faceunity.nama.data.FaceUnityDataFactory
import com.faceunity.nama.listener.FURendererListener
import com.wangxingxing.agorawithfaceunitydemo.databinding.ActivityMainBinding
import io.agora.rtc2.Constants
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.video.VideoEncoderConfiguration

class MainActivity : RtcBasedActivity(), RtcEngineEventHandler, SensorEventListener {

    private val PERMISSION_REQ_ID = 22
    private val REQUESTED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )

    private lateinit var binding: ActivityMainBinding

    private var preprocessor: PreprocessorFaceUnity? = null

    private var mFaceUnityDataFactory: FaceUnityDataFactory? = null


    private var mRemoteUid = -1
    private var mSensorManager: SensorManager? = null
    private val mFURenderer = FURenderer.getInstance()

    //默认本地大窗预览
    private var mLocalViewSmall = false

    private var textureViewLocal: TextureView? = null
    private var surfaceViewRemote: SurfaceView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (checkPermission()) {
            initUI()
            initRoom()
            mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            mFURenderer.bindListener(mFURendererListener)
            val sdkVersion = RtcEngine.getSdkVersion()
            Log.i(TAG, "onCreate: agora sdk version $sdkVersion")
        }
    }

    private fun initUI() {
//        initRemoteViewLayout()

        binding.btnSwitchCamera.setOnClickListener {
            preprocessor!!.skipFrame()
            rtcEngine().switchCamera()
        }

        binding.localVideoView.setOnClickListener {
            if (binding.fuView.isShown) {
                binding.fuView.visibility = View.GONE
            } else {
                binding.fuView.visibility = View.VISIBLE
            }
        }

        binding.btnChange.setOnClickListener {
            changeLocalRemoteView()
        }
    }

    private fun initRoom() {
        preprocessor = PreprocessorFaceUnity.getInstance()
        mFaceUnityDataFactory = FaceUnityDataFactory(0)
        binding.fuView.bindDataFactory(mFaceUnityDataFactory)
//        val localView: FrameLayout = findViewById(R.id.local_video_view)
        // Create render view by RtcEngine
        textureViewLocal = TextureView(this)
        // Add to the local container
        binding.localVideoView.addView(
            textureViewLocal,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        // Setup local video to render your local camera preview
        rtcEngine().setupLocalVideo(VideoCanvas(textureViewLocal, VideoCanvas.RENDER_MODE_HIDDEN, 0))
        joinChannel()
    }

    private fun joinChannel() {
        rtcEngine().setVideoEncoderConfiguration(
            VideoEncoderConfiguration(
                VideoEncoderConfiguration.VD_640x360,
                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_10,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT
            )
        )
        rtcEngine().setClientRole(Constants.CLIENT_ROLE_BROADCASTER)
        rtcEngine().enableLocalAudio(false)
        rtcEngine().startPreview()

        //TOKEN需要在有效期内才能看到对方画面
        rtcEngine().joinChannel(TOKEN, CHANNEL_NAME, null, 0)
    }

    private fun initRemoteViewLayout() {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val params = binding.remoteVideoView.getLayoutParams() as RelativeLayout.LayoutParams

        params.width = displayMetrics.widthPixels / 3
        params.height = displayMetrics.heightPixels / 3
        binding.remoteVideoView.setLayoutParams(params)
    }

    private fun onRemoteUserLeft() {
        mRemoteUid = -1
        removeRemoteView()
    }

    private fun removeRemoteView() {
        binding.remoteVideoView.removeAllViews()
    }

    private fun setRemoteVideoView(uid: Int) {
        surfaceViewRemote = RtcEngine.CreateRendererView(this)
        rtcEngine().setupRemoteVideo(
            VideoCanvas(
                surfaceViewRemote, VideoCanvas.RENDER_MODE_HIDDEN, uid
            )
        )
        binding.remoteVideoView.addView(surfaceViewRemote)
    }

    /**
     * FURenderer状态回调
     */
    private val mFURendererListener: FURendererListener = object : FURendererListener {
        override fun onTrackStatusChanged(type: FUAIProcessorEnum, status: Int) {
            runOnUiThread {
                binding.ivFaceDetect.text =
                    if (type == FUAIProcessorEnum.FACE_PROCESSOR) "未检测到人脸" else "未检测到人体"
                binding.ivFaceDetect.visibility = if (status > 0) View.INVISIBLE else View.VISIBLE
            }
        }

        override fun onFpsChanged(fps: Double, callTime: Double) {}
    }

    override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
        Log.i(
            TAG,
            "onJoinChannelSuccess " + channel + " " + (uid.toLong() and 0xFFFFFFFFL)
        )
    }

    override fun onUserOffline(uid: Int, reason: Int) {
        runOnUiThread { this.onRemoteUserLeft() }
    }

    override fun onUserJoined(uid: Int, elapsed: Int) {
        Log.i(TAG, "onUserJoined " + (uid.toLong() and 0xFFFFFFFFL))
    }

    override fun onRemoteVideoStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {
        Log.i(
            TAG,
            "onRemoteVideoStateChanged " + (uid.toLong() and 0xFFFFFFFFL) + " " + state + " " + reason
        )
        if (mRemoteUid == -1 && state == Constants.REMOTE_VIDEO_STATE_STARTING) {
            runOnUiThread {
                mRemoteUid = uid
                setRemoteVideoView(uid)
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event!!.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            if (Math.abs(x) > 3 || Math.abs(y) > 3) {
                if (Math.abs(x) > Math.abs(y)) {
                    mFURenderer.deviceOrientation = if (x > 0) 0 else 180
                } else {
                    mFURenderer.deviceOrientation = if (y > 0) 90 else 270
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onResume() {
        super.onResume()
        val sensor = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mSensorManager!!.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        preprocessor!!.setRenderEnable(true)
    }

    override fun onPause() {
        super.onPause()
        preprocessor!!.setRenderEnable(false)
        mSensorManager!!.unregisterListener(this)
    }

    override fun onDestroy() {
        rtcEngine().leaveChannel()
        super.onDestroy()
    }

    private fun checkPermission(): Boolean {
        return (checkSelfPermission(
            REQUESTED_PERMISSIONS.get(0),
            PERMISSION_REQ_ID
        ) &&
                checkSelfPermission(
                    REQUESTED_PERMISSIONS.get(1),
                    PERMISSION_REQ_ID
                )
                )
    }

    private fun checkSelfPermission(permission: String, requestCode: Int): Boolean {
        if (ContextCompat.checkSelfPermission(this, permission) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, requestCode)
            return false
        }
        return true
    }

    private fun changeLocalRemoteView() {
        mLocalViewSmall = !mLocalViewSmall
        LogUtils.i("mLocalViewSmall=$mLocalViewSmall")
        if (mLocalViewSmall) {
            //本地是小窗预览时
            binding.localVideoView.removeView(textureViewLocal)
            binding.remoteVideoView.removeView(surfaceViewRemote)

            binding.localVideoView.removeAllViews()
            binding.localVideoView.addView(surfaceViewRemote)

            binding.remoteVideoView.removeAllViews()
            binding.remoteVideoView.addView(textureViewLocal)
        } else {
            binding.localVideoView.removeView(surfaceViewRemote)
            binding.remoteVideoView.removeView(textureViewLocal)

            binding.localVideoView.removeAllViews()
            binding.localVideoView.addView(textureViewLocal)

            binding.remoteVideoView.removeAllViews()
            binding.remoteVideoView.addView(surfaceViewRemote)

        }
    }

    companion object {
        const val TAG = "wxx"

        const val CHANNEL_NAME = "test"
        const val TOKEN =
            "007eJxTYIjp5f8Ws+ZuobxpZNgivzW1z0wK81/NP/G/K1Wh2Hpbl7gCQ6JBsol5apqBiYWZpYlBYnJiUlJKmoWhcZKJoXkykDn1LldKQyAjQ5KpHisjAwSC+CwMJanFJQwMAGISH4o="
    }


}