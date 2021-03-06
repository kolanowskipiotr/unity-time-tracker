<#-- @ftlvariable name="templates" type="pko.delorean.time.tracker.ui.work.day.dto.WorkDayDto[]" -->
<#-- @ftlvariable name="template" type="pko.delorean.time.tracker.ui.work.day.dto.WorkDayDto" -->

<#include "/header.ftl">
<#include "/macros/issue-utils.ftl">
<#include "/macros/project-utils.ftl">
<#include "/macros/duration-utils.ftl">
<#include "/macros/enum-utils.ftl">

<@header "Work days"/>

<div class="container" >
    <h1>Work days</h1>
    <nav class="navbar navbar-light bg-light justify-content-between" style="background-color: #e3f2fd;">
        <a class="navbar-brand">Navigation</a>
        <a class="btn btn-outline-success" href="/jira/credentials/edit" role="button">🔑 Jira credentials</a>
        <a class="btn btn-outline-secondary" href="/h2-console" role="button">🗄️ H2 database console</a>
    </nav>
    <div class="card">
        <div class="card-body">
            <form action="/work-day/add" method="post">
                <div class="form-inline">
                    <div class="form-group mb-2 w-75">
                        <label for="date" class="control-label col-sm-2" >Date: </label>
                        <input type="date" class="form-control" id="date" name="date" placeholder="dd.MM.yyyy" required="required"/>
                    </div>
                </div>
                <button type="submit" class="btn btn-primary mb-2">➕ New work day</button>
            </form>
        </div>
    </div>

    <div class="card">
        <div class="card-body">
            <form action="/work-day/list" method="get">
                <div class="form-inline">
                    <div class="form-group mb-2 w-75">
                        <label for="date" class="control-label col-sm-2" >Created from: </label>
                        <input type="date" class="form-control" id="createDateStart" name="createDateStart" placeholder="dd.MM.yyyy" value="${filters.createDateStart!}"/>
                        <label for="date" class="control-label col-sm-2" >Created to: </label>
                        <input type="date" class="form-control" id="createDateEnd" name="createDateEnd" placeholder="dd.MM.yyyy" value="${filters.createDateEnd!}"/>
                    </div>
                </div>
                <div class="form-inline">
                    <div class="form-group">
                        <button type="submit" class="btn btn-primary mb-2">🔍 Filter</button>
                        <a class="mb-2 ml-2 btn btn-warning" href="/work-day/list" role="button">🧹 Clear</a>
                    </div>
                </div>
            </form>
        </div>
    </div>

    <div class="card">
        <div class="card-body">
            <table class="table table-striped">
                <thead class="thead-dark">
                <tr>
                    <th></th>
                    <th>#</th>
                    <th>Day</th>
                    <th>Duration</th>
                    <th>Statistics</th>
                    <th>State</th>
                    <th>Actions</th>
                </tr>
                </thead>
                <tbody>
                <#list workDays as workDay>
                    <tr>
                        <td class="<#if workDay.state == "EXPORTED">bg-success text-white <#elseif workDay.state == "IN_PROGRSS">bg-primary text-white</#if>" ></td>
                        <td>${workDay_index +1}</td>
                        <td>${workDay.date?html}</td>
                        <td class="text-center"><#if workDay.duration??>${workDay.duration}m<br>(${(workDay.duration/60)?floor}h ${workDay.duration - ((workDay.duration/60)?floor * 60)}m)</#if> </td>
                        <td>
                            <#if workDay.statistics??>
                                <ul style="list-style-type:none;">
                                    <li>
                                        <#assign privateTimeDuration=workDay.statistics.privateTime.duration/>
                                        <@prityName workDay.statistics.privateTime.issueKey/> - <#if privateTimeDuration?? && workDay.duration gt 0> ${privateTimeDuration/workDay.duration*100}% - ${privateTimeDuration}m (${(privateTimeDuration/60)?floor}h ${privateTimeDuration - ((privateTimeDuration/60)?floor * 60)}m)<#else>0% - 0m (0h 0m)</#if>
                                    </li>
                                    <#list workDay.statistics.projectsStatistics as projectStatistics>
                                        <li>
                                            <#assign projectDuration=projectStatistics.duration/>
                                            ${projectStatistics.projectKey} - <#if projectDuration?? && workDay.duration gt 0> ${projectDuration/workDay.duration*100}% - ${projectDuration}m (${(projectDuration/60)?floor}h ${projectDuration - ((projectDuration/60)?floor * 60)}m)<#else>0% - 0m (0h 0m)</#if>
                                        </li>
                                    </#list>
                                </ul>
                            </#if>
                        </td>
                        <td><@stateIcon workDay.state!?html/></td>
                        <td>
                            <div class="btn-toolbar" role="toolbar">
                                <div class="btn-group mr-2" role="group">
                                    <a class="btn btn-primary" href="/work-day/edit?workDayId=${workDay.id?c}" role="button">✏️ Edit</a>
                                </div>
                                <div class="btn-group mr-2" role="group">
                                    <@action "workDayId" "${workDay.id?c}" "" ""  "btn btn-danger" "/work-day/delete" "🗑️ Delete" />
                                </div>
                            </div>
                        </td>
                    </tr>
                </#list>
                </tbody>
            </table>
        </div>
    </div>

    <div class="card">
        <ul class="card-body">
            <h3 class="card-title">Statistics:</h3>

            <ul class="list-group">
                <div class="list-group-item list-group-item-action flex-column align-items-start active py-2">
                    <div class="d-flex w-100 justify-content-between">
                        <h5 class="mb-1">Period ${filters.createDateStart} - ${filters.createDateEnd} </h5>
                        <small>
                            <@duraton periodStatistics.fullDuration periodStatistics.fullDuration/>
                        </small>
                    </div>
                </div>
                <#if periodStatistics.projectsStatistics??>
                    <@projectsStatistics periodStatistics periodStatistics.fullDuration/>
                </#if>
            </ul>
        </div>
    </div>
</div> <!-- /container -->
<#include "/footer.ftl">