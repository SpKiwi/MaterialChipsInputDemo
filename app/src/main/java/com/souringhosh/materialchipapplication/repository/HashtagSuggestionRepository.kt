package com.souringhosh.materialchipapplication.repository

import kotlinx.coroutines.delay

interface HashtagSuggestionRepository {
    suspend fun getHashtagSuggestions(): List<String>
}

class MockHashtagSuggestionRepositoryImpl : HashtagSuggestionRepository {

    override suspend fun getHashtagSuggestions(): List<String> {
        delay(3_000L)
        return listOf(
                "previous1",
                "previous2"
        )
    }
}