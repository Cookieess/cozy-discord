package org.quiltmc.community.database.collections

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.MessageBehavior
import org.bson.conversions.Bson
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.litote.kmongo.eq
import org.quiltmc.community.database.Collection
import org.quiltmc.community.database.Database
import org.quiltmc.community.database.entities.Suggestion

class SuggestionsCollection : KoinComponent {
    private val database: Database by inject()
    private val col = database.mongo.getCollection<Suggestion>(name)

    suspend fun get(id: Snowflake) =
        col.findOne(Suggestion::_id eq id)

    suspend fun getByMessage(id: Snowflake) =
        col.findOne(Suggestion::message eq id)

    suspend fun getByMessage(message: MessageBehavior) =
        getByMessage(message.id)

    suspend fun find(filter: Bson) =
        col.find(filter)

    suspend fun set(suggestion: Suggestion) =
        col.save(suggestion)

    companion object : Collection("suggestions")
}
