package com.samyak2403.scratchview

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.samyak2403.scratchview.databinding.ActivityMainBinding
import com.samyak2403.scratchview.scratchview.ScratchView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Access views through binding
        val scratchView = binding.scratchView
        val maskButton = binding.mask

        scratchView.setRevealListener(object : ScratchView.IRevealListener {
            override fun onRevealed(scratchView: ScratchView?) {
                Toast.makeText(applicationContext, "Revealed", Toast.LENGTH_LONG).show()
            }

            override fun onRevealPercentChangedListener(scratchView: ScratchView?, percent: Float) {
                if (percent >= 0.5) {
                    Log.d("Reveal Percentage", "onRevealPercentChangedListener: $percent")
                }
            }
        })

        maskButton.setOnClickListener {
            // Assuming `mask` is a method in `ScratchView`
            scratchView.mask()
        }
    }
}
