package com.jeremyfox.PhotoMojo.Helpers;

public abstract class FileStorageHelperDecorator extends FileStorageHelper{
	private FileStorageHelper fh;
	
	
	public FileStorageHelperDecorator(FileStorageHelper fh) {
		this.fh=fh;
	}
	
	public void todo() {
		fh.todo();
	}
	
	
}
