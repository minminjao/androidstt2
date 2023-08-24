package com.example.androidstt

import android.os.Bundle
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentResultListener

open class BaseFragment : Fragment(), FragmentResultListener {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().supportFragmentManager.setFragmentResultListener(
            SpeechRecognizer.RESULTS_RECOGNITION,
            viewLifecycleOwner,
            this
        )
    }

    protected open fun speakOut(text: String) {
        (activity as? MainActivity)?.speakOut(text)
    }

    protected open fun onRecognized(results: List<String>) {
        Log.d("BaseFragment", results.joinToString())
    }

    override fun onFragmentResult(requestKey: String, result: Bundle) {
        if (requestKey == SpeechRecognizer.RESULTS_RECOGNITION) {
            onRecognized(result.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: listOf())
        }
    }
}