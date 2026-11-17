/*
 * Copyright (C) 2025-2026 Flow | A-EDev
 *
 * This file is part of Flow (https://github.com/Flow-Tube/Flow).
 *
 * The word lists below are ported from NewPipe Extractor's timeago patterns
 * (https://github.com/TeamNewPipe/NewPipeExtractor, package
 * org.schabi.newpipe.extractor.timeago.patterns, v0.26.5), Copyright (C) the NewPipe
 * contributors, licensed under the GNU General Public License v3, the same licence as Flow.
 * Flow's changes: lowercased, zero-width characters and embedded numerals dropped, regional
 * variants identical to their base language folded into it, Arabic duals added as fixed
 * amounts, English limited to whole words, and the abbreviated units YouTube writes in the
 * related list added.
 */

package com.jmtube.app.utils.relativedate

internal enum class RelativeDateUnit(
    val millis: Long,
) {
    SECOND(1_000L),
    MINUTE(60_000L),
    HOUR(3_600_000L),
    DAY(86_400_000L),
    WEEK(7L * 86_400_000L),
    MONTH(30L * 86_400_000L),
    YEAR(365L * 86_400_000L),
}

/**
 * How one YouTube host language words an age. [spaced] languages separate words, so a unit only
 * counts as a whole word; the others ("3æ—¥å‰") are matched as a plain substring. [abbreviated] units
 * ("hace 3 m", "5 Ð´Ð½. Ð½Ð°Ð·Ð°Ð´") only count right after a whole number, since a lone letter is also a word.
 */
internal class RelativeDateVocabulary(
    val spaced: Boolean,
    val units: List<Pair<RelativeDateUnit, List<String>>>,
    val fixed: Map<String, Pair<RelativeDateUnit, Int>>,
    val abbreviated: List<Pair<RelativeDateUnit, List<String>>> = emptyList(),
)

