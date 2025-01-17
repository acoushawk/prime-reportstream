package gov.cdc.prime.router

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.size
import assertk.assertions.startsWith
import gov.cdc.prime.router.metadata.ConcatenateMapper
import gov.cdc.prime.router.metadata.ElementAndValue
import gov.cdc.prime.router.metadata.LIVDLookupMapper
import gov.cdc.prime.router.metadata.LookupMapper
import gov.cdc.prime.router.metadata.LookupTable
import gov.cdc.prime.router.metadata.Mapper
import gov.cdc.prime.router.metadata.NullMapper
import gov.cdc.prime.router.metadata.TrimBlanksMapper
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.test.Test
import kotlin.test.assertFails

private const val dateFormat = "yyyy-MM-dd"

internal class ElementTests {

    @Test
    fun `create element`() {
        val elem1 = Element(name = "first", type = Element.Type.NUMBER)
        assertThat(elem1).isNotNull()
    }

    @Test
    fun `compare elements`() {
        val elem1 = Element(name = "first", type = Element.Type.NUMBER)
        val elem2 = Element(name = "first", type = Element.Type.NUMBER)
        assertThat(elem1).isEqualTo(elem2)

        val elem3 = Element(name = "first", type = Element.Type.TEXT)
        assertThat(elem1).isNotEqualTo(elem3)
    }

    @Test
    fun `test schema element`() {
        val elem1 = Element(name = "first", type = Element.Type.NUMBER)
        val elem2 = Element(name = "first", type = Element.Type.NUMBER)
        assertThat(elem1).isEqualTo(elem2)
        val elem3 = Element(name = "first", type = Element.Type.TEXT)
        assertThat(elem1).isNotEqualTo(elem3)
    }

    @Test
    fun `test toNormalize altValues`() {
        val one = Element(
            "b",
            type = Element.Type.CODE,
            altValues = listOf(
                // Use french as an alternative display for these code
                ValueSet.Value("Y", "Oui"),
                ValueSet.Value("N", "Non"),
                ValueSet.Value("UNK", "?")
            ),
            csvFields = Element.csvFields("b", format = "\$alt")
        )
        one.toNormalized("Non", "\$alt").run {
            assertThat(this).isEqualTo("N")
        }
        one.toNormalized("Oui", "\$alt").run {
            assertThat(this).isEqualTo("Y")
        }
    }

    @Test
    fun `test toNormalize date`() {
        val one = Element(
            "a",
            type = Element.Type.DATE,
        )
        // Iso formatted should work
        one.toNormalized("1998-03-30").run {
            assertThat(this).isEqualTo("19980330")
        }
        one.toNormalized("1998-30-03", "yyyy-dd-MM").run {
            assertThat(this).isEqualTo("19980330")
        }
        // Check for dates with trailing spaces
        one.toNormalized("1998-03-30 ").run {
            assertThat(this).isEqualTo("19980330")
        }
        // Check for dates with leading spaces
        one.toNormalized(" 1998-03-30").run {
            assertThat(this).isEqualTo("19980330")
        }
        // Check for dates with leading and trailing spaces
        one.toNormalized(" 1998-03-30 ").run {
            assertThat(this).isEqualTo("19980330")
        }
        // check edge cases
        one.toNormalized("").run {
            assertThat(this).isEqualTo("")
        }
        one.toNormalized("     ").run {
            assertThat(this).isEqualTo("")
        }
    }

    @Test
    fun `test toNormalize dateTime`() {
        val one = Element(
            "a",
            type = Element.Type.DATETIME,
        )
        // Iso formatted should work
        one.toNormalized("1998-03-30T12:00Z").run {
            assertThat(this).isEqualTo("199803301200+0000")
        }
        one.toNormalized("199803300000+0000").run {
            assertThat(this).isEqualTo("199803300000+0000")
        }
        one.toNormalized("20210908105903").run {
            assertThat(this).startsWith("202109081059")
        }
        val two = Element(
            "a",
            type = Element.Type.DATETIME,
        )
        val o = ZoneOffset.UTC.rules.getOffset(Instant.now()).toString()
        val offset = if (o == "Z") {
            "+0000"
        } else {
            o.replace(":", "")
        }
        two.toNormalized("19980330", "yyyyMMdd").run {
            assertThat(this).isEqualTo("199803300000$offset")
        }
        val three = Element(
            "a",
            type = Element.Type.DATETIME,
        )
        three.toNormalized("2020-12-09", dateFormat).run {
            assertThat(this).isEqualTo("202012090000$offset")
        }
        mapOf(
            "20210908105903" to "20210908105903",
            "199803300000+0000" to "19980330000000"
        ).forEach {
            val optionalDateTime = "[yyyyMMddHHmmssZ][yyyyMMddHHmmZ][yyyyMMddHHmmss]"
            val df = DateTimeFormatter.ofPattern(optionalDateTime)
            val ta = df.parseBest(it.key, OffsetDateTime::from, LocalDateTime::from, Instant::from)
            val dt = LocalDateTime.from(ta)
            assertThat(df.format(dt)).isEqualTo(it.value)
        }
        // edge cases
        one.toNormalized("1998-03-30T12:00Z ").run {
            assertThat(this).isEqualTo("199803301200+0000")
        }
        one.toNormalized(" 1998-03-30T12:00Z").run {
            assertThat(this).isEqualTo("199803301200+0000")
        }
        one.toNormalized(" 1998-03-30T12:00Z ").run {
            assertThat(this).isEqualTo("199803301200+0000")
        }
        one.toNormalized("").run {
            assertThat(this).isEqualTo("")
        }
        one.toNormalized("     ").run {
            assertThat(this).isEqualTo("")
        }

        // Hours with single digits and afternoon times
        var testTimes = listOf(
            "3/30/1998 9:35", "3/30/1998 09:35", "1998/3/30 9:35", "1998/3/30 09:35", "19980330 9:35:00",
            "19980330 09:35:00", "1998-03-30 9:35:00", "1998-03-30 09:35:00"
        )
        testTimes.forEach {
            assertThat(one.toNormalized(it)).isEqualTo("199803300935-0600")
        }
        testTimes = listOf("11/30/1998 16:35", "1998/11/30 16:35", "19981130 16:35:00", "1998-11-30 16:35:00")
        testTimes.forEach {
            assertThat(one.toNormalized(it)).isEqualTo("199811301635-0600")
        }
    }

