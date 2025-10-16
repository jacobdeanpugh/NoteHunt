package dev.notequest.events;

import java.util.ArrayList;

import dev.notequest.service.FileResult;
import dev.notequest.service.FileResult.FileStatus;

public class SetFilesToCompleteEvent {
    final private ArrayList<FileResult> completedFiles;

    public SetFilesToCompleteEvent(ArrayList<FileResult> completedFiles) {
        this.completedFiles = completedFiles;
    }

    public SetFilesToCompleteEvent() {
        this.completedFiles = new ArrayList<FileResult>();
    }

    public FileResult[] getCompletedFiles() {
        return this.completedFiles.toArray(new FileResult[0]);
    }

    public void addFileResult(FileResult fr) {
        fr.setFileStatus(FileStatus.COMPLETE);
        completedFiles.add(fr);
    }
}
