package network.loki.messenger

import android.content.Context
import android.text.format.DateUtils
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.runner.RunWith
import org.session.libsignal.utilities.Log
import org.thoughtcrime.securesms.ApplicationContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
@SmallTest
class LocalisedStringTests {

    private val TAG = "LocalisedStringTests"

    // ACL Note: This `to2partString` method is from Andy and will print things like "2h 14m" but it does NOT localise at all based on locale!
    private fun Duration.to2partString(): String? =
        toComponents { days, hours, minutes, seconds, nanoseconds -> listOf(days.days, hours.hours, minutes.minutes, seconds.seconds) }
            .filter { it.inWholeSeconds > 0L }.take(2).takeIf { it.isNotEmpty() }?.joinToString(" ")
    
    fun getAbbreviatedDurationStringMinutes(duration: Duration) : String {
        //return DateUtils.getRelativeTimeSpanString(duration.inWholeMilliseconds, 0L, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString()
        return DateUtils.getRelativeTimeSpanString(duration.inWholeMilliseconds, 0L, 0, DateUtils.FORMAT_ABBREV_RELATIVE).toString()
        //return DateUtils.getRelativeDateTimeString(duration.inWholeMilliseconds, 0L, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString()
        //return DateUtils.getTimeSpanString(duration, nowMS, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString()
    }

    // DateUtils
    fun getDateUtilsStringWithoutPreposition(context: Context, duration: Duration) : String {
        return DateUtils.getRelativeTimeSpanString(context, duration.inWholeMilliseconds, false).toString()
    }

    fun Duration.inWholeWeeks(): Int {
      return this.inWholeDays.floorDiv(7).toInt()
    }

    fun Duration.weeks(): Long {
        return this.inWholeDays.floorDiv(7)
    }

    val languageNameKey = "LanguageName"
    val languageCodeKey = "LanguageName"
    val weekKey         = "Week"
    val weeksKey        = "Weeks"
    val dayKey          = "Day"
    val daysKey         = "Days"
    val hourKey         = "Hour"
    val hoursKey        = "Hours"
    val minuteKey       = "Minute"
    val minutesKey      = "Minutes"
    val secondKey       = "Second"
    val secondsKey      = "Seconds"

    // Flat timestamps
    val oneSecond                = 1.seconds
    val twoSeconds               = 2.seconds
    val oneMinute                = 1.minutes
    val twoMinutes               = 2.minutes
    val oneHour                  = 1.hours
    val twoHours                 = 2.hours
    val oneDay                   = 1.days
    val twoDays                  = 2.days
    val oneDaySevenHours         = 1.days.plus(7.hours)
    val fourDaysTwentyThreeHours = 4.days.plus(23.hours)
    val oneWeekTwoDays           = 9.days
    val twoWeekTwoDays           = 16.days

    fun formatForSingleLargestTimeUnit(timeStringMap: Map<String, String>) {
        val durations = listOf(oneSecond, twoSeconds, oneMinute, twoMinutes, oneHour, twoHours, oneDay, twoDays, oneDaySevenHours, fourDaysTwentyThreeHours, oneWeekTwoDays, twoWeekTwoDays)
        val desc = listOf("1s: ", "2s: ", "1m: ", "2m: ", "1h: ", "2h: ", "1d: ", "2d: ", "1d7h: ", "4d23h: ", "1w2d: ", "2w2d: ")

        var i = 0
        println("----- Single Largest Time Unit: ${Locale.getDefault().language} -----")
        for (duration in durations) {
            print(desc[i++])
            val s = if (duration.inWholeWeeks() > 0) {
                if (duration.inWholeWeeks() > 1) "${duration.inWholeWeeks()} ${timeStringMap[weeksKey]}"   else "${duration.inWholeWeeks()} ${timeStringMap[weekKey]}"
            } else if (duration.inWholeDays > 0) {
                if (duration.inWholeDays > 1)    "${duration.inWholeDays} ${timeStringMap[daysKey]}"       else "${duration.inWholeDays} ${timeStringMap[dayKey]}"
            } else if (duration.inWholeHours > 0) {
                if (duration.inWholeHours > 1)   "${duration.inWholeHours} ${timeStringMap[hoursKey]}"     else "${duration.inWholeHours} ${timeStringMap[hourKey]}"
            } else if (duration.inWholeMinutes > 0) {
                if (duration.inWholeMinutes > 1) "${duration.inWholeMinutes} ${timeStringMap[minutesKey]}" else "${duration.inWholeMinutes} ${timeStringMap[minuteKey]}"
            } else {
                if (duration.inWholeSeconds > 1) "${duration.inWholeSeconds} ${timeStringMap[secondsKey]}" else "${duration.inWholeSeconds} ${timeStringMap[secondKey]}"
            }
            println(s)
        }
    }

