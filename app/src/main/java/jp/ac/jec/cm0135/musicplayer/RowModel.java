package jp.ac.jec.cm0135.musicplayer;

import java.io.File;

public class RowModel {
//    private String fileName;
//    private long fileSize;

    private File file;

    public RowModel(File file) {
        this.file = file;
    }

//    public RowModel(String fileName, long fileSize) {
//        this.fileName = fileName;
//        this.fileSize = fileSize;
//    }

    public String getFileName() {
        return file.getName();
    }

    public long getFileSize() {
        return file.length();
    }

    public File getFile() {
        return file;
    }


}
