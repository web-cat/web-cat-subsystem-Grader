<?
############################################################
#
# SETUP:::
#	Edit $basedir = "/PATH/TO/YOUR/DIR"; to reflect your site
#
############################################################
############### Set up some variables and functions

############### CHANGE THIS TO YOUR DESIRED ROOT DIRECTORY
############### Add a slash at the start of the line. NO slash at the end.
$basedir = "/usr/local/etc/";
$ROdir = "http://";
$pages = "";

$webname = "";
$updatecheck = "no";

$enable_css = "yes";
$enable_logout = "yes";
$allow_move = "yes";
$allow_download = "yes";
$allow_copy = "yes";
$allow_touch = "yes";
$allow_rename = "yes";
$allow_edit = "yes";
$allow_chmod = "yes";
$allow_delete = "yes";
$allow_upload = "yes";
$allow_create_dir = "yes";
$allow_create_file = "yes";
$allow_display_env = "no";

$not_allowed = "This action is not allowed. Consult your system administrator.";

$version = "2.0";


############### Size for textarea
if($enable_css == "yes")
	{
	$textrows = "27";
	$textcols = "90";
	}
else
	{
	$textrows = "20";
	$textcols = "90";
	}

############### If $wdir (working directory) isn't specified, set it as a slash (/)
if(!$wdir) $wdir="/";

############### HTML ender
$html_ender = "</td></tr></table></body></html>";

############### Calculate image size
function imagesize()
	{
	$size = GetImageSize("$image");
	}

