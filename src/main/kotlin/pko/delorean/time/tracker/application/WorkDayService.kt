package pko.delorean.time.tracker.application

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pko.delorean.time.tracker.domain.*
import pko.delorean.time.tracker.infrastructure.JiraService
import pko.delorean.time.tracker.infrastructure.JiraService.ConnectionResult
import pko.delorean.time.tracker.infrastructure.WorkDayJpaRepository
import pko.delorean.time.tracker.kernel.Utils.Companion.formatTime
import pko.delorean.time.tracker.kernel.Utils.Companion.workLogDuration
import pko.delorean.time.tracker.ui.jira.dto.JiraIssueDto
import pko.delorean.time.tracker.ui.work.day.dto.*
import java.time.LocalDate

@Service
class WorkDayService @Autowired constructor(
    private val workDayJpaRepository: WorkDayJpaRepository,
    private val jiraService: JiraService
) {

    @Transactional(readOnly = true)
    fun findWorkDays(createDateStart: LocalDate, createDateEnd: LocalDate): List<WorkDayDto> {
        return workDayJpaRepository.findAllByCreateDateBetween(createDateStart, createDateEnd)
                .map { it.toDto() }
                .sortedByDescending { it.date }
    }

    @Transactional(readOnly = true)
    fun getWorkDay(workDayId: Long): WorkDayDto? =
        workDayJpaRepository.getOne(workDayId).toDto()

    @Transactional(readOnly = true)
    fun findWorkDayBefore(workDayId: Long): WorkDayDto? {
        val workDay = workDayJpaRepository.getOne(workDayId)
        return workDayJpaRepository.findAllWithCreateDateBefore(PageRequest.of(0, 1), workDay.createDate)
            .maxBy { it.createDate }?.toDto()
    }

    @Transactional(readOnly = true)
    fun workLogInConflictIds(workDayId: Long): Set<Long> =
        workDayJpaRepository.getOne(workDayId).workLogInConflictIds()

    @Transactional(readOnly = true)
    fun lastUsedIssues(): List<JiraIssueDto> {
        val issues = workDayJpaRepository.findAll()
            .flatMap { it.workLogs }
            .sortedByDescending { it.started }
            .distinctBy { it.jiraId }
            .map { JiraIssueDto(it.jiraId, it.jiraName, it.comment) }
        return if (issues.size > 100) issues.subList(0, 99) else issues
    }

    @Transactional
    fun addWorkDay(workDayDto: WorkDayDto): Long =
        workDayJpaRepository.saveAndFlush(WorkDay(workDayDto.date))
            .id

    @Transactional
    fun removeWorkDay(workDayId: Long) {
        workDayJpaRepository.deleteAll(
            workDayJpaRepository.findAllById(listOf(workDayId))
        )
    }

    @Transactional
    fun updateWorkDay(workDayId: Long, date: LocalDate) {
        val workDay = workDayJpaRepository.getOne(workDayId)
        workDay.update(date)
        workDayJpaRepository.saveAndFlush(workDay)
    }

    @Transactional
    fun addWorkLog(workDayId: Long, workLogDto: WorkLogDto) {
        val workDay = workDayJpaRepository.getOne(workDayId)
        workDay.addWorkLog(workLogDto)
        workDayJpaRepository.saveAndFlush(workDay)
    }

    @Transactional
    fun editWorkLog(workDayId: Long, workLogDto: WorkLogDto) {
        val workDay = workDayJpaRepository.getOne(workDayId)
        workDay.editWorkLog(workLogDto)
        workDayJpaRepository.saveAndFlush(workDay)
    }

    @Transactional
    fun removeWorkLog(workDayId: Long, workLogId: Long) {
        val workDay = workDayJpaRepository.getOne(workDayId)
        workDay.removeWorkLog(workLogId)
        workDayJpaRepository.saveAndFlush(workDay)
    }

    @Transactional
    fun stopWorklog(workDayId: Long) {
        val workDay = workDayJpaRepository.getOne(workDayId)
        workDay.stopTracking()
        workDayJpaRepository.saveAndFlush(workDay)
    }

    @Transactional
    fun startWorkLog(workDayId: Long, workLogId: Long) {
        val workDay = workDayJpaRepository.getOne(workDayId)
        workDay.startTracking(workLogId)
        workDayJpaRepository.saveAndFlush(workDay)
    }

    @Transactional
    fun continueWorkLog(workDayId: Long, workLogId: Long) {
        val workDay = workDayJpaRepository.getOne(workDayId)
        workDay.continueTracking(workLogId)
        workDayJpaRepository.saveAndFlush(workDay)
    }

    @Transactional
    fun exportWorkDay(workDayId: Long): ConnectionResult<List<Long>> {
        val workDay = workDayJpaRepository.getOne(workDayId)
        workDay.stopTracking()
        val exportStatus = jiraService.exportWorkDay(workDay.unexportedWorkLogs)
        workDay.markExported(exportStatus.value.orEmpty())
        workDayJpaRepository.saveAndFlush(workDay)
        return exportStatus
    }

    @Transactional
    fun toggleExport(workDayId: Long, workLogId: Long) {
        val workDay = workDayJpaRepository.getOne(workDayId)
        workDay.toggleExport(workLogId)
        workDayJpaRepository.saveAndFlush(workDay)
    }

    fun calculateStatistics(workDays: List<WorkDayDto>): WorkDaysPeriodStatisticsDto {
        val statistics = workDays.flatMap { it.projectsStatistics.orEmpty() }
            .groupBy { it.projectKey }
            .map { it.key to it.value.map { projectStatistics ->  projectStatistics.duration }.sum() }
            .toMap()
        return WorkDaysPeriodStatisticsDto(statistics, statistics.map { it.value }.sum())
    }

    private fun WorkDay.toDto() =
        WorkDayDto(
            id,
            createDate,
            status.name,
            duration,
            statistics.map { it.toDto() },
            summary.map { it.toDto() },
            workLogs
                .sortedBy { it.started }
                .map { it.toDto(this) }
        )

    private fun WorkLog.toDto(workDay: WorkDay) =
        WorkLogDto(
            id,
            jiraId,
            formatTime(started),
            formatTime(ended),
            workLogDuration(this, workDay.createDate),
            jiraName,
            comment,
            status.name
        )

    private fun IssueSummary.toDto() =
        IssueSummaryDto(jiraId, jiraNames, comments)

    private fun ProjectStatistics.toDto() =
        ProjectStatisticsDto(projectKey, duration, issuesStatistics.map { it.toDto() })

    private fun IssueStatistics.toDto() =
        IssueStatisticsDto(issueKey, duration, jiraNames)
}
