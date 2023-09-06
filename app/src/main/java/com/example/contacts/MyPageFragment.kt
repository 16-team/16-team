package com.example.contacts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.manager.SupportRequestManagerFragment
import com.example.contacts.databinding.FragmentMyPageBinding

class MyPageFragment : Fragment() {
    private lateinit var binding: FragmentMyPageBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMyPageBinding.inflate(inflater, container, false)

        binding.editBtn.setOnClickListener {
            val editMyPageFragment = EditMyPageFragment()

            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.myPageContainer, editMyPageFragment)
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit()


        }
        return binding.root
    }
}

