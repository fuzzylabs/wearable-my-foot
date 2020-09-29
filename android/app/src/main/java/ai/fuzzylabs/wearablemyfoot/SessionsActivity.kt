package ai.fuzzylabs.wearablemyfoot

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

private val TAG = SessionsActivity::class.java.simpleName

/**
 * Activity that displays recorded and saved sessions
 *
 * Allows exporting CSV and GPX files for sessions (if available)
 */
class SessionsActivity : AppCompatActivity() {
    private val viewManager: RecyclerView.LayoutManager by lazy { LinearLayoutManager(this) }
    private val viewAdapter: RecyclerView.Adapter<*> by lazy { SessionsAdapter(getSavedSessions()) }
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sessions)

        getSavedSessions()
        recyclerView = findViewById<RecyclerView>(R.id.sessionListView).apply{
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }
    }

    private fun getSavedSessions(): List<SavedSession> {
        val path: File? = applicationContext.getExternalFilesDir(null)
        val csvs = path?.list { _, s -> s.contains(".csv") }?.map { it.replace(".csv", "") }
        val gpxs = path?.list { _, s -> s.contains(".gpx") }?.map { it.replace(".gpx", "") }
        var both = csvs?.toSet() ?: setOf()
        both = if(gpxs != null) both.union(gpxs) else both
        val bothList = both.toList().sortedDescending()
        return bothList.map {
            SavedSession(it, csvs?.contains(it) ?: false, gpxs?.contains(it) ?: false)
        }
    }
}