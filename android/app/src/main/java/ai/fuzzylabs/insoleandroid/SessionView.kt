package ai.fuzzylabs.insoleandroid

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import java.io.File

private val TAG = SessionView::class.java.simpleName

class SessionView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : ConstraintLayout(
    context,
    attrs,
    defStyleAttr
) {

    private var filenamePrefix: String? = null

    private var csvExportButton: Button? = null
    private var gpxExportButton: Button? = null
    private val sessionNameTextView: TextView by lazy { findViewById(R.id.sessionNameTextView) }

    init {
        inflate(context, R.layout.view_session, this)

        csvExportButton = findViewById(R.id.csvExportButton)
        csvExportButton?.setOnClickListener { onCsvExportButton() }

        gpxExportButton = findViewById(R.id.gpxExportButton)
        gpxExportButton?.setOnClickListener { onGpxExportButton() }
    }

    fun setSession(session: SavedSession) {
        filenamePrefix = session.filenamePrefix
        sessionNameTextView.text = filenamePrefix

        csvExportButton?.isEnabled = session.isCsv
        gpxExportButton?.isEnabled = session.isGpx
    }

    private fun onCsvExportButton() {
        Log.d(TAG, "Export ${filenamePrefix}.csv")
        val path: File? = context.getExternalFilesDir(null)
        val filename = "$filenamePrefix.csv"
        val uri = FileProvider.getUriForFile(context, "ai.fuzzylabs.fileprovider", File(path, filename))
        shareFile(filename, uri, "text/csv")
    }

    private fun onGpxExportButton() {
        Log.d(TAG, "Export ${filenamePrefix}.gpx")
        val path: File? = context.getExternalFilesDir(null)
        val filename = "$filenamePrefix.gpx"
        val uri = FileProvider.getUriForFile(context, "ai.fuzzylabs.fileprovider", File(path, filename))
        shareFile(filename, uri, "text/xml")
    }

    private fun shareFile(filename: String, uri: Uri, mimetype: String) {
        val shareIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = mimetype
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share session: $filename"))
    }
}