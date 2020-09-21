package ai.fuzzylabs.insoleandroid.imu

import org.xmlpull.v1.XmlSerializer
import java.time.Instant
import kotlin.math.roundToInt

class IMUSessionElement(val time: Instant, val cadence: Double) {
    fun fillGPX(gpx: XmlSerializer) {
        gpx.startTag(null, "trkpt")
        gpx.attribute(null, "lat", "0")
        gpx.attribute(null, "lon", "0")

        gpx.startTag(null, "time")
        gpx.text(this.time.toString())
        gpx.endTag(null, "time")

        gpx.startTag(null, "extensions")
        gpx.startTag("http://www.garmin.com/xmlschemas/TrackPointExtension/v1", "TrackPointExtension")
        gpx.startTag("http://www.garmin.com/xmlschemas/TrackPointExtension/v1", "cad")
        gpx.text(this.cadence.roundToInt().toString())
        gpx.endTag("http://www.garmin.com/xmlschemas/TrackPointExtension/v1", "cad")
        gpx.endTag("http://www.garmin.com/xmlschemas/TrackPointExtension/v1", "TrackPointExtension")
        gpx.endTag(null, "extensions")

        gpx.endTag(null, "trkpt")
    }
}