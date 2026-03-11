package top.nihoru.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import top.nihoru.app.R
import top.nihoru.app.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnStartConverting.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_converter)
        }

        binding.btnLearnMore.setOnClickListener {
            binding.sectionHowItWorks.post {
                binding.root.smoothScrollTo(0, binding.sectionHowItWorks.top)
            }
        }

        binding.footerTextToMal.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_converter)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
