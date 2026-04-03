package app.gamegrub.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.gamegrub.data.AmazonGame
import app.gamegrub.data.AppInfo
import app.gamegrub.data.CachedLicense
import app.gamegrub.data.ChangeNumbers
import app.gamegrub.data.DownloadingAppInfo
import app.gamegrub.data.EncryptedAppTicket
import app.gamegrub.data.EpicGame
import app.gamegrub.data.FileChangeLists
import app.gamegrub.data.GOGGame
import app.gamegrub.data.SteamApp
import app.gamegrub.data.SteamLicense
import app.gamegrub.data.UnifiedGame
import app.gamegrub.db.converters.AppConverter
import app.gamegrub.db.converters.ByteArrayConverter
import app.gamegrub.db.converters.FriendConverter
import app.gamegrub.db.converters.GOGConverter
import app.gamegrub.db.converters.GameSourceConverter
import app.gamegrub.db.converters.LicenseConverter
import app.gamegrub.db.converters.PathTypeConverter
import app.gamegrub.db.converters.UserFileInfoListConverter
import app.gamegrub.db.dao.AmazonGameDao
import app.gamegrub.db.dao.AppInfoDao
import app.gamegrub.db.dao.CachedLicenseDao
import app.gamegrub.db.dao.ChangeNumbersDao
import app.gamegrub.db.dao.DownloadingAppInfoDao
import app.gamegrub.db.dao.EncryptedAppTicketDao
import app.gamegrub.db.dao.EpicGameDao
import app.gamegrub.db.dao.FileChangeListsDao
import app.gamegrub.db.dao.GOGGameDao
import app.gamegrub.db.dao.GameDao
import app.gamegrub.db.dao.SteamAppDao
import app.gamegrub.db.dao.SteamLicenseDao

const val DATABASE_NAME = "gamegrub.db"

@Database(
    entities = [
        AppInfo::class,
        CachedLicense::class,
        ChangeNumbers::class,
        EncryptedAppTicket::class,
        FileChangeLists::class,
        SteamApp::class,
        SteamLicense::class,
        GOGGame::class,
        EpicGame::class,
        AmazonGame::class,
        DownloadingAppInfo::class,
        UnifiedGame::class,
    ],
    version = 14,
    // For db migration, visit https://developer.android.com/training/data-storage/room/migrating-db-versions for more information
    exportSchema = true, // It is better to handle db changes carefully, as GN is getting much more users.
)
@TypeConverters(
    AppConverter::class,
    ByteArrayConverter::class,
    FriendConverter::class,
    LicenseConverter::class,
    PathTypeConverter::class,
    UserFileInfoListConverter::class,
    GOGConverter::class,
    GameSourceConverter::class,
)
abstract class GameGrubDatabase : RoomDatabase() {

    abstract fun steamLicenseDao(): SteamLicenseDao

    abstract fun steamAppDao(): SteamAppDao

    abstract fun appChangeNumbersDao(): ChangeNumbersDao

    abstract fun appFileChangeListsDao(): FileChangeListsDao

    abstract fun appInfoDao(): AppInfoDao

    abstract fun cachedLicenseDao(): CachedLicenseDao

    abstract fun encryptedAppTicketDao(): EncryptedAppTicketDao

    abstract fun gogGameDao(): GOGGameDao

    abstract fun epicGameDao(): EpicGameDao

    abstract fun amazonGameDao(): AmazonGameDao

    abstract fun downloadingAppInfoDao(): DownloadingAppInfoDao

    abstract fun gameDao(): GameDao
}
