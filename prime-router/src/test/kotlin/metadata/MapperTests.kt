package gov.cdc.prime.router.metadata

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.Element
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.common.NPIUtilities
import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.fail

class MapperTests {
    @Test
    fun `test MiddleInitialMapper`() {
        val mapper = MiddleInitialMapper()
        val args = listOf("test_element")
        val element = Element("test")
        assertThat(mapper.apply(element, args, listOf(ElementAndValue(element, "Rick"))).value).isEqualTo("R")
        assertThat(mapper.apply(element, args, listOf(ElementAndValue(element, "rick"))).value).isEqualTo("R")
    }

    @Test
    fun `test LookupMapper`() {
        val csv = """
            a,b,c
            1,2,x
            3,4,y
            5,6,z
        """.trimIndent()
        val table = LookupTable.read(inputStream = ByteArrayInputStream(csv.toByteArray()))
        val schema = Schema(
            "test", topic = "test",
            elements = listOf(
                Element("a", type = Element.Type.TABLE, table = "test", tableColumn = "a"),
                Element("c", type = Element.Type.TABLE, table = "test", tableColumn = "c")
            )
        )
        val metadata = Metadata(schema = schema, table = table, tableName = "test")
        val indexElement = metadata.findSchema("test")?.findElement("a") ?: fail("")
        val lookupElement = metadata.findSchema("test")?.findElement("c") ?: fail("")
        val mapper = LookupMapper()
        val args = listOf("a")
        assertThat(mapper.valueNames(lookupElement, args)).isEqualTo(listOf("a"))
        assertThat(mapper.apply(lookupElement, args, listOf(ElementAndValue(indexElement, "3"))).value)
            .isEqualTo("y")
    }

    @Test
    fun `test LookupMapper with two`() {
        val csv = """
            a,b,c
            1,2,x
            3,4,y
            5,6,z
        """.trimIndent()
        val table = LookupTable.read(inputStream = ByteArrayInputStream(csv.toByteArray()))
        val schema = Schema(
            "test", topic = "test",
            elements = listOf(
                Element("a", type = Element.Type.TABLE, table = "test", tableColumn = "a"),
                Element("b", type = Element.Type.TABLE, table = "test", tableColumn = "b"),
                Element("c", type = Element.Type.TABLE, table = "test", tableColumn = "c")
            )
        )
        val metadata = Metadata(schema = schema, table = table, tableName = "test")
        val lookupElement = metadata.findSchema("test")?.findElement("c") ?: fail("")
        val indexElement = metadata.findSchema("test")?.findElement("a") ?: fail("")
        val index2Element = metadata.findSchema("test")?.findElement("b") ?: fail("")
        val mapper = LookupMapper()
        val args = listOf("a", "b")
        val elementAndValues = listOf(ElementAndValue(indexElement, "3"), ElementAndValue(index2Element, "4"))
        assertThat(mapper.valueNames(lookupElement, args)).isEqualTo(listOf("a", "b"))
        assertThat(mapper.apply(lookupElement, args, elementAndValues).value).isEqualTo("y")
    }

    @Test
    fun `test ifPresent`() {
        val element = Element("a")
        val mapper = IfPresentMapper()
        val args = listOf("a", "const")
        assertThat(mapper.valueNames(element, args)).isEqualTo(listOf("a"))
        assertThat(mapper.apply(element, args, listOf(ElementAndValue(element, "3"))).value).isEqualTo("const")
        assertThat(mapper.apply(element, args, emptyList()).value).isNull()
    }

    @Test
    fun `test get cleaned up model name`() {
        val modelName = "some model"
        assertThat(LivdLookupUtilities.getCleanedModelName(modelName)).isEqualTo(modelName)
        assertThat(LivdLookupUtilities.getCleanedModelName("$modelName*")).isEqualTo(modelName)
        assertThat(LivdLookupUtilities.getCleanedModelName("*$modelName**")).isEqualTo("*$modelName*")
    }

