package top.nihoru.app.data.model

data class AniListResponse(
    val data: AniListData?
)

data class AniListData(
    val Page: PageData?
)

data class PageData(
    val media: List<MediaItem>?
)

data class MediaItem(
    val id: Int,
    val idMal: Int?,
    val title: MediaTitle,
    val type: String?,
    val format: String?,
    val status: String?,
    val episodes: Int?,
    val chapters: Int?,
    val volumes: Int?,
    val popularity: Int?,
    val startDate: FuzzyDate?,
    val coverImage: CoverImage?,
    val siteUrl: String?
)

data class MediaTitle(
    val romaji: String?,
    val english: String?
)

data class FuzzyDate(
    val year: Int?
)

data class CoverImage(
    val large: String?
)

// For batch queries
data class BatchAniListResponse(
    val data: Map<String, PageData>?
)

// For relations
data class RelationsResponse(
    val data: RelationsData?
)

data class RelationsData(
    val Media: RelationsMedia?
)

data class RelationsMedia(
    val relations: RelationConnection?
)

data class RelationConnection(
    val edges: List<RelationEdge>?
)

data class RelationEdge(
    val relationType: String?,
    val node: MediaItem?
)
