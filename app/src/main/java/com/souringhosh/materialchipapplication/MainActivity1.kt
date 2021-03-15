package com.souringhosh.materialchipapplication

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_DRAGGING
import com.souringhosh.materialchipapplication.recycler.HashtagAdapter
import com.souringhosh.materialchipapplication.recycler.SuggestionAdapter
import com.souringhosh.materialchipapplication.repository.MockSuggestionRepository
import com.souringhosh.materialchipapplication.repository.HashtagSuggestionInteractor
import com.souringhosh.materialchipapplication.repository.MockHashtagSuggestionRepositoryImpl
import kotlinx.android.synthetic.main.activity_main1.*

class MainActivity1 : AppCompatActivity() {

    private val hashtagAdapter: HashtagAdapter = HashtagAdapter(
            onHashtagDeleteClick = { viewModel.deleteHashtag(it) },
            onHashtagSelected = { viewModel.selectActiveHashtag(it) },
            onHashtagEditListener = object : OnHashtagEditListener {
                override fun onHashtagEdit(position: Int, before: String, after: String) {
                    viewModel.editHashtag(position, before, after)
                }
            },
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
            MockHashtagSuggestionRepositoryImpl(),
            HashtagSuggestionInteractor(
                    MockSuggestionRepository()
            )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main1)
        parentView.setOnTouchListener { v, event ->
            currentFocus?.let {
                (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(it.windowToken, 0)
            }
            true
        }

        suggestionRecycler.apply {
            adapter = suggestionAdapter
            layoutManager = LinearLayoutManager(this@MainActivity1, LinearLayoutManager.VERTICAL, false)
        }
        hashtagRecycler.apply {
            adapter = hashtagAdapter
            layoutManager = LinearLayoutManager(this@MainActivity1, LinearLayoutManager.HORIZONTAL, false)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == SCROLL_STATE_DRAGGING) {
                        currentFocus?.let {
                            (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(it.windowToken, 0)
                        }
                    }
                }
            })
        }

        viewModel.hashtags.observe(this, Observer { hashtags ->
            hashtagAdapter.items = hashtags
            val readyHashtagsSize = hashtags.count { it.state == Hashtag.State.READY }
            if (readyHashtagsSize == hashtags.size - 1) {
                hashtagRecycler.scrollToPosition(hashtags.lastIndex)
            }
            hashtagRecycler.focusedChild
        })

        viewModel.suggestions.observe(this, Observer {
            suggestionAdapter.items = it
        })
        viewModel.error.observe(this, Observer {
            if (it == null) {
                hashtagError.visibility = View.INVISIBLE
            } else {
                hashtagError.visibility = View.VISIBLE
                hashtagError.text = it.toString()
            }
        })
        viewModel.start()
    }

}

interface KeyCallbacks {
    fun onDeletePressed(position: Int)
    fun onFinishInputPresses(position: Int)
}

interface OnHashtagEditListener {
    fun onHashtagEdit(position: Int, before: String, after: String)
}