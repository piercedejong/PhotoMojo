package com.jeremyfox.PhotoMojo.Helpers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class FiletypeHelper extends FileStorageHelperDecorator{

	
	public FiletypeHelper(FileStorageHelper fh) {
		super(fh);
		
	}

	public void showType() {
		
		File file = new File(super.FILE_NAME);
		try {
			String type= Files.probeContentType(file.toPath());
			System.out.println("the type of file is: "+type);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void todo() {
		showType();
		super.todo();
		
	}
}