    @Test
    fun `test toFormatted dateTime`() {
        val one = Element(
            "a",
            type = Element.Type.DATETIME,
        )
        // Iso formatted should work
        one.toFormatted("199803301200").run {
            assertThat(this).isEqualTo("199803301200")
        }
        one.toFormatted("199803300000").run {
            assertThat(this).isEqualTo("199803300000")
        }

        val two = Element(
            "a",
            type = Element.Type.DATETIME,
        )
        // Other formats should work too, including sans the time.
        two.toFormatted("202012090000+0000", dateFormat).run {
            assertThat(this).isEqualTo("2020-12-09")
        }
    }

    @Test
    fun `test toFormatted date`() {
        val two = Element(
            "a",
            type = Element.Type.DATE,
        )
        // Check correct formats should work too, including sans the time.
        two.toFormatted("20201201", dateFormat).run {
            assertThat(this).isEqualTo("2020-12-01")
        }
        // Check another correct format and expect to return only date.
        two.toFormatted("20201202", "M/d/yyyy HH:nn").run {
            assertThat(this).isEqualTo("12/2/2020 00:00")
        }
        // Given datetime format and expect to return only date.
        two.toFormatted("20201203", "$dateFormat HH:mm:ss").run {
            assertThat(this).isEqualTo("2020-12-03 00:00:00")
        }
    }

    @Test
    fun `test getDate output format`() {
        val two = Element(
            "a",
            type = Element.Type.DATE,
        )

        // Check LocalDate output formate
        val localDate = LocalDate.parse("2020-12-01")
        two.getDate(localDate, dateFormat).run {
            assertThat(this).isEqualTo("2020-12-01")
        }

        // Check LocalDateTime output format.
        val localDateTime = LocalDateTime.parse("2018-12-12T13:30:30")
        two.getDate(localDateTime, "M/d/yyyy HH:nn").run {
            assertThat(this).isEqualTo("12/12/2018 13:00")
        }

        // Check OffsetDateTime output format.
        val offsetDateTime = OffsetDateTime.parse("2018-12-12T13:30:30+05:00")
        two.getDate(offsetDateTime, "$dateFormat HH:mm:ss").run {
            assertThat(this).isEqualTo("2018-12-12 13:30:30")
        }

        // Check OffsetDateTime output format.
        val instant = Instant.parse("2020-12-03T13:30:30.000Z")
        two.getDate(instant, dateFormat).run {
            assertThat(this).isEqualTo("2020-12-03T13:30:30Z")
        }

        // now let's check some other date formats
        val parser = DateTimeFormatter.ofPattern(
            "[yyyy-MM-dd'T'HH:mm:ssZ]" +
                "[yyyy-MM-dd'T'HH:mm:ssxxx]" +
                "[yyyy-MM-dd'T'HH:mm:ssx]"
        )
        // Check OffsetDateTime output format.
        listOf(
            "2018-12-12T13:30:30+00:00",
            "2018-12-12T13:30:30+00",
            "2018-12-12T13:30:30+0000",
        ).forEach { date ->
            val odt = parser.parseBest(date, OffsetDateTime::from, Instant::from)
            two.getDate(odt, "$dateFormat HH:mm:ss").run {
                assertThat(this).isEqualTo("2018-12-12 13:30:30")
            }
            two.getDate(odt, "$dateFormat HH:mm:ssZZZ").run {
                assertThat(this).isEqualTo("2018-12-12 13:30:30+0000")
            }
            // now check converting the date time to the negative offset
            two.getDate(odt, "$dateFormat HH:mm:ssZZZ", true).run {
                assertThat(this).isEqualTo("2018-12-12 13:30:30-0000")
            }
        }
    }

    @Test
    fun `test convert positive zero offset to negative offset`() {
        Element("a", type = Element.Type.DATE).run {
            // all happy path tests
            Element.convertPositiveOffsetToNegativeOffset("2022-01-05 08:00:00").run {
                assertThat(this).isEqualTo("2022-01-05 08:00:00")
            }
            Element.convertPositiveOffsetToNegativeOffset("2022-01-05 08:00:00+0000").run {
                assertThat(this).isEqualTo("2022-01-05 08:00:00-0000")
            }
            Element.convertPositiveOffsetToNegativeOffset("2022-01-05 08:00:00-0000").run {
                assertThat(this).isEqualTo("2022-01-05 08:00:00-0000")
            }
            Element.convertPositiveOffsetToNegativeOffset("2022-01-05 08:00:00+00").run {
                assertThat(this).isEqualTo("2022-01-05 08:00:00-00")
            }
            Element.convertPositiveOffsetToNegativeOffset("2022-01-05 08:00:00+00:00").run {
                assertThat(this).isEqualTo("2022-01-05 08:00:00-00:00")
            }
            // non-zero offsets
            Element.convertPositiveOffsetToNegativeOffset("2022-01-05 08:00:00-0400").run {
                assertThat(this).isEqualTo("2022-01-05 08:00:00-0400")
            }
            Element.convertPositiveOffsetToNegativeOffset("2022-01-05 08:00:00+12").run {
                assertThat(this).isEqualTo("2022-01-05 08:00:00+12")
            }
            Element.convertPositiveOffsetToNegativeOffset("2022-01-05 08:00:00+03:30").run {
                assertThat(this).isEqualTo("2022-01-05 08:00:00+03:30")
            }
            // some unhappy paths. Don't use these formats.
            Element.convertPositiveOffsetToNegativeOffset("2022-01-05 08:00:00+").run {
                assertThat(this).isEqualTo("2022-01-05 08:00:00+")
            }
            Element.convertPositiveOffsetToNegativeOffset("2022+01+05 08:00:00").run {
                assertThat(this).isEqualTo("2022+01+05 08:00:00")
            }
            Element.convertPositiveOffsetToNegativeOffset("").run {
                assertThat(this).isEqualTo("")
            }
            Element.convertPositiveOffsetToNegativeOffset("     ").run {
                assertThat(this).isEqualTo("     ")
            }
            Element.convertPositiveOffsetToNegativeOffset("+0000").run {
                assertThat(this).isEqualTo("+0000")
            }
            Element.convertPositiveOffsetToNegativeOffset("+0000 2022-01-05").run {
                assertThat(this).isEqualTo("+0000 2022-01-05")
            }
        }
    }

