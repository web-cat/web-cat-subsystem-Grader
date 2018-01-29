//
// htmlArea v3.0 - Copyright (c) 2002 interactivetools.com, inc.
// This copyright notice MUST stay intact for use (see license.txt).
//
// A free WYSIWYG editor replacement for <textarea> fields.
// For full source code and docs, visit http://www.interactivetools.com/
//
//
//
// $Id: codearea.js,v 1.10 2013/08/27 02:02:48 stedwar2 Exp $

// Creates a new HTMLArea object.  Tries to replace the textarea with the given
// ID with it.
function HTMLArea(textarea, config) {
    if (HTMLArea.checkSupportedBrowser()) {
        if (typeof config == "undefined") {
            this.config = new HTMLArea.Config();
        } else {
            this.config = config;
        }
        if (this.config.debug) {
            // alert("DEBUG ON!!");
        }
        this._htmlArea = null;
        this._textArea = textarea;
        this._mode = "wysiwyg";
            this._initHist = document.forms[0].history;
    }
};

HTMLArea.Config = function () {
    this.version = "3.0";

    this.width = "auto";
    this.height = "auto";

    // the next parameter specifies whether the toolbar should be included
    // in the size or not.
    this.sizeIncludesToolbar = true;

    this.bodyStyle = "background-color: #fff; font-family: verdana,sans-serif";
    this.editorURL = "http://web-cat.cs.vt.edu/htmlarea/";
    this.coreResourceURL = "http://web-cat.cs.vt.edu/";

    // URL-s
    this.imgURL = "images/";
    this.popupURL = "popups/";
    this.userName = "You";
    this.numComments = 0;
    this.debug = 0;

    this.replaceNextLines = 0;
    this.plainTextInput = 0;

    this.toolbar = [ ["newc", "space", "deletec", "space", "separator"],
             //[ "fontname", "space" ],
             //[ "fontsize", "space" ],
            // [ "formatblock", "space"],
             [ "historylist", "space", "category", "space", "target", "separator" ],
             [ "bold", "italic", "underline", "separator" ],
            // [ "strikethrough", "subscript", "superscript", "linebreak" ],
            // [ "justifyleft", "justifycenter", "justifyright", "justifyfull", "separator" ],
            // [ "orderedlist", "unorderedlist", "outdent", "indent", "separator" ],
            // [ "forecolor", "backcolor", "textindicator", "separator" ],
            ["createlink", "separator"],
            ["showhidecomments"]
            // [ "htmlmode", "separator" ]
            // [ "horizontalrule", "createlink", "insertimage", "inserttable", "htmlmode", "separator" ],
            // [ "popupeditor", "about" ]
        ];

    /* added: allevato, 2010.10.06 */
    this.showhidecomments = {
        "Show all comments":      "showall",
        "Hide all comments":      "hideall",
        "Show errors only":       "Error",
        "Show warnings only":     "Warning",
        "Show questions only":    "Question",
        "Show suggestions only":  "Suggestion",
        "Show answers only":      "Answer",
        "Show good only":         "Good",
        "Show extra credit only": "Extra_Credit"
    };

    this.category = {
        "Error":		"Error",
        "Warning":		"Warning",
        "Question":		"Question",
        "Suggestion":		"Suggestion",
        "Answer":		"Answer",
        "Good":			"Good",
        "Extra Credit":		"Extra_Credit"
    };
    this.target = {
        "To Everyone":		"To Everyone",
        "To Faculty/TAs":	"To Faculty/TAs",
        "To Faculty Only":	"To Faculty Only"
    };

    this.historylist = {
        "Select from previous comments":	"",
        "-----------------------------":	""
    };

    this.fontname = {
        "Arial":           'arial,helvetica,sans-serif',
        "Courier New":     'courier new,courier,monospace',
        "Georgia":         'georgia,times new roman,times,serif',
        "Tahoma":          'tahoma,arial,helvetica,sans-serif',
        "Times New Roman": 'times new roman,times,serif',
        "Verdana":         'verdana,arial,helvetica,sans-serif',
        "impact":          'impact',
        "WingDings":       'wingdings'
    };

    this.fontsize = {
        "1 (8 pt)":  "1",
        "2 (10 pt)": "2",
        "3 (12 pt)": "3",
        "4 (14 pt)": "4",
        "5 (18 pt)": "5",
        "6 (24 pt)": "6",
        "7 (36 pt)": "7"
    };

    this.formatblock = {
        "Heading 1": "h1",
        "Heading 2": "h2",
        "Heading 3": "h3",
        "Heading 4": "h4",
        "Heading 5": "h5",
        "Heading 6": "h6",
        "Normal": "p",
        "Address": "address",
        "Formatted": "pre"
    };

    //      ID              CMD                      ToolTip               Icon                        Enabled in text mode?
    this.btnList = {
        bold:           ["Bold",                 "Bold",               "ed_format_bold.gif",       false],
        italic:         ["Italic",               "Italic",             "ed_format_italic.gif",     false],
        underline:      ["Underline",            "Underline",          "ed_format_underline.gif",  false],
        //strikethrough:  ["StrikeThrough",        "Strikethrough",      "ed_format_strike.gif",     false],
        //subscript:      ["SubScript",            "Subscript",          "ed_format_sub.gif",        false],
        //superscript:    ["SuperScript",          "Superscript",        "ed_format_sup.gif",        false],
        //justifyleft:    ["JustifyLeft",          "Justify Left",       "ed_align_left.gif",        false],
        //justifycenter:  ["JustifyCenter",        "Justify Center",     "ed_align_center.gif",      false],
        //justifyright:   ["JustifyRight",         "Justify Right",      "ed_align_right.gif",       false],
        //justifyfull:    ["JustifyFull",          "Justify Full",       "ed_align_justify.gif",     false],
        //orderedlist:    ["InsertOrderedList",    "Ordered List",       "ed_list_num.gif",          false],
        //unorderedlist:  ["InsertUnorderedList",  "Bulleted List",      "ed_list_bullet.gif",       false],
        //outdent:        ["Outdent",              "Decrease Indent",    "ed_indent_less.gif",       false],
        //indent:         ["Indent",               "Increase Indent",    "ed_indent_more.gif",       false],
        //forecolor:      ["ForeColor",            "Font Color",         "ed_color_fg.gif",          false],
        //backcolor:      ["BackColor",            "Background Color",   "ed_color_bg.gif",          false],
        //horizontalrule: ["InsertHorizontalRule", "Horizontal Rule",    "ed_hr.gif",                false],
        createlink:     ["CreateLink",           "Insert Web Link",    	"ed_link.gif",              false],
        //insertimage:    ["InsertImage",          "Insert Image",       "ed_image.gif",             false],
        //inserttable:    ["InsertTable",          "Insert Table",       "insert_table.gif",         false],
        newc:			["NewC",				 "Add a new comment",  "ed_new.gif",			   false],
        deletec:		["DeleteC",		"Remove an existing comment",  "ed_remove.gif",			   false],
        //htmlmode:       ["HtmlMode",             "Toggle HTML Source", "ed_html.gif",              true],
        //popupeditor:    ["popupeditor",          "Enlarge Editor",     "fullscreen_maximize.gif",  true],
        //about:          ["about",                "About this editor",  "ed_about.gif",             true],
        help:           ["showhelp",             "Help using editor",  "ed_help.gif",              true]
    };

    // initialize tooltips from the I18N module
    for (var i in this.btnList) {
        var btn = this.btnList[i];
        if (typeof HTMLArea.I18N.tooltips[i] != "undefined") {
            btn[1] = HTMLArea.I18N.tooltips[i];
        }
    }
};

/** Helper function: replace all TEXTAREA-s in the document with HTMLArea-s. */
HTMLArea.replaceAll = function() {
    var tas = document.getElementsByTagName("textarea");
    for (var i = tas.length; i > 0; (new HTMLArea(tas[--i])).generate());
};

