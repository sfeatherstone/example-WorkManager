package com.sfeatherstone.workmanagerexample

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.*

class MainViewModel : ViewModel() {

    data class JobDescription(val index: Int, val state: String, internal val liveData: LiveData<WorkInfo>, internal val observer: Observer<WorkInfo>)
    private val currentJobs = mutableMapOf<UUID, JobDescription>()

    var jobsByTag: LiveData<List<WorkInfo>>? = null
    var jobsByTagObserver: Observer<List<WorkInfo>>? = null

    private val mutableJobs: MutableLiveData<Map<UUID, JobDescription>> = MutableLiveData(currentJobs)
    val jobs: LiveData<Map<UUID, JobDescription>> = mutableJobs

    fun postToServer(applicationContext: Context, successPercent: Int, index: Int) {
        val workManager = WorkManager.getInstance(applicationContext)

        val uuid = PostToServerWorker.submitPost(applicationContext, 3000, successPercent, "Example-$index")

        val liveData = workManager.getWorkInfoByIdLiveData(uuid)

        val observer = Observer<WorkInfo> {
            updateLiveData(it)
        }
        currentJobs[uuid] = JobDescription(index, "Submitted", liveData, observer)
        liveData.observeForever(observer)
    }

    fun getAllJobs(applicationContext: Context) {
        val workManager = WorkManager.getInstance(applicationContext)

        jobsByTag = workManager.getWorkInfosByTagLiveData("com.sfeatherstone.workmanagerexample.PostToServerWorker")
        jobsByTagObserver = Observer {
            for(wi in it) {
                if (!currentJobs.containsKey(wi.id)) {
                    val observer = Observer<WorkInfo> {
                        updateLiveData(it)
                    }
                    val liveData = workManager.getWorkInfoByIdLiveData(wi.id)
                    currentJobs[wi.id] = JobDescription(0, "Historic", liveData, observer)
                    liveData.observeForever(observer)
                }
            }
        }
        jobsByTag?.observeForever(jobsByTagObserver!!)
    }

    override fun onCleared() {
        super.onCleared()
        // Need to clear observers as we have no lifecycle here
        for(item in currentJobs) {
            item.value.liveData.removeObserver(item.value.observer)
        }
        jobsByTag?.let {
            it.removeObserver(jobsByTagObserver!!)
        }
    }

    private fun updateLiveData(workInfo: WorkInfo) {
        // Copy jobDescription with new status
        val newJobDescription = currentJobs[workInfo.id]
            ?.copy(state = "id:${workInfo.id}\ntags:${workInfo.tags.joinToString(separator = ", ")}\nstate:${workInfo.state.name} progress:${workInfo.progress}")

        newJobDescription?.let {
            currentJobs[workInfo.id] = it
        }
        // Trigger change in LD
        mutableJobs.value = currentJobs
    }

}