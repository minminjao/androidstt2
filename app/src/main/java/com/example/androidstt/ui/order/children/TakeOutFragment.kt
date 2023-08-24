package com.example.androidstt.ui.order.children

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.androidstt.databinding.FragmentTakeOutBinding

//region Option fragment
class TakeOutFragment : OrderChildrenBaseFragment() {
    private var _binding: FragmentTakeOutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTakeOutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            toolbar.setOnClickListener { parentFragmentManager.popBackStack() }

            option1Container.setOnClickListener {
                orderList.takeOut = true
                goToOrderConfirmation(orderList)
            }

            option2Container.setOnClickListener {
                orderList.takeOut = false
                goToOrderConfirmation(orderList)
            }
        }

        speakOut("포장하시나요? 매장에서 식사하시나요?")
    }

    override fun onRecognized(message: String) {
        super.onRecognized(message)

        if (message.contains("포장")) {
            orderList.takeOut = true
            goToOrderConfirmation(orderList)

        } else if (message.contains("매장")) {
            orderList.takeOut = false
            goToOrderConfirmation(orderList)
        }
    }
}
//endregion
