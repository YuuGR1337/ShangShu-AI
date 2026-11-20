package com.example.aiagent.data.repository

import com.example.aiagent.core.network.NetworkModule
import com.example.aiagent.core.network.ApiException
import com.example.aiagent.core.network.safeApiCall
import com.example.aiagent.core.storage.SessionManager
import com.example.aiagent.data.model.LoginReq
import com.example.aiagent.data.model.LoginResp
import com.example.aiagent.data.model.LogoutReq
import com.example.aiagent.data.model.RegisterReq
import com.example.aiagent.data.model.UserInfo
import com.example.aiagent.data.remote.AuthApi

class AuthRepository(
    private val api: AuthApi = NetworkModule.api(),
) {

    suspend fun login(account: String, password: String): UserInfo {
        val resp = safeApiCall { api.login(LoginReq(account, password)) }
        persistSession(resp)
        return fetchCurrentUser(resp)
    }

    suspend fun register(username: String, email: String, password: String): UserInfo {
        val resp = safeApiCall { api.register(RegisterReq(username, email, password)) }
        persistSession(resp)
        return fetchCurrentUser(resp)
    }

    suspend fun logout() {
        val access = SessionManager.accessToken
        val refresh = SessionManager.refreshToken
        if (!access.isNullOrBlank() && !refresh.isNullOrBlank()) {
            runCatching {
                api.logout(LogoutReq(access, refresh)).code
            }
        }
        SessionManager.clear()
    }

    /** 刷新令牌；成功返回 true。 */
    suspend fun refresh(): Boolean = NetworkModule.refreshBlocking()

    private fun persistSession(resp: LoginResp) {
        SessionManager.saveTokens(
            access = resp.accessToken,
            refresh = resp.refreshToken,
            csrf = resp.csrfToken,
        )
    }

    /** 登录后优先获取完整用户信息，失败则回退到登录响应的基础字段。 */
    private suspend fun fetchCurrentUser(resp: LoginResp): UserInfo {
        val user = runCatching { safeApiCall { api.me() } }.getOrNull()
            ?: UserInfo(
                userId = resp.userId,
                username = resp.username,
                email = resp.email,
            )
        SessionManager.saveUser(user)
        return user
    }
}