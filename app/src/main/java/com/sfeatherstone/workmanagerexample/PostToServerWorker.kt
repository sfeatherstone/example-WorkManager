package com.sfeatherstone.workmanagerexample

import android.content.Context
import android.util.Log
import androidx.work.*
import java.lang.Thread.*
import java.util.*

class PostToServerWorker(appContext: Context, workerParams: WorkerParameters): CoroutineWorker(appContext, workerParams) {

    private val random by lazy { Random(Date().time)}
    private var jobDuration: Int = 100
    private var jobFail: Int = 100
    private lateinit var jobName: String
    private var maxAttempts: Int = 5


    override suspend fun doWork(): Result {
        // Read input data
        jobDuration = inputData.getInt(JOB_TIME, 100)
        jobFail = inputData.getInt(JOB_FAIL_CHANCE, 0)
        maxAttempts = inputData.getInt(ATTEMPT_RETRIES, 5)

        log("Starting job")

        //Fake some work with progress
        setProgress(workDataOf(Progress to 0))
        for (i in 1..10) {
            sleep((jobDuration / 10).toLong())
            setProgress(workDataOf(Progress to i * 10))
        }

        // Add a random fail
        if (random.nextInt(100) > jobFail) {
            if (runAttemptCount <= maxAttempts) {
                // Try again
                log("Retrying job")
                return  Result.retry()
            }
            log("Failing job")
            return Result.failure()
        }

        // Return the output
        log("Finishing successful job")
        return Result.success()
    }

    private fun log(message: String) {
        Log.d("PostToServerWorker", "$message $id ${tags.joinToString(separator = ",")} attempt:$runAttemptCount of $maxAttempts")
    }


    companion object {
        fun submitPost(applicationContext: Context, duration: Int, failPercent:Int, tagName: String): UUID {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val restWorkRequest = OneTimeWorkRequestBuilder<PostToServerWorker>()
                .setInputData(workDataOf(JOB_TIME to duration,
                    JOB_FAIL_CHANCE to failPercent,
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
        private const val JOB_FAIL_CHANCE = "JOB_FAIL_CHANCE_FLOAT"
        private const val JOB_NAME = "JOB_NAME_STRING"
        private const val ATTEMPT_RETRIES = "ATTEMPT_RETRIES_INT"
    }

}