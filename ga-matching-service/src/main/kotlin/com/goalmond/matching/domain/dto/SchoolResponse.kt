package com.goalmond.matching.domain.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "학교 상세 정보")
data class SchoolResponse(
    @field:Schema(description = "학교 ID", example = "school-001")
    val id: String,
    @field:Schema(description = "학교명", example = "Irvine Valley College")
    val name: String,
    @field:Schema(description = "유형", example = "community_college")
    val type: String,
    @field:Schema(description = "주", example = "CA")
    val state: String,
    @field:Schema(description = "도시", example = "Irvine")
    val city: String,
    @field:Schema(description = "학비", example = "18000")
    val tuition: Int,
    @field:Schema(description = "생활비", example = "15000")
    val livingCost: Int,
    @field:Schema(description = "랭킹", example = "15")
    val ranking: Int,
    @field:Schema(description = "설명")
    val description: String,
    @field:Schema(description = "캠퍼스 정보")
    val campusInfo: String,
    @field:Schema(description = "기숙사 여부", example = "false")
    val dormitory: Boolean,
    @field:Schema(description = "식당 여부", example = "true")
    val dining: Boolean,
    @field:Schema(description = "프로그램 목록")
    val programs: List<ProgramSummary>,
    @field:Schema(description = "합격률", example = "45")
    val acceptanceRate: Int,
    @field:Schema(description = "편입률", example = "75")
    val transferRate: Int,
    @field:Schema(description = "졸업률", example = "68")
    val graduationRate: Int,
    @field:Schema(description = "이미지 URL 목록")
    val images: List<String>,
    @field:Schema(description = "웹사이트", example = "https://www.ivc.edu")
    val website: String,
    @field:Schema(description = "연락처")
    val contact: Contact
) {
    @Schema(description = "프로그램 요약")
    data class ProgramSummary(
        @field:Schema(description = "프로그램 ID", example = "program-001")
        val id: String,
        @field:Schema(description = "프로그램명", example = "Computer Science AA")
        val name: String,
        @field:Schema(description = "학위", example = "AA")
        val degree: String,
        @field:Schema(description = "기간", example = "2 years")
        val duration: String
    )

    @Schema(description = "연락처 정보")
    data class Contact(
        @field:Schema(description = "이메일", example = "admissions@ivc.edu")
        val email: String,
        @field:Schema(description = "전화번호", example = "+1-949-451-5100")
        val phone: String,
        @field:Schema(description = "주소", example = "5500 Irvine Center Dr, Irvine, CA 92618")
        val address: String
    )
}
