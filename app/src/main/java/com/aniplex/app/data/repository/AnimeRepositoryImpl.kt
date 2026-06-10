package com.aniplex.app.data.repository

import com.aniplex.app.data.local.dao.CacheDao
import com.aniplex.app.data.local.entity.CacheEntity
import com.aniplex.app.data.mapper.toDomain
import com.aniplex.app.data.remote.api.HiAnimeApiService
import com.aniplex.app.data.remote.dto.AnimeDetailResponse
import com.aniplex.app.data.remote.dto.EpisodesResponse
import com.aniplex.app.data.remote.dto.HomeResponse
import com.aniplex.app.domain.model.*
import com.aniplex.app.domain.repository.AnimeRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException
import javax.inject.Inject

class AnimeRepositoryImpl @Inject constructor(
    private val apiService: HiAnimeApiService,
    private val cacheDao: CacheDao,
    private val gson: Gson
) : AnimeRepository {

    private val HOME_CACHE_LIFETIME = 10 * 60 * 1000L // 10 minutes
    private val DETAIL_CACHE_LIFETIME = 30 * 60 * 1000L // 30 minutes
    private val EPISODES_CACHE_LIFETIME = 60 * 60 * 1000L // 1 hour

    override fun getHomePage(forceRefresh: Boolean): Flow<Result<HomeData>> = flow {
        val cacheKey = "home_page"
        emit(Result.Loading)

        val cachedEntity = cacheDao.getCache(cacheKey)
        val currentTime = System.currentTimeMillis()

        if (cachedEntity != null && !forceRefresh && (currentTime - cachedEntity.timestamp < HOME_CACHE_LIFETIME)) {
            try {
                val cachedResponse = gson.fromJson(cachedEntity.jsonContent, HomeResponse::class.java)
                emit(Result.Success(cachedResponse.data.toDomain()))
                return@flow
            } catch (e: Exception) {
                // JSON parsing failed, fallback to network
            }
        }

        try {
            val response = apiService.getHomePage()
            if (response.success) {
                cacheDao.insertCache(
                    CacheEntity(
                        cacheKey = cacheKey,
                        jsonContent = gson.toJson(response),
                        timestamp = currentTime
                    )
                )
                emit(Result.Success(response.data.toDomain()))
            } else {
                emit(Result.Error("API returned success = false"))
            }
        } catch (e: Exception) {
            if (cachedEntity != null) {
                try {
                    val cachedResponse = gson.fromJson(cachedEntity.jsonContent, HomeResponse::class.java)
                    emit(Result.Success(cachedResponse.data.toDomain()))
                } catch (jsonEx: Exception) {
                    emit(Result.Error(e.localizedMessage ?: "Unknown network error"))
                }
            } else {
                emit(Result.Error(e.localizedMessage ?: "Unknown network error"))
            }
        }
    }

    override fun getAnimeDetail(id: String, forceRefresh: Boolean): Flow<Result<AnimeDetail>> = flow {
        val cacheKey = "detail_$id"
        emit(Result.Loading)

        val cachedEntity = cacheDao.getCache(cacheKey)
        val currentTime = System.currentTimeMillis()

        if (cachedEntity != null && !forceRefresh && (currentTime - cachedEntity.timestamp < DETAIL_CACHE_LIFETIME)) {
            try {
                val cachedResponse = gson.fromJson(cachedEntity.jsonContent, AnimeDetailResponse::class.java)
                emit(Result.Success(cachedResponse.data.toDomain()))
                return@flow
            } catch (e: Exception) {
                // Fallback to network
            }
        }

        try {
            val response = apiService.getAnimeDetail(id)
            if (response.success) {
                cacheDao.insertCache(
                    CacheEntity(
                        cacheKey = cacheKey,
                        jsonContent = gson.toJson(response),
                        timestamp = currentTime
                    )
                )
                emit(Result.Success(response.data.toDomain()))
            } else {
                emit(Result.Error("API returned success = false"))
            }
        } catch (e: Exception) {
            if (cachedEntity != null) {
                try {
                    val cachedResponse = gson.fromJson(cachedEntity.jsonContent, AnimeDetailResponse::class.java)
                    emit(Result.Success(cachedResponse.data.toDomain()))
                } catch (jsonEx: Exception) {
                    emit(Result.Error(e.localizedMessage ?: "Network error"))
                }
            } else {
                emit(Result.Error(e.localizedMessage ?: "Network error"))
            }
        }
    }

    override fun getEpisodes(id: String, forceRefresh: Boolean): Flow<Result<List<Episode>>> = flow {
        val cacheKey = "episodes_$id"
        emit(Result.Loading)

        val cachedEntity = cacheDao.getCache(cacheKey)
        val currentTime = System.currentTimeMillis()

        if (cachedEntity != null && !forceRefresh && (currentTime - cachedEntity.timestamp < EPISODES_CACHE_LIFETIME)) {
            try {
                val cachedResponse = gson.fromJson(cachedEntity.jsonContent, EpisodesResponse::class.java)
                emit(Result.Success(cachedResponse.data.episodes.map { it.toDomain() }))
                return@flow
            } catch (e: Exception) {
                // Fallback to network
            }
        }

        try {
            val response = apiService.getEpisodes(id)
            if (response.success) {
                cacheDao.insertCache(
                    CacheEntity(
                        cacheKey = cacheKey,
                        jsonContent = gson.toJson(response),
                        timestamp = currentTime
                    )
                )
                emit(Result.Success(response.data.episodes.map { it.toDomain() }))
            } else {
                emit(Result.Error("API returned success = false"))
            }
        } catch (e: Exception) {
            if (cachedEntity != null) {
                try {
                    val cachedResponse = gson.fromJson(cachedEntity.jsonContent, EpisodesResponse::class.java)
                    emit(Result.Success(cachedResponse.data.episodes.map { it.toDomain() }))
                } catch (jsonEx: Exception) {
                    emit(Result.Error(e.localizedMessage ?: "Network error"))
                }
            } else {
                emit(Result.Error(e.localizedMessage ?: "Network error"))
            }
        }
    }

    override fun search(query: String, page: Int): Flow<Result<List<Anime>>> = flow {
        emit(Result.Loading)
        try {
            val response = apiService.search(query, page)
            if (response.success) {
                emit(Result.Success(response.data.animes?.map { it.toDomain() } ?: emptyList()))
            } else {
                emit(Result.Error("Search failed"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.localizedMessage ?: "Search request failed"))
        }
    }

    override fun getSuggestions(query: String): Flow<Result<List<Anime>>> = flow {
        emit(Result.Loading)
        try {
            val response = apiService.getSuggestions(query)
            if (response.success) {
                val list = response.data.suggestions?.map {
                    Anime(
                        id = it.id,
                        title = it.name,
                        poster = it.poster,
                        type = it.moreInfo?.firstOrNull() ?: "",
                        duration = it.moreInfo?.getOrNull(1) ?: "",
                        subEpisodes = 0,
                        dubEpisodes = 0,
                        rate = ""
                    )
                } ?: emptyList()
                emit(Result.Success(list))
            } else {
                emit(Result.Error("Suggestions failed"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.localizedMessage ?: "Suggestions failed"))
        }
    }

    override fun getAnimeByCategory(category: String, page: Int): Flow<Result<List<Anime>>> = flow {
        emit(Result.Loading)
        try {
            val response = apiService.getAnimeByCategory(category, page)
            if (response.success) {
                emit(Result.Success(response.data.animes?.map { it.toDomain() } ?: emptyList()))
            } else {
                emit(Result.Error("Category loading failed"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.localizedMessage ?: "Category request failed"))
        }
    }

    override fun getAnimeByGenre(genre: String, page: Int): Flow<Result<List<Anime>>> = flow {
        emit(Result.Loading)
        try {
            val response = apiService.getAnimeByGenre(genre, page)
            if (response.success) {
                emit(Result.Success(response.data.animes?.map { it.toDomain() } ?: emptyList()))
            } else {
                emit(Result.Error("Genre loading failed"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.localizedMessage ?: "Genre request failed"))
        }
    }

    override fun getSchedules(date: String?): Flow<Result<List<ScheduleItem>>> = flow {
        emit(Result.Loading)
        try {
            val response = apiService.getSchedules(date)
            if (response.success) {
                emit(Result.Success(response.data.scheduledAnimes?.map { it.toDomain() } ?: emptyList()))
            } else {
                emit(Result.Error("Schedule load failed"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.localizedMessage ?: "Schedule request failed"))
        }
    }

    override fun getCharacters(id: String): Flow<Result<List<Character>>> = flow {
        emit(Result.Loading)
        try {
            val response = apiService.getCharacters(id)
            if (response.success) {
                emit(Result.Success(response.data.characters?.map { it.toDomain() } ?: emptyList()))
            } else {
                emit(Result.Error("Characters loading failed"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.localizedMessage ?: "Characters request failed"))
        }
    }

    override fun getEpisodeStream(episodeId: String, server: String, category: String): Flow<Result<EpisodeStream>> = flow {
        emit(Result.Loading)
        try {
            val response = apiService.getEpisodeSources(episodeId, server, category)
            if (response.success) {
                val data = response.data
                val source = data.sources?.firstOrNull()
                if (source != null) {
                    val stream = EpisodeStream(
                        videoUrl = source.url,
                        isHls = source.type.equals("hls", ignoreCase = true) || source.url.contains(".m3u8"),
                        subtitles = data.tracks?.filter { it.kind == "captions" || it.kind == "subtitles" }?.map {
                            SubtitleTrack(
                                url = it.file,
                                label = it.label ?: "English",
                                isDefault = it.label?.equals("english", ignoreCase = true) == true
                            )
                        } ?: emptyList(),
                        introStart = data.intro?.let { it.start * 1000L } ?: 0L,
                        introEnd = data.intro?.let { it.end * 1000L } ?: 0L,
                        outroStart = data.outro?.let { it.start * 1000L } ?: 0L,
                        outroEnd = data.outro?.let { it.end * 1000L } ?: 0L
                    )
                    emit(Result.Success(stream))
                } else {
                    emit(Result.Error("No video source link returned by API"))
                }
            } else {
                emit(Result.Error("API returned success = false"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.localizedMessage ?: "Failed to load episode stream sources"))
        }
    }

    override fun filterAnime(
        type: String?,
        status: String?,
        genres: String?,
        sort: String?,
        language: String?,
        page: Int
    ): Flow<Result<List<Anime>>> = flow {
        emit(Result.Loading)
        try {
            val response = apiService.filterAnime(
                type = type,
                status = status,
                genres = genres,
                sort = sort,
                language = language,
                page = page
            )
            if (response.success) {
                emit(Result.Success(response.data.animes?.map { it.toDomain() } ?: emptyList()))
            } else {
                emit(Result.Error("Filter request failed"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.localizedMessage ?: "Filter request failed"))
        }
    }

    override fun getSeasons(malId: String): Flow<Result<List<Season>>> = flow {
        emit(Result.Loading)
        try {
            val response = apiService.getSeasons(malId)
            if (response.success) {
                val seasons = response.data.seasons?.map { it.toDomain() } ?: emptyList()
                emit(Result.Success(seasons))
            } else {
                emit(Result.Success(emptyList()))
            }
        } catch (e: Exception) {
            emit(Result.Success(emptyList())) // Gracefully degrade - no seasons is fine
        }
    }

    override fun resolveMAL(malId: String): Flow<Result<String>> = flow {
        emit(Result.Loading)
        try {
            val response = apiService.resolveMAL(malId)
            if (response.success && response.data != null) {
                emit(Result.Success(response.data.anikotoId))
            } else {
                emit(Result.Error("Could not resolve MAL ID"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.localizedMessage ?: "Failed to resolve MAL ID"))
        }
    }
}
