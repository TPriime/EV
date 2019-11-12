package com.prime.ev;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

public class ImageCompressor {
    public static void compress(File inputImageFile, File outputImageFile, String format, float quality) throws IOException{
        BufferedImage image = ImageIO.read(inputImageFile);
        OutputStream os = new FileOutputStream(outputImageFile);

        compress(image, os, format, quality);
    }

    public static void compress(BufferedImage inputImageBuffer, File outputImageFile, String format, float quality) throws IOException{
        OutputStream os = new FileOutputStream(outputImageFile);

        compress(inputImageBuffer, os, format, quality);
    }

    public static void compress(BufferedImage inputImage, OutputStream outputImageStream, String format, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(format);
        ImageWriter writer = writers.next();

        ImageOutputStream ios = ImageIO.createImageOutputStream(outputImageStream);
        writer.setOutput(ios);

        ImageWriteParam param = writer.getDefaultWriteParam();

        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);  // Change the quality value you prefer
        writer.write(null, new IIOImage(inputImage, null, null), param);

        outputImageStream.close();
        ios.close();
        writer.dispose();
    }

}
