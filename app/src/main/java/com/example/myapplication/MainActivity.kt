package com.example.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {
    val TAG = this.javaClass.simpleName

   private  lateinit var  mCircleView:CircleProgressView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mCircleView = findViewById<CircleProgressView>(R.id.circle_center)
        findViewById<TextView>(R.id.tv_start).setOnClickListener {
//
            mCircleView.start()
        }
        findViewById<TextView>(R.id.tv_success).setOnClickListener {
            mCircleView.stop(true)
        }
        findViewById<TextView>(R.id.tv_error).setOnClickListener {
            mCircleView.stop(false)
        }
    }

}