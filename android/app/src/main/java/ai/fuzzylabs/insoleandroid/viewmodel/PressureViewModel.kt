package ai.fuzzylabs.insoleandroid.viewmodel

import ai.fuzzylabs.insoleandroid.model.PressureSensorEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData

class PressureViewModel : ViewModel() {
    private val pressureSensorLiveData = PressureSensorLiveData()

    fun getSensors(): LiveData<PressureSensorEvent> {
        return pressureSensorLiveData
    }
}
