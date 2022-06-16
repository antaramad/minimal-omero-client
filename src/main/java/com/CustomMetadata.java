package com;

import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.annotations.NotNull;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class CustomMetadata {

    public CustomMetadata() {
    }

    /**
     * Write all extracted values to stdout.
     */
    private static void print(Metadata metadata, String method)
    {
        System.out.println();
        System.out.println("-------------------------------------------------");
        System.out.print(' ');
        System.out.print(method);
        System.out.println("-------------------------------------------------");
        System.out.println();

        //
        // A Metadata object contains multiple Directory objects
        //
        for (Directory directory : metadata.getDirectories()) {

            //
            // Each Directory stores values in Tag objects
            //
            for (Tag tag : directory.getTags()) {
                System.out.println(tag.getDescription());
            }

            //
            // Each Directory may also contain error messages
            //
            for (String error : directory.getErrors()) {
                System.err.println("ERROR: " + error);
            }
        }
    }


 /*   public static void main(String[] args) {
        CustomMetaData meta = new CustomMetaData();
        int length = args.length;
        for ( int i = 0; i < length; i++ )
            meta.getCustomTiffMetadata(args[i]);
        //TODO get metadata and insert them in a file after that inject them in OMERO

    }*/

   public  Map<String, String> getCustomTiffMetadata(String filepath) {
        File file = new File(filepath);
       Map<String, String> metadataToOMERO = new HashMap<>();

        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            String s = "";

            for (Directory directory : metadata.getDirectories()) {
                for (Tag tag : directory.getTags()) {
                    if (tag.getTagName().equals("TESCAN_CUSTOM")) {
                        s = tag.getDescription().substring(tag.getDescription().indexOf("Date"));
                        String[] tabs = removeAllNonReadableInfos(s);

                        for (String u : tabs) {
                            String[] po = u.split("=");
                            if (po.length > 1)
                                metadataToOMERO.put(removeAllNonAlphanumeriChars(po[0]), po[1] != null ? po[1] : " ");

                            po = null;
                        }
                    }
                }

            }
            //print(metadata, "TIFF");
            //log
            //metadataToOMERO.forEach((k, v) -> System.out.println(k + " - " + v) );
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return metadataToOMERO;
    }

    private String[] removeAllNonReadableInfos(String s) {
        String temp = s.replaceAll("\\r\\n", ";");
        String[] tabs = temp.split(";");
        return tabs;
    }

    @NotNull
    private String removeAllNonAlphanumeriChars(String rawString){
        return rawString.replaceAll("[^A-Za-z0-9]", "");
    }

}