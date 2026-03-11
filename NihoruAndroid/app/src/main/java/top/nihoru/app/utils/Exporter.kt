package top.nihoru.app.utils

import top.nihoru.app.data.model.AnimeEntry
import org.w3c.dom.Document
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import java.io.StringWriter

object Exporter {

    fun generateXML(list: List<AnimeEntry>, mode: String): String {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc: Document = builder.newDocument()

        val root = doc.createElement("myanimelist")
        doc.appendChild(root)

        val myinfo = doc.createElement("myinfo")
        val exportType = doc.createElement("user_export_type")
        exportType.textContent = "1"
        myinfo.appendChild(exportType)
        root.appendChild(myinfo)

        list.forEach { entry ->
            val finalStatus = entry.userStatus
            val isNotAired = entry.status == "Not yet aired" || entry.status == "Not yet published"

            if (mode == "anime") {
                val totalEps = entry.episodes ?: 0
                var watchedCount = 0
                var effectiveStatus = finalStatus

                if (finalStatus == "Completed") watchedCount = totalEps
                if (isNotAired && (finalStatus == "Completed" || finalStatus == "Watching")) {
                    effectiveStatus = "Plan to Watch"
                    watchedCount = 0
                }

                val node = doc.createElement("anime")
                node.appendChild(createEl(doc, "series_animedb_id", entry.malId.toString()))
                node.appendChild(createCdataEl(doc, "series_title", entry.title))
                node.appendChild(createEl(doc, "series_type", entry.type ?: "TV"))
                node.appendChild(createEl(doc, "series_episodes", totalEps.toString()))
                node.appendChild(createEl(doc, "my_watched_episodes", watchedCount.toString()))
                node.appendChild(createEl(doc, "my_start_date", "0000-00-00"))
                node.appendChild(createEl(doc, "my_finish_date", "0000-00-00"))
                node.appendChild(createEl(doc, "my_score", entry.userScore.toString()))
                node.appendChild(createEl(doc, "my_status", effectiveStatus))
                node.appendChild(createEl(doc, "update_on_import", "1"))
                root.appendChild(node)

            } else {
                val totalChp = entry.chapters ?: 0
                val totalVol = entry.volumes ?: 0
                var readCount = 0
                var effectiveStatus = finalStatus

                if (finalStatus == "Completed") readCount = totalChp
                if (isNotAired && (finalStatus == "Completed" || finalStatus == "Reading")) {
                    effectiveStatus = "Plan to Read"
                    readCount = 0
                }

                val node = doc.createElement("manga")
                node.appendChild(createEl(doc, "manga_mangadb_id", entry.malId.toString()))
                node.appendChild(createCdataEl(doc, "series_title", entry.title))
                node.appendChild(createEl(doc, "series_type", entry.type ?: "Manga"))
                node.appendChild(createEl(doc, "series_chapters", totalChp.toString()))
                node.appendChild(createEl(doc, "series_volumes", totalVol.toString()))
                node.appendChild(createEl(doc, "my_read_chapters", readCount.toString()))
                node.appendChild(createEl(doc, "my_read_volumes",
                    if (effectiveStatus == "Completed" && totalVol > 0) totalVol.toString() else "0"))
                node.appendChild(createEl(doc, "my_start_date", "0000-00-00"))
                node.appendChild(createEl(doc, "my_finish_date", "0000-00-00"))
                node.appendChild(createEl(doc, "my_score", entry.userScore.toString()))
                node.appendChild(createEl(doc, "my_status", effectiveStatus))
                node.appendChild(createEl(doc, "update_on_import", "1"))
                root.appendChild(node)
            }
        }

        val transformer = TransformerFactory.newInstance().newTransformer()
        val writer = StringWriter()
        transformer.transform(DOMSource(doc), StreamResult(writer))
        return writer.toString()
    }

    fun generateJSON(list: List<AnimeEntry>, mode: String): String {
        val statusMap = mapOf(
            "Completed" to "COMPLETED",
            "Plan to Watch" to "PLANNING",
            "Plan to Read" to "PLANNING",
            "Watching" to "CURRENT",
            "Reading" to "CURRENT",
            "On-Hold" to "PAUSED",
            "Dropped" to "DROPPED"
        )

        val entries = list.map { entry ->
            val aniStatus = statusMap[entry.userStatus] ?: "PLANNING"
            val progress = if (aniStatus == "COMPLETED") {
                if (mode == "anime") entry.episodes ?: 0 else entry.chapters ?: 0
            } else 0

            """{"mal_id":${entry.malId},"title":"${entry.title.replace("\"", "\\\"")}","status":"$aniStatus","score":${entry.userScore},"progress":$progress,"type":"${mode.uppercase()}"}"""
        }

        return "[\n${entries.joinToString(",\n")}\n]"
    }

    private fun createEl(doc: Document, tag: String, content: String): org.w3c.dom.Element {
        val el = doc.createElement(tag)
        el.textContent = content
        return el
    }

    private fun createCdataEl(doc: Document, tag: String, content: String): org.w3c.dom.Element {
        val el = doc.createElement(tag)
        el.appendChild(doc.createCDATASection(content))
        return el
    }
}
