/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:OptIn(PrivilegedIntent::class)

/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package org.quiltmc.community

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.checks.guildFor
import com.kotlindiscord.kord.extensions.checks.types.Check
import com.kotlindiscord.kord.extensions.modules.extra.mappings.extMappings
import com.kotlindiscord.kord.extensions.modules.extra.phishing.DetectionAction
import com.kotlindiscord.kord.extensions.modules.extra.phishing.extPhishing
import com.kotlindiscord.kord.extensions.modules.extra.pluralkit.extPluralKit
import com.kotlindiscord.kord.extensions.utils.envOrNull
import com.kotlindiscord.kord.extensions.utils.getKoin
import dev.kord.core.entity.Guild
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.message.create.embed
import org.quiltmc.community.cozy.modules.logs.extLogParser
import org.quiltmc.community.cozy.modules.logs.processors.PiracyProcessor
import org.quiltmc.community.cozy.modules.logs.processors.ProblematicLauncherProcessor
import org.quiltmc.community.cozy.modules.tags.TagFormatter
import org.quiltmc.community.cozy.modules.tags.config.TagsConfig
import org.quiltmc.community.cozy.modules.tags.tags
import org.quiltmc.community.cozy.modules.welcome.welcomeChannel
import org.quiltmc.community.database.collections.InvalidMentionsCollection
import org.quiltmc.community.database.collections.TagsCollection
import org.quiltmc.community.database.collections.WelcomeChannelCollection
import org.quiltmc.community.database.entities.InvalidMention
import org.quiltmc.community.database.getSettings
import org.quiltmc.community.logs.NonQuiltLoaderProcessor
import org.quiltmc.community.logs.RuleBreakingModProcessor
import org.quiltmc.community.modes.quilt.extensions.*
import org.quiltmc.community.modes.quilt.extensions.filtering.FilterExtension
import org.quiltmc.community.modes.quilt.extensions.github.GithubExtension
import org.quiltmc.community.modes.quilt.extensions.minecraft.MinecraftExtension
import org.quiltmc.community.modes.quilt.extensions.moderation.ModerationExtension
import org.quiltmc.community.modes.quilt.extensions.rotatinglog.ExtraLogExtension
import org.quiltmc.community.modes.quilt.extensions.rotatinglog.MessageLogExtension
import org.quiltmc.community.modes.quilt.extensions.settings.SettingsExtension
import org.quiltmc.community.modes.quilt.extensions.suggestions.SuggestionsExtension
import kotlin.time.Duration.Companion.minutes

val MODE = envOrNull("MODE")?.lowercase() ?: "ladysnake"

