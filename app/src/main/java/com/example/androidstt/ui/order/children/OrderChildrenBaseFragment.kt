package com.example.androidstt.ui.order.children

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentResultListener
import androidx.lifecycle.Lifecycle
import com.example.androidstt.R
import com.example.androidstt.model.BotResponse
import com.example.androidstt.model.Food
import com.example.androidstt.model.Order
import com.example.androidstt.model.OrderList
import com.example.androidstt.ui.order.OrderFragment
import com.example.androidstt.ui.order.OrderFragment.Companion.KEY_ORDER_LIST
import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

open class OrderChildrenBaseFragment : Fragment(), FragmentResultListener {
    companion object {
        fun <T : OrderChildrenBaseFragment> getInstance(
            orderList: OrderList? = null,
            init: () -> T
        ): T {
            val fragment = init()
            if (orderList != null) {
                fragment.arguments = bundleOf(KEY_ORDER_LIST to orderList)
            }

            return fragment
        }
    }

    enum class IncompleteType {
        MainFoodOption,
        HamburgerSetSideMenu,
        HamburgerSetSideMenuOption,
        HamburgerSetBeverage,
        HamburgerSetBeverageOption,
        Count
    }

    private var _orderList: OrderList? = null
    protected lateinit var orderList: OrderList

    protected lateinit var order: Order
    protected lateinit var type: IncompleteType
    protected lateinit var food: Food
    protected lateinit var options: HashMap<String, Long>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            _orderList = arguments?.getParcelable(KEY_ORDER_LIST)
        }

        if (savedInstanceState != null) {
            _orderList = savedInstanceState.getParcelable(KEY_ORDER_LIST)
        }

        if (_orderList != null) {
            orderList = _orderList!!.copy()

            val orderAndType = getIncompleteOrderAndType(orderList) ?: return
            order = orderAndType.first
            type = orderAndType.second

            food = when (type) {
                IncompleteType.HamburgerSetSideMenuOption -> order.sideMenu!!
                IncompleteType.HamburgerSetBeverageOption -> order.beverage!!
                else -> order.food
            }

            options = food.options

        } else {
            if (this is FullMenuFragment) {
                orderList = OrderList(arrayListOf(), false)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(KEY_ORDER_LIST, _orderList)
        super.onSaveInstanceState(outState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        parentFragmentManager.setFragmentResultListener(
            OrderFragment.REQUEST_RECOGNITION,
            viewLifecycleOwner,
            this
        )
    }

    protected fun speakOut(text: String) {
        parentFragmentManager.setFragmentResult(
            OrderFragment.REQUEST_SPEAK_OUT,
            bundleOf("text" to text)
        )
    }

    protected fun addMyMessage(text: String) {
        parentFragmentManager.setFragmentResult(
            OrderFragment.REQUEST_ADD_MY_MESSAGE,
            bundleOf("text" to text)
        )
    }
    // 파이어베이스 function의 detectIntent 호출하는 코드
    protected suspend fun sendMessageToBot(text: String) = withContext(Dispatchers.IO) {
        val progressView = parentFragment?.view?.findViewById<View>(R.id.progress_view)
        if (progressView?.isVisible == true) return@withContext null

        launch(Dispatchers.Main) {
            progressView?.isVisible = true
        }

        try {
            val result = FirebaseFunctions.getInstance().getHttpsCallable("detectIntent")
                .call(hashMapOf("question" to text))
                .await()

            launch(Dispatchers.Main) {
                progressView?.isVisible = false
            }

            return@withContext Gson().fromJson(result.data as String, BotResponse::class.java)

        } catch (e: Exception) {
            launch(Dispatchers.Main) {
                progressView?.isVisible = false
            }

            return@withContext BotResponse(
                false,
                e.message ?: "Failed to get a bot response.",
                null
            )
        }
    }

    protected fun next(orderList: OrderList, backStackName: String? = null) {
        if (orderList.elements.isEmpty()) return
        if (!isAdded) return

        // 옵션, 햄버거 세트 사이드 메뉴, 햄버거 세트 음료, 수량 순
        val type = getIncompleteOrderAndType(orderList)?.second

        if (type == null) {
            // 주문 완료
            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.child_fragment_container,
                    getInstance(orderList, ::TakeOutFragment)
                )
                .addToBackStack(backStackName)
                .commit()

        } else {
            when (type) {
                IncompleteType.MainFoodOption,
                IncompleteType.HamburgerSetSideMenuOption,
                IncompleteType.HamburgerSetBeverageOption -> {
                    // 옵션 선택 안함
                    parentFragmentManager.beginTransaction()
                        .replace(
                            R.id.child_fragment_container,
                            getInstance(orderList, ::OptionFragment)
                        )
                        .addToBackStack(backStackName)
                        .commit()
                }

                IncompleteType.HamburgerSetSideMenu,
                IncompleteType.HamburgerSetBeverage -> {
                    // 햄버거 세트 사이드 메뉴 또는 음료 선택 안함
                    parentFragmentManager.beginTransaction()
                        .replace(
                            R.id.child_fragment_container,
                            getInstance(orderList, ::CategoryMenuFragment)
                        )
                        .addToBackStack(backStackName)
                        .commit()
                }

                else -> {
                    // 수량 선택 안함
                    parentFragmentManager.beginTransaction()
                        .replace(
                            R.id.child_fragment_container,
                            getInstance(orderList, ::CountFragment)
                        )
                        .addToBackStack(backStackName)
                        .commit()
                }
            }
        }
    }

    private fun getIncompleteOrderAndType(orderList: OrderList): Pair<Order, IncompleteType>? {
        orderList.elements.forEach {
            if (it.food.options.size > 1 && it.option == null) {
                return it to IncompleteType.MainFoodOption
            }

            if (it.food.type == Food.Type.HAMBURGER && it.option == "세트") {
                if (it.sideMenu == null) {
                    return it to IncompleteType.HamburgerSetSideMenu
                }

                if (it.sideMenuOption == null) {
                    return it to IncompleteType.HamburgerSetSideMenuOption
                }

                if (it.beverage == null) {
                    return it to IncompleteType.HamburgerSetBeverage
                }

                if (it.beverageOption == null) {
                    return it to IncompleteType.HamburgerSetBeverageOption
                }
            }

            if (it.count == null) {
                return it to IncompleteType.Count
            }
        }

        return null
    }

    protected fun goToOrderConfirmation(orderList: OrderList) {
        parentFragmentManager.beginTransaction()
            .replace(
                R.id.child_fragment_container,
                getInstance(orderList, ::OrderConfirmationFragment)
            )
            .addToBackStack(null)
            .commit()
    }

    protected fun goToPaymentFragment(orderList: OrderList) {
        parentFragmentManager.beginTransaction()
            .replace(
                R.id.child_fragment_container,
                getInstance(orderList, ::PaymentFragment)
            )
            .addToBackStack(null)
            .commit()
    }

    protected fun backToFullMenuFragment() {
        with(parentFragmentManager) {
            popBackStack("main", 1)

            beginTransaction()
                .replace(R.id.child_fragment_container, FullMenuFragment())
                .addToBackStack("main")
                .commit()
        }
    }

    protected open fun onRecognized(message: String) {
        Log.d("OrderChildrenBaseFragment", message)
    }

    override fun onFragmentResult(requestKey: String, result: Bundle) {
        val message = result.getString("message")
            // ?: return

        Log.d("Lifecycle", lifecycle.currentState.toString())
        Log.d("Lifecycle", Lifecycle.State.RESUMED.toString())

//        if (lifecycle.currentState == Lifecycle.State.RESUMED) {
            if (message != null) {
                onRecognized(message)
            }
//        }
    }
}