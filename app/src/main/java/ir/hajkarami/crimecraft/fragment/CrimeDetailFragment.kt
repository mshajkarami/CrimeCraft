package ir.hajkarami.crimecraft.fragment

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.activity.OnBackPressedCallback
import androidx.core.view.doOnLayout
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import ir.hajkarami.crimecraft.CrimeDetailViewModel
import ir.hajkarami.crimecraft.CrimeDetailViewModelFactory
import ir.hajkarami.crimecraft.R
import ir.hajkarami.crimecraft.database.Crime
import ir.hajkarami.crimecraft.databinding.FragmentCrimeDetailBinding
import ir.hajkarami.crimecraft.getScaledBitmap
import kotlinx.coroutines.launch
import java.util.Date
import java.io.File

private const val DATE_FORMAT = "EEE, MMM, dd"

class CrimeDetailFragment : Fragment() {

    private var _binding: FragmentCrimeDetailBinding? = null
    private val binding
        get() = checkNotNull(_binding)

    private val args: CrimeDetailFragmentArgs by navArgs()

    private val crimeDetailViewModel: CrimeDetailViewModel by viewModels {
        CrimeDetailViewModelFactory(args.crimeId)
    }

    private val selectSuspect =
        registerForActivityResult(ActivityResultContracts.PickContact()) { uri: Uri? ->
            uri?.let { parseContactSelection(it) }
        }

    private val takePhoto =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { didTakePhoto: Boolean ->
            if (didTakePhoto && photoName != null) {
                crimeDetailViewModel.updateCrime { oldCrime ->
                    oldCrime.copy(photoFileName = photoName)
                }
            }
        }
    private var photoName: String? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCrimeDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            editText.doOnTextChanged { text, _, _, _ ->
                crimeDetailViewModel.updateCrime { oldCrime ->
                    oldCrime.copy(title = text.toString())
                }
            }
            crimeSolved.setOnCheckedChangeListener { _, isChecked ->
                crimeDetailViewModel.updateCrime { oldCrime -> oldCrime.copy(isSolved = isChecked) }
            }
            crimeSuspect.setOnClickListener {
                selectSuspect.launch(null)
            }
            val selectSuspectIntent = selectSuspect.contract.createIntent(
                requireContext(),
                null
            )
            crimeSuspect.isEnabled = canResolveIntent(selectSuspectIntent)

            crimeCamera.setOnClickListener {
                photoName = "IMG_${Date()}.JPG"
                val photoFile = File(requireContext().applicationContext.filesDir, photoName)
                val photoUri = FileProvider.getUriForFile(
                    requireContext(),
                    "ir.hajkarami.crimecraft.fileprovider",
                    photoFile
                )
                takePhoto.launch(photoUri)
            }

            val photoNameTemp = "IMG_${System.currentTimeMillis()}.JPG"
            val photoFileTemp = File(requireContext().applicationContext.filesDir, photoNameTemp)
            val photoUriTemp = FileProvider.getUriForFile(
                requireContext(),
                "ir.hajkarami.crimecraft.fileprovider",
                photoFileTemp
            )

            val captureImageIntent = takePhoto.contract.createIntent(
                requireContext(),
                photoUriTemp
            )
            crimeCamera.isEnabled = canResolveIntent(captureImageIntent)

        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val currentTitle = binding.editText.text.toString()
                    if (currentTitle.isBlank()) {
                        Snackbar.make(binding.root, "Title cannot be empty", Snackbar.LENGTH_SHORT)
                            .show()

                    } else {
                        isEnabled = false
                        findNavController().popBackStack()
                    }
                }
            }
        )
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED,
                crimeDetailViewModel.crime.collect { crime ->
                    crime?.let { updateUi(it) }
                })
        }

        setFragmentResultListener(
            DatePickerFragment.REQUEST_KEY_DATE
        ) { _, bundle ->
            val timestamp = bundle.getLong(DatePickerFragment.BUNDLE_KEY_DATE)
            val selectedDate = Date(timestamp)
            crimeDetailViewModel.updateCrime { it.copy(date = selectedDate) }
        }
    }


    private fun updateUi(crime: Crime) {
        binding.apply {
            if (editText.text.toString() != crime.title) editText.setText(crime.title)
            crimeDate.setOnClickListener {
                findNavController().navigate(
                    CrimeDetailFragmentDirections.selectDate(crime.date)
                )
            }
            crimeSolved.isChecked = crime.isSolved
            crimeReport.setOnClickListener {
                val reportIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, getCrimeReport(crime))
                    putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_suspect))
                }

                val chooserIntent =
                    Intent.createChooser(reportIntent, getString(R.string.send_report))
                startActivity(chooserIntent)
            }
            crimeSuspect.text = crime.suspect.ifEmpty {
                getString(R.string.crime_suspect_text)
            }
            updatePhoto(crime.photoFileName)


        }

    }



    private fun getCrimeReport(crime: Crime): String {
        val solvedString =
            if (crime.isSolved) getString(R.string.crime_report)
            else getString(R.string.crime_report_no_suspect)
        val dataString = DateFormat.format(DATE_FORMAT, crime.date).toString()
        val suspectText =
            if (crime.suspect.isBlank()) getString(R.string.crime_report_no_suspect)
            else getString(R.string.crime_report_subject)

        return getString(
            R.string.crime_report,
            crime.title,
            dataString,
            solvedString,
            suspectText
        )
    }

    private fun parseContactSelection(contactUri: Uri) {
        val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
        val queryCursor = requireActivity().contentResolver
            .query(contactUri, queryFields, null, null, null)
        queryCursor?.use { cursor ->
            if (cursor.moveToFirst()) {
                val suspect = cursor.getString(0)
                crimeDetailViewModel.updateCrime { oldCrime ->
                    oldCrime.copy(suspect = suspect)
                }
            }
        }
    }

    private fun canResolveIntent(intent: Intent): Boolean {

        val packageManager: PackageManager = requireActivity().packageManager
        val resolvedActivity: ResolveInfo? =
            packageManager.resolveActivity(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
        return resolvedActivity != null
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun updatePhoto(photoFileName: String?) {
        if (binding.crimePhoto.tag != photoFileName) {
            val photoFile = photoFileName?.let {
                File(requireContext().applicationContext.filesDir, it)
            }

            if (photoFile?.exists() == true) {
                binding.crimePhoto.doOnLayout { measuredView ->
                    val scaledBitmap = getScaledBitmap(
                        photoFile.path,
                        measuredView.width,
                        measuredView.height
                    )
                    binding.crimePhoto.setImageBitmap(scaledBitmap)
                    binding.crimePhoto.tag = photoFileName
                }
            } else {
                binding.crimePhoto.setImageBitmap(null)
                binding.crimePhoto.tag = null
            }
        }
    }

}

