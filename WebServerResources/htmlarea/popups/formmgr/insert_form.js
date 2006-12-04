//document.title = "Form Editor";
  
  var highlightedText = window.dialogArguments;
  var type = "clientemail";
  
  function returnSelected() {
	if (type == "clientemail") {
	    var text = '<FORM CLASS=borders'
			if (document.set.action.value != "") {
			text = text + escape( " action=\"" ) ;
			text = text + escape( document.set.action.value );
		}
		if (document.set.email.value != "") {
			text = text + escape( document.set.email.value );
		}
		else {alert("You must type in a valid e-mail address"); return true;}
		text = text + escape( "\"" ) ;
		
		if (!document.set.method.value ||document.set.method.value == "post"){
		text = text + escape( " method=\"" ) ;
		text = text + escape( document.set.method.value );
		text = text + escape( "\"" ) ;
		}
		
		if (document.set.name.value != "") {
        text = text + escape( " name=" ) ;
		text = text + escape( document.set.name.value );
		}
		else {alert("You must type in a Name"); return true;}
		
		if (document.set.event.value  !== "" && document.set.event.value  == "OnSubmit" ){
		text = text + escape( " OnSubmit=\"" ) ;
		text = text + escape( document.set.inputevent.value );
		text = text + escape( "\"" );
		}
		
		else if (document.set.event.value  !== "" && document.set.event.value  == "OnReset" ){
		text = text + escape( " OnReset=\"" ) ;
		text = text + escape( document.set.inputevent.value );
		text = text + escape( "\"" );
		}
		
		if (document.set.style.value  !== "" ){
		text = text + escape( "Style=\"" );
		text = text + escape( document.set.style.value );
		text = text + escape( "\"" );
		}
		
        text = text + escape( " enctype=" ) ;
		text = text + escape( document.set.enctype.value );
		text = text + escape( ">" );
		text = text + escape("</FORM>") ;
	}
	
	else if (type == "serverscript") {
		var text = escape("<FORM class=borders");
		if (document.set.action.value != "") {
			text = text + escape( " action=\"" ) ;
			text = text + escape( document.set.action.value );
		}		
		
		text = text + escape( "\"" ) ;
		text = text + escape( " method=\"" ) ;
		text = text + escape( document.set.method.value );
		text = text + escape( "\"" ) ;
		
		if (document.set.name.value != "") {
        text = text + escape( " name=" ) ;
		text = text + escape( document.set.name.value );
		}
		else {alert("You must type in a Name"); return true;}
		
		if (document.set.style.value  !== "" ){
		text = text + escape( "Style=\"" );
		text = text + escape( document.set.style.value );
		text = text + escape( "\"" );
		}
		
        text = text + escape( " enctype=" ) ;
		text = text + escape( document.set.enctype.value );
				//text = text + escape( "" ) ;
		text = text + escape( ">" );
		text = text + escape("</FORM>") ;
	}
	
	window.returnValue = text;
	window.close();
  }
    
  function updateTarget() {
	var selectedItem	= document.set.type.selectedIndex;
	var selectedItemValue	= document.set.type.options[selectedItem].value;
	type = selectedItemValue;

	var lblEmail = document.getElementById ("divEmail")
        
	if (type == "clientemail") {
		lblEmail.style.display = "block"
		document.set.email.style.display = "block" ;
		document.set.action.value = "mailto:" ;
		document.set.action.disabled = true ;	
		document.set.fldLayout.style.height = "18.3em" ;	
		window.dialogHeight = "355px" ;
	}
	
	else if (type == "serverscript") {
		lblEmail.style.display = "none"
		document.set.email.style.display = "none" ;
		document.set.action.value = "http://" ;
		document.set.action.disabled = false ;
		document.set.fldLayout.style.height = "18.3em" ;	
		window.dialogHeight = "355px" ;
	}	
  }