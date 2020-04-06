package com.sfeatherstone.workmanagerexample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    val viewModel: MainViewModel by viewModels()
    var index = 1

    fun submitWork(precentPass: Int) {
        viewModel.postToServer(applicationContext,precentPass, index++)
        submitWorkButton80.text = "Submit 80% (${index})"
        submitWorkButton20.text = "Submit 20% (${index})"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        submitWorkButton80.text = "Submit 80% (${index})"
        submitWorkButton80.setOnClickListener {
            submitWork(80)
        }

        submitWorkButton20.text = "Submit 20% (${index})"
        submitWorkButton20.setOnClickListener {
            submitWork(20)
        }

        viewModel.jobs.observe(this, Observer {
            val sb = StringBuilder()
            for(element in it) {
                sb.appendln(element.value.state)
            }
            status.text = sb.toString()
        })


    }
}