    @Test
    fun `test use`() {
        val elementA = Element("a")
        val elementB = Element("b")
        val elementC = Element("c")
        val mapper = UseMapper()
        val args = listOf("b", "c")
        assertThat(mapper.valueNames(elementA, args)).isEqualTo(listOf("b", "c"))
        assertThat(
            mapper.apply(elementA, args, listOf(ElementAndValue(elementB, "B"), ElementAndValue(elementC, "C")))
                .value
        ).isEqualTo("B")
        assertThat(mapper.apply(elementA, args, listOf(ElementAndValue(elementC, "C"))).value).isEqualTo("C")
        assertThat(mapper.apply(elementA, args, emptyList()).value).isNull()
    }

    @Test
    fun `test useSenderSetting`() {
        val sender = Sender(
            "senderName",
            "orgName",
            format = Sender.Format.CSV,
            "covid-19",
            CustomerStatus.ACTIVE,
            "mySchemaName",
            keys = null,
            processingType = Sender.ProcessingType.async
        )
        val elementA = Element("a")
        val mapper = UseSenderSettingMapper()
        var args = listOf("name")
        assertThat(mapper.valueNames(elementA, args)).isEqualTo(emptyList())
        assertThat(mapper.apply(elementA, args, emptyList(), sender).value)
            .isEqualTo("senderName")
        args = listOf("organizationName")
        assertThat(mapper.apply(elementA, args, emptyList(), sender).value)
            .isEqualTo("orgName")
        args = listOf("fullName")
        assertThat(mapper.apply(elementA, args, emptyList(), sender).value)
            .isEqualTo("orgName.senderName")
        args = listOf("topic")
        assertThat(mapper.apply(elementA, args, emptyList(), sender).value)
            .isEqualTo("covid-19")
        args = listOf("schemaName")
        assertThat(mapper.apply(elementA, args, emptyList(), sender).value)
            .isEqualTo("mySchemaName")

        // useSenderSetting does a best effort to get string values for other types.
        args = listOf("processingType")
        assertThat(mapper.apply(elementA, args, emptyList(), sender).value)
            .isEqualTo("async")
        args = listOf("format")
        assertThat(mapper.apply(elementA, args, emptyList(), sender).value)
            .isEqualTo("CSV")
        args = listOf("customerStatus")
        assertThat(mapper.apply(elementA, args, emptyList(), sender).value)
            .isEqualTo("ACTIVE")
        args = listOf("processingModeCode")
        assertThat(mapper.apply(elementA, args, emptyList(), sender).value)
            .isEqualTo("P")
        // Error cases:
        // Must have an arg
        args = emptyList()
        assertFails(block = { mapper.apply(elementA, args, emptyList(), sender) })
        // Must have a single arg
        args = listOf("a", "b")
        assertFails(block = { mapper.apply(elementA, args, emptyList(), sender) })
        // Arg must be a valid name of a Sender settings field
        args = listOf("not a valid value")
        val response = mapper.apply(elementA, args, emptyList(), sender)
        assertThat(response.value).isNull()
        assertThat(response.errors.size).isEqualTo(1)
        assertThat(response.errors[0].message).contains("not a valid value")
    }

    @Test
    fun `test ConcatenateMapper`() {
        val mapper = ConcatenateMapper()
        val args = listOf("a", "b", "c")
        val elementA = Element("a")
        val elementB = Element("b")
        val elementC = Element("c")
        val values = listOf(
            ElementAndValue(elementA, "string1"),
            ElementAndValue(elementB, "string2"),
            ElementAndValue(elementC, "string3")
        )
        assertThat(mapper.apply(elementA, args, values).value).isEqualTo("string1, string2, string3")
    }