// Creates the toolbar and appends it to the _htmlarea
HTMLArea.prototype._createToolbar = function () {
    var editor = this;	// to access this in nested functions

    var toolbar = document.createElement("div");
    this._toolbar = toolbar;
    toolbar.className = "toolbar";
    toolbar.unselectable = "1";
    if (editor.config.debug) {
        toolbar.style.border = "1px solid red";
    }
    var tb_row = null;
    var tb_objects = new Object();
    this._toolbarObjects = tb_objects;

    // creates a new line in the toolbar
    function newLine() {
        var table = document.createElement("table");
//		table.border = "0px";
        table.cellSpacing = "0px";
        table.cellPadding = "0px";
        toolbar.appendChild(table);
        // TBODY is required for IE, otherwise you don't see anything
        // in the TABLE.
        var tb_body = document.createElement("tbody");
        table.appendChild(tb_body);
        tb_row = document.createElement("tr");
        tb_body.appendChild(tb_row);
    };
    // init first line
    newLine();

    // appends a new button to toolbar
    function createButton(txt) {
        // updates the state of a toolbar element
        function setButtonStatus(id, newval) {
            var oldval = this[id];
            var el = this.element;
            if (oldval != newval) {
                switch (id) {
                    case "enabled":
                    if (newval) {
                        HTMLArea._removeClass(el, "buttonDisabled");
                        el.disabled = false;
                    } else {
                        HTMLArea._addClass(el, "buttonDisabled");
                        el.disabled = true;
                    }
                    break;
                    case "active":
                    if (newval) {
                        HTMLArea._addClass(el, "buttonPressed");
                    } else {
                        HTMLArea._removeClass(el, "buttonPressed");
                    }
                    break;
                }
                this[id] = newval;
            }
        };
        // this function will handle creation of combo boxes
        function createSelect() {
            var options = null;
            var el = null;
            var cmd = null;
            switch (txt) {
                case "fontsize":
                case "fontname":
                case "formatblock":
                case "category":
                case "target":
                case "historylist":
                case "showhidecomments":
                options = editor.config[txt]; // HACK ;)
                cmd = txt;
                break;
            }
            if (options) {
                el = document.createElement("select");
                var obj = {
                    name: txt,     // field name
                    element: el,   // the UI element (SELECT)
                    enabled: true, // is it enabled?
                    text: false,   // enabled in text mode?
                    cmd: cmd,      // command ID
                    state: setButtonStatus // for changing state
                };
                tb_objects[txt] = obj;
                for (var i in options) {
                    var op = document.createElement("option");
                    op.appendChild(document.createTextNode(i));
                    op.value = options[i];
                    el.appendChild(op);
                }
                if(txt == "historylist")// load the initial history list
                {
                    var initHist = editor._initHist.value;
                         if( (initHist != null) && (initHist != "") )
                    {
                        initHist = initHist.split("\n");
                          for(var i = 0; i < initHist.length; i++)
                        {
                            var item = initHist[i];
                                          if((item != "") && (item != null))
                                          {
                              itemfields = item.split("$");
                              var msgbody = itemfields[5];
                              if ( msgbody )
                              {
                              var op = document.createElement("option");
                              msgbody = msgbody.replace( /<(.|\n)+?>/g, "" ); // remove all tags
                              if ( msgbody .length > 25 )
                                    msgbody = msgbody.substr( 0, 25 ) + "...";
                              op.appendChild(document.createTextNode(msgbody));
                              op.value = item;
                              el.appendChild(op);
                              }
                                          }
                        }
                    }
                }
                HTMLArea._addEvent(el, "change", function () {
                    editor._comboSelected(el, txt);
                });
            }
            return el;
        };
        // the element that will be created
        var el = null;
        var btn = null;
        switch (txt) {
            case "separator":
            el = document.createElement("div");
            el.className = "separator";
            break;
            case "space":
            el = document.createElement("div");
            el.className = "space";
            break;
            case "linebreak":
            newLine();
            return false;
            case "textindicator":
            el = document.createElement("div");
            el.appendChild(document.createTextNode("A"));
            el.className = "indicator";
            el.title = HTMLArea.I18N.tooltips.textindicator;
            var obj = {
                name: txt,     // the button name (i.e. 'bold')
                element: el,   // the UI element (DIV)
                enabled: true, // is it enabled?
                active: false, // is it pressed?
                text: false,   // enabled in text mode?
                cmd: "textindicator", // the command ID
                state: setButtonStatus // for changing state
            };
            tb_objects[txt] = obj;
            break;
            default:
            btn = editor.config.btnList[txt];
            break;
        }
        if (!el && btn) {
            el = document.createElement("div");
            el.title = btn[1];
            el.className = "button";
            // let's just pretend we have a button object, and
            // assign all the needed information to it.
            var obj = {
                name: txt,     // the button name (i.e. 'bold')
                element: el,   // the UI element (DIV)
                enabled: true, // is it enabled?
                active: false, // is it pressed?
                text: btn[3],  // enabled in text mode?
                cmd: btn[0],   // the command ID
                state: setButtonStatus // for changing state
            };
            tb_objects[txt] = obj;
            // handlers to emulate nice flat toolbar buttons
            HTMLArea._addEvent(el, "mouseover", function () {
                if (obj.enabled) {
                    HTMLArea._addClass(el, "buttonHover");
                }
            });
            HTMLArea._addEvent(el, "mouseout", function () {
                if (obj.enabled) with (HTMLArea) {
                    _removeClass(el, "buttonHover");
                    _removeClass(el, "buttonActive");
                    (obj.active) && _addClass(el, "buttonPressed");
                }
            });
            HTMLArea._addEvent(el, "mousedown", function (ev) {
                if (obj.enabled) with (HTMLArea) {
                    _addClass(el, "buttonActive");
                    _removeClass(el, "buttonPressed");
                    _stopEvent(is_ie ? window.event : ev);
                }
            });
            // when clicked, do the following:
            HTMLArea._addEvent(el, "click", function (ev) {
                if (obj.enabled) with (HTMLArea) {
                    _removeClass(el, "buttonActive");
                    _removeClass(el, "buttonHover");
                    editor._buttonClicked(txt);
                    _stopEvent(is_ie ? window.event : ev);
                }
            });
            var img = document.createElement("img");
            img.src = editor.imgURL(btn[2]);
            el.appendChild(img);
        } else if (!el) {
            el = createSelect();
            if(txt == "category")
                editor._catlist = el; // storing the reference to the category list for later use
            else if(txt == "target")
                editor._targlist = el; // storing reference to the target list for later use
            else if(txt == "historylist")
                editor._histlist = el;
        }
        if (el) {
            var tb_cell = document.createElement("td");
            tb_row.appendChild(tb_cell);
            tb_cell.appendChild(el);
        } else {
            alert("FIXME: Unknown toolbar item: " + txt);
        }
        return el;
    };

    for (var i in this.config.toolbar) {
        var group = this.config.toolbar[i];
        for (var j in group) {
            createButton(group[j]);
        }
    }

    this._htmlArea.appendChild(toolbar);
};

// Creates the HTMLArea object and replaces the textarea with it.
HTMLArea.prototype.generate = function () {
    var editor = this;	// we'll need "this" in some nested functions
    // get the textarea
    var textarea = this._textArea;
    if (typeof textarea == "string") {
        // it's not element but ID
        this._textArea = textarea = document.getElementById(textarea);
    }
    this._ta_size = {
        w: textarea.offsetWidth,
        h: textarea.offsetHeight
    };
    // hide the textarea
    textarea.style.display = "none";

    // create the editor framework
    var htmlarea = document.createElement("div");
    htmlarea.className = "htmlarea";
    this._htmlArea = htmlarea;

    // insert the editor before the textarea.
    textarea.parentNode.insertBefore(htmlarea, textarea);

    // retrieve the HTML on submit
    textarea.form.onsubmit = function() {
      // alert("inside onsubmit");
    editor._textArea.value = editor.getHTML();
      // alert("getHTML succeeded");
    var histString = "";
      // alert("Length of list = " + editor._histlist.childNodes.length);
      for(var i = 2; i < editor._histlist.childNodes.length; i++)
    {
        // alert(editor._histlist.childNodes[i].value);
        arr = editor._histlist.childNodes[i].value.split("$");
            arr[6] = -1;
            arr[7] = 0;
            var joinback = arr.join("$");
            histString += joinback + "\n";
      }
      // alert("history = " + histString);
      editor._initHist.value = histString;
    };
    // creates & appends the toolbar
    this._createToolbar();

    // create the IFRAME
    var iframe = document.createElement("iframe");
    htmlarea.appendChild(iframe);
    this._iframe = iframe;

    // remove the default border as it keeps us from computing correctly
    // the sizes.  (somebody tell me why doesn't this work in IE)
    // iframe.style.border = "none";
    // iframe.frameborder = "0";

    // size the IFRAME according to user's prefs or initial textarea
    var height = (this.config.height == "auto" ? (this._ta_size.h + "px") : this.config.height);
    height = parseInt(height);
    var width = (this.config.width == "auto" ? (this._ta_size.w + "px") : this.config.width);
    width = parseInt(width);

    iframe.style.width = width + "px";
    if (this.config.sizeIncludesToolbar) {
        // substract toolbar height
        height -= this._toolbar.offsetHeight;
    }
    iframe.style.height = height + "px";

    // now create a secondary textarea so that we can switch between
    // WYSIWYG & text mode.
    textarea = document.createElement("textarea");

    // hidden by default
    textarea.style.display = "none";

    // make it the same size as the editor
    textarea.style.width = iframe.style.width;
    textarea.style.height = iframe.style.height;

    // insert it after the iframe
    htmlarea.appendChild(textarea);

    // remember it for later
    this._textArea2 = textarea;
    //initialize global variables
    this._messageCounter = this.config.numComments;//set the mesage counter to the last added comment box number through db
    this._clickedrow = null;
    this._nextrow = null;
    this._lastclickedrow = null;// to keep track of last clicked box
    this._wasinside = 0;// to keep track if the last click was inside the box or outside
    // IMPORTANT: we have to allow Mozilla a short time to recognize the
    // new frame.  Otherwise we get a stupid exception.
    function initIframe() {
        var doc = editor._iframe.contentWindow.document;
        if (!doc) {
            if (HTMLArea.is_gecko) {
                setTimeout(function () { editor._initIframe(); }, 10);
                return false;
            } else {
                alert("ERROR: IFRAME can't be initialized.");
            }
        }
        if (HTMLArea.is_gecko) {
            // enable editable mode for Mozilla
            doc.designMode = "off"; // not "on";
            doc.captureEvents(Event.CLICK);
        }
        editor._doc = doc;
        doc.open();
        var html = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 ";
        html += "Strict//EN ";
        html += "\"http://www.w3.org/TR/xhtml1/DTD/";
        html += "xhtml1-strict.dtd\">\n<html lang=\"en\"";
        html += " xml:lang=\"en\" xmlns=\"http://www.w3.org/1999/";
        html += "xhtml\"><head>\n";
        html += "<style> body { " + editor.config.bodyStyle + " } </style>\n";
        html += "</head>\n";
        html += "<body>\n";
        html += editor._textArea.value;
        html += "</body>\n";
        html += "</html>";
        doc.write(html);
        doc.close();

        if (HTMLArea.is_ie) {
            // enable editable mode for IE.  For some reason this
            // doesn't work if done in the same place as for Gecko
            // (above).
            doc.body.contentEditable = false;

        }
        editor.focusEditor();
        // intercept some events; for updating the toolbar & keyboard handlers
        HTMLArea._addEvents
            (doc, ["keydown", "keypress", "mousedown", "mouseup", "drag", "click"],
             function (event) {
                 return editor._editorEvent(HTMLArea.is_ie ? editor._iframe.contentWindow.event : event);
             });
        editor.updateToolbar();
        editor.focusEditor();
    };

    setTimeout(initIframe, HTMLArea.is_gecko ? 10 : 0);
};

