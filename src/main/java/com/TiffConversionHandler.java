package com;

import ij.IJ;
import ij.ImagePlus;
import ij.io.Opener;
import ij.plugin.ImagesToStack;
import ij.plugin.StackEditor;
import ij.process.ImageProcessor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class TiffConversionHandler {

    private static final String[] formats = {"TIFF", "8-bit TIFF", "JPEG", "GIF", "PNG", "PGM", "BMP", "FITS", "Text Image", "ZIP", "Raw"};
    private static String format = formats[0];
    private static double scale = 1.0;
    private static boolean useBioFormats = true;
    private static int interpolationMethod = ImageProcessor.BILINEAR;
    private static boolean averageWhenDownSizing = true;


    public TiffConversionHandler() {
    }

    public void convertTo3DTiff(String pathToDirectory) {
        Path dirPath = Paths.get(pathToDirectory);

        if (dirPath.equals("")) {
            System.err.println("Please choose an input folder");
            return;
        }

        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            System.err.println("Input does not exist or is not a folder\n \n"+dirPath.toString());
            return;
        }

        String[] list = (new File(dirPath.toString())).list();

        Opener opener = new Opener();
        opener.setSilentMode(true);
        List<ImagePlus> listOfImagePlus = new ArrayList<>();

        for (int i = 0; i< list.length; i++) {
            Path path = dirPath.resolve(list[i]);
            ImagePlus imp = null;

            if (Files.isDirectory(path)) continue;
            if (list[i].startsWith(".")) continue;

            if (useBioFormats) imp = Opener.openUsingBioFormats(path.toString());
            else imp = opener.openImage(pathToDirectory, list[i]);

            if (imp==null) {
                String reader = useBioFormats?"Bio-Formats not found or":"IJ.openImage()";
                System.err.println(reader+" returned null: "+path);
                continue;
            }

            if (scale!=1.0) {
                int width = (int)(scale*imp.getWidth());
                int height = (int)(scale*imp.getHeight());
                ImageProcessor ip = imp.getProcessor();
                ip.setInterpolationMethod(interpolationMethod);
                imp.setProcessor(null, ip.resize(width,height,averageWhenDownSizing));
                ip = null;
            }

            if (format.equals("8-bit TIFF")) {
                if (imp.getBitDepth()==24) IJ.run(imp, "8-bit Color", "number=256");
                else IJ.run(imp, "8-bit", "");
            }

            listOfImagePlus.add(imp);
            imp.close();
        }

        // Convert TIFF 2D to stacked TIFF
        ImagePlus stackedTiff = convert2DTiffToStackedTiff(listOfImagePlus);

        IJ.saveAs(stackedTiff, format, pathToDirectory.toString());
        stackedTiff.close();
    }

    private ImagePlus convert2DTiffToStackedTiff(List<ImagePlus> listOfImagePlus) {
        ImagePlus stackedTiff = ImagesToStack.run(listOfImagePlus.toArray(new ImagePlus[listOfImagePlus.size()]));
        return stackedTiff;
    }

    /**
     * Convert a 3D Tiff to multiple single Tiff file into the target directory.
     * @param pathToFile
     * @param outputDirectory
     */
    public void convert3DTiffToSingleTiff(String pathToFile, String outputDirectory){
        Path filePath = Paths.get(pathToFile);
        String inputPath = filePath.toString();

        System.out.format("toString: %s%n", inputPath);

        if (inputPath.equals("")) {
            System.err.println("Please choose an input folder");
            return;
        }

        ImagePlus imp = openTiffFile(filePath, inputPath);

        if (imp == null) {
            String reader = useBioFormats ? "Bio-Formats not found or" : "IJ.openImage()";
            System.err.println(reader + " returned null: " + inputPath);
            return;
        }

        Path destinationPath = filePath.getParent().resolve(outputDirectory);

        try {
            Files.createDirectories(Paths.get(destinationPath.toString()));
        } catch (IOException e) {
            System.err.println("Unable to create directory :"+e.getMessage());
        }

        createSingleTiffFiles(imp, destinationPath);
    }

    private ImagePlus openTiffFile(Path filePath, String inputPath) {
        Opener opener = new Opener();
        opener.setSilentMode(true);
        ImagePlus imp;
        if (useBioFormats) imp = Opener.openUsingBioFormats(filePath.toString());
        else imp = opener.openImage(inputPath, filePath.getFileName().toString());
        return imp;
    }

    private void createSingleTiffFiles(ImagePlus imp, Path destinationPath) {
        (new StackEditor()).run("toimages", format, destinationPath, imp);
    }

    public static void main(String args[]){
        // Ex : C:\Users\sam-abu-khdair\imagerie_labo\tests-dataset\FIB-SEM_exemple-donnees\FIB-SEM_exemple-donnees\test_prog\00_raw
        //(new TiffConversionHandler()).convertTo3DTiff(args[0]);

        // Ex : C:\Users\sam-abu-khdair\imagerie_labo\tests-dataset\FIB-SEM_exemple-donnees\FIB-SEM_exemple-donnees\test_prog\00_raw.tif
        //(new TiffConversionHandler()).convert3DTiffToSingleTiff(args[0], "test-output");
    }
}
