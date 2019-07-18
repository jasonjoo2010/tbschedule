<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page isELIgnored="false" %>
<jsp:include page="../header.jsp"/>
<style>
.problem {
    background: #e17760;
}
.invalid {
    background: #A9A9A9;
}
</style>
<c:if test="${fn:length(infoList) == 0}">
任务 ${taskName} 还没有运行期信息
</c:if>
<table border="1" >

    <c:forEach items="${infoList }" var="info" varStatus="s">
    <tr style="background-color:#F3F5F8;color:#013299;">
        <td style="font-size:14px; font-weight:bold">
            Strategy: ${info.taskName } -- ${info.ownSign }
        </td>
    </tr>
    <tr>
        <td>
            <!-- thread groups -->
           <table border="1" style="border-COLLAPSE: collapse;display:block;">
               <tr>
                   <th nowrap>NO</th>
                   <th>ThreadGroup UUID</th>
                   <th>OwnSign</th>
                   <th>IP</th>
                   <th>Hostname</th>
                   <th nowrap>Threads</th>
                   <th>Register</th>
                   <th>Heartbeat</th>
                   <th>Fetch Time</th>   
                   <th nowrap>Version</th>
                   <th nowrap>Schedule</th>
                   <th>Detail</th>
                   <th>Machine</th>
               </tr>
               <c:forEach items="${strategyMap[info.taskName] }" var="g" varStatus="gs">
        	   <tr class="
                    <c:if test="${globalTime - g.heartBeatTime.time > taskMap[info.taskName].judgeDeadInterval}"> invalid</c:if>
                    <c:if test="${g.nextRunStart.time <= now && (g.lastFetchDataTime == null || globalTime - g.lastFetchDataTime.time > taskMap[info.taskName].heartBeatRate * 20)}"> problem</c:if>
                    ">
            	   <td>${gs.index + 1 }</td>
            	   <td nowrap>${g.uuid}</td>
            	   <td>${g.ownSign }</td>	  
            	   <td nowrap>${g.ip }</td>	  
            	   <td nowrap>${g.hostName }</td>
            	   <td>${g.threadNum }</td>	
            	   <td nowrap>${g.registerTime }</td>	
            	   <td nowrap>${g.heartBeatTime }</td>	
            	   <td nowrap>${g.lastFetchDataTime == null ? "--" : g.lastFetchDataTime}</td>		   
            	   <td>${g.version}</td>	
            	   <td nowrap>
                       ${g.nextRunStartTime == null ? "--" : g.nextRunStartTime}<br />
                       ${g.nextRunEndTime == null ? "--" : g.nextRunEndTime}
                   </td>
            	   <td nowrap>${g.dealInfoDesc }</td>	
            	   <td nowrap>${g.managerFactoryUUID }</td>	
        	   </tr>      
               </c:forEach>
           </table> 
        </td>
    </tr>
    <!-- Task Items -->
    <tr>
        <td>
           <table border="1" style="border-COLLAPSE: collapse;display:block;">
               <tr>
                   <th>Task Item</th>
                   <th>Current ThreadGroup</th>
                   <th>Request ThreadGroup</th>
                   <th>Parameter</th>
               </tr>
               <c:forEach items="${itemMap[info.runningEntry] }" var="ti" varStatus="is">
        	   <tr>
        	       <td>${ti.taskItem }</td>
            	   <td>${ti.currentScheduleServer == null ? "--" : ti.currentScheduleServer}</td>	   
            	   <td>${ti.requestScheduleServer == null ? "--" : ti.requestScheduleServer}</td>	   
            	   <td>${ti.dealParameter}</td>
        	   </tr>
               </c:forEach>
           </table> 
        </td>
    </tr>
    </c:forEach>
</table>
<jsp:include page="../footer.jsp"/>