package org.quiltmc.community.cozy.modules.logs.processors

import org.quiltmc.community.cozy.modules.logs.data.Launcher
import org.quiltmc.community.cozy.modules.logs.data.Log
import org.quiltmc.community.cozy.modules.logs.data.Order
import org.quiltmc.community.cozy.modules.logs.types.LogProcessor

private val CHECK_REGEX = "Failed to verify authentication\n.+?401\n".toRegex()

public class PiracyProcessor : LogProcessor() {
	override val identifier: String = "piracy"
	override val order: Order = Order.Earlier

	override suspend fun process(log: Log) {
		if (log.launcher?.name == Launcher.TLauncher) {
			log.abort(
				"**You seem to be using TLauncher.**\n\n" +

						"TLauncher is widely-known throughout the Minecraft community as a piracy tool. As all " +
						"communities on Discord must obey its the Terms of Service, this bot is unable to " +
						"provide you with support - as long as you make use of this launcher. This is the " +
						"case regardless of what you've heard elsewhere, or what servers have yet to have " +
						"been banned from Discord.\n\n" +

						"Please bear in mind that neither this server's staff, nor the developers working on " +
						"this bot, are unable to do anything about the Discord Terms of Service."
			)

			return
		}

		CHECK_REGEX.find(log.content)
			?: return

		log.abort(
			"**You seem to be running Minecraft in offline mode, or with invalid credentials.**\n\n" +

					"As all communities on Discord must obey its the Terms of Service, this bot is unable to " +
					"provide you with support - at least until you switch to a legitimate Minecraft account. This " +
					"is the case regardless of what you've heard elsewhere, or what servers have yet to have " +
					"been banned from Discord.\n\n" +

					"Please bear in mind that neither this server's staff, nor the developers working on " +
					"this bot, are unable to do anything about the Discord Terms of Service."
		)
	}
}
