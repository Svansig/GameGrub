package app.gamegrub.di

import android.content.Context
import androidx.room.Room
import app.gamegrub.db.DATABASE_NAME
import app.gamegrub.db.GameGrubDatabase
import app.gamegrub.db.dao.AppInfoDao
import app.gamegrub.db.dao.CachedLicenseDao
import app.gamegrub.db.dao.DownloadingAppInfoDao
import app.gamegrub.db.dao.EncryptedAppTicketDao
import app.gamegrub.db.dao.GameDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GameGrubDatabase {
        // The db will be considered unstable during development.
        // Once stable we should add a (room) db migration
        return Room.databaseBuilder(context, GameGrubDatabase::class.java, DATABASE_NAME)
            .fallbackToDestructiveMigration(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideSteamLicenseDao(db: GameGrubDatabase) = db.steamLicenseDao()

    @Provides
    @Singleton
    fun provideSteamAppDao(db: GameGrubDatabase) = db.steamAppDao()

    @Provides
    @Singleton
    fun provideAppChangeNumbersDao(db: GameGrubDatabase) = db.appChangeNumbersDao()

    @Provides
    @Singleton
    fun provideAppFileChangeListsDao(db: GameGrubDatabase) = db.appFileChangeListsDao()

    @Provides
    @Singleton
    fun provideAppInfoDao(db: GameGrubDatabase): AppInfoDao = db.appInfoDao()

    @Provides
    @Singleton
    fun provideCachedLicenseDao(db: GameGrubDatabase): CachedLicenseDao = db.cachedLicenseDao()

    @Provides
    @Singleton
    fun provideEncryptedAppTicketDao(db: GameGrubDatabase): EncryptedAppTicketDao = db.encryptedAppTicketDao()

    @Provides
    @Singleton
    fun provideGOGGameDao(db: GameGrubDatabase) = db.gogGameDao()

    @Provides
    @Singleton
    fun provideEpicGameDao(db: GameGrubDatabase) = db.epicGameDao()

    @Provides
    @Singleton
    fun provideAmazonGameDao(db: GameGrubDatabase) = db.amazonGameDao()

    @Provides
    @Singleton
    fun provideDownloadingAppInfoDao(db: GameGrubDatabase): DownloadingAppInfoDao = db.downloadingAppInfoDao()

    @Provides
    @Singleton
    fun provideGameDao(db: GameGrubDatabase): GameDao = db.gameDao()
}
