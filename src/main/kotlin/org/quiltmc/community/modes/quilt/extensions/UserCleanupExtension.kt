/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:OptIn(ExperimentalTime::class)

package org.quiltmc.community.modes.quilt.extensions

import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
import com.kotlindiscord.kord.extensions.DISCORD_FUCHSIA
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.time.TimestampType
import com.kotlindiscord.kord.extensions.time.toDiscord
import com.kotlindiscord.kord.extensions.types.editingPaginator
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.scheduling.Scheduler
import com.kotlindiscord.kord.extensions.utils.scheduling.Task
import dev.kord.cache.api.getEntry
import dev.kord.cache.api.query
import dev.kord.common.entity.Permission
import dev.kord.common.entity.optional.optional
import dev.kord.core.cache.data.MemberData
import dev.kord.core.cache.data.UserData
import dev.kord.core.entity.Member
import dev.kord.core.event.guild.MembersChunkEvent
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import mu.KotlinLogging
import org.koin.core.component.inject
import org.quiltmc.community.GUILDS
import org.quiltmc.community.database.collections.ServerSettingsCollection
import org.quiltmc.community.hasPermissionInMainGuild
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime

private const val MAX_PENDING_DAYS = 3
private const val MEMBER_CHUNK_SIZE = 10

private val TASK_DELAY = 1.hours
private val MAX_PENDING_DURATION = MAX_PENDING_DAYS.days

class UserCleanupExtension : Extension() {
    override val name: String = "user-cleanup"

    private val logger = KotlinLogging.logger {}
    private val servers: ServerSettingsCollection by inject()

    private val scheduler = Scheduler()
    private lateinit var task: Task

    override suspend fun setup() {
        task = scheduler.schedule(TASK_DELAY, pollingSeconds = 60, callback = ::taskRun)

        event<MembersChunkEvent> {
            action {
                logger.info { "Member chunk event: ${event.guildId} -> ${event.members.count()}" }
            }
        }

        GUILDS.forEach { guildId ->
            ephemeralSlashCommand(::CleanupArgs) {
                name = "cleanup-users"
                description = "Clean up likely bot accounts"

                guild(guildId)

                check { hasPermissionInMainGuild(Permission.Administrator) }

                action {
                    if (::task.isInitialized) {
                        task.cancel()
                    }

                    val guilds = servers.getByLadysnakeServers()
                        .toList()
                        .sortedBy { it.ladysnakeServerType!!.readableName }

                    val removed = taskRun(arguments.dryRun).groupBy { it.guildId }

                    if (removed.isEmpty()) {
                        respond {
                            content = "It doesn't look like there's anyone that needs to be removed."
                        }

                        return@action
                    }

                    editingPaginator {
                        var defaultSet = false

                        for (guild in guilds) {
                            val members = removed[guild._id] ?: continue

                            if (members.isEmpty()) {
                                continue
                            }

                            if (!defaultSet) {
                                defaultSet = true

                                pages.defaultGroup = guild.ladysnakeServerType!!.readableName
                            }

                            members.chunked(MEMBER_CHUNK_SIZE).forEach { chunk ->
                                page(guild.ladysnakeServerType!!.readableName) {
                                    color = DISCORD_FUCHSIA
                                    title = "User Cleanup: ${guild.ladysnakeServerType!!.readableName}"

                                    if (arguments.dryRun) {
                                        color = DISCORD_BLURPLE
                                        title += " (dry-run)"
                                    }

                                    description = "**Mention** | **Tag** | **Join date**\n\n"

                                    chunk.forEach { member ->
                                        description += "${member.mention} | ${member.tag} |" +
                                                "${member.joinedAt.toDiscord(TimestampType.Default)}\n"
                                    }
                                }
                            }
                        }
                    }.send()
                }
            }
        }
    }

    suspend fun taskRun(dryRun: Boolean = false): MutableSet<Member> {
        val removed: MutableSet<Member> = mutableSetOf()
        val now = Clock.System.now()

        try {
            val guilds = servers
                .getByLadysnakeServers()
                .toList()
                .mapNotNull { kord.getGuild(it._id) }

            guilds.forEach { guild ->
                val members = kord.cache.getEntry<MemberData>()!!
                    .query { MemberData::pending eq true.optional() }
                    .asFlow()

                val membersInGuild = members.filter { it.guildId == guild.id }.toList()
                val count = membersInGuild.size

                val users = kord.cache.getEntry<UserData>()!!
                    .query { UserData::bot eq false.optional() }
                    .asFlow()
                    .filter { user -> membersInGuild.any { it.userId == user.id } }
                    .toList()
                    .associateBy { it.id }

                logger.info { "Members in cache (before, ${guild.name}): $count" }

                membersInGuild
                    .map { Member(it, users[it.userId]!!, kord) }
                    .filter { (it.joinedAt + MAX_PENDING_DURATION) <= now }
                    .toList()
                    .forEach {
                        if (!dryRun) {
                            it.kick("Failed to pass member screening within $MAX_PENDING_DAYS days.")
                        }

                        removed += it
                    }
            }

            return removed
        } finally {
            if (::task.isInitialized) {
                task.restart()
            }
        }
    }

    override suspend fun unload() {
        super.unload()

        if (::task.isInitialized) {
            task.cancel()
        }
    }

    inner class CleanupArgs : Arguments() {
        val dryRun by defaultingBoolean {
            name = "dry-run"
            description = "Whether to preview the member to kick instead of actually kicking them"

            defaultValue = true
        }
    }
}
