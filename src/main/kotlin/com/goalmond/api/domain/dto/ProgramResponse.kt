package com.goalmond.api.domain.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "프로그램 정보")
data class ProgramResponse(
    @field:Schema(description = "프로그램 ID", example = "program-001")
    val id: String,
    @field:Schema(description = "학교 ID", example = "school-001")
    val schoolId: String,
    @field:Schema(description = "학교명", example = "Irvine Valley College")
    val schoolName: String,
    @field:Schema(description = "프로그램명", example = "Computer Science AA")
    val programName: String,
    @field:Schema(description = "유형", example = "community_college")
    val type: String,
    @field:Schema(description = "학위", example = "AA")
    val degree: String,
    @field:Schema(description = "기간", example = "2 years")
    val duration: String,
    @field:Schema(description = "학비", example = "18000")
    val tuition: Int,
    @field:Schema(description = "주", example = "CA")
    val state: String,
    @field:Schema(description = "도시", example = "Irvine")
    val city: String,
    @field:Schema(description = "OPT 가능 여부", example = "true")
    val optAvailable: Boolean,
    @field:Schema(description = "편입률", example = "75")
    val transferRate: Int,
    @field:Schema(description = "커리어 경로", example = "Software Developer, Web Developer")
    val careerPath: String
)

@Schema(description = "프로그램 목록 응답")
data class ProgramListResponse(
    @field:Schema(description = "전체 개수", example = "45")
    val total: Int,
    @field:Schema(description = "페이지 번호", example = "1")
    val page: Int,
    @field:Schema(description = "페이지 크기", example = "10")
    val size: Int,
    @field:Schema(description = "프로그램 목록")
    val programs: List<ProgramResponse>
)