// Switches editor mode; parameter can be "textmode" or "wysiwyg"
HTMLArea.prototype.setMode = function(mode) {
    switch (mode) {
        case "textmode":
        this._textArea2.value = this.getHTML();
        this._iframe.style.display = "none";
        this._textArea2.style.display = "block";
        break;
        case "wysiwyg":
        this._doc.body.innerHTML = this.getHTML();
        this._iframe.style.display = "block";
        this._textArea2.style.display = "none";
        if (HTMLArea.is_gecko) {
            // we need to refresh that info for Moz-1.3a
            this._doc.designMode = "off"; // not on!
        }
        break;
        default:
        alert("Mode <" + mode + "> not defined!");
        return false;
    }
    this._mode = mode;
    this.focusEditor();
};

/***************************************************
 *  Category: EDITOR UTILITIES
 ***************************************************/

// focuses the iframe window.  returns a reference to the editor document.
HTMLArea.prototype.focusEditor = function() {
    switch (this._mode) {
        case "wysiwyg":
        this._iframe.contentWindow.focus();
        break;
        case "textmode":
        this._textArea2.focus();
        break;
        default:
        alert("ERROR: mode " + this._mode + " is not defined");
        break;
    }
    return this._doc;
};
HTMLArea.prototype.trimString = function(str) {
    //str = this != window? this : str;
    str = str.replace(/&nbsp\;+/g, ''); // remove the nbsp
    str = str.replace(/^\s*/,'').replace(/\s*$/,''); //remove leading and trailing white spaces
    return str;
};

HTMLArea.prototype.updateCombos = function(param) {
    var doc = this._doc;
    var clickedrow = this._clickedrow;
    if(clickedrow == null)
    {
        if( (this._lastclickedrow != null) && (this._wasinside == 1) )// clicked on a newly formatted text, so use the previous clicked row
        {
            clickedrow = this._clickedrow = this._lastclickedrow;
        }
    }
    if(clickedrow != null)
    {
        var arr = clickedrow.id.split(":");
        if(clickedrow.id.charAt(0) == 'I') // if clicked inside a message box
        {
            var currid = "";
            var value = "";
            if(param == "category")
            {
                currid = "I"+this.getBoxNumber(arr[0])+ ":" + arr[1] + ":"+ arr[2] + ":C";
                if(doc.getElementById(currid) != null)
                    value = this.trimString(doc.getElementById(currid).innerHTML).toLowerCase();
            }
            else
            {
                currid = "I"+this.getBoxNumber(arr[0])+ ":" + arr[1] + ":" + arr[2] + ":T";
                if(doc.getElementById(currid) != null)
                    value = doc.getElementById(currid).getAttribute("value").toLowerCase();
            }
            var btn = this._toolbarObjects[param];
            var options = this.config[param]; // HACK!!
            var k = 0;
            // btn.element.selectedIndex = 0;
            for (var j in options) {
                // FIXME: the following line is scary.
                if ((j.toLowerCase() == value) ||
                    (options[j].substr(0, value.length).toLowerCase() == value)) {
                    btn.element.selectedIndex = k;
                    break;
                }
                ++k;
            }
        }
    }
};
// updates enabled/disable/active state of the toolbar elements
HTMLArea.prototype.updateToolbar = function() {

    var doc = this._doc;
    var text = (this._mode == "textmode");
    for (var i in this._toolbarObjects) {
        var btn = this._toolbarObjects[i];
        var cmd = btn.cmd;
        if (typeof cmd == "function") {
            continue;
        }
        cmd = cmd.toLowerCase();
        btn.state("enabled", !text || btn.text);
        switch (cmd) {
            case "fontname":
            case "fontsize":
            case "formatblock":
            if (!text) {
                var value = ("" + doc.queryCommandValue(cmd)).toLowerCase();
                if (!value) {
                    // FIXME: what do we do here?
                    break;
                }
                var options = this.config[i]; // HACK!!
                var k = 0;
                // btn.element.selectedIndex = 0;
                for (var j in options) {
                    // FIXME: the following line is scary.
                    if ((j.toLowerCase() == value) ||
                        (options[j].substr(0, value.length).toLowerCase() == value)) {
                        btn.element.selectedIndex = k;
                        break;
                    }
                    ++k;
                }
            }
            break;
            case "textindicator":
            if (!text) {
                try {with (btn.element.style) {
                    backgroundColor = HTMLArea._makeColor(doc.queryCommandValue("backcolor"));
                    color = HTMLArea._makeColor(doc.queryCommandValue("forecolor"));
                    fontFamily = doc.queryCommandValue("fontname");
                    fontWeight = doc.queryCommandState("bold") ? "bold" : "normal";
                    fontStyle = doc.queryCommandState("italic") ? "italic" : "normal";
                }} catch (e) {
                    alert(e + "\n\n" + cmd);
                }
            }
            break;
            case "htmlmode":
            btn.state("active", text);
            break;
            default:
            try {
                btn.state("active", (!text && doc.queryCommandState(cmd)));
            } catch (e) {}
            break;
        }
    }
};

/** Returns a node after which we can insert other nodes, in the current
 * selection.  The selection is removed.  It splits a text node, if needed.
 */
HTMLArea.prototype.insertNodeAtSelection = function(toBeInserted) {
    if (!HTMLArea.is_ie) {
        var sel = this._getSelection();
        var range = this._createRange(sel);
        // remove the current selection
        sel.removeAllRanges();
        range.deleteContents();
        var node = range.startContainer;
        var pos = range.startOffset;
        range = this._createRange();
        switch (node.nodeType) {
            case 3: // Node.TEXT_NODE
            // we have to split it at the caret position.
            if (toBeInserted.nodeType == 3) {
                // do optimized insertion
                node.insertData(pos, toBeInserted.data);
                range.setEnd(node, pos + toBeInserted.length);
                range.setStart(node, pos + toBeInserted.length);
            } else {
                node = node.splitText(pos);
                node.parentNode.insertBefore(toBeInserted, node);
                range.setStart(node, 0);
                range.setEnd(node, 0);
            }
            break;
            case 1: // Node.ELEMENT_NODE
            node = node.childNodes[pos];
            node.parentNode.insertBefore(toBeInserted, node);
            range.setStart(node, 0);
            range.setEnd(node, 0);
            break;
        }
        sel.addRange(range);
    } else {
        return null;	// this function not yet used for IE <FIXME>
    }
};

/** Call this function to insert HTML code at the current position.  It deletes
 * the selection, if any.
 */
HTMLArea.prototype.insertHTML = function(html) {
    var sel = this._getSelection();
    var range = this._createRange(sel);
    if (HTMLArea.is_ie) {
        range.pasteHTML(html);
    } else {
        // construct a new document fragment with the given HTML
        var fragment = this._doc.createDocumentFragment();
        var div = this._doc.createElement("div");
        div.innerHTML = html;
        while (div.firstChild) {
            // the following call also removes the node from div
            fragment.appendChild(div.firstChild);
        }
        // this also removes the selection
        var node = this.insertNodeAtSelection(fragment);
    }
};

/**
 *  Call this function to surround the existing HTML code in the selection with
 *  your tags.
 */
HTMLArea.prototype.surroundHTML = function(startTag, endTag) {
    var html = this.getSelectedHTML();
    // the following also deletes the selection
    this.insertHTML(startTag + html + endTag);
};

/// Retrieve the selected block
HTMLArea.prototype.getSelectedHTML = function() {
    var sel = this._getSelection();
    var range = this._createRange(sel);
    var existing = null;
    if (HTMLArea.is_ie) {
        existing = range.htmlText;
    } else {
        existing = HTMLArea.getHTML(range.cloneContents(), false);
    }
    return existing;
};