    @Test
    fun `test toNormalized zip`() {
        val one = Element(
            "a",
            type = Element.Type.POSTAL_CODE,
            csvFields = Element.csvFields("zip")
        )
        one.toNormalized("99999").run {
            assertThat(this).isEqualTo("99999")
        }
        one.toNormalized("99999-9999").run {
            assertThat(this).isEqualTo("99999-9999")
        }
        // format should not affect normalization
        one.toNormalized("999999999", "\$zipFive").run {
            assertThat(this).isEqualTo("999999999")
        }
        one.toNormalized("KY1-6666").run {
            assertThat(this).isEqualTo("KY1-6666")
        } // Cayman zipcode
        one.toNormalized("KX33-77777").run {
            assertThat(this).isEqualTo("KX33-77777")
        } // Letters and numbers, but a made up zipcode
        assertFails {
            one.toNormalized("%%%%XXXX") // Unreasonable
        }
    }

    @Test
    fun `test toNormalized phone`() {
        val one = Element(
            "a",
            type = Element.Type.TELEPHONE,
            csvFields = Element.csvFields("phone")
        )
        one.toNormalized("5559938322").run {
            assertThat(this).isEqualTo("5559938322:1:")
        }
        one.toNormalized("1(555)-968-5052").run {
            assertThat(this).isEqualTo("5559685052:1:")
        }
        one.toNormalized("555-968-5052").run {
            assertThat(this).isEqualTo("5559685052:1:")
        }
        one.toNormalized("+1(555)-968-5052").run {
            assertThat(this).isEqualTo("5559685052:1:")
        }
        one.toNormalized("968-5052").run {
            assertThat(this).isEqualTo("0009685052:1:")
        }
        one.toNormalized("+1(555)-968-5052 x5555").run {
            assertThat(this).isEqualTo("5559685052:1:5555")
        }
        // MX phone number
        one.toNormalized("+52-65-8888-8888").run {
            assertThat(this).isEqualTo("6588888888:52:")
        }
    }

    @Test
    fun `test toNormalized dateTime`() {
        val one = Element(
            "a",
            type = Element.Type.DATETIME,
            csvFields = Element.csvFields("datetime")
        )

        // Test MMddyyyy, 12012021 format
        one.toNormalized("12012021").run {
            assertThat(this).isEqualTo("202112010000-0600")
        }
        // Test M/d/yyyy,12/2/2021 format
        one.toNormalized("12/2/2021").run {
            assertThat(this).isEqualTo("202112020000-0600")
        }
        // Test yyyy/M/d,2021/12/3
        one.toNormalized("2021/12/3").run {
            assertThat(this).isEqualTo("202112030000-0600")
        }
        // Test M/d/yyyy HH:mm,12/4/2021 09:00
        one.toNormalized("12/4/2021 09:00").run {
            assertThat(this).isEqualTo("202112040900-0600")
        }
        // Test yyyy/M/d HH:mm,2021/12/05 10:00
        one.toNormalized("2021/12/05 10:00").run {
            assertThat(this).isEqualTo("202112051000-0600")
        }
    }

    @Test
    fun `test failed toNormalized dateTime`() {
        val one = Element(
            "a",
            type = Element.Type.DATETIME,
            csvFields = Element.csvFields("datetime")
        )

        // Test wrong date = 50
        assertThat { one.toNormalized("12502021") }.isFailure()
        // Test wrong month = 13
        assertThat { one.toNormalized("13/2/2021") }.isFailure()
        // Test wrong year = abcd
        assertThat { one.toNormalized("abcd/12/3") }.isFailure()
    }

    @Test
    fun `test toFormatted phone`() {
        val one = Element(
            "a",
            type = Element.Type.TELEPHONE,
            csvFields = Element.csvFields("phone")
        )
        one.toFormatted("5559938322:1:").run {
            assertThat(this).isEqualTo("5559938322")
        }
        one.toFormatted("5559938322:1:", "\$country-\$area-\$exchange-\$subscriber").run {
            assertThat(this).isEqualTo("1-555-993-8322")
        }
        one.toFormatted("5559938322:1:", "(\$area)\$exchange-\$subscriber").run {
            assertThat(this).isEqualTo("(555)993-8322")
        }
    }

    @Test
    fun `test toFormatted zip`() {
        val one = Element(
            "a",
            type = Element.Type.POSTAL_CODE,
            csvFields = Element.csvFields("zip")
        )
        one.toFormatted("99999").run {
            assertThat(this).isEqualTo("99999")
        }
        one.toFormatted("99999-9999").run {
            assertThat(this).isEqualTo("99999-9999")
        }
        one.toFormatted("99999-9999", "\$zipFivePlusFour").run {
            assertThat(this).isEqualTo("99999-9999")
        }
        one.toFormatted("99999-9999", "\$zipFive").run {
            assertThat(this).isEqualTo("99999")
        }
        one.toFormatted("999999999", "\$zipFive").run {
            assertThat(this).isEqualTo("99999")
        }
        one.toFormatted("999999999", "\$zipFivePlusFour").run {
            assertThat(this).isEqualTo("99999-9999")
        }
        one.toFormatted("99999", "\$zipFivePlusFour").run {
            assertThat(this).isEqualTo("99999")
        }
        one.toFormatted("KY1-5555", "\$zipFivePlusFour").run {
            assertThat(this).isEqualTo("KY1-5555")
        }
        one.toFormatted("XZ5555", "\$zipFivePlusFour").run {
            assertThat(this).isEqualTo("XZ5555")
        }
        // check for padding
        one.toFormatted("9999").run {
            assertThat(this).isEqualTo("09999")
        }
        // check for padding again
        one.toFormatted("999").run {
            assertThat(this).isEqualTo("00999")
        }
        // check for padding one last time
        one.toFormatted("9").run {
            assertThat(this).isEqualTo("00009")
        }
        // check for padding with alt format
        one.toFormatted("9999", "\$zipFive").run {
            assertThat(this).isEqualTo("09999")
        }
        // check for padding again
        one.toFormatted("999", "\$zipFive").run {
            assertThat(this).isEqualTo("00999")
        }
        // check for padding one last time
        one.toFormatted("9", "\$zipFive").run {
            assertThat(this).isEqualTo("00009")
        }
    }

    @Test
    fun `test inherit from`() {
        val parent = Element(
            "parent",
            type = Element.Type.TEXT,
            csvFields = Element.csvFields("sampleField"),
            documentation = "I am the parent element"
        )
        // instead of using var, just call run and transmogrify the child then and there
        val child = Element("child").run { inheritFrom(parent) }
        assertThat(
            parent.documentation,
            name = "Documentation value didn't carry over from parent"
        ).isEqualTo(child.documentation)
        assertThat(
            parent.type,
            name = "Element type didn't carry over from parent"
        ).isEqualTo(child.type)
    }

