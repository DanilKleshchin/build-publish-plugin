package ru.kode.android.build.publish.plugin.task.slack.changelog.work

import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.task.slack.changelog.entity.SlackChangelogBody
import ru.kode.android.build.publish.plugin.task.slack.changelog.sender.SlackWebhookSender
import javax.inject.Inject

interface SendSlackChangelogParameters : WorkParameters {
    val baseOutputFileName: Property<String>
    val buildName: Property<String>
    val changelog: Property<String>
    val webhookUrl: Property<String>
    val userMentions: Property<String>
    val iconUrl: Property<String>
    val attachmentColor: Property<String>
}

abstract class SendSlackChangelogWork @Inject constructor() : WorkAction<SendSlackChangelogParameters> {

    private val logger = Logging.getLogger(this::class.java)
    private val webhookSender = SlackWebhookSender(logger)

    override fun execute() {
        val baseOutputFileName = parameters.baseOutputFileName.get()
        val buildName = parameters.buildName.get()
        val changelogMessages = parameters.changelog.get()
            .split("\n")
            .filter { it.isNotBlank() }
        val body = SlackChangelogBody(
            icon_url = parameters.iconUrl.get(),
            username = "buildBot",
            blocks = listOf(buildHeaderBlock("$baseOutputFileName $buildName")),
            attachments = listOf(
                SlackChangelogBody.Attachment(
                    color = parameters.attachmentColor.get(),
                    blocks = listOf(buildSectionBlock(parameters.userMentions.get()))
                        .plus(changelogMessages.map { buildSectionBlock(it) }),
                )
            )
        )
        webhookSender.send(parameters.webhookUrl.get(), body)
        logger.info("changelog sent to Slack")
    }

    private fun buildHeaderBlock(text: String): SlackChangelogBody.Block {
        return SlackChangelogBody.Block(
            type = BLOCK_TYPE_HEADER,
            text = SlackChangelogBody.Text(
                type = TEXT_TYPE_PLAIN_TEXT, // header can have plain_text type only
                text = text,
            ),
        )
    }

    private fun buildSectionBlock(text: String, textType: String = TEXT_TYPE_MARKDOWN): SlackChangelogBody.Block {
        return SlackChangelogBody.Block(
            type = BLOCK_TYPE_SECTION,
            text = SlackChangelogBody.Text(
                type = textType,
                text = text,
            ),
        )
    }
}

private const val BLOCK_TYPE_HEADER = "header"
private const val BLOCK_TYPE_SECTION = "section"
private const val TEXT_TYPE_MARKDOWN = "mrkdwn"
private const val TEXT_TYPE_PLAIN_TEXT = "plain_text"
