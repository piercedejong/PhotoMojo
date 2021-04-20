package com.jeremyfox.PhotoMojo.Vendor;

public class GenrateAlbumDirFactory {
	public AlbumStorageDirFactory getAlbumDirFactory(String albumType) {
		if(albumType.equalsIgnoreCase("base")) {
			return new BaseAlbumDirFactory();
		}else if(albumType.equalsIgnoreCase("froyo")) {
			return new FroyoAlbumDirFactory();
		}else {
			return null;
		}
	}

}
