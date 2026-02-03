package com.goalmond.api.service.matching

import com.goalmond.api.domain.dto.FilterType
import com.goalmond.api.domain.entity.AcademicProfile
import com.goalmond.api.domain.entity.Program
import com.goalmond.api.domain.entity.School
import com.goalmond.api.domain.entity.UserPreference
import com.goalmond.api.repository.ProgramRepository
import com.goalmond.api.repository.SchoolRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal

/**
 * HardFilterService 단위 테스트 (GAM-3, Phase 4).
 * 
 * 테스트 목표:
 * 1. 예산 초과 프로그램 필터링
 * 2. 영어 점수 미달 필터링
 * 3. 비자 요건 필터링
 * 4. 4가지 필터 모두 적용
 * 5. 필터링 사유 명확한 로깅
 */
@SpringBootTest
@ActiveProfiles("local")
class HardFilterServiceTest {
    
    @Autowired
    private lateinit var hardFilterService: HardFilterService
    
    @Autowired
    private lateinit var schoolRepository: SchoolRepository
    
    @Autowired
    private lateinit var programRepository: ProgramRepository
    
    private lateinit var testSchool: School
    
    @BeforeEach
    fun setUp() {
        // 테스트용 School 생성
        testSchool = School(
            name = "Test School for Filter",
            type = "community_college",
            state = "CA",
            city = "Test City",
            tuition = 18000,
            livingCost = 12000
        )
        testSchool = schoolRepository.save(testSchool)
    }
    
    @Test
    fun `예산 초과 프로그램 필터링`() {
        // Given: 예산 25000, 프로그램 학비 18000 + 생활비 12000 = 30000 (초과)
        val preference = UserPreference(
            budgetUsd = 25000
        )
        val program = Program(
            schoolId = testSchool.id,
            name = "CS Program",
            type = "community_college",
            tuition = 18000
        )
        val savedProgram = programRepository.save(program)
        
        // When
        val result = hardFilterService.filterPrograms(
            profile = AcademicProfile(degree = "대학교"),
            preference = preference,
            programs = listOf(savedProgram)
        )
        
        // Then
        assertThat(result.passed).isEmpty()
        assertThat(result.filtered).hasSize(1)
        assertThat(result.filtered[0].filterType).isEqualTo(FilterType.BUDGET_EXCEEDED)
        assertThat(result.filtered[0].reason).contains("예산 초과")
    }
    
    @Test
    fun `예산 여유 있는 프로그램 통과`() {
        // Given: 예산 35000, 총 비용 30000 (통과)
        val preference = UserPreference(
            budgetUsd = 35000
        )
        val program = Program(
            schoolId = testSchool.id,
            name = "CS Program",
            type = "community_college",
            tuition = 18000
        )
        val savedProgram = programRepository.save(program)
        
        // When
        val result = hardFilterService.filterPrograms(
            profile = AcademicProfile(degree = "대학교"),
            preference = preference,
            programs = listOf(savedProgram)
        )
        
        // Then
        assertThat(result.passed).hasSize(1)
        assertThat(result.filtered).isEmpty()
    }
    
    @Test
    fun `초등 프로그램은 비자 요건으로 필터링`() {
        // Given
        val program = Program(
            schoolId = testSchool.id,
            name = "Elementary Program",
            type = "elementary"
        )
        val savedProgram = programRepository.save(program)
        
        // When
        val result = hardFilterService.filterPrograms(
            profile = AcademicProfile(degree = "대학교"),
            preference = UserPreference(),
            programs = listOf(savedProgram)
        )
        
        // Then
        assertThat(result.filtered).hasSize(1)
        assertThat(result.filtered[0].filterType).isEqualTo(FilterType.VISA_REQUIREMENT)
    }
    
