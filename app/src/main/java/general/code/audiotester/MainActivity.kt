package general.code.audiotester

import android.os.Bundle
import android.os.Handler
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnPreDraw
import liblayout.Builder

class MainActivity : AppCompatActivity() {
    var player: MP? = null
    lateinit var underruns: TextView
    lateinit var previousunderruns: TextView
    lateinit var framesperburst: TextView
    lateinit var buffersize: TextView
    lateinit var buffercapacity: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Builder(this)
            .row().column {
                UpdatingTextView(this).also {
                    it.addOnFirstDrawAction {
                        it.text = "underruns: 0"
                    }
                    it.addOnDrawAction {
                        it.text = "underruns: ${player!!.Oboe_underrunCount()}"
                    }
                }
            }
            .row().column {
                UpdatingTextView(this).also {
                    it.addOnFirstDrawAction {
                        it.text = "previous underruns: 0"
                    }
                    it.addOnDrawAction {
                        it.text = "previous underruns: ${player!!.Oboe_previousUnderrunCount()}"
                    }
                }
            }
            .row().column {
                UpdatingTextView(this).also {
                    it.addOnFirstDrawAction {
                        it.text = "frames per burst: 0"
                    }
                    it.addOnDrawAction {
                        it.text = "frames per burst: ${player!!.Oboe_framesPerBurst()}"
                    }
                }
            }
            .row().column {
                UpdatingTextView(this).also {
                    it.addOnFirstDrawAction {
                        it.text = "buffer size: 0"
                    }
                    it.addOnDrawAction {
                        it.text = "buffer size: ${player!!.Oboe_bufferSize()}"
                    }
                }
            }
            .row().column {
                UpdatingTextView(this).also {
                    it.addOnFirstDrawAction {
                        it.text = "buffer capacity: 0"
                    }
                    it.addOnDrawAction {
                        it.text = "buffer capacity: ${player!!.Oboe_bufferCapacity()}"
                    }
                }
            }
        .build()

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

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        player!!.destroy()
        super.onDestroy()
    }
}