suspend fun setupLadysnake() = ExtensibleBot(DISCORD_TOKEN) {
	common()
	database(true)
	settings()

	chatCommands {
		defaultPrefix = "%"
		enabled = true
	}

	intents {
		+Intents.all
	}

	members {
		all()

		fillPresences = true
	}

	extensions {
        add(::FilterExtension)

        add(::MessageLogExtension)
        add(::MinecraftExtension)
        add(::SettingsExtension)
        add(::SuggestionsExtension)
        add(::SyncExtension)
        add(::UtilityExtension)
        add(::ModerationExtension)
        add(::UserFunExtension)
        add(::PersistentCacheExtension)
        add(::MessageEditExtension)
        add(::ExtraLogExtension)
        add(::ForcedPermissionExtension)
		add(::ConsoleLogExtension)

        if (GITHUB_TOKEN != null) {
            add(::GithubExtension)
        }

        extMappings { }

		extPluralKit()

		extLogParser {
			// Bundled non-default processors
			processor(PiracyProcessor())
			processor(ProblematicLauncherProcessor())

			// Quilt-specific processors
			processor(NonQuiltLoaderProcessor())
			processor(RuleBreakingModProcessor())

//			@Suppress("TooGenericExceptionCaught")
//			suspend fun predicate(handler: BaseLogHandler, event: Event): Boolean = with(handler) {
//				val predicateLogger = KotlinLogging.logger(
//					"org.quiltmc.community.AppKt.setupQuilt.extLogParser.predicate"
//				)
//
//				val kord: Kord = getKoin().get()
//				val channelId = channelSnowflakeFor(event)
//				val guild = guildFor(event)
//
//				try {
//					val skippableChannelIds = SKIPPABLE_HANDLER_CATEGORIES.mapNotNull {
//						kord.getChannelOf<Category>(it)
//							?.channels
//							?.map { ch -> ch.id }
//							?.toList()
//					}.flatten()
//
//					val isSkippable = identifier in SKIPPABLE_HANDLER_IDS
//
//					if (guild?.id == TOOLCHAIN_GUILD && isSkippable) {
//						predicateLogger.info {
//							"Skipping handler '$identifier' in <#$channelId>: Skippable handler, and on Toolchain"
//						}
//
//						return false
//					}
//
//					if (channelId in skippableChannelIds && isSkippable) {
//						predicateLogger.info {
//							"Skipping handler '$identifier' in <#$channelId>: Skippable handler, and in a dev category"
//						}
//
//						return false
//					}
//
//					predicateLogger.debug { "Passing handler '$identifier' in <#$channelId>" }
//
//					return true
//				} catch (e: Exception) {
//					predicateLogger.warn(e) { "Skipping processor '$identifier' in <#$channelId> due to an error." }
//
//					return true
//				}
//			}
//
//			globalPredicate(::predicate)
		}

		help {
			enableBundledExtension = true
		}

		welcomeChannel(getKoin().get<WelcomeChannelCollection>()) {
			staffCommandCheck {
				hasBaseModeratorRole()
			}

			getLogChannel { _, guild ->
				guild.getSettings()?.getConfiguredLogChannel()
			}

			refreshDuration = 5.minutes
		}

		tags(
			object : TagsConfig {
				override suspend fun getTagFormatter(): TagFormatter = { tag ->
					embed {
						title = tag.title
						description = tag.description
						color = tag.color

						footer {
							text = "${tag.category}/${tag.key}"
						}

						image = tag.image
					}
				}

				override suspend fun getUserCommandChecks(): List<Check<*>> {
					return listOf {
						val event = event as? ChatInputCommandInteractionCreateEvent ?: return@listOf
						val cmd = event.interaction.command

						if (cmd.data.name.value == "tag") {
							cmd.members["user"]?.let { user ->
								val mention = getKoin().get<InvalidMentionsCollection>().get(user.id) ?: return@listOf
								if (mention.type != InvalidMention.Type.USER) {
									val guild = guildFor(event) ?: return@listOf
									if (mention._id in event.interaction.user.asMember(guild.id).roleIds) {
										fail("You can't mention that user.")
									}
									return@listOf
								}
								if (event.interaction.user.id in mention.exceptions) return@listOf
								if (event !is GuildChatInputCommandInteractionCreateEvent) {
									fail("You can't mention that user.")
								} else {
									val guild = event.interaction.guild
									val member = guild.getMemberOrNull(user.id)
									if (member == null) {
										fail("You can't mention that user.")
									} else {
										if (member.roleIds.none { it in mention.exceptions }) {
											hasBaseModeratorRole()
											if (!passed) {
												fail("You can't mention that user.")
											}
										}
									}
								}
							}
						}
					}
				}

				override suspend fun getStaffCommandChecks(): List<Check<*>> {
					return listOf {
						hasBaseModeratorRole()
					}
				}

				override suspend fun getLoggingChannelOrNull(guild: Guild) = guild.getCozyLogChannel()
			},
			getKoin().get<TagsCollection>()
		)

		extPhishing {
			appName = "Ladysnake's Modification of Quilt's Cozy Bot"
			detectionAction = DetectionAction.Kick
			logChannelName = "rtuuy-message-log"
			requiredCommandPermission = null

			check { inLadysnakeGuild() }
			check { notHasBaseModeratorRole() }
		}

//		userCleanup {
//			maxPendingDuration = 3.days
//			taskDelay = 1.days
//			loggingChannelName = "rtuuy-message-log"
//
//			runAutomatically = true
//
//			guildPredicate {
//				val servers = getKoin().get<ServerSettingsCollection>()
//				val serverEntry = servers.get(it.id)
//
//				serverEntry?.ladysnakeServerType != null
//			}
//
//			commandCheck { hasPermission(Permission.Administrator) }
//		}

		sentry {
            distribution = "ladysnake"
            dsn = envOrNull("SENTRY_DSN")
		}
	}
}

@Suppress("UseIfInsteadOfWhen") // currently only one mode but that could change
suspend fun main() {
	val bot = when (MODE) {
		"ladysnake" -> setupLadysnake()

		else -> error("Invalid mode: $MODE")
	}

	bot.start()
}
