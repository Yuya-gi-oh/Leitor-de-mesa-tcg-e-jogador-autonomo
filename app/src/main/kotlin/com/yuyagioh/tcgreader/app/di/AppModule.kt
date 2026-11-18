package com.yuyagioh.tcgreader.app.di

import android.content.Context
import androidx.room.Room
import com.yuyagioh.tcgreader.core.model.GameId
import com.yuyagioh.tcgreader.core.rules.GameRules
import com.yuyagioh.tcgreader.data.db.CardDao
import com.yuyagioh.tcgreader.data.db.TcgDatabase
import com.yuyagioh.tcgreader.data.remote.ScryfallService
import com.yuyagioh.tcgreader.data.remote.YgoProDeckService
import com.yuyagioh.tcgreader.data.repository.CardRepository
import com.yuyagioh.tcgreader.rules.mtg.MtgRules
import com.yuyagioh.tcgreader.rules.ygo.YgoRules
import com.yuyagioh.tcgreader.vision.scan.CardScanner
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun tcgDatabase(@ApplicationContext context: Context): TcgDatabase =
        Room.databaseBuilder(context, TcgDatabase::class.java, TcgDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun cardDao(db: TcgDatabase): CardDao = db.cardDao()

    @Provides
    @Singleton
    fun retrofit(): Retrofit = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun scryfallService(retrofit: Retrofit): ScryfallService =
        retrofit.newBuilder()
            .baseUrl("https://api.scryfall.com/")
            .build()
            .create(ScryfallService::class.java)

    @Provides
    @Singleton
    fun ygoProDeckService(retrofit: Retrofit): YgoProDeckService =
        retrofit.newBuilder()
            .baseUrl("https://db.ygoprodeck.com/")
            .build()
            .create(YgoProDeckService::class.java)

    @Provides
    @Singleton
    fun cardRepository(
        scryfall: ScryfallService,
        ygoProDeck: YgoProDeckService,
        dao: CardDao,
    ): CardRepository = CardRepository(scryfall, ygoProDeck, dao)

    @Provides
    @Singleton
    fun cardScanner(repository: CardRepository): CardScanner = CardScanner(repository)

    @Provides
    @Singleton
    fun rulesRegistry(): RulesRegistry = RulesRegistry(
        mapOf(
            GameId.MAGIC to MtgRules(),
            GameId.YUGIOH to YgoRules(),
        ),
    )
}
