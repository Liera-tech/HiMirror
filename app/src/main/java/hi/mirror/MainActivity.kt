package hi.mirror

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import hi.mirror.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        val handler = Handler()
//        val obtainMessage = handler.obtainMessage()

        // Example of a call to a native method
        binding.sampleText.text = stringFromJNI()
    }

    /**
     * A native method that is implemented by the 'mirror' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'mirror' library on application startup.
        init {
            System.loadLibrary("mirror")
        }
    }
}