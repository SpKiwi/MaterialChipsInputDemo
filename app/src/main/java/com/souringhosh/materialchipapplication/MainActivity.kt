package com.souringhosh.materialchipapplication

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.souringhosh.materialchipapplication.recycler.SuggestionAdapter
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        horizontal_scroll_view.background = textInputEditText.background
        textInputEditText.background = null

        val suggestionAdapter = SuggestionAdapter {

        }
        suggestionRecycler.apply {
            adapter = suggestionAdapter
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.INVALID_OFFSET, false)
        }

        /**
         * Listening to text changes in the TextInputEditText field and generate new chip on entering a comma
         **/
        textInputEditText.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable?) {
                val trimmed = s.toString().trim { it <= ' ' }
                if (trimmed.length > 1 && trimmed.endsWith(",")) {
                    val chip = HashtagView(this@MainActivity)
                    chip.setText(trimmed.substring(0, trimmed.length - 1))
                    chipGroup.addView(chip)
                    chip.setOnCloseIconClickListener {
                        chipGroup.removeView(chip)
                    }
                    s?.clear()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        /*
        * Deleting chips on pressing backspace on keypad*/
        textInputEditText.setOnKeyListener { _, _, event ->
            if (event != null && event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_DEL) {
                if (textInputEditText.length() == 0 && chipGroup.childCount > 0) {
                    val chip = chipGroup.getChildAt(chipGroup.childCount - 1)
                    chipGroup.removeView(chip)
                }
            }
            false
        }
    }
}
