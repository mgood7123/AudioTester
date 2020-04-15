package general.code.audiotester

import android.app.Activity
import android.content.Context
import android.content.res.AssetManager
import android.media.AudioManager
import android.util.Log
import android.widget.Toast
import java.io.*

class MP (private val activity: Activity) {

    private external fun Oboe_Init(sampleRate: Int, framesPerBurst: Int)
    private external fun Oboe_LoadTrackFromAssets(asset: String, assetManager: AssetManager)
    private external fun Oboe_Play()
    private external fun Oboe_Pause()
    private external fun Oboe_Stop()
    private external fun Oboe_Loop(value: Boolean)
    private external fun Oboe_Looper(start: Double, end: Double, timing: Int)
    private external fun Oboe_Cleanup()
    private external fun Oboe_SampleIndex(index: Long): Short
    private external fun Oboe_SampleCount(): Long
    private external fun Oboe_SampleRate(): Int
    private external fun Oboe_ChannelCount(): Int

    val LOG_TAG = "AUDIO_TESTER_APK"

    lateinit var currentTemporyMediaFilesDirectory: String
    lateinit var audioManager: AudioManager
    private val focusManager = FocusManager()

    var audioManagerObtained = false
    lateinit var ASSETS: String

    fun `init`(): MP {
        ASSETS = activity.filesDir.absolutePath + "/ASSETS"

        Log.i(LOG_TAG, "copying assets folder")

        assetsManager().copyAssetFolder(activity.assets)

        audioManager = activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManagerObtained = true;

        // library is loaded at application startup
        Oboe_Init(
            audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE).toInt(),
            audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER).toInt()
        )
        return this
    }

    fun loadMediaAsset(asset: String): MP {
        Oboe_LoadTrackFromAssets(asset, activity.assets)
        return this
    }

    fun loop(value: Boolean): MP {
        Oboe_Loop(value)
        return this
    }

    fun play(): MP {
        focusManager.request()
        Oboe_Play()
        return this
    }

    fun pause(): MP {
        focusManager.release()
        Oboe_Pause()
        return this
    }

    fun destroy(): MP {
        focusManager.release()
        Oboe_Cleanup()
        return this
    }

    inner class assetsManager {

        fun copyAssetFolder(assetManager: AssetManager): Boolean = copyAssetFolder(assetManager, null, ASSETS)

        fun copyAssetFolder(
            assetManager: AssetManager,
            toPath: String
        ): Boolean = copyAssetFolder(assetManager, null, toPath)

        fun copyAssetFolder(
            assetManager: AssetManager,
            fromAssetPath: String?,
            toPath: String
        ): Boolean {
            try {
                val files: Array<String>? = assetManager.list(if (fromAssetPath.isNullOrBlank()) "" else fromAssetPath)
                if (files == null) return false else if (files.isEmpty()) return false
                Log.i(LOG_TAG, "obtained a list of assets")
                File(toPath).mkdirs()
                var res = true
                for (file in files)
                    if (file.contains("."))
                        res = res and copyAsset(
                            assetManager,
                            if (fromAssetPath.isNullOrBlank()) file else "$fromAssetPath/$file",
                            "$toPath/$file"
                        )
                    else
                        res = res and copyAssetFolder(
                            assetManager,
                            if (fromAssetPath.isNullOrBlank()) file else "$fromAssetPath/$file",
                            "$toPath/$file"
                        )
                return res
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }

        }

        fun copyAsset(
            assetManager: AssetManager,
            fromAssetPath: String, toPath: String
        ): Boolean {
            var `in`: InputStream? = null
            var out: OutputStream? = null
            try {
                Log.i(LOG_TAG, "copying \"$fromAssetPath\" to \"$toPath\"")
                `in` = assetManager.open(fromAssetPath)
                File(toPath).createNewFile()
                out = FileOutputStream(toPath)
                copyFile(`in`!!, out)
                `in`.close()
                `in` = null
                out.flush()
                out.close()
                out = null
                return true
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
        }

        @Throws(IOException::class)
        fun copyFile(`in`: InputStream, out: OutputStream) {
            val buffer = ByteArray(1024)
            var read = `in`.read(buffer)
            while (read != -1) {
                out.write(buffer, 0, read)
                read = `in`.read(buffer)
            }
        }
    }

    inner class FocusManager {
        private var mAudioFocusChangeListener: AudioFocusChangeListenerImpl? = null
        var hasFocus: Boolean = false
        private var focusChanged: Boolean = false
        private val TAG = "FocusManager"

        fun request(): Boolean {
            if (hasFocus) return true
            mAudioFocusChangeListener = AudioFocusChangeListenerImpl()
            val result = audioManager.requestAudioFocus(
                mAudioFocusChangeListener,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN
            )

            when (result) {
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> hasFocus = true
                AudioManager.AUDIOFOCUS_REQUEST_FAILED -> hasFocus = false
            }

            val message = "Focus request " + if (hasFocus) "granted" else "failed"
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
            Log.i(TAG, message)
            return hasFocus
        }

        fun release(): Boolean {
            if (!hasFocus) return true
            val result = audioManager.abandonAudioFocus(mAudioFocusChangeListener)
            when (result) {
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> hasFocus = false
                AudioManager.AUDIOFOCUS_REQUEST_FAILED -> hasFocus = true
            }
            val message = "Abandon focus request " + if (!hasFocus) "granted" else "failed"
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
            Log.i(TAG, message)
            return !hasFocus
        }

        private inner class AudioFocusChangeListenerImpl : AudioManager.OnAudioFocusChangeListener {

            override fun onAudioFocusChange(focusChange: Int) {
                focusChanged = true
                Log.i(TAG, "Focus changed")

                when (focusChange) {
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        Log.i(TAG, "AUDIOFOCUS_GAIN")
                        Toast.makeText(activity, "Focus GAINED", Toast.LENGTH_LONG).show()
                        play()
                    }
                    AudioManager.AUDIOFOCUS_LOSS -> {
                        Log.i(TAG, "AUDIOFOCUS_LOSS")
                        Toast.makeText(activity, "Focus LOST", Toast.LENGTH_LONG).show()
                        pause()
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        Log.i(TAG, "AUDIOFOCUS_LOSS_TRANSIENT")
                        Toast.makeText(activity, "Focus LOST TRANSIENT", Toast.LENGTH_LONG).show()
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                        Log.i(TAG, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK")
                        Toast.makeText(activity, "Focus LOST TRANSIENT CAN DUCK", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}