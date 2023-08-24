package com.example.androidstt.ui.order.children

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.androidstt.databinding.FragmentOrderConfirmationBinding
import com.example.androidstt.databinding.ItemPriceBinding
import java.text.NumberFormat
import java.util.Locale

//region Option fragment
class OrderConfirmationFragment : OrderChildrenBaseFragment() {
    private var _binding: FragmentOrderConfirmationBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderConfirmationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            toolbar.setOnClickListener { parentFragmentManager.popBackStack() }

            orderList.elements.forEachIndexed { index, order ->
                val priceBinding = ItemPriceBinding.inflate(
                    LayoutInflater.from(view.context),
                    binding.contentContainer,
                    false
                )

                with(priceBinding) {
                    foodNameTextView.text = order.food.name
                    optionTextView.text = if (order.sideMenu == null) {
                        "단품"
                    } else {
                        "${order.sideMenu!!.name} ${order.beverage!!.name}"
                    }
                }

                binding.contentContainer.addView(priceBinding.root, index)
            }

            priceTextView.text = String.format(
                Locale.KOREA,
                "%s원",
                NumberFormat.getInstance(Locale.KOREA).format(orderList.elements.sumOf { it.price })
            )

            negativeButton.setOnClickListener {
                backToFullMenuFragment()
            }

            positiveButton.setOnClickListener {
                goToPaymentFragment(orderList)
            }
        }

        speakOut("주문하신 메뉴가 일치하면 '맞아' 아니면 '아니야' 라고 말씀해 주세요.")
    }

    override fun onRecognized(message: String) {
        super.onRecognized(message)

        if (message.contains("맞아") ||
            message.contains("예")
        ) {
            goToPaymentFragment(orderList)

        } else if (
            message.contains("아니야") ||
            message.contains("아니오")
        ) {
            backToFullMenuFragment()
        }
    }
}
//endregion
