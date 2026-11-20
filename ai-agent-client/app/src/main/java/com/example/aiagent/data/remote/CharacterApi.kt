package com.example.aiagent.data.remote

import com.example.aiagent.data.model.CharacterCardCreateReq
import com.example.aiagent.data.model.CharacterCardResp
import com.example.aiagent.data.model.CharacterCardUpdateReq
import com.example.aiagent.core.network.ApiResult
import retrofit2.http.*

interface CharacterApi {

    @POST("api/v1/character-cards")
    suspend fun createCharacterCard(@Body req: CharacterCardCreateReq): ApiResult<CharacterCardResp>

    @GET("api/v1/character-cards")
    suspend fun listCharacterCards(): ApiResult<List<CharacterCardResp>>

    @GET("api/v1/character-cards/public")
    suspend fun listPublicCharacterCards(@Query("keyword") keyword: String? = null): ApiResult<List<CharacterCardResp>>

    @GET("api/v1/character-cards/{id}")
    suspend fun getCharacterCard(@Path("id") id: Long): ApiResult<CharacterCardResp>

    @PUT("api/v1/character-cards/{id}")
    suspend fun updateCharacterCard(@Path("id") id: Long, @Body req: CharacterCardUpdateReq): ApiResult<CharacterCardResp>

    @DELETE("api/v1/character-cards/{id}")
    suspend fun deleteCharacterCard(@Path("id") id: Long): ApiResult<Unit>

    @PUT("api/v1/character-cards/{id}/default")
    suspend fun setDefault(@Path("id") id: Long): ApiResult<CharacterCardResp>

    @GET("api/v1/character-cards/default")
    suspend fun getDefault(): ApiResult<CharacterCardResp>
}