// Called when the user clicks on "InsertImage" button
HTMLArea.prototype._insertImage = function() {
    var sel = this._getSelection();
    var range = this._createRange(sel);
    var editor = this;	// for nested functions
    this._popupDialog("insert_image.html", function(param) {
        if (!param) {	// user must have pressed Cancel
            return false;
        }
        editor._execCommand("insertimage", false, param["f_url"]);
        var img = null;
        if (HTMLArea.is_ie) {
            img = range.parentElement();
            // wonder if this works...
            if (img.tagName.toLowerCase() != "img") {
                img = img.previousSibling;
            }
        } else {
            img = range.startContainer.previousSibling;
        }
        for (field in param) {
            var value = param[field];
            if (!value) {
                continue;
            }
            switch (field) {
                case "f_alt":
                img.alt = value;
                break;
                case "f_border":
                img.border = parseInt(value);
                break;
                case "f_align":
                img.align = value;
                break;
                case "f_vert":
                img.vspace = parseInt(value);
                break;
                case "f_horiz":
                img.hspace = parseInt(value);
                break;
            }
        }
    }, null);
};
// Called when the user clicks on "New Comment" button
HTMLArea.prototype._insertComment = function(param) {
    var editor = this;	// for nested functions
    var doc = editor._doc;
    var box_number = this._messageCounter;


    function createMessageBox(parentid)
    {
    box_number++;
    editor._messageCounter = box_number;
    // initialize values to ""
    var cat = "";
    var tar = "";
    var pts = "";
    var msg = "";
    var valToInsert = "";
    var reference = "";
    if ( param != "" && param != null )
    {
        // chose from history list
        var props = param.split("$");
        cat = props[2];
        tar = props[3];
        pts = props[4];
        msg = props[5];
        var refNum = parseInt(props[6]);
        var occurences = parseInt(props[7]);
        var indexInHist = parseInt(props[8]);
        editor._histlist.removeChild(editor._histlist.item(indexInHist));
        occurences = occurences + 1;
          if(refNum == -1) //from the initial list
          {
            reference = box_number;
            valToInsert = parentid + "$" + box_number + "$" + cat + "$" + tar
             + "$" + pts + "$" + msg + "$" + reference + "$" + occurences;

          }
        else  //use the same one as is
        {
            reference = refNum;
            valToInsert = props[0] + "$" + props[1] + "$" + cat + "$" + tar
                 + "$" + pts + "$" + msg + "$" + reference + "$" + occurences;
          }
    }
    else
    {
        // clicked on insert comment
        cat = editor._catlist.options[editor._catlist.selectedIndex].value;
        tar = editor._targlist.options[editor._targlist.selectedIndex].value;
        if ( editor.config.viewPoints )
        {
        // check to see if the user is authorized to view points
        if ( cat == "Error" )
            pts = "-2.0";
        else if ( cat == "Extra Credit" )
            pts = "+2.0";
        else
            pts = "";
        }
        msg = "Replace this line with your comment";

        reference = box_number;
        valToInsert = parentid + "$" + box_number + "$" + cat + "$" + tar
        + "$" + pts + "$" + msg + "$" + reference + "$" + "1";

        // find out how many previous comments there are in the history
            // list. If less than 20, then add normally, else remove oldest
            // before adding.
        if ( editor._histlist.length >= 22 )
        editor._histlist.removeChild(editor._histlist.lastChild);
    }
    var op = document.createElement( "input" );
    op.type = "hidden";
    var idTag = "I" + box_number + ":" + parentid + ":" +reference;
    var shorty = "";
      shorty = msg.replace( /<(.|\n)+?>/g, "" ); // remove all tags
     if ( shorty.length > 25 )
        shorty = shorty.substr( 0, 25 ) + "...";
     op.appendChild( document.createTextNode( shorty ) );
    op.value = valToInsert;
    // add to beginning of list
    editor._histlist.insertBefore(
        op, editor._histlist.firstChild.nextSibling.nextSibling );
      //Replace &&&& in the ids for style of the message with this idTag.
    msg = msg.replace(/&&&&/g,idTag);// replace all &&&& with idTags

      // ---- first row ----
    var comment = doc.createDocumentFragment();
    var tr1 = doc.createElement( "tr" );
    tr1.setAttribute( "id", idTag );
    var td1 = doc.createElement( "td" );
    td1.setAttribute( "id", idTag );
    td1.setAttribute( "colspan", "3" );
    var txt1 = doc.createElement( "div" );
    txt1.setAttribute( "id", idTag );
    txt1.innerHTML = "<img id=\"" + idTag
        + "\" src=\"" + editor.config.coreResourceURL + "/images/blank.gif\" width=\"1\" height=\"3\"/>";
    td1.appendChild( txt1 );
    tr1.appendChild( td1 );
    comment.appendChild( tr1 );
    // ---- second row ----
    var tr2 = doc.createElement( "tr" );
    tr2.setAttribute( "id", idTag );
    var td2a = doc.createElement( "td" );
    td2a.setAttribute( "id", idTag );
    var td2b = doc.createElement( "td" );
    td2b.setAttribute( "id", idTag );
    var td2c = doc.createElement( "td" );
    td2c.setAttribute( "id", idTag );
    var txt2a = doc.createElement( "div" );
    txt2a.setAttribute( "id", idTag );
    var txt2b = doc.createElement( "div" );
    txt2b.setAttribute( "id", idTag );
    var txt2c = doc.createElement( "div" );
    txt2c.setAttribute( "id", idTag );
    txt2a.innerHTML = "&nbsp\;";
    td2a.appendChild( txt2a );
    txt2b.innerHTML = "&nbsp\;";
    td2b.appendChild( txt2b );


//	if ( pts != ""  &&  pts != " " )
//	    pts = ": "+pts;
    txt2c.innerHTML = "<table id=\"" + idTag
        + ":X\" border=\"0\" cellpadding=\"0\"><tbody id=\"" + idTag
        + ":B\"><tr id=\"" + idTag + ":R\"><td id=\"" + idTag
        + ":D\" class=\"messageBox\"><img id=\"" + idTag
        + ":I\" src=\"" + editor.config.coreResourceURL + "/icons/"
            + editor.getIcon(cat)
        + ".png\" border=\"0\"/><input type=\"hidden\" id=\"" + idTag + ":T\" value=\""
        + tar + "\"/><b id=\"" + idTag + "\"><span id=\"" + idTag
        + ":C\">&nbsp;" + cat + "&nbsp;</span><span id=\"" + idTag
        + ":N\">[" + editor.config.userName + "]"
            + ( ( pts == "" || pts == " " ) ? "" : " : " ) + "</span><span id=\""
        + idTag + ":P\" contentEditable=\"" + editor.config.viewPoints
        + "\">" + ( ( pts == "" || pts == " " ) ? "" : pts )
            + "</span></b><br id=\"" + idTag + "\"/><i id=\""
        + idTag + "\"><div id=\"" + idTag
        + ":M\" contentEditable=\"true\">" + msg
        + "</div></i></td></tr></tbody></table>";

    td2c.appendChild( txt2c );
    td2c.setAttribute( "align", "left" );

    tr2.appendChild( td2a );
    tr2.appendChild( td2b );
    tr2.appendChild( td2c );
    comment.appendChild( tr2 );

    // ---- third row ----

    var tr3 = doc.createElement( "tr" );
    tr3.setAttribute( "id", idTag );
    var td3 = doc.createElement( "td" );
    td3.setAttribute( "colspan", "3" );
    td3.setAttribute( "id", idTag );
    var txt3 = doc.createElement( "div" );
    txt3.setAttribute( "id", idTag );
    txt3.innerHTML = "<img id=\"" + idTag
        + "\" src=\"" + editor.config.coreResourceURL + "/images/blank.gif\" width=\"1\" height=\"3\"/>";
    td3.appendChild( txt3 );
    tr3.appendChild( td3 );
    comment.appendChild( tr3 );
    return comment;
    };
    var clickedrow = this._clickedrow;
    var nextrow = this._nextrow;
    var parentrowId = "";

    if ( clickedrow == null )
    {
    // case where user clicks on newly formatted text or if he clicks on
    // insert comment before clicking the source
    if ( this._lastclickedrow != null )
    {
        // clicked on a newly formatted text,
        // so use the previous clicked row
        clickedrow  = this._clickedrow = this._lastclickedrow;
    }
    else
    {
        // pressed the insert comment button before clicking anywhere else
        alert( "First, please click on the line to which your "
           + "comment applies." );
    }
    }
    else if( clickedrow.id == "tab" ) // case where user selection spans lines
    {
    alert( "First, please click on the line to which your comment "
           + "applies." );
    clickedrow = null;
    }

    if ( clickedrow != null )
    {
    var pid = "";
    var arr = clickedrow.id.split( ":" );
    if (clickedrow.id.charAt( 0 ) == 'I')
    {
         // if clicked inside a message box
        nextrow = doc.getElementById( "I" + this.getBoxNumber( arr[0] )
                      + ":" + arr[1] + ":" + arr[2] )
        .nextSibling.nextSibling.nextSibling;
        this._nextrow = nextrow;
    }
    if (clickedrow.id.charAt( 0 ) == 'F')
    {
         // if clicked inside a message box
        nextrow = doc.getElementById( "F" + this.getBoxNumber( arr[0] )
                      + ":" + arr[1] + ":" + arr[2] )
        .nextSibling.nextSibling.nextSibling;
        this._nextrow = nextrow;
    }
    pid = arr[1];
    if ( clickedrow.id.charAt( 0 ) == 'N' )
    {
        // if clicked inside a tool generated message box
        var currow = arr[2];
        this._clickedrow = clickedrow = doc.getElementById( "O:" + currow);
        this._nextrow = nextrow = clickedrow.nextSibling;
        pid = currow;
    }
    if ( nextrow != null )
    {
        // next row is not the last row, so use insertBefore
        nextrow.parentNode.insertBefore( createMessageBox( pid ), nextrow);
    }
    else
    {
         // no next row. so insertAfter
        doc.getElementById( "tab" ).appendChild( createMessageBox( pid ) );
    }
    if ( param != ""  &&  param != null ) // chose from history list
      {
        var reference = parseInt( param.split( "$" )[6] );
          if(reference == -1)
          {
              reference = parseInt( this._messageCounter );
          }
      }
    else
        var reference = parseInt( this._messageCounter );
    // make the newly added box active
    this._clickedrow = doc.getElementById( "I" + this._messageCounter
                           + ":" + pid + ":" + reference );
    this.swapBoxSelection( true, false );

    this._wasinside = 1; // its now clicked inside the newly added box
    this._lastclickedrow = this._clickedrow;
    this.updateHistory();
    // update the highlighting color for the main row.
    var mainrow = doc.getElementById( "O:" + pid );
    this.highlightLine( mainrow );
    }
};
HTMLArea.prototype._removeComment = function() {
    var editor = this;	// for nested functions
    var doc = editor._doc;
    var clickedrow = this._clickedrow;

    if ( clickedrow == null )
    {
    // case where user clicks on newly formatted text or clicks on
    // remove comment before clicking the source
    if ( ( this._lastclickedrow != null )  &&  ( this._wasinside == 1 ) )
    {
        // clicked on a newly formatted text, so use the previous clicked
        // row
        clickedrow = this._clickedrow = this._lastclickedrow;
    }
    else
    {
        // pressed the insert comment button before clicking anywhere else
        alert( "Please select a comment by clicking on it first." );
    }
    }
    if ( clickedrow != null )
    {
        var id = clickedrow.id;
        var arr = id.split(":");
        if ( id.charAt( 0 ) == 'I' ) // if clicked inside a message box
        {
            if(confirm("Are you sure you want to delete this comment"))
            {
                var currid = "I" + this.getBoxNumber( arr[0] ) + ":" + arr[1]
                + ":" + arr[2];
                var mainparent = doc.getElementById( "tab" ); // the tbody element
                for ( var h = 0; h < 3; h++ )
                {
                var parent = doc.getElementById( currid );
                var kids = parent.childNodes;
                for ( var j = 0; j < kids.length; j++ )
                {
                    parent.removeChild( kids[j] );
                }
                mainparent.removeChild( parent );
                }
                if ( this.getBoxNumber( id ) ==
                this.getBoxNumber( this._lastclickedrow.id ) )
                {
                    this._lastclickedrow = null;
                }
                this._clickedrow = null;
                this._nextrow = null;
                this._wasinside = 0;
                this.highlightLine( doc.getElementById( "O:" + arr[1] ) );
            }
        }
        else if ( id.charAt( 0 ) == 'N' )
        {
            alert( "You cannot remove a tool-generated comment." );
        }
        else if ( id.charAt( 0 ) == 'F' )
        {
            alert( "You do not have permission to remove "
            + "this comment box." );
        }
        else
        {
            alert( "Please select a comment box by clicking on it first.");
        }
    }
};
// returns the box number
HTMLArea.prototype.getBoxNumber = function(id) {
    var i = 0;
    if( (i = id.search(/:/) ) == -1)
        i = id.length;// couldnt find : , so just extract till end of string
    var tempnum = "";// for storing the string number (box num)
    for(var j = 1; j < i; j++) // ignore the first letter I and extract till :
        tempnum += id.charAt(j);
    var messnum = 0;
    if(tempnum != "")
        messnum = parseInt(tempnum);// convert it to int. this is the box number

    return messnum;
};
HTMLArea.prototype.highlightLine = function(row) {
    var children = 0;
    var id = "";
    var category = "";
    var colors = new Object();
    colors = {"Error":1,"Warning":2,"Question":3,"Suggestion":4,"Answer":5,"Good":6,"Extra_Credit":7,
            1:"Error", 2:"Warning",3:"Question",4:"Suggestion",5:"Answer",6:"Good",7:"Extra_Credit"};
    var nextrow = row.nextSibling;
    var minsofar = 8;
    while( (nextrow != null)
               && ( (nextrow.id.charAt(0) == 'I')
             || (nextrow.id.charAt(0) == 'N')
                || (nextrow.id.charAt(0) == 'F') ) )
    {
        children++;//increment children counter
        if( ((children / 3) >= 1) && (children % 3 == 0)) // after reading one box
        {
            id = this._doc.getElementById(nextrow.id + ":C");
            category = this.trimString(id.innerHTML);
            category = category.replace(" ", "_");
            if(colors[category] <= minsofar)// take the smaller of the two and save it in maxsofar
                minsofar = colors[category];
        }
        nextrow = nextrow.nextSibling;
    }
    if(children > 0)
    {
         row.className = colors[minsofar];
    }
    else
    {
        //check to see if there was an original color from the tool generated comments
        row.className = "";
    }

};
// returns the icon str
HTMLArea.prototype.getIcon = function(cat) {
    var imgname = "";
    if(cat == "Error")
        imgname = "comment-error";
    else if(cat == "Good")
        imgname = "comment-good";
    else if(cat == "Warning")
        imgname = "comment-warning";
    else if(cat == "Extra Credit")
        imgname = "comment-extracredit";
    else if(cat == "Suggestion")
        imgname = "comment-suggestion";
    else if(cat == "Question")
        imgname = "comment-question";
    else if(cat == "Answer")
        imgname = "comment-answer";
    else
        imgname = "comment-question";

    return imgname;
};

