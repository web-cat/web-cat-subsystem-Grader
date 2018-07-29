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


function SubmissionEnergyBar(domSelector) {
    var element = document.querySelector(domSelector);
    if (element == null)
        throw new Error("Unable to find submission energy bar with selector: " + domSelector);
    if (element.dataset.serverTime == undefined)
        throw new Error("Unable to find server time (used for calculations) for bar with selector: " + domSelector);

    var timeRemainingElement = element.querySelector(".time-remaining");
    if (timeRemainingElement == null)
        throw new Error("Unable to find the element to output time remaining for energy bar with selector: " + domSelector);

    var slots = [];
    var nextRechargedSlot = null;
    var interval = null;
    var now = element.dataset.serverTime;

    // If server epoch time is more than five minutes off from local epoch time...
    if (Math.abs(now - new Date().getTime()) > 5 * 60000) {
        notifyUserOfTimeMisconfiguration();
    }

    function start() {
        element.querySelectorAll(".energy-slot")
            .forEach(function(slot) {
                var expiration = (slot.dataset.expiration == 0) ? 0 : slot.dataset.expiration - now;
                expiration = Math.round(expiration / 1000);
                var slot = { element : slot, expiration : expiration };
                if (expiration > 0 && nextRechargedSlot == null)
                    nextRechargedSlot = slot;

                if (expiration > 0)
                    slot.element.classList.add("empty");
                slots.push(slot);
            });

        updateTimeRemaining();
        setInterval(function() { updateTimeRemaining(); }, 1000);
    };

    function updateTimeRemaining() {
        slots.forEach(function(slot) { slot.expiration--; });
        if (nextRechargedSlot != null && nextRechargedSlot.expiration == 0) {
            nextRechargedSlot.element.classList.remove("empty");
            nextRechargedSlot = null;
            for (var i = 0; i < slots.length; i++) {
                if (slots[i].expiration > 0) {
                    nextRechargedSlot = slots[i];
                    break;
                }
            }
        }

        if (nextRechargedSlot == null) {
            return;
        }
        var numMinutes = Math.floor(nextRechargedSlot.expiration  / 60);
        var numSeconds = nextRechargedSlot.expiration % 60;
        if (numSeconds < 10) numSeconds = "0" + numSeconds;
        timeRemainingElement.innerText = numMinutes + ":" + numSeconds;
    }

    function notifyUserOfTimeMisconfiguration() {
        if (localStorage.getItem("user-time-misconfiguration-notification") != null)
            return;
        localStorage.setItem("user-time-misconfiguration-notification", "performed");
        alert(
            "It appears that there is a misconfiguration between your machine's configured time and timezone.\n\n" +
            "This often appears when your machine is configured for one timezone, but then the clock is adjusted to appear as in another.  For example, the machine may be configured to use PST, but the clock was adjusted three hours ahead to align with EST.\n\n" +
            "To fix this issue, we recommend realigning your machine's clock with the configured timezone (or enable syncing with a time server). Then, adjust the timezone as desired.\n\n" +
            "If you need additional assistance, please contact your instructor."
        );
    }

    this.getSlots = function() { return slots; };

    start();
}


// Listen for click events
document.addEventListener('click', function (event)
{
    // Make sure clicked element is our toggle
    if (event.target.classList.contains('seeMoreLink'))
    {
        // Prevent default link behavior
        // event.preventDefault();

        // Get the content
        var content = document.querySelector(event.target.hash);
        if (!content) return;

        var element;
        if (content.id == "codingPanel")
        {
            element = document.getElementById("coding-moreInfo");
        }
        else if (content.id == "testingPanel")
        {
            element = document.getElementById("testing-moreInfo");
        }
        else if (content.id == "behaviorPanel")
        {
            element = document.getElementById("behavior-moreInfo");
        }
        else if (content.id == "stylePanel")
        {
          element = document.getElementById("style-moreInfo");
        }

        // Toggle the content
        if (element != null && !element.classList.contains('visible'))
        {
            toggle(element);
        }
        else
        {
            event.preventDefault();
        }
    }
}, false);


// Show an element
var show = function (elem)
{
    // Get the natural height of the element
    var getHeight = function ()
    {
        elem.style.display = 'block'; // Make it visible
        var height = elem.scrollHeight + 'px'; // Get it's height
        elem.style.display = ''; //  Hide it again
        return height;
    };

    var height = getHeight(); // Get the natural height
    elem.classList.add('visible'); // Make the element visible
    elem.style.height = height; // Update the max-height

    // Once the transition is complete, remove the inline max-height so
    // the content can scale responsively
    window.setTimeout(function ()
    {
        elem.style.height = '';
    }, 750);

    var element;
    if ((element = document.getElementById('coding-moreInfo')) != null
        && element.classList.contains('visible'))
        element = document.getElementsByClassName('codingarrow');
    else if ((element = document.getElementById('testing-moreInfo')) != null
        && element.classList.contains('visible'))
        element = document.getElementsByClassName('testingarrow');
    else if ((element = document.getElementById('behavior-moreInfo')) != null
        && element.classList.contains('visible'))
        element = document.getElementsByClassName('behaviorarrow');
    else if ((element = document.getElementById('style-moreInfo')) != null
        && element.classList.contains('visible'))
        element = document.getElementsByClassName('stylearrow');

    if (element != null)
    {
        for (var i = 0 ; i < element.length; i++)
        {
            element[i].classList.add('visible');
        }
    }
};


// Hide an element
var hide = function ()
{
    var element;
    var arrow;

    if ((element = document.getElementById('coding-moreInfo')) != null
        && element.classList.contains('visible'))
    {
        element = document.getElementById('coding-moreInfo');
        arrow = document.getElementsByClassName('codingarrow');
    }
    else if ((element = document.getElementById('testing-moreInfo')) != null
        && element.classList.contains('visible'))
    {
        element = document.getElementById('testing-moreInfo');
        arrow = document.getElementsByClassName('testingarrow');
    }
    else if ((element = document.getElementById('behavior-moreInfo')) != null
        && element.classList.contains('visible'))
    {
        element = document.getElementById('behavior-moreInfo');
        arrow = document.getElementsByClassName('behaviorarrow');
    }
    else if ((element = document.getElementById('style-moreInfo')) != null
        && element.classList.contains('visible'))
    {
        element = document.getElementById('style-moreInfo');
        arrow = document.getElementsByClassName('stylearrow');
    }

    if (element != null)
        element.classList.remove('visible');
    if (arrow != null && arrow.length == 2)
    {
        arrow[0].classList.remove('visible');
        arrow[1].classList.remove('visible');
    }
};


// Toggle element visibility
var toggle = function (elem, timing)
{
    hide();
    show(elem);
};
