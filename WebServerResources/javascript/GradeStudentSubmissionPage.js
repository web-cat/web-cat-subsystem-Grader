/* Custom functions for COMTOR report support */

var disputeWarningSeen = false;

function submitWord(submittedWord, reportName, elementID)
{
    var element = document.getElementById(elementID);
    if (submittedWord && reportName && element)
    {
        var request = new XMLHttpRequest();
        request.open("GET",
            "http://cloud.comtor.org/wordSubmitDev/wordproblem?submittedWord="
            + submittedWord + "&reportName=" + reportName,
            true);
        request.send();
        var newClass = "";
        element.className = newClass.concat(element.className, " disabled");
        element.href = "javascript:void";
        if (disputeWarningSeen == false)
        {
            disputeWarningSeen = true;
//			$('#modalWarning').modal('show');
            webcat.alert({'title': 'A note about disputes ...',
                'message':'<img src="http://www.comtor.org/website/images/comtor/comtorLogo.png" class="pull-left" style="float:left" alt="COMTOR logo" width="100px"></img><p>Please note that each dispute results in an email being sent to the development team.</p><p>Dispute decisions are not automatic and a development team member will review the dispute and adjudicate any spelling/offensive word changes to the system as soon as possible. Please do not rerun the report and expect to see the dispute resolved immediately. Dispute adjudication decisions are not currently emailed to you, but we may consider doing this in the future.</p><p>We appreciate your feedback and welcome your input to make COMTOR the best possible system.</p>'});
        }
    }
}

function unhideContent(spanID)
{
    var preID = spanID + 'Pre';
    var postID = spanID + 'Post';
    var buttonID = spanID + 'Button';

    var preElement = document.getElementById(preID);
    if (preElement)
    {
        preElement.className = (preElement.className == 'hidden')
            ? 'unhidden' : 'hidden';
    }

    var postElement = document.getElementById(postID);
    if (postElement)
    {
        postElement.className = (postElement.className == 'hidden')
            ? 'unhidden' : 'hidden';
    }

    var buttonElement = document.getElementById(buttonID);
    if (buttonElement)
    {
        buttonElement.innerHTML =
            (buttonElement.innerHTML == 'Display pre-/post-analysis content')
            ? 'Hide pre-/post-analysis content'
            : 'Display pre-/post-analysis content';
    }
}

function unhideReport(reportID)
{
    var report = document.getElementById(reportID);
    if (report)
    {
        report.className = (report.className == 'hidden')
            ? 'unhidden' : 'hidden';
    }

    var buttonID = reportID + 'Button';
    var buttonElement = document.getElementById(buttonID);
    if (buttonElement)
    {
        buttonElement.innerHTML =
            (buttonElement.innerHTML == 'Display report')
            ? 'Hide report' : 'Display report';
    }
}