    @Test
    fun `test concatenate mapper with custom delimiter`() {
        // arrange
        val mapper = ConcatenateMapper()
        val args = listOf("a", "b", "c")
        val elementA = Element("a", delimiter = "^")
        val elementB = Element("b")
        val elementC = Element("c")
        val values = listOf(
            ElementAndValue(elementA, "string1"),
            ElementAndValue(elementB, "string2"),
            ElementAndValue(elementC, "string3")
        )
        // act
        val expected = "string1^string2^string3"
        val actual = mapper.apply(elementA, args, values)
        // assert
        assertThat(actual.value).isEqualTo(expected)
    }

    @Test
    fun `test lookup with NpiLookupMapper`() {
        /* Schema and table configuration for tests */
        val table = LookupTable.read("./src/test/resources/metadata/tables/npi-lookup.csv")
        val tableName = "npi-lookup"

        /* Hopefully this shuts SonarCloud up about duplicate code */
        val elementInfo: List<Pair<String, String>> = listOf(
            Pair("ordering_provider_id", "ordering_provider_id"),
            Pair("testing_lab_clia", "testing_lab_clia"),
            Pair("sender_id", "sender_id"),
            Pair("ordering_provider_first_name", "first_name"),
            Pair("ordering_provider_last_name", "last_name")
        )
        val elements: List<Element> = elementInfo.map { info ->
            Element(
                name = info.first,
                type = Element.Type.TABLE,
                table = tableName,
                tableColumn = info.second
            )
        }
        val schema = Schema(
            tableName, topic = "Testing NPI lookup mapper",
            elements = elements
        )
        val metadata = Metadata(schema = schema, table = table, tableName = tableName)
        val npiElement = metadata.findSchema(tableName)?.findElement("ordering_provider_id")
            ?: fail("Did not find Ordering_provider_id in test schema")
        val cliaElement = metadata.findSchema(tableName)?.findElement("testing_lab_clia")
            ?: fail("Did not find Testing_lab_clia in test schema")
        val senderIdElement = metadata.findSchema(tableName)?.findElement("sender_id")
            ?: fail("Did not find sender_id in test schema")
        val firstNameElement = metadata.findSchema(tableName)?.findElement("ordering_provider_first_name")
            ?: fail("Did not find Ordering_provider_first_name in test schema")
        val lastNameElement = metadata.findSchema(tableName)?.findElement("ordering_provider_last_name")
            ?: fail("Did not find Ordering_provider_last_name in test schema")
        val mapper = NpiLookupMapper()

        val args = listOf(npiElement.name, cliaElement.name, senderIdElement.name)

        /* Testing value lookup when NPI is present */
        val evNpiPresent = listOf(
            ElementAndValue(npiElement, "1023040318"),
            ElementAndValue(cliaElement, "01D2079572"),
            ElementAndValue(senderIdElement, "cuc-al")
        )

        assertThat(mapper.valueNames(firstNameElement, args))
            .isEqualTo(listOf("ordering_provider_id", "testing_lab_clia", "sender_id"))
        assertThat(mapper.apply(firstNameElement, args, evNpiPresent).value).isEqualTo("Paul")

        assertThat(mapper.valueNames(lastNameElement, args))
            .isEqualTo(listOf("ordering_provider_id", "testing_lab_clia", "sender_id"))
        assertThat(mapper.apply(lastNameElement, args, evNpiPresent).value).isEqualTo("Fineburg")

        /* Testing value lookup when NPI is NOT present */
        val evNpiNotPresent = listOf(
            ElementAndValue(npiElement, ""),
            ElementAndValue(cliaElement, "01D2079572"),
            ElementAndValue(senderIdElement, "cuc-al")
        )

        assertThat(mapper.valueNames(firstNameElement, args))
            .isEqualTo(listOf("ordering_provider_id", "testing_lab_clia", "sender_id"))
        assertThat(mapper.apply(firstNameElement, args, evNpiNotPresent).value).isEqualTo("Paul")

        assertThat(mapper.valueNames(lastNameElement, args))
            .isEqualTo(listOf("ordering_provider_id", "testing_lab_clia", "sender_id"))
        assertThat(mapper.apply(lastNameElement, args, evNpiNotPresent).value).isEqualTo("Fineburg")
    }

