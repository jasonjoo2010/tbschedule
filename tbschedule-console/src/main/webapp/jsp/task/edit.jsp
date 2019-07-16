<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page isELIgnored="false" %>
<jsp:include page="../header.jsp"/>
<form id="frmTask" method="get" action="save">
<input type="hidden" name="isCreate" value="${isCreate }" />
<table>
<tr>
	<td>Task Name:</td>
    <td>
        <input type="text" id="taskName" name="taskName"<c:if test="${!isCreate }"> style="background-color: #ddd" readonly="readonly"</c:if> value="${task.name}" width="30">
    </td>
	<td>SpringBean Name:</td>
    <td><input type="text" id="dealBean" name="dealBean" value="${task.dealBeanName}" width="30"></td>
</tr>
<tr>
	<td>Hearbeat Interval(sec):</td>
    <td><input type="text" name="heartBeatRate" value="${task.heartBeatRate/1000.0}" width="30"></td>
	<td>Dead After(sec):</td>
    <td><input type="text" name="judgeDeadInterval" value="${task.judgeDeadInterval/1000.0}" width="30"></td>
</tr>
<tr>
	<td>Threads: </td>
    <td><input type="text" name="threadNumber" value="${task.threadNumber}"  width="30"></td>
	<td>Mode: </td>
    <td>
        <input type="text" name="processType" value="${task.processorType}" width="30">
		SLEEP or NOTSLEEP
    </td>
</tr>
<tr>
	<td>Fetch Count：</td><td><input type="text" name="fetchNumber" value="${task.fetchDataNumber}" width="30"></td>
	<td>Batch：</td><td><input type="text" name="executeNumber" value="${task.executeNumber}" width="30">
		For beans implemented <b>IScheduleTaskDealMulti</b></td>
</tr>
<tr>
	<td>Sleep When No Fetched(sec)：</td>
    <td><input type="text" name="sleepTimeNoData" value="${task.sleepTimeNoData/1000.0}" width="30"></td>
	<td>Sleep After Fetch(sec)：</td>
    <td><input type="text" name="sleepTimeInterval" value="${task.sleepTimeInterval/1000.0}" width="30"></td>
</tr>
<tr>
	<td>Schedule Begin: </td>
    <td><input type="text" name="permitRunStartTime" value="${task.permitRunStartTime}" width="30"></td>
	<td>Schedule End: </td>
    <td><input type="text" name="permitRunEndTime" value="${task.permitRunEndTime}" width="30"></td>
</tr>
<tr>
	<td>Items in ThreadGroup</td>
    <td><input type="text" name="maxTaskItemsOfOneThreadGroup" value="${task.maxTaskItemsOfOneThreadGroup}" width="30"></td>
	<td colspan="2">The maximum count of single <b>ThreadGroup</b> could be assigned. Avoiding overload in rare conditions. 0 indicating no limit.</td>
</tr>
<tr>
	<td>Parameter(string):</td>
    <td colspan="3"><input type="text" id="taskParameter" name="taskParameter" value="${task.taskParameter}" style="width:657"></td>
</tr>
<tr>
	<td>Task Items<br />("," separated):</td>
    <td colspan="3"><textarea  rows="5" id="taskItems" name="taskItems" style="width:657">${fn:join(task.taskItems, ",")}</textarea></td>
</tr>

</table>
<br/>
<input type="submit" value="Save" style="width:100px" >

</form>
<b>执行开始时间说明：</b><br/>
1.允许执行时段的开始时间crontab的时间格式.'0 * * * * ?'  表示在每分钟的0秒开始<br/>
2.以startrun:开始，则表示开机立即启动调度.<br/>
3.格式参见： http://dogstar.javaeye.com/blog/116130<br/><br/>
<b>执行结束时间说明：</b><br/>
1.允许执行时段的结束时间crontab的时间格式,'20 * * * * ?'  表示在每分钟的20秒终止<br/>
2.如果不设置，表示取不到数据就停止 <br/>
3.格式参见：http://dogstar.javaeye.com/blog/116130<br/><br/>
<b>任务项的说明：</b><br/>
1、将一个数据表中所有数据的ID按10取模，就将数据划分成了0、1、2、3、4、5、6、7、8、9供10个任务项。<br/>
2、将一个目录下的所有文件按文件名称的首字母(不区分大小写)， 就划分成了A、B、C、D、E、F、G、H、I、J、K、L、M、N、O、P、Q、R、S、T、U、V、W、X、Y、Z供26个任务项。<br/>
3、将一个数据表的数据ID哈希后按1000取模作为最后的HASHCODE,我们就可以将数据按[0,100)、[100,200) 、[200,300)、[300,400) 、[400,500)、[500,600)、[600,700)、[700,800)、[800,900)、 [900,1000)划分为十个任务项，
	当然你也可以划分为100个任务项，最多是1000个项。<br/>
4、任务项是进行任务分配的最小单位。一个任务队列只能由一个ScheduleServer来进行处理。但一个Server可以处理任意数量的任务项。

<script>
$(function() {
    $("#frmTask").ajaxForm({
        dataType : 'json',
        beforeserialize : function () {
            var taskName = $("#taskName").val();
        	var reg = /.*[\u4e00-\u9fa5]+.*$/; 
        	if(reg.test(taskName)){
        	   alert('任务类型不能含中文');
        	   return false;
        	}
        	if (taskName==null || taskName=='' || isContainSpace(taskName)) {
        		alert('任务类型不能为空或存在空格');
        		return false;
        	}
        	var str = $("#dealBean").val();
        	if (str == null || str.length == 0) {
        		alert("Bean name can't be empty");
        		return false;
        	}
        	if (isContainSpace(str)) {
        		alert("Bean name can't include spaces");
        		return false;
        	}
        	if (reg.test(str)) {
        	   alert('Bean name not allowed');
        	   return false;
        	}
            str = document.all("taskItems").value;
        	if (str == null || str.length == 0) {
        		alert("Task Items can't be empty");
        		return false;
        	}
        	return true;
        },
        success : function(data) {
            if (data.errno == 0) {
                parent.location.reload();
            } else {
                alert(data.errdesc);
            }
        }
    });
});

function isContainSpace(array) {   
	if (array.indexOf(' ') >= 0) {
		return true;
	}
    return false;
}
</script>
<jsp:include page="../footer.jsp"/>