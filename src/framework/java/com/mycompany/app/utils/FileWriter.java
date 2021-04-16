package com.mycompany.app.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class FileWriter {

    public static void outputScoresToFile(int numberOfSamples, List<Double> resultScores, String dataFolder, String namePrefix) {
        try(FileOutputStream fos = new FileOutputStream(dataFolder + namePrefix + ".txt"))
        {
            int sampleSize = resultScores.size() / numberOfSamples;

            for (int sampleId = 0; sampleId < numberOfSamples; ++sampleId) {
                // Sample doubles to a one line string.
                StringBuilder sampleString = new StringBuilder();
                for (int memberId = 0; memberId < sampleSize; ++memberId) {
                    int idx = sampleId * sampleSize + memberId;
                    sampleString.append(resultScores.get(idx).toString()).append(" ");
                }

                // Write a sample to the file.
                byte[] buffer = (sampleString + "\n").getBytes();
                fos.write(buffer, 0, buffer.length);
            }

            fos.close();
        }
        catch(IOException ex){
            ex.printStackTrace();
        }

        System.out.println("--------------------------------------------------------------------");
        System.out.println("Agent [" + namePrefix + "]: The file has been written");
    }
}