/** Keyed by lowercased hl tag ("es", "zh-tw"); [RelativeUploadDateParser] resolves aliases. */
internal object RelativeDatePatterns {
    val byTag: Map<String, RelativeDateVocabulary> =
        mapOf(
            "af" to spaced("sekonde|sekondes", "minute|minuut", "ure|uur", "dae|dag", "week|weke", "maand|maande", "jaar"),
            "am" to spaced("áˆ°áŠ®áŠ•á‹¶á‰½|áˆ´áŠ®áŠ•á‹µ", "á‹°á‰‚á‰ƒ|á‹°á‰‚á‰ƒá‹Žá‰½", "áˆ°á‹“á‰µ|áˆ°á‹“á‰¶á‰½", "á‰€áŠ•|á‰€áŠ–á‰½", "áˆ³áˆáŠ•á‰³á‰µ|áˆ³áˆáŠ•á‰µ", "á‹ˆáˆ«á‰µ|á‹ˆáˆ­", "á‹“áˆ˜á‰³á‰µ|á‹“áˆ˜á‰µ"),
            "ar" to
                spaced(
                    "Ø«Ø§Ù†ÙŠØ©|Ø«ÙˆØ§Ù†Ù",
                    "Ø¯Ù‚Ø§Ø¦Ù‚|Ø¯Ù‚ÙŠÙ‚Ø©",
                    "Ø³Ø§Ø¹Ø§Øª|Ø³Ø§Ø¹Ø©",
                    "Ø£ÙŠØ§Ù…|ÙŠÙˆÙ…",
                    "Ø£Ø³Ø§Ø¨ÙŠØ¹|Ø£Ø³Ø¨ÙˆØ¹",
                    "Ø£Ø´Ù‡Ø±|Ø´Ù‡Ø±|Ø´Ù‡Ø±Ù‹Ø§",
                    "Ø³Ù†Ø©|Ø³Ù†ÙˆØ§Øª",
                    fixed =
                        mapOf(
                            "Ø«Ø§Ù†ÙŠØªÙŠÙ†" to (RelativeDateUnit.SECOND to 2),
                            "Ø¯Ù‚ÙŠÙ‚ØªÙŠÙ†" to (RelativeDateUnit.MINUTE to 2),
                            "Ø³Ø§Ø¹ØªÙŠÙ†" to (RelativeDateUnit.HOUR to 2),
                            "ÙŠÙˆÙ…ÙŠÙ†" to (RelativeDateUnit.DAY to 2),
                            "Ø£Ø³Ø¨ÙˆØ¹ÙŠÙ†" to (RelativeDateUnit.WEEK to 2),
                            "Ø´Ù‡Ø±ÙŠÙ†" to (RelativeDateUnit.MONTH to 2),
                            "Ø³Ù†ØªÙŠÙ†" to (RelativeDateUnit.YEAR to 2),
                        ),
                ),
            "az" to spaced("saniyÉ™", "dÉ™qiqÉ™", "saat", "gÃ¼n", "hÉ™ftÉ™", "ay", "il"),
            "be" to
                spaced(
                    "ÑÐµÐºÑƒÐ½Ð´|ÑÐµÐºÑƒÐ½Ð´Ñƒ|ÑÐµÐºÑƒÐ½Ð´Ñ‹",
                    "Ñ…Ð²Ñ–Ð»Ñ–Ð½|Ñ…Ð²Ñ–Ð»Ñ–Ð½Ñƒ|Ñ…Ð²Ñ–Ð»Ñ–Ð½Ñ‹",
                    "Ð³Ð°Ð´Ð·Ñ–Ð½|Ð³Ð°Ð´Ð·Ñ–Ð½Ñƒ|Ð³Ð°Ð´Ð·Ñ–Ð½Ñ‹",
                    "Ð´Ð·ÐµÐ½ÑŒ|Ð´Ð·Ñ‘Ð½|Ð´Ð½Ñ|Ð´Ð½Ñ–",
                    "Ñ‚Ñ‹Ð´Ð·ÐµÐ½ÑŒ|Ñ‚Ñ‹Ð´Ð½Ñ|Ñ‚Ñ‹Ð´Ð½Ñ–",
                    "Ð¼ÐµÑÑÑ†|Ð¼ÐµÑÑÑ†Ð°|Ð¼ÐµÑÑÑ†Ñ‹|Ð¼ÐµÑÑÑ†Ð°Ñž",
                    "Ð³Ð¾Ð´|Ð³Ð¾Ð´Ð°|Ð³Ð°Ð´Ñ‹|Ð³Ð°Ð´Ð¾Ñž",
                ),
            "bg" to spaced("ÑÐµÐºÑƒÐ½Ð´Ð°|ÑÐµÐºÑƒÐ½Ð´Ð¸", "Ð¼Ð¸Ð½ÑƒÑ‚Ð°|Ð¼Ð¸Ð½ÑƒÑ‚Ð¸", "Ñ‡Ð°Ñ|Ñ‡Ð°ÑÐ°", "Ð´ÐµÐ½|Ð´Ð½Ð¸", "ÑÐµÐ´Ð¼Ð¸Ñ†Ð°|ÑÐµÐ´Ð¼Ð¸Ñ†Ð¸", "Ð¼ÐµÑÐµÑ†|Ð¼ÐµÑÐµÑ†Ð°", "Ð³Ð¾Ð´Ð¸Ð½Ð°|Ð³Ð¾Ð´Ð¸Ð½Ð¸"),
            "bn" to spaced("à¦¸à§‡à¦•à§‡à¦¨à§à¦¡", "à¦®à¦¿à¦¨à¦¿à¦Ÿ", "à¦˜à¦£à§à¦Ÿà¦¾", "à¦¦à¦¿à¦¨", "à¦¸à¦ªà§à¦¤à¦¾à¦¹", "à¦®à¦¾à¦¸", "à¦¬à¦›à¦°"),
            "bs" to
                spaced(
                    "sekundi|sekunde|sekundu",
                    "minuta|minute|minutu",
                    "h|sat|sata|sati",
                    "dan|dana",
                    "sedm.",
                    "mj.|mjesec|mjeseca|mjeseci",
                    "godina|godine|godinu",
                ),
            "ca" to spaced("segon|segons", "minut|minuts", "hora|hores", "dia|dies", "setmana|setmanes", "mes|mesos", "any|anys"),
            "cs" to
                spaced(
                    "sekundami|sekundou",
                    "minutami|minutou",
                    "hodinami|hodinou",
                    "dny|vÄera",
                    "tÃ½dnem|tÃ½dny",
                    "mÄ›sÃ­cem|mÄ›sÃ­ci",
                    "rokem|roky|lety",
                ),
            "da" to spaced("sekund|sekunder", "minut|minutter", "time|timer", "dag|dage", "uge|uger", "mÃ¥ned|mÃ¥neder", "Ã¥r"),
            "de" to
                spaced(
                    "sekunde|sekunden",
                    "minute|minuten",
                    "stunde|stunden",
                    "tag|tagen",
                    "woche|wochen",
                    "monat|monaten",
                    "jahr|jahren",
                    abbreviated = mapOf(RelativeDateUnit.HOUR to "std."),
                ),
            "el" to
                spaced(
                    "Î´ÎµÏ…Ï„ÎµÏÏŒÎ»ÎµÏ€Ï„Î±|Î´ÎµÏ…Ï„ÎµÏÏŒÎ»ÎµÏ€Ï„Î¿",
                    "Î»ÎµÏ€Ï„Î¬|Î»ÎµÏ€Ï„ÏŒ",
                    "ÏŽÏÎ±|ÏŽÏÎµÏ‚",
                    "Î·Î¼Î­ÏÎ±|Î·Î¼Î­ÏÎµÏ‚",
                    "ÎµÎ²Î´Î¿Î¼Î¬Î´Î±|ÎµÎ²Î´Î¿Î¼Î¬Î´ÎµÏ‚",
                    "Î¼Î®Î½Î±|Î¼Î®Î½ÎµÏ‚",
                    "Ï‡ÏÏŒÎ½Î¹Î±|Ï‡ÏÏŒÎ½Î¿",
                ),
            "en" to
                spaced(
                    "second|seconds|sec|secs",
                    "minute|minutes|min|mins",
                    "hour|hours|hr|hrs",
                    "day|days",
                    "week|weeks|wk|wks",
                    "month|months",
                    "year|years|yr|yrs",
                ),
            "es" to
                spaced(
                    "segundo|segundos",
                    "minuto|minutos",
                    "hora|horas",
                    "dÃ­a|dÃ­as",
                    "semana|semanas",
                    "mes|meses",
                    "aÃ±o|aÃ±os",
                    abbreviated =
                        mapOf(
                            RelativeDateUnit.HOUR to "h",
                            RelativeDateUnit.DAY to "d",
                            RelativeDateUnit.WEEK to "sem.",
                            RelativeDateUnit.MONTH to "m",
                            RelativeDateUnit.YEAR to "a",
                        ),
                ),
            "et" to spaced("sekund|sekundit", "minut|minutit", "tund|tundi", "pÃ¤ev|pÃ¤eva", "nÃ¤dal|nÃ¤dalat", "kuu|kuud", "aasta|aastat"),
            "eu" to spaced("segundo", "minutu", "ordu|ordubete", "egun", "aste|astebete", "hilabete", "urte|urtebete"),
            "fa" to spaced("Ø«Ø§Ù†ÛŒÙ‡", "Ø¯Ù‚ÛŒÙ‚Ù‡", "Ø³Ø§Ø¹Øª", "Ø±ÙˆØ²", "Ù‡ÙØªÙ‡", "Ù…Ø§Ù‡", "Ø³Ø§Ù„"),
            "fi" to
                spaced(
                    "sekunti|sekuntia",
                    "minuutti|minuuttia",
                    "tunti|tuntia",
                    "pÃ¤ivÃ¤|pÃ¤ivÃ¤Ã¤",
                    "viikko|viikkoa",
                    "kuukausi|kuukautta",
                    "vuosi|vuotta",
                ),
            "fil" to spaced("segundo", "minuto", "oras", "araw", "linggo", "buwan", "taon"),
            "fr" to
                spaced(
                    "seconde|secondes",
                    "minute|minutes",
                    "heure|heures",
                    "jour|jours",
                    "semaine|semaines",
                    "mois",
                    "an|ans",
                    abbreviated =
                        mapOf(
                            RelativeDateUnit.HOUR to "h",
                            RelativeDateUnit.DAY to "j",
                            RelativeDateUnit.WEEK to "sem.",
                            RelativeDateUnit.MONTH to "m.",
                            RelativeDateUnit.YEAR to "a",
                        ),
                ),
            "gl" to spaced("segundo|segundos", "minuto|minutos", "hora|horas", "dÃ­a|dÃ­as", "semana|semanas", "mes|meses", "ano|anos"),
            "gu" to spaced("àª¸à«‡àª•àª‚àª¡", "àª®àª¿àª¨àª¿àªŸ", "àª•àª²àª¾àª•", "àª¦àª¿àªµàª¸", "àª…àª àªµàª¾àª¡àª¿àª¯àª¾", "àª®àª¹àª¿àª¨àª¾", "àªµàª°à«àª·"),
            "hi" to spaced("à¤¸à¥‡à¤•à¤‚à¤¡", "à¤®à¤¿à¤¨à¤Ÿ", "à¤˜à¤‚à¤Ÿà¤¾|à¤˜à¤‚à¤Ÿà¥‡", "à¤¦à¤¿à¤¨", "à¤¸à¤ªà¥à¤¤à¤¾à¤¹|à¤¹à¤«à¤¼à¥à¤¤à¥‡", "à¤®à¤¹à¥€à¤¨à¤¾|à¤®à¤¹à¥€à¤¨à¥‡", "à¤µà¤°à¥à¤·"),
            "hr" to
                spaced(
                    "sekunde|sekundi|sekundu",
                    "minuta|minute|minutu",
                    "sat|sata|sati",
                    "dan|dana",
                    "tjedan|tjedna",
                    "mjesec|mjeseca|mjeseci",
                    "godina|godine|godinu",
                ),
            "hu" to spaced("mÃ¡sodperce", "perce", "Ã³rÃ¡ja", "napja", "hete", "hÃ³napja", "Ã©ve"),
            "hy" to spaced("Õ¾Õ¡ÕµÖ€Õ¯ÕµÕ¡Õ¶", "Ö€Õ¸ÕºÕ¥", "ÕªÕ¡Õ´", "Ö…Ö€", "Õ·Õ¡Õ¢Õ¡Õ©", "Õ¡Õ´Õ«Õ½", "Õ¿Õ¡Ö€Õ«"),
            "id" to spaced("detik", "menit", "jam", "hari", "minggu", "bulan", "tahun"),
            "is" to
                spaced(
                    "sekÃºndu|sekÃºndum|second|seconds",
                    "mÃ­nÃºtu|mÃ­nÃºtum|minute|minutes",
                    "klukkustund|klukkustundum|hour|hours",
                    "degi|dÃ¶gum|day|days",
                    "viku|vikum|week|weeks",
                    "mÃ¡nuÃ°i|mÃ¡nuÃ°um",
                    "Ã¡ri|Ã¡rum",
                ),
            "it" to
                spaced(
                    "secondi|secondo",
                    "minuti|minuto",
                    "ora|ore",
                    "giorni|giorno",
                    "settimana|settimane",
                    "mese|mesi",
                    "anni|anno",
                    abbreviated =
                        mapOf(
                            RelativeDateUnit.HOUR to "h",
                            RelativeDateUnit.DAY to "g|gg",
                            RelativeDateUnit.WEEK to "sett.",
                        ),
                ),
            "iw" to
                spaced(
                    "×©× ×™×•×ª|×©× ×™×™×”",
                    "×“×§×”|×“×§×•×ª",
                    "×©×¢×”|×©×¢×•×ª",
                    "×™×•×|×™×ž×™×",
                    "×©×‘×•×¢|×©×‘×•×¢×•×ª",
                    "×—×•×“×©|×—×•×“×©×™×",
                    "×©× ×”|×©× ×™×",
                    fixed =
                        mapOf(
                            "×©×¢×ª×™×™×" to (RelativeDateUnit.HOUR to 2),
                            "×™×•×ž×™×™×" to (RelativeDateUnit.DAY to 2),
                            "×©×‘×•×¢×™×™×" to (RelativeDateUnit.WEEK to 2),
                            "×—×•×“×©×™×™×" to (RelativeDateUnit.MONTH to 2),
                            "×©× ×ª×™×™×" to (RelativeDateUnit.YEAR to 2),
                        ),
                ),
            "ja" to unspaced("ç§’å‰", "åˆ†å‰", "æ™‚é–“å‰", "æ—¥å‰", "é€±é–“å‰", "ã‹æœˆå‰", "å¹´å‰"),
            "ka" to spaced("áƒ¬áƒáƒ›áƒ˜áƒ¡", "áƒ¬áƒ£áƒ—áƒ˜áƒ¡", "áƒ¡áƒáƒáƒ—áƒ˜áƒ¡", "áƒ“áƒ¦áƒ˜áƒ¡", "áƒ™áƒ•áƒ˜áƒ áƒ˜áƒ¡", "áƒ—áƒ•áƒ˜áƒ¡", "áƒ¬áƒšáƒ˜áƒ¡"),
            "kk" to spaced("ÑÐµÐºÑƒÐ½Ð´", "Ð¼Ð¸Ð½ÑƒÑ‚", "ÑÐ°Ò“Ð°Ñ‚", "ÐºÒ¯Ð½", "Ð°Ð¿Ñ‚Ð°", "Ð°Ð¹", "Ð¶Ñ‹Ð»"),
            "km" to unspaced("ážœáž·áž“áž¶áž‘áž¸áž˜áž»áž“", "áž“áž¶áž‘áž¸áž˜áž»áž“", "áž˜áŸ‰áŸ„áž„áž˜áž»áž“", "ážáŸ’áž„áŸƒáž˜áž»áž“", "ážŸáž”áŸ’ážáž¶áž áŸáž˜áž»áž“", "ážáŸ‚áž˜áž»áž“", "áž†áŸ’áž“áž¶áŸ†áž˜áž»áž“"),
            "kn" to
                spaced(
                    "à²¸à³†à²•à³†à²‚à²¡à³à²—à²³|à²¸à³†à²•à³†à²‚à²¡à³",
                    "à²¨à²¿à²®à²¿à²·à²—à²³|à²¨à²¿à²®à²¿à²·à²¦",
                    "à²—à²‚à²Ÿà³†à²—à²³|à²—à²‚à²Ÿà³†à²¯",
                    "à²¦à²¿à²¨à²—à²³|à²¦à²¿à²¨à²¦",
                    "à²µà²¾à²°à²—à²³|à²µà²¾à²°à²¦",
                    "à²¤à²¿à²‚à²—à²³|à²¤à²¿à²‚à²—à²³à³à²—à²³",
                    "à²µà²°à³à²·à²—à²³|à²µà²°à³à²·à²¦",
                ),
            "ko" to unspaced("ì´ˆ", "ë¶„", "ì‹œê°„", "ì¼", "ì£¼", "ê°œì›”", "ë…„"),
            "ky" to spaced("ÑÐµÐºÑƒÐ½Ð´", "Ð¼Ò¯Ð½Ó©Ñ‚", "ÑÐ°Ð°Ñ‚", "ÐºÒ¯Ð½", "Ð°Ð¿Ñ‚Ð°", "Ð°Ð¹", "Ð¶Ñ‹Ð»"),
            "lo" to unspaced("àº§àº´àº™àº²àº—àºµàºà»ˆàº­àº™àº™àºµà»‰", "àº™àº²àº—àºµàºà»ˆàº­àº™àº™àºµà»‰", "àºŠàº»à»ˆàº§à»‚àº¡àº‡àºà»ˆàº­àº™àº™àºµà»‰", "àº¡àº·à»‰àºà»ˆàº­àº™àº™àºµà»‰", "àº­àº²àº—àº´àº”àºà»ˆàº­àº™àº™àºµà»‰", "à»€àº”àº·àº­àº™àºà»ˆàº­àº™àº™àºµà»‰", "àº›àºµàºà»ˆàº­àº™àº™àºµà»‰"),
            "lt" to
                spaced(
                    "sekundes|sekundÄ™|sekundÅ¾iÅ³",
                    "minutes|minutÄ™|minuÄiÅ³",
                    "valandas|valandÄ…|valandÅ³",
                    "dienas|dienÄ…",
                    "savaites|savaitÄ™",
                    "mÄ—nesius|mÄ—nesiÅ³|mÄ—nesÄ¯",
                    "metus|metÅ³",
                ),
            "lv" to
                spaced(
                    "sekundes|sekundÄ“m",
                    "minÅ«tes|minÅ«tÄ“m",
                    "stundas|stundÄm",
                    "dienas|dienÄm",
                    "nedÄ“Ä¼as|nedÄ“Ä¼Äm",
                    "mÄ“neÅ¡a|mÄ“neÅ¡iem",
                    "gada|gadiem",
                ),
            "mk" to spaced("ÑÐµÐºÑƒÐ½Ð´Ð°|ÑÐµÐºÑƒÐ½Ð´Ð¸", "Ð¼Ð¸Ð½ÑƒÑ‚Ð°|Ð¼Ð¸Ð½ÑƒÑ‚Ð¸", "Ñ‡Ð°Ñ|Ñ‡Ð°ÑÐ°", "Ð´ÐµÐ½|Ð´ÐµÐ½Ð°", "Ð½ÐµÐ´ÐµÐ»Ð°|Ð½ÐµÐ´ÐµÐ»Ð¸", "Ð¼ÐµÑÐµÑ†|Ð¼ÐµÑÐµÑ†Ð¸", "Ð³Ð¾Ð´Ð¸Ð½Ð°|Ð³Ð¾Ð´Ð¸Ð½Ð¸"),
            "ml" to unspaced("à´¸àµ†à´•àµà´•à´¨àµà´±àµ|à´¸àµ†à´•àµà´•àµ»à´¡àµ", "à´®à´¿à´¨à´¿à´±àµà´±àµ", "à´®à´£à´¿à´•àµà´•àµ‚àµ¼", "à´¦à´¿à´µà´¸à´‚", "à´†à´´àµà´š", "à´®à´¾à´¸à´‚", "à´µàµ¼à´·à´‚"),
            "mn" to spaced("ÑÐµÐºÑƒÐ½Ð´Ñ‹Ð½", "Ð¼Ð¸Ð½ÑƒÑ‚Ñ‹Ð½", "Ñ†Ð°Ð³Ð¸Ð¹Ð½", "Ó©Ð´Ñ€Ð¸Ð¹Ð½", "Ð´Ð¾Ð»Ð¾Ð¾|Ñ…Ð¾Ð½Ð¾Ð³Ð¸Ð¹Ð½", "ÑÐ°Ñ€Ñ‹Ð½", "Ð¶Ð¸Ð»Ð¸Ð¹Ð½"),
            "mr" to
                unspaced(
                    "à¤¸à¥‡à¤•à¤‚à¤¦à¤¾à¤‚à¤ªà¥‚à¤°à¥à¤µà¥€|à¤¸à¥‡à¤•à¤‚à¤¦à¤¾à¤ªà¥‚à¤°à¥à¤µà¥€",
                    "à¤®à¤¿à¤¨à¤¿à¤Ÿà¤¾à¤‚à¤ªà¥‚à¤°à¥à¤µà¥€|à¤®à¤¿à¤¨à¤¿à¤Ÿà¤¾à¤ªà¥‚à¤°à¥à¤µà¥€",
                    "à¤¤à¤¾à¤¸à¤¾à¤‚à¤ªà¥‚à¤°à¥à¤µà¥€|à¤¤à¤¾à¤¸à¤¾à¤ªà¥‚à¤°à¥à¤µà¥€",
                    "à¤¦à¤¿à¤µà¤¸à¤¾à¤‚à¤ªà¥‚à¤°à¥à¤µà¥€|à¤¦à¤¿à¤µà¤¸à¤¾à¤ªà¥‚à¤°à¥à¤µà¥€",
                    "à¤†à¤ à¤µà¤¡à¥à¤¯à¤¾à¤‚à¤ªà¥‚à¤°à¥à¤µà¥€|à¤†à¤ à¤µà¤¡à¥à¤¯à¤¾à¤ªà¥‚à¤°à¥à¤µà¥€",
                    "à¤®à¤¹à¤¿à¤¨à¥à¤¯à¤¾à¤‚à¤ªà¥‚à¤°à¥à¤µà¥€|à¤®à¤¹à¤¿à¤¨à¥à¤¯à¤¾à¤ªà¥‚à¤°à¥à¤µà¥€",
                    "à¤µà¤°à¥à¤·à¤¾à¤‚à¤ªà¥‚à¤°à¥à¤µà¥€|à¤µà¤°à¥à¤·à¤¾à¤ªà¥‚à¤°à¥à¤µà¥€",
                ),
            "ms" to spaced("saat", "minit", "jam", "hari", "minggu", "bulan", "tahun"),
            "my" to spaced("á€…á€€á€¹á€€á€”á€·á€º", "á€™á€­á€”á€…á€º", "á€”á€¬á€›á€®", "á€›á€€á€º", "á€•á€á€º", "á€œ", "á€”á€¾á€…á€º"),
            "ne" to spaced("à¤¸à¥‡à¤•à¥‡à¤¨à¥à¤¡", "à¤®à¤¿à¤¨à¥‡à¤Ÿ", "à¤˜à¤¨à¥à¤Ÿà¤¾", "à¤¦à¤¿à¤¨", "à¤¹à¤ªà¥à¤¤à¤¾", "à¤®à¤¹à¤¿à¤¨à¤¾", "à¤µà¤°à¥à¤·"),
            "nl" to spaced("seconde|seconden", "minuten|minuut", "uur", "dag|dagen", "week|weken", "maand|maanden", "jaar"),
            "no" to spaced("sekund|sekunder", "minutt|minutter", "time|timer", "dag|dager", "uke|uker", "md.", "Ã¥r"),
            "pa" to spaced("à¨¸à¨•à¨¿à©°à¨Ÿ", "à¨®à¨¿à©°à¨Ÿ", "à¨˜à©°à¨Ÿà¨¾|à¨˜à©°à¨Ÿà©‡", "à¨¦à¨¿à¨¨", "à¨¹à¨«à¨¼à¨¤à¨¾|à¨¹à¨«à¨¼à¨¤à©‡", "à¨®à¨¹à©€à¨¨à¨¾|à¨®à¨¹à©€à¨¨à©‡", "à¨¸à¨¾à¨²"),
            "pl" to
                spaced(
                    "sekund|sekundy|sekundÄ™",
                    "minut|minuty|minutÄ™",
                    "godzin|godziny|godzinÄ™",
                    "dni|dzieÅ„",
                    "tydzieÅ„|tygodnie",
                    "miesiÄ…c|miesiÄ…ce|miesiÄ™cy",
                    "lat|lata|rok",
                ),
            "pt" to
                spaced(
                    "segundo|segundos",
                    "minuto|minutos",
                    "hora|horas",
                    "dia|dias",
                    "semana|semanas",
                    "meses|mÃªs",
                    "ano|anos",
                    abbreviated = mapOf(RelativeDateUnit.WEEK to "sem."),
                ),
            "ro" to spaced("secunde|secundÄƒ", "minut|minute", "ore|orÄƒ", "zi|zile", "sÄƒptÄƒmÃ¢ni|sÄƒptÄƒmÃ¢nÄƒ", "luni|lunÄƒ", "an|ani"),
            "ru" to
                spaced(
                    "ÑÐµÐºÑƒÐ½Ð´|ÑÐµÐºÑƒÐ½Ð´Ñƒ|ÑÐµÐºÑƒÐ½Ð´Ñ‹|Ñ‚Ð¾Ð»ÑŒÐºÐ¾ Ñ‡Ñ‚Ð¾",
                    "Ð¼Ð¸Ð½ÑƒÑ‚|Ð¼Ð¸Ð½ÑƒÑ‚Ñƒ|Ð¼Ð¸Ð½ÑƒÑ‚Ñ‹",
                    "Ñ‡Ð°Ñ|Ñ‡Ð°ÑÐ°|Ñ‡Ð°ÑÐ¾Ð²",
                    "Ð´ÐµÐ½ÑŒ|Ð´Ð½ÐµÐ¹|Ð´Ð½Ñ",
                    "Ð½ÐµÐ´ÐµÐ»ÑŽ|Ð½ÐµÐ´ÐµÐ»Ð¸",
                    "Ð¼ÐµÑÑÑ†|Ð¼ÐµÑÑÑ†Ð°|Ð¼ÐµÑÑÑ†ÐµÐ²",
                    "Ð³Ð¾Ð´|Ð³Ð¾Ð´Ð°|Ð»ÐµÑ‚",
                    abbreviated =
                        mapOf(
                            RelativeDateUnit.HOUR to "Ñ‡",
                            RelativeDateUnit.DAY to "Ð´Ð½.",
                            RelativeDateUnit.WEEK to "Ð½ÐµÐ´.",
                            RelativeDateUnit.MONTH to "Ð¼ÐµÑ.",
                            RelativeDateUnit.YEAR to "Ð³.|Ð».",
                        ),
                ),
            "si" to spaced("à¶­à¶­à·Šà¶´à¶»", "à¶¸à·’à¶±à·’à¶­à·Šà¶­à·”", "à¶´à·à¶º", "à¶¯à·’à¶±", "à·ƒà¶­à·’", "à¶¸à·à·ƒ", "à·€à·ƒà¶»"),
            "sk" to
                spaced(
                    "sekundami|sekundou",
                    "minÃºtami|minÃºtou",
                    "hodinami|hodinou",
                    "dÅˆami|dÅˆom",
                    "tÃ½Å¾dÅˆami|tÃ½Å¾dÅˆom",
                    "mesiacmi|mesiacom",
                    "rokmi|rokom",
                ),
            "sl" to
                spaced(
                    "sekundama|sekundami|sekundo",
                    "minutama|minutami|minuto",
                    "urama|urami|uro",
                    "dnem|dnevi|dnevoma",
                    "tedni|tednom|tednoma",
                    "mesecem|mesecema|meseci",
                    "leti|letom|letoma",
                ),
            "sq" to spaced("sekonda|sekondÃ«", "minuta|minutÃ«", "orÃ«", "ditÃ«", "javÃ«", "muaj", "vit|vjet"),
            "sr" to
                spaced(
                    "ÑÐµÐºÑƒÐ½Ð´Ðµ|ÑÐµÐºÑƒÐ½Ð´Ð¸",
                    "Ð¼Ð¸Ð½ÑƒÑ‚Ð°",
                    "ÑÐ°Ñ‚|ÑÐ°Ñ‚Ð°|ÑÐ°Ñ‚Ð¸",
                    "Ð´Ð°Ð½|Ð´Ð°Ð½Ð°",
                    "Ð½ÐµÐ´ÐµÑ™Ðµ|Ð½ÐµÐ´ÐµÑ™Ñƒ",
                    "Ð¼ÐµÑÐµÑ†|Ð¼ÐµÑÐµÑ†Ð°|Ð¼ÐµÑÐµÑ†Ð¸",
                    "Ð³Ð¾Ð´Ð¸Ð½Ð°|Ð³Ð¾Ð´Ð¸Ð½Ðµ|Ð³Ð¾Ð´Ð¸Ð½Ñƒ",
                ),
            "sr-latn" to
                spaced(
                    "sekunde|sekundi",
                    "minuta",
                    "sat|sati|sata",
                    "dan|dana",
                    "nedelja|nedelje|nedelju",
                    "mesec|meseci|meseca",
                    "godine|godina|godinu",
                ),
            "sv" to spaced("sekund|sekunder", "minut|minuter", "timmar|timme", "dag|dagar", "vecka|veckor", "mÃ¥nad|mÃ¥nader", "Ã¥r"),
            "sw" to spaced("sekunde", "dakika", "saa", "siku", "wiki", "mwezi|miezi", "miaka|mwaka"),
            "ta" to
                spaced(
                    "à®µà®¿à®©à®¾à®Ÿà®¿|à®µà®¿à®©à®¾à®Ÿà®¿à®•à®³à¯à®•à¯à®•à¯",
                    "à®¨à®¿à®®à®¿à®Ÿà®™à¯à®•à®³à¯|à®¨à®¿à®®à®¿à®Ÿà®®à¯",
                    "à®®à®£à®¿à®¨à¯‡à®°à®¤à¯à®¤à®¿à®±à¯à®•à¯",
                    "à®¨à®¾à®Ÿà¯à®•à®³à¯à®•à¯à®•à¯|à®¨à®¾à®³à¯à®•à¯à®•à¯",
                    "à®µà®¾à®°à®™à¯à®•à®³à¯à®•à¯à®•à¯|à®µà®¾à®°à®®à¯",
                    "à®®à®¾à®¤à®™à¯à®•à®³à¯|à®®à®¾à®¤à®®à¯",
                    "à®†à®£à¯à®Ÿà¯|à®†à®£à¯à®Ÿà¯à®•à®³à¯à®•à¯à®•à¯",
                ),
            "te" to spaced("à°¸à±†à°•à°¨à±|à°¸à±†à°•à°¨à±à°²", "à°¨à°¿à°®à°¿à°·à°‚|à°¨à°¿à°®à°¿à°·à°¾à°²", "à°—à°‚à°Ÿ|à°—à°‚à°Ÿà°²", "à°°à±‹à°œà±|à°°à±‹à°œà±à°²", "à°µà°¾à°°à°‚|à°µà°¾à°°à°¾à°²", "à°¨à±†à°²|à°¨à±†à°²à°²", "à°¸à°‚à°µà°¤à±à°¸à°°à°‚|à°¸à°‚à°µà°¤à±à°¸à°°à°¾à°²"),
            "th" to
                unspaced(
                    "à¸§à¸´à¸™à¸²à¸—à¸µà¸—à¸µà¹ˆà¸œà¹ˆà¸²à¸™à¸¡à¸²",
                    "à¸™à¸²à¸—à¸µà¸—à¸µà¹ˆà¸œà¹ˆà¸²à¸™à¸¡à¸²",
                    "à¸Šà¸±à¹ˆà¸§à¹‚à¸¡à¸‡à¸—à¸µà¹ˆà¸œà¹ˆà¸²à¸™à¸¡à¸²",
                    "à¸§à¸±à¸™à¸—à¸µà¹ˆà¸œà¹ˆà¸²à¸™à¸¡à¸²",
                    "à¸ªà¸±à¸›à¸”à¸²à¸«à¹Œà¸—à¸µà¹ˆà¸œà¹ˆà¸²à¸™à¸¡à¸²",
                    "à¹€à¸”à¸·à¸­à¸™à¸—à¸µà¹ˆà¸œà¹ˆà¸²à¸™à¸¡à¸²",
                    "à¸›à¸µà¸—à¸µà¹ˆà¸œà¹ˆà¸²à¸™à¸¡à¸²",
                ),
            "tr" to spaced("saniye", "dakika", "saat", "gÃ¼n", "hafta", "ay", "yÄ±l"),
            "uk" to
                spaced(
                    "ÑÐµÐºÑƒÐ½Ð´|ÑÐµÐºÑƒÐ½Ð´Ð¸|ÑÐµÐºÑƒÐ½Ð´Ñƒ",
                    "Ñ…Ð²Ð¸Ð»Ð¸Ð½|Ñ…Ð²Ð¸Ð»Ð¸Ð½Ð¸|Ñ…Ð²Ð¸Ð»Ð¸Ð½Ñƒ",
                    "Ð³Ð¾Ð´Ð¸Ð½|Ð³Ð¾Ð´Ð¸Ð½Ð¸|Ð³Ð¾Ð´Ð¸Ð½Ñƒ",
                    "Ð´ÐµÐ½ÑŒ|Ð´Ð½Ñ–|Ð´Ð½Ñ–Ð²",
                    "Ñ‚Ð¸Ð¶Ð´ÐµÐ½ÑŒ|Ñ‚Ð¸Ð¶Ð½Ñ–",
                    "Ð¼Ñ–ÑÑÑ†ÑŒ|Ð¼Ñ–ÑÑÑ†Ñ–|Ð¼Ñ–ÑÑÑ†Ñ–Ð²",
                    "Ñ€Ð¾ÐºÐ¸|Ñ€Ð¾ÐºÑ–Ð²|Ñ€Ñ–Ðº",
                ),
            "ur" to spaced("Ø³ÛŒÚ©Ù†Úˆ|Ø³ÛŒÚ©Ù†ÚˆØ²", "Ù…Ù†Ù¹|Ù…Ù†Ù¹Ø³", "Ú¯Ú¾Ù†Ù¹Û|Ú¯Ú¾Ù†Ù¹Û’", "Ø¯Ù†", "ÛÙØªÛ|ÛÙØªÛ’", "Ù…Ø§Û", "Ø³Ø§Ù„"),
            "uz" to spaced("soniya", "daqiqa", "soat", "kun", "hafta", "oy", "yil"),
            "vi" to spaced("giÃ¢y", "phÃºt", "giá»|tiáº¿ng", "ngÃ y", "tuáº§n", "thÃ¡ng", "nÄƒm"),
            "zh-cn" to unspaced("ç§’å‰", "åˆ†é’Ÿå‰", "å°æ—¶å‰", "å¤©å‰", "å‘¨å‰", "ä¸ªæœˆå‰", "å¹´å‰"),
            "zh-hk" to unspaced("ç§’å‰", "åˆ†é˜å‰", "å°æ™‚å‰", "å¤©å‰", "é€±å‰", "å€‹æœˆå‰", "å¹´å‰"),
            "zh-tw" to unspaced("ç§’å‰", "åˆ†é˜å‰", "å°æ™‚å‰", "å¤©å‰", "é€±å‰", "å€‹æœˆå‰", "å¹´å‰"),
            "zu" to
                spaced(
                    "amasekhondi|isekhondi",
                    "amaminithi|iminithi",
                    "amahora|ihora",
                    "izinsuku|usuku",
                    "amaviki|iviki",
                    "inyanga|izinyanga",
                    "iminyaka|unyaka",
                ),
        )

    private fun spaced(
        seconds: String,
        minutes: String,
        hours: String,
        days: String,
        weeks: String,
        months: String,
        years: String,
        fixed: Map<String, Pair<RelativeDateUnit, Int>> = emptyMap(),
        abbreviated: Map<RelativeDateUnit, String> = emptyMap(),
    ) = vocabulary(true, listOf(seconds, minutes, hours, days, weeks, months, years), fixed, abbreviated)

    private fun unspaced(
        seconds: String,
        minutes: String,
        hours: String,
        days: String,
        weeks: String,
        months: String,
        years: String,
    ) = vocabulary(false, listOf(seconds, minutes, hours, days, weeks, months, years), emptyMap())

    private fun vocabulary(
        spaced: Boolean,
        words: List<String>,
        fixed: Map<String, Pair<RelativeDateUnit, Int>>,
        abbreviated: Map<RelativeDateUnit, String> = emptyMap(),
    ) = RelativeDateVocabulary(
        spaced = spaced,
        units = RelativeDateUnit.entries.zip(words) { unit, list -> unit to list.split('|') },
        fixed = fixed,
        abbreviated = abbreviated.map { (unit, list) -> unit to list.split('|') },
    )
}

