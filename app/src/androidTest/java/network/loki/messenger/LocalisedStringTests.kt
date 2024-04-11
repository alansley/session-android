package network.loki.messenger

//import org.thoughtcrime.securesms.util.DateUtils

import android.content.Context
import android.icu.text.DisplayContext
import android.icu.text.NumberFormat
import android.icu.text.RelativeDateTimeFormatter
import android.icu.text.RelativeDateTimeFormatter.AbsoluteUnit
import android.icu.text.RelativeDateTimeFormatter.Direction
import android.icu.text.RelativeDateTimeFormatter.RelativeUnit
import android.icu.text.RelativeDateTimeFormatter.RelativeUnit.DAYS
import android.icu.text.RelativeDateTimeFormatter.RelativeUnit.HOURS
import android.icu.text.RelativeDateTimeFormatter.RelativeUnit.MINUTES
import android.icu.text.RelativeDateTimeFormatter.RelativeUnit.SECONDS
import android.icu.text.RelativeDateTimeFormatter.RelativeUnit.WEEKS
import android.icu.util.ULocale
import android.text.format.DateUtils
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.runner.RunWith
import org.ocpsoft.prettytime.PrettyTime
import org.session.libsignal.utilities.Log
import org.thoughtcrime.securesms.ApplicationContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Date
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration


@RunWith(AndroidJUnit4::class)
@SmallTest
class LocalisedStringTests {

    private val TAG = "LocalisedStringTests"

    private fun Duration.to2partString(): String? =
        toComponents { days, hours, minutes, seconds, nanoseconds -> listOf(days.days, hours.hours, minutes.minutes, seconds.seconds) }
            .filter { it.inWholeSeconds > 0L }.take(2).takeIf { it.isNotEmpty() }?.joinToString(" ")

    private fun Duration.aclTo2partString(): String {
        val components = this.toComponents { days, hours, minutes, seconds -> listOf(days.days, hours.hours, minutes.minutes, seconds.seconds) }

        if (inWholeDays > 0) {
            return "At least 1 day so should format as 'in X days, Y hours' or 'X days, Y hours ago'"
        }
        else if (inWholeHours > 0) {
            return "At least 1 hour so should format as 'in X hours Y minutes' or 'X hours Y minutes ago'"
        }
        else if (inWholeMinutes > 0) {
            return "At least 1 hour so should format as 'in X hours Y minutes' or 'X hours Y minutes ago'"
        }

        return "nope"
    }


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

    /*
    fun formatDurationLocalizedRelative(context: Context, duration: Duration, past: Boolean = false): String {
        val now = System.currentTimeMillis().milliseconds
        val targetDateTime = if (past) now.minus(duration) else now.plus(duration)

        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG) // .RELATIVE)
        val formattedDateTime = targetDateTime. format(formatter)

        return formattedDateTime
    }

     */

    fun formatDuration(futureOrPastTimestamp: Duration, ulocale: ULocale, nf: NumberFormat, style: RelativeDateTimeFormatter.Style, capitalisationContext: DisplayContext): String { //, style: RelativeDateTimeFormatter.Style): String {

        val nowDuration = System.currentTimeMillis().milliseconds

        val nowMS = System.currentTimeMillis()

        //val direction = RelativeDateTimeFormatter.Direction.NEXT //if (futureOrPastTimestamp.inWholeMilliseconds > now.inWholeSeconds) RelativeDateTimeFormatter.Direction.NEXT else RelativeDateTimeFormatter.Direction.LAST

        var direction: Direction = if (futureOrPastTimestamp.inWholeMilliseconds > nowMS) Direction.NEXT else Direction.LAST

        var f = RelativeDateTimeFormatter.getInstance(ulocale, nf, RelativeDateTimeFormatter.Style.LONG, DisplayContext.CAPITALIZATION_NONE)
        //f.formatStyle = style


        //var dtf = DateTimeFormatter()
        //f.formatStyle

        if (futureOrPastTimestamp.inWholeWeeks() > 0) return f.format(futureOrPastTimestamp.inWholeDays.toDouble(), direction, WEEKS)
        if (futureOrPastTimestamp.inWholeDays > 7) return f.format(futureOrPastTimestamp.inWholeDays.toDouble(), direction, DAYS)
        else if (futureOrPastTimestamp.inWholeHours > 24) return f.format(futureOrPastTimestamp.inWholeHours.toDouble(), direction, HOURS)
        else if (futureOrPastTimestamp.inWholeMinutes > 60) return f.format(futureOrPastTimestamp.inWholeMinutes.toDouble(), direction, MINUTES)
        else if (futureOrPastTimestamp.inWholeSeconds >= 60) return f.format(futureOrPastTimestamp.inWholeMinutes.toDouble(), direction, MINUTES)
        else // Duration must be < 60 seconds
        {
            return f.format(futureOrPastTimestamp.inWholeSeconds.toDouble(), direction, SECONDS)
        }
        //return "Bad stuff happened"
    }