    fun formatForDualLargestTimeUnits(timeStringMap: Map<String, String>) {

        val durations = listOf(oneSecond, twoSeconds, oneMinute, twoMinutes, oneHour, twoHours, oneDay, twoDays, oneDaySevenHours, fourDaysTwentyThreeHours, oneWeekTwoDays, twoWeekTwoDays)
        val desc = listOf("1s: ", "2s: ", "1m: ", "2m: ", "1h: ", "2h: ", "1d: ", "2d: ", "1d7h: ", "4d23h: ", "1w2d: ", "2w2d: ")

        println("----- Dual Largest Time Units:  ${Locale.getDefault().language} -----")
        var i = 0
        for (duration in durations) {
            print(desc[i++])
            var s = ""
            if (duration.inWholeWeeks() > 0) {
                s = if (duration.inWholeWeeks() > 1) "${duration.inWholeWeeks()} ${timeStringMap[weeksKey]}" else "${duration.inWholeWeeks()} ${timeStringMap[weekKey]}"

                val durationMinusWeeks = duration.minus(7.days.times(duration.inWholeWeeks()))
                if (durationMinusWeeks.inWholeDays > 0) {
                    s += if (durationMinusWeeks.inWholeDays > 1) ", ${durationMinusWeeks.inWholeDays} ${timeStringMap[daysKey]}" else ", ${durationMinusWeeks.inWholeDays} ${timeStringMap[dayKey]}"
                }

            } else if (duration.inWholeDays > 0) {
                s = if (duration.inWholeDays > 1)    "${duration.inWholeDays} ${timeStringMap[daysKey]}" else "${duration.inWholeDays} ${timeStringMap[dayKey]}"

                val durationMinusDays = duration.minus(1.days.times(duration.inWholeDays.toInt()))
                if (durationMinusDays.inWholeHours > 0) {
                    s += if (durationMinusDays.inWholeHours > 1) ", ${durationMinusDays.inWholeHours} ${timeStringMap[hoursKey]}" else ", ${durationMinusDays.inWholeHours} ${timeStringMap[hourKey]}"
                }
            } else if (duration.inWholeHours > 0) {
                s = if (duration.inWholeHours > 1)   "${duration.inWholeHours} ${timeStringMap[hoursKey]}" else "${duration.inWholeHours} ${timeStringMap[hourKey]}"

                val durationMinusHours = duration.minus(1.hours.times(duration.inWholeHours.toInt()))
                if (durationMinusHours.inWholeMinutes > 0) {
                    s += if (durationMinusHours.inWholeMinutes > 1) ", ${durationMinusHours.inWholeMinutes} ${timeStringMap[minutesKey]}" else ", ${durationMinusHours.inWholeMinutes} ${timeStringMap[minuteKey]}"
                }

            } else if (duration.inWholeMinutes > 0) {
                s = if (duration.inWholeMinutes > 1) "${duration.inWholeMinutes} ${timeStringMap[minutesKey]}" else "${duration.inWholeMinutes} ${timeStringMap[minuteKey]}"


                val durationMinusMinutes = duration.minus(1.minutes.times(duration.inWholeMinutes.toInt()))
                if (durationMinusMinutes.inWholeSeconds > 0) {
                    s += if (durationMinusMinutes.inWholeSeconds > 1) ", ${durationMinusMinutes.inWholeSeconds} ${timeStringMap[secondsKey]}" else ", ${durationMinusMinutes.inWholeSeconds} ${timeStringMap[secondKey]}"
                }

            } else {
                s = if (duration.inWholeSeconds > 1) "${duration.inWholeSeconds} ${timeStringMap[secondsKey]}" else "${duration.inWholeSeconds} ${timeStringMap[secondKey]}"
            }
            println(s)
        }
    }

    @Test
    fun time_span_strings() {

        var l = Locale.ENGLISH
        Locale.setDefault(l)
        var timeStringsMap = getTimeStringMapForCurrentLocale()
        formatForSingleLargestTimeUnit(timeStringsMap)
        formatForDualLargestTimeUnits(timeStringsMap)

        // Note: For locales from language codes:
        // - Arabic is "ar"
        // - Japanese is "ja"
        // - Urdu is "ur"
        l = Locale.forLanguageTag("ur")
        Locale.setDefault(l)
        timeStringsMap = getTimeStringMapForCurrentLocale()
        formatForSingleLargestTimeUnit(timeStringsMap)
        formatForDualLargestTimeUnits(timeStringsMap)
    }

    private fun getTimeStringMapForCurrentLocale(): Map<String, String> {
        val locale = Locale.getDefault()
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as ApplicationContext

        // Attempt to load the appropriate time strings map based on the language code of our locale, i.e., "en" for English, "fr" for French etc.
        val filename = "csv/time_string_maps/time_strings_dict_" + locale.language + ".json"
        var inputStream: InputStream? = null
        try {
            inputStream = context.assets.open(filename)
        }
        catch (ioe: IOException) {
            Log.e(TAG, "Failed to open time string map file: $filename - attempting to use English!", ioe)
            inputStream = context.assets.open("csv/time_string_maps/time_strings_dict_en.json")
        }
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))
        val jsonString = bufferedReader.use { it.readText() }
        return Json.decodeFromString(jsonString)
    }

}