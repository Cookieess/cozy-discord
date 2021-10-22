package org.quiltmc.community.modes.quilt.extensions.filtering

import com.kotlindiscord.kord.extensions.commands.application.slash.converters.ChoiceEnum
import kotlinx.serialization.Serializable

@Serializable
enum class MatchType(override val readableName: String) : ChoiceEnum {
    CONTAINS("Message contains this text"),
    EXACT("Message is exactly this text"),
    REGEX("Message exactly matches this regular expression"),
    REGEX_CONTAINS("Message contains this regular expression")
}