    fun rdtfTest() {

        val uloc = ULocale.ENGLISH

        val rdtf = RelativeDateTimeFormatter.getInstance(
            uloc,
            null,
            RelativeDateTimeFormatter.Style.LONG,
            DisplayContext.CAPITALIZATION_NONE
        )

        val d = 1.days.plus(7.hours)
        val formattedDuration = rdtf.format(d.inWholeHours.toDouble(),
            RelativeDateTimeFormatter.Direction.NEXT,
            HOURS
        )

        val fmt = RelativeDateTimeFormatter.getInstance()
        fmt.format(1.0, Direction.NEXT, DAYS) // "in 1 day"
        fmt.format(3.0, Direction.NEXT, DAYS) // "in 3 days"
        fmt.format(3.2, Direction.LAST, RelativeUnit.YEARS) // "3.2 years ago"

        fmt.format(Direction.LAST, AbsoluteUnit.SUNDAY) // "last Sunday"
        fmt.format(Direction.THIS, AbsoluteUnit.SUNDAY) // "this Sunday"
        fmt.format(Direction.NEXT, AbsoluteUnit.SUNDAY) // "next Sunday"
        fmt.format(Direction.PLAIN, AbsoluteUnit.SUNDAY) // "Sunday"

        fmt.format(Direction.LAST, AbsoluteUnit.DAY) // "yesterday"
        fmt.format(Direction.THIS, AbsoluteUnit.DAY) // "today"
        fmt.format(Direction.NEXT, AbsoluteUnit.DAY) // "tomorrow"

        fmt.format(Direction.PLAIN, AbsoluteUnit.NOW) // "now"

    }

    fun redo() {

        val noCapitalisation = DisplayContext.CAPITALIZATION_NONE

        val nowMS = System.currentTimeMillis()     // Current timestamp in ms
        val nowDuration = nowMS.milliseconds       // Current timestamp as a Duration

        val timeToAdd = 1.minutes                  // How far in the future or past are we looking at?

        val timeToFormat = nowDuration + timeToAdd // Get that point in time as a Duration

        // Figure out if the point in time is in the past or the future
        var direction: Direction = if (timeToFormat.inWholeMilliseconds > nowMS) Direction.NEXT else Direction.LAST


        val loc = ULocale.ENGLISH
        val nf = NumberFormat.getInstance(loc)
        //val style = FORMAT_ABBREV_RELATIVE
        var rdtf = RelativeDateTimeFormatter.getInstance(loc, nf)

    }

    // OG
    /*
    fun getKotlinRelativeDateTimeFormatterString(futureOrPastTimestamp: Duration, style: RelativeDateTimeFormatter.Style): String {

        val now = System.currentTimeMillis().milliseconds
        val direction = RelativeDateTimeFormatter.Direction.NEXT //if (futureOrPastTimestamp.inWholeMilliseconds > now.inWholeSeconds) RelativeDateTimeFormatter.Direction.NEXT else RelativeDateTimeFormatter.Direction.LAST

        var f = RelativeDateTimeFormatter.getInstance()
        f.formatStyle = style


        //var dtf = DateTimeFormatter()
        //f.formatStyle

        if (futureOrPastTimestamp.inWholeWeeks() > 0) return f.format(futureOrPastTimestamp.inWholeDays.toDouble(), direction, WEEKS)
        if (futureOrPastTimestamp.inWholeDays > 7) return f.format(futureOrPastTimestamp.inWholeDays.toDouble(), direction, DAYS)
        else if (futureOrPastTimestamp.inWholeHours > 24) return f.format(futureOrPastTimestamp.inWholeHours.toDouble(), direction, HOURS)
        else if (futureOrPastTimestamp.inWholeMinutes > 60) return f.format(futureOrPastTimestamp.inWholeMinutes.toDouble(), direction, MINUTES)
        else if (futureOrPastTimestamp.inWholeSeconds > 60) return f.format(futureOrPastTimestamp.inWholeSeconds.toDouble(), direction, SECONDS)

        return "Bad stuff happened"
    }
    */