    @Test
    fun `test checkForError DATE`() {
        val checkForErrorDateElementNullify = Element(
            "a",
            type = Element.Type.DATE,
            csvFields = Element.csvFields("date"),
            nullifyValue = true
        )

        // nullify an invalid date if nullifyValue is true
        assertThat(
            checkForErrorDateElementNullify.checkForError("a week ago")
        ).isEqualTo(
            null
        )

        val checkForErrorDateElement = Element(
            "a",
            type = Element.Type.DATE,
            csvFields = Element.csvFields("date")
        )

        // passing through a valid date of known manual formats does not throw an error
        val dateStrings = arrayOf("12/20/2020", "12202020", "2020/12/20", "12/20/2020 12:15", "2020/12/20 12:15")
        dateStrings.forEach { dateString ->
            assertThat(
                checkForErrorDateElement.checkForError(dateString)
            ).isEqualTo(
                null
            )
        }

        // return an InvalidDateMessage
        val actual = checkForErrorDateElement.checkForError("a week ago", null)
        assertThat(actual is InvalidDateMessage).isTrue()
    }

    @Test
    fun `test checkForError dateTime`() {
        val checkForErrorDateTimeElementNullify = Element(
            "a",
            type = Element.Type.DATETIME,
            csvFields = Element.csvFields("datetime"),
            nullifyValue = true
        )

        // nullify an invalid date if nullifyValue is true
        assertThat(checkForErrorDateTimeElementNullify.checkForError("a week ago")).isNull()

        val checkForErrorDateTimeElement = Element(
            "a",
            type = Element.Type.DATETIME,
            csvFields = Element.csvFields("datetime")
        )

        // passing through a valid date of known manual formats does not throw an error
        val dateTimeStrings = arrayOf(
            "12012021", "12/2/2021", "2021/12/3", "12/4/2021 09:00", "2021/12/05 10:00",
            "3/30/1998 9:35", "1998/3/30 9:35", "2022-01-30 9:35:09", "20220130 9:35:09", "20220130 09:35:09"
        )

        dateTimeStrings.forEach { dateTimeString ->
            assertThat(checkForErrorDateTimeElement.checkForError(dateTimeString)).isNull()
        }

        // return an InvalidDateMessage
        val actual = checkForErrorDateTimeElement.checkForError("a week ago", null)
        assertThat(actual is InvalidDateMessage).isTrue()
    }

    @Test
    fun `test failed checkForError dateTime`() {
        val checkForErrorDateTimeElement = Element(
            "a",
            type = Element.Type.DATETIME,
            csvFields = Element.csvFields("datetime")
        )

        // passing through a valid date of known manual formats does not throw an error
        val dateTimeStrings = arrayOf(
            "12502021", // Wrong date = 50
            "13/01/2021", // Wrong month = 13
            "abcd/12/3" // Wrong year = abcd
        )

        dateTimeStrings.forEach { dateTimeString ->
            assertThat(checkForErrorDateTimeElement.checkForError(dateTimeString) is InvalidDateMessage).isTrue()
        }
    }

    @Test
    fun `test normalize and formatted round-trips`() {
        val postal = Element(
            "a",
            type = Element.Type.POSTAL_CODE,
            csvFields = Element.csvFields("zip")
        )
        assertThat(
            postal.toNormalized("94040")
        ).isEqualTo(
            postal.toFormatted("94040")
        )
        assertThat(
            postal.toNormalized("94040-6000")
        ).isEqualTo(
            postal.toFormatted("94040-6000")
        )
        assertThat(
            postal.toFormatted(
                postal.toNormalized("94040-3600", Element.zipFiveToken), Element.zipFiveToken
            )
        ).isEqualTo(
            "94040"
        )
        assertThat(
            postal.toFormatted(
                postal.toNormalized("94040-3600", Element.zipFivePlusFourToken),
                Element.zipFivePlusFourToken
            )
        ).isEqualTo(
            "94040-3600"
        )

        val telephone = Element(
            "a",
            type = Element.Type.TELEPHONE,
            csvFields = Element.csvFields("phone")
        )
        assertThat(
            telephone.toFormatted(telephone.toNormalized("6509999999"))
        ).isEqualTo(
            "6509999999"
        )
        assertThat(
            telephone.toFormatted(telephone.toNormalized("+16509999999"))
        ).isEqualTo(
            "6509999999"
        )

        val date = Element(
            "a",
            type = Element.Type.DATE,
            csvFields = Element.csvFields("date")
        )
        assertThat(
            date.toFormatted(date.toNormalized("20201220"))
        ).isEqualTo(
            "20201220"
        )
        assertThat(
            date.toFormatted(date.toNormalized("2020-12-20"))
        ).isEqualTo(
            "20201220"
        )

        // normalize manually entered date use cases
        // "M/d/yyyy", "MMddyyyy", "yyyy/M/d", "M/d/yyyy HH:mm", "yyyy/M/d HH:mm"
        val dateStrings = arrayOf("12/20/2020", "12202020", "2020/12/20", "12/20/2020 12:15", "2020/12/20 12:15")
        dateStrings.forEach { dateString ->
            assertThat(
                date.toFormatted(date.toNormalized(dateString))
            ).isEqualTo(
                "20201220"
            )
        }

        val nullifyDateElement = Element(
            "a",
            type = Element.Type.DATE,
            csvFields = Element.csvFields("date"),
            nullifyValue = true
        )

        // normalize and nullify an invalid date
        assertThat(
            nullifyDateElement.toFormatted(nullifyDateElement.toNormalized("a week ago"))
        ).isEqualTo(
            ""
        )

        val nullifyDateTimeElement = Element(
            "a",
            type = Element.Type.DATETIME,
            csvFields = Element.csvFields("datetime"),
            nullifyValue = true
        )

        // normalize and nullify an invalid datetime
        assertThat(
            nullifyDateTimeElement.toFormatted(nullifyDateTimeElement.toNormalized("a week ago"))
        ).isEqualTo(
            ""
        )

        val datetime = Element(
            "a",
            type = Element.Type.DATETIME,
            csvFields = Element.csvFields("datetime")
        )
        assertThat(
            datetime.toFormatted(datetime.toNormalized("202012200000+0000"))
        ).isEqualTo(
            "202012200000+0000"
        )
        assertThat(
            datetime.toFormatted(datetime.toNormalized("2020-12-20T00:00Z"))
        ).isEqualTo(
            "202012200000+0000"
        )

        val hd = Element(
            "a",
            type = Element.Type.HD,
            csvFields = Element.csvFields("hd")
        )
        assertThat(
            hd.toFormatted(hd.toNormalized("HDName"))
        ).isEqualTo(
            "HDName"
        )
        assertThat(postal.toFormatted(hd.toNormalized("HDName^0.0.0.0.0.1^ISO"))).isEqualTo(
            "HDName^0.0.0.0.0.1^ISO"
        )

        val ei = Element(
            "a",
            type = Element.Type.EI,
            csvFields = Element.csvFields("ei")
        )
        assertThat(ei.toFormatted(ei.toNormalized("EIName")))
            .isEqualTo(
                "EIName"
            )
        assertThat(
            postal.toFormatted(
                ei.toNormalized("EIName^EINamespace^0.0.0.0.0.1^ISO")
            )
        )
            .isEqualTo(
                "EIName^EINamespace^0.0.0.0.0.1^ISO"
            )
    }

