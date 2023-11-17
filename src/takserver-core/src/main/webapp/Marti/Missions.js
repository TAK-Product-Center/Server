$.urlParam = function(name){
    var results = new RegExp('[\?&]' + name + '=([^&#]*)').exec(window.location.href);
    if (results==null){
        return null;
    }
    else{
        return decodeURI(results[1]) || 0;
    }
}

function clearTable()
{
    var tbl = document.getElementById('missionTable');
    var iNitialLength = tbl.rows.length;
    for (var iNdx=0; iNdx < iNitialLength  - 1; iNdx++)
    {
        tbl.deleteRow(1);
    }
}

function sanitize(str) {
     var temp = document.createElement('div');
     temp.textContent = str;
     return temp.innerHTML;
}

function createDiv(text, id) {

    var div = document.createElement('div');
    if (id != undefined) {
        div.id = id;
    }

    div.innerHTML = text;
    return div;
}

function createContents(contents) {
    var html = "";
    for (var i = 0; i < contents.length; i++) {
        if (i!=0) html += "<br>";
        html += "<a href=\"sync/content?hash=" + encodeURI(contents[i].data.hash)+ "\">" + sanitize(contents[i].data.name) + "</a>";
    }
    return html;
}

function createUids(uids) {
    var html = "";
    for (var i = 0; i < uids.length; i++) {
        if (i!=0) html += "<br>";
        html += "<a href=\"api/cot/xml/" + encodeURI(uids[i].data) + "\">" + sanitize(uids[i].data) + "</a>"
    }
    return html;
}

function displayInUnits ( seconds ) {
   var returntext = '';
   if ( seconds === undefined ) {
     return returntext;
   }
   function numberEnding (number) {
      return (number > 1) ? 's' : '';
    }

    var years = Math.floor(seconds / 31536000);
    if (years) {
        return years + ' year' + numberEnding(years);
    }
    //TODO: Months! Maybe weeks?
    var days = Math.floor((seconds %= 31536000) / 86400);
    if (days) {
        return days + ' day' + numberEnding(days);
    }
    var hours = Math.floor((seconds %= 86400) / 3600);
    if (hours) {
        return hours + ' hour' + numberEnding(hours);
    }
    var minutes = Math.floor((seconds %= 3600) / 60);
    if (minutes) {
        return minutes + ' minute' + numberEnding(minutes);
    }
    var seconds = seconds % 60;
    if (seconds) {
        return seconds + ' second' + numberEnding(seconds);
    }
    return 'less than a second'; //'just now' //or other string you like;
}

function createGroups(groups) {
    var html = "";
    for (var i = 0; i < groups.length - 1; i++) {
        html += sanitize(groups[i]);
        html += ", ";
    }
    html += sanitize(groups[groups.length - 1])
    return html;
}

function createFeeds(feeds) {
    var html = "";
    for (var i = 0; i < feeds.length; i++) {
        if (i!=0) html += "<br>";
        // use datafeed UID to get datafeed's url/info to encode the url
        html +=  "<a href=\"inputs/index.html#!/modifyPluginDataFeed/" + feeds[i].name + "\">";
        html += sanitize(feeds[i].name) + "</a></li>";
    }
    return html;
}

function createKeywords(keywords) {
    var html = "";
    for (var i = 0; i < keywords.length; i++) {
        if (i!=0) html += " ";
        html += sanitize(keywords[i]);
    }
    return html;
}

function addCell(row, index, child, align) {
    var cell = row.insertCell(index);
    cell.style.verticalAlign=align;
    cell.appendChild(child);
}

function createMissionTable(missions) {
    var tbl = document.getElementById('missionTable');
    var rowNumber = 0;
    for (var i = 0; i < missions.length; i++) {
        var row = tbl.insertRow(rowNumber + 1);
        rowNumber++;

        var cell = 0;

        addCell(row, cell++, createDiv("<a href=\"api/missions/" +  encodeURI(missions[i].name) + "\">" + sanitize(missions[i].name) + "</a>"), 'top');
        addCell(row, cell++, document.createTextNode(missions[i].description), 'top');
        addCell(row, cell++, createDiv(createContents(missions[i].contents)), 'top');
        addCell(row, cell++, createDiv(createUids(missions[i].uids)), 'top');
		addCell(row, cell++, createDiv(createGroups(missions[i].groups)), 'top');
        addCell(row, cell++, createDiv(createFeeds(missions[i].feeds)), 'top');
        addCell(row, cell++, createDiv(createKeywords(missions[i].keywords)), 'top');
        addCell(row, cell++, document.createTextNode(missions[i].creatorUid), 'top');
        addCell(row, cell++, document.createTextNode(missions[i].createTime), 'top');
        addCell(row, cell++, document.createTextNode(displayInUnits(missions[i].expiration)), 'top');

        var controls =
            "<input type=\"button\" id=\"edit\" value=\"Edit\" onClick=\"window.location.href='MissionEditor.jsp?name=" + encodeURI(missions[i].name) + "';\"/>&nbsp;" +
            //"<input type=\"button\" id=\"invite\" value=\"Invite\" onClick=\"window.location.href='MissionInvite.jsp?name=" + missions[i].name + "';\"/>&nbsp;" +
            "<input type=\"button\" id=\"delete\" value=\"Delete\" onClick=\"deleteMission('" + sanitize(missions[i].name) + "');\"/>";
        addCell(row, cell++, createDiv(controls), 'top');
    }
}

function getMissions() {
    $.ajax({
        url  : "api/missions",
        type : "GET",
        data: { 
            passwordProtected: "true", 
            defaultRole: "true"
        },
        async : false,
        cache : false,
        contentType : false,
        processData : true,
        success: function (response) {
            createMissionTable(response.data);
            document.getElementById('loadingDiv').style.display = 'none';
        },
        error : function(stat, err) {
            $.jnotify("Error getting missions", "error");
        }
    });
}