    @Test
    fun `test date time offset mapper with seconds`() {
        // arrange
        val mapper = DateTimeOffsetMapper()
        val args = listOf(
            "a",
            "seconds",
            "6"
        )
        val element = Element("a")
        val values = listOf(
            ElementAndValue(element, "202103020000-0600")
        )
        // act
        val expected = "20210302000006.0000-0600"
        val actual = mapper.apply(element, args, values)
        // assert
        assertThat(actual.value).isEqualTo(expected)
    }

    @Test
    fun `test date time offset mapper with negative seconds`() {
        // arrange
        val mapper = DateTimeOffsetMapper()
        val args = listOf(
            "a",
            "seconds",
            "-6"
        )
        val element = Element("a")
        val values = listOf(
            ElementAndValue(element, "20210302000006.0000-0600")
        )
        // act
        val expected = "20210302000000.0000-0600"
        val actual = mapper.apply(element, args, values)
        // assert
        assertThat(actual.value).isEqualTo(expected)
    }

    @Test
    fun `test date time offset mapper with minutes`() {
        // arrange
        val mapper = DateTimeOffsetMapper()
        val args = listOf(
            "a",
            "minutes",
            "1"
        )
        val element = Element("a")
        val values = listOf(
            ElementAndValue(element, "202103020000-0600")
        )
        // act
        val expected = "20210302000100.0000-0600"
        val actual = mapper.apply(element, args, values)
        // assert
        assertThat(actual.value).isEqualTo(expected)
    }

    @Test
    fun `test date time offset mapper with negative minutes`() {
        // arrange
        val mapper = DateTimeOffsetMapper()
        val args = listOf(
            "a",
            "minutes",
            "-1"
        )
        val element = Element("a")
        val values = listOf(
            ElementAndValue(element, "20210302000100.0000-0600")
        )
        // act
        val expected = "20210302000000.0000-0600"
        val actual = mapper.apply(element, args, values)
        // assert
        assertThat(actual.value).isEqualTo(expected)
    }

    @Test
    fun `test coalesce mapper`() {
        // arrange
        val mapper = CoalesceMapper()
        val args = listOf("a", "b", "c")
        val element = Element("target")
        var values = listOf(
            ElementAndValue(Element("a"), ""),
            ElementAndValue(Element("b"), ""),
            ElementAndValue(Element("c"), "c")
        )
        // act
        var expected = "c"
        var actual = mapper.apply(element, args, values)
        // assert
        assertThat(actual.value).isEqualTo(expected)

        values = listOf(
            ElementAndValue(Element("a"), ""),
            ElementAndValue(Element("b"), "b"),
            ElementAndValue(Element("c"), "c")
        )
        expected = "b"
        actual = mapper.apply(element, args, values)
        assertThat(actual.value).isEqualTo(expected)
    }

    @Test
    fun `test strip formatting mapper`() {
        val mapper = StripPhoneFormattingMapper()
        val args = listOf("patient_phone_number_raw")
        val element = Element("patient_phone_number")
        val values = listOf(
            ElementAndValue(Element("patient_phone_number_raw"), "(850) 999-9999xHOME")
        )
        val expected = "8509999999:1:"
        val actual = mapper.apply(element, args, values)
        assertThat(actual.value).isEqualTo(expected)
    }

    @Test
    fun `test strip numeric mapper`() {
        val mapper = StripNumericDataMapper()
        val args = listOf("patient_age_and_units")
        val element = Element("patient_age")
        val values = listOf(
            ElementAndValue(Element("patient_age_and_units"), "99 years")
        )
        val expected = "years"
        val actual = mapper.apply(element, args, values)
        assertThat(actual.value).isEqualTo(expected)
    }

