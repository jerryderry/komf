package snd.komf.util

import snd.komf.model.BookRange

object BookNameParser {
    private val volumeRegexes = listOf(
        "(?i),?\\s\\(?volume\\s(?<volumeStart>[0-9]+)(,?\\s?[0-9]+,)+(?<volumeEnd>\\s?[0-9]+)\\)?".toRegex(),
        "(?i),?\\s\\(?([vtT]|vols\\.\\s|vol\\.\\s|volume\\s)(?<volumeStart>[0-9]+([.x#][0-9]+)?)(?<volumeEnd>-[0-9]+([.x#][0-9]+)?)?\\)?".toRegex(),
        ".*第(?<volumeStart>\\d+)-?(?<volumeEnd>\\d+)?.*巻".toRegex(),
        ".*年(?:[0-9]+月)?(?:[0-9]+日)?(?<volumeStart>\\d+)-?(?<volumeEnd>\\d+)?号".toRegex(),
    )

    private val chapterRegexes = listOf(
        // Chapter markers appear with any separator, not just a space: "ch. 12",
        // "ch12", "_ch12", "-Ep.12". The bare "c" form stays restricted to a
        // preceding space, because convention codes look identical otherwise -
        // "(C105)" is Comiket 105, not chapter 105, and a looser pattern
        // mis-reads a large part of a doujinshi library as chapters.
        ("(?i)(?:(?<=\\s)c|(?:^|[\\s_\\-\\[(])(?:chapter|episode|ch|ep))" +
            "\\.?[\\s_\\-]*(?<start>[0-9]+([.x#][0-9]+)?)(?<end>-[0-9]+([.x#][0-9]+)?)?").toRegex(),
        // 話 is the Japanese form; 话 the simplified Chinese one, which is what
        // Chinese-language releases use and which was previously unmatched.
        ".*第(?<start>\\d+(\\.\\d+)?)-?(?<end>\\d+(\\.\\d+)?)?.*[話话]".toRegex(),
    )
    private val bookNumberRegexes = listOf(
        "(?i)(?:\\s|#|no\\.)(?<start>[0-9]+[AB]?([.x#][0-9]+)?)(?<end>-[0-9]+([.x#][0-9]+)?)?(?:\\s\\(.*\\)\\s*)*$".toRegex(),
        "Issue (?<start>[0-9]+[AB]?([.x#][0-9]+)?)(?<end>-[0-9]+([.x#][0-9]+)?)?".toRegex(),
        "Volume (?<start>[0-9]+[AB]?([.x#][0-9]+)?)(?<end>-[0-9]+([.x#][0-9]+)?)?".toRegex(),
    )
    private val extraDataRegex = "\\[(?<extra>.*?)]".toRegex()

    fun getVolumes(name: String): BookRange? {
        val matchedGroups = volumeRegexes.firstNotNullOfOrNull { it.find(name)?.groups }
        val startVolume = matchedGroups?.get("volumeStart")?.value
            ?.replace("[x#]".toRegex(), ".")
            ?.toDoubleOrNull()
        val endVolume = matchedGroups?.get("volumeEnd")?.value
            ?.replace("-", "")
            ?.replace("[x#]".toRegex(), ".")
            ?.toDoubleOrNull()

        return if (startVolume != null && endVolume != null) {
            BookRange(startVolume, endVolume)
        } else if (startVolume != null) {
            BookRange(startVolume, startVolume)
        } else null
    }

    fun getChapters(name: String) = getBookNumber(name, chapterRegexes)
    fun getBookNumber(name: String) = getBookNumber(name, bookNumberRegexes)

    private fun getBookNumber(name: String, regexes: List<Regex>): BookRange? {
        val matchedGroups = regexes.firstNotNullOfOrNull { it.findAll(name).lastOrNull()?.groups }
        val startChapter = matchedGroups?.get("start")?.value
            ?.replace("[x#]".toRegex(), ".")
            ?.toDoubleOrNull()
        val endChapter = matchedGroups?.get("end")?.value
            ?.replace("-", "")
            ?.replace("[x#]".toRegex(), ".")
            ?.toDoubleOrNull()

        return if (startChapter != null && endChapter != null) {
            BookRange(startChapter, endChapter)
        } else if (startChapter != null) {
            BookRange(startChapter)
        } else null
    }

    fun getExtraData(name: String): List<String> {
        return extraDataRegex.findAll(name).mapNotNull { it.groups["extra"]?.value }.toList()
    }
}
