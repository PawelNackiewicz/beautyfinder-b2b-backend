package com.beautyfinder.b2b.domain.salon

data class Address(
    val street: String,
    val city: String,
    val postalCode: String,
    val country: String = "PL",
) {
    fun formatted(): String = "$street, $postalCode $city"

    companion object {
        private val PL_POSTAL_CODE_REGEX = Regex("^\\d{2}-\\d{3}$")

        fun validate(street: String, city: String, postalCode: String, country: String = "PL"): Result<Unit> {
            if (street.isBlank()) return Result.failure(IllegalArgumentException("Street cannot be blank"))
            if (city.isBlank()) return Result.failure(IllegalArgumentException("City cannot be blank"))
            if (country == "PL" && !PL_POSTAL_CODE_REGEX.matches(postalCode)) {
                return Result.failure(IllegalArgumentException("Invalid Polish postal code format. Expected: dd-ddd"))
            }
            return Result.success(Unit)
        }
    }
}
