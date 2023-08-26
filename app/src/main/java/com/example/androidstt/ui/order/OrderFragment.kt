package com.example.androidstt.ui.order

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import com.example.androidstt.BaseFragment
import com.example.androidstt.R
import com.example.androidstt.databinding.FragmentOrderBinding
import com.example.androidstt.databinding.ItemMyMessageBinding
import com.example.androidstt.databinding.ItemOtherMessageBinding
import com.example.androidstt.model.Message
import com.example.androidstt.ui.order.children.FullMenuFragment

class OrderFragment : BaseFragment() {
    companion object {
        const val REQUEST_RECOGNITION = "REQUEST_RECOGNITION"
        const val REQUEST_SPEAK_OUT = "REQUEST_SPEAK_OUT"
        const val REQUEST_ADD_MY_MESSAGE = "REQUEST_ADD_MY_MESSAGE"

        const val KEY_ORDER_LIST = "KEY_ORDER_LIST"
    }

    private var _binding: FragmentOrderBinding? = null
    private val binding get() = _binding!!

    private val adapter = MessageAdapter()

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (childFragmentManager.backStackEntryCount > 1) {
                childFragmentManager.popBackStack()
            } else {
                this.isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(onBackPressedCallback)

        with(childFragmentManager) {
            setFragmentResultListener(REQUEST_SPEAK_OUT, viewLifecycleOwner) { _, result ->
                val text = result.getString("text") ?: return@setFragmentResultListener
                speakOut(text)
            }

            setFragmentResultListener(REQUEST_ADD_MY_MESSAGE, viewLifecycleOwner) { _, result ->
                val text = result.getString("text") ?: return@setFragmentResultListener
                addMyMessage(text)
            }

            beginTransaction()
                .replace(R.id.child_fragment_container, FullMenuFragment())
                .addToBackStack("main")
                .commit()
        }

        with(binding) {
            recyclerView.adapter = adapter
        }
    }

    public override fun speakOut(text: String) {
        super.speakOut(text)

        adapter.addMessage(text, false)
        binding.recyclerView.scrollToPosition(adapter.itemCount - 1)
    }

    private fun addMyMessage(message: String) {
        adapter.addMessage(message, true)
        binding.recyclerView.scrollToPosition(adapter.itemCount - 1)
    }

    // 주문 관련 음성 인식은 여기에서 다 처리합니다.
    override fun onRecognized(results: List<String>) {
        super.onRecognized(results)
        Log.d("OrderFragment", results.joinToString())

        val message = results.firstOrNull { it.isNotBlank() } ?: return
        addMyMessage(message)

        childFragmentManager.setFragmentResult(
            REQUEST_RECOGNITION,
            bundleOf("message" to message)
        )
    }

    class MessageAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val messages = ArrayList<Message>()

        fun addMessage(message: String, isMyMessage: Boolean) {
            messages.add(Message(message, isMyMessage))
            notifyItemInserted(messages.size - 1)
        }

        override fun getItemViewType(position: Int): Int {
            return if (messages.getOrNull(position)?.isMyMessage == true) {
                1
            } else {
                0
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)

            if (viewType == 1) {
                return MyMessageViewHolder(ItemMyMessageBinding.inflate(inflater, parent, false))
            } else {
                return OtherMessageViewHolder(
                    ItemOtherMessageBinding.inflate(
                        inflater,
                        parent,
                        false
                    )
                )
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val message = messages[position]

            if (holder is OtherMessageViewHolder) {
                holder.binding.textView.text = message.message
            } else if (holder is MyMessageViewHolder) {
                holder.binding.textView.text = message.message
            }
        }

        override fun getItemCount(): Int {
            return messages.size
        }

        class OtherMessageViewHolder(val binding: ItemOtherMessageBinding) :
            RecyclerView.ViewHolder(binding.root)

        class MyMessageViewHolder(val binding: ItemMyMessageBinding) :
            RecyclerView.ViewHolder(binding.root)
    }
}