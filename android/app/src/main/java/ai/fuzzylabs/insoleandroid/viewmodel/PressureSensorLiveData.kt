package ai.fuzzylabs.insoleandroid.viewmodel

import ai.fuzzylabs.insoleandroid.bluetooth.PressureSensorReceiver
import ai.fuzzylabs.insoleandroid.model.PressureSensorEvent
import androidx.lifecycle.LiveData

class PressureSensorLiveData : LiveData<PressureSensorEvent>() {
    private val eventSource = PressureSensorReceiver()

    private val listener = { event: PressureSensorEvent ->
        value = event
    }

    override fun onActive() {
        listener(PressureSensorEvent(200, 300, 3.2f))
        //eventSource.requestUpdates(listener)
    }

    override fun onInactive() {
        //eventSource.removeUpdates(listener)
    }
}