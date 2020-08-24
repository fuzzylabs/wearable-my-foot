package ai.fuzzylabs.insoleandroid

import android.content.Context
import java.io.File
import java.time.Instant

class CSVWriter(val context: Context) {

    fun saveResults(iter: Iterable<IMUReading>?) {
        val filename = "${Instant.now()}.csv"
        context.openFileOutput(filename, Context.MODE_PRIVATE).writer().use {
            it.write("datetime,aX,aY,aZ,gX,gY,gZ\n")
            iter?.forEach { reading -> it.write(reading.toCSVRow()) }
        }
    }
}