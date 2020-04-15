package general.code.audiotester

import android.content.Context
import android.content.res.AssetManager
import android.media.AudioManager
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import java.io.*

class MainActivity : AppCompatActivity() {
    var player: MP? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // load Player

        System.loadLibrary("PlayerOboe")

        // create Player instance

        player = MP(this)

        // initialize Player

        player!!.init()

        // load an audio track

        player!!.loadMediaAsset("00001313_48000.raw")

        // loop the audio track

        player!!.loop(true)
    }

    override fun onStart() {
        super.onStart()
        player!!.play()
    }

    override fun onDestroy() {
        player!!.destroy()
        super.onDestroy()
    }
}