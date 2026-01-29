package com.goalmond.api.domain.dto

data class SchoolResponse(
    val id: String,
    val name: String,
    val type: String,
    val state: String,
    val city: String,
    val tuition: Int,
    val livingCost: Int,
    val ranking: Int,
    val description: String,
    val campusInfo: String,
    val dormitory: Boolean,
    val dining: Boolean,
    val programs: List<ProgramSummary>,
    val acceptanceRate: Int,
    val transferRate: Int,
    val graduationRate: Int,
    val images: List<String>,
    val website: String,
    val contact: Contact
) {
    data class ProgramSummary(
        val id: String,
        val name: String,
        val degree: String,
        val duration: String
    )

    data class Contact(
        val email: String,
        val phone: String,
        val address: String
    )
}
