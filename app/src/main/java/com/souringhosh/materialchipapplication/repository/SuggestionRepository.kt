package com.souringhosh.materialchipapplication.repository

import com.souringhosh.materialchipapplication.utils.ui.adapter.ListItem
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timer

interface SuggestionRepository {

    suspend fun getSuggestions(): List<Suggestion>

}

class MockSuggestionRepository : SuggestionRepository {

    override suspend fun getSuggestions(): List<Suggestion> {
        delay(500)
        return mockData
                .shuffled()
                .take(10)
//        return Single.timer(500, TimeUnit.MILLISECONDS, Schedulers.io())
//                .map {
//                    mockData
//                            .shuffled()
//                            .take(10)
//                }
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