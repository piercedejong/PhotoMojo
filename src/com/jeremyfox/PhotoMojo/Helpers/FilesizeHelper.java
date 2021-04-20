package com.jeremyfox.PhotoMojo.Helpers;

import java.io.File;

public class FilesizeHelper extends FileStorageHelperDecorator{
	
	public FilesizeHelper(FileStorageHelper fh) {
		super(fh);
	}

	public void showSize() {
		File file = new File(super.FILE_NAME);
			long size= file.length();
			System.out.println("the size of file is: "+size);	
	}
	public void todo() {
		showSize();
		super.todo();
	}

}