// Called when the user clicks the Insert Table button
HTMLArea.prototype._insertTable = function() {
    var sel = this._getSelection();
    var range = this._createRange(sel);
    var editor = this;	// for nested functions
    this._popupDialog("insert_table.html", function(param) {
        if (!param) {	// user must have pressed Cancel
            return false;
        }
        var doc = editor._doc;
        // create the table element
        var table = doc.createElement("table");
        // assign the given arguments
        for (var field in param) {
            var value = param[field];
            if (!value) {
                continue;
            }
            switch (field) {
                case "f_width":
                table.style.width = value + param["f_unit"];
                break;
                case "f_align":
                table.align = value;
                break;
                case "f_border":
                table.border = parseInt(value);
                break;
                case "f_spacing":
                table.cellspacing = parseInt(value);
                break;
                case "f_padding":
                table.cellpadding = parseInt(value);
                break;
            }
        }
        var tbody = doc.createElement("tbody");
        table.appendChild(tbody);
        for (var i = 0; i < param["f_rows"]; ++i) {
            var tr = doc.createElement("tr");
            tbody.appendChild(tr);
            for (var j = 0; j < param["f_cols"]; ++j) {
                var td = doc.createElement("td");
                tr.appendChild(td);
                if (HTMLArea.is_gecko) {
                    // Mozilla likes to see something
                    // inside the cell.
                    td.appendChild(doc.createElement("br"));
                }
            }
        }
        if (HTMLArea.is_ie) {
            range.pasteHTML(HTMLArea.getHTML(table, true));
        } else {
            // insert the table
            editor.insertNodeAtSelection(table);
        }
        return true;
    }, null);
};

/***************************************************
 *  Category: EVENT HANDLERS
 ***************************************************/

/**
 * Added: allevato, 2010.10.06
 */
HTMLArea.prototype.changeCommentVisibility = function(type) {
    var editor = this;
    var doc = editor._doc;
    var table = doc.getElementById('bigtab');
    var rows = table.rows;
    for (var i = 0; i < rows.length; i++)
    {
        var row = rows[i];
        var lastRow, lastRowType;

        if (row.id.match(/O:\d+/))
        {
            lastRowType = row.className;
        }
        else if (row.id.match(/[INF]\d*:.*/))
        {
            var shouldShow = (type == "showall" || type == lastRowType);
            row.style.display = shouldShow ? 'table-row' : 'none';
        }
    }
};

// txt is the name of the button, as in config.toolbar
HTMLArea.prototype._buttonClicked = function(txt) {
    var editor = this;	// needed in nested functions
    this.focusEditor();
    var btn = this.config.btnList[txt];
    if (!btn) {
        alert("FIXME: Unconfigured button!");
        return false;
    }
    var cmd = btn[0];
    if (typeof cmd == "function") {
        return cmd(this, txt);
    }
    switch (cmd.toLowerCase()) {
        case "htmlmode":
        this.setMode(this._mode != "textmode" ? "textmode" : "wysiwyg");
        break;
        case "forecolor":
        case "backcolor":
        this._popupDialog("select_color.html", function(color) {
            editor._execCommand(cmd, false, "#" + color);
        }, HTMLArea._colorToRgb(this._doc.queryCommandValue(btn[0])));
        break;
        case "createlink":
        this._execCommand(cmd, true);
        break;
        case "insertimage":
        this._insertImage();
        break;
        case "inserttable":
        this._insertTable();
        break;
        case "popupeditor":
        if (HTMLArea.is_ie) {
            window.open(this.popupURL("fullscreen.html"), "ha_fullscreen",
                    "toolbar=no,location=no,directories=no,status=yes,menubar=no," +
                    "scrollbars=no,resizable=yes,width=640,height=480");
        } else {
            window.open(this.popupURL("fullscreen.html"), "ha_fullscreen",
                    "toolbar=no,menubar=no,personalbar=no,width=640,height=480," +
                    "scrollbars=no,resizable=yes");
        }
        // pass this object to the newly opened window
        HTMLArea._object = this;
        break;
           case "about":
        this._popupDialog("about.html", null, null);
        break;
        case "showhidec":
        this._showHideComments();
        break;
        case "newc":
        this._insertComment("");
        break;
        case "deletec":
        this._removeComment();
        break;
        case "help":
        alert("Help not implemented");
        break;
        default:
        this._execCommand(btn[0], false, "");
        break;
    }
    this.updateToolbar();
    return false;
};

// el is reference to the SELECT object
// txt is the name of the select field, as in config.toolbar
HTMLArea.prototype._comboSelected = function(el, txt) {
    this.focusEditor();
    var value = el.options[el.selectedIndex].value;
    switch (txt) {
        case "fontname":
        case "fontsize":
        case "category":
        case "target":
        case "showhidecomments":
        case "historylist":
        if((value != "") && (value != null)){
            this._execCommand(txt, false, value);
        }
        break;
        case "formatblock":
        if (HTMLArea.is_ie) { // sad but true
            value = "<" + value + ">";
        }
        this._execCommand(txt, false, value);
        break;
        default:
        alert("FIXME: combo box " + txt + " not implemented");
        break;
    }
};

// the execCommand function (intercepts some commands and replaces them with
// our own implementation)
HTMLArea.prototype._execCommand = function(cmdID, UI, param) {
    switch (cmdID.toLowerCase()) {
        case "createlink":
        if (HTMLArea.is_ie || !UI) {
            this._doc.execCommand(cmdID, UI, param);
        } else {
            // browser is Mozilla & wants UI
            if ((param = prompt("Enter URL"))) {
                this._doc.execCommand(cmdID, false, param);
            }
        }
        break;
        case "backcolor":
        if (HTMLArea.is_ie) {
            this._doc.execCommand(cmdID, UI, param);
        } else {
            this._doc.execCommand("hilitecolor", UI, param);
        }
        break;
        case "category":
        this._doCategory(param);
        break;
        case "target":
        this._doTarget(param);
        break;
        case "historylist":
        this._doHistory(param);
        break;
        case "showhidecomments":
        this.changeCommentVisibility(param);
        break;
        default:
        //if(this._doc.isContentEditable)
            this._doc.execCommand(cmdID, UI, param);
            //check to see if italic, bold or underline or link was clicked then
              if( (this._wasinside == 1) && (this._lastclickedrow != null) ) // was clicked inside A box
            {
                var clickedId = this._clickedrow;
                //alert(clickedId.id);
                if(clickedId != null)
                {
                    var arr = clickedId.id.split(":");
                    var boxnum = this.getBoxNumber(arr[0]);
                    var parent = arr[1];
                    var reference = arr[2];
                    var idForStyle = "I" + boxnum + ":" + parent + ":" + reference;
                    //check to see if theres a :M in the clickedid
                    if(clickedId.id.indexOf(":M") == -1) // this means IT IS NOT the message element
                    {
                        clickedId = this._doc.getElementById(idForStyle+":M");
                    }
                    // here traverse through all the children of the message element and make sure the
                    //tags have ids
                    var boldTags = clickedId.getElementsByTagName("strong");
                    var italicTags = clickedId.getElementsByTagName("i");
                    var underlineTags = clickedId.getElementsByTagName("u");
                    var anchorTags = clickedId.getElementsByTagName("a");
                    var emTags = clickedId.getElementsByTagName("em");
                    for(var i = 0; i < boldTags.length; i++)
                    {
                        boldTags[i].setAttribute("id", idForStyle);
                    }
                    for(var i = 0; i < italicTags.length; i++)
                    {
                        italicTags[i].setAttribute("id", idForStyle);
                    }
                    for(var i = 0; i < underlineTags.length; i++)
                    {
                        underlineTags[i].setAttribute("id", idForStyle);
                    }
                    for(var i = 0; i < anchorTags.length; i++)
                    {
                        anchorTags[i].setAttribute("id", idForStyle);
                    }
                    for(var i = 0; i < emTags.length; i++)
                    {
                        emTags[i].setAttribute("id", idForStyle);
                    }
                }

            }
        break;
    }
    this.focusEditor();
};