    @Test
    fun `영어 점수 미달 프로그램 필터링`() {
        // Given: TOEFL 50, CC 요구 61 (미달)
        val profile = AcademicProfile(
            schoolName = "Test School",
            degree = "대학교",
            englishTestType = "TOEFL",
            englishScore = 50
        )
        val program = Program(
            schoolId = testSchool.id,
            name = "CS Program",
            type = "community_college"
        )
        val savedProgram = programRepository.save(program)
        
        // When
        val result = hardFilterService.filterPrograms(
            profile = profile,
            preference = UserPreference(),
            programs = listOf(savedProgram)
        )
        
        // Then
        assertThat(result.filtered).hasSize(1)
        assertThat(result.filtered[0].filterType).isEqualTo(FilterType.ENGLISH_SCORE)
        assertThat(result.filtered[0].reason).contains("영어 점수 미달")
    }
    
    @Test
    fun `영어 점수 충분한 프로그램 통과`() {
        // Given: TOEFL 85, CC 요구 61 (통과)
        val profile = AcademicProfile(
            schoolName = "Test School",
            degree = "대학교",
            englishTestType = "TOEFL",
            englishScore = 85
        )
        val program = Program(
            schoolId = testSchool.id,
            name = "CS Program",
            type = "community_college"
        )
        val savedProgram = programRepository.save(program)
        
        // When
        val result = hardFilterService.filterPrograms(
            profile = profile,
            preference = UserPreference(),
            programs = listOf(savedProgram)
        )
        
        // Then
        assertThat(result.passed).hasSize(1)
        assertThat(result.filtered).isEmpty()
    }
    
    @Test
    fun `IELTS 점수를 TOEFL로 환산하여 필터링`() {
        // Given: IELTS 5.5 = TOEFL 46, Vocational 요구 45 (통과)
        val profile = AcademicProfile(
            schoolName = "Test School",
            degree = "대학교",
            englishTestType = "IELTS",
            englishScore = 6 // IELTS 6.0 = TOEFL 60
        )
        val program = Program(
            schoolId = testSchool.id,
            name = "Vocational Program",
            type = "vocational" // 요구 TOEFL 45
        )
        val savedProgram = programRepository.save(program)
        
        // When
        val result = hardFilterService.filterPrograms(
            profile = profile,
            preference = UserPreference(),
            programs = listOf(savedProgram)
        )
        
        // Then
        assertThat(result.passed).hasSize(1) // IELTS 6.0 = TOEFL 60 > 45
    }
    
    @Test
    fun `4가지 필터 혼합 시나리오`() {
        // Given: 4개 프로그램
        val programs = listOf(
            // 1. 예산 초과
            Program(
                schoolId = testSchool.id,
                name = "Expensive Program",
                type = "university",
                tuition = 30000 // 학비 30000 + 생활비 12000 = 42000
            ),
            // 2. 초등 프로그램
            Program(
                schoolId = testSchool.id,
                name = "Elementary Program",
                type = "elementary"
            ),
            // 3. 영어 미달 (University는 TOEFL 80 요구)
            Program(
                schoolId = testSchool.id,
                name = "University Program",
                type = "university",
                tuition = 20000
            ),
            // 4. 통과
            Program(
                schoolId = testSchool.id,
                name = "Good Program",
                type = "community_college",
                tuition = 15000
            )
        ).map { programRepository.save(it) }
        
        val profile = AcademicProfile(
            schoolName = "Test",
            degree = "대학교",
            englishTestType = "TOEFL",
            englishScore = 70 // TOEFL 70: CC 통과(61), University 미달(80)
        )
        val preference = UserPreference(
            budgetUsd = 35000
        )
        
        // When
        val result = hardFilterService.filterPrograms(profile, preference, programs)
        
        // Then
        assertThat(result.passed).hasSize(1) // Good Program만 통과
        assertThat(result.filtered).hasSize(3)
        
        // 필터 유형별 확인
        val filterTypes = result.filtered.map { it.filterType }
        assertThat(filterTypes).contains(FilterType.BUDGET_EXCEEDED)
        assertThat(filterTypes).contains(FilterType.VISA_REQUIREMENT)
        assertThat(filterTypes).contains(FilterType.ENGLISH_SCORE)
    }
}
