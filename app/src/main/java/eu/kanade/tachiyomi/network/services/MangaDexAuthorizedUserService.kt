package eu.kanade.tachiyomi.network.services

import com.skydoves.sandwich.ApiResponse
import eu.kanade.tachiyomi.source.online.models.dto.ForumThreadDto
import eu.kanade.tachiyomi.source.online.models.dto.ForumThreadResponseDto
import eu.kanade.tachiyomi.source.online.models.dto.UserListDto
import org.nekomanga.constants.MdConstants
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

/** MangaDex endpoints that need a signed-in session. */
interface MangaDexAuthorizedUserService {

    @POST(MdConstants.Api.createThread)
    suspend fun createForumThread(
        @Body forumThreadDto: ForumThreadDto
    ): ApiResponse<ForumThreadResponseDto>

    @Headers("Cache-Control: no-cache")
    @GET(MdConstants.Api.user)
    suspend fun uploader(@Query("username") username: String): ApiResponse<UserListDto>
}
