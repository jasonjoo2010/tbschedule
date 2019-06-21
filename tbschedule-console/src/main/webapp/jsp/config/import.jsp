<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page isELIgnored="false" %>
<jsp:include page="../header.jsp"/>
<form id="frmImport" method="POST" action="importSave">
    <label style="margin: 0; width: 150px; float: left;">Config file content</label>
    <textarea name="content" style="width: 600px; height: 200px;"></textarea><br />
    <label style="margin: 0; width: 150px; float: left;">Update Forcely</label> 
    <select name="force">
    	<option value="true">Yes</option>
    	<option value="false" selected>No</option>
    </select>
    <input type="submit" value="Import" />
</form>
<h3 style="color: red"></h3>
<script type="text/javascript">
$(function() {
    $("#frmImport").ajaxForm({
        dataType : 'json',
        beforeserialize: function() {
	        $("h3").html('');
        },
        success: function(data) {
            if (data.errno == 0) {
    	        alert("Config updated");
    	    } else {
    	        $("h3").html(data.errdesc);
    	    }
        }
    });
});
</script>
<jsp:include page="../footer.jsp"/>