package com.example.androidstt.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.androidstt.BaseFragment
import com.example.androidstt.ui.order.OrderFragment
import com.example.androidstt.R
import com.example.androidstt.databinding.FragmentHomeBinding

class HomeFragment : BaseFragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            orderButton.setOnClickListener {
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, OrderFragment())
                    .addToBackStack(null)
                    .commit()
            }

            speakOut(binding.descriptionTextView.text.toString())
        }
    }

    override fun onRecognized(results: List<String>) {
        super.onRecognized(results)

        if (results.any { it.contains("주문") }) {
            binding.orderButton.performClick()
        }
    }
}