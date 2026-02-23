package com.beautyfinder.b2b.domain.billing

import java.util.UUID

data class InvoiceNumber(val value: String) {

    companion object {
        private val FORMAT_REGEX = Regex("""^BF/\d{4}/\d{2}/\d{3}$""")

        fun generate(salonId: UUID, year: Int, month: Int, sequence: Int): InvoiceNumber {
            require(year in 2020..2100) { "Year must be between 2020 and 2100" }
            require(month in 1..12) { "Month must be between 1 and 12" }
            require(sequence in 1..999) { "Sequence must be between 1 and 999" }
            return InvoiceNumber("BF/%d/%02d/%03d".format(year, month, sequence))
        }

        fun parse(value: String): InvoiceNumber {
            require(FORMAT_REGEX.matches(value)) { "Invalid invoice number format: $value. Expected: BF/YYYY/MM/NNN" }
            return InvoiceNumber(value)
        }
    }
}
