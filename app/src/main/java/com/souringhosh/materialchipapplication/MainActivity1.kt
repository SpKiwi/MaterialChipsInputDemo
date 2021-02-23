package com.souringhosh.materialchipapplication

import android.os.Bundle
import android.view.View
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
            onHashtagDeleteClick = { viewModel.deleteHashtag(it) },
            onHashtagSelected = { viewModel.selectActiveHashtag(it) },
            editHashtag = { position, before, after -> viewModel.editHashtag(position, before, after) },
            keyCallbacks = object : KeyCallbacks {
                override fun onDeletePressed(position: Int) {
                    viewModel.deleteFromHashtag(position)
                }

                override fun onFinishInputPresses(position: Int) {
                    // TODO viewModel.keyboardAction()
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
        viewModel.error.observe(this, Observer {
            if (it == null) {
                hashtagError.visibility = View.INVISIBLE
            } else {
                hashtagError.visibility = View.VISIBLE
                // TODO change text message
            }
        })
    }

}

interface KeyCallbacks {
    fun onDeletePressed(position: Int)
    fun onFinishInputPresses(position: Int)
}