############### HTML header
function html_header(){
	global $basedir;
	global $pages;
	global $wdir;
	global $lastaction;
	global $version;
	global $HTTP_REFERER;
	global $action;
	global $webname;
	global $display;
	global $file;
	global $browse;
	global $raw;
	global $image;
	global $fileurl;
	global $enable_css;
	global $allow_display_env;
	global $enable_logout;
	
	echo "<html style=\"font: 6 pt 'MS Shell Dlg', Helvetica, sans-serif; width: 500px; height: 510px; \">";
	echo "";
	echo "<HEAD>";
	echo "<TITLE>(Dsm) Light $version ($webname)</TITLE>

<script language=\"Javascript1.2\"><!-- // load htmlarea
_editor_url = \"\";                     // URL to htmlarea files
_editor_field = \"\";
var win_ie_ver = parseFloat(navigator.appVersion.split(\"MSIE\")[1]);
if (navigator.userAgent.indexOf('Mac')        >= 0) { win_ie_ver = 0; }
if (navigator.userAgent.indexOf('Windows CE') >= 0) { win_ie_ver = 0; }
if (navigator.userAgent.indexOf('Opera')      >= 0) { win_ie_ver = 0; }
if (win_ie_ver >= 5.5) {
  document.write('<scr' + 'ipt src=\"' +_editor_url+ 'editor.js\"');
  document.write(' language=\"Javascript1.2\"></scr' + 'ipt>');        
} else { document.write('<scr'+'ipt>function editor_generate() { return false; }</scr'+'ipt>'); }
// --></script>
";
	
	############### Cascaded Style Sheets
	if($enable_css == "yes")
		{
		?>
		<STYLE  TYPE="text/css">
		<!--
        BODY { 
		   background: background : ButtonFace; 
		   color: windowtext; 
		   font: 8pt 'MS Shell Dlg', arial, sans-serif; 
		    }
		td {
		font: 7pt
		   }
		input
			{
			font-family : Arial, Helvetica;
			font-size : 8;
			color : #000033;
			font-weight : normal;
   			border-color : #999999;
   			border-width : 1;
   			background-color : #FFFFFF;
			}
		textarea
			{
			font-family : Arial, Helvetica;
			font-size : 10;
			color : #000033;
			font-weight : normal;
   			border-color : #999999;
	   		border-width : 1;
   			background-color : #FFFFFF;
			}
        .tditem {
            font-size: 8px;
            text-decoration: none;
            color: gray;
            }

		-->
		</style>
		<?
		}
	echo "<meta HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=windows-1255\">";
	echo "</HEAD>";
	echo "<BODY  style='background : ButtonFace; color: windowtext; margin: 10px; BORDER-STYLE: none' font-size: 7px;\" link=\"#626262\" vlink=\"#626262\" alink=\"#626262\">";

	echo "<fieldset align=\"center\" width=\"290\">
<legend align=\"left\">(Dsm) Light - $version</legend>";

	echo "<table border=\"0\" align=\"center\" cellspacing=\"2\" cellpadding=\"2\" width=\"100%\"><tr>";

	echo "<td>";
	
	echo " &nbsp; <A HREF=\"$PHP_SELF?action=help&wdir=$wdir\"><img src=\"../images/help.gif\" alt=\"Help for DsM light\" border=\"0\" width=\"16\" height=\"16\"></A> &nbsp; ";	
	
	if($allow_display_env == "yes")
		{
		echo " &nbsp; <A HREF=\"$PHP_SELF?action=env&wdir=$wdir\"><img src=\"../images/env.gif\" alt=\"Environment\" border=\"0\" width=\"16\" height=\"15\"></A> &nbsp; ";
		}

	if($enable_logout == "yes")
		{
		echo " &nbsp; <A onclick=\"self.close()\" HREF=\"$PHP_SELF?action=logout\"><img src=\"../images/DsM_explorer_close.gif\" width=\"18\" height=\"17\" alt=\"Logout and close Window\" border=\"0\"></A> &nbsp; ";
		}



	echo "</td>";

			echo "<td align=\"right\"><A HREF=\"$PHP_SELF?wdir=$wdir\" title=\"Refresh current dir\">$wdir</font><img src=\"../images/refresh.gif\" border=\"0\" width=\"16\" height=\"16\" align=\"absmiddle\" hspace=\"2\"></a> &nbsp; <A HREF=\"$PHP_SELF?action=root\"><img src=\"../images/home.gif\" width=\"14\" height=\"14\" alt=\"Back to Main Dir\" border=\"0\" align=\"absmiddle\" hspace=\"2\"></a></td>
";
	echo "</tr></table></fieldset>";	

	############ We want a BACK link when viewing pictures and raw text.
	if($action == "show")
		{
		echo "<br><a href=\"$HTTP_REFERER\" title=\"Back to previous page\"><img src=\"../images/back.gif\" width=\"20\" height=\"22\" alt=\"Back\" border=\"0\"></a> &nbsp; ";		
		echo "$lastaction<br><br>";
		}

	############ We dont want a BACK link
	else
		{
		echo "<br>$lastaction</font></b><br>";
		}


}

############ File size calculations
function display_size($file){
$file_size = filesize($file);
if($file_size >= 1073741824)
 	{
        $file_size = round($file_size / 1073741824 * 100) / 100 . "g";
	}
elseif($file_size >= 1048576)
	{
        $file_size = round($file_size / 1048576 * 100) / 100 . "m";
	}
elseif($file_size >= 1024)
	{
        $file_size = round($file_size / 1024 * 100) / 100 . "k";
	}
else{
        $file_size = $file_size . "b";
	}
return $file_size;
}

############ List the files function
function list_files()
	{
	global $basedir;
	global $pages;
	global $wdir;	
	global $single;
	global $key;

	global $allow_move;
	global $allow_chmod;
	global $allow_create_file;
	global $allow_create_dir;	
	global $allow_upload;

	global $allow_touch;
	global $allow_delete;

	################## Load directory into array
	$handle=opendir(".");
	while ($file = readdir($handle))
		{
		if(is_file($file)) $filelist[] = $file;
		}
	closedir($handle);

	############### List files
	if($filelist)
		{
		############### Sort the filelist alphabetically
		asort($filelist);
		while (list ($key, $file) = each ($filelist))
			{

############### Registered filetypes. You can add more filetypes here at wish..
############### Check what fileformat it is and give it the correct icon and attributes
			$ext = strrchr ( $file , "." );

			############### gif
			if(!strcasecmp ($ext, ".gif"))
				{
				$icon = "<IMG SRC=\"../images/gif.gif\" alt=\"gif file\" border=\"0\" width=\"20\" height=\"20\">";
				$browse = "1";
				$raw = "0";
				$image = "1";
				}

			############### png
			elseif(!strcasecmp ($ext, ".png"))
				{
				$icon = "<IMG SRC=\"../images/png.gif\" alt=\"PNG file\" border=\"0\" width=\"16\" height=\"16\">";
				$browse = "1";
				$raw = "0";
				$image = "1";
				}

							############### bmp
			elseif(!strcasecmp ($ext, ".bmp"))
				{
				$icon = "<IMG SRC=\"../images/bmp.gif\" alt=\"BMP file\" border=\"0\" width=\"20\" height=\"20\">";
				$browse = "1";
				$raw = "0";
				$image = "1";
				}

							############### CSS
			elseif(!strcasecmp ($ext, ".css"))
				{
				$icon = "<IMG SRC=\"../images/css.gif\" alt=\"CSS file\" border=\"0\" width=\"20\" height=\"20\">";
				$browse = "1";
				$raw = "1";
				$image = "0";
				}
				

							############### PPT
			elseif(!strcasecmp ($ext, ".ppt"))
				{
				$icon = "<IMG SRC=\"../images/ppt.gif\" alt=\"Power point file\" border=\"0\" width=\"19\" height=\"19\">";
				$browse = "1";
				$raw = "0";
				$image = "0";
				}

							############### Excel
			elseif((!strcasecmp ($ext, ".xls")) || (!strcasecmp ($ext, ".csv")))
				{
				$icon = "<IMG SRC=\"../images/xls.gif\" alt=\"Excel file\" border=\"0\" width=\"17\" height=\"17\">";
				$browse = "1";
				$raw = "0";
				$image = "0";
				}
				
							############### Flash
			elseif((!strcasecmp ($ext, ".fla")) || (!strcasecmp ($ext, ".swf")))
				{
				$icon = "<IMG SRC=\"../images/fla.gif\" alt=\"FLASH file\" border=\"0\" width=\"19\" height=\"19\">";
				$browse = "1";
				$raw = "0";
				$image = "0";
				}

							############### PSD
			elseif(!strcasecmp ($ext, ".psd"))
				{
				$icon = "<IMG SRC=\"../images/psd.gif\" alt=\"psd file\" border=\"0\" width=\"19\" height=\"19\">";
				$browse = "1";
				$raw = "0";
				$image = "0";
				}
				
							############### WORD
			elseif(!strcasecmp ($ext, ".doc"))
				{
				$icon = "<IMG SRC=\"../images/word.gif\" alt=\"WORD file\" border=\"0\" width=\"17\" height=\"17\">";
				$browse = "1";
				$raw = "0";
				$image = "0";
				}

							############### php
			elseif((!strcasecmp ($ext, ".php")) || (!strcasecmp ($ext, ".php3")) || (!strcasecmp ($ext, ".phtml")) || (!strcasecmp ($ext, ".php4"))  || (!strcasecmp ($ext, ".phps")))
				{
				$icon = "<IMG SRC=\"../images/php.gif\" alt=\"BMP file\" border=\"0\" width=\"20\" height=\"20\">";
				$browse = "1";
				$raw = "1";
				$image = "0";
				}

			############### jpg
			elseif((!strcasecmp ($ext, ".jpg")) ||  (!strcasecmp ($ext, ".jpeg")))
				{
				$icon = "<IMG SRC=\"../images/jpg.gif\" alt=\"JPG file\" border=\"0\" width=\"20\" height=\"20\">";
				$browse = "1";
				$raw = "0";
				$image = "1";
				}

			############### Textfile
			elseif(!strcasecmp ($ext, ".txt"))
				{
				$icon = "<IMG SRC=\"../images/text.gif\" alt=\"Text\" border=\"0\" width=\"13\" height=\"16\">";
				$browse = "1";
				$raw = "1";
				$image = "0";
				}
		
			############### Audiofile
			elseif((!strcasecmp ($ext, ".wav")) || (!strcasecmp ($ext, ".mp2")) || (!strcasecmp ($ext, ".mp3")) || (!strcasecmp ($ext, ".mp4")) || (!strcasecmp ($ext, ".vqf")) || (!strcasecmp ($ext, ".midi")) || (!strcasecmp ($ext, ".mid")))
				{
				$icon = "<IMG SRC=\"../images/audio.gif\" alt=\"Audio\" border=\"0\" width=\"16\" height=\"16\">";
				$browse = "1";
				$raw = "0";
				$image = "0";
				}

			############### REAL Audio file
			elseif((!strcasecmp ($ext, ".ra")) || (!strcasecmp ($ext, ".ram")))
				{
				$icon = "<IMG SRC=\"../images/real.gif\" alt=\"Real Audio\" border=\"0\" width=\"19\" height=\"19\">";
				$browse = "1";
				$raw = "0";
				$image = "0";
				}
				
			############### Media Player file
			elseif((!strcasecmp ($ext, ".asf")) || (!strcasecmp ($ext, ".aifc")) || (!strcasecmp ($ext, ".asx")) || (!strcasecmp ($ext, ".mp2v")) || (!strcasecmp ($ext, ".mpeg")) || (!strcasecmp ($ext, ".mpg")) || (!strcasecmp ($ext, ".mpe")) || (!strcasecmp ($ext, ".wma")) || (!strcasecmp ($ext, ".wmv")) || (!strcasecmp ($ext, ".wvx")))
				{
				$icon = "<IMG SRC=\"../images/asf.gif\" alt=\"Real Audio\" border=\"0\" width=\"19\" height=\"19\">";
				$browse = "1";
				$raw = "0";
				$image = "0";
				}
				
			############### XML file
			elseif(!strcasecmp ($ext, ".xml"))
				{
				$icon = "<IMG SRC=\"../images/xml.gif\" alt=\"XML file\" border=\"0\" width=\"19\" height=\"19\">";
				$browse = "1";
				$raw = "1";
				$image = "0";
				}
				
			############### Webscript
			elseif((!strcasecmp ($ext, ".asp")) || (!strcasecmp ($ext, ".asa")) || (!strcasecmp ($ext, ".cgi")) || (!strcasecmp ($ext, ".shtml")) || (!strcasecmp ($ext, ".pl")))
				{
				$icon = "<IMG SRC=\"../images/webscript.gif\" alt=\"Web program\" border=\"0\" width=\"15\" height=\"15\">";
				$browse = "1";
				$raw = "1";
				$image = "0";
				}

			############### Apache Webserver security settings
			elseif((!strcasecmp ($ext, ".htaccess")) ||  (!strcasecmp ($ext, ".htpasswd")))
				{
				$icon = "<IMG SRC=\"../images/security.gif\" alt=\"Apache Webserver security settings\" border=\"0\" width=\"15\" height=\"16\">" ;
				$browse = "0";
				$raw = "1";
				$image = "0";
				}

							############### PDF page
			elseif(!strcasecmp ($ext, ".pdf"))
				{
				$icon = "<IMG SRC=\"../images/pdf.gif\" alt=\"Adobe PDF Document\" border=\"0\" width=\"20\" height=\"20\">";
				$browse = "1";
				$raw = "0";
				$image = "0";
				}

							############### RTF page
			elseif(!strcasecmp ($ext, ".rtf"))
				{
				$icon = "<IMG SRC=\"../images/rtf.gif\" alt=\"Rich Text file\" border=\"0\" width=\"18\" height=\"18\">";
				$browse = "1";
				$raw = "0";
				$image = "0";
				}

			############### Web page
			elseif ((!strcasecmp ($ext, ".html")) || (!strcasecmp ($ext, ".htm")))
				{
				$icon = "<IMG SRC=\"../images/webpage.gif\" alt=\"Web page\" border=\"0\" width=\"15\" height=\"15\">";
				$browse = "1";
				$raw = "1";
				$image = "0";
				}

			############### WAP page
			elseif(!strcasecmp ($ext, ".wml"))
				{
				$icon = "<IMG SRC=\"../images/webscript.gif\" alt=\"WAP page\" border=\"0\" width=\"15\" height=\"15\">";
				$browse = "0";
				$raw = "1";
				$image = "0";
				}

			############### Compressed file
			elseif((!strcasecmp ($ext, ".zip")) || (!strcasecmp ($ext, ".tar")) || (!strcasecmp ($ext, ".ace")) || (!strcasecmp ($ext, ".rar")) || (!strcasecmp ($ext, ".gz")))
				{
				$icon = "<IMG SRC=\"../images/zip.gif\" alt=\"Compressed file\" border=\"0\" width=\"20\" height=\"20\">";
				$browse = "0";
				$raw = "0";
				$image = "0";
				}

			############### Unknown
			else
				{ 
				$icon = "<IMG SRC=\"../images/text.gif\" alt=\"Unknown filetype\" border=\"0\" width=\"15\" height=\"15\">";
				$browse = "1";
				$raw = "1";
				$image = "0";
				}
			
			############### List the file(s)
			$filename=$basedir.$wdir.$file;
			$fileurl=rawurlencode($wdir.$file);
			$lastchanged = filectime($filename);
			$changeddate = date("d-m-Y H:i:s", $lastchanged);
			echo "<TR onmouseover=\"bgColor='#d1d1d1'\" onmouseout=\"bgColor='ButtonFace'\">";
			echo "<TD align=\"center\" nobreak>";
			
			############### Make the fileicon clickable for quickviewing
			if($raw == "1")
				{
				echo "<A HREF=\"$PHP_SELF?action=show&wdir=$wdir&file=$fileurl&object=file&browse=$browse&raw=$raw\">";
				}
			
			if($image == "1")
				{
				echo "<A HREF=\"$PHP_SELF?action=show&wdir=$wdir&file=$pages$fileurl&image=$image&object=file&browse=$browse&raw=$raw\">";
				}
			
			echo "$icon</TD>\n";
			echo "<TD nobreak>" . htmlspecialchars($file) . "</TD>\n";
			echo "<TD align=\"right\" nobreak>" . display_size($filename) . "</font></TD>";
			echo "<TD align=\"right\" nobreak>" . $changeddate . "</font></TD><TD align=\"center\">";
		
			############### CHMOD file?
			if($allow_chmod == "yes")
				{
				echo "<A HREF=\"$PHP_SELF?action=chmod&wdir=$wdir&file=$fileurl&browse=$browse&raw=$raw&image=$image&fileurl=$fileurl\" title=\"Change permission level on $file\">";
				}
			printf("%o", (fileperms($filename)) & 0777);
	
			if($allow_chmod == "yes")
				{
				echo "</A>";
				}
	
			echo "</TD><TD nobreak>";
				
			############### Move file?
			if($allow_move == "yes")
				{
				echo " <A HREF=\"$PHP_SELF?action=move&wdir=$wdir&file=$fileurl&object=file&browse=$browse&raw=$raw&image=$image&fileurl=$fileurl\"><img src=\"../images/move.gif\" alt=\"Move, rename or copy $file\" border=\"0\" width=\"16\" height=\"16\" hspace=\"2\"></A> ";
				}
		
			############### Touch file?
			if($allow_touch == "yes")
				{
				echo " <A HREF=\"$PHP_SELF?action=touch&wdir=$wdir&touchfile=$fileurl&browse=$browse&raw=$raw&image=$image&fileurl=$fileurl\"><img src=\"../images/touch.gif\" alt=\"Touch $file\" border=\"0\" width=\"16\" height=\"18\" hspace=\"2\"></A> ";
				}

			############### Delete file?
			if($allow_delete == "yes")
				{
				echo "<A HREF=\"$PHP_SELF?action=del&wdir=$wdir&file=$fileurl&browse=$browse&raw=$raw&image=$image&fileurl=$fileurl\"><img src=\"../images/delete.gif\" alt=\"Delete $file\" border=\"0\" width=\"16\" height=\"16\" hspace=\"2\"></A> ";
				}
	
			############### If the file can be browsed, give it the browse icon
			if($browse == "1")
				{
				echo " <A HREF=\"$pages$wdir$file\"><img src=\"../images/browse.gif\" alt=\"Browse $file\" border=\"0\" width=\"16\" height=\"16\" hspace=\"2\"></A> ";
				}

			############### If the file can be edited, give it the edit icon
			if($raw =="1")
				{
				echo " <A HREF=\"$PHP_SELF?wdir=$wdir&action=edit&display=1&file=$fileurl&browse=$browse&raw=$raw&image=$image&fileurl=$fileurl\"><img src=\"../images/edit.gif\" alt=\"Edit $file\" border=\"0\" width=\"16\" height=\"16\" hspace=\"2\"></A> ";
				}
			}
		}
	}

############ List the directory function
function displaydir()
	{
	global $file;
	global $basedir;
	global $wdir;	
	global $allow_create_file;
	global $allow_create_dir;	
	global $allow_upload;
	global $allow_touch;
	global $allow_delete;
	global $allow_move;
	
	global $single;
	
	############### Draw the head table
	if(isset($single))
		{
		echo "<TABLE BORDER=\"0\" cellspacing=\"1\" cellpadding=\"1\" align=\"center\">";
		}
	else
		{
		echo "<TABLE BORDER=\"0\" cellspacing=\"1\" cellpadding=\"1\" width=\"100%\">";	
		}
	
	echo "<tr style=\" color : white;\">";
	echo "<td bgcolor=\"#626262\">File</td>";
	echo "<td bgcolor=\"#626262\">Name</td>";
	echo "<td bgcolor=\"#626262\">size</td>";
	echo "<td bgcolor=\"#626262\">Date</td>";
	echo "<td bgcolor=\"#626262\">chmod</td>";
	echo "<td bgcolor=\"#626262\">Action</td>";
	echo "</tr>";
	
	################## Load directory into array
	if(!isset($single))
		{
		chdir($basedir . $wdir);
		$handle=opendir(".");
		while ($file = readdir($handle))
			{
			if(is_dir($file)) $dirlist[] = $file;
			}
		closedir($handle);

		############### List directories first		
		if($dirlist)
			{
			############### Sort alphabetically
			asort($dirlist);
			############### Walk through array
			while (list ($key, $file) = each ($dirlist))
				{
				################## Skip the tiresome "."
				if (!($file == "."))
					{
					$filename=$basedir.$wdir.$file;
					$fileurl=rawurlencode($wdir.$file);
					$lastchanged = filectime($filename);
					$changeddate = date("d-m-Y H:i:s", $lastchanged);
					echo "<TR onmouseover=\"bgColor='#bfbfbf'\" onmouseout=\"bgColor='ButtonFace'\">";

					############### Print PARENT arrow
					if($file == "..")
						{
						$downdir = dirname("$wdir");
						echo "<TD align=\"center\" nobreak><A HREF=\"$PHP_SELF?action=chdr&file=$downdir\"><img src=\"../images/parent.gif\" alt=\"Current directory\" border=\"0\" width=\"20\" height=\"16\"></a></TD>\n";
						echo "<TD></TD>\n";
						echo "<TD align=\"right\" nobreak>" . display_size($filename) . "</TD>";
						echo "<TD align=\"right\" nobreak>" . $changeddate . "</TD><TD align=\"center\">";
						printf("%o", (fileperms($filename)) & 0777);
						echo "</TD><TD nobreak>";
						echo "<A HREF=\"$PHP_SELF?action=chdr&file=$downdir\"><img src=\"../images/parent.gif\" alt=\"Parent directory\" border=\"0\" width=\"20\" height=\"16\"></A> ";
						}

					############### List the directory
					else
						{
						$lastchanged = filectime($filename);
						echo "<TD align=\"center\" nobreak><A HREF=\"$PHP_SELF?action=chdr&file=$fileurl\"><img src=\"../images/folder.gif\" alt=\"Change working directory to $file\" border=\"0\" width=\"15\" height=\"13\"></a></TD>\n";
						echo "<TD nobreak>" . htmlspecialchars($file) . "</TD>\n";
						echo "<TD align=\"right\" nobreak>" . display_size($filename) . "</TD>";
						echo "<TD align=\"right\" nobreak>" . $changeddate . "</TD><TD align=\"center\">";
						echo "<A HREF=\"$PHP_SELF?action=chmod&file=$filename\" title=\"Change permission level on $file\">";
						printf("%o", (fileperms($filename)) & 0777);
						echo "</A>";
						echo "</TD><TD nobreak>";

						############### Move directory?
						if($allow_move == "yes")
							{
							echo " <A HREF=\"$PHP_SELF?action=move&wdir=$wdir&file=$fileurl\"><img src=\"../images/move.gif\" alt=\"Rename $file\" border=\"0\" width=\"16\" height=\"16\" hspace=\"2\"></A> ";
							}
						
						############### Touch directory?
						if($allow_touch == "yes")
							{
							echo " <A HREF=\"$PHP_SELF?action=touch&wdir=$wdir&touchfile=$fileurl\"><img src=\"../images/touch.gif\" alt=\"Touch $file\" border=\"0\" width=\"16\" height=\"18\" hspace=\"2\"></A> ";
							}

						############### Delete directory?
						if($allow_delete == "yes")
							{
							echo "<A HREF=\"$PHP_SELF?action=del&wdir=$wdir&file=$fileurl\"><img src=\"../images/delete.gif\" alt=\"Delete $file\" border=\"0\" width=\"16\" height=\"16\" hspace=\"2\"></A> ";
							}
						}
					}	
				}
			}
		list_files();
		echo "</TD></TR>\n";
		echo "</TABLE>";

		############### Display forms for different actions
		echo "<br><fieldset align=\"center\" width=\"290\">
<legend align=\"left\">New Files & Directories</legend><table border=\"0\" width=\"100%\">";

		############### Upload file
		if($allow_upload  == "yes")
			{
			echo "<TR><TD>upload file</td><td>";
			echo "<FORM ENCTYPE=\"multipart/form-data\" METHOD=\"POST\" ACTION=\"$PHP_SELF\">";
			echo "<INPUT TYPE=\"HIDDEN\" NAME=\"wdir\" VALUE=\"$wdir\">";
			echo "<INPUT NAME=\"userfile\" TYPE=\"file\" size=\"40\">";
			echo "<INPUT TYPE=\"SUBMIT\" NAME=\"upload\" VALUE=\"Go!\"></TD></TR>";
			echo "</FORM>";
			}

		############### Create directory
		if($allow_create_dir == "yes")
			{
			echo "<FORM METHOD=\"POST\" ACTION=\"$PHP_SELF\">";	
			echo "<TR><TD>new Directory</td><td>";
			echo "<INPUT TYPE=\"TEXT\" NAME=\"mkdirfile\" size=\"40\">";
			echo "<INPUT TYPE=\"HIDDEN\" NAME=\"action\" VALUE=\"mkdir\">";
			echo "<INPUT TYPE=\"HIDDEN\" NAME=\"wdir\" VALUE=\"$wdir\">";
			echo "<INPUT TYPE=\"SUBMIT\" NAME=\"mkdir\"  VALUE=\"Go!\"></TD></TR>";
			echo "</FORM>";
			}

		############### Create file
		if($allow_create_file == "yes")
			{
			echo "<FORM METHOD=\"POST\" ACTION=\"$PHP_SELF\">";
			echo "<TR><TD>New File</td><td>";
			echo "<INPUT TYPE=\"TEXT\" NAME=\"file\" size=\"40\">";
			echo "<INPUT TYPE=\"HIDDEN\" NAME=\"action\" VALUE=\"createfile\"> ";
			echo "<input type=\"checkbox\" name=\"html\" value=\"yes\"><font size =\"-2\">(template)</font> ";
			echo "<INPUT TYPE=\"HIDDEN\" NAME=\"wdir\" VALUE=\"$wdir\">";
			echo "<INPUT TYPE=\"SUBMIT\" NAME=\"createfile\" VALUE=\"Go!\">";
			echo "</TD></TR>";
			echo "</FORM>";
			}
		echo "</TABLE></fieldset>";
		}

	else
		{
		list_files();
		}
	}



#########################################################################################################
############### The user pressed CANCEL, set the $action to nothing
if($cancel) $action="";

############### User has entered .. as directory. Potential security breach. Deny access.
//$regexp="\\.\\.";
//if (ereg( $regexp, $file, $regs )| ereg( $regexp, $wdir,$regs ))
//{
  //  $lastaction = "ERROR: Directories may not contain the character \"..\"";
    //html_header();
    //echo $html_ender;
    //exit;
//}

############### Upload file
if($upload) 
	{
	copy($userfile,$basedir.$wdir.$userfile_name); 
		$lastaction = "Uploaded $userfile_name to $wdir";
		html_header();
		displaydir();
		echo $html_ender;
		exit;
	}

#########################################################################################################
############### Begin actions code
switch ($action)
{

#########################################################################################################
############### No $action variable? Display initial page
	case "":
		$lastaction = "Listing directory";
		html_header();
		displaydir();
		echo $html_ender;
		break;


#########################################################################################################
############### Help
	case "help":
		$lastaction = "Displaying help";
		html_header();

		if($updatecheck == yes)
			{
			############### Check to see if there is an update
			$filename = "http://www.--------- some other time -----------";
  			$fd = fopen ($filename, "r");
  			$contents = fread ($fd, 1024);
  			fclose ($fd);
		
			############### There is. Give the user information about this
			if($version < $contents)
				{
				echo"<b>NOTE:<br>";
				echo"an update is available.<br>";
				echo"You are currently using ver$version, and ver$contents is out.<br>";
				echo"<hr>";
				}
			}
		?>

 <font face=\"arial\" size="1">
		<ul lang="he" dir="rtl">
		<h3 lang="he" dir="rtl">
		<a href="#introduction"><font face=\"arial\" size="1">1. הקדמה</a></font><br>
		<a href="#requirements"><font face=\"arial\" size="1">2. דרישות מערכת</a></font><br>
		<a href="#filetypes"><font face=\"arial\" size="1">3. תאור סוגי הקבצים</a></font><br>
		<a href="#actions"><font face=\"arial\" size="1">4. תאור הפעולות</a></font><br>
		</h3>
		</ul>
		<hr>
</font>
<fieldset align=\"center\" width=\"290\" lang="he" dir="rtl">
<legend align=\"right\"><a name="introduction">1. הקדמה</a></legend>
מערכת DsM פותחה ע"י Tzvook ונועדה לתת מענה ללקוחות החברה על הצורך בעדכון  אתרי האינטרנט שלהן במו ידיהם, ללא חיובים כספיים נוספים, בזמן אמת, בעלות נמוכה ובמקסימום הבטיחות האפשרית, קובץ עזרה זה מתייחס לסייר הקבצים של המערכת.<br>
<font color="#FF0000" size="1">אנא קראו היטב את ההוראות והיו בטוחים שהנכם מבינים כל פעולה שאתם מבצעים, חלק מפעולות אלו עלולות להסתיים בקבצים שנמחקו ללא יכולת שחזור, או בספרייה פעילה באתר שביטלתם את הגישה אליה מהרשת.</font>
<br>
<br>
<br>
<p align="left">[<a href="#top"><font size="1">לראש העמוד</a>]</font></p>
</fieldset>

<br>

<fieldset align=\"center\" width=\"290\" lang="he" dir="rtl">
<legend align=\"right\"><a name="requirements">2. דרישות מערכת</a></legend>
- שרת התומך ב- PHP4 או גירסה מתקדמת יותר של PHP.<br>
- על המחשב של מעדכן האתר צריך להיות מותקן דפדפן אקספלורר 5.5. ומעלה<br>
 &nbsp; שעודכן לפחות בתחילת שנת 2003 וכולל את MSHTML.<br>
- פלט ה- HTML שנוצר מכוון לתמיכת כל הדפדפנים מגירסאות 4 ומעלה.<br>
<br>
<p align="left">[<a href="#top"><font size="1">לראש העמוד</a>]</font></p>
</fieldset>

<br>

<fieldset align=\"center\" width=\"290\" lang="he" dir="rtl">
<legend align=\"right\"><a name="filetypes">3. תאור סוגי הקבצים</a></legend>
<table border="0" lang="he" dir="rtl" width="100%">
<tr><td colspan="2"><h3 lang="he" dir="rtl" style="background-color:#cdcdcd;"></h3>
<i>(רוב האייקונים מקושרים לקבצים התואמים.)</i>
<ul lang="he" dir="rtl"></td></tr>
		<tr>
<td valign="top"><img src="../images/folder.gif" width="15" height="13"></td>
<td>מראה שהאובייקט הנו תיקיה.<br>
<i>לחץ למעבר לאותה הספריה</i></td>
</tr>
		<tr>
<td valign="top"><img src="../images/security.gif" width="15" height="16"></td>
<td>הקובץ הוא קובץ הגדרות אבטחה או סיסמאות של שרת  Apache (.httaccess או .htpassword).<br>
אפשר ללחוץ ולקבל את קוד המקור.<br>
<font color="#FF0000" size="1">לא מומלץ לשנות את הקובץ!!! הדבר עלול לפגוע בבטיחות השרת כולו או לשינוי הגדרות של גישת גולשים לקבצים שונים ואפילו למחיקה של האתר כולו מהרשת.</font></td>
		</tr>
		<tr>
<td valign="top"><img src="../images/audio.gif" width="16" height="16"></td>
<td>קובץ אודיו</td>
		</tr>
		<tr>
<td valign="top"><img src="../images/webpage.gif" width="15" height="15"></td>
<td>הקובץ הוא או קובץ HTML או HTM . ניתן לעריכה בעזרת הדפדפן.<br>
<i>לחץ לקבלת קוד המקור</i></td>
		</tr>
		<tr>
<td valign="top"><img src="../images/webscript.gif" width="15" height="15"></td>
<td>קובץ PHP, PHPS, PHP2, PHP3, PHP4, PHTML, ASP, ASA, CGI, PL או קובץ SHTML.<br>
הקובץ צריך לעבור הרצת-קוד על השרת בטרם נקרא על ידי הדפדפן.<br>
<i>לחץ לקבלת קוד המקור</i></td>
		</tr>
		<tr>
<td valign="top"><img src="../images/image.gif" width="15" height="15"></td><td>קובץ תמונה (GIF, PNG או JPG).<br>
<i>לחץ לתצוגה מוקדמת בדפדפן.</i></td>
		</tr>
		</table>
<br>
<p align="left">[<font size="1"><a href="#top">לראש העמוד</a>]</font></p>
</fieldset>

<br>

<fieldset align=\"center\" width=\"290\" lang="he" dir="rtl">
<legend align=\"right\"><a name="actions">4. תאור הפעולות הניתנות לביצוע</a></legend>
<table border="0" lang="he" dir="rtl" width="100%">
       <tr>
			<td valign="top"><img src="../images/explore.gif" width="15" height="15"></td>
			<td>רענון תצוגת הספריה הנוכחית.</td>
		</tr>
		<tr>
			<td valign="top"><img src="../images/parent.gif" width="20" height="16"></td>
			<td>מעבר אל הספריה העליונה.</td>
		</tr>
		<tr>
			<td valign="top"><img src="../images/delete.gif" width="16" height="16"></td>
			<td>מחיקת הקובץ או הספריה. לפני ביצוע הפעולה תוצג בקשת אישור.</td>
		</tr>
		<tr>
			<td valign="top"><img src="../images/browse.gif" width="16" height="16"></td>
			<td>הצגת הקובץ בדפדפן.</td>
		</tr>
		<tr>
			<td valign="top"><img src="../images/edit.gif" width="16" height="16"></td>
			<td>עריכת הקובץ בצורת קוד טקסט פשוט.</td>
		</tr>
		<tr>
			<td valign="top"><img src="../images/move.gif" width="16" height="16"></td>
			<td>העברת הקובץ לספריה או למיקום אחר לפי הגדרתך.</td>
		</tr>
		<tr>
			<td valign="top"><img src="../images/touch.gif" width="16" height="18"></td>
			<td>יצירת תו-זמן חדש לקובץ.</td>
		</tr>
		</table>
<br>
<p align="left">[<font size="1"><a href="#top">לראש העמוד</a>]</font></p>
</fieldset>

		<?
		echo $html_ender;
		break;

#########################################################################################################
############### User pressed ROOT.. Change to root dir
	case "root":
   		$wdir="/";
		$lastaction = "Changed to root directory";
		html_header();
		displaydir();
		echo $html_ender;
		break;

#########################################################################################################
############### Display PHP env
	case "env":
		if($allow_display_env == "no")
			{
			$lastaction = $not_allowed;
			html_header();
			displaydir();
			echo $html_ender;
			}
		else
			{
	   		$lastaction = "Displaying PHP environment";
			html_header();
			phpinfo();
			echo $html_ender;
			}
		break;

#########################################################################################################
############### Change directory
	case "chdr":
		$wdir=$file."/";
		$lastaction = "Changed directory to $wdir";
		html_header();
		displaydir();
		echo $html_ender;
		break;

#########################################################################################################
############### Touch object (create a new timestamp)
	case "touch":
		if($allow_touch == "no")
			{
			$lastaction = $not_allowed;
			html_header();
			displaydir();
			echo $html_ender;
			}
		else
			{
			touch($basedir.$touchfile);
			$lastaction = "Touched $touchfile";
			html_header();
			displaydir();
			echo $html_ender;
			}
		break;

#########################################################################################################
############### Bug report form
	case "bugreport":
		if ($send)
			{
 			$lastaction = "Bug reported. Thank you!";
			html_header();
			mail("tzvook@hotmail.com","Bug report","Name: $name \nVersion: $version \n\nProblem: $problem");
			echo "<h3><a href=\"$PHP_SELF?action=help&wdir=$wdir\">Back to help</a></h3>";
			}
		else
			{
			$lastaction = "Bug report form";
			html_header();
			?>
			<table>
			<form action="<? echo "$PHP_SELF?action=bugreport&wdir=$wdir&send=1"; ?>" method="POST">
			<tr>
				<td>Your name:</td>
				<td><input name="name" size="24" maxlength="30"></td>
			</tr>
			<tr>
				<td>Your email adress:</td>
				<td><input name="email" size="24" maxlength="30"></td>
			</tr>
			<tr>
				<td>Description of problem(s):</td>
				<td><textarea name="problem" cols="30" rows="5"></textarea></td>
			</tr>
			<tr>
				<td colspan="2" align="center"><input type="submit" value="Send"></td>
			</tr>	
			</table>
			<?
			}
		echo $html_ender;
		break;

#########################################################################################################
############### Delete file or directory
	case "del":
		############### The user has comfirmed the deletion
		if ($confirm)
			{
			
			############### Object is a directory
			if(is_dir($basedir.$file))
				{
				rmdir($basedir.$file);
				}

			############### Object is a file
			else
				{
				unlink($basedir.$file);
				}
			$lastaction = "Deleted $file";
			html_header();
			displaydir();
			}

		############### Prompt the user for confirmation
		else
			{
			if($raw == "1")
				{
				$lastaction = "Are you sure you want to DELETE<br><A HREF=\"$PHP_SELF?action=show&wdir=$wdir&file=$fileurl\" title=\"View the file in raw format\">$file</a>?";
				}
			elseif($image == "1")
				{
				$lastaction = "Are you sure you want to DELETE<br><A HREF=\"$PHP_SELF?action=show&wdir=$wdir&file=$fileurl&image=$image\" title=\"View the image\">$file</a>?";
				}
			else
				{
				$lastaction = "Are you sure you want to DELETE<br>$file?";
				}

			html_header();
			echo "<center><b><font size =\"5\" face=\"arial, helvetica\"><A HREF=\"$PHP_SELF?action=del&wdir=$wdir&file=$file&confirm=1\">YES!</A></font><br>";
			echo "<p><font size =\"5\" face=\"arial, helvetica\"><A HREF=\"$PHP_SELF?wdir=$wdir\">NO!</A></font><br><b></center>";
			}
		echo $html_ender;
		break;

#########################################################################################################
############### Change permission level
	case "chmod":

		############### The user has confirmed
		if ($confirm)
			{
			$level = "0";
			$level .= $owner;
			$level .= $group;
			$level .= $public;
			$showlevel = $level;
			$level=octdec($level);
			chmod($basedir.$file,$level);
			$lastaction = "Changed permission on $file to $showlevel";
			html_header();
			displaydir();
			}

		############### Prompt the user for confirmation
		else
			{
			$lastaction = "Change permission level on $file";
			html_header();			
			echo "<font face=\"arial, helvetica\"><center><h4>Current level: ";
			printf("%o", (fileperms($basedir.$file)) & 0777);
			echo "</h4><FORM METHOD=\"POST\" ACTION=\"$PHP_SELF\">\n";
			
			function selections($type)  //  type: 0 Owner, 1 Group, 2 Public
				{
				echo "<option value=\"0\""; if (substr($GLOBALS["perm"], $type, 1)=="0") echo "selected"; echo ">0 - No permissions";
				echo "<option value=\"1\""; if (substr($GLOBALS["perm"], $type, 1)=="1") echo "selected"; echo ">1 - Execute";
				echo "<option value=\"2\""; if (substr($GLOBALS["perm"], $type, 1)=="2") echo "selected"; echo ">2 - Write ";
				echo "<option value=\"3\""; if (substr($GLOBALS["perm"], $type, 1)=="3") echo "selected"; echo ">3 - Execute & Write";
				echo "<option value=\"4\""; if (substr($GLOBALS["perm"], $type, 1)=="4") echo "selected"; echo ">4 - Read";
				echo "<option value=\"5\""; if (substr($GLOBALS["perm"], $type, 1)=="5") echo "selected"; echo ">5 - Execute & Read";
				echo "<option value=\"6\""; if (substr($GLOBALS["perm"], $type, 1)=="6") echo "selected"; echo ">6 - Write & Read";
				echo "<option value=\"7\""; if (substr($GLOBALS["perm"], $type, 1)=="7") echo "selected"; echo ">7 - Write, Execute & Read";
				echo "</select>";
				}
			
			$perm = sprintf ("%o", (fileperms($basedir.$file)) & 0777);  // Definition of a variable containing the file permissions
			echo "<p><h4>Owner<br>";
			echo "<select name=\"owner\">";
			selections(0);

			echo "<p>Group<br>";
			echo "<select name=\"group\">";
			selections(1);

			echo "<p>Public<br>";
			echo "<select name=\"public\">";
			selections(2);

			echo "</h4>";
			echo "<p>";
			echo "<INPUT TYPE=\"SUBMIT\" NAME=\"confirm\" VALUE=\"Change\">\n";
			echo "<INPUT TYPE=\"SUBMIT\" NAME=\"cancel\" VALUE=\"Cancel\">\n";
			echo "<INPUT TYPE=\"HIDDEN\" NAME=\"action\" VALUE=\"chmod\">\n";
			echo "<INPUT TYPE=\"HIDDEN\" NAME=\"file\" VALUE=\"$file\">";
			echo "<INPUT TYPE=\"HIDDEN\" NAME=\"wdir\" VALUE=\"$wdir\">";
			echo "</FORM>";
			echo "</center>";
			}
		echo $html_ender;
		break;

#########################################################################################################
############### Move file
	case "move":
		############### The user has confirmed renaming/moving/copying of the object
		if($confirm && $newfile)
 			{
			############### The destination object exists
    			if(file_exists($basedir.$newfile))
				{
				$lastaction = "Destination file already exists. Aborted.";
				}
			else
				{
				if($do == copy)
					{
					copy($basedir.$file,$basedir.$newfile);
					$lastaction = "Copied\n$file to $newfile";
					}
				else
					{
					rename($basedir.$file,$basedir.$newfile);
					$lastaction = "Moved/renamed\n$file to $newfile";
					}
				}
			html_header();
			displaydir();
    			echo $html_ender;
			}

		############### Prompt the user for destination name and action
		else
			{
			if($object == "file")
				{
				if($raw == "1")
					{
					$lastaction = "Moving/renaming or copying <A HREF=\"$PHP_SELF?action=show&wdir=$wdir&file=$fileurl\" title=\"View the file in raw format\">$file</a>";
					}
				elseif($image == "1")
					{
					$lastaction = "Moving/renaming or copying <A HREF=\"$PHP_SELF?action=show&wdir=$wdir&file=$fileurl&image=$image\" title=\"View the image\">$file</a>";
					}
				else
					{
					$lastaction = "Moving/renaming or copying $file";
					}

				html_header();
				echo "<FORM METHOD=\"POST\" ACTION=\"$PHP_SELF\">\n";
				echo "<select name=\"do\">";
				echo "<option value=\"copy\">Copy";
				echo "<option value=\"move\">Move/rename";
				echo "</select> ";
				echo "($file)";
				echo "<h4>To</h4>";
				echo "<INPUT TYPE=\"TEXT\" NAME=\"newfile\" value=\"$file\" size=\"40\">\n";
				echo "<INPUT TYPE=\"HIDDEN\" NAME=\"wdir\" VALUE=\"$wdir\">\n";			
				echo "<INPUT TYPE=\"HIDDEN\" NAME=\"action\" VALUE=\"move\">\n";
				echo "<INPUT TYPE=\"HIDDEN\" NAME=\"file\" VALUE=\"$file\">\n";
				echo "<p>";
				echo "<INPUT TYPE=\"SUBMIT\" NAME=\"confirm\" VALUE=\"Do\">\n";
				echo "<INPUT TYPE=\"SUBMIT\" NAME=\"cancel\" VALUE=\"Cancel\">\n";
				echo "</FORM>";
				echo $html_ender;
				}
			else
				{
				$lastaction = "Renaming $file";

				html_header();
				echo "<FORM METHOD=\"POST\" ACTION=\"$PHP_SELF\">\n";
				echo "<h4>From</h4>";
				echo "$file";
				echo "<h4>To</h4>";
				echo "<INPUT TYPE=\"TEXT\" NAME=\"newfile\" value=\"$file\" size=\"40\">\n";
				echo "<INPUT TYPE=\"HIDDEN\" NAME=\"wdir\" VALUE=\"$wdir\">\n";
				echo "<INPUT TYPE=\"HIDDEN\" NAME=\"action\" VALUE=\"move\">\n";
				echo "<INPUT TYPE=\"HIDDEN\" NAME=\"file\" VALUE=\"$file\">\n";
				echo "<p>";
				echo "<INPUT TYPE=\"SUBMIT\" NAME=\"confirm\" VALUE=\"Do\">\n";
				echo "<INPUT TYPE=\"SUBMIT\" NAME=\"cancel\" VALUE=\"Cancel\">\n";
				echo "<INPUT TYPE=\"HIDDEN\" NAME=\"do\" VALUE=\"move\">\n";
				echo "</FORM>";
				echo $html_ender;
				}
			}
		break;

#########################################################################################################
############### Edit file
	case "edit":

		############### Function for saving the file.
		function savefile()
			{
			global $basedir;
			global $file;
			global $code;
			html_header();
			$fp=fopen($basedir.$file,"w");
			fputs($fp,stripslashes($code));
			fclose($fp);
			}

		function buttons()
			{
			global $file;
			global $wdir;
			
			echo "<center>";
			echo "<INPUT TYPE=\"HIDDEN\" NAME=\"file\" VALUE=\"$file\">";
			echo "<INPUT TYPE=\"HIDDEN\" NAME=\"action\" VALUE=\"edit\">";
			echo "<INPUT TYPE=\"HIDDEN\" NAME=\"wdir\" VALUE=\"$wdir\">";
			echo "<INPUT TYPE=\"RESET\" VALUE=\"Restore original\"> ";
			echo "<INPUT TYPE=\"SUBMIT\" NAME=\"save\" VALUE=\"Save\"> ";
			echo "<INPUT TYPE=\"SUBMIT\" NAME=\"saveexit\" VALUE=\"Save & Exit\"> ";
			echo "<INPUT TYPE=\"SUBMIT\" NAME=\"cancel\" VALUE=\"Cancel / Exit\"> ";
			echo "</center><BR>\n";
			}
					 
		############### The user is done editing. Return to main screen
		if($saveexit)
   			{
    			$lastaction = "Edited <a href=\"$file\" title=\"View the file\">$file</a>";
			savefile();
			displaydir();
			}

		############### Save the file, but continue editing.
		if($save)
   			{
    			$lastaction = "Saved <a href=\"$file\" title=\"View the file\">$file</a>, still editing.";
			savefile();
			echo "<FORM METHOD=\"POST\" ACTION=\"$PHP_SELF\">\n";
			$fp=fopen($basedir.$file,"r");
			$contents=fread($fp,filesize($basedir.$file));
			echo "<TEXTAREA NAME=\"code\" rows=\"$textrows\" cols=\"$textcols\">\n";
			echo htmlspecialchars($contents);
			echo "</TEXTAREA><script language='javascript1.2'>
editor_generate('code');
</script><BR>\n";
			echo "<center>";
			buttons();
			echo "</FORM>";
			}

		############### Display file in textarea
		if($display)
			{
			if($raw == "1")
				{
				$lastaction = "Editing <A HREF=\"$PHP_SELF?action=show&wdir=$wdir&file=$file&browse=$browse&raw=$raw&image=$image&fileurl=$fileurl\" title=\"View the file in raw format\">$file</a>";
				}
			elseif($image == "1")
				{
				$lastaction = "Editing <A HREF=\"$PHP_SELF?action=show&wdir=$wdir&file=$fileurl&image=$image\" title=\"View the image\">$file</a>";
				}
			else
				{
				$lastaction = "Editing $file";
				}
	
			html_header();
			echo "<FORM METHOD=\"POST\" ACTION=\"$PHP_SELF\">\n";
			$fp=fopen($basedir.$file,"r");
			$contents=fread($fp,filesize($basedir.$file));
			echo "<TEXTAREA NAME=\"code\" style=\"width:700; height:200;\" rows=\"$textrows\" cols=\"$textcols\">\n";
			echo htmlspecialchars($contents);
			echo "</TEXTAREA><script language='javascript1.2'>
editor_generate('code');
</script><BR>\n";
			buttons();
			echo "</FORM>";
			}
		echo $html_ender;
		break;

		
#########################################################################################################
############### Display file
	case "show":

		############### Display file in textformat
		$filelocation = $wdir.$file;	

		$lastaction = "Displaying $file";
		html_header();

		############### It is a picture, display it. The filename needs to be either relative to the current document, or an absolute filesystem path. 
		if($image == "1")
			{
			$size = GetImageSize($DOCUMENT_ROOT.$file);
			echo "<center><img src=\"$file\" $size[3]></center>";
			}

		############### It is text, display it.
		else
			{
			$single = "yes";
#			displaydir();
			echo"<hr><br>";
			show_source($basedir.$file);
			}
		echo $html_ender;
		break;

#########################################################################################################
############### Create directory
	case "mkdir":
		
		############### Is the action allowed?
		if($allow_create_dir == "no")
			{
			$lastaction = $not_allowed;
			html_header();
			}
		else
			{
			############### The directory already exists. 
			if(file_exists($basedir.$wdir.$mkdirfile))
				{
				$lastaction = "The directory $wdir$mkdirfile allready exists.";
				html_header();
				}

			############### Create directory
			else
				{
				$lastaction = "Created the directory $wdir$mkdirfile";
				html_header();
				mkdir($basedir.$wdir.$mkdirfile,0750);
				}
			displaydir();
			echo $html_ender;
			}
		break;

#########################################################################################################
############### Create file
	case "createfile":
		$filelocation = $wdir.$file;

		############### The user is done editing. Return to main screen
		if($done == "1")
   			{
			$lastaction = "Created $file";
			html_header();

#			if($convert == "yes")
#   				{
#				$code = str_replace ("\n", "<BR>");
#				}
		
			$fp=fopen($basedir.$filelocation,"w");
			fputs($fp,stripslashes($code));
      			fclose($fp);
			displaydir();
			}

		############### Display a textarea that will be the file
		else
			{

   			############### The file allready exists
   			if(file_exists($basedir.$filelocation))
   				{
   				$lastaction = "$file allready exists.";
				html_header();
				displaydir();
				}

			############### Give the user a textarea to write the contents of file
			else
				{
				$lastaction = "Creating $file";
				html_header();
				echo "<FORM METHOD=\"POST\" ACTION=\"$PHP_SELF\">\n";
				echo "<INPUT TYPE=\"HIDDEN\" NAME=\"file\" VALUE=\"$file\">\n";
				echo "<INPUT TYPE=\"HIDDEN\" NAME=\"action\" VALUE=\"createfile\">\n";
				echo "<INPUT TYPE=\"HIDDEN\" NAME=\"wdir\" VALUE=\"$wdir\">\n";
				echo "<INPUT TYPE=\"HIDDEN\" NAME=\"done\" VALUE=\"1\">\n";
				echo "<TEXTAREA NAME=\"code\" rows=\"$textrows\" cols=\"$textcols\">\n";

				############### The user selected to use a html template. Put it inside the textarea
				if(isset($html))
					{
					echo "<HTML><HEAD><title>תור-חיפה:  כותרת צריכה לבו כאן </title>\n";
					echo "<META NAME=\"description\" CONTENT=\"HAIFA: Mountain City By the Sea: The Official Haifa Tourism-Info Site.  all the Information you'll need about Haifa, Israel, Brought to you by the Haifa Tourists Board\">\n";
					echo "<META NAME=\"keywords\" CONTENT=\"Haifa, Israel, Bahai, Tours, Religions, tempel Green, Cafe, pubs, tourism, Beaches, sea, mountain, beautiful, zoo, hotels, B&B, What's new in Haifa, Mountain City By the Sea\">\n";
					echo "<?php // Include more Meta tags & some HEAD scripts \n";
					echo "include (\"../../includes/metaH.php\");\n";
					echo " ?>\n";
					echo "</HEAD>\n";
					echo "<BODY BGCOLOR=\"#ffffff\" marginwidth=\"0\" marginheight=\"0\" bottommargin=\"0\" leftmargin=\"0\" rightmargin=\"0\" topmargin=\"0\" background=\"http://www.tour-haifa.co.il/pix/bg.gif\">\n";

					echo "<?php    // Title Pic+link Variables\n";
					echo "\$titlePIC = \"../images/databasses.gif\";\n";
					echo "\$titleLINK = \"BnB.phtml\"; \n";
					echo "   // Include Title bar & Navigation Bar\n";
					echo "include (\"../../includes/Hebnavigation.php\");\n";
										
					echo "?>\n";
					echo "<!-- CONTENT -->\n";
					echo "<SNIPPET NAME=\"תבנית ריקה\">\n";

					echo "\n\n\n";
					echo "\n\n";
					echo "</SNIPPET>\n";
					echo "<!-- /CONTENT --><br>\n\n";
					echo "<?php // Include more Meta tags & some HEAD scripts\n";
					echo "include (\"http://www.tour-haifa.co.il/includes/footH.php\");\n";
					echo "?>\n";

					}
				echo "</TEXTAREA><BR>\n";
				echo "<center><INPUT TYPE=\"SUBMIT\" NAME=\"confirm\" VALUE=\"Create\">\n";
				echo "<INPUT TYPE=\"SUBMIT\" NAME=\"cancel\" VALUE=\"Cancel\"><br>";
				$ext = strrchr ( $file , "." );
				if(!strcasecmp ($ext, ".txt"))
					{
					echo "<input type=\"checkbox\" name=\"convert\" value=\"yes\">(convert line returns to BR)";					
					}
				echo "</center><BR>\n</FORM>";
				}
			}
		echo $html_ender;
		break;
}

?>