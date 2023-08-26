package com.example.androidstt.ui.order.children

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import com.example.androidstt.databinding.FragmentOptionsBinding
import com.example.androidstt.databinding.ItemOptionBinding
import com.example.androidstt.model.Food

//region Option fragment
class OptionFragment : OrderChildrenBaseFragment() {
    private var _binding: FragmentOptionsBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            toolbar.setOnClickListener { parentFragmentManager.popBackStack() }
            toolbar.title = food.name + " 옵션 선택"

            initOptionButtons()
        }

        if (food.type == Food.Type.HAMBURGER) {
            speakOut("${food.name} 주문합니다. 단품으로 하시겠어요. 세트로 하시겠어요?")
        } else {
            speakOut("${food.name} 주문합니다.")
        }
    }

    private fun initOptionButtons() {
        val inflater = LayoutInflater.from(requireContext())
        val buttons = options.keys.map {
            ItemOptionBinding.inflate(inflater).apply {
                optionTextView.text = it
            }
        }

        if (buttons.size == 2) {
            binding.optionContainer.updateLayoutParams<ConstraintLayout.LayoutParams> {
                dimensionRatio = "4:3"
            }

            binding.optionContainer.orientation = LinearLayout.HORIZONTAL
            binding.optionContainer.addView(
                buttons[0].root,
                LinearLayout.LayoutParams(0, MATCH_PARENT).apply {
                    weight = 1f
                })
            binding.optionContainer.addView(
                buttons[1].root,
                LinearLayout.LayoutParams(0, MATCH_PARENT).apply {
                    weight = 1f
                    leftMargin = (resources.displayMetrics.density * 16).toInt()
                })
        } else {
            binding.optionContainer.updateLayoutParams<ConstraintLayout.LayoutParams> {
                dimensionRatio = "1:1"
            }

            binding.optionContainer.orientation = LinearLayout.VERTICAL

            val top = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL

                addView(
                    buttons[0].root,
                    LinearLayout.LayoutParams(0, MATCH_PARENT).apply {
                        weight = 1f
                    })
                addView(
                    buttons[1].root,
                    LinearLayout.LayoutParams(0, MATCH_PARENT).apply {
                        weight = 1f
                        leftMargin = (resources.displayMetrics.density * 16).toInt()
                    })
            }

            val bottom = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL

                addView(
                    buttons[2].root,
                    LinearLayout.LayoutParams(0, MATCH_PARENT).apply {
                        weight = 1f
                    })

                if (buttons.size == 3) {
                    addView(
                        View(requireContext()),
                        LinearLayout.LayoutParams(0, MATCH_PARENT).apply {
                            weight = 1f
                            leftMargin = (resources.displayMetrics.density * 16).toInt()
                        })
                } else {
                    addView(
                        buttons[3].root,
                        LinearLayout.LayoutParams(0, MATCH_PARENT).apply {
                            weight = 1f
                            leftMargin = (resources.displayMetrics.density * 16).toInt()
                        })
                }
            }

            binding.optionContainer.addView(
                top,
                LinearLayout.LayoutParams(MATCH_PARENT, 0).apply {
                    weight = 1f
                })

            binding.optionContainer.addView(
                bottom,
                LinearLayout.LayoutParams(MATCH_PARENT, 0).apply {
                    weight = 1f
                    topMargin = (resources.displayMetrics.density * 16).toInt()
                })
        }

        buttons.forEachIndexed { index, itemOptionBinding ->
            itemOptionBinding.root.setOnClickListener {
                onSelectedOption(options.keys.toList()[index])
            }
        }
    }

    private fun onSelectedOption(option: String) {
        when (type) {
            IncompleteType.MainFoodOption -> order.option = option
            IncompleteType.HamburgerSetSideMenuOption -> order.sideMenuOption = option
            IncompleteType.HamburgerSetBeverageOption -> order.beverageOption = option
            else -> error("Not found options.")
        }

        next(orderList)
    }

    override fun onRecognized(message: String) {
//        super.onRecognized(message)

        val options = options.keys.firstOrNull { message.contains(it) } ?: run {
            speakOut("다시 말씀해 주세요.")
            return
        }

        onSelectedOption(options)
    }
}
//endregion
