package com.example.voicerecorder;

public class Recording {
    private String name;
    private String filePath;

    public Recording(String name, String filePath) {
        this.name = name;
        this.filePath = filePath;
    }

    public String getName() {
        return name;
    }

    public String getFilePath() {
        return filePath;
    }

    @Override
    public String toString() {
        return name;
    }
}
