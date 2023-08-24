package com.example.androidstt

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.example.androidstt.databinding.ActivityMainBinding
import com.example.androidstt.ui.home.HomeFragment
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*


class MainActivity : AppCompatActivity(), RecognitionListener, TextToSpeech.OnInitListener {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (!it) {
                Toast.makeText(
                    this,
                    "권한이 없으면 해당 서비스를 사용하실 수 없습니다.",
                    Toast.LENGTH_SHORT
                ).show()

                finish()
            }
        }

    private val speechRecognizer by lazy { SpeechRecognizer.createSpeechRecognizer(this) }
    private var speechRecognizerIntent: Intent? = null

    private lateinit var textToSpeech: TextToSpeech
    private val textToSpeechReady = MutableLiveData(false)

    private var job: Job? = null


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        speechRecognizer.setRecognitionListener(this)
        textToSpeech = TextToSpeech(this, this)

        binding.micButton.setOnClickListener {
            if (binding.micButton.isSelected) return@setOnClickListener

            startSpeechRecognizer()
            it.isSelected = true

            job?.cancel()
            job = lifecycleScope.launch {
                delay(10000)
                stopSpeechRecognizer()
            }
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, HomeFragment())
            .commit()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startSpeechRecognizer() {
        if (speechRecognizerIntent != null) return

        if (textToSpeechReady.value == true && textToSpeech.isSpeaking) {
            textToSpeech.stop()
        }
        // start 2023.08.20. botbinoo
        cleanWavBuffer()
        // end 2023.08.20. botbinoo

        speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREA)
        }

        speechRecognizer.startListening(speechRecognizerIntent)
    }

    private fun stopSpeechRecognizer() {
        if (speechRecognizerIntent == null) return

        speechRecognizer.stopListening()
        speechRecognizerIntent = null

        binding.micButton.isSelected = false
        job?.cancel()

        // start 2023.08.20. botbinoo
        makeWav()
        // end 2023.08.20. botbinoo
    }

    fun speakOut(text: String?) {
        textToSpeechReady.observe(this, object : androidx.lifecycle.Observer<Boolean> {
            override fun onChanged(t: Boolean?) {
                if (t == true) {
                    textToSpeechReady.removeObserver(this)

                    if (textToSpeech.isSpeaking) {
                        textToSpeech.stop()
                    }

                    if (text == null) return

                    textToSpeech.speak(
                        text,
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        UUID.randomUUID().toString()
                    )
                }
            }
        })
    }

    override fun onDestroy() {
        textToSpeech.stop()
        textToSpeech.shutdown()

        speechRecognizer.destroy()

        super.onDestroy()
    }

    //region STT
    override fun onReadyForSpeech(params: Bundle?) {
        Log.d("MainActivity", "onReadyForSpeech")
    }

    override fun onBeginningOfSpeech() {
        Log.d("MainActivity", "onBeginningOfSpeech")
        job?.cancel()
    }

    override fun onRmsChanged(rmsdB: Float) {
        // Log.d("MainActivity", "onRmsChanged")
    }

    override fun onBufferReceived(buffer: ByteArray?) {
        Log.d("MainActivity", "onBufferReceived")

        // start 2023.08.20. botbinoo
        saveRawWavByBuffer(buffer)
        // end 2023.08.20. botbinoo
    }
    // start 2023.08.20. botbinoo
    private var wavBuffer: ByteArray? = null
    private var wavFileName: String? = null

    fun cleanWavBuffer(){
        wavBuffer = null;
        wavFileName = null;
    }
    fun saveRawWavByBuffer(buffer: ByteArray?){
        if(wavBuffer == null || wavFileName == null) {
            wavFileName = SystemClock.currentThreadTimeMillis().toString() + ".wav"
            wavBuffer = ByteArray(0)
        }

        wavBuffer = buffer?.let { wavBuffer?.plus(it) }
    }
    fun wavHeader(): ByteArray{
        val littleBytes: ByteArray = ByteBuffer
                .allocate(14)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putShort(AudioFormat.CHANNEL_IN_MONO.toShort())
            .putInt(44100)
            .putInt(44100 * AudioFormat.CHANNEL_IN_MONO.toShort() * (AudioFormat.ENCODING_PCM_16BIT / 8))
            .putShort((AudioFormat.CHANNEL_IN_MONO.toShort() * (AudioFormat.ENCODING_PCM_16BIT / 8)).toShort())
            .putShort(AudioFormat.ENCODING_PCM_16BIT.toShort())
            .array();

        var tmpBytes: ByteArray = ByteArray(44)
        val arr: Array<Byte> = arrayOf(
            'R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte(), // Chunk ID
            0, 0, 0, 0, // Chunk Size (나중에 업데이트 될것)
            'W'.code.toByte(), 'A'.code.toByte(), 'V'.code.toByte(), 'E'.code.toByte(), // Format
            'f'.code.toByte(), 'm'.code.toByte(), 't'.code.toByte(), ' '.code.toByte(), //Chunk ID
            16, 0, 0, 0, // Chunk Size
            1, 0, // AudioFormat
            littleBytes[0], littleBytes[1], // Num of Channels
            littleBytes[2], littleBytes[3], littleBytes[4], littleBytes[5], // SampleRate
            littleBytes[6], littleBytes[7], littleBytes[8], littleBytes[9], // Byte Rate
            littleBytes[10], littleBytes[11], // Block Align
            littleBytes[12], littleBytes[13], // Bits Per Sample
            'd'.code.toByte(), 'a'.code.toByte(), 't'.code.toByte(), 'a'.code.toByte(), // Chunk ID
            0, 0, 0, 0
        )
        arr.forEach {
            tmpBytes.plus(it)
        }

        return tmpBytes
    }
    fun makeWav(){
        val wavFilePath: File = File(Environment.getExternalStorageDirectory().absolutePath + wavFileName)
        wavFilePath.mkdirs()
        val outputStream: FileOutputStream = FileOutputStream(wavFilePath)

        outputStream.write(wavHeader())
        outputStream.write(wavBuffer, 0, wavBuffer!!.size)
        outputStream.flush()
        outputStream.close()

        val sizes = ByteBuffer
            .allocate(8)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt((wavFilePath.length() - 8) as Int)
            .putInt((wavFilePath.length() - 44) as Int)
            .array()
        val pcmRdFile:RandomAccessFile = RandomAccessFile(wavFilePath, "rw")
        pcmRdFile.seek(4)
        pcmRdFile.write(sizes, 0, 4)
        pcmRdFile.seek(40)
        pcmRdFile.write(sizes, 40, 4)
        pcmRdFile.close()
    }
    // end 2023.08.20. botbinoo

    override fun onEndOfSpeech() {
        Log.d("MainActivity", "onEndOfSpeech")

        stopSpeechRecognizer()
    }

    override fun onError(error: Int) {
        val message: String = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "오디오 에러"
            SpeechRecognizer.ERROR_CLIENT -> "클라이언트 에러"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "퍼미션 없음"
            SpeechRecognizer.ERROR_NETWORK -> "네트워크 에러"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "네트웍 타임아웃"
            SpeechRecognizer.ERROR_NO_MATCH -> "찾을 수 없음"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RECOGNIZER가 바쁨"
            SpeechRecognizer.ERROR_SERVER -> "서버가 이상함"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "말하는 시간초과"
            else -> "알 수 없는 오류임"
        }

        Log.e("MainActivity", "onError: $message")
    }

    override fun onResults(results: Bundle?) {
        Log.d("MainActivity", "onResults")

        if (results == null) return
        supportFragmentManager.setFragmentResult(SpeechRecognizer.RESULTS_RECOGNITION, results)
    }

    override fun onPartialResults(partialResults: Bundle?) {
        Log.d("MainActivity", "onPartialResults")
    }

    override fun onEvent(eventType: Int, params: Bundle?) {
        Log.d("MainActivity", "onEvent")
    }
    //endregion

    //region TTS
    override fun onInit(status: Int) {
        Log.d("MainActivity", "onInit")

        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale.KOREA)
            if (result == TextToSpeech.LANG_MISSING_DATA
                || result == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                Log.e("MainActivity", "This Language is not supported")
            } else {
                textToSpeechReady.postValue(true)
            }
        } else {
            Log.e("MainActivity", "Initialization Failed!")
        }
    }
    //endregion



}
