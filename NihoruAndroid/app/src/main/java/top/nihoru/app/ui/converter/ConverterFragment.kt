package top.nihoru.app.ui.converter

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import top.nihoru.app.R
import top.nihoru.app.databinding.FragmentConverterBinding
import java.io.File

class ConverterFragment : Fragment() {

    private var _binding: FragmentConverterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ConverterViewModel by viewModels()
    private lateinit var adapter: AnimeAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentConverterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSpinner()
        setupModeToggle()
        setupObservers()
        setupClickListeners()
        viewModel.loadSaved()
    }

    private fun setupRecyclerView() {
        adapter = AnimeAdapter(
            onDelete = { entry -> viewModel.deleteEntry(entry) },
            onStatusChange = { malId, status -> viewModel.updateEntryStatus(malId, status) }
        )
        binding.rvAnimeList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAnimeList.adapter = adapter
    }

    private fun setupSpinner() {
        val statuses = listOf("Plan to Watch", "Watching", "Completed", "On-Hold", "Dropped")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statuses)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStatus.adapter = spinnerAdapter
    }

    private fun setupModeToggle() {
        binding.btnAnimeMode.setOnClickListener {
            viewModel.switchMode("anime")
            updateModeUI("anime")
            updateStatusLabels("anime")
        }
        binding.btnMangaMode.setOnClickListener {
            viewModel.switchMode("manga")
            updateModeUI("manga")
            updateStatusLabels("manga")
        }
    }

    private fun updateModeUI(mode: String) {
        val accentBg = R.drawable.btn_accent_bg
        val transparentBg = android.R.color.transparent
        val whiteColor = android.R.color.white
        val mutedColor = R.color.text_muted

        if (mode == "anime") {
            binding.btnAnimeMode.setBackgroundResource(accentBg)
            binding.btnAnimeMode.setTextColor(requireContext().getColor(whiteColor))
            binding.btnMangaMode.setBackgroundResource(transparentBg)
            binding.btnMangaMode.setTextColor(requireContext().getColor(mutedColor))
        } else {
            binding.btnMangaMode.setBackgroundResource(accentBg)
            binding.btnMangaMode.setTextColor(requireContext().getColor(whiteColor))
            binding.btnAnimeMode.setBackgroundResource(transparentBg)
            binding.btnAnimeMode.setTextColor(requireContext().getColor(mutedColor))
        }
    }

    private fun updateStatusLabels(mode: String) {
        val statuses = if (mode == "anime")
            listOf("Plan to Watch", "Watching", "Completed", "On-Hold", "Dropped")
        else
            listOf("Plan to Read", "Reading", "Completed", "On-Hold", "Dropped")

        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statuses)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStatus.adapter = spinnerAdapter
    }

    private fun setupObservers() {
        viewModel.animeList.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list.toList())
            binding.tvCountBadge.text = getString(R.string.items_count, list.size)
            if (list.isNotEmpty()) {
                binding.llResults.visibility = View.VISIBLE
                binding.llExport.visibility = View.VISIBLE
            }
        }

        viewModel.toast.observe(viewLifecycleOwner) { state ->
            state ?: return@observe
            binding.llToast.visibility = View.VISIBLE
            binding.tvToastMsg.text = state.message
            when (state.type) {
                "success" -> binding.ivToastIcon.setImageResource(android.R.drawable.checkbox_on_background)
                "error" -> binding.ivToastIcon.setImageResource(android.R.drawable.ic_dialog_alert)
                "loading" -> binding.ivToastIcon.setImageResource(android.R.drawable.ic_popup_sync)
                "warning" -> binding.ivToastIcon.setImageResource(android.R.drawable.ic_dialog_alert)
                else -> binding.ivToastIcon.setImageResource(android.R.drawable.ic_menu_info_details)
            }
        }

        viewModel.progress.observe(viewLifecycleOwner) { progress ->
            if (progress == null) {
                binding.llProgress.visibility = View.GONE
            } else {
                binding.llProgress.visibility = View.VISIBLE
                val (current, total, label) = progress
                binding.tvProgressText.text = label
                binding.tvProgressCount.text = "$current/$total"
                binding.progressBar.max = total
                binding.progressBar.progress = current
            }
        }

        viewModel.isProcessing.observe(viewLifecycleOwner) { isProcessing ->
            binding.btnSearch.isEnabled = !isProcessing
            binding.btnSearch.text = if (isProcessing) "Processing…" else getString(R.string.search_process)
        }

        viewModel.exportXml.observe(viewLifecycleOwner) { xml ->
            if (xml != null) {
                shareFile(xml, "mal_${viewModel.currentMode}_list.xml", "application/xml")
                viewModel.clearExportState()
            }
        }

        viewModel.exportJson.observe(viewLifecycleOwner) { json ->
            if (json != null) {
                shareFile(json, "anilist_${viewModel.currentMode}_export.json", "application/json")
                viewModel.clearExportState()
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnSearch.setOnClickListener {
            val rawText = binding.etRawInput.text.toString()
            val autoSeason = binding.switchAutoSeason.isChecked
            val status = binding.spinnerStatus.selectedItem?.toString() ?: "Plan to Watch"
            viewModel.processInput(rawText, autoSeason, status)
        }

        binding.btnClearList.setOnClickListener {
            viewModel.clearList()
            binding.llResults.visibility = View.GONE
            binding.llExport.visibility = View.GONE
            binding.llToast.visibility = View.GONE
        }

        binding.btnExportXml.setOnClickListener { viewModel.exportXML() }
        binding.btnExportJson.setOnClickListener { viewModel.exportJSON() }
    }

    private fun shareFile(content: String, filename: String, mimeType: String) {
        try {
            val file = File(requireContext().cacheDir, filename)
            file.writeText(content)
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share $filename"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
