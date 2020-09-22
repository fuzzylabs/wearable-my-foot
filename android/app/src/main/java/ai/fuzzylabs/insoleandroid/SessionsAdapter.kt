package ai.fuzzylabs.insoleandroid

import android.util.Log
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

private val TAG = SessionsAdapter::class.java.simpleName

data class SavedSession(val filenamePrefix: String, val isCsv: Boolean, val isGpx: Boolean)

class SessionsAdapter(private val dataset: List<SavedSession>): RecyclerView.Adapter<SessionsAdapter.SessionsViewHolder>() {

    class SessionsViewHolder(val view: SessionView) : RecyclerView.ViewHolder(view)



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionsViewHolder {
        val view = SessionView(parent.context)
        view.layoutParams

        return SessionsViewHolder(view)
    }

    override fun onBindViewHolder(holder: SessionsViewHolder, position: Int) {
        holder.view.setSession(dataset[position])
        Log.d(TAG, "Holder $position populated with ${dataset[position]}")
    }

    override fun getItemCount(): Int {
        Log.d(TAG, "Dataset size: ${dataset.size}")
        return dataset.size
    }
}