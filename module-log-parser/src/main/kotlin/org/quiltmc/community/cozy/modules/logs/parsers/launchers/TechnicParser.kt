package org.quiltmc.community.cozy.modules.logs.parsers.launchers

import com.kotlindiscord.kord.extensions.utils.capitalizeWords
import org.quiltmc.community.cozy.modules.logs.data.Launcher
import org.quiltmc.community.cozy.modules.logs.data.Log
import org.quiltmc.community.cozy.modules.logs.data.Order
import org.quiltmc.community.cozy.modules.logs.types.LogParser

private val OS_REGEX = "] OS: ([^\n]+)\n".toRegex(RegexOption.IGNORE_CASE)
private val JAVA_REGEX = "] Java: ([^\n]+)\n".toRegex(RegexOption.IGNORE_CASE)
private val JAVA_ARGS_REGEX = "] Running .+? (-.+?) -Djava.library.path".toRegex(RegexOption.IGNORE_CASE)

public class TechnicParser : LogParser() {
	override val identifier: String = "launcher-technic"
	override val order: Order = Order.Early

	override suspend fun predicate(log: Log): Boolean =
		log.launcher?.name in arrayOf(Launcher.Technic)

	override suspend fun process(log: Log) {
		val javaMatch = JAVA_REGEX.find(log.content)

		if (javaMatch != null) {
			val java = "Java ${javaMatch.groupValues[1]}"

			log.environment.jvmVersion = java
		}

		val javaArgsMatch = JAVA_ARGS_REGEX.find(log.content)

		if (javaArgsMatch != null) {
			log.environment.jvmArgs = javaArgsMatch.groupValues[1]
		}

		val osMatch = OS_REGEX.find(log.content)

		if (osMatch != null) {
			log.environment.os = osMatch.groupValues[1].capitalizeWords()
		}
	}
}