    @Test
    fun `test normalize with multiple csvFields`() {
        val sendingApp = Element(
            "a",
            type = Element.Type.HD,
            csvFields = listOf(
                Element.CsvField("sending_app", format = "\$name"),
                Element.CsvField("sending_oid", format = "\$universalId")
            )
        )
        val normalized = sendingApp.toNormalized(
            listOf(
                Element.SubValue("sending_app", "happy", "\$name"),
                Element.SubValue("sending_oid", "0.0.0.011", "\$universalId")
            )
        )
        assertThat("happy^0.0.0.011^ISO").isEqualTo(normalized)

        val sendingAppName = sendingApp.toFormatted(normalized, Element.hdNameToken)
        assertThat(sendingAppName).isEqualTo("happy")
        val sendingOid = sendingApp.toFormatted(normalized, Element.hdUniversalIdToken)
        assertThat(sendingOid).isEqualTo("0.0.0.011")
    }

    @Test
    fun `test code format field`() {
        val values = ValueSet(
            "test",
            system = ValueSet.SetSystem.LOCAL,
            values = listOf(
                ValueSet.Value(code = "Y", display = "Yes"),
                ValueSet.Value(code = "N", display = "No"),
                ValueSet.Value(code = "U", display = "Unk")
            )
        )
        val one = Element(
            "a",
            valueSet = "test",
            valueSetRef = values,
            type = Element.Type.CODE,
            altValues = listOf(
                ValueSet.Value(code = "Y", display = "Yes"),
                ValueSet.Value(code = "N", display = "No"),
                ValueSet.Value(code = "U", display = "Unknown")
            )
        )
        // because something is a
        assertThat(one.toFormatted("Y", "\$code")).isEqualTo("Y")
        assertThat(one.toNormalized("Y", "\$code")).isEqualTo("Y")
        assertThat(one.checkForError("Y", "\$code")).isNull()
    }

    @Test
    fun `test truncate`() {
        Element(
            name = "uno",
            type = Element.Type.TEXT,
            maxLength = 2,
        ).run {
            assertThat(this.truncateIfNeeded("abcde")).isEqualTo("ab")
        }
        Element(
            name = "dos",
            type = Element.Type.ID_CLIA, // this type is never truncated.
            maxLength = 2,
        ).run {
            assertThat(this.truncateIfNeeded("abcde")).isEqualTo("abcde")
        }
        Element(
            name = "tres",
            type = Element.Type.TEXT,
            maxLength = 20, // max > actual strlen, nothing to truncate
        ).run {
            assertThat(this.truncateIfNeeded("abcde")).isEqualTo("abcde")
        }
        Element( // zilch is an ok valuer = Element(  // maxLength is null, don't truncate.
            name = "cuatro",
            type = Element.Type.TEXT,
        ).run {
            assertThat(this.truncateIfNeeded("abcde")).isEqualTo("abcde")
        }
        Element(
            name = "cinco",
            type = Element.Type.TEXT,
            maxLength = 0, // zilch is an ok value
        ).run {
            assertThat(this.truncateIfNeeded("abcde")).isEqualTo("")
        }
    }

    @Test
    fun `test toFormatted values for UNK`() {
        val values = ValueSet(
            "hl70136",
            system = ValueSet.SetSystem.HL7,
            values = listOf(
                ValueSet.Value(code = "Y", display = "Yes"),
                ValueSet.Value(code = "N", display = "No"),
                ValueSet.Value(code = "UNK", display = "Unk")
            )
        )
        val element = Element(
            "a",
            valueSet = "test",
            valueSetRef = values,
            type = Element.Type.CODE,
            altValues = listOf(
                ValueSet.Value(code = "Y", display = "Yes"),
                ValueSet.Value(code = "N", display = "No"),
                ValueSet.Value(code = "UNK", display = "Unknown")
            )
        )
        element.toFormatted("UNK", "\$system").run {
            assertThat(this).isEqualTo("NULLFL")
        }
    }

