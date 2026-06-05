package com.getmecookies.licenseplatequest.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.getmecookies.licenseplatequest.data.local.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for the player roster repository (SPEC §6/§7): soft-delete preserves history, duplicate
 * names are case-insensitive, and the roster stays alphabetized.
 */
@RunWith(RobolectricTestRunner::class)
class PlayerRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: PlayerRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = PlayerRepository(db.playerDao(), db.tripPlayerDao(), db.eventLogDao())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun addPlayer_appearsInRoster() = runBlocking {
        repo.addPlayer("Alice")
        assertEquals(listOf("Alice"), repo.observePlayers().first().map { it.name })
    }

    @Test
    fun deletePlayer_hidesFromRoster_butKeepsRow() = runBlocking {
        val id = repo.addPlayer("Bob")
        repo.deletePlayer(id)

        assertTrue(repo.observePlayers().first().none { it.id == id })
        val row = db.playerDao().getById(id)
        assertNotNull(row)
        assertTrue(row!!.deleted)
    }

    @Test
    fun deletedPlayer_freesNameForReuse() = runBlocking {
        val id = repo.addPlayer("Alice")
        repo.deletePlayer(id)
        assertFalse(repo.nameExists("Alice"))
    }

    @Test
    fun nameExists_isCaseInsensitive() = runBlocking {
        repo.addPlayer("Alice")
        assertTrue(repo.nameExists("alice"))
        assertTrue(repo.nameExists("ALICE"))
        assertFalse(repo.nameExists("Bob"))
    }

    @Test
    fun nameExists_excludeIdAllowsSelf() = runBlocking {
        val id = repo.addPlayer("Alice")
        assertFalse(repo.nameExists("Alice", excludeId = id))
        assertTrue(repo.nameExists("Alice"))
    }

    @Test
    fun renamePlayer_updatesName() = runBlocking {
        val id = repo.addPlayer("Bob")
        repo.renamePlayer(id, "Bobby")
        assertEquals("Bobby", repo.observePlayers().first().first { it.id == id }.name)
    }

    @Test
    fun setPlayerColor_persists() = runBlocking {
        val id = repo.addPlayer("Bob")
        repo.setPlayerColor(id, "teal")
        assertEquals("teal", repo.observePlayers().first().first { it.id == id }.color)
    }

    @Test
    fun observePlayers_isAlphabeticalCaseInsensitive() = runBlocking {
        repo.addPlayer("Charlie")
        repo.addPlayer("alice")
        repo.addPlayer("Bob")
        assertEquals(
            listOf("alice", "Bob", "Charlie"),
            repo.observePlayers().first().map { it.name },
        )
    }
}