HTMLArea.prototype._doCategory = function(param) {
    var editor = this;	// for nested functions
    var doc = editor._doc;

    var clickedrow = this._clickedrow;
    var nextrow = this._nextrow;
    if(clickedrow == null) // case where user clicks on newly formatted text
    {
        if((this._lastclickedrow != null) && (this._wasinside == 1))// clicked on a newly formatted text, so use the previous clicked row
            clickedrow = this._clickedrow = this._lastclickedrow;
    }
    if(clickedrow != null)
    {
        if(clickedrow.id.charAt(0) == 'I') // if clicked inside a message box
        {
            var arr = clickedrow.id.split(":");
            var boxnum = this.getBoxNumber(arr[0]);
            var parent = arr[1];
            var reference = arr[2];
            var Cidtofind = "I" + boxnum + ":" + parent + ":" + reference + ":C";// search for the element that has the category
            var Ctarget = doc.getElementById(Cidtofind);
            Ctarget.innerHTML = "&nbsp\;" + param + "&nbsp\;"; // replace the inner string with the new category
            var Iidtofind = "I" + boxnum + ":" + parent + ":" + reference + ":I"; // search for the element that has the icon
            var Itarget = doc.getElementById(Iidtofind);
            Itarget.setAttribute("src", this.config.coreResourceURL + "/icons/"+ this.getIcon(param) +".png");// replace the src to be the new icon
            var pts = "";
            if(editor.config.viewPoints) // check to see if the user is authorized to view points
            {
                if (param == "Error")
                    pts = ": -2.0";
                else if(param == "Extra Credit")
                    pts = ": +2.0";
                else
                    pts = "";
            }
            var Pidtofind = "I" + boxnum + ":" + parent + ":" + reference + ":P";// search for the element that has the points
            var Ptarget = doc.getElementById(Pidtofind); // replace the points value
            Ptarget.innerHTML = pts;
            this.highlightLine(doc.getElementById("O:" + parent));
        }
    }
};
HTMLArea.prototype._doTarget = function(param) {
    var editor = this;	// for nested functions
    var doc = editor._doc;

    var clickedrow = this._clickedrow;
    var nextrow = this._nextrow;
    if(clickedrow == null) // case where user clicks on newly formatted text
    {
        if( (this._lastclickedrow != null) && (this._wasinside == 1) )// clicked on a newly formatted text, so use the previous clicked row
            clickedrow = this._clickedrow = this._lastclickedrow;
    }
    if(clickedrow != null)
    {
        if(clickedrow.id.charAt(0) == 'I') // if clicked inside a message box
        {
            var arr = clickedrow.id.split(":");
            var boxnum = this.getBoxNumber(arr[0]);
            var parent = arr[1];
            var reference = arr[2];
            var Tidtofind = "I" + boxnum + ":" + parent + ":" + reference + ":T";// search for the element that has the target
            var Ttarget = doc.getElementById(Tidtofind);
            Ttarget.setAttribute("value", param); // replace the inner string with the new target
        }
    }
};
HTMLArea.prototype._doHistory = function(param) {
    var btn = this._toolbarObjects["historylist"];
    param = param+"$"+btn.element.selectedIndex;
    if((param != "") && (param != null))
        this._insertComment(param);
    btn.element.selectedIndex = 0;

};

HTMLArea.prototype.updateHistory = function() {
    var doc = editor._doc;
    // we now check to see if the last click was inside a box
    if( (this._wasinside == 1) && (this._lastclickedrow != null) ) // was clicked inside A box
    {
        //update the last clicked box's info in the history list
        //search for it in the history list and then replace its value with the new value
        var idarr = this._lastclickedrow.id.split(":");
        var box = this.getBoxNumber(idarr[0]);
        var par = idarr[1];
        var reference = idarr[2];
        if(doc.getElementById("I"+box+":"+par+":"+reference+":C") != null)
              var cat = this.trimString(doc.getElementById("I"+box+":"+par+":"+reference+":C").innerHTML);
        if(doc.getElementById("I"+box+":"+par+":"+reference+":T") != null)
            var tar = doc.getElementById("I"+box+":"+par+":"+reference+":T").getAttribute("value");
        if(doc.getElementById("I"+box+":"+par+":"+reference+":P") != null)
        {
            var pt = doc.getElementById("I"+box+":"+par+":"+reference+":P").innerHTML;
            pt = pt.replace(/:\s*/,'');
            pt = parseFloat(pt);
            if(isNaN(pt))
                pt = "";
        }
        if(doc.getElementById("I"+box+":"+par+":"+reference+":M") != null)
            var msg = doc.getElementById("I"+box+":"+par+":"+reference+":M").innerHTML;
        //replace the id in the messages with &&&&
        msg = msg.replace(/I(\d+)\:(\d+)\:(\d+)/g,"&&&&");
        var newvalue = par+"$"+box+"$"+cat+"$"+tar+"$"+pt+"$"+msg+"$"+box+"$"+"1";
        var arr = "";
        var refnum = "";
        for(var i = 0; i < this._histlist.childNodes.length; i++)
        {
            if(this._histlist.childNodes[i].value != "")// value is not empty (i.e ignore first 2)
            {
                arr = this._histlist.childNodes[i].value.split("$");

                refnum = parseInt(arr[6]);
                if(refnum == reference) //search for that reference num in history list
                {
                    var temppt = parseFloat(arr[4]);
                    if(isNaN(temppt))
                        temppt = "";
                    //first check to see if any changes were made
                    if( (cat != arr[2]) || (tar != arr[3]) || (pt != temppt) || (msg != arr[5]) )
                    {
                        //decrement the occurence
                        if(parseInt(arr[7]) > 1) // more than one rubber stamp (checking occurence)
                        {
                            arr[7] = parseInt(arr[7]) - 1;//decrement the occurence
                            var joinback = arr.join("$");
                            this._histlist.childNodes[i].value = joinback;
                        }
                        else
                        {
                            //remove that item so it could be added to top of list
                            this._histlist.removeChild(this._histlist.item(i));
                        }
                        // replace the reference number to be the box number for that box
                        //for that we have to remove the box and insert a new one with the new reference number
                        var currid = "I"+box + ":" + par + ":" + reference;// id of current box we dealing with
                        var nextrow = doc.getElementById(currid).nextSibling.nextSibling.nextSibling;//get the next row to use later

                        var mainparent = doc.getElementById("tab");// the tbody element
                        for(var h = 0; h < 3; h++)
                        {
                            var parent = doc.getElementById(currid);
                            var kids = parent.childNodes;
                            for(var j = 0; j < kids.length; j++)
                            {
                                parent.removeChild(kids[j]);
                            }
                            mainparent.removeChild(parent);
                        }

                        // make a new comment box
                        var comment = doc.createDocumentFragment();
                        var box_number = box;
                        var parentid = par;
                        reference = box_number;
                        var idTag = "I" + box_number + ":" + parentid + ":" +reference;
                        //Replace &&&& in the ids for style of the message with this idTag.
                        msg = msg.replace(/&&&&/g,idTag);// replace all &&&& with idTags
                        //////////first row/////////
                        var tr1 = doc.createElement("tr");
                        tr1.setAttribute("id", idTag );
                        var td1 = doc.createElement("td");
                        td1.setAttribute("id", idTag );
                        td1.setAttribute("colspan","3");
                        var txt1 = doc.createElement("div");
                        txt1.setAttribute("id", idTag );
                        txt1.innerHTML = "<img id=\"" + idTag
                                + "\" src=\"" + this.config.coreResourceURL + "/images/blank.gif\" width=\"1\" height=\"3\"/>";
                        td1.appendChild(txt1);
                        tr1.appendChild(td1);
                        comment.appendChild(tr1);
                    ////////////second row/////////////////////////////////
                        var tr2 = doc.createElement("tr");
                        tr2.setAttribute("id", idTag );
                        var td2a = doc.createElement("td");
                        td2a.setAttribute("id",idTag );
                        var td2b = doc.createElement("td");
                        td2b.setAttribute("id",idTag );
                        var td2c = doc.createElement("td");
                        td2c.setAttribute("id",idTag );
                        var txt2a = doc.createElement("div");
                        txt2a.setAttribute("id",idTag );
                        var txt2b = doc.createElement("div");
                        txt2b.setAttribute("id",idTag );
                        var txt2c = doc.createElement("div");
                        txt2c.setAttribute("id",idTag );
                        txt2a.innerHTML = "&nbsp\;";
                        td2a.appendChild(txt2a);
                        txt2b.innerHTML = "&nbsp\;";
                        td2b.appendChild(txt2b);
                        if(isNaN(pt))
                            pt = "";
//						if( (pt != "") && (pt != " ") )
//							pt = ": "+pt;
                        txt2c.innerHTML = "<table id=\"" + idTag
                            + ":X\" border=\"0\" cellpadding=\"0\"><tbody id=\"" + idTag
                            + ":B\"><tr id=\"" + idTag + ":R\"><td id=\"" + idTag
                            + ":D\" class=\"messageBox\"><img id=\"" + idTag
                            + ":I\" src=\"" + editor.config.coreResourceURL + "/icons/" + editor.getIcon(cat)
                            + ".png\" border=\"0\"/><input type=\"hidden\" id=\"" + idTag + ":T\" value=\""
                            + tar + "\"/><b id=\"" + idTag + "\"><span id=\"" + idTag
                            + ":C\">&nbsp;" + cat + "&nbsp;</span><span id=\"" + idTag
                            + ":N\">[" + editor.config.userName + "]"
            + ( ( pt == "" || pt == " " ) ? "" : " : " ) + "</span><span id=\""
                            + idTag + ":P\" contentEditable=\"" + editor.config.viewPoints
                            + "\" > " + ( ( pt == "" || pt == " " ) ? "" : pt )
            + "</span></b><br id=\"" + idTag + "\"><i id=\""
                            + idTag + "\"><div id=\"" + idTag
                            + ":M\" contentEditable=\"true\">" + msg
                            + "</div></i></td></tr></tbody></table>";


                        td2c.appendChild(txt2c);
                        td2c.setAttribute("align", "left");

                        tr2.appendChild(td2a);
                        tr2.appendChild(td2b);
                        tr2.appendChild(td2c);
                        comment.appendChild(tr2);

                    /////////third row//////////////////////////////////////////////

                        var tr3 = doc.createElement("tr");
                        tr3.setAttribute("id", idTag);
                        var td3 = doc.createElement("td");
                        td3.setAttribute("colspan","3");
                        td3.setAttribute("id", idTag);
                        var txt3 = doc.createElement("div");
                        txt3.setAttribute("id", idTag);
                        txt3.innerHTML = "<img id=\"" + idTag
                                + "\" src=\"" + this.config.coreResourceURL + "/images/blank.gif\" width=\"1\" height=\"3\"/>";
                        td3.appendChild(txt3);
                        tr3.appendChild(td3);
                        comment.appendChild(tr3);

                        if(nextrow != null) // next row is not the last row, so use insertBefore
                        {
                            nextrow.parentNode.insertBefore(comment, nextrow);
                        }
                        else // no next row. so insertAfter
                        {
                            doc.getElementById("tab").appendChild(comment);
                        }
                        //add a new item
                        var op = document.createElement("option");
                        // remove all tags
                        var shorty = msg.replace(/<(.|\n)+?>/g,"");
                        if ( shorty.length > 25 )
                            shorty = shorty.substr(0,25) + "...";
                        op.appendChild(document.createTextNode(shorty));
                        //newvalue = newvalue.replace(/&&&&/g,idTag);
                        //alert("newvalue that goes in list = "+ newvalue);
                        op.value = newvalue;
                        this._histlist.insertBefore(op, this._histlist.firstChild.nextSibling.nextSibling);// add to end of list
                        this._lastclickedrow = doc.getElementById(idTag);
                        if(doc.getElementById(this._clickedrow.id) == null)
                            this._clickedrow = this._lastclickedrow;
                        break;
                    }
                }
            }
        }
    }
};
HTMLArea.prototype.swapBoxSelection = function(choice, highlightScore) {
    var doc = editor._doc;
    var Carr = this._clickedrow.id.split(":");
    if(choice) // if user now clicked inside a box
    {
        var Cboxnum = this.getBoxNumber(Carr[0]);
        var Cparent = Carr[1];
        var Cref = Carr[2];
        var Cboxcol = doc.getElementById("I"+Cboxnum + ":" + Cparent + ":" + Cref + ":D"); // get refence to that box
        Cboxcol.className = "newMessageBox";
        var highlightNode = doc.getElementById("I" + Cboxnum + ":"
            + Cparent + ":" + Cref +
                        ( highlightScore ? ":P" : ":M" ) );
        if ( HTMLArea.is_ie )
        {
            var range = this._createRange();
            range.moveToElementText( highlightNode );
            range.select();
        }
        else
        {
                var selection = this._getSelection();
                    selection.removeAllRanges();
            var range = this._createRange();
            range.selectNodeContents( highlightNode );
                    selection.addRange( range );
        }
        // we now check to see if the last click was inside a box
        if( (this._wasinside == 1) && (this._lastclickedrow != null) ) // was clicked inside A box
        {
            // unselect that box
            // we know that last click was inside a box but we dont know which one so we check
            // that means user clicked in a diff box last time
            if(this.getBoxNumber(this._clickedrow.id) != this.getBoxNumber(this._lastclickedrow.id))
            {
                // unselect the last box and select this one
                var Larr = this._lastclickedrow.id.split(":");
                var Lboxnum = this.getBoxNumber(Larr[0]);
                var Lparent = Larr[1];
                var Lref = Larr[2];
                var Lboxcol = doc.getElementById("I"+Lboxnum + ":" + Lparent + ":" + Lref +":D");
                Lboxcol.className = "messageBox";
            }
        }
    }
    else // user has now clicked outside a box
    {
        // we check to see if last click was inside a box
        if( (this._wasinside == 1)  && (this._lastclickedrow != null))
        {
            // so unselect it
            var Larr = this._lastclickedrow.id.split(":");
            var Lboxnum = this.getBoxNumber(Larr[0]);
            var Lparent = Larr[1];
            var Lref = Larr[2];
            var Lboxcol = doc.getElementById("I"+Lboxnum + ":" + Lparent + ":" + Lref + ":D");
                  if(Lboxcol != null)
                Lboxcol.className = "messageBox";
        }
        var highlightNode = doc.getElementById( "O:" + Carr[1] );
        if ( HTMLArea.is_ie )
        {
            var range = this._createRange();
            range.moveToElementText( highlightNode );
            range.select();
        }
        else
        {
                var selection = this._getSelection();
                    selection.removeAllRanges();
            var range = this._createRange();
            range.selectNodeContents( highlightNode );
                    selection.addRange( range );
        }
    }
};
/** A generic event handler for things that happen in the IFRAME's document.
 * This function also handles key bindings. */