    @Test
    fun `test processing of raw data`() {
        val elements = listOf(
            Element("a", Element.Type.TEXT),
            Element("b", Element.Type.TEXT, default = "someDefault"),
            Element(
                "c", Element.Type.TEXT, mapper = "concat(a,e)", mapperRef = ConcatenateMapper(),
                mapperArgs = listOf("a", "e"), default = "someDefault"
            ),
            Element(
                "d", Element.Type.TEXT, mapper = "concat(a,e)", mapperRef = ConcatenateMapper(),
                mapperArgs = listOf("a", "e"), default = "someDefault"
            ),
            Element(
                "e", Element.Type.TEXT, mapper = "concat(a,e)", mapperRef = ConcatenateMapper(),
                mapperArgs = listOf("a", "e"), mapperOverridesValue = true, default = "someDefault"
            ),
            Element(
                "f", Element.Type.TEXT, mapper = "concat(a,e,\$index)", mapperRef = ConcatenateMapper(),
                mapperArgs = listOf("a", "e", "\$index"), mapperOverridesValue = true, default = "someDefault",
                delimiter = "-"
            ),
            Element(
                "g", Element.Type.TEXT, mapper = "concat(a,e,\$currentDate)", mapperRef = ConcatenateMapper(),
                mapperArgs = listOf("a", "e", "\$currentDate"), mapperOverridesValue = true, default = "someDefault",
                delimiter = "-"
            ),
            Element("h", Element.Type.TEXT, default = "someDefault", defaultOverridesValue = false), // 7
            Element("i", Element.Type.TEXT, default = "someDefault", defaultOverridesValue = true), // 8
            Element("j", Element.Type.TEXT, defaultOverridesValue = true), // 9   (null default)
        )
        val schema = Schema("one", "covid-19", elements)
        val currentDate = LocalDate.now().format(Element.dateFormatter)
        val mappedValues = mutableMapOf(
            elements[0].name to "TEST",
            elements[1].name to "",
            elements[2].name to "",
            elements[3].name to "TEST3",
            elements[4].name to "TEST4",
            elements[5].name to "TEST-TEST4-1",
            elements[6].name to "TEST-TEST4-$currentDate",
            elements[7].name to "value",
            elements[8].name to "value",
            elements[9].name to "value",
        )

        // Element has value and mapperAlwaysRun is false, so we get the raw value
        var finalValue = elements[0].processValue(mappedValues, schema, itemIndex = 1)
        assertThat(finalValue.value).isEqualTo(mappedValues[elements[0].name])

        // Element with no raw value, no mapper and default returns a default.
        finalValue = elements[1].processValue(mappedValues, schema, itemIndex = 1)
        assertThat(finalValue.value).isEqualTo(elements[1].default)

        // Element with mapper and no raw value returns mapper value
        finalValue = elements[2].processValue(mappedValues, schema, itemIndex = 1)
        assertThat(finalValue.value).isEqualTo("${mappedValues[elements[0].name]}, ${mappedValues[elements[4].name]}")

        // Element with raw value and mapperAlwaysRun to false returns raw value
        finalValue = elements[3].processValue(mappedValues, schema, itemIndex = 1)
        assertThat(finalValue.value).isEqualTo(mappedValues[elements[3].name])

        // Element with raw value and mapperAlwaysRun to true returns mapper value
        finalValue = elements[4].processValue(mappedValues, schema, itemIndex = 1)
        assertThat(finalValue.value).isEqualTo("${mappedValues[elements[0].name]}, ${mappedValues[elements[4].name]}")

        // Element with $index
        finalValue = elements[5].processValue(mappedValues, schema, emptyMap(), 1)
        assertThat(finalValue.value).isEqualTo("${mappedValues[elements[5].name]}")

        // Element with $currentDate
        finalValue = elements[6].processValue(mappedValues, schema, itemIndex = 1)
        assertThat(finalValue.value).isEqualTo("${mappedValues[elements[6].name]}")

        // Default does not override
        finalValue = elements[7].processValue(mappedValues, schema, itemIndex = 1)
        assertThat(finalValue.value).isEqualTo("${mappedValues[elements[7].name]}")

        // Default forces override
        finalValue = elements[8].processValue(mappedValues, schema, itemIndex = 1)
        assertThat(finalValue.value).isEqualTo("someDefault")

        // Default forces override, and the default is null.
        finalValue = elements[9].processValue(mappedValues, schema, itemIndex = 1)
        assertThat(finalValue.value).isEqualTo("")
    }

    @Test
    fun `test use mapper check`() {
        val elementA = Element("a")
        val elementB = Element("b", mapper = "concat(a,b)", mapperRef = ConcatenateMapper())
        val elementC = Element(
            "b", mapper = "concat(a,b)", mapperRef = ConcatenateMapper(),
            mapperOverridesValue = true
        )

        assertThat(elementA.useMapper("")).isFalse()
        assertThat(elementA.useMapper("dummyValue")).isFalse()

        assertThat(elementB.useMapper("")).isTrue()
        assertThat(elementB.useMapper("dummyValue")).isFalse()

        assertThat(elementC.useMapper("")).isTrue()
        assertThat(elementC.useMapper("dummyValue")).isTrue()
    }

    @Test
    fun `test use default check`() {
        val elementA = Element("a")

        assertThat(elementA.useDefault("")).isTrue()
        assertThat(elementA.useDefault("dummyValue")).isFalse()
    }

    @Test
    fun `test tokenized value mapping`() {
        val mockElement = Element("mock")

        // sending in "$index" should return the index of the row being processed
        val elementNameIndex = "\$index"
        val elementAndValueIndex = mockElement.tokenizeMapperValue(elementNameIndex, 3)
        assertThat(elementAndValueIndex.value).isEqualTo("3")

        // sending in "$currentDate" should return the current date of when the function was ran
        val elementNameCurrentDate = "\$currentDate"
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val currentDate = LocalDate.now().format(formatter)
        val elementAndValueCurrentDate = mockElement.tokenizeMapperValue(elementNameCurrentDate)
        assertThat(elementAndValueCurrentDate.value).isEqualTo(currentDate)

        // if nothing "parsable" comes through, the token value will be an empty string
        val elementNameNonValidToken = "\$nonValidToken:not valid"
        val elementAndValueNotValidToken = mockElement.tokenizeMapperValue(elementNameNonValidToken)
        assertThat(elementAndValueNotValidToken.value).isEqualTo("")

        // sending in a "mode:literal" should return just the mode, which in this case is "literal"
        val elementNameMode = "\$mode:literal"
        val elementAndValueMode = mockElement.tokenizeMapperValue(elementNameMode)
        assertThat(elementAndValueMode.value).isEqualTo("literal")

        // sending in a "string:someDefaultString" should return just the string that needs to be the default value,
        // which in this case is "someDefaultString"
        val elementNameString = "\$string:someDefaultString"
        val elementAndValueString = mockElement.tokenizeMapperValue(elementNameString)
        assertThat(elementAndValueString.value).isEqualTo("someDefaultString")
    }

    @Test
    fun `test element result data class`() {
        val element = Element("a")
        var result = ElementResult(null)
        assertThat(result.value).isNull()

        result = ElementResult("value")
        assertThat(result.value).isEqualTo("value")
        assertThat(result.errors).isEmpty()
        assertThat(result.warnings).isEmpty()

        result.warning(InvalidEquipmentMessage(element.fieldMapping))
        assertThat(result.warnings.size).isEqualTo(1)
        assertThat(result.errors).isEmpty()

        result.error(InvalidEquipmentMessage(element.fieldMapping))
        assertThat(result.errors.size).isEqualTo(1)

        result = ElementResult(
            "value",
            mutableListOf(
                InvalidEquipmentMessage(element.fieldMapping),
                InvalidEquipmentMessage(element.fieldMapping)
            ),
            mutableListOf(
                InvalidEquipmentMessage(element.fieldMapping), InvalidEquipmentMessage(element.fieldMapping),
                InvalidEquipmentMessage(element.fieldMapping)
            )
        )
        result.warning(InvalidEquipmentMessage(element.fieldMapping))
        assertThat(result.warnings.size).isEqualTo(4)
        assertThat(result.errors.size).isEqualTo(2)
        result.error(InvalidEquipmentMessage(element.fieldMapping))
        assertThat(result.errors.size).isEqualTo(3)
    }

