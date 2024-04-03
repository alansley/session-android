package network.loki.messenger

import android.content.Context
import android.icu.text.RelativeDateTimeFormatter
import android.icu.text.RelativeDateTimeFormatter.RelativeUnit.DAYS
import android.icu.text.RelativeDateTimeFormatter.RelativeUnit.HOURS
import android.icu.text.RelativeDateTimeFormatter.RelativeUnit.MINUTES
import android.icu.text.RelativeDateTimeFormatter.RelativeUnit.SECONDS
import android.icu.text.RelativeDateTimeFormatter.RelativeUnit.WEEKS
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import org.session.libsignal.utilities.Log
import org.thoughtcrime.securesms.ApplicationContext
import org.thoughtcrime.securesms.util.DateUtils
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
@SmallTest
class LocalisedStringTests {

    val TAG = "StringTests"

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

    fun Duration.inWholeWeeks(): Long {
      return this.inWholeDays.floorDiv(7)
    }


    fun getKotlinRelativeDateTimeFormatterString(futureOrPastTimestamp: Duration, style: RelativeDateTimeFormatter.Style): String {

        val now = System.currentTimeMillis().milliseconds
        val direction = RelativeDateTimeFormatter.Direction.NEXT //if (futureOrPastTimestamp.inWholeMilliseconds > now.inWholeSeconds) RelativeDateTimeFormatter.Direction.NEXT else RelativeDateTimeFormatter.Direction.LAST

        var f = RelativeDateTimeFormatter.getInstance()
        var dtf = DateTimeFormatter()
        //f.formatStyle

        if (futureOrPastTimestamp.inWholeWeeks() > 0) return f.format(futureOrPastTimestamp.inWholeDays.toDouble(), direction, WEEKS)
        if (futureOrPastTimestamp.inWholeDays > 7) return f.format(futureOrPastTimestamp.inWholeDays.toDouble(), direction, DAYS)
        else if (futureOrPastTimestamp.inWholeHours > 24) return f.format(futureOrPastTimestamp.inWholeHours.toDouble(), direction, HOURS)
        else if (futureOrPastTimestamp.inWholeMinutes > 60) return f.format(futureOrPastTimestamp.inWholeMinutes.toDouble(), direction, MINUTES)
        else if (futureOrPastTimestamp.inWholeSeconds > 60) return f.format(futureOrPastTimestamp.inWholeSeconds.toDouble(), direction, SECONDS)
    }



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

        Log.d(TAG, "KOTLIN - 1 minute        : " + getKotlinRelativeDateTimeFormatterString(1.minutes))
        Log.d(TAG, "KOTLIN - 1 hour          : " + getKotlinRelativeDateTimeFormatterString(1.hours))
        Log.d(TAG, "KOTLIN - 1 day           : " + getKotlinRelativeDateTimeFormatterString(1.days))
        Log.d(TAG, "KOTLIN - 1 day 7 hours   : " + getKotlinRelativeDateTimeFormatterString(1.days.plus(7.hours)))
        Log.d(TAG, "KOTLIN - 4 days 23 hours : " + getKotlinRelativeDateTimeFormatterString(4.days.plus(23.hours)))
        Log.d(TAG, "KOTLIN - 1 week 2 days   : " + getKotlinRelativeDateTimeFormatterString(9.days))





        // ----- Going forward -----
        Log.d(TAG, "----- Going forward $localeString -----")
        val in8MinsString = DateUtils.getRelativeTimeSpanString(in8Mins, nowMS, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString()
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

    @Test
    fun time_span_strings() {
        Locale.setDefault(Locale.ENGLISH)
        print_relative_times("ENGLISH")

        Locale.setDefault(Locale.FRENCH)
        print_relative_times("FRENCH")

        Locale.setDefault(Locale.GERMAN)
        print_relative_times("GERMAN")

        Locale.setDefault(Locale.JAPANESE)
        print_relative_times("JAPANESE")
    }
}