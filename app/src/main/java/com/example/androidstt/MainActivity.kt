package com.example.androidstt

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.RecognizerResultsIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.arthenica.ffmpegkit.*
import com.example.androidstt.databinding.ActivityMainBinding
import com.example.androidstt.retrofit.FlaskFilterServiceImpl
import com.example.androidstt.ui.home.HomeFragment
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.*
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

        speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREA)
            // 구글 stt - audio/amr 로 저장시키기
//            putExtra(RecognizerIntent.EXTRA_ORIGIN, true)
            putExtra("android.speech.extra.GET_AUDIO_FORMAT", "audio/AMR");
            putExtra("android.speech.extra.GET_AUDIO", true);

        }

        //
//        speechRecognizer.startListening(speechRecognizerIntent)
        recognitionIntentForResult.launch(speechRecognizerIntent)
        //
    }

    private fun stopSpeechRecognizer() {
        if (speechRecognizerIntent == null) return

        speechRecognizer.stopListening()
        speechRecognizerIntent = null

        binding.micButton.isSelected = false
        job?.cancel()
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
    }
    // wav 파일 저장시키기

    private val recognitionIntentForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result -> when(result.resultCode) {
                RESULT_OK -> {
                    val uri: Uri? = result.data?.data;

                    if (uri == null) {
                        Log.e("registerForActivityResult", result.data.toString());
                        return@registerForActivityResult;
                    }

                    val baseDirectory: String = cacheDir.absolutePath + "/"
                    val baseExternalDirectory: String = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.absolutePath + "/"
                    val tempFileDef: String = SystemClock.currentThreadTimeMillis().toString()
                    val amrFileName: String = "$baseDirectory$tempFileDef.amr"
                    val wavFileName: String = "$baseExternalDirectory$tempFileDef.wav"
                    val wavFilePath: File = File(baseDirectory)
                    val exFilePath: File = File(baseExternalDirectory)

                    if(!wavFilePath.exists()) {
                        // NOTICE: 음원 경로가 없을 경우.(예외처리용)
                        wavFilePath.mkdirs()
                    }
                    if(!exFilePath.exists()) {
                        // NOTICE: 음원 경로가 없을 경우 (예외처리용)
                        exFilePath.mkdirs()
                    }

                    val amrFile: File = File(amrFileName)
                    amrFile.createNewFile()

                    val amrInputStream = contentResolver.openInputStream(uri) as InputStream
                    val outputStream: FileOutputStream = FileOutputStream(amrFile)
                    var buffer: ByteArray = ByteArray(1024)
                    var readCount: Int = amrInputStream.read(buffer)

                    while (readCount > -1) {
                        outputStream.write(buffer, 0, readCount)
                        buffer = ByteArray(1024)
                        readCount = amrInputStream.read(buffer)
                    }
                    outputStream.flush()
                    amrInputStream.close()
                    outputStream.close()

                    val session = FFmpegKit.execute("-y -i $amrFileName $wavFileName")
                    // wav 파일로 변환하여 저장성공
                    if (ReturnCode.isSuccess(session.getReturnCode())) {
                        Log.d("저장 성공", wavFileName)
                        // 2023.09.12. botbinoo
//                        uploadStorage(File(wavFileName))
                        uploadRawFileToStorage(amrFile)
                        uploadWavFileToStorage(File(wavFileName))
                        uploadRawFileToFlaskAPI(amrFile)
                        // end 2023.09.12. botbinoo
                        amrFile.delete()

                        supportFragmentManager.setFragmentResult(SpeechRecognizer.RESULTS_RECOGNITION,
                            bundleOf(SpeechRecognizer.RESULTS_RECOGNITION to result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS))
                        )
                    } else if (ReturnCode.isCancel(session.getReturnCode())) {
                    } else {
                        // wav 파일로 저장실패
                        Log.d("FFmpeg", String.format("Command failed with state %s and rc %s.%s", session.getState(), session.getReturnCode(), session.getFailStackTrace()));

                    }
                }
            }
    }
    /*
    fun uploadStorage(wavFile: File){
        val storage = Firebase.storage(getString(R.string.base_directory_by_firebase_storage))
        val storageRef = storage.reference

        var file = Uri.fromFile(wavFile)
        val riversRef = storageRef.child("wav/${file.lastPathSegment}")
        val uploadTask = riversRef.putFile(file) //file 사용
        uploadTask.addOnFailureListener {
            Log.e("uploadStorage err", it.message.toString())
        }.addOnSuccessListener { taskSnapshot ->
            Log.d("업로드 성공", taskSnapshot.uploadSessionUri.toString())
        }
    }
    */ // notice: uploadFileToStorage 로 함수 변경
    // 250- 252: wav파일로 저장된 것을 파이어베이스로 전송
    // 2023.09.12. botbinoo
    fun uploadWavFileToStorage(file: File){
        uploadFileToStorage(file, "wav");
    }
    fun uploadRawFileToStorage(file: File){
        uploadFileToStorage(file, "raw");
    }
    // notice: flask 로 파일 전송 모듈
    fun uploadRawFileToFlaskAPI(file: File){
        FlaskFilterServiceImpl().sendRawFile(file)
    }

    fun uploadFileToStorage(file: File, folder: String){
        val storage = Firebase.storage(getString(R.string.base_directory_by_firebase_storage))
        val storageRef = storage.reference

        var file = Uri.fromFile(file)
        val riversRef = storageRef.child("${folder}/${file.lastPathSegment}")
        val uploadTask = riversRef.putFile(file)
        uploadTask.addOnFailureListener {
            Log.e("uploadStorage err", it.message.toString())
        }.addOnSuccessListener { taskSnapshot ->
            Log.d("업로드 성공", taskSnapshot.uploadSessionUri.toString())
        }
    }
    // end 2023.09.12. botbinoo

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

    fun print(list : ArrayList<String>, tag : String) {
        for (idx: Int in 0 until list.size) {
            Log.e(tag, list[idx])
        }
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
