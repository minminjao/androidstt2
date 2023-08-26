package com.example.androidstt.ui.order.children

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.androidstt.databinding.FragmentPaymentBinding
import java.text.NumberFormat
import java.util.Locale

//region Option fragment
class PaymentFragment : OrderChildrenBaseFragment() {
    private var _binding: FragmentPaymentBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            toolbar.setOnClickListener { parentFragmentManager.popBackStack() }

            priceTextView.text = String.format(
                Locale.KOREA,
                "총 결제 금액 : %s원",
                NumberFormat.getInstance(Locale.KOREA).format(orderList.elements.sumOf { it.price })
            )

            option1Container.setOnClickListener {
                //TODO: 카드 결제

                backToFullMenuFragment()
            }

            option2Container.setOnClickListener {
                //TODO: 쿠폰 결제

                backToFullMenuFragment()
            }
        }
    }

    override fun onRecognized(message: String) {
//        super.onRecognized(message)
    }
}
//endregion