HTMLArea.prototype._editorEvent = function(ev) {
    var editor = this;
    var keyEvent = (HTMLArea.is_ie && ev.type == "keydown") || (ev.type == "keypress");
        if (keyEvent && ev.ctrlKey) {
        var sel = null;
        var range = null;
        var key = String.fromCharCode(HTMLArea.is_ie ? ev.keyCode : ev.charCode).toLowerCase();
        var cmd = null;
        var value = null;
        switch (key) {
            case 'a':
            if (!HTMLArea.is_ie) {
                // KEY select all
                sel = this._getSelection();
                sel.removeAllRanges();
                range = this._createRange();
                range.selectNodeContents(this._doc.body);
                sel.addRange(range);
                HTMLArea._stopEvent(ev);
            }
            break;

            // simple key commands follow

            case 'b':	// KEY bold
            (!HTMLArea.is_ie) && (cmd = "bold");
            break;
            case 'i':	// KEY italic
            (!HTMLArea.is_ie) && (cmd = "italic");
            break;
            case 'u':	// KEY underline
            (!HTMLArea.is_ie) && (cmd = "underline");
            break;
            case 's':	// KEY justify full
            cmd = "strikethrough";
            break;
            case 'l':	// KEY justify left
            cmd = "justifyleft";
            break;
            case 'e':	// KEY justify center
            cmd = "justifycenter";
            break;
            case 'r':	// KEY justify right
            cmd = "justifyright";
            break;
            case 'j':	// KEY justify full
            cmd = "justifyfull";
            break;

            // headings
            case '1':	// KEY heading 1
            case '2':	// KEY heading 2
            case '3':	// KEY heading 3
            case '4':	// KEY heading 4
            case '5':	// KEY heading 5
            case '6':	// KEY heading 6
            cmd = "formatblock";
            value = "h" + key;
            if (HTMLArea.is_ie) {
                value = "<" + value + ">";
            }
            break;
        }
        if (cmd) {
            // execute simple command
            this._execCommand(cmd, false, value);
            HTMLArea._stopEvent(ev);
        }
    }


    // when mouse click happens
//	var clicked = ( HTMLArea.is_ie && ev.type == "click")
//                      || ( !HTMLArea.is_ie && ev.type == "mouseup" );
    var clicked = ( /* HTMLArea.is_ie && */ ev.type == "click");
    var doc = editor._doc;

    if(clicked) {
    // here we get the location of the cursor where the click occured
    // We retrieve the location by getting the id of the target and then getting its element and seeing what row/col it is in
        var clicked_id = ( HTMLArea.is_ie ? ev.srcElement.id
                                   :  ev.target.getAttribute("id") );
        if(clicked_id != "")// an id exist
        {
            var clicked_elem = doc.getElementById(clicked_id); // get the element from that id
            //if the user clicks on the last row. then the next sibling will return null
            var nextrow = clicked_elem.nextSibling;
                        if ( this._clickedrow != clicked_elem ) {
            this._clickedrow = clicked_elem;
            this._nextrow = nextrow;//remember it for later
            if(clicked_id.charAt(0) == 'I') //if clicked inside a box
            {
                                if ( !HTMLArea.is_ie )
                                {
                                    this._doc.designMode = "on";
                                }
                this.updateHistory();
                this.swapBoxSelection(true, 'P'
                                  == clicked_id.charAt( clicked_id.length - 1 )
                );
                                // remember the last box that was clicked
                this._lastclickedrow = this._clickedrow;
                this._wasinside = 1;
                //update the categories and target.
                this.updateCombos("category");
                this.updateCombos("target");
            }
            else // clicked outside the box
            {
                this.updateHistory();
                this.swapBoxSelection(false, false);
                this._wasinside = 0;
                                if ( !HTMLArea.is_ie )
                                {
                                    this._doc.designMode = "off";
                                }
            }
            }
        }
        else // clicked on something that does not have id
        {
            this._clickedrow = null; //make it null, indicating  no value
            this._nextrow = null;//make it null, indicating  no value
        }
    }

    else if (keyEvent) {
                if ( !HTMLArea.is_ie && this._doc.designMode == "off" )
                {
            HTMLArea._stopEvent(ev);
                }
                else
        // other keys here
        switch (ev.keyCode) {
            case 13: // KEY enter
            // if (HTMLArea.is_ie) {
            this.insertHTML("<br />");
            HTMLArea._stopEvent(ev);
            // }
            break;
        }
    }

    // update the toolbar state after some time
    setTimeout(function() {
        editor.updateToolbar();
    }, 50);
};

// gets called before the form is submitted
HTMLArea.prototype._formSubmit = function(ev) {
    // retrieve the HTML
      // alert("inside formsubmit");
    this._textArea.value = this.getHTML();
};