    @Test
    fun `test process value`() {
        val elementA = Element("a")
        val elementB = Element("b", default = "default")
        val elementC = Element("c", mapperRef = NullMapper())
        val elementD = Element("d", mapperRef = NullMapper(), default = "default")
        val elementE = Element("e", mapperRef = TrimBlanksMapper(), mapperArgs = listOf("a"))
        val schema = Schema(
            "name", "topic",
            elements = listOf(elementA, elementB, elementC, elementD, elementE)
        )

        // Simple value tests with elements with no mapper or default value
        assertThat(elementA.processValue(emptyMap(), schema, itemIndex = 1).value).isEqualTo("")

        var allElementValues = mapOf(elementB.name to "")
        assertThat(elementA.processValue(allElementValues, schema, itemIndex = 1).value).isEqualTo("")

        allElementValues = mapOf(elementA.name to "")
        assertThat(elementA.processValue(allElementValues, schema, itemIndex = 1).value).isEqualTo("")

        allElementValues = mapOf(elementA.name to "value")
        assertThat(elementA.processValue(allElementValues, schema, itemIndex = 1).value).isEqualTo("value")

        // Simple value tests with elements with no mapper, but default value
        allElementValues = mapOf(elementA.name to "", elementB.name to "")
        assertThat(elementB.processValue(allElementValues, schema, itemIndex = 1).value).isEqualTo(elementB.default)

        allElementValues = mapOf(elementA.name to "", elementB.name to "value")
        assertThat(elementB.processValue(allElementValues, schema, itemIndex = 1).value).isEqualTo("value")

        // Now with a mapper that returns an empty/missing value
        allElementValues = mapOf(elementC.name to "")
        assertThat(elementC.processValue(allElementValues, schema, itemIndex = 1).value).isEqualTo("")

        allElementValues = mapOf(elementD.name to "")
        assertThat(elementD.processValue(allElementValues, schema, itemIndex = 1).value).isEqualTo(elementD.default)

        // Now with a mapper that returns some non-blank value
        allElementValues = mapOf(elementA.name to "untrimmedvalue  ")
        assertThat(elementE.processValue(allElementValues, schema, itemIndex = 1).value).isEqualTo("untrimmedvalue")
    }

    @Test
    fun `test process value returns warnings or errors`() {
        class SomeCoolMapper : Mapper {
            override val name = "some"

            override fun valueNames(element: Element, args: List<String>): List<String> {
                return args
            }

            override fun apply(
                element: Element,
                args: List<String>,
                values: List<ElementAndValue>,
                sender: Sender?
            ): ElementResult {
                return if (args.isEmpty()) ElementResult(null)
                else when (args[0]) {
                    "1warning" -> ElementResult(null).warning(InvalidEquipmentMessage(element.fieldMapping))
                    "2warnings" -> ElementResult(null).warning(InvalidEquipmentMessage(element.fieldMapping))
                        .warning(InvalidEquipmentMessage(element.fieldMapping))
                    "1error" -> ElementResult(null).error(InvalidEquipmentMessage(element.fieldMapping))
                    "2errors" -> ElementResult(null).error(InvalidEquipmentMessage(element.fieldMapping))
                        .error(InvalidEquipmentMessage(element.fieldMapping))
                    "mixed" -> ElementResult(null).error(InvalidEquipmentMessage(element.fieldMapping))
                        .warning(InvalidEquipmentMessage(element.fieldMapping))
                    else -> throw UnsupportedOperationException()
                }
            }
        }
        val elementA = Element("a", mapperRef = SomeCoolMapper())
        val elementB = Element(
            "a", mapperRef = SomeCoolMapper(), mapperArgs = listOf("1warning"),
            cardinality = Element.Cardinality.ZERO_OR_ONE
        )
        val elementC = Element(
            "a", mapperRef = SomeCoolMapper(), mapperArgs = listOf("2warnings"),
            cardinality = Element.Cardinality.ZERO_OR_ONE
        )
        val elementD = Element(
            "a", mapperRef = SomeCoolMapper(), mapperArgs = listOf("1error"),
            cardinality = Element.Cardinality.ZERO_OR_ONE
        )
        val elementE = Element(
            "a", mapperRef = SomeCoolMapper(), mapperArgs = listOf("2errors"),
            cardinality = Element.Cardinality.ZERO_OR_ONE
        )
        val elementF = Element(
            "a", mapperRef = SomeCoolMapper(), mapperArgs = listOf("mixed"),
            cardinality = Element.Cardinality.ZERO_OR_ONE
        )
        val elementG = Element(
            "a", mapperRef = SomeCoolMapper(), mapperArgs = listOf("1warning"),
            cardinality = Element.Cardinality.ONE
        )
        val elementH = Element(
            "a", mapperRef = SomeCoolMapper(), mapperArgs = listOf("1error"),
            cardinality = Element.Cardinality.ONE
        )
        val elementI = Element(
            "a", mapperRef = SomeCoolMapper(), mapperArgs = listOf("mixed"),
            cardinality = Element.Cardinality.ONE
        )
        val elementJ = Element("a", mapperRef = SomeCoolMapper(), cardinality = Element.Cardinality.ONE)
        val schema = Schema(
            "name", "topic",
            elements = listOf(
                elementA, elementB, elementC, elementD, elementE, elementF, elementG, elementH, elementI,
                elementJ
            )
        )

        var result = elementA.processValue(emptyMap(), schema, itemIndex = 1)
        assertThat(result.errors).isEmpty()
        assertThat(result.warnings).isEmpty()

        result = elementB.processValue(emptyMap(), schema, itemIndex = 1)
        assertThat(result.errors).isEmpty()
        assertThat(result.warnings.size).isEqualTo(1)

        result = elementC.processValue(emptyMap(), schema, itemIndex = 1)
        assertThat(result.errors).isEmpty()
        assertThat(result.warnings.size).isEqualTo(2)

        // The mapper returns an error, but the field is not required, so we get warnings.
        result = elementD.processValue(emptyMap(), schema, itemIndex = 1)
        assertThat(result.errors).isEmpty()
        assertThat(result.warnings.size).isEqualTo(1)

        result = elementE.processValue(emptyMap(), schema, itemIndex = 1)
        assertThat(result.errors).isEmpty()
        assertThat(result.warnings.size).isEqualTo(2)

        result = elementF.processValue(emptyMap(), schema, itemIndex = 1)
        assertThat(result.errors).isEmpty()
        assertThat(result.warnings.size).isEqualTo(2)

        // The element value is required, so we always get an error.
        result = elementG.processValue(emptyMap(), schema, itemIndex = 1)
        assertThat(result.errors).isNotEmpty()
        assertThat(result.warnings).isEmpty()

        result = elementH.processValue(emptyMap(), schema, itemIndex = 1)
        assertThat(result.errors).isNotEmpty()
        assertThat(result.warnings).isEmpty()

        result = elementI.processValue(emptyMap(), schema, itemIndex = 1)
        assertThat(result.errors).isNotEmpty()
        assertThat(result.warnings).isNotEmpty()

        // And now just a required element that has a blank value
        result = elementJ.processValue(emptyMap(), schema, itemIndex = 1)
        assertThat(result.errors).isNotEmpty()
        assertThat(result.warnings).isEmpty()

        // Test an incorrect index
        assertThat { elementJ.processValue(emptyMap(), schema, itemIndex = 0) }
            .isFailure()
    }