    fun print_relative_times(localeString: String) {
        val MS_PER_SEC    = 1000L
        val SECS_PER_MIN  = 60L
        val MINS_PER_HOUR = 60L
        val HOURS_PER_DAY = 24L
        val nowMS = System.currentTimeMillis()


        val stringToSubstitute = "Disappears {time}"

        val oneHundredAndFiftyMinutes = 150.minutes
        Log.d(TAG, "150 minutes as a 2-part string is: ${oneHundredAndFiftyMinutes.to2partString()}") // "2h 30m"

        val eightDays = 8.days
        Log.d(TAG, "8 days as a 2-part string is: ${eightDays.to2partString()}") // "8d"

        val sixtySixMinutes = 91.minutes
        val s = getAbbreviatedDurationStringMinutes(sixtySixMinutes)
        Log.d(TAG, "66 minutes string is: $s")


        /*
        val now: Duration       = kotlin.time.Duration(System.currentTimeMillis())

        val _8seconds: Duration = 8.seconds
        val _8mins: Duration    = 8.minutes
        val _150mins: Duration  = 150.minutes
        val _8days: Duration    = 8.days

        val in8secs = now.plus(_8seconds)
        val in8mins = now.plus(_8mins)
        val in150mins = now.plus(_150mins)
        val in8days = now.plus(_8days)

        val _8secsAgo = now.minus(_8seconds)
        val _8minsAgo = now.minus(_8mins)
        val _150minsAgo = now.minus(_150mins)
        val _8daysAgo = now.minus(_8days)

        Log.d(TAG, "FORMAT_ABBREV_RELATIVE (forward)  - in 8 secs    : ${getAbbreviatedDurationMinutes(in8secs)}")
        Log.d(TAG, "FORMAT_ABBREV_RELATIVE (forward)  - in 8 mins    : ${getAbbreviatedDurationMinutes(in8mins)}")
        Log.d(TAG, "FORMAT_ABBREV_RELATIVE (forward)  - in 150 mins  : ${getAbbreviatedDurationMinutes(in150mins)}")
        Log.d(TAG, "FORMAT_ABBREV_RELATIVE (forward)  - in 8 days    : ${getAbbreviatedDurationMinutes(in8days)}")
        Log.d(TAG, "FORMAT_ABBREV_RELATIVE (backward) - 8 secs ago   : ${getAbbreviatedDurationMinutes(_8secsAgo)}")
        Log.d(TAG, "FORMAT_ABBREV_RELATIVE (backward) - 8 mins ago   : ${getAbbreviatedDurationMinutes(_8minsAgo)}")
        Log.d(TAG, "FORMAT_ABBREV_RELATIVE (backward) - 150 mins ago : ${getAbbreviatedDurationMinutes(_150minsAgo)}")
        Log.d(TAG, "FORMAT_ABBREV_RELATIVE (backward) - 8 days ago   : ${getAbbreviatedDurationMinutes(_8daysAgo)}")
        */


        val in8Mins   = nowMS + MS_PER_SEC * SECS_PER_MIN * 8L
        val in150Mins = nowMS + MS_PER_SEC * SECS_PER_MIN * 150L
        val in8Days   = nowMS + MS_PER_SEC * SECS_PER_MIN * MINS_PER_HOUR * HOURS_PER_DAY * 8L

        val back8Mins   = nowMS - (MS_PER_SEC * SECS_PER_MIN * 8L)
        val back150Mins = nowMS - (MS_PER_SEC * SECS_PER_MIN * 150L)
        val back8Days = nowMS - (MS_PER_SEC * SECS_PER_MIN * MINS_PER_HOUR * HOURS_PER_DAY * 8L)

        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as ApplicationContext

        val now = System.currentTimeMillis().milliseconds

        Log.d(TAG, "NoPreposition - 1 minute        : " + getDateUtilsStringWithoutPreposition(context, now.plus(1.minutes)))
        Log.d(TAG, "NoPreposition - 1 hour          : " + getDateUtilsStringWithoutPreposition(context, now.plus(1.hours)))
        Log.d(TAG, "NoPreposition - 1 day           : " + getDateUtilsStringWithoutPreposition(context, now.plus(1.days)))
        Log.d(TAG, "NoPreposition - 1 day 7 hours   : " + getDateUtilsStringWithoutPreposition(context, now.plus(1.days.plus(7.hours))))
        Log.d(TAG, "NoPreposition - 4 days 23 hours : " + getDateUtilsStringWithoutPreposition(context, now.plus(4.days.plus(23.hours))))
        Log.d(TAG, "NoPreposition - 1 week 2 days   : " + getDateUtilsStringWithoutPreposition(context, now.plus(9.days)))

        /*
        Log.d(TAG, "KOTLIN - 1 minute        : " + getKotlinRelativeDateTimeFormatterString(1.minutes))
        Log.d(TAG, "KOTLIN - 1 hour          : " + getKotlinRelativeDateTimeFormatterString(1.hours))
        Log.d(TAG, "KOTLIN - 1 day           : " + getKotlinRelativeDateTimeFormatterString(1.days))
        Log.d(TAG, "KOTLIN - 1 day 7 hours   : " + getKotlinRelativeDateTimeFormatterString(1.days.plus(7.hours)))
        Log.d(TAG, "KOTLIN - 4 days 23 hours : " + getKotlinRelativeDateTimeFormatterString(4.days.plus(23.hours)))
        Log.d(TAG, "KOTLIN - 1 week 2 days   : " + getKotlinRelativeDateTimeFormatterString(9.days))
        */




        // ----- Going forward -----
        Log.d(TAG, "----- Going forward $localeString -----")
        val in8MinsString = DateUtils.getRelativeTimeSpanString(in8Mins, nowMS, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString().lowercase()
        Log.d(TAG, "Minute resolution / FORMAT_ABBREV_RELATIVE -> in8Minutes is: $in8MinsString") // "In 8 min"

        val in8MinsStringAbbrevTime = DateUtils.getRelativeTimeSpanString(in8Mins, nowMS, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_TIME).toString()
        Log.d(TAG, "Minute resolution / FORMAT_ABBREV_TIME -> in8Minutes is: $in8MinsStringAbbrevTime") // "In 8 minutes"

        val in150MinsString = DateUtils.getRelativeTimeSpanString(in150Mins, nowMS, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString()
        Log.d(TAG, "Minute resolution / FORMAT_ABBREV_RELATIVE -> in150Min is: $in150MinsString") // "In 2 hr."

        val in150MinsStringHourInMillis = DateUtils.getRelativeTimeSpanString(in150Mins, nowMS, DateUtils.HOUR_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString()
        Log.d(TAG, "Hour resolution / FORMAT_ABBREV_RELATIVE -> in150Min is: $in150MinsStringHourInMillis") // "In 2 hr."

        val in150MinsStringHoursAbbrevTime = DateUtils.getRelativeTimeSpanString(in150Mins, nowMS, DateUtils.HOUR_IN_MILLIS, DateUtils.FORMAT_ABBREV_TIME).toString()
        Log.d(TAG, "Hour resolution / FORMAT_ABBREV_TIME -> in150Min is: $in150MinsStringHoursAbbrevTime") // "In 2 hours"

        val in8DaysString = DateUtils.getRelativeTimeSpanString(in8Days, nowMS, DateUtils.DAY_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString()
        Log.d(TAG, "Day resolution / FORMAT_ABBREV_RELATIVE -> in8Days is: $in8DaysString") // foo

        val in8DaysString2 = DateUtils.getRelativeTimeSpanString(in8Days, nowMS, DateUtils.DAY_IN_MILLIS, DateUtils.FORMAT_ABBREV_TIME).toString()
        Log.d(TAG, "Day resolution / FORMAT_ABBREV_TIME -> in8Days is: $in8DaysString2") // foo


        // ----- Going backward -----
        Log.d(TAG, "----- Going backward $localeString -----")
        val back8MinsString = DateUtils.getRelativeTimeSpanString(back8Mins, nowMS, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString()
        Log.d(TAG, "Minute resolution / FORMAT_ABBREV_RELATIVE -> backn8Minutes is: $back8MinsString") // "8 min. ago"

        val back8MinsStringAbbrevTime = DateUtils.getRelativeTimeSpanString(back8Mins, nowMS, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_TIME).toString()
        Log.d(TAG, "Minute resolution / FORMAT_ABBREV_TIME -> back8Minutes is: $back8MinsStringAbbrevTime") // "8 minutes ago"

        val back150MinsString = DateUtils.getRelativeTimeSpanString(back150Mins, nowMS, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString()
        Log.d(TAG, "Minute resolution / FORMAT_ABBREV_RELATIVE -> back150Min is: $back150MinsString") // "2 hr. ago"

        val back150MinsStringHourInMillis = DateUtils.getRelativeTimeSpanString(back150Mins, nowMS, DateUtils.HOUR_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString()
        Log.d(TAG, "Hour resolution / FORMAT_ABBREV_RELATIVE -> back150Mins is: $back150MinsStringHourInMillis") // "2 hr. ago"

        var back150MinsStringHoursAbbrevTime = DateUtils.getRelativeTimeSpanString(back150Mins, nowMS, DateUtils.HOUR_IN_MILLIS, DateUtils.FORMAT_ABBREV_TIME).toString()
        Log.d(TAG, "Hour resolution / FORMAT_ABBREV_TIME -> back150Mins is: $back150MinsStringHoursAbbrevTime") // "2 hours ago"

        val back8DaysString = DateUtils.getRelativeTimeSpanString(back8Days, nowMS, DateUtils.DAY_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString()
        Log.d(TAG, "Day resolution / FORMAT_ABBREV_RELATIVE -> back8Days is: $back8DaysString") // foo

        val back8DaysString2 = DateUtils.getRelativeTimeSpanString(back8Days, nowMS, DateUtils.DAY_IN_MILLIS, DateUtils.FORMAT_ABBREV_TIME).toString()
        Log.d(TAG, "Day resolution / FORMAT_ABBREV_TIME -> back8Days is: $back8DaysString2") // foo
    }



    fun print_relative_times2(locale: Locale) {

        Locale.setDefault(locale)

        val MS_PER_SEC    = 1000L
        val SECS_PER_MIN  = 60L
        val MINS_PER_HOUR = 60L
        val HOURS_PER_DAY = 24L
        val nowMS = System.currentTimeMillis()


        val stringToSubstitute = "Disappears {time}"



        val in8Mins   = nowMS + MS_PER_SEC * SECS_PER_MIN * 8L
        val in150Mins = nowMS + MS_PER_SEC * SECS_PER_MIN * 150L
        val in8Days   = nowMS + MS_PER_SEC * SECS_PER_MIN * MINS_PER_HOUR * HOURS_PER_DAY * 8L

        val back8Mins   = nowMS - (MS_PER_SEC * SECS_PER_MIN * 8L)
        val back150Mins = nowMS - (MS_PER_SEC * SECS_PER_MIN * 150L)
        val back8Days = nowMS - (MS_PER_SEC * SECS_PER_MIN * MINS_PER_HOUR * HOURS_PER_DAY * 8L)

        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as ApplicationContext

        val nowTimestampAsDuration = System.currentTimeMillis().milliseconds


        // Ahead timestamps
        val oneMinuteAhead                = 1.minutes.plus(nowMS.milliseconds)
        val oneHourAhead                  = 1.hours.plus(nowMS.milliseconds)
        val oneHourTwentyMinutesAhead     = 80.minutes.plus(nowMS.milliseconds)
        val oneDayAhead                   = 1.days.plus(nowMS.milliseconds)
        val oneDaySevenHoursAhead         = 1.days.plus(7.hours).plus(nowMS.milliseconds)
        val fourDaysTwentyThreeHoursAhead = 4.days.plus(23.hours).plus(nowMS.milliseconds)
        val oneWeekTwoDaysAhead           = 9.days.plus(nowMS.milliseconds)



        // ----- Going forward -----
        Log.d(TAG, "----- Going forward, locale: $locale -----")

        // --- One Minute Ahead ---
        var minRes = DateUtils.MINUTE_IN_MILLIS
        var s = DateUtils.getRelativeTimeSpanString(oneMinuteAhead.inWholeMilliseconds, nowMS, minRes, DateUtils.FORMAT_ABBREV_TIME).toString().lowercase(locale) // "in 1 minute"
        Log.d(TAG, "1 minute - FORMAT_ABBREV_TIME: $s")
        s = DateUtils.getRelativeTimeSpanString(oneMinuteAhead.inWholeMilliseconds, nowMS, minRes, DateUtils.FORMAT_ABBREV_RELATIVE).toString().lowercase(locale) // "in 1 min"
        Log.d(TAG, "1 minute - FORMAT_ABBREV_RELATIVE: $s")

        // --- One Hour Ahead ---
        minRes = DateUtils.HOUR_IN_MILLIS
        s = DateUtils.getRelativeTimeSpanString(oneHourAhead.inWholeMilliseconds, nowMS, minRes, DateUtils.FORMAT_ABBREV_TIME).toString().lowercase(locale) // "in 1 hour"
        Log.d(TAG, "1 hour - FORMAT_ABBREV_TIME: $s")
        s = DateUtils.getRelativeTimeSpanString(oneHourAhead.inWholeMilliseconds, nowMS, minRes, DateUtils.FORMAT_ABBREV_RELATIVE).toString().lowercase(locale) // "in 1 hr."
        Log.d(TAG, "1 hour - FORMAT_ABBREV_RELATIVE: $s")

        // --- One Day Ahead ---
        minRes = DateUtils.DAY_IN_MILLIS
        s = DateUtils.getRelativeTimeSpanString(oneDayAhead.inWholeMilliseconds, nowMS, minRes, DateUtils.FORMAT_ABBREV_TIME).toString().lowercase(locale) // "in 1 hour"
        Log.d(TAG, "1 day - FORMAT_ABBREV_TIME: $s")
        s = DateUtils.getRelativeTimeSpanString(oneDayAhead.inWholeMilliseconds, nowMS, minRes, DateUtils.FORMAT_ABBREV_RELATIVE).toString().lowercase(locale) // "in 1 hr."
        Log.d(TAG, "1 day - FORMAT_ABBREV_RELATIVE: $s")

        // --- One Day Ahead ---
        minRes = DateUtils.HOUR_IN_MILLIS
        s = DateUtils.getRelativeTimeSpanString(oneDaySevenHoursAhead.inWholeMilliseconds, nowMS, minRes, DateUtils.FORMAT_ABBREV_TIME).toString().lowercase(locale) // "in 1 hour"
        Log.d(TAG, "1 day 7 hours - FORMAT_ABBREV_TIME: $s")
        s = DateUtils.getRelativeTimeSpanString(oneDaySevenHoursAhead.inWholeMilliseconds, nowMS, minRes, DateUtils.FORMAT_ABBREV_RELATIVE).toString().lowercase(locale) // "in 1 hr."
        Log.d(TAG, "1 day 7 hours - FORMAT_ABBREV_RELATIVE: $s")



        //s = DateUtils.getRelativeTimeSpanString(c=context, time=oneHourTwentyMinutesAhead.inWholeMilliseconds, minResolution=DateUtils.MINUTE_IN_MILLIS, transitionResolution=DateUtils.MINUTE_IN_MILLIS, flags=DateUtils.FORMAT_ABBREV_RELATIVE).toString().lowercase(locale)
        //Log.d(TAG, "1:20 - DDD: $s")

/*

        val in8MinsStringAbbrevTime = DateUtils.getRelativeTimeSpanString(in8Mins, nowMS, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_TIME).toString().lowercase(locale)
        Log.d(TAG, "Minute resolution / FORMAT_ABBREV_TIME -> in8Minutes is: $in8MinsStringAbbrevTime") // "In 8 minutes"

        val in150MinsString = DateUtils.getRelativeTimeSpanString(in150Mins, nowMS, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString().lowercase(locale)
        Log.d(TAG, "Minute resolution / FORMAT_ABBREV_RELATIVE -> in150Min is: $in150MinsString") // "In 2 hr."

        val in150MinsStringHourInMillis = DateUtils.getRelativeTimeSpanString(in150Mins, nowMS, DateUtils.HOUR_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString().lowercase(locale)
        Log.d(TAG, "Hour resolution / FORMAT_ABBREV_RELATIVE -> in150Min is: $in150MinsStringHourInMillis") // "In 2 hr."

        val in150MinsStringHoursAbbrevTime = DateUtils.getRelativeTimeSpanString(in150Mins, nowMS, DateUtils.HOUR_IN_MILLIS, DateUtils.FORMAT_ABBREV_TIME).toString().lowercase(locale)
        Log.d(TAG, "Hour resolution / FORMAT_ABBREV_TIME -> in150Min is: $in150MinsStringHoursAbbrevTime") // "In 2 hours"

        val in8DaysString = DateUtils.getRelativeTimeSpanString(in8Days, nowMS, DateUtils.DAY_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString().lowercase(locale)
        Log.d(TAG, "Day resolution / FORMAT_ABBREV_RELATIVE -> in8Days is: $in8DaysString") // foo

        val in8DaysString2 = DateUtils.getRelativeTimeSpanString(in8Days, nowMS, DateUtils.DAY_IN_MILLIS, DateUtils.FORMAT_ABBREV_TIME).toString().lowercase(locale)
        Log.d(TAG, "Day resolution / FORMAT_ABBREV_TIME -> in8Days is: $in8DaysString2") // foo


        // ----- Going backward -----
        Log.d(TAG, "----- Going backward $locale -----")
        val back8MinsString = DateUtils.getRelativeTimeSpanString(back8Mins, nowMS, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString().lowercase(locale)
        Log.d(TAG, "Minute resolution / FORMAT_ABBREV_RELATIVE -> backn8Minutes is: $back8MinsString") // "8 min. ago"

        val back8MinsStringAbbrevTime = DateUtils.getRelativeTimeSpanString(back8Mins, nowMS, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_TIME).toString().lowercase(locale)
        Log.d(TAG, "Minute resolution / FORMAT_ABBREV_TIME -> back8Minutes is: $back8MinsStringAbbrevTime") // "8 minutes ago"

        val back150MinsString = DateUtils.getRelativeTimeSpanString(back150Mins, nowMS, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString().lowercase(locale)
        Log.d(TAG, "Minute resolution / FORMAT_ABBREV_RELATIVE -> back150Min is: $back150MinsString") // "2 hr. ago"

        val back150MinsStringHourInMillis = DateUtils.getRelativeTimeSpanString(back150Mins, nowMS, DateUtils.HOUR_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString().lowercase(locale)
        Log.d(TAG, "Hour resolution / FORMAT_ABBREV_RELATIVE -> back150Mins is: $back150MinsStringHourInMillis") // "2 hr. ago"

        var back150MinsStringHoursAbbrevTime = DateUtils.getRelativeTimeSpanString(back150Mins, nowMS, DateUtils.HOUR_IN_MILLIS, DateUtils.FORMAT_ABBREV_TIME).toString().lowercase(locale)
        Log.d(TAG, "Hour resolution / FORMAT_ABBREV_TIME -> back150Mins is: $back150MinsStringHoursAbbrevTime") // "2 hours ago"

        val back8DaysString = DateUtils.getRelativeTimeSpanString(back8Days, nowMS, DateUtils.DAY_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString().lowercase(locale)
        Log.d(TAG, "Day resolution / FORMAT_ABBREV_RELATIVE -> back8Days is: $back8DaysString") // foo

        val back8DaysString2 = DateUtils.getRelativeTimeSpanString(back8Days, nowMS, DateUtils.DAY_IN_MILLIS, DateUtils.FORMAT_ABBREV_TIME).toString().lowercase(locale)
        Log.d(TAG, "Day resolution / FORMAT_ABBREV_TIME -> back8Days is: $back8DaysString2") // foo
        */
    }

    fun doubt() {
        val duration = DateUtils.HOUR_IN_MILLIS * 31

        val weeks = duration / DateUtils.WEEK_IN_MILLIS
        val days = duration / DateUtils.DAY_IN_MILLIS
        val hours = (duration % DateUtils.DAY_IN_MILLIS) / DateUtils.HOUR_IN_MILLIS

        val formattedDuration = DateUtils.formatElapsedTime(duration / 1000)



        println("$formattedDuration ($days day${if (days != 1L) "s" else ""}, $hours hour${if (hours != 1L) "s" else ""})")
    }

    fun doubt2() {
        val locale = Locale.ENGLISH
        Locale.setDefault(locale) // Set the locale or it doesn't take affect in the time formatting
        val nowMS = System.currentTimeMillis()
        val x = 80.seconds.plus(nowMS.milliseconds)

        var combined = ""

        if (x.inWholeMinutes > 0) {
            // Do minutes part
            val minRes = DateUtils.SECOND_IN_MILLIS
            val format = DateUtils.FORMAT_ABBREV_ALL
            val minuteString = DateUtils.getRelativeTimeSpanString(x.inWholeMilliseconds, nowMS, minRes, format).toString().lowercase(locale)

            // Do seconds part
            val minutesDuration = x.inWholeMinutes.toDuration(DurationUnit.MINUTES)
            val secondsOnly = x.minus(minutesDuration).plus(nowMS.milliseconds)
            val secondsString = DateUtils.getRelativeTimeSpanString(secondsOnly.inWholeMilliseconds, nowMS, minRes, format).toString().lowercase(locale)

            //DateUtils

            combined = minuteString + " " + secondsString
        }

        println(combined)
    }


    fun testFormattingDurationListInThePast() {
        val t: PrettyTime = PrettyTime(Date(1000 * 60 * 60 * 24 * 3 + 1000 * 60 * 60 * 15 + 1000 * 60 * 38))
        val durations: MutableList<org.ocpsoft.prettytime.Duration>? = t.calculatePreciseDuration(Date(0))
        //assertEquals("3 days 15 hours 38 minutes ago", t.format(durations))
        println(t.format(durations))
    }

    fun testFormattingDurationListInThePast(durationTimestamp: Duration) {
        val t: PrettyTime = PrettyTime(Date(durationTimestamp.inWholeMilliseconds))
        val durations: MutableList<org.ocpsoft.prettytime.Duration>? = t.calculatePreciseDuration(Date(0))
        //assertEquals("3 days 15 hours 38 minutes ago", t.format(durations))
        println(t.format(durations))
    }

    fun testFormattingDurationListInTheFuture() {

        val days = 3L
        val daysMillis = 1000 * 60 * 60 * 24 * days
        val hours = 15L
        val hoursMillis = 1000 * 60 * 60 * hours
        val minutes = 38L
        val minutesMillis = 1000 * 60 * minutes

        val combinedMillis = daysMillis + hoursMillis + minutesMillis

        val t = PrettyTime(Date(0))
        val durations: MutableList<org.ocpsoft.prettytime.Duration>? = t.calculatePreciseDuration(Date((combinedMillis).toLong()))
        //assertEquals("3 days 15 hours 38 minutes from now", t.format(durations))
        println(t.format(durations))
    }

    fun testFormattingDurationListInThePast2(durationTimestamp: Duration) {
        val t: PrettyTime = PrettyTime(Date(1000 * 60 * 60 * 24 * 3 + 1000 * 60 * 60 * 15 + 1000 * 60 * 38))
        val durations: MutableList<org.ocpsoft.prettytime.Duration>? = t.calculatePreciseDuration(Date(0))
        //assertEquals("3 days 15 hours 38 minutes ago", t.format(durations))
        println(t.format(durations))
    }

    fun formatDurationForLargestUnitOnly(duration: Duration, useShortFormat: Boolean) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as ApplicationContext

        val nowMS = System.currentTimeMillis()

        // Get the approximate duration using the largest unit only. So for example, if a duration is 3 days and 15 hours the
        // formatted date range returned will be '3 days', while if the duration is 4 hours and 16 minutes it will be '4 hours' etc.
        var approxDuration = 0.minutes
        var minResolution = 0L
        if (duration.inWholeWeeks() > 0) {
            println("Choosing weeks as largest unit!")
            approxDuration = duration.inWholeWeeks().days.times(7)
            minResolution = DateUtils.WEEK_IN_MILLIS

        }
        else if (duration.inWholeDays    > 0) {
            println("Choosing days as largest unit!")
            approxDuration = duration.inWholeDays.days
            minResolution = DateUtils.DAY_IN_MILLIS
        }
        else if (duration.inWholeMinutes > 0) {
            println("Choosing minutes as min resolution!")
            approxDuration = duration.inWholeMinutes.minutes
            minResolution = DateUtils.MINUTE_IN_MILLIS

        }
        else {
            approxDuration = duration.inWholeSeconds.seconds
            minResolution = DateUtils.SECOND_IN_MILLIS
            println("Choosing seconds as largest unit!") }


        val relativeDurationMS = duration.inWholeMilliseconds + nowMS

        val approxDurationMS = approxDuration.inWholeMilliseconds
        val relativeApproxDurationMS = approxDuration.inWholeMilliseconds + nowMS




        //val s = DateUtils.formatDateRange(context, nowMS, relativeApproxDuration, DateUtils.LENGTH_SHORTER) // NO! This is for DATES!
        //val s = DateUtils.getRelativeTimeSpanString(context, relativeApproxDurationMS, true)                // NO! This returns things like: "11:20 AM" or "at 11:22 AM"

        //val s = DateUtils.getRelativeTimeSpanString(relativeApproxDurationMS, nowMS, minResolution, DateUtils.FORMAT_ABBREV_ALL) NO! This returns "In 1 minute" or "In 1 min."



        //val s = DateUtils.getRelativeTimeSpanString(context, relativeApproxDurationMS)                  // NO! Returns things like "11:31 AM"
        //val s = DateUtils.getRelativeTimeSpanString(context, relativeApproxDurationMS, false)           // NO! This is the same as the above convenience accessor!
        //val s = DateUtils.getRelativeTimeSpanString(context, relativeApproxDurationMS, true) // NO! Returns things like "at 11:34am"

        // Note: FORMAT_ABREV_TIME will return things like "In 1 minute" while FORMAT_ABBREV_ALL will return things like "In 1 min."
        var format = if (useShortFormat) DateUtils.FORMAT_ABBREV_RELATIVE else DateUtils.FORMAT_ABBREV_TIME


        //val s = DateUtils.getRelativeTimeSpanString(relativeApproxDurationMS, nowMS, DateUtils.MINUTE_IN_MILLIS, format).toString().lowercase(Locale.getDefault())


        val s = DateUtils.getRelativeTimeSpanString(relativeDurationMS, nowMS, minResolution, format)


        println(s)
    }

    fun formatDurationForLargeAndSmallUnits(duration: Duration) {

    }

    fun foo(locale: Locale) {
        Locale.setDefault(locale)
        val nowMS = System.currentTimeMillis()

        // Flat timestamps
        val oneMinute                = 1.minutes
        val oneHour                  = 1.hours
        val oneDay                   = 1.days
        val oneDaySevenHours         = 1.days.plus(7.hours)
        val fourDaysTwentyThreeHours = 4.days.plus(23.hours)
        val oneWeekTwoDays           = 9.days


        formatDurationForLargestUnitOnly(oneMinute, useShortFormat = false)
        formatDurationForLargestUnitOnly(oneDaySevenHours, useShortFormat = true)

    }

    fun formatForSingleLargestTimeUnit(timeStringMap: Map<String, String>) {
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

        val nowMS = System.currentTimeMillis()

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

        val durations = listOf(oneSecond, twoSeconds, oneMinute, twoMinutes, oneHour, twoHours, oneDay, twoDays, oneDaySevenHours, fourDaysTwentyThreeHours, oneWeekTwoDays, twoWeekTwoDays)

        println("----- Single Largest Time Unit: ${Locale.getDefault().language} -----")
        for (duration in durations) {
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

        val nowMS = System.currentTimeMillis()

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

        val durations = listOf(oneSecond, twoSeconds, oneMinute, twoMinutes, oneHour, twoHours, oneDay, twoDays, oneDaySevenHours, fourDaysTwentyThreeHours, oneWeekTwoDays, twoWeekTwoDays)

        println("----- Dual Largest Time Units:  ${Locale.getDefault().language} -----")
        for (duration in durations) {
            var s = ""
            if (duration.inWholeWeeks() > 0) {
                s = if (duration.inWholeWeeks() > 1) "${duration.inWholeWeeks()} ${timeStringMap[weeksKey]}"   else "${duration.inWholeWeeks()} ${timeStringMap[weekKey]}"

                val durationMinusWeeks = duration.minus(7.days.times(duration.inWholeWeeks()))
                if (durationMinusWeeks.inWholeDays > 0) {
                    s += if (durationMinusWeeks.inWholeDays > 1) ", ${durationMinusWeeks.inWholeDays} ${timeStringMap[daysKey]}" else ", ${durationMinusWeeks.inWholeDays} ${timeStringMap[dayKey]}"
                }

            } else if (duration.inWholeDays > 0) {
                s = if (duration.inWholeDays > 1)    "${duration.inWholeDays} ${timeStringMap[daysKey]}"       else "${duration.inWholeDays} ${timeStringMap[dayKey]}"

                val durationMinusDays = duration.minus(1.days.times(duration.inWholeDays.toInt()))
                if (durationMinusDays.inWholeHours > 0) {
                    s += if (durationMinusDays.inWholeHours > 1) ", ${durationMinusDays.inWholeHours} ${timeStringMap[hoursKey]}" else ", ${durationMinusDays.inWholeHours} ${timeStringMap[hourKey]}"
                }
            } else if (duration.inWholeHours > 0) {
                s = if (duration.inWholeHours > 1)   "${duration.inWholeHours} ${timeStringMap[hoursKey]}"     else "${duration.inWholeHours} ${timeStringMap[hourKey]}"

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
        //foo(Locale.ENGLISH)

        var timeStringsMap = getTimeStringMapForCurrentLocale()
        //println(timeStringsMap.toString())
        //for (key in timeStringsMap.keys) { println(key) }
        //println("Language-specific string for 'Weeks' is: " + timeStringsMap["Weeks"])

        formatForSingleLargestTimeUnit(timeStringsMap)
        formatForDualLargestTimeUnits(timeStringsMap)


        Locale.setDefault(Locale.FRENCH)
        timeStringsMap = getTimeStringMapForCurrentLocale()
        formatForSingleLargestTimeUnit(timeStringsMap)
        formatForDualLargestTimeUnits(timeStringsMap)

        //println(timeStringsMap.toString())
        //println("Language-specific string for 'Weeks' is: " + timeStringsMap["Weeks"])

        //val l = Locale.ENGLISH
        //if (l == Locale.ENGLISH) { println("a") } else if (l == Locale.FRENCH) { println("b") } else { println("c") }


        //print_relative_times("ENGLISH")

        //var x = 80.seconds
        //println("80 seconds minutes is: " + x.)

        /*
        val oneHundredAndFiftyMinutes = 150.minutes
        Log.d(TAG, "150 minutes as a 2-part string is: ${oneHundredAndFiftyMinutes.to2partString()}") // "2h 30m"

        val eightDays = 8.days
        Log.d(TAG, "8 days as a 2-part string is: ${eightDays.to2partString()}") // "8d"

        //print_relative_times2(Locale.ENGLISH)
        //doubt()
        doubt2()

        testFormattingDurationListInThePast()

        val oneHour20Mins = 1.hours.plus(20.minutes)
        testFormattingDurationListInThePast(oneHour20Mins)

        testFormattingDurationListInTheFuture()

         */

        /*
        Locale.setDefault(Locale.FRENCH)
        print_relative_times("FRENCH")

        Locale.setDefault(Locale.GERMAN)
        print_relative_times("GERMAN")

        Locale.setDefault(Locale.JAPANESE)
        print_relative_times("JAPANESE")
        */
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