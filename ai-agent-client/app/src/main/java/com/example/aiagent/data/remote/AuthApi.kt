package com.example.aiagent.data.remote

import com.example.aiagent.core.network.ApiResult
import com.example.aiagent.data.model.LoginReq
import com.example.aiagent.data.model.LoginResp
import com.example.aiagent.data.model.LogoutReq
import com.example.aiagent.data.model.RegisterReq
import com.example.aiagent.data.model.UserInfo
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {

    @POST("auth/login")
    suspend fun login(@Body req: LoginReq): ApiResult<LoginResp>

    @POST("auth/register")
    suspend fun register(@Body req: RegisterReq): ApiResult<LoginResp>

    @POST("auth/logout")
    suspend fun logout(@Body req: LogoutReq): ApiResult<Unit>

    @GET("users/me")
    suspend fun me(): ApiResult<UserInfo>
}