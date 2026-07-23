package com.example.data.dictionary

object BanglaPhoneticConverter {

    val commonJuktobornoList = listOf(
        "ক্ষ", "জ্ঞ", "ঞ্চ", "ঞ্ছ", "ঞ্জ", "ক্ত", "ক্ষ্ম", "ঙ্ক", "ঙ্গ", "চ্ছ",
        "জ্জ", "ট্ট", "ণ্ট", "ণ্ঠ", "ণ্ড", "ত্ত", "ত্থ", "ত্ম", "ত্র", "ত্ব",
        "দ্দ", "দ্ধ", "দ্ব", "দ্ম", "ন্দ", "ন্ধ", "ন্ন", "প্ত", "প্ল", "ব্দ",
        "ব্ধ", "ম্প", "ম্ব", "ম্ভ", "ম্ম", "ল্ক", "ল্প", "শ্চ", "শ্ব", "শ্ম",
        "ষ্ক", "ষ্ট", "ষ্ঠ", "ষ্ণ", "ষ্প", "স্ক", "স্ত", "স্থ", "স্প", "স্ম", "স্ব", "হ্ম"
    )

    fun convertPhonetic(input: String): String {
        if (input.isEmpty()) return ""
        var text = input.trim()
        
        // Quick phonetic replacements for common english transliteration
        text = text.replace("amar", "আমার")
            .replace("amra", "আমরা")
            .replace("tumi", "তুমি")
            .replace("tomar", "তোমার")
            .replace("kemon", "কেমন")
            .replace("acho", "আছো")
            .replace("achi", "আছি")
            .replace("dhonnobad", "ধন্যবাদ")
            .replace("valo", "ভালো")
            .replace("bhala", "ভালো")
            .replace("khobor", "খবর")
            .replace("bangla", "বাংলা")
            .replace("bangladesh", "বাংলাদেশ")
            .replace("dhaka", "ঢাকা")
            .replace("ki", "কি")
            .replace("kino", "কেন")
            .replace("kothay", "কোথায়")
            .replace("khabar", "খাবার")
            .replace("pani", "পানি")
            .replace("shubho", "শুভ")
            .replace("sokal", "সকাল")
            .replace("sondhya", "সন্ধ্যা")
            .replace("raat", "রাত")

        return text
    }
}
