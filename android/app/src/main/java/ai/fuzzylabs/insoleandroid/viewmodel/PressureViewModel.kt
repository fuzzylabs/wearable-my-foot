package ai.fuzzylabs.insoleandroid.viewmodel

import ai.fuzzylabs.insoleandroid.model.PressureSensorEvent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlin.random.Random

class PressureViewModel : ViewModel() {
    private val sensors = mapOf(
        Pair(25, Pair(300, 200)),
        Pair(33, Pair(800, 260)),
        Pair(32, Pair(300, 800)),
        Pair(36, Pair(800, 820)),
        Pair(34, Pair(700, 1400))
    )
    val pressureSensorLiveData = MediatorLiveData<PressureSensorEvent>()

    fun update(reading: String) {
        val components = reading.split(":")
        if (components.size == 2) {
            val sensor = components[0].toInt()
            val pressure = components[1].toFloat()
            pressureSensorLiveData.postValue(PressureSensorEvent(sensors[sensor]!!.first, sensors[sensor]!!.second, pressure, sensor))
        } else {
            Log.e("", "Unable to parse sensor reading $reading")
        }
    }
}
