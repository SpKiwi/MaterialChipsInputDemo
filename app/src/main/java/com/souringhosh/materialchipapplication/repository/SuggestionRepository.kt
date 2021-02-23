package com.souringhosh.materialchipapplication.repository

import com.souringhosh.materialchipapplication.utils.ui.adapter.ListItem
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

interface SuggestionRepository {

    fun getSuggestions(): Single<List<Suggestion>>

}

class MockSuggestionRepository : SuggestionRepository {

    override fun getSuggestions(): Single<List<Suggestion>> {
        return Single.timer(500, TimeUnit.MILLISECONDS, Schedulers.io())
                .map {
                    mockData
                            .shuffled()
                            .take(10)
                }
    }

    private val mockData: List<Suggestion> = listOf(
            Suggestion("music"),
            Suggestion("songs"),
            Suggestion("lifestyle"),
            Suggestion("life"),
            Suggestion("swag"),
            Suggestion("guitar"),
            Suggestion("hobby"),
            Suggestion("fashion"),
            Suggestion("lol"),
            Suggestion("cats"),
            Suggestion("dogs"),
            Suggestion("cute"),
            Suggestion("like4like"),
            Suggestion("_no_1_"),
            Suggestion("null"),
            Suggestion("kitty"),
            Suggestion("kitten"),
            Suggestion("mlp"),
            Suggestion("freedom"),
            Suggestion("belarus"),
            Suggestion("minsk")
    )
}

inline class Suggestion(
        val value: String
) : ListItem {
    override val id: Any get() = value
}