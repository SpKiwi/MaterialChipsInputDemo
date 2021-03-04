package com.souringhosh.materialchipapplication.repository

import com.souringhosh.materialchipapplication.utils.ui.adapter.ListItem
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timer
import kotlin.random.Random

interface SuggestionRepository {

    suspend fun getSuggestions(search: String): SearchResultModel

}

class MockSuggestionRepository : SuggestionRepository {

    override suspend fun getSuggestions(search: String): SearchResultModel {
        delay(500)
        val items = mockData.shuffled()
        val isResponseValid = bannedList.any {
            search.contains(it)
        }
        return SearchResultModel(
                id = Random.nextInt().toString(),
                itemsCount = items.size,
                items = items,
                isResponseValid = isResponseValid
        )
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

    private val bannedList: List<String> = listOf(
            "fuck",
            "fag",
            "bitch",
            "ass",
            "sex",
            "boob"
    )
}

inline class Suggestion(
        val value: String
) : ListItem {
    override val id: Any get() = value
}

data class SearchResultModel(
        val id: String,
        val itemsCount: Int,
        val items: List<Suggestion>,
        val isResponseValid: Boolean
)