    @Test
    fun `test strip non numeric mapper`() {
        val mapper = StripNonNumericDataMapper()
        val args = listOf("patient_age_and_units")
        val element = Element("patient_age")
        val values = listOf(
            ElementAndValue(Element("patient_age_and_units"), "99 years")
        )
        val expected = "99"
        val actual = mapper.apply(element, args, values)
        assertThat(actual.value).isEqualTo(expected)
    }

    @Test
    fun `test split mapper`() {
        val mapper = SplitMapper()
        val args = listOf("patient_name", "0")
        val element = Element("patient_first_name")
        val values = listOf(
            ElementAndValue(Element("patient_name"), "John Doe")
        )
        val expected = "John"
        val actual = mapper.apply(element, args, values)
        assertThat(actual.value).isEqualTo(expected)
    }

    @Test
    fun `test split mapper with error condition`() {
        val mapper = SplitMapper()
        val args = listOf("patient_name", "1")
        val element = Element("patient_first_name")
        val values = listOf(
            ElementAndValue(Element("patient_name"), "ThereAreNoSpacesHere")
        )
        val actual = mapper.apply(element, args, values)
        assertThat(actual.value).isNull()
    }

    @Test
    fun `test split by comma mapper`() {
        val mapper = SplitByCommaMapper()
        val args = listOf("patient_name", "2")
        val element = Element("patient_first_name")
        val values = listOf(
            ElementAndValue(Element("patient_name"), "Antley, ARNP, Mona")
        )
        val expected = "Mona"
        val actual = mapper.apply(element, args, values)
        assertThat(actual.value).isEqualTo(expected)
    }

    @Test
    fun `test split by comma mapper error condition`() {
        val mapper = SplitByCommaMapper()
        val args = listOf("patient_name", "2")
        val element = Element("patient_first_name")
        val values = listOf(
            ElementAndValue(Element("patient_name"), "I have no commas")
        )
        val actual = mapper.apply(element, args, values)
        assertThat(actual.value).isNull()
    }

    @Test
    fun `test zip code to county mapper`() {
        val mapper = ZipCodeToCountyMapper()
        val csv = """
            zipcode,county
            32303,Leon
        """.trimIndent()
        val table = LookupTable.read(inputStream = ByteArrayInputStream(csv.toByteArray()))
        val schema = Schema(
            "test", topic = "test",
            elements = listOf(
                Element("a", type = Element.Type.TABLE, table = "test", tableColumn = "a"),
            )
        )
        val metadata = Metadata(schema = schema, table = table, tableName = "test")
        val lookupElement = metadata.findSchema("test")?.findElement("a") ?: fail("Schema element missing")
        val values = listOf(
            ElementAndValue(Element("patient_zip_code"), "32303-4509")
        )
        val expected = "Leon"
        val actual = mapper.apply(lookupElement, listOf("patient_zip_code"), values)
        assertThat(actual.value).isEqualTo(expected)
    }

    @Test
    fun `test HashMapper`() {
        val mapper = HashMapper()
        val elementA = Element("a")
        val elementB = Element("b")
        val elementC = Element("c")

        // Single value conversion
        val arg = listOf("a")
        val value = listOf(ElementAndValue(elementA, "6086edf8e412650032408e96"))
        assertThat(mapper.apply(elementA, arg, value).value)
            .isEqualTo("47496cafa04e9c489444b60575399f51e9abc061f4fdda40c31d814325bfc223")
        // Multiple values concatenated
        val args = listOf("a", "b", "c")
        val values = listOf(
            ElementAndValue(elementA, "string1"),
            ElementAndValue(elementB, "string2"),
            ElementAndValue(elementC, "string3")
        )
        assertThat(mapper.apply(elementA, args, values).value)
            .isEqualTo("c8fa773cd54e7a7eb7ca08577d0bd23e6ce3a73e61df176213d9ec90f06cb45f")
        // Unhappy path cases
        assertFails { mapper.apply(elementA, listOf(), listOf()) } // must pass a field name
        assertThat(mapper.apply(elementA, arg, listOf()).value).isNull() // column not found in the data.
        // column has empty data
        assertThat(mapper.apply(elementA, arg, listOf(ElementAndValue(elementA, ""))).value).isNull()
    }

