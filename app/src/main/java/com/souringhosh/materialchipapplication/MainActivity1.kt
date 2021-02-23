package com.souringhosh.materialchipapplication

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.souringhosh.materialchipapplication.recycler.HashtagAdapter
import com.souringhosh.materialchipapplication.recycler.SuggestionAdapter
import com.souringhosh.materialchipapplication.repository.MockSuggestionRepository
import com.souringhosh.materialchipapplication.repository.SuggestionInteractor
import kotlinx.android.synthetic.main.activity_main1.*

class MainActivity1 : AppCompatActivity() {

    private val hashtagAdapter: HashtagAdapter = HashtagAdapter(
            onHashtagDeleteClick = {
                viewModel.deleteHashtag(it)
            },
            onHashtagSelected = {
                viewModel.selectActiveHashtag(it)
            },
            hashtagTextWatcher = object : HashtagTextWatcher {
                private val beforeTexts: MutableMap<Int, String> = mutableMapOf()
                override fun beforeTextChanged(position: Int, s: CharSequence?, start: Int, count: Int, after: Int) {
                    val beforeText = s?.toString() ?: ""
                    beforeTexts[position] = beforeText
                }

                override fun onTextChanged(position: Int, s: CharSequence?, start: Int, before: Int, count: Int) {
                    val beforeText = beforeTexts[position] ?: ""
                    val afterText = s?.toString() ?: ""
                    viewModel.editHashtag(position, beforeText, afterText)
                }
            }
    )
    private val suggestionAdapter: SuggestionAdapter = SuggestionAdapter { viewModel.selectSuggestion(it) }
    private val viewModel: ViewModelImpl = ViewModelImpl(
            SuggestionInteractor(
                    MockSuggestionRepository()
            )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main1)

        suggestionRecycler.apply {
            adapter = suggestionAdapter
            layoutManager = LinearLayoutManager(this@MainActivity1, LinearLayoutManager.VERTICAL, false)
        }
        hashtagRecycler.apply {
            adapter = hashtagAdapter
            layoutManager = LinearLayoutManager(this@MainActivity1, LinearLayoutManager.HORIZONTAL, false)
        }

        viewModel.hashtags.observe(this, Observer {
            hashtagAdapter.hashtags = it
        })
        viewModel.suggestions.observe(this, Observer {
            suggestionAdapter.suggestions = it
        })
    }

}

interface HashtagTextWatcher {
    fun beforeTextChanged(position: Int, s: CharSequence?, start: Int, count: Int, after: Int)
    fun onTextChanged(position: Int, s: CharSequence?, start: Int, before: Int, count: Int)
}
