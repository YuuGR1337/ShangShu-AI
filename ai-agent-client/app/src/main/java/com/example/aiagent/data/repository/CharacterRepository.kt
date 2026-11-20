package com.example.aiagent.data.repository

import com.example.aiagent.core.network.NetworkModule
import com.example.aiagent.core.network.safeApiCall
import com.example.aiagent.data.model.CharacterCardCreateReq
import com.example.aiagent.data.model.CharacterCardResp
import com.example.aiagent.data.model.CharacterCardUpdateReq
import com.example.aiagent.data.remote.CharacterApi

class CharacterRepository {

    private val api = NetworkModule.api<CharacterApi>()

    suspend fun createCharacterCard(req: CharacterCardCreateReq): CharacterCardResp =
        safeApiCall { api.createCharacterCard(req) }

    suspend fun listCharacterCards(): List<CharacterCardResp> =
        safeApiCall { api.listCharacterCards() }

    suspend fun listPublicCharacterCards(keyword: String? = null): List<CharacterCardResp> =
        safeApiCall { api.listPublicCharacterCards(keyword) }

    suspend fun getCharacterCard(id: Long): CharacterCardResp =
        safeApiCall { api.getCharacterCard(id) }

    suspend fun updateCharacterCard(id: Long, req: CharacterCardUpdateReq): CharacterCardResp =
        safeApiCall { api.updateCharacterCard(id, req) }

    suspend fun deleteCharacterCard(id: Long) {
        safeApiCall { api.deleteCharacterCard(id) }
    }

    suspend fun setDefault(id: Long): CharacterCardResp =
        safeApiCall { api.setDefault(id) }

    suspend fun getDefault(): CharacterCardResp =
        safeApiCall { api.getDefault() }
}