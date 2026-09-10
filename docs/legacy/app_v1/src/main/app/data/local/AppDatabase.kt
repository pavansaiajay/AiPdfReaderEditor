package pavansaiajayx.aipdfreadereditor.app.data.local

import androidx.room3.Database
import androidx.room3.RoomDatabase

@Database(entities = [DocumentEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
}
