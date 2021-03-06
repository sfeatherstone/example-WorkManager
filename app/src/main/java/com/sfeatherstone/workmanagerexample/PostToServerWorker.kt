package com.sfeatherstone.workmanagerexample

import android.content.Context
import android.util.Log
import androidx.work.*
import kotlinx.coroutines.delay
import java.util.*

class PostToServerWorker(appContext: Context, workerParams: WorkerParameters): CoroutineWorker(appContext, workerParams) {

    private val random by lazy { Random(Date().time)}

    override suspend fun doWork(): Result {
        // Read input data
        val jobDuration = inputData.getInt(JOB_TIME, 100)
        val jobSuccessRate = inputData.getInt(JOB_SUCCESS_CHANCE, 0)
        val maxAttempts = inputData.getInt(ATTEMPT_RETRIES, 5)

        log("Starting job", maxAttempts)

        //Fake some work with progress
        setProgress(workDataOf(Progress to 0))
        for (i in 1..10) {
            // Simulate work
            delay((jobDuration / 10).toLong())
            setProgress(workDataOf(Progress to i * 10))
        }

        // Add a random fail
        if (random.nextInt(100) > jobSuccessRate) {
            // Get number of attempts made against max attempts we have set
            if (runAttemptCount <= maxAttempts) {
                // Try again
                log("Retrying job", maxAttempts)
                return  Result.retry()
            }
            log("Failing job", maxAttempts)
            return Result.failure()
        }

        // Return the output
        log("Finishing successful job", maxAttempts)
        return Result.success()
    }

    private fun log(message: String, maxAttempts: Int) {
        Log.d("PostToServerWorker", "$message $id ${tags.joinToString(separator = ",")} attempt:$runAttemptCount of $maxAttempts")
    }


    companion object {
        fun submitPost(applicationContext: Context, duration: Int, successPercent:Int, tagName: String): UUID {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val restWorkRequest = OneTimeWorkRequestBuilder<PostToServerWorker>()
                .setInputData(workDataOf(JOB_TIME to duration,
                    JOB_SUCCESS_CHANCE to successPercent,
                    ATTEMPT_RETRIES to 2))
                .setConstraints(constraints)
                .addTag(tagName)
                .build()

            val workManager = WorkManager.getInstance(applicationContext)
            workManager.enqueue(restWorkRequest)

            return restWorkRequest.id
        }

        const val Progress = "Progress"
        private const val JOB_TIME = "JOB_TIME_INT"
        private const val JOB_SUCCESS_CHANCE = "JOB_FAIL_CHANCE_FLOAT"
        private const val ATTEMPT_RETRIES = "ATTEMPT_RETRIES_INT"
    }

}