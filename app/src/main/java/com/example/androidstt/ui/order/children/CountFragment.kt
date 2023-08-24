package com.example.androidstt.ui.order.children

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.androidstt.databinding.FragmentCountBinding

//region Option fragment
class CountFragment : OrderChildrenBaseFragment() {
    private var _binding: FragmentCountBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            toolbar.setOnClickListener { parentFragmentManager.popBackStack() }
            toolbar.title = food.name + " 갯수 선택"

            minusButton.setOnClickListener {
                var count = countTextView.text.toString().removeSuffix("개").toInt()
                if (count > 1) {
                    count -= 1
                }

                countTextView.text = "${count}개"
            }

            plusButton.setOnClickListener {
                var count = countTextView.text.toString().removeSuffix("개").toInt()
                count += 1

                countTextView.text = "${count}개"
            }

            nextButton.setOnClickListener {
                val count = countTextView.text.toString().removeSuffix("개").toInt()
                onSelectedCount(count)
            }
        }

        speakOut("${food.name} 수량을 선택해 주세요.")
    }

    private fun onSelectedCount(count: Int) {
        order.count = count
        next(orderList)
    }

    override fun onRecognized(message: String) {
        super.onRecognized(message)

        val count = message.replace("[^0-9]", "").toIntOrNull()
        if (count != null) {
            onSelectedCount(count)
        } else {
            val message = message.replace(" ", "")

            if (message.startsWith("한개") || message.startsWith("하나")) {
                onSelectedCount(1)
            } else if (message.startsWith("두개") || message.startsWith("둘")) {
                onSelectedCount(2)
            } else if (message.startsWith("세개") || message.startsWith("셋")) {
                onSelectedCount(3)
            } else if (message.startsWith("네개") || message.startsWith("넷")) {
                onSelectedCount(4)
            } else if (message.startsWith("다섯")) {
                onSelectedCount(5)
            } else if (message.startsWith("여섯")) {
                onSelectedCount(6)
            } else if (message.startsWith("일곱")) {
                onSelectedCount(7)
            } else if (message.startsWith("여덟") || message.startsWith("여덜")) {
                onSelectedCount(8)
            } else if (message.startsWith("아홉")) {
                onSelectedCount(9)
            } else {
                speakOut("다시 말씀해 주세요.")
            }
        }
    }
}
//endregion
