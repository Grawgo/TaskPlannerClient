package com.taskplanner.app

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.taskplanner.app.data.local.SearchHistoryDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchHistoryDaoTest {
    @Test fun keepsTenLatestQueriesAndSeparatesUsers() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, SearchHistoryDatabase::class.java).build()
        try {
            val dao = database.searchHistoryDao()
            for (index in 1..12) dao.remember("user-a", "запрос $index", 1_000L)
            assertEquals((12 downTo 3).map { "запрос $it" }, dao.observe("user-a").first())

            dao.remember("user-a", "запрос 5", 1_000L)
            assertEquals("запрос 5", dao.observe("user-a").first().first())
            assertEquals(10, dao.observe("user-a").first().size)

            dao.remember("user-b", "личный запрос", 1_000L)
            assertEquals(listOf("личный запрос"), dao.observe("user-b").first())
            dao.clear("user-a")
            assertEquals(emptyList<String>(), dao.observe("user-a").first())
            assertEquals(listOf("личный запрос"), dao.observe("user-b").first())
        } finally {
            database.close()
        }
    }
}
