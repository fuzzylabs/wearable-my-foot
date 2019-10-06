package ai.fuzzylabs.insoleandroid.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import ai.fuzzylabs.insoleandroid.R
import ai.fuzzylabs.insoleandroid.model.PressureSensorEvent
import ai.fuzzylabs.insoleandroid.viewmodel.PageViewModel
import ai.fuzzylabs.insoleandroid.viewmodel.PressureViewModel
import ai.fuzzylabs.insoleandroid.view.PressureView
import androidx.lifecycle.Observer

/**
 * A placeholder fragment containing a simple view.
 */
class PlaceholderFragment : Fragment() {

    private lateinit var pageViewModel: PageViewModel
    private lateinit var pressureViewModel: PressureViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel::class.java).apply {
            setIndex(arguments?.getInt(ARG_SECTION_NUMBER) ?: 1)
        }
        pressureViewModel = ViewModelProviders.of(activity!!).get(PressureViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_main, container, false)
        val pressureView: PressureView = root.findViewById(R.id.pressure_view)
        pressureViewModel.pressureSensorLiveData.observe(this, Observer<PressureSensorEvent> {
            pressureView.react(it)
        })
        return root
    }

    companion object {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private const val ARG_SECTION_NUMBER = "section_number"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        @JvmStatic
        fun newInstance(sectionNumber: Int): PlaceholderFragment {
            return PlaceholderFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, sectionNumber)
                }
            }
        }
    }
}