// retrieve the HTML
HTMLArea.prototype.getHTML = function() {
    switch (this._mode) {
        case "wysiwyg":
        return HTMLArea.getHTML(this._doc.body, false);
        case "textmode":
        return this._textArea2.value;
        default:
        alert("Mode <" + mode + "> not defined!");
        return false;
    }
};

// retrieve the HTML (fastest version, but uses innerHTML)
HTMLArea.prototype.getInnerHTML = function() {
    switch (this._mode) {
        case "wysiwyg":
        return this._doc.body.innerHTML;
        case "textmode":
        return this._textArea2.value;
        default:
        alert("Mode <" + mode + "> not defined!");
        return false;
    }
};

// completely change the HTML inside
HTMLArea.prototype.setHTML = function(html) {
    switch (this._mode) {
        case "wysiwyg":
        this._doc.body.innerHTML = html;
        break;
        case "textmode":
        this._textArea2.value = html;
        break;
        default:
        alert("Mode <" + mode + "> not defined!");
    }
    return false;
};

/***************************************************
 *  Category: UTILITY FUNCTIONS
 ***************************************************/

// browser identification

HTMLArea.agt = navigator.userAgent.toLowerCase();
HTMLArea.is_ie     = ((HTMLArea.agt.indexOf("msie") != -1) && (HTMLArea.agt.indexOf("opera") == -1));
HTMLArea.is_opera  = (HTMLArea.agt.indexOf("opera") != -1);
HTMLArea.is_mac    = (HTMLArea.agt.indexOf("mac") != -1);
HTMLArea.is_mac_ie = (HTMLArea.is_ie && HTMLArea.is_mac);
HTMLArea.is_win_ie = (HTMLArea.is_ie && !HTMLArea.is_mac);
HTMLArea.is_gecko  = (navigator.product == "Gecko");

// variable used to pass the object to the popup editor window.
HTMLArea._object = null;

// FIXME!!! this should return false for IE < 5.5
HTMLArea.checkSupportedBrowser = function() {
    /*
    var gigi = "Navigator:\n\n";
    for (var i in navigator) {
        gigi += i + " = " + navigator[i] + "\n";
    }
    alert(gigi);
    */
    if (HTMLArea.is_gecko) {
        if (navigator.productSub < 20021201) {
            alert("You need at least Mozilla-1.3 Alpha.\n" +
                  "Sorry, your Gecko is not supported.");
            return false;
        }
/*		if (navigator.productSub < 20030210) {
            alert("Mozilla < 1.3 Beta is not supported!\n" +
                  "I'll try, though, but it might not work.");
        }*/
    }
    return HTMLArea.is_gecko || HTMLArea.is_ie;
};

// selection & ranges

// returns the current selection object
HTMLArea.prototype._getSelection = function() {
    if (HTMLArea.is_ie) {
        return this._doc.selection;
    } else {
        return this._iframe.contentWindow.getSelection();
    }
};

// returns a range for the current selection
HTMLArea.prototype._createRange = function(sel) {
    if (HTMLArea.is_ie) {
        if (!sel) sel = this._getSelection();
        return sel.createRange();
    } else {
        this.focusEditor();
        if (sel) {
            return sel.getRangeAt(0);
        } else {
            return this._doc.createRange();
        }
    }
};

// event handling

HTMLArea._addEvent = function(el, evname, func) {
    if (HTMLArea.is_ie) {
        el.attachEvent("on" + evname, func);
    } else {
        el.addEventListener(evname, func, true);
    }
};

HTMLArea._addEvents = function(el, evs, func) {
    for (var i in evs) {
        HTMLArea._addEvent(el, evs[i], func);
    }
};

HTMLArea._removeEvent = function(el, evname, func) {
    if (HTMLArea.is_ie) {
        el.detachEvent("on" + evname, func);
    } else {
        el.removeEventListener(evname, func, true);
    }
};

HTMLArea._removeEvents = function(el, evs, func) {
    for (var i in evs) {
        HTMLArea._removeEvent(el, evs[i], func);
    }
};

HTMLArea._stopEvent = function(ev) {
    if (HTMLArea.is_ie) {
        ev.cancelBubble = true;
        ev.returnValue = false;
    } else {
        ev.preventDefault();
        ev.stopPropagation();
    }
};

HTMLArea._removeClass = function(el, className) {
    if (!(el && el.className)) {
        return;
    }
    var cls = el.className.split(" ");
    var ar = new Array();
    for (var i = cls.length; i > 0;) {
        if (cls[--i] != className) {
            ar[ar.length] = cls[i];
        }
    }
    el.className = ar.join(" ");
};

HTMLArea._addClass = function(el, className) {
    // remove the class first, if already there
    HTMLArea._removeClass(el, className);
    el.className += " " + className;
};

HTMLArea._hasClass = function(el, className) {
    if (!(el && el.className)) {
        return false;
    }
    var cls = el.className.split(" ");
    for (var i = cls.length; i > 0;) {
        if (cls[--i] == className) {
            return true;
        }
    }
    return false;
};

HTMLArea._isBlockElement = function(el) {
    var blockTags = " body form textarea fieldset ul ol dl li div " +
        "p h1 h2 h3 h4 h5 h6 quote pre table thead " +
        "tbody tfoot tr td iframe ";
    return (blockTags.indexOf(" " + el.tagName.toLowerCase() + " ") != -1);
};

HTMLArea._needsClosingTag = function(el) {
    var closingTags = " script style div span ";
    return (closingTags.indexOf(" " + el.tagName.toLowerCase() + " ") != -1);
};

// Retrieves the HTML code from the given node.  This is a replacement for
// getting innerHTML, using standard DOM calls.
HTMLArea.getHTML = function(root, outputRoot) {
    function encode(str) {
        // we don't need regexp for that, but.. so be it for now.
        str = str.replace(/&/ig, "&amp;");
        str = str.replace(/</ig, "&lt;");
        str = str.replace(/>/ig, "&gt;");
        str = str.replace(/\"/ig, "&quot;");
        return str;
    };
    var html = "";
    switch (root.nodeType) {
        case 1: // Node.ELEMENT_NODE
        case 11: // Node.DOCUMENT_FRAGMENT_NODE
        var closed;
        var i;
        if (outputRoot) {
            closed = (!(root.hasChildNodes() || HTMLArea._needsClosingTag(root)));
            html = "<" + root.tagName.toLowerCase();
            var attrs = root.attributes;
            for (i = 0; i < attrs.length; ++i) {
                var a = attrs.item(i);
                if (!a.specified) {
                    continue;
                }
                var name = a.name.toLowerCase();
                if (name.substr(0, 4) == "_moz") {
                    // Mozilla reports some special tags
                    // here; we don't need them.
                    continue;
                }
                var value;
                if (name != 'style') {
                    value = a.value;
                } else { // IE fails to put style in attributes list
                    value = root.style.cssText.toLowerCase();
                }
                if (value.substr(0, 4) == "_moz") {
                    // Mozilla reports some special tags
                    // here; we don't need them.
                    continue;
                }
                value = value.replace(/&/ig, "&amp;");
                value = value.replace(/</ig, "&lt;");
                value = value.replace(/>/ig, "&gt;");
                value = value.replace(/\"/ig, "&quot;");
                html += " " + name + '="' + value + '"';
            }
            html += closed ? " />" : ">";
        }
        for (i = root.firstChild; i; i = i.nextSibling) {
            html += HTMLArea.getHTML(i, true);
        }
        if (outputRoot && !closed) {
            html += "</" + root.tagName.toLowerCase() + ">";
        }
        break;
        case 3: // Node.TEXT_NODE
        html = encode(root.data);
        break;
        case 8: // Node.COMMENT_NODE
        html = "<!--" + root.data + "-->";
        break;		// skip comments, for now.
    }
    return html;
};

// creates a rgb-style color from a number
HTMLArea._makeColor = function(v) {
    if (typeof v != "number") {
        // already in rgb (hopefully); IE doesn't get here.
        return v;
    }
    // IE sends number; convert to rgb.
    var r = v & 0xFF;
    var g = (v >> 8) & 0xFF;
    var b = (v >> 16) & 0xFF;
    return "rgb(" + r + "," + g + "," + b + ")";
};

// returns hexadecimal color representation from a number or a rgb-style color.
HTMLArea._colorToRgb = function(v) {
    // returns the hex representation of one byte (2 digits)
    function hex(d) {
        return (d < 16) ? ("0" + d.toString(16)) : d.toString(16);
    };

    if (typeof v == "number") {
        // we're talking to IE here
        var r = v & 0xFF;
        var g = (v >> 8) & 0xFF;
        var b = (v >> 16) & 0xFF;
        return "#" + hex(r) + hex(g) + hex(b);
    }

    if (v.substr(0, 3) == "rgb") {
        // in rgb(...) form -- Mozilla
        var re = /rgb\s*\(\s*([0-9]+)\s*,\s*([0-9]+)\s*,\s*([0-9]+)\s*\)/;
        if (v.match(re)) {
            var r = parseInt(RegExp.$1);
            var g = parseInt(RegExp.$2);
            var b = parseInt(RegExp.$3);
            return "#" + hex(r) + hex(g) + hex(b);
        }
        // doesn't match RE?!  maybe uses percentages or float numbers
        // -- FIXME: not yet implemented.
        return null;
    }

    if (v[0] == "#") {
        // already hex rgb (hopefully :D )
        return v;
    }

    // if everything else fails ;)
    return null;
};

// modal dialogs for Mozilla (for IE we're using the showModalDialog() call).

// receives an URL to the popup dialog and a function that receives one value;
// this function will get called after the dialog is closed, with the return
// value of the dialog.
HTMLArea.prototype._popupDialog = function(url, action, init) {
    Dialog(this.popupURL(url), action, init);
};

// paths

HTMLArea.prototype.imgURL = function(file) {
    return this.config.editorURL + this.config.imgURL + file;
};

HTMLArea.prototype.popupURL = function(file) {
    return this.config.editorURL + this.config.popupURL + file;
};

// EOF
// Local variables: //
// c-basic-offset:8 //
// indent-tabs-mode:t //
// End: //
