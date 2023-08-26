package com.example.androidstt.ui.order.children

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.androidstt.databinding.FragmentFullMenuBinding
import com.example.androidstt.databinding.ItemFoodBinding
import com.example.androidstt.model.Food
import com.example.androidstt.model.Order
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.snapshots
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

//region Food list fragment
class FullMenuFragment : OrderChildrenBaseFragment() {
    private var _binding: FragmentFullMenuBinding? = null
    private val binding get() = _binding!!

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val adapter by lazy { ViewPagerAdapter() }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFullMenuBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            toolbar.setOnClickListener { requireActivity().onBackPressed() }

            viewPager.apply {
                offscreenPageLimit = 3
                isUserInputEnabled = false
                adapter = this@FullMenuFragment.adapter.apply {
                    onItemClickListener = {
                        next(orderList.copy().apply {
                            elements.add(
                                Order(
                                    it,
                                    if (it.options.size <= 1) "" else null
                                )
                            )
                        })
                    }
                }
            }

            tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    val position = tab?.position ?: return
                    if (position >= 0) {
                        viewPager.currentItem = position
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {
                }

                override fun onTabReselected(tab: TabLayout.Tab?) {
                }
            })
        }

        lifecycleScope.launch {
            db.collection("foods")
                .orderBy("type")
                .snapshots()
                .collectLatest {
                    val foods =
                        it.documents.mapNotNull { it.toObject(Food::class.java) }
                            .groupBy { it.type }

                    val items = listOf(
                        Food.Type.HAMBURGER, Food.Type.SIDE_MENU, Food.Type.BEVERAGE,
                        Food.Type.DESSERT
                    ).map {
                        if (foods.containsKey(it)) {
                            return@map it to foods[it]!!
                        } else {
                            return@map it to listOf()
                        }
                    }

                    adapter.submitList(items)
                }
        }

        speakOut("주문하고 싶으신 메뉴가 있나요?")
    }

    override fun onResume() {
        super.onResume()

        if (binding.viewPager.currentItem != binding.tabLayout.selectedTabPosition) {
            binding.tabLayout.selectTab(binding.tabLayout.getTabAt(binding.viewPager.currentItem))
        }
    }

    override fun onRecognized(message: String) {
//        super.onRecognized(message)
        Log.d("FullMenuFragment", message)

        lifecycleScope.launch {
            val response = sendMessageToBot(message) ?: return@launch

            if (response.foods == null) {
                speakOut(response.fulfillmentText)

            } else {
                val foods = ArrayList(response.foods)
                foods.forEach {
                    if (it.option == null && it.food.options.size <= 1) {
                        it.option = ""
                    }
                }

                next(orderList.copy().apply {
                    elements.addAll(foods)
                })
            }
        }
    }

    //region View pager adapter
    private class ViewPagerAdapter :
        ListAdapter<Pair<Food.Type, List<Food>>, RecyclerView.ViewHolder>(diffUtil) {
        var onItemClickListener: ((Food) -> Unit)? = null

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): RecyclerView.ViewHolder {
            val recyclerView = RecyclerView(parent.context).apply {
                layoutManager =
                    GridLayoutManager(parent.context, 3, LinearLayoutManager.VERTICAL, false)
                layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)

                val padding = (parent.context.resources.displayMetrics.density * 8).toInt()
                setPadding(padding, padding * 2, padding, 0)
            }

            return object : RecyclerView.ViewHolder(recyclerView) {
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val adapter = FoodListAdapter().apply {
                onItemClickListener = this@ViewPagerAdapter.onItemClickListener
            }

            adapter.submitList(getItem(position).second)
            (holder.itemView as RecyclerView).adapter = adapter
        }

        companion object {
            val diffUtil = object : DiffUtil.ItemCallback<Pair<Food.Type, List<Food>>>() {
                override fun areItemsTheSame(
                    oldItem: Pair<Food.Type, List<Food>>,
                    newItem: Pair<Food.Type, List<Food>>
                ): Boolean {
                    return oldItem.first == newItem.first
                }

                override fun areContentsTheSame(
                    oldItem: Pair<Food.Type, List<Food>>,
                    newItem: Pair<Food.Type, List<Food>>
                ): Boolean {
                    if (oldItem.second.size != newItem.second.size) {
                        return false
                    }

                    oldItem.second.zip(newItem.second).forEach {
                        if (it.first.documentId != it.second.documentId) {
                            return false
                        }
                    }

                    return true
                }
            }
        }

        //region Food list adapter
        private class FoodListAdapter :
            ListAdapter<Food, FoodListAdapter.FoodItemViewHolder>(diffUtil) {

            var onItemClickListener: ((Food) -> Unit)? = null

            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): FoodItemViewHolder {
                return FoodItemViewHolder(
                    ItemFoodBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }

            override fun onBindViewHolder(holder: FoodItemViewHolder, position: Int) {
                val item = getItem(position)
                with(holder.binding) {
                    Glide.with(imageView)
                        .load(item.image)
                        .into(imageView)

                    nameTextView.text = item.name

                    item.options.toList().zip(listOf(option1TextView, option2TextView))
                        .forEach {
                            val textView = it.second
                            val option = it.first.first
                            val price = it.first.second

                            textView.text = if (item.options.size == 1) {
                                String.format(
                                    Locale.KOREA,
                                    "%s원",
                                    NumberFormat.getInstance(Locale.KOREA).format(price)
                                )
                            } else {
                                String.format(
                                    Locale.KOREA,
                                    "%s: %s원",
                                    option,
                                    NumberFormat.getInstance(Locale.KOREA).format(price)
                                )
                            }
                        }

                    imageContainer.setOnClickListener {
                        onItemClickListener?.invoke(item)
                    }
                }
            }

            companion object {
                val diffUtil = object : DiffUtil.ItemCallback<Food>() {
                    override fun areItemsTheSame(oldItem: Food, newItem: Food): Boolean {
                        return oldItem.documentId == newItem.documentId
                    }

                    override fun areContentsTheSame(oldItem: Food, newItem: Food): Boolean {
                        return oldItem.name == newItem.name &&
                                oldItem.options.size == newItem.options.size &&
                                oldItem.options.toList().zip(newItem.options.toList())
                                    .none { it.first.first != it.second.first || it.first.second != it.second.second }
                    }
                }
            }

            class FoodItemViewHolder(val binding: ItemFoodBinding) :
                RecyclerView.ViewHolder(binding.root)
        }
        //endregion
    }
    //endregion
}
//endregion
