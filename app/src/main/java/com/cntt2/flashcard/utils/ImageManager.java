package com.cntt2.flashcard.utils;


import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImageManager {
    public static Set<String> extractImagePathsFromHtml(String html, Context context) {
        Set<String> imagePaths = new HashSet<>();
        if (html == null || html.isEmpty()) {
            return imagePaths;
        }

        // find all image src paths in the HTML
        Pattern pattern = Pattern.compile("<img[^>]+src=[\"']([^\"']+)[\"'][^>]*>");
        Matcher matcher = pattern.matcher(html);

        while (matcher.find()) {
            String srcPath = matcher.group(1);

            // process path file://
            if (srcPath.startsWith("file://")) {
                srcPath = srcPath.substring(7); // Remove "file://"
            } else if (srcPath.startsWith("content://")) {
                // Convert content URI to file path
                srcPath = getRealPathFromContentUri(Uri.parse(srcPath), context);
            }

            if (srcPath != null) {
                imagePaths.add(srcPath);
                Log.d("ImagePath", "Found image path: " + srcPath);
            }
        }

        return imagePaths;
    }

    private static String getRealPathFromContentUri(Uri contentUri, Context context) {
        try {
            if ("com.cntt2.flashcard.fileprovider".equals(contentUri.getAuthority())) {
                File file = new File(context.getFilesDir(), contentUri.getPath().replace("/my_images/", "images/"));
                return file.getAbsolutePath();
            }
        } catch (Exception e) {
            Log.e("ImagePath", "Error converting content URI to file path: " + e.getMessage());
        }
        return null;
    }

    public static void deleteImageFiles(Set<String> images, Context context) {
        try {
            for (String addedImagePath : images) {
                File fileToDelete = new File(addedImagePath);
                boolean deleted = fileToDelete.delete();
                Log.d("ImageCleanup", "Deleted unused image: " +
                        fileToDelete.getName() + ", success: " + deleted);
            }

        } catch (Exception e) {
            Log.e("ImageCleanup", "Error cleaning up images: " + e.getMessage(), e);
        }
    }

}