    @Test
    fun `test parseMapperField validation - allow mapper tokens to be parsed`() {
        // it should allow mapper tokens to be parsed: i.e. "$index"
        val vals = Mappers.parseMapperField("concat(patient_id, \$index)")
        assertThat(vals.second[1]).isEqualTo("\$index")
    }

    @Test
    fun `test IfNotPresent mapper`() {
        val mapper = IfNotPresentMapper()
        val elementA = Element("a")
        val elementB = Element("lookup_field")
        val elementC1 = Element("condition_field_1")
        val elementC2 = Element("condition_field_2")

        // $mode:"literal" tests

        // conditional fields are blank: should return the "$string" value
        var args = listOf(
            "\$mode:literal", "\$string:*** No Address Given ***", "condition_field_1", "condition_field_2"
        )
        var values = listOf(ElementAndValue(elementC1, ""), ElementAndValue(elementC2, ""))
        assertThat(mapper.apply(elementA, args, values).value)
            .isEqualTo("*** No Address Given ***")

        // any conditional field is non-blank: should return null
        args = listOf("\$mode:literal", "\$string:*** No Address Given ***", "condition_field_1", "condition_field_2")
        values = listOf(ElementAndValue(elementC1, ""), ElementAndValue(elementC2, "nonBlank"))
        assertThat(mapper.apply(elementA, args, values).value)
            .isEqualTo(null)

        // conditional fields not present: should return the "$string" value
        args = listOf("\$mode:literal", "\$string:*** No Address Given ***", "condition_field_1", "condition_field_2")
        values = listOf(ElementAndValue(elementA, ""))
        assertThat(mapper.apply(elementA, args, values).value)
            .isEqualTo("*** No Address Given ***")

        // $mode:"lookup" tests

        // conditional fields are blank: should return the value of lookup_field (Element B)
        args = listOf("\$mode:lookup", "lookup_field", "condition_field_1", "condition_field_2")
        values = listOf(
            ElementAndValue(elementB, "value of B"),
            ElementAndValue(elementC1, ""),
            ElementAndValue(elementC2, "")
        )
        assertThat(mapper.apply(elementA, args, values).value)
            .isEqualTo("value of B")

        // any conditional field is non-blank: should return null
        args = listOf("\$mode:lookup", "lookup_field", "condition_field_1", "condition_field_2")
        values = listOf(
            ElementAndValue(elementB, "value of B"),
            ElementAndValue(elementC1, ""),
            ElementAndValue(elementC2, "nonBlank")
        )
        assertThat(mapper.apply(elementA, args, values).value)
            .isEqualTo(null)

        // conditional fields not present: should return the value of lookup_field (Element B)
        args = listOf("\$mode:lookup", "lookup_field", "condition_field_1", "condition_field_2")
        values = listOf(ElementAndValue(elementA, ""), ElementAndValue(elementB, "value of B"))
        assertThat(mapper.apply(elementA, args, values).value)
            .isEqualTo("value of B")

        // single non-blank condition: should return null
        args = listOf("\$mode:lookup", "lookup_field", "condition_field_1")
        values = listOf(ElementAndValue(elementB, "value of B"), ElementAndValue(elementC1, "nonBlank"))
        assertThat(mapper.apply(elementA, args, values).value)
            .isEqualTo(null)

        // invalid $mode should return null
        args = listOf("\$mode:iNvAlId", "lookup_field", "condition_field_1", "condition_field_2")
        values = listOf(ElementAndValue(elementA, ""), ElementAndValue(elementB, "value of B"))
        assertThat(mapper.apply(elementA, args, values).value)
            .isEqualTo(null)
    }

