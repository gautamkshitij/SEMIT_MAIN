package org.zm;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.zm.Copier;

/*
 * A basic format is as follows. fastcopy.exe [/options] file1 file2 ...
 * [/to=dest_dir] Please use space character(' ') as separator(not semicolon).
 * If filename contains space character, please enclose with dobule quotation
 * marks. Ex) fastopy.exe C:\Windows "C:\Program Files" /to="D:\Backup Folder\"
 * Supported options are as follows. (Please don't put space characters before
 * and behind "=") /cmd=(noexist_only|diff|update|sync|force_copy|move|delete)
 * ... Specify operation mode. (In default, diff mode is used. If delete mode is
 * specified, then "/to=" option isn't used.) cmdline in GUI noexist_only Diff
 * (No Overwrite) diff Diff (Size/date) update Diff (update) force_copy Copy
 * (Overwrite) sync Sync (Size/date) move Move (Overwrite) delete Delete
 * /auto_close ... Close automatically after execution is finished with no
 * errors. /force_close ... Close automatically and compulsorily after execution
 * is finished. /open_window ... Don't stored in the task tray. /estimate ...
 * Estimate complete time. /no_exec ... Don't start to execute. /no_confirm_del
 * ... Don't confirm before deleting. /no_confirm_stop ... Don't Show error
 * dialog, Even if critical errors occurred. /error_stop ... Show error dialog
 * (and operation is interrupted), if an error occurred. (to disable,
 * /error_stop=FALSE) /bufsize=N(MB) ... Specify the size(MB) of the main buffer
 * for Read/Write opration. /log ... Write the operation/errors information to
 * the logfile(fastcopy.log). (to disable, /log=FALSE) /logfile=filename ...
 * Specify the filename of logfile. /filelog ... Write to the filelog(detail of
 * copy/delete files). It is stored TIMESTAMP.log in FastCopy/Log directory. If
 * using verify mode, write digest data(default:md5) as additional data. (To
 * specify filelogname, /filelog=filename) /utf8 ... Write to the logfile as
 * UTF-8. /skip_empty_dir ... Skip to create empty directories when /include or
 * /exclude option is used. (to disable, /log=FALSE) /job=job_name ... Specify
 * the job that is already registered. /force_start ... Start at once without
 * waiting for the finish of other FastCopy executing. (to disable, /log=FALSE)
 * /disk_mode=(auto|same|diff) ... Specify Auto/Same/Diff HDD mode. (default:
 * Auto) /speed=(full|autoslow|9-1(90%-10%)|suspend) ... Specify speed control
 * level. /srcfile="files.txt" ... Specify source files by textfile. It is able
 * to describe 1 filename as 1 line. (Attention: If a lot of files are
 * specified, it will take many times to display/refresh Source combobox.)
 * /srcfile_w="files.txt" ... same as "/srcfile=", except describing by UNICODE.
 * /include="..." ... Specify include filter. (details) /exclude="..." ...
 * Specify exclude filter. (details) /from_date="..." ... Specify oldest
 * timestamp filter. (details) /to_date="..." ... Specify newest timestamp
 * filter. (details) /min_size="..." ... Specify minimum size filter. (details)
 * /max_size="..." ... Specify maximum size filter. (details) /wipe_del ...
 * Rename filename and wipe(overwrite Random data) before deleting. /acl ...
 * Copy ACL (only NTFS) (to disable, /acl=FALSE) /stream ... Copy Alternate
 * Stream (only NTFS) (to disable, /stream=FALSE) /reparse ... Copy
 * junction/mountpoint/symlink itself(to disable, /reparse=FALSE) (details)
 * /verify ... Verify written files data by MD5(or SHA-1) (to disable,
 * /verify=FALSE) (details) /linkdest ... Reproduce hardlink as much as
 * possible. (details) /recreate ... Change updating behavior
 * "overwrite the target" to "delete and recreate the target". (If /linkdest
 * option is enabled, this option is enabled tacitly.) If you want always to
 * enable, write [main] recreate=1 in fastcopy.ini. /postproc=actoin_name ...
 * Specify post-process action name (to disable, /postproc=FALSE) Ex) Copy
 * C:\Test Folder to D:\Backup Folder by "diff(Size/Date)" mode is...
 * fastcopy.exe /cmd=diff "C:\Test Folder" /to="D:\Backup Folder\"
 */

/**
 * a file copier using FastCopy on windows
 * 
 * @author zmiller
 */
public class FastCopy implements Copier, Mover, Deleter {

    String FastCopyPath = "C:\\Program Files\\FastCopy\\FastCopy.exe";

    @Override
    public boolean copy(File source, File destination) {
	boolean ret = false;

	String command = FastCopyPath + " /estimate \"" + source.getAbsolutePath() + "\" /to=\"" + destination.getAbsolutePath() + "\"";

	Process p;
	try {
	    p = Runtime.getRuntime().exec(command);
	} catch (IOException e) {
	    return false;
	}

	BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));

	return p.exitValue() == 0;
    }

    @Override
    public boolean isAvailable() {
	boolean ret = false;
	if (System.getProperty("os.name").toLowerCase().contains("windows")) {
	    File fastcopy = new File(FastCopyPath);
	    ret = fastcopy.canExecute();
	}
	return ret;
    }

    @Override
    public boolean move(File source, File destination) {
	boolean ret = false;

	String command = FastCopyPath + " /estimate /cmd=move \"" + source.getAbsolutePath() + "\" /to=\"" + destination.getAbsolutePath() + "\"";

	Process p;
	try {
	    p = Runtime.getRuntime().exec(command);
	} catch (IOException e) {
	    return false;
	}

	BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));

	return p.exitValue() == 0;
    }

    @Override
    public boolean delete(File target) {
	boolean ret = false;

	String command = FastCopyPath + " /estimate /cmd=delete \"" + target.getAbsolutePath() + "\"";

	Process p;
	try {
	    p = Runtime.getRuntime().exec(command);
	} catch (IOException e) {
	    return false;
	}

	BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));

	return p.exitValue() == 0;
    }

    public FastCopy() {
	super();
    }

    public FastCopy(String fastCopyPath) {
	super();
	FastCopyPath = fastCopyPath;
    }

}
