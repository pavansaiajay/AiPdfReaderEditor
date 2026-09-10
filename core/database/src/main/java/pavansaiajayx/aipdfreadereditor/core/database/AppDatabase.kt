package pavansaiajayx.aipdfreadereditor.core.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
import pavansaiajayx.aipdfreadereditor.core.database.dao.DocumentDao
import pavansaiajayx.aipdfreadereditor.core.database.model.DocumentEntity

@Database(
    entities = [DocumentEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao

    companion object {
        const val DATABASE_NAME = "aipdf_reader_editor.db"
    }
}