    @Test
    fun `test ifNPI with valid NPI`() {
        val mapper = IfNPIMapper()
        val elementA = Element("a")
        val args = listOf("a", "NPI", "U")
        val values = listOf(ElementAndValue(elementA, NPIUtilities.VALID_NPI))
        assertThat(mapper.apply(elementA, args, values).value).isEqualTo("NPI")
    }

    @Test
    fun `test ifNPI with invalid NPI`() {
        val mapper = IfNPIMapper()
        val elementA = Element("a")
        val args = listOf("a", "NPI", "U")
        val values = listOf(ElementAndValue(elementA, "xyz"))
        assertThat(mapper.apply(elementA, args, values).value).isEqualTo("U")
    }

    @Test
    fun `test ifNPI with invalid NPI and 2 args`() {
        val mapper = IfNPIMapper()
        val elementA = Element("a")
        val args = listOf("a", "NPI")
        val values = listOf(ElementAndValue(elementA, "xyz"))
        assertThat(mapper.apply(elementA, args, values).value).isNull()
    }

    @Test
    fun `test LookupSenderValuesetsMapper`() {
        val table = LookupTable.read("./src/test/resources/metadata/tables/sender_valuesets.csv")
        val schema = Schema(
            "test", topic = "test",
            elements = listOf(
                Element(
                    "pregnant", type = Element.Type.TABLE, table = "sender_valuesets", tableColumn = "result",
                    mapperOverridesValue = true
                )
            )
        )
        val metadata = Metadata(schema = schema, table = table, tableName = "sender_valuesets")
        val indexElement = Element("sender_id")
        val lookupElement = metadata.findSchema("test")?.findElement("pregnant") ?: fail("")
        val mapper = LookupSenderValuesetsMapper()
        val args = listOf("sender_id", "pregnant")
        val elementAndValues = listOf(ElementAndValue(indexElement, "all"), ElementAndValue(lookupElement, "y"))
        assertThat(mapper.valueNames(lookupElement, args)).isEqualTo(listOf("sender_id", "pregnant"))
        assertThat(mapper.apply(lookupElement, args, elementAndValues).value).isEqualTo("77386006")

        val elementAndValuesUNK = listOf(
            ElementAndValue(indexElement, "all"),
            ElementAndValue(lookupElement, "yas queen")
        )
        assertThat(mapper.apply(lookupElement, args, elementAndValuesUNK).value).isNull()
    }

    @Test
    fun `test country mapper`() {
        val mapper = CountryMapper()
        val args = listOf("patient_zip_code")
        val countryElement = Element("patient_country")
        val patientPostalCodeElement = Element("patient_zip_code")
        listOf(ElementAndValue(countryElement, "USA")).also {
            assertThat(mapper.apply(countryElement, args, it).value).isEqualTo("USA")
        }
        listOf(ElementAndValue(countryElement, "MEX")).also {
            assertThat(mapper.apply(countryElement, args, it).value).isEqualTo("MEX")
        }
        listOf(ElementAndValue(countryElement, "CAN")).also {
            assertThat(mapper.apply(countryElement, args, it).value).isEqualTo("CAN")
        }
        listOf(ElementAndValue(countryElement, "USA"), ElementAndValue(patientPostalCodeElement, "90210")).also {
            assertThat(mapper.apply(countryElement, args, it).value).isEqualTo("USA")
        }
        listOf(ElementAndValue(countryElement, "CAN"), ElementAndValue(patientPostalCodeElement, "H0H0H0")).also {
            assertThat(mapper.apply(countryElement, args, it).value).isEqualTo("CAN")
        }
        listOf(ElementAndValue(countryElement, ""), ElementAndValue(patientPostalCodeElement, "90210")).also {
            assertThat(mapper.apply(countryElement, args, it).value).isEqualTo("USA")
        }
        listOf(ElementAndValue(countryElement, ""), ElementAndValue(patientPostalCodeElement, "H0H0H0")).also {
            assertThat(mapper.apply(countryElement, args, it).value).isEqualTo("CAN")
        }
    }
}