    @Test
    fun `test element validation`() {
        // Type tests
        assertThat(Element("name").validate()).isNotEmpty()
        assertThat(Element("name", type = Element.Type.TEXT).validate()).isEmpty()

        // Lookup mapper tests
        assertThat(
            Element("name", type = Element.Type.TEXT, mapperRef = LookupMapper())
                .validate()
        ).isNotEmpty()
        assertThat(
            Element("name", type = Element.Type.TEXT, mapperRef = LookupMapper(), tableRef = LookupTable())
                .validate()
        ).isNotEmpty()
        assertThat(
            Element("name", type = Element.Type.TEXT, mapperRef = LookupMapper(), tableColumn = "column")
                .validate()
        ).isNotEmpty()
        assertThat(
            Element(
                "name", type = Element.Type.TEXT, mapperRef = LookupMapper(), tableRef = LookupTable(),
                tableColumn = "column"
            )
                .validate()
        ).isEmpty()
        assertThat(
            Element("name", type = Element.Type.TEXT, mapperRef = LIVDLookupMapper())
                .validate()
        ).isNotEmpty()
        assertThat(
            Element("name", type = Element.Type.TEXT, mapperRef = LIVDLookupMapper(), tableRef = LookupTable())
                .validate()
        ).isNotEmpty()
        assertThat(
            Element("name", type = Element.Type.TEXT, mapperRef = LIVDLookupMapper(), tableColumn = "column")
                .validate()
        ).isNotEmpty()
        assertThat(
            Element(
                "name", type = Element.Type.TEXT, mapperRef = LIVDLookupMapper(), tableRef = LookupTable(),
                tableColumn = "column"
            )
                .validate()
        ).isEmpty()

        // Mapper tests
        assertThat(
            Element("name", type = Element.Type.TEXT, mapperOverridesValue = true)
                .validate()
        ).isNotEmpty()
        assertThat(
            Element("name", type = Element.Type.TEXT, mapperOverridesValue = true, mapperRef = NullMapper())
                .validate()
        ).isEmpty()

        assertThat(
            Element("name", type = Element.Type.TEXT, mapperArgs = listOf("arg"), mapperRef = NullMapper())
                .validate()
        ).isEmpty()
        assertThat(
            Element("name", type = Element.Type.TEXT, mapperArgs = listOf("arg"), mapperRef = NullMapper())
                .validate()
        ).isEmpty()

        // Table tests
        assertThat(
            Element("name", type = Element.Type.TABLE)
                .validate()
        ).isNotEmpty()
        assertThat(
            Element("name", type = Element.Type.TABLE, tableRef = LookupTable())
                .validate()
        ).isEmpty()
        assertThat(
            Element("name", type = Element.Type.TABLE_OR_BLANK)
                .validate()
        ).isNotEmpty()
        assertThat(
            Element("name", type = Element.Type.TABLE_OR_BLANK, tableRef = LookupTable())
                .validate()
        ).isEmpty()
        assertThat(
            Element("name", type = Element.Type.TEXT, tableColumn = "column")
                .validate()
        ).isNotEmpty()
        assertThat(
            Element("name", type = Element.Type.TEXT, tableColumn = "column", tableRef = LookupTable())
                .validate()
        ).isEmpty()

        // Can be blank test
        assertThat(
            Element("name", type = Element.Type.TEXT_OR_BLANK)
                .validate()
        ).isEmpty()
        assertThat(
            Element("name", type = Element.Type.TEXT_OR_BLANK, default = "default")
                .validate()
        ).isNotEmpty()

        // Multiple errors returned
        assertThat(
            Element("name", type = Element.Type.TEXT_OR_BLANK, default = "default", mapperArgs = listOf("arg"))
                .validate()
        ).size().isGreaterThan(1)
    }

    @Test
    fun `test field mapping output`() {
        var element = Element("name")
        assertThat(element.fieldMapping).contains(element.name)

        element = Element("name", hl7Field = "OBX-1")
        assertThat(element.fieldMapping).contains(element.name)
        assertThat(element.fieldMapping).contains(element.hl7Field!!)

        element = Element("name", hl7Field = "OBX-1", hl7OutputFields = listOf("OBX-2", "OBX-3"))
        assertThat(element.fieldMapping).contains(element.name)
        assertThat(element.fieldMapping).contains(element.hl7Field!!)
        element.hl7OutputFields!!.forEach { assertThat(element.fieldMapping).doesNotContain(it) }

        element = Element("name", hl7OutputFields = listOf("OBX-2", "OBX-3"))
        assertThat(element.fieldMapping).contains(element.name)
        element.hl7OutputFields!!.forEach { assertThat(element.fieldMapping).contains(it) }

        element = Element(
            "name",
            csvFields = listOf(Element.CsvField("fielda", null), Element.CsvField("fieldb", null))
        )
        assertThat(element.fieldMapping).contains(element.name)
        element.csvFields!!.forEach { assertThat(element.fieldMapping).contains(it.name) }

        // CSV fields win over HL7
        element = Element(
            "name", hl7Field = "OBX-1",
            csvFields = listOf(Element.CsvField("fielda", null))
        )
        assertThat(element.fieldMapping).contains(element.name)
        element.csvFields!!.forEach { assertThat(element.fieldMapping).contains(it.name) }
        assertThat(element.fieldMapping).doesNotContain(element.hl7Field!!)
    }
}