package pavansaiajayx.aipdfreadereditor.core.database.di

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test
import pavansaiajayx.aipdfreadereditor.core.database.AppDatabase
import pavansaiajayx.aipdfreadereditor.core.database.FakeDocumentDao
import pavansaiajayx.aipdfreadereditor.core.database.dao.DocumentDao

class DatabaseModuleTest {

    private class TestAppDatabase(private val fakeDao: DocumentDao) : AppDatabase() {
        override fun documentDao(): DocumentDao = fakeDao
        override suspend fun clearAllTables() {}
    }


    @Test
    fun provideDocumentDaoExtractsDaoFromDatabase() {
        val fakeDao = FakeDocumentDao()
        val db = TestAppDatabase(fakeDao)

        val providedDao = DatabaseModule.provideDocumentDao(db)

        assertNotNull(providedDao)
        assertSame(fakeDao, providedDao)
    }
}
